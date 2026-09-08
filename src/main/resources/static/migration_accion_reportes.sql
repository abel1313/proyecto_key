-- ============================================================
-- Migración: acciones puntuales para "reportes" (Reportes de ventas) -- criterio confirmado por
-- el usuario 2026-09-08 ("reporte de ventas configurable porque hay varios botones"; "dashboard"
-- se queda todo-o-nada, sin acciones puntuales).
--
-- Cada pestaña es un reporte independiente -- se separa una acción por pestaña, para poder dar
-- por ejemplo "Diario" y "Por cliente" sin dar "Promociones".
-- ============================================================

INSERT INTO accion_submenu (submenu_id, clave, etiqueta, descripcion, orden)
SELECT s.id, 'reporte-diario', 'Pestaña Diario (📅)',
       'Pestaña "📅 Diario" en Reportes de ventas.', 1
FROM submenu s
WHERE s.ruta = 'reportes'
  AND NOT EXISTS (SELECT 1 FROM accion_submenu e WHERE e.submenu_id = s.id AND e.clave = 'reporte-diario');

INSERT INTO accion_submenu (submenu_id, clave, etiqueta, descripcion, orden)
SELECT s.id, 'reporte-mensual', 'Pestaña Mensual (📆)',
       'Pestaña "📆 Mensual" en Reportes de ventas.', 2
FROM submenu s
WHERE s.ruta = 'reportes'
  AND NOT EXISTS (SELECT 1 FROM accion_submenu e WHERE e.submenu_id = s.id AND e.clave = 'reporte-mensual');

INSERT INTO accion_submenu (submenu_id, clave, etiqueta, descripcion, orden)
SELECT s.id, 'reporte-cliente', 'Pestaña Por cliente (👤)',
       'Pestaña "👤 Por cliente" en Reportes de ventas.', 3
FROM submenu s
WHERE s.ruta = 'reportes'
  AND NOT EXISTS (SELECT 1 FROM accion_submenu e WHERE e.submenu_id = s.id AND e.clave = 'reporte-cliente');

INSERT INTO accion_submenu (submenu_id, clave, etiqueta, descripcion, orden)
SELECT s.id, 'reporte-mas-vendidos', 'Pestaña Más vendidos (🏆)',
       'Pestaña "🏆 Más vendidos" en Reportes de ventas.', 4
FROM submenu s
WHERE s.ruta = 'reportes'
  AND NOT EXISTS (SELECT 1 FROM accion_submenu e WHERE e.submenu_id = s.id AND e.clave = 'reporte-mas-vendidos');

INSERT INTO accion_submenu (submenu_id, clave, etiqueta, descripcion, orden)
SELECT s.id, 'reporte-promociones', 'Pestaña Promociones (🎁)',
       'Pestaña "🎁 Promociones" en Reportes de ventas.', 5
FROM submenu s
WHERE s.ruta = 'reportes'
  AND NOT EXISTS (SELECT 1 FROM accion_submenu e WHERE e.submenu_id = s.id AND e.clave = 'reporte-promociones');

-- Se dan a todo rol que hoy tiene Ver en "reportes", para preservar el comportamiento actual
-- (las 5 pestañas eran abiertas a cualquiera con acceso a la pantalla).
INSERT INTO rol_accion (rol_id, accion_submenu_id)
SELECT rs.rol_id, a.id
FROM rol_submenu rs
JOIN submenu s ON s.id = rs.submenu_id AND s.ruta = 'reportes'
JOIN accion_submenu a ON a.submenu_id = s.id
WHERE NOT EXISTS (
    SELECT 1 FROM rol_accion e WHERE e.rol_id = rs.rol_id AND e.accion_submenu_id = a.id
  );

-- ============================================================
-- VERIFICACIÓN
-- ============================================================
-- SELECT a.clave, a.etiqueta, a.orden FROM accion_submenu a
-- JOIN submenu s ON s.id = a.submenu_id WHERE s.ruta = 'reportes' ORDER BY a.orden;
