-- ============================================================
-- Migración: Rifa Mensual (tipo/mesReferencia/esPrueba + agregadoEnPrueba)
-- Ver RIFA_MENSUAL_PROPUESTA.md para el contrato completo
-- ============================================================

-- 1. configurar_rifa: tipo de rifa, mes de referencia y modo de prueba
ALTER TABLE configurar_rifa
    ADD COLUMN tipo           VARCHAR(20) NULL,
    ADD COLUMN mes_referencia VARCHAR(7)  NULL,
    ADD COLUMN es_prueba      TINYINT(1)  NOT NULL DEFAULT 0;

-- 2. concursantes: distinguir a los agregados durante una rifa de prueba
ALTER TABLE concursantes
    ADD COLUMN agregado_en_prueba TINYINT(1) NOT NULL DEFAULT 0;

-- ============================================================
-- VERIFICACIÓN
-- ============================================================
-- SHOW CREATE TABLE configurar_rifa;
-- SHOW CREATE TABLE concursantes;
