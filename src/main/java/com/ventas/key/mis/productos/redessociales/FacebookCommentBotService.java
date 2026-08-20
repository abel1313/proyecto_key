package com.ventas.key.mis.productos.redessociales;

import com.ventas.key.mis.productos.chatbot.ChatbotBlockService;
import com.ventas.key.mis.productos.chatbot.ChatbotService;
import com.ventas.key.mis.productos.entity.productoVariantes.Variantes;
import com.ventas.key.mis.productos.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

// Orquesta el bot de comentarios de Facebook: recibe el evento ya parseado del webhook
// (FacebookWebhookController), decide si contesta, escala por correo, o se calla -- y si contesta
// reusa el mismo "cerebro" del chat del sitio (ChatbotService) -- misma logica de entender/
// resolver, distinto canal de salida.
@Service
@Slf4j
@RequiredArgsConstructor
public class FacebookCommentBotService {

    private final ChatbotService chatbotService;
    private final ChatbotBlockService blockService;
    private final FacebookGraphClient facebookGraphClient;
    private final EmailService emailService;
    private final IPublicacionSocialRepository publicacionSocialRepository;
    private final IComentarioSocialRepository comentarioSocialRepository;
    private final IComentarioPausaRepository comentarioPausaRepository;

    @Value("${facebook.page-id:}")
    private String pageId;

    @Value("${chat.admin-email:}")
    private String adminEmail;

    // Facebook espera una respuesta rapida al webhook (idealmente unos pocos segundos) -- se
    // procesa en el mismo hilo por simplicidad (OpenAI + Facebook, ~2-4s en total). Si en
    // produccion esto empieza a causar timeouts/reintentos de Meta, mover a un job async (el
    // proyecto ya tiene RabbitMQ para eso, ver PublicacionSocialScheduler como referencia).
    public void procesarComentario(String commentId, String postId, String parentId, String comentarioTexto, String autorId) {
        if (autorId != null && autorId.equals(pageId)) {
            // Es un comentario hecho por la propia pagina -- nuestro propio bot, o un admin
            // respondiendo manualmente desde la app real de Facebook. Nunca se auto-contesta a si
            // misma (evita loop infinito); si es una respuesta manual, se detecta y se pausa el
            // bot para ese cliente en ese post.
            detectarRespuestaManualYPausar(commentId, parentId);
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

        // Un admin ya contestó manualmente a este cliente en este post -- el bot no vuelve a
        // meterse en esa conversación, indefinido, hasta que se borre la pausa a mano.
        if (autorId != null && postId != null
                && comentarioPausaRepository.existsByAutorIdAndPostId(autorId, postId)) {
            log.info("Comentario {} ignorado -- un admin ya intervino manualmente con este cliente en este post", commentId);
            return;
        }

        String claveAbuso = autorId != null ? autorId : commentId;
        if (blockService.estaBloqueado(claveAbuso) || blockService.estaCooldown(claveAbuso)) {
            log.info("Comentario {} ignorado -- autor {} en cooldown/bloqueado", commentId, claveAbuso);
            return;
        }

        // Limite de 20 mensajes/hora por autor -- evita agotar la cuota de OpenAI aunque el
        // cliente pregunte cosas validas sin parar. Igual que "no comprendido", no se contesta
        // nada publico (decision del dueno de no publicar mensajes de rechazo bajo un comentario).
        if (blockService.limiteMensajesExcedido(claveAbuso)) {
            log.info("Comentario {} ignorado -- autor {} alcanzo el limite de mensajes por hora", commentId, claveAbuso);
            guardarRegistro(commentId, postId, autorId, comentarioTexto, null, null);
            return;
        }
        blockService.registrarMensaje(claveAbuso);

        // Primera vez de este autor -- nunca antes le contestamos -- el bot SIEMPRE saluda
        // (salvo que la pregunta se tenga que escalar, ver ChatbotService).
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

        if (respuesta != null && respuesta.contains("##ESCALAR##")) {
            // El bot detectó una pregunta específica que no puede contestar con seguridad (ej. un
            // dato que falta en el catálogo) -- no se contesta nada en Facebook, se avisa al admin
            // por correo para que conteste personalmente. Decisión explícita del dueño.
            escalarPorCorreo(commentId, postId, comentarioTexto);
            guardarRegistro(commentId, postId, autorId, comentarioTexto, null, null);
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
            guardarRegistro(commentId, postId, autorId, comentarioTexto, null, null);
            return;
        }

        blockService.registrarMensajeNormal(claveAbuso);
        String respuestaCommentId = null;
        try {
            respuestaCommentId = facebookGraphClient.responderComentario(commentId, respuestaLimpia);
            log.info("Comentario {} respondido por el bot (primeraVez={})", commentId, esPrimeraVez);
        } catch (Exception e) {
            log.warn("No se pudo responder el comentario {} en Facebook: {}", commentId, e.getMessage());
            respuestaLimpia = null;
        }

        guardarRegistro(commentId, postId, autorId, comentarioTexto, respuestaLimpia, respuestaCommentId);
    }

    // Detecta si un comentario hecho por la Pagina es el eco de nuestra propia respuesta (ya
    // registrada con su commentId en respuesta_comment_id) o una respuesta manual del admin desde
    // la app real de Facebook. Si es manual, pausa el bot para ese cliente en ese post.
    private void detectarRespuestaManualYPausar(String commentId, String parentId) {
        if (parentId == null || parentId.isBlank()) {
            return;
        }
        if (comentarioSocialRepository.existsByRespuestaCommentId(commentId)) {
            return;
        }
        comentarioSocialRepository.findByCommentId(parentId).ifPresent(original -> {
            if (original.getAutorId() == null || original.getPostId() == null) {
                return;
            }
            if (!comentarioPausaRepository.existsByAutorIdAndPostId(original.getAutorId(), original.getPostId())) {
                ComentarioPausa pausa = new ComentarioPausa();
                pausa.setAutorId(original.getAutorId());
                pausa.setPostId(original.getPostId());
                pausa.setFecha(LocalDateTime.now());
                comentarioPausaRepository.save(pausa);
                log.info("Admin respondió manualmente al comentario {} -- bot pausado para autor={} en post={}",
                        parentId, original.getAutorId(), original.getPostId());
            }
        });
    }

    private void escalarPorCorreo(String commentId, String postId, String comentarioTexto) {
        if (adminEmail == null || adminEmail.isBlank()) {
            log.warn("No se pudo escalar el comentario {} -- chat.admin-email no configurado", commentId);
            return;
        }
        String asunto = "Un cliente preguntó algo que el bot no supo responder -- Facebook";
        String html = "<p>Un cliente comentó en un post de Facebook y el bot no tenía suficiente información "
                + "en el catálogo para contestar con seguridad -- no se publicó ninguna respuesta.</p>"
                + "<p><b>Comentario:</b> " + escapeHtml(comentarioTexto) + "</p>"
                + "<p><b>Post:</b> " + escapeHtml(postId) + "</p>"
                + "<p>Entra a Facebook y contesta directamente el comentario.</p>";
        emailService.enviarTicket(adminEmail, asunto, html);
        log.info("Comentario {} escalado por correo al admin (bot no tenía el dato)", commentId);
    }

    private String escapeHtml(String texto) {
        return texto == null ? "" : texto.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private void guardarRegistro(String commentId, String postId, String autorId, String mensaje,
                                  String respuesta, String respuestaCommentId) {
        ComentarioSocial registro = new ComentarioSocial();
        registro.setCommentId(commentId);
        registro.setPostId(postId);
        registro.setRedSocial("facebook");
        registro.setAutorId(autorId);
        registro.setMensaje(mensaje);
        registro.setRespuesta(respuesta);
        registro.setRespuestaCommentId(respuestaCommentId);
        registro.setFecha(LocalDateTime.now());
        comentarioSocialRepository.save(registro);
    }
}
