-- ============================================================
-- Migración: vender un artículo con descuento (2026-09-29, R8 del dominio precio)
--
-- En la tarjeta de Tienda (botón 💲) el admin elige "Precio venta" o "Precio descuento" para un
-- artículo. Con "Precio descuento" el artículo se vende al descuento para todos (tienda, carrito,
-- chatbot, pedidos de clientes) hasta que vuelva a elegir "Precio venta".
--
--   usar_descuento  0 = se vende al precio normal (como hasta hoy)
--                   1 = se vende al precio con descuento (variantes.precio_rebaja)
--
-- Sin esta migración el back NO ARRANCA bien: la entidad Variantes ya mapea la columna y cualquier
-- consulta de artículos falla con "Unknown column". Correrla ANTES del deploy.
--
-- No mueve ningún precio: todos los artículos nacen en 0 y siguen cobrando lo mismo que hoy.
-- Idempotente: la columna se agrega solo si no existe.
-- ============================================================

SET @existe := (SELECT COUNT(*) FROM information_schema.columns
                WHERE table_schema = DATABASE() AND table_name = 'variantes' AND column_name = 'usar_descuento');
SET @sql := IF(@existe = 0,
    'ALTER TABLE variantes ADD COLUMN usar_descuento TINYINT(1) NOT NULL DEFAULT 0 COMMENT ''1 = el articulo se vende al precio con descuento (R8)''',
    'SELECT ''usar_descuento ya existe''');
PREPARE st FROM @sql; EXECUTE st; DEALLOCATE PREPARE st;

-- Verificación 1: debe devolver 1 fila (tinyint, NO, default 0).
SELECT column_name, column_type, is_nullable, column_default
FROM information_schema.columns
WHERE table_schema = DATABASE() AND table_name = 'variantes' AND column_name = 'usar_descuento';

-- Verificación 2: justo después de correrla debe dar 0 (ningún artículo cambia de precio).
SELECT COUNT(*) AS articulos_con_descuento_activo FROM variantes WHERE usar_descuento = 1;
