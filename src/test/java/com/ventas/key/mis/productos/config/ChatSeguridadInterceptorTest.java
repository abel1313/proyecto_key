package com.ventas.key.mis.productos.config;

import com.ventas.key.mis.productos.entity.ChatSesion;
import com.ventas.key.mis.productos.entity.Usuario;
import com.ventas.key.mis.productos.filter.JwtAuthenticationFilter;
import com.ventas.key.mis.productos.jwt.JwtUtil;
import com.ventas.key.mis.productos.service.api.IChatSesionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Chat en vivo: lo del administrador pide permiso y una conversacion con dueño solo la usa su dueño (auditoria de seguridad 2026-11-03). */
class ChatSeguridadInterceptorTest {

    private IChatSesionService sesiones;
    private ChatSeguridadInterceptor interceptor;

    @BeforeEach
    void preparar() {
        sesiones = mock(IChatSesionService.class);
        interceptor = new ChatSeguridadInterceptor(mock(JwtUtil.class), mock(UserDetailsService.class), mock(JwtAuthenticationFilter.class),
                new ChatAutorizacion(), sesiones);
    }

    private static Authentication usuario(int id, String... autoridades) {
        Usuario u = new Usuario();
        u.setId(id);
        return new UsernamePasswordAuthenticationToken(u, "t", java.util.Arrays.stream(autoridades).map(SimpleGrantedAuthority::new).toList());
    }

    private static Message<byte[]> mensaje(StompCommand comando, String destino, Authentication auth) {
        StompHeaderAccessor h = StompHeaderAccessor.create(comando);
        h.setDestination(destino);
        h.setSessionId("s1");
        if (auth != null) h.setUser(auth);
        h.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], h.getMessageHeaders());
    }

    private boolean pasa(StompCommand c, String destino, Authentication a) {
        return interceptor.preSend(mensaje(c, destino, a), null) != null;
    }

    @Test
    @DisplayName("un anonimo o un cliente comun NO se suscribe al panel del admin ni le contesta a nadie como admin")
    void panelAdminCerrado() {
        for (Authentication a : new Authentication[]{null, usuario(5, "ROLE_USUARIO")}) {
            assertThat(pasa(StompCommand.SUBSCRIBE, "/topic/chat.admin", a)).isFalse();
            assertThat(pasa(StompCommand.SEND, "/app/chat.admin.responder", a)).isFalse();
            assertThat(pasa(StompCommand.SEND, "/app/chat.admin.conectado", a)).isFalse();
        }
    }

    @Test
    @DisplayName("el administrador (o quien tiene la pantalla Chat) sí; para responder hace falta Editar")
    void panelAdminAbiertoAlAdmin() {
        Authentication admin = usuario(1, "ROLE_ADMIN");
        assertThat(pasa(StompCommand.SUBSCRIBE, "/topic/chat.admin", admin)).isTrue();
        assertThat(pasa(StompCommand.SEND, "/app/chat.admin.responder", admin)).isTrue();

        Authentication soloVer = usuario(2, "PANTALLA_admin/chat");
        assertThat(pasa(StompCommand.SUBSCRIBE, "/topic/chat.admin", soloVer)).isTrue();
        assertThat(pasa(StompCommand.SEND, "/app/chat.admin.responder", soloVer)).isFalse();
        assertThat(pasa(StompCommand.SEND, "/app/chat.admin.responder", usuario(3, "PANTALLA_admin/chat", "PANTALLA_admin/chat_ESCRIBIR"))).isTrue();
    }

    @Test
    @DisplayName("la conversacion de un cliente solo la escucha ese cliente (o el admin); la anonima, quien tenga su UUID")
    void conversacionConDueno() {
        ChatSesion ajena = new ChatSesion();
        ajena.setUsuarioId(17);
        when(sesiones.buscarSesion("de-17")).thenReturn(Optional.of(ajena));
        ChatSesion anonima = new ChatSesion();
        when(sesiones.buscarSesion("anonima")).thenReturn(Optional.of(anonima));

        assertThat(pasa(StompCommand.SUBSCRIBE, "/topic/chat.usuario.de-17", null)).isFalse();
        assertThat(pasa(StompCommand.SUBSCRIBE, "/topic/chat.usuario.de-17", usuario(99, "ROLE_USUARIO"))).isFalse();
        assertThat(pasa(StompCommand.SUBSCRIBE, "/topic/chat.usuario.de-17", usuario(17, "ROLE_USUARIO"))).isTrue();
        assertThat(pasa(StompCommand.SUBSCRIBE, "/topic/chat.usuario.de-17", usuario(1, "ROLE_ADMIN"))).isTrue();
        assertThat(pasa(StompCommand.SUBSCRIBE, "/topic/chat.usuario.anonima", null)).isTrue();
    }

    @Test
    @DisplayName("el canal de inicio sí es de cualquiera (tempId aleatorio) y cualquier otro /topic se niega")
    void otrosDestinos() {
        assertThat(pasa(StompCommand.SUBSCRIBE, "/topic/chat.inicio.abc", null)).isTrue();
        assertThat(pasa(StompCommand.SUBSCRIBE, "/topic/otra.cosa", null)).isFalse();
        assertThat(pasa(StompCommand.SEND, "/app/chat.mensaje", null)).isTrue();    // la pertenencia de la conversacion se revisa en el controller
    }
}
