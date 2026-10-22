package com.ventas.key.hexagonal.pagotarjeta.infraestructura.entrada.programado;

import com.ventas.key.hexagonal.pagotarjeta.dominio.puerto.entrada.ExpirarCobrosVencidosCasoUso;
import com.ventas.key.hexagonal.pagotarjeta.dominio.puerto.entrada.ReconciliarCobrosCasoUso;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Trabajos automaticos del cobro con tarjeta (T7): vencer los cobros que nadie contesto y volver a preguntar por los
 * que se dieron por perdidos. Deben correr en UNA sola instancia (hoy el despliegue es de un pod; si se escala,
 * usar un candado como ShedLock). Un fallo nunca tumba el trabajo: se registra y se reintenta en la siguiente vuelta.
 *
 * <p>[Hexagonal: Driving Adapter] [Clean: Interface Adapter]
 */
@Component
@ConditionalOnProperty(name = "pagos.tarjeta.habilitado", havingValue = "true")
@Slf4j
@RequiredArgsConstructor
public class PagoTarjetaTareas {

    private final ExpirarCobrosVencidosCasoUso expirar;
    private final ReconciliarCobrosCasoUso reconciliar;

    @Scheduled(fixedDelayString = "${pagos.tarjeta.expirar-cada-ms:60000}", initialDelayString = "${pagos.tarjeta.expirar-cada-ms:60000}")
    public void expirarVencidos() {
        try {
            int n = expirar.expirarVencidos();
            if (n > 0) {
                log.info("Cobros con tarjeta resueltos por vencimiento: {}", n);
            }
        } catch (RuntimeException e) {
            log.error("Falló el trabajo de vencer cobros con tarjeta", e);
        }
    }

    @Scheduled(fixedDelayString = "${pagos.tarjeta.reconciliar-cada-ms:900000}", initialDelayString = "${pagos.tarjeta.reconciliar-cada-ms:900000}")
    public void reconciliar() {
        try {
            int n = reconciliar.reconciliar();
            if (n > 0) {
                log.warn("Cobros con tarjeta que se habían dado por perdidos y se cobraron: {}", n);
            }
        } catch (RuntimeException e) {
            log.error("Falló el trabajo de reconciliar cobros con tarjeta", e);
        }
    }
}
