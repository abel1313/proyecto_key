package com.ventas.key.mis.productos.entity;

import com.ventas.key.mis.productos.entity.productoVariantes.Variantes;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "accesorio_ramo")
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class AccesorioRamo extends BaseId {

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false)
    private Double precio;

    @Column(name = "precio_costo")
    private Double precioCosto;

    @Column(name = "admite_texto_libre", nullable = false)
    private Boolean admiteTextoLibre = false;

    // Regla del umbral: si la cantidad final de flores es > 10, el accesorio marcado como
    // "es papel" se agrega y cobra automatico, sin preguntar (ver FlorPedidoServiceImpl).
    @Column(name = "es_papel", nullable = false)
    private Boolean esPapel = false;

    @Column(nullable = false)
    private Boolean activo = true;

    // Variante "sombra" -- ver comentario en TipoFlor.variante. Stock fijo alto (no se
    // controla inventario de accesorios todavia), lo administra AccesorioRamoServiceImpl.
    @ManyToOne
    @JoinColumn(name = "variante_id")
    private Variantes variante;
}
