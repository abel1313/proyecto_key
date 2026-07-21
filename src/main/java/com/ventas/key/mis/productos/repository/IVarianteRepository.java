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

    List<Variantes> findByProductoIdIn(List<Integer> productoIds);

    Page<Variantes> findByProductoId(Integer productoId, Pageable pageable);

    List<Variantes> findByProductoNombreContainingIgnoreCase(String nombre);
    Page<Variantes> findByProductoNombreContainingIgnoreCase(String nombre, Pageable pageable);
    Page<Variantes> findByStockGreaterThanAndProducto_HabilitadoAndProducto_NombreContainingIgnoreCase(int stock, char habilitado, String nombre, Pageable pageable);

    List<Variantes> findByProductoCodigoBarrasCodigoBarras(String codigoBarras);
    Page<Variantes> findByProductoCodigoBarrasCodigoBarras(String codigoBarras, Pageable pageable);

    // BUG 2026-07-13: el buscador admin (buscarPorCodigoBarrasPaginado) usaba el metodo exacto de
    // arriba, asi que "glpd" nunca encontraba "GLPD-066" -- mismo bug que en IProductosRepository
    // (findByCodigoBarrasContainingAdmin). Esta version parcial es la que ahora se usa ahi.
    Page<Variantes> findByProductoCodigoBarrasCodigoBarrasContainingIgnoreCase(String codigoBarras, Pageable pageable);
    Page<Variantes> findByStockGreaterThanAndProducto_Habilitado(int stock, char habilitado, Pageable pageable);
    Page<Variantes> findByStockGreaterThanAndProducto_HabilitadoAndProducto_CodigoBarras_CodigoBarrasContaining(int stock, char habilitado, String codigoBarras, Pageable pageable);

    Page<Variantes> findByStockGreaterThanAndProductoHabilitado(int stock, char habilitado, Pageable pageable);

    @Query("SELECT v FROM Variantes v WHERE v.stock = 0 AND v.producto.habilitado <> '1'")
    Page<Variantes> findVariantesSinStockDeshabilitadas(Pageable pageable);

    List<Variantes> findByProductoIdAndHabilitado(Integer productoId, char habilitado);
    List<Variantes> findByProductoIdAndHabilitadoOrderByIdDesc(Integer productoId, char habilitado);

    // Stock bajo = todavía se puede vender (stock > 0) pero se está agotando.
    // No incluye stock = 0 — eso ya se cubre en findVariantesSinStockDeshabilitadas.
    @Query("SELECT COUNT(v) FROM Variantes v WHERE v.stock > 0 AND v.stock < :umbral AND v.producto.habilitado = '1'")
    long countStockBajo(@Param("umbral") int umbral);

    // --- búsqueda por palabra clave ---
    Page<Variantes> findByPalabraClave_NombreIgnoreCase(String nombre, Pageable pageable);
    Page<Variantes> findByStockGreaterThanAndProducto_HabilitadoAndPalabraClave_NombreIgnoreCase(int stock, char habilitado, String nombre, Pageable pageable);

    // --- listado público: stock + habilitado (producto Y variante) + con imagen (cliente normal) ---
    @Query("SELECT v FROM Variantes v WHERE v.stock > 0 AND v.producto.habilitado = '1' AND v.habilitado = '1' " +
           "AND EXISTS (SELECT 1 FROM VarianteImagen vi WHERE vi.variante = v)")
    Page<Variantes> findConStockYImagenPublico(Pageable pageable);

    @Query("SELECT v FROM Variantes v WHERE v.stock > 0 AND v.producto.habilitado = '1' AND v.habilitado = '1' " +
           "AND v.producto.codigoBarras.codigoBarras LIKE CONCAT('%', :codigoBarras, '%') " +
           "AND EXISTS (SELECT 1 FROM VarianteImagen vi WHERE vi.variante = v)")
    Page<Variantes> findByCodigoBarrasPublico(@Param("codigoBarras") String codigoBarras, Pageable pageable);

    @Query("SELECT v FROM Variantes v WHERE v.stock > 0 AND v.producto.habilitado = '1' AND v.habilitado = '1' " +
           "AND LOWER(v.palabraClave.nombre) = LOWER(:nombre) " +
           "AND EXISTS (SELECT 1 FROM VarianteImagen vi WHERE vi.variante = v)")
    Page<Variantes> findByPalabraClavePublico(@Param("nombre") String nombre, Pageable pageable);

    @Query("SELECT v FROM Variantes v WHERE v.stock > 0 AND v.producto.habilitado = '1' AND v.habilitado = '1' " +
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

    // Filtro combinado de admin: nombreOCodigo/conStock/conImagenes/habilitado son todos
    // opcionales (Boolean nullable = tri-estado: null = cualquiera). Se combinan con AND.
    // habilitado usa v.habilitado (de la variante), no v.producto.habilitado.
    // nombreOCodigo matchea nombre, codigo de barras O palabra clave (OR, una sola pasada) --
    // tambien usado por el buscador publico/admin /v1/buscar en vez de la cascada vieja de 3
    // queries secuenciales, ver VarianteServiceImpl.buscarVariantes.
    // countQuery explicito obligatorio: con EXISTS + Page, sin countQuery propio Spring genera
    // uno automatico que puede devolver vacio aunque si haya datos.
    @Query(value = """
        SELECT v FROM Variantes v
        LEFT JOIN v.palabraClave pc
        WHERE (:nombreOCodigo IS NULL
               OR LOWER(v.producto.nombre) LIKE LOWER(CONCAT('%', :nombreOCodigo, '%'))
               OR (v.producto.codigoBarras IS NOT NULL
                   AND LOWER(v.producto.codigoBarras.codigoBarras) LIKE LOWER(CONCAT('%', :nombreOCodigo, '%')))
               OR (pc IS NOT NULL AND LOWER(pc.nombre) LIKE LOWER(CONCAT('%', :nombreOCodigo, '%'))))
          AND (:conStock IS NULL OR (:conStock = TRUE AND v.stock > 0) OR (:conStock = FALSE AND v.stock = 0))
          AND (:conImagenes IS NULL
               OR (:conImagenes = TRUE AND EXISTS (SELECT 1 FROM VarianteImagen vi WHERE vi.variante = v))
               OR (:conImagenes = FALSE AND NOT EXISTS (SELECT 1 FROM VarianteImagen vi WHERE vi.variante = v)))
          AND (:habilitado IS NULL OR (:habilitado = TRUE AND v.habilitado = '1') OR (:habilitado = FALSE AND v.habilitado <> '1'))
        """,
        countQuery = """
        SELECT COUNT(v) FROM Variantes v
        LEFT JOIN v.palabraClave pc
        WHERE (:nombreOCodigo IS NULL
               OR LOWER(v.producto.nombre) LIKE LOWER(CONCAT('%', :nombreOCodigo, '%'))
               OR (v.producto.codigoBarras IS NOT NULL
                   AND LOWER(v.producto.codigoBarras.codigoBarras) LIKE LOWER(CONCAT('%', :nombreOCodigo, '%')))
               OR (pc IS NOT NULL AND LOWER(pc.nombre) LIKE LOWER(CONCAT('%', :nombreOCodigo, '%'))))
          AND (:conStock IS NULL OR (:conStock = TRUE AND v.stock > 0) OR (:conStock = FALSE AND v.stock = 0))
          AND (:conImagenes IS NULL
               OR (:conImagenes = TRUE AND EXISTS (SELECT 1 FROM VarianteImagen vi WHERE vi.variante = v))
               OR (:conImagenes = FALSE AND NOT EXISTS (SELECT 1 FROM VarianteImagen vi WHERE vi.variante = v)))
          AND (:habilitado IS NULL OR (:habilitado = TRUE AND v.habilitado = '1') OR (:habilitado = FALSE AND v.habilitado <> '1'))
        """)
    Page<Variantes> buscarVariantesAdmin(@Param("nombreOCodigo") String nombreOCodigo,
                                          @Param("conStock") Boolean conStock,
                                          @Param("conImagenes") Boolean conImagenes,
                                          @Param("habilitado") Boolean habilitado,
                                          Pageable pageable);

    // Catalogo publico con filtros: mismas restricciones de visibilidad que findConStockYImagenPublico
    // (stock>0, producto y variante habilitados, con imagen) + termino/precioMin/precioMax/talla/
    // color/marca opcionales (tri-estado, se combinan con AND). talla/color/marca son match exacto
    // (pensado para dropdowns poblados con /variantes/v1/filtros-disponibles, no texto libre).
    @Query(value = """
        SELECT v FROM Variantes v LEFT JOIN v.palabraClave pc
        WHERE v.stock > 0 AND v.producto.habilitado = '1' AND v.habilitado = '1'
          AND EXISTS (SELECT 1 FROM VarianteImagen vi WHERE vi.variante = v)
          AND (:termino IS NULL
               OR LOWER(v.producto.nombre) LIKE LOWER(CONCAT('%', :termino, '%'))
               OR LOWER(v.marca) LIKE LOWER(CONCAT('%', :termino, '%'))
               OR (pc IS NOT NULL AND LOWER(pc.nombre) LIKE LOWER(CONCAT('%', :termino, '%')))
               OR (v.producto.codigoBarras IS NOT NULL
                   AND LOWER(v.producto.codigoBarras.codigoBarras) LIKE LOWER(CONCAT('%', :termino, '%'))))
          AND (:precioMin IS NULL OR v.producto.precioVenta >= :precioMin)
          AND (:precioMax IS NULL OR v.producto.precioVenta <= :precioMax)
          AND (:talla IS NULL OR LOWER(v.talla) = LOWER(:talla))
          AND (:color IS NULL OR LOWER(v.color) = LOWER(:color))
          AND (:marca IS NULL OR LOWER(v.marca) = LOWER(:marca))
        """,
        countQuery = """
        SELECT COUNT(v) FROM Variantes v LEFT JOIN v.palabraClave pc
        WHERE v.stock > 0 AND v.producto.habilitado = '1' AND v.habilitado = '1'
          AND EXISTS (SELECT 1 FROM VarianteImagen vi WHERE vi.variante = v)
          AND (:termino IS NULL
               OR LOWER(v.producto.nombre) LIKE LOWER(CONCAT('%', :termino, '%'))
               OR LOWER(v.marca) LIKE LOWER(CONCAT('%', :termino, '%'))
               OR (pc IS NOT NULL AND LOWER(pc.nombre) LIKE LOWER(CONCAT('%', :termino, '%')))
               OR (v.producto.codigoBarras IS NOT NULL
                   AND LOWER(v.producto.codigoBarras.codigoBarras) LIKE LOWER(CONCAT('%', :termino, '%'))))
          AND (:precioMin IS NULL OR v.producto.precioVenta >= :precioMin)
          AND (:precioMax IS NULL OR v.producto.precioVenta <= :precioMax)
          AND (:talla IS NULL OR LOWER(v.talla) = LOWER(:talla))
          AND (:color IS NULL OR LOWER(v.color) = LOWER(:color))
          AND (:marca IS NULL OR LOWER(v.marca) = LOWER(:marca))
        """)
    Page<Variantes> buscarVariantesPublicoFiltrado(@Param("termino") String termino,
                                                     @Param("precioMin") Double precioMin,
                                                     @Param("precioMax") Double precioMax,
                                                     @Param("talla") String talla,
                                                     @Param("color") String color,
                                                     @Param("marca") String marca,
                                                     Pageable pageable);

    // Valores disponibles para poblar los filtros en el front (solo del catalogo visible al publico).
    @Query("SELECT DISTINCT v.talla FROM Variantes v WHERE v.stock > 0 AND v.producto.habilitado = '1' " +
           "AND v.habilitado = '1' AND v.talla IS NOT NULL AND v.talla <> '' ORDER BY v.talla")
    List<String> findTallasDisponiblesPublico();

    @Query("SELECT DISTINCT v.color FROM Variantes v WHERE v.stock > 0 AND v.producto.habilitado = '1' " +
           "AND v.habilitado = '1' AND v.color IS NOT NULL AND v.color <> '' ORDER BY v.color")
    List<String> findColoresDisponiblesPublico();

    @Query("SELECT DISTINCT v.marca FROM Variantes v WHERE v.stock > 0 AND v.producto.habilitado = '1' " +
           "AND v.habilitado = '1' AND v.marca IS NOT NULL AND v.marca <> '' ORDER BY v.marca")
    List<String> findMarcasDisponiblesPublico();

    @Query("SELECT MIN(v.producto.precioVenta), MAX(v.producto.precioVenta) FROM Variantes v " +
           "WHERE v.stock > 0 AND v.producto.habilitado = '1' AND v.habilitado = '1'")
    Object[] findRangoPreciosPublico();

    // --- búsqueda para chatbot: por nombre de producto, marca o palabra clave ---
    @Query(value = "SELECT v FROM Variantes v LEFT JOIN v.palabraClave pc " +
                   "WHERE v.stock > 0 AND v.producto.habilitado = '1' AND v.habilitado = '1' " +
                   "AND (LOWER(v.producto.nombre) LIKE LOWER(CONCAT('%', :q, '%')) " +
                   "OR LOWER(v.marca) LIKE LOWER(CONCAT('%', :q, '%')) " +
                   "OR (pc IS NOT NULL AND LOWER(pc.nombre) LIKE LOWER(CONCAT('%', :q, '%'))) " +
                   "OR (v.producto.codigoBarras IS NOT NULL AND v.producto.codigoBarras.codigoBarras LIKE CONCAT('%', :q, '%')))",
           countQuery = "SELECT COUNT(v) FROM Variantes v LEFT JOIN v.palabraClave pc " +
                        "WHERE v.stock > 0 AND v.producto.habilitado = '1' AND v.habilitado = '1' " +
                        "AND (LOWER(v.producto.nombre) LIKE LOWER(CONCAT('%', :q, '%')) " +
                        "OR LOWER(v.marca) LIKE LOWER(CONCAT('%', :q, '%')) " +
                        "OR (pc IS NOT NULL AND LOWER(pc.nombre) LIKE LOWER(CONCAT('%', :q, '%'))) " +
                        "OR (v.producto.codigoBarras IS NOT NULL AND v.producto.codigoBarras.codigoBarras LIKE CONCAT('%', :q, '%')))")
    Page<Variantes> buscarParaChatbot(@Param("q") String q, Pageable pageable);
}
