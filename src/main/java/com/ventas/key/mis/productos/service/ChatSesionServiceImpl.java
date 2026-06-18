package com.ventas.key.mis.productos.service;

import com.ventas.key.mis.productos.entity.ChatSesion;
import com.ventas.key.mis.productos.models.chat.ChatEventoUsuario;
import com.ventas.key.mis.productos.repository.IChatSesionRepository;
import com.ventas.key.mis.productos.service.api.IChatSesionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class ChatSesionServiceImpl implements IChatSesionService {

    private final IChatSesionRepository repository;
    private final SimpMessagingTemplate messagingTemplate;

    public ChatSesionServiceImpl(IChatSesionRepository repository, SimpMessagingTemplate messagingTemplate) {
        this.repository = repository;
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    @Transactional
    public String conectar(String ip, String nombreUsuario, Integer usuarioId) {
        String sesionId = UUID.randomUUID().toString();
        LocalDateTime ahora = LocalDateTime.now();
        ChatSesion sesion = ChatSesion.builder()
                .sesionId(sesionId)
                .usuarioId(usuarioId)
                .identificador(ip != null ? ip : "desconocido")
                .nombreUsuario(nombreUsuario != null && !nombreUsuario.isBlank() ? nombreUsuario : "Visitante")
                .estado("ACTIVA")
                .fechaInicio(ahora)
                .ultimaActividad(ahora)
                .build();
        repository.save(sesion);
        log.info("Nueva sesión de chat: {} - usuarioId={}", sesionId, usuarioId);
        return sesionId;
    }

    @Override
    @Transactional
    public void cerrarSesion(String sesionId) {
        repository.findBySesionId(sesionId).ifPresent(sesion -> {
            sesion.setEstado("CERRADA");
            repository.save(sesion);
            messagingTemplate.convertAndSend(
                "/topic/chat.usuario." + sesionId,
                ChatEventoUsuario.builder().tipo("SESION_CERRADA").build()
            );
            log.info("Sesión {} cerrada", sesionId);
        });
    }

    @Override
    @Transactional
    public void actualizarActividad(String sesionId) {
        repository.findBySesionId(sesionId).ifPresent(sesion -> {
            sesion.setUltimaActividad(LocalDateTime.now());
            repository.save(sesion);
        });
    }

    @Override
    public List<ChatSesion> obtenerSesionesActivas() {
        return repository.findByEstado("ACTIVA");
    }

    @Override
    public List<ChatSesion> obtenerSesionesRecientes() {
        return repository.findByUltimaActividadAfterOrderByUltimaActividadDesc(
            LocalDateTime.now().minusHours(24)
        );
    }

    @Override
    public Optional<ChatSesion> buscarSesionActiva(String sesionId) {
        return repository.findBySesionIdAndEstado(sesionId, "ACTIVA");
    }

    @Override
    public boolean existeSesion(String sesionId) {
        return repository.findBySesionId(sesionId).isPresent();
    }

    @Override
    @Transactional
    public void cerrarSesionesInactivas() {
        LocalDateTime limite = LocalDateTime.now().minusMinutes(5);
        List<ChatSesion> inactivas = repository.findByEstadoAndUltimaActividadBefore("ACTIVA", limite);
        for (ChatSesion sesion : inactivas) {
            sesion.setEstado("CERRADA");
            repository.save(sesion);
            messagingTemplate.convertAndSend(
                "/topic/chat.usuario." + sesion.getSesionId(),
                ChatEventoUsuario.builder().tipo("SESION_CERRADA").build()
            );
        }
        if (!inactivas.isEmpty()) {
            log.info("{} sesiones cerradas por inactividad (>30 min)", inactivas.size());
        }
    }
}
