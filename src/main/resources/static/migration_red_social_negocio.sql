-- Redes sociales dinamicas del negocio (Instagram, TikTok, etc.) — antes el back solo
-- soportaba whatsapp_url y facebook_url como columnas fijas en configuracion_negocio.
-- Con esta tabla el admin puede dar de alta cualquier red social sin tocar codigo.
--
-- OJO: ddl-auto esta en 'none' en todos los perfiles, asi que esta tabla NO se crea sola —
-- hay que ejecutar este script en cada ambiente ANTES de desplegar.

CREATE TABLE red_social_negocio (
    id      INT          NOT NULL AUTO_INCREMENT,
    nombre  VARCHAR(100) NOT NULL,
    url     VARCHAR(500) NOT NULL,
    activo  TINYINT(1)   NOT NULL DEFAULT 1,
    PRIMARY KEY (id)
);
