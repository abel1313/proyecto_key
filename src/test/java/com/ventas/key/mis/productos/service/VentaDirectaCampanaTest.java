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

import com.ventas.key.hexagonal.campana.dominio.modelo.DecisionDeCampana;
import com.ventas.key.hexagonal.campana.dominio.modelo.FormaDeCobro;
import com.ventas.key.hexagonal.campana.dominio.modelo.SolicitudDeCampana;
import com.ventas.key.hexagonal.campana.dominio.puerto.entrada.ParticiparEnCampanaCasoUso;
import com.ventas.key.hexagonal.preciocantidad.dominio.modelo.OrigenDePrecio;
import com.ventas.key.mis.productos.entity.DetallePedido;
import com.ventas.key.mis.productos.models.CampanaVentaRequest;
import java.math.BigDecimal;
import java.util.Map;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Venta directa con campana vigente: el back decide el precio de campana de los juguetes (la decision viene de la campana); la venta lo acepta,
 * lo recuerda como "precio de antes" con origen CAMPANA y liga el pedido a la persona. Sin campana, o si la campana falla, todo igual que siempre.
 */
class VentaDirectaCampanaTest {

    private IVentaRepository ventas;
    private IPedidoRepository pedidos;
    private VentaServiceImpl servicio;
    private VentaDirectaRequest request;
    private ParticiparEnCampanaCasoUso campana;
    private IVarianteRepository variantesRepo;

    @BeforeEach
    void setUp() {
        ventas = mock(IVentaRepository.class);
        pedidos = mock(IPedidoRepository.class);
        IUsuarioRepository usuarios = mock(IUsuarioRepository.class);
        IClienteRepository clientes = mock(IClienteRepository.class);
        IPagosYMesesRepository formas = mock(IPagosYMesesRepository.class);
        IVarianteRepository variantes = mock(IVarianteRepository.class);
        variantesRepo = variantes;
        servicio = new VentaServiceImpl(ventas, mock(IProductosRepository.class), usuarios, clientes, formas,
                mock(IDetallePagoRepository.class), variantes, mock(IClienteSinRegistroRepository.class), pedidos,
                mock(IPromocionRepository.class), mock(PromocionServiceImpl.class),
                mock(com.ventas.key.mis.productos.errores.ErrorGenerico.class));
        ReflectionTestUtils.setField(servicio, "cacheService", mock(com.ventas.key.mis.productos.service.CacheService.class));
        ReflectionTestUtils.setField(servicio, "rabbitTemplate", mock(RabbitTemplate.class));
        // Sin BD: el bloqueo devuelve la misma entidad.
        ReflectionTestUtils.setField(servicio, "bloqueoStock", mock(BloqueoStock.class, inv -> inv.getArgument(0)));

        campana = mock(ParticiparEnCampanaCasoUso.class);
        ReflectionTestUtils.setField(servicio, "campanas", campana);
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
        when(variantes.findById(20)).thenReturn(Optional.of(v));

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

    private static DecisionDeCampana decision(String precio) {
        return new DecisionDeCampana(1, "Reyes", Map.of(10, new BigDecimal(precio)), null, null, BigDecimal.ZERO, true);
    }

    private Pedido pedidoGuardado() {
        ArgumentCaptor<Pedido> guardado = ArgumentCaptor.forClass(Pedido.class);
        verify(pedidos).save(guardado.capture());
        return guardado.getValue();
    }

    @Test
    @DisplayName("contado con campana: el juguete se cobra a $300, recuerda $400 con origen CAMPANA y liga el pedido")
    void contadoConPrecioDeCampana() throws Exception {
        ReflectionTestUtils.setField(servicio, "pagoTarjetaHabilitado", false);
        request.setPagosYMesesId(1);
        request.setCampana(new CampanaVentaRequest());
        when(campana.decidir(any())).thenReturn(decision("300"));

        servicio.saveVentaDetalle(request);

        Pedido p = pedidoGuardado();
        assertThat(p.getTotalPedido()).isEqualTo(300.0);
        DetallePedido d = p.getDetalles().get(0);
        assertThat(d.getPrecioUnitario()).isEqualTo(300.0);
        assertThat(d.getPrecioAntesDeGrupo()).isEqualTo(400.0);
        assertThat(d.getOrigenDelDescuento()).isEqualTo(OrigenDePrecio.CAMPANA);
        verify(campana).registrarCompra(any(), org.mockito.ArgumentMatchers.eq(77), org.mockito.ArgumentMatchers.eq(FormaDeCobro.CONTADO), org.mockito.ArgumentMatchers.eq(true));
    }

    @Test
    @DisplayName("el front manda el precio de campana ya bajado: se acepta y el 'antes' sigue siendo el de catalogo")
    void frontManda300() throws Exception {
        ReflectionTestUtils.setField(servicio, "pagoTarjetaHabilitado", false);
        request.setPagosYMesesId(1);
        request.setDetalles(List.of(new DetalleVentaDto(20, 1, 300.0, 300.0, null)));
        when(campana.decidir(any())).thenReturn(decision("300"));

        servicio.saveVentaDetalle(request);

        DetallePedido d = pedidoGuardado().getDetalles().get(0);
        assertThat(d.getPrecioUnitario()).isEqualTo(300.0);
        assertThat(d.getPrecioAntesDeGrupo()).isEqualTo(400.0);
    }

    @Test
    @DisplayName("el front manda $300 pero el back NO decidio campana: se rechaza (el back es la autoridad)")
    void precioDeCampanaSinDecision() {
        ReflectionTestUtils.setField(servicio, "pagoTarjetaHabilitado", false);
        request.setPagosYMesesId(1);
        request.setDetalles(List.of(new DetalleVentaDto(20, 1, 300.0, 300.0, null)));
        when(campana.decidir(any())).thenReturn(DecisionDeCampana.NINGUNA);

        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class, () -> servicio.saveVentaDetalle(request));
    }

    @Test
    @DisplayName("sin campana vigente: precio normal, sin 'precio de antes'")
    void sinCampana() throws Exception {
        ReflectionTestUtils.setField(servicio, "pagoTarjetaHabilitado", false);
        request.setPagosYMesesId(1);
        when(campana.decidir(any())).thenReturn(DecisionDeCampana.NINGUNA);

        servicio.saveVentaDetalle(request);

        Pedido p = pedidoGuardado();
        assertThat(p.getTotalPedido()).isEqualTo(400.0);
        assertThat(p.getDetalles().get(0).getPrecioAntesDeGrupo()).isNull();
    }

    @Test
    @DisplayName("si la campana falla, la venta sigue y se cobra normal")
    void campanaFallaNoTiraLaVenta() throws Exception {
        ReflectionTestUtils.setField(servicio, "pagoTarjetaHabilitado", false);
        request.setPagosYMesesId(1);
        when(campana.decidir(any())).thenThrow(new IllegalStateException("base caida"));

        servicio.saveVentaDetalle(request);

        assertThat(pedidoGuardado().getTotalPedido()).isEqualTo(400.0);
    }

    @Test
    @DisplayName("Apartado e Ir pagando le piden a la campana su forma de cobro; el codigo y los checks viajan en la solicitud")
    void formaDeCobroViaja() throws Exception {
        request.setTipoPedido("APARTADO");
        CampanaVentaRequest c = new CampanaVentaRequest();
        c.setCodigo("abc-123");
        c.setRecurrenteManual(true);
        c.setAceptaPrivacidad(true);
        request.setCampana(c);
        when(campana.decidir(any())).thenReturn(decision("300"));

        servicio.saveVentaDetalle(request);

        ArgumentCaptor<SolicitudDeCampana> s = ArgumentCaptor.forClass(SolicitudDeCampana.class);
        verify(campana).decidir(s.capture());
        assertThat(s.getValue().cobro()).isEqualTo(FormaDeCobro.APARTADO);
        assertThat(s.getValue().codigo()).isEqualTo("abc-123");
        assertThat(s.getValue().recurrenteManual()).isTrue();
        assertThat(s.getValue().aceptaPrivacidad()).isTrue();
        assertThat(s.getValue().lineas()).hasSize(1);
        assertThat(s.getValue().lineas().get(0).precioBase()).isEqualByComparingTo("400");
        assertThat(pedidoGuardado().getTotalPedido()).isEqualTo(300.0);
        verify(campana).registrarCompra(any(), org.mockito.ArgumentMatchers.eq(77), org.mockito.ArgumentMatchers.eq(FormaDeCobro.APARTADO), org.mockito.ArgumentMatchers.eq(true));
    }
}
