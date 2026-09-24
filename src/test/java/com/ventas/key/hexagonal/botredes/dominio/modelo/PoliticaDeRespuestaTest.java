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
    void alEscalarSaludaAvisaYSePausaYSoloLaPrimeraVezSePresenta() {
        Accion primera = PoliticaDeRespuesta.decidir(Canal.COMENTARIO, new ResultadoDelCerebro.Escalar(), true);
        Accion despues = PoliticaDeRespuesta.decidir(Canal.COMENTARIO, new ResultadoDelCerebro.Escalar(), false);

        assertThat(primera.avisarAdmin()).isTrue();
        assertThat(primera.pausar()).isTrue();
        assertThat(primera.textoPublico()).startsWith("¡Hola!").contains("asistente automático")
                .contains("En un momento te compartimos la información");
        assertThat(despues.textoPublico()).startsWith("¡Hola!").doesNotContain("asistente automático");
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
    void laPausaVenceALosTreintaMinutos() {
        LocalDateTime desde = LocalDateTime.of(2026, 9, 24, 15, 0);
        Duration media = Duration.ofMinutes(30);

        assertThat(Pausa.vigente(desde, desde.plusMinutes(29), media)).isTrue();
        assertThat(Pausa.vigente(desde, desde.plusMinutes(30), media)).isFalse();
        assertThat(Pausa.vigente(null, desde, media)).isFalse();
    }
}
