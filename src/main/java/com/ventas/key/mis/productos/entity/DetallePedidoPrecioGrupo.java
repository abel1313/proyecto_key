package com.ventas.key.mis.productos.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Tabla {@code detalle_pedido_precio_grupo} (migration_precio_base_linea_grupo.sql): para una línea de pedido que se cobró con precio de
 * grupo, el precio que tenía SIN el grupo (el normal o la rebaja). Sirve para regresarla a ese precio si al editar el pedido el grupo deja
 * de juntar el mínimo. La entidad de la línea ({@link DetallePedido}) no cambia, a propósito: así el pedido no depende de esta tabla.
 */
@Entity
@Table(name = "detalle_pedido_precio_grupo")
@Getter
@Setter
@NoArgsConstructor
public class DetallePedidoPrecioGrupo {

    /** El id de la línea ({@code detalle_pedidos.id}); no se genera, se asigna. */
    @Id
    @Column(name = "detalle_pedido_id")
    private Integer detallePedidoId;

    @Column(name = "precio_sin_grupo", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioSinGrupo;

    /** Por qué se cobró con descuento: {@code GRUPO} (precio por cantidad) o {@code CAMPANA} (campaña de juguetes). Cada una se revierte por su cuenta. */
    @Column(name = "origen", nullable = false, length = 10)
    private String origen = "GRUPO";
}
