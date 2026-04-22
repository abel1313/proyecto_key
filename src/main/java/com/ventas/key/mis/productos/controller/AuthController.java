package com.ventas.key.mis.productos.controller;

import com.ventas.key.mis.productos.entity.Usuario;
import com.ventas.key.mis.productos.jwt.JwtUtil;
import com.ventas.key.mis.productos.models.AuthRequest;
import com.ventas.key.mis.productos.models.AuthResponse;
import com.ventas.key.mis.productos.service.LoginRateLimiterService;
import com.ventas.key.mis.productos.service.RegistroService;
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

@RestController
@RequestMapping("/auth")
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

    private static final String REFRESH_COOKIE = "refreshToken";
    private static final int REFRESH_MAX_AGE = 60 * 60 * 24 * 7; // 7 días en segundos

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody AuthRequest request,
                                   HttpServletRequest httpRequest,
                                   HttpServletResponse response) {
        String clientIp = resolverIp(httpRequest);

        if (!rateLimiterService.tryConsume(clientIp)) {
            log.warn("Rate limit excedido para IP: {}", clientIp);
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body("Demasiados intentos fallidos. Intente de nuevo en 15 minutos.");
        }

        try {
            Authentication auth = authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUserName(), request.getPassword())
            );
            Usuario usr = (Usuario) auth.getPrincipal();
            String accessToken  = jwtUtil.generateToken((UserDetails) auth.getPrincipal(), usr.getId());
            String refreshToken = jwtUtil.generateRefreshToken((UserDetails) auth.getPrincipal(), usr.getId());

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

            String newAccessToken  = jwtUtil.generateToken(userDetails, usr.getId());
            String newRefreshToken = jwtUtil.generateRefreshToken(userDetails, usr.getId());

            agregarRefreshCookie(response, newRefreshToken);

            return ResponseEntity.ok(new AuthResponse(newAccessToken));
        } catch (Exception e) {
            log.error("Error al refrescar token: {}", e.getMessage());
            limpiarRefreshCookie(response);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("No se pudo renovar la sesión");
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse response) {
        limpiarRefreshCookie(response);
        return ResponseEntity.ok("Sesión cerrada");
    }

    @PostMapping("/registrar")
    public ResponseEntity<?> registrar(@Valid @RequestBody AuthRequest request) throws Exception {
        return ResponseEntity.ok(registroService.registrarUsuario(request.getUserName(), request.getPassword(), request.getEmail()));
    }

    @GetMapping("/validar")
    public ResponseEntity<?> validarToken(@RequestHeader("Authorization") String authHeader) {
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
        response.addHeader("Set-Cookie",
                String.format("%s=%s; Max-Age=%d; Path=/auth; HttpOnly%s",
                        REFRESH_COOKIE, refreshToken, REFRESH_MAX_AGE, secureFlag));
    }

    private void limpiarRefreshCookie(HttpServletResponse response) {
        String secureFlag = cookieSecure ? "; Secure; SameSite=None" : "; SameSite=Lax";
        response.addHeader("Set-Cookie",
                String.format("%s=; Max-Age=0; Path=/auth; HttpOnly%s",
                        REFRESH_COOKIE, secureFlag));
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
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}