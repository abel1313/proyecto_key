package com.ventas.key.mis.productos.repository;

import com.ventas.key.mis.productos.entity.TemaVariable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ITemaVariableRepository extends BaseRepository<TemaVariable, Integer> {

    List<TemaVariable> findByClaveIgnoreCase(String clave);

    default List<TemaVariable> findAllOrdenadas() {
        return findAll(Sort.by(Sort.Direction.ASC, "orden", "id"));
    }
}
