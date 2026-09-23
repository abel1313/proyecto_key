package com.ventas.key.hexagonal.precio.dominio.puerto.salida;

/**
 * [Hexagonal: Driven Port] [Clean: Interface Adapter]
 *
 * <p>Separado de {@link PreciosProductoPort}: guardar escribe en BD y nada mas; invalidar las
 * caches del catalogo es otra responsabilidad (regla "un metodo, una responsabilidad").
 */
public interface AvisarCambioCatalogoPort {

    void catalogoCambio();
}
