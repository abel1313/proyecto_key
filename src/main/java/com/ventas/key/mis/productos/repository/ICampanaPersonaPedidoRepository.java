package com.ventas.key.mis.productos.repository;

import com.ventas.key.mis.productos.entity.CampanaPersonaPedidoEntidad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;

public interface ICampanaPersonaPedidoRepository extends JpaRepository<CampanaPersonaPedidoEntidad, Integer> {

    @Query("SELECT COALESCE(SUM(x.monto), 0) FROM CampanaPersonaPedidoEntidad x WHERE x.campanaId = :campanaId AND x.personaId = :personaId "
            + "AND EXISTS (SELECT 1 FROM Pedido p WHERE p.id = x.pedidoId AND LOWER(p.estadoPedido) NOT IN ('pendiente', 'cancelado'))")
    BigDecimal acumulado(@Param("campanaId") Integer campanaId, @Param("personaId") Integer personaId);

    @Query("SELECT x.personaId FROM CampanaPersonaPedidoEntidad x WHERE x.pedidoId = :pedidoId")
    java.util.List<Integer> personaDelPedido(@Param("pedidoId") Integer pedidoId);

    // Un pedido Pendiente (tarjeta sin aprobar) o cancelado NO suma hacia la meta: solo lo que de verdad se pagó y sigue en pie.
    /** [personaId, suma, cantidad de pedidos] de cada persona en la campana, la que mas lleva primero. */
    @Query("SELECT x.personaId, SUM(x.monto), COUNT(x) FROM CampanaPersonaPedidoEntidad x WHERE x.campanaId = :campanaId "
            + "AND EXISTS (SELECT 1 FROM Pedido p WHERE p.id = x.pedidoId AND LOWER(p.estadoPedido) NOT IN ('pendiente', 'cancelado')) "
            + "GROUP BY x.personaId ORDER BY SUM(x.monto) DESC")
    java.util.List<Object[]> resumenPorPersona(@Param("campanaId") Integer campanaId);

    boolean existsByCampanaIdAndPersonaIdAndPedidoId(Integer campanaId, Integer personaId, Integer pedidoId);

    @Modifying
    @Query("DELETE FROM CampanaPersonaPedidoEntidad x WHERE x.personaId = :personaId")
    void borrarDePersona(@Param("personaId") Integer personaId);
}
