package com.ventas.key.mis.productos.entity.productoVariantes;

import com.ventas.key.mis.productos.entity.BaseId;
import com.ventas.key.mis.productos.entity.PalabraClave;
import com.ventas.key.mis.productos.entity.Producto;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "variantes")
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class Variantes  extends BaseId {

    @ManyToOne
    @JoinColumn(name = "producto_id")
    private Producto producto;

    private String talla;

    private String descripcion;

    private String color;

    private String presentacion;

    private int stock;

    private String marca;

    @Column(name = "contenido_neto")
    private String contenidoNeto;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "palabra_clave_id")
    private PalabraClave palabraClave;

    @Column(name = "habilitado")
    private char habilitado = '1';

    // Nace null en variantes creadas antes de esta migracion (sin backfill retroactivo, mismo
    // criterio que Producto.fechaCreacion). Ver comentario en Producto.java para el motivo.
    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion;

    @PrePersist
    private void asignarFechaCreacion() {
        if (this.fechaCreacion == null) {
            this.fechaCreacion = LocalDateTime.now();
        }
    }
}
