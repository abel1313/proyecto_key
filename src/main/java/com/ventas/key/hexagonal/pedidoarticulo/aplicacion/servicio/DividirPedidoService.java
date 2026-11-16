package com.ventas.key.hexagonal.pedidoarticulo.aplicacion.servicio;

import com.ventas.key.hexagonal.pedidoarticulo.dominio.excepcion.ArticuloNoEncontradoException;
import com.ventas.key.hexagonal.pedidoarticulo.dominio.excepcion.EdicionPedidoException;
import com.ventas.key.hexagonal.pedidoarticulo.dominio.excepcion.PedidoCerradoException;
import com.ventas.key.hexagonal.pedidoarticulo.dominio.modelo.ArticuloDePedido;
import com.ventas.key.hexagonal.pedidoarticulo.dominio.modelo.PedidoEditable;
import com.ventas.key.hexagonal.pedidoarticulo.dominio.puerto.entrada.DividirPedidoCasoUso;
import com.ventas.key.hexagonal.pedidoarticulo.dominio.puerto.salida.CobroConTarjetaPort;
import com.ventas.key.hexagonal.pedidoarticulo.dominio.puerto.salida.DivisionDePedidoPort;
import com.ventas.key.hexagonal.pedidoarticulo.dominio.puerto.salida.PagoTrasEdicionPort;
import com.ventas.key.hexagonal.pedidoarticulo.dominio.puerto.salida.PedidoArticuloPort;
import com.ventas.key.hexagonal.pedidoarticulo.dominio.puerto.salida.RepreciarPorCantidadPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Orquesta la division de un pedido: valida, crea el pedido nuevo con la linea movida y recalcula los dos pedidos. Todo o nada (una transaccion).
 *
 * <p>[Hexagonal: dentro del hexagono] [Clean: Use Case interactor]
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DividirPedidoService implements DividirPedidoCasoUso {

    private final PedidoArticuloPort pedidos;
    private final CobroConTarjetaPort cobrosConTarjeta;
    private final PagoTrasEdicionPort pagos;
    private final DivisionDePedidoPort division;

    /** Precio por cantidad y precio manual (opcional: sin el, todo como antes). */
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private RepreciarPorCantidadPort preciosPorCantidad;

    @Override
    @Transactional
    public int pasarArticuloACliente(Integer pedidoId, Integer detalleId, Integer cantidad, DestinoCliente destino) {
        division.bloquear(pedidoId);
        PedidoEditable origen = pedidos.buscarPedido(pedidoId)
                .orElseThrow(() -> new ArticuloNoEncontradoException("Pedido no encontrado: " + pedidoId));
        if (origen.estaCerrado()) {
            throw new PedidoCerradoException(pedidoId, origen.motivoDelCierre());
        }
        if (!origen.admiteCambioDePrecio()) {
            throw new EdicionPedidoException("Solo se puede pasar un artículo a otro cliente en un pedido en Apartado o Ir pagando que todavía no está pagado. "
                    + "Este pedido ya está en estado " + origen.estado());
        }
        if (cobrosConTarjeta.tieneCobroConTarjeta(pedidoId)) {
            throw new EdicionPedidoException("El pedido " + pedidoId + " tiene un cobro con tarjeta en proceso o sin devolver por completo: no se puede dividir todavía");
        }
        if (division.estaEnGrupoActivo(pedidoId)) {
            throw new EdicionPedidoException("Este pedido está unido a otros: sepáralo del grupo antes de pasar un artículo a otro cliente");
        }
        ArticuloDePedido linea = origen.linea(detalleId)
                .orElseThrow(() -> ArticuloNoEncontradoException.lineaDePedido(pedidoId, detalleId));
        if (linea.esDePromocion()) {
            throw new EdicionPedidoException("'" + linea.nombre() + "' es parte de una promoción (combo): no se puede pasar suelto a otro cliente");
        }
        if (destino == null || !destino.esValido()) {
            throw new EdicionPedidoException("Elige a qué cliente pasar el artículo");
        }
        if (!division.destinoValido(pedidoId, destino)) {
            throw new EdicionPedidoException("Ese cliente no existe o es el mismo cliente de este pedido");
        }
        int piezas = cantidad == null ? linea.cantidad() : cantidad;
        if (linea.varianteId() == null || linea.precioUnitario() <= 0) {
            throw new EdicionPedidoException("'" + linea.nombre() + "' no se puede pasar a otro cliente (sin artículo o sin precio)");
        }
        if (piezas < 1 || piezas > linea.cantidad()) {
            throw new EdicionPedidoException("La cantidad a pasar debe ser de 1 a " + linea.cantidad() + " pieza(s)");
        }
        boolean todasLasPiezas = piezas == linea.cantidad();
        if (todasLasPiezas && origen.articulos().size() == 1) {
            throw new EdicionPedidoException("Es el único artículo del pedido: no se puede dejar vacío. Para pasarlo todo a otro cliente, cancela este pedido o cambia su cliente");
        }
        String origenDelPrecio = preciosPorCantidad == null ? null : preciosPorCantidad.origenDelPrecio(detalleId).orElse(null);
        if ("CAMPANA".equals(origenDelPrecio)) {
            throw new EdicionPedidoException("'" + linea.nombre() + "' lleva el precio de campaña de este cliente: ese precio no se pasa a otro cliente. "
                    + "Cambia el precio de la línea (botón «$») si quieres pasarlo");
        }
        // Lo que ya se pago se queda en este pedido: no puede quedar con dinero de mas.
        double totalNuevo = origen.total() - piezas * linea.precioUnitario();
        if (totalNuevo < origen.totalPagado() - 0.005) {
            throw new EdicionPedidoException(String.format(java.util.Locale.US,
                    "El cliente de este pedido ya pagó $%.2f y sin esas piezas el pedido quedaría en $%.2f: le sobrarían $%.2f. "
                            + "Los abonos se quedan en este pedido. Pasa menos piezas o cámbialo cuando el saldo lo permita",
                    origen.totalPagado(), totalNuevo, origen.totalPagado() - totalNuevo));
        }

        int nuevoId = division.crearPedidoParaCliente(pedidoId, destino, "Pasado del pedido #" + pedidoId);
        Integer nuevaLineaId = pedidos.agregarLinea(nuevoId, linea.varianteId(), piezas, linea.precioUnitario());
        if (preciosPorCantidad != null && origenDelPrecio != null) {
            preciosPorCantidad.copiarPrecioDeAntes(detalleId, nuevaLineaId);
        }
        if (todasLasPiezas) {
            pedidos.borrarLineas(List.of(linea));
            if (preciosPorCantidad != null) {
                preciosPorCantidad.olvidarPrecioDeAntes(detalleId);
            }
        } else {
            pedidos.cambiarCantidad(detalleId, linea.cantidad() - piezas);
        }
        recalcular(pedidoId);
        recalcular(nuevoId);
        PedidoEditable despues = pedidos.buscarPedido(pedidoId).orElseThrow();
        if (despues.total() < despues.totalPagado() - 0.005) {
            throw new EdicionPedidoException("Con el precio vigente, el pedido de origen quedaría con dinero de más; no se pasó nada");
        }
        log.info("Pedido {}: {} pieza(s) de la linea {} pasaron al pedido nuevo {} (otro cliente)", pedidoId, piezas, detalleId, nuevoId);
        return nuevoId;
    }

    /** Igual que al editar articulos: reprecia por cantidad, guarda el total desde cero y deja el estado como corresponde a los abonos. */
    private void recalcular(Integer pedidoId) {
        if (preciosPorCantidad != null) {
            preciosPorCantidad.repreciar(pedidoId);
        }
        PedidoEditable actualizado = pedidos.buscarPedido(pedidoId)
                .orElseThrow(() -> new ArticuloNoEncontradoException("Pedido no encontrado: " + pedidoId));
        pedidos.guardarTotal(pedidoId, actualizado.total());
        pagos.ajustarTrasEditar(pedidoId);
    }
}
