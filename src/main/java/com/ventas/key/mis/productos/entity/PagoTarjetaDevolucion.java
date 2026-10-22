package com.ventas.key.mis.productos.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Tabla {@code pago_tarjeta_devolucion}: una devolucion de un cobro con tarjeta. Nace
 * {@code PENDIENTE} (reservada, antes de hablar con la pasarela) y pasa a {@code CONFIRMADA}.
 * La {@code clave} es unica: es la clave de idempotencia que tambien se manda a Mercado Pago.
 */
@Entity
@Table(name = "pago_tarjeta_devolucion")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PagoTarjetaDevolucion extends BaseId {

    @Column(name = "intento_id", nullable = false)
    private Integer intentoId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    @Column(name = "hecha_por", nullable = false, length = 100)
    private String hechaPor;

    @Column(nullable = false, length = 100)
    private String clave;

    @Column(nullable = false, length = 12)
    private String estado;

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn;

    @Column(name = "confirmada_en")
    private LocalDateTime confirmadaEn;
}
