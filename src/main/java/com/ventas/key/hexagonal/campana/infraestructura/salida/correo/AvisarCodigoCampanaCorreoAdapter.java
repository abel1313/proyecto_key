package com.ventas.key.hexagonal.campana.infraestructura.salida.correo;

import com.ventas.key.hexagonal.campana.dominio.modelo.PersonaCampana;
import com.ventas.key.hexagonal.campana.dominio.puerto.salida.AvisarCodigoCampanaPort;
import com.ventas.key.mis.productos.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** [Hexagonal: Driven Adapter] Manda el código de campaña con el {@link EmailService} de siempre. Un fallo se registra y no se propaga. */
@Component
public class AvisarCodigoCampanaCorreoAdapter implements AvisarCodigoCampanaPort {

    private static final Logger log = LoggerFactory.getLogger(AvisarCodigoCampanaCorreoAdapter.class);

    private final EmailService email;

    public AvisarCodigoCampanaCorreoAdapter(EmailService email) {
        this.email = email;
    }

    @Override
    public void enviar(PersonaCampana persona, String nombreCampana) {
        if (persona.correo() == null) {
            return;
        }
        // Dentro de una venta, el correo sale hasta que la venta se confirma: si se revierte, no sale un código de una persona que no se guardó.
        if (org.springframework.transaction.support.TransactionSynchronizationManager.isSynchronizationActive()) {
            org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                    new org.springframework.transaction.support.TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            mandar(persona, nombreCampana);
                        }
                    });
            return;
        }
        mandar(persona, nombreCampana);
    }

    private void mandar(PersonaCampana persona, String nombreCampana) {
        try {
            if (!email.enviarCodigoCampana(persona.correo(), persona.nombre(), nombreCampana, persona.uuid())) {
                log.warn("No se pudo mandar el código de campaña a la persona {}", persona.id());
            }
        } catch (RuntimeException e) {
            log.warn("No se pudo mandar el código de campaña a la persona {}: {}", persona.id(), e.getMessage());
        }
    }
}
