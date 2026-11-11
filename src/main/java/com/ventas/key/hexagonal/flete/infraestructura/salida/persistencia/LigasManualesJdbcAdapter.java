package com.ventas.key.hexagonal.flete.infraestructura.salida.persistencia;

import com.ventas.key.hexagonal.flete.dominio.modelo.LigaManual;
import com.ventas.key.hexagonal.flete.dominio.modelo.LineaSinViaje;
import com.ventas.key.hexagonal.flete.dominio.modelo.RenglonParaLigar;
import com.ventas.key.hexagonal.flete.dominio.puerto.salida.LigasManualesPort;
import com.ventas.key.hexagonal.preciocantidad.infraestructura.salida.persistencia.TablasDisponibles;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** [Hexagonal: Driven Adapter] Ligar a mano ventas anteriores a un viaje, con JDBC (mismo estilo que {@link LigasDeVentaJdbcAdapter}). */
@Component
public class LigasManualesJdbcAdapter implements LigasManualesPort {

    /** Piezas ya ligadas por línea (todas, automáticas y manuales; las ventas devueltas no se incluyen en el filtro de abajo). */
    private static final String LINEAS = """
            SELECT d.id AS did, ve.id AS vid, ve.fecha_venta AS fecha, d.cantidad AS cantidad,
                   d.cantidad - COALESCE(x.lig, 0) AS pendientes, d.precio_unitario AS precio
            FROM detalle_venta_variantes d
            JOIN ventas ve ON ve.id = d.venta_id
            JOIN variantes va ON va.id = d.variante_id
            LEFT JOIN (SELECT detalle_venta_variante_id AS did, SUM(cantidad) AS lig FROM venta_detalle_lote GROUP BY detalle_venta_variante_id) x ON x.did = d.id
            WHERE va.producto_id = ? AND COALESCE(ve.estado_venta, '') <> 'Devuelta' AND d.cantidad - COALESCE(x.lig, 0) > 0
            """;

    private final JdbcTemplate jdbc;
    private final TablasDisponibles tablas;

    public LigasManualesJdbcAdapter(JdbcTemplate jdbc, TablasDisponibles tablas) {
        this.jdbc = jdbc;
        this.tablas = tablas;
    }

    @Override
    public boolean disponible() {
        return tablas.existe("venta_detalle_lote") && tablas.existe("compra_lote_producto") && tablas.existe("compra_lote");
    }

    @Override
    @Transactional(readOnly = true)
    public List<LineaSinViaje> lineasSinViaje(int productoId, LocalDate desde, LocalDate hasta) {
        StringBuilder sql = new StringBuilder(LINEAS);
        List<Object> params = new ArrayList<>();
        params.add(productoId);
        if (desde != null) {
            sql.append(" AND ve.fecha_venta >= ?");
            params.add(Timestamp.valueOf(desde.atStartOfDay()));
        }
        if (hasta != null) {
            sql.append(" AND ve.fecha_venta < ?");
            params.add(Timestamp.valueOf(hasta.plusDays(1).atStartOfDay()));
        }
        sql.append(" ORDER BY ve.fecha_venta, d.id LIMIT 500");
        return jdbc.query(sql.toString(), (rs, n) -> {
            Timestamp f = rs.getTimestamp("fecha");
            return new LineaSinViaje(rs.getInt("did"), rs.getInt("vid"), f == null ? null : f.toLocalDateTime(), rs.getInt("cantidad"),
                    rs.getInt("pendientes"), rs.getBigDecimal("precio"));
        }, params.toArray());
    }

    @Override
    public Optional<RenglonParaLigar> bloquearRenglon(int viajeId, int productoId) {
        List<Integer> ids = jdbc.queryForList("SELECT id FROM compra_lote_producto WHERE lote_id = ? AND producto_id = ? FOR UPDATE", Integer.class, viajeId, productoId);
        if (ids.isEmpty()) {
            return Optional.empty();
        }
        return jdbc.query("""
                SELECT clp.id AS id, l.nombre AS viaje, p.nombre AS producto, clp.flete_por_pieza AS flete,
                       clp.piezas_compradas - COALESCE((
                           SELECT SUM(vdl.cantidad) FROM venta_detalle_lote vdl
                           JOIN detalle_venta_variantes d ON d.id = vdl.detalle_venta_variante_id
                           JOIN ventas ve ON ve.id = d.venta_id
                           WHERE vdl.compra_lote_producto_id = clp.id AND COALESCE(ve.estado_venta, '') <> 'Devuelta'), 0) AS restantes
                FROM compra_lote_producto clp
                JOIN compra_lote l ON l.id = clp.lote_id
                JOIN producto p ON p.id = clp.producto_id
                WHERE clp.id = ?
                """, (rs, n) -> new RenglonParaLigar(rs.getInt("id"), rs.getString("viaje"), rs.getString("producto"), Math.max(0, rs.getInt("restantes")), rs.getBigDecimal("flete")),
                ids.get(0)).stream().findFirst();
    }

    @Override
    public Map<Integer, Integer> pendientesDe(int productoId, Collection<Integer> detalleVentaIds) {
        Map<Integer, Integer> r = new HashMap<>();
        if (detalleVentaIds == null || detalleVentaIds.isEmpty()) {
            return r;
        }
        String marcas = String.join(",", java.util.Collections.nCopies(detalleVentaIds.size(), "?"));
        List<Object> params = new ArrayList<>();
        params.add(productoId);
        params.addAll(detalleVentaIds);
        jdbc.query(LINEAS + " AND d.id IN (" + marcas + ")", rs -> {
            r.put(rs.getInt("did"), rs.getInt("pendientes"));
        }, params.toArray());
        return r;
    }

    @Override
    public void guardarManuales(int renglonId, BigDecimal fletePorPieza, List<LigaManual> ligas) {
        Timestamp ahora = Timestamp.valueOf(LocalDateTime.now());
        for (LigaManual l : ligas) {
            jdbc.update("INSERT INTO venta_detalle_lote (detalle_venta_variante_id, compra_lote_producto_id, cantidad, flete_por_pieza, origen, fecha_creacion) "
                    + "VALUES (?, ?, ?, ?, 'MANUAL', ?)", l.detalleVentaId(), renglonId, l.cantidad(), fletePorPieza == null ? BigDecimal.ZERO : fletePorPieza, ahora);
        }
    }

    @Override
    public boolean quitarManual(int ligaId) {
        return jdbc.update("DELETE FROM venta_detalle_lote WHERE id = ? AND origen = 'MANUAL'", ligaId) > 0;
    }
}
