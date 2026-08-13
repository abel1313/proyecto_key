package com.ventas.key.mis.productos.repository;

import com.ventas.key.mis.productos.entity.ColorFlor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IColorFlorRepository extends BaseRepository<ColorFlor, Integer> {
    List<ColorFlor> findByTipoFlorIdAndActivoTrue(Integer tipoFlorId);
}
