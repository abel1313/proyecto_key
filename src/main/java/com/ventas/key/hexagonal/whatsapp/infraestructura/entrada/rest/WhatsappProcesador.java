package com.ventas.key.hexagonal.whatsapp.infraestructura.entrada.rest;

import com.ventas.key.hexagonal.whatsapp.dominio.modelo.EstadoDeEntrega;
import com.ventas.key.hexagonal.whatsapp.dominio.modelo.MensajeEntrante;
import com.ventas.key.hexagonal.whatsapp.dominio.puerto.entrada.RecibirMensajeWhatsappCasoUso;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Procesa el payload FUERA del hilo del webhook: el controlador responde 200 a Meta de inmediato y
 * esto corre aparte (el chatbot y la API de Meta tardan segundos). Los mensajes de un mismo número se
 * procesan de uno en uno (un candado por número), para que dos mensajes seguidos no hagan que el bot
 * conteste dos veces a la vez ni fuera de orden. Alcanza con un solo pod, igual que {@code botredes}.
 */
@Component
class WhatsappProcesador {

    private static final Logger log = LoggerFactory.getLogger(WhatsappProcesador.class);
    private static final int CANDADOS = 64;

    private final Executor ejecutor;
    private final RecibirMensajeWhatsappCasoUso casoUso;
    private final ReentrantLock[] candados = new ReentrantLock[CANDADOS];

    WhatsappProcesador(@Qualifier("whatsappExecutor") Executor ejecutor, RecibirMensajeWhatsappCasoUso casoUso) {
        this.ejecutor = ejecutor;
        this.casoUso = casoUso;
        for (int i = 0; i < CANDADOS; i++) {
            candados[i] = new ReentrantLock();
        }
    }

    /** {@code false} si la cola está llena: el controlador responde 503 y Meta reintenta más tarde. */
    boolean encolar(PayloadWhatsapp payload) {
        try {
            ejecutor.execute(() -> procesar(payload));
            return true;
        } catch (RejectedExecutionException e) {
            log.warn("WhatsApp: la cola de procesamiento está llena, se pide a Meta que reintente");
            return false;
        }
    }

    void procesar(PayloadWhatsapp payload) {
        for (EstadoDeEntrega estado : payload.estados()) {
            try {
                casoUso.registrarEstadoDeEntrega(estado);
            } catch (Exception e) {
                log.error("WhatsApp: error registrando el estado de {}", estado.waMessageId(), e);
            }
        }
        for (MensajeEntrante mensaje : payload.mensajes()) {
            ReentrantLock candado = candados[Math.floorMod(String.valueOf(mensaje.telefono()).hashCode(), CANDADOS)];
            candado.lock();
            try {
                casoUso.recibir(mensaje);
            } catch (Exception e) {
                // El mensaje ya se guardó antes de contestar; un fallo aquí no debe perder los demás del lote.
                log.error("WhatsApp: error procesando el mensaje {}", mensaje.waMessageId(), e);
            } finally {
                candado.unlock();
            }
        }
    }
}
