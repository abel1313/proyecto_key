package com.ventas.key.hexagonal.reportecategoria.infraestructura.config;

import com.ventas.key.mis.productos.repository.IReporteGeneradoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/** Borra cada noche los reportes generados que ya vencieron (a los 7 días): los archivos pesan. */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReporteLimpiezaScheduler {

    private final IReporteGeneradoRepository repo;

    @Scheduled(cron = "0 30 3 * * *")
    @Transactional
    public void borrarVencidos() {
        int n = repo.borrarVencidos(LocalDateTime.now());
        if (n > 0) log.info("Reportes generados vencidos borrados: {}", n);
    }
}
