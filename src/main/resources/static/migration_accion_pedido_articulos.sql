-- ============================================================
-- Migracion: acciones para editar los articulos de un pedido ya creado (2026-09-22).
--
-- Caso que las origina: hoy solo se puede QUITAR una linea (el boton "-"). Para agregar algo o
-- cambiar una talla hay que cancelar el pedido entero y rehacerlo -- que devuelve y vuelve a
-- descontar el stock, y deja registrado algo distinto de lo que realmente paso.
--
-- Son tres botones distintos y por eso tres acciones: se puede querer que alguien agregue
-- articulos sin poder desarmar una promocion, que es una decision de dinero mas grande.
--
-- Backend: dominio hexagonal `pedidoarticulo` (PedidoArticuloController).
-- ============================================================

INSERT INTO accion_submenu (submenu_id, clave, etiqueta, descripcion, categoria, orden)
SELECT s.id, 'agregar-articulo', 'Agregar articulo al pedido (+)',
       'Boton "+ Agregar articulo" dentro del detalle de un pedido ya creado. Se cobra a precio normal o de rebaja, nunca a un precio libre.',
       'Detalle del pedido', 15
FROM submenu s
WHERE s.ruta = 'pedidos/mis-pedidos'
  AND NOT EXISTS (SELECT 1 FROM accion_submenu e WHERE e.submenu_id = s.id AND e.clave = 'agregar-articulo');

INSERT INTO accion_submenu (submenu_id, clave, etiqueta, descripcion, categoria, orden)
SELECT s.id, 'cambiar-articulo', 'Cambiar un articulo del pedido (por otra talla o modelo)',
       'Boton "Cambiar" en cada linea del detalle. Si la linea es de una promocion, busca el reemplazo dentro del combo y avisa si no esta.',
       'Detalle del pedido', 16
FROM submenu s
WHERE s.ruta = 'pedidos/mis-pedidos'
  AND NOT EXISTS (SELECT 1 FROM accion_submenu e WHERE e.submenu_id = s.id AND e.clave = 'cambiar-articulo');

INSERT INTO accion_submenu (submenu_id, clave, etiqueta, descripcion, categoria, orden)
SELECT s.id, 'quitar-promocion', 'Quitar una promocion completa del pedido',
       'Saca de un solo golpe todas las lineas de una promocion y devuelve su stock. Es lo unico que puede desarmar un combo: una linea suelta no se quita.',
       'Detalle del pedido', 17
FROM submenu s
WHERE s.ruta = 'pedidos/mis-pedidos'
  AND NOT EXISTS (SELECT 1 FROM accion_submenu e WHERE e.submenu_id = s.id AND e.clave = 'quitar-promocion');

-- Solo ROLE_ADMIN de arranque -- igual que 'cambiar-tipo' y por la misma razon: editar lo que un
-- cliente ya compro mueve dinero y stock. Un cliente viendo sus propios pedidos no las recibe.
-- Si el negocio quiere darle 'agregar-articulo' a quien atiende el mostrador, se hace desde
-- Gestion de roles, sin tocar codigo.
INSERT INTO rol_accion (rol_id, accion_submenu_id)
SELECT r.id, a.id
FROM roles r
CROSS JOIN accion_submenu a
JOIN submenu s ON s.id = a.submenu_id AND s.ruta = 'pedidos/mis-pedidos'
WHERE r.nombre_rol = 'ROLE_ADMIN'
  AND a.clave IN ('agregar-articulo', 'cambiar-articulo', 'quitar-promocion')
  AND NOT EXISTS (
    SELECT 1 FROM rol_accion e WHERE e.rol_id = r.id AND e.accion_submenu_id = a.id
  );

-- ============================================================
-- VERIFICACION -- tienen que salir 3 filas, todas para ROLE_ADMIN
-- ============================================================
-- SELECT r.nombre_rol, a.clave, a.etiqueta, a.orden
-- FROM rol_accion ra
-- JOIN roles r ON r.id = ra.rol_id
-- JOIN accion_submenu a ON a.id = ra.accion_submenu_id
-- JOIN submenu s ON s.id = a.submenu_id
-- WHERE s.ruta = 'pedidos/mis-pedidos'
--   AND a.clave IN ('agregar-articulo', 'cambiar-articulo', 'quitar-promocion')
-- ORDER BY a.orden;
