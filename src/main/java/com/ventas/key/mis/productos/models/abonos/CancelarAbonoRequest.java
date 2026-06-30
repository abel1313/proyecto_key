package com.ventas.key.mis.productos.models.abonos;

import lombok.Data;

@Data
public class CancelarAbonoRequest {
    private String motivo; // opcional — razón de la cancelación
}
