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

/**
 * Variantes = lo que el usuario final ve como "PRODUCTO" en la app.
 *
 * Es la unidad vendible: tiene talla, color, marca, stock, precio y codigo de barras. El cliente
 * final solo navega variantes -- nunca ve un {@link com.ventas.key.mis.productos.entity.Producto},
 * que se le muestra como "MODELO".
 *
 * Regla de oro: el Modelo agrupa, el Producto se vende. Asi que un ticket que dice "el producto
 * no tiene stock" o "no encuentro el producto en el buscador" habla de esta clase, no de Producto.
 *
 * Los nombres de la API no cambian por esto (sigue siendo /variantes/v1/..., varianteId, etc.):
 * la traduccion vive solo en la capa de presentacion del front. Ver TAXONOMIA_NOMBRES_BACK.md.
 */
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

    /**
     * Precio propio del articulo (2026-09-29). Null = usa el del producto, como todos hasta hoy.
     * Van siempre juntos: con precio propio, {@link #precioRebaja} es su descuento (0 = sin).
     * Se cambian solo desde {@code hexagonal/precio}. Para cobrar, usar {@link #precioNormal()} y
     * {@link #precioDescuento()}, nunca el precio del producto directo.
     */
    @Column(name = "precio_venta")
    private Double precioVenta;

    @Column(name = "precio_rebaja")
    private Double precioRebaja;

    public boolean tienePrecioPropio() {
        return precioVenta != null;
    }

    /** El precio normal al que se vende este articulo. */
    public Double precioNormal() {
        if (tienePrecioPropio()) {
            return precioVenta;
        }
        return producto != null ? producto.getPrecioVenta() : null;
    }

    /** Su precio con descuento; null o 0 = no tiene. Solo se cobra si el admin lo elige. */
    public Double precioDescuento() {
        if (tienePrecioPropio()) {
            return precioRebaja;
        }
        return producto != null ? producto.getPrecioRebaja() : null;
    }

    @PrePersist
    private void asignarFechaCreacion() {
        if (this.fechaCreacion == null) {
            this.fechaCreacion = LocalDateTime.now();
        }
    }
}
