package com.ventas.key.mis.productos.models.pedidos;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AbonoDetalleItem {
    private Integer id;
    private Double monto;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate fechaPago;

    private String metodoPago;
    private String nota;
    private Double montoDado;
}
