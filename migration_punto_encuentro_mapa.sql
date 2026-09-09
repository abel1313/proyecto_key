-- ============================================================
-- Migración: ubicación exacta del punto de encuentro de "Entregas por zona" (2026-09-09)
--
-- Motivo: al programar el viaje semanal a una zona solo se guardaba `punto_encuentro` en texto
-- libre ("Centro de Zacazonapan, frente a la iglesia"). El cliente que no conoce la zona no
-- tiene forma de llegar con eso: el link "Cómo llegar" de su pedido apuntaba a `latitud` /
-- `longitud`, que son las coordenadas de SU PROPIA casa (capturadas en el checkout), no del
-- lugar al que tiene que ir. Ahora el admin marca el punto en un mapa al programar y esas
-- coordenadas viajan al pedido de cada cliente avisado.
--
-- Columnas nuevas, ambas NULL: los pedidos ya programados sin mapa siguen mostrando solo el
-- texto de `punto_encuentro`, como hasta ahora.
--
-- Idempotente: se puede correr varias veces sin error.
-- Aplica a inventario_key_qa (dev/qa) y a inventario_key (main/producción).
-- ============================================================

SET @db := DATABASE();

SET @sql := IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'pedido' AND COLUMN_NAME = 'latitud_encuentro') = 0,
    'ALTER TABLE pedido ADD COLUMN latitud_encuentro DOUBLE NULL COMMENT ''Latitud del punto de encuentro del viaje a la zona (NO es la casa del cliente)''',
    'SELECT ''pedido.latitud_encuentro ya existe'' AS aviso');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'pedido' AND COLUMN_NAME = 'longitud_encuentro') = 0,
    'ALTER TABLE pedido ADD COLUMN longitud_encuentro DOUBLE NULL COMMENT ''Longitud del punto de encuentro del viaje a la zona (NO es la casa del cliente)''',
    'SELECT ''pedido.longitud_encuentro ya existe'' AS aviso');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Verificación
SELECT COLUMN_NAME, DATA_TYPE, IS_NULLABLE
  FROM information_schema.COLUMNS
 WHERE TABLE_SCHEMA = DATABASE()
   AND TABLE_NAME = 'pedido'
   AND COLUMN_NAME IN ('latitud_encuentro', 'longitud_encuentro');
