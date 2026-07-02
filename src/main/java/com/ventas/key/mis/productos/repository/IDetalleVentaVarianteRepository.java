package com.ventas.key.mis.productos.repository;

import com.ventas.key.mis.productos.entity.DetalleVentaVariante;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface IDetalleVentaVarianteRepository extends BaseRepository<DetalleVentaVariante, Integer> {

    @Query("SELECT dv.variante.id, dv.variante.producto.nombre, dv.variante.talla, dv.variante.color, " +
           "SUM(dv.cantidad), SUM(dv.subTotal) " +
           "FROM DetalleVentaVariante dv WHERE dv.fechaVenta BETWEEN :desde AND :hasta " +
           "GROUP BY dv.variante.id, dv.variante.producto.nombre, dv.variante.talla, dv.variante.color " +
           "ORDER BY SUM(dv.cantidad) DESC")
    List<Object[]> productosMasVendidos(
            @Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta, Pageable pageable);
}