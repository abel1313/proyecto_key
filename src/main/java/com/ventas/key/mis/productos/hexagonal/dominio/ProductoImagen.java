package com.ventas.key.mis.productos.hexagonal.dominio;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ProductoImagen {
    private Integer id;
    private Long imagenId;
    private Integer productoId;


}
