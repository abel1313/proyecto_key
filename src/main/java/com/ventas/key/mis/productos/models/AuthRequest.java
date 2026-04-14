package com.ventas.key.mis.productos.models;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AuthRequest {

    @NotBlank(message = "El nombre de usuario es obligatorio")
    private String userName;

    @NotBlank(message = "La contraseña es obligatoria")
    private String password;

    private String email;
}
