-- ============================================================
-- Migración: pantalla "Boletos de rifa" en el menú Rifas
-- La pantalla nueva (rifa PLATAFORMAS) no tenía fila en submenu, así que no
-- aparecía en el navbar ni la dejaba pasar PantallaGuard en el front, y en el
-- back /v1/boletoRifa/** solo aceptaba las authorities de rifas/agregar,
-- rifas/mes y rifas/buscar -- un rol al que solo se le diera esta pantalla
-- nueva se hubiera quedado bloqueado igual. Ver SecurityConfig.java, ya
-- actualizado para incluir 'rifas/boletos' en esas listas.
-- ============================================================

INSERT INTO submenu (menu_id, nombre, ruta, icono, orden, descripcion)
SELECT id, 'Boletos de rifa', 'rifas/boletos', '🎟️', 4,
       'Rifa por acciones en redes sociales: cada seguir/compartir es un boleto, la ruleta sortea entre boletos. Vive en el menú: Rifas → Boletos de rifa.'
FROM menu WHERE nombre = 'Rifas'
  AND NOT EXISTS (SELECT 1 FROM submenu WHERE ruta = 'rifas/boletos');

-- Se la asigna a ROLE_ADMIN (ver, editar), igual que el resto de Rifas
INSERT INTO rol_submenu (rol_id, submenu_id)
SELECT r.id, s.id
FROM roles r
CROSS JOIN submenu s
WHERE r.nombre_rol = 'ROLE_ADMIN'
  AND s.ruta = 'rifas/boletos'
  AND NOT EXISTS (
    SELECT 1 FROM rol_submenu rs WHERE rs.rol_id = r.id AND rs.submenu_id = s.id
  );

INSERT INTO rol_submenu_escritura (rol_id, submenu_id)
SELECT r.id, s.id
FROM roles r
CROSS JOIN submenu s
WHERE r.nombre_rol = 'ROLE_ADMIN'
  AND s.ruta = 'rifas/boletos'
  AND NOT EXISTS (
    SELECT 1 FROM rol_submenu_escritura rs WHERE rs.rol_id = r.id AND rs.submenu_id = s.id
  );

-- ============================================================
-- VERIFICACIÓN
-- ============================================================
-- SELECT s.nombre, s.ruta, r.nombre_rol
-- FROM submenu s
-- JOIN rol_submenu rs ON rs.submenu_id = s.id
-- JOIN roles r ON r.id = rs.rol_id
-- WHERE s.ruta = 'rifas/boletos';
