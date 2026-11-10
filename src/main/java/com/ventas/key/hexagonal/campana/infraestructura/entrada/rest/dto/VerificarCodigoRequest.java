package com.ventas.key.hexagonal.campana.infraestructura.entrada.rest.dto;

/** {@code POST /v1/campanas/personas/verificar}: el código y los datos del cliente de la venta (con ellos se comprueba que el código es suyo). */
public record VerificarCodigoRequest(String codigo, String correo, String telefono) {
}
