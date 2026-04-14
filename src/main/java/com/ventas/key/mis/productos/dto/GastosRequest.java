package com.ventas.key.mis.productos.dto;

import lombok.*;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class GastosRequest {

    private Integer id;
    private String descripcionGasto;
    private Double precioGasto;



}
