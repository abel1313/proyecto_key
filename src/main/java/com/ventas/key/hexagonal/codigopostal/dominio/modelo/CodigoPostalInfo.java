package com.ventas.key.hexagonal.codigopostal.dominio.modelo;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * [Hexagonal: dentro del hexágono] [Clean: Entities]
 *
 * <p>Lo que se sabe de un código postal: municipio, estado y colonias. Java puro.
 *
 * <p>R7: los códigos casi no cambian; uno guardado hace más de un año se vuelve a consultar.
 */
public record CodigoPostalInfo(String codigo, String municipio, String estado,
                               List<String> colonias, LocalDateTime consultado) {

    public static final Duration VIGENCIA = Duration.ofDays(365);

    public CodigoPostalInfo {
        if (!esValido(codigo)) {
            throw new IllegalArgumentException("El código postal debe tener 5 dígitos");
        }
        if (municipio == null || municipio.isBlank()) {
            throw new IllegalArgumentException("Un código postal sin municipio no sirve");
        }
        colonias = colonias == null ? List.of() : List.copyOf(colonias);
    }

    public static boolean esValido(String codigo) {
        return codigo != null && codigo.matches("\\d{5}");
    }

    /** R7: pasó más de un año desde que se consultó al servicio externo. */
    public boolean caducado(LocalDateTime ahora) {
        return consultado == null || consultado.plus(VIGENCIA).isBefore(ahora);
    }
}
