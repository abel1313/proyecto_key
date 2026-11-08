-- ============================================================
-- Script (NO es migración): poner una categoría a los productos SIN categoría dados de alta desde una fecha hasta hoy.
-- Usa producto.fecha_creacion. Solo toca los que tienen palabra_clave_id = NULL (los que ya tienen otra categoría no se tocan).
-- Categoría = tabla palabra_clave (menú «🏷️ Categorías»). Si no existe, se crea.
-- Se corrió primero en QA (2026-11-08) con categoría 'Juguetes' y fecha '2026-09-12'. En prod solo cuando el dueño lo ordene (con respaldo antes).
-- Pasos: 1) cambia @categoria y @desde, 2) corre hasta la VISTA PREVIA, 3) revisa, 4) corre el UPDATE, 5) comprueba.
-- ============================================================

SET @categoria = 'Juguetes';     -- <-- NOMBRE de la categoría
SET @desde     = '2026-09-12';   -- <-- fecha de alta, desde ese día hasta hoy

-- Crea la categoría si no existe
INSERT INTO palabra_clave (nombre)
SELECT @categoria FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM palabra_clave WHERE nombre = @categoria);
SET @cat_id = (SELECT id FROM palabra_clave WHERE nombre = @categoria);
SELECT @cat_id AS categoria_id;

-- 1) VISTA PREVIA (no cambia nada)
SELECT COUNT(*) AS se_van_a_actualizar
FROM producto
WHERE fecha_creacion >= @desde AND palabra_clave_id IS NULL;

SELECT id, nombre, precio_venta, stock, fecha_creacion
FROM producto
WHERE fecha_creacion >= @desde AND palabra_clave_id IS NULL
ORDER BY fecha_creacion, id;

-- 2) APLICAR
UPDATE producto
SET palabra_clave_id = @cat_id
WHERE fecha_creacion >= @desde AND palabra_clave_id IS NULL;

SELECT ROW_COUNT() AS actualizados;     -- debe ser igual a se_van_a_actualizar

-- 3) COMPROBAR
SELECT COUNT(*) AS en_la_categoria FROM producto WHERE palabra_clave_id = @cat_id;
SELECT COUNT(*) AS faltan FROM producto WHERE fecha_creacion >= @desde AND palabra_clave_id IS NULL;   -- debe dar 0
-- Los productos con fecha_creacion vacía no entran: SELECT COUNT(*) FROM producto WHERE fecha_creacion IS NULL;
