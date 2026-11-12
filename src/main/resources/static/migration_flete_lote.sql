-- ============================================================
-- Migración: fletes por viaje de compra — FASE 1, solo registrar (2026-11-11).
--
-- Un viaje de compra (lote) = «hoy fui a comprar, el flete me costó $100 y traje estos productos». Lleva nombre, fecha, flete total, gastos extra
-- opcionales (gasolina, comida, bolsas... cada uno marcable «cuenta para cubrir») y los productos que llegaron (piezas, costo y flete por pieza).
-- En esta fase SOLO se registra: no cambia ventas, stock, costos ni reportes que ya existen.
-- Además: la pantalla «Fletes» del menú (ruta 'productos/fletes'), solo ROLE_ADMIN, justo después de «Campañas» si existe (si no, después de
-- «Buscar producto»); si no hay ninguna de las dos, no se crea la pantalla (el endpoint y las tablas funcionan igual).
-- Si se borra el viaje, sus gastos y productos se borran solos; si se borra el producto, sale de los viajes (ON DELETE CASCADE).
-- Idempotente: se puede correr dos veces.
-- ============================================================

CREATE TABLE IF NOT EXISTS compra_lote (
    id             INT           NOT NULL AUTO_INCREMENT,
    nombre         VARCHAR(100)  NOT NULL,
    fecha_compra   DATE          NOT NULL,
    flete_total    DECIMAL(10,2) NOT NULL DEFAULT 0,
    nota           VARCHAR(255)  NULL,
    fecha_creacion DATETIME      NULL,
    PRIMARY KEY (id),
    KEY idx_compra_lote_fecha (fecha_compra)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS compra_lote_gasto (
    id                 INT           NOT NULL AUTO_INCREMENT,
    lote_id            INT           NOT NULL,
    concepto           VARCHAR(100)  NOT NULL,
    monto              DECIMAL(10,2) NOT NULL,
    cuenta_para_cubrir TINYINT(1)    NOT NULL DEFAULT 1,
    PRIMARY KEY (id),
    KEY idx_clg_lote (lote_id),
    CONSTRAINT fk_clg_lote FOREIGN KEY (lote_id) REFERENCES compra_lote (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS compra_lote_producto (
    id               INT           NOT NULL AUTO_INCREMENT,
    lote_id          INT           NOT NULL,
    producto_id      INT           NOT NULL,
    piezas_compradas INT           NOT NULL,
    costo_unitario   DECIMAL(10,2) NOT NULL DEFAULT 0,
    flete_por_pieza  DECIMAL(10,2) NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_clp_lote_producto (lote_id, producto_id),
    KEY idx_clp_producto (producto_id),
    CONSTRAINT fk_clp_lote     FOREIGN KEY (lote_id)     REFERENCES compra_lote (id) ON DELETE CASCADE,
    CONSTRAINT fk_clp_producto FOREIGN KEY (producto_id) REFERENCES producto (id)    ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO submenu (menu_id, nombre, ruta, icono, descripcion, orden, tiene_escritura)
SELECT b.menu_id, 'Fletes', 'productos/fletes', '🚚',
       'Viajes de compra: flete, gastos del viaje y los productos que llegaron. Solo administrador.',
       b.orden + 1, 1
FROM submenu b
WHERE b.ruta = 'productos/campanas'
  AND NOT EXISTS (SELECT 1 FROM submenu e WHERE e.ruta = 'productos/fletes');

INSERT INTO submenu (menu_id, nombre, ruta, icono, descripcion, orden, tiene_escritura)
SELECT b.menu_id, 'Fletes', 'productos/fletes', '🚚',
       'Viajes de compra: flete, gastos del viaje y los productos que llegaron. Solo administrador.',
       b.orden + 1, 1
FROM submenu b
WHERE b.ruta = 'productos/buscar'
  AND NOT EXISTS (SELECT 1 FROM submenu e WHERE e.ruta = 'productos/fletes');

INSERT INTO rol_submenu (rol_id, submenu_id)
SELECT ro.id, s.id
FROM roles ro
CROSS JOIN submenu s
WHERE s.ruta = 'productos/fletes'
  AND ro.nombre_rol = 'ROLE_ADMIN'
  AND NOT EXISTS (SELECT 1 FROM rol_submenu e WHERE e.rol_id = ro.id AND e.submenu_id = s.id);

-- ============================================================
-- VERIFICACIÓN
--   SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name IN ('compra_lote','compra_lote_gasto','compra_lote_producto');  -- debe salir 3
--   SELECT id, nombre, ruta, orden FROM submenu WHERE ruta = 'productos/fletes';   -- una fila
--   SELECT ro.nombre_rol FROM rol_submenu rs JOIN roles ro ON ro.id = rs.rol_id JOIN submenu s ON s.id = rs.submenu_id WHERE s.ruta = 'productos/fletes';  -- solo ROLE_ADMIN
-- Después de correrla: volver a entrar (el JWT trae las pantallas).
-- ============================================================
