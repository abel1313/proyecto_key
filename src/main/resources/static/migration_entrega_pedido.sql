-- ============================================================
-- Migración: la entrega se guarda aparte del pago (2026-10-06).
--
-- Regla del dueño (PLAN_PEDIDOS_VENTAS_ENTREGA.md §11, E1–E10): la card de un pedido dice siempre
-- dos cosas, el pago (Pagado / Falta pagar) y la entrega (Entregado / Falta entregar). Hasta hoy
-- solo existía estado_pedido, que mezclaba las dos: un contado cobrado quedaba 'Entregado' y un
-- Apartado liquidado 'PAGADO' sin saber si se lo llevó.
--
-- Qué hace:
--   1. Agrega pedidos.entregado (0/1) y pedidos.fecha_entregado. estado_pedido NO cambia: sigue
--      diciendo solo el pago.
--   2. Solo la PRIMERA vez (cuando la columna todavía no existía) llena la entrega de los pedidos
--      que ya existen (E10):
--        - Entregado: contado cobrado ('Entregado'), cualquier Ir pagando (se lo llevó) y todo
--          Apartado / Ir pagando ya liquidado ('PAGADO'). Es historial.
--        - Falta entregar: Apartados abiertos y contados sin cobrar ('Pendiente'). Los cancelados
--          se quedan en 0 (no importa: un cancelado no se entrega).
--      Si se corre otra vez, NO vuelve a tocar la entrega de nadie (ya pudo cambiar a mano).
--   3. Gestión de roles: dos acciones nuevas en Mis pedidos, solo para ROLE_ADMIN:
--        - entregar          → botón "📦 Entregar" (tarjeta y detalle)
--        - regresar-entrega  → "Regresar a Falta entregar" (por si se marcó por error)
--      y cambia los textos de los filtros de Estado, que ahora son dos bloques: Pago y Entrega.
--      Las claves de los filtros NO cambian, solo su texto.
--
-- Correrla ANTES de subir el back que la usa: la entidad Pedido ya mapea las dos columnas y sin
-- ellas falla cualquier consulta de pedidos. Idempotente. Hay que volver a entrar después (los
-- permisos viajan dentro del token).
-- ============================================================

SET @safe_updates_antes = @@SQL_SAFE_UPDATES;
SET SQL_SAFE_UPDATES = 0;

-- ── 1. Columnas ─────────────────────────────────────────────────────────────────────────────
SET @entregado_existia = (SELECT COUNT(*) FROM information_schema.columns
                          WHERE table_schema = DATABASE() AND table_name = 'pedidos'
                            AND column_name = 'entregado');

SET @sql = IF(@entregado_existia = 0,
    'ALTER TABLE pedidos ADD COLUMN entregado TINYINT(1) NOT NULL DEFAULT 0 COMMENT ''1 = el cliente ya se lo llevo (aparte del pago)''',
    'SELECT ''entregado ya existe''');
PREPARE st FROM @sql; EXECUTE st; DEALLOCATE PREPARE st;

SET @sql = IF((SELECT COUNT(*) FROM information_schema.columns
               WHERE table_schema = DATABASE() AND table_name = 'pedidos'
                 AND column_name = 'fecha_entregado') = 0,
    'ALTER TABLE pedidos ADD COLUMN fecha_entregado DATETIME NULL COMMENT ''Cuando se marco como entregado''',
    'SELECT ''fecha_entregado ya existe''');
PREPARE st FROM @sql; EXECUTE st; DEALLOCATE PREPARE st;

START TRANSACTION;

-- ── 2. Pedidos que ya existen (solo la primera vez) ─────────────────────────────────────────
UPDATE pedidos p
SET p.entregado = 1,
    p.fecha_entregado = COALESCE(p.fecha_hora_registro, CAST(p.fecha_pedido AS DATETIME))
WHERE @entregado_existia = 0
  AND LOWER(COALESCE(p.estado_pedido, '')) <> 'cancelado'
  AND (LOWER(COALESCE(p.estado_pedido, '')) IN ('entregado', 'pagado')
       OR p.tipo_pedido = 'FIADO');

-- ── 3. Gestión de roles ─────────────────────────────────────────────────────────────────────
INSERT INTO accion_submenu (submenu_id, clave, etiqueta, descripcion, categoria, orden)
SELECT s.id, n.clave, n.etiqueta, n.descripcion, n.categoria, n.orden
FROM submenu s
JOIN (
    SELECT 'entregar' AS clave, 'Entregar (📦 en la tarjeta y el detalle)' AS etiqueta,
           'Botón "📦 Entregar": marca que el cliente ya se llevó el pedido (o todo el grupo si está unido). También la pregunta "¿Ya se lo llevó?" al cobrar.' AS descripcion,
           'Tarjeta de pedido' AS categoria, 21 AS orden
    UNION ALL SELECT 'regresar-entrega', 'Regresar a "Falta entregar"',
           'Deshacer un "Entregado" marcado por error. Por default solo el administrador.',
           'Tarjeta de pedido', 22
) n ON 1 = 1
WHERE s.ruta = 'pedidos/mis-pedidos'
  AND NOT EXISTS (SELECT 1 FROM accion_submenu e WHERE e.submenu_id = s.id AND e.clave = n.clave);

INSERT INTO rol_accion (rol_id, accion_submenu_id)
SELECT r.id, a.id
FROM roles r
JOIN submenu s ON s.ruta = 'pedidos/mis-pedidos'
JOIN accion_submenu a ON a.submenu_id = s.id
WHERE r.nombre_rol = 'ROLE_ADMIN'
  AND a.clave IN ('entregar', 'regresar-entrega')
  AND NOT EXISTS (SELECT 1 FROM rol_accion e WHERE e.rol_id = r.id AND e.accion_submenu_id = a.id);

-- El bloque Estado se parte en Pago y Entrega. Mismas claves, textos nuevos.
UPDATE accion_submenu a JOIN submenu s ON s.id = a.submenu_id AND s.ruta = 'pedidos/mis-pedidos'
SET a.etiqueta = 'Filtro: Falta pagar (💰)',
    a.descripcion = 'Opción "💰 Falta pagar" del bloque Pago, en ⚙️ Filtros de Mis pedidos.',
    a.categoria = 'Filtros — pago', a.orden = 4
WHERE a.clave = 'filtro-por-cobrar';

UPDATE accion_submenu a JOIN submenu s ON s.id = a.submenu_id AND s.ruta = 'pedidos/mis-pedidos'
SET a.etiqueta = 'Filtro: Pagado (✅)',
    a.descripcion = 'Opción "✅ Pagado" del bloque Pago, en ⚙️ Filtros de Mis pedidos.',
    a.categoria = 'Filtros — pago', a.orden = 5
WHERE a.clave = 'filtro-pagados';

UPDATE accion_submenu a JOIN submenu s ON s.id = a.submenu_id AND s.ruta = 'pedidos/mis-pedidos'
SET a.etiqueta = 'Filtro: Cancelado (❌)',
    a.descripcion = 'Opción "❌ Cancelado" del bloque Pago, en ⚙️ Filtros de Mis pedidos.',
    a.categoria = 'Filtros — pago', a.orden = 6
WHERE a.clave = 'filtro-cancelados';

UPDATE accion_submenu a JOIN submenu s ON s.id = a.submenu_id AND s.ruta = 'pedidos/mis-pedidos'
SET a.etiqueta = 'Filtro: Falta entregar (📦)',
    a.descripcion = 'Opción "📦 Falta entregar" del bloque Entrega, en ⚙️ Filtros de Mis pedidos.',
    a.categoria = 'Filtros — entrega', a.orden = 7
WHERE a.clave = 'filtro-pendientes';

UPDATE accion_submenu a JOIN submenu s ON s.id = a.submenu_id AND s.ruta = 'pedidos/mis-pedidos'
SET a.etiqueta = 'Filtro: Entregado (🤝)',
    a.descripcion = 'Opción "🤝 Entregado" del bloque Entrega, en ⚙️ Filtros de Mis pedidos.',
    a.categoria = 'Filtros — entrega', a.orden = 8
WHERE a.clave = 'filtro-entregados';

-- Detalle del pedido se recorre 2 lugares para dejar 21 y 22 a la tarjeta (las categorías tienen
-- que llevar orden seguido o Gestión de roles las parte en dos grupos).
UPDATE accion_submenu a JOIN submenu s ON s.id = a.submenu_id AND s.ruta = 'pedidos/mis-pedidos'
SET a.orden = CASE a.clave
        WHEN 'editar-ramo'      THEN 23
        WHEN 'ajustar-cantidad' THEN 24
        WHEN 'cambiar-tipo'     THEN 25
        WHEN 'agregar-articulo' THEN 26
        WHEN 'cambiar-articulo' THEN 27
        WHEN 'quitar-promocion' THEN 28
        WHEN 'unir-pedidos'     THEN 29
    END
WHERE a.clave IN ('editar-ramo', 'ajustar-cantidad', 'cambiar-tipo', 'agregar-articulo',
                  'cambiar-articulo', 'quitar-promocion', 'unir-pedidos');

COMMIT;

SET SQL_SAFE_UPDATES = @safe_updates_antes;

-- ── Verificación ────────────────────────────────────────────────────────────────────────────
-- 1) Las dos columnas existen:
-- SELECT column_name, column_type, is_nullable, column_default FROM information_schema.columns
-- WHERE table_schema = DATABASE() AND table_name = 'pedidos' AND column_name IN ('entregado','fecha_entregado');
--
-- 2) Cómo quedó la entrega por tipo y estado (Apartados abiertos y contados Pendiente en 0):
-- SELECT tipo_pedido, estado_pedido, entregado, COUNT(*) FROM pedidos
-- GROUP BY tipo_pedido, estado_pedido, entregado ORDER BY tipo_pedido, estado_pedido, entregado;
--
-- 3) Acciones de Mis pedidos en orden (categorías seguidas) y con el administrador:
-- SELECT a.orden, a.categoria, a.clave, a.etiqueta,
--        EXISTS (SELECT 1 FROM rol_accion ra JOIN roles r ON r.id = ra.rol_id
--                WHERE ra.accion_submenu_id = a.id AND r.nombre_rol = 'ROLE_ADMIN') AS admin
-- FROM accion_submenu a JOIN submenu s ON s.id = a.submenu_id
-- WHERE s.ruta = 'pedidos/mis-pedidos' ORDER BY a.orden;
