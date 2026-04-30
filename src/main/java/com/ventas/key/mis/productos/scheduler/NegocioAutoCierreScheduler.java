package com.ventas.key.mis.productos.scheduler;

import com.ventas.key.mis.productos.service.NegocioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NegocioAutoCierreScheduler {

    private final NegocioService negocioService;

    /** Revisa cada minuto si se pasó la hora de auto-cierre */
    @Scheduled(cron = "0 * * * * *")
    public void verificarAutoCierre() {
        negocioService.verificarAutoCierre();
    }
}