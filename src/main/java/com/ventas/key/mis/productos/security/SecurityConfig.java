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

                        // ── Preflight CORS ────────────────────────────────────────────────
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // ── Documentación / herramientas externas ─────────────────────────
                        .requestMatchers("/swagger-ui/**", "/dipomex/**").permitAll()

                        // ── Auth ──────────────────────────────────────────────────────────
                        .requestMatchers("/auth/login", "/auth/registrar", "/auth/refresh", "/auth/validar").permitAll()
                        .requestMatchers("/auth/logout").authenticated()

                        // ── Webhook MercadoPago (llamada sin auth desde MP) ────────────────
                        .requestMatchers("/mp/webhook").permitAll()

                        // ── Productos (GETs públicos; escritura solo ADMIN) ────────────────
                        .requestMatchers(HttpMethod.GET, "/productos/**").permitAll()
                        .requestMatchers("/productos/**").hasRole("ADMIN")

                        // ── Variantes (GETs públicos; escritura solo ADMIN) ────────────────
                        .requestMatchers(HttpMethod.GET, "/variantes/**").permitAll()
                        .requestMatchers("/variantes/**").hasRole("ADMIN")

                        // ── Imágenes (GETs públicos excepto caché; escritura solo ADMIN) ────
                        .requestMatchers(HttpMethod.GET, "/imagen/cache/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/imagen/**").permitAll()
                        .requestMatchers("/imagen/**").hasRole("ADMIN")

                        // ── Usuarios ──────────────────────────────────────────────────────
                        .requestMatchers("/usuarios/buscarClientePorIdUsuario/**").permitAll()
                        .requestMatchers(HttpMethod.DELETE, "/usuarios/**").hasRole("ADMIN")
                        .requestMatchers("/usuarios/**").authenticated()

                        // ── Clientes (alta y edición para autenticado; baja solo ADMIN) ────
                        .requestMatchers(HttpMethod.DELETE, "/clientes/**").hasRole("ADMIN")
                        .requestMatchers("/clientes/**").authenticated()

                        // ── Pedidos (consulta y alta para autenticado; gestión solo ADMIN) ──
                        .requestMatchers(HttpMethod.GET,    "/pedidos/**").authenticated()
                        .requestMatchers(HttpMethod.POST,   "/pedidos/savePedido").authenticated()
                        .requestMatchers(HttpMethod.PUT,    "/pedidos/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/pedidos/**").hasRole("ADMIN")

                        // ── Ventas ────────────────────────────────────────────────────────
                        .requestMatchers("/ventas/**").hasRole("ADMIN")

                        // ── MercadoPago (resto) ────────────────────────────────────────────
                        .requestMatchers("/mp/**").hasRole("ADMIN")

                        // ── Pagos catálogo ────────────────────────────────────────────────
                        .requestMatchers("/pagos/**").hasRole("ADMIN")

                        // ── Gastos ────────────────────────────────────────────────────────
                        .requestMatchers("/gastos/**").hasRole("ADMIN")

                        // ── Rifas y concursantes ──────────────────────────────────────────
                        .requestMatchers(
                                "/rifa/**", "/ganadorRifa/**",
                                "/configurarRifa/**", "/concursante/**"
                        ).hasRole("ADMIN")

                        // ── Admin (gestión interna del servidor) ──────────────────────────
                        .requestMatchers("/admin/**").hasRole("ADMIN")

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