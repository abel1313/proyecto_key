-- ============================================================
-- Migración: "Corte de caja" como pantalla propia del menú (2026-11-05).
--
-- El corte de caja (pedidos sin entregar y con saldo, por zona de entrega, con su ganancia) vivía solo como pestaña de
-- Reportes. Esta migración le da su fila en `submenu` (ruta 'reportes/corte-caja'), en el MISMO grupo que "Reportes de ventas"
-- (el menu_id de la fila con ruta 'reportes'), para que salga como opción del menú y se pueda dar o quitar desde Gestión de roles.
--
-- Solo lectura (tiene_escritura = 0) y SOLO ROLE_ADMIN: lleva costo y ganancia, y el endpoint exige ADMIN.
-- Idempotente: se puede correr dos veces.
-- ============================================================

INSERT INTO submenu (menu_id, nombre, ruta, icono, descripcion, orden, tiene_escritura)
SELECT r.menu_id, 'Corte de caja', 'reportes/corte-caja', '🧾',
       'Pedidos sin entregar y con saldo por cobrar, por zona de entrega, con subtotal por zona, total general y la ganancia (oculta hasta desplegarla). Solo lectura, solo administrador.',
       r.orden + 1, 0
FROM submenu r
WHERE r.ruta = 'reportes'
  AND NOT EXISTS (SELECT 1 FROM submenu e WHERE e.ruta = 'reportes/corte-caja');

INSERT INTO rol_submenu (rol_id, submenu_id)
SELECT ro.id, s.id
FROM roles ro
CROSS JOIN submenu s
WHERE s.ruta = 'reportes/corte-caja'
  AND ro.nombre_rol = 'ROLE_ADMIN'
  AND NOT EXISTS (SELECT 1 FROM rol_submenu e WHERE e.rol_id = ro.id AND e.submenu_id = s.id);

-- ============================================================
-- VERIFICACIÓN
-- ============================================================
-- SELECT s.id, s.ruta, s.nombre, s.menu_id, s.orden, s.tiene_escritura FROM submenu s WHERE s.ruta IN ('reportes', 'reportes/corte-caja');
-- SELECT ro.nombre_rol, s.ruta FROM rol_submenu rs JOIN roles ro ON ro.id = rs.rol_id JOIN submenu s ON s.id = rs.submenu_id WHERE s.ruta = 'reportes/corte-caja';
