-- Hallazgo 7 de ANALISIS_RENDIMIENTO_BUSQUEDAS.md — indices para las busquedas.
--
-- Este script NO se ejecuta solo: primero se VERIFICA y despues se decide.
-- Correr contra inventario_key_qa (dev/qa) o inventario_key (produccion).

-- ============================================================
-- PASO 1 — ver que indices existen hoy
-- ============================================================

SHOW INDEX FROM variante_imagen;
SHOW INDEX FROM producto_imagen_copy;
SHOW INDEX FROM variantes;
SHOW INDEX FROM producto;

-- Que buscar en la salida: una fila cuyo Column_name sea 'variante_id'
-- (o 'producto_id' en producto_imagen_copy). Si no aparece, falta el indice.
--
-- Nota: las llaves foraneas en InnoDB crean su indice automaticamente. Si las
-- tablas se crearon con FK declarada, es probable que el indice YA exista y no
-- haya nada que hacer. Por eso se verifica antes.


-- ============================================================
-- PASO 2 — cuantas filas hay (decide si vale la pena optimizar mas)
-- ============================================================

SELECT
    (SELECT COUNT(*) FROM producto)         AS total_productos,
    (SELECT COUNT(*) FROM variantes)        AS total_variantes,
    (SELECT COUNT(*) FROM variante_imagen)  AS total_variante_imagen;

-- Con unos pocos miles de filas, el LIKE '%termino%' (hallazgo 4) es
-- imperceptible y NO vale la pena tocarlo. Con cientos de miles, pasa a ser el
-- problema principal del buscador.


-- ============================================================
-- PASO 3 — collation (decide el hallazgo 5: quitar los LOWER())
-- ============================================================

SELECT table_name, table_collation
FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND table_name IN ('producto', 'variantes');

-- Si la collation termina en _ci (case-insensitive), los LOWER() de las
-- comparaciones de igualdad son REDUNDANTES y ademas impiden usar el indice.
-- Quitarlos no cambiaria el comportamiento. Si termina en _bin o _cs, NO tocar.


-- ============================================================
-- PASO 4 — solo si el PASO 1 mostro que faltan
-- ============================================================

-- CREATE INDEX idx_variante_imagen_variante ON variante_imagen (variante_id);
-- CREATE INDEX idx_producto_imagen_producto ON producto_imagen_copy (producto_id);

-- Estos aceleran el EXISTS (SELECT 1 FROM VarianteImagen vi WHERE vi.variante = v)
-- que se evalua fila por fila en las queries de catalogo publico, y el
-- findByVarianteIdIn del armado de miniaturas.
--
-- Ejecutar en horario de poco movimiento: CREATE INDEX bloquea escrituras en la
-- tabla mientras se construye (poco tiempo si la tabla es chica).
