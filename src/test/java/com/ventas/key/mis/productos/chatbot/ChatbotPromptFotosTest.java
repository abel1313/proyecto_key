package com.ventas.key.mis.productos.chatbot;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

// El chat en vivo se pinta como texto: no dibuja tarjetas ni imagenes. Aun asi el bot le ofrecia
// fotos al cliente ("...a $250 MXN 😊 ¿Quieres ver una foto?"), el cliente decia que si y se
// quedaba esperando una imagen que nunca llega (reportado 2026-09-17, dos veces).
//
// El primer intento de arreglo quito SOLO la linea del prompt que manda preguntar por la foto, y no
// sirvio: abajo seguian los ejemplos calcados con la misma pregunta y una REGLA CRITICA que ordena
// mostrar imagenes siempre. El modelo copia el ejemplo antes que obedecer una prohibicion. Por eso
// ahora el canal sobreescribe la seccion COMPLETA (seccionMostrarProductos) y por eso estos tests
// miran el prompt final armado, no una linea suelta: es la unica forma de que el mismo error no
// vuelva a pasar la revision.
class ChatbotPromptFotosTest {

    private final ChatbotChatVivoService chatVivo = new ChatbotChatVivoService(null, null);
    private final ChatbotSitioWebService sitioWeb = new ChatbotSitioWebService(null, null);

    @Test
    void elPromptDelChatEnVivoNoLeOfreceFotosAlCliente() {
        String prompt = chatVivo.promptBase();

        // Ni la frase (el modelo la copiaba tal cual) ni un solo ##BUSCAR[...]## de ejemplo: el
        // orquestador borra esa marca antes de mandar el mensaje (ChatVivoBotService.limpiarMarcadores),
        // asi que emitirla deja un "¡Aqui la tienes! 📸" sin nada abajo.
        assertThat(prompt).doesNotContain("¿Quieres ver una foto?");
        assertThat(prompt).doesNotContain("##BUSCAR[");
        // Señal de que el override reemplazo la seccion entera y no solo una linea.
        assertThat(prompt).doesNotContain("MOSTRAR PRODUCTOS EN TARJETAS");
    }

    @Test
    void elPromptDelChatEnVivoSiLeDiceQueHacerCuandoPidenFotos() {
        String prompt = chatVivo.promptBase();

        // No basta con callar la oferta: si el cliente pide una foto igual hay que contestarle algo.
        assertThat(prompt).contains("tienda en línea");
        assertThat(prompt).contains("NUNCA uses ##BUSCAR##");
    }

    @Test
    void elChatEnVivoConservaLoSuyo() {
        String prompt = chatVivo.promptBase();

        // Escalar a una persona y el catalogo son de este canal: la seccion nueva no los tapa.
        assertThat(prompt).contains("##HUMANO##");
        assertThat(prompt).contains("CATÁLOGO ACTUAL (variantes disponibles con stock):");
        // Y lo que se arreglo antes (negar shorts que si hay) sigue en la base.
        assertThat(prompt).contains("\"sorth\" = short");
    }

    @Test
    void elSitioWebSiSigueMostrandoTarjetas() {
        // Regresion al reves: sacar las tarjetas del chat en vivo no debe apagarlas donde SI
        // funcionan. El widget del sitio dibuja la tarjeta con imagen, ahi la foto se ofrece.
        String prompt = sitioWeb.promptBase();

        assertThat(prompt).contains("¿Quieres ver una foto?");
        assertThat(prompt).contains("##BUSCAR[término,offset]##");
        assertThat(prompt).contains("CATÁLOGO ACTUAL (variantes disponibles con stock):");
    }
}
