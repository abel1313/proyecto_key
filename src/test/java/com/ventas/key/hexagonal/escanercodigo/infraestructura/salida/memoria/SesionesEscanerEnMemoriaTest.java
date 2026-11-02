package com.ventas.key.hexagonal.escanercodigo.infraestructura.salida.memoria;

import com.ventas.key.hexagonal.escanercodigo.dominio.modelo.SesionEscaner;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class SesionesEscanerEnMemoriaTest {

    private static final Instant T0 = Instant.parse("2026-11-02T12:00:00Z");
    private final SesionesEscanerEnMemoria memoria = new SesionesEscanerEnMemoria(Clock.fixed(T0, ZoneOffset.UTC));

    @Test
    @DisplayName("guarda, busca, cuenta por origen y elimina")
    void basico() {
        memoria.guardar(new SesionEscaner("a", "1.1.1.1", T0));
        memoria.guardar(new SesionEscaner("b", "1.1.1.1", T0));
        memoria.guardar(new SesionEscaner("c", "2.2.2.2", T0));
        assertThat(memoria.buscar("a")).isPresent();
        assertThat(memoria.buscar("nada")).isEmpty();
        assertThat(memoria.vigentes(T0)).isEqualTo(3);
        assertThat(memoria.vigentesDe("1.1.1.1", T0)).isEqualTo(2);
        memoria.guardar(new SesionEscaner("d", "usuario:ana", T0));
        assertThat(memoria.vigentes(T0)).isEqualTo(4);
        assertThat(memoria.vigentesAnonimas(T0, "usuario:")).isEqualTo(3);              // la del personal no cuenta para el cupo anónimo
        assertThat(memoria.buscarVigentePorOrigen("usuario:ana", T0)).map(SesionEscaner::id).contains("d");
        assertThat(memoria.buscarVigentePorOrigen("usuario:beto", T0)).isEmpty();
        memoria.eliminar("d");
        memoria.eliminar("a");
        assertThat(memoria.vigentesDe("1.1.1.1", T0)).isEqualTo(1);
    }

    @Test
    @DisplayName("las vencidas (por tiempo o por inactividad) o cerradas no cuentan y la limpieza las quita")
    void limpieza() {
        SesionEscaner abandonada = new SesionEscaner("abandonada", "o", T0);              // nadie hace nada: vence a los 5 min
        SesionEscaner activa = new SesionEscaner("activa", "o", T0);
        SesionEscaner cerrada = new SesionEscaner("cerrada", "o", T0);
        cerrada.cerrar();
        for (SesionEscaner s : new SesionEscaner[]{abandonada, activa, cerrada}) memoria.guardar(s);

        Instant dentroDeTres = T0.plusSeconds(400);
        activa.leer(0, T0.plusSeconds(200));
        activa.leer(0, T0.plusSeconds(390));                                              // la PC sigue preguntando
        assertThat(memoria.vigentes(dentroDeTres)).isEqualTo(1);
        assertThat(memoria.vigentesDe("o", dentroDeTres)).isEqualTo(1);
        assertThat(memoria.limpiar(dentroDeTres)).isEqualTo(2);
        assertThat(memoria.buscar("activa")).isPresent();
        assertThat(memoria.buscar("abandonada")).isEmpty();
    }
}
