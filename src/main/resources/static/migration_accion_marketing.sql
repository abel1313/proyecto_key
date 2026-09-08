-- ============================================================
-- Migración: acciones puntuales para el grupo Marketing (admin/promociones, admin/cinta) --
-- continuación del audit sistemático de permisos finos (2026-09-08).
--
-- "promociones" (Promociones activas, pública) es de cara al cliente (ver, agregar al carrito),
-- sin acciones admin. "admin/facebook" (Publicar en redes) y "admin/hashtags" son cada una un
-- único flujo (armar+publicar un post; editar hashtags por red) sin acciones puntuales separables
-- -- llegar a la pantalla YA implica poder usarla completa. Las 3 quedan fuera del audit.
-- ============================================================

-- admin/promociones: activar/desactivar + enviar correo de la promo son 2 acciones con sentido
-- de negocio propio (ej. un rol que arma promociones pero no debe mandar correos masivos).
INSERT INTO accion_submenu (submenu_id, clave, etiqueta, descripcion, orden)
SELECT s.id, 'habilitar', 'Activar / desactivar promoción',
       'Botón que activa/desactiva una promoción ya creada, en Gestionar promociones.',
       1
FROM submenu s
WHERE s.ruta = 'admin/promociones'
  AND NOT EXISTS (SELECT 1 FROM accion_submenu e WHERE e.submenu_id = s.id AND e.clave = 'habilitar');

INSERT INTO accion_submenu (submenu_id, clave, etiqueta, descripcion, orden)
SELECT s.id, 'enviar-correo', 'Enviar correo de la promoción',
       'Botón para enviar el correo masivo de una promoción vigente a los clientes, en Gestionar promociones.',
       2
FROM submenu s
WHERE s.ruta = 'admin/promociones'
  AND NOT EXISTS (SELECT 1 FROM accion_submenu e WHERE e.submenu_id = s.id AND e.clave = 'enviar-correo');

-- admin/cinta: activar/desactivar + eliminar frase de la cinta de anuncios.
INSERT INTO accion_submenu (submenu_id, clave, etiqueta, descripcion, orden)
SELECT s.id, 'habilitar', 'Activar / desactivar (👁️🚫 en la fila)',
       'Ícono para activar/desactivar una frase de la cinta de anuncios, en Cinta de anuncios.',
       1
FROM submenu s
WHERE s.ruta = 'admin/cinta'
  AND NOT EXISTS (SELECT 1 FROM accion_submenu e WHERE e.submenu_id = s.id AND e.clave = 'habilitar');

INSERT INTO accion_submenu (submenu_id, clave, etiqueta, descripcion, orden)
SELECT s.id, 'eliminar', 'Eliminar (🗑️ en la fila)',
       'Ícono para quitar una frase de la cinta de anuncios, en Cinta de anuncios.',
       2
FROM submenu s
WHERE s.ruta = 'admin/cinta'
  AND NOT EXISTS (SELECT 1 FROM accion_submenu e WHERE e.submenu_id = s.id AND e.clave = 'eliminar');

INSERT INTO rol_accion (rol_id, accion_submenu_id)
SELECT rs.rol_id, a.id
FROM rol_submenu rs
JOIN submenu s ON s.id = rs.submenu_id AND s.ruta IN ('admin/promociones', 'admin/cinta')
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
-- WHERE s.ruta IN ('admin/promociones', 'admin/cinta')
-- ORDER BY s.ruta, a.orden;
