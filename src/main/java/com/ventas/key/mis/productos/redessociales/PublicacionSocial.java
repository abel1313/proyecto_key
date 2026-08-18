package com.ventas.key.mis.productos.redessociales;

import com.ventas.key.mis.productos.entity.BaseId;
import com.ventas.key.mis.productos.entity.productoVariantes.Variantes;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "publicacion_social")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class PublicacionSocial extends BaseId {

    @ManyToOne
    @JoinColumn(name = "variante_id")
    private Variantes variante;

    private String plataforma;

    @Column(name = "tipo_publicacion")
    private String tipoPublicacion;

    @Column(name = "descripcion_publicada", columnDefinition = "TEXT")
    private String descripcionPublicada;

    @Column(name = "imagen_id")
    private Long imagenId;

    @Column(name = "post_id_facebook")
    private String postIdFacebook;

    @Column(name = "scheduled_publish_time")
    private LocalDateTime scheduledPublishTime;

    @Column(name = "fecha_publicacion")
    private LocalDateTime fechaPublicacion;

    private String estado;
}
