package com.ventas.key.mis.productos.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "pedidos")
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class Pedido extends BaseId{


    @ManyToOne
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @Column(name = "fecha_pedido")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate fechaPedido;

    @Column(name = "estado_pedido")
    private String estadoPedido;

    private String observaciones;

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL)
    @JsonManagedReference
    private List<DetallePedido> detalles;

}
