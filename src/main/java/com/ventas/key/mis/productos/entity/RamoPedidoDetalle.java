package com.ventas.key.mis.productos.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

// "Ticket de produccion" de un ramo dentro de un pedido ya creado. NO guarda precios que ya
// viven como lineas reales de DetallePedido (flores, papel, accesorios, liston predefinido,
// envio) -- ver PROPUESTA_FLORES_ETERNAS.md. Solo guarda lo que no tiene lugar ahi: que frase
// personalizada quedo pendiente de validar (y su anticipo), zona de entrega/recoger en local,
// contacto del pedido y el comentario libre de accesorio no disponible.
@Entity
@Table(name = "ramo_pedido_detalle")
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class RamoPedidoDetalle extends BaseId {

    @ManyToOne
    @JoinColumn(name = "pedido_id", nullable = false)
    private Pedido pedido;

    // Referencia informativa: si el cliente partio de un ramo preconfigurado. Null si fue
    // armado a la medida.
    @ManyToOne
    @JoinColumn(name = "ramo_armado_id")
    private RamoArmado ramoArmado;

    @ManyToOne
    @JoinColumn(name = "tipo_flor_id", nullable = false)
    private TipoFlor tipoFlor;

    @Column(name = "cantidad_final", nullable = false)
    private Integer cantidadFinal;

    // Desglose por color -- para produccion (armar el ramo con la mezcla exacta que pidio el
    // cliente), no para precio (eso ya vive en las lineas reales de DetallePedido).
    @OneToMany(mappedBy = "ramoPedidoDetalle", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<RamoPedidoDetalleColor> colores;

    @ManyToOne
    @JoinColumn(name = "frase_liston_predefinida_id")
    private FraseListonPredefinida fraseListonPredefinida;

    @Column(name = "frase_liston_personalizada", columnDefinition = "TEXT")
    private String fraseListonPersonalizada;

    // NO_APLICA (no eligio liston o eligio predefinida) / PENDIENTE_VALIDACION / VALIDADA / RECHAZADA
    @Column(name = "frase_liston_estado", nullable = false, length = 30)
    private String fraseListonEstado = "NO_APLICA";

    // Precio que el ADMIN le asigna a la frase personalizada al validarla. Cobrarlo es manual
    // (agregar la linea correspondiente al pedido) -- ver limitacion documentada.
    @Column(name = "frase_liston_precio_asignado")
    private Double fraseListonPrecioAsignado;

    @Column(name = "anticipo_requerido", nullable = false)
    private Boolean anticipoRequerido = false;

    @Column(name = "anticipo_pagado", nullable = false)
    private Boolean anticipoPagado = false;

    @Column(name = "monto_anticipo")
    private Double montoAnticipo;

    // Pedido APARTADO separado, creado solo para cobrar esta frase una vez que el admin le
    // asigna precio -- reutiliza el modulo de abonos existente (POST /v1/abonos/{id}) en vez de
    // forzar el pedido original completo (que ya se cobro normal) a volverse credito.
    @ManyToOne
    @JoinColumn(name = "pedido_anticipo_id")
    private Pedido pedidoAnticipo;

    @ManyToOne
    @JoinColumn(name = "lugar_entrega_id")
    private LugarEntrega lugarEntrega;

    @Column(name = "recoger_en_local", nullable = false)
    private Boolean recogerEnLocal = false;

    @Column(name = "telefono_contacto", length = 20)
    private String telefonoContacto;

    @Column(name = "correo_contacto", length = 150)
    private String correoContacto;

    @Column(name = "comentario_accesorio_no_disponible", columnDefinition = "TEXT")
    private String comentarioAccesorioNoDisponible;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;
}
