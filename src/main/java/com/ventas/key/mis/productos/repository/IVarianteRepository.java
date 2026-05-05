package com.ventas.key.mis.productos.repository;


import com.ventas.key.mis.productos.entity.productoVariantes.Variantes;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IVarianteRepository extends BaseRepository<Variantes, Integer> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT v FROM Variantes v WHERE v.id = :id")
    Optional<Variantes> findByIdWithLock(@Param("id") Integer id);

    List<Variantes> findByProductoId(Integer productoId);

    Page<Variantes> findByProductoId(Integer productoId, Pageable pageable);

    List<Variantes> findByProductoNombreContainingIgnoreCase(String nombre);
    Page<Variantes> findByProductoNombreContainingIgnoreCase(String nombre, Pageable pageable);
    Page<Variantes> findByStockGreaterThanAndProducto_HabilitadoAndProducto_NombreContainingIgnoreCase(int stock, char habilitado, String nombre, Pageable pageable);

    List<Variantes> findByProductoCodigoBarrasCodigoBarras(String codigoBarras);
    Page<Variantes> findByProductoCodigoBarrasCodigoBarras(String codigoBarras, Pageable pageable);
    Page<Variantes> findByStockGreaterThanAndProducto_Habilitado(int stock, char habilitado, Pageable pageable);
    Page<Variantes> findByStockGreaterThanAndProducto_HabilitadoAndProducto_CodigoBarras_CodigoBarrasContaining(int stock, char habilitado, String codigoBarras, Pageable pageable);

    Page<Variantes> findByStockGreaterThanAndProductoHabilitado(int stock, char habilitado, Pageable pageable);

    @Query("SELECT v FROM Variantes v WHERE v.stock = 0 AND v.producto.habilitado <> '1'")
    Page<Variantes> findVariantesSinStockDeshabilitadas(Pageable pageable);
}
