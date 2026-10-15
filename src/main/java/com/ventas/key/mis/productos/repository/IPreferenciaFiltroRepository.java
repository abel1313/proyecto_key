package com.ventas.key.mis.productos.repository;

import com.ventas.key.mis.productos.entity.PreferenciaFiltro;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IPreferenciaFiltroRepository extends JpaRepository<PreferenciaFiltro, Integer> {

    Optional<PreferenciaFiltro> findByUsuarioIdAndPantalla(Integer usuarioId, String pantalla);

    void deleteByUsuarioIdAndPantalla(Integer usuarioId, String pantalla);
}
