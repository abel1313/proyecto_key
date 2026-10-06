-- ============================================================
-- Migración: Gestión de roles al día con Mis pedidos (2026-10-06).
--
-- Qué arregla:
--   1. Los 5 permisos de filtro que ya existían describen la pantalla de ANTES ("junto al
--      buscador por lugar", "🛒 Normal"). Desde el panel ⚙️ Filtros (dominio busquedapedido) esos
--      botones viven en los bloques "Forma de cobro" y "Estado", y "Normal" se llama "Contado".
--   2. Los bloques nuevos del panel (Pendiente, Por cobrar, Entregado, Dinero, Fecha de entrega,
--      Dónde se entrega, Unidos y otros, Registrado, Total) no tenían permiso: cualquiera con la
--      pantalla los veía y no se podían quitar desde Gestión de roles. Se dan de alta aquí.
--   3. "Registrar abono" ya no es solo del detalle: desde 2026-10-06 la tarjeta tiene Liquidar,
--      Dar abono, Liquidar el grupo y Abonar al grupo, y usan el mismo permiso. "Cobrar" quedó
--      solo para los pedidos de contado.
--   4. Gestión de roles agrupa por categoría solo si las acciones de una misma categoría
--      vienen con `orden` seguido (gestion-roles.component.ts, agruparPorCategoria). Las que se
--      agregaron después (cambiar-tipo, agregar-articulo, unir-pedidos...) traían órdenes
--      repetidos (17 dos veces). Se renumeran todas las de Mis pedidos.
--
-- No cambia lo que nadie puede hacer: las acciones nuevas se le dan a ROLE_ADMIN (que hoy es el
-- único que ve el panel), así que el administrador sigue viendo exactamente los mismos filtros.
-- Idempotente: correrla dos veces deja lo mismo. Hay que volver a entrar después de correrla
-- (los permisos viajan dentro del token).
-- ============================================================

-- Workbench trae Safe Updates prendido y rechaza los UPDATE por clave/ruta (no tienen índice).
-- Se apaga solo durante el script y se deja como estaba al final.
SET @safe_updates_antes = @@SQL_SAFE_UPDATES;
SET SQL_SAFE_UPDATES = 0;

START TRANSACTION;

-- ── 1. Acciones nuevas (bloques del panel de filtros) ────────────────────────────────────────
INSERT INTO accion_submenu (submenu_id, clave, etiqueta, descripcion, categoria, orden)
SELECT s.id, n.clave, n.etiqueta, n.descripcion, n.categoria, n.orden
FROM submenu s
JOIN (
    SELECT 'filtro-pendientes' AS clave, 'Filtro: Pendiente (⏳)' AS etiqueta,
           'Opción "⏳ Pendiente" del bloque Estado, en ⚙️ Filtros de Mis pedidos.' AS descripcion,
           'Filtros — estado' AS categoria, 4 AS orden
    UNION ALL SELECT 'filtro-por-cobrar', 'Filtro: Por cobrar (🕒)',
           'Opción "🕒 Por cobrar" del bloque Estado, en ⚙️ Filtros de Mis pedidos.',
           'Filtros — estado', 5
    UNION ALL SELECT 'filtro-entregados', 'Filtro: Entregado (🤝)',
           'Opción "🤝 Entregado" del bloque Estado, en ⚙️ Filtros de Mis pedidos.',
           'Filtros — estado', 7
    UNION ALL SELECT 'filtro-dinero', 'Filtro: Dinero (💰 Debe dinero, Sin abonos, Saldo a favor)',
           'Bloque "Dinero" de ⚙️ Filtros en Mis pedidos: Debe dinero, Sin abonos y Saldo a favor.',
           'Filtros — más filtros', 9
    UNION ALL SELECT 'filtro-fecha-entrega', 'Filtro: Fecha de entrega (📅 Hoy, Mañana, Atrasados)',
           'Bloque "Fecha de entrega" de ⚙️ Filtros en Mis pedidos: Hoy, Mañana, Esta semana y Atrasados.',
           'Filtros — más filtros', 10
    UNION ALL SELECT 'filtro-lugar', 'Filtro: Dónde se entrega (lugar, 🏪 Recoge, 🚚 Envío)',
           'Bloque "Dónde se entrega" de ⚙️ Filtros en Mis pedidos: buscador de lugar de entrega, Recoge en tienda y Envío.',
           'Filtros — más filtros', 11
    UNION ALL SELECT 'filtro-unidos-otros', 'Filtro: Unidos y otros (🔗, 💐, 🏷️)',
           'Bloque "Unidos y otros" de ⚙️ Filtros en Mis pedidos: Solo unidos, Sin unir, Ramos de flores y Con promoción.',
           'Filtros — más filtros', 12
    UNION ALL SELECT 'filtro-registrado', 'Filtro: Registrado (fechas desde / hasta)',
           'Bloque "Registrado" de ⚙️ Filtros en Mis pedidos: pedidos hechos entre dos fechas.',
           'Filtros — más filtros', 13
    UNION ALL SELECT 'filtro-total', 'Filtro: Total del pedido ($ desde / hasta)',
           'Bloque "Total del pedido" de ⚙️ Filtros en Mis pedidos: pedidos entre dos montos.',
           'Filtros — más filtros', 14
) n ON 1 = 1
WHERE s.ruta = 'pedidos/mis-pedidos'
  AND NOT EXISTS (SELECT 1 FROM accion_submenu e WHERE e.submenu_id = s.id AND e.clave = n.clave);

-- Solo ROLE_ADMIN, como todas las de esta pantalla: el administrador sigue viendo lo mismo.
INSERT INTO rol_accion (rol_id, accion_submenu_id)
SELECT r.id, a.id
FROM roles r
JOIN submenu s ON s.ruta = 'pedidos/mis-pedidos'
JOIN accion_submenu a ON a.submenu_id = s.id
WHERE r.nombre_rol = 'ROLE_ADMIN'
  AND a.clave IN ('filtro-pendientes', 'filtro-por-cobrar', 'filtro-entregados', 'filtro-dinero',
                  'filtro-fecha-entrega', 'filtro-lugar', 'filtro-unidos-otros',
                  'filtro-registrado', 'filtro-total')
  AND NOT EXISTS (SELECT 1 FROM rol_accion e WHERE e.rol_id = r.id AND e.accion_submenu_id = a.id);

-- ── 2. Textos, categoría y orden de las que ya existían ──────────────────────────────────────
-- Un UPDATE por fila con su clave: si una no existe en esta base, simplemente no toca nada.
UPDATE accion_submenu a JOIN submenu s ON s.id = a.submenu_id AND s.ruta = 'pedidos/mis-pedidos'
SET a.etiqueta = 'Filtro: Contado (🛒)',
    a.descripcion = 'Opción "🛒 Contado" del bloque Forma de cobro, en ⚙️ Filtros de Mis pedidos.',
    a.categoria = 'Filtros — forma de cobro', a.orden = 1
WHERE a.clave = 'filtro-normal';

UPDATE accion_submenu a JOIN submenu s ON s.id = a.submenu_id AND s.ruta = 'pedidos/mis-pedidos'
SET a.etiqueta = 'Filtro: Apartado (📦)',
    a.descripcion = 'Opción "📦 Apartado" del bloque Forma de cobro, en ⚙️ Filtros de Mis pedidos.',
    a.categoria = 'Filtros — forma de cobro', a.orden = 2
WHERE a.clave = 'filtro-apartado';

UPDATE accion_submenu a JOIN submenu s ON s.id = a.submenu_id AND s.ruta = 'pedidos/mis-pedidos'
SET a.etiqueta = 'Filtro: Ir pagando (💳)',
    a.descripcion = 'Opción "💳 Ir pagando" del bloque Forma de cobro, en ⚙️ Filtros de Mis pedidos.',
    a.categoria = 'Filtros — forma de cobro', a.orden = 3
WHERE a.clave = 'filtro-fiado';

UPDATE accion_submenu a JOIN submenu s ON s.id = a.submenu_id AND s.ruta = 'pedidos/mis-pedidos'
SET a.etiqueta = 'Filtro: Pagado (✅)',
    a.descripcion = 'Opción "✅ Pagado" del bloque Estado, en ⚙️ Filtros de Mis pedidos.',
    a.categoria = 'Filtros — estado', a.orden = 6
WHERE a.clave = 'filtro-pagados';

UPDATE accion_submenu a JOIN submenu s ON s.id = a.submenu_id AND s.ruta = 'pedidos/mis-pedidos'
SET a.etiqueta = 'Filtro: Cancelado (❌)',
    a.descripcion = 'Opción "❌ Cancelado" del bloque Estado, en ⚙️ Filtros de Mis pedidos.',
    a.categoria = 'Filtros — estado', a.orden = 8
WHERE a.clave = 'filtro-cancelados';

-- Tarjeta de pedido (15–19)
UPDATE accion_submenu a JOIN submenu s ON s.id = a.submenu_id AND s.ruta = 'pedidos/mis-pedidos'
SET a.orden = 15 WHERE a.clave = 'editar-entrega';

UPDATE accion_submenu a JOIN submenu s ON s.id = a.submenu_id AND s.ruta = 'pedidos/mis-pedidos'
SET a.etiqueta = 'Cobrar de contado (💲 en la tarjeta)',
    a.descripcion = 'Botón "Cobrar" en la tarjeta de un pedido de contado, suelto o unido. Apartado e Ir pagando se cobran con el permiso de abonar.',
    a.categoria = 'Tarjeta de pedido', a.orden = 16
WHERE a.clave = 'cobrar';

UPDATE accion_submenu a JOIN submenu s ON s.id = a.submenu_id AND s.ruta = 'pedidos/mis-pedidos'
SET a.orden = 17 WHERE a.clave = 'imprimir-ticket';

UPDATE accion_submenu a JOIN submenu s ON s.id = a.submenu_id AND s.ruta = 'pedidos/mis-pedidos'
SET a.orden = 18 WHERE a.clave = 'enviar-correo';

UPDATE accion_submenu a JOIN submenu s ON s.id = a.submenu_id AND s.ruta = 'pedidos/mis-pedidos'
SET a.orden = 19 WHERE a.clave = 'cancelar-pedido';

-- "Abonar" se pinta junto a la tarjeta porque ahora también vive ahí (Liquidar, Dar abono...).
UPDATE accion_submenu a JOIN submenu s ON s.id = a.submenu_id AND s.ruta = 'pedidos/mis-pedidos'
SET a.etiqueta = 'Abonar y liquidar (tarjeta y detalle)',
    a.descripcion = 'Tarjeta: Liquidar, Dar abono, Liquidar el grupo y Abonar al grupo. Detalle: 💳 Registrar abono y 💵 Abonar al grupo. Solo Apartado e Ir pagando.',
    a.categoria = 'Tarjeta de pedido', a.orden = 20
WHERE a.clave = 'abonar';

-- Detalle del pedido (21–27)
UPDATE accion_submenu a JOIN submenu s ON s.id = a.submenu_id AND s.ruta = 'pedidos/mis-pedidos'
SET a.orden = 21 WHERE a.clave = 'editar-ramo';

UPDATE accion_submenu a JOIN submenu s ON s.id = a.submenu_id AND s.ruta = 'pedidos/mis-pedidos'
SET a.etiqueta = 'Quitar piezas de un artículo (− en el detalle)',
    a.descripcion = 'Botón "−" de cada línea del detalle: quita piezas de esa línea; la cuenta es solo de ese pedido.',
    a.categoria = 'Detalle del pedido', a.orden = 22
WHERE a.clave = 'ajustar-cantidad';

UPDATE accion_submenu a JOIN submenu s ON s.id = a.submenu_id AND s.ruta = 'pedidos/mis-pedidos'
SET a.categoria = 'Detalle del pedido', a.orden = 23 WHERE a.clave = 'cambiar-tipo';

UPDATE accion_submenu a JOIN submenu s ON s.id = a.submenu_id AND s.ruta = 'pedidos/mis-pedidos'
SET a.etiqueta = 'Agregar artículo al pedido (➕)',
    a.categoria = 'Detalle del pedido', a.orden = 24
WHERE a.clave = 'agregar-articulo';

UPDATE accion_submenu a JOIN submenu s ON s.id = a.submenu_id AND s.ruta = 'pedidos/mis-pedidos'
SET a.etiqueta = 'Cambiar un artículo del pedido (⇄ por otra talla o modelo)',
    a.categoria = 'Detalle del pedido', a.orden = 25
WHERE a.clave = 'cambiar-articulo';

UPDATE accion_submenu a JOIN submenu s ON s.id = a.submenu_id AND s.ruta = 'pedidos/mis-pedidos'
SET a.etiqueta = 'Quitar una promoción completa del pedido',
    a.categoria = 'Detalle del pedido', a.orden = 26
WHERE a.clave = 'quitar-promocion';

UPDATE accion_submenu a JOIN submenu s ON s.id = a.submenu_id AND s.ruta = 'pedidos/mis-pedidos'
SET a.etiqueta = 'Unir y separar pedidos (🔗)',
    a.descripcion = 'En el detalle: 🔗 Unir pedidos, agregar más pedidos al grupo y separarlo repartiendo lo que dio el cliente.',
    a.categoria = 'Detalle del pedido', a.orden = 27
WHERE a.clave = 'unir-pedidos';

-- ── 3. Descripción de la pantalla Créditos / Abonos (decía "fiado") ─────────────────────────
UPDATE submenu
SET descripcion = 'Pedidos Apartado e Ir pagando: registrar abonos y liquidarlos. Vive en el menú: Ventas → Créditos / Abonos.'
WHERE ruta = 'abonos';

COMMIT;

SET SQL_SAFE_UPDATES = @safe_updates_antes;

-- ── Verificación (debe salir una fila por acción, con categorías seguidas y orden 1–27) ─────
-- SELECT a.orden, a.categoria, a.clave, a.etiqueta,
--        EXISTS (SELECT 1 FROM rol_accion ra JOIN roles r ON r.id = ra.rol_id
--                WHERE ra.accion_submenu_id = a.id AND r.nombre_rol = 'ROLE_ADMIN') AS admin
-- FROM accion_submenu a JOIN submenu s ON s.id = a.submenu_id
-- WHERE s.ruta = 'pedidos/mis-pedidos'
-- ORDER BY a.orden;
--
-- Debe dar 0 (ninguna acción de Mis pedidos sin el administrador):
-- SELECT COUNT(*) FROM accion_submenu a JOIN submenu s ON s.id = a.submenu_id
-- WHERE s.ruta = 'pedidos/mis-pedidos'
--   AND NOT EXISTS (SELECT 1 FROM rol_accion ra JOIN roles r ON r.id = ra.rol_id
--                   WHERE ra.accion_submenu_id = a.id AND r.nombre_rol = 'ROLE_ADMIN');
