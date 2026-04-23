-- ============================================================
-- TABLA: variante_imagen
-- Pivot entre variantes e imagenes_copy
-- ============================================================
CREATE TABLE variante_imagen (
    id          INT          NOT NULL AUTO_INCREMENT,
    variante_id INT          NOT NULL,
    imagen_id   BIGINT       NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_vi_variante FOREIGN KEY (variante_id) REFERENCES variantes (id),
    CONSTRAINT fk_vi_imagen   FOREIGN KEY (imagen_id)   REFERENCES imagenes_copy (id)
);