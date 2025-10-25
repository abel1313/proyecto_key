package com.ventas.key.mis.productos.entity;


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

    @Column(name = "codigo_postal", unique = true)
    private int codigoPostal;

    private String municipio;

    private String referencias;

    @ManyToOne(optional = false)
    @JoinColumn(name = "producto_id", nullable = false)
    private Cliente cliente;
}

