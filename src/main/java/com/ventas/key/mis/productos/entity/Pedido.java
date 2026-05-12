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
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    @ManyToOne
    @JoinColumn(name = "cliente_sin_registro_id")
    private ClienteSinRegistro clienteSinRegistro;

    @Column(name = "fecha_pedido")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate fechaPedido;

    @Column(name = "estado_pedido")
    private String estadoPedido;

    @Column(name = "fecha_recogida")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate fechaRecogida;

    private String observaciones;

    @Column(name = "motivo_cancelacion", length = 30)
    private String motivoCancelacion;

    @Column(name = "fecha_cancelacion")
    private LocalDate fechaCancelacion;

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL)
    @JsonManagedReference
    private List<DetallePedido> detalles;

}
