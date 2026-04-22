package com.ventas.key.mis.productos.entity;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "ventas")
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class Venta  extends BaseId{

    @Column(name = "total_venta", nullable = false)
    private Double totalVenta;

    @Column(name = "ganancia_total")
    private Double gananciaTotal;

    @Column(name = "estado_venta", nullable = false)
    private String estadoVenta;

    @ManyToOne
    @JoinColumn(name = "pagos_y_meses_id")
    private PagosYMeses pagosYMeses;

    @ManyToOne
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne
    @JoinColumns({
        @JoinColumn(name = "forma_pago_id",      referencedColumnName = "tipo_pago_id"),
        @JoinColumn(name = "tarifa_terminal_id",  referencedColumnName = "tarifa_terminal_id"),
        @JoinColumn(name = "iva_id",              referencedColumnName = "iva_id")
    })
    private DetallePago detallePago;

    @Column(name = "fecha_venta")
    private LocalDateTime fechaVenta;

    @ManyToOne
    @JoinColumn(name = "pedido_id")
    private Pedido pedido;

    @OneToMany(mappedBy = "venta", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DetalleVenta> detalles;

}
