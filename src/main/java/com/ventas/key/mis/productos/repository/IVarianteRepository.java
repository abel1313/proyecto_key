package com.ventas.key.mis.productos.repository;


import com.ventas.key.mis.productos.entity.productoVariantes.Variantes;
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
public interface IVarianteRepository extends BaseRepository<Variantes, Integer> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT v FROM Variantes v WHERE v.id = :id")
    Optional<Variantes> findByIdWithLock(@Param("id") Integer id);

    List<Variantes> findByProductoId(Integer productoId);

    Page<Variantes> findByProductoId(Integer productoId, Pageable pageable);

    List<Variantes> findByProductoNombreContainingIgnoreCase(String nombre);
    Page<Variantes> findByProductoNombreContainingIgnoreCase(String nombre, Pageable pageable);
    Page<Variantes> findByStockGreaterThanAndProducto_HabilitadoAndProducto_NombreContainingIgnoreCase(int stock, char habilitado, String nombre, Pageable pageable);

    List<Variantes> findByProductoCodigoBarrasCodigoBarras(String codigoBarras);
    Page<Variantes> findByProductoCodigoBarrasCodigoBarras(String codigoBarras, Pageable pageable);
    Page<Variantes> findByStockGreaterThanAndProducto_Habilitado(int stock, char habilitado, Pageable pageable);
    Page<Variantes> findByStockGreaterThanAndProducto_HabilitadoAndProducto_CodigoBarras_CodigoBarrasContaining(int stock, char habilitado, String codigoBarras, Pageable pageable);

    Page<Variantes> findByStockGreaterThanAndProductoHabilitado(int stock, char habilitado, Pageable pageable);

    @Query("SELECT v FROM Variantes v WHERE v.stock = 0 AND v.producto.habilitado <> '1'")
    Page<Variantes> findVariantesSinStockDeshabilitadas(Pageable pageable);

    // Stock bajo = todavía se puede vender (stock > 0) pero se está agotando.
    // No incluye stock = 0 — eso ya se cubre en findVariantesSinStockDeshabilitadas.
    @Query("SELECT COUNT(v) FROM Variantes v WHERE v.stock > 0 AND v.stock < :umbral AND v.producto.habilitado = '1'")
    long countStockBajo(@Param("umbral") int umbral);

    // --- búsqueda por palabra clave ---
    Page<Variantes> findByPalabraClave_NombreIgnoreCase(String nombre, Pageable pageable);
    Page<Variantes> findByStockGreaterThanAndProducto_HabilitadoAndPalabraClave_NombreIgnoreCase(int stock, char habilitado, String nombre, Pageable pageable);

    // --- listado público: stock + habilitado + con imagen (cliente normal) ---
    @Query("SELECT v FROM Variantes v WHERE v.stock > 0 AND v.producto.habilitado = '1' " +
           "AND EXISTS (SELECT 1 FROM VarianteImagen vi WHERE vi.variante = v)")
    Page<Variantes> findConStockYImagenPublico(Pageable pageable);

    @Query("SELECT v FROM Variantes v WHERE v.stock > 0 AND v.producto.habilitado = '1' " +
           "AND v.producto.codigoBarras.codigoBarras LIKE CONCAT('%', :codigoBarras, '%') " +
           "AND EXISTS (SELECT 1 FROM VarianteImagen vi WHERE vi.variante = v)")
    Page<Variantes> findByCodigoBarrasPublico(@Param("codigoBarras") String codigoBarras, Pageable pageable);

    @Query("SELECT v FROM Variantes v WHERE v.stock > 0 AND v.producto.habilitado = '1' " +
           "AND LOWER(v.palabraClave.nombre) = LOWER(:nombre) " +
           "AND EXISTS (SELECT 1 FROM VarianteImagen vi WHERE vi.variante = v)")
    Page<Variantes> findByPalabraClavePublico(@Param("nombre") String nombre, Pageable pageable);

    @Query("SELECT v FROM Variantes v WHERE v.stock > 0 AND v.producto.habilitado = '1' " +
           "AND LOWER(v.producto.nombre) LIKE LOWER(CONCAT('%', :nombre, '%')) " +
           "AND EXISTS (SELECT 1 FROM VarianteImagen vi WHERE vi.variante = v)")
    Page<Variantes> findByNombrePublico(@Param("nombre") String nombre, Pageable pageable);

    // --- filtros de admin (ve todo, sin restriccion de habilitado salvo el filtro elegido) ---
    Page<Variantes> findByStock(int stock, Pageable pageable);
    Page<Variantes> findByStockGreaterThan(int stock, Pageable pageable);

    @Query("SELECT v FROM Variantes v WHERE EXISTS (SELECT 1 FROM VarianteImagen vi WHERE vi.variante = v)")
    Page<Variantes> findConImagen(Pageable pageable);

    @Query("SELECT v FROM Variantes v WHERE v.stock > 0 " +
           "AND EXISTS (SELECT 1 FROM VarianteImagen vi WHERE vi.variante = v)")
    Page<Variantes> findConStockYImagenAdmin(Pageable pageable);

    // --- búsqueda para chatbot: por nombre de producto, marca o palabra clave ---
    @Query(value = "SELECT v FROM Variantes v LEFT JOIN v.palabraClave pc " +
                   "WHERE v.stock > 0 AND v.producto.habilitado = '1' " +
                   "AND (LOWER(v.producto.nombre) LIKE LOWER(CONCAT('%', :q, '%')) " +
                   "OR LOWER(v.marca) LIKE LOWER(CONCAT('%', :q, '%')) " +
                   "OR (pc IS NOT NULL AND LOWER(pc.nombre) LIKE LOWER(CONCAT('%', :q, '%'))) " +
                   "OR (v.producto.codigoBarras IS NOT NULL AND v.producto.codigoBarras.codigoBarras LIKE CONCAT('%', :q, '%')))",
           countQuery = "SELECT COUNT(v) FROM Variantes v LEFT JOIN v.palabraClave pc " +
                        "WHERE v.stock > 0 AND v.producto.habilitado = '1' " +
                        "AND (LOWER(v.producto.nombre) LIKE LOWER(CONCAT('%', :q, '%')) " +
                        "OR LOWER(v.marca) LIKE LOWER(CONCAT('%', :q, '%')) " +
                        "OR (pc IS NOT NULL AND LOWER(pc.nombre) LIKE LOWER(CONCAT('%', :q, '%'))) " +
                        "OR (v.producto.codigoBarras IS NOT NULL AND v.producto.codigoBarras.codigoBarras LIKE CONCAT('%', :q, '%')))")
    Page<Variantes> buscarParaChatbot(@Param("q") String q, Pageable pageable);
}
