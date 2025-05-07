package com.ventas.key.mis.productos.entity;

import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "ventas")
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class Venta  extends BaseId{

    @Column(name = "usuario_id", nullable = false)
    private Integer usuarioId;

    @Column(name = "total_venta", nullable = false)
    private Double totalVenta;

    @Column(name = "forma_pago", nullable = false)
    private String formaPago;

    @Column(name = "estado_venta", nullable = false)
    private String estadoVenta;


    
}
