package com.ventas.key.mis.productos.entity;

import java.sql.Date;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
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
public class GanadorRifa extends BaseId{

    @Valid
    @OneToOne(cascade = {CascadeType.PERSIST})
    @JoinColumn( name = "cliente_id")
    private Cliente cliente;

    @OneToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name = "producto_id")
    private Producto producto;


}
