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
    // Una linea por cada color pedido, cada una con su propio varianteId -- usarlas para armar
    // las lineas de /v1/pedidos/savePedido (una por color, no una sola linea "de flores").
    private List<ColorCalculadoDto> coloresCalculados;

    private Boolean papelObligatorioAplicado;
    // Total a cobrar por papel (ya multiplicado por los pliegos si aplica).
    private Double precioPapel;
    // Variante "sombra" del accesorio marcado como "es papel" -- null si no aplico la regla.
    private Integer papelVarianteId;
    // Cuantos pliegos se necesitan para este ramo -- null si el papel no tiene floresPorPliego
    // configurado (precio fijo unico, retrocompatible) o si no aplico ningun papel.
    private Integer pliegosPapel;
    // precioUnitario EXACTO que hay que mandar en la linea de /v1/pedidos/savePedido para esta
    // variante (Producto.precioVenta del producto sombra) -- usar cantidad=pliegosPapel (o 1 si
    // es null) y precioUnitario=este campo, NUNCA precioPapel/1, o el pedido real lo rechaza por
    // no coincidir con el precio de catalogo (ver PedidoServiceImpl.validarPrecioCatalogo).
    private Double precioUnitarioPapel;

    private List<AccesorioCalculadoDto> accesoriosCalculados;
    private Double subtotalAccesorios;

    private List<ListonCalculadoDto> listonesCalculados;
    private Double subtotalListones;
    // true si algun liston quedo con frase personalizada sin validar todavia. NO implica que se
    // cobre nada ahora -- el anticipo real (monto + cuando se cobra) se define recien cuando el
    // admin valida la frase (ver POST /v1/flores/pedidos/detalle/{id}/validar-frase). El texto
    // de aviso para el cliente esta en FloresEternasConstantes.AVISO_FRASE_PENDIENTE.
    private Boolean tieneListonPendienteValidacion;
    private String avisoFrasePendiente;

    private Boolean recogerEnLocal;
    private Double costoEnvio;
    // Variante "sombra" del lugar de entrega elegido -- null si recogerEnLocal o si el lugar
    // no tiene costo de envio configurado (nada que cobrar, no hace falta linea).
    private Integer envioVarianteId;

    // Total de todo lo que ya tiene precio conocido (NO incluye la frase personalizada
    // pendiente, que todavia no tiene precio -- ver arriba).
    private Double total;
}
