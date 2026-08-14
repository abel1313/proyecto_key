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

    // Solo aplica cuando esPapel=true: hasta esta cantidad de flores (inclusive), si el cliente
    // elige papel se cobra fijo 1 pliego, sin importar floresPorPliego ni el pliegos explicito de
    // CantidadFlorValida -- pensado para ventas chicas/sueltas (1 a N flores) donde no tiene
    // sentido prorratear el papel. Gana sobre cualquier otra config en ese rango. Null = esta
    // regla nunca se dispara, se usa la prioridad normal (ver calcularPliegosPapel).
    @Column(name = "umbral_pliego_fijo")
    private Integer umbralPliegoFijo;

    // Si esta activo, este accesorio se agrega y cobra automatico en TODO ramo armado, sin
    // importar la cantidad de flores (a diferencia del papel, que solo se auto-agrega arriba de
    // su umbralActivacion). Pensado para costos que siempre aplican, ej. mano de obra de armado.
    // No es exclusivo -- puede haber varios accesorios auto-incluidos a la vez.
    @Column(name = "auto_incluido", nullable = false)
    private Boolean autoIncluido = false;

    @Column(nullable = false)
    private Boolean activo = true;

    // Variante "sombra" -- ver comentario en TipoFlor.variante. Stock fijo alto (no se
    // controla inventario de accesorios todavia), lo administra AccesorioRamoServiceImpl.
    @ManyToOne
    @JoinColumn(name = "variante_id")
    private Variantes variante;
}
