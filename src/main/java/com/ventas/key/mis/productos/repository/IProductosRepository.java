package com.ventas.key.mis.productos.repository;

import java.util.List;
import java.util.Optional;

import com.ventas.key.mis.productos.models.ProductoResumen;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ventas.key.mis.productos.entity.Producto;

@Repository
public interface IProductosRepository extends BaseRepository<Producto,Integer>{
    Page<Producto> findByNombreContaining(String nombre, Pageable pageable); // 🔍 Busca por nombre
    Page<Producto> findByCodigoBarras_CodigoBarrasContaining(String codigoBarras, Pageable pageable); // 🔍 Busca por código de barras
    Optional<Producto> findByCodigoBarras_CodigoBarrasAndNombre(String codigoBarras, String nombre); // 🔍 Busca por código de barras
    Optional<Producto> findByCodigoBarras_CodigoBarras(String codigoBarras); // 🔍 Busca por código de barras
    Page<Producto> findByStockGreaterThan(int stock, Pageable pageable);


    @Query("""
    SELECT new com.ventas.key.mis.productos.models.ProductoResumen(
        p.id, p.nombre, p.descripcion, p.stock, p.precioVenta, cb.codigoBarras
    )
    FROM Producto p
    JOIN p.codigoBarras cb
    WHERE p.id = :id
""")
    ProductoResumen findProductoConImagenes(@Param("id") int id);

    


}
