package com.ventas.key.mis.productos.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
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
    @OneToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn( name = "cliente_id")
    private Cliente cliente;

}
