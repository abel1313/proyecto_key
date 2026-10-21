package com.ventas.key.hexagonal.pagotarjeta.infraestructura.salida.persistencia;

import com.ventas.key.hexagonal.pagotarjeta.dominio.modelo.ArticuloACobrar;
import com.ventas.key.hexagonal.pagotarjeta.dominio.modelo.CanalDePago;
import com.ventas.key.hexagonal.pagotarjeta.dominio.modelo.EstadoIntento;
import com.ventas.key.hexagonal.pagotarjeta.dominio.modelo.PedidoACobrar;
import com.ventas.key.hexagonal.pagotarjeta.dominio.modelo.TipoDeCobro;
import com.ventas.key.hexagonal.pagotarjeta.dominio.puerto.salida.ActualizarPedidoPort;
import com.ventas.key.hexagonal.pagotarjeta.dominio.puerto.salida.PedidoACobrarPort;
import com.ventas.key.mis.productos.entity.DetallePedido;
import com.ventas.key.mis.productos.entity.DetallePago;
import com.ventas.key.mis.productos.entity.DetallePagoId;
import com.ventas.key.mis.productos.entity.DetalleVentaVariante;
import com.ventas.key.mis.productos.entity.MesesIntereses;
import com.ventas.key.mis.productos.entity.PagoTarjetaIntento;
import com.ventas.key.mis.productos.entity.PagosYMeses;
import com.ventas.key.mis.productos.entity.Pedido;
import com.ventas.key.mis.productos.entity.Usuario;
import com.ventas.key.mis.productos.entity.Venta;
import com.ventas.key.mis.productos.repository.IDetallePagoRepository;
import com.ventas.key.mis.productos.repository.IPagoTarjetaIntentoRepository;
import com.ventas.key.mis.productos.repository.IPagosYMesesRepository;
import com.ventas.key.mis.productos.repository.IPedidoRepository;
import com.ventas.key.mis.productos.repository.IUsuarioRepository;
import com.ventas.key.mis.productos.repository.IVentaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * [Hexagonal: Driven Adapter] [Clean: Interface Adapter]
 *
 * <p>El unico lugar donde el cobro con tarjeta toca el pedido y la venta. NO reutiliza
 * {@code PedidoServiceImpl.updatePedido} (pone "Entregado", rechaza Apartado y no es idempotente).
 *
 * <p><b>Lectura:</b> {@code tipo_pedido} NORMAL = Contado, APARTADO = Apartado, FIADO = Ir pagando.
 * "Pagado" es {@code estado_pedido} PAGADO o Entregado. Si el cobro lo pago un intento, se reconoce
 * por la tabla de intentos. La casilla de MSI sale de {@code COALESCE(variantes.acepta_msi, producto.acepta_msi)}
 * (consulta nativa: las entidades todavia no mapean esas columnas).
 *
 * <p><b>Escritura ({@link #marcarPagado}), idempotente:</b> pedido PAGADO con su total pagado, venta con la
 * forma de pago de tarjeta (con la comision de los meses elegidos) y, si se cobro en la terminal del local
 * (el cliente se lo lleva en ese momento), el pedido tambien queda entregado. Pagado en linea: queda
 * Pagado y falta entregarlo. Si ya hay una venta del pedido no se crea otra. Debe llamarse dentro de la
 * transaccion del servicio (lo hace {@code TransaccionPort}).
 */
@Component
public class PedidoCobroAdapter implements PedidoACobrarPort, ActualizarPedidoPort {

    private static final Logger log = LoggerFactory.getLogger(PedidoCobroAdapter.class);

    /** Fila de {@code pagos_y_meses} de "tarjeta de contado" (la misma que usa el cobro de abonos). */
    static final int PAGOS_TARJETA_CONTADO = 2;
    private static final Set<String> PAGADOS = Set.of("PAGADO", "ENTREGADO");

    private final IPedidoRepository pedidos;
    private final IVentaRepository ventas;
    private final IPagoTarjetaIntentoRepository intentos;
    private final IPagosYMesesRepository pagosYMeses;
    private final IDetallePagoRepository detallesPago;
    private final IUsuarioRepository usuarios;
    private final JdbcTemplate jdbc;

    public PedidoCobroAdapter(IPedidoRepository pedidos, IVentaRepository ventas, IPagoTarjetaIntentoRepository intentos,
                              IPagosYMesesRepository pagosYMeses, IDetallePagoRepository detallesPago,
                              IUsuarioRepository usuarios, JdbcTemplate jdbc) {
        this.pedidos = pedidos;
        this.ventas = ventas;
        this.intentos = intentos;
        this.pagosYMeses = pagosYMeses;
        this.detallesPago = detallesPago;
        this.usuarios = usuarios;
        this.jdbc = jdbc;
    }

    // ── PedidoACobrarPort ─────────────────────────────────────────────────

    @Override
    public Optional<PedidoACobrar> buscar(Integer pedidoId) {
        return pedidos.findById(pedidoId).map(this::aDominio);
    }

    private PedidoACobrar aDominio(Pedido p) {
        String estado = p.getEstadoPedido() == null ? "" : p.getEstadoPedido().trim().toUpperCase();
        boolean pagado = PAGADOS.contains(estado);
        boolean cancelado = "CANCELADO".equals(estado);
        Long pagadoPor = pagado ? intentoQuePago(p.getId()) : null;
        BigDecimal total = BigDecimal.valueOf(p.getTotalPedido() == null ? 0.0 : p.getTotalPedido())
                .setScale(2, RoundingMode.HALF_UP);
        List<ArticuloACobrar> articulos = new ArrayList<>();
        if (p.getDetalles() != null) {
            for (DetallePedido d : p.getDetalles()) {
                Integer varianteId = d.getVariante() != null ? d.getVariante().getId() : null;
                String nombre = d.getProducto() != null ? d.getProducto().getNombre() : "Artículo";
                articulos.add(new ArticuloACobrar(varianteId, nombre, varianteId != null && aceptaMsi(varianteId)));
            }
        }
        Integer clienteId = p.getCliente() != null ? p.getCliente().getId() : null;
        return new PedidoACobrar(p.getId(), clienteId, tipoDeCobro(p.getTipoPedido()), total, pagado, cancelado,
                pagadoPor, articulos);
    }

    static TipoDeCobro tipoDeCobro(String tipoPedido) {
        if (tipoPedido == null || "NORMAL".equals(tipoPedido)) return TipoDeCobro.CONTADO;
        return switch (tipoPedido) {
            case "APARTADO" -> TipoDeCobro.APARTADO;
            default -> TipoDeCobro.IR_PAGANDO;                // FIADO y cualquier otro: nunca tarjeta
        };
    }

    private Long intentoQuePago(Integer pedidoId) {
        return intentos.findByPedidoIdAndEstadoInOrderByIdAsc(pedidoId, List.of(
                        EstadoIntento.APROBADO.name(), EstadoIntento.DEVUELTO_PARCIAL.name(), EstadoIntento.DEVUELTO.name()))
                .stream().findFirst().map(i -> i.getId().longValue()).orElse(null);
    }

    private boolean aceptaMsi(Integer varianteId) {
        List<Integer> r = jdbc.queryForList(
                "SELECT COALESCE(v.acepta_msi, p.acepta_msi) FROM variantes v "
                        + "JOIN producto p ON p.id = v.producto_id WHERE v.id = ?", Integer.class, varianteId);
        return !r.isEmpty() && r.get(0) != null && r.get(0) == 1;
    }

    // ── ActualizarPedidoPort ──────────────────────────────────────────────

    @Override
    public void marcarPagado(Integer pedidoId, Long intentoId) {
        Pedido pedido = pedidos.findById(pedidoId)
                .orElseThrow(() -> new IllegalStateException("No existe el pedido " + pedidoId));
        PagoTarjetaIntento intento = intentos.findById(IntentosDePagoJpaAdapter.aId(intentoId))
                .orElseThrow(() -> new IllegalStateException("No existe el cobro " + intentoId));
        String estado = pedido.getEstadoPedido() == null ? "" : pedido.getEstadoPedido().trim().toUpperCase();
        if (PAGADOS.contains(estado)) {
            return;                                                 // idempotente: ya esta pagado
        }

        pedido.setTotalPagado(pedido.getTotalPedido());
        pedido.setEstadoPedido("PAGADO");
        if (CanalDePago.valueOf(intento.getCanal()) == CanalDePago.TERMINAL) {
            pedido.marcarEntregado(true);                           // el cliente esta en el local y se lo lleva
        }
        pedidos.save(pedido);

        if (ventas.findByPedidoId(pedidoId).isEmpty()) {
            ventas.save(crearVenta(pedido, intento));
        }
    }

    private Venta crearVenta(Pedido pedido, PagoTarjetaIntento intento) {
        Usuario usuario = usuarios.findByUsername(intento.getIniciadoPor())
                .orElseThrow(() -> new IllegalStateException("No existe el usuario " + intento.getIniciadoPor()));
        PagosYMeses forma = formaDePago(intento.getCuotas());
        MesesIntereses meses = forma.getMesesIntereses();
        double tasa = (meses.getTarifaTerminal() != null ? meses.getTarifaTerminal().getTarifa() / 100.0 : 0.0)
                + (meses.getIvaTerminal() != null ? meses.getIvaTerminal().getIva() / 100.0 : 0.0);
        DetallePago detallePago = null;
        if (meses.getTarifaTerminal() != null && meses.getIvaTerminal() != null) {
            detallePago = detallesPago.findById(new DetallePagoId(forma.getTipoPago().getId(),
                    meses.getTarifaTerminal().getId(), meses.getIvaTerminal().getId())).orElse(null);
        }

        Venta venta = new Venta();
        venta.setEstadoVenta("Entregado");
        venta.setFechaVenta(LocalDateTime.now());
        venta.setPedido(pedido);
        venta.setUsuario(usuario);
        venta.setCliente(pedido.getCliente());
        venta.setClienteSinRegistro(pedido.getClienteSinRegistro());
        venta.setPagosYMeses(forma);
        venta.setDetallePago(detallePago);

        List<DetalleVentaVariante> detalles = new ArrayList<>();
        for (DetallePedido dp : pedido.getDetalles()) {
            double costo = dp.getVariante().getProducto().getPrecioCosto();
            double subTotal = dp.getSubTotal();
            DetalleVentaVariante dvv = new DetalleVentaVariante();
            dvv.setCantidad(dp.getCantidad());
            dvv.setPrecioUnitario(dp.getPrecioUnitario());
            dvv.setSubTotal(subTotal);
            dvv.setPrecioCosto(costo);
            dvv.setGanancia(subTotal - costo * dp.getCantidad() - subTotal * tasa);
            dvv.setFechaVenta(LocalDate.now());
            dvv.setVariante(dp.getVariante());
            dvv.setVenta(venta);
            detalles.add(dvv);
        }
        venta.setTotalVenta(detalles.stream().mapToDouble(DetalleVentaVariante::getSubTotal).sum());
        venta.setGananciaTotal(detalles.stream().mapToDouble(DetalleVentaVariante::getGanancia).sum());
        venta.setDetalles(detalles);
        return venta;
    }

    /**
     * La forma de pago del catalogo {@code pagos_y_meses}. De contado (1 pago) se usa la fila 2, que es la que ya
     * usa el cobro de abonos (en QA: "Tarjeta Debito" y "Tarjeta credito" a 1 pago llevan la misma comision, asi
     * que no importa cual de las dos sea). A meses sin intereses se busca la fila de tarjeta de CREDITO con
     * esos meses. Si no existe (la comision no queda exacta), se usa la de contado y se avisa en el log:
     * registrar el pago importa mas que la comision.
     */
    private PagosYMeses formaDePago(int cuotas) {
        PagosYMeses contado = pagosYMeses.findById(PAGOS_TARJETA_CONTADO)
                .orElseThrow(() -> new IllegalStateException("Catálogo de pago no encontrado: " + PAGOS_TARJETA_CONTADO));
        if (cuotas <= 1) {
            return contado;
        }
        for (PagosYMeses candidata : pagosYMeses.findAll()) {
            String forma = candidata.getTipoPago().getFormaPago();
            boolean esCredito = forma != null && forma.toLowerCase(java.util.Locale.ROOT).replace("é", "e").contains("credito");
            Integer plazo = PlanesMsiJdbcAdapter.interpretar(candidata.getMesesIntereses().getMeses());
            if (esCredito && plazo != null && plazo == cuotas) {
                return candidata;
            }
        }
        log.warn("No hay forma de pago de tarjeta de crédito a {} meses en pagos_y_meses; se registra como tarjeta de contado", cuotas);
        return contado;
    }

    @Override
    public void registrarDevolucion(Integer pedidoId, Long intentoId, BigDecimal monto, String hechaPor, String clave) {
        // El registro de la devolucion (quien, cuanto, cuando, clave unica) ya vive en pago_tarjeta_devolucion;
        // el pedido y la venta no cambian: regresar mercancia y stock es una decision aparte del dueño.
        log.info("Devolución de {} del cobro {} (pedido {}) por {} [{}]", monto, intentoId, pedidoId, hechaPor, clave);
    }
}
