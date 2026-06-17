package com.ventas.key.mis.productos.repository;

import com.ventas.key.mis.productos.entity.ChatMensaje;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface IChatMensajeRepository extends JpaRepository<ChatMensaje, Long> {

    List<ChatMensaje> findBySesionIdOrderByTimestampAsc(String sesionId);

    Page<ChatMensaje> findBySesionIdOrderByTimestampDesc(String sesionId, Pageable pageable);

    Optional<ChatMensaje> findTop1BySesionIdOrderByTimestampDesc(String sesionId);
}
