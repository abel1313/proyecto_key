package com.ventas.key.mis.productos.models;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ConfigurarRifaVarianteDto {
    private Integer id;
    private String palabraClave;
    private int giroGanador;
    private int orden;
    private boolean permitirNuevos;
    private int stockReservado;
    private VarianteResumenDto variante;
}