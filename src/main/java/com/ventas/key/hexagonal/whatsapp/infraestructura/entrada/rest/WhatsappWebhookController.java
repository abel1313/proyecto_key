package com.ventas.key.hexagonal.whatsapp.infraestructura.entrada.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * [Hexagonal: Driving Adapter] Webhook de la API de WhatsApp de Meta: {@code /v1/whatsapp/webhook}.
 * Público (Meta lo llama sin nuestro JWT, ver {@code SecurityConfig}); toda petición se valida con la
 * firma {@code X-Hub-Signature-256} y el App Secret. Responde 200 de inmediato y procesa aparte.
 * Sin lógica de negocio: solo traduce y encola.
 */
@Tag(name = "WhatsApp Webhook", description = "Recibe de Meta los mensajes de WhatsApp y los avisos de entrega")
@RestController
@RequestMapping("/v1/whatsapp/webhook")
public class WhatsappWebhookController {

    private static final Logger log = LoggerFactory.getLogger(WhatsappWebhookController.class);

    private final WhatsappPayloadParser parser;
    private final WhatsappProcesador procesador;

    @Value("${whatsapp.cloud.verify-token:}")
    private String verifyToken;

    @Value("${whatsapp.cloud.app-secret:}")
    private String appSecret;

    WhatsappWebhookController(WhatsappPayloadParser parser, WhatsappProcesador procesador) {
        this.parser = parser;
        this.procesador = procesador;
    }

    @Operation(summary = "Verificación del webhook (la llama Meta al registrar la URL)",
            description = "Devuelve hub.challenge tal cual si hub.verify_token coincide con WHATSAPP_VERIFY_TOKEN.")
    @GetMapping
    public ResponseEntity<String> verificar(@RequestParam("hub.mode") String modo,
                                            @RequestParam("hub.verify_token") String tokenRecibido,
                                            @RequestParam("hub.challenge") String challenge) {
        boolean coincide = !verifyToken.isBlank() && MessageDigest.isEqual(
                verifyToken.getBytes(StandardCharsets.UTF_8), tokenRecibido.getBytes(StandardCharsets.UTF_8));
        if ("subscribe".equals(modo) && coincide) {
            return ResponseEntity.ok(challenge);
        }
        log.warn("Verificación del webhook de WhatsApp rechazada (modo={}, token no coincide)", modo);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    @Operation(summary = "Recibir mensajes y avisos de entrega (lo llama Meta, no el front)",
            description = "Valida X-Hub-Signature-256 contra WHATSAPP_APP_SECRET, responde 200 enseguida y procesa aparte.")
    @PostMapping
    public ResponseEntity<Void> recibir(@RequestBody byte[] cuerpo,
                                        @RequestHeader(value = "X-Hub-Signature-256", required = false) String firma) {
        if (!FirmaDeMeta.valida(appSecret, cuerpo, firma)) {
            log.warn("Firma inválida (o sin WHATSAPP_APP_SECRET) en el webhook de WhatsApp: petición rechazada");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        PayloadWhatsapp payload;
        try {
            payload = parser.parsear(cuerpo);
        } catch (Exception e) {
            // Firma válida pero cuerpo ilegible: 200 para que Meta no lo reintente en bucle.
            log.error("Payload ilegible en el webhook de WhatsApp", e);
            return ResponseEntity.ok().build();
        }
        if (payload.vacio()) {
            return ResponseEntity.ok().build();
        }
        // Cola llena: 503 para que Meta reintente más tarde (el id único del mensaje evita duplicados).
        return procesador.encolar(payload)
                ? ResponseEntity.ok().build()
                : ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
    }
}
