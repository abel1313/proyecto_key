package com.ventas.key.mis.productos.models.floreseternas;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CalcularPrecioRequestDto {
    // Uno o varios colores de LA MISMA especie (TipoFlor) -- ej. [{colorFlorId:1,cantidad:6},
    // {colorFlorId:2,cantidad:6}] para un ramo mitad rojo, mitad blanco. La cantidad final del
    // ramo es la suma de todas las entradas.
    private List<ColorSeleccionadoDto> colores;
    private List<AccesorioSeleccionadoDto> accesorios;
    private List<ListonSeleccionadoDto> listones;
    private Integer lugarEntregaId;
    private Boolean recogerEnLocal;
    // Opcional -- cuando el cliente pide para/le importa una fecha y hora de entrega puntual.
    // Si esta cantidad tiene horasMinimasAnticipacion configurada y no da tiempo, el calculo
    // rechaza el pedido (ver FlorPedidoServiceImpl.validarAnticipacionYUrgencia). Sin este campo,
    // no se valida nada (comportamiento de siempre).
    private LocalDateTime fechaHoraEntrega;
    // Mismo flag que se manda a /v1/flores/fechas-disponibles -- si el cliente eligio el plazo
    // urgente del calendario. Determina si se cobra CantidadFlorValida.cargoUrgente (modelo
    // nuevo de config-entrega) y si el pedido requiere anticipo.
    private Boolean urgente;
    // Punto exacto marcado en el mapa (opcional) -- solo hace falta cuando la zona elegida tiene
    // anillos de cobro por distancia configurados (ver DISENO_ZONAS_POR_ANILLO.md). Sin anillos
    // en esa zona, estos 2 campos se ignoran y el envio sigue siendo el costo fijo de siempre.
    private Double latitud;
    private Double longitud;
}
