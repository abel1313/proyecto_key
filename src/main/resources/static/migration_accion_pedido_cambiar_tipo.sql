-- ============================================================
-- Migracion: accion "cambiar-tipo" en pedidos/mis-pedidos -- cambiar la forma de cobro de un
-- pedido YA CREADO (2026-09-22).
--
-- Caso real que la origina: se aparto un pedido, al ir a entregarlo el cliente decidio pagarlo
-- completo. No habia forma de cambiarlo, asi que quedo registrado como apartado; la alternativa
-- era cancelar y rehacer el pedido entero, que ademas devuelve y vuelve a descontar el stock.
--
-- Es una accion de dinero: mueve un pedido entre NORMAL / APARTADO / FIADO y, cuando el cliente
-- paga en ese momento, registra el abono. Por eso va como accion puntual y NO colgada del
-- "Editar" general de la pantalla: se le puede dar a quien cobra sin darle el resto.
--
-- Backend: PUT /v1/pedidos/{id}/tipo (PedidoController.cambiarTipoPedido).
-- ============================================================

INSERT INTO accion_submenu (submenu_id, clave, etiqueta, descripcion, categoria, orden)
SELECT s.id, 'cambiar-tipo', 'Cambiar forma de cobro del pedido (🔁)',
       'Boton para cambiar un pedido ya creado entre Normal (contado), Apartado e Ir pagando. Si el cliente paga en ese momento, cobra y deja el abono con la nota de lo que paso.',
       'Detalle del pedido', 14
FROM submenu s
WHERE s.ruta = 'pedidos/mis-pedidos'
  AND NOT EXISTS (SELECT 1 FROM accion_submenu e WHERE e.submenu_id = s.id AND e.clave = 'cambiar-tipo');

-- Solo ROLE_ADMIN de arranque. A diferencia de Entrega/Ticket/Cancelar/Abonar, esta NO se le da
-- a todo rol con Ver en la pantalla: un cliente viendo sus propios pedidos no puede convertir su
-- apartado en fiado. Si el negocio quiere darsela a otro rol (ej. quien atiende el mostrador), se
-- hace desde Gestion de roles, sin tocar codigo.
INSERT INTO rol_accion (rol_id, accion_submenu_id)
SELECT r.id, a.id
FROM roles r
CROSS JOIN accion_submenu a
JOIN submenu s ON s.id = a.submenu_id AND s.ruta = 'pedidos/mis-pedidos'
WHERE r.nombre_rol = 'ROLE_ADMIN'
  AND a.clave = 'cambiar-tipo'
  AND NOT EXISTS (
    SELECT 1 FROM rol_accion e WHERE e.rol_id = r.id AND e.accion_submenu_id = a.id
  );

-- ============================================================
-- VERIFICACION
-- ============================================================
-- SELECT r.nombre_rol, a.clave, a.etiqueta
-- FROM rol_accion ra
-- JOIN roles r ON r.id = ra.rol_id
-- JOIN accion_submenu a ON a.id = ra.accion_submenu_id
-- JOIN submenu s ON s.id = a.submenu_id
-- WHERE s.ruta = 'pedidos/mis-pedidos' AND a.clave = 'cambiar-tipo';
