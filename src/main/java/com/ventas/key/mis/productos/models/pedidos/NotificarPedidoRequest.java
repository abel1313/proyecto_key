package com.ventas.key.mis.productos.models.pedidos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class NotificarPedidoRequest {
    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "El correo debe tener un formato valido")
    private String correo;

    @NotBlank(message = "El ticketHtml es obligatorio")
    private String ticketHtml;
}
