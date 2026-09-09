package com.ventas.key.mis.productos.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Registro de auditoria de acceso — a diferencia de {@link SesionRefresh} (que es de seguridad
 * y se BORRA en logout/expiracion/reuso), esta tabla nunca se borra: una fila por login, que se
 * va actualizando mientras la sesion sigue activa.
 *
 * <p>{@code ultimaActividad} se actualiza en {@code SesionRefreshService.rotar()}, que ya corre
 * cada vez que el access token expira (~15 min) y el front sigue usando la app — sin agregar
 * ninguna llamada nueva del front. Consecuencia: la duracion de la sesion
 * ({@code ultimaActividad - fechaLogin}) se mide en bloques de ~15 minutos, no al segundo. Una
 * visita muy corta que nunca llega a refrescar el token se ve con duracion ~0 aunque haya durado
 * un par de minutos.
 */
@Entity
@Table(name = "historial_acceso", indexes = {
        @Index(name = "idx_historial_acceso_usuario", columnList = "usuario_id"),
        @Index(name = "idx_historial_acceso_session_id", columnList = "session_id"),
        @Index(name = "idx_historial_acceso_fecha_login", columnList = "fecha_login")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HistorialAcceso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false)
    private Integer usuarioId;

    /** Misma familia de sesion que {@link SesionRefresh#getSessionId()} — permite correlacionar. */
    @Column(name = "session_id", nullable = false, length = 36)
    private String sessionId;

    @Column(name = "fecha_login", nullable = false)
    private LocalDateTime fechaLogin;

    @Column(name = "ultima_actividad", nullable = false)
    private LocalDateTime ultimaActividad;
}
