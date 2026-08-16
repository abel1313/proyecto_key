package com.ventas.key.mis.productos.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ventas.key.mis.productos.entity.Usuario;
import com.ventas.key.mis.productos.jwt.JwtUtil;
import com.ventas.key.mis.productos.models.ResponseGeneric;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Date;
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

    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private UserDetailsService userDetailsService;

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
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(userDetails, jwt, userDetails.getAuthorities());
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
