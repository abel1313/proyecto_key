-- ============================================================
-- REPARACIÓN: el permiso "Ayuda contextual" no aparece en Gestión de roles (2026-09-17)
--
-- Qué pasó: `migration_submenu_ayuda_contextual.sql` insertaba así:
--
--     INSERT INTO submenu (...)
--     SELECT gr.menu_id, 'Ayuda contextual', ...
--     FROM submenu gr
--     WHERE gr.ruta = 'gestion-menu/roles'      <-- fila ancla
--
-- Si en esa base no hay ninguna fila con ruta = 'gestion-menu/roles', el SELECT devuelve 0
-- filas y el INSERT inserta 0 filas SIN MARCAR ERROR. Por eso se dio por corrida (no falló)
-- pero el permiso nunca apareció en la pantalla de Gestión de roles.
--
-- Esta versión NO depende de ninguna fila ancla: inserta siempre una vez, y si el grupo
-- "Sistema" no existe la deja sin grupo (Gestión de roles ya pinta un bloque "sin grupo",
-- así que igual se ve). Sigue siendo idempotente: correrla dos veces no duplica.
--
-- Correr en QA (cubre dev y qa, misma base) y en prod.
-- ============================================================

-- ─── PASO 1 — DIAGNÓSTICO (solo lee, no cambia nada). Córrelo primero. ───
-- Si la primera consulta devuelve 0 filas, se confirma que nunca se insertó.
--
--   SELECT id, ruta, nombre, menu_id, orden FROM submenu WHERE ruta = 'ayuda-contextual';
--   SELECT id, nombre FROM menu WHERE nombre = 'Sistema';
--   SELECT id, ruta FROM submenu WHERE ruta = 'gestion-menu/roles';   -- la fila ancla que faltaba


-- ─── PASO 2 — ALTA DE LA PANTALLA-PERMISO ───
-- El FROM (SELECT 1) dummy es lo que hace que inserte aunque no exista ninguna fila ancla.
INSERT INTO submenu (menu_id, nombre, ruta, icono, descripcion, orden)
SELECT (SELECT m.id FROM menu m WHERE m.nombre = 'Sistema' ORDER BY m.id LIMIT 1),
       'Ayuda contextual',
       'ayuda-contextual',
       '❓',
       'Muestra u oculta el icono "?" que explica qué hace cada pantalla del admin y en qué se diferencia de las parecidas. No es una pantalla navegable: es solo el interruptor de ese icono.',
       99
FROM (SELECT 1) AS dummy
WHERE NOT EXISTS (
    SELECT 1 FROM (SELECT ruta FROM submenu) AS existente WHERE existente.ruta = 'ayuda-contextual'
);

-- ─── PASO 3 — CONCEDÉRSELA A ROLE_ADMIN ───
-- Se corre aparte del paso 2 a propósito: si la fila ya existía pero sin conceder, esto la arregla.
INSERT INTO rol_submenu (rol_id, submenu_id)
SELECT r.id, s.id
FROM roles r
CROSS JOIN submenu s
WHERE s.ruta = 'ayuda-contextual'
  AND r.nombre_rol = 'ROLE_ADMIN'
  AND NOT EXISTS (
    SELECT 1 FROM (SELECT rol_id, submenu_id FROM rol_submenu) AS existente
    WHERE existente.rol_id = r.id AND existente.submenu_id = s.id
  );

-- No se inserta en rol_submenu_escritura: la ayuda es de solo lectura, no hay nada que editar
-- desde el modal, así que el checkbox "✏️ Editar" de esa fila no controla nada.

-- ─── PASO 4 — VERIFICACIÓN (debe devolver 1 fila cada una) ───
--   SELECT s.id, s.ruta, s.nombre, s.menu_id, s.orden FROM submenu s WHERE s.ruta = 'ayuda-contextual';
--   SELECT r.nombre_rol, s.ruta
--     FROM rol_submenu rs
--     JOIN roles r   ON r.id = rs.rol_id
--     JOIN submenu s ON s.id = rs.submenu_id
--    WHERE s.ruta = 'ayuda-contextual';
--
-- Después de correrla: entra a Gestión de roles y recarga con Ctrl+Shift+R. "Ayuda contextual"
-- aparece dentro del grupo Sistema (o en el bloque sin grupo, si no hay grupo "Sistema").
