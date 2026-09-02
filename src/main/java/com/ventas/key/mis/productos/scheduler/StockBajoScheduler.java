package com.ventas.key.mis.productos.scheduler;

import com.ventas.key.mis.productos.service.StockBajoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class StockBajoScheduler {

    private final StockBajoService stockBajoService;

    // 7 AM -- no necesita autenticarse contra ningun microservicio externo (a diferencia de
    // ImagenScheduler), es puro read de BD + envio de correo, asi que no hace falta el patron
    // de autenticarYEjecutar de ese scheduler.
    @Scheduled(cron = "0 0 7 * * *")
    public void verificarStockBajo() {
        log.info("Iniciando verificacion diaria de stock bajo (7 AM)");
        try {
            stockBajoService.verificarYNotificar();
        } catch (Exception e) {
            log.error("Error en verificacion diaria de stock bajo: {}", e.getMessage());
        }
    }
}
