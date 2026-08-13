-- Migración 2026-08-12: catálogos base de "flores eternas" (ver PROPUESTA_FLORES_ETERNAS.md).
-- Primera etapa: catálogos administrables + motor de cálculo (validar-cantidad / calcular-precio).
-- Todavía NO incluye la persistencia de un "pedido de ramo" confirmado ni su enganche a
-- pedidos/detalle_pedidos -- eso queda pendiente de una decisión de arquitectura documentada
-- en la sección "Nota de alcance" del documento de diseño.

CREATE TABLE IF NOT EXISTS tipo_flor (
    id              INT           NOT NULL AUTO_INCREMENT,
    nombre          VARCHAR(100)  NOT NULL,
    precio_por_flor DECIMAL(10,2) NOT NULL COMMENT 'Precio unico por flor de este tipo (ej. rosa eterna)',
    activo          TINYINT(1)    NOT NULL DEFAULT 1,
    PRIMARY KEY (id),
    UNIQUE KEY uq_tipo_flor_nombre (nombre)
);

CREATE TABLE IF NOT EXISTS cantidad_flor_valida (
    id           INT        NOT NULL AUTO_INCREMENT,
    tipo_flor_id INT        NOT NULL,
    cantidad     INT        NOT NULL COMMENT 'Cantidad de flores que si forma bien el circulo',
    activo       TINYINT(1) NOT NULL DEFAULT 1,
    PRIMARY KEY (id),
    UNIQUE KEY uq_cantidad_flor_valida (tipo_flor_id, cantidad),
    CONSTRAINT fk_cantidad_flor_valida_tipo_flor
        FOREIGN KEY (tipo_flor_id) REFERENCES tipo_flor (id)
        ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS accesorio_ramo (
    id                 INT           NOT NULL AUTO_INCREMENT,
    nombre             VARCHAR(100)  NOT NULL,
    precio             DECIMAL(10,2) NOT NULL,
    admite_texto_libre TINYINT(1)    NOT NULL DEFAULT 0,
    es_papel           TINYINT(1)    NOT NULL DEFAULT 0 COMMENT 'Regla de umbral: si la cantidad final es > 10, este accesorio se cobra automatico',
    activo             TINYINT(1)    NOT NULL DEFAULT 1,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS frase_liston_predefinida (
    id     INT           NOT NULL AUTO_INCREMENT,
    texto  VARCHAR(200)  NOT NULL,
    precio DECIMAL(10,2) NOT NULL,
    activo TINYINT(1)    NOT NULL DEFAULT 1,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS ramo_armado (
    id                       INT           NOT NULL AUTO_INCREMENT,
    nombre                   VARCHAR(150)  NOT NULL,
    tipo_flor_id             INT           NOT NULL,
    cantidad_flor_valida_id  INT           NOT NULL,
    precio_flores            DECIMAL(10,2) NOT NULL,
    papel_incluido           TINYINT(1)    NOT NULL DEFAULT 0,
    precio_papel             DECIMAL(10,2) NULL,
    precio_total             DECIMAL(10,2) NOT NULL,
    activo                   TINYINT(1)    NOT NULL DEFAULT 1,
    PRIMARY KEY (id),
    CONSTRAINT fk_ramo_armado_tipo_flor
        FOREIGN KEY (tipo_flor_id) REFERENCES tipo_flor (id),
    CONSTRAINT fk_ramo_armado_cantidad_valida
        FOREIGN KEY (cantidad_flor_valida_id) REFERENCES cantidad_flor_valida (id)
);

CREATE TABLE IF NOT EXISTS ramo_armado_accesorio (
    id             INT NOT NULL AUTO_INCREMENT,
    ramo_armado_id INT NOT NULL,
    accesorio_id   INT NOT NULL,
    cantidad       INT NOT NULL DEFAULT 1,
    PRIMARY KEY (id),
    CONSTRAINT fk_ramo_armado_accesorio_ramo
        FOREIGN KEY (ramo_armado_id) REFERENCES ramo_armado (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_ramo_armado_accesorio_accesorio
        FOREIGN KEY (accesorio_id) REFERENCES accesorio_ramo (id)
);

-- lugares_entrega ya existe (catalogo de zonas de entrega) -- se reutiliza como catalogo de
-- "zonas de envio" para flores eternas en vez de crear uno nuevo. Se le agrega el costo.
ALTER TABLE lugares_entrega
    ADD COLUMN costo_envio DECIMAL(10,2) NULL COMMENT 'NULL = sin costo de envio configurado para este lugar';

-- ============================================================
-- VERIFICACIÓN
-- ============================================================
-- SHOW CREATE TABLE tipo_flor;
-- SHOW CREATE TABLE cantidad_flor_valida;
-- SHOW CREATE TABLE accesorio_ramo;
-- SHOW CREATE TABLE frase_liston_predefinida;
-- SHOW CREATE TABLE ramo_armado;
-- SHOW CREATE TABLE ramo_armado_accesorio;
-- SHOW CREATE TABLE lugares_entrega;
