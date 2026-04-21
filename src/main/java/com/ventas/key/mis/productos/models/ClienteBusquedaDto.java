package com.ventas.key.mis.productos.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ClienteBusquedaDto {
    private int id;
    private String nombrePersona;
    private String apeidoPaterno;
    private String apeidoMaterno;
    private String correoElectronico;
    private String numeroTelefonico;
}