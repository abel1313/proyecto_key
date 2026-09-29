-- ============================================================
-- Migración: precio propio por artículo (2026-09-29, hotfix de precios)
--
-- Hasta hoy el precio vivía solo en el producto y todos sus artículos (tallas/colores) lo
-- heredaban: el botón 💲 de la tarjeta cambiaba el precio de TODOS. El dueño necesita cambiar el
-- precio de UN artículo (por ejemplo, a un cliente que se lleva mucho), sin tocar los demás.
--
-- Dos columnas en `variantes`, siempre juntas:
--   precio_venta  NULL = el artículo usa el precio del producto (como hasta hoy)
--   precio_rebaja el precio con descuento propio (0 = sin descuento). Solo cuenta si
--                 precio_venta no es NULL.
--
-- Backend: PUT / DELETE /v1/precios/articulo/{varianteId} (hexagonal/precio).
-- Sin esta migración el back NO ARRANCA bien: la entidad Variantes ya mapea las dos columnas y
-- cualquier consulta de artículos falla con "Unknown column". Correrla ANTES del deploy.
--
-- No mueve ningún precio: todos los artículos nacen con NULL y siguen cobrando el del producto.
-- Idempotente: cada columna se agrega solo si no existe.
-- ============================================================

SET @existe := (SELECT COUNT(*) FROM information_schema.columns
                WHERE table_schema = DATABASE() AND table_name = 'variantes' AND column_name = 'precio_venta');
SET @sql := IF(@existe = 0,
    'ALTER TABLE variantes ADD COLUMN precio_venta DOUBLE NULL COMMENT ''Precio propio del articulo; NULL = usa el del producto''',
    'SELECT ''precio_venta ya existe''');
PREPARE st FROM @sql; EXECUTE st; DEALLOCATE PREPARE st;

SET @existe := (SELECT COUNT(*) FROM information_schema.columns
                WHERE table_schema = DATABASE() AND table_name = 'variantes' AND column_name = 'precio_rebaja');
SET @sql := IF(@existe = 0,
    'ALTER TABLE variantes ADD COLUMN precio_rebaja DOUBLE NULL COMMENT ''Descuento propio del articulo (0 = sin descuento); solo cuenta si precio_venta no es NULL''',
    'SELECT ''precio_rebaja ya existe''');
PREPARE st FROM @sql; EXECUTE st; DEALLOCATE PREPARE st;

-- Verificación: debe devolver 2 filas.
SELECT column_name, data_type, is_nullable
FROM information_schema.columns
WHERE table_schema = DATABASE() AND table_name = 'variantes'
  AND column_name IN ('precio_venta', 'precio_rebaja');
