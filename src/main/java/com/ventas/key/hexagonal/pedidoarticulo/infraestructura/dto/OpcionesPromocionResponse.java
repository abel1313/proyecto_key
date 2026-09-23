package com.ventas.key.hexagonal.pedidoarticulo.infraestructura.dto;

import com.ventas.key.hexagonal.pedidoarticulo.dominio.excepcion.CambioRompePromocionException;

import java.util.List;

/**
 * Las dos salidas cuando el articulo nuevo no pertenece a la promocion (R4).
 *
 * <p>[Hexagonal: Driving Adapter] [Clean: Interface Adapter]
 *
 * <p>Esto <b>no es un mensaje de error</b>: es lo que el modal necesita para preguntar. Por eso
 * viaja con estructura (que lineas se irian, cuanto suman) y no como un texto que el front tendria
 * que parsear.
 *
 * @param requiereDecision siempre true; lo lleva para que el front distinga esta respuesta de un
 *                         rechazo comun sin mirar el status
 * @param opciones         los valores que se pueden mandar en {@code modo} para resolverlo
 */
public record OpcionesPromocionResponse(
        boolean requiereDecision,
        String mensaje,
        Integer promocionId,
        String promocion,
        String articuloNuevo,
        double importeDelCombo,
        List<PedidoArticulosResponse.LineaResponse> lineasDelCombo,
        List<Opcion> opciones) {

    public static OpcionesPromocionResponse de(CambioRompePromocionException e) {
        return new OpcionesPromocionResponse(
                true,
                e.getMessage(),
                e.promocion().promocionId(),
                e.promocion().descripcion(),
                e.nombreArticuloNuevo(),
                e.importeDelCombo(),
                e.lineasDelCombo().stream().map(PedidoArticulosResponse.LineaResponse::de).toList(),
                List.of(
                        new Opcion("QUITAR_PROMOCION", "Quitar la promocion completa",
                                "Salen las " + e.lineasDelCombo().size() + " linea(s) de la promocion '"
                                        + e.promocion().descripcion() + "' y entra '" + e.nombreArticuloNuevo()
                                        + "' a precio normal. Lo demas del pedido no se toca"),
                        new Opcion("CONSERVAR_PROMOCION", "Conservarla y agregarlo aparte",
                                "La promocion queda como esta y '" + e.nombreArticuloNuevo()
                                        + "' se suma como una linea nueva a precio normal")));
    }

    /**
     * @param modo        lo que hay que mandar en el campo {@code modo} del request para elegirla
     * @param titulo      el texto del boton
     * @param explicacion que pasa si se elige, para ponerlo debajo del boton
     */
    public record Opcion(String modo, String titulo, String explicacion) {}
}
