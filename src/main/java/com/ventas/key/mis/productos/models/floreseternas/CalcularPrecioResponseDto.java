package com.ventas.key.mis.productos.models.floreseternas;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CalcularPrecioResponseDto {
    private Integer cantidadFinal;
    private Double precioBase;
    // Variante "sombra" del tipo de flor -- usarla para la linea de flores en /v1/pedidos/savePedido
    // (cantidad = cantidadFinal, precioUnitario = precioBase/cantidadFinal, subTotal = precioBase).
    private Integer tipoFlorVarianteId;

    private Boolean papelObligatorioAplicado;
    private Double precioPapel;
    // Variante "sombra" del accesorio marcado como "es papel" -- null si no aplico la regla.
    private Integer papelVarianteId;

    private List<AccesorioCalculadoDto> accesoriosCalculados;
    private Double subtotalAccesorios;

    private List<ListonCalculadoDto> listonesCalculados;
    private Double subtotalListones;
    private Boolean tieneListonPendienteValidacion;
    private Boolean requiereAnticipo50Porciento;
    private Double montoAnticipoSugerido;
    private String avisoNoReembolso;

    private Boolean recogerEnLocal;
    private Double costoEnvio;
    // Variante "sombra" del lugar de entrega elegido -- null si recogerEnLocal o si el lugar
    // no tiene costo de envio configurado (nada que cobrar, no hace falta linea).
    private Integer envioVarianteId;

    // Total de todo lo que ya tiene precio conocido. Si tieneListonPendienteValidacion es true,
    // este total es provisional -- falta el precio que el admin le asigne a la frase personalizada.
    private Double total;
}
