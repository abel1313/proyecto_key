package com.ventas.key.mis.productos.chatbot;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

// "ya te sigo", "ya compartí": sin la sección de avisos el modelo los tomaba como fuera de tema,
// usaba ##FAREWELL## y la persona no recibía ni un gracias (reportado 2026-09-23).
class ChatbotAgradecimientosTest {

    private final ChatbotFacebookService facebook = new ChatbotFacebookService(null, null);
    private final ChatbotInstagramService instagram = new ChatbotInstagramService(null, null);

    @Test
    void facebookEInstagramAgradecenLosAvisosSinVenderNiCallarse() {
        for (ChatbotBase canal : new ChatbotBase[]{facebook, instagram}) {
            String instrucciones = canal.instruccionesRedSocial(null, false);

            assertThat(instrucciones).contains("\"ya te sigo\"", "\"ya compartí tu publicación\"", "\"ya comenté\"");
            assertThat(instrucciones).contains("Gracias por compartir");
            assertThat(instrucciones).contains("NUNCA uses ##FAREWELL## ni ##ESCALAR## con estos mensajes");
            assertThat(instrucciones).contains("no intentes vender");
            assertThat(instrucciones).contains("No confirmes boletos");
        }
    }

    @Test
    void elPrimerMensajeConservaLaBienvenidaYTambienAgradece() {
        String instrucciones = facebook.instruccionesRedSocial(null, true);

        assertThat(instrucciones).contains("AVISOS Y AGRADECIMIENTOS");
        assertThat(instrucciones).contains("PRIMER comentario");
        assertThat(instrucciones).contains("##ESCALAR##");
    }
}
