-- Agrega campo habilitado a la tabla variantes
-- '1' = habilitado (visible), '0' = deshabilitado (soft-delete)
ALTER TABLE variantes ADD COLUMN habilitado CHAR(1) NOT NULL DEFAULT '1';
