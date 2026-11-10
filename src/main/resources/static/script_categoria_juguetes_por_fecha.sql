-- ============================================================
-- Script (NO es migración): ponerle la categoría JUGUETES a los productos dados de alta desde una fecha (2026-11-09).
--
-- Se corre a mano en MySQL, TODO de una vez y en la MISMA sesión (usa variables @...). Orden: 0) categoría, 1) vista previa (no cambia nada),
-- 2) respaldo, 3) aplicar, 4) verificar. Para deshacer: sección 5.
-- Reglas:
--   • Solo los productos con fecha_creacion >= @desde. Los anteriores a la columna (agosto 2026) tienen fecha NULL y NO entran (ver la vista previa).
--   • Por defecto SOLO los que no tienen categoría (palabra_clave_id IS NULL): no pisa categorías que ya pusiste. Para pisarlas, ver la nota en 3.
--   • No toca los productos internos del sistema (es_catalogo_interno = 1).
--   • Las categorías se guardan en MAYÚSCULAS (así las normaliza el sistema): 'JUGUETES'.
-- Es seguro repetirlo: no duplica la categoría y el UPDATE solo toca los que siguen sin categoría.
-- Después: limpiar el caché (DELETE /v1/admin/cache con el administrador) o esperar 1 hora para verlo en Catálogo → Buscar producto.
-- ============================================================

SET @categoria = 'JUGUETES';              -- <-- nombre de la categoría (en MAYÚSCULAS)
SET @desde     = '2026-11-01 00:00:00';   -- <-- AQUÍ LA FECHA: entran los productos dados de alta desde ese momento (AAAA-MM-DD HH:MM:SS)

-- 0) La categoría (se crea si no existe)
INSERT INTO palabra_clave (nombre)
SELECT @categoria FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM palabra_clave WHERE nombre = @categoria);
SET @cat_id = (SELECT id FROM palabra_clave WHERE nombre = @categoria);
SELECT @cat_id AS id_categoria, @categoria AS categoria;

-- 1) VISTA PREVIA (no cambia nada): cuántos entran y cómo están hoy
SELECT COUNT(*)                                                                       AS productos_desde_la_fecha,
       SUM(CASE WHEN palabra_clave_id IS NULL THEN 1 ELSE 0 END)                      AS sin_categoria_entran,
       SUM(CASE WHEN palabra_clave_id = @cat_id THEN 1 ELSE 0 END)                    AS ya_son_juguetes,
       SUM(CASE WHEN palabra_clave_id IS NOT NULL AND palabra_clave_id <> @cat_id THEN 1 ELSE 0 END) AS con_otra_categoria_no_se_tocan
FROM producto
WHERE fecha_creacion >= @desde AND es_catalogo_interno = 0;

SELECT COUNT(*) AS sin_fecha_de_alta_no_entran FROM producto WHERE fecha_creacion IS NULL;

SELECT p.id, p.nombre, p.fecha_creacion, pc.nombre AS categoria_actual
FROM producto p LEFT JOIN palabra_clave pc ON pc.id = p.palabra_clave_id
WHERE p.fecha_creacion >= @desde AND p.es_catalogo_interno = 0
ORDER BY p.id LIMIT 30;

-- 2) RESPALDO de lo que se va a tocar (para poder deshacer)
CREATE TABLE IF NOT EXISTS respaldo_categoria_juguetes (
    producto_id        INT      NOT NULL,
    categoria_anterior INT      NULL,
    respaldado_en      DATETIME NOT NULL,
    PRIMARY KEY (producto_id, respaldado_en)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
SET @respaldo = NOW();
INSERT INTO respaldo_categoria_juguetes (producto_id, categoria_anterior, respaldado_en)
SELECT id, palabra_clave_id, @respaldo
FROM producto
WHERE fecha_creacion >= @desde AND es_catalogo_interno = 0 AND palabra_clave_id IS NULL;

-- 3) APLICAR: solo a los que no tienen categoría.
--    (Para pisar TAMBIÉN la categoría de los que ya tienen otra: quitar «AND palabra_clave_id IS NULL» aquí y en el respaldo de arriba.)
UPDATE producto
SET palabra_clave_id = @cat_id
WHERE fecha_creacion >= @desde AND es_catalogo_interno = 0 AND palabra_clave_id IS NULL;

-- 4) VERIFICAR: cuántos quedaron y las categorías con más productos
SELECT COUNT(*) AS juguetes_desde_la_fecha FROM producto WHERE palabra_clave_id = @cat_id AND fecha_creacion >= @desde;
SELECT pc.nombre AS categoria, COUNT(*) AS productos
FROM producto p JOIN palabra_clave pc ON pc.id = p.palabra_clave_id
GROUP BY pc.nombre ORDER BY productos DESC LIMIT 10;

-- 5) DESHACER (solo si hace falta; corre con @cat_id y @respaldo de esta misma sesión, o pon el valor de respaldado_en a mano):
-- UPDATE producto p JOIN respaldo_categoria_juguetes r ON r.producto_id = p.id AND r.respaldado_en = @respaldo
--    SET p.palabra_clave_id = r.categoria_anterior
--  WHERE p.palabra_clave_id = @cat_id;
-- SELECT respaldado_en, COUNT(*) FROM respaldo_categoria_juguetes GROUP BY respaldado_en;   -- para ver las corridas guardadas
