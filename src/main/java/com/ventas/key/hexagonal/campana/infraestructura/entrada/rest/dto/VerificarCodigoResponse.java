package com.ventas.key.hexagonal.campana.infraestructura.entrada.rest.dto;

import com.ventas.key.hexagonal.campana.dominio.modelo.VerificacionDeCodigo;

/** Estado: {@code OK}, {@code NO_COINCIDE} (el código es de otra persona) o {@code NO_EXISTE}. Los montos solo vienen con {@code OK}. */
public record VerificarCodigoResponse(String estado, String nombre, double acumulado, double meta, double falta) {

    public static VerificarCodigoResponse de(VerificacionDeCodigo v) {
        return new VerificarCodigoResponse(v.estado().name(), v.nombre(), v.acumulado().doubleValue(), v.meta().doubleValue(), v.falta().doubleValue());
    }
}
