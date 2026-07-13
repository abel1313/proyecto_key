package com.ventas.key.mis.productos.repository;

import com.ventas.key.mis.productos.entity.Promocion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface IPromocionRepository extends BaseRepository<Promocion, Integer> {

    @Query("SELECT DISTINCT p FROM Promocion p LEFT JOIN FETCH p.detalles d LEFT JOIN FETCH d.variante WHERE p.id = :id")
    Optional<Promocion> findByIdConDetalle(@Param("id") Integer id);

    @Query(value = "SELECT DISTINCT p FROM Promocion p LEFT JOIN FETCH p.detalles d LEFT JOIN FETCH d.variante v LEFT JOIN FETCH v.producto ORDER BY p.id DESC",
           countQuery = "SELECT COUNT(p) FROM Promocion p")
    Page<Promocion> findAllConDetalle(Pageable pageable);

    @Query(value = "SELECT DISTINCT p FROM Promocion p LEFT JOIN FETCH p.detalles d LEFT JOIN FETCH d.variante v LEFT JOIN FETCH v.producto "
                  + "WHERE p.activo = true AND p.fechaVencimiento > :ahora ORDER BY p.fechaVencimiento ASC",
           countQuery = "SELECT COUNT(p) FROM Promocion p WHERE p.activo = true AND p.fechaVencimiento > :ahora")
    Page<Promocion> findActivasConDetalle(@Param("ahora") LocalDateTime ahora, Pageable pageable);

    // Por cada promocion, agrupa las ventas que la incluyeron (venta_id + promocion_id) y usa
    // MIN(cantidad DIV detalle.cantidad) como "numero de combos" de esa transaccion -- evita
    // contar de mas cuando el combo tiene varias piezas (cada pieza aporta su propia fila en
    // detalle_venta_variantes). Promociones sin ventas aparecen con 0 gracias al LEFT JOIN.
    @Query(value = """
        SELECT
            p.id                        AS promocionId,
            p.descripcion               AS descripcion,
            COALESCE(SUM(x.combos), 0)  AS combosVendidos,
            COUNT(x.venta_id)           AS numeroTransacciones,
            COALESCE(SUM(x.venta_linea), 0)   AS ventaTotal,
            COALESCE(SUM(x.ganancia_linea), 0) AS gananciaTotal,
            MAX(x.fecha_venta)          AS ultimaVenta
        FROM promocion p
        LEFT JOIN (
            SELECT
                dv.promocion_id                         AS promocion_id,
                dv.venta_id                              AS venta_id,
                MAX(dv.fecha_venta)                      AS fecha_venta,
                SUM(dv.sub_total)                        AS venta_linea,
                SUM(dv.ganancia)                         AS ganancia_linea,
                MIN(dv.cantidad DIV pd.cantidad)         AS combos
            FROM detalle_venta_variantes dv
            INNER JOIN promocion_detalle pd
                ON pd.promocion_id = dv.promocion_id AND pd.variante_id = dv.variante_id
            WHERE dv.promocion_id IS NOT NULL
              AND (:desde IS NULL OR dv.fecha_venta >= :desde)
              AND (:hasta IS NULL OR dv.fecha_venta <= :hasta)
            GROUP BY dv.promocion_id, dv.venta_id
        ) x ON x.promocion_id = p.id
        GROUP BY p.id, p.descripcion
        ORDER BY combosVendidos DESC
        """, nativeQuery = true)
    List<Object[]> reportePromociones(@Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);
}
