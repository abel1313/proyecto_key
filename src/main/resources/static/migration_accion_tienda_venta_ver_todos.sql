-- ============================================================
-- Migración: acción "ver-todos-los-modelos" en "tienda/venta" (🧩 Agregar producto = Agregar artículo)
--
-- Motivo (2026-10-08): el dueño pidió que en Agregar artículo se puedan buscar TODOS los modelos,
-- también los que no tienen stock, los deshabilitados y los dados de baja, y que al elegir uno la
-- pantalla diga en qué estado está y deje habilitarlo o subirle stock ahí mismo.
--
-- Backend: GET /v1/productos/buscarNombreOrCodigoBarra?todos=true. Sin esta acción (y sin ser
-- admin) el parámetro se ignora y el buscador trae lo de siempre: con stock, habilitados y con foto.
-- El admin los ve siempre, con o sin la migración. Después de correrla hay que volver a entrar:
-- los permisos viajan en el JWT.
--
-- Idempotente: los dos INSERT llevan NOT EXISTS.
-- ============================================================

-- DIAGNÓSTICO (debe devolver 1 fila: la pantalla existe)
-- SELECT id, ruta FROM submenu WHERE ruta = 'tienda/venta';

INSERT INTO accion_submenu (submenu_id, clave, etiqueta, descripcion, orden)
SELECT s.id, 'ver-todos-los-modelos', 'Ver todos los modelos (sin stock, deshabilitados o dados de baja)',
       'En el buscador de modelos salen también los que no tienen stock, los deshabilitados y los dados de baja, con una etiqueta de su estado. Al elegir uno se puede habilitar (si además tiene el permiso Habilitar de Modelos) o subirle stock.',
       10
FROM submenu s
WHERE s.ruta = 'tienda/venta'
  AND NOT EXISTS (
    SELECT 1 FROM accion_submenu e WHERE e.submenu_id = s.id AND e.clave = 'ver-todos-los-modelos'
  );

-- Solo ROLE_ADMIN de arranque. A otros roles se les marca a mano en Gestión de roles.
INSERT INTO rol_accion (rol_id, accion_submenu_id)
SELECT r.id, a.id
FROM roles r
CROSS JOIN accion_submenu a
JOIN submenu s ON s.id = a.submenu_id AND s.ruta = 'tienda/venta'
WHERE r.nombre_rol = 'ROLE_ADMIN'
  AND a.clave = 'ver-todos-los-modelos'
  AND NOT EXISTS (
    SELECT 1 FROM rol_accion e WHERE e.rol_id = r.id AND e.accion_submenu_id = a.id
  );

-- ============================================================
-- VERIFICACIÓN -- debe devolver al menos una fila (ROLE_ADMIN)
-- ============================================================
-- SELECT s.ruta, a.clave, a.etiqueta, r.nombre_rol
-- FROM rol_accion ra
-- JOIN roles r ON r.id = ra.rol_id
-- JOIN accion_submenu a ON a.id = ra.accion_submenu_id
-- JOIN submenu s ON s.id = a.submenu_id
-- WHERE s.ruta = 'tienda/venta' AND a.clave = 'ver-todos-los-modelos';
