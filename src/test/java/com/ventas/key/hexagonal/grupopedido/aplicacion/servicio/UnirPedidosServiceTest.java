package com.ventas.key.hexagonal.grupopedido.aplicacion.servicio;

import com.ventas.key.hexagonal.grupopedido.dominio.excepcion.PedidosDeDistintoTipoException;
import com.ventas.key.hexagonal.grupopedido.dominio.modelo.AbonoAlGrupo;
import com.ventas.key.hexagonal.grupopedido.dominio.modelo.AbonoRegistrado;
import com.ventas.key.hexagonal.grupopedido.dominio.modelo.Movimiento;
import com.ventas.key.hexagonal.grupopedido.dominio.modelo.GrupoPedidos;
import com.ventas.key.hexagonal.grupopedido.dominio.modelo.PedidoDelGrupo;
import com.ventas.key.hexagonal.grupopedido.dominio.modelo.RegistroGrupo;
import com.ventas.key.hexagonal.grupopedido.dominio.puerto.entrada.UnirPedidosCasoUso;
import com.ventas.key.hexagonal.grupopedido.dominio.puerto.salida.AbonoPedidoPort;
import com.ventas.key.hexagonal.grupopedido.dominio.puerto.salida.AbonosDelGrupoPort;
import com.ventas.key.hexagonal.grupopedido.dominio.puerto.salida.EstadoDePagoPort;
import com.ventas.key.hexagonal.grupopedido.dominio.puerto.salida.BitacoraPedidoPort;
import com.ventas.key.hexagonal.grupopedido.dominio.puerto.salida.ConfirmarPedidoPort;
import com.ventas.key.hexagonal.grupopedido.dominio.puerto.salida.GrupoPedidosPort;
import com.ventas.key.hexagonal.grupopedido.dominio.puerto.salida.PedidosDelGrupoPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UnirPedidosServiceTest {

    private PedidosDelGrupoPort pedidos;
    private GrupoPedidosPort grupos;
    private AbonoPedidoPort abonos;
    private BitacoraPedidoPort bitacora;
    private ConfirmarPedidoPort confirmar;
    private AbonosDelGrupoPort abonosDelGrupo;
    private EstadoDePagoPort estadoDePago;
    private UnirPedidosService service;

    private static final Integer USUARIO = 7;

    @BeforeEach
    void setUp() {
        pedidos = mock(PedidosDelGrupoPort.class);
        grupos = mock(GrupoPedidosPort.class);
        abonos = mock(AbonoPedidoPort.class);
        bitacora = mock(BitacoraPedidoPort.class);
        confirmar = mock(ConfirmarPedidoPort.class);
        abonosDelGrupo = mock(AbonosDelGrupoPort.class);
        estadoDePago = mock(EstadoDePagoPort.class);
        service = new UnirPedidosService(pedidos, grupos, abonos, bitacora, confirmar, abonosDelGrupo, estadoDePago);
    }

    private static PedidoDelGrupo fiado(int id, long total, long pagado, int dia) {
        return new PedidoDelGrupo(id, "FIADO", "FIADO", total, pagado, LocalDateTime.of(2026, 9, dia, 9, 0), "C" + id);
    }

    private void grupoGuardado(int grupoId, boolean activo, PedidoDelGrupo... ps) {
        List<Integer> ids = java.util.Arrays.stream(ps).map(PedidoDelGrupo::pedidoId).toList();
        when(grupos.buscar(grupoId)).thenReturn(Optional.of(
                new RegistroGrupo(grupoId, ids.get(0), activo, LocalDateTime.now(), null, ids)));
        when(pedidos.buscar(ids)).thenReturn(List.of(ps));
    }

    @Test
    @DisplayName("unir guarda el grupo y deja nota en cada pedido")
    void unir() {
        List<PedidoDelGrupo> ps = List.of(fiado(1, 10_000, 0, 1), fiado(2, 20_000, 0, 2));
        when(pedidos.buscar(List.of(1, 2))).thenReturn(ps);
        when(grupos.gruposActivosDe(List.of(1, 2))).thenReturn(Map.of());
        when(grupos.crear(2, List.of(1, 2), "recoge la hermana", USUARIO)).thenReturn(30);
        grupoGuardado(30, true, ps.toArray(PedidoDelGrupo[]::new));

        GrupoPedidos g = service.unir(List.of(1, 2, 2), 2, "recoge la hermana", USUARIO);

        assertThat(g.grupoId()).isEqualTo(30);
        assertThat(g.saldoCentavos()).isEqualTo(30_000);
        verify(bitacora).anotar(eq(1), contains("grupo #30"));
        verify(bitacora).anotar(eq(2), contains("recoge la hermana"));
    }

    @Test
    @DisplayName("distinta forma de cobro: no se guarda nada")
    void unirDistintoTipo() {
        when(pedidos.buscar(List.of(1, 2))).thenReturn(List.of(fiado(1, 10_000, 0, 1),
                new PedidoDelGrupo(2, "APARTADO", "APARTADO", 5_000, 0, LocalDateTime.now(), "C2")));
        when(grupos.gruposActivosDe(anyList())).thenReturn(Map.of());

        assertThatThrownBy(() -> service.unir(List.of(1, 2), 1, null, USUARIO))
                .isInstanceOf(PedidosDeDistintoTipoException.class);
        verify(grupos, never()).crear(any(), anyList(), any(), any());
        verify(bitacora, never()).anotar(anyInt(), anyString());
    }

    @Test
    @DisplayName("abonar reparte del mas viejo al mas nuevo con la nota del grupo")
    void abonar() {
        grupoGuardado(30, true, fiado(2, 20_000, 0, 5), fiado(1, 10_000, 0, 1));

        UnirPedidosCasoUso.ResultadoAbono r = service.abonar(30, new AbonoAlGrupo(15_000, "EFECTIVO", 20_000L, "semana 1"), USUARIO);

        InOrder orden = inOrder(abonos);
        orden.verify(abonos).abonar(1, 10_000, "EFECTIVO", "Abono del grupo #30: semana 1", USUARIO);
        orden.verify(abonos).abonar(2, 5_000, "EFECTIVO", "Abono del grupo #30: semana 1", USUARIO);
        assertThat(r.cambioCentavos()).isEqualTo(5_000);
        assertThat(r.repartos()).hasSize(2);
    }

    @Test
    @DisplayName("si el efectivo no alcanza no se registra ningun abono")
    void abonarSinCambio() {
        grupoGuardado(30, true, fiado(1, 10_000, 0, 1), fiado(2, 10_000, 0, 2));

        assertThatThrownBy(() -> service.abonar(30, new AbonoAlGrupo(15_000, "EFECTIVO", 10_000L, null), USUARIO))
                .hasMessageContaining("menor");
        verify(abonos, never()).abonar(anyInt(), anyLong(), anyString(), anyString(), any());
    }

    @Test
    @DisplayName("deshacer sin dinero de por medio marca el grupo y no mueve nada")
    void deshacer() {
        grupoGuardado(30, true, fiado(1, 10_000, 0, 1), fiado(2, 10_000, 0, 2));

        service.deshacer(30, "cada quien paga lo suyo", USUARIO);

        verify(grupos).marcarDeshecho(30, USUARIO);
        verify(bitacora, times(2)).anotar(anyInt(), contains("Se deshizo el grupo #30"));
        verify(abonos, never()).abonar(anyInt(), anyLong(), anyString(), anyString(), any());
    }

    @Test
    @DisplayName("no se deshace dos veces")
    void deshacerDosVeces() {
        grupoGuardado(30, false, fiado(1, 10_000, 0, 1), fiado(2, 10_000, 0, 2));

        assertThatThrownBy(() -> service.deshacer(30, null, USUARIO)).hasMessageContaining("ya se habia deshecho");
        verify(grupos, never()).marcarDeshecho(any(), any());
    }

    private static PedidoDelGrupo contado(int id, String estado, int dia) {
        return new PedidoDelGrupo(id, "NORMAL", estado, 10_000, 0, LocalDateTime.of(2026, 9, dia, 9, 0), "C" + id);
    }

    @Test
    @DisplayName("cobrar de contado confirma cada pedido pendiente con la misma forma de pago y lo anota")
    void cobrarDeContado() {
        grupoGuardado(30, true, contado(1, "Entregado", 1), contado(2, "Pendiente", 2), contado(3, "Pendiente", 3));

        UnirPedidosCasoUso.ResultadoCobro r = service.cobrarDeContado(30, 5, USUARIO);

        assertThat(r.pedidosCobrados()).containsExactly(2, 3);
        InOrder orden = inOrder(confirmar);
        orden.verify(confirmar).confirmarDeContado(2, 5);
        orden.verify(confirmar).confirmarDeContado(3, 5);
        verify(confirmar, never()).confirmarDeContado(eq(1), any());
        verify(bitacora, times(2)).anotar(anyInt(), contains("Cobrado de contado junto con el grupo #30"));
    }

    @Test
    @DisplayName("sin forma de pago no se confirma nada")
    void cobrarDeContadoSinFormaDePago() {
        grupoGuardado(30, true, contado(1, "Pendiente", 1), contado(2, "Pendiente", 2));

        assertThatThrownBy(() -> service.cobrarDeContado(30, null, USUARIO)).hasMessageContaining("forma de pago");
        verify(confirmar, never()).confirmarDeContado(any(), any());
    }

    @Test
    @DisplayName("un grupo a credito no se cobra de contado")
    void cobrarDeContadoCredito() {
        grupoGuardado(30, true, fiado(1, 10_000, 0, 1), fiado(2, 10_000, 0, 2));

        assertThatThrownBy(() -> service.cobrarDeContado(30, 5, USUARIO)).hasMessageContaining("a credito");
        verify(confirmar, never()).confirmarDeContado(any(), any());
    }

    private static AbonoRegistrado abono(int id, int pedidoId, long centavos, int dia) {
        return new AbonoRegistrado(id, pedidoId, centavos, java.time.LocalDate.of(2026, 9, dia));
    }

    @Test
    @DisplayName("con dinero dado, deshacer sin repartir no se deja: hay que separar diciendo cuanto se queda cada uno")
    void deshacerConDinero() {
        grupoGuardado(30, true, fiado(1, 10_000, 4_000, 1), fiado(2, 10_000, 0, 2));

        assertThatThrownBy(() -> service.deshacer(30, null, USUARIO)).hasMessageContaining("cuanto se queda cada pedido");
        verify(grupos, never()).marcarDeshecho(any(), any());
    }

    @Test
    @DisplayName("separar todos: los $100 que estaban en el 1 se van completos al 3 y se ajusta el estado de cada uno")
    void separarTodosMueveElDinero() {
        grupoGuardado(30, true, fiado(1, 10_000, 10_000, 1), fiado(2, 10_000, 0, 2), fiado(3, 10_000, 0, 3));
        when(abonosDelGrupo.abonosDe(List.of(1, 2, 3))).thenReturn(List.of(abono(50, 1, 10_000, 5)));

        UnirPedidosCasoUso.ResultadoSeparacion r = service.separar(30, List.of(1, 2, 3),
                Map.of(1, 0L, 2, 0L, 3, 10_000L), null, "cada quien lo suyo", USUARIO);

        assertThat(r.grupoNuevo()).isNull();
        verify(abonosDelGrupo).mover(new Movimiento(50, 1, 3, 10_000), "Reparto al separar el grupo #30");
        verify(estadoDePago).ajustarAlosAbonos(1, USUARIO);
        verify(estadoDePago).ajustarAlosAbonos(3, USUARIO);
        verify(grupos).marcarDeshecho(30, USUARIO);
        verify(grupos, never()).crear(any(), anyList(), any(), any());
    }

    @Test
    @DisplayName("separar solo uno: se lleva lo que se diga y los otros siguen unidos con el titular elegido")
    void separarUno() {
        grupoGuardado(30, true, fiado(1, 10_000, 10_000, 1), fiado(2, 10_000, 0, 2), fiado(3, 10_000, 0, 3));
        when(abonosDelGrupo.abonosDe(List.of(1, 2, 3))).thenReturn(List.of(abono(50, 1, 10_000, 5)));
        when(grupos.crear(eq(3), eq(List.of(2, 3)), anyString(), eq(USUARIO))).thenReturn(31);

        UnirPedidosCasoUso.ResultadoSeparacion r = service.separar(30, List.of(1), Map.of(1, 4_000L), 3, null, USUARIO);

        assertThat(r.grupoNuevo()).isEqualTo(31);
        // Los $60 que no se llevo el 1 se quedan en el grupo: llenan primero al 2, el mas viejo.
        verify(abonosDelGrupo).mover(new Movimiento(50, 1, 2, 6_000), "Reparto al separar el grupo #30");
        verify(grupos).crear(eq(3), eq(List.of(2, 3)), contains("Sigue del grupo #30"), eq(USUARIO));
    }

    @Test
    @DisplayName("si el reparto no da exacto lo abonado no se separa nada")
    void separarRepartoIncompleto() {
        grupoGuardado(30, true, fiado(1, 10_000, 10_000, 1), fiado(2, 10_000, 0, 2));
        when(abonosDelGrupo.abonosDe(List.of(1, 2))).thenReturn(List.of(abono(50, 1, 10_000, 5)));

        assertThatThrownBy(() -> service.separar(30, List.of(1, 2), Map.of(1, 3_000L, 2, 3_000L), null, null, USUARIO))
                .hasMessageContaining("tiene que ser exacto");
        verify(abonosDelGrupo, never()).mover(any(), any());
        verify(grupos, never()).marcarDeshecho(any(), any());
    }

    @Test
    @DisplayName("cambiar quien recoge solo entre los pedidos del grupo")
    void cambiarTitular() {
        grupoGuardado(30, true, fiado(1, 10_000, 0, 1), fiado(2, 10_000, 0, 2));

        service.cambiarTitular(30, 2, USUARIO);
        verify(grupos).cambiarTitular(30, 2);
        assertThatThrownBy(() -> service.cambiarTitular(30, 9, USUARIO)).hasMessageContaining("pedidos del grupo");
    }
}
