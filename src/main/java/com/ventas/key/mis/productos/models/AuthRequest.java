package com.ventas.key.mis.productos.models;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AuthRequest {

    @NotBlank(message = "El nombre de usuario es obligatorio")
    @Size(min = 3, max = 100, message = "El nombre de usuario debe tener entre 3 y 100 caracteres")
    private String userName;

    @NotBlank(message = "La contrasena es obligatoria")
    // Se queda en 3 a proposito: subir el minimo en el LOGIN no aporta seguridad y dejaria fuera
    // a los usuarios que ya tienen una contrasena corta de antes. El minimo de 8 aplica al crear
    // o cambiar la contrasena (registro, cambio y reset).
    @Size(min = 3, max = 200, message = "La contrasena debe tener entre 3 y 200 caracteres")
    private String password;

    @Email(message = "El email debe tener un formato valido")
    @Size(max = 150, message = "El email no puede superar los 150 caracteres")
    private String email;
}
