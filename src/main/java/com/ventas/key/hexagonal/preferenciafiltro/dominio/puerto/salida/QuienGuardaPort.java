package com.ventas.key.hexagonal.preferenciafiltro.dominio.puerto.salida;

import com.ventas.key.hexagonal.preferenciafiltro.dominio.modelo.QuienGuarda;

/**
 * [Hexagonal: Driven Port] [Clean: Interface Adapter]
 *
 * <p>El usuario autenticado de la petición (sale del token, nunca del body).
 */
public interface QuienGuardaPort {

    QuienGuarda actual();
}
