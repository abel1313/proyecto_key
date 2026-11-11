package com.ventas.key.hexagonal.flete.infraestructura.salida.persistencia;

import com.ventas.key.hexagonal.flete.dominio.modelo.VentaLigada;
import com.ventas.key.hexagonal.flete.dominio.puerto.salida.VentasDeViajePort;
import com.ventas.key.hexagonal.preciocantidad.infraestructura.salida.persistencia.TablasDisponibles;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;

/** [Hexagonal: Driven Adapter] Lee las piezas vendidas de un viaje. El total y la ganancia de la línea se reparten por piezas (una línea puede salir de dos viajes). */
@Component
public class VentasDeViajeJdbcAdapter implements VentasDeViajePort {

    private final JdbcTemplate jdbc;
    private final TablasDisponibles tablas;

    public VentasDeViajeJdbcAdapter(JdbcTemplate jdbc, TablasDisponibles tablas) {
        this.jdbc = jdbc;
        this.tablas = tablas;
    }

    @Override
    @Transactional(readOnly = true)
    public List<VentaLigada> deViaje(int viajeId) {
        if (!tablas.existe("venta_detalle_lote")) {
            return List.of();
        }
        return jdbc.query("""
                SELECT ve.id AS venta_id, d.id AS detalle_id, ve.fecha_venta AS fecha, clp.producto_id AS producto_id,
                       vdl.cantidad AS cantidad, vdl.flete_por_pieza AS flete,
                       d.sub_total * vdl.cantidad / d.cantidad AS total,
                       COALESCE(d.precio_costo, 0) * vdl.cantidad AS costo,
                       COALESCE(d.ganancia, 0) * vdl.cantidad / d.cantidad AS ganancia
                FROM venta_detalle_lote vdl
                JOIN compra_lote_producto clp ON clp.id = vdl.compra_lote_producto_id
                JOIN detalle_venta_variantes d ON d.id = vdl.detalle_venta_variante_id
                JOIN ventas ve ON ve.id = d.venta_id
                WHERE clp.lote_id = ? AND COALESCE(ve.estado_venta, '') <> 'Devuelta'
                """,
                (rs, n) -> {
                    Timestamp f = rs.getTimestamp("fecha");
                    return new VentaLigada(rs.getInt("venta_id"), rs.getInt("detalle_id"), f == null ? null : f.toLocalDateTime(), rs.getInt("producto_id"),
                            rs.getInt("cantidad"), nz(rs.getBigDecimal("flete")), nz(rs.getBigDecimal("total")), nz(rs.getBigDecimal("costo")), nz(rs.getBigDecimal("ganancia")));
                }, viajeId);
    }

    private static BigDecimal nz(BigDecimal b) {
        return b == null ? BigDecimal.ZERO : b;
    }
}
