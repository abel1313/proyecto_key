package com.ventas.key.mis.productos.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "gastos_surtir")
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class Gastos extends BaseId {

    @Column(name = "descripcion_gasto", nullable = false)
    private String descripcion;

    @Column(name = "precio_gasto", nullable = false)
    private Double monto;

    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;

    @Enumerated(EnumType.STRING)
    @Column(name = "categoria", nullable = false, length = 20)
    private CategoriaGasto categoria;

    @Column(name = "proveedor", length = 150)
    private String proveedor;

    @Column(name = "comprobante", length = 100)
    private String comprobante;

    @Column(name = "notas", columnDefinition = "TEXT")
    private String notas;

    public enum CategoriaGasto {
        INVENTARIO, OPERATIVO, SERVICIOS, OTROS
    }
}
