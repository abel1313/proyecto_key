package com.ventas.key.mis.productos.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
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
@Table(name = "ramo_armado_accesorio")
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class RamoArmadoAccesorio extends BaseId {

    @ManyToOne
    @JoinColumn(name = "ramo_armado_id", nullable = false)
    @JsonBackReference
    private RamoArmado ramoArmado;

    @ManyToOne
    @JoinColumn(name = "accesorio_id", nullable = false)
    private AccesorioRamo accesorio;

    @Column(nullable = false)
    private Integer cantidad = 1;
}
