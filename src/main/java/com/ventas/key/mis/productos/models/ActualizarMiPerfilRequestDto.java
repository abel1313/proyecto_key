package com.ventas.key.mis.productos.models;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ActualizarMiPerfilRequestDto {

    @NotBlank(message = "El nombre de usuario es obligatorio")
    private String username;
}
