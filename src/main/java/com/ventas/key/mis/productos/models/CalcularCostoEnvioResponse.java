package com.ventas.key.mis.productos.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CalcularCostoEnvioResponse {
    // false = el punto quedo fuera de todos los anillos configurados para la zona -- el front
    // debe bloquear el avance del checkout en ese caso. Si la zona no tiene anillos configurados
    // (todavia sin migrar a este esquema), siempre da true con el costoEnvio fijo de la zona.
    private boolean dentroDeRango;
    private Double costoEnvio;
    private Integer anilloId;
    // Variante "sombra" a facturar (del anillo si aplico, o de la zona si no tiene anillos
    // configurados) -- la usa FlorPedidoServiceImpl.calcularEnvio para armar la linea del pedido,
    // igual que ya hacia con LugarEntrega.variante antes de que existieran los anillos.
    private Integer varianteId;
}
