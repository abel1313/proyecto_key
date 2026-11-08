package com.ventas.key.hexagonal.campana.infraestructura.entrada.rest.dto;

import com.ventas.key.hexagonal.campana.dominio.modelo.ClienteDeCodigo;

/**
 * A quién pertenece el código. {@code valido = false} si no existe. {@code clienteId} / {@code clienteSinRegistroId}: el cliente ligado (puede venir
 * vacío en códigos anteriores al ligado; entonces el front deja elegir al cliente y se comprueba por correo o teléfono). {@code acumulado} y
 * {@code meta} son de la campaña vigente (0 si no hay).
 */
public record BuscarPorCodigoResponse(boolean valido, Integer clienteId, Integer clienteSinRegistroId, String nombre, String correo,
                                      String telefono, double acumulado, double meta, double falta) {

    public static BuscarPorCodigoResponse de(ClienteDeCodigo c) {
        if (!c.valido()) {
            return new BuscarPorCodigoResponse(false, null, null, "", null, null, 0, 0, 0);
        }
        var p = c.persona();
        return new BuscarPorCodigoResponse(true, p.clienteId(), p.clienteSinRegistroId(), p.nombre(), p.correo(), p.telefono(),
                c.acumulado().doubleValue(), c.meta().doubleValue(), c.falta().doubleValue());
    }
}
