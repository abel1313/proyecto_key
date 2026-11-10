-- ============================================================
-- Migración: campañas (eventos con fechas y precios de campaña) (2026-11-07).
--
-- Una campaña tiene nombre, fecha de inicio y fin, una meta de monto y los juguetes (productos) con su precio de campaña. Solo una campaña
-- puede estar encendida a la vez; se pueden guardar y duplicar para el siguiente evento. Un producto no se repite dentro de la misma campaña.
-- Si se borra la campaña o el producto, sus filas se borran solas (ON DELETE CASCADE).
-- Además: la pantalla «Campañas» del menú (ruta 'productos/campanas'), solo ROLE_ADMIN, justo después de «Grupos de precio por cantidad» si
-- existe (si no, después de «Buscar producto»); si no hay ninguna de las dos, no se crea la pantalla (el endpoint y las tablas funcionan igual).
-- Idempotente: se puede correr dos veces.
-- ============================================================

CREATE TABLE IF NOT EXISTS campana (
    id             INT           NOT NULL AUTO_INCREMENT,
    nombre         VARCHAR(100)  NOT NULL,
    fecha_inicio   DATE          NOT NULL,
    fecha_fin      DATE          NOT NULL,
    activa         TINYINT(1)    NOT NULL DEFAULT 0,
    meta_monto     DECIMAL(10,2) NOT NULL DEFAULT 0,
    fecha_creacion DATETIME      NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS campana_producto (
    id            INT           NOT NULL AUTO_INCREMENT,
    campana_id    INT           NOT NULL,
    producto_id   INT           NOT NULL,
    precio_campana DECIMAL(10,2) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_campana_producto (campana_id, producto_id),
    KEY idx_campana_producto_producto (producto_id),
    CONSTRAINT fk_cp_campana  FOREIGN KEY (campana_id)  REFERENCES campana (id)  ON DELETE CASCADE,
    CONSTRAINT fk_cp_producto FOREIGN KEY (producto_id) REFERENCES producto (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO submenu (menu_id, nombre, ruta, icono, descripcion, orden, tiene_escritura)
SELECT b.menu_id, 'Campañas', 'productos/campanas', '🎁',
       'Eventos con fechas (Reyes, roscas...): juguetes con precio de campaña para quien llegue a la meta. Solo administrador.',
       b.orden + 1, 1
FROM submenu b
WHERE b.ruta = 'productos/grupos-precio'
  AND NOT EXISTS (SELECT 1 FROM submenu e WHERE e.ruta = 'productos/campanas');

INSERT INTO submenu (menu_id, nombre, ruta, icono, descripcion, orden, tiene_escritura)
SELECT b.menu_id, 'Campañas', 'productos/campanas', '🎁',
       'Eventos con fechas (Reyes, roscas...): juguetes con precio de campaña para quien llegue a la meta. Solo administrador.',
       b.orden + 1, 1
FROM submenu b
WHERE b.ruta = 'productos/buscar'
  AND NOT EXISTS (SELECT 1 FROM submenu e WHERE e.ruta = 'productos/campanas');

INSERT INTO rol_submenu (rol_id, submenu_id)
SELECT ro.id, s.id
FROM roles ro
CROSS JOIN submenu s
WHERE s.ruta = 'productos/campanas'
  AND ro.nombre_rol = 'ROLE_ADMIN'
  AND NOT EXISTS (SELECT 1 FROM rol_submenu e WHERE e.rol_id = ro.id AND e.submenu_id = s.id);

-- ============================================================
-- VERIFICACIÓN
-- ============================================================
-- SHOW CREATE TABLE campana_producto;
-- SELECT s.id, s.ruta, s.nombre, s.menu_id, s.orden FROM submenu s WHERE s.ruta IN ('productos/buscar', 'productos/grupos-precio', 'productos/campanas');
-- SELECT ro.nombre_rol, s.ruta FROM rol_submenu rs JOIN roles ro ON ro.id = rs.rol_id JOIN submenu s ON s.id = rs.submenu_id WHERE s.ruta = 'productos/campanas';
