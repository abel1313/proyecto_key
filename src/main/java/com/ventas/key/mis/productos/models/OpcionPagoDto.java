package com.ventas.key.mis.productos.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OpcionPagoDto {

    private Integer tipoPagoId;
    private String formaPago;
    private boolean mostrarMeses;

    // Si mostrarMeses=false → este campo tiene el id listo para mandar directo
    private Integer pagosYMesesId;

    // Si mostrarMeses=true → el frontend muestra estas opciones al usuario
    private List<OpcionMesesDto> opciones;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class OpcionMesesDto {
        private Integer pagosYMesesId;
        private String descripcion;
    }
}