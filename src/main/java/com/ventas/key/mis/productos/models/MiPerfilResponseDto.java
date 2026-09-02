package com.ventas.key.mis.productos.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** Self-service: lo que el propio usuario puede ver de su cuenta (no editar). */
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class MiPerfilResponseDto {
    private String username;
    private String email;
    private Boolean aceptoPrivacidad;
    private LocalDateTime fechaAceptoPrivacidad;
}
