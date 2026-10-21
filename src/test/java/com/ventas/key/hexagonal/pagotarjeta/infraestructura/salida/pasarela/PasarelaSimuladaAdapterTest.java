package com.ventas.key.hexagonal.pagotarjeta.infraestructura.salida.pasarela;

import com.ventas.key.hexagonal.pagotarjeta.dominio.modelo.CanalDePago;
import com.ventas.key.hexagonal.pagotarjeta.dominio.modelo.EstadoExterno;
import com.ventas.key.hexagonal.pagotarjeta.dominio.modelo.IntentoDePago;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PasarelaSimuladaAdapterTest {

    private static class Reloj extends Clock {
        Instant ahora = Instant.parse("2026-10-21T10:00:00Z");
        @Override public java.time.ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId z) { return this; }
        @Override public Instant instant() { return ahora; }
    }

    private final Reloj reloj = new Reloj();
    private final PasarelaSimuladaAdapter pasarela = new PasarelaSimuladaAdapter(reloj, Duration.ofSeconds(4));

    private String cobrar(long id, String monto) {
        IntentoDePago i = IntentoDePago.nuevo(1, 1, CanalDePago.TERMINAL, new BigDecimal(monto), 1, "admin", reloj.ahora).conId(id);
        return pasarela.crearCobro(i);
    }

    @Test
    void soloExisteEnDevYQa_yConLaPropiedadExplicita() {
        Profile perfiles = PasarelaSimuladaAdapter.class.getAnnotation(Profile.class);
        assertThat(Arrays.asList(perfiles.value())).containsExactlyInAnyOrder("dev", "qa");
        ConditionalOnProperty prop = PasarelaSimuladaAdapter.class.getAnnotation(ConditionalOnProperty.class);
        assertThat(prop.name()).containsExactly("pagos.tarjeta.pasarela");
        assertThat(prop.havingValue()).isEqualTo("simulada");
    }

    @Test
    void apruebaDespuesDeLaDemora() {
        String id = cobrar(1, "450.00");
        assertThat(pasarela.consultar(id)).isEqualTo(EstadoExterno.PENDIENTE);
        reloj.ahora = reloj.ahora.plusSeconds(5);
        assertThat(pasarela.consultar(id)).isEqualTo(EstadoExterno.APROBADO);
    }

    @Test
    void losCentavosProvocanCadaResultado() {
        String rechazada = cobrar(1, "450.01");
        String muda = cobrar(2, "450.02");
        String cancelada = cobrar(3, "450.03");
        reloj.ahora = reloj.ahora.plusSeconds(60);
        assertThat(pasarela.consultar(rechazada)).isEqualTo(EstadoExterno.RECHAZADO);
        assertThat(pasarela.consultar(muda)).isEqualTo(EstadoExterno.PENDIENTE);
        assertThat(pasarela.consultar(cancelada)).isEqualTo(EstadoExterno.CANCELADO);
    }

    @Test
    void unCobroAprobadoNoSePuedeCancelar_perounoPendienteSi() {
        String id = cobrar(1, "450.00");
        pasarela.cancelar(id);
        reloj.ahora = reloj.ahora.plusSeconds(60);
        assertThat(pasarela.consultar(id)).isEqualTo(EstadoExterno.CANCELADO);       // cancelado antes de aprobarse: se queda cancelado

        String otro = cobrar(2, "450.00");
        reloj.ahora = reloj.ahora.plusSeconds(60);
        assertThatThrownBy(() -> pasarela.cancelar(otro)).hasMessageContaining("ya está aprobado");
    }

    @Test
    void sePuedeReencontrarPorReferenciaYLaDevolucionEsIdempotente() {
        String id = cobrar(7, "450.00");
        assertThat(pasarela.buscarPorReferencia(7L)).get().extracting("idExterno").isEqualTo(id);
        assertThat(pasarela.buscarPorReferencia(99L)).isEmpty();
        pasarela.devolver(id, BigDecimal.TEN, "dev-7-0.00");
        pasarela.devolver(id, BigDecimal.TEN, "dev-7-0.00");
        assertThatThrownBy(() -> pasarela.devolver("sim-404", BigDecimal.TEN, "x")).hasMessageContaining("desconocido");
    }
}
