package com.ventas.key.mis.productos.models;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class EnviarCodigoVerificacionUsuarioRequest {

    @NotBlank(message = "El nombre de usuario o correo es obligatorio")
    private String userName;
}
