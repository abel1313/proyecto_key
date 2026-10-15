package com.ventas.key.hexagonal.datoslegales.dominio.puerto.salida;

import com.ventas.key.hexagonal.datoslegales.dominio.modelo.DatosLegales;

import java.util.Optional;

/** [Hexagonal: Driven Port] [Clean: Interface Adapter] — dónde se guardan los datos del negocio. */
public interface DatosLegalesPort {
    Optional<DatosLegales> leer();

    void guardar(DatosLegales datos);
}
