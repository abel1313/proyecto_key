package com.ventas.key.mis.productos.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "pagos_y_meses")
@Getter @Setter @AllArgsConstructor @NoArgsConstructor
public class PagosYMeses extends BaseId {

    @ManyToOne
    @JoinColumn(name = "tipo_pago_id", nullable = false)
    private TipoPago tipoPago;

    @ManyToOne
    @JoinColumn(name = "meses_intereses_id", nullable = false)
    private MesesIntereses mesesIntereses;
}