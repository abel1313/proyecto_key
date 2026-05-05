package com.ventas.key.mis.productos.repository;

import com.ventas.key.mis.productos.entity.Producto;
import com.ventas.key.mis.productos.models.ProductoResumen;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IProductosRepository extends BaseRepository<Producto,Integer>{
    Page<Producto> findByNombreContainingAndHabilitado(String nombre, char habilitadot, Pageable pageable); // 🔍 Busca por nombre
    Page<Producto> findByNombreContaining(String nombre, Pageable pageable); // 🔍 Busca por nombre
    //Page<Producto> findByCodigoBarras_CodigoBarrasContainingAndHabilitado(String codigoBarras, char habilitadot, Pageable pageable); // 🔍 Busca por código de barras
    Page<Producto> findByCodigoBarras_CodigoBarrasContaining(String codigoBarras, Pageable pageable); // 🔍 Busca por código de barras
    Optional<Producto> findByCodigoBarras_CodigoBarrasAndNombre(String codigoBarras, String nombre); // 🔍 Busca por código de barras
    Optional<Producto> findByCodigoBarras_CodigoBarras(String codigoBarras); // 🔍 Busca por código de barras
    Optional<Producto> findByStockGreaterThanAndHabilitadoAndCodigoBarras_CodigoBarras(int stock, char habilitado, @Param("codigoBarras")String nombre); // 🔍 Busca por código de barras
    Page<Producto> findByStockGreaterThanAndHabilitadoAndCodigoBarras_CodigoBarrasContaining(int stock, char habilitado, @Param("codigoBarras")String nombre, Pageable pageable); // 🔍 Busca por código de barras
    Page<Producto> findDistinctByStockGreaterThanAndHabilitado(int stock, char habilitadot, Pageable pageable);


    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Producto p WHERE p.id = :id")
    Optional<Producto> findByIdWithLock(@Param("id") Integer id);

    @Query("""
    SELECT new com.ventas.key.mis.productos.models.ProductoResumen(
        p.id, p.nombre, p.descripcion, p.stock, p.precioVenta, cb.codigoBarras
    )
    FROM Producto p
    JOIN p.codigoBarras cb
    WHERE p.id = :id
""")
    ProductoResumen findProductoConImagenes(@Param("id") int id);

    @Query("SELECT p FROM Producto p WHERE p.habilitado <> '1'")
    Page<Producto> findProductosNoHabilitados(Pageable pageable);

    Page<Producto> findByStock(int stock, Pageable pageable);

    @Query("SELECT p FROM Producto p WHERE NOT EXISTS (SELECT v FROM Variantes v WHERE v.producto.id = p.id)")
    List<Producto> findProductosSinVariantes();

}
