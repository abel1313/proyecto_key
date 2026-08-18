-- Migracion 2026-08-18: soporte de programacion propia para Facebook/Instagram/TikTok.
-- Decision del dueno: ninguna red (ni Facebook, que si tiene scheduler nativo de Meta) usa la
-- programacion de la plataforma -- las 3 se guardan aqui como "PROGRAMADA" y un job propio
-- (PublicacionSocialScheduler) las publica a la hora exacta, para que las 3 salgan siempre
-- sincronizadas (Instagram y TikTok nunca tuvieron opcion nativa, asi que ya era obligatorio
-- para esas dos; se unifico Facebook tambien en vez de dejar un mecanismo aparte).
ALTER TABLE publicacion_social
    ADD COLUMN contenido_bytes LONGBLOB NULL COMMENT 'Archivo crudo (video o foto ad-hoc) mientras la publicacion esta PROGRAMADA -- se limpia a null en cuanto se publica o falla definitivamente.',
    ADD COLUMN content_type VARCHAR(100) NULL COMMENT 'Content-Type del archivo en contenido_bytes.',
    ADD COLUMN intentos INT NOT NULL DEFAULT 0 COMMENT 'Cuantas veces el job intento publicarla sin exito.',
    ADD COLUMN ultimo_error TEXT NULL COMMENT 'Mensaje del ultimo intento fallido, para diagnostico.';
