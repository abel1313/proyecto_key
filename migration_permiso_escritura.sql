-- ============================================================
-- Migración: permisos de acción (LEER / ESCRIBIR) por pantalla, Fase 2
-- (sigue a migration_rol_submenu_usuario_submenu.sql)
--
-- Hasta ahora rol_submenu era todo-o-nada: si un rol veía una pantalla, automáticamente podía
-- crear/editar/borrar en ella. Esta tabla separa esa segunda capacidad (ESCRIBIR) de la primera
-- (VER, que sigue siendo rol_submenu sin cambios). Un submenu_id en rol_submenu_escritura SIEMPRE
-- debe estar también en rol_submenu para ese mismo rol -- lo garantiza RolesServiceImpl en el
-- backend, no una constraint de esta tabla.
-- ============================================================

CREATE TABLE rol_submenu_escritura (
    rol_id     INT NOT NULL,
    submenu_id INT NOT NULL,
    PRIMARY KEY (rol_id, submenu_id),
    CONSTRAINT fk_rol_submenu_escritura_rol     FOREIGN KEY (rol_id) REFERENCES roles (id) ON DELETE CASCADE,
    CONSTRAINT fk_rol_submenu_escritura_submenu FOREIGN KEY (submenu_id) REFERENCES submenu (id) ON DELETE CASCADE
);

-- ============================================================
-- SEMILLA -- todo rol que HOY tiene una pantalla (rol_submenu) recibe automáticamente también el
-- permiso de ESCRIBIR en ella, para que nadie pierda acceso al correr esta migración: el
-- comportamiento de "ver = poder editar" que existía hasta ahora queda exactamente igual para
-- todos los roles ya existentes (incluido ROLE_ADMIN, que ya tiene las 45 pantallas). A partir de
-- aquí, el admin puede -- desde Gestión de roles -- quitarle a un rol el "Editar" de una pantalla
-- puntual y dejarlo solo en modo lectura, sin tocar las demás.
-- ============================================================

INSERT INTO rol_submenu_escritura (rol_id, submenu_id)
SELECT rol_id, submenu_id FROM rol_submenu;

-- ============================================================
-- VERIFICACIÓN
-- ============================================================
-- SELECT r.nombre_rol, COUNT(*) AS pantallas_ver
-- FROM rol_submenu rs JOIN roles r ON r.id = rs.rol_id GROUP BY r.nombre_rol;
--
-- SELECT r.nombre_rol, COUNT(*) AS pantallas_editar
-- FROM rol_submenu_escritura rse JOIN roles r ON r.id = rse.rol_id GROUP BY r.nombre_rol;
--
-- Antes/después de la migración, "pantallas_ver" y "pantallas_editar" deben coincidir para cada
-- rol -- confirma que nadie perdió acceso de escritura al pasar de la Fase 1 (todo-o-nada) a la
-- Fase 2 (ver/editar separados).
