package com.ventas.key.mis.productos.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.PagingAndSortingRepository;

@NoRepositoryBean
public interface BaseRepository<Entity,TipoDato> extends JpaRepository<Entity, TipoDato> {

}
