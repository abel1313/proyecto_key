package com.ventas.key.hexagonal.codigopostal.dominio.puerto.entrada;

import com.ventas.key.hexagonal.codigopostal.dominio.modelo.CodigoPostalInfo;

/**
 * [Hexagonal: Driving Port] [Clean: Use Case Input Boundary]
 */
public interface BuscarCodigoPostalCasoUso {

    /** @throws com.ventas.key.hexagonal.codigopostal.dominio.excepcion.CodigoPostalException si no es válido, no existe o no hay servicio */
    CodigoPostalInfo buscar(String codigo);
}
