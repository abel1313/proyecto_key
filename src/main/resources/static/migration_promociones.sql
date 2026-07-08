-- ============================================================
-- MODULO: Promociones por variante (combos)
-- ============================================================
-- Conectarse al servidor
-- sudo mysql -u root -p

-- Seleccionar la base segun el ambiente:
-- use inventario_key_qa;   <- dev y qa (misma BD)
-- use inventario_key;      <- main / prod
--
-- Diseno completo en PROMOCIONES.md (raiz del repo).
-- Estado por ambiente: back implementado en dev (2026-07-05) — pendiente correr en dev/qa/prod.
-- ============================================================

CREATE TABLE IF NOT EXISTS promociones (
    id                BIGINT NOT NULL AUTO_INCREMENT,
    descripcion       VARCHAR(255) NOT NULL,
    fecha_vencimiento DATETIME NOT NULL COMMENT 'Fecha y hora exacta de vencimiento',
    activo            TINYINT(1) NOT NULL DEFAULT 1 COMMENT 'Apagado manual del admin, independiente del vencimiento',
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS promocion_detalle (
    id                   BIGINT NOT NULL AUTO_INCREMENT,
    promocion_id         BIGINT NOT NULL,
    variante_id          INT NOT NULL,
    cantidad             INT NOT NULL DEFAULT 1 COMMENT 'Unidades de esa variante que consume una venta del combo',
    precio_en_promocion  DECIMAL(10,2) NOT NULL COMMENT 'Precio rebajado de esa pieza dentro del combo',
    PRIMARY KEY (id),
    CONSTRAINT fk_promocion_detalle_promocion FOREIGN KEY (promocion_id) REFERENCES promociones (id) ON DELETE CASCADE,
    CONSTRAINT fk_promocion_detalle_variante FOREIGN KEY (variante_id) REFERENCES variantes (id)
);

-- Etiqueta cada linea de pedido/venta que pertenece a un combo (nullable — la mayoria de las
-- lineas son ventas normales sin promocion).
ALTER TABLE detalle_pedidos
    ADD COLUMN promocion_id BIGINT NULL COMMENT 'FK a promociones; null en lineas de venta normal',
    ADD CONSTRAINT fk_detalle_pedidos_promocion FOREIGN KEY (promocion_id) REFERENCES promociones (id);

ALTER TABLE detalle_venta_variantes
    ADD COLUMN promocion_id BIGINT NULL COMMENT 'FK a promociones; null en lineas de venta normal',
    ADD CONSTRAINT fk_detalle_venta_variantes_promocion FOREIGN KEY (promocion_id) REFERENCES promociones (id);
