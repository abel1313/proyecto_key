-- ============================================================
-- Migración: pantalla «Publicaciones de WhatsApp» — 2026-11-22. Ver PLAN_WHATSAPP_PUBLICACIONES.md.
--
-- Da de alta el submenú (ruta whatsapp/publicaciones) en el grupo donde está «Campañas» y se lo da SOLO a ROLE_ADMIN.
-- No crea tablas ni columnas (eso lo hace migration_whatsapp_publicaciones.sql, que se corre antes).
-- Solo AGREGA filas; no borra ni cambia datos. Idempotente: se puede correr dos veces.
-- Después de correrla: volver a entrar (las pantallas viajan en el JWT).
-- ============================================================

INSERT INTO submenu (menu_id, nombre, ruta, icono, descripcion, orden, tiene_escritura)
SELECT b.menu_id, 'Publicaciones de WhatsApp', 'whatsapp/publicaciones', '💬',
       'Publica productos en tus grupos de WhatsApp (con foto, ahora o programado) y mira cuáles ya salieron. Solo administrador.',
       b.orden + 1, 1
FROM submenu b
WHERE b.ruta = 'productos/campanas'
  AND NOT EXISTS (SELECT 1 FROM submenu e WHERE e.ruta = 'whatsapp/publicaciones');

-- Respaldo si «Campañas» no existe en este ambiente: se cuelga de Buscar productos.
INSERT INTO submenu (menu_id, nombre, ruta, icono, descripcion, orden, tiene_escritura)
SELECT b.menu_id, 'Publicaciones de WhatsApp', 'whatsapp/publicaciones', '💬',
       'Publica productos en tus grupos de WhatsApp (con foto, ahora o programado) y mira cuáles ya salieron. Solo administrador.',
       b.orden + 1, 1
FROM submenu b
WHERE b.ruta = 'productos/buscar'
  AND NOT EXISTS (SELECT 1 FROM submenu e WHERE e.ruta = 'whatsapp/publicaciones');

INSERT INTO rol_submenu (rol_id, submenu_id)
SELECT ro.id, s.id
FROM roles ro
CROSS JOIN submenu s
WHERE s.ruta = 'whatsapp/publicaciones'
  AND ro.nombre_rol = 'ROLE_ADMIN'
  AND NOT EXISTS (SELECT 1 FROM rol_submenu e WHERE e.rol_id = ro.id AND e.submenu_id = s.id);

-- ============================================================
-- VERIFICACIÓN (debe devolver 1 fila en cada consulta)
-- ============================================================
-- SELECT s.id, s.ruta, s.nombre, s.menu_id, s.orden FROM submenu s WHERE s.ruta = 'whatsapp/publicaciones';
-- SELECT ro.nombre_rol, s.ruta FROM rol_submenu rs JOIN roles ro ON ro.id = rs.rol_id JOIN submenu s ON s.id = rs.submenu_id WHERE s.ruta = 'whatsapp/publicaciones';
