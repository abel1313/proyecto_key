package com.ventas.key.mis.productos.models;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ConcursanteEditarRequest {
    private String nombre;
    private String apellidoPaterno;
    private String telefono;
    private String palabraClave;
    private Integer ordenDesde;
}
