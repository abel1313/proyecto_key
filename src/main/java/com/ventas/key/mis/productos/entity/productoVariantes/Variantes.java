package com.ventas.key.mis.productos.entity.productoVariantes;

import com.ventas.key.mis.productos.entity.BaseId;
import com.ventas.key.mis.productos.entity.PalabraClave;
import com.ventas.key.mis.productos.entity.Producto;
import com.ventas.key.mis.productos.Utils.TextoNormalizado;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Variantes = lo que el admin ve como "ARTICULO" en la app (desde 2026-10-20; el cliente final lo ve como
 * "producto", a proposito).
 *
 * Es la unidad vendible: tiene talla, color, marca, stock, precio y codigo de barras. El cliente
 * final solo navega articulos -- nunca ve un {@link com.ventas.key.mis.productos.entity.Producto},
 * que al admin se le muestra como "PRODUCTO" (el producto base).
 *
 * Regla de oro: el Producto agrupa, el Articulo se vende. Un ticket que dice "el articulo no tiene stock"
 * habla de esta clase; uno que dice "el producto no tiene stock libre" habla de Producto.
 *
 * Los nombres de clases, tablas y rutas /v1/variantes no cambian (conviven con /v2/articulos); solo cambia
 * lo que se lee en pantalla. Ver RENOMBRE_MODELO_A_PRODUCTO.md y RENOMBRE_VARIANTE_A_ARTICULO.md.
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

    /**
     * R8 (2026-09-29): el admin activo "Precio descuento" en la tarjeta. Mientras siga activo, el
     * articulo se vende al descuento para todos. Solo tiene efecto con un descuento valido; ver
     * {@link #cobraConDescuento()}. Requiere migration_usar_descuento_variante.sql.
     */
    @Column(name = "usar_descuento", nullable = false)
    private boolean usarDescuento;

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

    /** Su precio con descuento; null o 0 = no tiene. Se cobra si el admin lo elige o si esta activo (R8). */
    public Double precioDescuento() {
        if (tienePrecioPropio()) {
            return precioRebaja;
        }
        return producto != null ? producto.getPrecioRebaja() : null;
    }

    /** R8: se vende al descuento si el admin lo activo y el descuento es menor al normal. */
    public boolean cobraConDescuento() {
        Double normal = precioNormal();
        Double descuento = precioDescuento();
        return usarDescuento && normal != null && descuento != null && descuento > 0 && descuento < normal;
    }

    /** El precio al que se vende por default: el descuento si esta activo (R8), si no el normal. */
    public Double precioACobrar() {
        return cobraConDescuento() ? precioDescuento() : precioNormal();
    }

    @PrePersist
    private void asignarFechaCreacion() {
        if (this.fechaCreacion == null) {
            this.fechaCreacion = LocalDateTime.now();
        }
        normalizarTextos();
    }

    @PreUpdate
    private void normalizarTextos() {
        this.talla = TextoNormalizado.mayusculas(this.talla);
        this.color = TextoNormalizado.mayusculas(this.color);
        this.marca = TextoNormalizado.mayusculas(this.marca);
    }
}
