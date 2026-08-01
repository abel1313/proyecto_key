-- ============================================================
-- DIAGNÓSTICO: pedidos.total_pagado vs SUM(abono_pedido.monto)
-- ============================================================
-- Motivo: el front reportó (2026-08-01) que un ticket mostró saldo $200 en un
-- pedido de $300 con $100 abonado antes + $100 abonado en el momento (esperado $100).
--
-- El back calcula saldoRestante = total_pedido - (total_pagado + monto del abono nuevo)
-- DENTRO de la misma transacción (AbonoServiceImpl.registrarAbono, línea ~110), así que
-- el número solo puede salir mal si la columna total_pagado del pedido está desfasada
-- respecto a la suma real de sus abonos — p. ej. porque algún abono se insertó por SQL
-- a mano, o porque el pedido es anterior a la migración del módulo de abonos.
--
-- Este script NO modifica nada: solo lista los pedidos desfasados.
--
-- Seleccionar la base según el ambiente:
USE inventario_key_qa;   -- dev / qa
-- USE inventario_key;   -- producción
-- ============================================================


-- ------------------------------------------------------------
-- 1) Pedidos de crédito donde total_pagado NO cuadra con sus abonos
--    Si devuelve 0 filas, el dato está sano y el $200 del ticket vino
--    de una versión cacheada del front, no del back.
-- ------------------------------------------------------------
SELECT
    p.id                                   AS pedido_id,
    p.tipo_pedido,
    p.estado_pedido,
    p.total_pedido,
    p.total_pagado                         AS total_pagado_guardado,
    COALESCE(SUM(ap.monto), 0)             AS suma_real_abonos,
    p.total_pagado - COALESCE(SUM(ap.monto), 0) AS diferencia,
    COUNT(ap.id)                           AS num_abonos
FROM pedidos p
LEFT JOIN abono_pedido ap ON ap.pedido_id = p.id
WHERE p.tipo_pedido IN ('APARTADO', 'FIADO')
GROUP BY p.id, p.tipo_pedido, p.estado_pedido, p.total_pedido, p.total_pagado
HAVING ABS(p.total_pagado - COALESCE(SUM(ap.monto), 0)) > 0.001
ORDER BY ABS(p.total_pagado - COALESCE(SUM(ap.monto), 0)) DESC;


-- ------------------------------------------------------------
-- 2) Detalle de un pedido concreto (cambiar el id) — para reconstruir
--    a mano el ticket que reportó el front
-- ------------------------------------------------------------
SET @pedido_id = 0;   -- <<< poner aquí el id del pedido reportado

SELECT p.id, p.total_pedido, p.total_pagado,
       p.total_pedido - p.total_pagado AS saldo_segun_columna
FROM pedidos p
WHERE p.id = @pedido_id;

SELECT ap.id, ap.monto, ap.fecha_pago, ap.metodo_pago, ap.nota
FROM abono_pedido ap
WHERE ap.pedido_id = @pedido_id
ORDER BY ap.fecha_pago ASC, ap.id ASC;


-- ------------------------------------------------------------
-- 3) CORRECCIÓN (solo si el punto 1 devolvió filas) — recalcula
--    total_pagado desde los abonos reales.
--    Revisar primero el resultado del punto 1; esto SÍ escribe.
-- ------------------------------------------------------------
-- UPDATE pedidos p
-- SET p.total_pagado = (
--     SELECT COALESCE(SUM(ap.monto), 0) FROM abono_pedido ap WHERE ap.pedido_id = p.id
-- )
-- WHERE p.tipo_pedido IN ('APARTADO', 'FIADO');
