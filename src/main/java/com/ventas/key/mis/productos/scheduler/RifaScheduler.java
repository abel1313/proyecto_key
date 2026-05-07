package com.ventas.key.mis.productos.scheduler;

import com.ventas.key.mis.productos.service.ConfiguracionRifaServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RifaScheduler {

    private final ConfiguracionRifaServiceImpl configuracionRifaService;

    @Scheduled(cron = "0 0 2 * * *")
    public void desactivarRifasVencidas() {
        log.info("Revisando rifas activas vencidas (2 AM)");
        configuracionRifaService.desactivarVencidas();
    }
}