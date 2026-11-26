package com.ventas.key.mis.productos.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Tabla {@code whatsapp_cliente}: un número con quien hay conversación. Todas las fechas se guardan
 * en UTC (el dominio trabaja con {@code Instant}); ver {@code migration_whatsapp_cloud.sql}.
 * El dominio vive en {@code hexagonal/whatsapp}; la entidad está aquí porque el escaneo de JPA
 * solo cubre este paquete.
 */
@Entity
@Table(name = "whatsapp_cliente")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WhatsappClienteEntidad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "telefono", nullable = false, unique = true, length = 20)
    private String telefono;

    @Column(name = "nombre", length = 100)
    private String nombre;

    /** Hora (de Meta) del último mensaje entrante: de aquí se cuenta la ventana de 24 h. */
    @Column(name = "ultimo_entrante_at")
    private LocalDateTime ultimoEntranteAt;

    @Column(name = "ultima_actividad_at", nullable = false)
    private LocalDateTime ultimaActividadAt;

    @Column(name = "pausa_bot_desde")
    private LocalDateTime pausaBotDesde;

    @Column(name = "bot_fijo_humano", nullable = false)
    private boolean botFijoHumano;

    @Column(name = "requiere_atencion", nullable = false)
    private boolean requiereAtencion;

    @Column(name = "ultimo_mensaje", length = 200)
    private String ultimoMensaje;

    @Column(name = "creado_at", nullable = false)
    private LocalDateTime creadoAt;
}
