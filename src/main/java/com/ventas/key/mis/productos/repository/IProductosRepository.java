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
public interface IProductosRepository extends BaseRepository<Producto, Integer> {

    // --- listado general ---
    Page<Producto> findDistinctByStockGreaterThanAndHabilitado(int stock, char habilitado, Pageable pageable);

    // --- búsqueda paso 1: código de barras exacto ---
    Optional<Producto> findByCodigoBarras_CodigoBarrasIgnoreCase(String codigoBarras);
    Optional<Producto> findByStockGreaterThanAndHabilitadoAndCodigoBarras_CodigoBarrasIgnoreCase(int stock, char habilitado, String codigoBarras);

    // --- búsqueda paso 2: palabra clave exacta ---
    Page<Producto> findByPalabraClave_NombreIgnoreCase(String nombre, Pageable pageable);
    Page<Producto> findByPalabraClave_NombreIgnoreCaseAndStockGreaterThanAndHabilitado(String nombre, int stock, char habilitado, Pageable pageable);

    // --- búsqueda paso 3: nombre contiene ---
    Page<Producto> findByNombreContaining(String nombre, Pageable pageable);
    Page<Producto> findByNombreContainingAndHabilitado(String nombre, char habilitado, Pageable pageable);

    // --- listado público: stock + habilitado + con imagen (cliente normal) ---
    @Query("SELECT p FROM Producto p WHERE p.stock > 0 AND p.habilitado = '1' " +
           "AND EXISTS (SELECT 1 FROM ProductoImagen pi WHERE pi.producto = p)")
    Page<Producto> findConStockYImagenPublico(Pageable pageable);

    @Query("SELECT p FROM Producto p WHERE p.stock > 0 AND p.habilitado = '1' " +
           "AND LOWER(p.codigoBarras.codigoBarras) = LOWER(:codigoBarras) " +
           "AND EXISTS (SELECT 1 FROM ProductoImagen pi WHERE pi.producto = p)")
    Optional<Producto> findByCodigoBarrasPublico(@Param("codigoBarras") String codigoBarras);

    @Query("SELECT p FROM Producto p WHERE p.stock > 0 AND p.habilitado = '1' " +
           "AND LOWER(p.palabraClave.nombre) = LOWER(:nombre) " +
           "AND EXISTS (SELECT 1 FROM ProductoImagen pi WHERE pi.producto = p)")
    Page<Producto> findByPalabraClavePublico(@Param("nombre") String nombre, Pageable pageable);

    @Query("SELECT p FROM Producto p WHERE p.stock > 0 AND p.habilitado = '1' " +
           "AND LOWER(p.nombre) LIKE LOWER(CONCAT('%', :nombre, '%')) " +
           "AND EXISTS (SELECT 1 FROM ProductoImagen pi WHERE pi.producto = p)")
    Page<Producto> findByNombrePublico(@Param("nombre") String nombre, Pageable pageable);

    // --- filtros de admin (ve todo, sin restriccion de stock/habilitado salvo el filtro elegido) ---
    Page<Producto> findByStockGreaterThan(int stock, Pageable pageable);

    @Query("SELECT p FROM Producto p WHERE EXISTS (SELECT 1 FROM ProductoImagen pi WHERE pi.producto = p)")
    Page<Producto> findConImagen(Pageable pageable);

    @Query("SELECT p FROM Producto p WHERE p.stock > 0 " +
           "AND EXISTS (SELECT 1 FROM ProductoImagen pi WHERE pi.producto = p)")
    Page<Producto> findConStockYImagenAdmin(Pageable pageable);

    // --- guardado ---
    Optional<Producto> findByCodigoBarras_CodigoBarrasAndNombre(String codigoBarras, String nombre);

    // --- detalle y admin ---
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Producto p WHERE p.id = :id")
    Optional<Producto> findByIdWithLock(@Param("id") Integer id);

    @Query("""
        SELECT new com.ventas.key.mis.productos.models.ProductoResumen(
            p.id, p.nombre, p.descripcion, p.stock, p.precioVenta, cb.codigoBarras, pk.id, pk.nombre
        )
        FROM Producto p
        JOIN p.codigoBarras cb
        LEFT JOIN p.palabraClave pk
        WHERE p.id = :id
    """)
    ProductoResumen findProductoConImagenes(@Param("id") int id);

    @Query("SELECT p FROM Producto p WHERE p.habilitado <> '1'")
    Page<Producto> findProductosNoHabilitados(Pageable pageable);

    Page<Producto> findByStock(int stock, Pageable pageable);

    @Query("SELECT p FROM Producto p WHERE NOT EXISTS (SELECT v FROM Variantes v WHERE v.producto.id = p.id)")
    List<Producto> findProductosSinVariantes();
}