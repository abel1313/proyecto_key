-- ============================================================
-- Migración: completar `descripcion` de las 5 acciones originales de "Modelos" (productos/buscar)
-- (sigue a migration_accion_tienda_habilitar_compartir.sql)
--
-- Motivo: estas 5 acciones (habilitar, eliminar, crear-variantes, compartir-imagen,
-- descargar-excel) se sembraron en migration_accion_submenu.sql, ANTES de que existiera la
-- columna `descripcion` (la agregó migration_filtros_granulares.sql, y ahi solo se le puso
-- descripcion a los 9 filtro-* nuevos, no se toco lo que ya existia de Modelos). Resultado: en
-- Gestión de roles, el botón ℹ️ de estas 5 casillas de Modelos muestra "Todavía no tiene
-- descripción cargada" -- a diferencia de Tienda, que desde el arranque las tiene completas.
-- Esto solo agrega el texto que faltaba, no crea filas ni cambia permisos de ningún rol.
-- ============================================================

UPDATE accion_submenu a
JOIN submenu s ON s.id = a.submenu_id
SET a.descripcion = CASE a.clave
    WHEN 'habilitar' THEN
        'Barra de acciones en lote (arriba de la lista, al seleccionar), checkbox de selección de cada tarjeta, y botón 🔒/🔓 de habilitar/deshabilitar en el pie de cada tarjeta de modelo, en Modelos.'
    WHEN 'eliminar' THEN
        'Botón ✕ de eliminar, en la esquina de la tarjeta de cada modelo, en Modelos.'
    WHEN 'crear-variantes' THEN
        'Botón 🧩 "Productos" en el pie de la tarjeta de cada modelo, en Modelos.'
    WHEN 'compartir-imagen' THEN
        'Botón 📤 "Imagen" (compartir) en el pie de la tarjeta de cada modelo, en Modelos.'
    WHEN 'descargar-excel' THEN
        'Botón 📥 "Excel sin productos" en la barra de filtros, arriba de la lista, en Modelos.'
    END
WHERE s.ruta = 'productos/buscar'
  AND a.clave IN ('habilitar', 'eliminar', 'crear-variantes', 'compartir-imagen', 'descargar-excel')
  AND (a.descripcion IS NULL OR a.descripcion = '');

-- ============================================================
-- VERIFICACIÓN
-- ============================================================
-- SELECT a.clave, a.etiqueta, a.descripcion
-- FROM accion_submenu a
-- JOIN submenu s ON s.id = a.submenu_id
-- WHERE s.ruta = 'productos/buscar'
-- ORDER BY a.orden;
