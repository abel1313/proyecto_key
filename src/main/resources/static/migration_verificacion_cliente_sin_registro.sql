-- ============================================================
-- MÓDULO: Verificación de correo para cliente sin registro
--         + correo del ganador en concursantes
-- ============================================================
-- Conectarse al servidor
-- sudo mysql -u root -p

-- Seleccionar la base según el ambiente:
-- use inventario_key_qa;   ← dev y qa (misma BD)
-- use inventario_key;      ← main / prod
--
-- Estado por ambiente:
--   dev  → ✅ ya corrido (2026-07-21) — comparte BD inventario_key_qa con qa
--   qa   → ✅ ya corrido (2026-07-21) — misma BD que dev
--   prod → ✅ ya corrido (2026-07-21) — inventario_key
-- ============================================================

ALTER TABLE clientes_sin_registro
    ADD COLUMN correo_verificado          TINYINT(1) NOT NULL DEFAULT 0
        COMMENT '0 = correo sin verificar, 1 = verificado con codigo',
    ADD COLUMN codigo_verificacion        VARCHAR(6)  NULL
        COMMENT 'Codigo de 6 digitos vigente, null si ya se verifico o no se ha solicitado',
    ADD COLUMN codigo_verificacion_expira DATETIME    NULL
        COMMENT 'Vencimiento del codigo (15 minutos desde el envio)';

-- Correo del concursante congelado al importar (cliente registrado o sin registro), para poder
-- notificar por correo si resulta ganador. Puede quedar NULL si el participante solo dio telefono.
ALTER TABLE concursantes
    ADD COLUMN correo VARCHAR(150) NULL
        COMMENT 'Correo del participante al momento del import, para notificar si gana';
