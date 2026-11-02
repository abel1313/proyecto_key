package com.ventas.key.hexagonal.graficasventas.aplicacion.servicio;

import com.ventas.key.hexagonal.graficasventas.dominio.excepcion.GraficasVentasException;
import com.ventas.key.hexagonal.graficasventas.dominio.modelo.ComparativoDelMes;
import com.ventas.key.hexagonal.graficasventas.dominio.modelo.GananciaPorProducto;
import com.ventas.key.hexagonal.graficasventas.dominio.modelo.ProductoRentable;
import com.ventas.key.hexagonal.graficasventas.dominio.modelo.VentasDelMes;
import com.ventas.key.hexagonal.graficasventas.dominio.puerto.salida.GraficasPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ComparativoYGananciaServiceTest {

    private GraficasPort puerto;
    private ConsultarGraficasService servicio;

    @BeforeEach
    void preparar() {
        puerto = mock(GraficasPort.class);
        servicio = new ConsultarGraficasService(puerto, Clock.fixed(Instant.parse("2026-11-15T18:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    @DisplayName("comparativo: este mes del 1 al 15 contra octubre del 1 al 15")
    void comparaTramosIguales() {
        LocalDate hoy = LocalDate.of(2026, 11, 15);
        when(puerto.ventasPorMes(LocalDate.of(2026, 11, 1), hoy)).thenReturn(Map.of(YearMonth.of(2026, 11), new VentasDelMes(12, 6000, 2400)));
        when(puerto.gastosPorMes(LocalDate.of(2026, 11, 1), hoy)).thenReturn(Map.of(YearMonth.of(2026, 11), 400.0));
        when(puerto.ventasPorMes(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 15))).thenReturn(Map.of(YearMonth.of(2026, 10), new VentasDelMes(10, 4000, 1600)));
        when(puerto.gastosPorMes(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 15))).thenReturn(Map.of());

        ComparativoDelMes c = servicio.comparativoDelMes();

        assertThat(c.hastaElDia()).isEqualTo(15);
        assertThat(c.actual().ventas()).isEqualTo(6000);
        assertThat(c.actual().gananciaNeta()).isEqualTo(2000);
        assertThat(c.anterior().ventas()).isEqualTo(4000);
        assertThat(c.anterior().gastos()).isEqualTo(0);
        assertThat(ComparativoDelMes.cambio(c.actual().ventas(), c.anterior().ventas())).isEqualTo(50.0);
        assertThat(ComparativoDelMes.cambio(c.actual().gastos(), c.anterior().gastos())).isNull();   // antes era 0: no se compara
    }

    @Test
    @DisplayName("si el mes anterior es más corto, el tramo termina en su último día (31 de marzo contra febrero)")
    void mesAnteriorMasCorto() {
        servicio = new ConsultarGraficasService(puerto, Clock.fixed(Instant.parse("2026-03-31T18:00:00Z"), ZoneOffset.UTC));
        when(puerto.ventasPorMes(any(), any())).thenReturn(Map.of());
        when(puerto.gastosPorMes(any(), any())).thenReturn(Map.of());

        ComparativoDelMes c = servicio.comparativoDelMes();

        assertThat(c.anterior().desde()).isEqualTo("2026-02-01");
        assertThat(c.anterior().hasta()).isEqualTo("2026-02-28");
    }

    @Test
    @DisplayName("ganancia por producto: calcula el margen y qué parte del total dejan los mejores")
    void gananciaPorProducto() {
        when(puerto.gananciaPorProducto(LocalDate.of(2026, 8, 18), LocalDate.of(2026, 11, 15), 2)).thenReturn(List.of(
                new ProductoRentable(1, "Bolsa Lucía", 4, 1400, 700, 0), new ProductoRentable(2, "Falda", 2, 600, 150, 0)));
        when(puerto.gananciaTotalDeRenglones(LocalDate.of(2026, 8, 18), LocalDate.of(2026, 11, 15))).thenReturn(1700.0);

        GananciaPorProducto g = servicio.gananciaPorProducto(90, 2);

        assertThat(g.productos().get(0).margen()).isEqualTo(50.0);
        assertThat(g.productos().get(1).margen()).isEqualTo(25.0);
        assertThat(g.gananciaDeLosMejores()).isEqualTo(850.0);
        assertThat(g.gananciaTotal()).isEqualTo(1700.0);
    }

    @Test
    @DisplayName("ganancia por producto: días fuera de 7–365 o límite fuera de 1–20 se rechaza")
    void fueraDeRango() {
        assertThatThrownBy(() -> servicio.gananciaPorProducto(3, 10)).isInstanceOf(GraficasVentasException.class);
        assertThatThrownBy(() -> servicio.gananciaPorProducto(90, 0)).isInstanceOf(GraficasVentasException.class);
        assertThatThrownBy(() -> servicio.gananciaPorProducto(90, 21)).isInstanceOf(GraficasVentasException.class);
        verify(puerto, never()).gananciaPorProducto(any(), any(), org.mockito.ArgumentMatchers.anyInt());
    }
}
