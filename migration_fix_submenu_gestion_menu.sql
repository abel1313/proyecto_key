-- ============================================================
-- FIX: faltaban las filas de "Menús y submenús" y "Gestión de roles" en el
-- catálogo submenu -- por eso NINGÚN rol (ni ROLE_ADMIN) podía tener esas
-- pantallas asignadas, y el PantallaGuard bloqueaba a todos por igual,
-- mandando a /tienda/buscar. El link del navbar y la protección para que no
-- se le pudieran quitar a ROLE_ADMIN ya existían -- lo que faltaba era el
-- dato base en la tabla.
-- ============================================================

INSERT INTO submenu (menu_id, nombre, ruta, icono, orden)
SELECT id, 'Menús y submenús', 'gestion-menu', '🗂️', 8
FROM menu WHERE nombre = 'Sistema'
  AND NOT EXISTS (SELECT 1 FROM submenu WHERE ruta = 'gestion-menu');

INSERT INTO submenu (menu_id, nombre, ruta, icono, orden)
SELECT id, 'Gestión de roles', 'gestion-menu/roles', '🛡️', 9
FROM menu WHERE nombre = 'Sistema'
  AND NOT EXISTS (SELECT 1 FROM submenu WHERE ruta = 'gestion-menu/roles');

-- Se las asigna a ROLE_ADMIN (y a cualquier otro rol que ya tuviera TODAS las
-- demás pantallas de Sistema asignadas, por si se creó otro rol admin-like a mano).
INSERT INTO rol_submenu (rol_id, submenu_id)
SELECT r.id, s.id
FROM roles r
CROSS JOIN submenu s
WHERE r.nombre_rol = 'ROLE_ADMIN'
  AND s.ruta IN ('gestion-menu', 'gestion-menu/roles')
  AND NOT EXISTS (
    SELECT 1 FROM rol_submenu rs WHERE rs.rol_id = r.id AND rs.submenu_id = s.id
  );

-- ============================================================
-- VERIFICACIÓN
-- ============================================================
-- SELECT s.nombre, s.ruta, r.nombre_rol
-- FROM submenu s
-- JOIN rol_submenu rs ON rs.submenu_id = s.id
-- JOIN roles r ON r.id = rs.rol_id
-- WHERE s.ruta IN ('gestion-menu', 'gestion-menu/roles');
