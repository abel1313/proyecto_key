-- ============================================================
-- MÓDULO: Verificación de correo del Usuario (mejora 15, PLAN_MEJORAS.md)
-- ============================================================
-- Conectarse al servidor
-- sudo mysql -u root -p

-- Seleccionar la base según el ambiente:
-- use inventario_key_qa;   ← dev y qa (misma BD)
-- use inventario_key;      ← main / prod
--
-- Estado por ambiente: back ya implementado en dev (2026-07-03) — pendiente correr en dev/qa/prod.
-- Correr junto con migration_datos_completos_cliente.sql (mismo módulo).
--
-- IMPORTANTE — sin grandfathering (decisión del usuario, PLAN_MEJORAS.md mejora 15 punto 5):
-- correo_verificado nace en 0 (default) para TODOS, incluidos los usuarios ya existentes.
-- En su próximo login serán forzados a verificar su correo, igual que un usuario nuevo.
-- ============================================================

ALTER TABLE usuario_modificacion
    ADD COLUMN correo_verificado          TINYINT(1) NOT NULL DEFAULT 0
        COMMENT '0 = correo sin verificar (bloquea login), 1 = verificado con codigo',
    ADD COLUMN codigo_verificacion        VARCHAR(6)  NULL
        COMMENT 'Codigo de 6 digitos vigente, null si ya se verifico o no se ha solicitado',
    ADD COLUMN codigo_verificacion_expira DATETIME    NULL
        COMMENT 'Vencimiento del codigo (15 minutos desde el envio)';
