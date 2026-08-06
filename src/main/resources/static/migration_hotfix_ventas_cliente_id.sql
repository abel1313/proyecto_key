-- ============================================================
-- HOTFIX URGENTE (2026-07-24): tabla `ventas` en PROD nunca recibió
-- las columnas cliente_id / cliente_sin_registro_id agregadas al
-- entity Venta.java en su momento (commits abd9550 y c73d247) sin
-- migración asociada. Causaba: "Unknown column 'cliente_id' in
-- 'field list'" en CUALQUIER insert a ventas.
--
-- De paso cubre monto_dado (migration_venta_monto_dado.sql) y
-- codigo_reclamo/correo_reclamo/reclamado_en (migration_reclamo_venta.sql)
-- por si tampoco corrieron en prod.
--
-- Idempotente: seguro de correr aunque alguna columna/constraint ya
-- exista (la salta en vez de fallar). Correr conectado a la BD de
-- producción (inventario_key).
-- ============================================================

-- ---------- cliente_id ----------
SET @dbname = DATABASE();
SET @preparedStatement = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = 'ventas' AND COLUMN_NAME = 'cliente_id') > 0,
  'SELECT ''cliente_id ya existe, se omite''',
  'ALTER TABLE ventas ADD COLUMN cliente_id INT NULL'
));
PREPARE stmt FROM @preparedStatement;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @preparedStatement = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
    WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = 'ventas' AND CONSTRAINT_NAME = 'fk_ventas_cliente') > 0,
  'SELECT ''fk_ventas_cliente ya existe, se omite''',
  'ALTER TABLE ventas ADD CONSTRAINT fk_ventas_cliente FOREIGN KEY (cliente_id) REFERENCES clientes (id) ON DELETE SET NULL ON UPDATE CASCADE'
));
PREPARE stmt FROM @preparedStatement;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ---------- cliente_sin_registro_id ----------
SET @preparedStatement = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = 'ventas' AND COLUMN_NAME = 'cliente_sin_registro_id') > 0,
  'SELECT ''cliente_sin_registro_id ya existe, se omite''',
  'ALTER TABLE ventas ADD COLUMN cliente_sin_registro_id INT NULL'
));
PREPARE stmt FROM @preparedStatement;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @preparedStatement = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
    WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = 'ventas' AND CONSTRAINT_NAME = 'fk_ventas_csr') > 0,
  'SELECT ''fk_ventas_csr ya existe, se omite''',
  'ALTER TABLE ventas ADD CONSTRAINT fk_ventas_csr FOREIGN KEY (cliente_sin_registro_id) REFERENCES clientes_sin_registro (id) ON DELETE SET NULL ON UPDATE CASCADE'
));
PREPARE stmt FROM @preparedStatement;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ---------- monto_dado ----------
SET @preparedStatement = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = 'ventas' AND COLUMN_NAME = 'monto_dado') > 0,
  'SELECT ''monto_dado ya existe, se omite''',
  'ALTER TABLE ventas ADD COLUMN monto_dado DOUBLE NULL'
));
PREPARE stmt FROM @preparedStatement;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ---------- codigo_reclamo / correo_reclamo / reclamado_en ----------
SET @preparedStatement = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = 'ventas' AND COLUMN_NAME = 'codigo_reclamo') > 0,
  'SELECT ''codigo_reclamo ya existe, se omite''',
  'ALTER TABLE ventas ADD COLUMN codigo_reclamo VARCHAR(36) NULL'
));
PREPARE stmt FROM @preparedStatement;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @preparedStatement = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = 'ventas' AND COLUMN_NAME = 'correo_reclamo') > 0,
  'SELECT ''correo_reclamo ya existe, se omite''',
  'ALTER TABLE ventas ADD COLUMN correo_reclamo VARCHAR(150) NULL'
));
PREPARE stmt FROM @preparedStatement;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @preparedStatement = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = 'ventas' AND COLUMN_NAME = 'reclamado_en') > 0,
  'SELECT ''reclamado_en ya existe, se omite''',
  'ALTER TABLE ventas ADD COLUMN reclamado_en DATETIME NULL'
));
PREPARE stmt FROM @preparedStatement;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @preparedStatement = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
    WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = 'ventas' AND CONSTRAINT_NAME = 'uq_ventas_codigo_reclamo') > 0,
  'SELECT ''uq_ventas_codigo_reclamo ya existe, se omite''',
  'ALTER TABLE ventas ADD CONSTRAINT uq_ventas_codigo_reclamo UNIQUE (codigo_reclamo)'
));
PREPARE stmt FROM @preparedStatement;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ============================================================
-- VERIFICACIÓN
-- ============================================================
-- SHOW CREATE TABLE ventas;
