package com.ventas.key.mis.productos.models.floreseternas;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FechasDisponiblesRequestDto {
    // El front no lo propuso en su spec (solo "cantidad"), pero CantidadFlorValida es por
    // (tipoFlor, cantidad) -- sin la especie no se puede saber contra que catalogo de tamanos
    // buscar el redondeo hacia arriba. Obligatorio.
    private Integer tipoFlorId;
    private Integer cantidad;
    private Integer lugarEntregaId;
    private Boolean urgente;
}
