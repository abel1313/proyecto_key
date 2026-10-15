-- =====================================================================================
-- QUITAR LOS DATOS DE PRUEBA DEL CATÁLOGO — SOLO inventario_key_qa
-- =====================================================================================
-- Da de BAJA (habilitado = '0', stock 0) los productos y artículos que creó
-- datos_prueba_qa_catalogo.sql (código de barras 2099000000001 a 2099000000200).
-- No hace DELETE de producto ni variantes: los pedidos y ventas de prueba pueden apuntar a
-- ellos (regla del proyecto: baja lógica, nunca DELETE).
-- Quita sus ligas de imagen (variante_imagen, producto_imagen_copy). NO borra ninguna imagen:
-- las fotos son de productos reales.
-- Se puede correr dos veces. En otra base no hace nada.
-- Probado antes de entregar (2026-09-29) en MySQL 8.0, con Safe Updates encendido como en Workbench.
-- =====================================================================================

SET NAMES utf8mb4;

SELECT IF(DATABASE() = 'inventario_key_qa', 'OK: base inventario_key_qa',
          CONCAT('ALTO: estás en ', IFNULL(DATABASE(), '(ninguna)'), ' - este script no va a cambiar nada')) AS base_actual;

SET @safe_updates_antes = @@SQL_SAFE_UPDATES;
SET SQL_SAFE_UPDATES = 0;

START TRANSACTION;

DELETE vi FROM variante_imagen vi
JOIN variantes v ON v.id = vi.variante_id
JOIN producto p ON p.id = v.producto_id
JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras LIKE '2099000%' AND LENGTH(cb.codigo_barras) = 13 AND DATABASE() = 'inventario_key_qa';

DELETE pi FROM producto_imagen_copy pi
JOIN producto p ON p.id = pi.producto_id
JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras LIKE '2099000%' AND LENGTH(cb.codigo_barras) = 13 AND DATABASE() = 'inventario_key_qa';

UPDATE variantes v
JOIN producto p ON p.id = v.producto_id
JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
SET v.habilitado = '0', v.stock = 0
WHERE cb.codigo_barras LIKE '2099000%' AND LENGTH(cb.codigo_barras) = 13 AND DATABASE() = 'inventario_key_qa';

UPDATE producto p
JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
SET p.habilitado = '0', p.stock = 0
WHERE cb.codigo_barras LIKE '2099000%' AND LENGTH(cb.codigo_barras) = 13 AND DATABASE() = 'inventario_key_qa';

COMMIT;

SET SQL_SAFE_UPDATES = @safe_updates_antes;

-- Esperado: productos_activos = 0, articulos_activos = 0, ligas_imagen = 0
SELECT SUM(p.habilitado = '1') AS productos_activos,
       (SELECT COUNT(*) FROM variantes v JOIN producto p2 ON p2.id = v.producto_id JOIN codigo_barras cb2 ON cb2.id = p2.codigo_barras_id
         WHERE cb2.codigo_barras LIKE '2099000%' AND LENGTH(cb2.codigo_barras) = 13 AND v.habilitado = '1') AS articulos_activos,
       (SELECT COUNT(*) FROM variante_imagen vi JOIN variantes v3 ON v3.id = vi.variante_id JOIN producto p3 ON p3.id = v3.producto_id
         JOIN codigo_barras cb3 ON cb3.id = p3.codigo_barras_id
         WHERE cb3.codigo_barras LIKE '2099000%' AND LENGTH(cb3.codigo_barras) = 13) AS ligas_imagen
FROM producto p
JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras LIKE '2099000%' AND LENGTH(cb.codigo_barras) = 13;
