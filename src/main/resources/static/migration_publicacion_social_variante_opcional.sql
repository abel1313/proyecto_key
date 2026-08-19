-- Migracion 2026-08-18: variante_id pasa a ser opcional en publicacion_social. Facebook/Instagram/
-- TikTok video-reel siempre suben un archivo "suelto" (nunca depende de una imagen guardada de
-- un producto), a diferencia de la foto que si necesita una variante (imagen guardada o principal
-- de la variante). Antes se exigia variante igual para video/reel, obligando a inventar una
-- aunque el contenido no tuviera nada que ver con un producto del catalogo. La FK se mantiene,
-- solo se quita el NOT NULL.
ALTER TABLE publicacion_social MODIFY COLUMN variante_id INT NULL;
