-- Migración 2026-09-04: día de la semana (recurrente) en que se hace el viaje de entrega a cada
-- zona del catálogo lugares_entrega. 1=lunes .. 7=domingo (java.time.DayOfWeek.getValue()).
-- NULL = sin configurar todavía (o "recoger en tienda", que no aplica).
--
-- Después de correr esto, en el admin (Lugares de entrega) configura el día de cada zona real
-- (ej. Zacazonapan -> 6 = sábado).

ALTER TABLE lugares_entrega
    ADD COLUMN dia_entrega_semanal TINYINT NULL;

-- ============================================================
-- VERIFICACIÓN
-- ============================================================
-- SHOW CREATE TABLE lugares_entrega;
-- SELECT id, nombre, dia_entrega_semanal FROM lugares_entrega;
