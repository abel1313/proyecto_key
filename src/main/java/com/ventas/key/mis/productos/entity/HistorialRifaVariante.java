package com.ventas.key.mis.productos.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "historial_rifa_variante")
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class HistorialRifaVariante extends BaseId {

    @ManyToOne
    @JoinColumn(name = "configurar_rifa_id", nullable = false)
    private ConfigurarRifa configurarRifa;

    @ManyToOne
    @JoinColumn(name = "configurar_rifa_variante_id", nullable = false)
    private ConfigurarRifaVariante configurarRifaVariante;

    @ManyToOne
    @JoinColumn(name = "concursante_ganador_id")
    private Concursante concursanteGanador;

    @Column(name = "orden", nullable = false)
    private int orden;

    @Enumerated(EnumType.STRING)
    @Column(name = "modo_continuacion")
    private ModoContinuacion modoContinuacion;

    public enum ModoContinuacion {
        RESTANTES, CERO, NUEVOS
    }
}