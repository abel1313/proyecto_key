package com.ventas.key.mis.productos.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

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

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", length = 20)
    private TipoRifa tipo;

    @Column(name = "mes_referencia", length = 7)
    private String mesReferencia;

    @Column(name = "es_prueba", nullable = false)
    private Boolean esPrueba = false;

    @OneToMany(mappedBy = "configurarRifa", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orden ASC")
    private List<ConfigurarRifaVariante> variantes = new ArrayList<>();

    public enum TipoRifa {
        MENSUAL, DIARIA
    }
}