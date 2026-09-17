-- ============================================================
-- Migración: acción "eliminar" en "tienda/buscar" (dar de baja un modelo)
--
-- Motivo: en QA (2026-09-17) se reportó que no hay forma de dar de baja un modelo ni un
-- producto desde la aplicación. En Modelos (productos/buscar) el botón ✕ ya existe y cuelga de
-- la acción "eliminar"; en Tienda no existía ni el botón ni la acción, y el endpoint que el
-- front tenía cableado (DELETE /v1/variantes/delete, heredado del AbstractController) es un
-- stub que no hace nada y devuelve null. Se agregó DELETE /v1/variantes/deleteBy/{id} en el
-- back y esta migración da de alta el permiso que muestra el botón.
--
-- OJO -- es BAJA LÓGICA, no borrado: deja la variante en habilitado=0 y le borra las imágenes.
-- No se puede borrar la fila porque hay 13 tablas que apuntan a variante (detalle_pedido,
-- detalle_venta_variante, resena, favorito, promocion_detalle, configurar_rifa_variante...) y
-- el historial de ventas y pedidos quedaría apuntando a una fila inexistente. Mismo criterio
-- que ProductosServiceImpl.deleteByIdProducto().
-- ============================================================

INSERT INTO accion_submenu (submenu_id, clave, etiqueta, descripcion, orden)
SELECT s.id, v.clave, v.etiqueta, v.descripcion, v.orden
FROM submenu s
CROSS JOIN (
    SELECT 'eliminar' AS clave, 'Dar de baja modelo (✕ en la tarjeta)' AS etiqueta,
           'Botón ✕ de la tarjeta de cada modelo, en Tienda. Da de baja el modelo (deja de mostrarse y pierde sus fotos), no borra el historial de ventas ni de pedidos.' AS descripcion, 18 AS orden
) v
WHERE s.ruta = 'tienda/buscar'
  AND NOT EXISTS (
    SELECT 1 FROM accion_submenu existente WHERE existente.submenu_id = s.id AND existente.clave = v.clave
  );

-- ------------------------------------------------------------
-- Solo a ROLE_ADMIN: dar de baja es destructivo (pierde las fotos), así que no se reparte a
-- otros roles. Quien lo necesite se le marca a mano en Gestión de roles.
-- ------------------------------------------------------------
INSERT INTO rol_accion (rol_id, accion_submenu_id)
SELECT r.id, a.id
FROM roles r
CROSS JOIN accion_submenu a
JOIN submenu s ON s.id = a.submenu_id AND s.ruta = 'tienda/buscar'
WHERE r.nombre_rol = 'ROLE_ADMIN'
  AND a.clave = 'eliminar'
  AND NOT EXISTS (
    SELECT 1 FROM rol_accion existente WHERE existente.rol_id = r.id AND existente.accion_submenu_id = a.id
  );

-- ============================================================
-- VERIFICACIÓN -- debe devolver al menos una fila
-- ============================================================
-- SELECT s.ruta, a.clave, a.etiqueta, r.nombre_rol
-- FROM rol_accion ra
-- JOIN roles r ON r.id = ra.rol_id
-- JOIN accion_submenu a ON a.id = ra.accion_submenu_id
-- JOIN submenu s ON s.id = a.submenu_id
-- WHERE a.clave = 'eliminar' AND s.ruta = 'tienda/buscar';
