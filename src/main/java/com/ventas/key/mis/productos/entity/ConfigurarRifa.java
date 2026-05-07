package com.ventas.key.mis.productos.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "configurar_rifa")
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ConfigurarRifa extends BaseId {

    @Column(name = "fecha_hora_limite", nullable = false)
    private LocalDateTime fechaHoraLimite;

    @Column(name = "activa")
    private Boolean activa = true;

    @OneToMany(mappedBy = "configurarRifa", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orden ASC")
    private List<ConfigurarRifaProducto> productos = new ArrayList<>();
}