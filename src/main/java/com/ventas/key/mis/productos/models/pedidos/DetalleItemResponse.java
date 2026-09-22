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

    // Para que el cliente reconozca lo que compro sin tener que abrir cada renglon: hasta ahora
    // el detalle solo traia nombre, talla y color, y con varios modelos parecidos no alcanzaba.
    // urlImagen apunta a la miniatura del micro de imagenes (no al archivo completo): es un
    // listado, y la foto original puede pesar megas. Van los tres como NON_NULL: un producto sin
    // codigo de barras o sin foto simplemente no manda el campo.
    private String codigoBarras;
    private Long imagenId;
    private String urlImagen;
    // true si es una linea que el cliente no deberia ver por separado (ej. el papel de un ramo
    // de flores eternas, que va incluido en el precio de las flores) -- el front la agrupa o la
    // esconde en vez de mostrarla como renglon suelto. false/ausente en cualquier otra linea.
    private Boolean esLineaInterna;
}
