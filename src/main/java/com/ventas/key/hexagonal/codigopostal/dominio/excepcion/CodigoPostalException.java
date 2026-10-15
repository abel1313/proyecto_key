package com.ventas.key.hexagonal.codigopostal.dominio.excepcion;

/**
 * [Hexagonal: dentro del hexágono] [Clean: Entities]
 *
 * <p>Por qué no se pudo dar el municipio de un código. El controller traduce {@link Motivo}:
 * NO_VALIDO y NO_EXISTE → 404, SERVICIO_NO_DISPONIBLE → 502. La pantalla en los tres casos deja
 * escribir el municipio a mano (R4).
 */
public class CodigoPostalException extends RuntimeException {

    public enum Motivo { NO_VALIDO, NO_EXISTE, SERVICIO_NO_DISPONIBLE }

    private final Motivo motivo;

    private CodigoPostalException(Motivo motivo, String mensaje) {
        super(mensaje);
        this.motivo = motivo;
    }

    public Motivo motivo() {
        return motivo;
    }

    public static CodigoPostalException noValido() {
        return new CodigoPostalException(Motivo.NO_VALIDO, "El código postal debe tener 5 dígitos");
    }

    public static CodigoPostalException noExiste(String codigo) {
        return new CodigoPostalException(Motivo.NO_EXISTE, "No encontramos el código postal " + codigo);
    }

    public static CodigoPostalException servicioNoDisponible() {
        return new CodigoPostalException(Motivo.SERVICIO_NO_DISPONIBLE,
                "No pudimos buscar el código postal en este momento. Escribe el municipio a mano");
    }
}
