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

    /**
     * Fase 2 de PLAN_PERMISOS_PANTALLAS.md: en vez de hasRole("ADMIN") fijo, los endpoints con
     * una pantalla equivalente en el catálogo Menu/Submenu piden ROLE_ADMIN O cualquiera de las
     * authorities "PANTALLA_&lt;ruta&gt;" que {@link JwtAuthenticationFilter} calcula en cada
     * request (ver esa clase). ROLE_ADMIN sigue explícito aquí a propósito, como red de
     * seguridad: hoy ya tiene todas las pantallas via rol_submenu, pero si esa tabla alguna vez
     * queda incompleta para una pantalla nueva (como pasó con gestion-menu, ver
     * migration_fix_submenu_gestion_menu.sql) el admin no se queda bloqueado de su propio
     * endpoint mientras se corrige el catálogo.
     */
    private static String[] pantalla(String... rutas) {
        String[] authorities = new String[rutas.length + 1];
        authorities[0] = "ROLE_ADMIN";
        for (int i = 0; i < rutas.length; i++) {
            authorities[i + 1] = JwtAuthenticationFilter.PREFIJO_AUTORIDAD_PANTALLA + rutas[i];
        }
        return authorities;
    }

    /**
     * Fase 2 de permisos de accion (2026-08-27): igual que {@link #pantalla}, pero para las
     * operaciones de ESCRITURA (crear/editar/borrar) de una pantalla -- exige la authority
     * "PANTALLA_&lt;ruta&gt;_ESCRIBIR" que {@link JwtAuthenticationFilter} solo agrega cuando el
     * rol tiene esa pantalla en rol_submenu_escritura, no solo en rol_submenu. Antes de esto,
     * dar de alta una pantalla alcanzaba automaticamente para escribir en ella; ahora es un
     * segundo permiso explicito. Los GET de cada bloque siguen pidiendo {@link #pantalla} (ver
     * la pantalla no cambia), solo los metodos que modifican datos piden este.
     */
    private static String[] pantallaEscribir(String... rutas) {
        String[] authorities = new String[rutas.length + 1];
        authorities[0] = "ROLE_ADMIN";
        for (int i = 0; i < rutas.length; i++) {
            authorities[i + 1] = JwtAuthenticationFilter.PREFIJO_AUTORIDAD_PANTALLA + rutas[i]
                    + JwtAuthenticationFilter.SUFIJO_AUTORIDAD_ESCRITURA;
        }
        return authorities;
    }

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
                        .requestMatchers(HttpMethod.GET, "/v1/negocio/**").hasAnyAuthority(pantalla("admin/negocio"))
                        .requestMatchers("/v1/negocio/**").hasAnyAuthority(pantallaEscribir("admin/negocio"))
                        .requestMatchers(HttpMethod.GET, "/presentacion/**").hasAnyAuthority(pantalla("admin/presentacion"))
                        .requestMatchers("/presentacion/**").hasAnyAuthority(pantallaEscribir("admin/presentacion"))

                        // ── Personalización de tema -- catálogo dinámico de variables (GET
                        //    /activo público: hasta un visitante anónimo necesita el tema activo
                        //    para pintar la tienda; el resto del CRUD es solo ADMIN) ──────────
                        .requestMatchers(HttpMethod.GET, "/v1/tema-variable/activo").permitAll()
                        .requestMatchers(HttpMethod.GET, "/v1/tema-variable/**").hasAnyAuthority(pantalla("personalizacion"))
                        .requestMatchers("/v1/tema-variable/**").hasAnyAuthority(pantallaEscribir("personalizacion"))

                        // ── Auth ──────────────────────────────────────────────────────────
                        .requestMatchers("/v1/auth/login", "/v1/auth/registrar", "/v1/auth/refresh", "/v1/auth/validar",
                                "/v1/auth/olvide-password", "/v1/auth/restablecer-password",
                                "/v1/auth/enviar-codigo-verificacion", "/v1/auth/verificar-correo").permitAll()
                        .requestMatchers("/v1/auth/logout").permitAll()

                        // ── Webhook MercadoPago (llamada sin auth desde MP) ────────────────
                        .requestMatchers("/v1/mp/webhook").permitAll()

                        // ── Webhook Facebook -- comentarios (llamada sin auth desde Meta,
                        // validado por firma X-Hub-Signature-256 dentro del controlador) ──────
                        .requestMatchers("/v1/redes-sociales/facebook/webhook").permitAll()

                        // ── Palabras clave (GET público; escritura solo ADMIN) ────────────
                        .requestMatchers(HttpMethod.GET, "/v1/palabras-clave/**").permitAll()
                        .requestMatchers("/v1/palabras-clave/**").hasAnyAuthority(pantallaEscribir("palabras-clave"))

                        // ── Productos (GETs públicos; escritura solo ADMIN) ────────────────
                        // Varias pantallas distintas escriben aca (Modelos, Agregar modelo,
                        // Agregar producto) -- cualquiera de las tres basta.
                        .requestMatchers(HttpMethod.GET, "/v1/productos/admin/**")
                                .hasAnyAuthority(pantalla("productos/buscar", "productos/agregar", "tienda/venta"))
                        .requestMatchers(HttpMethod.GET, "/v1/productos/**").permitAll()
                        .requestMatchers("/v1/productos/**")
                                .hasAnyAuthority(pantallaEscribir("productos/buscar", "productos/agregar", "tienda/venta"))

                        // ── Tienda / variantes (GETs públicos; escritura solo ADMIN) ────────
                        // Gestionar promociones usa /tienda/v1/admin/filtrar para su buscador de
                        // variantes embebido -- sin admin/promociones aca, ese permiso solo
                        // alcanzaba para el CRUD propio de promociones pero no para buscar la
                        // variante a promocionar.
                        .requestMatchers(HttpMethod.GET, "/tienda/admin/**", "/tienda/v1/admin/**")
                                .hasAnyAuthority(pantalla("productos/buscar", "productos/agregar", "tienda/venta",
                                        "admin/promociones"))
                        // El CRUD generico heredado de AbstractController devuelve la entidad
                        // Variantes cruda -> arrastra el Producto completo, con precio_costo y
                        // precio_rebaja, y sin el filtro de catalogo publico (listaba tambien
                        // variantes deshabilitadas y sin stock). Estaba cayendo en el permitAll de
                        // abajo, asi que cualquiera sin token podia sacar el margen de la tienda
                        // con /tienda/getAll?page=0&size=1000. El front no los usa (usa
                        // /tienda/v1/buscar y /tienda/v1/buscar-filtrado), asi que pasan a ADMIN.
                        .requestMatchers(HttpMethod.GET, "/tienda/getAll", "/tienda/v1/getAll",
                                "/tienda/getOne/**", "/tienda/v1/getOne/**")
                                .hasAnyAuthority(pantalla("productos/buscar", "productos/agregar", "tienda/venta"))
                        .requestMatchers(HttpMethod.GET, "/tienda/**").permitAll()
                        // Catalogos de flores y Administrar ramos armados suben fotos de sus
                        // variantes via /tienda/v1/guardarConImagenes (mismo endpoint generico de
                        // Variantes) -- sin esto, dar solo el permiso de esas pantallas no alcanzaba
                        // para guardar una foto y el usuario se topaba con un 403 "escondido".
                        .requestMatchers("/tienda/**")
                                .hasAnyAuthority(pantallaEscribir("productos/buscar", "productos/agregar", "tienda/venta",
                                        "flores/catalogos", "flores/ramos-admin"))

                        // ── Carga rápida de imágenes (crea producto+variante borrador) ─────
                        .requestMatchers(HttpMethod.GET, "/v1/carga-imagenes/**").hasAnyAuthority(pantalla("carga-imagenes"))
                        .requestMatchers("/v1/carga-imagenes/**").hasAnyAuthority(pantallaEscribir("carga-imagenes"))

                        // ── Imágenes (GETs públicos excepto caché; escritura solo ADMIN) ────
                        .requestMatchers(HttpMethod.GET, "/imagen/cache/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/imagen/**").permitAll()
                        .requestMatchers("/imagen/**").hasRole("ADMIN")

                        // ── Usuarios (gestion de cuentas/roles/permisos: solo ADMIN) ──────
                        .requestMatchers("/v1/usuarios/buscarClientePorIdUsuario/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/v1/usuarios/**").hasAnyAuthority(pantalla("usuarios/buscar"))
                        .requestMatchers("/v1/usuarios/**").hasAnyAuthority(pantallaEscribir("usuarios/buscar"))

                        // ── Clientes (alta/edicion propia para autenticado — control de
                        //    propiedad en ClienteControllerImpl; busqueda y baja solo ADMIN) ──
                        .requestMatchers(HttpMethod.GET, "/v1/clientes/buscar").hasAnyAuthority(pantalla("clientes/buscar"))
                        .requestMatchers(HttpMethod.DELETE, "/v1/clientes/**").hasAnyAuthority(pantallaEscribir("clientes/buscar"))
                        .requestMatchers("/v1/clientes/**").authenticated()

                        // ── Cliente sin registro (alta + verificacion de correo, solo ADMIN
                        //    lo captura durante la venta directa) ──────────────────────────
                        .requestMatchers(HttpMethod.GET, "/v1/clientes-sin-registro/**").hasAnyAuthority(pantalla("tienda/venta-directa"))
                        .requestMatchers("/v1/clientes-sin-registro/**").hasAnyAuthority(pantallaEscribir("tienda/venta-directa"))

                        // ── Pedidos (consulta y alta para autenticado; gestión solo ADMIN) ──
                        // buscarClientePedido busca SIN filtro de dueño en TODOS los pedidos de
                        // TODOS los clientes (PedidoServiceImpl.buscarClientePorPedido) -- es la
                        // busqueda global del admin, nunca fue pensada para un cliente. Estaba
                        // cayendo en el .authenticated() de abajo, asi que cualquier usuario
                        // logueado podia buscar y ver los pedidos de cualquier otro cliente
                        // (encontrado 2026-08-27, junto con la misma IDOR en findPedido/**).
                        .requestMatchers(HttpMethod.GET, "/v1/pedidos/buscarClientePedido").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET,    "/v1/pedidos/**").authenticated()
                        .requestMatchers(HttpMethod.POST,   "/v1/pedidos/savePedido").authenticated()
                        .requestMatchers(HttpMethod.POST,   "/v1/pedidos/*/notificar").hasRole("ADMIN")
                        // El cliente edita los datos de entrega (direccion, mapa, zona) de SU
                        // PROPIO pedido -- PedidoServiceImpl.editarDatosEntrega ya valida dueno
                        // vs ADMIN (mismo patron que ResenaServiceImpl). Antes esta ruta caia en
                        // el hasRole("ADMIN") de abajo: el front ya tenia el modal "Info de
                        // entrega" armado para el cliente (mapa, zona) pero el back lo rechazaba
                        // con 403 en cualquier intento real de un cliente (encontrado 2026-08-25,
                        // curl real de un ROLE_USUARIO contra PUT /v1/pedidos/99/entrega).
                        .requestMatchers(HttpMethod.PUT,    "/v1/pedidos/*/entrega").authenticated()
                        .requestMatchers(HttpMethod.PUT,    "/v1/pedidos/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/v1/pedidos/**").hasRole("ADMIN")

                        // ── Abonos (apartado / fiado) ────────────────────────────────────
                        .requestMatchers(HttpMethod.GET, "/v1/abonos/**").hasAnyAuthority(pantalla("abonos"))
                        .requestMatchers("/v1/abonos/**").hasAnyAuthority(pantallaEscribir("abonos"))

                        // ── Menu/Submenu (catalogo de pantallas, Fase 1 de PLAN_PERMISOS_PANTALLAS.md
                        //    -- repo compartido). Fase 2: ya hay pantallas dedicadas para esto
                        //    (gestion-menu / gestion-menu/roles), asi que dejan de ser hasRole fijo.
                        .requestMatchers(HttpMethod.GET, "/v1/menu/**").hasAnyAuthority(pantalla("gestion-menu"))
                        .requestMatchers("/v1/menu/**").hasAnyAuthority(pantallaEscribir("gestion-menu"))
                        .requestMatchers(HttpMethod.GET, "/v1/submenu/**").hasAnyAuthority(pantalla("gestion-menu"))
                        .requestMatchers("/v1/submenu/**").hasAnyAuthority(pantallaEscribir("gestion-menu"))
                        // rol_submenu -- CRUD de roles + asignacion de pantallas por rol.
                        .requestMatchers(HttpMethod.GET, "/v1/roles/**").hasAnyAuthority(pantalla("gestion-menu/roles"))
                        .requestMatchers("/v1/roles/**").hasAnyAuthority(pantallaEscribir("gestion-menu/roles"))

                        // ── Lugares de entrega (catalogo; lectura publica -- mismo criterio que
                        //    los catalogos de flores de abajo: nombre de zona y costo de envio,
                        //    nada sensible. Antes era solo "autenticado", lo que dejaba fuera al
                        //    visitante anonimo del configurador publico de flores (2026-08-14);
                        //    alta/edicion/baja sigue solo ADMIN) ──────────────────────────────
                        .requestMatchers(HttpMethod.GET, "/v1/lugares-entrega/**").permitAll()
                        // calcular-costo (anillos) lo llama el checkout ANTES de que el cliente tenga
                        // sesion necesariamente (visitante anonimo cotizando) -- ver DISENO_ZONAS_POR_ANILLO.md.
                        .requestMatchers(HttpMethod.POST, "/v1/lugares-entrega/*/calcular-costo").permitAll()
                        .requestMatchers("/v1/lugares-entrega/**").hasAnyAuthority(pantallaEscribir("lugares-entrega"))

                        // ── Promociones (catalogo publico -- mismo criterio que la cinta de
                        //    promociones de abajo: el listado de "hay promos activas" lo pinta la
                        //    tienda para CUALQUIER visitante, incluso sin login. Estaba en
                        //    ".authenticated()" -- el visitante anonimo de /tienda/buscar recibia
                        //    401 en esta llamada de fondo (BuscarComponent.ngOnInit la dispara
                        //    siempre), lo que el interceptor del front interpretaba como "sesion
                        //    muerta" y lo mandaba al login sin haber iniciado sesion nunca
                        //    (encontrado 2026-08-25, al arreglar el redirect raiz que antes
                        //    tapaba este bug enviando a todos al login de todas formas). Gestion
                        //    (crear/editar/borrar) sigue solo ADMIN) ────────────────────────────
                        .requestMatchers(HttpMethod.GET, "/v1/promociones/admin/**").hasAnyAuthority(pantalla("admin/promociones"))
                        .requestMatchers(HttpMethod.GET, "/v1/promociones/activas").permitAll()
                        .requestMatchers(HttpMethod.GET, "/v1/promociones/**").hasAnyAuthority(pantalla("admin/promociones"))
                        .requestMatchers("/v1/promociones/**").hasAnyAuthority(pantallaEscribir("admin/promociones"))

                        // ── Flores eternas — catalogos (lectura publica: el cliente configura
                        //    y cotiza su ramo sin necesidad de estar logueado, igual que la
                        //    cinta de promociones; alta/edicion/baja solo ADMIN) ────────────
                        .requestMatchers(HttpMethod.GET, "/v1/tipos-flor/**").permitAll()
                        .requestMatchers("/v1/tipos-flor/**").hasAnyAuthority(pantallaEscribir("flores/catalogos"))
                        .requestMatchers(HttpMethod.GET, "/v1/cantidades-flor/**").permitAll()
                        .requestMatchers("/v1/cantidades-flor/**").hasAnyAuthority(pantallaEscribir("flores/catalogos"))
                        .requestMatchers(HttpMethod.GET, "/v1/accesorios-ramo/**").permitAll()
                        .requestMatchers("/v1/accesorios-ramo/**").hasAnyAuthority(pantallaEscribir("flores/catalogos"))
                        .requestMatchers(HttpMethod.GET, "/v1/frases-liston/**").permitAll()
                        .requestMatchers("/v1/frases-liston/**").hasAnyAuthority(pantallaEscribir("flores/catalogos"))
                        // Colores de cada especie -- publico para que el cliente elija color tras la cantidad.
                        .requestMatchers(HttpMethod.GET, "/v1/colores-flor/**").permitAll()
                        .requestMatchers("/v1/colores-flor/**").hasAnyAuthority(pantallaEscribir("flores/catalogos"))
                        .requestMatchers(HttpMethod.GET, "/v1/ramos-armados/admin").hasAnyAuthority(pantalla("flores/ramos-admin"))
                        .requestMatchers(HttpMethod.GET, "/v1/ramos-armados/**").permitAll()
                        .requestMatchers("/v1/ramos-armados/**").hasAnyAuthority(pantallaEscribir("flores/ramos-admin"))
                        // "Ticket de produccion" de un ramo, colgado de un Pedido ya creado -- requiere
                        // sesion igual que el resto de /v1/pedidos/**; validar la frase y la bandeja
                        // de frases pendientes de TODOS los pedidos son solo ADMIN.
                        .requestMatchers(HttpMethod.PUT, "/v1/flores/pedidos/detalle/*/validar-frase").hasAnyAuthority(pantallaEscribir("flores/frases"))
                        .requestMatchers(HttpMethod.GET, "/v1/flores/pedidos/frases-pendientes").hasAnyAuthority(pantalla("flores/frases"))
                        .requestMatchers("/v1/flores/pedidos/**").authenticated()
                        // Motor de calculo (validar cantidad / cotizar precio): publico, solo lectura/calculo.
                        .requestMatchers("/v1/flores/**").permitAll()

                        // ── Cinta de promociones (letrero corrido; el GET /activos lo pinta
                        //    la tienda para CUALQUIER visitante, incluso sin login -- si exigiera
                        //    auth el cliente anonimo la veria vacia. Alta/edicion/baja solo ADMIN) ─
                        .requestMatchers(HttpMethod.GET, "/v1/cinta/activos").permitAll()
                        .requestMatchers(HttpMethod.GET, "/v1/cinta/**").hasAnyAuthority(pantalla("admin/cinta"))
                        .requestMatchers("/v1/cinta/**").hasAnyAuthority(pantallaEscribir("admin/cinta"))

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
                        .requestMatchers(HttpMethod.GET, "/v1/gastos/**").hasAnyAuthority(pantalla("gastos/buscar"))
                        .requestMatchers("/v1/gastos/**").hasAnyAuthority(pantallaEscribir("gastos/buscar"))

                        // ── Reportes de ventas ───────────────────────────────────────────
                        .requestMatchers(HttpMethod.GET, "/v1/reportes/**").hasAnyAuthority(pantalla("reportes"))
                        .requestMatchers("/v1/reportes/**").hasAnyAuthority(pantallaEscribir("reportes"))

                        // ── Dashboard ─────────────────────────────────────────────────────
                        .requestMatchers(HttpMethod.GET, "/v1/dashboard/**").hasAnyAuthority(pantalla("dashboard"))
                        .requestMatchers("/v1/dashboard/**").hasAnyAuthority(pantallaEscribir("dashboard"))

                        // ── Redes sociales (publicar variantes en Facebook) ───────────────
                        .requestMatchers(HttpMethod.GET, "/v1/redes-sociales/**").hasAnyAuthority(pantalla("admin/facebook", "admin/hashtags"))
                        .requestMatchers("/v1/redes-sociales/**").hasAnyAuthority(pantallaEscribir("admin/facebook", "admin/hashtags"))

                        // ── Rifas y concursantes ──────────────────────────────────────────
                        .requestMatchers(HttpMethod.GET,
                                "/v1/rifa/**", "/v1/ganadorRifa/**",
                                "/v1/configurarRifa/**", "/v1/configurarRifaVariante/**", "/v1/concursante/**"
                        ).hasAnyAuthority(pantalla("rifas/agregar", "rifas/mes", "rifas/buscar"))
                        .requestMatchers(
                                "/v1/rifa/**", "/v1/ganadorRifa/**",
                                "/v1/configurarRifa/**", "/v1/configurarRifaVariante/**", "/v1/concursante/**"
                        ).hasAnyAuthority(pantallaEscribir("rifas/agregar", "rifas/mes", "rifas/buscar"))

                        // ── Carga de documentos (Excel) ───────────────────────────────────
                        .requestMatchers(HttpMethod.GET, "/v1/documentos/**").hasAnyAuthority(pantalla("tienda/cargar-excel"))
                        .requestMatchers("/v1/documentos/**").hasAnyAuthority(pantallaEscribir("tienda/cargar-excel"))

                        // ── Admin (gestión interna del servidor) ──────────────────────────
                        .requestMatchers(HttpMethod.GET, "/v1/admin/test-rabbit").permitAll()
                        // Reconciliacion de imagenes ya tiene su propia pantalla en el catalogo
                        // (admin/reconciliacion-imagenes) -- antes vivia bajo admin/cache, asi que
                        // dar solo ese permiso especifico no alcanzaba para usarla.
                        .requestMatchers(HttpMethod.GET, "/v1/admin/reconciliacion/**")
                                .hasAnyAuthority(pantalla("admin/reconciliacion-imagenes"))
                        .requestMatchers("/v1/admin/reconciliacion/**")
                                .hasAnyAuthority(pantallaEscribir("admin/reconciliacion-imagenes"))
                        .requestMatchers(HttpMethod.GET, "/v1/admin/**").hasAnyAuthority(pantalla("admin/cache"))
                        .requestMatchers("/v1/admin/**").hasAnyAuthority(pantallaEscribir("admin/cache"))

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
                        .requestMatchers(HttpMethod.GET, "/v1/chat/admin/**").hasAnyAuthority(pantalla("admin/chat"))
                        .requestMatchers("/v1/chat/admin/**").hasAnyAuthority(pantallaEscribir("admin/chat"))
                        // El historial por usuarioId/clienteId es SOLO para el chat de un usuario
                        // ya autenticado (ver ChatLiveService.conectar() en el front -- nunca lo
                        // llama si no hay usuarioId). Estaba cayendo en el permitAll de abajo sin
                        // ni siquiera pedir sesion, y el controller tampoco validaba dueno --
                        // cualquiera en internet, sin login, podia leer el chat privado de
                        // CUALQUIER cliente solo cambiando el numero en la URL (encontrado
                        // 2026-08-27, junto con la misma IDOR de Pedidos/Flores). El historial
                        // por sesionId (chat anonimo) SI sigue publico a proposito: sesionId es
                        // un UUID random generado por el cliente, imposible de adivinar -- ver
                        // ChatAdminController.historialUsuario().
                        .requestMatchers("/v1/chat/historial/usuario/**", "/v1/chat/historial/cliente/**").authenticated()
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

    // El bean PasswordEncoder se movio a PasswordEncoderConfig -- rompe una dependencia
    // circular con JwtAuthenticationFilter/UsuarioServiceImpl (ver esa clase para el detalle).

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