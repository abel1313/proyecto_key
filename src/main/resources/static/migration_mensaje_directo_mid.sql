-- Migración 2026-09-24: agrandar mid y respuesta_mid de mensaje_directo_social.
-- Ejecutar a mano en cada base (ddl-auto: none). Correrla en inventario_key_qa cubre dev y qa.
--
-- Por qué: los mid de Instagram miden ~180 caracteres (los de Messenger son cortos). Con
-- VARCHAR(100) el primer mensaje directo de Instagram que llegó a QA falló al guardarse:
--   "Data truncation: Data too long for column 'mid' at row 1"
-- Sin el registro el bot no reconoce reenvíos de Meta ni su propio eco, y podría contestar dos
-- veces. 512 caracteres en utf8mb4 = 2048 bytes: cabe en el índice UNIQUE (límite InnoDB 3072).
--
-- Idempotente: MODIFY a la misma definición no cambia nada si ya se corrió.

ALTER TABLE mensaje_directo_social
    MODIFY mid VARCHAR(512) NOT NULL,
    MODIFY respuesta_mid VARCHAR(512) NULL;

-- Verificación: las dos columnas deben decir 512.
SELECT COLUMN_NAME, CHARACTER_MAXIMUM_LENGTH
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'mensaje_directo_social'
  AND COLUMN_NAME IN ('mid', 'respuesta_mid');
