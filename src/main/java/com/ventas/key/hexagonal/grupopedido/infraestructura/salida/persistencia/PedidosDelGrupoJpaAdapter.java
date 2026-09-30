package com.ventas.key.hexagonal.grupopedido.infraestructura.salida.persistencia;

import com.ventas.key.hexagonal.grupopedido.dominio.modelo.PedidoDelGrupo;
import com.ventas.key.hexagonal.grupopedido.dominio.puerto.salida.PedidosDelGrupoPort;
import com.ventas.key.mis.productos.entity.Pedido;
import com.ventas.key.mis.productos.repository.IPedidoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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

    @Override
    public PaginaDePedidos candidatos(String tipo, Integer excluir, String buscar, int pagina, int tamano) {
        Page<Integer> ids = pedidoRepository.candidatosParaUnir(tipo, excluir == null ? 0 : excluir,
                buscar == null ? "" : buscar.trim(), PageRequest.of(pagina, tamano));
        Map<Integer, PedidoDelGrupo> porId = new HashMap<>();
        for (Pedido p : pedidoRepository.findAllById(ids.getContent())) {
            porId.put(p.getId(), aDominio(p));
        }
        // findAllById no respeta el orden: se regresa en el del query (del mas nuevo al mas viejo).
        List<PedidoDelGrupo> enOrden = ids.getContent().stream().map(porId::get).filter(Objects::nonNull).toList();
        return new PaginaDePedidos(enOrden, ids.hasNext());
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
