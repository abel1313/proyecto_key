package com.ventas.key.mis.productos.scheduler;

import com.ventas.key.mis.productos.entity.Usuario;
import com.ventas.key.mis.productos.jwt.JwtUtil;
import com.ventas.key.mis.productos.service.ReconciliacionImagenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ImagenScheduler {

    private final ReconciliacionImagenService reconciliacionImagenService;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    @Value("${reconciliacion.admin-username:}")
    private String adminUsername;

    @Value("${reconciliacion.admin-password:}")
    private String adminPassword;

    @Scheduled(cron = "0 0 0 * * *")
    public void reconciliarTodos() {
        if (adminUsername.isBlank() || adminPassword.isBlank()) {
            log.warn("reconciliacion.admin-username/password no configurados, se omite la reconciliacion automatica");
            return;
        }
        log.info("Iniciando reconciliacion automatica de imagenes (medianoche)");
        autenticarYEjecutar(() -> reconciliacionImagenService.reconciliarTodos());
    }

    @Scheduled(cron = "0 0 4 * * *")
    public void limpiarDiscoDia() {
        log.info("Iniciando limpieza de archivos huerfanos en disco (4 AM)");
        // La limpieza solo opera en disco local, no necesita autenticacion con el microservicio
        reconciliacionImagenService.limpiarDiscoDia();
    }

    private void autenticarYEjecutar(Runnable tarea) {
        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(adminUsername, adminPassword));
            Usuario usuario = (Usuario) auth.getPrincipal();
            String jwt = jwtUtil.generateToken(usuario, usuario.getId());

            UsernamePasswordAuthenticationToken authConToken =
                    new UsernamePasswordAuthenticationToken(usuario, jwt, usuario.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(authConToken);

            tarea.run();
        } catch (Exception e) {
            log.error("Error en tarea programada de imagenes: {}", e.getMessage());
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}