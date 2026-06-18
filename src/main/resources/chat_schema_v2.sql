-- Migración chat v2: agrega cliente_id (anónimos) y usuario_id (registrados)
-- Ejecutar manualmente en la BD (ddl-auto: none)

ALTER TABLE chat_sesion ADD COLUMN cliente_id VARCHAR(36) NULL;
CREATE INDEX idx_chat_sesion_cliente ON chat_sesion(cliente_id);

ALTER TABLE chat_sesion ADD COLUMN usuario_id INT NULL;
CREATE INDEX idx_chat_sesion_usuario ON chat_sesion(usuario_id);
