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

    // Regla del umbral: si la cantidad final de flores es > umbralActivacion, este accesorio se
    // agrega y cobra automatico, sin preguntar (ver FlorPedidoServiceImpl). Null = nunca se
    // agrega solo, siempre es opcional (se pregunta, como cualquier otro accesorio).
    @Column(name = "es_papel", nullable = false)
    private Boolean esPapel = false;

    @Column(name = "umbral_activacion")
    private Integer umbralActivacion;

    // Solo aplica cuando esPapel=true: cuantas flores cubre 1 pliego. Si esta configurado,
    // "precio" se interpreta como precio POR PLIEGO (ver AccesorioRamoServiceImpl.calcularPrecioPapel)
    // -- el costo real escala con la cantidad de flores del ramo en vez de ser un monto fijo.
    // Null = se mantiene el precio fijo unico anterior, retrocompatible.
    @Column(name = "flores_por_pliego")
    private Integer floresPorPliego;

    @Column(nullable = false)
    private Boolean activo = true;

    // Variante "sombra" -- ver comentario en TipoFlor.variante. Stock fijo alto (no se
    // controla inventario de accesorios todavia), lo administra AccesorioRamoServiceImpl.
    @ManyToOne
    @JoinColumn(name = "variante_id")
    private Variantes variante;
}
