package com.ventas.key.mis.productos.entity;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.*;
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

    @Column(name = "total_venta", nullable = false)
    private Double totalVenta;

    @Column(name = "forma_pago", nullable = false)
    private String formaPago;

    @Column(name = "estado_venta", nullable = false)
    private String estadoVenta;

    @ManyToOne
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "fecha_venta")
    private LocalDateTime fechaVenta;


//    @ManyToOne
//    @JoinColumn(name = "pagos_y_meses_id")
//    private PagosYMeses pagosYMeses;

    @Column(name = "pagos_y_meses_id")
    private int pagosMesesInteres;

    @ManyToOne
    @JoinColumn(name = "pedido_id")
    private Pedido pedido;

    @OneToMany(mappedBy = "venta", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DetalleVenta> detalles;

    @Override
    public String toString() {
        return "Venta{" +
                "totalVenta=" + totalVenta +
                ", formaPago='" + formaPago + '\'' +
                ", estadoVenta='" + estadoVenta + '\'' +
                ", usuario=" + usuario +
                ", fechaVenta=" + fechaVenta +
                ", pagosMesesInteres=" + pagosMesesInteres +
                ", pedido=" + pedido +
                ", detalles=" + detalles +
                '}';
    }
}
