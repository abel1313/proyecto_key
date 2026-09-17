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

    // Ordena por id, NO por timestamp: la columna es DATETIME (segundos), asi que varios mensajes
    // del mismo segundo empatan y MySQL devuelve uno arbitrario. El id es autoincremental, o sea el
    // orden real de llegada. Con timestamp, el bot que comprueba "sigo siendo el ultimo mensaje?"
    // podia recibir un mensaje distinto al suyo y quedarse callado sin contestarle al cliente.
    Optional<ChatMensaje> findTop1BySesionIdOrderByIdDesc(String sesionId);

    // Mensajes del cliente que nadie contesto todavia: los que no tienen ninguna respuesta
    // (ADMIN o BOT) despues. Es lo que alimenta el globito de no-leidos del panel del admin,
    // que antes arrancaba siempre en 0 y escondia los mensajes que llegaron con el panel cerrado.
    @org.springframework.data.jpa.repository.Query(
        "SELECT COUNT(m) FROM ChatMensaje m WHERE m.sesionId = :sesionId AND m.remitente = 'USUARIO' " +
        "AND (SELECT COUNT(r) FROM ChatMensaje r WHERE r.sesionId = :sesionId " +
        "     AND r.remitente <> 'USUARIO' AND r.timestamp > m.timestamp) = 0"
    )
    long contarSinResponder(@org.springframework.data.repository.query.Param("sesionId") String sesionId);
}
