-- Migración chat v2: agrega cliente_id para vincular sesiones del mismo usuario
-- Ejecutar manualmente en la BD (ddl-auto: none)

ALTER TABLE chat_sesion ADD COLUMN cliente_id VARCHAR(36) NULL;
CREATE INDEX idx_chat_sesion_cliente ON chat_sesion(cliente_id);
