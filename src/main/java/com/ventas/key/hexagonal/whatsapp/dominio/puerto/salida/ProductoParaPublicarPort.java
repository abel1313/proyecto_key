package com.ventas.key.hexagonal.whatsapp.dominio.puerto.salida;

import com.ventas.key.hexagonal.whatsapp.dominio.modelo.ProductoParaPublicar;

import java.util.Optional;

/**
 * [Hexagonal: Driven Port] [Clean: Interface Adapter]
 *
 * <p>Los datos del articulo que se va a publicar. Vacio si no existe o esta dado de baja (habilitado = 0).
 */
public interface ProductoParaPublicarPort {

    Optional<ProductoParaPublicar> de(int varianteId);
}
