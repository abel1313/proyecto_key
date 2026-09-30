package com.ventas.key.mis.productos.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VarianteResumenDto {
    private Integer id;
    private Integer productoId;
    private String talla;
    private String descripcion;
    private String color;
    private String presentacion;
    private int stock;
    private String marca;
    private String contenidoNeto;
    /** URL al micro de imagenes -- el navegador la resuelve y la cachea. Nunca el binario. */
    private String imagenUrl;
    private double precio;

    /** Solo admin: true si el articulo tiene precio propio y no el de su producto (2026-09-29). */
    private Boolean precioPropio;
    /**
     * Solo admin: el precio normal. Con {@link #usarDescuento} en true, {@code precio} trae el
     * descuento (es a lo que se vende) y aqui queda el normal, para cobrarlo en una venta puntual.
     */
    private Double precioNormal;
    /** Solo admin: R8, el articulo se vende al descuento (check "Precio descuento" de la card). */
    private Boolean usarDescuento;
    private String codigoBarras;
    private String nombreProducto;
    private char habilitado;
    // Null en variantes creadas antes de la migracion -- ver comentario en Variantes.java.
    private LocalDateTime fechaCreacion;
}