package com.ventas.key.mis.productos.repository;

import com.ventas.key.mis.productos.entity.CampanaPersonaEntidad;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ICampanaPersonaRepository extends JpaRepository<CampanaPersonaEntidad, Integer> {

    Optional<CampanaPersonaEntidad> findByUuid(String uuid);

    Optional<CampanaPersonaEntidad> findFirstByCorreoIgnoreCase(String correo);

    Optional<CampanaPersonaEntidad> findFirstByTelefono(String telefono10);
}
