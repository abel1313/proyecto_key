package com.ventas.key.mis.productos.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ventas.key.mis.productos.entity.Pedido;
import com.ventas.key.mis.productos.errores.ErrorGenerico;
import com.ventas.key.mis.productos.models.abonos.AbonoRequest;
import com.ventas.key.mis.productos.models.pedidos.CambiarTipoPedidoRequest;
import com.ventas.key.mis.productos.models.pedidos.PedidoDetalleResponse;
import com.ventas.key.mis.productos.repository.IAccesorioRamoRepository;
import com.ventas.key.mis.productos.repository.IClienteRepository;
import com.ventas.key.mis.productos.repository.IColorFlorRepository;
import com.ventas.key.mis.productos.repository.IDetallePagoRepository;
import com.ventas.key.mis.productos.repository.IDetallePedidoRepository;
import com.ventas.key.mis.productos.repository.ILugarEntregaRepository;
import com.ventas.key.mis.productos.repository.IPagosYMesesRepository;
import com.ventas.key.mis.productos.repository.IPedidoRepository;
import com.ventas.key.mis.productos.repository.IProductosRepository;
import com.ventas.key.mis.productos.repository.IPromocionRepository;
import com.ventas.key.mis.productos.repository.IRamoPedidoDetalleRepository;
import com.ventas.key.mis.productos.repository.IUsuarioRepository;
import com.ventas.key.mis.productos.repository.IVarianteRepository;
import com.ventas.key.mis.productos.repository.IVentaRepository;
import com.ventas.key.mis.productos.entity.Venta;
import com.ventas.key.mis.productos.service.api.IAbonoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Cambiar la forma de cobro de un pedido ya creado (2026-09-22).
 *
 * <p>Nace de un caso real: se aparto un pedido, al ir a entregarlo el cliente decidio pagarlo
 * completo, y como no habia forma de cambiarlo quedo registrado como apartado. La otra salida era
 * cancelar y rehacer el pedido, que devuelve y vuelve a descontar el stock.
 *
 * <p>Lo que se prueba aqui son las reglas de negocio del cambio, no el cobro: el abono se delega a
 * {@link IAbonoService} y eso ya tiene sus propias pruebas. Lo que si se verifica es que el cobro
 * salga ANTES del cambio de tipo -- {@code registrarAbono} exige un pedido de credito, asi que si
 * el orden se invirtiera, pasar un apartado a contado cobrando reventaria en produccion.
 */
class CambiarTipoPedidoTest {

    private IPedidoRepository pedidoRepo;
    private IAbonoService abonoService;
    private IVentaRepository ventaRepo;
    private PedidoServiceImpl service;

    private static final int PEDIDO_ID = 501;

    @BeforeEach
    void setUp() {
        pedidoRepo = mock(IPedidoRepository.class);
        abonoService = mock(IAbonoService.class);

        PedidoServiceImpl real = new PedidoServiceImpl(
                pedidoRepo,
                mock(ErrorGenerico.class),
                mock(IClienteRepository.class),
                mock(IProductosRepository.class),
                mock(VentaServiceImpl.class),
                mock(IUsuarioRepository.class),
                new ObjectMapper(),
                mock(IDetallePagoRepository.class),
                mock(IDetallePedidoRepository.class),
                mock(IPagosYMesesRepository.class),
                mock(IVarianteRepository.class),
                mock(IPromocionRepository.class),
                mock(PromocionServiceImpl.class),
                mock(ILugarEntregaRepository.class),
                mock(IRamoPedidoDetalleRepository.class),
                mock(IAccesorioRamoRepository.class),
                mock(IColorFlorRepository.class));

        ReflectionTestUtils.setField(real, "iAbonoService", abonoService);
        ReflectionTestUtils.setField(real, "cacheService", mock(CacheService.class));
        ventaRepo = mock(IVentaRepository.class);
        ReflectionTestUtils.setField(real, "iVentaRepository", ventaRepo);

        // getDetallePedido arma el response leyendo media docena de repositorios mas; lo que se
        // prueba aqui es el cambio, no como se pinta el detalle.
        service = spy(real);
        doReturn(new PedidoDetalleResponse()).when(service).getDetallePedido(anyInt());
    }

    private Pedido pedido(String tipo, double total, double pagado) {
        Pedido p = new Pedido();
        p.setId(PEDIDO_ID);
        p.setTipoPedido(tipo);
        p.setTotalPedido(total);
        p.setTotalPagado(pagado);
        p.setEstadoPedido("Pendiente");
        when(pedidoRepo.findById(PEDIDO_ID)).thenReturn(Optional.of(p));
        return p;
    }

    private static CambiarTipoPedidoRequest cambioA(String tipo) {
        CambiarTipoPedidoRequest r = new CambiarTipoPedidoRequest();
        r.setTipoPedido(tipo);
        r.setUsuarioId(7);
        return r;
    }

    private static CambiarTipoPedidoRequest cobrando(String tipo, double monto, String nota) {
        CambiarTipoPedidoRequest r = cambioA(tipo);
        r.setMonto(monto);
        r.setNota(nota);
        return r;
    }

    @Test
    @DisplayName("un apartado pasa a fiado sin cobrar nada")
    void apartadoAFiadoSinCobro() {
        Pedido p = pedido("APARTADO", 1000, 300);

        service.cambiarTipoPedido(PEDIDO_ID, cambioA("FIADO"));

        assertThat(p.getTipoPedido()).isEqualTo("FIADO");
        verify(abonoService, never()).registrarAbono(anyInt(), any());
    }

    @Test
    @DisplayName("el tipo se acepta en minusculas")
    void aceptaMinusculas() {
        Pedido p = pedido("APARTADO", 1000, 300);

        service.cambiarTipoPedido(PEDIDO_ID, cambioA("fiado"));

        assertThat(p.getTipoPedido()).isEqualTo("FIADO");
    }

    @Test
    @DisplayName("un tipo que no existe se rechaza y dice cuales valen")
    void tipoInventadoSeRechaza() {
        pedido("APARTADO", 1000, 300);

        assertThatThrownBy(() -> service.cambiarTipoPedido(PEDIDO_ID, cambioA("CREDITO")))
                .hasMessageContaining("invalido")
                .hasMessageContaining("NORMAL")
                .hasMessageContaining("APARTADO")
                .hasMessageContaining("FIADO");
    }

    @Test
    @DisplayName("cambiar al mismo tipo que ya tiene se rechaza")
    void mismoTipoSeRechaza() {
        pedido("APARTADO", 1000, 300);

        assertThatThrownBy(() -> service.cambiarTipoPedido(PEDIDO_ID, cambioA("APARTADO")))
                .hasMessageContaining("ya es de tipo APARTADO");
    }

    private Pedido contadoEntregado(double total) {
        Pedido p = pedido("NORMAL", total, total);
        p.setEstadoPedido("Entregado");
        return p;
    }

    @Test
    @DisplayName("un pedido de credito ya entregado no cambia de forma de cobro")
    void creditoEntregadoNoCambia() {
        Pedido p = pedido("APARTADO", 1000, 1000);
        p.setEstadoPedido("Entregado");

        assertThatThrownBy(() -> service.cambiarTipoPedido(PEDIDO_ID, cambioA("NORMAL")))
                .hasMessageContaining("ya se entrego");
    }

    @Test
    @DisplayName("contado entregado solo puede pasar a apartado o ir pagando")
    void contadoEntregadoSoloACredito() {
        contadoEntregado(1000);

        assertThatThrownBy(() -> service.cambiarTipoPedido(PEDIDO_ID, cambioA("NORMAL")))
                .hasMessageContaining("solo se puede pasar a Apartado o Ir pagando");
        verify(ventaRepo, never()).delete(any());
    }

    @Test
    @DisplayName("el caso real: promocion cobrada como efectivo que en realidad es ir pagando")
    void contadoEntregadoPasaAFiado() {
        Pedido p = contadoEntregado(1000);
        Venta venta = new Venta();
        venta.setTotalVenta(1000.0);
        when(ventaRepo.findByPedidoId(PEDIDO_ID)).thenReturn(Optional.of(venta));

        service.cambiarTipoPedido(PEDIDO_ID, cambioA("FIADO"));

        // Borrada: al liquidar con abonos se crea la venta real; si quedara, el ingreso se duplica.
        verify(ventaRepo).delete(venta);
        assertThat(p.getTipoPedido()).isEqualTo("FIADO");
        assertThat(p.getEstadoPedido()).isEqualTo("FIADO");
        assertThat(p.getTotalPagado()).isZero();
        assertThat(p.getObservaciones()).contains("Se cobro como contado y se paso a Ir pagando");
        verify(abonoService, never()).registrarAbono(anyInt(), any());
    }

    @Test
    @DisplayName("contado entregado a apartado con enganche: el enganche entra como primer abono")
    void contadoEntregadoAApartadoConEnganche() {
        Pedido p = contadoEntregado(1000);
        p.setObservaciones("Venta de mostrador");
        when(abonoService.registrarAbono(eq(PEDIDO_ID), any())).thenAnswer(inv -> {
            assertThat(p.getTipoPedido()).isEqualTo("APARTADO");
            assertThat(p.getTotalPagado()).isZero();
            return null;
        });

        service.cambiarTipoPedido(PEDIDO_ID, cobrando("APARTADO", 200, "dio 200 de enganche"));

        ArgumentCaptor<AbonoRequest> abono = ArgumentCaptor.forClass(AbonoRequest.class);
        verify(abonoService).registrarAbono(eq(PEDIDO_ID), abono.capture());
        assertThat(abono.getValue().getMonto()).isEqualTo(200.0);
        assertThat(abono.getValue().getNota()).isEqualTo("Cambio de NORMAL a APARTADO: dio 200 de enganche");
        assertThat(p.getObservaciones()).startsWith("Venta de mostrador\n").contains("Apartado: dio 200 de enganche");
    }

    @Test
    @DisplayName("un pedido cancelado no cambia de forma de cobro")
    void canceladoNoCambia() {
        Pedido p = pedido("APARTADO", 1000, 300);
        p.setEstadoPedido("cancelado");

        assertThatThrownBy(() -> service.cambiarTipoPedido(PEDIDO_ID, cambioA("NORMAL")))
                .hasMessageContaining("cancelado");
    }

    @Test
    @DisplayName("pasar a contado con saldo pendiente y sin cobrar se rechaza")
    void aContadoConSaldoSinCobrarSeRechaza() {
        pedido("APARTADO", 1000, 300);

        // El caso que hay que impedir: un NORMAL con saldo pendiente es exactamente lo que NORMAL
        // dice que no existe -- el pedido desapareceria de la lista de lo que falta cobrar.
        assertThatThrownBy(() -> service.cambiarTipoPedido(PEDIDO_ID, cambioA("NORMAL")))
                .hasMessageContaining("saldo completo")
                .hasMessageContaining("700");
    }

    @Test
    @DisplayName("pasar a contado cobrando menos del saldo se rechaza")
    void aContadoCobrandoDeMenosSeRechaza() {
        pedido("APARTADO", 1000, 300);

        assertThatThrownBy(() -> service.cambiarTipoPedido(PEDIDO_ID, cobrando("NORMAL", 400, null)))
                .hasMessageContaining("saldo completo");
        verify(abonoService, never()).registrarAbono(anyInt(), any());
    }

    @Test
    @DisplayName("pasar a contado ya liquidado no necesita cobro")
    void aContadoYaLiquidado() {
        Pedido p = pedido("APARTADO", 1000, 1000);

        assertThatCode(() -> service.cambiarTipoPedido(PEDIDO_ID, cambioA("NORMAL")))
                .doesNotThrowAnyException();
        assertThat(p.getTipoPedido()).isEqualTo("NORMAL");
        verify(abonoService, never()).registrarAbono(anyInt(), any());
    }

    @Test
    @DisplayName("el caso real: apartado que el cliente termina pagando completo")
    void apartadoQueSeLiquidaEnLaEntrega() {
        Pedido p = pedido("APARTADO", 1000, 300);

        service.cambiarTipoPedido(PEDIDO_ID, cobrando("NORMAL", 700, "Pago el resto al entregarlo"));

        verify(abonoService).registrarAbono(eq(PEDIDO_ID), any(AbonoRequest.class));
        assertThat(p.getTipoPedido()).isEqualTo("NORMAL");
    }

    @Test
    @DisplayName("el cobro sale ANTES de cambiar el tipo")
    void cobraAntesDeCambiarElTipo() {
        Pedido p = pedido("APARTADO", 1000, 300);

        // registrarAbono exige un pedido de credito. Si el tipo se cambiara primero, el pedido ya
        // seria NORMAL al momento del cobro y el abono reventaria.
        when(abonoService.registrarAbono(eq(PEDIDO_ID), any())).thenAnswer(inv -> {
            assertThat(p.getTipoPedido()).isEqualTo("APARTADO");
            return null;
        });

        service.cambiarTipoPedido(PEDIDO_ID, cobrando("NORMAL", 700, null));

        assertThat(p.getTipoPedido()).isEqualTo("NORMAL");
    }

    @Test
    @DisplayName("la nota del abono deja escrito de que tipo a cual cambio")
    void laNotaDiceElCambio() {
        pedido("APARTADO", 1000, 300);

        service.cambiarTipoPedido(PEDIDO_ID, cobrando("NORMAL", 700, "Pago el resto al entregarlo"));

        ArgumentCaptor<AbonoRequest> abono = ArgumentCaptor.forClass(AbonoRequest.class);
        verify(abonoService).registrarAbono(eq(PEDIDO_ID), abono.capture());
        assertThat(abono.getValue().getNota())
                .isEqualTo("Cambio de APARTADO a NORMAL: Pago el resto al entregarlo");
    }

    @Test
    @DisplayName("sin nota del usuario queda al menos el cambio registrado")
    void sinNotaQuedaElCambio() {
        pedido("APARTADO", 1000, 300);

        service.cambiarTipoPedido(PEDIDO_ID, cobrando("NORMAL", 700, "   "));

        ArgumentCaptor<AbonoRequest> abono = ArgumentCaptor.forClass(AbonoRequest.class);
        verify(abonoService).registrarAbono(eq(PEDIDO_ID), abono.capture());
        assertThat(abono.getValue().getNota()).isEqualTo("Cambio de APARTADO a NORMAL");
    }

    @Test
    @DisplayName("el abono se arma con lo que mando quien hizo el cambio")
    void elAbonoLlevaLosDatosDelCobro() {
        pedido("FIADO", 1000, 0);
        CambiarTipoPedidoRequest req = cobrando("APARTADO", 400, "Abono en mostrador");
        req.setMetodoPago("TRANSFERENCIA");
        req.setMontoDado(400.0);

        service.cambiarTipoPedido(PEDIDO_ID, req);

        ArgumentCaptor<AbonoRequest> abono = ArgumentCaptor.forClass(AbonoRequest.class);
        verify(abonoService).registrarAbono(eq(PEDIDO_ID), abono.capture());
        assertThat(abono.getValue().getMonto()).isEqualTo(400.0);
        assertThat(abono.getValue().getMetodoPago()).isEqualTo("TRANSFERENCIA");
        assertThat(abono.getValue().getMontoDado()).isEqualTo(400.0);
        assertThat(abono.getValue().getUsuarioId()).isEqualTo(7);
    }

    @Test
    @DisplayName("un pedido de contado no tiene saldo que cobrar en el cambio")
    void contadoNoTieneSaldoQueCobrar() {
        pedido("NORMAL", 1000, 1000);

        assertThatThrownBy(() -> service.cambiarTipoPedido(PEDIDO_ID, cobrando("APARTADO", 200, null)))
                .hasMessageContaining("no tiene saldo que cobrar");
        verify(abonoService, never()).registrarAbono(anyInt(), any());
    }

    @Test
    @DisplayName("un pedido que no existe no se puede cambiar")
    void pedidoInexistente() {
        when(pedidoRepo.findById(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.cambiarTipoPedido(999, cambioA("NORMAL")))
                .hasMessageContaining("no encontrado");
    }
}
