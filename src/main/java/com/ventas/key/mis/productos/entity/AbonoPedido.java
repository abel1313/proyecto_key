package com.ventas.key.mis.productos.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "abono_pedido")
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class AbonoPedido extends BaseId {

    @ManyToOne
    @JoinColumn(name = "pedido_id", nullable = false)
    @JsonBackReference
    private Pedido pedido;

    @Column(nullable = false)
    private Double monto;

    @Column(name = "fecha_pago", nullable = false)
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate fechaPago;

    @Column(name = "metodo_pago", nullable = false, length = 15)
    private String metodoPago;

    @Column(length = 200)
    private String nota;

    @Column(name = "monto_dado")
    private Double montoDado;
}
