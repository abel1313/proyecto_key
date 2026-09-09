-- ============================================================
-- Migración: acciones puntuales para usuarios/buscar -- criterio confirmado por el usuario
-- 2026-09-08 ("vamos con usuario buscar igual configurable").
--
-- La tarjeta de cada usuario en Gestión de usuarios (all-usuarios.component.html) tiene 3
-- botones: Actualizar (editar), Eliminar y, cuando se ven desactivados, Reactivar. Antes de esta
-- migración estaban completamente abiertos a cualquiera con Ver en la pantalla (sin gating
-- alguno) -- se separan en 3 acciones para poder, por ejemplo, dar Ver+Editar sin dar Eliminar.
-- ============================================================

INSERT INTO accion_submenu (submenu_id, clave, etiqueta, descripcion, orden)
SELECT s.id, 'editar-usuario', 'Actualizar usuario (botón "Actualizar" en la tarjeta)',
       'Botón "✏️ Actualizar" en la tarjeta de cada usuario, en Usuarios (Buscar). Lleva a la '
       'pantalla de edición de ese usuario.',
       1
FROM submenu s
WHERE s.ruta = 'usuarios/buscar'
  AND NOT EXISTS (SELECT 1 FROM accion_submenu e WHERE e.submenu_id = s.id AND e.clave = 'editar-usuario');

INSERT INTO accion_submenu (submenu_id, clave, etiqueta, descripcion, orden)
SELECT s.id, 'eliminar-usuario', 'Eliminar usuario (botón "Eliminar" en la tarjeta)',
       'Botón "🗑️ Eliminar" en la tarjeta de cada usuario, en Usuarios (Buscar). Da de baja al '
       'usuario (pasa a la lista de desactivados).',
       2
FROM submenu s
WHERE s.ruta = 'usuarios/buscar'
  AND NOT EXISTS (SELECT 1 FROM accion_submenu e WHERE e.submenu_id = s.id AND e.clave = 'eliminar-usuario');

INSERT INTO accion_submenu (submenu_id, clave, etiqueta, descripcion, orden)
SELECT s.id, 'activar-usuario', 'Reactivar usuario (botón "Reactivar")',
       'Botón "✅ Reactivar" en la tarjeta de un usuario desactivado, al ver "🚫 Ver desactivados" '
       'en Usuarios (Buscar).',
       3
FROM submenu s
WHERE s.ruta = 'usuarios/buscar'
  AND NOT EXISTS (SELECT 1 FROM accion_submenu e WHERE e.submenu_id = s.id AND e.clave = 'activar-usuario');

-- Se dan a todo rol que hoy tiene Ver en usuarios/buscar, para preservar el comportamiento actual
-- (los 3 botones eran abiertos a cualquiera con acceso a la pantalla).
INSERT INTO rol_accion (rol_id, accion_submenu_id)
SELECT rs.rol_id, a.id
FROM rol_submenu rs
JOIN submenu s ON s.id = rs.submenu_id AND s.ruta = 'usuarios/buscar'
JOIN accion_submenu a ON a.submenu_id = s.id
WHERE NOT EXISTS (
    SELECT 1 FROM rol_accion e WHERE e.rol_id = rs.rol_id AND e.accion_submenu_id = a.id
  );

-- ============================================================
-- VERIFICACIÓN
-- ============================================================
-- SELECT a.clave, a.etiqueta, a.orden FROM accion_submenu a
-- JOIN submenu s ON s.id = a.submenu_id WHERE s.ruta = 'usuarios/buscar' ORDER BY a.orden;
