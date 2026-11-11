package com.ventas.key.hexagonal.flete.infraestructura.salida.persistencia;

import com.ventas.key.hexagonal.flete.dominio.modelo.ParteAsignada;
import com.ventas.key.hexagonal.flete.dominio.modelo.RenglonDisponible;
import com.ventas.key.hexagonal.flete.dominio.puerto.salida.LigasDeVentaPort;
import com.ventas.key.hexagonal.preciocantidad.infraestructura.salida.persistencia.TablasDisponibles;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

/**
 * [Hexagonal: Driven Adapter] Liga las ventas a los viajes con JDBC (no con JPA): un error aquí no marca la transacción de la venta para deshacerse
 * (una excepción de JPA sí lo haría). Corre dentro de la misma transacción de la venta, así ve las líneas recién guardadas y bloquea con
 * {@code SELECT ... FOR UPDATE}. Sin la migración ({@code venta_detalle_lote}) no hace nada.
 */
@Component
public class LigasDeVentaJdbcAdapter implements LigasDeVentaPort {

    private final JdbcTemplate jdbc;
    private final TablasDisponibles tablas;

    public LigasDeVentaJdbcAdapter(JdbcTemplate jdbc, TablasDisponibles tablas) {
        this.jdbc = jdbc;
        this.tablas = tablas;
    }

    @Override
    public boolean disponible() {
        return tablas.existe("venta_detalle_lote") && tablas.existe("compra_lote_producto") && tablas.existe("compra_lote");
    }

    @Override
    public List<RenglonDisponible> bloquearYLeer(int productoId) {
        // Se bloquean primero los renglones del producto (en orden de id) y luego se leen con lo ya vendido.
        List<Integer> ids = jdbc.queryForList("SELECT id FROM compra_lote_producto WHERE producto_id = ? ORDER BY id FOR UPDATE", Integer.class, productoId);
        if (ids.isEmpty()) {
            return List.of();
        }
        return jdbc.query("""
                SELECT clp.id AS id, clp.piezas_compradas - COALESCE(x.vendidas, 0) AS restantes, clp.flete_por_pieza AS flete
                FROM compra_lote_producto clp
                JOIN compra_lote l ON l.id = clp.lote_id
                LEFT JOIN (
                    SELECT vdl.compra_lote_producto_id AS cid, SUM(vdl.cantidad) AS vendidas
                    FROM venta_detalle_lote vdl
                    JOIN detalle_venta_variantes d ON d.id = vdl.detalle_venta_variante_id
                    JOIN ventas ve ON ve.id = d.venta_id
                    WHERE COALESCE(ve.estado_venta, '') <> 'Devuelta'
                    GROUP BY vdl.compra_lote_producto_id
                ) x ON x.cid = clp.id
                WHERE clp.producto_id = ?
                ORDER BY l.fecha_compra, l.id, clp.id
                """,
                (rs, n) -> new RenglonDisponible(rs.getInt("id"), rs.getInt("restantes"), rs.getBigDecimal("flete")),
                productoId).stream().filter(r -> r.restantes() > 0).toList();
    }

    @Override
    public int existenciaActual(int productoId) {
        Integer s = jdbc.queryForObject("SELECT stock FROM producto WHERE id = ?", Integer.class, productoId);
        return s == null ? 0 : Math.max(0, s);
    }

    @Override
    public void guardar(int detalleVentaId, List<ParteAsignada> partes) {
        Timestamp ahora = Timestamp.valueOf(LocalDateTime.now());
        for (ParteAsignada p : partes) {
            jdbc.update("INSERT INTO venta_detalle_lote (detalle_venta_variante_id, compra_lote_producto_id, cantidad, flete_por_pieza, origen, fecha_creacion) "
                            + "VALUES (?, ?, ?, ?, 'AUTO', ?)",
                    detalleVentaId, p.renglonId(), p.cantidad(), p.fletePorPieza() == null ? BigDecimal.ZERO : p.fletePorPieza(), ahora);
        }
    }
}
