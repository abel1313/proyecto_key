-- Migración 2026-08-13 (parte 3): ramos multicolor + exclusión de variantes "sombra" del
-- catálogo general + umbral del papel configurable.
-- Catálogos de flores eternas siguen vacíos en QA/producción a esta fecha (confirmado con el
-- front) -- seguro dropear/renombrar columnas de la parte 2 sin migración de datos.
-- Ver PROPUESTA_FLORES_ETERNAS.md, sección "Diseño técnico, parte 4".

-- 1. TipoFlor pasa a ser solo la "especie" (Rosa eterna) -- el stock y la variante sombra se
--    mueven a ColorFlor (un color especifico de esa especie).
ALTER TABLE tipo_flor
    DROP FOREIGN KEY fk_tipo_flor_variante,
    DROP COLUMN variante_id,
    DROP COLUMN stock;

CREATE TABLE IF NOT EXISTS color_flor (
    id           INT        NOT NULL AUTO_INCREMENT,
    tipo_flor_id INT        NOT NULL,
    nombre       VARCHAR(50) NOT NULL,
    stock        INT        NULL,
    activo       TINYINT(1) NOT NULL DEFAULT 1,
    variante_id  INT        NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_color_flor_tipo_flor
        FOREIGN KEY (tipo_flor_id) REFERENCES tipo_flor (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_color_flor_variante
        FOREIGN KEY (variante_id) REFERENCES variantes (id)
);

-- 2. Umbral de la regla del papel, configurable por el admin en vez de fijo en el código.
ALTER TABLE accesorio_ramo
    ADD COLUMN umbral_activacion INT NULL COMMENT 'Cantidad final por encima de la cual este accesorio se agrega solo. NULL = siempre opcional';

-- 3. RamoArmado: referencia un color especifico (no solo la especie) + foto de referencia.
ALTER TABLE ramo_armado
    DROP FOREIGN KEY fk_ramo_armado_tipo_flor,
    DROP COLUMN tipo_flor_id,
    ADD COLUMN imagen_url  VARCHAR(500) NULL,
    ADD COLUMN color_flor_id INT NULL,
    ADD CONSTRAINT fk_ramo_armado_color_flor
        FOREIGN KEY (color_flor_id) REFERENCES color_flor (id);
ALTER TABLE ramo_armado
    MODIFY COLUMN color_flor_id INT NOT NULL;

-- 4. Desglose por color del "ticket de producción" (para armar la mezcla exacta, no para precio).
CREATE TABLE IF NOT EXISTS ramo_pedido_detalle_color (
    id                     INT NOT NULL AUTO_INCREMENT,
    ramo_pedido_detalle_id INT NOT NULL,
    color_flor_id          INT NOT NULL,
    cantidad               INT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_rpd_color_detalle
        FOREIGN KEY (ramo_pedido_detalle_id) REFERENCES ramo_pedido_detalle (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_rpd_color_color
        FOREIGN KEY (color_flor_id) REFERENCES color_flor (id)
);

-- 5. Flag para excluir productos/variantes "sombra" (ver ProductoSombraServiceImpl) del
--    catálogo público, buscadores de admin, selectores de promoción/rifa y reportes de venta.
--    Los productos/variantes ya existentes (reales) quedan en false por el DEFAULT.
ALTER TABLE producto
    ADD COLUMN es_catalogo_interno TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'true = producto sintetico de flores eternas, nunca visible en catalogo/buscadores';

-- ============================================================
-- VERIFICACIÓN
-- ============================================================
-- SHOW CREATE TABLE tipo_flor;
-- SHOW CREATE TABLE color_flor;
-- SHOW CREATE TABLE accesorio_ramo;
-- SHOW CREATE TABLE ramo_armado;
-- SHOW CREATE TABLE ramo_pedido_detalle_color;
-- SHOW CREATE TABLE producto;
