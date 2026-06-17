package com.ventas.key.mis.productos.config;

import com.ventas.key.mis.productos.service.api.IChatNotificacionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
@Slf4j
public class ChatWebSocketEventListener {

    private final IChatNotificacionService notificacionService;

    public ChatWebSocketEventListener(IChatNotificacionService notificacionService) {
        this.notificacionService = notificacionService;
    }

    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();
        if (notificacionService.isAdminSession(sessionId)) {
            notificacionService.marcarAdminDesconectado();
            log.info("Admin del chat desconectado (ws={})", sessionId);
        }
    }
}
