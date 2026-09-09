-- ============================================================
-- Fix: ColorFlor.stock desincronizado de su variante "sombra"
--
-- Bug encontrado 2026-08-28 (reporte real: variante id 619 -- el configurador de "Arma tu ramo"
-- ofrecía 100 disponibles, la variante ya tenía solo 20, y el pedido reventaba al guardar con
-- "Stock insuficiente").
--
-- Causa: ColorFlor.stock es una COPIA del stock real, que vive en su variante "sombra"
-- (color_flor.variante_id -> variantes.id, ver ProductoSombraServiceImpl). Cada vez que se vendía
-- un ramo, PedidoServiceImpl descontaba `variantes.stock` correctamente, pero nunca actualizaba
-- `color_flor.stock` de vuelta -- así que con cada ramo vendido la variante bajaba pero el número
-- que ve el cliente en el configurador se quedaba pegado en el valor viejo, cada vez más alto que
-- la realidad, hasta que el pedido se rechazaba al final con el número real.
--
-- El código ya se corrigió (PedidoServiceImpl ahora resincroniza color_flor.stock cada vez que
-- toca el stock de su variante) -- este script es el arreglo de UNA VEZ para los datos que ya
-- quedaron desincronizados antes del fix.
-- ============================================================

UPDATE color_flor cf
JOIN variantes v ON v.id = cf.variante_id
SET cf.stock = v.stock
WHERE cf.stock <> v.stock;

-- ============================================================
-- VERIFICACIÓN
-- ============================================================
-- Antes de correr el UPDATE, para ver qué filas estaban desincronizadas:
-- SELECT cf.id, cf.nombre, cf.stock AS stock_color_flor, v.id AS variante_id, v.stock AS stock_variante
-- FROM color_flor cf JOIN variantes v ON v.id = cf.variante_id
-- WHERE cf.stock <> v.stock;
--
-- Después del UPDATE, esta consulta debe devolver 0 filas:
-- SELECT COUNT(*) FROM color_flor cf JOIN variantes v ON v.id = cf.variante_id WHERE cf.stock <> v.stock;
