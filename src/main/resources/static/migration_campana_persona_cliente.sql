-- ============================================================
-- Migración: a qué cliente pertenece cada código de campaña (2026-11-08). Requiere migration_campana_personas.sql.
--
-- Para que al escanear el QR se llenen solos los datos del cliente, la persona (campana_persona) guarda el cliente de su primera compra:
--   cliente_id               → clientes (cliente registrado)
--   cliente_sin_registro_id  → clientes_sin_registro (el que se creó en el modal «Registrar datos del cliente»)
-- Si ese cliente se borra, la columna queda en NULL (ON DELETE SET NULL): el código y sus datos de contacto siguen valiendo.
-- Las personas que ya existen quedan con las dos columnas en NULL; se rellenan solas en su siguiente compra.
-- Solo AGREGA columnas, índices y llaves; no borra ni cambia datos. Idempotente: se puede correr dos veces.
-- ============================================================

SET @sql = IF((SELECT COUNT(*) FROM information_schema.columns
               WHERE table_schema = DATABASE() AND table_name = 'campana_persona' AND column_name = 'cliente_id') = 0,
    'ALTER TABLE campana_persona ADD COLUMN cliente_id INT NULL',
    'SELECT ''campana_persona.cliente_id ya existe''');
PREPARE st FROM @sql; EXECUTE st; DEALLOCATE PREPARE st;

SET @sql = IF((SELECT COUNT(*) FROM information_schema.columns
               WHERE table_schema = DATABASE() AND table_name = 'campana_persona' AND column_name = 'cliente_sin_registro_id') = 0,
    'ALTER TABLE campana_persona ADD COLUMN cliente_sin_registro_id INT NULL',
    'SELECT ''campana_persona.cliente_sin_registro_id ya existe''');
PREPARE st FROM @sql; EXECUTE st; DEALLOCATE PREPARE st;

SET @sql = IF((SELECT COUNT(*) FROM information_schema.statistics
               WHERE table_schema = DATABASE() AND table_name = 'campana_persona' AND index_name = 'idx_campana_persona_cliente') = 0,
    'ALTER TABLE campana_persona ADD INDEX idx_campana_persona_cliente (cliente_id)',
    'SELECT ''idx_campana_persona_cliente ya existe''');
PREPARE st FROM @sql; EXECUTE st; DEALLOCATE PREPARE st;

SET @sql = IF((SELECT COUNT(*) FROM information_schema.statistics
               WHERE table_schema = DATABASE() AND table_name = 'campana_persona' AND index_name = 'idx_campana_persona_sin_registro') = 0,
    'ALTER TABLE campana_persona ADD INDEX idx_campana_persona_sin_registro (cliente_sin_registro_id)',
    'SELECT ''idx_campana_persona_sin_registro ya existe''');
PREPARE st FROM @sql; EXECUTE st; DEALLOCATE PREPARE st;

SET @sql = IF((SELECT COUNT(*) FROM information_schema.table_constraints
               WHERE table_schema = DATABASE() AND table_name = 'campana_persona' AND constraint_name = 'fk_cper_cliente') = 0,
    'ALTER TABLE campana_persona ADD CONSTRAINT fk_cper_cliente FOREIGN KEY (cliente_id) REFERENCES clientes (id) ON DELETE SET NULL',
    'SELECT ''fk_cper_cliente ya existe''');
PREPARE st FROM @sql; EXECUTE st; DEALLOCATE PREPARE st;

SET @sql = IF((SELECT COUNT(*) FROM information_schema.table_constraints
               WHERE table_schema = DATABASE() AND table_name = 'campana_persona' AND constraint_name = 'fk_cper_sin_registro') = 0,
    'ALTER TABLE campana_persona ADD CONSTRAINT fk_cper_sin_registro FOREIGN KEY (cliente_sin_registro_id) REFERENCES clientes_sin_registro (id) ON DELETE SET NULL',
    'SELECT ''fk_cper_sin_registro ya existe''');
PREPARE st FROM @sql; EXECUTE st; DEALLOCATE PREPARE st;

-- ============================================================
-- VERIFICACIÓN
-- ============================================================
-- SELECT column_name, column_type, is_nullable FROM information_schema.columns
--  WHERE table_schema = DATABASE() AND table_name = 'campana_persona' AND column_name IN ('cliente_id', 'cliente_sin_registro_id');
-- SELECT constraint_name FROM information_schema.table_constraints
--  WHERE table_schema = DATABASE() AND table_name = 'campana_persona' AND constraint_type = 'FOREIGN KEY';
