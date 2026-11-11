package com.ventas.key.hexagonal.flete.dominio.puerto.entrada;

import com.ventas.key.hexagonal.flete.dominio.modelo.LineaVendida;

import java.util.List;

/** [Hexagonal: Driving Port] Se llama justo después de guardar una venta: liga cada línea a los viajes de donde salieron sus piezas. Nunca debe tirar la venta. */
public interface AsignarViajeAVentaCasoUso {
    void asignar(List<LineaVendida> lineas);
}
