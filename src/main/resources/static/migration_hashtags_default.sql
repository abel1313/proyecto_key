-- Migracion 2026-08-18: tabla hashtags_default -- un set fijo de hashtags por red social
-- (facebook/instagram/tiktok) que el front precarga siempre al abrir el formulario de publicar,
-- para que el admin no los vuelva a escribir a mano cada vez. Editable con
-- PUT /v1/redes-sociales/hashtags-default/{redSocial}. Se siembran las 3 filas vacias para que
-- GET /v1/redes-sociales/hashtags-default siempre devuelva las 3, sin manejo especial de 404 en
-- el front la primera vez que se usa.
CREATE TABLE IF NOT EXISTS hashtags_default (
    id INT AUTO_INCREMENT PRIMARY KEY,
    red_social VARCHAR(20) NOT NULL UNIQUE,
    hashtags TEXT,
    actualizado_en DATETIME
);

INSERT IGNORE INTO hashtags_default (red_social, hashtags, actualizado_en) VALUES
    ('facebook', '', NOW()),
    ('instagram', '', NOW()),
    ('tiktok', '', NOW());
