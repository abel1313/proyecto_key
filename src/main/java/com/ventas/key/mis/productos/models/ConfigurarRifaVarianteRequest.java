package com.ventas.key.mis.productos.models;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ConfigurarRifaVarianteRequest {
    private Integer configurarRifaId;
    private Integer varianteId;
    private String palabraClave;
    private int giroGanador = 1;
    private int orden;
    private boolean permitirNuevos = false;
}