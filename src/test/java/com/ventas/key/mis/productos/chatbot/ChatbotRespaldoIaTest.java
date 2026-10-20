package com.ventas.key.mis.productos.chatbot;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Prueba de punta a punta del respaldo de IA con servidores falsos: "OpenAI" sin saldo y "DeepSeek" que
 * sí contesta. El descanso de los modelos es compartido, por eso cada prueba lo reinicia.
 */
class ChatbotRespaldoIaTest {

    private HttpServer openai;
    private HttpServer deepseek;
    private final AtomicInteger llamadasOpenai = new AtomicInteger();
    private final AtomicInteger llamadasDeepseek = new AtomicInteger();

    @BeforeEach
    void arrancar() throws Exception {
        ChatbotBase.reiniciarCircuitoIa();
        llamadasOpenai.set(0);
        llamadasDeepseek.set(0);
        openai = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        deepseek = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    }

    @AfterEach
    void parar() {
        openai.stop(0);
        deepseek.stop(0);
    }

    private void responder(HttpServer s, AtomicInteger contador, int status, String cuerpo) {
        s.createContext("/chat/completions", ex -> {
            contador.incrementAndGet();
            byte[] b = cuerpo.getBytes(StandardCharsets.UTF_8);
            ex.getResponseHeaders().add("Content-Type", "application/json");
            ex.sendResponseHeaders(status, b.length);
            ex.getResponseBody().write(b);
            ex.close();
        });
        s.start();
    }

    private static final String OK = "{\"choices\":[{\"message\":{\"content\":\"Hola desde el respaldo\"}}]}";
    private static final String SIN_SALDO = "{\"error\":{\"code\":\"insufficient_quota\"}}";

    private ChatbotSitioWebService servicio(boolean conRespaldo) {
        ChatbotSitioWebService s = new ChatbotSitioWebService(null, null);
        s.openaiUrl = "http://127.0.0.1:" + openai.getAddress().getPort();
        s.openaiApiKey = "sk-prueba";
        s.model = "gpt-prueba";
        s.respaldoUrl = "http://127.0.0.1:" + deepseek.getAddress().getPort();
        s.respaldoApiKey = conRespaldo ? "ds-prueba" : "";
        s.respaldoModelo = "ds-modelo";
        s.iaPausaMinutos = 10;
        return s;
    }

    private static final List<Map<String, String>> MENSAJES = List.of(Map.of("role", "user", "content", "hola"));

    @Test
    void openaiSinSaldo_contestaElRespaldo() {
        responder(openai, llamadasOpenai, 429, SIN_SALDO);
        responder(deepseek, llamadasDeepseek, 200, OK);

        String r = servicio(true).llamarOpenAI(MENSAJES).block();

        assertThat(r).isEqualTo("Hola desde el respaldo");
        assertThat(llamadasOpenai.get()).isEqualTo(1);
        assertThat(llamadasDeepseek.get()).isEqualTo(1);
    }

    @Test
    void sinLlaveDeRespaldo_secomportaComoAntes_yFalla() {
        responder(openai, llamadasOpenai, 500, "{}");
        responder(deepseek, llamadasDeepseek, 200, OK);

        assertThatThrownBy(() -> servicio(false).llamarOpenAI(MENSAJES).block())
                .isInstanceOf(WebClientResponseException.class);
        assertThat(llamadasDeepseek.get()).isZero();
    }

    @Test
    void unErrorNuestro_noCambiaDeModelo() {
        responder(openai, llamadasOpenai, 400, "{\"error\":\"mal\"}");
        responder(deepseek, llamadasDeepseek, 200, OK);

        assertThatThrownBy(() -> servicio(true).llamarOpenAI(MENSAJES).block())
                .isInstanceOf(WebClientResponseException.class);
        assertThat(llamadasDeepseek.get()).isZero();
    }

    @Test
    void siFallanLosDos_fallaConElUltimoError() {
        responder(openai, llamadasOpenai, 503, "{}");
        responder(deepseek, llamadasDeepseek, 402, "{\"error\":\"Insufficient Balance\"}");

        assertThatThrownBy(() -> servicio(true).llamarOpenAI(MENSAJES).block())
                .isInstanceOf(WebClientResponseException.class);
        assertThat(llamadasOpenai.get()).isEqualTo(1);
        assertThat(llamadasDeepseek.get()).isEqualTo(1);
    }

    @Test
    void openaiContestaBien_noSeTocaElRespaldo() {
        responder(openai, llamadasOpenai, 200, "{\"choices\":[{\"message\":{\"content\":\"Hola desde openai\"}}]}");
        responder(deepseek, llamadasDeepseek, 200, OK);

        assertThat(servicio(true).llamarOpenAI(MENSAJES).block()).isEqualTo("Hola desde openai");
        assertThat(llamadasDeepseek.get()).isZero();
    }
}
