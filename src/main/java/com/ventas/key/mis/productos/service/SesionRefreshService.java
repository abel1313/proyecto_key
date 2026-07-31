package com.ventas.key.mis.productos.service;

import com.ventas.key.mis.productos.entity.SesionRefresh;
import com.ventas.key.mis.productos.repository.ISesionRefreshRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Ciclo de vida de las sesiones de refresh token (hallazgos 3, 5 y 6 de SEGURIDAD_AUTH.md).
 *
 * <p>Resuelve tres cosas que antes no tenian solucion posible con un refresh token puramente
 * criptografico:
 * <ul>
 *   <li><b>Logout real:</b> se borra la fila y el token muere en el instante, no en 7 dias.</li>
 *   <li><b>Limite absoluto de sesion:</b> {@code sessionStart} por fin se evalua contra un maximo,
 *       asi la cadena de renovaciones deja de ser infinita.</li>
 *   <li><b>Deteccion de reuso:</b> si llega un jti ya rotado se mata la familia entera, que es la
 *       senal de que el token fue robado.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SesionRefreshService {

    /** Duracion maxima de una sesion contada desde el login original, sin importar cuantas veces se renueve. */
    private static final int SESION_MAXIMA_DIAS = 30;
    /** Debe coincidir con la expiracion del refresh token que emite JwtUtil. */
    private static final int REFRESH_EXPIRA_DIAS = 7;

    private final ISesionRefreshRepository sesionRepository;

    /** Datos que el controller necesita para armar el refresh token de una sesion recien abierta. */
    public record SesionNueva(String sessionId, String jti, long sessionStartMillis) {}

    /**
     * Abre una sesion nueva (login). Genera la familia ({@code sessionId}) y el primer jti.
     */
    @Transactional
    public SesionNueva crearSesion(Integer usuarioId) {
        LocalDateTime ahora = LocalDateTime.now();
        SesionRefresh sesion = new SesionRefresh();
        sesion.setUsuarioId(usuarioId);
        sesion.setSessionId(UUID.randomUUID().toString());
        sesion.setJti(UUID.randomUUID().toString());
        sesion.setSessionStart(ahora);
        sesion.setExpira(ahora.plusDays(REFRESH_EXPIRA_DIAS));
        sesion.setCreado(ahora);
        sesionRepository.save(sesion);

        long sessionStartMillis = ahora.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
        return new SesionNueva(sesion.getSessionId(), sesion.getJti(), sessionStartMillis);
    }

    /**
     * Rota el refresh token de una sesion existente. Devuelve el jti nuevo, o vacio si la sesion
     * no es utilizable — en cuyo caso el controller debe responder 401 y limpiar la cookie.
     *
     * <p>Casos que rechazan:
     * <ul>
     *   <li>La familia no existe: la sesion se cerro (logout, cambio de contrasena) o nunca existio.</li>
     *   <li>El jti no es el vigente: <b>reuso</b> de un token ya rotado. Se borra la familia
     *       completa, porque es la senal de que alguien mas tiene una copia.</li>
     *   <li>El refresh vigente ya expiro, o la sesion supero el limite absoluto de
     *       {@value #SESION_MAXIMA_DIAS} dias.</li>
     * </ul>
     */
    @Transactional
    public Optional<String> rotar(String sessionId, String jti) {
        if (sessionId == null || jti == null) {
            return Optional.empty();
        }
        Optional<SesionRefresh> encontrada = sesionRepository.findBySessionId(sessionId);
        if (encontrada.isEmpty()) {
            log.warn("Refresh con sesion inexistente o ya cerrada, sessionId: {}", sessionId);
            return Optional.empty();
        }

        SesionRefresh sesion = encontrada.get();

        if (!sesion.getJti().equals(jti)) {
            // Token viejo que ya fue rotado => alguien conserva una copia. Se cierra la sesion
            // entera para expulsar tanto al atacante como al usuario legitimo.
            log.warn("Reuso de refresh token detectado; se cierra la sesion completa. usuarioId: {}, sessionId: {}",
                    sesion.getUsuarioId(), sessionId);
            sesionRepository.delete(sesion);
            return Optional.empty();
        }

        LocalDateTime ahora = LocalDateTime.now();

        if (ahora.isAfter(sesion.getExpira())) {
            log.info("Refresh token expirado, se cierra la sesion. usuarioId: {}", sesion.getUsuarioId());
            sesionRepository.delete(sesion);
            return Optional.empty();
        }

        if (ahora.isAfter(sesion.getSessionStart().plusDays(SESION_MAXIMA_DIAS))) {
            log.info("Sesion supero el limite absoluto de {} dias, se obliga a iniciar sesion de nuevo. usuarioId: {}",
                    SESION_MAXIMA_DIAS, sesion.getUsuarioId());
            sesionRepository.delete(sesion);
            return Optional.empty();
        }

        String jtiNuevo = UUID.randomUUID().toString();
        sesion.setJti(jtiNuevo);
        sesion.setExpira(ahora.plusDays(REFRESH_EXPIRA_DIAS));
        sesionRepository.save(sesion);
        return Optional.of(jtiNuevo);
    }

    /** Logout: el refresh token deja de servir en el acto. */
    @Transactional
    public void cerrarSesion(String sessionId) {
        if (sessionId == null) {
            return;
        }
        sesionRepository.deleteBySessionId(sessionId);
    }

    /**
     * Cierra todas las sesiones del usuario. Se llama al cambiar o restablecer la contrasena para
     * que el caso "me entraron a la cuenta, cambio la contrasena" expulse de verdad al atacante.
     */
    @Transactional
    public void cerrarTodasLasSesiones(Integer usuarioId) {
        int cerradas = sesionRepository.deleteByUsuarioId(usuarioId);
        if (cerradas > 0) {
            log.info("Se cerraron {} sesiones del usuario id: {}", cerradas, usuarioId);
        }
    }

    /** Barrido de sesiones vencidas; lo dispara SesionRefreshScheduler. */
    @Transactional
    public int limpiarExpiradas() {
        return sesionRepository.deleteExpiradas(LocalDateTime.now());
    }
}
