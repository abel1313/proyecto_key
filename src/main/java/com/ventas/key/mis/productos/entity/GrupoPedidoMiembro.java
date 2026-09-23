package com.ventas.key.mis.productos.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Un pedido dentro de un {@link GrupoPedido}. La fila se conserva al deshacer el grupo, para
 * que quede el historial de con quien estuvo unido.
 */
@Entity
@Table(name = "grupo_pedido_miembro")
@Getter
@Setter
@NoArgsConstructor
public class GrupoPedidoMiembro extends BaseId {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grupo_id", nullable = false)
    private GrupoPedido grupo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pedido_id", nullable = false)
    private Pedido pedido;
}
