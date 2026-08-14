package com.ventas.key.mis.productos.entity;

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
@Table(name = "cantidad_flor_valida")
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class CantidadFlorValida extends BaseId {

    @ManyToOne
    @JoinColumn(name = "tipo_flor_id", nullable = false)
    private TipoFlor tipoFlor;

    @Column(nullable = false)
    private Integer cantidad;

    // Cuantos pliegos de papel lleva ESTE ramo, puesto a mano por el dueno -- no es proporcional
    // a la cantidad de flores (depende de como se arma, tamano del pliego, etc.), por eso no se
    // deriva de una formula. Null = todavia no lo configuro; en ese caso se usa como respaldo
    // AccesorioRamo.floresPorPliego (formula) y, si tampoco esta, el precio fijo unico de siempre
    // (ver AccesorioRamoServiceImpl.calcularPliegosPapel).
    @Column(name = "pliegos")
    private Integer pliegos;

    @Column(nullable = false)
    private Boolean activo = true;
}
