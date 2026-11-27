package com.ventas.key.hexagonal.whatsapp.dominio.modelo;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Optional;

/**
 * Horario en que hay una persona para atender las conversaciones (regla R12). Sale del horario del
 * negocio (Sistema → Negocio y contactos); si el negocio no tiene horario, no existe y el bot no da ninguno.
 *
 * <p>Lunes a viernes tienen su horario; sábado y domingo pueden tener otro. Si el de fin de semana no está
 * configurado (null), los fines de semana valen lo mismo que entre semana.</p>
 */
public record HorarioDeAtencion(LocalTime apertura, LocalTime cierre,
                                LocalTime aperturaFinSemana, LocalTime cierreFinSemana) {

    /** Mismo horario todos los días (como antes de que existiera el de fin de semana). */
    public HorarioDeAtencion(LocalTime apertura, LocalTime cierre) {
        this(apertura, cierre, null, null);
    }

    /** El horario del negocio es hora local de México (el servidor corre en esa zona y México ya no usa horario de verano). */
    public static final ZoneId ZONA_DEL_NEGOCIO = ZoneId.of("America/Mexico_City");

    /** Vacío si falta una de las dos horas o si son iguales (no se puede saber cuándo se atiende). */
    public static Optional<HorarioDeAtencion> de(LocalTime apertura, LocalTime cierre) {
        if (apertura == null || cierre == null || apertura.equals(cierre)) {
            return Optional.empty();
        }
        return Optional.of(new HorarioDeAtencion(apertura, cierre));
    }

    /**
     * Con horario propio de fin de semana. Si el de fin de semana está incompleto o es de duración cero, se ignora
     * (valen los de entre semana); lo que no se puede entender nunca cierra el negocio por accidente.
     */
    public static Optional<HorarioDeAtencion> de(LocalTime apertura, LocalTime cierre,
                                                 LocalTime aperturaFinSemana, LocalTime cierreFinSemana) {
        Optional<HorarioDeAtencion> base = de(apertura, cierre);
        if (base.isEmpty() || aperturaFinSemana == null || cierreFinSemana == null
                || aperturaFinSemana.equals(cierreFinSemana)) {
            return base;
        }
        return Optional.of(new HorarioDeAtencion(apertura, cierre, aperturaFinSemana, cierreFinSemana));
    }

    private boolean tieneFinDeSemanaPropio() {
        return aperturaFinSemana != null && cierreFinSemana != null;
    }

    /** Lo mismo, a partir de un instante (se convierte a la hora local del negocio). */
    public boolean abiertoEn(Instant instante) {
        java.time.ZonedDateTime local = instante.atZone(ZONA_DEL_NEGOCIO);
        DayOfWeek dia = local.getDayOfWeek();
        boolean finDeSemana = dia == DayOfWeek.SATURDAY || dia == DayOfWeek.SUNDAY;
        if (finDeSemana && tieneFinDeSemanaPropio()) {
            return dentro(local.toLocalTime(), aperturaFinSemana, cierreFinSemana);
        }
        return abiertoEn(local.toLocalTime());
    }

    /** Con el horario de entre semana (la hora sola no dice qué día es). */
    public boolean abiertoEn(LocalTime hora) {
        return dentro(hora, apertura, cierre);
    }

    /** La hora de apertura cuenta; la de cierre ya no. Si el cierre es antes que la apertura, cruza la medianoche. */
    private static boolean dentro(LocalTime hora, LocalTime desde, LocalTime hasta) {
        if (desde.isBefore(hasta)) {
            return !hora.isBefore(desde) && hora.isBefore(hasta);
        }
        return !hora.isBefore(desde) || hora.isBefore(hasta);
    }

    /**
     * Va después de «de»: «10:00 a.m. a 6:00 p.m.» si es el mismo todos los días, o
     * «10:00 a.m. a 6:00 p.m. de lunes a viernes, y de 10:00 a.m. a 2:00 p.m. sábados y domingos».
     */
    public String aTexto() {
        String semana = hora12(apertura) + " a " + hora12(cierre);
        if (!tieneFinDeSemanaPropio()
                || (aperturaFinSemana.equals(apertura) && cierreFinSemana.equals(cierre))) {
            return semana;
        }
        return semana + " de lunes a viernes, y de " + hora12(aperturaFinSemana) + " a " + hora12(cierreFinSemana)
                + " sábados y domingos";
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
