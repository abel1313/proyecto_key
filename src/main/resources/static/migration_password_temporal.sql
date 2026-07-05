-- ============================================================
-- MODULO: Reseteo de contrasena por ADMIN con password temporal
-- ============================================================
-- Conectarse al servidor
-- sudo mysql -u root -p

-- Seleccionar la base segun el ambiente:
-- use inventario_key_qa;   <- dev y qa (misma BD)
-- use inventario_key;      <- main / prod
--
-- Estado por ambiente: back implementado en dev (2026-07-04) — pendiente correr en dev/qa/prod.
-- ============================================================

ALTER TABLE usuario_modificacion
    ADD COLUMN password_temporal TINYINT(1) NOT NULL DEFAULT 0
        COMMENT '1 = la contrasena la puso un ADMIN via reseteo, se debe cambiar en el siguiente login';
