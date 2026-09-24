package com.ventas.key.mis.productos.chatbot;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

// Ningún bot contesta seco ni grosero, en ningún lado (pedido del dueño el 2026-09-24).
class ChatbotRespetoTest {

    @Test
    void todosLosBotsLlevanLaReglaDeRespeto() {
        assertThat(new ChatbotFacebookService(null, null).promptBase()).contains("RESPETO — REGLA QUE NUNCA SE ROMPE");
        assertThat(new ChatbotSitioWebService(null, null).promptBase()).contains("RESPETO — REGLA QUE NUNCA SE ROMPE");
        assertThat(ChatbotBase.REGLAS_DE_RESPETO).contains("groserías", "Nunca contestes seco o cortante");
    }

    @Test
    void unSaludoPelonSeCompletaConUnaPreguntaAmable() {
        assertThat(ChatbotBase.sinSaludoSeco("¡Hola! 😊")).isEqualTo("¡Hola! 😊 ¿En qué te puedo ayudar? 😊");
        assertThat(ChatbotBase.sinSaludoSeco("Buenas tardes")).endsWith("¿En qué te puedo ayudar? 😊");
        assertThat(ChatbotBase.sinSaludoSeco("¡Hola! Sí tenemos esa bolsa en $300 😊"))
                .isEqualTo("¡Hola! Sí tenemos esa bolsa en $300 😊");
        assertThat(ChatbotBase.sinSaludoSeco(null)).isNull();
    }
}
