-- ============================================================
-- Migración: acción "reporte-revision" (pestaña "🔎 Revisión" en Reportes de ventas, 2026-11-01).
--
-- La Revisión de ventas avisa de cosas raras en pedidos, ventas, abonos, stock y cobros con tarjeta. Es solo
-- lectura, pero enseña datos internos, así que NO se da a todo rol con "Ver reportes" (a diferencia de las otras
-- pestañas, ver migration_accion_reportes.sql): se da SOLO a ROLE_ADMIN. El endpoint además exige ADMIN.
-- Idempotente: se puede correr dos veces. Si se quiere dar a otro rol, se hace desde Gestión de roles.
-- ============================================================

INSERT INTO accion_submenu (submenu_id, clave, etiqueta, descripcion, orden)
SELECT s.id, 'reporte-revision', 'Pestaña Revisión (🔎)',
       'Pestaña "🔎 Revisión" en Reportes de ventas: avisa de pedidos, ventas, abonos y stock que no cuadran.', 6
FROM submenu s
WHERE s.ruta = 'reportes'
  AND NOT EXISTS (SELECT 1 FROM accion_submenu e WHERE e.submenu_id = s.id AND e.clave = 'reporte-revision');

INSERT INTO rol_accion (rol_id, accion_submenu_id)
SELECT r.id, a.id
FROM roles r
JOIN submenu s ON s.ruta = 'reportes'
JOIN accion_submenu a ON a.submenu_id = s.id AND a.clave = 'reporte-revision'
WHERE r.nombre_rol = 'ROLE_ADMIN'
  AND NOT EXISTS (SELECT 1 FROM rol_accion e WHERE e.rol_id = r.id AND e.accion_submenu_id = a.id);

-- ============================================================
-- VERIFICACIÓN
-- ============================================================
-- SELECT a.clave, a.etiqueta, a.orden FROM accion_submenu a
-- JOIN submenu s ON s.id = a.submenu_id WHERE s.ruta = 'reportes' ORDER BY a.orden;
-- SELECT r.nombre_rol FROM rol_accion ra JOIN roles r ON r.id = ra.rol_id
-- JOIN accion_submenu a ON a.id = ra.accion_submenu_id WHERE a.clave = 'reporte-revision';
