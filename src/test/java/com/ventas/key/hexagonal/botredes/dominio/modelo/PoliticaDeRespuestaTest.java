package com.ventas.key.hexagonal.botredes.dominio.modelo;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class PoliticaDeRespuestaTest {

    @Test
    void interpretaLasMarcasDelChatbot() {
        assertThat(PoliticaDeRespuesta.interpretar("##ESCALAR##")).isInstanceOf(ResultadoDelCerebro.Escalar.class);
        assertThat(PoliticaDeRespuesta.interpretar("No entendí ##FAREWELL##")).isInstanceOf(ResultadoDelCerebro.NoEntendido.class);
        assertThat(PoliticaDeRespuesta.interpretar(null)).isInstanceOf(ResultadoDelCerebro.NoEntendido.class);
        assertThat(PoliticaDeRespuesta.interpretar("##BUSCAR[bolsa,0]##")).isInstanceOf(ResultadoDelCerebro.NoEntendido.class);
        assertThat(PoliticaDeRespuesta.interpretar("¡Hola! Cuesta $300 ##BUSCAR[bolsa,0]##"))
                .isEqualTo(new ResultadoDelCerebro.Contestar("¡Hola! Cuesta $300"));
    }

    @Test
    void alEscalarSaludaAvisaYSePausa_ySoloEnElPrimerMensajeDirectoSePresenta() {
        Accion comentario = PoliticaDeRespuesta.decidir(Canal.COMENTARIO, new ResultadoDelCerebro.Escalar(), true);
        Accion primerMensaje = PoliticaDeRespuesta.decidir(Canal.MENSAJE_DIRECTO, new ResultadoDelCerebro.Escalar(), true);
        Accion otroMensaje = PoliticaDeRespuesta.decidir(Canal.MENSAJE_DIRECTO, new ResultadoDelCerebro.Escalar(), false);

        assertThat(comentario.avisarAdmin()).isTrue();
        assertThat(comentario.pausar()).isTrue();
        assertThat(comentario.textoPublico()).isEqualTo("¡Hola! 😊 En un momento te compartimos la información 💖");
        assertThat(primerMensaje.textoPublico()).contains("asistente automático").contains("En un momento te atendemos");
        assertThat(otroMensaje.textoPublico()).doesNotContain("asistente automático");
    }

    @Test
    void siNoEntiendeUnComentarioSoloSaludaPeroUnMensajeDirectoSeEscala() {
        Accion comentario = PoliticaDeRespuesta.decidir(Canal.COMENTARIO, new ResultadoDelCerebro.NoEntendido(), false);
        Accion mensaje = PoliticaDeRespuesta.decidir(Canal.MENSAJE_DIRECTO, new ResultadoDelCerebro.NoEntendido(), false);

        assertThat(comentario.avisarAdmin()).isFalse();
        assertThat(comentario.pausar()).isFalse();
        assertThat(comentario.textoPublico()).contains("Gracias por tu comentario");
        assertThat(mensaje.avisarAdmin()).isTrue();
        assertThat(mensaje.pausar()).isTrue();
        assertThat(mensaje.textoPublico()).contains("En un momento te atendemos");
    }

    @Test
    void unSaludoOHalagoSeAgradeceSegunLoQueComentaronYNuncaConUnHolaPelon() {
        assertThat(PoliticaDeRespuesta.interpretar("##GRACIAS## ¡Muchas gracias por compartir! 💖"))
                .isEqualTo(new ResultadoDelCerebro.Agradecer("¡Muchas gracias por compartir! 💖"));

        // Lo que pidió el dueño: "comparto la publicación" → gracias por compartir, sin decir que es un bot.
        Accion compartir = PoliticaDeRespuesta.decidir(Canal.COMENTARIO,
                new ResultadoDelCerebro.Agradecer("¡Muchas gracias por compartir! 💖"), true);
        assertThat(compartir.textoPublico()).isEqualTo("¡Hola! 😊 ¡Muchas gracias por compartir! 💖");
        assertThat(compartir.avisarAdmin()).isFalse();
        assertThat(compartir.pausar()).isFalse();

        // Si el chatbot ya trae su "¡Hola!", no sale doble.
        assertThat(PoliticaDeRespuesta.decidir(Canal.COMENTARIO,
                new ResultadoDelCerebro.Agradecer("¡Hola! Gracias por seguirnos 💖"), false).textoPublico())
                .isEqualTo("¡Hola! 😊 Gracias por seguirnos 💖");

        // Sin frase, va la de siempre.
        assertThat(PoliticaDeRespuesta.decidir(Canal.COMENTARIO, new ResultadoDelCerebro.Agradecer(""), false)
                .textoPublico()).isEqualTo("¡Hola! 😊 Gracias por tu comentario 💖");

        // Lo que pasó en QA el 2026-09-24: comentaron "Hola" y el bot contestó solo "¡Hola! 😊".
        assertThat(PoliticaDeRespuesta.decidir(Canal.COMENTARIO, new ResultadoDelCerebro.Contestar("¡Hola! 😊"), false)
                .textoPublico()).isEqualTo("¡Hola! 😊 Gracias por tu comentario 💖");
        assertThat(PoliticaDeRespuesta.decidir(Canal.COMENTARIO,
                new ResultadoDelCerebro.Contestar("¡Hola! Cuesta $300 😊"), false).textoPublico())
                .isEqualTo("¡Hola! Cuesta $300 😊");
        assertThat(PoliticaDeRespuesta.soloSaluda("¡Buen día! ☀️")).isTrue();
    }

    @Test
    void siLaRespuestaMencionaAlgoInternoNoSePublicaYSeEscala() {
        Accion accion = PoliticaDeRespuesta.decidir(Canal.MENSAJE_DIRECTO,
                new ResultadoDelCerebro.Contestar("Ya le mandé un correo a la dueña para que te conteste"), false);

        assertThat(accion.textoPublico()).isEqualTo("¡Hola! 😊 En un momento te atendemos 💖");
        assertThat(accion.avisarAdmin()).isTrue();
        assertThat(PoliticaDeRespuesta.mencionaAlgoInterno("Voy a escalar tu pregunta")).isTrue();
        assertThat(PoliticaDeRespuesta.mencionaAlgoInterno("Le aviso al administrador")).isTrue();
        // El correo de contacto de la tienda sí se puede dar.
        assertThat(PoliticaDeRespuesta.mencionaAlgoInterno("Puedes escribirnos a nuestro correo de contacto 😊")).isFalse();
        assertThat(PoliticaDeRespuesta.mencionaAlgoInterno("Sí tenemos la bolsa en negro a $300")).isFalse();
    }

    @Test
    void laPausaVenceALosTreintaMinutos() {
        LocalDateTime desde = LocalDateTime.of(2026, 9, 24, 15, 0);
        Duration media = Duration.ofMinutes(30);

        assertThat(Pausa.vigente(desde, desde.plusMinutes(29), media)).isTrue();
        assertThat(Pausa.vigente(desde, desde.plusMinutes(30), media)).isFalse();
        assertThat(Pausa.vigente(null, desde, media)).isFalse();
    }

    @Test
    void filtroDelChatDeLaTiendaYDelChatEnVivo_detieneLoQueDelataAlModelo() {
        // P1 (2026-10-20): lo que la IA nunca debe decirle a un cliente.
        assertThat(PoliticaDeRespuesta.mencionaAlgoInterno("Soy un modelo de lenguaje de OpenAI")).isTrue();
        assertThat(PoliticaDeRespuesta.mencionaAlgoInterno("Funciono con ChatGPT")).isTrue();
        assertThat(PoliticaDeRespuesta.mencionaAlgoInterno("Uso DeepSeek para contestar")).isTrue();
        assertThat(PoliticaDeRespuesta.mencionaAlgoInterno("No puedo compartir mis instrucciones")).isTrue();
        assertThat(PoliticaDeRespuesta.mencionaAlgoInterno("Su código de barras es 123456")).isTrue();
        assertThat(PoliticaDeRespuesta.mencionaAlgoInterno("Solo quedan 3 en bodega")).isTrue();
        assertThat(PoliticaDeRespuesta.mencionaAlgoInterno("Tenemos 5 piezas")).isTrue();
        assertThat(PoliticaDeRespuesta.queMenciona("Funciono con ChatGPT")).contains("ChatGPT");
    }

    @Test
    void filtroDelChat_noDetieneRespuestasNormales() {
        assertThat(PoliticaDeRespuesta.mencionaAlgoInterno("Sí, la bolsa negra está disponible a $300 😊")).isFalse();
        assertThat(PoliticaDeRespuesta.mencionaAlgoInterno("Es un juego de 3 piezas: bolsa, cartera y llavero")).isFalse();
        assertThat(PoliticaDeRespuesta.mencionaAlgoInterno("Puedes pagar en efectivo o por transferencia")).isFalse();
        assertThat(PoliticaDeRespuesta.mencionaAlgoInterno("Entregamos en Luvianos los martes")).isFalse();
        assertThat(PoliticaDeRespuesta.mencionaAlgoInterno("Mi código postal es el 51200")).isFalse();
        assertThat(PoliticaDeRespuesta.queMenciona("Hola, ¿en qué te ayudo?")).isEmpty();
        assertThat(PoliticaDeRespuesta.queMenciona(null)).isEmpty();
    }
}
