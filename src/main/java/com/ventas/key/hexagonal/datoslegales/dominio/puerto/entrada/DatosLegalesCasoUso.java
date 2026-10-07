package com.ventas.key.hexagonal.datoslegales.dominio.puerto.entrada;

import com.ventas.key.hexagonal.datoslegales.dominio.modelo.DatosLegales;

/** [Hexagonal: Driving Port] [Clean: Use Case Boundary] */
public interface DatosLegalesCasoUso {
    /** Los datos capturados; vacíos si todavía no se llenan. */
    DatosLegales consultar();

    DatosLegales guardar(DatosLegales datos);
}
