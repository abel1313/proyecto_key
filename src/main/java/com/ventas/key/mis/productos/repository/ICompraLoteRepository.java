package com.ventas.key.mis.productos.repository;

import com.ventas.key.mis.productos.entity.CompraLoteEntidad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ICompraLoteRepository extends JpaRepository<CompraLoteEntidad, Integer> {

    @Query("SELECT DISTINCT l FROM CompraLoteEntidad l LEFT JOIN FETCH l.gastos ORDER BY l.fechaCompra DESC, l.id DESC")
    List<CompraLoteEntidad> listarConGastos();

    @Query("SELECT DISTINCT l FROM CompraLoteEntidad l LEFT JOIN FETCH l.gastos WHERE l.id IN "
            + "(SELECT p.lote.id FROM CompraLoteProductoEntidad p WHERE p.producto.id = :productoId) ORDER BY l.fechaCompra DESC, l.id DESC")
    List<CompraLoteEntidad> deProducto(@Param("productoId") Integer productoId);

    @Query("SELECT l FROM CompraLoteEntidad l LEFT JOIN FETCH l.gastos WHERE l.id = :id")
    Optional<CompraLoteEntidad> buscarConGastos(@Param("id") Integer id);
}
