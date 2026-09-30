package com.ventas.key.hexagonal.precio.dominio.puerto.entrada;

import com.ventas.key.hexagonal.precio.dominio.modelo.PreciosDeArticulo;

/**
 * [Hexagonal: Driving Port] [Clean: Input Boundary]
 *
 * <p>R9: el precio con descuento no viaja en las listas; se pide uno por uno, cuando el admin lo
 * quiere ver o aplicar.
 */
public interface ConsultarDescuentoArticuloCasoUso {

    PreciosDeArticulo consultar(Integer varianteId);
}
