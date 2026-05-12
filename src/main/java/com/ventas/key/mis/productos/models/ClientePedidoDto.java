package com.ventas.key.mis.productos.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ClientePedidoDto {
    private Integer clientePedidoId;
    private String nombre;
    private String apellidoPaterno;
    private String telefono;
    private boolean sinRegistro;
}