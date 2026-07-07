-- Migración 2026-07-07: agrega fecha_hora_registro a pedidos (para mostrar hora de compra en el front).
-- fecha_pedido sigue siendo DATE (no se toca, muchos reportes ya dependen de ese tipo).
-- Pedidos existentes quedan en NULL; el back hace COALESCE(fecha_hora_registro, fecha_pedido) al leer.
ALTER TABLE pedidos ADD COLUMN fecha_hora_registro DATETIME NULL;
