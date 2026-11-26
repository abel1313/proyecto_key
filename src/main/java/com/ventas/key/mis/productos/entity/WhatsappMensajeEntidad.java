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
 * Tabla {@code whatsapp_mensaje}: el historial, separado por cliente. {@code wa_message_id} es único
 * (así no se procesa dos veces el mismo mensaje de Meta). Fechas en UTC.
 */
@Entity
@Table(name = "whatsapp_mensaje")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WhatsappMensajeEntidad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cliente_id", nullable = false)
    private Long clienteId;

    @Column(name = "wa_message_id", unique = true, length = 128)
    private String waMessageId;

    /** ENTRANTE o SALIENTE. */
    @Column(name = "direccion", nullable = false, length = 10)
    private String direccion;

    /** CLIENTE, BOT, PERSONA o SISTEMA. */
    @Column(name = "origen", nullable = false, length = 10)
    private String origen;

    @Column(name = "tipo", nullable = false, length = 20)
    private String tipo;

    @Column(name = "contenido", columnDefinition = "TEXT")
    private String contenido;

    @Column(name = "estado", nullable = false, length = 30)
    private String estado;

    @Column(name = "motivo", length = 255)
    private String motivo;

    @Column(name = "codigo_error")
    private Integer codigoError;

    @Column(name = "plantilla", length = 100)
    private String plantilla;

    /** La hora de Meta para los entrantes; la nuestra para los salientes. */
    @Column(name = "enviado_en", nullable = false)
    private LocalDateTime enviadoEn;

    @Column(name = "creado_at", nullable = false)
    private LocalDateTime creadoAt;
}
