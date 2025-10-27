package com.ventas.key.mis.productos.dipomex;


import lombok.*;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
public class CodigoPostal {

    private String estado_id;
    private String municipio_id;
    private String estado;
    private String estado_abreviatura;
    private String municipio;
    private String centro_reparto;
    private String codigo_postal;
    private List<String> colonias;

}
