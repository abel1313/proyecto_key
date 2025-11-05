package com.ventas.key.mis.productos.models.pedidos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ClienteQuery {
    private int id;
    private String nombreCliente;
    private String correoElectronico;
    private String numeroTelefonico;
}
