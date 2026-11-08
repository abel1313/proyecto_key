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
        try {
            if (!email.enviarCodigoCampana(persona.correo(), persona.nombre(), nombreCampana, persona.uuid())) {
                log.warn("No se pudo mandar el código de campaña a la persona {}", persona.id());
            }
        } catch (RuntimeException e) {
            log.warn("No se pudo mandar el código de campaña a la persona {}: {}", persona.id(), e.getMessage());
        }
    }
}
