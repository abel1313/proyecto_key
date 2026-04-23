package com.ventas.key.mis.productos.repository;


import com.ventas.key.mis.productos.entity.productoVariantes.Variantes;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IVarianteRepository extends BaseRepository<Variantes, Integer> {

    List<Variantes> findByProductoId(Integer productoId);

    Page<Variantes> findByProductoId(Integer productoId, Pageable pageable);

    List<Variantes> findByProductoNombreContainingIgnoreCase(String nombre);

    List<Variantes> findByProductoCodigoBarrasCodigoBarras(String codigoBarras);
}
