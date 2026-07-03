package com.ventas.key.mis.productos.models;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VerificarCorreoRequest {

    @NotBlank(message = "El codigo es obligatorio")
    private String codigo;
}
