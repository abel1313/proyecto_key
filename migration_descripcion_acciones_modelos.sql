-- ============================================================
-- Migración: descripción ("¿para qué sirve? ¿dónde lo veo?") de las 5 acciones originales
-- de Modelos (habilitar, eliminar, crear-variantes, compartir-imagen, descargar-excel).
--
-- Las 9 acciones de filtros (migration_filtros_granulares.sql) ya nacieron con descripcion --
-- estas 5 son anteriores (migration_accion_submenu.sql) y se quedaron sin ese campo. En Gestión
-- de roles, cada acción ahora tiene un botón ℹ️ junto al checkbox que abre un popup con este
-- texto -- antes solo había un tooltip al pasar el mouse (y estas 5 lo tenían vacío).
-- ============================================================

UPDATE accion_submenu a
JOIN submenu s ON s.id = a.submenu_id AND s.ruta = 'productos/buscar'
SET a.descripcion = CASE a.clave
    WHEN 'habilitar' THEN
        'Deja Habilitar/Deshabilitar un producto para que se vea o no en la Tienda. Aparece en Modelos (productos/buscar) de 3 formas: el checkbox de selección en cada tarjeta, la barra "Habilitar/Deshabilitar seleccionados" cuando hay varias marcadas, y el botón 🔒/🔓 individual de cada tarjeta.'
    WHEN 'eliminar' THEN
        'Deja borrar un producto por completo (con sus variantes e imágenes). Aparece en Modelos (productos/buscar): el botón 🗑️ en cada tarjeta.'
    WHEN 'crear-variantes' THEN
        'Deja crear variantes (tallas/colores) en lote a partir de un modelo. Aparece en Modelos (productos/buscar): el botón para crear variantes en cada tarjeta de producto.'
    WHEN 'compartir-imagen' THEN
        'Deja compartir la imagen del producto (WhatsApp, etc.). Aparece en Modelos (productos/buscar): el botón 📤 en las tarjetas que ya tienen imagen cargada.'
    WHEN 'descargar-excel' THEN
        'Deja descargar un Excel con los modelos que todavía no tienen producto/variante creada. Aparece en Modelos (productos/buscar): el botón "📥 Excel sin productos" en la barra de filtros.'
    ELSE a.descripcion
END
WHERE a.clave IN ('habilitar', 'eliminar', 'crear-variantes', 'compartir-imagen', 'descargar-excel');

-- ============================================================
-- VERIFICACIÓN
-- ============================================================
-- SELECT clave, etiqueta, descripcion FROM accion_submenu WHERE clave IN
--   ('habilitar','eliminar','crear-variantes','compartir-imagen','descargar-excel');
