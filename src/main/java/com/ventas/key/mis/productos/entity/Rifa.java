package com.ventas.key.mis.productos.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "rifas")
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class Rifa extends BaseId {

    @Valid
    @ManyToOne(cascade = CascadeType.MERGE)
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    @Column(name = "palabra_rifa")
    private String palabraRifa;

    @ManyToOne
    @JoinColumn(name = "configurar_rifa_id")
    @JsonIgnoreProperties("producto")
    private ConfigurarRifa configurarRifa;
}