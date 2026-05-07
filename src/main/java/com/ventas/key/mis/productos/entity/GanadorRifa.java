package com.ventas.key.mis.productos.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "ganador_rifa")
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class GanadorRifa extends BaseId {

    @ManyToOne
    @JoinColumn(name = "concursante_id", nullable = false)
    private Concursante concursante;

    @ManyToOne
    @JoinColumn(name = "configurar_rifa_variante_id", nullable = false)
    private ConfigurarRifaVariante configurarRifaVariante;

    @Column(name = "descartado", nullable = false)
    private boolean descartado = false;
}