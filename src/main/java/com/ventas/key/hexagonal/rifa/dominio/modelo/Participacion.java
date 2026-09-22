package com.ventas.key.hexagonal.rifa.dominio.modelo;

import java.time.LocalDate;

/**
 * Una cosa que el cliente hizo en la red (dio like, compartio, comento), identificada por
 * la URL de esa accion.
 *
 * <p><b>Una participacion es un boleto</b> (regla R1) y, por la decision D4, es tambien
 * <b>una fila</b> en {@code boletos_rifa}: el sorteo elige filas al azar, asi que agrupar
 * varias participaciones en una sola fila le quitaria chances a la persona.</p>
 *
 * <p>[Hexagonal: Domain Model] [Clean: Entity]</p>
 */
public record Participacion(Integer boletoId, String urlParticipacion, String motivo, LocalDate fecha) {

    public static Participacion nueva(String urlParticipacion, String motivo) {
        return new Participacion(null, urlParticipacion, motivo, LocalDate.now());
    }

    public boolean yaExiste() {
        return boletoId != null;
    }

    public boolean tieneUrl() {
        return urlParticipacion != null && !urlParticipacion.isBlank();
    }

    /** La forma comparable de la URL, para detectar duplicados igual que con el perfil. */
    public String claveUrl() {
        return PerfilEnRed.normalizar(urlParticipacion);
    }

    public boolean mismaUrlQue(Participacion otra) {
        return otra != null && claveUrl().equals(otra.claveUrl());
    }
}
