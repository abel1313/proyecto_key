package com.ventas.key.hexagonal.pagotarjeta.infraestructura.entrada.rest.dto;

/**
 * Lo unico que manda el front para cobrar. NUNCA el monto ni el tope de cuotas (T2): el back los calcula.
 *
 * @param canal  {@code TERMINAL} (solo administrador) o {@code EN_LINEA}
 * @param cuotas 1 = de contado; 3, 6, 9 o 12 = meses sin intereses (se validan contra el pedido)
 */
public record IniciarCobroRequest(Integer pedidoId, String canal, Integer cuotas) {
}
