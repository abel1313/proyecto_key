package com.ventas.key.mis.productos.repository;

import com.ventas.key.mis.productos.entity.ConfigurarRifaProducto;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IConfigurarRifaProductoRepository extends BaseRepository<ConfigurarRifaProducto, Integer> {

    List<ConfigurarRifaProducto> findByConfigurarRifaIdOrderByOrdenAsc(Integer configurarRifaId);
}