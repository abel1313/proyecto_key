package com.ventas.key.mis.productos.scheduler;

import com.ventas.key.mis.productos.service.SesionRefreshService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Barre las sesiones de refresh ya vencidas. La mayoria de las sesiones nunca pasa por logout
 * (el usuario simplemente cierra el navegador), asi que sin esta limpieza la tabla
 * {@code sesion_refresh} solo creceria.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SesionRefreshScheduler {

    private final SesionRefreshService sesionRefreshService;

    @Scheduled(cron = "0 30 3 * * *")
    public void limpiarSesionesExpiradas() {
        int eliminadas = sesionRefreshService.limpiarExpiradas();
        if (eliminadas > 0) {
            log.info("Limpieza de sesiones de refresh expiradas: {} eliminadas", eliminadas);
        }
    }
}
