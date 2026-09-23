package com.ventas.key.hexagonal.grupopedido.infraestructura.salida.persistencia;

import com.ventas.key.hexagonal.grupopedido.dominio.puerto.salida.EstadoDePagoPort;
import com.ventas.key.mis.productos.service.api.IAbonoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Usa {@code AbonoServiceImpl}, que es quien crea la venta al liquidar: dos caminos que liquidan
 * serian dos ventas distintas.
 *
 * <p>[Hexagonal: Driven Adapter] [Clean: Frameworks & Drivers]
 */
@Component
@RequiredArgsConstructor
public class EstadoDePagoAdapter implements EstadoDePagoPort {

    private final IAbonoService abonoService;

    @Override
    public void ajustarAlosAbonos(Integer pedidoId, Integer usuarioId) {
        abonoService.ajustarEstadoALosAbonos(pedidoId, usuarioId);
    }
}
