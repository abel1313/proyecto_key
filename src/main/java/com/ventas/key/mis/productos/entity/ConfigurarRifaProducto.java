package com.ventas.key.mis.productos.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "configurar_rifa_producto")
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ConfigurarRifaProducto extends BaseId {

    @ManyToOne
    @JoinColumn(name = "configurar_rifa_id", nullable = false)
    private ConfigurarRifa configurarRifa;

    @ManyToOne
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @Column(name = "orden", nullable = false)
    private int orden;

    @Column(name = "giro_ganador", nullable = false)
    private int giroGanador = 1;

    @Column(name = "permitir_nuevos")
    private boolean permitirNuevos = false;
}