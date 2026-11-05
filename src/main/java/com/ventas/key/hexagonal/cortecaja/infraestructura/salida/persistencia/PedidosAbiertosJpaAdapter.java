package com.ventas.key.hexagonal.cortecaja.infraestructura.salida.persistencia;

import com.ventas.key.hexagonal.cortecaja.dominio.excepcion.CorteDeCajaException;
import com.ventas.key.hexagonal.cortecaja.dominio.modelo.FiltroDeCorte;
import com.ventas.key.hexagonal.cortecaja.dominio.modelo.MontosDeGanancia;
import com.ventas.key.hexagonal.cortecaja.dominio.modelo.PedidoPorCobrar;
import com.ventas.key.hexagonal.cortecaja.dominio.puerto.salida.PedidosAbiertosPort;
import com.ventas.key.mis.productos.Utils.CompradorDelPedido;
import com.ventas.key.mis.productos.entity.DetallePedido;
import com.ventas.key.mis.productos.entity.Pedido;
import com.ventas.key.mis.productos.entity.Producto;
import com.ventas.key.mis.productos.entity.productoVariantes.Variantes;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * [Hexagonal: Driven Adapter] [Clean: Interface Adapter]
 *
 * <p>Lee los pedidos sin cancelar de la zona, fechas y entrega pedidas, con sus piezas, sus pagos y su costo. Solo {@code SELECT}. Las entidades JPA viven en
 * {@code mis/productos/entity}; el dominio no las ve.
 *
 * <p>El costo es el costo ACTUAL del producto (no se guarda en el pedido): si cambia después, la ganancia de pedidos viejos cambia con él
 * (decisión del dueño, 2026-11-04).
 */
@Component
public class PedidosAbiertosJpaAdapter implements PedidosAbiertosPort {

    @PersistenceContext
    private EntityManager entityManager;

    /** Más pedidos que esto no se suman de una vez: se pide acotar fechas o zona. */
    static final int MAXIMO_PEDIDOS = 3000;

    @Override
    @Transactional(readOnly = true)
    public List<PedidoPorCobrar> buscar(FiltroDeCorte filtro) {
        StringBuilder jpql = new StringBuilder("SELECT p FROM Pedido p LEFT JOIN FETCH p.lugarEntrega WHERE LOWER(COALESCE(p.estadoPedido, '')) <> 'cancelado'");
        if (filtro.entrega() == FiltroDeCorte.Entrega.SIN_ENTREGAR) {
            jpql.append(" AND p.entregado = false AND LOWER(COALESCE(p.estadoPedido, '')) <> 'entregado'");
        } else if (filtro.entrega() == FiltroDeCorte.Entrega.ENTREGADOS) {
            jpql.append(" AND (p.entregado = true OR LOWER(COALESCE(p.estadoPedido, '')) = 'entregado')");
        }
        if (!filtro.todasLasZonas()) {
            boolean reales = !filtro.zonasReales().isEmpty();
            jpql.append(" AND (");
            if (reales) {
                jpql.append("p.lugarEntrega.id IN :zonas");
            }
            if (reales && filtro.incluyeSinZona()) {
                jpql.append(" OR ");
            }
            if (filtro.incluyeSinZona()) {
                jpql.append("p.lugarEntrega IS NULL");
            }
            jpql.append(")");
        }
        if (filtro.desde() != null) {
            jpql.append(" AND p.fechaPedido >= :desde");
        }
        if (filtro.hasta() != null) {
            jpql.append(" AND p.fechaPedido <= :hasta");
        }
        var consulta = entityManager.createQuery(jpql.toString(), Pedido.class).setMaxResults(MAXIMO_PEDIDOS + 1);
        if (!filtro.zonasReales().isEmpty()) {
            consulta.setParameter("zonas", filtro.zonasReales());
        }
        if (filtro.desde() != null) {
            consulta.setParameter("desde", filtro.desde());
        }
        if (filtro.hasta() != null) {
            consulta.setParameter("hasta", filtro.hasta());
        }
        List<Pedido> pedidos = consulta.getResultList();
        if (pedidos.size() > MAXIMO_PEDIDOS) {
            throw new CorteDeCajaException.DemasiadosPedidos(MAXIMO_PEDIDOS);
        }
        if (pedidos.isEmpty()) {
            return List.of();
        }
        List<Integer> ids = pedidos.stream().map(Pedido::getId).toList();
        Map<Integer, List<DetallePedido>> renglones = renglonesPorPedido(ids);
        Map<Integer, Long> pagos = pagosPorPedido(ids);
        return pedidos.stream()
                .map(p -> aDominio(p, renglones.getOrDefault(p.getId(), List.of()), pagos.getOrDefault(p.getId(), 0L).intValue()))
                .toList();
    }

    private Map<Integer, List<DetallePedido>> renglonesPorPedido(List<Integer> ids) {
        List<DetallePedido> todos = entityManager.createQuery(
                        "SELECT d FROM DetallePedido d JOIN FETCH d.variante v JOIN FETCH v.producto JOIN FETCH d.pedido p WHERE p.id IN :ids",
                        DetallePedido.class)
                .setParameter("ids", ids)
                .getResultList();
        Map<Integer, List<DetallePedido>> porPedido = new HashMap<>();
        for (DetallePedido d : todos) {
            porPedido.computeIfAbsent(d.getPedido().getId(), k -> new java.util.ArrayList<>()).add(d);
        }
        return porPedido;
    }

    private Map<Integer, Long> pagosPorPedido(List<Integer> ids) {
        List<Object[]> filas = entityManager.createQuery(
                        "SELECT a.pedido.id, COUNT(a) FROM AbonoPedido a WHERE a.pedido.id IN :ids GROUP BY a.pedido.id", Object[].class)
                .setParameter("ids", ids)
                .getResultList();
        Map<Integer, Long> pagos = new HashMap<>();
        for (Object[] f : filas) {
            pagos.put((Integer) f[0], (Long) f[1]);
        }
        return pagos;
    }

    private PedidoPorCobrar aDominio(Pedido p, List<DetallePedido> renglones, int pagos) {
        var zona = p.getLugarEntrega();
        String cliente = CompradorDelPedido.nombre(p);
        int piezas = renglones.stream().mapToInt(d -> d.getCantidad() == null ? 0 : d.getCantidad()).sum();
        return new PedidoPorCobrar(
                p.getId(),
                cliente == null || cliente.isBlank() ? "Sin nombre" : cliente,
                piezas,
                dinero(p.getTotalPedido()),
                dinero(p.getTotalPagado()),
                pagos,
                p.getFechaPedido(),
                p.getEstadoPedido(),
                p.isEntregado(),
                zona == null ? null : zona.getId(),
                zona == null ? null : zona.getNombre(),
                montos(renglones));
    }

    private static MontosDeGanancia montos(List<DetallePedido> renglones) {
        BigDecimal costo = BigDecimal.ZERO;
        BigDecimal normal = BigDecimal.ZERO;
        BigDecimal conDescuento = BigDecimal.ZERO;
        boolean hayDescuento = false;
        for (DetallePedido d : renglones) {
            BigDecimal cantidad = BigDecimal.valueOf(d.getCantidad() == null ? 0 : d.getCantidad());
            Variantes v = d.getVariante();
            Producto producto = v.getProducto();
            costo = costo.add(cantidad.multiply(dinero(producto.getPrecioCosto())));
            // Si el artículo ya no tiene precio normal (dato viejo), se usa el precio al que se cobró.
            Double precioNormal = v.precioNormal() != null ? v.precioNormal() : d.getPrecioUnitario();
            normal = normal.add(cantidad.multiply(dinero(precioNormal)));
            Double descuento = v.precioDescuento();
            boolean tieneDescuento = descuento != null && descuento > 0 && precioNormal != null && descuento < precioNormal;
            hayDescuento = hayDescuento || tieneDescuento;
            conDescuento = conDescuento.add(cantidad.multiply(dinero(tieneDescuento ? descuento : precioNormal)));
        }
        return new MontosDeGanancia(costo, normal, conDescuento, hayDescuento);
    }

    private static BigDecimal dinero(Double valor) {
        return BigDecimal.valueOf(valor == null ? 0.0 : valor);
    }
}
