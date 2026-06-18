package com.ventas.key.mis.productos.repository;

import com.ventas.key.mis.productos.entity.ChatSesion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface IChatSesionRepository extends JpaRepository<ChatSesion, Long> {

    Optional<ChatSesion> findBySesionId(String sesionId);

    Optional<ChatSesion> findBySesionIdAndEstado(String sesionId, String estado);

    List<ChatSesion> findByEstado(String estado);

    List<ChatSesion> findByEstadoAndUltimaActividadBefore(String estado, LocalDateTime fecha);

    List<ChatSesion> findByUltimaActividadAfterOrderByUltimaActividadDesc(LocalDateTime desde);

    List<ChatSesion> findByUsuarioIdOrderByUltimaActividadDesc(Integer usuarioId);
}
