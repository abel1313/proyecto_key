-- ============================================================
-- Migración v3: Boletos de rifa -- descarte por boleto
-- Sigue a migration_boletos_rifa_v2.sql (ya ejecutada en qa y prod).
--
-- En la rifa tipo PLATAFORMAS el sorteo descarta BOLETOS, no personas: si
-- alguien tiene 5 boletos y pierde un giro, se descarta 1 y le quedan 4 en
-- juego. Esta columna marca cuáles ya salieron.
--
-- El tipo nuevo de rifa (PLATAFORMAS) NO necesita script: configurar_rifa.tipo
-- ya es VARCHAR(20) y el valor entra tal cual.
-- ============================================================

ALTER TABLE boletos_rifa
    ADD COLUMN descartado TINYINT(1) NOT NULL DEFAULT 0;

-- ============================================================
-- VERIFICACIÓN
-- ============================================================
-- SHOW CREATE TABLE boletos_rifa;
