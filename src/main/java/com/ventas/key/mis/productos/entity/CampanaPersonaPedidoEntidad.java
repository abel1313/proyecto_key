package com.ventas.key.mis.productos.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Tabla {@code campana_persona_pedido}: qué pedido es de qué persona dentro de qué campaña y cuánto suma hacia su meta. Son ids simples (sin
 * relaciones JPA): las llaves foráneas con borrado en cascada están en la migración.
 */
@Entity
@Table(name = "campana_persona_pedido")
@Getter
@Setter
@NoArgsConstructor
public class CampanaPersonaPedidoEntidad extends BaseId {

    @Column(name = "campana_id", nullable = false)
    private Integer campanaId;

    @Column(name = "persona_id", nullable = false)
    private Integer personaId;

    @Column(name = "pedido_id", nullable = false)
    private Integer pedidoId;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal monto = BigDecimal.ZERO;
}
