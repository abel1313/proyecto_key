package com.ventas.key.hexagonal.graficasventas.aplicacion.servicio;

import com.ventas.key.hexagonal.graficasventas.dominio.excepcion.GraficasVentasException;
import com.ventas.key.hexagonal.graficasventas.dominio.modelo.ArticuloParado;
import com.ventas.key.hexagonal.graficasventas.dominio.modelo.CapitalParado;
import com.ventas.key.hexagonal.graficasventas.dominio.modelo.CategoriaPedido;
import com.ventas.key.hexagonal.graficasventas.dominio.modelo.DineroPorCobrar;
import com.ventas.key.hexagonal.graficasventas.dominio.modelo.EstadoDePedidos;
import com.ventas.key.hexagonal.graficasventas.dominio.modelo.SaldoPendiente;
import com.ventas.key.hexagonal.graficasventas.dominio.puerto.salida.GraficasPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConsultarGraficasNuevasServiceTest {

    private GraficasPort puerto;
    private ConsultarGraficasService servicio;

    @BeforeEach
    void preparar() {
        puerto = mock(GraficasPort.class);
        servicio = new ConsultarGraficasService(puerto, Clock.fixed(Instant.parse("2026-11-15T18:00:00Z"), ZoneOffset.UTC)); // hoy = 2026-11-15
    }

    private static SaldoPendiente saldo(int id, String tipo, double saldo, String fecha) {
        return new SaldoPendiente(id, "Cliente " + id, tipo, saldo, LocalDate.parse(fecha));
    }

    @Test
    @DisplayName("por cobrar: reparte por antigüedad (0–7, 8–30, más de 30 días) y suma el total")
    void tramos() {
        when(puerto.saldosPendientes()).thenReturn(List.of(
                saldo(1, "FIADO", 100, "2026-11-15"),      // 0 días
                saldo(2, "FIADO", 200, "2026-11-08"),      // 7 días: límite del primer tramo
                saldo(3, "APARTADO", 300, "2026-11-07"),   // 8 días: segundo tramo
                saldo(4, "FIADO", 400, "2026-10-16"),      // 30 días: límite del segundo
                saldo(5, "FIADO", 500, "2026-10-15"),      // 31 días: tercer tramo
                saldo(6, "FIADO", 600, "2026-11-20")));    // fecha futura: cuenta como 0 días, no negativa

        DineroPorCobrar r = servicio.dineroPorCobrar();

        assertThat(r.tramos()).extracting(t -> t.etiqueta()).containsExactly("0 a 7 días", "8 a 30 días", "Más de 30 días");
        assertThat(r.tramos()).extracting(t -> t.pedidos()).containsExactly(3, 2, 1);
        assertThat(r.tramos()).extracting(t -> t.saldo()).containsExactly(900.0, 700.0, 500.0);
        assertThat(r.total()).isEqualTo(2100.0);
        assertThat(r.pedidos()).isEqualTo(6);
    }

    @Test
    @DisplayName("por cobrar: «a quién cobrarle primero» va del más viejo al más nuevo, con el tipo en palabras del dueño, y solo 8")
    void masAntiguos() {
        List<SaldoPendiente> muchos = new ArrayList<>();
        for (int i = 1; i <= 12; i++) {
            muchos.add(saldo(i, i == 12 ? "APARTADO" : "FIADO", 100, LocalDate.of(2026, 11, 14).minusDays(i).toString()));   // el 12 es el más viejo
        }
        when(puerto.saldosPendientes()).thenReturn(muchos);
        DineroPorCobrar r = servicio.dineroPorCobrar();

        assertThat(r.masAntiguos()).hasSize(8);
        assertThat(r.masAntiguos().get(0).pedidoId()).isEqualTo(12);
        assertThat(r.masAntiguos().get(0).tipo()).isEqualTo("Apartado");
        assertThat(r.masAntiguos().get(1).tipo()).isEqualTo("Ir pagando");
        assertThat(r.masAntiguos().get(0).dias()).isEqualTo(13);
    }

    @Test
    @DisplayName("por cobrar: sin deudas todo en cero y sin lista")
    void sinDeudas() {
        when(puerto.saldosPendientes()).thenReturn(List.of());
        DineroPorCobrar r = servicio.dineroPorCobrar();
        assertThat(r.total()).isZero();
        assertThat(r.tramos()).hasSize(3);
        assertThat(r.masAntiguos()).isEmpty();
    }

    @Test
    @DisplayName("capital parado: pide al puerto la fecha de corte, calcula stock × costo, ordena por capital y marca «nunca vendido»")
    void capitalParado() {
        when(puerto.articulosSinMovimiento(any())).thenReturn(List.of(
                new ArticuloParado(1, "Pantalón M", 3, 100.0, LocalDate.of(2026, 9, 1)),
                new ArticuloParado(2, "Bolsa", 10, 50.0, null),
                new ArticuloParado(3, "Falda", 1, 80.0, LocalDate.of(2026, 6, 1))));

        CapitalParado r = servicio.capitalParado(60);

        verify(puerto).articulosSinMovimiento(LocalDate.of(2026, 9, 16));   // hoy 15-nov menos 60 días
        assertThat(r.articulos()).isEqualTo(3);
        assertThat(r.piezas()).isEqualTo(14);
        assertThat(r.capital()).isEqualTo(880.0);                           // 300 + 500 + 80
        assertThat(r.mayores()).extracting(c -> c.articuloId()).containsExactly(2, 1, 3);
        assertThat(r.mayores().get(0).diasSinVender()).isNull();           // nunca vendido
        assertThat(r.mayores().get(1).diasSinVender()).isEqualTo(75L);     // del 1-sep al 15-nov
    }

    @Test
    @DisplayName("capital parado: solo se muestran los 10 que más capital tienen, pero el total cuenta todos")
    void soloLosMayores() {
        List<ArticuloParado> muchos = new ArrayList<>();
        for (int i = 1; i <= 15; i++) muchos.add(new ArticuloParado(i, "A" + i, 1, i * 10.0, null));
        when(puerto.articulosSinMovimiento(any())).thenReturn(muchos);
        CapitalParado r = servicio.capitalParado(60);
        assertThat(r.mayores()).hasSize(10);
        assertThat(r.mayores().get(0).articuloId()).isEqualTo(15);
        assertThat(r.articulos()).isEqualTo(15);
        assertThat(r.capital()).isEqualTo(1200.0);
    }

    @Test
    @DisplayName("pedidos por estado: siempre vienen todas las categorías (con 0), suma los abiertos y manda los atrasados")
    void pedidosPorEstado() {
        when(puerto.pedidosPorCategoria(any())).thenReturn(Map.of(CategoriaPedido.PENDIENTE, 4, CategoriaPedido.APARTADO, 2,
                CategoriaPedido.ENTREGADO, 30));
        when(puerto.pedidosAtrasados(LocalDate.of(2026, 11, 15))).thenReturn(3);

        EstadoDePedidos r = servicio.pedidosPorEstado(90);

        verify(puerto).pedidosPorCategoria(LocalDate.of(2026, 8, 17));
        assertThat(r.estados()).extracting(e -> e.codigo()).containsExactly(
                "PENDIENTE", "APARTADO", "IR_PAGANDO", "PAGADO_POR_ENTREGAR", "ENTREGADO", "CANCELADO");
        assertThat(r.estados().get(2).pedidos()).isZero();                  // IR_PAGANDO no vino: cero
        assertThat(r.activos()).isEqualTo(6);                               // pendiente + apartado (el entregado ya no está abierto)
        assertThat(r.atrasados()).isEqualTo(3);
    }

    @Test
    @DisplayName("días fuera de 7–365: se rechaza y no se consulta nada")
    void diasInvalidos() {
        assertThatThrownBy(() -> servicio.capitalParado(6)).isInstanceOf(GraficasVentasException.class);
        assertThatThrownBy(() -> servicio.pedidosPorEstado(366)).isInstanceOf(GraficasVentasException.class);
        verify(puerto, never()).articulosSinMovimiento(any());
        verify(puerto, never()).pedidosPorCategoria(any());
    }
}
