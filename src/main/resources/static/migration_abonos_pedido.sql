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
--   dev  → BLOQUE 1 ✅ BLOQUE 2 ✅ BLOQUE 3 ✅ — no correr nada
--   qa   → BLOQUE 1 ✅ BLOQUE 2 ✅ BLOQUE 3 ✅ — no correr nada
--   prod → Correr solo BLOQUE 1 + BLOQUE 2 (monto_dado ya está en el CREATE TABLE)
--          NO correr BLOQUE 3 en prod — daría error 1060 (columna ya existe)
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
        COMMENT 'EFECTIVO | TRANSFERENCIA (TARJETA rechazada en crédito — genera comisión)',
    nota         VARCHAR(200)   NULL,
    monto_dado   DECIMAL(10,2)  NULL
        COMMENT 'Monto entregado por el cliente — para calcular cambio (solo EFECTIVO)',
    PRIMARY KEY (id),
    CONSTRAINT fk_abono_pedido FOREIGN KEY (pedido_id) REFERENCES pedidos (id)
);


-- ============================================================
-- BLOQUE 2 — Delta 2026-06-30 (cancelar)
-- Estado: dev ✅ ya existía | qa ✅ ya existía | prod ⏳ pendiente
-- ============================================================
ALTER TABLE pedidos
    ADD COLUMN motivo_cancelacion  VARCHAR(30)    NULL
        COMMENT 'Motivo de cancelación del pedido de crédito',
    ADD COLUMN fecha_cancelacion   DATE           NULL
        COMMENT 'Fecha en que se canceló el pedido de crédito';

-- ============================================================
-- BLOQUE 3 — Delta 2026-06-30: monto dado por cliente (cambio/vuelto)
-- Solo para ambientes que ya tenían la tabla SIN monto_dado (dev/qa)
-- Estado: dev ✅ aplicado | qa ✅ aplicado | prod NO CORRER (ya está en BLOQUE 1)
-- ============================================================
ALTER TABLE abono_pedido
    ADD COLUMN monto_dado DECIMAL(10,2) NULL
        COMMENT 'Monto entregado por el cliente — para calcular cambio (solo EFECTIVO)';
