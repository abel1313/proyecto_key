-- ============================================================
-- Script (NO es migración): poner una categoría a los productos desde cierto id en adelante.
-- La tabla producto no guarda fecha de alta; los ids crecen en orden de alta, así que "desde esa fecha hasta ahora" = id >= @desde_id.
-- Categoría = tabla palabra_clave (menú «🏷️ Categorías»). Si no existe, se crea.
-- Se corre PRIMERO en QA. En prod solo cuando el dueño lo ordene (y con respaldo antes).
-- Pasos: 1) cambia las dos variables, 2) corre todo hasta la VISTA PREVIA, 3) revisa, 4) corre el UPDATE.
-- ============================================================

SET @categoria = 'Juguetes';     -- <-- NOMBRE de la categoría
SET @desde_id  = 1;              -- <-- id del PRIMER producto de esa fecha

-- Ayuda para encontrar el id: busca el primer producto de esa fecha por nombre
-- SELECT id, nombre FROM producto WHERE nombre LIKE '%parte del nombre%' ORDER BY id;

-- Crea la categoría si no existe
INSERT INTO palabra_clave (nombre)
SELECT @categoria FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM palabra_clave WHERE nombre = @categoria);
SET @cat_id = (SELECT id FROM palabra_clave WHERE nombre = @categoria);

-- 1) VISTA PREVIA (no cambia nada)
SELECT @cat_id AS categoria_id, COUNT(*) AS productos_desde_ese_id, SUM(palabra_clave_id IS NULL) AS sin_categoria,
       SUM(palabra_clave_id IS NOT NULL) AS ya_tienen_otra
FROM producto WHERE id >= @desde_id;
SELECT id, nombre, palabra_clave_id FROM producto WHERE id >= @desde_id ORDER BY id LIMIT 30;

-- 2) APLICAR: solo a los que NO tienen categoría. Para pisar también las que ya tienen otra, quita "AND palabra_clave_id IS NULL".
UPDATE producto SET palabra_clave_id = @cat_id WHERE id >= @desde_id AND palabra_clave_id IS NULL;
SELECT ROW_COUNT() AS actualizados;

-- VERIFICACIÓN
-- SELECT COUNT(*) FROM producto WHERE id >= @desde_id AND palabra_clave_id = @cat_id;
