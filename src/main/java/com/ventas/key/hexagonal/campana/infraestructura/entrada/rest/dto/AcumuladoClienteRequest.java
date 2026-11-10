package com.ventas.key.hexagonal.campana.infraestructura.entrada.rest.dto;

/**
 * {@code POST /v1/campanas/personas/acumulado}: los datos del cliente de la venta (sin código) para saber lo que lleva pagado en la campaña vigente.
 * Todos opcionales; se reconoce por correo, teléfono o cliente (registrado o sin registro).
 */
public record AcumuladoClienteRequest(String correo, String telefono, Integer clienteId, Integer clienteSinRegistroId) {
}
