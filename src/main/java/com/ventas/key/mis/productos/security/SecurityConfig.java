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

    /**
     * Origenes CORS permitidos, por perfil (hallazgo 10 de SEGURIDAD_AUTH.md). Antes la lista
     * estaba hardcodeada aqui y era la misma para todos los ambientes, asi que produccion
     * aceptaba con credenciales dos origenes HTTP planos de desarrollo.
     */
    @Value("${seguridad.cors.origenes-permitidos}")
    private String[] origenesPermitidos;

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
                        .requestMatchers("/v1/auth/login", "/v1/auth/registrar", "/v1/auth/refresh", "/v1/auth/validar",
                                "/v1/auth/olvide-password", "/v1/auth/restablecer-password",
                                "/v1/auth/enviar-codigo-verificacion", "/v1/auth/verificar-correo").permitAll()
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

                        // ── Tienda / variantes (GETs públicos; escritura solo ADMIN) ────────
                        .requestMatchers(HttpMethod.GET, "/tienda/admin/**", "/tienda/v1/admin/**").hasRole("ADMIN")
                        // El CRUD generico heredado de AbstractController devuelve la entidad
                        // Variantes cruda -> arrastra el Producto completo, con precio_costo y
                        // precio_rebaja, y sin el filtro de catalogo publico (listaba tambien
                        // variantes deshabilitadas y sin stock). Estaba cayendo en el permitAll de
                        // abajo, asi que cualquiera sin token podia sacar el margen de la tienda
                        // con /tienda/getAll?page=0&size=1000. El front no los usa (usa
                        // /tienda/v1/buscar y /tienda/v1/buscar-filtrado), asi que pasan a ADMIN.
                        .requestMatchers(HttpMethod.GET, "/tienda/getAll", "/tienda/v1/getAll",
                                "/tienda/getOne/**", "/tienda/v1/getOne/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/tienda/**").permitAll()
                        .requestMatchers("/tienda/**").hasRole("ADMIN")

                        // ── Carga rápida de imágenes (crea producto+variante borrador) ─────
                        .requestMatchers("/v1/carga-imagenes/**").hasRole("ADMIN")

                        // ── Imágenes (GETs públicos excepto caché; escritura solo ADMIN) ────
                        .requestMatchers(HttpMethod.GET, "/imagen/cache/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/imagen/**").permitAll()
                        .requestMatchers("/imagen/**").hasRole("ADMIN")

                        // ── Usuarios (gestion de cuentas/roles/permisos: solo ADMIN) ──────
                        .requestMatchers("/v1/usuarios/buscarClientePorIdUsuario/**").permitAll()
                        .requestMatchers("/v1/usuarios/**").hasRole("ADMIN")

                        // ── Clientes (alta/edicion propia para autenticado — control de
                        //    propiedad en ClienteControllerImpl; busqueda y baja solo ADMIN) ──
                        .requestMatchers(HttpMethod.GET, "/v1/clientes/buscar").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/v1/clientes/**").hasRole("ADMIN")
                        .requestMatchers("/v1/clientes/**").authenticated()

                        // ── Cliente sin registro (alta + verificacion de correo, solo ADMIN
                        //    lo captura durante la venta directa) ──────────────────────────
                        .requestMatchers("/v1/clientes-sin-registro/**").hasRole("ADMIN")

                        // ── Pedidos (consulta y alta para autenticado; gestión solo ADMIN) ──
                        .requestMatchers(HttpMethod.GET,    "/v1/pedidos/**").authenticated()
                        .requestMatchers(HttpMethod.POST,   "/v1/pedidos/savePedido").authenticated()
                        .requestMatchers(HttpMethod.POST,   "/v1/pedidos/*/notificar").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT,    "/v1/pedidos/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/v1/pedidos/**").hasRole("ADMIN")

                        // ── Abonos (apartado / fiado) ────────────────────────────────────
                        .requestMatchers("/v1/abonos/**").hasRole("ADMIN")

                        // ── Lugares de entrega (catalogo; lectura publica -- mismo criterio que
                        //    los catalogos de flores de abajo: nombre de zona y costo de envio,
                        //    nada sensible. Antes era solo "autenticado", lo que dejaba fuera al
                        //    visitante anonimo del configurador publico de flores (2026-08-14);
                        //    alta/edicion/baja sigue solo ADMIN) ──────────────────────────────
                        .requestMatchers(HttpMethod.GET, "/v1/lugares-entrega/**").permitAll()
                        .requestMatchers("/v1/lugares-entrega/**").hasRole("ADMIN")

                        // ── Promociones (catalogo para cualquier autenticado; gestion ADMIN) ─
                        .requestMatchers(HttpMethod.GET, "/v1/promociones/admin/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/v1/promociones/activas").authenticated()
                        .requestMatchers("/v1/promociones/**").hasRole("ADMIN")

                        // ── Flores eternas — catalogos (lectura publica: el cliente configura
                        //    y cotiza su ramo sin necesidad de estar logueado, igual que la
                        //    cinta de promociones; alta/edicion/baja solo ADMIN) ────────────
                        .requestMatchers(HttpMethod.GET, "/v1/tipos-flor/**").permitAll()
                        .requestMatchers("/v1/tipos-flor/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/v1/cantidades-flor/**").permitAll()
                        .requestMatchers("/v1/cantidades-flor/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/v1/accesorios-ramo/**").permitAll()
                        .requestMatchers("/v1/accesorios-ramo/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/v1/frases-liston/**").permitAll()
                        .requestMatchers("/v1/frases-liston/**").hasRole("ADMIN")
                        // Colores de cada especie -- publico para que el cliente elija color tras la cantidad.
                        .requestMatchers(HttpMethod.GET, "/v1/colores-flor/**").permitAll()
                        .requestMatchers("/v1/colores-flor/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/v1/ramos-armados/admin").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/v1/ramos-armados/**").permitAll()
                        .requestMatchers("/v1/ramos-armados/**").hasRole("ADMIN")
                        // "Ticket de produccion" de un ramo, colgado de un Pedido ya creado -- requiere
                        // sesion igual que el resto de /v1/pedidos/**; validar la frase y la bandeja
                        // de frases pendientes de TODOS los pedidos son solo ADMIN.
                        .requestMatchers(HttpMethod.PUT, "/v1/flores/pedidos/detalle/*/validar-frase").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/v1/flores/pedidos/frases-pendientes").hasRole("ADMIN")
                        .requestMatchers("/v1/flores/pedidos/**").authenticated()
                        // Motor de calculo (validar cantidad / cotizar precio): publico, solo lectura/calculo.
                        .requestMatchers("/v1/flores/**").permitAll()

                        // ── Cinta de promociones (letrero corrido; el GET /activos lo pinta
                        //    la tienda para CUALQUIER visitante, incluso sin login -- si exigiera
                        //    auth el cliente anonimo la veria vacia. Alta/edicion/baja solo ADMIN) ─
                        .requestMatchers(HttpMethod.GET, "/v1/cinta/activos").permitAll()
                        .requestMatchers("/v1/cinta/**").hasRole("ADMIN")

                        // ── Favoritos (100% del cliente autenticado, sin vista admin) ───────
                        .requestMatchers("/v1/favoritos/**").authenticated()

                        // ── Reseñas (lectura publica; crear/editar/borrar autenticado -- el
                        //    service decide si es dueno o ADMIN el que puede borrar) ─────────
                        .requestMatchers(HttpMethod.GET, "/v1/resenas/mis-resenas").authenticated()
                        .requestMatchers(HttpMethod.GET, "/v1/resenas/**").permitAll()
                        .requestMatchers(HttpMethod.PUT, "/v1/resenas/*/responder").hasRole("ADMIN")
                        .requestMatchers("/v1/resenas/**").authenticated()

                        // ── Ventas (reclamo de compra es del cliente autenticado; el resto ADMIN) ──
                        .requestMatchers(HttpMethod.POST, "/v1/ventas/reclamar").authenticated()
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

                        // ── Actuator ──────────────────────────────────────────────────────
                        // qa/docker exponen 'caches' ademas de 'health'. Sin esta regla caian en
                        // el anyRequest().authenticated() del final, asi que CUALQUIER usuario con
                        // sesion (un cliente de la tienda) podia listar y vaciar los caches del
                        // micro (DELETE /actuator/caches) y degradar la tienda a voluntad.
                        // health queda abierto porque lo consulta el probe de k8s.
                        .requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers("/actuator/**").hasRole("ADMIN")

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
        config.setAllowedOrigins(List.of(origenesPermitidos));
        log.info("CORS habilitado para {} origenes", origenesPermitidos.length);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}