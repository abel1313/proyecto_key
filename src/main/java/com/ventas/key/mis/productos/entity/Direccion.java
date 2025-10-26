package com.ventas.key.mis.productos.entity;


import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "direcciones")
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class Direccion extends BaseId{
    private String calle;
    private String colonia;

    @Column(name = "codigo_postal")
    private int codigoPostal;

    private String municipio;

    private String referencias;

    @ManyToOne(optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    @JsonBackReference
    private Cliente cliente;
}

