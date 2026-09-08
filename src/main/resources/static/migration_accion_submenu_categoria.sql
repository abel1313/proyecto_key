-- ============================================================
-- Migración: agrupar visualmente el checklist de acciones en Gestión de roles
-- (sigue a migration_accion_modelos_etiquetas_y_escaner.sql)
--
-- Motivo: con 15 acciones en Modelos y 11 en Tienda, salían todas juntas en una sola lista --
-- el usuario pidió que se vea separado: primero "Filtros" (los 9 checkboxes + Excel, que viven
-- en la misma barra de filtros), después "Tarjeta de modelo/variante" (los botones de cada
-- tarjeta), y "Buscador" para lo que está pegado al campo de búsqueda (el escáner).
--
-- El AGRUPAMIENTO lo decide la columna nueva `categoria`; el front agrupa por bloques
-- CONTIGUOS de `orden` (no reordena por su cuenta), así que esta migración también renumera
-- `orden` para que cada categoría quede junta.
-- ============================================================

ALTER TABLE accion_submenu ADD COLUMN categoria VARCHAR(60) NULL;

-- ── Modelos (productos/buscar) ──────────────────────────────────────
UPDATE accion_submenu a
JOIN submenu s ON s.id = a.submenu_id
SET a.categoria = 'Filtros',
    a.orden = CASE a.clave
        WHEN 'filtro-con-stock'       THEN 1
        WHEN 'filtro-sin-stock'       THEN 2
        WHEN 'filtro-con-imagenes'    THEN 3
        WHEN 'filtro-sin-imagenes'    THEN 4
        WHEN 'filtro-habilitados'     THEN 5
        WHEN 'filtro-no-habilitados'  THEN 6
        WHEN 'filtro-codigo-generado' THEN 7
        WHEN 'filtro-codigo-real'     THEN 8
        WHEN 'filtro-fecha-creacion'  THEN 9
        WHEN 'descargar-excel'        THEN 10
    END
WHERE s.ruta = 'productos/buscar'
  AND a.clave IN ('filtro-con-stock','filtro-sin-stock','filtro-con-imagenes','filtro-sin-imagenes',
                   'filtro-habilitados','filtro-no-habilitados','filtro-codigo-generado',
                   'filtro-codigo-real','filtro-fecha-creacion','descargar-excel');

UPDATE accion_submenu a
JOIN submenu s ON s.id = a.submenu_id
SET a.categoria = 'Tarjeta de modelo',
    a.orden = CASE a.clave
        WHEN 'eliminar'         THEN 11
        WHEN 'habilitar'        THEN 12
        WHEN 'crear-variantes'  THEN 13
        WHEN 'compartir-imagen' THEN 14
    END
WHERE s.ruta = 'productos/buscar'
  AND a.clave IN ('eliminar','habilitar','crear-variantes','compartir-imagen');

UPDATE accion_submenu a
JOIN submenu s ON s.id = a.submenu_id
SET a.categoria = 'Buscador',
    a.orden = 15
WHERE s.ruta = 'productos/buscar' AND a.clave = 'escanear-codigo';

-- ── Tienda (tienda/buscar) ───────────────────────────────────────────
UPDATE accion_submenu a
JOIN submenu s ON s.id = a.submenu_id
SET a.categoria = 'Filtros',
    a.orden = CASE a.clave
        WHEN 'filtro-con-stock'       THEN 1
        WHEN 'filtro-sin-stock'       THEN 2
        WHEN 'filtro-con-imagenes'    THEN 3
        WHEN 'filtro-sin-imagenes'    THEN 4
        WHEN 'filtro-habilitados'     THEN 5
        WHEN 'filtro-no-habilitados'  THEN 6
        WHEN 'filtro-codigo-generado' THEN 7
        WHEN 'filtro-codigo-real'     THEN 8
        WHEN 'filtro-fecha-creacion'  THEN 9
    END
WHERE s.ruta = 'tienda/buscar'
  AND a.clave IN ('filtro-con-stock','filtro-sin-stock','filtro-con-imagenes','filtro-sin-imagenes',
                   'filtro-habilitados','filtro-no-habilitados','filtro-codigo-generado',
                   'filtro-codigo-real','filtro-fecha-creacion');

UPDATE accion_submenu a
JOIN submenu s ON s.id = a.submenu_id
SET a.categoria = 'Tarjeta de variante',
    a.orden = CASE a.clave
        WHEN 'habilitar'        THEN 10
        WHEN 'compartir-imagen' THEN 11
    END
WHERE s.ruta = 'tienda/buscar' AND a.clave IN ('habilitar','compartir-imagen');

-- ============================================================
-- VERIFICACIÓN
-- ============================================================
-- SELECT s.ruta, a.categoria, a.orden, a.clave, a.etiqueta
-- FROM accion_submenu a
-- JOIN submenu s ON s.id = a.submenu_id
-- WHERE s.ruta IN ('productos/buscar', 'tienda/buscar')
-- ORDER BY s.ruta, a.orden;
