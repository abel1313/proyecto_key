-- ============================================================
-- Migración: a qué TIPO DE PRECIO se vendió cada línea de venta (2026-11-11).
--
-- Hasta hoy cada línea de venta guardaba el precio cobrado pero no si fue el precio de venta, la rebaja, la campaña, un precio de grupo o una
-- promoción. Esta columna lo guarda desde ahora en cada venta nueva (VENTA, REBAJA, CAMPANA, GRUPO, PROMOCION u OTRO).
-- Las ventas anteriores quedan en NULL: el reporte por categoría las estima comparando el precio cobrado con los precios del producto y las
-- marca como «estimadas».
-- Solo AGREGA una columna NULL; no cambia ni borra datos. Idempotente: se puede correr dos veces. CÓRRELA ANTES de desplegar el back: la entidad ya
-- mapea la columna y, sin ella, guardar una venta nueva falla.
-- ============================================================

SET @sql = IF((SELECT COUNT(*) FROM information_schema.columns
               WHERE table_schema = DATABASE() AND table_name = 'detalle_venta_variantes' AND column_name = 'tipo_precio') = 0,
    'ALTER TABLE detalle_venta_variantes ADD COLUMN tipo_precio VARCHAR(12) NULL',
    'SELECT ''detalle_venta_variantes.tipo_precio ya existe''');
PREPARE st FROM @sql; EXECUTE st; DEALLOCATE PREPARE st;

-- Verificación (debe salir 1):
-- SELECT COUNT(*) AS columna_creada FROM information_schema.columns
-- WHERE table_schema = DATABASE() AND table_name = 'detalle_venta_variantes' AND column_name = 'tipo_precio';
-- Después de unas ventas nuevas:
-- SELECT tipo_precio, COUNT(*) FROM detalle_venta_variantes GROUP BY tipo_precio;
