package com.ventas.key.mis.productos.config;

import com.ventas.key.mis.productos.filter.JwtAuthenticationFilter;
import com.ventas.key.mis.productos.jwt.JwtUtil;
import com.ventas.key.mis.productos.service.api.IChatSesionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Revisión de seguridad 2026-11-05: el simple broker de Spring trata '*' como patrón, así que suscribirse a
 * {@code /topic/chat.usuario.*} o {@code /topic/chat.inicio.*} recibía los mensajes de TODAS las conversaciones. Un visitante anónimo
 * no debe poder hacerlo.
 */
class ChatSeguridadInterceptorComodinesTest {

    private final IChatSesionService sesiones = mock(IChatSesionService.class);
    private final ChatSeguridadInterceptor interceptor = new ChatSeguridadInterceptor(
            mock(JwtUtil.class), mock(UserDetailsService.class), mock(JwtAuthenticationFilter.class), new ChatAutorizacion(), sesiones);

    private Message<?> suscribir(String destino) {
        StompHeaderAccessor h = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        h.setDestination(destino);
        h.setSessionId("ws-1");
        h.setSubscriptionId("sub-1");
        h.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], h.getMessageHeaders());
    }

    @Test
    @DisplayName("un visitante anónimo NO puede suscribirse con comodines")
    void comodinesRechazados() {
        for (String destino : new String[]{"/topic/chat.usuario.*", "/topic/chat.inicio.*", "/topic/chat.usuario.{x}",
                "/topic/chat.usuario.?", "/topic/chat.inicio.abc*", "/topic/chat.*"}) {
            assertThat(interceptor.preSend(suscribir(destino), mock(MessageChannel.class))).as(destino).isNull();
        }
    }

    @Test
    @DisplayName("el chat anónimo normal sigue funcionando: su propio tempId y su propia sesión")
    void chatAnonimoNormalSigueFuncionando() {
        when(sesiones.buscarSesion(anyString())).thenReturn(Optional.empty());
        assertThat(interceptor.preSend(suscribir("/topic/chat.inicio.tmp-9f3a"), mock(MessageChannel.class))).isNotNull();
        assertThat(interceptor.preSend(suscribir("/topic/chat.usuario.7c2f-uuid"), mock(MessageChannel.class))).isNotNull();
    }

    @Test
    @DisplayName("el tópico del administrador sigue cerrado para anónimos")
    void panelAdminCerrado() {
        assertThat(interceptor.preSend(suscribir("/topic/chat.admin"), mock(MessageChannel.class))).isNull();
    }
}
