package com.ventas.key.hexagonal.datosprueba.dominio.puerto.salida;

import com.ventas.key.hexagonal.datosprueba.dominio.modelo.DatosDelModelo;
import com.ventas.key.hexagonal.datosprueba.dominio.modelo.LineaDePedido;

import java.util.List;

/**
 * [Hexagonal: Driven Port] Lo que hace falta para preparar una prueba (R16), por los mismos
 * servicios que usa la pantalla (R8): el pedido que hace el cliente desde su cuenta, la venta directa,
 * el abono y la cancelacion. Va aparte de {@link PedidosPruebaPort} para no cambiar ese puerto.
 *
 * <p>R20: cada pedido se crea con un cliente que lleva el nombre exacto de la prueba y la clave
 * ({@code NombresDePrueba.cliente}: "Prueba 24 K4"), para que lo que dice el documento sea lo que se ve
 * en pantalla.
 */
public interface EscenariosPruebaPort {

    /** Una zona de entrega de prueba (no "recoger en tienda"). La reusa si ya existe. */
    int zonaDePrueba();

    /**
     * Un pedido 🕓 Pendiente, como el que hace el cliente desde su cuenta. El cliente es uno con cuenta,
     * correo verificado y datos completos, llamado "Prueba {prueba} {clave}", con un correo .invalid (no le
     * llega a nadie); se reusa si ya existe. {@code lugarEntregaId} null = recoge en el local. Regresa su numero.
     */
    int pedidoDelCliente(String prueba, String clave, List<LineaDePedido> lineas, Integer lugarEntregaId);

    /**
     * Un pedido por la venta directa, a nombre de un cliente "Prueba {prueba} {clave}" (se reusa si ya
     * existe). {@code tipoPedido}: NORMAL (Contado en efectivo), APARTADO o FIADO (Ir pagando).
     * {@code seLoLlevo} = false: queda "Falta entregar". Regresa su numero.
     */
    int ventaDirecta(String tipoPedido, String prueba, String clave, int usuarioId, List<LineaDePedido> lineas,
                     Integer lugarEntregaId, boolean seLoLlevo);

    /** Lo cancela como el boton ✖ Cancelar con el motivo "El cliente aviso" (devuelve el stock). */
    void cancelar(int pedidoId);

    // ── Reiniciar una prueba (R21): encontrar y quitar lo que se metió ──

    /**
     * Los pedidos que todavía no están cancelados y llevan artículos de esta prueba (modelos de marca
     * "Prueba QA" que empiezan con "QA Prueba {prueba} "). Incluye los de antes de que los pedidos llevaran
     * nombre de prueba.
     */
    List<Integer> pedidosDeLaPrueba(String prueba);

    /**
     * Baja lógica de los modelos y artículos de esta prueba (stock en 0, sin ligas de imagen): nunca se borra
     * una fila ni una imagen. Solo toca modelos de marca "Prueba QA". Regresa cuántos modelos dio de baja.
     */
    int darDeBajaCatalogoDeLaPrueba(String prueba);

    // ── Catálogo (R18): dejar un modelo o artículo recién creado en el estado que pide la prueba ──

    /** Nombre y código de barras del modelo de ese artículo, para buscarlo en pantalla. */
    DatosDelModelo datosDelModelo(int articuloId);

    /** Stock del modelo de ese artículo (stock libre = este menos lo repartido en sus artículos). */
    void stockDelModelo(int articuloId, int stock);

    /** Habilita o deshabilita el modelo de ese artículo (sus artículos se quedan como están). */
    void habilitarModelo(int articuloId, boolean habilitado);

    /**
     * Un artículo deshabilitado que guarda stock: el caso de los artículos viejos (hoy, al
     * deshabilitar uno, su stock queda en 0). Solo para las pruebas que revisan ese caso.
     */
    void articuloDeshabilitadoConStock(int articuloId, int stock);

    /** Precio propio del artículo con el mismo caso de uso que la pantalla (💲 de la tarjeta). */
    void precioDelArticulo(int articuloId, double precioVenta, double precioRebaja, boolean usarDescuento);

    /** Que la tienda muestre lo nuevo de inmediato (la misma limpieza que hace guardar un pedido). */
    void limpiarCache();
}
