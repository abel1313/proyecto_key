package com.ventas.key.mis.productos.redessociales;

import com.ventas.key.mis.productos.chatbot.ChatbotBlockService;
import com.ventas.key.mis.productos.chatbot.ChatbotInstagramService;
import com.ventas.key.mis.productos.service.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

// El eco de la respuesta del bot podía llegar antes de guardar respuestaMid: se tomaba como
// respuesta de un admin y el cliente quedaba pausado para siempre.
class InstagramDirectMessagePausaTest {

    private static final String NEGOCIO = "ig-negocio";
    private static final String CLIENTE = "cliente-1";

    private ChatbotInstagramService chatbot;
    private InstagramGraphClient graphClient;
    private IMensajeDirectoSocialRepository mensajeRepo;
    private IMensajePausaRepository pausaRepo;
    private InstagramDirectMessageBotService service;

    @BeforeEach
    void setUp() {
        chatbot = mock(ChatbotInstagramService.class);
        graphClient = mock(InstagramGraphClient.class);
        mensajeRepo = mock(IMensajeDirectoSocialRepository.class);
        pausaRepo = mock(IMensajePausaRepository.class);
        service = new InstagramDirectMessageBotService(chatbot, mock(ChatbotBlockService.class), graphClient,
                mock(EmailService.class), mensajeRepo, pausaRepo);
        ReflectionTestUtils.setField(service, "igUserId", NEGOCIO);

        when(mensajeRepo.findByMid(anyString())).thenReturn(Optional.empty());
        when(mensajeRepo.existsByRespuestaMid(anyString())).thenReturn(false);
        when(chatbot.responderComentario(anyString(), isNull(), anyBoolean()))
                .thenReturn(Mono.just("¡Hola! Sí tenemos bolsas negras."));
    }

    @Test
    void elEcoQueLlegaMientrasElBotContestaNoPausaAlCliente() {
        when(graphClient.enviarMensajeDirecto(eq(CLIENTE), anyString())).thenAnswer(inv -> {
            service.procesarMensaje("eco-1", NEGOCIO, CLIENTE, "¡Hola! Sí tenemos bolsas negras.", true);
            return "eco-1";
        });

        service.procesarMensaje("mid-1", CLIENTE, NEGOCIO, "¿tienen bolsas negras?", false);

        verify(pausaRepo, never()).save(any());
        verify(mensajeRepo).save(argThat(r -> "eco-1".equals(r.getRespuestaMid())));
    }

    @Test
    void unaRespuestaManualSinBotContestandoSiPausa() {
        service.procesarMensaje("manual-1", NEGOCIO, CLIENTE, "Te atiendo yo", true);

        verify(pausaRepo).save(argThat(p -> CLIENTE.equals(p.getAutorId())));
    }
}
