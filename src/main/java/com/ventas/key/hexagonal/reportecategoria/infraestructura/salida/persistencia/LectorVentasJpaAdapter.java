package com.ventas.key.hexagonal.reportecategoria.infraestructura.salida.persistencia;

import com.ventas.key.hexagonal.reportecategoria.dominio.modelo.FilaBase;
import com.ventas.key.hexagonal.reportecategoria.dominio.modelo.FiltroReporte;
import com.ventas.key.hexagonal.reportecategoria.dominio.puerto.salida.LectorVentasPort;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * [Hexagonal: Driven Adapter] Un solo SELECT: todos los productos del filtro (menos los del catálogo interno) y, a su lado, lo vendido en las
 * fechas. Una venta cuenta igual que en los reportes Diario y Mensual: por su fecha de venta y sin las «Devuelta» (las canceladas).
 * Las piezas de varias ventas del mismo producto se SUMAN.
 */
@Component
public class LectorVentasJpaAdapter implements LectorVentasPort {

    @PersistenceContext
    private EntityManager em;

    @Override
    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public List<FilaBase> leer(FiltroReporte f) {
        StringBuilder sql = new StringBuilder("""
                SELECT p.id, p.nombre, cb.codigo_barras, pk.nombre,
                       p.precio_costo, p.precio_venta, p.precio_rebaja, camp.precio_campana, p.stock,
                       COALESCE(v.piezas, 0), COALESCE(v.ventas, 0), COALESCE(v.total, 0), COALESCE(v.costo, 0), COALESCE(v.ganancia, 0),
                       COALESCE(v.pz_venta, 0), COALESCE(v.pz_rebaja, 0), COALESCE(v.pz_campana, 0), COALESCE(v.pz_otro, 0),
                       COALESCE(v.m_venta, 0), COALESCE(v.m_rebaja, 0), COALESCE(v.m_campana, 0), COALESCE(v.m_otro, 0), COALESCE(v.pz_estimadas, 0)
                FROM producto p
                LEFT JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
                LEFT JOIN palabra_clave pk ON pk.id = p.palabra_clave_id
                LEFT JOIN (
                    SELECT t.pid, SUM(t.cantidad) AS piezas, COUNT(DISTINCT t.venta_id) AS ventas,
                           SUM(t.sub_total) AS total, SUM(t.cantidad * t.precio_costo) AS costo, SUM(t.ganancia) AS ganancia,
                           SUM(CASE WHEN t.tipo = 'VENTA' THEN t.cantidad ELSE 0 END) AS pz_venta,
                           SUM(CASE WHEN t.tipo = 'REBAJA' THEN t.cantidad ELSE 0 END) AS pz_rebaja,
                           SUM(CASE WHEN t.tipo = 'CAMPANA' THEN t.cantidad ELSE 0 END) AS pz_campana,
                           SUM(CASE WHEN t.tipo NOT IN ('VENTA', 'REBAJA', 'CAMPANA') THEN t.cantidad ELSE 0 END) AS pz_otro,
                           SUM(CASE WHEN t.tipo = 'VENTA' THEN t.sub_total ELSE 0 END) AS m_venta,
                           SUM(CASE WHEN t.tipo = 'REBAJA' THEN t.sub_total ELSE 0 END) AS m_rebaja,
                           SUM(CASE WHEN t.tipo = 'CAMPANA' THEN t.sub_total ELSE 0 END) AS m_campana,
                           SUM(CASE WHEN t.tipo NOT IN ('VENTA', 'REBAJA', 'CAMPANA') THEN t.sub_total ELSE 0 END) AS m_otro,
                           SUM(CASE WHEN t.exacto = 0 THEN t.cantidad ELSE 0 END) AS pz_estimadas
                    FROM (
                        SELECT va.producto_id AS pid, d.venta_id, d.cantidad, d.sub_total, d.precio_costo, d.ganancia,
                               CASE WHEN d.tipo_precio IS NULL THEN 0 ELSE 1 END AS exacto,
                               COALESCE(d.tipo_precio,
                                   CASE WHEN d.promocion_id IS NOT NULL THEN 'PROMOCION'
                                        WHEN (CASE WHEN va.precio_venta IS NOT NULL THEN va.precio_rebaja ELSE pr.precio_rebaja END) > 0
                                             AND (CASE WHEN va.precio_venta IS NOT NULL THEN va.precio_rebaja ELSE pr.precio_rebaja END)
                                                 < (CASE WHEN va.precio_venta IS NOT NULL THEN va.precio_venta ELSE pr.precio_venta END)
                                             AND ABS(d.precio_unitario - (CASE WHEN va.precio_venta IS NOT NULL THEN va.precio_rebaja ELSE pr.precio_rebaja END)) < 0.01
                                             THEN 'REBAJA'
                                        WHEN ABS(d.precio_unitario - (CASE WHEN va.precio_venta IS NOT NULL THEN va.precio_venta ELSE pr.precio_venta END)) < 0.01 THEN 'VENTA'
                                        WHEN cpe.precio_campana IS NOT NULL AND ABS(d.precio_unitario - cpe.precio_campana) < 0.01 THEN 'CAMPANA'
                                        ELSE 'OTRO' END) AS tipo
                        FROM detalle_venta_variantes d
                        JOIN ventas ve ON ve.id = d.venta_id
                        JOIN variantes va ON va.id = d.variante_id
                        JOIN producto pr ON pr.id = va.producto_id
                        LEFT JOIN (
                            SELECT cp.producto_id AS pid, cp.precio_campana AS precio_campana
                            FROM campana_producto cp
                            WHERE cp.id = (SELECT MAX(cp2.id) FROM campana_producto cp2 WHERE cp2.producto_id = cp.producto_id)
                        ) cpe ON cpe.pid = pr.id
                        WHERE ve.fecha_venta >= :desde AND ve.fecha_venta < :hastaExcl AND COALESCE(ve.estado_venta, '') <> 'Devuelta'
                    ) t
                    GROUP BY t.pid
                ) v ON v.pid = p.id
                LEFT JOIN (
                    SELECT cp.producto_id AS pid, cp.precio_campana AS precio_campana
                    FROM campana_producto cp
                    WHERE cp.id = (SELECT MAX(cp2.id) FROM campana_producto cp2 WHERE cp2.producto_id = cp.producto_id)
                ) camp ON camp.pid = p.id
                WHERE COALESCE(p.es_catalogo_interno, 0) = 0
                """);
        if (f.porProductos()) {
            sql.append(" AND p.id IN (:ids)");
        } else if (f.categoria() != null) {
            sql.append(" AND UPPER(pk.nombre) = UPPER(:categoria)");
        } else if (f.sinCategoria()) {
            sql.append(" AND p.palabra_clave_id IS NULL");
        }
        sql.append(" ORDER BY p.id");

        Query q = em.createNativeQuery(sql.toString());
        q.setParameter("desde", f.desde().atStartOfDay());
        q.setParameter("hastaExcl", f.hasta().plusDays(1).atStartOfDay());
        if (f.porProductos()) q.setParameter("ids", f.productoIds());
        else if (f.categoria() != null) q.setParameter("categoria", f.categoria());

        List<Object[]> filas = q.getResultList();
        List<FilaBase> salida = new ArrayList<>(filas.size());
        for (Object[] r : filas) {
            salida.add(new FilaBase(
                    ((Number) r[0]).intValue(), (String) r[1], (String) r[2], (String) r[3],
                    dinero(r[4]), dinero(r[5]), dinero(r[6]), dinero(r[7]),
                    r[8] == null ? 0 : ((Number) r[8]).intValue(),
                    ((Number) r[9]).longValue(), ((Number) r[10]).longValue(),
                    dinero0(r[11]), dinero0(r[12]), dinero0(r[13]),
                    ((Number) r[14]).longValue(), ((Number) r[15]).longValue(), ((Number) r[16]).longValue(), ((Number) r[17]).longValue(),
                    dinero0(r[18]), dinero0(r[19]), dinero0(r[20]), dinero0(r[21]), ((Number) r[22]).longValue()));
        }
        return salida;
    }

    private static BigDecimal dinero(Object o) {
        if (o == null) return null;
        BigDecimal b = o instanceof BigDecimal bd ? bd : BigDecimal.valueOf(((Number) o).doubleValue());
        return b.setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal dinero0(Object o) {
        BigDecimal b = dinero(o);
        return b == null ? BigDecimal.ZERO.setScale(2) : b;
    }
}
