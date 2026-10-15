-- =====================================================================================
-- QUITAR LO QUE CREAN LAS PRUEBAS AUTOMÁTICAS (e2e/ del front) — SOLO inventario_key_qa
-- =====================================================================================
-- Cada corrida de las pruebas E2E da de alta modelos y artículos de verdad en QA. Todos llevan un
-- código de barras "E2E" + 13 dígitos (la hora en milisegundos, ver e2e/support/datos.ts).
--
-- Da de BAJA (habilitado = '0', stock 0) esos productos y sus artículos. No hace DELETE: los
-- pedidos y ventas de prueba pueden apuntar a ellos (regla del proyecto: baja lógica, nunca DELETE).
-- Las pruebas no suben imágenes, así que no hay nada de imágenes que limpiar.
--
-- Se puede correr las veces que se quiera. En otra base no hace nada.
-- Probado antes de entregar (2026-09-30) en MySQL 8.0, dos veces y con Safe Updates encendido.
-- =====================================================================================

SET NAMES utf8mb4;

SELECT IF(DATABASE() = 'inventario_key_qa', 'OK: base inventario_key_qa',
          CONCAT('ALTO: estás en ', IFNULL(DATABASE(), '(ninguna)'), ' - este script no va a cambiar nada')) AS base_actual;

-- Cuántos hay antes (solo lectura).
SELECT COUNT(*) AS modelos_e2e_habilitados
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras REGEXP '^E2E[0-9]{13}$' AND p.habilitado = '1';

SET @safe_updates_antes = @@SQL_SAFE_UPDATES;
SET SQL_SAFE_UPDATES = 0;

START TRANSACTION;

UPDATE variantes v
JOIN producto p ON p.id = v.producto_id
JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
SET v.habilitado = '0', v.stock = 0
WHERE cb.codigo_barras REGEXP '^E2E[0-9]{13}$' AND DATABASE() = 'inventario_key_qa';

UPDATE producto p
JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
SET p.habilitado = '0', p.stock = 0
WHERE cb.codigo_barras REGEXP '^E2E[0-9]{13}$' AND DATABASE() = 'inventario_key_qa';

COMMIT;

SET SQL_SAFE_UPDATES = @safe_updates_antes;

-- Verificación: en inventario_key_qa las dos deben dar 0.
SELECT COUNT(*) AS modelos_e2e_habilitados
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras REGEXP '^E2E[0-9]{13}$' AND p.habilitado = '1';

SELECT COUNT(*) AS articulos_e2e_habilitados
FROM variantes v JOIN producto p ON p.id = v.producto_id JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras REGEXP '^E2E[0-9]{13}$' AND v.habilitado = '1';
