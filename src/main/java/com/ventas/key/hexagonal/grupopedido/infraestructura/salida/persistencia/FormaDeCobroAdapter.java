package com.ventas.key.hexagonal.grupopedido.infraestructura.salida.persistencia;

import com.ventas.key.hexagonal.grupopedido.dominio.puerto.salida.FormaDeCobroPort;
import com.ventas.key.mis.productos.service.api.IPedidoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Cambia la forma de cobro con {@code PedidoServiceImpl.cambiarTipoDentroDeGrupo}, la misma logica
 * que {@code PUT /v1/pedidos/{id}/tipo} pero sin la guardia de "esta en un grupo" (R9): aqui el
 * cambio es del grupo completo y lo valido el dominio (R20).
 *
 * <p>[Hexagonal: Driven Adapter] [Clean: Frameworks & Drivers]
 */
@Component
@RequiredArgsConstructor
public class FormaDeCobroAdapter implements FormaDeCobroPort {

    private final IPedidoService pedidoService;

    @Override
    public void cambiar(Integer pedidoId, String tipoNuevo, String nota) {
        pedidoService.cambiarTipoDentroDeGrupo(pedidoId, tipoNuevo, nota);
    }
}
