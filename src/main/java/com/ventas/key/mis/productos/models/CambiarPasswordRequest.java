package com.ventas.key.mis.productos.models;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CambiarPasswordRequest {

    @NotBlank(message = "La contrasena actual es obligatoria")
    private String passwordActual;

    @NotBlank(message = "La nueva contrasena es obligatoria")
    @Size(min = 3, max = 200, message = "La contrasena debe tener entre 6 y 200 caracteres")
    private String nuevaPassword;
}
