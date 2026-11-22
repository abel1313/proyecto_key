-- ============================================================
-- Migración: publicaciones de WhatsApp (bot NovedadesJade) — 2026-11-22. Ver PLAN_WHATSAPP_PUBLICACIONES.md.
--
-- Reutiliza publicacion_social con plataforma = 'whatsapp' y agrega:
--   grupo_whatsapp → nombre EXACTO del grupo donde se publica (el bot lo busca por nombre)
--   codigo_ref     → código corto visible al inicio del texto (J + id, p. ej. J42); único: es como se sabe de qué publicación hablan
--   modo_venta     → GRUPO | PRIVADO (cómo se cierra la venta); por defecto PRIVADO
-- Estados de una publicación de WhatsApp: PENDIENTE → EN_PROCESO → PUBLICADA | ERROR, y CANCELADA desde el panel.
-- (NO usan 'PROGRAMADA': ese estado lo dispara el job de Facebook/Instagram/TikTok y publicaría esta fila en la red equivocada.)
-- Solo AGREGA columnas e índice; no borra ni cambia datos. Idempotente: se puede correr dos veces.
-- ============================================================

SET @sql = IF((SELECT COUNT(*) FROM information_schema.columns
               WHERE table_schema = DATABASE() AND table_name = 'publicacion_social' AND column_name = 'grupo_whatsapp') = 0,
    'ALTER TABLE publicacion_social ADD COLUMN grupo_whatsapp VARCHAR(200) NULL',
    'SELECT ''publicacion_social.grupo_whatsapp ya existe''');
PREPARE st FROM @sql; EXECUTE st; DEALLOCATE PREPARE st;

SET @sql = IF((SELECT COUNT(*) FROM information_schema.columns
               WHERE table_schema = DATABASE() AND table_name = 'publicacion_social' AND column_name = 'codigo_ref') = 0,
    'ALTER TABLE publicacion_social ADD COLUMN codigo_ref VARCHAR(20) NULL',
    'SELECT ''publicacion_social.codigo_ref ya existe''');
PREPARE st FROM @sql; EXECUTE st; DEALLOCATE PREPARE st;

SET @sql = IF((SELECT COUNT(*) FROM information_schema.columns
               WHERE table_schema = DATABASE() AND table_name = 'publicacion_social' AND column_name = 'modo_venta') = 0,
    'ALTER TABLE publicacion_social ADD COLUMN modo_venta VARCHAR(10) NULL',
    'SELECT ''publicacion_social.modo_venta ya existe''');
PREPARE st FROM @sql; EXECUTE st; DEALLOCATE PREPARE st;

SET @sql = IF((SELECT COUNT(*) FROM information_schema.statistics
               WHERE table_schema = DATABASE() AND table_name = 'publicacion_social' AND index_name = 'uk_publicacion_social_codigo_ref') = 0,
    'ALTER TABLE publicacion_social ADD UNIQUE INDEX uk_publicacion_social_codigo_ref (codigo_ref)',
    'SELECT ''uk_publicacion_social_codigo_ref ya existe''');
PREPARE st FROM @sql; EXECUTE st; DEALLOCATE PREPARE st;

-- Verificación: las 3 columnas y el índice deben aparecer (4 filas).
SELECT 'columna' AS tipo, column_name AS nombre FROM information_schema.columns
 WHERE table_schema = DATABASE() AND table_name = 'publicacion_social' AND column_name IN ('grupo_whatsapp','codigo_ref','modo_venta')
UNION ALL
SELECT 'indice', index_name FROM information_schema.statistics
 WHERE table_schema = DATABASE() AND table_name = 'publicacion_social' AND index_name = 'uk_publicacion_social_codigo_ref';
