-- Migración 2026-07-23: agrega nombre_receptor y direccion_entrega a pedidos.
-- Sirven para capturar a quién se le entrega el pedido y en qué dirección, editable
-- después de creado el pedido (no solo al momento de la venta directa).
-- fecha_recogida (ya existente) se reutiliza como fecha de entrega elegida por el usuario.
ALTER TABLE pedidos ADD COLUMN nombre_receptor VARCHAR(150) NULL;
ALTER TABLE pedidos ADD COLUMN direccion_entrega VARCHAR(300) NULL;
