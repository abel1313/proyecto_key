-- ============================================================
-- Migración: acciones puntuales para gastos/buscar -- criterio confirmado por el usuario
-- 2026-09-08 ("para gastos, ahí sí hay varios botones para que los agregues como permisos").
--
-- Solo la pestaña "Gastos" tiene acciones reales (agregar/editar/eliminar) -- "Ventas" y
-- "Reporte" son de puro consultar, sin botones, se quedan bajo el Ver general.
-- ============================================================

INSERT INTO accion_submenu (submenu_id, clave, etiqueta, descripcion, orden)
SELECT s.id, 'agregar-gasto', 'Agregar gasto (➕ en la pestaña Gastos)',
       'Botón "➕ Agregar gasto" en la pestaña Gastos, en Gastos/Ventas/Reporte.',
       1
FROM submenu s
WHERE s.ruta = 'gastos/buscar'
  AND NOT EXISTS (SELECT 1 FROM accion_submenu e WHERE e.submenu_id = s.id AND e.clave = 'agregar-gasto');

INSERT INTO accion_submenu (submenu_id, clave, etiqueta, descripcion, orden)
SELECT s.id, 'editar-gasto', 'Editar gasto (✏️ en la fila)',
       'Ícono ✏️ para editar un gasto ya registrado, en la pestaña Gastos.',
       2
FROM submenu s
WHERE s.ruta = 'gastos/buscar'
  AND NOT EXISTS (SELECT 1 FROM accion_submenu e WHERE e.submenu_id = s.id AND e.clave = 'editar-gasto');

INSERT INTO accion_submenu (submenu_id, clave, etiqueta, descripcion, orden)
SELECT s.id, 'eliminar-gasto', 'Eliminar gasto (🗑️ en la fila)',
       'Ícono 🗑️ para eliminar un gasto ya registrado, en la pestaña Gastos.',
       3
FROM submenu s
WHERE s.ruta = 'gastos/buscar'
  AND NOT EXISTS (SELECT 1 FROM accion_submenu e WHERE e.submenu_id = s.id AND e.clave = 'eliminar-gasto');

-- Se dan a todo rol que hoy tiene Ver en gastos/buscar, para preservar el comportamiento actual
-- (los 3 botones eran abiertos a cualquiera con acceso a la pantalla).
INSERT INTO rol_accion (rol_id, accion_submenu_id)
SELECT rs.rol_id, a.id
FROM rol_submenu rs
JOIN submenu s ON s.id = rs.submenu_id AND s.ruta = 'gastos/buscar'
JOIN accion_submenu a ON a.submenu_id = s.id
WHERE NOT EXISTS (
    SELECT 1 FROM rol_accion e WHERE e.rol_id = rs.rol_id AND e.accion_submenu_id = a.id
  );

-- ============================================================
-- VERIFICACIÓN
-- ============================================================
-- SELECT a.clave, a.etiqueta, a.orden FROM accion_submenu a
-- JOIN submenu s ON s.id = a.submenu_id WHERE s.ruta = 'gastos/buscar' ORDER BY a.orden;
