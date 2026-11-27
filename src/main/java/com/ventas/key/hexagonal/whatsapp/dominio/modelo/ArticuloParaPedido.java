package com.ventas.key.hexagonal.whatsapp.dominio.modelo;

/**
 * Un artículo tal como está en la base en este momento, para decidir si se puede apartar por WhatsApp.
 *
 * @param precioACobrar el precio al que se vende hoy (el normal, o el descuento solo si el dueño lo dejó activo)
 * @param esCatalogoInterno producto «sombra» (flores eternas): nunca se vende por este canal
 */
public record ArticuloParaPedido(int id, String nombre, double precioACobrar, int stock, boolean habilitado,
                                 boolean esCatalogoInterno, boolean tieneImagen) {

    /** Lo mismo que exige la tienda en línea para mostrarlo: habilitado, con stock, con imagen y no interno. */
    public boolean seOfrece() {
        return habilitado && !esCatalogoInterno && tieneImagen && stock > 0;
    }
}
