package com.ventas.key.mis.productos.config;

import com.ventas.key.mis.productos.filter.JwtAuthenticationFilter;
import com.ventas.key.mis.productos.jwt.JwtUtil;
import com.ventas.key.mis.productos.service.api.IChatSesionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

/**
 * Seguridad del chat en vivo (STOMP). {@code /ws} es publico (el visitante puede abrir el chat), asi que la identidad se toma del header
 * {@code Authorization: Bearer} del frame CONNECT (el front ya lo manda en el panel del admin y lo manda el chat del cliente con sesion); sin token
 * valido la conexion es de un visitante anonimo, que no puede tocar nada de administrador. Un mensaje no autorizado se descarta (devolver {@code null}).
 */
@Component
@Slf4j
public class ChatSeguridadInterceptor implements ChannelInterceptor {

    private static final String TOPIC_ADMIN = "/topic/chat.admin";
    private static final String TOPIC_USUARIO = "/topic/chat.usuario.";
    private static final String TOPIC_INICIO = "/topic/chat.inicio.";
    private static final String APP_ADMIN_RESPONDER = "/app/chat.admin.responder";
    private static final String APP_ADMIN_PREFIJO = "/app/chat.admin.";

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;
    private final JwtAuthenticationFilter autoridades;
    private final ChatAutorizacion autorizacion;
    private final IChatSesionService sesiones;

    public ChatSeguridadInterceptor(JwtUtil jwtUtil, UserDetailsService userDetailsService, @Lazy JwtAuthenticationFilter autoridades,
                                    ChatAutorizacion autorizacion, @Lazy IChatSesionService sesiones) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
        this.autoridades = autoridades;
        this.autorizacion = autorizacion;
        this.sesiones = sesiones;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }
        StompCommand comando = accessor.getCommand();
        if (StompCommand.CONNECT.equals(comando)) {
            Authentication auth = autenticar(accessor.getFirstNativeHeader("Authorization"));
            if (auth != null) {
                accessor.setUser(auth);
            }
            return message;
        }
        Authentication auth = accessor.getUser() instanceof Authentication a ? a : null;
        String destino = accessor.getDestination();
        if (destino == null) {
            return message;
        }
        if (StompCommand.SUBSCRIBE.equals(comando)) {
            return suscripcionPermitida(auth, destino) ? message : rechazar(comando, destino);
        }
        if (StompCommand.SEND.equals(comando) && destino.startsWith(APP_ADMIN_PREFIJO)) {
            boolean ok = APP_ADMIN_RESPONDER.equals(destino) ? autorizacion.puedeResponderComoAdmin(auth) : autorizacion.puedeVerPanelAdmin(auth);
            return ok ? message : rechazar(comando, destino);
        }
        return message;
    }

    private boolean suscripcionPermitida(Authentication auth, String destino) {
        if (destino.equals(TOPIC_ADMIN)) {
            return autorizacion.puedeVerPanelAdmin(auth);
        }
        if (destino.startsWith(TOPIC_USUARIO)) {
            String sesionId = destino.substring(TOPIC_USUARIO.length());
            return autorizacion.puedeUsarSesion(auth, sesiones.buscarSesion(sesionId).orElse(null));
        }
        if (destino.startsWith(TOPIC_INICIO)) {
            return true;                                   // tempId aleatorio que elige el propio cliente
        }
        return false;                                      // ningun otro /topic existe para el cliente
    }

    private Message<?> rechazar(StompCommand comando, String destino) {
        log.warn("[WS] {} a {} rechazado: sin permiso", comando, destino);
        return null;
    }

    /** El usuario del token, o {@code null} si no hay token o no vale (queda como visitante anonimo). */
    private Authentication autenticar(String cabecera) {
        if (cabecera == null || !cabecera.startsWith("Bearer ")) {
            return null;
        }
        String jwt = cabecera.substring(7);
        try {
            if (jwtUtil.isRefreshToken(jwt)) {
                return null;
            }
            UserDetails usuario = userDetailsService.loadUserByUsername(jwtUtil.extractUsername(jwt));
            if (!usuario.isEnabled() || !jwtUtil.validateToken(jwt, usuario)) {
                return null;
            }
            return new UsernamePasswordAuthenticationToken(usuario, jwt, autoridades.autoridadesConPantallas(usuario));
        } catch (Exception e) {
            log.warn("[WS] Token del chat no valido: {}", e.getMessage());
            return null;
        }
    }
}
