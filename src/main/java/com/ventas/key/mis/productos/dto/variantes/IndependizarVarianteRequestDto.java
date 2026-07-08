package com.ventas.key.mis.productos.dto.variantes;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class IndependizarVarianteRequestDto {

    private String nombre;
    private String descripcion;
    private String marca;
    private String color;
    private String contenido;
    private Double precioCosto;
    private Double precioVenta;
    private Double precioRebaja;
    private Integer palabraClaveId;
    private String codigoBarras;
    private Long imagenPrincipalId;
}
