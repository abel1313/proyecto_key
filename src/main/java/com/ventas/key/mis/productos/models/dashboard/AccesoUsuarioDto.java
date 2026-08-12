package com.ventas.key.mis.productos.models.dashboard;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AccesoUsuarioDto {
    private Integer usuarioId;
    private String username;
    private LocalDateTime fechaLogin;
    private LocalDateTime ultimaActividad;
    // Aproximada: se mide contra la ultima vez que el refresh token roto con exito (~cada 15
    // min mientras la sesion sigue activa), no contra clicks reales del usuario.
    private Long duracionMinutosAprox;
}
