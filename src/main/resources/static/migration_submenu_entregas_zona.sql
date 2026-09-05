-- ============================================================
-- Migración: registrar "Entregas por zona" como pantalla real del sistema de permisos
-- (2026-09-05)
--
-- Motivo: "entregas-zona" (programar el viaje semanal a una zona y avisar por correo a los
-- clientes que pidieron ahí) se agregó el 2026-09-04 sin PantallaGuard ni fila en `submenu` --
-- ver el comentario en app-routing.module.ts. Hoy "vive de prestado": en el navbar el link
-- aparece si el rol tiene la pantalla "lugares-entrega" (comparten el grupo "Envíos" del
-- acordeón), pero el back sí exige ROLE_ADMIN de verdad -- así que un rol no-admin con
-- "lugares-entrega" ve el link y se topa con 403 al usarlo, mientras que el guard real (quién
-- puede entrar) nunca pasó por Gestión de roles.
--
-- Esta migración le da su propia fila en submenu, en el MISMO grupo (menu_id) que ya usa
-- "lugares-entrega" -- lo que sea que tenga ese grupo hoy (no se asume ningún ID fijo), para
-- que sigan apareciendo juntos bajo "Envíos" en Gestión de roles.
--
-- View (rol_submenu) y Escritura (rol_submenu_escritura) se dan solo a ROLE_ADMIN -- es lo único
-- que hoy puede usar la pantalla de verdad (SecurityConfig exigía ROLE_ADMIN puro antes de esta
-- migración), así que ROLE_ADMIN es el único rol cuyo comportamiento actual hay que preservar.
-- Ver el fix relacionado en SecurityConfig.java (pantalla/pantallaEscribir en vez de hasRole fijo).
-- ============================================================

INSERT INTO submenu (menu_id, nombre, ruta, icono, descripcion, orden)
SELECT le.menu_id, 'Entregas por zona', 'entregas-zona', '📦',
       'Programar el viaje semanal a una zona de entrega y avisar por correo a los clientes que pidieron ahí esa semana. Vive en el grupo "Envíos" del menú.',
       le.orden + 1
FROM submenu le
WHERE le.ruta = 'lugares-entrega'
  AND NOT EXISTS (SELECT 1 FROM submenu existente WHERE existente.ruta = 'entregas-zona');

INSERT INTO rol_submenu (rol_id, submenu_id)
SELECT r.id, s.id
FROM roles r
CROSS JOIN submenu s
WHERE s.ruta = 'entregas-zona'
  AND r.nombre_rol = 'ROLE_ADMIN'
  AND NOT EXISTS (
    SELECT 1 FROM rol_submenu existente WHERE existente.rol_id = r.id AND existente.submenu_id = s.id
  );

INSERT INTO rol_submenu_escritura (rol_id, submenu_id)
SELECT r.id, s.id
FROM roles r
CROSS JOIN submenu s
WHERE s.ruta = 'entregas-zona'
  AND r.nombre_rol = 'ROLE_ADMIN'
  AND NOT EXISTS (
    SELECT 1 FROM rol_submenu_escritura existente WHERE existente.rol_id = r.id AND existente.submenu_id = s.id
  );

-- ============================================================
-- VERIFICACIÓN
-- ============================================================
-- SELECT s.id, s.ruta, s.nombre, s.menu_id, s.orden FROM submenu s WHERE s.ruta IN ('lugares-entrega', 'entregas-zona');
-- SELECT r.nombre_rol, s.ruta FROM rol_submenu rs JOIN roles r ON r.id = rs.rol_id JOIN submenu s ON s.id = rs.submenu_id WHERE s.ruta = 'entregas-zona';
-- SELECT r.nombre_rol, s.ruta FROM rol_submenu_escritura rse JOIN roles r ON r.id = rse.rol_id JOIN submenu s ON s.id = rse.submenu_id WHERE s.ruta = 'entregas-zona';
