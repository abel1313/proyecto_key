-- ============================================================
-- Versión IDEMPOTENTE de migration_boletos_rifa_v3.sql -- segura de correr
-- aunque ya se haya ejecutado antes.
-- Agrega, solo si falta: boletos_rifa.descartado
-- ============================================================

SET @dbname = DATABASE();

SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE table_schema = @dbname AND table_name = 'boletos_rifa' AND column_name = 'descartado');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE boletos_rifa ADD COLUMN descartado TINYINT(1) NOT NULL DEFAULT 0',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ============================================================
-- VERIFICACIÓN
-- ============================================================
-- SHOW CREATE TABLE boletos_rifa;
