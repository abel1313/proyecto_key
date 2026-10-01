package com.ventas.key.hexagonal.datosprueba.infraestructura.salida.persistencia;

import com.ventas.key.hexagonal.datosprueba.dominio.puerto.salida.AmbientePort;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * [Hexagonal: Driven Adapter] R1: el nombre de la base se le pregunta a la base misma. La
 * configuracion (yml, perfil) puede estar mal copiada; la conexion abierta no miente.
 */
@Component
@RequiredArgsConstructor
public class AmbienteMysqlAdapter implements AmbientePort {

    private final JdbcTemplate jdbc;

    @Override
    public String nombreDeLaBase() {
        String base = jdbc.queryForObject("SELECT DATABASE()", String.class);
        return base == null ? "(ninguna)" : base;
    }
}
