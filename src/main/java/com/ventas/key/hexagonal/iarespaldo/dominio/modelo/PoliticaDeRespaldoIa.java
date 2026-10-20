package com.ventas.key.hexagonal.iarespaldo.dominio.modelo;

import java.util.List;

/**
 * Reglas para pasar de un modelo de IA a otro cuando el principal no contesta (ver
 * {@code iarespaldo/README.md}). Java puro: no sabe de Spring ni de HTTP.
 */
public final class PoliticaDeRespaldoIa {

    private PoliticaDeRespaldoIa() {
    }

    /**
     * Clasifica un fallo.
     *
     * @param httpStatus código HTTP que contestó el proveedor, o {@code null} si no contestó
     *                   (tiempo agotado, sin conexión, respuesta vacía)
     * @param cuerpo     texto de la respuesta de error (puede ser {@code null}); OpenAI avisa que se
     *                   acabó el saldo con un 429 y la frase "insufficient_quota"
     */
    public static TipoDeFallo clasificar(Integer httpStatus, String cuerpo) {
        if (httpStatus == null) {
            return TipoDeFallo.CAIDO;
        }
        int s = httpStatus;
        if (s == 429) {
            String c = cuerpo == null ? "" : cuerpo.toLowerCase();
            return c.contains("insufficient_quota") || c.contains("exceeded your current quota")
                    || c.contains("insufficient balance")
                    ? TipoDeFallo.SIN_SALDO_O_LLAVE : TipoDeFallo.LIMITE_TEMPORAL;
        }
        if (s == 401 || s == 402 || s == 403) {
            return TipoDeFallo.SIN_SALDO_O_LLAVE;
        }
        if (s >= 500) {
            return TipoDeFallo.CAIDO;
        }
        return TipoDeFallo.NUESTRO;
    }

    /** El orden en que se prueban: el principal primero y, después, solo los respaldos que sí tienen llave. */
    public static List<ProveedorIa> ordenDePrueba(ProveedorIa principal, ProveedorIa respaldo) {
        if (respaldo != null && respaldo.configurado()) {
            return List.of(principal, respaldo);
        }
        return List.of(principal);
    }
}
