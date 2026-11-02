package com.ventas.key.hexagonal.revisionventas.infraestructura.salida.persistencia;

import com.ventas.key.hexagonal.revisionventas.dominio.excepcion.RevisionVentasException;
import com.ventas.key.hexagonal.revisionventas.dominio.modelo.BusquedaDeHallazgos;
import com.ventas.key.hexagonal.revisionventas.dominio.modelo.Hallazgo;
import com.ventas.key.hexagonal.revisionventas.dominio.modelo.TipoHallazgo;
import com.ventas.key.hexagonal.revisionventas.dominio.puerto.salida.HallazgosPort;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * [Hexagonal: Driven Adapter] [Clean: Interface Adapter]
 *
 * <p>Las consultas de la Revisión de ventas. SOLO {@code SELECT}: nunca modifica nada. SQL estándar (se prueba con H2
 * y corre en MySQL). Cada tipo es una consulta interna; el total sale de contarla y los ejemplos de las primeras filas.
 * Si falta una tabla o columna (producción sin una migración) lanza {@code RevisionNoDisponible} para ese tipo.
 */
@Component
public class HallazgosJdbcAdapter implements HallazgosPort {

    /** Una forma de buscar un tipo: su consulta, con qué parámetros, cómo ordenar y cómo contar el caso en una frase. */
    private record Consulta(String sql, ParametrosPorFecha parametros, String ordenarPor, RowMapper<Hallazgo> aHallazgo) {
    }

    private interface ParametrosPorFecha {
        Object[] de(LocalDate desde, LocalDate hasta);
    }

    private static final ParametrosPorFecha SIN_FECHAS = (d, h) -> new Object[0];
    private static final ParametrosPorFecha FECHA_DEL_PEDIDO = (d, h) -> new Object[]{Date.valueOf(d), Date.valueOf(h)};
    private static final ParametrosPorFecha FECHA_DE_LA_VENTA =
            (d, h) -> new Object[]{Timestamp.valueOf(d.atStartOfDay()), Timestamp.valueOf(h.plusDays(1).atStartOfDay())};

    /** Días que un pedido pagado puede esperar sin entregarse antes de avisar. */
    static final int DIAS_PARA_AVISAR_SIN_ENTREGAR = 14;
    private static final ParametrosPorFecha PEDIDO_VIEJO =
            (d, h) -> new Object[]{Date.valueOf(LocalDate.now().minusDays(DIAS_PARA_AVISAR_SIN_ENTREGAR))};

    private final JdbcTemplate jdbc;
    private final Map<TipoHallazgo, Consulta> consultas = new EnumMap<>(TipoHallazgo.class);

    public HallazgosJdbcAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;

        consultas.put(TipoHallazgo.PAGADO_SIN_VENTA, new Consulta(
                "SELECT p.id AS pedido_id, p.total_pedido AS monto, p.estado_pedido AS estado FROM pedidos p "
                        + "WHERE UPPER(p.estado_pedido) IN ('ENTREGADO', 'PAGADO') "
                        + "AND p.fecha_pedido BETWEEN ? AND ? "
                        + "AND NOT EXISTS (SELECT 1 FROM ventas v WHERE v.pedido_id = p.id)",
                FECHA_DEL_PEDIDO, "pedido_id",
                (rs, n) -> new Hallazgo(TipoHallazgo.PAGADO_SIN_VENTA, rs.getInt("pedido_id"), null, null,
                        "Pedido #" + rs.getInt("pedido_id") + " (" + rs.getString("estado") + ") por "
                                + pesos(rs.getDouble("monto")) + " no tiene venta.")));

        consultas.put(TipoHallazgo.TOTAL_PEDIDO_NO_CUADRA, new Consulta(
                "SELECT p.id AS pedido_id, COALESCE(p.total_pedido, 0) AS guardado, d.suma AS suma FROM pedidos p "
                        + "JOIN (SELECT pedido_id, SUM(sub_total) AS suma FROM detalle_pedidos GROUP BY pedido_id) d ON d.pedido_id = p.id "
                        + "WHERE UPPER(COALESCE(p.estado_pedido, '')) <> 'CANCELADO' "
                        + "AND p.fecha_pedido BETWEEN ? AND ? "
                        + "AND ABS(COALESCE(p.total_pedido, 0) - d.suma) > 0.01",
                FECHA_DEL_PEDIDO, "pedido_id",
                (rs, n) -> new Hallazgo(TipoHallazgo.TOTAL_PEDIDO_NO_CUADRA, rs.getInt("pedido_id"), null, null,
                        "Pedido #" + rs.getInt("pedido_id") + ": el total guardado es " + pesos(rs.getDouble("guardado"))
                                + " pero sus artículos suman " + pesos(rs.getDouble("suma")) + ".")));

        consultas.put(TipoHallazgo.TOTAL_VENTA_NO_CUADRA, new Consulta(
                "SELECT v.id AS venta_id, v.pedido_id AS pedido_id, COALESCE(v.total_venta, 0) AS guardado, d.suma AS suma FROM ventas v "
                        + "JOIN (SELECT venta_id, SUM(sub_total) AS suma FROM detalle_venta_variantes GROUP BY venta_id) d ON d.venta_id = v.id "
                        + "WHERE v.fecha_venta >= ? AND v.fecha_venta < ? "
                        + "AND ABS(COALESCE(v.total_venta, 0) - d.suma) > 0.01",
                FECHA_DE_LA_VENTA, "venta_id",
                (rs, n) -> new Hallazgo(TipoHallazgo.TOTAL_VENTA_NO_CUADRA, entero(rs, "pedido_id"), rs.getInt("venta_id"), null,
                        "Venta #" + rs.getInt("venta_id") + ": el total guardado es " + pesos(rs.getDouble("guardado"))
                                + " pero sus artículos suman " + pesos(rs.getDouble("suma")) + ".")));

        consultas.put(TipoHallazgo.ABONOS_NO_CUADRAN, new Consulta(
                "SELECT p.id AS pedido_id, p.tipo_pedido AS tipo, COALESCE(p.total_pagado, 0) AS pagado, COALESCE(a.suma, 0) AS abonos FROM pedidos p "
                        + "LEFT JOIN (SELECT pedido_id, SUM(monto) AS suma FROM abono_pedido GROUP BY pedido_id) a ON a.pedido_id = p.id "
                        + "WHERE p.tipo_pedido IN ('FIADO', 'APARTADO') "
                        + "AND UPPER(COALESCE(p.estado_pedido, '')) <> 'CANCELADO' "
                        + "AND p.fecha_pedido BETWEEN ? AND ? "
                        + "AND ABS(COALESCE(p.total_pagado, 0) - COALESCE(a.suma, 0)) > 0.01",
                FECHA_DEL_PEDIDO, "pedido_id",
                (rs, n) -> new Hallazgo(TipoHallazgo.ABONOS_NO_CUADRAN, rs.getInt("pedido_id"), null, null,
                        "Pedido #" + rs.getInt("pedido_id") + " (" + etiqueta(rs.getString("tipo")) + "): dice que lleva pagado "
                                + pesos(rs.getDouble("pagado")) + " pero sus abonos suman " + pesos(rs.getDouble("abonos")) + ".")));

        consultas.put(TipoHallazgo.COBRO_TARJETA_SIN_APLICAR, new Consulta(
                "SELECT i.id AS intento_id, i.pedido_id AS pedido_id, i.monto AS monto, i.estado AS estado FROM pago_tarjeta_intento i "
                        + "JOIN pedidos p ON p.id = i.pedido_id "
                        + "WHERE i.estado = 'COBRADO_SIN_APLICAR' "
                        + "OR (i.estado = 'APROBADO' AND UPPER(COALESCE(p.estado_pedido, '')) NOT IN ('ENTREGADO', 'PAGADO'))",
                SIN_FECHAS, "intento_id",
                (rs, n) -> new Hallazgo(TipoHallazgo.COBRO_TARJETA_SIN_APLICAR, rs.getInt("pedido_id"), null, null,
                        "Cobro #" + rs.getInt("intento_id") + " de " + pesos(rs.getDouble("monto")) + " (" + rs.getString("estado")
                                + ") del pedido #" + rs.getInt("pedido_id") + " no quedó aplicado al pedido.")));

        consultas.put(TipoHallazgo.STOCK_NEGATIVO, new Consulta(
                "SELECT v.id AS articulo_id, p.id AS modelo_id, p.nombre AS nombre, v.stock AS stock, 'artículo' AS donde "
                        + "FROM variantes v JOIN producto p ON p.id = v.producto_id WHERE v.stock < 0 "
                        + "UNION ALL "
                        + "SELECT NULL AS articulo_id, p.id AS modelo_id, p.nombre AS nombre, p.stock AS stock, 'modelo' AS donde "
                        + "FROM producto p WHERE p.stock < 0",
                SIN_FECHAS, "modelo_id",
                (rs, n) -> new Hallazgo(TipoHallazgo.STOCK_NEGATIVO, null, null, entero(rs, "articulo_id"),
                        "El " + rs.getString("donde") + " de «" + rs.getString("nombre") + "» (modelo #" + rs.getInt("modelo_id")
                                + (entero(rs, "articulo_id") != null ? ", artículo #" + rs.getInt("articulo_id") : "")
                                + ") tiene stock " + rs.getInt("stock") + ".")));

        consultas.put(TipoHallazgo.ARTICULO_CON_STOCK_SIN_MODELO, new Consulta(
                "SELECT v.id AS articulo_id, p.id AS modelo_id, p.nombre AS nombre, v.talla AS talla, v.color AS color, "
                        + "v.stock AS stock_articulo, COALESCE(p.stock, 0) AS stock_modelo "
                        + "FROM variantes v JOIN producto p ON p.id = v.producto_id "
                        + "WHERE v.habilitado = '1' AND p.habilitado = '1' AND p.es_catalogo_interno = 0 "
                        + "AND v.stock > 0 AND COALESCE(p.stock, 0) < 1",
                SIN_FECHAS, "articulo_id",
                (rs, n) -> new Hallazgo(TipoHallazgo.ARTICULO_CON_STOCK_SIN_MODELO, null, null, rs.getInt("articulo_id"),
                        nombreDelArticulo(rs) + " (artículo #" + rs.getInt("articulo_id") + "): tiene " + rs.getInt("stock_articulo")
                                + " pieza(s) pero su modelo #" + rs.getInt("modelo_id") + " tiene " + rs.getInt("stock_modelo") + ".")));

        consultas.put(TipoHallazgo.VENTA_SIN_PEDIDO, new Consulta(
                "SELECT v.id AS venta_id, COALESCE(v.total_venta, 0) AS monto FROM ventas v "
                        + "WHERE v.pedido_id IS NULL AND v.fecha_venta >= ? AND v.fecha_venta < ?",
                FECHA_DE_LA_VENTA, "venta_id",
                (rs, n) -> new Hallazgo(TipoHallazgo.VENTA_SIN_PEDIDO, null, rs.getInt("venta_id"), null,
                        "Venta #" + rs.getInt("venta_id") + " por " + pesos(rs.getDouble("monto")) + " no está ligada a ningún pedido.")));

        consultas.put(TipoHallazgo.VENDIDO_BAJO_COSTO, new Consulta(
                "SELECT d.id AS detalle_id, d.venta_id AS venta_id, d.precio_unitario AS precio, d.precio_costo AS costo, d.cantidad AS cantidad "
                        + "FROM detalle_venta_variantes d "
                        + "WHERE d.precio_costo > 0 AND d.precio_unitario < d.precio_costo AND d.fecha_venta BETWEEN ? AND ?",
                FECHA_DEL_PEDIDO, "detalle_id",
                (rs, n) -> new Hallazgo(TipoHallazgo.VENDIDO_BAJO_COSTO, null, rs.getInt("venta_id"), null,
                        "Venta #" + rs.getInt("venta_id") + ": " + rs.getInt("cantidad") + " pieza(s) a " + pesos(rs.getDouble("precio"))
                                + " cada una, pero costaron " + pesos(rs.getDouble("costo")) + ".")));

        consultas.put(TipoHallazgo.ARTICULO_SIN_PRECIO, new Consulta(
                "SELECT v.id AS articulo_id, p.id AS modelo_id, p.nombre AS nombre, v.talla AS talla, v.color AS color, v.stock AS stock_articulo "
                        + "FROM variantes v JOIN producto p ON p.id = v.producto_id "
                        + "WHERE v.habilitado = '1' AND p.habilitado = '1' AND p.es_catalogo_interno = 0 "
                        + "AND v.stock > 0 AND (v.precio_venta IS NULL OR v.precio_venta <= 0)",
                SIN_FECHAS, "articulo_id",
                (rs, n) -> new Hallazgo(TipoHallazgo.ARTICULO_SIN_PRECIO, null, null, rs.getInt("articulo_id"),
                        nombreDelArticulo(rs) + " (artículo #" + rs.getInt("articulo_id") + "): tiene " + rs.getInt("stock_articulo")
                                + " pieza(s) y no tiene precio de venta.")));

        consultas.put(TipoHallazgo.PEDIDO_PAGADO_SIN_ENTREGAR, new Consulta(
                "SELECT p.id AS pedido_id, p.total_pedido AS monto, p.fecha_pedido AS fecha FROM pedidos p "
                        + "WHERE UPPER(p.estado_pedido) = 'PAGADO' AND p.entregado = FALSE AND p.fecha_pedido <= ?",
                PEDIDO_VIEJO, "pedido_id",
                (rs, n) -> new Hallazgo(TipoHallazgo.PEDIDO_PAGADO_SIN_ENTREGAR, rs.getInt("pedido_id"), null, null,
                        "Pedido #" + rs.getInt("pedido_id") + " por " + pesos(rs.getDouble("monto")) + " (del " + rs.getDate("fecha")
                                + ") está pagado y sigue sin entregarse.")));
    }

    @Override
    @Transactional(readOnly = true)
    public BusquedaDeHallazgos buscar(TipoHallazgo tipo, LocalDate desde, LocalDate hasta, int limiteEjemplos) {
        Consulta c = consultas.get(tipo);
        if (c == null) {
            throw new RevisionVentasException.RevisionNoDisponible("No hay consulta para " + tipo);
        }
        Object[] fechas = c.parametros().de(desde, hasta);
        try {
            Integer total = jdbc.queryForObject("SELECT COUNT(*) FROM (" + c.sql() + ") t", Integer.class, fechas);
            List<Hallazgo> ejemplos = new ArrayList<>();
            if (total != null && total > 0 && limiteEjemplos > 0) {
                Object[] conLimite = java.util.Arrays.copyOf(fechas, fechas.length + 1);
                conLimite[fechas.length] = limiteEjemplos;
                ejemplos = jdbc.query("SELECT * FROM (" + c.sql() + ") t ORDER BY " + c.ordenarPor() + " DESC LIMIT ?",
                        c.aHallazgo(), conLimite);
            }
            return new BusquedaDeHallazgos(total == null ? 0 : total, ejemplos);
        } catch (BadSqlGrammarException e) {
            // Falta una tabla o columna en este ambiente (por ejemplo producción sin la migración del cobro con tarjeta).
            throw new RevisionVentasException.RevisionNoDisponible("No se pudo revisar " + tipo + ": " + e.getMostSpecificCause().getMessage());
        }
    }

    private static String pesos(double monto) {
        return String.format(Locale.US, "$%,.2f", monto);
    }

    private static Integer entero(ResultSet rs, String columna) throws SQLException {
        int valor = rs.getInt(columna);
        return rs.wasNull() ? null : valor;
    }

    private static String etiqueta(String tipoPedido) {
        return "APARTADO".equals(tipoPedido) ? "Apartado" : "Ir pagando";
    }

    private static String nombreDelArticulo(ResultSet rs) throws SQLException {
        StringBuilder sb = new StringBuilder(rs.getString("nombre") != null ? rs.getString("nombre") : "Artículo");
        String talla = rs.getString("talla");
        String color = rs.getString("color");
        if (talla != null && !talla.isBlank()) sb.append(" talla ").append(talla);
        if (color != null && !color.isBlank()) sb.append(' ').append(color);
        return sb.toString();
    }
}
