package com.ventas.key.mis.productos.models.pedidos;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DetalleItemResponse {
    private Integer id;
    private Integer productoId;
    private Integer varianteId;
    private String productoNombre;
    private String talla;
    private String color;
    private String descripcion;
    private Integer cantidad;
    private Double precioUnitario;
    private Double subTotal;
    private Integer promocionId;
    private String promocionDescripcion;
    // true si es una linea que el cliente no deberia ver por separado (ej. el papel de un ramo
    // de flores eternas, que va incluido en el precio de las flores) -- el front la agrupa o la
    // esconde en vez de mostrarla como renglon suelto. false/ausente en cualquier otra linea.
    private Boolean esLineaInterna;
}
