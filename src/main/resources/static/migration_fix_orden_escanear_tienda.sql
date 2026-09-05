-- ============================================================
-- Fix: orden incorrecto de "escanear-codigo" en tienda/buscar
-- (corrige migration_accion_tienda_escanear.sql)
--
-- Esa migración le puso orden=2 a la acción nueva, pero en tienda/buscar el orden 1-9 ya está
-- ocupado por la categoría "Filtros" (ver migration_accion_submenu_categoria.sql) y 10-11 por
-- "Tarjeta de variante" -- el front agrupa por bloques CONTIGUOS de `orden` dentro de cada
-- `categoria`, así que un orden=2 con categoria='Buscador' quedaba encajado en medio del bloque
-- de Filtros (colisionando además con filtro-sin-stock, que también es orden=2) y rompía el
-- agrupamiento -- por eso no aparecía en Gestión de roles.
--
-- Corrige a orden=12, justo después de "Tarjeta de variante" (10-11), para que "Buscador" quede
-- como su propio bloque contiguo al final.
-- ============================================================

UPDATE accion_submenu a
JOIN submenu s ON s.id = a.submenu_id
SET a.orden = 12
WHERE s.ruta = 'tienda/buscar' AND a.clave = 'escanear-codigo';

-- ============================================================
-- VERIFICACIÓN
-- ============================================================
-- SELECT a.clave, a.categoria, a.orden
-- FROM accion_submenu a
-- JOIN submenu s ON s.id = a.submenu_id
-- WHERE s.ruta = 'tienda/buscar'
-- ORDER BY a.orden;
