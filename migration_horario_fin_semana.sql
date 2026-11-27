-- ============================================================
-- Horario de sabado y domingo del negocio (configuracion_negocio).
-- Hasta hoy el negocio solo tenia un horario (hora_apertura / hora_cierre) para todos los dias.
-- Ahora esos dos son de lunes a viernes y estas dos son de sabado y domingo.
-- Los dos en NULL = los fines de semana valen lo mismo que entre semana (nada cambia al correrlo).
-- Solo agrega columnas, no borra ni cambia datos. Idempotente. Mismo patron que migration_boletos_rifa_v2_idempotente.sql.
-- Correrla ANTES de subir el back que la usa (la entidad ya mapea las columnas).
-- ============================================================

SET @dbname = DATABASE();

SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE table_schema = @dbname AND table_name = 'configuracion_negocio' AND column_name = 'hora_apertura_fin_semana');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE configuracion_negocio ADD COLUMN hora_apertura_fin_semana DATETIME NULL',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE table_schema = @dbname AND table_name = 'configuracion_negocio' AND column_name = 'hora_cierre_fin_semana');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE configuracion_negocio ADD COLUMN hora_cierre_fin_semana DATETIME NULL',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- VERIFICACION: debe devolver 2 filas.
SELECT column_name FROM information_schema.COLUMNS
 WHERE table_schema = DATABASE() AND table_name = 'configuracion_negocio'
   AND column_name IN ('hora_apertura_fin_semana', 'hora_cierre_fin_semana');
