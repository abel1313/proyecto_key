package com.ventas.key.hexagonal.pagotarjeta.dominio.modelo;

import com.ventas.key.hexagonal.pagotarjeta.dominio.excepcion.PagoConTarjetaException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReglasMsiTest {

    private static final List<PlanMsi> PLANES = List.of(
            new PlanMsi(3, true, BigDecimal.ONE), new PlanMsi(6, true, BigDecimal.ONE),
            new PlanMsi(9, false, BigDecimal.ONE), new PlanMsi(12, true, BigDecimal.ONE));
    private static final List<ArticuloACobrar> SI = List.of(new ArticuloACobrar(1, "Bolsa", true));
    private static final List<ArticuloACobrar> MIXTO = List.of(new ArticuloACobrar(1, "Bolsa", true), new ArticuloACobrar(2, "Falda", false));

    private List<Integer> meses(TipoDeCobro tipo, String total, List<ArticuloACobrar> arts) {
        return ReglasMsi.mesesOfrecidos(tipo, new BigDecimal(total), arts, PLANES, new BigDecimal("300"));
    }

    @Test
    void ofreceLosPlanesActivosDeUnPedidoQueCumple() {
        assertThat(meses(TipoDeCobro.CONTADO, "450", SI)).containsExactly(3, 6, 12);
    }

    @Test
    void nadaSiAlgunArticuloNoLoAcepta_M2() {
        assertThat(meses(TipoDeCobro.CONTADO, "450", MIXTO)).isEmpty();
    }

    @Test
    void nadaBajoElMinimo_M3_peroElMinimoExactoSiAplica() {
        assertThat(meses(TipoDeCobro.CONTADO, "299.99", SI)).isEmpty();
        assertThat(meses(TipoDeCobro.CONTADO, "300", SI)).isNotEmpty();
    }

    @Test
    void ApartadoEIrPagandoNuncaTienenMsi_M5() {
        assertThat(meses(TipoDeCobro.APARTADO, "450", SI)).isEmpty();
        assertThat(meses(TipoDeCobro.IR_PAGANDO, "450", SI)).isEmpty();
    }

    @Test
    void sinArticulosONulosNoHayMsi() {
        assertThat(meses(TipoDeCobro.CONTADO, "450", List.of())).isEmpty();
        assertThat(ReglasMsi.mesesOfrecidos(TipoDeCobro.CONTADO, null, SI, PLANES, null)).isEmpty();
    }

    @Test
    void validarCuotas_deContadoSiempreValeYLosPlanesInactivosNo() {
        ReglasMsi.validarCuotas(1, TipoDeCobro.CONTADO, new BigDecimal("10"), MIXTO, PLANES, null);
        ReglasMsi.validarCuotas(6, TipoDeCobro.CONTADO, new BigDecimal("450"), SI, PLANES, null);
        assertThatThrownBy(() -> ReglasMsi.validarCuotas(9, TipoDeCobro.CONTADO, new BigDecimal("450"), SI, PLANES, null))
                .isInstanceOf(PagoConTarjetaException.class).hasMessageContaining("9 meses");
        assertThatThrownBy(() -> ReglasMsi.validarCuotas(0, TipoDeCobro.CONTADO, new BigDecimal("450"), SI, PLANES, null))
                .hasMessageContaining("cuotas");
    }

    @Test
    void validarCuotas_explicaElMotivoYNombraElArticulo() {
        assertThatThrownBy(() -> ReglasMsi.validarCuotas(3, TipoDeCobro.CONTADO, new BigDecimal("450"), MIXTO, PLANES, null))
                .hasMessageContaining("no acepta meses sin intereses").hasMessageContaining("Falda");
        assertThatThrownBy(() -> ReglasMsi.validarCuotas(3, TipoDeCobro.CONTADO, new BigDecimal("100"), SI, PLANES, null))
                .hasMessageContaining("$300.00");
        assertThatThrownBy(() -> ReglasMsi.validarCuotas(3, TipoDeCobro.APARTADO, new BigDecimal("450"), SI, PLANES, null))
                .hasMessageContaining("no los admite");
    }

    // ── ofertaDeMeses: lo mismo que mesesOfrecidos, pero explicando por qué ──────────────

    private OfertaDeMeses oferta(TipoDeCobro tipo, String total, List<ArticuloACobrar> arts) {
        return ReglasMsi.ofertaDeMeses(tipo, new BigDecimal(total), arts, PLANES, new BigDecimal("300"));
    }

    @Test
    void laOfertaTraeLosMismosPlazosQueMesesOfrecidos_yNingunMotivo() {
        OfertaDeMeses o = oferta(TipoDeCobro.CONTADO, "450", SI);
        assertThat(o.meses()).containsExactly(3, 6, 12);
        assertThat(o.motivo()).isNull();
        assertThat(o.articulosSinMeses()).isEmpty();
        assertThat(o.hayMeses()).isTrue();
    }

    @Test
    void siUnArticuloNoAcepta_dicePorQueYCualEs() {
        OfertaDeMeses o = oferta(TipoDeCobro.CONTADO, "450", MIXTO);
        assertThat(o.meses()).isEmpty();
        assertThat(o.articulosSinMeses()).containsExactly("Falda");
        assertThat(o.motivo()).contains("Este artículo no acepta meses sin intereses").contains("Falda").contains("de contado");
    }

    @Test
    void bajoElMinimo_dicePorQueConElMonto() {
        OfertaDeMeses o = oferta(TipoDeCobro.CONTADO, "299.99", SI);
        assertThat(o.meses()).isEmpty();
        assertThat(o.motivo()).contains("al menos").contains("300");
        assertThat(o.articulosSinMeses()).isEmpty();
    }

    @Test
    void apartadoEIrPagando_nuncaTienenMeses_yLoDice() {
        assertThat(oferta(TipoDeCobro.APARTADO, "900", SI).motivo()).contains("efectivo o transferencia");
        assertThat(oferta(TipoDeCobro.IR_PAGANDO, "900", SI).hayMeses()).isFalse();
    }

    @Test
    void sinPlanesActivos_dicePorQue() {
        OfertaDeMeses o = ReglasMsi.ofertaDeMeses(TipoDeCobro.CONTADO, new BigDecimal("450"), SI,
                List.of(new PlanMsi(3, false, BigDecimal.ONE)), new BigDecimal("300"));
        assertThat(o.meses()).isEmpty();
        assertThat(o.motivo()).contains("No hay planes");
    }

    @Test
    void pedidoSinArticulos_noOfreceMeses() {
        assertThat(oferta(TipoDeCobro.CONTADO, "450", List.of()).motivo()).contains("no tiene artículos");
    }
}
