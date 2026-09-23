package com.ventas.key.hexagonal.rifa.dominio.modelo;

import java.util.Locale;
import java.util.Objects;

/**
 * El cliente identificado en una red: (plataforma + URL de su perfil). Es la clave de
 * agrupamiento de la regla R2, y no se repite dentro de una rifa.
 *
 * <p>Guarda la URL tal como la escribio el admin para mostrarla, pero compara por
 * {@link #clave()}, que es la forma normalizada. Sin eso
 * {@code facebook.com/juan} y {@code https://facebook.com/juan/} serian dos personas
 * distintas y el agrupamiento no serviria de nada.</p>
 *
 * <p>[Hexagonal: Domain Model] [Clean: Entity]</p>
 */
public record PerfilEnRed(Plataforma plataforma, String urlPerfil) {

    public PerfilEnRed {
        Objects.requireNonNull(plataforma, "La plataforma es obligatoria");
    }

    /**
     * La forma comparable del perfil. Lo que se ignora al comparar: mayusculas, el
     * esquema ({@code http://} / {@code https://}), el {@code www.}, la barra final y
     * los espacios de los costados.
     */
    public String clave() {
        return plataforma.name() + "|" + normalizar(urlPerfil);
    }

    public boolean mismoQue(PerfilEnRed otro) {
        return otro != null && clave().equals(otro.clave());
    }

    public boolean tieneUrl() {
        return urlPerfil != null && !urlPerfil.isBlank();
    }

    /** Se expone para que el puerto de salida pueda buscar por la misma forma normalizada. */
    public static String normalizar(String url) {
        if (url == null) {
            return "";
        }
        String limpia = url.trim().toLowerCase(Locale.ROOT);
        limpia = limpia.replaceFirst("^https?://", "");
        limpia = limpia.replaceFirst("^www\\.", "");
        while (limpia.endsWith("/")) {
            limpia = limpia.substring(0, limpia.length() - 1);
        }
        return limpia;
    }
}
