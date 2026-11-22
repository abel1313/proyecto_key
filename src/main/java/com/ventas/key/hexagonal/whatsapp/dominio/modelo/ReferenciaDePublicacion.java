package com.ventas.key.hexagonal.whatsapp.dominio.modelo;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * [Hexagonal: dentro del hexagono] [Clean: Entities]
 *
 * <p>El codigo de una publicacion (J42) dentro de un texto. Es la unica forma confiable de saber de que publicacion hablan: se busca como
 * palabra completa ("J42", "j42", "info j42"), nunca dentro de otra palabra ("J42x", "MAJ42").
 */
public final class ReferenciaDePublicacion {

    private static final Pattern CODIGO = Pattern.compile("(?<![\\p{L}\\p{N}])[Jj](\\d{1,9})(?![\\p{L}\\p{N}])");

    private ReferenciaDePublicacion() {
    }

    /** El primer codigo que aparezca en los textos, en orden (primero el mensaje, luego la cita), ya normalizado a mayuscula (J42). */
    public static Optional<String> en(String... textos) {
        for (String t : textos) {
            if (t == null) {
                continue;
            }
            Matcher m = CODIGO.matcher(t);
            if (m.find()) {
                return Optional.of(PublicacionWhatsapp.PREFIJO_CODIGO + m.group(1));
            }
        }
        return Optional.empty();
    }

    public static Optional<String> normalizar(String codigo) {
        return codigo == null ? Optional.empty() : en(codigo.trim());
    }
}
