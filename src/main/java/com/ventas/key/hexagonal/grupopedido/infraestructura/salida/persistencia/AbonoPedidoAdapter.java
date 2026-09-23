package com.ventas.key.hexagonal.grupopedido.infraestructura.salida.persistencia;

import com.ventas.key.hexagonal.grupopedido.dominio.puerto.salida.AbonoPedidoPort;
import com.ventas.key.mis.productos.entity.Pedido;
import com.ventas.key.mis.productos.models.abonos.AbonoRequest;
import com.ventas.key.mis.productos.repository.IPedidoRepository;
import com.ventas.key.mis.productos.service.api.IAbonoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Registra cada parte del reparto con el abono de siempre ({@code AbonoServiceImpl.registrarAbono}).
 *
 * <p>[Hexagonal: Driven Adapter] [Clean: Frameworks & Drivers]
 */
@Component
@RequiredArgsConstructor
public class AbonoPedidoAdapter implements AbonoPedidoPort {

    private final IAbonoService abonoService;
    private final IPedidoRepository pedidoRepository;

    @Override
    public void abonar(Integer pedidoId, long montoCentavos, String metodoPago, String nota, Integer usuarioId) {
        AbonoRequest request = new AbonoRequest();
        request.setMonto(montoExacto(pedidoId, montoCentavos));
        request.setMetodoPago(metodoPago);
        // abono_pedido.nota es VARCHAR(200)
        request.setNota(nota != null && nota.length() > 200 ? nota.substring(0, 200) : nota);
        request.setUsuarioId(usuarioId);
        abonoService.registrarAbono(pedidoId, request);
    }

    /**
     * El dominio reparte en centavos, pero la tabla guarda {@code double}: un saldo de $100.20
     * puede estar guardado como 100.19999999. Si se mandara 100.20, registrarAbono lo rechazaria
     * por "excede el saldo". Cuando la parte liquida el pedido se manda el saldo tal cual esta.
     */
    private double montoExacto(Integer pedidoId, long montoCentavos) {
        double monto = montoCentavos / 100.0;
        Pedido pedido = pedidoRepository.findById(pedidoId).orElse(null);
        if (pedido == null || pedido.getTotalPedido() == null) {
            return monto;
        }
        double pagado = pedido.getTotalPagado() != null ? pedido.getTotalPagado() : 0.0;
        double saldo = pedido.getTotalPedido() - pagado;
        return Math.abs(saldo - monto) < 0.005 ? saldo : monto;
    }
}
