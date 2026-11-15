package com.ventas.key.hexagonal.campana.infraestructura.entrada.rest.dto;

import lombok.Data;

/** Datos opcionales al mandar el codigo de campana desde un pedido: el correo (si no se escribe, el de la persona), y nombre y telefono si hay que crear a la persona. */
@Data
public class EnviarCodigoPedidoRequest {
    private String correo;
    private String nombre;
    private String telefono;
}
