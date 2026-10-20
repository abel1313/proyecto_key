package com.ventas.key.hexagonal.iarespaldo.dominio.modelo;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class RespaldoIaTest {

    private static final ProveedorIa OPENAI = new ProveedorIa("openai", "https://api.openai.com/v1", "k1", "gpt-4o-mini");
    private static final ProveedorIa DEEPSEEK = new ProveedorIa("deepseek", "https://api.deepseek.com", "k2", "deepseek-chat");
    private static final Duration DIEZ_MIN = Duration.ofMinutes(10);

    @Test
    void sinSaldoOLlave_cambiaDeProveedor() {
        assertThat(PoliticaDeRespaldoIa.clasificar(401, null)).isEqualTo(TipoDeFallo.SIN_SALDO_O_LLAVE);
        assertThat(PoliticaDeRespaldoIa.clasificar(402, "Insufficient Balance")).isEqualTo(TipoDeFallo.SIN_SALDO_O_LLAVE);
        assertThat(PoliticaDeRespaldoIa.clasificar(403, null)).isEqualTo(TipoDeFallo.SIN_SALDO_O_LLAVE);
        // OpenAI avisa que se acabó el saldo con 429 + "insufficient_quota".
        assertThat(PoliticaDeRespaldoIa.clasificar(429, "{\"error\":{\"code\":\"insufficient_quota\"}}"))
                .isEqualTo(TipoDeFallo.SIN_SALDO_O_LLAVE);
    }

    @Test
    void limiteTemporalCaidaYSinRespuesta_tambienCambian() {
        assertThat(PoliticaDeRespaldoIa.clasificar(429, "Rate limit reached")).isEqualTo(TipoDeFallo.LIMITE_TEMPORAL);
        assertThat(PoliticaDeRespaldoIa.clasificar(500, null)).isEqualTo(TipoDeFallo.CAIDO);
        assertThat(PoliticaDeRespaldoIa.clasificar(503, null)).isEqualTo(TipoDeFallo.CAIDO);
        assertThat(PoliticaDeRespaldoIa.clasificar(null, null)).isEqualTo(TipoDeFallo.CAIDO);
        assertThat(TipoDeFallo.CAIDO.cambiaDeProveedor()).isTrue();
    }

    @Test
    void errorNuestro_noCambia() {
        assertThat(PoliticaDeRespaldoIa.clasificar(400, null)).isEqualTo(TipoDeFallo.NUESTRO);
        assertThat(PoliticaDeRespaldoIa.clasificar(404, null)).isEqualTo(TipoDeFallo.NUESTRO);
        assertThat(TipoDeFallo.NUESTRO.cambiaDeProveedor()).isFalse();
    }

    @Test
    void elRespaldoSoloEntraSiTieneLlave() {
        ProveedorIa sinLlave = new ProveedorIa("deepseek", "https://api.deepseek.com", "", "deepseek-chat");
        assertThat(PoliticaDeRespaldoIa.ordenDePrueba(OPENAI, DEEPSEEK)).containsExactly(OPENAI, DEEPSEEK);
        assertThat(PoliticaDeRespaldoIa.ordenDePrueba(OPENAI, sinLlave)).containsExactly(OPENAI);
        assertThat(PoliticaDeRespaldoIa.ordenDePrueba(OPENAI, null)).containsExactly(OPENAI);
    }

    @Test
    void elProveedorQueFalloDescansaYVuelveAlCabo() {
        CircuitoDeProveedores circuito = new CircuitoDeProveedores();
        Instant t0 = Instant.parse("2026-10-20T10:00:00Z");

        assertThat(circuito.disponible(OPENAI, t0)).isTrue();
        // Primera vez: avisar. Sin saldo descansa los 10 minutos.
        assertThat(circuito.registrarFallo(OPENAI, TipoDeFallo.SIN_SALDO_O_LLAVE, DIEZ_MIN, t0)).isTrue();
        assertThat(circuito.disponible(OPENAI, t0.plusSeconds(60))).isFalse();
        assertThat(circuito.disponible(OPENAI, t0.plus(DIEZ_MIN))).isTrue();
        // El otro proveedor no se ve afectado.
        assertThat(circuito.disponible(DEEPSEEK, t0)).isTrue();
    }

    @Test
    void elAvisoSaleUnaSolaVezMientrasDescansa() {
        CircuitoDeProveedores circuito = new CircuitoDeProveedores();
        Instant t0 = Instant.parse("2026-10-20T10:00:00Z");
        assertThat(circuito.registrarFallo(OPENAI, TipoDeFallo.CAIDO, DIEZ_MIN, t0)).isTrue();
        assertThat(circuito.registrarFallo(OPENAI, TipoDeFallo.CAIDO, DIEZ_MIN, t0.plusSeconds(10))).isFalse();
        // Un error nuestro no lo manda a descansar.
        assertThat(circuito.registrarFallo(DEEPSEEK, TipoDeFallo.NUESTRO, DIEZ_MIN, t0)).isFalse();
        assertThat(circuito.disponible(DEEPSEEK, t0)).isTrue();
    }

    @Test
    void siContestaBien_dejaDeDescansar() {
        CircuitoDeProveedores circuito = new CircuitoDeProveedores();
        Instant t0 = Instant.parse("2026-10-20T10:00:00Z");
        circuito.registrarFallo(OPENAI, TipoDeFallo.CAIDO, DIEZ_MIN, t0);
        circuito.registrarExito(OPENAI);
        assertThat(circuito.disponible(OPENAI, t0)).isTrue();
    }

    @Test
    void laLlaveNuncaSeImprime() {
        assertThat(OPENAI.toString()).doesNotContain("k1");
    }
}
