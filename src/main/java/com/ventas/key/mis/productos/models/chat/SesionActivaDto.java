package com.ventas.key.mis.productos.models.chat;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SesionActivaDto {
    private String sesionId;
    private String nombreUsuario;
    private String estado;
    private String fechaInicio;
    private String ultimaActividad;
    private String ultimoMensaje;
}
