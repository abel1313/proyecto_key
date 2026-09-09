-- ============================================================
-- Migración: catálogo de acciones granulares por pantalla, Fase 3 de permisos
-- (piloto en Modelos, sigue a migration_permiso_escritura.sql)
--
-- rol_submenu_escritura (Fase 2) es un único "Editar" para toda la pantalla. Esta migración va
-- más fino: dentro de una pantalla, cada botón/acción puntual (ej. "eliminar", "habilitar" en
-- Modelos) es su propio permiso independiente. Un rol puede tener Editar sin tener "eliminar",
-- o viceversa. Piloto en una sola pantalla (productos/buscar = "Modelos") antes de extenderlo
-- a las demás.
-- ============================================================

CREATE TABLE accion_submenu (
    id         INT AUTO_INCREMENT PRIMARY KEY,
    submenu_id INT NOT NULL,
    clave      VARCHAR(60) NOT NULL,
    etiqueta   VARCHAR(120) NOT NULL,
    orden      INT,
    CONSTRAINT fk_accion_submenu_submenu FOREIGN KEY (submenu_id) REFERENCES submenu (id) ON DELETE CASCADE
);

CREATE TABLE rol_accion (
    rol_id            INT NOT NULL,
    accion_submenu_id INT NOT NULL,
    PRIMARY KEY (rol_id, accion_submenu_id),
    CONSTRAINT fk_rol_accion_rol    FOREIGN KEY (rol_id) REFERENCES roles (id) ON DELETE CASCADE,
    CONSTRAINT fk_rol_accion_accion FOREIGN KEY (accion_submenu_id) REFERENCES accion_submenu (id) ON DELETE CASCADE
);

-- ============================================================
-- SEMILLA -- las 6 acciones puntuales de la tarjeta de producto en "Modelos" (productos/buscar).
-- ============================================================

INSERT INTO accion_submenu (submenu_id, clave, etiqueta, orden)
SELECT s.id, v.clave, v.etiqueta, v.orden
FROM submenu s
CROSS JOIN (
    SELECT 'habilitar'      AS clave, 'Habilitar / deshabilitar producto'        AS etiqueta, 1 AS orden UNION ALL
    SELECT 'eliminar',          'Eliminar producto',                                2 UNION ALL
    SELECT 'crear-variantes',   'Crear variantes ("Productos")',                    3 UNION ALL
    SELECT 'compartir-imagen',  'Compartir imagen',                                 4 UNION ALL
    SELECT 'descargar-excel',   'Descargar Excel de productos sin variantes',       5 UNION ALL
    SELECT 'filtros-admin',     'Ver filtros de administración',                    6
) v
WHERE s.ruta = 'productos/buscar';

-- ============================================================
-- Todo rol que HOY tiene ESCRITURA en "Modelos" (rol_submenu_escritura) recibe automáticamente
-- las 6 acciones -- preserva el comportamiento actual (Editar = podía hacer todo esto) para
-- quien ya lo tenía. A partir de aquí, el admin puede -- desde Gestión de roles -- quitarle a un
-- rol una acción puntual (ej. "eliminar") sin tocarle el Editar general.
-- ============================================================

INSERT INTO rol_accion (rol_id, accion_submenu_id)
SELECT rse.rol_id, a.id
FROM rol_submenu_escritura rse
JOIN submenu s ON s.id = rse.submenu_id AND s.ruta = 'productos/buscar'
JOIN accion_submenu a ON a.submenu_id = s.id;

-- ============================================================
-- VERIFICACIÓN
-- ============================================================
-- SELECT r.nombre_rol, a.clave
-- FROM rol_accion ra
-- JOIN roles r ON r.id = ra.rol_id
-- JOIN accion_submenu a ON a.id = ra.accion_submenu_id
-- ORDER BY r.nombre_rol, a.orden;
