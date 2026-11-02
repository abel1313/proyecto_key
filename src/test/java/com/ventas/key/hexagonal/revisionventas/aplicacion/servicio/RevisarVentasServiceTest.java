package com.ventas.key.hexagonal.revisionventas.aplicacion.servicio;

import com.ventas.key.hexagonal.revisionventas.dominio.excepcion.RevisionVentasException;
import com.ventas.key.hexagonal.revisionventas.dominio.modelo.BusquedaDeHallazgos;
import com.ventas.key.hexagonal.revisionventas.dominio.modelo.Hallazgo;
import com.ventas.key.hexagonal.revisionventas.dominio.modelo.Gravedad;
import com.ventas.key.hexagonal.revisionventas.dominio.modelo.HallazgosDeUnTipo;
import com.ventas.key.hexagonal.revisionventas.dominio.modelo.ResultadoRevision;
import com.ventas.key.hexagonal.revisionventas.dominio.modelo.TipoHallazgo;
import com.ventas.key.hexagonal.revisionventas.dominio.puerto.salida.HallazgosPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RevisarVentasServiceTest {

    private HallazgosPort puerto;
    private RevisarVentasService servicio;

    @BeforeEach
    void preparar() {
        puerto = mock(HallazgosPort.class);
        when(puerto.buscar(any(), any(), any(), anyInt())).thenReturn(new BusquedaDeHallazgos(0, List.of()));
        servicio = new RevisarVentasService(puerto, Clock.fixed(Instant.parse("2026-11-01T18:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    @DisplayName("sin fechas: revisa los últimos 90 días hasta hoy")
    void fechasPorDefecto() {
        ResultadoRevision r = servicio.revisar(null, null);
        assertThat(r.hasta()).isEqualTo(LocalDate.of(2026, 11, 1));
        assertThat(r.desde()).isEqualTo(LocalDate.of(2026, 11, 1).minusDays(90));
    }

    @Test
    @DisplayName("revisa TODOS los tipos y pide como máximo 50 ejemplos de cada uno")
    void recorreTodosLosTipos() {
        ResultadoRevision r = servicio.revisar(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31));
        assertThat(r.revisados()).extracting(HallazgosDeUnTipo::tipo).containsExactly(TipoHallazgo.values());
        assertThat(r.todoEnOrden()).isTrue();
        verify(puerto).buscar(eq(TipoHallazgo.PAGADO_SIN_VENTA), eq(LocalDate.of(2026, 10, 1)), eq(LocalDate.of(2026, 10, 31)), eq(50));
    }

    @Test
    @DisplayName("suma los hallazgos de todos los tipos")
    void sumaLosHallazgos() {
        when(puerto.buscar(eq(TipoHallazgo.STOCK_NEGATIVO), any(), any(), anyInt()))
                .thenReturn(new BusquedaDeHallazgos(3, List.of(new Hallazgo(TipoHallazgo.STOCK_NEGATIVO, null, null, 11, "x"))));
        when(puerto.buscar(eq(TipoHallazgo.PAGADO_SIN_VENTA), any(), any(), anyInt()))
                .thenReturn(new BusquedaDeHallazgos(2, List.of()));
        ResultadoRevision r = servicio.revisar(null, null);
        assertThat(r.totalHallazgos()).isEqualTo(5);
        assertThat(r.todoEnOrden()).isFalse();
    }

    @Test
    @DisplayName("un tipo que no se puede revisar en este ambiente se anota y no tumba a los demás")
    void tipoNoDisponible() {
        when(puerto.buscar(eq(TipoHallazgo.COBRO_TARJETA_SIN_APLICAR), any(), any(), anyInt()))
                .thenThrow(new RevisionVentasException.RevisionNoDisponible("falta la tabla"));
        ResultadoRevision r = servicio.revisar(null, null);
        assertThat(r.noRevisados()).containsExactly(TipoHallazgo.COBRO_TARJETA_SIN_APLICAR);
        assertThat(r.revisados()).hasSize(TipoHallazgo.values().length - 1);
    }

    @Test
    @DisplayName("fechas al revés: se rechaza con un mensaje claro y no se consulta nada")
    void fechasAlReves() {
        assertThatThrownBy(() -> servicio.revisar(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 10, 1)))
                .isInstanceOf(RevisionVentasException.FechasInvalidas.class)
                .hasMessageContaining("desde");
        verify(puerto, never()).buscar(any(), any(), any(), anyInt());
    }

    @Test
    @DisplayName("las graves van antes que las de gravedad media")
    void ordenPorGravedad() {
        ArgumentCaptor<TipoHallazgo> pedidos = ArgumentCaptor.forClass(TipoHallazgo.class);
        servicio.revisar(null, null);
        verify(puerto, org.mockito.Mockito.times(TipoHallazgo.values().length)).buscar(pedidos.capture(), any(), any(), anyInt());
        List<TipoHallazgo> orden = pedidos.getAllValues();
        assertThat(orden.get(orden.size() - 1).gravedad()).isEqualTo(Gravedad.MEDIA);
        assertThat(orden.subList(0, 7)).allMatch(t -> t.gravedad() == Gravedad.ALTA);
    }
}
