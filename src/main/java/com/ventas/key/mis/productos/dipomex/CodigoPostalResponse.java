package com.ventas.key.mis.productos.dipomex;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
public class CodigoPostalResponse {

    private boolean error;
    private String message;
    private CodigoPostal codigo_postal;

}
