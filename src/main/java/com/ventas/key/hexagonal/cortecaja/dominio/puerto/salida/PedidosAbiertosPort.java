package com.ventas.key.hexagonal.cortecaja.dominio.puerto.salida;

import com.ventas.key.hexagonal.cortecaja.dominio.modelo.FiltroDeCorte;
import com.ventas.key.hexagonal.cortecaja.dominio.modelo.PedidoPorCobrar;

import java.util.List;

/** [Hexagonal: Driven Port] [Clean: Gateway] Los pedidos (sin cancelar) que cumplen zona, fechas y entrega del filtro. Solo lee. */
public interface PedidosAbiertosPort {

    /**
     * Pedidos sin cancelar de la zona y las fechas pedidas, y con la entrega que pide el filtro. NO filtra por pago: eso lo decide el dominio
     * con el saldo de cada pedido.
     *
     * @throws com.ventas.key.hexagonal.cortecaja.dominio.excepcion.CorteDeCajaException.DemasiadosPedidos si salen más de los que se pueden sumar
     */
    List<PedidoPorCobrar> buscar(FiltroDeCorte filtro);
}
