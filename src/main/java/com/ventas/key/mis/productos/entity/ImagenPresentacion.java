package com.ventas.key.mis.productos.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "imagen_presentacion")
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ImagenPresentacion extends BaseId {

    @Column(nullable = false, length = 20)
    private String tipo;

    @Column(nullable = false)
    private Integer orden;

    /** Nombre de archivo en disco (UUID + nombre original), no URL */
    @Column(name = "url_imagen", length = 500, nullable = false)
    private String nombreArchivo;

    @Column(length = 10)
    private String extension;

    @Column(name = "nombre_original", length = 200)
    private String nombreOriginal;

    @Column(length = 300)
    private String descripcion;

    private boolean activo;

    @Column(name = "actualizado_en")
    private LocalDateTime actualizadoEn;
}