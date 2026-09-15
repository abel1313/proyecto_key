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

    // Un usuario = UNA conversacion. Antes cada reconexion creaba una sesion nueva, asi que el mismo
    // cliente salia como varias conversaciones distintas en el panel y su historial quedaba partido
    // en pedazos (reportado 2026-09-15: "si mando un mensaje y despues otro rato mando otro, se ven
    // 2 conversaciones"). Ahora se reusa la conversacion que ya tenia y todo queda en un solo hilo.
    @Override
    @Transactional
    public String conectar(String ip, String nombreUsuario, Integer usuarioId) {
        LocalDateTime ahoraReuso = LocalDateTime.now();
        if (usuarioId != null) {
            Optional<ChatSesion> previa = repository
                    .findTop1ByUsuarioIdAndEstadoNotOrderByUltimaActividadDesc(usuarioId, ESTADO_BOT);
            if (previa.isPresent()) {
                ChatSesion vigente = previa.get();
                if (!"ACTIVA".equals(vigente.getEstado())) {
                    // Volver despues del silencio es una conversacion nueva dentro del mismo hilo:
                    // se reabre y retoma el bot.
                    vigente.setEstado("ACTIVA");
                    vigente.setModo(MODO_BOT);
                }
                if (nombreUsuario != null && !nombreUsuario.isBlank()) {
                    vigente.setNombreUsuario(nombreUsuario);
                }
                if (ip != null) vigente.setIdentificador(ip);
                vigente.setUltimaActividad(ahoraReuso);
                repository.save(vigente);
                log.info("Chat: se reusa la conversación {} del usuario {}", vigente.getSesionId(), usuarioId);
                return vigente.getSesionId();
            }
        }

        String sesionId = UUID.randomUUID().toString();
        LocalDateTime ahora = LocalDateTime.now();
        ChatSesion sesion = ChatSesion.builder()
                .sesionId(sesionId)
                .usuarioId(usuarioId)
                .identificador(ip != null ? ip : "desconocido")
                .nombreUsuario(nombreUsuario != null && !nombreUsuario.isBlank() ? nombreUsuario : "Visitante")
                .estado("ACTIVA")
                .modo(MODO_BOT)
                .fechaInicio(ahora)
                .ultimaActividad(ahora)
                .build();
        repository.save(sesion);
        log.info("Nueva sesión de chat: {} - usuarioId={}", sesionId, usuarioId);
        return sesionId;
    }

    // Estado propio para las conversaciones del chatbot. No es "ACTIVA" a proposito: asi el
    // scheduler de inactividad y la lista de sesiones activas (las que esperan a un humano) no
    // las tocan, pero siguen saliendo en la lista de sesiones recientes del admin, que no filtra
    // por estado -- que es justo lo que se pidio: poder leerlas desde chat directo.
    public static final String ESTADO_BOT = "BOT";


    @Override
    @Transactional
    public String asegurarSesionBot(String sesionId, String ip) {
        if (sesionId != null && !sesionId.isBlank()) {
            Optional<ChatSesion> existente = repository.findBySesionId(sesionId);
            if (existente.isPresent()) {
                ChatSesion sesion = existente.get();
                sesion.setUltimaActividad(LocalDateTime.now());
                repository.save(sesion);
                return sesion.getSesionId();
            }
        }
        LocalDateTime ahora = LocalDateTime.now();
        String nuevo = UUID.randomUUID().toString();
        repository.save(ChatSesion.builder()
                .sesionId(nuevo)
                .identificador(ip != null ? ip : "desconocido")
                .nombreUsuario("Visitante (chatbot)")
                .estado(ESTADO_BOT)
                .fechaInicio(ahora)
                .ultimaActividad(ahora)
                .build());
        log.info("Nueva sesión de chatbot: {}", nuevo);
        return nuevo;
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

    // Hasta donde atras se listan las conversaciones en el panel del admin. Eran 24 horas, que con
    // una conversacion por usuario dejaba fuera a cualquiera que no hubiera escrito ese dia -- y lo
    // que se pidio es poder volver a revisar la conversacion despues.
    private static final int DIAS_LISTADO_ADMIN = 30;

    @Override
    public List<ChatSesion> obtenerSesionesRecientes() {
        return repository.findByUltimaActividadAfterOrderByUltimaActividadDesc(
            LocalDateTime.now().minusDays(DIAS_LISTADO_ADMIN)
        );
    }

    @Override
    public Optional<ChatSesion> buscarSesionActiva(String sesionId) {
        return repository.findBySesionIdAndEstado(sesionId, "ACTIVA");
    }

    // El scheduler cierra la sesion a los 5 minutos de silencio. Si el cliente vuelve a escribir en
    // esa pestana, la sesion ya esta CERRADA y el mensaje se perdia sin que nadie se enterara. Aqui
    // se vuelve a abrir en lugar de descartar: el historial es el mismo y el admin lo recibe igual.
    // Las sesiones del chatbot (estado BOT) se quedan fuera: no son del chat en vivo.
    @Override
    @Transactional
    public Optional<ChatSesion> reactivarSesion(String sesionId) {
        if (sesionId == null || sesionId.isBlank()) return Optional.empty();
        return repository.findBySesionId(sesionId)
                .filter(sesion -> !ESTADO_BOT.equals(sesion.getEstado()))
                .map(sesion -> {
                    if (!"ACTIVA".equals(sesion.getEstado())) {
                        // Volver despues del silencio es una conversacion nueva: retoma el bot.
                        // Es el "si el cliente deja de contestar, vuelve el bot" -- no hace falta
                        // otro temporizador, el cierre por inactividad ya marca ese corte.
                        log.info("Sesión {} reabierta: el cliente volvió a escribir, retoma el bot", sesionId);
                        sesion.setEstado("ACTIVA");
                        sesion.setModo(MODO_BOT);
                    }
                    sesion.setUltimaActividad(LocalDateTime.now());
                    return repository.save(sesion);
                });
    }

    @Override
    public String modoDe(String sesionId) {
        return repository.findBySesionId(sesionId)
                .map(ChatSesion::getModo)
                .filter(m -> m != null && !m.isBlank())
                .orElse(MODO_BOT);
    }

    @Override
    @Transactional
    public void cambiarModo(String sesionId, String modo) {
        repository.findBySesionId(sesionId).ifPresent(sesion -> {
            if (modo.equals(sesion.getModo())) return;
            sesion.setModo(modo);
            repository.save(sesion);
            log.info("Sesión {} pasa a modo {}", sesionId, modo);
        });
    }

    @Override
    public boolean existeSesion(String sesionId) {
        return repository.findBySesionId(sesionId).isPresent();
    }

    // Minutos sin actividad tras los que una sesion se marca CERRADA. El log decia "(>30 min)"
    // pero el corte siempre fue de 5 -- se corrigio el 2026-09-15 para que lo que se ve en los
    // logs sea lo que de verdad pasa.
    private static final int MINUTOS_INACTIVIDAD = 5;

    @Override
    @Transactional
    public void cerrarSesionesInactivas() {
        // Cerrar NO borra nada: la sesion pasa a CERRADA y sus mensajes siguen en chat_mensaje.
        // El historial completo se conserva, no hay ninguna tarea que lo purgue.
        LocalDateTime limite = LocalDateTime.now().minusMinutes(MINUTOS_INACTIVIDAD);
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
            log.info("{} sesiones cerradas por inactividad (>{} min)", inactivas.size(), MINUTOS_INACTIVIDAD);
        }
    }
}
