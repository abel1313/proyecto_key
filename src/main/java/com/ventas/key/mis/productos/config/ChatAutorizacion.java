package com.ventas.key.mis.productos.config;

import com.ventas.key.mis.productos.entity.ChatSesion;
import com.ventas.key.mis.productos.entity.Usuario;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * Quien puede hacer que en el chat en vivo (WebSocket). Antes NO se revisaba nada: un anonimo se suscribia a {@code /topic/chat.admin} y leia todas
 * las conversaciones, contestaba como ADMIN por {@code /app/chat.admin.responder}, y mandaba un {@code usuarioId} ajeno para colarse en el chat de otro
 * cliente (auditoria de seguridad 2026-11-03). Reglas:
 * <ul>
 *   <li>Lo del administrador (leer {@code /topic/chat.admin}, responder, marcar «conectado») pide ROLE_ADMIN o la pantalla «admin/chat»
 *       (responder, ademas, con permiso de Editar en esa pantalla), igual que {@code /v1/chat/admin/**} en SecurityConfig.</li>
 *   <li>Una conversacion con dueño ({@code usuarioId}) solo la lee/escribe ese usuario o un administrador del chat. Las anonimas se protegen con su
 *       UUID, como el historial por sesionId.</li>
 * </ul>
 */
@Component
public class ChatAutorizacion {

    private static final String ADMIN = "ROLE_ADMIN";
    private static final String PANTALLA = "PANTALLA_admin/chat";
    private static final String PANTALLA_ESCRIBIR = "PANTALLA_admin/chat_ESCRIBIR";

    public boolean puedeVerPanelAdmin(Authentication auth) {
        return tiene(auth, ADMIN) || tiene(auth, PANTALLA);
    }

    public boolean puedeResponderComoAdmin(Authentication auth) {
        return tiene(auth, ADMIN) || tiene(auth, PANTALLA_ESCRIBIR);
    }

    public boolean puedeUsarSesion(Authentication auth, ChatSesion sesion) {
        if (sesion == null || sesion.getUsuarioId() == null) {
            return true;                                   // conversacion anonima: la protege su UUID
        }
        return puedeVerPanelAdmin(auth) || (idDe(auth) != null && idDe(auth).equals(sesion.getUsuarioId()));
    }

    /** El id del usuario con sesion, o {@code null} si es un visitante anonimo. */
    public Integer idDe(Authentication auth) {
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof Usuario u) {
            return u.getId();
        }
        return null;
    }

    private static boolean tiene(Authentication auth, String autoridad) {
        return auth != null && auth.isAuthenticated()
                && auth.getAuthorities().stream().anyMatch(a -> autoridad.equals(a.getAuthority()));
    }
}
