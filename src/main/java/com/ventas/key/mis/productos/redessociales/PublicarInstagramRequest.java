package com.ventas.key.mis.productos.redessociales;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

// Alcance recortado a proposito: solo foto ya guardada en el catalogo, sin archivo ad-hoc --
// Instagram necesita una URL publica para la imagen (no acepta bytes subidos directo como
// Facebook). Instagram NO tiene programacion nativa (limite real de su API, sin importar
// scheduledPublishTime): si viene, se guarda como "PROGRAMADA" y la publica nuestro propio job
// (PublicacionSocialScheduler) a esa hora -- ver PublicacionSocialService.
@Getter
@Setter
public class PublicarInstagramRequest {

    private Integer varianteId;

    private String descripcion;

    /** Si es null, se usa la imagen principal ya guardada de la variante. */
    private Long imagenId;

    /** Si es null, se publica de inmediato. Si viene, se programa (mínimo 10 min de anticipación). */
    private LocalDateTime scheduledPublishTime;
}
