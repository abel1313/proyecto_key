package com.ventas.key.mis.productos.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * El bean {@code PasswordEncoder} vivia dentro de {@link SecurityConfig}, que tambien autowirea
 * {@code JwtAuthenticationFilter} -- y ese filtro autowirea {@code UsuarioServiceImpl}, cuyo
 * constructor pide {@code PasswordEncoder}. Para crear el bean {@code passwordEncoder} Spring
 * necesita primero terminar de construir {@code SecurityConfig} por completo (inyectar su campo
 * {@code jwtFilter}), lo que dispara la construccion de {@code UsuarioServiceImpl}, que vuelve a
 * pedir {@code PasswordEncoder} -- ciclo. Como {@code UsuarioServiceImpl} lo pide por constructor
 * (no por campo), Spring no puede resolverlo con una referencia temprana y el arranque truena con
 * "Requested bean is currently in creation" (encontrado 2026-08-27, CrashLoopBackOff en QA).
 *
 * <p>Sacar este bean a una clase de configuracion propia, sin ninguna otra dependencia, rompe el
 * ciclo de raiz: ya no hace falta construir {@code SecurityConfig} (ni por lo tanto
 * {@code JwtAuthenticationFilter}) para poder crear el {@code PasswordEncoder}. El bean sigue
 * siendo el mismo singleton de siempre para todo el que lo use (Spring lo resuelve por tipo, no
 * por la clase donde este declarado) -- este cambio no altera ningun comportamiento, solo el
 * orden en que Spring puede construir los beans.
 */
@Configuration
public class PasswordEncoderConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
