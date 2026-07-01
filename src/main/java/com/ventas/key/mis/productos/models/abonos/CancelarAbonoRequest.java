package com.ventas.key.mis.productos.models.abonos;

import com.ventas.key.mis.productos.models.NotificacionRequest;
import lombok.Data;

@Data
public class CancelarAbonoRequest {
    private String motivo;
    private NotificacionRequest notificacion;
}
