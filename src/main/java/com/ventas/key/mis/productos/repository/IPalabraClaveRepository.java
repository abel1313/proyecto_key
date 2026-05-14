package com.ventas.key.mis.productos.repository;

import com.ventas.key.mis.productos.entity.PalabraClave;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
public interface IPalabraClaveRepository extends BaseRepository<PalabraClave, Integer> {

    Page<PalabraClave> findByNombreContainingIgnoreCase(String nombre, Pageable pageable);
}
