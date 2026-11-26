package com.ventas.key.mis.productos.repository;

import com.ventas.key.mis.productos.entity.WhatsappClienteEntidad;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IWhatsappClienteRepository extends JpaRepository<WhatsappClienteEntidad, Long> {

    Optional<WhatsappClienteEntidad> findByTelefono(String telefono);

    // Ordena por id como desempate: la columna es DATETIME (segundos) y varias conversaciones pueden empatar.
    Page<WhatsappClienteEntidad> findAllByOrderByUltimaActividadAtDescIdDesc(Pageable pageable);
}
