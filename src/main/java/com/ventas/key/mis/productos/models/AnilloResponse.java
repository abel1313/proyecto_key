package com.ventas.key.mis.productos.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AnilloResponse {
    private Integer id;
    private Integer lugarEntregaId;
    private Double radioMetros;
    private Double costoEnvio;
    private Integer orden;
}
