package com.ventas.key.hexagonal.datosprueba.dominio.puerto.salida;

import com.ventas.key.hexagonal.datosprueba.dominio.modelo.LineaDePedido;
import com.ventas.key.hexagonal.datosprueba.dominio.modelo.TipoPedidoPrueba;

import java.util.List;

/**
 * [Hexagonal: Driven Port] Pedidos de prueba, por la venta directa y el abono reales (R8): el
 * adaptador no escribe tablas, llama a los mismos servicios que la pantalla.
 */
public interface PedidosPruebaPort {

    /** Observacion que llevan todos los pedidos de prueba (R4). */
    String MARCA_PEDIDO = "[DATOS DE PRUEBA]";

    /** R10: clientes sin correo ni telefono. Reusa los que ya existan. Regresa sus ids. */
    List<Integer> clientesDePrueba(int cuantos);

    /**
     * Crea el pedido. Regresa el total que calculo la venta y el id del pedido (null en contado:
     * la venta directa de contado regresa la venta, no el pedido, y no le hace falta abonar).
     */
    PedidoCreado crearPedido(TipoPedidoPrueba tipo, int clienteId, int usuarioId, List<LineaDePedido> lineas);

    /**
     * Un abono en efectivo, como el boton "Registrar abono". El usuario queda en la venta que se
     * crea si el abono termina de pagar el pedido.
     */
    void abonar(int pedidoId, double monto, int usuarioId);

    record PedidoCreado(Integer pedidoId, double total) {
    }
}
