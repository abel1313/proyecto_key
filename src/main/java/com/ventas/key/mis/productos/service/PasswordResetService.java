package com.ventas.key.mis.productos.service;

import com.ventas.key.mis.productos.entity.Usuario;
import com.ventas.key.mis.productos.exeption.ExceptionCodigoInvalido;
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
    /** Intentos fallidos permitidos por codigo antes de invalidarlo — corta la fuerza bruta de los 6 digitos. */
    private static final int MAX_INTENTOS_CODIGO = 5;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final IUsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final SesionRefreshService sesionRefreshService;

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
            usuario.setIntentosCodigoReset(0);
            usuarioRepository.save(usuario);
            emailService.enviarCodigoResetPassword(email, codigo);
        }, () -> log.info("Solicitud de reset de password para correo no registrado: {}", email));
    }

    /**
     * noRollbackFor: el contador de intentos fallidos se persiste en esta misma transaccion antes
     * de lanzar la excepcion. Con el rollback por defecto el incremento se perderia y el limite
     * de intentos nunca llegaria a aplicarse.
     */
    @Transactional(noRollbackFor = ExceptionCodigoInvalido.class)
    public void restablecerPassword(String email, String codigo, String nuevaPassword) {
        // Mismo mensaje en todos los caminos de fallo: no revelar si el correo existe ni si el
        // codigo era correcto pero expiro.
        Usuario usuario = usuarioRepository.findFirstByEmailIgnoreCase(email)
                .orElseThrow(() -> new ExceptionCodigoInvalido("Codigo invalido o expirado"));

        if (usuario.getCodigoResetPassword() == null
                || usuario.getCodigoResetPasswordExpira() == null
                || LocalDateTime.now().isAfter(usuario.getCodigoResetPasswordExpira())) {
            throw new ExceptionCodigoInvalido("Codigo invalido o expirado");
        }

        if (!usuario.getCodigoResetPassword().equals(codigo)) {
            registrarIntentoFallido(usuario);
            throw new ExceptionCodigoInvalido("Codigo invalido o expirado");
        }

        usuario.setPassword(passwordEncoder.encode(nuevaPassword));
        usuario.setCodigoResetPassword(null);
        usuario.setCodigoResetPasswordExpira(null);
        usuario.setIntentosCodigoReset(0);
        usuario.setPasswordTemporal(false);
        usuario.setPasswordActualizadoEn(LocalDateTime.now());
        usuarioRepository.save(usuario);

        // Caso "me entraron a la cuenta y cambio la contrasena": sin esto el atacante seguia
        // dentro 7 dias mas con su refresh token.
        sesionRefreshService.cerrarTodasLasSesiones(usuario.getId());
    }

    /**
     * Quema un intento del codigo vigente y, al agotarlos, lo invalida por completo — obliga a
     * pedir uno nuevo (que si esta limitado por IP en {@code /olvide-password}).
     */
    private void registrarIntentoFallido(Usuario usuario) {
        int intentos = usuario.getIntentosCodigoReset() == null ? 0 : usuario.getIntentosCodigoReset();
        intentos++;
        usuario.setIntentosCodigoReset(intentos);

        if (intentos >= MAX_INTENTOS_CODIGO) {
            usuario.setCodigoResetPassword(null);
            usuario.setCodigoResetPasswordExpira(null);
            usuario.setIntentosCodigoReset(0);
            log.warn("Codigo de reset invalidado por agotar {} intentos fallidos, usuario id: {}",
                    MAX_INTENTOS_CODIGO, usuario.getId());
        }
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
        usuario.setPasswordActualizadoEn(LocalDateTime.now());
        usuarioRepository.save(usuario);

        sesionRefreshService.cerrarTodasLasSesiones(usuario.getId());
    }
}
