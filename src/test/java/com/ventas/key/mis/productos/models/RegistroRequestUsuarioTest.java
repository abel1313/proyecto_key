package com.ventas.key.mis.productos.models;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RegistroRequestUsuarioTest {

    private final Validator validador = Validation.buildDefaultValidatorFactory().getValidator();

    private boolean usuarioValido(String nombre) {
        RegistroRequest r = new RegistroRequest();
        r.setUserName(nombre);
        return validador.validateProperty(r, "userName").isEmpty();
    }

    @Test
    @DisplayName("el usuario no puede llevar signos de HTML, pero sí acentos, espacios y puntos")
    void sinSignosDeHtml() {
        assertThat(usuarioValido("<img src=x onerror=alert(1)>")).isFalse();
        assertThat(usuarioValido("ana\"o")).isFalse();
        assertThat(usuarioValido("a&b")).isFalse();
        assertThat(usuarioValido("María José.perez_99")).isTrue();
        assertThat(usuarioValido("ana@correo.com")).isTrue();
    }
}
