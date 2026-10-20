package com.ventas.key.hexagonal.articulo.dominio.modelo;

import com.ventas.key.hexagonal.articulo.dominio.excepcion.ArticuloNoVendibleException;

/**
 * Un articulo en el momento de venderlo: pedido, venta directa o transferencia de saldo.
 *
 * <p>[Hexagonal: dentro del hexagono] [Clean: Entities]
 *
 * <p>R4 del README: se valida primero el modelo (habilitado y con stock) y despues el articulo
 * (habilitado y con stock). El back no confia en el front: el carrito vive en el navegador y puede
 * traer un articulo que se deshabilito despues de agregarlo.
 *
 * @param nombre              para los mensajes, como lo ve el cliente
 * @param modeloHabilitado    el producto padre
 * @param stockModelo         piezas del producto padre
 * @param articuloHabilitado  la variante; ignorado si {@code stockArticulo} es null
 * @param stockArticulo       piezas del articulo, o null en una linea que no trae articulo
 */
public record ArticuloAVender(
        String nombre,
        boolean modeloHabilitado,
        int stockModelo,
        boolean articuloHabilitado,
        Integer stockArticulo) {

    public static ArticuloAVender deArticulo(String nombre, boolean modeloHabilitado, int stockModelo,
                                             boolean articuloHabilitado, int stockArticulo) {
        return new ArticuloAVender(nombre, modeloHabilitado, stockModelo, articuloHabilitado, stockArticulo);
    }

    public static ArticuloAVender soloModelo(String nombre, boolean modeloHabilitado, int stockModelo) {
        return new ArticuloAVender(nombre, modeloHabilitado, stockModelo, true, null);
    }

    public void exigirQueSePuedaVender(int cantidad) {
        if (!modeloHabilitado) {
            throw ArticuloNoVendibleException.deshabilitado(nombre, "el producto");
        }
        if (stockModelo < cantidad) {
            throw ArticuloNoVendibleException.sinStock(nombre, stockModelo, cantidad);
        }
        if (stockArticulo == null) {
            return;
        }
        if (!articuloHabilitado) {
            throw ArticuloNoVendibleException.deshabilitado(nombre, "el artículo");
        }
        if (stockArticulo < cantidad) {
            throw ArticuloNoVendibleException.sinStock(nombre, stockArticulo, cantidad);
        }
    }

    /** "Pantalón talla 32 azul": como se llama en los mensajes. */
    public static String nombreVisible(String producto, String talla, String color) {
        StringBuilder sb = new StringBuilder(producto != null && !producto.isBlank() ? producto : "Artículo");
        if (talla != null && !talla.isBlank()) {
            sb.append(" talla ").append(talla);
        }
        if (color != null && !color.isBlank()) {
            sb.append(" ").append(color);
        }
        return sb.toString();
    }
}
