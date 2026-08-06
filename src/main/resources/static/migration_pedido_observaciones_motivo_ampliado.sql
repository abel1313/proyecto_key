-- Migración 2026-07-23: amplía observaciones y motivo_cancelacion en pedidos.
-- observaciones estaba en VARCHAR(100) — insuficiente para dirección de entrega + receptor + referencia
-- de Facebook/WhatsApp. motivo_cancelacion estaba en VARCHAR(30) — insuficiente para un motivo real de
-- devolución (se mantiene VARCHAR, no TEXT, porque se usa en comparaciones IN (...) para el score de rifa).
-- Ya ejecutada en qa y prod.
ALTER TABLE pedidos MODIFY COLUMN observaciones TEXT;
ALTER TABLE pedidos MODIFY COLUMN motivo_cancelacion VARCHAR(150);
