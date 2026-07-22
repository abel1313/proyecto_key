package com.ventas.key.mis.productos.models;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ImportarDePedidosRequest {
    private Integer configurarRifaId;
    private String palabraClave;
    private int ordenDesde = 1;
    private String mes;
    private List<ClientePedidoDto> clientes;

    @Getter
    @Setter
    public static class ClientePedidoDto {
        private Integer clientePedidoId;
        private String nombre;
        private String apellidoPaterno;
        private String telefono;
        private String correo;
        private boolean sinRegistro;
    }
}