package com.ventas.key.mis.productos.security;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ventas.key.mis.productos.models.ResponseGeneric;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
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

    @Autowired
    private JwtAuthenticationFilter jwtFilter;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        log.info("SecurityConfig cargado");
        return http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .exceptionHandling(ex -> ex
                        // No autenticado (token ausente, inválido o expirado) -> 401
                        // para que el interceptor del front dispare el refresh
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.getWriter().write(objectMapper.writeValueAsString(
                                    new ResponseGeneric<>(null, "Token inválido o expirado")));
                        })
                        // Autenticado pero sin el rol/permiso requerido -> 403
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.getWriter().write(objectMapper.writeValueAsString(
                                    new ResponseGeneric<>(null, "No tiene permisos para acceder a este recurso")));
                        })
                )
                .authorizeHttpRequests(auth -> auth

                        // ── Preflight CORS ────────────────────────────────────────────────
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // ── Documentación / herramientas externas ─────────────────────────
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/v1/dipomex/**").permitAll()

                        // ── Chatbot (público para todos los visitantes) ───────────────────
                        .requestMatchers("/v1/chatbot/**").permitAll()

                        // ── Estado del negocio e imágenes de presentación (GET público) ──
                        .requestMatchers(HttpMethod.GET, "/v1/negocio/estado").permitAll()
                        .requestMatchers(HttpMethod.GET, "/v1/negocio/contactos").permitAll()
                        .requestMatchers(HttpMethod.GET, "/presentacion/imagenes").permitAll()
                        .requestMatchers(HttpMethod.GET, "/presentacion/v1/imagenes").permitAll()
                        .requestMatchers(HttpMethod.GET, "/presentacion/imagenes/*/imagen").permitAll()
                        .requestMatchers(HttpMethod.GET, "/presentacion/v1/imagenes/*/imagen").permitAll()
                        .requestMatchers("/v1/negocio/**").hasRole("ADMIN")
                        .requestMatchers("/presentacion/**").hasRole("ADMIN")

                        // ── Auth ──────────────────────────────────────────────────────────
                        .requestMatchers("/v1/auth/login", "/v1/auth/registrar", "/v1/auth/refresh", "/v1/auth/validar").permitAll()
                        .requestMatchers("/v1/auth/logout").permitAll()

                        // ── Webhook MercadoPago (llamada sin auth desde MP) ────────────────
                        .requestMatchers("/v1/mp/webhook").permitAll()

                        // ── Palabras clave (GET público; escritura solo ADMIN) ────────────
                        .requestMatchers(HttpMethod.GET, "/v1/palabras-clave/**").permitAll()
                        .requestMatchers("/v1/palabras-clave/**").hasRole("ADMIN")

                        // ── Productos (GETs públicos; escritura solo ADMIN) ────────────────
                        .requestMatchers(HttpMethod.GET, "/v1/productos/admin/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/v1/productos/**").permitAll()
                        .requestMatchers("/v1/productos/**").hasRole("ADMIN")

                        // ── Variantes (GETs públicos; escritura solo ADMIN) ────────────────
                        .requestMatchers(HttpMethod.GET, "/variantes/admin/**", "/variantes/v1/admin/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/variantes/**").permitAll()
                        .requestMatchers("/variantes/**").hasRole("ADMIN")

                        // ── Imágenes (GETs públicos excepto caché; escritura solo ADMIN) ────
                        .requestMatchers(HttpMethod.GET, "/imagen/cache/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/imagen/**").permitAll()
                        .requestMatchers("/imagen/**").hasRole("ADMIN")

                        // ── Usuarios ──────────────────────────────────────────────────────
                        .requestMatchers("/v1/usuarios/buscarClientePorIdUsuario/**").permitAll()
                        .requestMatchers(HttpMethod.DELETE, "/v1/usuarios/**").hasRole("ADMIN")
                        .requestMatchers("/v1/usuarios/**").authenticated()

                        // ── Clientes (alta y edición para autenticado; baja solo ADMIN) ────
                        .requestMatchers(HttpMethod.DELETE, "/v1/clientes/**").hasRole("ADMIN")
                        .requestMatchers("/v1/clientes/**").authenticated()

                        // ── Pedidos (consulta y alta para autenticado; gestión solo ADMIN) ──
                        .requestMatchers(HttpMethod.GET,    "/v1/pedidos/**").authenticated()
                        .requestMatchers(HttpMethod.POST,   "/v1/pedidos/savePedido").authenticated()
                        .requestMatchers(HttpMethod.POST,   "/v1/pedidos/*/notificar").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT,    "/v1/pedidos/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/v1/pedidos/**").hasRole("ADMIN")

                        // ── Abonos (apartado / fiado) ────────────────────────────────────
                        .requestMatchers("/v1/abonos/**").hasRole("ADMIN")

                        // ── Ventas ────────────────────────────────────────────────────────
                        .requestMatchers("/v1/ventas/**").hasRole("ADMIN")

                        // ── MercadoPago (resto) ────────────────────────────────────────────
                        .requestMatchers("/v1/mp/**").hasRole("ADMIN")

                        // ── Pagos catálogo ────────────────────────────────────────────────
                        .requestMatchers("/v1/pagos/**").hasRole("ADMIN")

                        // ── Gastos ────────────────────────────────────────────────────────
                        .requestMatchers("/v1/gastos/**").hasRole("ADMIN")

                        // ── Reportes de ventas ───────────────────────────────────────────
                        .requestMatchers("/v1/reportes/**").hasRole("ADMIN")

                        // ── Dashboard ─────────────────────────────────────────────────────
                        .requestMatchers("/v1/dashboard/**").hasRole("ADMIN")

                        // ── Rifas y concursantes ──────────────────────────────────────────
                        .requestMatchers(
                                "/v1/rifa/**", "/v1/ganadorRifa/**",
                                "/v1/configurarRifa/**", "/v1/configurarRifaVariante/**", "/v1/concursante/**"
                        ).hasRole("ADMIN")

                        // ── Carga de documentos (Excel) ───────────────────────────────────
                        .requestMatchers("/v1/documentos/**").hasRole("ADMIN")

                        // ── Admin (gestión interna del servidor) ──────────────────────────
                        .requestMatchers(HttpMethod.GET, "/v1/admin/test-rabbit").permitAll()
                        .requestMatchers("/v1/admin/**").hasRole("ADMIN")

                        // ── WebSocket (handshake HTTP público) ────────────────────────────
                        .requestMatchers("/ws/**").permitAll()

                        // ── Chat en vivo (panel admin requiere ADMIN; conexión pública) ───
                        .requestMatchers("/v1/chat/admin/**").hasRole("ADMIN")
                        .requestMatchers("/v1/chat/**").permitAll()

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