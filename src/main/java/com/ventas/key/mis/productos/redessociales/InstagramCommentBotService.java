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

// Mismo patron que FacebookCommentBotService, aplicado a comentarios de Instagram -- mismo
// webhook (Meta permite recibir eventos de "page" e "instagram" en la misma URL, distinguidos
// por el campo "object" del payload, ver FacebookWebhookController), misma logica de
// saludo/escalar/pausar, distinto cliente de API (InstagramGraphClient, POST .../replies en vez
// de .../comments) y distinta forma de reconocerse a si mismo (igUserId en vez de pageId).
@Service
@Slf4j
@RequiredArgsConstructor
public class InstagramCommentBotService {

    private final ChatbotService chatbotService;
    private final ChatbotBlockService blockService;
    private final InstagramGraphClient instagramGraphClient;
    private final EmailService emailService;
    private final IPublicacionSocialRepository publicacionSocialRepository;
    private final IComentarioSocialRepository comentarioSocialRepository;
    private final IComentarioPausaRepository comentarioPausaRepository;

    @Value("${instagram.account-id:}")
    private String igUserId;

    @Value("${chat.admin-email:}")
    private String adminEmail;

    public void procesarComentario(String commentId, String postId, String parentId, String comentarioTexto, String autorId) {
        if (autorId != null && autorId.equals(igUserId)) {
            detectarRespuestaManualYPausar(commentId, parentId);
            return;
        }
        if (comentarioTexto == null || comentarioTexto.isBlank() || commentId == null) {
            return;
        }
        if (comentarioSocialRepository.findByCommentId(commentId).isPresent()) {
            log.info("Comentario IG {} ya fue procesado antes, se ignora el reenvío", commentId);
            return;
        }
        if (autorId != null && postId != null
                && comentarioPausaRepository.existsByAutorIdAndPostId(autorId, postId)) {
            log.info("Comentario IG {} ignorado -- un admin ya intervino manualmente con este cliente en este post", commentId);
            return;
        }

        String claveAbuso = autorId != null ? autorId : commentId;
        if (blockService.estaBloqueado(claveAbuso) || blockService.estaCooldown(claveAbuso)) {
            log.info("Comentario IG {} ignorado -- autor {} en cooldown/bloqueado", commentId, claveAbuso);
            return;
        }

        boolean esPrimeraVez = autorId == null || !comentarioSocialRepository.existsByAutorId(autorId);

        Variantes variante = publicacionSocialRepository.findByPostIdFacebook(postId)
                .map(PublicacionSocial::getVariante)
                .orElse(null);

        String respuesta;
        try {
            respuesta = chatbotService.responderComentarioRedSocial(comentarioTexto, variante, esPrimeraVez).block();
        } catch (Exception e) {
            log.warn("Error consultando el chatbot para el comentario IG {}: {}", commentId, e.getMessage());
            return;
        }

        if (respuesta != null && respuesta.contains("##ESCALAR##")) {
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
            blockService.registrarFarewell(claveAbuso);
            log.info("Comentario IG {} no se contesta (no comprendido)", commentId);
            guardarRegistro(commentId, postId, autorId, comentarioTexto, null, null);
            return;
        }

        blockService.registrarMensajeNormal(claveAbuso);
        String respuestaCommentId = null;
        try {
            respuestaCommentId = instagramGraphClient.responderComentario(commentId, respuestaLimpia);
            log.info("Comentario IG {} respondido por el bot (primeraVez={})", commentId, esPrimeraVez);
        } catch (Exception e) {
            log.warn("No se pudo responder el comentario IG {} en Instagram: {}", commentId, e.getMessage());
            respuestaLimpia = null;
        }

        guardarRegistro(commentId, postId, autorId, comentarioTexto, respuestaLimpia, respuestaCommentId);
    }

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
                log.info("Admin respondió manualmente al comentario IG {} -- bot pausado para autor={} en post={}",
                        parentId, original.getAutorId(), original.getPostId());
            }
        });
    }

    private void escalarPorCorreo(String commentId, String postId, String comentarioTexto) {
        if (adminEmail == null || adminEmail.isBlank()) {
            log.warn("No se pudo escalar el comentario IG {} -- chat.admin-email no configurado", commentId);
            return;
        }
        String asunto = "Un cliente preguntó algo que el bot no supo responder -- Instagram";
        String html = "<p>Un cliente comentó en un post de Instagram y el bot no tenía suficiente información "
                + "en el catálogo para contestar con seguridad -- no se publicó ninguna respuesta.</p>"
                + "<p><b>Comentario:</b> " + escapeHtml(comentarioTexto) + "</p>"
                + "<p><b>Post:</b> " + escapeHtml(postId) + "</p>"
                + "<p>Entra a Instagram y contesta directamente el comentario.</p>";
        emailService.enviarTicket(adminEmail, asunto, html);
        log.info("Comentario IG {} escalado por correo al admin (bot no tenía el dato)", commentId);
    }

    private String escapeHtml(String texto) {
        return texto == null ? "" : texto.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private void guardarRegistro(String commentId, String postId, String autorId, String mensaje,
                                  String respuesta, String respuestaCommentId) {
        ComentarioSocial registro = new ComentarioSocial();
        registro.setCommentId(commentId);
        registro.setPostId(postId);
        registro.setRedSocial("instagram");
        registro.setAutorId(autorId);
        registro.setMensaje(mensaje);
        registro.setRespuesta(respuesta);
        registro.setRespuestaCommentId(respuestaCommentId);
        registro.setFecha(LocalDateTime.now());
        comentarioSocialRepository.save(registro);
    }
}
