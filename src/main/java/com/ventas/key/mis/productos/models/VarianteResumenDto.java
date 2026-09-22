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

    /**
     * El tercer precio: la rebaja que maneja el admin.
     *
     * <p><b>Solo viaja para el admin.</b> Null para un cliente -- la rebaja no se publica en el
     * catalogo (R6 del dominio `promocion`): es un precio que alguien decide aplicar en una venta,
     * no un precio de lista. El cliente si la ve despues en su pedido, porque ahi es lo que
     * realmente pago.
     *
     * <p>Sin este campo la card de tienda no tenia con que ofrecer el precio de rebaja, aunque el
     * cobro ya lo aceptaba desde el 2026-09-22.
     */
    private Double precioRebaja;
    private String codigoBarras;
    private String nombreProducto;
    private char habilitado;
    // Null en variantes creadas antes de la migracion -- ver comentario en Variantes.java.
    private LocalDateTime fechaCreacion;
}