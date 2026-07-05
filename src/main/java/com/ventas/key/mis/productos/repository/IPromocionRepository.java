package com.ventas.key.mis.productos.repository;

import com.ventas.key.mis.productos.entity.Promocion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
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
}
