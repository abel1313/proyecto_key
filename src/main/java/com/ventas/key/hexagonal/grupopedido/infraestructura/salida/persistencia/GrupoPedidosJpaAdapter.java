package com.ventas.key.hexagonal.grupopedido.infraestructura.salida.persistencia;

import com.ventas.key.hexagonal.grupopedido.dominio.modelo.RegistroGrupo;
import com.ventas.key.hexagonal.grupopedido.dominio.puerto.salida.GrupoPedidosPort;
import com.ventas.key.mis.productos.entity.GrupoPedido;
import com.ventas.key.mis.productos.entity.GrupoPedidoMiembro;
import com.ventas.key.mis.productos.repository.IGrupoPedidoMiembroRepository;
import com.ventas.key.mis.productos.repository.IGrupoPedidoRepository;
import com.ventas.key.mis.productos.repository.IPedidoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Los grupos, contra las tablas {@code grupo_pedido} y {@code grupo_pedido_miembro}.
 *
 * <p>[Hexagonal: Driven Adapter] [Clean: Frameworks & Drivers]
 */
@Component
@RequiredArgsConstructor
public class GrupoPedidosJpaAdapter implements GrupoPedidosPort {

    private final IGrupoPedidoRepository grupoRepository;
    private final IGrupoPedidoMiembroRepository miembroRepository;
    private final IPedidoRepository pedidoRepository;

    @Override
    public Integer crear(Integer pedidoTitularId, List<Integer> pedidoIds, String nota, Integer usuarioId) {
        GrupoPedido grupo = new GrupoPedido();
        grupo.setPedidoTitular(pedidoRepository.getReferenceById(pedidoTitularId));
        grupo.setActivo(Boolean.TRUE);
        grupo.setFechaCreacion(LocalDateTime.now());
        grupo.setUsuarioCreoId(usuarioId);
        grupo.setNota(nota);
        for (Integer pedidoId : pedidoIds) {
            GrupoPedidoMiembro miembro = new GrupoPedidoMiembro();
            miembro.setGrupo(grupo);
            miembro.setPedido(pedidoRepository.getReferenceById(pedidoId));
            grupo.getMiembros().add(miembro);
        }
        return grupoRepository.save(grupo).getId();
    }

    @Override
    public Optional<RegistroGrupo> buscar(Integer grupoId) {
        return grupoRepository.findById(grupoId).map(g -> new RegistroGrupo(
                g.getId(),
                g.getPedidoTitular().getId(),
                Boolean.TRUE.equals(g.getActivo()),
                g.getFechaCreacion(),
                g.getNota(),
                miembroRepository.pedidosDelGrupo(g.getId())));
    }

    @Override
    public Optional<Integer> grupoActivoDe(Integer pedidoId) {
        return Optional.ofNullable(gruposActivosDe(List.of(pedidoId)).get(pedidoId));
    }

    @Override
    public Map<Integer, Integer> gruposActivosDe(Collection<Integer> pedidoIds) {
        Map<Integer, Integer> resultado = new HashMap<>();
        if (pedidoIds == null || pedidoIds.isEmpty()) {
            return resultado;
        }
        for (Object[] fila : miembroRepository.gruposActivosDe(pedidoIds)) {
            resultado.put((Integer) fila[0], (Integer) fila[1]);
        }
        return resultado;
    }

    @Override
    public void marcarDeshecho(Integer grupoId, Integer usuarioId) {
        GrupoPedido grupo = grupoRepository.findById(grupoId)
                .orElseThrow(() -> new IllegalStateException("Grupo no encontrado: " + grupoId));
        grupo.setActivo(Boolean.FALSE);
        grupo.setFechaDeshecho(LocalDateTime.now());
        grupo.setUsuarioDeshizoId(usuarioId);
        grupoRepository.save(grupo);
    }
}
