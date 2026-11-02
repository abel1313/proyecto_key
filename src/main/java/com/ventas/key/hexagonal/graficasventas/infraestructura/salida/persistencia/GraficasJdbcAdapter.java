package com.ventas.key.hexagonal.graficasventas.infraestructura.salida.persistencia;

import com.ventas.key.hexagonal.graficasventas.dominio.modelo.ArticuloParado;
import com.ventas.key.hexagonal.graficasventas.dominio.modelo.CategoriaPedido;
import com.ventas.key.hexagonal.graficasventas.dominio.modelo.ClienteVentas;
import com.ventas.key.hexagonal.graficasventas.dominio.modelo.CompraDeCliente;
import com.ventas.key.hexagonal.graficasventas.dominio.modelo.ProductoRentable;
import com.ventas.key.hexagonal.graficasventas.dominio.modelo.VentaPorDiaYHora;
import com.ventas.key.hexagonal.graficasventas.dominio.modelo.ZonaDeEntrega;
import com.ventas.key.hexagonal.graficasventas.dominio.modelo.FormaDePagoVentas;
import com.ventas.key.hexagonal.graficasventas.dominio.modelo.SaldoPendiente;
import com.ventas.key.hexagonal.graficasventas.dominio.modelo.VentasDelMes;
import com.ventas.key.hexagonal.graficasventas.dominio.puerto.salida.GraficasPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * [Hexagonal: Driven Adapter] [Clean: Interface Adapter]
 *
 * <p>SQL de solo lectura para las gráficas del Dashboard. Criterios iguales a las tarjetas del Dashboard: las ventas con
 * estado «Devuelta» no cuentan y los gastos son {@code gastos_surtir.precio_gasto}. La comisión usa la misma cuenta con la
 * que se calcula la ganancia al vender: {@code total × (tarifa + IVA de la terminal) / 100}.
 * Solo SQL estándar ({@code EXTRACT}, {@code COALESCE}), para probarlo con H2 y correr en MySQL.
 */
@Component
public class GraficasJdbcAdapter implements GraficasPort {

    /** Constante fija (no viene de nadie): va dentro del SQL para que SELECT y GROUP BY sean idénticos en MySQL. */
    private static final String SIN_FORMA = "Sin forma de pago registrada";

    private final JdbcTemplate jdbc;

    public GraficasJdbcAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    @Transactional(readOnly = true)
    public List<FormaDePagoVentas> ventasPorFormaDePago(LocalDate desde, LocalDate hasta) {
        return jdbc.query(
                "SELECT COALESCE(tp.forma_pago, '" + SIN_FORMA + "') AS forma, COUNT(*) AS ventas, "
                        + "COALESCE(SUM(v.total_venta), 0) AS total, "
                        + "COALESCE(SUM(v.total_venta * (COALESCE(tt.tarifa, 0) + COALESCE(it.iva, 0)) / 100.0), 0) AS comision "
                        + "FROM ventas v "
                        + "LEFT JOIN pagos_y_meses pm ON pm.id = v.pagos_y_meses_id "
                        + "LEFT JOIN tipo_pago tp ON tp.id = pm.tipo_pago_id "
                        + "LEFT JOIN meses_intereses mi ON mi.id = pm.meses_intereses_id "
                        + "LEFT JOIN tarifa_terminal tt ON tt.id = mi.tarifa_id "
                        + "LEFT JOIN iva_terminal it ON it.id = mi.iva_id "
                        + "WHERE v.fecha_venta >= ? AND v.fecha_venta < ? AND v.estado_venta <> 'Devuelta' "
                        + "GROUP BY COALESCE(tp.forma_pago, '" + SIN_FORMA + "')",
                (rs, n) -> new FormaDePagoVentas(rs.getString("forma"), rs.getInt("ventas"), rs.getDouble("total"), rs.getDouble("comision")),
                inicio(desde), finExclusivo(hasta));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<YearMonth, VentasDelMes> ventasPorMes(LocalDate desde, LocalDate hasta) {
        Map<YearMonth, VentasDelMes> resultado = new HashMap<>();
        jdbc.query(
                "SELECT EXTRACT(YEAR FROM v.fecha_venta) AS anio, EXTRACT(MONTH FROM v.fecha_venta) AS mes, COUNT(*) AS ventas, "
                        + "COALESCE(SUM(v.total_venta), 0) AS total, COALESCE(SUM(v.ganancia_total), 0) AS ganancia "
                        + "FROM ventas v WHERE v.fecha_venta >= ? AND v.fecha_venta < ? AND v.estado_venta <> 'Devuelta' "
                        + "GROUP BY EXTRACT(YEAR FROM v.fecha_venta), EXTRACT(MONTH FROM v.fecha_venta)",
                rs -> {
                    resultado.put(YearMonth.of(rs.getInt("anio"), rs.getInt("mes")),
                            new VentasDelMes(rs.getInt("ventas"), rs.getDouble("total"), rs.getDouble("ganancia")));
                },
                inicio(desde), finExclusivo(hasta));
        return resultado;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<YearMonth, Double> gastosPorMes(LocalDate desde, LocalDate hasta) {
        Map<YearMonth, Double> resultado = new HashMap<>();
        jdbc.query(
                "SELECT EXTRACT(YEAR FROM g.fecha) AS anio, EXTRACT(MONTH FROM g.fecha) AS mes, COALESCE(SUM(g.precio_gasto), 0) AS total "
                        + "FROM gastos_surtir g WHERE g.fecha BETWEEN ? AND ? "
                        + "GROUP BY EXTRACT(YEAR FROM g.fecha), EXTRACT(MONTH FROM g.fecha)",
                rs -> {
                    resultado.put(YearMonth.of(rs.getInt("anio"), rs.getInt("mes")), rs.getDouble("total"));
                },
                java.sql.Date.valueOf(desde), java.sql.Date.valueOf(hasta));
        return resultado;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SaldoPendiente> saldosPendientes() {
        // Mismo criterio que la tarjeta «Por cobrar» del Dashboard (estado APARTADO o FIADO), sin los saldos de un centavo o menos.
        return jdbc.query(
                "SELECT p.id AS pedido_id, p.tipo_pedido AS tipo, p.fecha_pedido AS fecha_del_pedido, "
                        + "(COALESCE(p.total_pedido, 0) - COALESCE(p.total_pagado, 0)) AS saldo, "
                        + "COALESCE(NULLIF(TRIM(CONCAT(COALESCE(c.nombre_persona, ''), ' ', COALESCE(c.apeido_paterno, ''))), ''), "
                        + "NULLIF(TRIM(CONCAT(COALESCE(s.nombre_persona, ''), ' ', COALESCE(s.apeido_paterno, ''))), ''), "
                        + "'Cliente sin nombre') AS cliente "
                        + "FROM pedidos p "
                        + "LEFT JOIN clientes c ON c.id = p.cliente_id "
                        + "LEFT JOIN clientes_sin_registro s ON s.id = p.cliente_sin_registro_id "
                        + "WHERE p.estado_pedido IN ('APARTADO', 'FIADO') "
                        + "AND (COALESCE(p.total_pedido, 0) - COALESCE(p.total_pagado, 0)) > 0.01 "
                        + "AND p.fecha_pedido IS NOT NULL",
                (rs, n) -> new SaldoPendiente(rs.getInt("pedido_id"), rs.getString("cliente"), rs.getString("tipo"),
                        rs.getDouble("saldo"), rs.getDate("fecha_del_pedido").toLocalDate()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ArticuloParado> articulosSinMovimiento(LocalDate corte) {
        return jdbc.query(
                "SELECT v.id AS articulo_id, p.nombre AS nombre, v.talla AS talla, v.color AS color, v.stock AS stock, "
                        + "COALESCE(p.precio_costo, 0) AS costo, u.ultima AS ultima_venta "
                        + "FROM variantes v JOIN producto p ON p.id = v.producto_id "
                        + "LEFT JOIN (SELECT variante_id, MAX(fecha_venta) AS ultima FROM detalle_venta_variantes GROUP BY variante_id) u "
                        + "ON u.variante_id = v.id "
                        + "WHERE v.habilitado = '1' AND p.habilitado = '1' AND p.es_catalogo_interno = 0 AND v.stock > 0 "
                        + "AND (v.fecha_creacion IS NULL OR v.fecha_creacion < ?) "
                        + "AND (u.ultima IS NULL OR u.ultima < ?)",
                (rs, n) -> new ArticuloParado(rs.getInt("articulo_id"), nombreDelArticulo(rs), rs.getInt("stock"),
                        rs.getDouble("costo"), rs.getDate("ultima_venta") == null ? null : rs.getDate("ultima_venta").toLocalDate()),
                inicio(corte), java.sql.Date.valueOf(corte));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<CategoriaPedido, Integer> pedidosPorCategoria(LocalDate desdeTerminados) {
        Map<CategoriaPedido, Integer> resultado = new EnumMap<>(CategoriaPedido.class);
        jdbc.query(
                "SELECT categoria, COUNT(*) AS pedidos FROM ("
                        + "SELECT CASE "
                        + "WHEN UPPER(COALESCE(p.estado_pedido, '')) = 'CANCELADO' THEN 'CANCELADO' "
                        + "WHEN UPPER(COALESCE(p.estado_pedido, '')) IN ('ENTREGADO', 'PAGADO') AND p.entregado = TRUE THEN 'ENTREGADO' "
                        + "WHEN UPPER(COALESCE(p.estado_pedido, '')) IN ('ENTREGADO', 'PAGADO') THEN 'PAGADO_POR_ENTREGAR' "
                        + "WHEN p.tipo_pedido = 'APARTADO' THEN 'APARTADO' "
                        + "WHEN p.tipo_pedido = 'FIADO' THEN 'IR_PAGANDO' "
                        + "ELSE 'PENDIENTE' END AS categoria, p.fecha_pedido AS fecha_del_pedido FROM pedidos p) t "
                        + "WHERE categoria IN ('PENDIENTE', 'APARTADO', 'IR_PAGANDO', 'PAGADO_POR_ENTREGAR') OR fecha_del_pedido >= ? "
                        + "GROUP BY categoria",
                rs -> {
                    resultado.put(CategoriaPedido.valueOf(rs.getString("categoria")), rs.getInt("pedidos"));
                },
                java.sql.Date.valueOf(desdeTerminados));
        return resultado;
    }

    @Override
    @Transactional(readOnly = true)
    public int pedidosAtrasados(LocalDate hoy) {
        Integer total = jdbc.queryForObject(
                "SELECT COUNT(*) FROM pedidos p WHERE p.fecha_recogida IS NOT NULL AND p.fecha_recogida < ? "
                        + "AND p.entregado = FALSE AND UPPER(COALESCE(p.estado_pedido, '')) <> 'CANCELADO'",
                Integer.class, java.sql.Date.valueOf(hoy));
        return total == null ? 0 : total;
    }

    private static String nombreDelArticulo(java.sql.ResultSet rs) throws java.sql.SQLException {
        StringBuilder sb = new StringBuilder(rs.getString("nombre") != null ? rs.getString("nombre") : "Artículo");
        String talla = rs.getString("talla");
        String color = rs.getString("color");
        if (talla != null && !talla.isBlank()) sb.append(" talla ").append(talla);
        if (color != null && !color.isBlank()) sb.append(' ').append(color);
        return sb.toString();
    }

    /** Ventas hechas al propio perfil de cliente de quien vende (mostrador sin cliente): no son un cliente real. */
    private static final String NO_ES_MOSTRADOR =
            "AND NOT EXISTS (SELECT 1 FROM clientes cu WHERE cu.id = v.cliente_id AND cu.usuario_id = v.usuario_id) ";

    @Override
    @Transactional(readOnly = true)
    public List<VentaPorDiaYHora> ventasPorDiaYHora(LocalDate desde, LocalDate hasta) {
        return jdbc.query(
                "SELECT CAST(v.fecha_venta AS DATE) AS dia, EXTRACT(HOUR FROM v.fecha_venta) AS hora, COUNT(*) AS ventas, "
                        + "COALESCE(SUM(v.total_venta), 0) AS total FROM ventas v "
                        + "WHERE v.fecha_venta >= ? AND v.fecha_venta < ? AND v.estado_venta <> 'Devuelta' "
                        + "GROUP BY CAST(v.fecha_venta AS DATE), EXTRACT(HOUR FROM v.fecha_venta)",
                (rs, n) -> new VentaPorDiaYHora(rs.getDate("dia").toLocalDate(), rs.getInt("hora"), rs.getInt("ventas"), rs.getDouble("total")),
                inicio(desde), finExclusivo(hasta));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Integer, LocalDate> primerasCompras() {
        Map<Integer, LocalDate> resultado = new HashMap<>();
        jdbc.query(
                "SELECT v.cliente_id AS cliente_id, MIN(v.fecha_venta) AS primera FROM ventas v "
                        + "WHERE v.cliente_id IS NOT NULL AND v.estado_venta <> 'Devuelta' " + NO_ES_MOSTRADOR
                        + "GROUP BY v.cliente_id",
                rs -> {
                    resultado.put(rs.getInt("cliente_id"), rs.getTimestamp("primera").toLocalDateTime().toLocalDate());
                });
        return resultado;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CompraDeCliente> comprasPorClienteYMes(LocalDate desde, LocalDate hasta) {
        return jdbc.query(
                "SELECT v.cliente_id AS cliente_id, EXTRACT(YEAR FROM v.fecha_venta) AS anio, EXTRACT(MONTH FROM v.fecha_venta) AS mes, "
                        + "COUNT(*) AS ventas, COALESCE(SUM(v.total_venta), 0) AS total FROM ventas v "
                        + "WHERE v.cliente_id IS NOT NULL AND v.fecha_venta >= ? AND v.fecha_venta < ? AND v.estado_venta <> 'Devuelta' "
                        + NO_ES_MOSTRADOR
                        + "GROUP BY v.cliente_id, EXTRACT(YEAR FROM v.fecha_venta), EXTRACT(MONTH FROM v.fecha_venta)",
                (rs, n) -> new CompraDeCliente(rs.getInt("cliente_id"), YearMonth.of(rs.getInt("anio"), rs.getInt("mes")),
                        rs.getInt("ventas"), rs.getDouble("total")),
                inicio(desde), finExclusivo(hasta));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClienteVentas> mejoresClientes(LocalDate desde, LocalDate hasta, int limite) {
        return jdbc.query(
                "SELECT v.cliente_id AS cliente_id, "
                        + "COALESCE(NULLIF(TRIM(CONCAT(COALESCE(c.nombre_persona, ''), ' ', COALESCE(c.apeido_paterno, ''))), ''), 'Cliente sin nombre') AS nombre, "
                        + "COUNT(*) AS ventas, COALESCE(SUM(v.total_venta), 0) AS total "
                        + "FROM ventas v JOIN clientes c ON c.id = v.cliente_id "
                        + "WHERE v.fecha_venta >= ? AND v.fecha_venta < ? AND v.estado_venta <> 'Devuelta' " + NO_ES_MOSTRADOR
                        + "GROUP BY v.cliente_id, c.nombre_persona, c.apeido_paterno "
                        + "ORDER BY SUM(v.total_venta) DESC LIMIT ?",
                (rs, n) -> new ClienteVentas(rs.getInt("cliente_id"), rs.getString("nombre"), rs.getInt("ventas"), rs.getDouble("total")),
                inicio(desde), finExclusivo(hasta), limite);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ZonaDeEntrega> pedidosPorZona(LocalDate desde, LocalDate hasta) {
        return jdbc.query(
                "SELECT COALESCE(le.nombre, 'Sin lugar de entrega') AS zona, COALESCE(le.es_recoger_en_tienda, FALSE) AS recoger, "
                        + "COUNT(*) AS pedidos, COALESCE(SUM(p.total_pedido), 0) AS total "
                        + "FROM pedidos p LEFT JOIN lugares_entrega le ON le.id = p.lugar_entrega_id "
                        + "WHERE p.fecha_pedido BETWEEN ? AND ? AND UPPER(COALESCE(p.estado_pedido, '')) <> 'CANCELADO' "
                        + "GROUP BY COALESCE(le.nombre, 'Sin lugar de entrega'), COALESCE(le.es_recoger_en_tienda, FALSE)",
                (rs, n) -> new ZonaDeEntrega(rs.getString("zona"), rs.getBoolean("recoger"), rs.getInt("pedidos"), rs.getDouble("total")),
                java.sql.Date.valueOf(desde), java.sql.Date.valueOf(hasta));
    }

    private static Timestamp inicio(LocalDate dia) {
        return Timestamp.valueOf(dia.atStartOfDay());
    }

    private static Timestamp finExclusivo(LocalDate dia) {
        return Timestamp.valueOf(dia.plusDays(1).atStartOfDay());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductoRentable> gananciaPorProducto(LocalDate desde, LocalDate hasta, int limite) {
        return jdbc.query(
                "SELECT * FROM (SELECT p.id AS producto_id, p.nombre AS nombre, COALESCE(SUM(d.cantidad), 0) AS piezas, "
                        + "COALESCE(SUM(d.sub_total), 0) AS vendido, COALESCE(SUM(d.ganancia), 0) AS ganancia "
                        + "FROM detalle_venta_variantes d "
                        + "JOIN ventas v ON v.id = d.venta_id "
                        + "JOIN variantes a ON a.id = d.variante_id "
                        + "JOIN producto p ON p.id = a.producto_id "
                        + "WHERE d.fecha_venta BETWEEN ? AND ? AND v.estado_venta <> 'Devuelta' "
                        + "GROUP BY p.id, p.nombre) t ORDER BY ganancia DESC, producto_id LIMIT ?",
                (rs, n) -> new ProductoRentable(rs.getInt("producto_id"), rs.getString("nombre"), rs.getInt("piezas"),
                        rs.getDouble("vendido"), rs.getDouble("ganancia"), 0.0),
                java.sql.Date.valueOf(desde), java.sql.Date.valueOf(hasta), limite);
    }

    @Override
    @Transactional(readOnly = true)
    public double gananciaTotalDeRenglones(LocalDate desde, LocalDate hasta) {
        Double total = jdbc.queryForObject(
                "SELECT COALESCE(SUM(d.ganancia), 0) FROM detalle_venta_variantes d JOIN ventas v ON v.id = d.venta_id "
                        + "WHERE d.fecha_venta BETWEEN ? AND ? AND v.estado_venta <> 'Devuelta'",
                Double.class, java.sql.Date.valueOf(desde), java.sql.Date.valueOf(hasta));
        return total == null ? 0.0 : total;
    }
}
