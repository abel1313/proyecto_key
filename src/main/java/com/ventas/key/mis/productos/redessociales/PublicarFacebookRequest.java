package com.ventas.key.mis.productos.redessociales;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** Carga los campos de texto del multipart entre el controller y el service. */
@Getter
@Setter
public class PublicarFacebookRequest {

    private Integer varianteId;

    private String descripcion;

    /** Si es null y no viene imagenNueva, se usa la imagen principal ya guardada de la variante. */
    private Long imagenId;

    /** Si es null, se publica de inmediato. Si viene, se programa (mínimo 10 min, máximo 6 meses a futuro). */
    private LocalDateTime scheduledPublishTime;
}
