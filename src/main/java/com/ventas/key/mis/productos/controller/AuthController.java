package com.ventas.key.mis.productos.controller;

import com.ventas.key.mis.productos.entity.Usuario;
import com.ventas.key.mis.productos.jwt.JwtUtil;
import com.ventas.key.mis.productos.models.AuthRequest;
import com.ventas.key.mis.productos.models.AuthResponse;
import com.ventas.key.mis.productos.models.RegistroRequest;
import com.ventas.key.mis.productos.service.LoginRateLimiterService;
import com.ventas.key.mis.productos.service.RegistroService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;

@Tag(name = "Autenticacion", description = "Login, logout, registro y renovacion de tokens JWT. El refresh token se guarda en cookie HttpOnly.")
@RestController
@RequestMapping("/v1/auth")
@Slf4j
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authManager;
    private final JwtUtil jwtUtil;
    private final RegistroService registroService;
    private final LoginRateLimiterService rateLimiterService;
    private final UserDetailsService userDetailsService;

    @Value("${cookie.secure:true}")
    private boolean cookieSecure;

    @Value("${server.servlet.context-path:}")
    private String contextPath;

    @Value("${seguridad.rate-limit-habilitado:true}")
    private boolean rateLimitHabilitado;

    private static final String REFRESH_COOKIE = "refreshToken";
    private static final int REFRESH_MAX_AGE = 60 * 60 * 24 * 7; // 7 días en segundos

    @Operation(summary = "Iniciar sesion", description = "Autentica con usuario y contrasena. Devuelve access token JWT en el body y guarda refresh token en cookie HttpOnly. Protegido contra fuerza bruta (5 intentos por IP cada 15 minutos).")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Login exitoso; devuelve access token"),
        @ApiResponse(responseCode = "401", description = "Credenciales invalidas"),
        @ApiResponse(responseCode = "429", description = "Demasiados intentos fallidos; esperar 15 minutos"),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody AuthRequest request,
                                   HttpServletRequest httpRequest,
                                   HttpServletResponse response) {
        String clientIp = resolverIp(httpRequest);
        // Normalizar a minúsculas para que "Admin", "ADMIN" y "admin" compartan el mismo bucket
        String usernameKey = "usr:" + request.getUserName().toLowerCase().trim();

        if (rateLimitHabilitado && !rateLimiterService.tryConsume(clientIp)) {
            log.warn("Rate limit por IP excedido: {}", clientIp);
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body("Demasiados intentos fallidos. Intente de nuevo en 15 minutos.");
        }
        if (rateLimitHabilitado && !rateLimiterService.tryConsume(usernameKey)) {
            log.warn("Rate limit por usuario excedido: {}", request.getUserName());
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body("Demasiados intentos fallidos. Intente de nuevo en 15 minutos.");
        }

        try {
            Authentication auth = authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUserName(), request.getPassword())
            );
            Usuario usr = (Usuario) auth.getPrincipal();
            String accessToken  = jwtUtil.generateToken((UserDetails) auth.getPrincipal(), usr.getId());
            String refreshToken = jwtUtil.generateRefreshToken((UserDetails) auth.getPrincipal(), usr.getId(), System.currentTimeMillis());

            agregarRefreshCookie(response, refreshToken);

            return ResponseEntity.ok(new AuthResponse(accessToken));
        } catch (BadCredentialsException e) {
            log.warn("Intento de login fallido para usuario: {} desde IP: {}", request.getUserName(), clientIp);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Credenciales inválidas");
        } catch (Exception e) {
            log.error("Error inesperado en login: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error al procesar la solicitud");
        }
    }

    @Operation(summary = "Renovar access token", description = "Lee el refresh token de la cookie HttpOnly, valida que no este expirado y devuelve nuevo access token.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Token renovado correctamente"),
        @ApiResponse(responseCode = "401", description = "Refresh token ausente, invalido o expirado")
    })
    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = leerRefreshCookie(request);

        if (refreshToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("No hay refresh token");
        }
        if (!jwtUtil.validateToken(refreshToken) || !jwtUtil.isRefreshToken(refreshToken)) {
            limpiarRefreshCookie(response);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Refresh token inválido o expirado");
        }

        try {
            String username = jwtUtil.extractUsername(refreshToken);
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            Usuario usr = (Usuario) userDetails;

            long sessionStart = jwtUtil.extractSessionStart(refreshToken);
            String newAccessToken  = jwtUtil.generateToken(userDetails, usr.getId());
            String newRefreshToken = jwtUtil.generateRefreshToken(userDetails, usr.getId(), sessionStart);

            agregarRefreshCookie(response, newRefreshToken);

            return ResponseEntity.ok(new AuthResponse(newAccessToken));
        } catch (Exception e) {
            log.error("Error al refrescar token: {}", e.getMessage());
            limpiarRefreshCookie(response);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("No se pudo renovar la sesión");
        }
    }

    @Operation(summary = "Cerrar sesion", description = "Limpia la cookie del refresh token cerrando la sesion del usuario.")
    @ApiResponse(responseCode = "200", description = "Sesion cerrada correctamente")
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse response) {
        limpiarRefreshCookie(response);
        return ResponseEntity.ok("Sesión cerrada");
    }

    @Operation(summary = "Registrar usuario", description = "Crea un nuevo usuario en el sistema. Protegido contra registro masivo por IP (mismo rate limit que login).")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Usuario registrado correctamente"),
        @ApiResponse(responseCode = "400", description = "Datos de registro invalidos"),
        @ApiResponse(responseCode = "429", description = "Demasiados intentos; esperar 15 minutos")
    })
    @PostMapping("/registrar")
    public ResponseEntity<?> registrar(@Valid @RequestBody RegistroRequest request,
                                       HttpServletRequest httpRequest) throws Exception {
        String clientIp = resolverIp(httpRequest);
        if (rateLimitHabilitado && !rateLimiterService.tryConsume(clientIp)) {
            log.warn("Rate limit de registro excedido para IP: {}", clientIp);
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body("Demasiados intentos de registro. Intente de nuevo en 15 minutos.");
        }
        return ResponseEntity.ok(registroService.registrarUsuario(request.getUserName(), request.getPassword(), request.getEmail()));
    }

    @Operation(summary = "Validar token JWT", description = "Verifica si el access token enviado en el header Authorization es valido y no esta expirado.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Token valido"),
        @ApiResponse(responseCode = "401", description = "Token invalido o expirado")
    })
    @GetMapping("/validar")
    public ResponseEntity<?> validarToken(
            @Parameter(description = "Bearer token JWT", required = true) @RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.replace("Bearer ", "");
            if (jwtUtil.validateToken(token)) {
                return ResponseEntity.ok("Token válido");
            }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token inválido");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token inválido");
        }
    }

    private void agregarRefreshCookie(HttpServletResponse response, String refreshToken) {
        String secureFlag = cookieSecure ? "; Secure; SameSite=None" : "; SameSite=Lax";
        String cookiePath = contextPath + "/v1/auth";
        response.addHeader("Set-Cookie",
                String.format("%s=%s; Max-Age=%d; Path=%s; HttpOnly%s",
                        REFRESH_COOKIE, refreshToken, REFRESH_MAX_AGE, cookiePath, secureFlag));
    }

    private void limpiarRefreshCookie(HttpServletResponse response) {
        String secureFlag = cookieSecure ? "; Secure; SameSite=None" : "; SameSite=Lax";
        String cookiePath = contextPath + "/v1/auth";
        response.addHeader("Set-Cookie",
                String.format("%s=; Max-Age=0; Path=%s; HttpOnly%s",
                        REFRESH_COOKIE, cookiePath, secureFlag));
    }

    private String leerRefreshCookie(HttpServletRequest request) {
        if (request.getCookies() == null) return null;
        return Arrays.stream(request.getCookies())
                .filter(c -> REFRESH_COOKIE.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }

    private String resolverIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            // Tomar la IP más a la DERECHA (la última que agregó el proxy de confianza).
            // La primera puede ser falsa si el cliente forjó el header.
            String[] ips = xForwardedFor.split(",");
            return ips[ips.length - 1].trim();
        }
        return request.getRemoteAddr();
    }
}