-- Hallazgos 3, 5 y 6 (SEGURIDAD_AUTH.md) — refresh token stateful.
--
-- Antes el refresh token solo se validaba criptograficamente, asi que no habia forma de matarlo:
--   * logout solo borraba la cookie, el token seguia valido 7 dias
--   * la cadena de renovaciones era infinita (sessionStart nunca se comparaba contra un maximo)
--   * cambiar la contrasena no expulsaba al que ya estaba dentro
--
-- Hay UNA fila por sesion, no una por token: guarda el jti del refresh vigente (se actualiza en
-- cada rotacion) y el session_id de la familia (no cambia nunca). Si llega un refresh cuyo
-- session_id existe pero cuyo jti ya no es el vigente, es un token robado y se borra la familia.
--
-- OJO: ddl-auto esta en 'none' en todos los perfiles, asi que esta tabla NO se crea sola —
-- hay que ejecutar este script en cada ambiente ANTES de desplegar, o el login truena.

CREATE TABLE sesion_refresh (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    usuario_id    INT          NOT NULL,
    session_id    VARCHAR(36)  NOT NULL,
    jti           VARCHAR(36)  NOT NULL,
    session_start DATETIME     NOT NULL,
    expira        DATETIME     NOT NULL,
    creado        DATETIME     NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_sesion_refresh_jti (jti),
    KEY idx_sesion_refresh_session_id (session_id),
    KEY idx_sesion_refresh_usuario (usuario_id),
    CONSTRAINT fk_sesion_refresh_usuario FOREIGN KEY (usuario_id)
        REFERENCES usuario_modificacion (id) ON DELETE CASCADE
);
