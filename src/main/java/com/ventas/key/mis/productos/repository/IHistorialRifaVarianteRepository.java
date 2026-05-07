package com.ventas.key.mis.productos.repository;

import com.ventas.key.mis.productos.entity.HistorialRifaVariante;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IHistorialRifaVarianteRepository extends BaseRepository<HistorialRifaVariante, Integer> {

    List<HistorialRifaVariante> findByConfigurarRifaIdOrderByOrdenAsc(Integer configurarRifaId);
}