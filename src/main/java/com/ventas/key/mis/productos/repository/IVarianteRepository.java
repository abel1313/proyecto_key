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

    // Listado general de admin (getAll/findAllNew) sin ningun filtro de negocio -- ve todo
    // (deshabilitados, sin stock, sin imagen) salvo las variantes "sombra" de flores eternas,
    // que nunca deben aparecer como si fueran un producto navegable mas.
    @Query(value = "SELECT v FROM Variantes v WHERE v.producto.esCatalogoInterno = false",
           countQuery = "SELECT COUNT(v) FROM Variantes v WHERE v.producto.esCatalogoInterno = false")
    Page<Variantes> findVisibleParaAdmin(Pageable pageable);

    // Resolver minimo para la ficha de producto publica: cuando el cliente entra por un link
    // directo/marcador a /tienda/detalle/{varianteId} sin haber pasado por el catalogo, el front
    // no tiene el productoId a mano (antes lo sacaba de GET /tienda/v1/getOne/{id}, que ahora es
    // solo ADMIN). Proyeccion directa al id en vez de la entidad completa -- no hay razon para
    // traer el resto de la variante solo para leer un campo.
    @Query("SELECT v.producto.id FROM Variantes v WHERE v.id = :varianteId")
    Optional<Integer> findProductoIdByVarianteId(@Param("varianteId") Integer varianteId);

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
    // JOIN FETCH del grafo que el DTO de resumen siempre lee (producto, su codigo de barras y la
    // palabra clave): sin el, cada variante de la pagina disparaba un SELECT extra por asociacion
    // porque las tres son EAGER. countQuery explicito obligatorio al usar fetch join con Page.
    @Query(value = "SELECT v FROM Variantes v " +
           "JOIN FETCH v.producto p " +
           "LEFT JOIN FETCH p.codigoBarras " +
           "LEFT JOIN FETCH v.palabraClave " +
           "WHERE v.stock > 0 AND p.habilitado = '1' AND v.habilitado = '1' AND p.esCatalogoInterno = false " +
           "AND EXISTS (SELECT 1 FROM VarianteImagen vi WHERE vi.variante = v)",
           countQuery = "SELECT COUNT(v) FROM Variantes v WHERE v.stock > 0 AND v.producto.habilitado = '1' " +
           "AND v.habilitado = '1' AND v.producto.esCatalogoInterno = false " +
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
    // El SELECT trae con JOIN FETCH producto + codigo de barras + palabra clave porque el DTO de
    // resumen lee las tres en cada fila y son EAGER: sin el fetch, cada variante de la pagina
    // costaba 3 SELECT extra. El count usa los mismos joins pero sin FETCH (no se permite fetch
    // en un COUNT). Los joins ademas son explicitos y LEFT donde toca: escribir
    // v.producto.codigoBarras.codigoBarras dentro de un OR genera un INNER JOIN implicito que
    // descarta de TODO el resultado a las variantes cuyo producto no tiene codigo de barras,
    // aunque hubieran matcheado por nombre o palabra clave.
    @Query(value = """
        SELECT v FROM Variantes v
        JOIN FETCH v.producto p
        LEFT JOIN FETCH p.codigoBarras cb
        LEFT JOIN FETCH v.palabraClave pc
        WHERE (:nombreOCodigo IS NULL
               OR LOWER(p.nombre) LIKE LOWER(CONCAT('%', :nombreOCodigo, '%'))
               OR (cb IS NOT NULL
                   AND LOWER(cb.codigoBarras) LIKE LOWER(CONCAT('%', :nombreOCodigo, '%')))
               OR (pc IS NOT NULL AND LOWER(pc.nombre) LIKE LOWER(CONCAT('%', :nombreOCodigo, '%'))))
          AND (:conStock IS NULL OR (:conStock = TRUE AND v.stock > 0) OR (:conStock = FALSE AND v.stock = 0))
          AND (:conImagenes IS NULL
               OR (:conImagenes = TRUE AND EXISTS (SELECT 1 FROM VarianteImagen vi WHERE vi.variante = v))
               OR (:conImagenes = FALSE AND NOT EXISTS (SELECT 1 FROM VarianteImagen vi WHERE vi.variante = v)))
          AND (:habilitado IS NULL
               OR (:habilitado = TRUE AND v.habilitado = '1' AND p.habilitado = '1')
               OR (:habilitado = FALSE AND (v.habilitado <> '1' OR p.habilitado <> '1')))
          AND (:codigoGenerado IS NULL
               OR (:codigoGenerado = TRUE AND p.codigoBarrasGenerado = TRUE)
               OR (:codigoGenerado = FALSE AND (p.codigoBarrasGenerado IS NULL OR p.codigoBarrasGenerado = FALSE)))
          AND (:fechaDesde IS NULL OR v.fechaCreacion >= :fechaDesde)
          AND (:fechaHasta IS NULL OR v.fechaCreacion <= :fechaHasta)
          AND p.esCatalogoInterno = false
        """,
        countQuery = """
        SELECT COUNT(v) FROM Variantes v
        JOIN v.producto p
        LEFT JOIN p.codigoBarras cb
        LEFT JOIN v.palabraClave pc
        WHERE (:nombreOCodigo IS NULL
               OR LOWER(p.nombre) LIKE LOWER(CONCAT('%', :nombreOCodigo, '%'))
               OR (cb IS NOT NULL
                   AND LOWER(cb.codigoBarras) LIKE LOWER(CONCAT('%', :nombreOCodigo, '%')))
               OR (pc IS NOT NULL AND LOWER(pc.nombre) LIKE LOWER(CONCAT('%', :nombreOCodigo, '%'))))
          AND (:conStock IS NULL OR (:conStock = TRUE AND v.stock > 0) OR (:conStock = FALSE AND v.stock = 0))
          AND (:conImagenes IS NULL
               OR (:conImagenes = TRUE AND EXISTS (SELECT 1 FROM VarianteImagen vi WHERE vi.variante = v))
               OR (:conImagenes = FALSE AND NOT EXISTS (SELECT 1 FROM VarianteImagen vi WHERE vi.variante = v)))
          AND (:habilitado IS NULL
               OR (:habilitado = TRUE AND v.habilitado = '1' AND p.habilitado = '1')
               OR (:habilitado = FALSE AND (v.habilitado <> '1' OR p.habilitado <> '1')))
          AND (:codigoGenerado IS NULL
               OR (:codigoGenerado = TRUE AND p.codigoBarrasGenerado = TRUE)
               OR (:codigoGenerado = FALSE AND (p.codigoBarrasGenerado IS NULL OR p.codigoBarrasGenerado = FALSE)))
          AND (:fechaDesde IS NULL OR v.fechaCreacion >= :fechaDesde)
          AND (:fechaHasta IS NULL OR v.fechaCreacion <= :fechaHasta)
          AND p.esCatalogoInterno = false
        """)
    Page<Variantes> buscarVariantesAdmin(@Param("nombreOCodigo") String nombreOCodigo,
                                          @Param("conStock") Boolean conStock,
                                          @Param("conImagenes") Boolean conImagenes,
                                          @Param("habilitado") Boolean habilitado,
                                          @Param("codigoGenerado") Boolean codigoGenerado,
                                          @Param("fechaDesde") java.time.LocalDateTime fechaDesde,
                                          @Param("fechaHasta") java.time.LocalDateTime fechaHasta,
                                          Pageable pageable);

    // Catalogo publico con filtros: mismas restricciones de visibilidad que findConStockYImagenPublico
    // (stock>0, producto y variante habilitados, con imagen) + termino/precioMin/precioMax/talla/
    // color/marca opcionales (tri-estado, se combinan con AND). talla/color/marca son match exacto
    // (pensado para dropdowns poblados con /tienda/v1/filtros-disponibles, no texto libre).
    // Mismo criterio que buscarVariantesAdmin: JOIN FETCH del grafo que lee el DTO (producto,
    // codigo de barras, palabra clave) para no pagar 3 SELECT extra por variante, y joins
    // explicitos LEFT sobre codigoBarras para no perder variantes cuyo producto no lo tiene.
    @Query(value = """
        SELECT v FROM Variantes v
        JOIN FETCH v.producto p
        LEFT JOIN FETCH p.codigoBarras cb
        LEFT JOIN FETCH v.palabraClave pc
        WHERE v.stock > 0 AND p.habilitado = '1' AND v.habilitado = '1' AND p.esCatalogoInterno = false
          AND EXISTS (SELECT 1 FROM VarianteImagen vi WHERE vi.variante = v)
          AND (:termino IS NULL
               OR LOWER(p.nombre) LIKE LOWER(CONCAT('%', :termino, '%'))
               OR LOWER(v.marca) LIKE LOWER(CONCAT('%', :termino, '%'))
               OR (pc IS NOT NULL AND LOWER(pc.nombre) LIKE LOWER(CONCAT('%', :termino, '%')))
               OR (cb IS NOT NULL
                   AND LOWER(cb.codigoBarras) LIKE LOWER(CONCAT('%', :termino, '%'))))
          AND (:precioMin IS NULL OR p.precioVenta >= :precioMin)
          AND (:precioMax IS NULL OR p.precioVenta <= :precioMax)
          AND (:talla IS NULL OR LOWER(v.talla) = LOWER(:talla))
          AND (:color IS NULL OR LOWER(v.color) = LOWER(:color))
          AND (:marca IS NULL OR LOWER(v.marca) = LOWER(:marca))
        """,
        countQuery = """
        SELECT COUNT(v) FROM Variantes v
        JOIN v.producto p
        LEFT JOIN p.codigoBarras cb
        LEFT JOIN v.palabraClave pc
        WHERE v.stock > 0 AND p.habilitado = '1' AND v.habilitado = '1' AND p.esCatalogoInterno = false
          AND EXISTS (SELECT 1 FROM VarianteImagen vi WHERE vi.variante = v)
          AND (:termino IS NULL
               OR LOWER(p.nombre) LIKE LOWER(CONCAT('%', :termino, '%'))
               OR LOWER(v.marca) LIKE LOWER(CONCAT('%', :termino, '%'))
               OR (pc IS NOT NULL AND LOWER(pc.nombre) LIKE LOWER(CONCAT('%', :termino, '%')))
               OR (cb IS NOT NULL
                   AND LOWER(cb.codigoBarras) LIKE LOWER(CONCAT('%', :termino, '%'))))
          AND (:precioMin IS NULL OR p.precioVenta >= :precioMin)
          AND (:precioMax IS NULL OR p.precioVenta <= :precioMax)
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
           "AND v.habilitado = '1' AND v.producto.esCatalogoInterno = false " +
           "AND v.talla IS NOT NULL AND v.talla <> '' ORDER BY v.talla")
    List<String> findTallasDisponiblesPublico();

    @Query("SELECT DISTINCT v.color FROM Variantes v WHERE v.stock > 0 AND v.producto.habilitado = '1' " +
           "AND v.habilitado = '1' AND v.producto.esCatalogoInterno = false " +
           "AND v.color IS NOT NULL AND v.color <> '' ORDER BY v.color")
    List<String> findColoresDisponiblesPublico();

    @Query("SELECT DISTINCT v.marca FROM Variantes v WHERE v.stock > 0 AND v.producto.habilitado = '1' " +
           "AND v.habilitado = '1' AND v.producto.esCatalogoInterno = false " +
           "AND v.marca IS NOT NULL AND v.marca <> '' ORDER BY v.marca")
    List<String> findMarcasDisponiblesPublico();

    // Object[] como tipo de retorno directo hace que Spring Data trate el metodo como
    // "collection query" y anide la fila real en result[0] en vez de venir aplanada
    // (ver mismo patron/comentario en IVentaRepository.sumVentasRaw). Por eso se expone
    // como List<Object[]> y se aplana en el metodo default.
    @Query("SELECT MIN(v.producto.precioVenta), MAX(v.producto.precioVenta) FROM Variantes v " +
           "WHERE v.stock > 0 AND v.producto.habilitado = '1' AND v.habilitado = '1' " +
           "AND v.producto.esCatalogoInterno = false")
    List<Object[]> findRangoPreciosPublicoRaw();

    default Object[] findRangoPreciosPublico() {
        List<Object[]> filas = findRangoPreciosPublicoRaw();
        return filas.isEmpty() ? new Object[]{null, null} : filas.get(0);
    }

    // --- búsqueda para chatbot: por nombre de producto, marca o palabra clave ---
    @Query(value = "SELECT v FROM Variantes v LEFT JOIN v.palabraClave pc " +
                   "WHERE v.stock > 0 AND v.producto.habilitado = '1' AND v.habilitado = '1' " +
                   "AND v.producto.esCatalogoInterno = false " +
                   "AND (LOWER(v.producto.nombre) LIKE LOWER(CONCAT('%', :q, '%')) " +
                   "OR LOWER(v.marca) LIKE LOWER(CONCAT('%', :q, '%')) " +
                   "OR (pc IS NOT NULL AND LOWER(pc.nombre) LIKE LOWER(CONCAT('%', :q, '%'))) " +
                   "OR (v.producto.codigoBarras IS NOT NULL AND v.producto.codigoBarras.codigoBarras LIKE CONCAT('%', :q, '%')))",
           countQuery = "SELECT COUNT(v) FROM Variantes v LEFT JOIN v.palabraClave pc " +
                        "WHERE v.stock > 0 AND v.producto.habilitado = '1' AND v.habilitado = '1' " +
                        "AND v.producto.esCatalogoInterno = false " +
                        "AND (LOWER(v.producto.nombre) LIKE LOWER(CONCAT('%', :q, '%')) " +
                        "OR LOWER(v.marca) LIKE LOWER(CONCAT('%', :q, '%')) " +
                        "OR (pc IS NOT NULL AND LOWER(pc.nombre) LIKE LOWER(CONCAT('%', :q, '%'))) " +
                        "OR (v.producto.codigoBarras IS NOT NULL AND v.producto.codigoBarras.codigoBarras LIKE CONCAT('%', :q, '%')))")
    Page<Variantes> buscarParaChatbot(@Param("q") String q, Pageable pageable);
}
