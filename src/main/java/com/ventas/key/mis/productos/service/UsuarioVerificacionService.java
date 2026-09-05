package com.ventas.key.mis.productos.service;

import com.ventas.key.mis.productos.entity.Cliente;
import com.ventas.key.mis.productos.entity.Usuario;
import com.ventas.key.mis.productos.exeption.ExceptionCodigoInvalido;
import com.ventas.key.mis.productos.exeption.ExceptionDuplicado;
import com.ventas.key.mis.productos.models.CambioCorreoPendienteResponseDto;
import com.ventas.key.mis.productos.repository.IClienteRepository;
import com.ventas.key.mis.productos.repository.IUsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

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
    /** Intentos fallidos permitidos por codigo antes de invalidarlo — corta la fuerza bruta de los 6 digitos. */
    private static final int MAX_INTENTOS_CODIGO = 5;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final IUsuarioRepository usuarioRepository;
    private final IClienteRepository clienteRepository;
    private final EmailService emailService;
    private final ClienteServiceImpl clienteService;

    @Transactional
    public void enviarCodigoVerificacion(String usernameOEmail) {
        enviarCodigoVerificacion(usernameOEmail, true);
    }

    /**
     * @param forzarNuevo false = reutiliza el código vigente si todavía no expiró, en vez de
     *                    generar y mandar otro (evita invalidar en silencio uno que el usuario
     *                    todavía no alcanzó a usar cuando el envío lo dispara la propia
     *                    aplicacion -- cargar la pantalla, un login fallido por correo sin
     *                    verificar, el modal de verificacion del admin -- en vez de un click
     *                    explicito de "reenviar". Mismo patron que ya usa
     *                    {@link #solicitarCambioCorreo(Usuario, String)} para el cambio de
     *                    correo. true = siempre manda uno nuevo (boton explicito de reenviar).
     */
    @Transactional
    public void enviarCodigoVerificacion(String usernameOEmail, boolean forzarNuevo) {
        Usuario usuario = buscarPorUsernameOEmail(usernameOEmail);
        if (usuario.getEmail() == null || usuario.getEmail().isBlank()) {
            throw new RuntimeException("El usuario no tiene correo registrado");
        }
        if (Boolean.TRUE.equals(usuario.getCorreoVerificado())) {
            throw new RuntimeException("El correo ya esta verificado");
        }
        boolean yaVigente = !forzarNuevo
                && usuario.getCodigoVerificacion() != null
                && usuario.getCodigoVerificacionExpira() != null
                && LocalDateTime.now().isBefore(usuario.getCodigoVerificacionExpira());
        if (yaVigente) {
            return;
        }
        String codigo = String.format("%06d", RANDOM.nextInt(1_000_000));
        usuario.setCodigoVerificacion(codigo);
        usuario.setCodigoVerificacionExpira(LocalDateTime.now().plusMinutes(CODIGO_EXPIRA_MINUTOS));
        usuario.setIntentosCodigoVerificacion(0);
        usuarioRepository.save(usuario);
        emailService.enviarCodigoVerificacion(usuario.getEmail(), codigo);
    }

    /**
     * noRollbackFor: igual que en PasswordResetService — el contador de intentos se persiste en
     * esta misma transaccion antes de lanzar, y con el rollback por defecto el incremento se
     * perderia y el limite nunca aplicaria.
     */
    @Transactional(noRollbackFor = ExceptionCodigoInvalido.class)
    public void verificarCorreo(String usernameOEmail, String codigo) {
        Usuario usuario = buscarPorUsernameOEmail(usernameOEmail);
        if (Boolean.TRUE.equals(usuario.getCorreoVerificado())) {
            return;
        }
        // Expiracion antes que la comparacion del codigo: si ya expiro no se quema intento, y el
        // usuario legitimo recibe el mensaje que le dice que pida uno nuevo.
        if (usuario.getCodigoVerificacion() == null
                || usuario.getCodigoVerificacionExpira() == null
                || LocalDateTime.now().isAfter(usuario.getCodigoVerificacionExpira())) {
            throw new ExceptionCodigoInvalido("El codigo de verificacion expiro, solicita uno nuevo");
        }
        if (!usuario.getCodigoVerificacion().equals(codigo)) {
            registrarIntentoFallido(usuario);
            throw new ExceptionCodigoInvalido("Codigo de verificacion invalido");
        }
        usuario.setCorreoVerificado(true);
        usuario.setCodigoVerificacion(null);
        usuario.setCodigoVerificacionExpira(null);
        usuario.setIntentosCodigoVerificacion(0);
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
     * Quema un intento del codigo de verificacion vigente y, al agotarlos, lo invalida por
     * completo — obliga a pedir uno nuevo por /enviar-codigo-verificacion o
     * /solicitar-cambio-correo. Sirve para los dos flujos porque ambos comparten el mismo par de
     * campos {@code codigoVerificacion} / {@code codigoVerificacionExpira}.
     */
    private void registrarIntentoFallido(Usuario usuario) {
        int intentos = usuario.getIntentosCodigoVerificacion() == null ? 0 : usuario.getIntentosCodigoVerificacion();
        intentos++;
        usuario.setIntentosCodigoVerificacion(intentos);

        if (intentos >= MAX_INTENTOS_CODIGO) {
            usuario.setCodigoVerificacion(null);
            usuario.setCodigoVerificacionExpira(null);
            usuario.setIntentosCodigoVerificacion(0);
            log.warn("Codigo de verificacion invalidado por agotar {} intentos fallidos, usuario id: {}",
                    MAX_INTENTOS_CODIGO, usuario.getId());
        }
        usuarioRepository.save(usuario);
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
        // Sin este chequeo, dos cuentas podian terminar con el mismo email (nunca se validaba
        // que el correo nuevo no perteneciera ya a otro usuario) -- encontrado 2026-09-04 junto
        // con el hotfix de mensajes de error crudos de JPA.
        usuarioRepository.findFirstByEmailIgnoreCase(correoNuevo).ifPresent(otro -> {
            if (!otro.getId().equals(usuario.getId())) {
                throw new RuntimeException("Ese correo ya está en uso por otra cuenta");
            }
        });
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
        usuario.setIntentosCodigoVerificacion(0);
        usuarioRepository.save(usuario);
        boolean correoEnviado = emailService.enviarCodigoVerificacion(correoNuevo, codigo);
        if (!correoEnviado) {
            // Si el correo no salio de verdad, no dejamos el codigo pendiente guardado - si no,
            // el siguiente intento lo veria como "yaVigente" y le diria al usuario que revise su
            // bandeja cuando nunca le llego nada. @Transactional hace rollback del save de arriba.
            throw new RuntimeException("No se pudo enviar el correo de verificacion, intenta de nuevo en unos minutos");
        }
        return true;
    }

    /**
     * Confirma el codigo del cambio de correo pendiente - solo aqui se actualiza el email real,
     * y solo si el codigo es correcto. En cualquier otro caso (codigo invalido, expirado, o
     * nunca se llama) el email real se queda como estaba.
     */
    // noRollbackFor incluye ExceptionDuplicado ademas de ExceptionCodigoInvalido: cuando el
    // correo pendiente ya quedo en conflicto, el metodo limpia ese estado (setCorreoPendiente a
    // null, etc.) y guarda ANTES de lanzar la excepcion -- sin esto en la lista, el rollback
    // automatico de RuntimeException deshacia esa limpieza y la cuenta quedaba atascada igual.
    @Transactional(noRollbackFor = {ExceptionCodigoInvalido.class, ExceptionDuplicado.class})
    public void confirmarCambioCorreo(Usuario usuario, String codigo) {
        if (usuario.getCorreoPendiente() == null) {
            throw new RuntimeException("No hay un cambio de correo pendiente");
        }
        if (usuario.getCodigoVerificacion() == null
                || usuario.getCodigoVerificacionExpira() == null
                || LocalDateTime.now().isAfter(usuario.getCodigoVerificacionExpira())) {
            throw new ExceptionCodigoInvalido("El codigo de verificacion expiro, solicita uno nuevo");
        }
        if (!usuario.getCodigoVerificacion().equals(codigo)) {
            registrarIntentoFallido(usuario);
            throw new ExceptionCodigoInvalido("Codigo de verificacion invalido");
        }
        // Mismo chequeo que solicitarCambioCorreo, pero repetido aqui porque un correoPendiente
        // ya pudo quedar guardado en conflicto ANTES de ese fix (o alguien mas tomo ese correo
        // mientras el codigo seguia vigente) -- sin esto, confirmar revienta el commit igual.
        // Se limpia el pendiente para no dejar la cuenta atascada mostrando un cambio que nunca
        // va a poder confirmarse.
        Optional<Usuario> otro = usuarioRepository.findFirstByEmailIgnoreCase(usuario.getCorreoPendiente());
        if (otro.isPresent() && !otro.get().getId().equals(usuario.getId())) {
            usuario.setCorreoPendiente(null);
            usuario.setCodigoVerificacion(null);
            usuario.setCodigoVerificacionExpira(null);
            usuario.setIntentosCodigoVerificacion(0);
            usuarioRepository.save(usuario);
            throw new ExceptionDuplicado("Ese correo ya está en uso por otra cuenta, solicita el cambio con uno distinto");
        }
        usuario.setEmail(usuario.getCorreoPendiente());
        usuario.setCorreoVerificado(true);
        usuario.setCorreoPendiente(null);
        usuario.setCodigoVerificacion(null);
        usuario.setCodigoVerificacionExpira(null);
        usuario.setIntentosCodigoVerificacion(0);
        usuarioRepository.save(usuario);

        // Antes este flujo (cambio de correo del Usuario, ej. admin editando "Actualizar
        // usuario") dejaba el Cliente vinculado con el correo VIEJO -- el otro sentido
        // (ClienteServiceImpl.verificarCorreo, cambio desde "Mi perfil") sí sincronizaba hacia
        // Usuario.email, pero este no sincronizaba hacia Cliente.correoElectronico. Resultado:
        // dependiendo de POR DONDE se cambiara el correo, quedaba consistente o no.
        //
        // UPDATE directo (no cliente.setX + save()) a proposito: save() dispara Bean Validation
        // de la entidad COMPLETA, y si el cliente tiene numeroTelefonico vacio/mal formado (dato
        // viejo, de antes de esa validacion) revienta el commit aunque no se este tocando ese
        // campo para nada (encontrado 2026-09-05, hotfix urgente en prod).
        if (usuario.getCliente() != null) {
            clienteRepository.actualizarCorreoElectronico(usuario.getCliente().getId(), usuario.getEmail());
        }
    }

    /** Variante self-service: identifica al usuario por el username del JWT (Authentication.getName()). */
    @Transactional
    public boolean solicitarCambioCorreo(String usernameActual, String correoNuevo) {
        return solicitarCambioCorreo(buscarPorUsernameOEmail(usernameActual), correoNuevo);
    }

    /**
     * Variante self-service: identifica al usuario por el username del JWT (Authentication.getName()).
     * Lleva el mismo noRollbackFor porque, al ser una llamada interna, la anotacion de la variante
     * que recibe el Usuario no la aplica el proxy — la que manda es la de este punto de entrada.
     */
    @Transactional(noRollbackFor = {ExceptionCodigoInvalido.class, ExceptionDuplicado.class})
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
