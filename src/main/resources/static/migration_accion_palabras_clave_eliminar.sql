-- ============================================================
-- Migración: acción puntual "eliminar" para "palabras-clave" (Categorías)
-- (Fase 3 de permisos, continúa el mismo patrón de Modelos/Tienda -- ver
-- migration_accion_tienda_habilitar_compartir.sql)
--
-- Motivo: la pantalla de Categorías tenía 2 botones por fila (✏️ Editar, 🗑️ Eliminar) sin
-- ningún permiso puntual -- "Editar" ya lo cubre pantallaEscribir("palabras-clave") (crear/
-- editar todo junto), pero no había forma de dar Editar sin dar también Eliminar, o viceversa.
-- Esto separa "eliminar" como su propia acción, igual que se hizo con "eliminar"/"habilitar" en
-- Modelos y Tienda.
-- ============================================================

INSERT INTO accion_submenu (submenu_id, clave, etiqueta, descripcion, categoria, orden)
SELECT s.id, 'eliminar', 'Eliminar categoría',
       'Botón 🗑️ de eliminar, junto a cada categoría en la lista, en Categorías (palabras clave).',
       'Tarjeta', 1
FROM submenu s
WHERE s.ruta = 'palabras-clave'
  AND NOT EXISTS (
    SELECT 1 FROM accion_submenu a WHERE a.submenu_id = s.id AND a.clave = 'eliminar'
  );

-- ------------------------------------------------------------
-- Preserva el comportamiento actual: todo rol que hoy puede escribir en "palabras-clave"
-- (rol_submenu_escritura) recibe automáticamente el permiso de eliminar -- desde Gestión de
-- roles se le puede quitar después sin afectar su Editar.
-- ------------------------------------------------------------
INSERT INTO rol_accion (rol_id, accion_submenu_id)
SELECT rse.rol_id, a.id
FROM rol_submenu_escritura rse
JOIN submenu s ON s.id = rse.submenu_id AND s.ruta = 'palabras-clave'
JOIN accion_submenu a ON a.submenu_id = s.id AND a.clave = 'eliminar'
WHERE NOT EXISTS (
    SELECT 1 FROM rol_accion existente WHERE existente.rol_id = rse.rol_id AND existente.accion_submenu_id = a.id
);

-- ============================================================
-- VERIFICACIÓN
-- ============================================================
-- SELECT r.nombre_rol, a.clave, a.etiqueta
-- FROM rol_accion ra
-- JOIN roles r ON r.id = ra.rol_id
-- JOIN accion_submenu a ON a.id = ra.accion_submenu_id
-- JOIN submenu s ON s.id = a.submenu_id
-- WHERE s.ruta = 'palabras-clave';
