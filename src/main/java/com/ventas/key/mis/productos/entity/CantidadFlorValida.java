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

    // Costo de mano de obra de armar ESTE tamano de ramo, puesto a mano por el dueno (ej. un ramo
    // de 62 lleva mas trabajo que uno de 20). Se suma directo a precioTotal sin aparecer como
    // accesorio -- el cliente ve un solo precio de ramo, no un desglose de material vs trabajo
    // (decision del dueno). Null = sin costo de mano de obra para esta cantidad.
    @Column(name = "mano_de_obra")
    private Double manoDeObra;

    // Minimo de horas de anticipacion (SIN contar la zona de entrega, ver
    // LugarEntrega.horasExtraAnticipacion) que el dueno necesita para poder armar ESTE tamano de
    // ramo -- lo sabe por experiencia, no es una formula. Si la fecha/hora de entrega pedida no
    // da ese margen, el pedido se rechaza (ver FlorPedidoServiceImpl.validarAnticipacionYUrgencia).
    // Null = sin validar anticipacion para esta cantidad (comportamiento de siempre).
    @Column(name = "horas_minimas_anticipacion")
    private Integer horasMinimasAnticipacion;

    // Extra que se cobra cuando el pedido cae dentro de la ventana de urgencia para este tamano
    // (justo al limite de horasMinimasAnticipacion, no con mucha anticipacion) -- se suma directo
    // al total, mismo criterio que manoDeObra: sin aparecer como accesorio. Null = sin cargo de
    // urgencia para esta cantidad.
    @Column(name = "precio_urgencia")
    private Double precioUrgencia;

    @Column(nullable = false)
    private Boolean activo = true;
}
