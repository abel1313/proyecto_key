package com.ventas.key.mis.productos.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "cantidad_flor_valida")
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class CantidadFlorValida extends BaseId {

    @ManyToOne
    @JoinColumn(name = "tipo_flor_id", nullable = false)
    private TipoFlor tipoFlor;

    @Column(nullable = false)
    private Integer cantidad;

    @Column(nullable = false)
    private Boolean activo = true;
}
