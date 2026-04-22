package com.ventas.key.mis.productos.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Entity
@Table(name = "concursantes")
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class Concursante extends BaseId {

    @NotBlank(message = "El nombre es requerido")
    @Column(name = "nombre", nullable = false)
    private String nombre;

    @Column(name = "apellido_paterno")
    private String apellidoPaterno;

    @Column(name = "palabra_rifa")
    private String palabraRifa;

    @Column(name = "telefono")
    private String telefono;

    @Column(name = "descartado", nullable = false)
    private boolean descartado = false;

    @ManyToOne
    @JoinColumn(name = "configurar_rifa_id", nullable = false)
    @JsonIgnoreProperties("producto")
    private ConfigurarRifa configurarRifa;
}