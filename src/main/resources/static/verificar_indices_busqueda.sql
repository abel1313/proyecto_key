-- Hallazgos 4, 5 y 7 de ANALISIS_RENDIMIENTO_BUSQUEDAS.md.
--
-- SOLO CONSULTA — no modifica nada. Sirve para decidir si vale la pena seguir
-- optimizando las busquedas o si con el volumen real ya esta bien asi.
--
-- IMPORTANTE: la primera linea (USE) no es opcional. Sin una base seleccionada,
-- DATABASE() devuelve NULL y las consultas a information_schema salen vacias.
-- Para produccion, cambiar solo esa linea por: USE inventario_key;

USE inventario_key_qa;


-- ============================================================
-- 1) VOLUMEN — decide el hallazgo 4 (el LIKE '%termino%')
-- ============================================================
SELECT (SELECT COUNT(*) FROM producto)        AS productos,
       (SELECT COUNT(*) FROM variantes)       AS variantes,
       (SELECT COUNT(*) FROM variante_imagen) AS variante_imagen;

-- Decenas de miles o menos -> el LIKE '%x%' es imperceptible, NO tocar nada.
-- Cientos de miles         -> pasa a ser el problema principal del buscador.


-- ============================================================
-- 2) COLLATION — decide el hallazgo 5 (quitar los LOWER())
-- ============================================================
SELECT table_name, table_collation
FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND table_name IN ('producto', 'variantes', 'variante_imagen');

-- Termina en _ci  -> los LOWER() son redundantes y ademas anulan los indices.
--                    Se pueden quitar sin cambiar el comportamiento.
-- Termina en _bin
-- o en _cs        -> NO tocar: ahi LOWER() si cambia el resultado.


-- ============================================================
-- 3) INDICES — decide el hallazgo 7
-- ============================================================
SELECT table_name, index_name, column_name, seq_in_index
FROM information_schema.statistics
WHERE table_schema = DATABASE()
  AND table_name IN ('variante_imagen', 'producto_imagen_copy')
ORDER BY table_name, index_name, seq_in_index;

-- Buscar una fila con column_name = 'variante_id' (en variante_imagen) y otra
-- con 'producto_id' (en producto_imagen_copy). Si estan, no hay nada que hacer:
-- InnoDB crea el indice solo cuando la columna es llave foranea.


-- ============================================================
-- SOLO SI EL PASO 3 MOSTRO QUE FALTAN — descomentar y ejecutar
-- ============================================================
-- CREATE INDEX idx_variante_imagen_variante ON variante_imagen (variante_id);
-- CREATE INDEX idx_producto_imagen_producto ON producto_imagen_copy (producto_id);
--
-- Aceleran el EXISTS (SELECT 1 FROM VarianteImagen vi WHERE vi.variante = v) que
-- se evalua fila por fila en el catalogo publico, y el findByVarianteIdIn del
-- armado de miniaturas. CREATE INDEX bloquea escrituras mientras se construye
-- (poco tiempo si la tabla es chica).
