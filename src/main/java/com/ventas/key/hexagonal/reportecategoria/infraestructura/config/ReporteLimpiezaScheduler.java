package com.ventas.key.hexagonal.reportecategoria.infraestructura.config;

import com.ventas.key.mis.productos.repository.IReporteGeneradoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Borra cada noche los reportes generados que ya vencieron (a los 7 días): los archivos pesan. Y cada pocos minutos pasa a ERROR los que
 * se quedaron PENDIENTE/GENERANDO más del límite (el pod se reinició a media tarea o la tarea se perdió): sin eso cuentan para el tope de 3
 * en curso del usuario hasta que vencen, 7 días después.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReporteLimpiezaScheduler {

    /**
     * Cuánto puede estar un reporte PENDIENTE/GENERANDO antes de darlo por atorado. Un reporte grande se arma en segundos o pocos minutos
     * y la cola espera como mucho 10 reportes detrás de 2 hilos; 15 minutos desde que se pidió deja margen de sobra a uno lento pero vivo.
     */
    public static final int MINUTOS_MAXIMOS_EN_CURSO = 15;
    public static final String MENSAJE_ATORADO = "El reporte se interrumpió (el servidor se reinició o tardó demasiado). Vuelve a pedirlo.";

    private final IReporteGeneradoRepository repo;

    @Scheduled(cron = "0 30 3 * * *")
    @Transactional
    public void borrarVencidos() {
        int n = repo.borrarVencidos(LocalDateTime.now());
        if (n > 0) log.info("Reportes generados vencidos borrados: {}", n);
    }

    @Scheduled(initialDelay = 60_000, fixedDelay = 300_000)
    @Transactional
    public void marcarAtorados() {
        LocalDateTime ahora = LocalDateTime.now();
        int n = repo.marcarErrorLosAtorados(ahora.minusMinutes(MINUTOS_MAXIMOS_EN_CURSO), MENSAJE_ATORADO, ahora);
        if (n > 0) log.warn("Reportes atorados (más de {} min en PENDIENTE/GENERANDO) pasados a ERROR: {}", MINUTOS_MAXIMOS_EN_CURSO, n);
    }
}
