-- ============================================================
-- Migración: acciones puntuales para pedidos/mis-pedidos -- criterio de negocio confirmado por
-- el usuario 2026-09-08 (pantalla de dinero, ver AUDIT_PERMISOS_FINOS_TODAS_PANTALLAS.md).
--
-- Pedido explícito: separar los 2 grupos de filtros por su propio nombre (mismo agrupamiento que
-- ya existe en el HTML -- "Grupo 1": buscador de pedido + Pagados/Cancelados; "Grupo 2":
-- buscador por lugar + Normal/Apartados/Ir pagando), cada botón de la tarjeta con su propia
-- acción, y cada botón del detalle (al entrar a un pedido) con la suya.
--
-- Todos estos botones hoy están gateados con `isAdminUser` (hardcodeado a ROLE_ADMIN, no usa el
-- sistema de permisos finos) o completamente abiertos (Entrega, Ticket, Cancelar -- los ve
-- cualquiera con acceso a la pantalla, incluido un cliente viendo sus propios pedidos). El
-- template se queda con `isAdminUser` como filtro estructural donde ya existía (evita romper la
-- vista del cliente) y se le agrega `tieneAccion` encima para el control fino.
-- ============================================================

-- Categoría "Filtros — buscador de pedido"
INSERT INTO accion_submenu (submenu_id, clave, etiqueta, descripcion, categoria, orden)
SELECT s.id, 'filtro-pagados', 'Filtro: Pagados (✅)',
       'Botón "✅ Pagados" junto al buscador por número de pedido, en Mis pedidos.',
       'Filtros — buscador de pedido', 1
FROM submenu s
WHERE s.ruta = 'pedidos/mis-pedidos'
  AND NOT EXISTS (SELECT 1 FROM accion_submenu e WHERE e.submenu_id = s.id AND e.clave = 'filtro-pagados');

INSERT INTO accion_submenu (submenu_id, clave, etiqueta, descripcion, categoria, orden)
SELECT s.id, 'filtro-cancelados', 'Filtro: Cancelados (❌)',
       'Botón "❌ Cancelados" junto al buscador por número de pedido, en Mis pedidos.',
       'Filtros — buscador de pedido', 2
FROM submenu s
WHERE s.ruta = 'pedidos/mis-pedidos'
  AND NOT EXISTS (SELECT 1 FROM accion_submenu e WHERE e.submenu_id = s.id AND e.clave = 'filtro-cancelados');

-- Categoría "Filtros — buscador por lugar"
INSERT INTO accion_submenu (submenu_id, clave, etiqueta, descripcion, categoria, orden)
SELECT s.id, 'filtro-normal', 'Filtro: Normal (🛒)',
       'Botón "🛒 Normal" junto al buscador por lugar de entrega, en Mis pedidos.',
       'Filtros — buscador por lugar', 3
FROM submenu s
WHERE s.ruta = 'pedidos/mis-pedidos'
  AND NOT EXISTS (SELECT 1 FROM accion_submenu e WHERE e.submenu_id = s.id AND e.clave = 'filtro-normal');

INSERT INTO accion_submenu (submenu_id, clave, etiqueta, descripcion, categoria, orden)
SELECT s.id, 'filtro-apartado', 'Filtro: Apartados (📦)',
       'Botón "📦 Apartados" junto al buscador por lugar de entrega, en Mis pedidos.',
       'Filtros — buscador por lugar', 4
FROM submenu s
WHERE s.ruta = 'pedidos/mis-pedidos'
  AND NOT EXISTS (SELECT 1 FROM accion_submenu e WHERE e.submenu_id = s.id AND e.clave = 'filtro-apartado');

INSERT INTO accion_submenu (submenu_id, clave, etiqueta, descripcion, categoria, orden)
SELECT s.id, 'filtro-fiado', 'Filtro: Ir pagando (💳)',
       'Botón "💳 Ir pagando" junto al buscador por lugar de entrega, en Mis pedidos.',
       'Filtros — buscador por lugar', 5
FROM submenu s
WHERE s.ruta = 'pedidos/mis-pedidos'
  AND NOT EXISTS (SELECT 1 FROM accion_submenu e WHERE e.submenu_id = s.id AND e.clave = 'filtro-fiado');

-- Categoría "Tarjeta de pedido"
INSERT INTO accion_submenu (submenu_id, clave, etiqueta, descripcion, categoria, orden)
SELECT s.id, 'editar-entrega', 'Entrega (🚚 en la tarjeta)',
       'Botón "Entrega" para agregar/editar la info de entrega, en la tarjeta de cada pedido.',
       'Tarjeta de pedido', 6
FROM submenu s
WHERE s.ruta = 'pedidos/mis-pedidos'
  AND NOT EXISTS (SELECT 1 FROM accion_submenu e WHERE e.submenu_id = s.id AND e.clave = 'editar-entrega');

INSERT INTO accion_submenu (submenu_id, clave, etiqueta, descripcion, categoria, orden)
SELECT s.id, 'cobrar', 'Cobrar (💲 en la tarjeta)',
       'Botón "Cobrar" en la tarjeta de cada pedido, en Mis pedidos.',
       'Tarjeta de pedido', 7
FROM submenu s
WHERE s.ruta = 'pedidos/mis-pedidos'
  AND NOT EXISTS (SELECT 1 FROM accion_submenu e WHERE e.submenu_id = s.id AND e.clave = 'cobrar');

INSERT INTO accion_submenu (submenu_id, clave, etiqueta, descripcion, categoria, orden)
SELECT s.id, 'imprimir-ticket', 'Imprimir ticket (🖨️)',
       'Botón de imprimir en la tarjeta de cada pedido y en el detalle, en Mis pedidos.',
       'Tarjeta de pedido', 8
FROM submenu s
WHERE s.ruta = 'pedidos/mis-pedidos'
  AND NOT EXISTS (SELECT 1 FROM accion_submenu e WHERE e.submenu_id = s.id AND e.clave = 'imprimir-ticket');

INSERT INTO accion_submenu (submenu_id, clave, etiqueta, descripcion, categoria, orden)
SELECT s.id, 'enviar-correo', 'Enviar comprobante por correo (✉️)',
       'Botón de enviar/reenviar comprobante por correo, en la tarjeta de cada pedido y en el detalle, en Mis pedidos.',
       'Tarjeta de pedido', 9
FROM submenu s
WHERE s.ruta = 'pedidos/mis-pedidos'
  AND NOT EXISTS (SELECT 1 FROM accion_submenu e WHERE e.submenu_id = s.id AND e.clave = 'enviar-correo');

INSERT INTO accion_submenu (submenu_id, clave, etiqueta, descripcion, categoria, orden)
SELECT s.id, 'cancelar-pedido', 'Cancelar pedido (✕ en la tarjeta)',
       'Botón "Cancelar" en la tarjeta de cada pedido, en Mis pedidos.',
       'Tarjeta de pedido', 10
FROM submenu s
WHERE s.ruta = 'pedidos/mis-pedidos'
  AND NOT EXISTS (SELECT 1 FROM accion_submenu e WHERE e.submenu_id = s.id AND e.clave = 'cancelar-pedido');

-- Categoría "Detalle del pedido"
INSERT INTO accion_submenu (submenu_id, clave, etiqueta, descripcion, categoria, orden)
SELECT s.id, 'editar-ramo', 'Editar ramo de flores (botón en el detalle)',
       'Botón para editar la configuración del ramo de flores, dentro del detalle de un pedido.',
       'Detalle del pedido', 11
FROM submenu s
WHERE s.ruta = 'pedidos/mis-pedidos'
  AND NOT EXISTS (SELECT 1 FROM accion_submenu e WHERE e.submenu_id = s.id AND e.clave = 'editar-ramo');

INSERT INTO accion_submenu (submenu_id, clave, etiqueta, descripcion, categoria, orden)
SELECT s.id, 'abonar', 'Registrar abono (botón + formulario en el detalle)',
       'Botón "+ Abono" y su formulario, dentro del detalle de un pedido a crédito.',
       'Detalle del pedido', 12
FROM submenu s
WHERE s.ruta = 'pedidos/mis-pedidos'
  AND NOT EXISTS (SELECT 1 FROM accion_submenu e WHERE e.submenu_id = s.id AND e.clave = 'abonar');

INSERT INTO accion_submenu (submenu_id, clave, etiqueta, descripcion, categoria, orden)
SELECT s.id, 'ajustar-cantidad', 'Reducir cantidad de un artículo (− en el detalle)',
       'Botón "−" para quitar artículos de un pedido ya confirmado, dentro del detalle.',
       'Detalle del pedido', 13
FROM submenu s
WHERE s.ruta = 'pedidos/mis-pedidos'
  AND NOT EXISTS (SELECT 1 FROM accion_submenu e WHERE e.submenu_id = s.id AND e.clave = 'ajustar-cantidad');

-- Se dan a ROLE_ADMIN para preservar el comportamiento actual (todo esto hoy depende de
-- isAdminUser, hardcodeado a ROLE_ADMIN). El resto de roles con Ver en la pantalla (clientes
-- viendo sus propios pedidos) NO las recibe -- Entrega/Ticket/Cancelar seguían abiertas para
-- ellos porque el template las deja fuera del `isAdminUser &&`, ver comentario en el componente.
INSERT INTO rol_accion (rol_id, accion_submenu_id)
SELECT r.id, a.id
FROM roles r
CROSS JOIN accion_submenu a
JOIN submenu s ON s.id = a.submenu_id AND s.ruta = 'pedidos/mis-pedidos'
WHERE r.nombre_rol = 'ROLE_ADMIN'
  AND NOT EXISTS (
    SELECT 1 FROM rol_accion e WHERE e.rol_id = r.id AND e.accion_submenu_id = a.id
  );

-- Entrega/Ticket/Cancelar/Abonar tambien se le dan a TODO rol con Ver en la pantalla (preserva
-- que hoy estan abiertas para cualquiera, no solo ROLE_ADMIN -- incluye al cliente pagando su
-- propio pedido a credito, "Registrar abono" no tiene ningun gate de isAdmin en el codigo actual).
INSERT INTO rol_accion (rol_id, accion_submenu_id)
SELECT rs.rol_id, a.id
FROM rol_submenu rs
JOIN submenu s ON s.id = rs.submenu_id AND s.ruta = 'pedidos/mis-pedidos'
JOIN accion_submenu a ON a.submenu_id = s.id AND a.clave IN ('editar-entrega', 'imprimir-ticket', 'cancelar-pedido', 'abonar')
WHERE NOT EXISTS (
    SELECT 1 FROM rol_accion e WHERE e.rol_id = rs.rol_id AND e.accion_submenu_id = a.id
  );

-- ============================================================
-- VERIFICACIÓN
-- ============================================================
-- SELECT a.categoria, a.clave, a.etiqueta, a.orden
-- FROM accion_submenu a
-- JOIN submenu s ON s.id = a.submenu_id
-- WHERE s.ruta = 'pedidos/mis-pedidos'
-- ORDER BY a.orden;
