-- Migración 2026-09-03: checkbox independiente de "recibir correos" para promociones.
-- Ejecutar manualmente en la BD de cada ambiente (ddl-auto: none).
ALTER TABLE clientes ADD COLUMN recibir_promociones TINYINT(1) NOT NULL DEFAULT 1;
