-- ============================================================
-- Versión IDEMPOTENTE de migration_boletos_rifa_v2.sql -- segura de correr
-- aunque ya se haya ejecutado antes (no truena por "columna duplicada").
-- Agrega, solo si faltan:
--   - configurar_rifa.fecha_inicio_boletos / fecha_fin_boletos
--   - boletos_rifa.plataforma
-- ============================================================

SET @dbname = DATABASE();

SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE table_schema = @dbname AND table_name = 'configurar_rifa' AND column_name = 'fecha_inicio_boletos');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE configurar_rifa ADD COLUMN fecha_inicio_boletos DATE NULL',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE table_schema = @dbname AND table_name = 'configurar_rifa' AND column_name = 'fecha_fin_boletos');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE configurar_rifa ADD COLUMN fecha_fin_boletos DATE NULL',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE table_schema = @dbname AND table_name = 'boletos_rifa' AND column_name = 'plataforma');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE boletos_rifa ADD COLUMN plataforma VARCHAR(20) NULL',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ============================================================
-- VERIFICACIÓN
-- ============================================================
-- SHOW CREATE TABLE configurar_rifa;
-- SHOW CREATE TABLE boletos_rifa;
