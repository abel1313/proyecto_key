package com.ventas.key.mis.productos.scheduler;

import com.ventas.key.mis.productos.repository.IClienteSinRegistroRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class ClienteSinRegistroLimpiezaScheduler {

    // Margen de seguridad: nunca borrar un registro creado hace menos de estas horas, para no
    // tocar una venta que se esta capturando justo esa noche (ver POST /v1/clientes-sin-registro).
    private static final int HORAS_GRACIA = 6;

    private final IClienteSinRegistroRepository iClienteSinRegistroRepository;

    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void limpiarHuerfanos() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(HORAS_GRACIA);
        int eliminados = iClienteSinRegistroRepository.eliminarHuerfanosAntesDe(cutoff);
        if (eliminados > 0) {
            log.info("Limpieza de clientes sin registro huerfanos: {} eliminados (creados antes de {})",
                    eliminados, cutoff);
        }
    }
}
