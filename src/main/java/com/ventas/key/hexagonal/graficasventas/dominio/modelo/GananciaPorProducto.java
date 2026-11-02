package com.ventas.key.hexagonal.graficasventas.dominio.modelo;

import java.time.LocalDate;
import java.util.List;

/**
 * Los productos que más ganancia dejaron en el periodo, del que más al que menos. {@code gananciaTotal} es la de TODOS los
 * productos vendidos (no solo los que salen en la lista), para poder decir qué parte representan.
 */
public record GananciaPorProducto(LocalDate desde, LocalDate hasta, double gananciaTotal, double gananciaDeLosMejores,
                                  List<ProductoRentable> productos) {
}
