-- ============================================================
-- Migración: acción "cambiar-precio" en "tienda/buscar" (botón 💲 de la tarjeta)
--
-- Motivo (2026-09-22): a un producto le bajaron el precio y, como no había forma rápida de
-- cambiarlo, se armó una promoción solo para eso -- y la promoción se registró como pago en
-- efectivo cuando el cliente iba a ir pagando. El botón cambia el precio normal y el precio con
-- descuento del PRODUCTO (todos sus artículos lo heredan).
--
-- Backend: PUT /v1/precios/producto/{productoId} (hexagonal/precio).
-- Sin correr esta migración el endpoint responde 403 a todos, incluido el admin, y el botón no
-- aparece. Después de correrla hay que volver a entrar: los permisos viajan en el JWT.
--
-- Idempotente: los dos INSERT llevan NOT EXISTS.
-- ============================================================

-- DIAGNÓSTICO (debe devolver 1 fila: la pantalla existe)
-- SELECT id, ruta FROM submenu WHERE ruta = 'tienda/buscar';

INSERT INTO accion_submenu (submenu_id, clave, etiqueta, descripcion, orden)
SELECT s.id, 'cambiar-precio', 'Cambiar precio (💲 en la tarjeta)',
       'Botón 💲 de la tarjeta de Tienda. Cambia el precio normal y el precio con descuento del producto, para todos sus artículos. Los pedidos ya hechos conservan su precio.',
       19
FROM submenu s
WHERE s.ruta = 'tienda/buscar'
  AND NOT EXISTS (
    SELECT 1 FROM accion_submenu e WHERE e.submenu_id = s.id AND e.clave = 'cambiar-precio'
  );

-- Solo ROLE_ADMIN de arranque: es una acción de dinero. A otros roles se les marca a mano en
-- Gestión de roles.
INSERT INTO rol_accion (rol_id, accion_submenu_id)
SELECT r.id, a.id
FROM roles r
CROSS JOIN accion_submenu a
JOIN submenu s ON s.id = a.submenu_id AND s.ruta = 'tienda/buscar'
WHERE r.nombre_rol = 'ROLE_ADMIN'
  AND a.clave = 'cambiar-precio'
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
-- WHERE s.ruta = 'tienda/buscar' AND a.clave = 'cambiar-precio';
