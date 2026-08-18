package com.ventas.key.mis.productos.redessociales;

import lombok.Getter;
import lombok.Setter;

// Primera version, alcance recortado a proposito (igual que se hizo con Facebook en su momento):
// solo foto ya guardada en el catalogo, sin archivo ad-hoc y sin programar. Instagram necesita una
// URL publica para la imagen (no acepta bytes subidos directo como Facebook), y su Content
// Publishing API no soporta scheduled_publish_time -- publica siempre de inmediato.
@Getter
@Setter
public class PublicarInstagramRequest {

    private Integer varianteId;

    private String descripcion;

    /** Si es null, se usa la imagen principal ya guardada de la variante. */
    private Long imagenId;
}
