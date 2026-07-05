package com.ventas.key.mis.productos.models;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RestablecerPasswordRequest {

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El email debe tener un formato valido")
    private String email;

    @NotBlank(message = "El codigo es obligatorio")
    private String codigo;

    @NotBlank(message = "La nueva contrasena es obligatoria")
    @Size(min = 3, max = 200, message = "La contrasena debe tener entre 6 y 200 caracteres")
    private String nuevaPassword;
}
