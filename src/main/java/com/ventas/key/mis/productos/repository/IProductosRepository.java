package com.ventas.key.mis.productos.repository;

import com.ventas.key.mis.productos.entity.EstadoCargaImagen;
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

    // --- búsqueda paso 1: código de barras exacto (uso interno: guardado/validaciones, no buscador) ---
    Optional<Producto> findByCodigoBarras_CodigoBarrasIgnoreCase(String codigoBarras);
    Optional<Producto> findByStockGreaterThanAndHabilitadoAndCodigoBarras_CodigoBarrasIgnoreCase(int stock, char habilitado, String codigoBarras);

    Page<Producto> findByPalabraClave_NombreIgnoreCaseAndStockGreaterThanAndHabilitado(String nombre, int stock, char habilitado, Pageable pageable);
    Page<Producto> findByNombreContainingAndHabilitado(String nombre, char habilitado, Pageable pageable);

    // --- listado público: stock + habilitado + con imagen (cliente normal) ---
    @Query("SELECT p FROM Producto p WHERE p.stock > 0 AND p.habilitado = '1' " +
           "AND EXISTS (SELECT 1 FROM ProductoImagen pi WHERE pi.producto = p)")
    Page<Producto> findConStockYImagenPublico(Pageable pageable);

    @Query("SELECT p FROM Producto p WHERE p.stock > 0 AND p.habilitado = '1' " +
           "AND LOWER(p.codigoBarras.codigoBarras) = LOWER(:codigoBarras) " +
           "AND EXISTS (SELECT 1 FROM ProductoImagen pi WHERE pi.producto = p)")
    Optional<Producto> findByCodigoBarrasPublico(@Param("codigoBarras") String codigoBarras);

    // --- filtros de admin (ve todo, sin restriccion de stock/habilitado salvo el filtro elegido) ---
    Page<Producto> findByStockGreaterThan(int stock, Pageable pageable);

    @Query("SELECT p FROM Producto p WHERE EXISTS (SELECT 1 FROM ProductoImagen pi WHERE pi.producto = p)")
    Page<Producto> findConImagen(Pageable pageable);

    @Query("SELECT p FROM Producto p WHERE p.stock > 0 " +
           "AND EXISTS (SELECT 1 FROM ProductoImagen pi WHERE pi.producto = p)")
    Page<Producto> findConStockYImagenAdmin(Pageable pageable);

    // Filtro combinado de admin: nombreOCodigo/conStock/conImagenes/habilitado son todos
    // opcionales (Boolean nullable = tri-estado: null = cualquiera). Se combinan con AND.
    // nombreOCodigo matchea nombre, codigo de barras O palabra clave (OR, una sola pasada) --
    // tambien usado por el buscador publico/admin findNombreOrCodigoBarra (con tri-state fijo
    // en TRUE para el publico) en vez de la cascada vieja de 3 queries secuenciales, ver
    // ProductosServiceImpl.findNombreOrCodigoBarra.
    // countQuery explicito obligatorio: con EXISTS + Page, sin countQuery propio Spring genera
    // uno automatico que puede devolver vacio aunque si haya datos.
    @Query(value = """
        SELECT p FROM Producto p
        LEFT JOIN p.codigoBarras cb
        LEFT JOIN p.palabraClave pk
        WHERE (:nombreOCodigo IS NULL
               OR LOWER(p.nombre) LIKE LOWER(CONCAT('%', :nombreOCodigo, '%'))
               OR (cb IS NOT NULL AND LOWER(cb.codigoBarras) LIKE LOWER(CONCAT('%', :nombreOCodigo, '%')))
               OR (pk IS NOT NULL AND LOWER(pk.nombre) LIKE LOWER(CONCAT('%', :nombreOCodigo, '%'))))
          AND (:conStock IS NULL OR (:conStock = TRUE AND p.stock > 0) OR (:conStock = FALSE AND p.stock = 0))
          AND (:conImagenes IS NULL
               OR (:conImagenes = TRUE AND EXISTS (SELECT 1 FROM ProductoImagen pi WHERE pi.producto = p))
               OR (:conImagenes = FALSE AND NOT EXISTS (SELECT 1 FROM ProductoImagen pi WHERE pi.producto = p)))
          AND (:habilitado IS NULL OR (:habilitado = TRUE AND p.habilitado = '1') OR (:habilitado = FALSE AND p.habilitado <> '1'))
        """,
        countQuery = """
        SELECT COUNT(p) FROM Producto p
        LEFT JOIN p.codigoBarras cb
        LEFT JOIN p.palabraClave pk
        WHERE (:nombreOCodigo IS NULL
               OR LOWER(p.nombre) LIKE LOWER(CONCAT('%', :nombreOCodigo, '%'))
               OR (cb IS NOT NULL AND LOWER(cb.codigoBarras) LIKE LOWER(CONCAT('%', :nombreOCodigo, '%')))
               OR (pk IS NOT NULL AND LOWER(pk.nombre) LIKE LOWER(CONCAT('%', :nombreOCodigo, '%'))))
          AND (:conStock IS NULL OR (:conStock = TRUE AND p.stock > 0) OR (:conStock = FALSE AND p.stock = 0))
          AND (:conImagenes IS NULL
               OR (:conImagenes = TRUE AND EXISTS (SELECT 1 FROM ProductoImagen pi WHERE pi.producto = p))
               OR (:conImagenes = FALSE AND NOT EXISTS (SELECT 1 FROM ProductoImagen pi WHERE pi.producto = p)))
          AND (:habilitado IS NULL OR (:habilitado = TRUE AND p.habilitado = '1') OR (:habilitado = FALSE AND p.habilitado <> '1'))
        """)
    Page<Producto> buscarProductosAdmin(@Param("nombreOCodigo") String nombreOCodigo,
                                         @Param("conStock") Boolean conStock,
                                         @Param("conImagenes") Boolean conImagenes,
                                         @Param("habilitado") Boolean habilitado,
                                         Pageable pageable);

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

    // --- carga rapida de imagenes: estado directo en producto, ver /v1/carga-imagenes/* ---
    List<Producto> findByEstadoImagenOrderByIdDesc(EstadoCargaImagen estadoImagen);
}