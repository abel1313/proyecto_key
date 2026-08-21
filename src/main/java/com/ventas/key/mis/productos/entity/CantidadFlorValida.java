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

import java.time.LocalTime;

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

    // ============================================================
    // Configuracion de entregas (reemplaza en la practica a horasMinimasAnticipacion/
    // precioUrgencia de arriba -- se dejan esos dos sin usar, no se borran). El dueno da de alta,
    // por cada tamano de ramo, cuanto tarda entregarlo normal y (opcional) si se puede apurar y
    // con que cargo. Ver FlorPedidoServiceImpl.fechasDisponibles.
    // ============================================================

    // Plazo normal: cuantos dias tarda armar/entregar este tamano, sin nada de urgencia. Null =
    // este tamano todavia no tiene plazo normal configurado.
    @Column(name = "dias_normal")
    private Integer diasNormal;

    // Hora del dia en que se entrega con el plazo normal (ej. 16:00).
    @Column(name = "hora_entrega_normal")
    private LocalTime horaEntregaNormal;

    // Plazo urgente: cuantos dias tarda si el cliente paga antes de horaLimitePedido. Null = este
    // tamano NO se puede apurar por ningun motivo (ej. un ramo de 100 flores para el dia
    // siguiente) -- el cliente ni siquiera ve la opcion de urgente.
    @Column(name = "dias_urgente")
    private Integer diasUrgente;

    // Hora de entrega del plazo urgente.
    @Column(name = "hora_entrega_urgente")
    private LocalTime horaEntregaUrgente;

    // Hora limite del dia para que el PAGO cuente para el plazo urgente de HOY -- si el pago
    // llega despues, el plazo urgente se corre a partir de manana (ver
    // FlorPedidoServiceImpl.fechasDisponibles). Solo aplica si diasUrgente no es null.
    @Column(name = "hora_limite_pedido")
    private LocalTime horaLimitePedido;

    // Cargo extra que se cobra cuando aplica el plazo urgente -- se suma directo al total, sin
    // aparecer como accesorio (mismo criterio que manoDeObra).
    @Column(name = "cargo_urgente")
    private Double cargoUrgente;

    @Column(nullable = false)
    private Boolean activo = true;
}
