package com.ventas.key.mis.productos.redessociales;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PublicacionSocialDto {

    private Integer id;
    private Integer varianteId;
    private String plataforma;
    private String tipoPublicacion;
    private String descripcionPublicada;
    private String postIdFacebook;
    private LocalDateTime scheduledPublishTime;
    private LocalDateTime fechaPublicacion;
    private String estado;
    private Integer intentos;
    private String ultimoError;

    public static PublicacionSocialDto from(PublicacionSocial p) {
        return new PublicacionSocialDto(
                p.getId(),
                p.getVariante() != null ? p.getVariante().getId() : null,
                p.getPlataforma(),
                p.getTipoPublicacion(),
                p.getDescripcionPublicada(),
                p.getPostIdFacebook(),
                p.getScheduledPublishTime(),
                p.getFechaPublicacion(),
                p.getEstado(),
                p.getIntentos(),
                p.getUltimoError()
        );
    }
}
