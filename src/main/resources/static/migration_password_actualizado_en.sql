-- ============================================================
-- MODULO: Invalidar access token vigente al cambiar contrasena
-- ============================================================
-- Conectarse al servidor
-- sudo mysql -u root -p

-- Seleccionar la base segun el ambiente:
-- use inventario_key_qa;   <- dev y qa (misma BD)
-- use inventario_key;      <- main / prod
--
-- Estado por ambiente: back implementado en dev (2026-08-16) — pendiente correr en dev/qa/prod.
-- ============================================================

ALTER TABLE usuario_modificacion
    ADD COLUMN password_actualizado_en DATETIME NULL
        COMMENT 'Momento del ultimo cambio de contrasena; los access tokens (JWT) emitidos antes se rechazan aunque no hayan expirado';
