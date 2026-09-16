package com.ventas.key.mis.productos.service;

import com.ventas.key.mis.productos.chatbot.ChatbotBlockService;
import com.ventas.key.mis.productos.chatbot.ChatbotChatVivoService;
import com.ventas.key.mis.productos.entity.ChatMensaje;
import com.ventas.key.mis.productos.service.api.IChatMensajeService;
import com.ventas.key.mis.productos.service.api.IChatNotificacionService;
import com.ventas.key.mis.productos.service.api.IChatSesionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ChatVivoBotServiceTest {

    private ChatbotChatVivoService bot;
    private ChatbotBlockService blockService;
    private IChatSesionService sesionService;
    private IChatMensajeService mensajeService;
    private IChatNotificacionService notificacionService;
    private SimpMessagingTemplate messagingTemplate;
    private ChatVivoBotService service;

    private static final String SESION = "sesion-1";

    @BeforeEach
    void setUp() {
        bot = mock(ChatbotChatVivoService.class);
        blockService = mock(ChatbotBlockService.class);
        sesionService = mock(IChatSesionService.class);
        mensajeService = mock(IChatMensajeService.class);
        notificacionService = mock(IChatNotificacionService.class);
        messagingTemplate = mock(SimpMessagingTemplate.class);
        service = new ChatVivoBotService(bot, blockService, sesionService, mensajeService,
                notificacionService, messagingTemplate);

        when(sesionService.modoDe(SESION)).thenReturn(IChatSesionService.MODO_BOT);
        when(blockService.limiteMensajesExcedido(anyString())).thenReturn(false);
        when(mensajeService.obtenerHistorial(SESION)).thenReturn(List.of());
        when(mensajeService.guardar(anyString(), anyString(), anyString()))
                .thenAnswer(i -> msg(99L, i.getArgument(1), i.getArgument(2)));
    }

    private ChatMensaje msg(Long id, String remitente, String contenido) {
        return ChatMensaje.builder().id(id).sesionId(SESION).remitente(remitente)
                .contenido(contenido).timestamp(LocalDateTime.now()).build();
    }

    @Test
    void contestaCuandoElClienteSigueSiendoElUltimo() throws Exception {
        when(mensajeService.ultimoMensaje(SESION)).thenReturn(Optional.of(msg(10L, "USUARIO", "hola")));
        when(bot.responder(anyString(), anyList())).thenReturn(Mono.just("¡Hola! ¿En qué te ayudo?"));

        service.atender(SESION, 7, "Abel", 10L, "hola");
        Thread.sleep(8000);

        ArgumentCaptor<String> remitente = ArgumentCaptor.forClass(String.class);
        verify(mensajeService, atLeastOnce()).guardar(eq(SESION), remitente.capture(), anyString());
        assertThat(remitente.getAllValues()).contains("BOT");
        verify(messagingTemplate, atLeastOnce())
                .convertAndSend(eq("/topic/chat.usuario." + SESION), any(Object.class));
    }

    @Test
    void enModoHumanoSinElDuenoEnElPanelNoSeEsperaSuTurno() throws Exception {
        // El turno del dueño solo se le guarda si esta en el panel. Si no esta, esperarlo deja al
        // cliente en silencio por nada: el bot contesta con la espera corta.
        when(sesionService.modoDe(SESION)).thenReturn(IChatSesionService.MODO_HUMANO);
        when(notificacionService.isAdminConectado()).thenReturn(false);
        when(mensajeService.ultimoMensaje(SESION)).thenReturn(Optional.of(msg(10L, "USUARIO", "hola")));
        when(bot.responder(anyString(), anyList())).thenReturn(Mono.just("¡Hola!"));

        service.atender(SESION, 7, "Abel", 10L, "hola");
        Thread.sleep(8000);

        verify(mensajeService).guardar(eq(SESION), eq("BOT"), anyString());
    }

    @Test
    void siElBotTruenaAntesDeLlamarAOpenAiElClienteNoSeQuedaSinNada() throws Exception {
        when(mensajeService.ultimoMensaje(SESION)).thenReturn(Optional.of(msg(10L, "USUARIO", "hola")));
        // Falla SINCRONA al armar el prompt (leer catalogo, palabras clave, etc.)
        when(bot.responder(anyString(), anyList())).thenThrow(new RuntimeException("boom armando el prompt"));

        service.atender(SESION, 7, "Abel", 10L, "hola");
        Thread.sleep(8000);

        verify(messagingTemplate, atLeastOnce())
                .convertAndSend(eq("/topic/chat.usuario." + SESION), any(Object.class));
    }
}
