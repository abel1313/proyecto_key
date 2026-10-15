-- ============================================================
-- Migración: el administrador no veía "➕ Agregar gasto" (2026-10-06).
--
-- Reportado en QA: "gastos/buscar solo dice un texto 'agregar' pero no hay ninguna opción para
-- agregar nada". El botón pide la acción agregar-gasto (migration_accion_gastos.sql, 2026-09-08),
-- pero esa migración solo se la dio a los roles que tenían la pantalla en rol_submenu; ROLE_ADMIN
-- ve todas las pantallas sin esa fila, así que se quedó sin las tres acciones de Gastos.
--
-- Qué hace (idempotente):
--   1. Da de alta las 3 acciones de Gastos si en esta base no existen.
--   2. Se las da a ROLE_ADMIN.
-- Hay que volver a entrar después (los permisos viajan dentro del token).
-- ============================================================

SET @safe_updates_antes = @@SQL_SAFE_UPDATES;
SET SQL_SAFE_UPDATES = 0;

START TRANSACTION;

INSERT INTO accion_submenu (submenu_id, clave, etiqueta, descripcion, orden)
SELECT s.id, n.clave, n.etiqueta, n.descripcion, n.orden
FROM submenu s
JOIN (
    SELECT 'agregar-gasto' AS clave, 'Agregar gasto (➕ en la pestaña Gastos)' AS etiqueta,
           'Botón "➕ Agregar gasto" en la pestaña Gastos, en Gastos/Ventas/Reporte.' AS descripcion, 1 AS orden
    UNION ALL SELECT 'editar-gasto', 'Editar gasto (✏️ en la fila)',
           'Ícono ✏️ para editar un gasto ya registrado, en la pestaña Gastos.', 2
    UNION ALL SELECT 'eliminar-gasto', 'Eliminar gasto (🗑️ en la fila)',
           'Ícono 🗑️ para eliminar un gasto ya registrado, en la pestaña Gastos.', 3
) n ON 1 = 1
WHERE s.ruta = 'gastos/buscar'
  AND NOT EXISTS (SELECT 1 FROM accion_submenu e WHERE e.submenu_id = s.id AND e.clave = n.clave);

INSERT INTO rol_accion (rol_id, accion_submenu_id)
SELECT r.id, a.id
FROM roles r
JOIN submenu s ON s.ruta = 'gastos/buscar'
JOIN accion_submenu a ON a.submenu_id = s.id
WHERE r.nombre_rol = 'ROLE_ADMIN'
  AND a.clave IN ('agregar-gasto', 'editar-gasto', 'eliminar-gasto')
  AND NOT EXISTS (SELECT 1 FROM rol_accion e WHERE e.rol_id = r.id AND e.accion_submenu_id = a.id);

COMMIT;

SET SQL_SAFE_UPDATES = @safe_updates_antes;

-- ── Verificación: 3 filas, las 3 con admin = 1 ─────────────────────────────────────────────
-- SELECT a.clave, a.etiqueta,
--        EXISTS (SELECT 1 FROM rol_accion ra JOIN roles r ON r.id = ra.rol_id
--                WHERE ra.accion_submenu_id = a.id AND r.nombre_rol = 'ROLE_ADMIN') AS admin
-- FROM accion_submenu a JOIN submenu s ON s.id = a.submenu_id
-- WHERE s.ruta = 'gastos/buscar' ORDER BY a.orden;
