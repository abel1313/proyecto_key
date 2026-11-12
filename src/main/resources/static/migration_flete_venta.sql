-- ============================================================
-- Migración: fletes fase 2 — cada venta guarda de qué VIAJE de compra salió (2026-11-11).
--
-- Al crear una línea de venta, el sistema toma las piezas del viaje más viejo que aún tenga (primero el más viejo), y guarda aquí cuántas
-- piezas salieron de cada viaje y a cuánto flete por pieza. Las piezas que no caben en ningún viaje (stock anterior a los viajes) no llevan fila:
-- quedan «sin viaje». Si la venta se devuelve, la fila se queda y se ignora (las piezas regresan al viaje); si la línea se borra, la fila se borra sola.
-- Solo AGREGA una tabla; no cambia ni borra datos. Idempotente: se puede correr dos veces.
-- CÓRRELA DESPUÉS de migration_flete_lote.sql. Sin esta tabla las ventas siguen igual (las piezas quedan «sin viaje»); no se rompe nada.
-- ============================================================

CREATE TABLE IF NOT EXISTS venta_detalle_lote (
    id                        INT           NOT NULL AUTO_INCREMENT,
    detalle_venta_variante_id INT           NOT NULL,
    compra_lote_producto_id   INT           NOT NULL,
    cantidad                  INT           NOT NULL,
    flete_por_pieza           DECIMAL(10,2) NOT NULL DEFAULT 0,
    origen                    VARCHAR(10)   NOT NULL DEFAULT 'AUTO',
    fecha_creacion            DATETIME      NULL,
    PRIMARY KEY (id),
    KEY idx_vdl_detalle (detalle_venta_variante_id),
    KEY idx_vdl_renglon (compra_lote_producto_id),
    CONSTRAINT chk_vdl_cantidad CHECK (cantidad > 0),
    CONSTRAINT fk_vdl_detalle FOREIGN KEY (detalle_venta_variante_id) REFERENCES detalle_venta_variantes (id) ON DELETE CASCADE,
    CONSTRAINT fk_vdl_renglon FOREIGN KEY (compra_lote_producto_id)   REFERENCES compra_lote_producto (id)    ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- VERIFICACIÓN
--   SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'venta_detalle_lote';   -- debe salir 1
-- Después de unas ventas de un producto que está en un viaje:
--   SELECT v.id, v.detalle_venta_variante_id, l.nombre AS viaje, v.cantidad, v.flete_por_pieza
--   FROM venta_detalle_lote v JOIN compra_lote_producto p ON p.id = v.compra_lote_producto_id JOIN compra_lote l ON l.id = p.lote_id
--   ORDER BY v.id DESC LIMIT 20;
-- ============================================================
