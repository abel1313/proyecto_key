-- ============================================================
-- Carga rápida de imágenes: producto+variante borrador por imagen
-- Ejecutar manualmente en la BD de cada ambiente (ddl-auto: none).
-- Todo el seguimiento vive en la tabla producto (sin tabla aparte).
-- ============================================================

-- 1. Flag: indica que el codigo_barras asignado es un placeholder autogenerado por
--    la carga rapida (no el codigo real todavia).
ALTER TABLE producto
    ADD COLUMN codigo_barras_generado TINYINT(1) NOT NULL DEFAULT 0;

-- 2. Estado de subida de la imagen (PENDIENTE mientras sube en background al
--    microservicio de imagenes, EXITOSO/FALLIDO cuando termina). NULL en productos
--    normales que no vienen de este flujo.
ALTER TABLE producto
    ADD COLUMN estado_imagen VARCHAR(20) NULL,
    ADD COLUMN mensaje_error_imagen VARCHAR(500) NULL;

CREATE INDEX idx_producto_estado_imagen ON producto (estado_imagen);
