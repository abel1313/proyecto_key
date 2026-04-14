package com.ventas.key.mis.productos.controller;

import com.ventas.key.mis.productos.entity.Usuario;
import com.ventas.key.mis.productos.jwt.JwtUtil;
import com.ventas.key.mis.productos.models.AuthRequest;
import com.ventas.key.mis.productos.models.AuthResponse;
import com.ventas.key.mis.productos.service.LoginRateLimiterService;
import com.ventas.key.mis.productos.service.RegistroService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@Slf4j
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authManager;
    private final JwtUtil jwtUtil;
    private final RegistroService registroService;
    private final LoginRateLimiterService rateLimiterService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody AuthRequest request, HttpServletRequest httpRequest) {
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
            String token = jwtUtil.generateToken((UserDetails) auth.getPrincipal(), usr.getId());
            return ResponseEntity.ok(new AuthResponse(token));
        } catch (BadCredentialsException e) {
            log.warn("Intento de login fallido para usuario: {} desde IP: {}", request.getUserName(), clientIp);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Credenciales inválidas");
        } catch (Exception e) {
            log.error("Error inesperado en login: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error al procesar la solicitud");
        }
    }

    private String resolverIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            // Puede venir una lista separada por comas: tomar solo la primera (IP real del cliente)
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
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

}
