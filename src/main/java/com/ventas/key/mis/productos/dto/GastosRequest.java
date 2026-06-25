package com.ventas.key.mis.productos.dto;

import com.ventas.key.mis.productos.entity.Gastos;
import lombok.*;

import java.time.LocalDate;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class GastosRequest {

    private String descripcion;
    private Double monto;
    private LocalDate fecha;
    private Gastos.CategoriaGasto categoria;
    private String proveedor;
    private String comprobante;
    private String notas;
}
