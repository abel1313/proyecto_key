package com.ventas.key.hexagonal.datosprueba.dominio.puerto.salida;

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
}
