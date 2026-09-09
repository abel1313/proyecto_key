-- Migración 2026-09-04: a la tabla direcciones le faltaba AUTO_INCREMENT en id.
-- Encontrado en QA (inventario_key_qa) al intentar guardar una direccion nueva:
-- "Field 'id' doesn't have a default value" -- Direccion usa GenerationType.IDENTITY (BaseId.java),
-- que depende de que MySQL genere el id solo; sin AUTO_INCREMENT, cualquier insert sin id explicito
-- truena. MySQL calcula el siguiente valor a partir del id mas alto que ya exista, es seguro
-- correrlo con datos existentes.
-- Ejecutar manualmente en cada ambiente donde la tabla no lo tenga ya (verificar primero con
-- SHOW CREATE TABLE direcciones -- si main/produccion tambien le falta, este mismo error le pasa
-- a cualquier cliente real intentando guardar una direccion).
ALTER TABLE direcciones MODIFY id INT NOT NULL AUTO_INCREMENT;
