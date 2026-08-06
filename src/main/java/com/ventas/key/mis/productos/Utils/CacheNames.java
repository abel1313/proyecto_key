package com.ventas.key.mis.productos.Utils;

/**
 * Nombres de los caches de Redis, para no repetirlos como literales sueltos por todo el proyecto
 * (hallazgo 11 de ANALISIS_RENDIMIENTO_BUSQUEDAS.md).
 *
 * <p>Son constantes de compilacion a proposito: solo asi se pueden usar dentro de anotaciones
 * como {@code @CacheEvict(value = {...})}.
 */
public final class CacheNames {

    private CacheNames() {}

    // --- Catalogo: TODOS estos incluyen el stock en su contenido ---
    /** Listado paginado de productos. TTL 1h. */
    public static final String PRODUCTOS = "obtenerProductosCache";
    /** Busqueda de productos por nombre o codigo de barras. TTL 2h. */
    public static final String PRODUCTOS_BUSQUEDA = "buscarNombreOrCodigoBarrasCache";
    /** Detalle de un producto por id. TTL 6h — el mas largo, y lleva stock. */
    public static final String PRODUCTO_DETALLE = "findByIdCache";
    /** Variantes por producto, busqueda y filtros. TTL 1h. */
    public static final String VARIANTES = "variantesProductoCache";
    /** Busqueda de variantes por nombre. TTL 2h. */
    public static final String VARIANTES_NOMBRE = "variantesNombreCache";
    /** Busqueda de variantes por codigo de barras. TTL 2h. */
    public static final String VARIANTES_CODIGO_BARRAS = "variantesCodigoBarrasCache";

    /**
     * Los caches que quedan obsoletos cuando se mueve el stock (venta, cancelacion, devolucion).
     *
     * <p>Deliberadamente NO incluye los caches de imagenes ({@code variantesImagenesCache},
     * {@code detalleImagen}, {@code detalle}) ni los de catalogos estaticos (tipos de pago,
     * tarifas, IVA): no contienen stock, son caros de reconstruir y vaciarlos seria puro
     * desperdicio. Por eso no se usa {@code evictAllCaches()}, que borra absolutamente todo.
     */
    public static final String[] CATALOGO_CON_STOCK = {
            PRODUCTOS, PRODUCTOS_BUSQUEDA, PRODUCTO_DETALLE,
            VARIANTES, VARIANTES_NOMBRE, VARIANTES_CODIGO_BARRAS
    };
}
