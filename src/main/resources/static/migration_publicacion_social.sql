-- Migración 2026-08-05: tabla publicacion_social — registra cada publicación hecha en
-- Facebook (foto de una variante) para poder auditar qué se publicó, con qué descripción
-- y vincular el post_id devuelto por la Graph API a la variante correspondiente.
-- Ya ejecutada en qa y prod.
CREATE TABLE IF NOT EXISTS publicacion_social (
    id INT AUTO_INCREMENT PRIMARY KEY,
    variante_id INT NOT NULL,
    plataforma VARCHAR(20) NOT NULL,
    tipo_publicacion VARCHAR(20) NOT NULL,
    descripcion_publicada TEXT,
    imagen_id BIGINT,
    post_id_facebook VARCHAR(100),
    scheduled_publish_time DATETIME NULL,
    fecha_publicacion DATETIME NOT NULL,
    estado VARCHAR(20) NOT NULL,
    CONSTRAINT fk_publicacion_social_variante FOREIGN KEY (variante_id) REFERENCES variantes(id)
);

CREATE INDEX idx_publicacion_social_variante ON publicacion_social(variante_id);
