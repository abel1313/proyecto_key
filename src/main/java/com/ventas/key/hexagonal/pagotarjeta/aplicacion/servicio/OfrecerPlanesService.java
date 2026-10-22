package com.ventas.key.hexagonal.pagotarjeta.aplicacion.servicio;

import com.ventas.key.hexagonal.pagotarjeta.dominio.excepcion.PagoConTarjetaException;
import com.ventas.key.hexagonal.pagotarjeta.dominio.modelo.PedidoACobrar;
import com.ventas.key.hexagonal.pagotarjeta.dominio.modelo.ReglasMsi;
import com.ventas.key.hexagonal.pagotarjeta.dominio.puerto.entrada.OfrecerPlanesCasoUso;
import com.ventas.key.hexagonal.pagotarjeta.dominio.puerto.salida.PedidoACobrarPort;
import com.ventas.key.hexagonal.pagotarjeta.dominio.puerto.salida.PlanesMsiPort;

import java.util.List;

/**
 * [Hexagonal: implementacion del puerto de entrada] [Clean: Use Case Interactor]
 *
 * <p>Dice que meses sin intereses se pueden ofrecer para un pedido. Las reglas viven en {@link ReglasMsi}.
 * Sin {@code @Service} todavia (ver {@link CobroConTarjetaService}).
 */
public class OfrecerPlanesService implements OfrecerPlanesCasoUso {

    private final PedidoACobrarPort pedidos;
    private final PlanesMsiPort planes;

    public OfrecerPlanesService(PedidoACobrarPort pedidos, PlanesMsiPort planes) {
        this.pedidos = pedidos;
        this.planes = planes;
    }

    @Override
    public List<Integer> mesesPara(Integer pedidoId) {
        PedidoACobrar pedido = pedidos.buscar(pedidoId)
                .orElseThrow(() -> PagoConTarjetaException.pedidoNoExiste(pedidoId));
        return ReglasMsi.mesesOfrecidos(pedido.tipo(), pedido.total(), pedido.articulos(),
                planes.planes(), planes.minimoParaMsi());
    }
}
