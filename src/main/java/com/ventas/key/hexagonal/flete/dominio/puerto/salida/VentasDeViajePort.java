package com.ventas.key.hexagonal.flete.dominio.puerto.salida;

import com.ventas.key.hexagonal.flete.dominio.modelo.VentaLigada;

import java.util.List;

/** [Hexagonal: Driven Port] Las piezas vendidas de un viaje (ventas no devueltas). Vacío si todavía no existe la tabla de ligas. */
public interface VentasDeViajePort {
    List<VentaLigada> deViaje(int viajeId);
}
