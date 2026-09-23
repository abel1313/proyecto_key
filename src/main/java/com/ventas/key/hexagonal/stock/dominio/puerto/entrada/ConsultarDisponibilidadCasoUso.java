package com.ventas.key.hexagonal.stock.dominio.puerto.entrada;

import com.ventas.key.hexagonal.stock.dominio.modelo.DisponibilidadStock;

import java.util.List;

/**
 * Consultar cuanto stock queda libre para armar variantes.
 *
 * <p>[Hexagonal: Driving Port] [Clean: Use Case boundary]
 */
public interface ConsultarDisponibilidadCasoUso {

    /** Para mostrarlo en la pantalla de alta/edicion antes de que el admin escriba un numero. */
    DisponibilidadStock de(Integer productoId);

    /** Reporte de inventario descuadrado -- ver ConsultarStockPort.productosDescuadrados(). */
    List<DisponibilidadStock> descuadrados();
}
