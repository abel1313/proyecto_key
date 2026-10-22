package com.ventas.key.hexagonal.pagotarjeta.dominio.modelo;

import com.ventas.key.hexagonal.pagotarjeta.dominio.excepcion.PagoConTarjetaException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IntentoDePagoTest {

    private static final Instant T0 = Instant.parse("2026-10-21T10:00:00.123456Z");

    private IntentoDePago nuevo(CanalDePago canal) {
        return IntentoDePago.nuevo(1, 50, canal, new BigDecimal("450"), 1, "admin", T0);
    }

    private IntentoDePago aprobado() {
        return nuevo(CanalDePago.TERMINAL).conId(7L).enviado("ext-1", T0).aprobado(T0);
    }

    @Test
    void nuevoNaceCreadoConElMontoEnCentavosYFechasEnMilisegundos() {
        IntentoDePago i = nuevo(CanalDePago.TERMINAL);
        assertThat(i.estado()).isEqualTo(EstadoIntento.CREADO);
        assertThat(i.monto().scale()).isEqualTo(2);
        assertThat(i.creadoEn()).isEqualTo(Instant.parse("2026-10-21T10:00:00.123Z"));
    }

    @Test
    void validaLosDatosAlCrear() {
        assertThatThrownBy(() -> IntentoDePago.nuevo(null, 1, CanalDePago.TERMINAL, BigDecimal.TEN, 1, "a", T0)).hasMessageContaining("pedido");
        assertThatThrownBy(() -> IntentoDePago.nuevo(1, 1, CanalDePago.TERMINAL, BigDecimal.ZERO, 1, "a", T0)).hasMessageContaining("mayor a 0");
        assertThatThrownBy(() -> IntentoDePago.nuevo(1, 1, CanalDePago.TERMINAL, BigDecimal.TEN, 0, "a", T0)).hasMessageContaining("cuotas");
        assertThatThrownBy(() -> IntentoDePago.nuevo(1, 1, CanalDePago.TERMINAL, BigDecimal.TEN, 1, " ", T0)).hasMessageContaining("quién");
    }

    @Test
    void flujoNormalCreadoEnviadoAprobado() {
        IntentoDePago i = aprobado();
        assertThat(i.estado()).isEqualTo(EstadoIntento.APROBADO);
        assertThat(i.idExterno()).isEqualTo("ext-1");
    }

    @Test
    void noSePuedeAprobarUnoQueNoSeEnvio_yLosMensajesUsanEtiquetas() {
        assertThatThrownBy(() -> nuevo(CanalDePago.TERMINAL).aprobado(T0))
                .isInstanceOf(PagoConTarjetaException.class)
                .hasMessageContaining("creado").hasMessageContaining("aprobado")
                .hasMessageNotContaining("CREADO");
    }

    @Test
    void expiraSegunSuCanal_T7() {
        IntentoDePago terminal = nuevo(CanalDePago.TERMINAL);
        IntentoDePago linea = nuevo(CanalDePago.EN_LINEA);
        assertThat(terminal.haExpirado(T0.plusSeconds(9 * 60))).isFalse();
        assertThat(terminal.haExpirado(T0.plusSeconds(11 * 60))).isTrue();
        assertThat(linea.haExpirado(T0.plusSeconds(11 * 60))).isFalse();
        assertThat(linea.haExpirado(T0.plusSeconds(31 * 60))).isTrue();
        assertThat(aprobado().haExpirado(T0.plusSeconds(3600))).isFalse();
    }

    @Test
    void aprobacionTardia_rechazadoCanceladoYExpiradoPuedenPasarAAprobado() {
        IntentoDePago base = nuevo(CanalDePago.TERMINAL).conId(1L).enviado("e", T0);
        assertThat(base.rechazado(T0).aprobado(T0).estado()).isEqualTo(EstadoIntento.APROBADO);
        assertThat(base.cancelado(T0).aprobado(T0).estado()).isEqualTo(EstadoIntento.APROBADO);
        assertThat(base.expirado(T0).aprobado(T0).estado()).isEqualTo(EstadoIntento.APROBADO);
        assertThat(base.expirado(T0).cobradoSinAplicar(T0).estado()).isEqualTo(EstadoIntento.COBRADO_SIN_APLICAR);
    }

    @Test
    void devolucionParcialYTotal_T8() {
        IntentoDePago parcial = aprobado().devolviendo(new BigDecimal("100"), T0);
        assertThat(parcial.estado()).isEqualTo(EstadoIntento.DEVUELTO_PARCIAL);
        assertThat(parcial.restanteADevolver()).isEqualByComparingTo("350");
        assertThat(parcial.devolviendo(new BigDecimal("350"), T0).estado()).isEqualTo(EstadoIntento.DEVUELTO);
    }

    @Test
    void devolucionRechazaDeMasDecimalesDeMasYEstadosQueNoSeCobraron() {
        assertThatThrownBy(() -> aprobado().devolviendo(new BigDecimal("451"), T0)).hasMessageContaining("máximo $450.00");
        assertThatThrownBy(() -> aprobado().devolviendo(new BigDecimal("1.001"), T0)).hasMessageContaining("centavos");
        assertThatThrownBy(() -> aprobado().devolviendo(BigDecimal.ZERO, T0)).hasMessageContaining("mayor a 0");
        assertThatThrownBy(() -> nuevo(CanalDePago.TERMINAL).devolviendo(BigDecimal.TEN, T0)).hasMessageContaining("creado");
    }

    @Test
    void conIdExternoNoCambiaElEstado() {
        IntentoDePago expirado = nuevo(CanalDePago.TERMINAL).conId(1L).expirado(T0);
        assertThat(expirado.conIdExterno("ext-9").estado()).isEqualTo(EstadoIntento.EXPIRADO);
        assertThat(expirado.conIdExterno("ext-9").idExterno()).isEqualTo("ext-9");
    }
}
