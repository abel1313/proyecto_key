-- Migración 2026-07-01: agrega monto_dado a ventas (para calcular cambio en el ticket)
-- Solo aplica a ventas NUEVAS (NORMAL al contado); ventas existentes quedan en NULL.
ALTER TABLE ventas ADD COLUMN monto_dado DOUBLE NULL;
