package com.ventas.key.hexagonal.whatsapp.dominio.modelo;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Optional;

/**
 * Horario en que hay una persona para atender las conversaciones (regla R12). Sale del horario del
 * negocio (Sistema → Negocio y contactos); si el negocio no tiene horario, no existe y el bot no da ninguno.
 *
 * <p>Es el mismo horario todos los días: el negocio solo guarda hora de apertura y de cierre.</p>
 */
public record HorarioDeAtencion(LocalTime apertura, LocalTime cierre) {

    /** El horario del negocio es hora local de México (el servidor corre en esa zona y México ya no usa horario de verano). */
    public static final ZoneId ZONA_DEL_NEGOCIO = ZoneId.of("America/Mexico_City");

    /** Vacío si falta una de las dos horas o si son iguales (no se puede saber cuándo se atiende). */
    public static Optional<HorarioDeAtencion> de(LocalTime apertura, LocalTime cierre) {
        if (apertura == null || cierre == null || apertura.equals(cierre)) {
            return Optional.empty();
        }
        return Optional.of(new HorarioDeAtencion(apertura, cierre));
    }

    /** Lo mismo, a partir de un instante (se convierte a la hora local del negocio). */
    public boolean abiertoEn(Instant instante) {
        return abiertoEn(LocalTime.ofInstant(instante, ZONA_DEL_NEGOCIO));
    }

    /** La hora de apertura cuenta; la de cierre ya no. Si el cierre es antes que la apertura, cruza la medianoche. */
    public boolean abiertoEn(LocalTime hora) {
        if (apertura.isBefore(cierre)) {
            return !hora.isBefore(apertura) && hora.isBefore(cierre);
        }
        return !hora.isBefore(apertura) || hora.isBefore(cierre);
    }

    /** «10:00 a.m. a 6:00 p.m.», como se lo diría una persona. */
    public String aTexto() {
        return hora12(apertura) + " a " + hora12(cierre);
    }

    /** Solo la hora de apertura, «10:00 a.m.». */
    public String apertura12() {
        return hora12(apertura);
    }

    static String hora12(LocalTime t) {
        int h = t.getHour();
        int h12 = h % 12 == 0 ? 12 : h % 12;
        return String.format("%d:%02d %s", h12, t.getMinute(), h < 12 ? "a.m." : "p.m.");
    }
}
