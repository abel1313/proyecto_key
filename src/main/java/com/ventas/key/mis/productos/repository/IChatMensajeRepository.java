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

    @org.springframework.data.jpa.repository.Query(
        value = "SELECT m FROM ChatMensaje m WHERE m.sesionId IN " +
                "(SELECT s.sesionId FROM ChatSesion s WHERE s.clienteId = :clienteId) " +
                "ORDER BY m.timestamp DESC",
        countQuery = "SELECT COUNT(m) FROM ChatMensaje m WHERE m.sesionId IN " +
                     "(SELECT s.sesionId FROM ChatSesion s WHERE s.clienteId = :clienteId)"
    )
    Page<ChatMensaje> findByClienteIdOrderByTimestampDesc(@org.springframework.data.repository.query.Param("clienteId") String clienteId, Pageable pageable);

    @org.springframework.data.jpa.repository.Query(
        value = "SELECT m FROM ChatMensaje m WHERE m.sesionId IN " +
                "(SELECT s.sesionId FROM ChatSesion s WHERE s.usuarioId = :usuarioId) " +
                "ORDER BY m.timestamp DESC",
        countQuery = "SELECT COUNT(m) FROM ChatMensaje m WHERE m.sesionId IN " +
                     "(SELECT s.sesionId FROM ChatSesion s WHERE s.usuarioId = :usuarioId)"
    )
    Page<ChatMensaje> findByUsuarioIdOrderByTimestampDesc(@org.springframework.data.repository.query.Param("usuarioId") Integer usuarioId, Pageable pageable);

    Optional<ChatMensaje> findTop1BySesionIdOrderByTimestampDesc(String sesionId);
}
