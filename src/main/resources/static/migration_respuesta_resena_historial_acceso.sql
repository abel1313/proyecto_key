-- ============================================================
-- MODULO: Respuesta del ADMIN a resenas + Historial de acceso
-- ============================================================
-- Conectarse al servidor
-- sudo mysql -u root -p

-- Seleccionar la base segun el ambiente:
-- use inventario_key_qa;   <- dev y qa (misma BD)
-- use inventario_key;      <- main / prod
--
-- Contrato completo en CAMBIOS_FRONT.md.
-- Estado por ambiente: back implementado en dev (2026-08-11) — pendiente correr en dev/qa/prod.
-- ============================================================

-- --------------------------------------------------------------
-- 1) Respuesta del ADMIN a una resena (una sola respuesta por
--    resena, no un hilo de mensajes)
-- --------------------------------------------------------------
ALTER TABLE resena
    ADD COLUMN respuesta_admin TEXT NULL AFTER comentario,
    ADD COLUMN fecha_respuesta DATETIME NULL AFTER respuesta_admin;

-- --------------------------------------------------------------
-- 2) Historial de acceso (login + actividad de sesion)
--    Tabla de auditoria: a diferencia de sesion_refresh, esta
--    NUNCA se borra -- una fila por login, que se va actualizando
--    mientras la sesion sigue activa.
-- --------------------------------------------------------------
CREATE TABLE IF NOT EXISTS historial_acceso (
    id                INT NOT NULL AUTO_INCREMENT,
    usuario_id        INT NOT NULL,
    session_id        VARCHAR(36) NOT NULL,
    fecha_login       DATETIME NOT NULL,
    ultima_actividad  DATETIME NOT NULL,
    PRIMARY KEY (id),
    KEY idx_historial_acceso_usuario (usuario_id),
    KEY idx_historial_acceso_session_id (session_id),
    KEY idx_historial_acceso_fecha_login (fecha_login)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
