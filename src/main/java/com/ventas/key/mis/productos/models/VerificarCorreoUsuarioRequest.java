package com.ventas.key.mis.productos.models;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VerificarCorreoUsuarioRequest {

    @NotBlank(message = "El nombre de usuario o correo es obligatorio")
    private String userName;

    @NotBlank(message = "El codigo es obligatorio")
    private String codigo;
}
