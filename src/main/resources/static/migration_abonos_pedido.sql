-- ============================================================
-- MÓDULO: Pagos parciales (Apartado / Fiado)
-- ============================================================
-- Conectarse al servidor
-- sudo mysql -u root -p

-- Seleccionar la base según el ambiente:
-- use inventario_key;      ← dev
-- use inventario_key_qa;   ← qa
--
-- Estado por ambiente (2026-06-30):
--   dev  → YA APLICADO completo — no correr nada
--   qa   → YA APLICADO completo — no correr nada
--   prod → Correr BLOQUE 1 completo + BLOQUE 2 al subir a main
-- ============================================================


-- ============================================================
-- BLOQUE 1 — Migración base (solo para ambientes limpios / prod)
-- NO correr en qa ni dev — estas columnas y tabla ya existen
-- ============================================================
ALTER TABLE pedidos
    ADD COLUMN tipo_pedido         VARCHAR(10)    NOT NULL DEFAULT 'NORMAL'
        COMMENT 'NORMAL | APARTADO | FIADO',
    ADD COLUMN total_pedido        DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    ADD COLUMN total_pagado        DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    ADD COLUMN fecha_recogida      DATE           NULL
        COMMENT 'Fecha en que se recogió el producto (APARTADO liquidado)';

CREATE TABLE IF NOT EXISTS abono_pedido (
    id           INT            NOT NULL AUTO_INCREMENT,
    pedido_id    INT            NOT NULL,
    monto        DECIMAL(10, 2) NOT NULL,
    fecha_pago   DATE           NOT NULL,
    metodo_pago  VARCHAR(15)    NOT NULL DEFAULT 'EFECTIVO'
        COMMENT 'EFECTIVO | TRANSFERENCIA | TARJETA',
    nota         VARCHAR(200)   NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_abono_pedido FOREIGN KEY (pedido_id) REFERENCES pedidos (id)
);


-- ============================================================
-- BLOQUE 2 — Delta 2026-06-30 (correr en QA y prod)
-- Columnas requeridas por PUT /v1/abonos/{id}/cancelar
-- En dev ya están aplicadas
-- ============================================================
ALTER TABLE pedidos
    ADD COLUMN motivo_cancelacion  VARCHAR(30)    NULL
        COMMENT 'Motivo de cancelación del pedido de crédito',
    ADD COLUMN fecha_cancelacion   DATE           NULL
        COMMENT 'Fecha en que se canceló el pedido de crédito';
