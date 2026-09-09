package com.ventas.key.mis.productos.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

// Catálogo de logos subidos por el admin (pedido 2026-08-28: antes no existía ningún archivo de
// logo en el proyecto -- ver comentario histórico en EmailService.encabezadoMarca()). Soporta
// varios logos a la vez ("por si tuviera más"); `activo` marca cuál de todos es el que se usa hoy
// en el encabezado de los correos -- se fuerza a que solo uno esté activo a la vez (ver
// LogoService.activar()), no es un multi-select como ImagenPresentacion.
//
// Entidad nueva en vez de reusar ImagenPresentacion: esa tabla está marcada @Deprecated
// ("Migrar a micro_imagenes, no agregar nueva lógica aquí" -- ver IImagenPresentacionRepository).
@Entity
@Table(name = "logo")
@Getter @Setter @AllArgsConstructor @NoArgsConstructor
public class Logo extends BaseId {

    /** Nombre de archivo en disco (UUID + nombre original), no URL. */
    @Column(name = "nombre_archivo", length = 300, nullable = false)
    private String nombreArchivo;

    @Column(length = 10)
    private String extension;

    @Column(name = "nombre_original", length = 200)
    private String nombreOriginal;

    /** true = este es el logo que se usa hoy en el encabezado de los correos. Único a la vez. */
    private boolean activo;

    @Column(name = "creado_en")
    private LocalDateTime creadoEn;
}
