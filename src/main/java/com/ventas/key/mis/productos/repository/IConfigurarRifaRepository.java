package com.ventas.key.mis.productos.repository;

import org.springframework.stereotype.Repository;

import com.ventas.key.mis.productos.entity.ConfigurarRifa;

import java.util.List;

@Repository
public interface IConfigurarRifaRepository extends BaseRepository<ConfigurarRifa, Integer> {

    List<ConfigurarRifa> findByActivaTrue();
}
