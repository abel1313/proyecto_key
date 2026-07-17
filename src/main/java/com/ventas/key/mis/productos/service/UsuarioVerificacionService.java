package com.ventas.key.mis.productos.service;

import com.ventas.key.mis.productos.entity.Usuario;
import com.ventas.key.mis.productos.models.CambioCorreoPendienteResponseDto;
import com.ventas.key.mis.productos.repository.IUsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

/**
 * Verificacion de correo del Usuario (login) al registrarse — mejora 15, PLAN_MEJORAS.md.
 * Mismo patron de codigo de 6 digitos que ya usa ClienteServiceImpl para el correo del Cliente,
 * pero aqui ademas se auto-crea el Cliente vinculado la primera vez que se verifica.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UsuarioVerificacionService {

    private static final int CODIGO_EXPIRA_MINUTOS = 15;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final IUsuarioRepository usuarioRepository;
    private final EmailService emailService;
    private final ClienteServiceImpl clienteService;

    @Transactional
    public void enviarCodigoVerificacion(String usernameOEmail) {
        Usuario usuario = buscarPorUsernameOEmail(usernameOEmail);
        if (usuario.getEmail() == null || usuario.getEmail().isBlank()) {
            throw new RuntimeException("El usuario no tiene correo registrado");
        }
        if (Boolean.TRUE.equals(usuario.getCorreoVerificado())) {
            throw new RuntimeException("El correo ya esta verificado");
        }
        String codigo = String.format("%06d", RANDOM.nextInt(1_000_000));
        usuario.setCodigoVerificacion(codigo);
        usuario.setCodigoVerificacionExpira(LocalDateTime.now().plusMinutes(CODIGO_EXPIRA_MINUTOS));
        usuarioRepository.save(usuario);
        emailService.enviarCodigoVerificacion(usuario.getEmail(), codigo);
    }

    @Transactional
    public void verificarCorreo(String usernameOEmail, String codigo) {
        Usuario usuario = buscarPorUsernameOEmail(usernameOEmail);
        if (Boolean.TRUE.equals(usuario.getCorreoVerificado())) {
            return;
        }
        if (usuario.getCodigoVerificacion() == null || !usuario.getCodigoVerificacion().equals(codigo)) {
            throw new RuntimeException("Codigo de verificacion invalido");
        }
        if (usuario.getCodigoVerificacionExpira() == null
                || LocalDateTime.now().isAfter(usuario.getCodigoVerificacionExpira())) {
            throw new RuntimeException("El codigo de verificacion expiro, solicita uno nuevo");
        }
        usuario.setCorreoVerificado(true);
        usuario.setCodigoVerificacion(null);
        usuario.setCodigoVerificacionExpira(null);
        usuarioRepository.save(usuario);

        // Auto-alta del Cliente vinculado — solo la primera vez (si ya tiene uno, no se toca).
        if (usuario.getCliente() == null) {
            clienteService.crearClienteDesdeRegistro(usuario, usuario.getEmail());
        }
    }

    private Usuario buscarPorUsernameOEmail(String usernameOEmail) {
        return usuarioRepository.findByUsername(usernameOEmail)
                .or(() -> usuarioRepository.findFirstByEmailIgnoreCase(usernameOEmail))
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
    }

    /**
     * Cambio de correo (admin editando a otro usuario, o el propio usuario) - patron
     * verificar-antes-de-guardar: el correo real NO se toca aqui, solo se guarda como
     * correoPendiente + se manda el codigo a esa direccion nueva. Si el codigo nunca se
     * confirma, el correo real nunca cambio.
     *
     * Si ya hay un codigo vigente para el MISMO correo nuevo (no expiro), no se reenvia correo -
     * se reutiliza el que ya tiene, para evitar que reintentos/doble-click manden varios correos
     * con codigos distintos (el ultimo invalidaria a los anteriores y confundiria al usuario).
     * Devuelve true si mando un correo nuevo, false si reutilizo uno ya vigente.
     */
    @Transactional
    public boolean solicitarCambioCorreo(Usuario usuario, String correoNuevo) {
        if (correoNuevo == null || correoNuevo.isBlank()) {
            throw new RuntimeException("El correo nuevo es requerido");
        }
        if (correoNuevo.equalsIgnoreCase(usuario.getEmail())) {
            throw new RuntimeException("Ese ya es el correo actual");
        }
        boolean yaVigente = correoNuevo.equalsIgnoreCase(usuario.getCorreoPendiente())
                && usuario.getCodigoVerificacionExpira() != null
                && LocalDateTime.now().isBefore(usuario.getCodigoVerificacionExpira());
        if (yaVigente) {
            return false;
        }
        String codigo = String.format("%06d", RANDOM.nextInt(1_000_000));
        usuario.setCorreoPendiente(correoNuevo);
        usuario.setCodigoVerificacion(codigo);
        usuario.setCodigoVerificacionExpira(LocalDateTime.now().plusMinutes(CODIGO_EXPIRA_MINUTOS));
        usuarioRepository.save(usuario);
        emailService.enviarCodigoVerificacion(correoNuevo, codigo);
        return true;
    }

    /**
     * Confirma el codigo del cambio de correo pendiente - solo aqui se actualiza el email real,
     * y solo si el codigo es correcto. En cualquier otro caso (codigo invalido, expirado, o
     * nunca se llama) el email real se queda como estaba.
     */
    @Transactional
    public void confirmarCambioCorreo(Usuario usuario, String codigo) {
        if (usuario.getCorreoPendiente() == null) {
            throw new RuntimeException("No hay un cambio de correo pendiente");
        }
        if (usuario.getCodigoVerificacion() == null || !usuario.getCodigoVerificacion().equals(codigo)) {
            throw new RuntimeException("Codigo de verificacion invalido");
        }
        if (usuario.getCodigoVerificacionExpira() == null
                || LocalDateTime.now().isAfter(usuario.getCodigoVerificacionExpira())) {
            throw new RuntimeException("El codigo de verificacion expiro, solicita uno nuevo");
        }
        usuario.setEmail(usuario.getCorreoPendiente());
        usuario.setCorreoVerificado(true);
        usuario.setCorreoPendiente(null);
        usuario.setCodigoVerificacion(null);
        usuario.setCodigoVerificacionExpira(null);
        usuarioRepository.save(usuario);
    }

    /** Variante self-service: identifica al usuario por el username del JWT (Authentication.getName()). */
    @Transactional
    public boolean solicitarCambioCorreo(String usernameActual, String correoNuevo) {
        return solicitarCambioCorreo(buscarPorUsernameOEmail(usernameActual), correoNuevo);
    }

    /** Variante self-service: identifica al usuario por el username del JWT (Authentication.getName()). */
    @Transactional
    public void confirmarCambioCorreo(String usernameActual, String codigo) {
        confirmarCambioCorreo(buscarPorUsernameOEmail(usernameActual), codigo);
    }

    /**
     * Estado de un cambio de correo pendiente (self-service). El back es la unica fuente de
     * verdad: no depende de que el front recuerde nada en sessionStorage/localStorage tras un
     * refresh, y devuelve la expiracion real (no una estimacion de 15 min contada del lado
     * cliente). Si el codigo ya expiro, se considera como "no pendiente" para el front aunque el
     * dato siga en BD hasta el proximo solicitar-cambio-correo (que lo sobreescribe).
     */
    public CambioCorreoPendienteResponseDto obtenerCambioCorreoPendiente(String usernameActual) {
        return obtenerCambioCorreoPendiente(buscarPorUsernameOEmail(usernameActual));
    }

    /** Admin: mismo estado, pero de OTRO usuario identificado por id (ver UsuarioServiceImpl). */
    public CambioCorreoPendienteResponseDto obtenerCambioCorreoPendiente(Usuario usuario) {
        boolean expirado = usuario.getCodigoVerificacionExpira() == null
                || LocalDateTime.now().isAfter(usuario.getCodigoVerificacionExpira());
        if (usuario.getCorreoPendiente() == null || expirado) {
            return new CambioCorreoPendienteResponseDto(false, null, null);
        }
        return new CambioCorreoPendienteResponseDto(
                true, usuario.getCorreoPendiente(), usuario.getCodigoVerificacionExpira());
    }
}
