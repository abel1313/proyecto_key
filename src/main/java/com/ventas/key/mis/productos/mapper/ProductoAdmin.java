package com.ventas.key.mis.productos.mapper;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ProductoAdmin extends ProductoUser {

    private Double precioCosto;
    private Double piezas;
    private Double precioRebaja;
    private String marca;
    private String contenido;
    private char habilitado;
    // Solo admin -- ver comentario en Producto.java sobre por que existe (carga rapida con
    // codigo de barras al azar) y por que puede venir null (registros previos a la migracion).
    private LocalDateTime fechaCreacion;
}
