package com.ventas.key.mis.productos.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.ventas.key.mis.productos.entity.productoVariantes.Variantes;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "configurar_rifa_variante")
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ConfigurarRifaVariante extends BaseId {

    @ManyToOne
    @JoinColumn(name = "configurar_rifa_id", nullable = false)
    @JsonIgnoreProperties("variantes")
    private ConfigurarRifa configurarRifa;

    @ManyToOne
    @JoinColumn(name = "variante_id", nullable = false)
    private Variantes variante;

    @Column(name = "palabra_clave", nullable = false, length = 50)
    private String palabraClave;

    @Column(name = "giro_ganador", nullable = false)
    private int giroGanador = 1;

    @Column(name = "orden", nullable = false)
    private int orden;

    @Column(name = "permitir_nuevos")
    private boolean permitirNuevos = false;

    @Column(name = "stock_reservado", nullable = false)
    private int stockReservado = 1;
}