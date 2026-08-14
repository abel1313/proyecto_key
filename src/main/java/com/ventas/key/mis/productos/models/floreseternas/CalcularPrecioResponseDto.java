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

    // Monto de mano de obra ya incluido en "total" -- informativo/interno, el front NO debe
    // mostrarlo como linea aparte al cliente (decision del dueno: "un solo precio de ramo", igual
    // que el papel automatico). Null si esta cantidad no tiene mano de obra configurada.
    private Double precioManoDeObra;

    // Extra por urgencia ya incluido en "total" -- mismo criterio que precioManoDeObra (sin linea
    // aparte para el cliente). Solo se calcula si el request trae fechaHoraEntrega Y esta cantidad
    // tiene horasMinimasAnticipacion configurada. Null = no aplico (sin fecha, o la entrega no es
    // de un dia para otro, o sin precioUrgencia configurado para esta cantidad).
    private Double precioUrgencia;

    // false = con la fecha/hora de entrega pedida NO da tiempo de armar este tamano de ramo (+
    // zona) -- el pedido NO se puede generar tal cual. Default true (sin fechaHoraEntrega, o con
    // tiempo suficiente). Cuando es false, el resto del response SI se sigue calculando (para que
    // el front pueda mostrar el precio igual), pero el front debe bloquear continuar y usar
    // mensajeEntrega para que el cliente cambie la fecha/hora o la cantidad.
    private Boolean entregaValida = true;
    private String mensajeEntrega;

    // true cuando precioUrgencia aplico -- senal para que el front cree el pedido con
    // tipoPedido:"APARTADO" en /v1/pedidos/savePedido (no "NORMAL") y registre montoAnticipoSugerido
    // como abono inicial via POST /v1/abonos/{pedidoId} -- MISMO mecanismo de credito que ya existe
    // en el sistema, no hay endpoint nuevo. El 50% es un ENGANCHE que sale del total, no un cobro
    // aparte: el pedido nace en $0 pagado y el abono cubre esa mitad, el resto se liquida despues
    // con otro abono al entregar.
    private Boolean requiereAnticipo = false;
    // 50% de "total" (incluye envio), solo presente cuando requiereAnticipo=true.
    private Double montoAnticipoSugerido;

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
