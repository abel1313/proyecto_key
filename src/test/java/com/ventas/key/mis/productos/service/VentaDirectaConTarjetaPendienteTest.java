package com.ventas.key.mis.productos.service;

import com.ventas.key.mis.productos.entity.Cliente;
import com.ventas.key.mis.productos.entity.MesesIntereses;
import com.ventas.key.mis.productos.entity.PagosYMeses;
import com.ventas.key.mis.productos.entity.Pedido;
import com.ventas.key.mis.productos.entity.Producto;
import com.ventas.key.mis.productos.entity.TarifaTerminal;
import com.ventas.key.mis.productos.entity.TipoPago;
import com.ventas.key.mis.productos.entity.Usuario;
import com.ventas.key.mis.productos.entity.Venta;
import com.ventas.key.mis.productos.entity.productoVariantes.Variantes;
import com.ventas.key.mis.productos.models.DetalleVentaDto;
import com.ventas.key.mis.productos.models.VentaDirectaRequest;
import com.ventas.key.mis.productos.models.VentaDirectaResponse;
import com.ventas.key.mis.productos.repository.IClienteRepository;
import com.ventas.key.mis.productos.repository.IClienteSinRegistroRepository;
import com.ventas.key.mis.productos.repository.IDetallePagoRepository;
import com.ventas.key.mis.productos.repository.IPagosYMesesRepository;
import com.ventas.key.mis.productos.repository.IPedidoRepository;
import com.ventas.key.mis.productos.repository.IProductosRepository;
import com.ventas.key.mis.productos.repository.IPromocionRepository;
import com.ventas.key.mis.productos.repository.IUsuarioRepository;
import com.ventas.key.mis.productos.repository.IVarianteRepository;
import com.ventas.key.mis.productos.repository.IVentaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Venta directa con tarjeta y el modulo nuevo de cobro: con el modulo encendido la venta NO nace pagada,
 * queda un pedido Pendiente (sin Venta) que cobra /v1/pagos-tarjeta; apagado, todo igual que siempre.
 */
class VentaDirectaConTarjetaPendienteTest {

    private IVentaRepository ventas;
    private IPedidoRepository pedidos;
    private VentaServiceImpl servicio;
    private VentaDirectaRequest request;

    @BeforeEach
    void setUp() {
        ventas = mock(IVentaRepository.class);
        pedidos = mock(IPedidoRepository.class);
        IUsuarioRepository usuarios = mock(IUsuarioRepository.class);
        IClienteRepository clientes = mock(IClienteRepository.class);
        IPagosYMesesRepository formas = mock(IPagosYMesesRepository.class);
        IVarianteRepository variantes = mock(IVarianteRepository.class);
        servicio = new VentaServiceImpl(ventas, mock(IProductosRepository.class), usuarios, clientes, formas,
                mock(IDetallePagoRepository.class), variantes, mock(IClienteSinRegistroRepository.class), pedidos,
                mock(IPromocionRepository.class), mock(PromocionServiceImpl.class),
                mock(com.ventas.key.mis.productos.errores.ErrorGenerico.class));
        ReflectionTestUtils.setField(servicio, "cacheService", mock(com.ventas.key.mis.productos.service.CacheService.class));
        ReflectionTestUtils.setField(servicio, "rabbitTemplate", mock(RabbitTemplate.class));

        when(usuarios.findById(1)).thenReturn(Optional.of(new Usuario()));
        when(clientes.findById(5)).thenReturn(Optional.of(new Cliente()));
        when(pedidos.save(any(Pedido.class))).thenAnswer(inv -> {
            Pedido p = inv.getArgument(0);
            p.setId(77);
            return p;
        });
        when(ventas.save(any(Venta.class))).thenAnswer(inv -> {
            Venta v = inv.getArgument(0);
            v.setId(88);
            return v;
        });

        Producto prod = new Producto();
        prod.setId(10);
        prod.setNombre("Pantalón");
        prod.setHabilitado('1');
        prod.setStock(5);
        prod.setPrecioCosto(100.0);
        prod.setPrecioVenta(400.0);
        Variantes v = new Variantes();
        v.setId(20);
        v.setProducto(prod);
        v.setStock(5);
        when(variantes.findByIdWithLock(20)).thenReturn(Optional.of(v));

        TarifaTerminal tarifa = new TarifaTerminal();
        tarifa.setId(1);
        tarifa.setTarifa(3.5);
        MesesIntereses meses = new MesesIntereses();
        meses.setMeses("1");
        meses.setDescripcion("Contado");
        meses.setTarifaTerminal(tarifa);
        TipoPago tarjeta = new TipoPago();
        tarjeta.setFormaPago("Tarjeta Debito");
        PagosYMeses forma = new PagosYMeses();
        forma.setTipoPago(tarjeta);
        forma.setMesesIntereses(meses);
        when(formas.findById(2)).thenReturn(Optional.of(forma));

        TipoPago efectivo = new TipoPago();
        efectivo.setFormaPago("Efectivo");
        TarifaTerminal sinTerminal = new TarifaTerminal();
        sinTerminal.setId(3);
        sinTerminal.setTarifa(0.0);
        MesesIntereses mesesEfectivo = new MesesIntereses();
        mesesEfectivo.setMeses("1");
        mesesEfectivo.setDescripcion("Efectivo");
        mesesEfectivo.setTarifaTerminal(sinTerminal);
        PagosYMeses formaEfectivo = new PagosYMeses();
        formaEfectivo.setTipoPago(efectivo);
        formaEfectivo.setMesesIntereses(mesesEfectivo);
        when(formas.findById(1)).thenReturn(Optional.of(formaEfectivo));

        request = new VentaDirectaRequest();
        request.setUsuarioId(1);
        request.setClienteId(5);
        request.setDetalles(List.of(new DetalleVentaDto(20, 1, 400.0, 400.0, null)));
    }

    @Test
    @DisplayName("tarjeta con el modulo encendido: pedido Pendiente, sin dinero, sin entregar y sin Venta")
    void conModuloEncendidoQuedaPendiente() throws Exception {
        ReflectionTestUtils.setField(servicio, "pagoTarjetaHabilitado", true);
        request.setPagosYMesesId(2);

        VentaDirectaResponse resp = servicio.saveVentaDetalle(request);

        ArgumentCaptor<Pedido> guardado = ArgumentCaptor.forClass(Pedido.class);
        verify(pedidos).save(guardado.capture());
        assertThat(guardado.getValue().getEstadoPedido()).isEqualTo("Pendiente");
        assertThat(guardado.getValue().getTipoPedido()).isEqualTo("NORMAL");
        assertThat(guardado.getValue().getTotalPagado()).isZero();
        assertThat(guardado.getValue().getTotalPedido()).isEqualTo(400.0);
        verify(ventas, never()).save(any(Venta.class));
        assertThat(resp.getPedidoId()).isEqualTo(77);
        assertThat(resp.getVentaId()).isNull();
        assertThat(resp.getCobroConModuloNuevo()).isTrue();
        assertThat(resp.isRequiereTerminal()).isTrue();
    }

    @Test
    @DisplayName("tarjeta con el modulo apagado: igual que siempre (venta creada, requiere terminal)")
    void conModuloApagadoNoCambia() throws Exception {
        ReflectionTestUtils.setField(servicio, "pagoTarjetaHabilitado", false);
        request.setPagosYMesesId(2);

        VentaDirectaResponse resp = servicio.saveVentaDetalle(request);

        verify(ventas).save(any(Venta.class));
        assertThat(resp.getVentaId()).isEqualTo(88);
        assertThat(resp.getCobroConModuloNuevo()).isNull();
        assertThat(resp.isRequiereTerminal()).isTrue();
    }

    @Test
    @DisplayName("efectivo con el modulo encendido: sigue siendo venta pagada y entregada")
    void efectivoNoCambia() throws Exception {
        ReflectionTestUtils.setField(servicio, "pagoTarjetaHabilitado", true);
        request.setPagosYMesesId(1);

        VentaDirectaResponse resp = servicio.saveVentaDetalle(request);

        verify(ventas).save(any(Venta.class));
        assertThat(resp.getVentaId()).isEqualTo(88);
        assertThat(resp.getCobroConModuloNuevo()).isNull();
        assertThat(resp.isRequiereTerminal()).isFalse();
    }
}
