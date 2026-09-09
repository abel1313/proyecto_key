-- ============================================================
-- Migración: asignación de pantallas por rol y excepciones por usuario
-- (Fase 1 de permisos por pantalla, siguiente paso -- ver PLAN_PERMISOS_PANTALLAS.md,
-- repo compartido, sección 6).
--
-- rol_submenu     -- pantallas base de cada rol (mismo patrón que rol_permiso, ya existente)
-- usuario_submenu -- excepción por usuario individual, encima de lo que da su rol.
--                    concedido=true  -> se le suma esta pantalla aunque su rol no la dé
--                    concedido=false -> se le quita esta pantalla aunque su rol sí la dé
-- ============================================================

CREATE TABLE rol_submenu (
    rol_id     INT NOT NULL,
    submenu_id INT NOT NULL,
    PRIMARY KEY (rol_id, submenu_id),
    CONSTRAINT fk_rol_submenu_rol     FOREIGN KEY (rol_id) REFERENCES roles (id) ON DELETE CASCADE,
    CONSTRAINT fk_rol_submenu_submenu FOREIGN KEY (submenu_id) REFERENCES submenu (id) ON DELETE CASCADE
);

CREATE TABLE usuario_submenu (
    id         INT AUTO_INCREMENT PRIMARY KEY,
    usuario_id INT NOT NULL,
    submenu_id INT NOT NULL,
    concedido  BOOLEAN NOT NULL,
    UNIQUE KEY uk_usuario_submenu (usuario_id, submenu_id),
    CONSTRAINT fk_usuario_submenu_usuario FOREIGN KEY (usuario_id) REFERENCES usuario_modificacion (id) ON DELETE CASCADE,
    CONSTRAINT fk_usuario_submenu_submenu FOREIGN KEY (submenu_id) REFERENCES submenu (id) ON DELETE CASCADE
);

-- ============================================================
-- SEMILLA -- ROLE_ADMIN recibe automáticamente TODAS las pantallas (preserva su comportamiento
-- actual: hoy ve todo). Ver PLAN_PERMISOS_PANTALLAS.md sección 4, punto 1.
-- Los demás roles (EMPLEADO/CAJERO/USUARIO) se quedan SIN pantallas asignadas por ahora --
-- el mapeo "qué guard tiene cada ruta hoy" contra "qué rol debería tener esa pantalla" es un
-- trabajo aparte (sección 4, punto 2) para no asignar de más ni de menos a ciegas.
-- ============================================================

INSERT INTO rol_submenu (rol_id, submenu_id)
SELECT r.id, s.id
FROM roles r
CROSS JOIN submenu s
WHERE r.nombre_rol = 'ROLE_ADMIN';

-- ============================================================
-- VERIFICACIÓN
-- ============================================================
-- SELECT r.nombre_rol, COUNT(*) FROM rol_submenu rs JOIN roles r ON r.id = rs.rol_id GROUP BY r.nombre_rol;
