package com.ventas.key.mis.productos.scheduler;

import com.ventas.key.mis.productos.service.api.IChatSesionService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ChatSesionScheduler {

    private final IChatSesionService sesionService;

    public ChatSesionScheduler(IChatSesionService sesionService) {
        this.sesionService = sesionService;
    }

    @Scheduled(fixedDelay = 300_000)
    public void cerrarSesionesInactivas() {
        sesionService.cerrarSesionesInactivas();
    }
}
