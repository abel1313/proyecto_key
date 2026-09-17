package com.ventas.key.mis.productos.models;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * El premio como lo ve un visitante sin sesión en la página pública de la rifa.
 *
 * Va aparte de {@link VarianteResumenDto} porque ese lleva stock, precio y código de
 * barras -- datos internos que no tienen por qué salir a una página que se comparte
 * por link. Aquí solo van los campos que describen lo que se va a ganar y todas sus
 * fotos, para el carrusel del detalle.
 */
@Getter
@Setter
public class PremioPublicoDto {

    private Integer id;
    private String nombreProducto;
    private String descripcion;
    private String talla;
    private String color;
    private String marca;
    private String presentacion;
    private String contenidoNeto;

    /**
     * URLs al micro de imágenes listas para {@code <img [src]>}, la principal primero.
     * Son URLs y no data URIs a propósito: el binario en base64 pesa ~33% más y el navegador
     * no lo puede cachear, así que un carrusel de cinco fotos se comía el plan de datos del
     * celular en cada apertura.
     */
    private List<String> imagenes = new ArrayList<>();
}
