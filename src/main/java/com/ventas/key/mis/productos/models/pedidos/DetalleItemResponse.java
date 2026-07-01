package com.ventas.key.mis.productos.models.pedidos;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DetalleItemResponse {
    private Integer id;
    private Integer varianteId;
    private String productoNombre;
    private String talla;
    private String color;
    private String descripcion;
    private Integer cantidad;
    private Double precioUnitario;
    private Double subTotal;
}
