package com.ventas.key.hexagonal.datosprueba.dominio.puerto.salida;

import com.ventas.key.hexagonal.datosprueba.dominio.modelo.DatosDelModelo;
import com.ventas.key.hexagonal.datosprueba.dominio.modelo.LineaDePedido;

import java.util.List;

/**
 * [Hexagonal: Driven Port] Lo que hace falta para preparar una prueba (R16), por los mismos
 * servicios que usa la pantalla (R8): el pedido que hace el cliente desde su cuenta, la venta directa,
 * el abono y la cancelacion. Va aparte de {@link PedidosPruebaPort} para no cambiar ese puerto.
 */
public interface EscenariosPruebaPort {

    /** Una zona de entrega de prueba (no "recoger en tienda"). La reusa si ya existe. */
    int zonaDePrueba();

    /**
     * Un cliente con cuenta, correo verificado y datos completos, como el que pide desde la tienda.
     * Su correo termina en .invalid: ningun correo le llega a nadie. Lo reusa si ya existe.
     */
    int clienteConCuentaDePrueba();

    /**
     * Un pedido 🕓 Pendiente, como el que hace el cliente desde su cuenta. {@code lugarEntregaId} null
     * = recoge en el local. Regresa su numero.
     */
    int pedidoDelCliente(int clienteId, List<LineaDePedido> lineas, Integer lugarEntregaId);

    /**
     * Un pedido por la venta directa. {@code tipoPedido}: NORMAL (Contado en efectivo), APARTADO o
     * FIADO (Ir pagando). {@code seLoLlevo} = false: queda "Falta entregar". Regresa su numero.
     */
    int ventaDirecta(String tipoPedido, int clienteSinRegistroId, int usuarioId, List<LineaDePedido> lineas,
                     Integer lugarEntregaId, boolean seLoLlevo);

    /** Lo cancela como el boton ✖ Cancelar con el motivo "El cliente aviso" (devuelve el stock). */
    void cancelar(int pedidoId);

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
