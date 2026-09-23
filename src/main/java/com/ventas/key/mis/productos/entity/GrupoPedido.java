package com.ventas.key.mis.productos.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Pedidos que se cobran y se entregan juntos (dominio {@code hexagonal/grupopedido}).
 *
 * <p>Vive aqui y no en {@code hexagonal/grupopedido/infraestructura} porque el escaneo de
 * entidades JPA solo cubre {@code com.ventas.key.mis.productos}.
 */
@Entity
@Table(name = "grupo_pedido")
@Getter
@Setter
@NoArgsConstructor
public class GrupoPedido extends BaseId {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pedido_titular_id", nullable = false)
    private Pedido pedidoTitular;

    @Column(nullable = false)
    private Boolean activo = Boolean.TRUE;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "usuario_creo_id")
    private Integer usuarioCreoId;

    @Column(name = "fecha_deshecho")
    private LocalDateTime fechaDeshecho;

    @Column(name = "usuario_deshizo_id")
    private Integer usuarioDeshizoId;

    @Column(length = 200)
    private String nota;

    @OneToMany(mappedBy = "grupo", cascade = CascadeType.ALL)
    private List<GrupoPedidoMiembro> miembros = new ArrayList<>();
}
