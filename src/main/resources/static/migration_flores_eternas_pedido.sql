-- Migración 2026-08-12 (parte 2): integración de "flores eternas" con Pedido/Venta.
-- No toca detalle_pedidos ni detalle_venta_variantes -- en vez de eso, cada catálogo (tipo_flor,
-- accesorio_ramo, frase_liston_predefinida, lugares_entrega) gana una variante "sombra" (con su
-- producto detrás) para poder venderse como una línea real y reutilizar 100% del flujo existente
-- (stock, precio, cancelación, ganancia, Mercado Pago) sin modificar ese código.
-- Ver PROPUESTA_FLORES_ETERNAS.md, sección "Diseño técnico — integración con Pedido".

ALTER TABLE tipo_flor
    ADD COLUMN precio_costo DECIMAL(10,2) NULL,
    ADD COLUMN stock        INT           NULL COMMENT 'Flores sueltas disponibles; se sincroniza con variantes.stock',
    ADD COLUMN variante_id  INT           NULL,
    ADD CONSTRAINT fk_tipo_flor_variante
        FOREIGN KEY (variante_id) REFERENCES variantes (id);

ALTER TABLE accesorio_ramo
    ADD COLUMN precio_costo DECIMAL(10,2) NULL,
    ADD COLUMN variante_id  INT           NULL,
    ADD CONSTRAINT fk_accesorio_ramo_variante
        FOREIGN KEY (variante_id) REFERENCES variantes (id);

ALTER TABLE frase_liston_predefinida
    ADD COLUMN variante_id INT NULL,
    ADD CONSTRAINT fk_frase_liston_predefinida_variante
        FOREIGN KEY (variante_id) REFERENCES variantes (id);

ALTER TABLE lugares_entrega
    ADD COLUMN variante_id INT NULL,
    ADD CONSTRAINT fk_lugares_entrega_variante
        FOREIGN KEY (variante_id) REFERENCES variantes (id);

-- "Ticket de producción" de un ramo dentro de un pedido ya creado -- ver comentario en la
-- entidad RamoPedidoDetalle.java. No guarda precios que ya viven en detalle_pedidos.
CREATE TABLE IF NOT EXISTS ramo_pedido_detalle (
    id                                 INT           NOT NULL AUTO_INCREMENT,
    pedido_id                          INT           NOT NULL,
    ramo_armado_id                     INT           NULL,
    tipo_flor_id                       INT           NOT NULL,
    cantidad_final                     INT           NOT NULL,
    frase_liston_predefinida_id        INT           NULL,
    frase_liston_personalizada         TEXT          NULL,
    frase_liston_estado                VARCHAR(30)   NOT NULL DEFAULT 'NO_APLICA',
    frase_liston_precio_asignado       DECIMAL(10,2) NULL,
    anticipo_requerido                 TINYINT(1)    NOT NULL DEFAULT 0,
    anticipo_pagado                    TINYINT(1)    NOT NULL DEFAULT 0,
    monto_anticipo                     DECIMAL(10,2) NULL,
    lugar_entrega_id                   INT           NULL,
    recoger_en_local                   TINYINT(1)    NOT NULL DEFAULT 0,
    telefono_contacto                  VARCHAR(20)   NULL,
    correo_contacto                    VARCHAR(150)  NULL,
    comentario_accesorio_no_disponible TEXT          NULL,
    fecha_creacion                     DATETIME      NOT NULL,
    -- Pedido APARTADO separado, creado al aprobar la frase personalizada, solo para cobrar
    -- esa linea via el modulo de abonos ya existente (POST /v1/abonos/{id}).
    pedido_anticipo_id                 INT           NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_ramo_pedido_detalle_pedido
        FOREIGN KEY (pedido_id) REFERENCES pedidos (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_ramo_pedido_detalle_ramo_armado
        FOREIGN KEY (ramo_armado_id) REFERENCES ramo_armado (id),
    CONSTRAINT fk_ramo_pedido_detalle_tipo_flor
        FOREIGN KEY (tipo_flor_id) REFERENCES tipo_flor (id),
    CONSTRAINT fk_ramo_pedido_detalle_frase
        FOREIGN KEY (frase_liston_predefinida_id) REFERENCES frase_liston_predefinida (id),
    CONSTRAINT fk_ramo_pedido_detalle_lugar_entrega
        FOREIGN KEY (lugar_entrega_id) REFERENCES lugares_entrega (id),
    CONSTRAINT fk_ramo_pedido_detalle_pedido_anticipo
        FOREIGN KEY (pedido_anticipo_id) REFERENCES pedidos (id)
);

-- ============================================================
-- VERIFICACIÓN
-- ============================================================
-- SHOW CREATE TABLE tipo_flor;
-- SHOW CREATE TABLE accesorio_ramo;
-- SHOW CREATE TABLE frase_liston_predefinida;
-- SHOW CREATE TABLE lugares_entrega;
-- SHOW CREATE TABLE ramo_pedido_detalle;
