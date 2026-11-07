-- ============================================================
-- Migración: grupos de precio por cantidad (2026-11-07).
--
-- "Si llevas 3 piezas de este grupo, cada una sale a su precio de promoción." Un grupo tiene nombre, mínimo de piezas, fechas opcionales,
-- un interruptor activo/apagado y sus productos, cada uno con su precio de promoción. Un producto está en UN SOLO grupo (producto_id es único).
-- Las piezas solo se suman dentro del mismo grupo. Reglas G1 a G8 de PLAN_CAMPANAS_Y_PRECIOS.md.
--
-- Si se borra un producto del catálogo, su fila en el grupo se borra sola (ON DELETE CASCADE).
-- Además: la pantalla «Grupos de precio por cantidad» del menú (ruta 'productos/grupos-precio'), solo ROLE_ADMIN, en el mismo grupo que
-- «Buscar producto» si existe; si no, no se crea la pantalla (el endpoint y las tablas funcionan igual).
-- Idempotente: se puede correr dos veces.
-- ============================================================

CREATE TABLE IF NOT EXISTS grupo_precio_cantidad (
    id             INT          NOT NULL AUTO_INCREMENT,
    nombre         VARCHAR(100) NOT NULL,
    minimo_piezas  INT          NOT NULL,
    fecha_inicio   DATE         NULL,
    fecha_fin      DATE         NULL,
    activo         TINYINT(1)   NOT NULL DEFAULT 1,
    fecha_creacion DATETIME     NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS grupo_precio_cantidad_producto (
    id               INT           NOT NULL AUTO_INCREMENT,
    grupo_id         INT           NOT NULL,
    producto_id      INT           NOT NULL,
    precio_promocion DECIMAL(10,2) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_grupo_precio_producto (producto_id),
    KEY idx_grupo_precio_producto_grupo (grupo_id),
    CONSTRAINT fk_gpcp_grupo    FOREIGN KEY (grupo_id)    REFERENCES grupo_precio_cantidad (id) ON DELETE CASCADE,
    CONSTRAINT fk_gpcp_producto FOREIGN KEY (producto_id) REFERENCES producto (id)              ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO submenu (menu_id, nombre, ruta, icono, descripcion, orden, tiene_escritura)
SELECT b.menu_id, 'Grupos de precio por cantidad', 'productos/grupos-precio', '🏷️',
       'Grupos de productos con precio de promoción cuando el cliente junta el mínimo de piezas. Solo administrador.',
       b.orden + 1, 1
FROM submenu b
WHERE b.ruta = 'productos/buscar'
  AND NOT EXISTS (SELECT 1 FROM submenu e WHERE e.ruta = 'productos/grupos-precio');

INSERT INTO rol_submenu (rol_id, submenu_id)
SELECT ro.id, s.id
FROM roles ro
CROSS JOIN submenu s
WHERE s.ruta = 'productos/grupos-precio'
  AND ro.nombre_rol = 'ROLE_ADMIN'
  AND NOT EXISTS (SELECT 1 FROM rol_submenu e WHERE e.rol_id = ro.id AND e.submenu_id = s.id);

-- ============================================================
-- VERIFICACIÓN
-- ============================================================
-- SHOW CREATE TABLE grupo_precio_cantidad_producto;
-- SELECT s.id, s.ruta, s.nombre, s.menu_id, s.orden FROM submenu s WHERE s.ruta IN ('productos/buscar', 'productos/grupos-precio');
-- SELECT ro.nombre_rol, s.ruta FROM rol_submenu rs JOIN roles ro ON ro.id = rs.rol_id JOIN submenu s ON s.id = rs.submenu_id WHERE s.ruta = 'productos/grupos-precio';
