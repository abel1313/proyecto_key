package com.ventas.key.mis.productos.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ventas.key.mis.productos.redessociales.FacebookCommentBotService;
import com.ventas.key.mis.productos.redessociales.InstagramCommentBotService;
import com.ventas.key.mis.productos.redessociales.InstagramDirectMessageBotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

// Webhook de Meta para comentarios de Facebook/Instagram y mensajes directos de Instagram --
// endpoint público (Meta lo llama directo, sin nuestro JWT, ver SecurityConfig). Una sola URL
// recibe todo: Meta distingue el tipo de evento con el campo "object" del payload ("page" para
// Facebook, "instagram" para Instagram) y la forma del evento (arreglo "changes" para comentarios,
// "messaging" para DMs) -- por eso no hizo falta registrar una URL nueva para cada uno, solo una
// suscripción adicional sobre el mismo callback_url. Dispara FacebookCommentBotService,
// InstagramCommentBotService o InstagramDirectMessageBotService según corresponda.
@Tag(name = "Facebook Webhook", description = "Recibe eventos de comentarios y mensajes directos de Meta (Facebook e Instagram) y dispara el bot de respuestas")
@RestController
@RequestMapping("/v1/redes-sociales/facebook")
@RequiredArgsConstructor
@Slf4j
public class FacebookWebhookController {

    private final FacebookCommentBotService commentBotService;
    private final InstagramCommentBotService instagramCommentBotService;
    private final InstagramDirectMessageBotService instagramDirectMessageBotService;
    private final ObjectMapper objectMapper;

    @Value("${facebook.webhook-verify-token:}")
    private String verifyToken;

    @Value("${facebook.app-secret:}")
    private String appSecret;

    @Operation(
        summary = "Verificación del webhook (paso único, lo llama Meta al registrar la URL)",
        description = "Meta llama esto UNA VEZ al configurar la URL del webhook en el portal de desarrolladores, " +
                "para confirmar que el servidor responde -- hay que devolver hub.challenge tal cual si " +
                "hub.verify_token coincide con FACEBOOK_WEBHOOK_VERIFY_TOKEN."
    )
    @GetMapping("/webhook")
    public ResponseEntity<String> verificarWebhook(
            @RequestParam("hub.mode") String modo,
            @RequestParam("hub.verify_token") String tokenRecibido,
            @RequestParam("hub.challenge") String challenge) {

        if ("subscribe".equals(modo) && !verifyToken.isBlank() && verifyToken.equals(tokenRecibido)) {
            return ResponseEntity.ok(challenge);
        }
        log.warn("Verificación de webhook de Facebook rechazada (modo={}, token no coincide)", modo);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    @Operation(
        summary = "Recibir evento de comentario (lo llama Meta, no el front)",
        description = "Se dispara cada vez que hay actividad en el feed de la página suscrita a este webhook. " +
                "Solo procesa comentarios nuevos (item=comment, verb=add); todo lo demás se ignora. " +
                "Valida la firma X-Hub-Signature-256 contra FACEBOOK_APP_SECRET antes de procesar nada."
    )
    @PostMapping("/webhook")
    public ResponseEntity<Void> recibirWebhook(
            @RequestBody String rawBody,
            @RequestHeader(value = "X-Hub-Signature-256", required = false) String firma) {

        if (!firmaValida(rawBody, firma)) {
            log.warn("Firma inválida en webhook de Facebook, payload ignorado");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        try {
            procesarPayload(rawBody);
        } catch (Exception e) {
            log.error("Error procesando webhook de Facebook", e);
        }
        // Siempre 200 aunque algo interno haya fallado -- si respondemos error, Meta reintenta
        // el mismo evento varias veces y puede terminar duplicando respuestas del bot.
        return ResponseEntity.ok().build();
    }

    private boolean firmaValida(String rawBody, String firmaHeader) {
        if (appSecret.isBlank()) {
            log.warn("FACEBOOK_APP_SECRET no configurado -- aceptando webhook SIN validar firma (inseguro, configurar en cuanto se pueda)");
            return true;
        }
        if (firmaHeader == null || !firmaHeader.startsWith("sha256=")) {
            return false;
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(appSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(rawBody.getBytes(StandardCharsets.UTF_8));
            String calculada = "sha256=" + HexFormat.of().formatHex(hash);
            return MessageDigest.isEqual(
                    calculada.getBytes(StandardCharsets.UTF_8), firmaHeader.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.error("Error validando firma del webhook de Facebook", e);
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    private void procesarPayload(String rawBody) throws Exception {
        Map<String, Object> payload = objectMapper.readValue(rawBody, Map.class);
        String object = stringDe(payload.get("object"));
        List<Map<String, Object>> entries = (List<Map<String, Object>>) payload.get("entry");
        if (entries == null) return;

        for (Map<String, Object> entry : entries) {
            List<Map<String, Object>> changes = (List<Map<String, Object>>) entry.get("changes");
            if (changes != null) {
                for (Map<String, Object> change : changes) {
                    if ("instagram".equals(object)) {
                        procesarCambioInstagram(change);
                    } else {
                        procesarCambioFacebook(change);
                    }
                }
            }

            // Los mensajes directos vienen en un arreglo distinto ("messaging"), no en "changes"
            // -- mismo formato que usa Meta para Messenger. Solo aplica a Instagram por ahora.
            List<Map<String, Object>> messaging = (List<Map<String, Object>>) entry.get("messaging");
            if (messaging != null && "instagram".equals(object)) {
                for (Map<String, Object> evento : messaging) {
                    procesarMensajeDirectoInstagram(evento);
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void procesarCambioFacebook(Map<String, Object> change) {
        if (!"feed".equals(change.get("field"))) return;
        Map<String, Object> value = (Map<String, Object>) change.get("value");
        if (value == null) return;
        if (!"comment".equals(value.get("item")) || !"add".equals(value.get("verb"))) return;

        String commentId = stringDe(value.get("comment_id"));
        String postId = stringDe(value.get("post_id"));
        String parentId = stringDe(value.get("parent_id"));
        String mensaje = (String) value.get("message");
        Map<String, Object> from = (Map<String, Object>) value.get("from");
        String autorId = from != null ? stringDe(from.get("id")) : null;

        commentBotService.procesarComentario(commentId, postId, parentId, mensaje, autorId);
    }

    // Sin verificar contra la API real todavia -- primera vez que este proyecto recibe webhooks
    // de comentarios de Instagram, escrito siguiendo la forma documentada del payload (campo
    // "comments", value.id/text/from/media/parent_id). Si algun campo llega distinto a lo
    // esperado, revisar aqui primero.
    @SuppressWarnings("unchecked")
    private void procesarCambioInstagram(Map<String, Object> change) {
        if (!"comments".equals(change.get("field"))) return;
        Map<String, Object> value = (Map<String, Object>) change.get("value");
        if (value == null) return;

        String commentId = stringDe(value.get("id"));
        String parentId = stringDe(value.get("parent_id"));
        String mensaje = (String) value.get("text");
        Map<String, Object> from = (Map<String, Object>) value.get("from");
        String autorId = from != null ? stringDe(from.get("id")) : null;
        Map<String, Object> media = (Map<String, Object>) value.get("media");
        String postId = media != null ? stringDe(media.get("id")) : null;

        instagramCommentBotService.procesarComentario(commentId, postId, parentId, mensaje, autorId);
    }

    // Formato "messaging" (igual al de Messenger): cada evento trae sender/recipient/message.mid/
    // message.text, y opcionalmente message.is_echo=true cuando el mensaje lo mando la propia
    // cuenta (bot o admin manual) -- en ese caso el remitente real es la pagina y el destinatario
    // es el cliente, al reves que en un mensaje entrante normal. Sin verificar contra la API real
    // todavia -- primera vez que este proyecto recibe DMs de Instagram, escrito siguiendo la forma
    // documentada del payload. Si algun campo llega distinto a lo esperado, revisar aqui primero.
    @SuppressWarnings("unchecked")
    private void procesarMensajeDirectoInstagram(Map<String, Object> evento) {
        Map<String, Object> message = (Map<String, Object>) evento.get("message");
        if (message == null) return;

        Map<String, Object> sender = (Map<String, Object>) evento.get("sender");
        Map<String, Object> recipient = (Map<String, Object>) evento.get("recipient");
        String senderId = sender != null ? stringDe(sender.get("id")) : null;
        String recipientId = recipient != null ? stringDe(recipient.get("id")) : null;
        String mid = stringDe(message.get("mid"));
        String texto = (String) message.get("text");
        boolean esEcho = Boolean.TRUE.equals(message.get("is_echo"));

        instagramDirectMessageBotService.procesarMensaje(mid, senderId, recipientId, texto, esEcho);
    }

    private String stringDe(Object valor) {
        return valor != null ? String.valueOf(valor) : null;
    }
}
