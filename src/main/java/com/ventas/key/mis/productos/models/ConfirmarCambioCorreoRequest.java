package com.ventas.key.mis.productos.models;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ConfirmarCambioCorreoRequest {

    @NotBlank(message = "El codigo de verificacion es obligatorio")
    private String codigo;
}
