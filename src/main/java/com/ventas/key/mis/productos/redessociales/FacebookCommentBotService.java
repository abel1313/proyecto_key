package com.ventas.key.mis.productos.redessociales;

import com.ventas.key.mis.productos.chatbot.ChatbotBlockService;
import com.ventas.key.mis.productos.chatbot.ChatbotService;
import com.ventas.key.mis.productos.entity.productoVariantes.Variantes;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

// Orquesta el bot de comentarios de Facebook: recibe el evento ya parseado del webhook
// (FacebookWebhookController), decide si contesta o se calla, y si contesta reusa el mismo
// "cerebro" del chat del sitio (ChatbotService) -- misma logica de entender/resolver, distinto
// canal de salida.
@Service
@Slf4j
@RequiredArgsConstructor
public class FacebookCommentBotService {

    private final ChatbotService chatbotService;
    private final ChatbotBlockService blockService;
    private final FacebookGraphClient facebookGraphClient;
    private final IPublicacionSocialRepository publicacionSocialRepository;
    private final IComentarioSocialRepository comentarioSocialRepository;

    @Value("${facebook.page-id:}")
    private String pageId;

    // Facebook espera una respuesta rapida al webhook (idealmente unos pocos segundos) -- se
    // procesa en el mismo hilo por simplicidad (OpenAI + Facebook, ~2-4s en total). Si en
    // produccion esto empieza a causar timeouts/reintentos de Meta, mover a un job async (el
    // proyecto ya tiene RabbitMQ para eso, ver PublicacionSocialScheduler como referencia).
    public void procesarComentario(String commentId, String postId, String comentarioTexto, String autorId) {
        if (autorId != null && autorId.equals(pageId)) {
            // Evita que el bot se responda a si mismo -- nuestras propias respuestas tambien
            // disparan este mismo webhook (son comentarios "add" en el feed de la pagina).
            return;
        }
        if (comentarioTexto == null || comentarioTexto.isBlank() || commentId == null) {
            return;
        }

        // Idempotencia: Meta a veces reenvia el mismo evento -- si ya existe una fila con este
        // commentId, no se vuelve a procesar (evita responder duplicado).
        if (comentarioSocialRepository.findByCommentId(commentId).isPresent()) {
            log.info("Comentario {} ya fue procesado antes, se ignora el reenvío", commentId);
            return;
        }

        String claveAbuso = autorId != null ? autorId : commentId;
        if (blockService.estaBloqueado(claveAbuso) || blockService.estaCooldown(claveAbuso)) {
            log.info("Comentario {} ignorado -- autor {} en cooldown/bloqueado", commentId, claveAbuso);
            return;
        }

        // Primera vez de este autor -- nunca antes le contestamos -- el bot SIEMPRE saluda.
        boolean esPrimeraVez = autorId == null || !comentarioSocialRepository.existsByAutorId(autorId);

        Variantes variante = publicacionSocialRepository.findByPostIdFacebook(postId)
                .map(PublicacionSocial::getVariante)
                .orElse(null);

        String respuesta;
        try {
            respuesta = chatbotService.responderComentarioRedSocial(comentarioTexto, variante, esPrimeraVez).block();
        } catch (Exception e) {
            log.warn("Error consultando el chatbot para el comentario {}: {}", commentId, e.getMessage());
            return;
        }

        boolean noComprendido = respuesta == null || respuesta.contains("##FAREWELL##");
        String respuestaLimpia = null;

        if (!noComprendido) {
            respuestaLimpia = respuesta.replaceAll("##BUSCAR\\[[^\\]]*\\]##", "").trim();
            if (respuestaLimpia.isBlank()) {
                noComprendido = true;
                respuestaLimpia = null;
            }
        }

        if (noComprendido) {
            // A diferencia del chat del sitio (que si muestra un mensaje de despedida), aqui NO
            // se contesta nada -- publicar un "no te entendi" bajo un comentario público se ve
            // mal. Decision explicita del dueño. (esPrimeraVez fuerza al modelo a no llegar aqui,
            // pero se deja la red de seguridad por si igual pasara.)
            blockService.registrarFarewell(claveAbuso);
            log.info("Comentario {} no se contesta (no comprendido)", commentId);
        } else {
            blockService.registrarMensajeNormal(claveAbuso);
            try {
                facebookGraphClient.responderComentario(commentId, respuestaLimpia);
                log.info("Comentario {} respondido por el bot (primeraVez={})", commentId, esPrimeraVez);
            } catch (Exception e) {
                log.warn("No se pudo responder el comentario {} en Facebook: {}", commentId, e.getMessage());
                respuestaLimpia = null; // no se guarda como respondido si Facebook lo rechazó
            }
        }

        guardarRegistro(commentId, postId, autorId, comentarioTexto, respuestaLimpia);
    }

    private void guardarRegistro(String commentId, String postId, String autorId, String mensaje, String respuesta) {
        ComentarioSocial registro = new ComentarioSocial();
        registro.setCommentId(commentId);
        registro.setPostId(postId);
        registro.setRedSocial("facebook");
        registro.setAutorId(autorId);
        registro.setMensaje(mensaje);
        registro.setRespuesta(respuesta);
        registro.setFecha(LocalDateTime.now());
        comentarioSocialRepository.save(registro);
    }
}
