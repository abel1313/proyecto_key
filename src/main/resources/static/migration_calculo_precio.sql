-- ============================================================
-- Migración: historial de cálculos de precio de un producto (2026-11-09).
--
-- Cada vez que el administrador pulsa «Usar este precio» en la calculadora de Nuevo/Actualizar producto (y guarda el producto), se guarda un
-- registro con lo que le costó la pieza (compra + transporte + otros), la comisión de tarjeta elegida, el margen, el precio que calculó la
-- calculadora y el que de verdad cobra (pueden ser distintos). El último registro del producto es el vigente.
-- Si se borra el producto, sus cálculos se borran solos (ON DELETE CASCADE).
-- Idempotente: se puede correr dos veces. Sin esta tabla la calculadora funciona igual (solo no se guarda el historial).
-- ============================================================

CREATE TABLE IF NOT EXISTS producto_calculo_precio (
    id                      INT           NOT NULL AUTO_INCREMENT,
    producto_id             INT           NOT NULL,
    costo_compra            DECIMAL(10,2) NOT NULL,
    transporte              DECIMAL(10,2) NOT NULL DEFAULT 0,
    otros                   DECIMAL(10,2) NOT NULL DEFAULT 0,
    comision_pct            DECIMAL(5,2)  NOT NULL DEFAULT 0,
    comision_descripcion    VARCHAR(100)  NULL,
    margen_pct              DECIMAL(5,2)  NOT NULL DEFAULT 0,
    precio_calculado        DECIMAL(10,2) NOT NULL,
    precio_cobrado          DECIMAL(10,2) NOT NULL,
    actualizo_precio_compra TINYINT(1)    NOT NULL DEFAULT 0,
    nota                    VARCHAR(300)  NULL,
    usuario                 VARCHAR(100)  NULL,
    fecha                   DATETIME      NOT NULL,
    PRIMARY KEY (id),
    KEY idx_calculo_precio_producto (producto_id, fecha),
    CONSTRAINT fk_calculo_precio_producto FOREIGN KEY (producto_id) REFERENCES producto (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
