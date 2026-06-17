-- Tablas para el módulo de Chat en Vivo
-- Ejecutar manualmente en la BD (ddl-auto: none)

CREATE TABLE IF NOT EXISTS chat_sesion (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    sesion_id        VARCHAR(36)  NOT NULL UNIQUE,
    identificador    VARCHAR(100) NOT NULL,
    nombre_usuario   VARCHAR(100),
    estado           VARCHAR(10)  NOT NULL DEFAULT 'ACTIVA',
    fecha_inicio     DATETIME     NOT NULL,
    ultima_actividad DATETIME     NOT NULL,
    CONSTRAINT chk_estado CHECK (estado IN ('ACTIVA', 'CERRADA'))
);

CREATE TABLE IF NOT EXISTS chat_mensaje (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    sesion_id   VARCHAR(36) NOT NULL,
    remitente   VARCHAR(10) NOT NULL,
    contenido   TEXT        NOT NULL,
    timestamp   DATETIME    NOT NULL,
    CONSTRAINT fk_chat_mensaje_sesion FOREIGN KEY (sesion_id) REFERENCES chat_sesion(sesion_id),
    CONSTRAINT chk_remitente CHECK (remitente IN ('USUARIO', 'ADMIN'))
);

CREATE INDEX idx_chat_sesion_estado ON chat_sesion(estado);
CREATE INDEX idx_chat_mensaje_sesion ON chat_mensaje(sesion_id);
