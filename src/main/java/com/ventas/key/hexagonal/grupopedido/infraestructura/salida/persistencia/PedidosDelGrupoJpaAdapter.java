package com.ventas.key.hexagonal.grupopedido.infraestructura.salida.persistencia;

import com.ventas.key.hexagonal.grupopedido.dominio.modelo.PedidoDelGrupo;
import com.ventas.key.hexagonal.grupopedido.dominio.puerto.salida.PedidosDelGrupoPort;
import com.ventas.key.mis.productos.entity.Pedido;
import com.ventas.key.mis.productos.repository.IPedidoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * Los pedidos del grupo, leidos de la tabla {@code pedidos}.
 *
 * <p>[Hexagonal: Driven Adapter] [Clean: Frameworks & Drivers]
 */
@Component
@RequiredArgsConstructor
public class PedidosDelGrupoJpaAdapter implements PedidosDelGrupoPort {

    private final IPedidoRepository pedidoRepository;

    @Override
    public List<PedidoDelGrupo> buscar(Collection<Integer> pedidoIds) {
        if (pedidoIds == null || pedidoIds.isEmpty()) {
            return List.of();
        }
        return pedidoRepository.findAllById(pedidoIds).stream()
                .map(PedidosDelGrupoJpaAdapter::aDominio)
                .toList();
    }

    static PedidoDelGrupo aDominio(Pedido p) {
        return new PedidoDelGrupo(
                p.getId(),
                p.getTipoPedido() != null ? p.getTipoPedido() : "NORMAL",
                p.getEstadoPedido(),
                centavos(p.getTotalPedido()),
                centavos(p.getTotalPagado()),
                registro(p),
                nombreCliente(p));
    }

    static long centavos(Double monto) {
        return monto == null ? 0 : Math.round(monto * 100);
    }

    // Los pedidos anteriores a 2026-07-07 no tienen hora real: se usa la fecha a medianoche.
    private static LocalDateTime registro(Pedido p) {
        if (p.getFechaHoraRegistro() != null) {
            return p.getFechaHoraRegistro();
        }
        return p.getFechaPedido() != null ? p.getFechaPedido().atStartOfDay() : null;
    }

    private static String nombreCliente(Pedido p) {
        if (p.getCliente() != null) {
            return p.getCliente().getNombrePersona();
        }
        if (p.getClienteSinRegistro() != null) {
            return p.getClienteSinRegistro().getNombrePersona();
        }
        return "Sin nombre";
    }
}
