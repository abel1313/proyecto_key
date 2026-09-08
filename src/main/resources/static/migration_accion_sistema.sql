-- ============================================================
-- Migración: acciones puntuales para el grupo Sistema (gestion-menu, gestion-menu/roles,
-- personalizacion, admin/reconciliacion-imagenes) -- continuación del audit sistemático de
-- permisos finos (2026-09-08).
--
-- admin/negocio, admin/chat, admin/presentacion, admin/diagnostico-imagenes y admin/cache son
-- cada una un único flujo (config del negocio, chat en vivo, subir imágenes, diagnóstico de
-- solo lectura, limpiar caché) sin acciones separables -- quedan fuera del audit.
--
-- usuarios/buscar (Usuarios) queda FUERA de esta migración a propósito -- editar/eliminar/
-- activar una cuenta de usuario es sensible (puede incluir escalar el propio rol), necesita
-- criterio de negocio antes de decidir la separación (ver AUDIT_PERMISOS_FINOS_TODAS_PANTALLAS.md).
-- ============================================================

-- gestion-menu: eliminar un menú y eliminar un submenú son 2 acciones distintas y de riesgo
-- estructural (borrar un submenú puede romper el permiso de esa pantalla en todo el sistema).
INSERT INTO accion_submenu (submenu_id, clave, etiqueta, descripcion, orden)
SELECT s.id, 'eliminar-menu', 'Eliminar menú (🗑️ en la lista de menús)',
       'Botón 🗑️ para borrar un grupo de menú completo, en Menús y submenús.',
       1
FROM submenu s
WHERE s.ruta = 'gestion-menu'
  AND NOT EXISTS (SELECT 1 FROM accion_submenu e WHERE e.submenu_id = s.id AND e.clave = 'eliminar-menu');

INSERT INTO accion_submenu (submenu_id, clave, etiqueta, descripcion, orden)
SELECT s.id, 'eliminar-submenu', 'Eliminar submenú/pantalla (🗑️ en la lista de submenús)',
       'Botón 🗑️ para borrar una pantalla dentro de un menú, en Menús y submenús.',
       2
FROM submenu s
WHERE s.ruta = 'gestion-menu'
  AND NOT EXISTS (SELECT 1 FROM accion_submenu e WHERE e.submenu_id = s.id AND e.clave = 'eliminar-submenu');

-- gestion-menu/roles: eliminar un rol completo. La edición de permisos (checkboxes Ver/Editar/
-- acciones puntuales) es el propósito mismo de la pantalla y se queda bajo Editar general -- solo
-- se separa el borrado de un rol entero.
INSERT INTO accion_submenu (submenu_id, clave, etiqueta, descripcion, orden)
SELECT s.id, 'eliminar', 'Eliminar rol (🗑️ en la lista de roles)',
       'Botón 🗑️ para borrar un rol completo, en Gestión de roles.',
       1
FROM submenu s
WHERE s.ruta = 'gestion-menu/roles'
  AND NOT EXISTS (SELECT 1 FROM accion_submenu e WHERE e.submenu_id = s.id AND e.clave = 'eliminar');

-- personalizacion: eliminar una variable/tema personalizado.
INSERT INTO accion_submenu (submenu_id, clave, etiqueta, descripcion, orden)
SELECT s.id, 'eliminar', 'Eliminar variable de tema (✏️/🗑️ en la fila)',
       'Botón 🗑️ para borrar una variable de personalización guardada, en Personalización.',
       1
FROM submenu s
WHERE s.ruta = 'personalizacion'
  AND NOT EXISTS (SELECT 1 FROM accion_submenu e WHERE e.submenu_id = s.id AND e.clave = 'eliminar');

-- admin/reconciliacion-imagenes: "Limpiar BD" es un botón marcado como peligroso (borra
-- registros huérfanos de la base) -- se separa de "Iniciar" (solo diagnostica/escanea).
INSERT INTO accion_submenu (submenu_id, clave, etiqueta, descripcion, orden)
SELECT s.id, 'limpiar-bd', 'Limpiar BD (botón rojo "Limpiar BD")',
       'Botón rojo que borra de la base de datos los registros huérfanos encontrados por la reconciliación, en Reconciliación de imágenes.',
       1
FROM submenu s
WHERE s.ruta = 'admin/reconciliacion-imagenes'
  AND NOT EXISTS (SELECT 1 FROM accion_submenu e WHERE e.submenu_id = s.id AND e.clave = 'limpiar-bd');

INSERT INTO rol_accion (rol_id, accion_submenu_id)
SELECT rs.rol_id, a.id
FROM rol_submenu rs
JOIN submenu s ON s.id = rs.submenu_id
  AND s.ruta IN ('gestion-menu', 'gestion-menu/roles', 'personalizacion', 'admin/reconciliacion-imagenes')
JOIN accion_submenu a ON a.submenu_id = s.id
WHERE NOT EXISTS (
    SELECT 1 FROM rol_accion e WHERE e.rol_id = rs.rol_id AND e.accion_submenu_id = a.id
  );

-- ============================================================
-- VERIFICACIÓN
-- ============================================================
-- SELECT s.ruta, a.clave, a.etiqueta, a.orden
-- FROM accion_submenu a
-- JOIN submenu s ON s.id = a.submenu_id
-- WHERE s.ruta IN ('gestion-menu', 'gestion-menu/roles', 'personalizacion', 'admin/reconciliacion-imagenes')
-- ORDER BY s.ruta, a.orden;
