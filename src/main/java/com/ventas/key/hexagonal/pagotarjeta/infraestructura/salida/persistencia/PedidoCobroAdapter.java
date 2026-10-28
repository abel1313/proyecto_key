package com.ventas.key.hexagonal.pagotarjeta.infraestructura.salida.persistencia;

import com.ventas.key.hexagonal.pagotarjeta.dominio.modelo.ArticuloACobrar;
import com.ventas.key.hexagonal.pagotarjeta.dominio.modelo.CanalDePago;
import com.ventas.key.hexagonal.pagotarjeta.dominio.modelo.EstadoIntento;
import com.ventas.key.hexagonal.pagotarjeta.dominio.excepcion.PagoConTarjetaException;
import com.ventas.key.hexagonal.pagotarjeta.dominio.modelo.PedidoACobrar;
import com.ventas.key.hexagonal.pagotarjeta.dominio.modelo.ResultadoAplicacion;
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
import com.ventas.key.mis.productos.repository.IGrupoPedidoMiembroRepository;
import com.ventas.key.mis.productos.repository.IPagoTarjetaIntentoRepository;
import com.ventas.key.mis.productos.repository.IPagosYMesesRepository;
import com.ventas.key.mis.productos.repository.IPedidoRepository;
import com.ventas.key.mis.productos.repository.IUsuarioRepository;
import com.ventas.key.mis.productos.repository.IVentaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
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
    private final IGrupoPedidoMiembroRepository grupos;
    private final EntityManager em;

    public PedidoCobroAdapter(IPedidoRepository pedidos, IVentaRepository ventas, IPagoTarjetaIntentoRepository intentos,
                              IPagosYMesesRepository pagosYMeses, IDetallePagoRepository detallesPago,
                              IUsuarioRepository usuarios, JdbcTemplate jdbc,
                              IGrupoPedidoMiembroRepository grupos, EntityManager em) {
        this.pedidos = pedidos;
        this.ventas = ventas;
        this.intentos = intentos;
        this.pagosYMeses = pagosYMeses;
        this.detallesPago = detallesPago;
        this.usuarios = usuarios;
        this.jdbc = jdbc;
        this.grupos = grupos;
        this.em = em;
    }

    // ── PedidoACobrarPort ─────────────────────────────────────────────────

    /** Transaccional (solo lectura): las lineas del pedido son LAZY y los trabajos programados no traen sesion. */
    @Override
    @Transactional(readOnly = true)
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
        List<DetallePedido> lineas = p.getDetalles() == null ? List.of() : p.getDetalles();
        Set<Integer> aceptan = variantesQueAceptanMsi(lineas.stream()
                .map(d -> d.getVariante() != null ? d.getVariante().getId() : null)
                .filter(java.util.Objects::nonNull).collect(java.util.stream.Collectors.toSet()));
        List<ArticuloACobrar> articulos = new ArrayList<>();
        for (DetallePedido d : lineas) {
            Integer varianteId = d.getVariante() != null ? d.getVariante().getId() : null;
            String nombre = d.getProducto() != null ? d.getProducto().getNombre() : "Artículo";
            articulos.add(new ArticuloACobrar(varianteId, nombre, varianteId != null && aceptan.contains(varianteId)));
        }
        Integer clienteId = p.getCliente() != null ? p.getCliente().getId() : null;
        return new PedidoACobrar(p.getId(), clienteId, tipoDeCobro(p.getTipoPedido()), total, pagado, cancelado,
                grupos.estaEnGrupoActivo(p.getId()), pagadoPor, articulos);
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

    /** Un cobro del camino anterior (mp_payment_intent, estado OPEN) sigue esperando la terminal. */
    @Override
    public boolean hayCobroEnProcesoPorElCaminoAnterior(Integer pedidoId) {
        try {
            Integer abiertos = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM mp_payment_intent WHERE pedido_id = ? AND estado = 'OPEN'", Integer.class, pedidoId);
            return abiertos != null && abiertos > 0;
        } catch (DataAccessException e) {
            log.warn("No se pudo revisar el camino anterior de la terminal (¿falta mp_payment_intent?): {}", e.getMessage());
            return false;
        }
    }

    /**
     * Una sola consulta para todos los articulos del pedido. Si la columna {@code acepta_msi} todavia no existe
     * (la migracion no se ha corrido en ese ambiente) nadie acepta MSI: se puede cobrar de contado y no truena.
     */
    private Set<Integer> variantesQueAceptanMsi(Collection<Integer> varianteIds) {
        if (varianteIds.isEmpty()) {
            return Set.of();
        }
        String marcas = String.join(",", java.util.Collections.nCopies(varianteIds.size(), "?"));
        try {
            return new HashSet<>(jdbc.queryForList(
                    "SELECT v.id FROM variantes v JOIN producto p ON p.id = v.producto_id "
                            + "WHERE v.id IN (" + marcas + ") AND COALESCE(v.acepta_msi, p.acepta_msi) = 1",
                    Integer.class, varianteIds.toArray()));
        } catch (DataAccessException e) {
            log.warn("No se pudo leer acepta_msi (¿falta migration_pago_tarjeta.sql?): {}", e.getMessage());
            return Set.of();
        }
    }

    // ── ActualizarPedidoPort ──────────────────────────────────────────────

    @Override
    public ResultadoAplicacion marcarPagado(Integer pedidoId, Long intentoId) {
        // Se BLOQUEA la fila del pedido y se refresca: con la sesion abierta de la peticion (OSIV) la entidad
        // pudo quedar vieja, y sin el candado un pago en efectivo simultaneo podria pasar por encima.
        Pedido pedido = em.find(Pedido.class, pedidoId, LockModeType.PESSIMISTIC_WRITE);
        if (pedido == null) {
            throw new PagoConTarjetaException.NoSePudoAplicar("No existe el pedido " + pedidoId);
        }
        em.refresh(pedido);
        PagoTarjetaIntento intento = intentos.findById(IntentosDePagoJpaAdapter.aId(intentoId))
                .orElseThrow(() -> new PagoConTarjetaException.NoSePudoAplicar("No existe el cobro " + intentoId));

        String motivo = motivoPorElQueNoAplica(pedido, intento);
        if (motivo != null) {
            log.warn("Cobro {} no se aplica al pedido {}: {}", intentoId, pedidoId, motivo);
            return ResultadoAplicacion.NO_APLICABLE;
        }

        // Un contado cobrado se guarda como 'Entregado' en estado_pedido (dice el PAGO, no la entrega: la
        // entrega fisica va aparte en la columna entregado) y su total_pagado se queda en 0, igual que la venta
        // de mostrador. Es lo que esperan el resto del sistema, los grupos, las entregas y los reportes.
        pedido.setEstadoPedido("Entregado");
        if (CanalDePago.valueOf(intento.getCanal()) == CanalDePago.TERMINAL) {
            pedido.marcarEntregado(true);                           // el cliente esta en el local y se lo lleva
        }
        pedidos.save(pedido);

        if (ventas.findByPedidoId(pedidoId).isEmpty()) {
            ventas.save(crearVenta(pedido, intento));
        }
        return ResultadoAplicacion.APLICADO;
    }

    /** Null si el pedido admite el pago; si no, por que no (con el pedido ya bloqueado y refrescado). */
    private String motivoPorElQueNoAplica(Pedido pedido, PagoTarjetaIntento intento) {
        String estado = pedido.getEstadoPedido() == null ? "" : pedido.getEstadoPedido().trim().toUpperCase();
        if ("CANCELADO".equals(estado)) return "el pedido está cancelado";
        if (PAGADOS.contains(estado)) return "el pedido ya estaba pagado";
        if (tipoDeCobro(pedido.getTipoPedido()) != TipoDeCobro.CONTADO) return "el pedido cambió de forma de cobro";
        if (pedido.getTotalPagado() != null && pedido.getTotalPagado() > 0.005) return "el pedido ya tiene dinero abonado";
        BigDecimal total = BigDecimal.valueOf(pedido.getTotalPedido() == null ? 0.0 : pedido.getTotalPedido())
                .setScale(2, RoundingMode.HALF_UP);
        if (total.compareTo(intento.getMonto()) != 0) return "el total del pedido cambió (" + total + " contra " + intento.getMonto() + ")";
        return null;
    }

    private Venta crearVenta(Pedido pedido, PagoTarjetaIntento intento) {
        Usuario usuario = usuarios.findByUsername(intento.getIniciadoPor())
                .orElseThrow(() -> new PagoConTarjetaException.NoSePudoAplicar(
                        "No existe el usuario " + intento.getIniciadoPor() + " para registrar la venta"));
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
            Double costoRegistrado = dp.getVariante().getProducto().getPrecioCosto();
            double costo = costoRegistrado != null ? costoRegistrado : 0.0;
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
                .orElseThrow(() -> new PagoConTarjetaException.NoSePudoAplicar(
                        "Catálogo de pago no encontrado: " + PAGOS_TARJETA_CONTADO));
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
