-- Repara pedidos a crédito cuyo estado quedó con el tipo viejo.
--
-- Causa (corregida en PedidoServiceImpl.cambiarTipoPedido, 2026-09-29): al cambiar un pedido
-- entre Apartado e Ir pagando se actualizaba tipo_pedido pero no estado_pedido, que mientras el
-- pedido sigue abierto es copia del tipo. Resultado: tipo_pedido = 'APARTADO' y
-- estado_pedido = 'FIADO' (o al revés), y la vista de pedidos unidos mostraba el tipo viejo.
--
-- Solo toca filas donde las dos columnas son tipos de crédito y no coinciden. No toca pedidos
-- pagados, entregados ni cancelados. Correrla dos veces no hace daño.
-- Correr primero en inventario_key_qa (cubre dev y qa) y después en inventario_key.

-- 1) Diagnóstico: cuáles se van a corregir
SELECT id, tipo_pedido, estado_pedido
FROM pedidos
WHERE tipo_pedido IN ('APARTADO', 'FIADO')
  AND estado_pedido IN ('APARTADO', 'FIADO')
  AND estado_pedido <> tipo_pedido;

-- 2) Corrección
UPDATE pedidos
SET estado_pedido = tipo_pedido
WHERE tipo_pedido IN ('APARTADO', 'FIADO')
  AND estado_pedido IN ('APARTADO', 'FIADO')
  AND estado_pedido <> tipo_pedido;

-- 3) Verificación: debe devolver 0
SELECT COUNT(*) AS pendientes
FROM pedidos
WHERE tipo_pedido IN ('APARTADO', 'FIADO')
  AND estado_pedido IN ('APARTADO', 'FIADO')
  AND estado_pedido <> tipo_pedido;
