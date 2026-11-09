package com.ventas.key.mis.productos.repository;

import com.ventas.key.mis.productos.entity.ProductoCalculoPrecioEntidad;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface IProductoCalculoPrecioRepository extends JpaRepository<ProductoCalculoPrecioEntidad, Integer> {

    Optional<ProductoCalculoPrecioEntidad> findFirstByProductoIdOrderByFechaDescIdDesc(Integer productoId);

    List<ProductoCalculoPrecioEntidad> findByProductoIdOrderByFechaDescIdDesc(Integer productoId, Pageable pageable);
}
