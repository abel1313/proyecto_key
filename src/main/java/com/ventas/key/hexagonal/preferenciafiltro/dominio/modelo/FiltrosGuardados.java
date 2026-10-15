package com.ventas.key.hexagonal.preferenciafiltro.dominio.modelo;

import com.ventas.key.hexagonal.preferenciafiltro.dominio.excepcion.PreferenciaFiltroException;

import java.time.LocalDateTime;

/**
 * [Hexagonal: dentro del hexagono] [Clean: Entities]
 *
 * <p>Los filtros que un usuario dejó puestos en una pantalla. El contenido es opaco para el back
 * (lo arma el front); aquí solo se cuida que sea un objeto JSON de tamaño razonable (R5).
 */
public record FiltrosGuardados(int usuarioId, Pantalla pantalla, String filtrosJson, LocalDateTime actualizado) {

    public static final int MAX_CARACTERES = 2000;

    public FiltrosGuardados {
        if (pantalla == null) {
            throw PreferenciaFiltroException.pantallaDesconocida(null);
        }
        String json = filtrosJson == null ? "" : filtrosJson.trim();
        if (!json.startsWith("{") || !json.endsWith("}")) {
            throw PreferenciaFiltroException.noEsObjeto();
        }
        if (json.length() > MAX_CARACTERES) {
            throw PreferenciaFiltroException.demasiadoGrandes(json.length());
        }
        filtrosJson = json;
    }

    /** R7: sin nada adentro equivale a "Limpiar". */
    public static boolean estanVacios(String filtrosJson) {
        return filtrosJson == null || filtrosJson.replaceAll("\\s", "").equals("{}") || filtrosJson.isBlank();
    }
}
