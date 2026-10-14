package com.ventas.key.hexagonal.codigopostal.dominio.puerto.salida;

import com.ventas.key.hexagonal.codigopostal.dominio.modelo.CodigoPostalInfo;

import java.util.Optional;

/**
 * [Hexagonal: Driven Port] [Clean: Interface Adapter]
 *
 * <p>El servicio de afuera que sabe de códigos postales.
 */
public interface CodigoPostalExternoPort {

    /**
     * @return vacío si el servicio dice que el código no existe (R3)
     * @throws com.ventas.key.hexagonal.codigopostal.dominio.excepcion.CodigoPostalException (SERVICIO_NO_DISPONIBLE) si falla, tarda
     *         de más o rechaza la llave (R4)
     */
    Optional<CodigoPostalInfo> consultar(String codigo);
}
