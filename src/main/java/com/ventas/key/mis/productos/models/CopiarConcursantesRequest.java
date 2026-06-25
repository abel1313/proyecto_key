package com.ventas.key.mis.productos.models;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CopiarConcursantesRequest {
    private Integer rifaOrigenId;
    private Integer rifaDestinoId;
    private String palabraClave;
}
