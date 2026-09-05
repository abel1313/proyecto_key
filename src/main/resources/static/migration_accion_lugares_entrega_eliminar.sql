-- ============================================================
-- Migración: extender permisos finos (Fase 3) a "lugares-entrega" -- acción puntual "eliminar"
-- (2026-09-05, mismo patrón que migration_accion_palabras_clave_eliminar.sql)
--
-- Motivo: "lugares-entrega" (Zonas de entrega) tenía TODO su CRUD (alta, edición, mapa de centro,
-- anillos de cobro por distancia, y eliminar) bajo un único permiso de Escritura -- sin
-- distinción. El dueño pidió auditar el menú "Envíos" a fondo ("tiene algunas [opciones]").
-- "Eliminar" es la única acción de esta pantalla que tiene sentido separar del resto (alta/
-- edición/anillos son un único flujo de "editar la zona", no tiene sentido partirlos más fino).
--
-- Preserva el comportamiento actual: se le da la acción nueva a todo rol que ya tuviera Escritura
-- en "lugares-entrega" (antes, tener Escritura alcanzaba para eliminar también).
-- ============================================================

INSERT INTO accion_submenu (submenu_id, clave, etiqueta, descripcion, orden)
SELECT s.id, 'eliminar', 'Eliminar zona de entrega',
       'Botón 🗑️ Eliminar de cada fila del catálogo, en Zonas de entrega.', 1
FROM submenu s
WHERE s.ruta = 'lugares-entrega'
  AND NOT EXISTS (
    SELECT 1 FROM accion_submenu existente WHERE existente.submenu_id = s.id AND existente.clave = 'eliminar'
  );

INSERT INTO rol_accion (rol_id, accion_submenu_id)
SELECT rse.rol_id, a.id
FROM rol_submenu_escritura rse
JOIN submenu s ON s.id = rse.submenu_id AND s.ruta = 'lugares-entrega'
JOIN accion_submenu a ON a.submenu_id = s.id AND a.clave = 'eliminar'
WHERE NOT EXISTS (
    SELECT 1 FROM rol_accion existente WHERE existente.rol_id = rse.rol_id AND existente.accion_submenu_id = a.id
  );

-- ============================================================
-- VERIFICACIÓN
-- ============================================================
-- SELECT s.ruta, a.clave, a.etiqueta, r.nombre_rol
-- FROM rol_accion ra
-- JOIN roles r ON r.id = ra.rol_id
-- JOIN accion_submenu a ON a.id = ra.accion_submenu_id
-- JOIN submenu s ON s.id = a.submenu_id
-- WHERE a.clave = 'eliminar' AND s.ruta = 'lugares-entrega'
-- ORDER BY r.nombre_rol;
