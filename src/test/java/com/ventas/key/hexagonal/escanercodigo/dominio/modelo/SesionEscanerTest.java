package com.ventas.key.hexagonal.escanercodigo.dominio.modelo;

import com.ventas.key.hexagonal.escanercodigo.dominio.excepcion.EscanerException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SesionEscanerTest {

    private static final Instant T0 = Instant.parse("2026-11-02T12:00:00Z");

    private SesionEscaner nueva() {
        return new SesionEscaner("abc", "1.2.3.4", T0);
    }

    @Test
    @DisplayName("E1: vale 30 minutos y después ya no recibe ni entrega")
    void vigencia() {
        SesionEscaner s = nueva();
        for (int min = 1; min <= 29; min++) {
            s.leer(0, T0.plusSeconds(min * 60L));          // la PC sigue preguntando (si no, E7 la libera antes)
        }
        assertThat(s.estaVigente(T0.plusSeconds(29 * 60))).isTrue();
        assertThat(s.estaVigente(T0.plusSeconds(30 * 60))).isFalse();
        assertThatThrownBy(() -> s.registrar("7501234567890", T0.plusSeconds(31 * 60))).isInstanceOf(EscanerException.SesionNoDisponible.class);
        assertThatThrownBy(() -> s.leer(0, T0.plusSeconds(31 * 60))).isInstanceOf(EscanerException.SesionNoDisponible.class);
    }

    @Test
    @DisplayName("E2: se limpia el código y se rechaza lo que no parece código de barras")
    void validacion() {
        SesionEscaner s = nueva();
        assertThat(s.registrar("  7501234567890 \n", T0).codigo()).isEqualTo("7501234567890");
        for (String malo : new String[]{null, "", "  ", "ab", "a".repeat(101), "75012\n34567", "código-ñ"}) {
            assertThatThrownBy(() -> s.registrar(malo, T0)).as("«%s»", malo).isInstanceOf(EscanerException.CodigoInvalido.class);
        }
    }

    @Test
    @DisplayName("E3: la PC pide los códigos mayores al último que vio, y solo se guardan los últimos 20")
    void secuencia() {
        SesionEscaner s = nueva();
        s.registrar("AAA111", T0);
        s.registrar("BBB222", T0.plusSeconds(1));
        assertThat(s.leer(0, T0.plusSeconds(2)).codigos()).extracting(CodigoEscaneado::codigo).containsExactly("AAA111", "BBB222");
        assertThat(s.leer(1, T0.plusSeconds(2)).codigos()).extracting(CodigoEscaneado::codigo).containsExactly("BBB222");
        assertThat(s.leer(2, T0.plusSeconds(2)).codigos()).isEmpty();
        assertThat(s.leer(0, T0.plusSeconds(2)).ultimaSecuencia()).isEqualTo(2);

        SesionEscaner larga = nueva();
        for (int i = 0; i < 25; i++) {
            larga.registrar("COD" + i + "XX", T0.plusSeconds(i * 3L));
        }
        var lectura = larga.leer(0, T0.plusSeconds(80));
        assertThat(lectura.codigos()).hasSize(20);
        assertThat(lectura.ultimaSecuencia()).isEqualTo(25);
    }

    @Test
    @DisplayName("E4: máximo 30 códigos por minuto; pasado el minuto se puede de nuevo")
    void ritmo() {
        SesionEscaner s = nueva();
        for (int i = 0; i < 30; i++) {
            s.registrar("COD" + i + "XX", T0.plusMillis(i * 100L));
        }
        assertThatThrownBy(() -> s.registrar("DEMASIADO", T0.plusSeconds(5))).isInstanceOf(EscanerException.DemasiadasSolicitudes.class);
        assertThat(s.registrar("YA-PUEDE", T0.plusSeconds(65)).codigo()).isEqualTo("YA-PUEDE");
    }

    @Test
    @DisplayName("E7: si la PC no pregunta en 2 minutos la conexión se libera; preguntar la mantiene")
    void inactividad() {
        SesionEscaner abandonada = nueva();
        assertThat(abandonada.estaVigente(T0.plusSeconds(119))).isTrue();
        assertThat(abandonada.estaVigente(T0.plusSeconds(121))).isFalse();

        SesionEscaner consultada = nueva();
        for (int seg = 60; seg <= 600; seg += 60) {
            consultada.leer(0, T0.plusSeconds(seg));       // la PC sigue preguntando
        }
        assertThat(consultada.estaVigente(T0.plusSeconds(650))).isTrue();
        assertThat(consultada.estaVigente(T0.plusSeconds(600 + 125))).isFalse();   // dejó de preguntar
    }

    @Test
    @DisplayName("E5: cerrada, ya no acepta códigos")
    void cerrada() {
        SesionEscaner s = nueva();
        s.cerrar();
        assertThat(s.estaVigente(T0)).isFalse();
        assertThatThrownBy(() -> s.registrar("7501234567890", T0)).isInstanceOf(EscanerException.SesionNoDisponible.class);
    }
}
