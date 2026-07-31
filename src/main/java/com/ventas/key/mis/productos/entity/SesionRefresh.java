package com.ventas.key.mis.productos.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Sesion de refresh token — hace el refresh token *stateful* (hallazgos 3, 5 y 6 de
 * SEGURIDAD_AUTH.md). Antes el refresh token solo se validaba criptograficamente, asi que:
 * el logout no lo invalidaba, la cadena de renovaciones era infinita, y cambiar la contrasena
 * no expulsaba a nadie.
 *
 * <p>Hay <b>una fila por sesion</b>, no una por token. La fila guarda el {@code jti} del refresh
 * token vigente y se actualiza en cada rotacion; el {@code sessionId} identifica a la familia
 * completa y nunca cambia. Con eso se detecta el reuso: si llega un refresh token cuyo
 * {@code sessionId} existe pero cuyo {@code jti} ya no es el vigente, es un token viejo que
 * alguien guardo — se mata la sesion entera.
 */
@Entity
@Table(name = "sesion_refresh", indexes = {
        @Index(name = "idx_sesion_refresh_session_id", columnList = "session_id"),
        @Index(name = "idx_sesion_refresh_usuario", columnList = "usuario_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SesionRefresh {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false)
    private Integer usuarioId;

    /** Identificador de la familia de sesion; se propaga sin cambios en cada rotacion. */
    @Column(name = "session_id", nullable = false, length = 36)
    private String sessionId;

    /** jti del unico refresh token valido ahora mismo para esta sesion. Cambia en cada rotacion. */
    @Column(name = "jti", nullable = false, unique = true, length = 36)
    private String jti;

    /** Momento del login original — contra esto se mide el limite absoluto de sesion. */
    @Column(name = "session_start", nullable = false)
    private LocalDateTime sessionStart;

    /** Expiracion del refresh token vigente (7 dias); se recalcula en cada rotacion. */
    @Column(name = "expira", nullable = false)
    private LocalDateTime expira;

    @Column(name = "creado", nullable = false)
    private LocalDateTime creado;
}
