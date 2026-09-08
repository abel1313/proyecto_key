-- ============================================================
-- Migración: grupo de menú propio para "Tienda" (tienda/buscar)
--
-- Motivo: "Tienda" (submenu.id=38) tenía menu_id = NULL, así que en Gestión de roles caía en el
-- bloque "Sin grupo" junto con Home/Clientes/Favoritos/Chat/QR/Login -- ahí era difícil de
-- encontrar para configurar sus acciones puntuales (habilitar/compartir-imagen/filtros/editar),
-- a pesar de que en el sidebar público SÍ aparece como su propio ícono de primer nivel, igual
-- que "Catálogo". Es pública (no necesita el permiso para que el cliente la vea), pero el modo
-- admin de esa pantalla sí depende de estas acciones, así que conviene que sea fácil de ubicar.
--
-- orden=0 para que aparezca primero en el acordeón (antes de "Catálogo", orden=1), reflejando
-- que en el sidebar Tienda va antes que Catálogo.
-- ============================================================

INSERT INTO menu (nombre, icono, orden)
SELECT 'Tienda', '🛍️', 0
WHERE NOT EXISTS (SELECT 1 FROM menu WHERE nombre = 'Tienda');

UPDATE submenu s
JOIN menu m ON m.nombre = 'Tienda'
SET s.menu_id = m.id
WHERE s.ruta = 'tienda/buscar';

-- ============================================================
-- VERIFICACIÓN
-- ============================================================
-- SELECT s.id, s.nombre, s.ruta, m.nombre AS grupo
-- FROM submenu s LEFT JOIN menu m ON m.id = s.menu_id
-- WHERE s.ruta = 'tienda/buscar';
