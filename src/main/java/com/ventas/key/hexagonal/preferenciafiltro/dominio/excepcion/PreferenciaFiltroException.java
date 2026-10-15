package com.ventas.key.hexagonal.preferenciafiltro.dominio.excepcion;

import com.ventas.key.hexagonal.preferenciafiltro.dominio.modelo.FiltrosGuardados;

/**
 * [Hexagonal: dentro del hexagono] [Clean: Entities]
 *
 * <p>Petición inválida: el controller la devuelve como 400 con este mensaje.
 */
public class PreferenciaFiltroException extends RuntimeException {

    protected PreferenciaFiltroException(String mensaje) {
        super(mensaje);
    }

    public static PreferenciaFiltroException pantallaDesconocida(String clave) {
        return new PreferenciaFiltroException("La pantalla '" + clave + "' no guarda filtros. "
                + "Solo tienda-buscar y productos-buscar");
    }

    public static PreferenciaFiltroException noEsObjeto() {
        return new PreferenciaFiltroException("Los filtros tienen que ser un objeto JSON, por ejemplo {\"filtroTalla\":\"M\"}");
    }

    public static PreferenciaFiltroException demasiadoGrandes(int caracteres) {
        return new PreferenciaFiltroException(String.format(
                "Los filtros miden %d caracteres y el máximo es %d. Solo se guardan los filtros, "
                        + "no el texto buscado ni los resultados", caracteres, FiltrosGuardados.MAX_CARACTERES));
    }
}
