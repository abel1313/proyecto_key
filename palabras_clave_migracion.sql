-- =============================================================
-- MIGRACIÓN: Palabra clave en productos y variantes
-- =============================================================

-- -------------------------------------------------------------
-- DDL
-- -------------------------------------------------------------

-- 1. Tabla catálogo de palabras clave (ya debe existir si Hibernate generó el schema)
CREATE TABLE IF NOT EXISTS palabra_clave (
    id   INT          NOT NULL AUTO_INCREMENT,
    nombre VARCHAR(100) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_palabra_clave_nombre (nombre)
);

-- 2. Eliminar tabla de relación muchos-a-muchos (ya no se usa)
DROP TABLE IF EXISTS producto_palabra_clave;

-- 3. Agregar columna palabra_clave_id a producto
ALTER TABLE producto
    ADD COLUMN palabra_clave_id INT NULL,
    ADD CONSTRAINT fk_producto_palabra_clave
        FOREIGN KEY (palabra_clave_id) REFERENCES palabra_clave (id);

-- 4. Agregar columna palabra_clave_id a variantes
ALTER TABLE variantes
    ADD COLUMN palabra_clave_id INT NULL,
    ADD CONSTRAINT fk_variante_palabra_clave
        FOREIGN KEY (palabra_clave_id) REFERENCES palabra_clave (id);

-- -------------------------------------------------------------
-- DML – datos iniciales de ejemplo (opcional)
-- -------------------------------------------------------------

-- Insertar palabras clave del catálogo
INSERT INTO palabra_clave (nombre) VALUES
    ('bolsa'),
    ('pantalon'),
    ('falda'),
    ('blusa'),
    ('perfume')
ON DUPLICATE KEY UPDATE nombre = nombre;

-- Asignar palabras clave a productos existentes (ajustar IDs según la BD)
-- UPDATE producto SET palabra_clave_id = 1 WHERE id = 10;  -- bolsa
-- UPDATE variantes SET palabra_clave_id = 1 WHERE id = 5;  -- bolsa