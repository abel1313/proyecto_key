package com.ventas.key.hexagonal.pedidoarticulo.infraestructura.salida.persistencia;

import com.ventas.key.hexagonal.pedidoarticulo.dominio.puerto.salida.CobroConTarjetaPort;
import com.ventas.key.mis.productos.repository.IPagoTarjetaIntentoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Pregunta si el pedido tiene un cobro con tarjeta en proceso o sin devolver del todo.
 *
 * <p>[Hexagonal: Driven Adapter] [Clean: Interface Adapter]
 *
 * <p>Con el modulo apagado ({@code pagos.tarjeta.habilitado=false}, o produccion sin la migracion) no se consulta
 * la tabla: no hay cobros con tarjeta que proteger.
 */
@Component
public class CobroDePedidoArticuloConTarjetaAdapter implements CobroConTarjetaPort {

    private static final List<String> ESTADOS_VIVOS =
            List.of("CREADO", "ENVIADO", "APROBADO", "COBRADO_SIN_APLICAR", "DEVUELTO_PARCIAL");

    @Autowired(required = false)
    private IPagoTarjetaIntentoRepository intentos;

    @Value("${pagos.tarjeta.habilitado:false}")
    private boolean habilitado;

    @Override
    public boolean tieneCobroConTarjeta(Integer pedidoId) {
        return habilitado && intentos != null && intentos.existsByPedidoIdAndEstadoIn(pedidoId, ESTADOS_VIVOS);
    }
}
