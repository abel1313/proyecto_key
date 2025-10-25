package com.ventas.key.mis.productos.controller;

import com.ventas.key.mis.productos.jwt.JwtUtil;
import com.ventas.key.mis.productos.models.AuthRequest;
import com.ventas.key.mis.productos.models.AuthResponse;
import com.ventas.key.mis.productos.service.RegistroService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {
    @Autowired
    private AuthenticationManager authManager;

    @Autowired
    private JwtUtil jwtUtil;


    @Autowired
    private RegistroService registroService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest request) {
        Authentication auth = null;
        String token = "";
        try {
            auth = authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUserName(), request.getPassword())
            );
            token = jwtUtil.generateToken((UserDetails) auth.getPrincipal());
        }catch (Exception e){
            System.out.println(e.getMessage());
        }

        return ResponseEntity.ok(new AuthResponse(token));
    }


    @PostMapping("/registrar")
    public ResponseEntity<?> registrar(@RequestBody AuthRequest request) {
        return ResponseEntity.ok(registroService.registrarUsuario(request.getUserName(), request.getPassword(), request.getEmail()));
    }

    @GetMapping("/validar")
    public ResponseEntity<?> validarToken(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.replace("Bearer ", "");
            String username = jwtUtil.extractUsername(token);

            if (username != null && jwtUtil.validateToken(token)) {
                return ResponseEntity.ok("Token válido para usuario: " + username);
            } else {
                return ResponseEntity.status(401).body("Token inválido");
            }
        } catch (Exception e) {
            return ResponseEntity.status(401).body("Error al validar token");
        }
    }


}
