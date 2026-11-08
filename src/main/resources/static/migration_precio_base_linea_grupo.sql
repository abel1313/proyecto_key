-- ============================================================
-- Migración: precio normal de una línea que se cobró con precio de grupo (2026-11-07, Fase 1b de
-- PLAN_CAMPANAS_Y_PRECIOS.md).
--
-- Cuando una venta junta el mínimo de un grupo de precio por cantidad, la línea se cobra a su precio de grupo. Si después se
-- edita el pedido (se quita una pieza y baja del mínimo), la línea tiene que REGRESAR al precio que tenía antes: el normal o la
-- rebaja que se le había dado. Ese precio "de antes" se guarda aquí, una fila por línea (detalle_pedidos.id), y solo para las
-- líneas que se cobraron con precio de grupo. detalle_pedidos NO cambia.
--
-- Requiere migration_grupo_precio_cantidad.sql (no depende de ella en la base, pero tiene sentido juntas). Si esta tabla no
-- existe, el sistema funciona igual: el precio de grupo se aplica al vender, pero al editar un pedido no se recalcula.
-- Si se borra la línea del pedido, su fila se borra sola (ON DELETE CASCADE). Idempotente: se puede correr dos veces.
-- ============================================================

-- origen: GRUPO = lo cobró un grupo de precio por cantidad (se recalcula al editar el pedido), CAMPANA = lo cobró una campaña de juguetes (solo se revierte al pasar a Ir pagando).
CREATE TABLE IF NOT EXISTS detalle_pedido_precio_grupo (
    detalle_pedido_id INT           NOT NULL,
    precio_sin_grupo  DECIMAL(10,2) NOT NULL,
    origen            VARCHAR(10)   NOT NULL DEFAULT 'GRUPO',
    PRIMARY KEY (detalle_pedido_id),
    CONSTRAINT fk_dppg_detalle FOREIGN KEY (detalle_pedido_id) REFERENCES detalle_pedidos (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- VERIFICACIÓN
-- ============================================================
-- SHOW CREATE TABLE detalle_pedido_precio_grupo;
-- SELECT * FROM detalle_pedido_precio_grupo ORDER BY detalle_pedido_id DESC LIMIT 10;
