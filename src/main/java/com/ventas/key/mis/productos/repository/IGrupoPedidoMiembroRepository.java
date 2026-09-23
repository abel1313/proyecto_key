package com.ventas.key.mis.productos.repository;

import com.ventas.key.mis.productos.entity.GrupoPedidoMiembro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface IGrupoPedidoMiembroRepository extends JpaRepository<GrupoPedidoMiembro, Integer> {

    /** [pedidoId, grupoId] de los que estan en un grupo activo. */
    @Query("SELECT m.pedido.id, m.grupo.id FROM GrupoPedidoMiembro m "
            + "WHERE m.grupo.activo = true AND m.pedido.id IN :pedidoIds")
    List<Object[]> gruposActivosDe(@Param("pedidoIds") Collection<Integer> pedidoIds);

    @Query("SELECT m.pedido.id FROM GrupoPedidoMiembro m WHERE m.grupo.id = :grupoId ORDER BY m.id")
    List<Integer> pedidosDelGrupo(@Param("grupoId") Integer grupoId);

    @Query("SELECT COUNT(m) > 0 FROM GrupoPedidoMiembro m WHERE m.grupo.activo = true AND m.pedido.id = :pedidoId")
    boolean estaEnGrupoActivo(@Param("pedidoId") Integer pedidoId);
}
