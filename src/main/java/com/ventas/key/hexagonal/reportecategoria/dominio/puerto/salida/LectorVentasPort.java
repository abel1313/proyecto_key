package com.ventas.key.hexagonal.reportecategoria.dominio.puerto.salida;

import com.ventas.key.hexagonal.reportecategoria.dominio.modelo.FilaBase;
import com.ventas.key.hexagonal.reportecategoria.dominio.modelo.FiltroReporte;

import java.util.List;

/** [Hexagonal: Driven Port] Los productos del filtro con lo vendido en las fechas (sin ventas devueltas), vendidos o no. */
public interface LectorVentasPort {
    List<FilaBase> leer(FiltroReporte filtro);
}
