package com.ventas.key.mis.productos.security;

import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.ventas.key.mis.productos.filter.JwtAuthenticationFilter;

@Configuration
@EnableMethodSecurity
@Slf4j
public class SecurityConfig {

    @Value("${api.cors_angular}")
    private String corsAngular;

    @Autowired
    private JwtAuthenticationFilter jwtFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        log.info("SecurityConfig cargado, cors: {}", corsAngular);
        return http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth

                        // ── Públicos ─────────────────────────────────────────────────────
                        .requestMatchers(
                                "/auth/login", "/auth/registrar", "/auth/refresh",
                                "/auth/logout", "/auth/validar",
                                "/swagger-ui/**", "/dipomex/**",
                                "/mp/webhook"
                        ).permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // ── Productos ─────────────────────────────────────────────────────
                        .requestMatchers(HttpMethod.GET,    "/productos/**").permitAll()
                        .requestMatchers(HttpMethod.POST,   "/productos/**").hasAuthority("PRODUCTOS_CREAR")
                        .requestMatchers(HttpMethod.PUT,    "/productos/**").hasAuthority("PRODUCTOS_EDITAR")
                        .requestMatchers(HttpMethod.DELETE, "/productos/**").hasAuthority("PRODUCTOS_ELIMINAR")

                        // ── Variantes ─────────────────────────────────────────────────────
                        .requestMatchers("/variantes/buscar").permitAll()
                        .requestMatchers(HttpMethod.GET,    "/variantes/**").hasAuthority("VARIANTES_LEER")
                        .requestMatchers(HttpMethod.POST,   "/variantes/**").hasAuthority("VARIANTES_CREAR")
                        .requestMatchers(HttpMethod.PUT,    "/variantes/**").hasAuthority("VARIANTES_EDITAR")

                        // ── Pedidos ───────────────────────────────────────────────────────
                        .requestMatchers(HttpMethod.GET,    "/pedidos/**").hasAuthority("PEDIDOS_LEER")
                        .requestMatchers(HttpMethod.POST,   "/pedidos/**").hasAuthority("PEDIDOS_CREAR")
                        .requestMatchers(HttpMethod.PUT,    "/pedidos/**").hasAuthority("PEDIDOS_EDITAR")
                        .requestMatchers(HttpMethod.DELETE, "/pedidos/**").hasAuthority("PEDIDOS_ELIMINAR")

                        // ── Ventas ────────────────────────────────────────────────────────
                        .requestMatchers(HttpMethod.GET,    "/ventas/**").hasAuthority("VENTAS_LEER")
                        .requestMatchers(HttpMethod.POST,   "/ventas/**").hasAuthority("VENTAS_CREAR")

                        // ── Clientes ──────────────────────────────────────────────────────
                        .requestMatchers(HttpMethod.POST,   "/clientes/**").permitAll()
                        .requestMatchers(HttpMethod.GET,    "/clientes/**").hasAuthority("CLIENTES_LEER")
                        .requestMatchers(HttpMethod.PUT,    "/clientes/**").hasAuthority("CLIENTES_EDITAR")
                        .requestMatchers(HttpMethod.DELETE, "/clientes/**").hasAuthority("CLIENTES_ELIMINAR")

                        // ── MercadoPago ───────────────────────────────────────────────────
                        .requestMatchers("/mp/**").hasAuthority("MP_COBRAR")

                        // ── Imágenes ──────────────────────────────────────────────────────
                        .requestMatchers(HttpMethod.GET, "/imagen/**").permitAll()
                        .requestMatchers("/imagen/**").hasAuthority("IMAGENES_GESTIONAR")

                        // ── Pagos catálogo ────────────────────────────────────────────────
                        .requestMatchers(HttpMethod.GET, "/pagos/**").hasAuthority("PAGOS_LEER")

                        // ── Gastos ────────────────────────────────────────────────────────
                        .requestMatchers("/gastos/**").hasAuthority("GASTOS_GESTIONAR")

                        // ── Rifas ─────────────────────────────────────────────────────────
                        .requestMatchers(
                                "/rifa/**", "/ganadorRifa/**",
                                "/configurarRifa/**", "/concursante/**"
                        ).hasAuthority("RIFAS_GESTIONAR")

                        // ── Usuarios (solo admin) ─────────────────────────────────────────
                        .requestMatchers("/usuarios/buscarClientePorIdUsuario/**").permitAll()
                        .requestMatchers("/usuarios/**").hasAuthority("USUARIOS_GESTIONAR")

                        .anyRequest().authenticated()
                )
                .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(
                "https://qa.shop.novedades-jade.com.mx",
                "https://shop.novedades-jade.com.mx",
                "https://front.novedades-jade.com.mx",
                "http://localhost:4200",
                "http://51.178.29.99:30001",
                "https://venta-bolsas-online.netlify.app",
                "https://novedades-jade.com.mx",
                "https://www.novedades-jade.com.mx"
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}