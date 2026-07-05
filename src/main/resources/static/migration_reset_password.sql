-- ============================================================
-- MÓDULO: Restablecer contraseña olvidada (código de 6 dígitos)
-- ============================================================
-- Conectarse al servidor
-- sudo mysql -u root -p

-- Seleccionar la base según el ambiente:
-- use inventario_key_qa;   ← dev y qa (misma BD)
-- use inventario_key;      ← main / prod
--
-- Estado por ambiente: pendiente de correr en los 3 ambientes.
-- Si la columna ya existe (error 1060 "Duplicate column name"), no hace falta correrlo —
-- ya paso antes con la migracion de habilitado en variantes, revisar con DESCRIBE primero.
-- ============================================================

ALTER TABLE usuario_modificacion
    ADD COLUMN codigo_reset_password        VARCHAR(6) NULL
        COMMENT 'Codigo de 6 digitos vigente, null si no hay solicitud pendiente',
    ADD COLUMN codigo_reset_password_expira DATETIME   NULL
        COMMENT 'Vencimiento del codigo (15 minutos desde el envio)';
