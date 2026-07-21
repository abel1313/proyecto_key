package com.ventas.key.mis.productos.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "producto")
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Producto  extends BaseId{

    private String nombre;
    @Column(name = "precio_costo")
    private Double precioCosto;
    private Double piezas;
    private String color;
    @Column(name = "precio_venta")
    private Double precioVenta;
    @Column(name = "precio_rebaja")
    private Double precioRebaja;
    private String descripcion;
    private Integer stock;
    private String marca;
    @Column(name = "contenido_neto")
    private String contenido;

    private char habilitado;

    // true = el codigo_barras asignado fue autogenerado por la carga rapida de imagenes
    // (aun no es el codigo real). Se limpia a false en cuanto el front manda el codigo real
    // via /v1/carga-imagenes/{productoId}/completar, que ademas borra el codigo placeholder.
    @Column(name = "codigo_barras_generado")
    private Boolean codigoBarrasGenerado = false;

    // Estado de la imagen de la carga rapida (null en productos normales, no creados por
    // ese flujo). PENDIENTE mientras la imagen se sube en background al microservicio,
    // EXITOSO/FALLIDO cuando termina. Ver /v1/carga-imagenes/*.
    @Enumerated(EnumType.STRING)
    @Column(name = "estado_imagen")
    private EstadoCargaImagen estadoImagen;

    @Column(name = "mensaje_error_imagen")
    private String mensajeErrorImagen;

    @OneToOne(optional = true, cascade = CascadeType.MERGE)
    @JoinColumn(name = "codigo_barras_id", unique = true)
    private CodigoBarra codigoBarras;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "palabra_clave_id")
    private PalabraClave palabraClave;
}
