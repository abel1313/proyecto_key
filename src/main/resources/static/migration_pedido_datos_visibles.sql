-- ============================================================
-- Migración: cuál dato se muestra del pedido — el del cliente o el de quien recibe (2026-11-16).
--
-- Agrega pedidos.datos_visibles VARCHAR(10) NULL con 'CLIENTE' o 'RECEPTOR'. NULL = como hasta hoy (se muestran los dos).
-- Solo AGREGA una columna; no cambia ni borra datos. Idempotente: se puede correr dos veces.
-- Correrla ANTES de desplegar el back que la usa (las consultas de pedidos leen esa columna).
-- ============================================================

SET @sql = IF((SELECT COUNT(*) FROM information_schema.columns
               WHERE table_schema = DATABASE() AND table_name = 'pedidos' AND column_name = 'datos_visibles') = 0,
    'ALTER TABLE pedidos ADD COLUMN datos_visibles VARCHAR(10) NULL',
    'SELECT ''pedidos.datos_visibles ya existe''');
PREPARE st FROM @sql; EXECUTE st; DEALLOCATE PREPARE st;

-- Verificación (debe dar 1):
SELECT COUNT(*) AS columna_creada FROM information_schema.columns
WHERE table_schema = DATABASE() AND table_name = 'pedidos' AND column_name = 'datos_visibles';
