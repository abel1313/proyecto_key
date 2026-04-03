package com.ventas.key.mis.productos.hexagonal.dominio.mapper;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class RequestProductoImagen {

    private Integer id;
    private Long imagenId;
    private Integer productoId;

}
