package com.ventas.key.mis.productos.service;

import com.ventas.key.mis.productos.entity.Usuario;
import com.ventas.key.mis.productos.repository.IUsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetService {

    private static final int CODIGO_EXPIRA_MINUTOS = 15;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final IUsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    /**
     * No revela si el correo existe o no en el sistema — siempre "tiene exito" desde afuera,
     * para no dejar enumerar cuentas por correo.
     */
    @Transactional
    public void solicitarReset(String email) {
        usuarioRepository.findFirstByEmailIgnoreCase(email).ifPresentOrElse(usuario -> {
            String codigo = String.format("%06d", RANDOM.nextInt(1_000_000));
            usuario.setCodigoResetPassword(codigo);
            usuario.setCodigoResetPasswordExpira(LocalDateTime.now().plusMinutes(CODIGO_EXPIRA_MINUTOS));
            usuarioRepository.save(usuario);
            emailService.enviarCodigoResetPassword(email, codigo);
        }, () -> log.info("Solicitud de reset de password para correo no registrado: {}", email));
    }

    @Transactional
    public void restablecerPassword(String email, String codigo, String nuevaPassword) {
        Usuario usuario = usuarioRepository.findFirstByEmailIgnoreCase(email)
                .orElseThrow(() -> new RuntimeException("Codigo invalido o expirado"));

        if (usuario.getCodigoResetPassword() == null || !usuario.getCodigoResetPassword().equals(codigo)) {
            throw new RuntimeException("Codigo invalido o expirado");
        }
        if (usuario.getCodigoResetPasswordExpira() == null
                || LocalDateTime.now().isAfter(usuario.getCodigoResetPasswordExpira())) {
            throw new RuntimeException("Codigo invalido o expirado");
        }

        usuario.setPassword(passwordEncoder.encode(nuevaPassword));
        usuario.setCodigoResetPassword(null);
        usuario.setCodigoResetPasswordExpira(null);
        usuario.setPasswordTemporal(false);
        usuarioRepository.save(usuario);
    }

    /**
     * Cambiar contrasena estando logueado — re-autentica con la contrasena actual en vez de
     * codigo por correo, ya que el usuario ya tiene una sesion JWT valida.
     */
    @Transactional
    public void cambiarPassword(String username, String passwordActual, String nuevaPassword) {
        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (!passwordEncoder.matches(passwordActual, usuario.getPassword())) {
            throw new RuntimeException("La contrasena actual es incorrecta");
        }

        usuario.setPassword(passwordEncoder.encode(nuevaPassword));
        usuario.setPasswordTemporal(false);
        usuarioRepository.save(usuario);
    }
}
