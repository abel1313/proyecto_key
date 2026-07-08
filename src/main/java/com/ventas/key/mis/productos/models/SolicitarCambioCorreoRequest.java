package com.ventas.key.mis.productos.models;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SolicitarCambioCorreoRequest {

    @NotBlank(message = "El correo nuevo es obligatorio")
    @Email(message = "El correo no tiene un formato valido")
    private String correoNuevo;
}
