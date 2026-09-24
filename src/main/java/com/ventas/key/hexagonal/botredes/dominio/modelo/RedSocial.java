package com.ventas.key.hexagonal.botredes.dominio.modelo;

/** Red donde ocurre la interacción. {@code codigo} es el valor de la columna {@code red_social}. */
public enum RedSocial {
    FACEBOOK("facebook"),
    INSTAGRAM("instagram");

    private final String codigo;

    RedSocial(String codigo) {
        this.codigo = codigo;
    }

    public String codigo() {
        return codigo;
    }
}
