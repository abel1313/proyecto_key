package com.ventas.key.mis.productos.repository;

import com.ventas.key.mis.productos.entity.CintaPromocion;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ICintaPromocionRepository extends BaseRepository<CintaPromocion, Integer> {

    List<CintaPromocion> findByActivoTrueOrderByOrdenAsc();
}
