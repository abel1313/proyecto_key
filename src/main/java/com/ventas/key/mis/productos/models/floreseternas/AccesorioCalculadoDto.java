package com.ventas.key.mis.productos.models.floreseternas;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AccesorioCalculadoDto {
    private Integer accesorioId;
    private String nombre;
    private Integer cantidad;
    private Double precioUnitario;
    private Double subtotal;
    private Boolean agregadoAutomaticoPorRegla;
    // Variante "sombra" del accesorio -- usarla para armar la linea real en /v1/pedidos/savePedido.
    // Null si el accesorio todavia no tiene variante sincronizada (no debería pasar en catálogos ya guardados).
    private Integer varianteId;
}
