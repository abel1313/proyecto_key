package com.ventas.key.mis.productos.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ventas.key.mis.productos.entity.AccionSubmenu;
import com.ventas.key.mis.productos.entity.Submenu;
import com.ventas.key.mis.productos.entity.Usuario;
import com.ventas.key.mis.productos.jwt.JwtUtil;
import com.ventas.key.mis.productos.models.PermisosEfectivosDto;
import com.ventas.key.mis.productos.models.ResponseGeneric;
import com.ventas.key.mis.productos.service.UsuarioServiceImpl;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;

@Component
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /**
     * Lo unico que puede hacer un usuario con la contrasena temporal que le puso un ADMIN
     * (hallazgo 14 de SEGURIDAD_AUTH.md). Antes el flag {@code passwordTemporal} viajaba en el
     * login pero el token traia permisos completos, asi que "estar obligado a cambiarla" era solo
     * una convencion del front: bastaba con ignorarla.
     */
    private static final Set<String> RUTAS_PERMITIDAS_PASSWORD_TEMPORAL = Set.of(
            "/v1/auth/cambiar-password",
            "/v1/auth/logout",
            "/v1/auth/refresh",
            "/v1/auth/validar"
    );

    /** Prefijo de authority para cada pantalla (submenu.ruta) que el usuario tiene concedida --
     * ver {@link #autoridadesConPantallas}. Fase 2 de PLAN_PERMISOS_PANTALLAS.md: hasta ahora
     * Menu/Submenu/rol_submenu solo decidian que ve el FRONT (menu dinamico + PantallaGuard),
     * pero el backend seguia protegiendo todo con hasRole("ADMIN") fijo -- darle una pantalla a
     * un rol no-admin la hacia aparecer en el menu, pero cualquier request real le devolvia 403
     * igual. Con esto SecurityConfig ya puede pedir hasAnyAuthority("ROLE_ADMIN",
     * "PANTALLA_<ruta>") en vez de hasRole("ADMIN") fijo en los endpoints que tengan una pantalla
     * equivalente en el catalogo. */
    public static final String PREFIJO_AUTORIDAD_PANTALLA = "PANTALLA_";

    /** Sufijo que se suma a la authority de una pantalla cuando el rol, ademas de poder VERLA
     * (authority base {@link #PREFIJO_AUTORIDAD_PANTALLA}&lt;ruta&gt;), tambien puede ESCRIBIR en
     * ella (crear/editar/borrar) -- Fase 2 de permisos de accion, ver
     * SecurityConfig.pantallaEscribir(). Antes de esto, dar una pantalla era todo-o-nada. */
    public static final String SUFIJO_AUTORIDAD_ESCRITURA = "_ESCRIBIR";

    /** Sufijo para una accion puntual dentro de una pantalla (Fase 3 de permisos, piloto en
     * Modelos 2026-08-27) -- authority final: "PANTALLA_&lt;ruta&gt;_ACCION_&lt;clave&gt;". Ver
     * SecurityConfig#accion. Independiente de {@link #SUFIJO_AUTORIDAD_ESCRITURA}: un rol puede
     * tener Editar sin una accion puntual, o viceversa. */
    public static final String SUFIJO_AUTORIDAD_ACCION = "_ACCION_";

    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private UserDetailsService userDetailsService;
    @Autowired
    private UsuarioServiceImpl usuarioService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String jwt = authHeader.substring(7);
        String username = null;

        try {
            // Rechazar refresh tokens — no son válidos como access tokens
            if (jwtUtil.isRefreshToken(jwt)) {
                log.warn("Se intentó usar un refresh token como access token");
                filterChain.doFilter(request, response);
                return;
            }
            username = jwtUtil.extractUsername(jwt);
        } catch (Exception e) {
            log.warn("No se pudo extraer el username del token: {}", e.getMessage());
        }

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                // Dar de baja a un usuario debe cortarle el acceso de inmediato: sin este
                // chequeo seguia operando con el access token ya emitido hasta que expirara.
                if (!userDetails.isEnabled()) {
                    log.warn("Token rechazado, cuenta deshabilitada: {}", username);
                } else if (tokenEmitidoAntesDeCambioPassword(jwt, userDetails)) {
                    log.warn("Token rechazado, emitido antes del ultimo cambio de contrasena: {}", username);
                } else if (jwtUtil.validateToken(jwt, userDetails)) {
                    if (debeBloquearPorPasswordTemporal(userDetails, request)) {
                        log.warn("Acceso bloqueado por contrasena temporal sin cambiar: {} -> {}",
                                username, request.getServletPath());
                        responderPasswordTemporal(response);
                        return;
                    }
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails, jwt, autoridadesConPantallas(userDetails));
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            } catch (Exception e) {
                log.warn("Error al autenticar el token: {}", e.getMessage());
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Junta los authorities normales del usuario (ROLE_x, permisos) con uno
     * "{@value #PREFIJO_AUTORIDAD_PANTALLA}&lt;ruta&gt;" por cada pantalla que
     * {@link UsuarioServiceImpl#permisosEfectivos} le da en ESTE momento -- se recalcula en cada
     * request, así que a diferencia del claim "pantallas" del JWT (que solo se recalcula en
     * login/refresh, hasta 15 min de rezago) esto refleja un cambio de permisos al instante. Si
     * falla (usuario recien creado sin rol, error de datos) no tumba el request: sigue solo con
     * los authorities normales, igual que antes de este cambio.
     *
     * <p><b>Encontrado 2026-08-27 (lentitud reportada en login y en cualquier pantalla que
     * mandara el token):</b> esto llamaba a submenusEfectivos + submenusEscritura +
     * accionesEfectivas por separado -- 3 fetches redundantes del MISMO usuario en cada request
     * autenticado. Se corrigió a UN SOLO fetch via {@code permisosEfectivos} -- pero ese metodo
     * TODAVIA volvia a pedir el Usuario a la BD, duplicando el fetch que
     * {@code userDetailsService.loadUserByUsername} YA hizo unas lineas arriba en el mismo
     * request. Aqui abajo se usa la variante que reutiliza el {@code usuario} que ya tenemos en
     * memoria (mismo objeto que dejó {@code userDetails}), sin volver a tocar la BD para
     * Usuario/Roles -- solo la query de excepciones de usuario_submenu.
     */
    private Collection<? extends GrantedAuthority> autoridadesConPantallas(UserDetails userDetails) {
        List<GrantedAuthority> authorities = new ArrayList<>(userDetails.getAuthorities());
        if (userDetails instanceof Usuario usuario && usuario.getId() != null) {
            try {
                PermisosEfectivosDto permisos = usuarioService.permisosEfectivos(usuario);
                for (Submenu s : permisos.getPantallas()) {
                    authorities.add(new SimpleGrantedAuthority(PREFIJO_AUTORIDAD_PANTALLA + s.getRuta()));
                }
                for (Submenu s : permisos.getPantallasEscritura()) {
                    authorities.add(new SimpleGrantedAuthority(
                            PREFIJO_AUTORIDAD_PANTALLA + s.getRuta() + SUFIJO_AUTORIDAD_ESCRITURA));
                }
                for (AccionSubmenu a : permisos.getAcciones()) {
                    authorities.add(new SimpleGrantedAuthority(PREFIJO_AUTORIDAD_PANTALLA
                            + a.getSubmenu().getRuta() + SUFIJO_AUTORIDAD_ACCION + a.getClave()));
                }
            } catch (Exception e) {
                log.warn("No se pudieron calcular las pantallas efectivas de {}: {}", usuario.getUsername(), e.getMessage());
            }
        }
        return authorities;
    }

    /**
     * El access token es stateless y vive hasta 15 minutos sin poder revocarse en el servidor.
     * Cambiar la contrasena ya mata el refresh token (sesion_refresh), pero el access token ya
     * emitido seguia funcionando hasta expirar por su cuenta. Comparando su iat contra
     * {@code passwordActualizadoEn} se corta de inmediato, igual que el chequeo de isEnabled().
     */
    private boolean tokenEmitidoAntesDeCambioPassword(String jwt, UserDetails userDetails) {
        if (!(userDetails instanceof Usuario usuario) || usuario.getPasswordActualizadoEn() == null) {
            return false;
        }
        try {
            Date iat = jwtUtil.extractIssuedAt(jwt);
            Date cambio = java.sql.Timestamp.valueOf(usuario.getPasswordActualizadoEn());
            return iat != null && iat.before(cambio);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * getServletPath() y no getRequestURI(): el primero ya viene sin el context-path
     * ({@code /mis-productos}), asi que la comparacion no depende de como este desplegado.
     */
    private boolean debeBloquearPorPasswordTemporal(UserDetails userDetails, HttpServletRequest request) {
        if (!(userDetails instanceof Usuario usuario) || !Boolean.TRUE.equals(usuario.getPasswordTemporal())) {
            return false;
        }
        return !RUTAS_PERMITIDAS_PASSWORD_TEMPORAL.contains(request.getServletPath());
    }

    /** Mismo envoltorio ResponseGeneric que usa SecurityConfig, para que el front lo lea igual. */
    private void responderPasswordTemporal(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        ResponseGeneric<String> cuerpo =
                new ResponseGeneric<>((String) null, "Debes cambiar tu contrasena temporal antes de continuar");
        response.getWriter().write(objectMapper.writeValueAsString(cuerpo));
    }
}
