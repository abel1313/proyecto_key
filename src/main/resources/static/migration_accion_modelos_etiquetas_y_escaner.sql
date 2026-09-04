-- ============================================================
-- Migración: renombrar etiquetas de las acciones de Modelos (productos/buscar) con el
-- icono/ubicación real del botón, y agregar la acción "escanear-codigo" (antes público)
-- (sigue a migration_descripcion_acciones_modelos.sql)
--
-- Motivo (feedback del usuario revisando Gestión de roles en Modelos):
--   - La etiqueta "Eliminar producto" y "Crear variantes" no dejan claro a qué botón real de la
--     pantalla corresponden -- "Crear variantes" en particular NO aparece tal cual en pantalla
--     (el botón dice "🧩 Productos", que además choca con que la pantalla entera también se
--     llama "Productos"). La descripcion ya tenía el icono real (ver migracion anterior), pero
--     el usuario lo ve recien al hacer click en el ℹ️ -- la ETIQUETA (lo que se lee de entrada,
--     sin hacer click) tambien tiene que traer el icono para no generar la confusion.
--   - El escáner de código de barras (📷, dos botones -- el de la barra de búsqueda y el de
--     arriba en vista móvil) hoy es público (cualquiera con acceso a Modelos lo usa, sin permiso
--     propio). El usuario pidió convertirlo en una acción real, bien descrita con su icono.
--
-- Como con el resto de las acciones de Modelos, la nueva "escanear-codigo" se le da solo a
-- ROLE_ADMIN para preservar su comportamiento actual -- cualquier rol no-admin que hoy pueda usar
-- el escáner (porque el botón era público) lo deja de ver hasta que se le asigne el permiso
-- explícitamente desde Gestión de roles.
-- ============================================================

UPDATE accion_submenu a
JOIN submenu s ON s.id = a.submenu_id
SET a.etiqueta = CASE a.clave
    WHEN 'habilitar'        THEN 'Habilitar / deshabilitar (🔒🔓 en la tarjeta y en lote)'
    WHEN 'eliminar'         THEN 'Eliminar (✕ en la tarjeta)'
    WHEN 'crear-variantes'  THEN 'Crear productos desde el modelo (🧩 "Productos" en la tarjeta)'
    WHEN 'compartir-imagen' THEN 'Compartir imagen (📤 "Imagen" en la tarjeta)'
    WHEN 'descargar-excel'  THEN 'Descargar Excel sin productos (📥 en la barra de filtros)'
    END
WHERE s.ruta = 'productos/buscar'
  AND a.clave IN ('habilitar', 'eliminar', 'crear-variantes', 'compartir-imagen', 'descargar-excel');

INSERT INTO accion_submenu (submenu_id, clave, etiqueta, descripcion, orden)
SELECT s.id, 'escanear-codigo', 'Escanear código de barras (📷)',
       'Botón 📷 del buscador (a la derecha del campo de texto) y botón 📷 "Escanear código de barras" que aparece arriba del buscador en vista móvil, en Modelos. Abre la cámara para leer el código de barras y buscar automáticamente por él.',
       16
FROM submenu s
WHERE s.ruta = 'productos/buscar'
  AND NOT EXISTS (
    SELECT 1 FROM accion_submenu existente WHERE existente.submenu_id = s.id AND existente.clave = 'escanear-codigo'
  );

-- Se lo damos a ROLE_ADMIN para no perder el comportamiento actual (era público, cualquier
-- admin lo usaba). Cualquier otro rol que hoy lo use por ser público lo deja de ver hasta que
-- se le asigne desde Gestión de roles.
INSERT INTO rol_accion (rol_id, accion_submenu_id)
SELECT r.id, a.id
FROM roles r
CROSS JOIN accion_submenu a
JOIN submenu s ON s.id = a.submenu_id AND s.ruta = 'productos/buscar'
WHERE r.nombre_rol = 'ROLE_ADMIN'
  AND a.clave = 'escanear-codigo'
  AND NOT EXISTS (
    SELECT 1 FROM rol_accion existente WHERE existente.rol_id = r.id AND existente.accion_submenu_id = a.id
  );

-- ============================================================
-- VERIFICACIÓN
-- ============================================================
-- SELECT a.clave, a.etiqueta, a.descripcion
-- FROM accion_submenu a
-- JOIN submenu s ON s.id = a.submenu_id
-- WHERE s.ruta = 'productos/buscar'
-- ORDER BY a.orden;
