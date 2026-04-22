package com.ventas.key.mis.productos.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "configurar_rifa")
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ConfigurarRifa extends BaseId {

    @OneToOne
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @Column(name = "fecha_hora_limite", nullable = false)
    private LocalDateTime fechaHoraLimite;

    @Column(name = "activa")
    private Boolean activa = true;
}