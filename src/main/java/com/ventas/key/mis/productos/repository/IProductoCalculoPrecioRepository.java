package com.ventas.key.mis.productos.repository;

import com.ventas.key.mis.productos.entity.ProductoCalculoPrecioEntidad;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

import java.util.List;
import java.util.Optional;

public interface IProductoCalculoPrecioRepository extends JpaRepository<ProductoCalculoPrecioEntidad, Integer> {

    Optional<ProductoCalculoPrecioEntidad> findFirstByProductoIdOrderByFechaDescIdDesc(Integer productoId);

    List<ProductoCalculoPrecioEntidad> findByProductoIdOrderByFechaDescIdDesc(Integer productoId, Pageable pageable);

    /** El último cálculo (el de id mayor) de cada producto, con su nombre y precio de venta de hoy. Cada fila: [cálculo, nombre, precioVenta]. */
    @Query(value = "select c, p.nombre, p.precioVenta from ProductoCalculoPrecioEntidad c, Producto p "
            + "where p.id = c.productoId "
            + "and c.id = (select max(x.id) from ProductoCalculoPrecioEntidad x where x.productoId = c.productoId) "
            + "and lower(p.nombre) like lower(concat('%', :nombre, '%')) "
            + "and c.fecha >= :desde and c.fecha < :hasta "
            + "order by c.fecha desc, c.id desc",
            countQuery = "select count(c) from ProductoCalculoPrecioEntidad c, Producto p "
            + "where p.id = c.productoId "
            + "and c.id = (select max(x.id) from ProductoCalculoPrecioEntidad x where x.productoId = c.productoId) "
            + "and lower(p.nombre) like lower(concat('%', :nombre, '%')) "
            + "and c.fecha >= :desde and c.fecha < :hasta")
    Page<Object[]> ultimosPorProducto(@Param("nombre") String nombre, @Param("desde") LocalDateTime desde,
                                      @Param("hasta") LocalDateTime hasta, Pageable pageable);
}
