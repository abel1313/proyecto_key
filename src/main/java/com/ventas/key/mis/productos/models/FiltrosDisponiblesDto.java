package com.ventas.key.mis.productos.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FiltrosDisponiblesDto {
    private List<String> tallas;
    private List<String> colores;
    private List<String> marcas;
    private Double precioMin;
    private Double precioMax;
}
