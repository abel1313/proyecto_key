package com.ventas.key.mis.productos.controller;

import com.ventas.key.mis.productos.entity.Usuario;
import com.ventas.key.mis.productos.exeption.ExceptionCodigoInvalido;
import com.ventas.key.mis.productos.jwt.JwtUtil;
import com.ventas.key.mis.productos.models.ActualizarMiPerfilRequestDto;
import com.ventas.key.mis.productos.models.AuthRequest;
import com.ventas.key.mis.productos.models.AuthResponse;
import com.ventas.key.mis.productos.models.CambiarPasswordRequest;
import com.ventas.key.mis.productos.models.CambioCorreoPendienteResponseDto;
import com.ventas.key.mis.productos.models.ConfirmarCambioCorreoRequest;
import com.ventas.key.mis.productos.models.EnviarCodigoVerificacionUsuarioRequest;
import com.ventas.key.mis.productos.models.OlvidePasswordRequest;
import com.ventas.key.mis.productos.models.RegistroRequest;
import com.ventas.key.mis.productos.models.ResponseGeneric;
import com.ventas.key.mis.productos.models.RestablecerPasswordRequest;
import com.ventas.key.mis.productos.models.SolicitarCambioCorreoRequest;
import com.ventas.key.mis.productos.models.VerificarCorreoUsuarioRequest;
import com.ventas.key.mis.productos.service.LoginRateLimiterService;
import com.ventas.key.mis.productos.service.PasswordResetService;
import com.ventas.key.mis.productos.service.RegistroService;
import com.ventas.key.mis.productos.service.SesionRefreshService;
import com.ventas.key.mis.productos.service.UsuarioServiceImpl;
import com.ventas.key.mis.productos.service.UsuarioVerificacionService;
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
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Tag(name = "Autenticacion", description = "Login, logout, registro y renovacion de tokens JWT. El refresh token se guarda en cookie HttpOnly.")
@RestController
@RequestMapping("/v1/auth")
@Slf4j
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authManager;
    private final JwtUtil jwtUtil;
    private final RegistroService registroService;
    private final PasswordResetService passwordResetService;
    private final LoginRateLimiterService rateLimiterService;
    private final UserDetailsService userDetailsService;
    private final UsuarioVerificacionService usuarioVerificacionService;
    private final UsuarioServiceImpl usuarioService;
    private final SesionRefreshService sesionRefreshService;

    @Value("${cookie.secure:true}")
    private boolean cookieSecure;

    @Value("${server.servlet.context-path:}")
    private String contextPath;

    @Value("${seguridad.rate-limit-habilitado:true}")
    private boolean rateLimitHabilitado;

    /**
     * Hallazgo 11 (SEGURIDAD_AUTH.md): CSRF esta deshabilitado y la cookie de refresh usa
     * SameSite=None, asi que un sitio de terceros puede forzar /refresh y /logout desde el
     * navegador de la victima. Con la rotacion con deteccion de reuso ya implementada (hallazgo 5)
     * eso dejo de ser una molestia y paso a ser un vector para tumbarle la sesion a alguien.
     *
     * <p>Exigir un header custom lo corta, porque un formulario cross-site no puede enviarlo sin
     * pasar por preflight CORS. <b>Viene apagado por defecto</b>: encenderlo antes de que el front
     * mande el header romperia el refresh de todos los usuarios. Prender cuando el front confirme.
     */
    @Value("${seguridad.exigir-header-refresh:false}")
    private boolean exigirHeaderRefresh;

    private static final String HEADER_ANTI_CSRF = "X-Requested-With";

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

        // Solo se CONSULTA el cupo; el intento se gasta despues, y unicamente si la autenticacion
        // falla. Antes se consumia por adelantado, asi que cinco entradas legitimas en 15 minutos
        // autobloqueaban al usuario y cinco POST con basura bloqueaban al admin a proposito.
        if (rateLimitHabilitado && !rateLimiterService.hayIntentosDisponibles(clientIp)) {
            log.warn("Rate limit por IP excedido: {}", clientIp);
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body("Demasiados intentos fallidos. Intente de nuevo en 15 minutos.");
        }
        if (rateLimitHabilitado && !rateLimiterService.hayIntentosDisponibles(usernameKey)) {
            log.warn("Rate limit por usuario excedido: {}", request.getUserName());
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body("Demasiados intentos fallidos. Intente de nuevo en 15 minutos.");
        }

        try {
            Authentication auth = authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUserName(), request.getPassword())
            );
            Usuario usr = (Usuario) auth.getPrincipal();

            // Credenciales correctas: se borra el historial de fallos del usuario. La clave por IP
            // no se limpia a proposito — la comparten registro y olvide-password, y un login
            // correcto no deberia borrar los fallos acumulados de esa IP.
            if (rateLimitHabilitado) {
                rateLimiterService.limpiarIntentos(usernameKey);
            }

            // Contrasena ya validada por authManager.authenticate() en este punto.
            // El chequeo de correo verificado va aparte (no en isEnabled()) para que una
            // contrasena incorrecta siempre responda 401, nunca el 403 de verificacion.
            if (!usr.esAdmin() && !Boolean.TRUE.equals(usr.getCorreoVerificado())) {
                log.warn("Login rechazado por correo sin verificar: {}", request.getUserName());
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("Debes verificar tu correo antes de iniciar sesión");
            }

            // La sesion se registra en BD: es lo que permite que logout, cambio de contrasena y
            // deteccion de reuso puedan invalidar el refresh token del lado del servidor.
            SesionRefreshService.SesionNueva sesion = sesionRefreshService.crearSesion(usr.getId());

            List<String> pantallas = pantallasEfectivas(usr.getId());
            List<String> pantallasEscritura = pantallasEscrituraEfectivas(usr.getId());
            List<String> pantallasAcciones = pantallasAccionesEfectivas(usr.getId());
            String accessToken  = jwtUtil.generateToken((UserDetails) auth.getPrincipal(), usr.getId(), pantallas, pantallasEscritura, pantallasAcciones);
            String refreshToken = jwtUtil.generateRefreshToken((UserDetails) auth.getPrincipal(), usr.getId(),
                    sesion.sessionStartMillis(), sesion.jti(), sesion.sessionId());

            agregarRefreshCookie(response, refreshToken);

            return ResponseEntity.ok(new AuthResponse(accessToken, Boolean.TRUE.equals(usr.getPasswordTemporal())));
        } catch (BadCredentialsException e) {
            registrarFalloLogin(clientIp, usernameKey);
            log.warn("Intento de login fallido para usuario: {} desde IP: {}", request.getUserName(), clientIp);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Credenciales inválidas");
        } catch (DisabledException e) {
            // Tambien gasta intento: el provider comprueba 'enabled' antes que la contrasena, asi
            // que sin esto seria un oraculo ilimitado de "esta cuenta existe y esta deshabilitada".
            registrarFalloLogin(clientIp, usernameKey);
            log.warn("Login rechazado, cuenta deshabilitada: {}", request.getUserName());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Cuenta deshabilitada");
        } catch (Exception e) {
            // La excepcion va como ultimo argumento para que SLF4J imprima el stack trace: con
            // solo getMessage() no habia con que diagnosticar un fallo real en produccion.
            log.error("Error inesperado en login para usuario: {}", request.getUserName(), e);
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
        if (faltaHeaderAntiCsrf(request)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Falta el header " + HEADER_ANTI_CSRF);
        }
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

            String sessionId = jwtUtil.extractSessionId(refreshToken);

            // Mismos controles que el login: sin esto, dar de baja a un usuario no cerraba su
            // sesion — seguia renovando su access token durante los 7 dias del refresh token.
            if (!usr.isEnabled()) {
                log.warn("Refresh rechazado, cuenta deshabilitada: {}", username);
                sesionRefreshService.cerrarSesion(sessionId);
                limpiarRefreshCookie(response);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Cuenta deshabilitada");
            }
            if (!usr.esAdmin() && !Boolean.TRUE.equals(usr.getCorreoVerificado())) {
                log.warn("Refresh rechazado por correo sin verificar: {}", username);
                sesionRefreshService.cerrarSesion(sessionId);
                limpiarRefreshCookie(response);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Debes verificar tu correo");
            }

            // Rotacion real: el jti anterior queda invalidado en BD. Si el que llega ya habia
            // sido rotado, rotar() cierra la sesion completa (reuso => token robado).
            Optional<String> jtiNuevo = sesionRefreshService.rotar(sessionId, jwtUtil.extractJti(refreshToken));
            if (jtiNuevo.isEmpty()) {
                limpiarRefreshCookie(response);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Sesión no válida, inicia sesión de nuevo");
            }

            long sessionStart = jwtUtil.extractSessionStart(refreshToken);
            List<String> pantallas = pantallasEfectivas(usr.getId());
            List<String> pantallasEscritura = pantallasEscrituraEfectivas(usr.getId());
            List<String> pantallasAcciones = pantallasAccionesEfectivas(usr.getId());
            String newAccessToken  = jwtUtil.generateToken(userDetails, usr.getId(), pantallas, pantallasEscritura, pantallasAcciones);
            String newRefreshToken = jwtUtil.generateRefreshToken(userDetails, usr.getId(), sessionStart,
                    jtiNuevo.get(), sessionId);

            agregarRefreshCookie(response, newRefreshToken);

            return ResponseEntity.ok(new AuthResponse(newAccessToken));
        } catch (Exception e) {
            log.error("Error al refrescar token: {}", e.getMessage());
            limpiarRefreshCookie(response);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("No se pudo renovar la sesión");
        }
    }

    @Operation(summary = "Cerrar sesion", description = "Invalida el refresh token en el servidor y limpia la cookie. A diferencia de antes, el token deja de servir en el acto aunque alguien tenga una copia.")
    @ApiResponse(responseCode = "200", description = "Sesion cerrada correctamente")
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response) {
        if (faltaHeaderAntiCsrf(request)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Falta el header " + HEADER_ANTI_CSRF);
        }
        // Borrar la cookie solo le pedia al navegador que la olvidara: el refresh token seguia
        // siendo valido 7 dias. Ahora ademas se elimina la sesion del servidor.
        String refreshToken = leerRefreshCookie(request);
        if (refreshToken != null) {
            try {
                sesionRefreshService.cerrarSesion(jwtUtil.extractSessionId(refreshToken));
            } catch (Exception e) {
                // Token ilegible o ya expirado: no hay sesion que cerrar, el logout igual procede.
                log.debug("No se pudo cerrar la sesion en el logout: {}", e.getMessage());
            }
        }
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

    @Operation(summary = "Enviar codigo de verificacion de correo (registro)", description = "Genera un codigo de 6 digitos (expira en 15 minutos) y lo envia al correo del usuario recien registrado. El usuario no puede iniciar sesion hasta verificarlo. Rate-limit independiente del de login/registro.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Codigo enviado"),
        @ApiResponse(responseCode = "400", description = "Usuario no encontrado, sin correo, o ya verificado"),
        @ApiResponse(responseCode = "429", description = "Demasiados intentos; esperar 15 minutos")
    })
    @PostMapping("/enviar-codigo-verificacion")
    public ResponseEntity<?> enviarCodigoVerificacionUsuario(@Valid @RequestBody EnviarCodigoVerificacionUsuarioRequest request,
                                                             HttpServletRequest httpRequest) {
        // Limite por IP ademas del de usuario: acepta username O email, asi que sin tope por
        // origen sirve para mandarle correos a la direccion de otra persona.
        String clientIp = resolverIp(httpRequest);
        if (rateLimitHabilitado && !rateLimiterService.tryConsume("verif-envio-ip:" + clientIp)) {
            log.warn("Rate limit de envio de codigo de verificacion excedido para IP: {}", clientIp);
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body("Demasiados intentos. Intente de nuevo en 15 minutos.");
        }
        String rateLimitKey = "verif-usr:" + request.getUserName().toLowerCase().trim();
        if (rateLimitHabilitado && !rateLimiterService.tryConsume(rateLimitKey)) {
            log.warn("Rate limit de verificacion de correo excedido para: {}", request.getUserName());
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body("Demasiados intentos. Intente de nuevo en 15 minutos.");
        }
        try {
            usuarioVerificacionService.enviarCodigoVerificacion(request.getUserName());
            return ResponseEntity.ok("Codigo enviado al correo registrado");
        } catch (Exception e) {
            log.warn("Error al enviar codigo de verificacion para {}: {}", request.getUserName(), e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @Operation(summary = "Verificar correo con codigo (registro)", description = "Valida el codigo de 6 digitos enviado al correo del usuario. Si es correcto y no expiro, marca el correo como verificado, habilita el login, y auto-crea el Cliente vinculado la primera vez. Limitado a 5 intentos por IP y 5 por usuario cada 15 minutos; ademas el propio codigo se invalida tras 5 fallos.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Correo verificado correctamente, ya puede iniciar sesion"),
        @ApiResponse(responseCode = "400", description = "Codigo invalido o expirado"),
        @ApiResponse(responseCode = "429", description = "Demasiados intentos; esperar 15 minutos")
    })
    @PostMapping("/verificar-correo")
    public ResponseEntity<?> verificarCorreoUsuario(@Valid @RequestBody VerificarCorreoUsuarioRequest request,
                                                    HttpServletRequest httpRequest) {
        // Endpoint publico: sin este limite el codigo de 6 digitos se puede probar por fuerza
        // bruta y activar una cuenta registrada con el correo de otra persona.
        String clientIp = resolverIp(httpRequest);
        if (rateLimitHabilitado && !rateLimiterService.tryConsume("verif-cod-ip:" + clientIp)) {
            log.warn("Rate limit de verificar-correo excedido para IP: {}", clientIp);
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body("Demasiados intentos. Intente de nuevo en 15 minutos.");
        }
        String usuarioKey = "verif-cod-usr:" + request.getUserName().toLowerCase().trim();
        if (rateLimitHabilitado && !rateLimiterService.tryConsume(usuarioKey)) {
            log.warn("Rate limit de verificar-correo excedido para el usuario: {}", request.getUserName());
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body("Demasiados intentos. Intente de nuevo en 15 minutos.");
        }
        try {
            usuarioVerificacionService.verificarCorreo(request.getUserName(), request.getCodigo());
            return ResponseEntity.ok("Correo verificado correctamente");
        } catch (ExceptionCodigoInvalido e) {
            log.warn("Error al verificar correo para {}: {}", request.getUserName(), e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            // Mensaje generico: e.getMessage() aqui puede ser "Usuario no encontrado", que en un
            // endpoint publico permite enumerar que usernames existen.
            log.warn("Error al verificar correo para {}: {}", request.getUserName(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Codigo de verificacion invalido");
        }
    }

    @Operation(summary = "Solicitar reseteo de contrasena", description = "Envia un codigo de 6 digitos al correo (vence en 15 minutos). Responde 200 exista o no la cuenta, para no revelar si un correo esta registrado.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Si el correo existe, se envio el codigo"),
        @ApiResponse(responseCode = "429", description = "Demasiados intentos; esperar 15 minutos")
    })
    @PostMapping("/olvide-password")
    public ResponseEntity<?> olvidePassword(@Valid @RequestBody OlvidePasswordRequest request,
                                            HttpServletRequest httpRequest) {
        String clientIp = resolverIp(httpRequest);
        if (rateLimitHabilitado && !rateLimiterService.tryConsume(clientIp)) {
            log.warn("Rate limit de reseteo de password excedido para IP: {}", clientIp);
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body("Demasiados intentos. Intente de nuevo en 15 minutos.");
        }
        passwordResetService.solicitarReset(request.getEmail());
        return ResponseEntity.ok("Si el correo esta registrado, se envio un codigo de verificacion");
    }

    @Operation(summary = "Restablecer contrasena con codigo", description = "Valida el codigo de 6 digitos enviado por correo y, si es correcto y no expiro, actualiza la contrasena. Limitado a 5 intentos por IP y 5 por correo cada 15 minutos; ademas el propio codigo se invalida tras 5 fallos.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Contrasena actualizada correctamente"),
        @ApiResponse(responseCode = "400", description = "Codigo invalido o expirado"),
        @ApiResponse(responseCode = "429", description = "Demasiados intentos; esperar 15 minutos")
    })
    @PostMapping("/restablecer-password")
    public ResponseEntity<?> restablecerPassword(@Valid @RequestBody RestablecerPasswordRequest request,
                                                 HttpServletRequest httpRequest) {
        // Sin este limite el codigo de 6 digitos se puede probar por fuerza bruta durante sus
        // 15 minutos de vida (1,000,000 de combinaciones, sin traba) => toma de cuenta.
        String clientIp = resolverIp(httpRequest);
        if (rateLimitHabilitado && !rateLimiterService.tryConsume("reset-ip:" + clientIp)) {
            log.warn("Rate limit de restablecer-password excedido para IP: {}", clientIp);
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body("Demasiados intentos. Intente de nuevo en 15 minutos.");
        }
        String emailKey = "reset-mail:" + request.getEmail().toLowerCase().trim();
        if (rateLimitHabilitado && !rateLimiterService.tryConsume(emailKey)) {
            log.warn("Rate limit de restablecer-password excedido para el correo: {}", request.getEmail());
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body("Demasiados intentos. Intente de nuevo en 15 minutos.");
        }
        try {
            passwordResetService.restablecerPassword(request.getEmail(), request.getCodigo(), request.getNuevaPassword());
            return ResponseEntity.ok("Contrasena actualizada correctamente");
        } catch (ExceptionCodigoInvalido e) {
            log.warn("Error al restablecer password para {}: {}", request.getEmail(), e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            // Mensaje generico: no devolver al cliente el getMessage() de un fallo inesperado
            // (NPE, error de BD), que puede exponer detalle interno.
            log.error("Error inesperado al restablecer password para {}", request.getEmail(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Codigo invalido o expirado");
        }
    }

    @Operation(summary = "Cambiar contrasena estando logueado", description = "Requiere sesion valida (JWT). Pide la contrasena actual en vez de codigo por correo, ya re-autentica al usuario.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Contrasena actualizada correctamente"),
        @ApiResponse(responseCode = "400", description = "Contrasena actual incorrecta"),
        @ApiResponse(responseCode = "401", description = "No autenticado")
    })
    @PutMapping("/cambiar-password")
    public ResponseEntity<?> cambiarPassword(@Valid @RequestBody CambiarPasswordRequest request,
                                             Authentication authentication) {
        try {
            passwordResetService.cambiarPassword(authentication.getName(), request.getPasswordActual(), request.getNuevaPassword());
            return ResponseEntity.ok("Contrasena actualizada correctamente");
        } catch (Exception e) {
            log.warn("Error al cambiar password para {}: {}", authentication.getName(), e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @Operation(summary = "Actualizar mi perfil (usuario logueado)", description = "El propio usuario autenticado actualiza su username. Nunca toca la contrasena (PUT /v1/auth/cambiar-password) ni el email (POST /v1/auth/solicitar-cambio-correo + confirmar-cambio-correo) - esos van por endpoints separados con su propia validacion.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Perfil actualizado correctamente"),
        @ApiResponse(responseCode = "400", description = "Datos invalidos"),
        @ApiResponse(responseCode = "401", description = "No autenticado")
    })
    @PutMapping("/mi-perfil")
    public ResponseEntity<?> actualizarMiPerfil(@Valid @RequestBody ActualizarMiPerfilRequestDto request,
                                                Authentication authentication) {
        try {
            usuarioService.actualizarMiPerfil(authentication.getName(), request);
            return ResponseEntity.ok("Perfil actualizado correctamente");
        } catch (Exception e) {
            log.warn("Error al actualizar perfil para {}: {}", authentication.getName(), e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    // ── Cambio de MI PROPIO correo (self-service) — verificar antes de guardar ──
    // El email real no cambia hasta confirmar-cambio-correo con el codigo correcto.

    @Operation(summary = "Solicitar cambio de mi correo", description = "Manda un codigo de 6 digitos al correo NUEVO (no al actual). El correo real no cambia todavia - solo se guarda como pendiente hasta confirmar el codigo.")
    @PostMapping("/solicitar-cambio-correo")
    public ResponseEntity<ResponseGeneric<String>> solicitarCambioCorreo(@Valid @RequestBody SolicitarCambioCorreoRequest request,
                                                    Authentication authentication) {
        try {
            boolean enviado = usuarioVerificacionService.solicitarCambioCorreo(authentication.getName(), request.getCorreoNuevo());
            String mensaje = enviado
                    ? "Codigo enviado al correo nuevo"
                    : "Ya tienes un codigo vigente enviado a ese correo, revisa tu bandeja";
            return ResponseEntity.ok(new ResponseGeneric<>(mensaje));
        } catch (Exception e) {
            log.warn("Error al solicitar cambio de correo para {}: {}", authentication.getName(), e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ResponseGeneric<>(null, e.getMessage()));
        }
    }

    @Operation(summary = "Estado de mi cambio de correo pendiente", description = "Consulta si hay un cambio de correo en proceso (codigo enviado, sin confirmar) y su expiracion real. Pensado para que el front restaure el estado del modal tras un refresh de pagina sin depender de sessionStorage/localStorage - el back es la unica fuente de verdad.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "pendiente=false si no hay cambio en curso o el codigo ya expiro")
    })
    @GetMapping("/cambio-correo-pendiente")
    public ResponseEntity<ResponseGeneric<CambioCorreoPendienteResponseDto>> obtenerCambioCorreoPendiente(
            Authentication authentication) {
        return ResponseEntity.ok(new ResponseGeneric<>(
                usuarioVerificacionService.obtenerCambioCorreoPendiente(authentication.getName())));
    }

    @Operation(summary = "Confirmar cambio de mi correo", description = "Valida el codigo de 6 digitos. Si es correcto, recien ahi se actualiza el correo real; si no, el correo real se queda como estaba. Limitado a 5 intentos por usuario cada 15 minutos; ademas el propio codigo se invalida tras 5 fallos.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Correo actualizado correctamente"),
        @ApiResponse(responseCode = "400", description = "Codigo invalido o expirado"),
        @ApiResponse(responseCode = "429", description = "Demasiados intentos; esperar 15 minutos")
    })
    @PostMapping("/confirmar-cambio-correo")
    public ResponseEntity<ResponseGeneric<String>> confirmarCambioCorreo(@Valid @RequestBody ConfirmarCambioCorreoRequest request,
                                                    Authentication authentication) {
        // Requiere sesion valida, asi que solo se puede atacar la cuenta propia — impacto bajo,
        // pero es el mismo agujero de fuerza bruta del codigo y se cierra igual.
        String usuarioKey = "cambio-correo:" + authentication.getName().toLowerCase().trim();
        if (rateLimitHabilitado && !rateLimiterService.tryConsume(usuarioKey)) {
            log.warn("Rate limit de confirmar-cambio-correo excedido para: {}", authentication.getName());
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(new ResponseGeneric<>(null, "Demasiados intentos. Intente de nuevo en 15 minutos."));
        }
        try {
            usuarioVerificacionService.confirmarCambioCorreo(authentication.getName(), request.getCodigo());
            return ResponseEntity.ok(new ResponseGeneric<>("Correo actualizado correctamente"));
        } catch (Exception e) {
            log.warn("Error al confirmar cambio de correo para {}: {}", authentication.getName(), e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ResponseGeneric<>(null, e.getMessage()));
        }
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
            // Un refresh token no sirve para autenticar (el filtro JWT los rechaza), asi que
            // responder "valido" aqui era enganoso y convertia al endpoint en un oraculo.
            if (jwtUtil.validateToken(token) && !jwtUtil.isRefreshToken(token)) {
                return ResponseEntity.ok("Token válido");
            }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token inválido");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token inválido");
        }
    }

    /**
     * Los dos endpoints que se autentican con cookie (refresh y logout) son los unicos alcanzables
     * por CSRF. Un formulario cross-site no puede mandar un header custom sin preflight, y el
     * preflight lo bloquea CORS.
     */
    private boolean faltaHeaderAntiCsrf(HttpServletRequest request) {
        return exigirHeaderRefresh && request.getHeader(HEADER_ANTI_CSRF) == null;
    }

    /** Rutas (Submenu.ruta) efectivas del usuario -- se meten al JWT para el PantallaGuard/menu dinamico. */
    private List<String> pantallasEfectivas(int usuarioId) {
        return usuarioService.submenusEfectivos(usuarioId).stream()
                .map(com.ventas.key.mis.productos.entity.Submenu::getRuta)
                .collect(Collectors.toList());
    }

    /** Subconjunto de {@link #pantallasEfectivas} en las que el usuario ademas puede ESCRIBIR --
     * Fase 2 de permisos de accion (2026-08-27), ver UsuarioServiceImpl.submenusEscritura. */
    private List<String> pantallasEscrituraEfectivas(int usuarioId) {
        return usuarioService.submenusEscritura(usuarioId).stream()
                .map(com.ventas.key.mis.productos.entity.Submenu::getRuta)
                .collect(Collectors.toList());
    }

    /** Acciones puntuales dentro de una pantalla que el usuario puede usar (Fase 3 de permisos,
     * piloto en Modelos 2026-08-27), formato "ruta:clave" -- ver {@link #pantallasEscrituraEfectivas}. */
    private List<String> pantallasAccionesEfectivas(int usuarioId) {
        return usuarioService.accionesEfectivas(usuarioId).stream()
                .map(a -> a.getSubmenu().getRuta() + ":" + a.getClave())
                .collect(Collectors.toList());
    }

    /** Gasta un intento en las dos claves del login (IP y usuario) — solo tras un fallo real. */
    private void registrarFalloLogin(String clientIp, String usernameKey) {
        if (!rateLimitHabilitado) {
            return;
        }
        rateLimiterService.registrarFallo(clientIp);
        rateLimiterService.registrarFallo(usernameKey);
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