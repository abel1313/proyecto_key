-- ============================================================
-- Migración v2: Boletos de rifa -- plataforma + rango de fechas configurable
-- Sigue a migration_boletos_rifa.sql (ya ejecutada en qa y prod). Esta agrega:
--   1. configurar_rifa.fecha_inicio_boletos / fecha_fin_boletos -- ventana en la
--      que se aceptan boletos por acciones en redes sociales. Si queda en NULL,
--      el backend sigue validando por el mes de la rifa (mes_referencia), igual
--      que antes.
--   2. boletos_rifa.plataforma -- FACEBOOK / INSTAGRAM / TIKTOK / OTRO, separado
--      del campo "motivo" (qué hizo).
-- ============================================================

ALTER TABLE configurar_rifa
    ADD COLUMN fecha_inicio_boletos DATE NULL,
    ADD COLUMN fecha_fin_boletos    DATE NULL;

ALTER TABLE boletos_rifa
    ADD COLUMN plataforma VARCHAR(20) NULL;

-- ============================================================
-- VERIFICACIÓN
-- ============================================================
-- SHOW CREATE TABLE configurar_rifa;
-- SHOW CREATE TABLE boletos_rifa;
