-- ============================================================
-- Migración: acciones puntuales para los filtros públicos del catálogo en "tienda/buscar"
-- (Talla / Color / Marca / Precio)
--
-- Motivo: pedido explícito del dueño -- TODO lo que tiene la pantalla debe tener su propio
-- permiso separado, sin excepciones. Hasta ahora estos 4 selectores (dropdown Talla, dropdown
-- Color, dropdown Marca, rango de precio $mín-$máx) no dependían de ningún permiso -- se
-- mostraban siempre, a cualquiera.
--
-- Igual que con el escáner (migration_accion_tienda_escanear.sql): un visitante SIN sesión sigue
-- viendo estos 4 filtros siempre -- son parte central de cómo cualquier cliente compra en la
-- tienda. La acción solo aplica a cuentas CON sesión (cualquier rol, incluido ROLE_ADMIN), dada
-- por defecto solo a ROLE_ADMIN para preservar el comportamiento actual.
--
-- Categoría propia "Filtros públicos" (no "Filtros" a secas) para no mezclarlos visualmente con
-- los 9 filtros de admin (con-stock, con-imagenes, etc.) en Gestión de roles -- son de naturaleza
-- distinta (uno es catálogo para comprar, el otro es herramienta de administración).
--
-- Renumera orden para mantener cada categoría en un bloque contiguo (el front agrupa así, no
-- reordena por su cuenta):
--   Filtros (admin):      1-9   (sin cambios)
--   Filtros públicos:     10-13 (nuevo, esta migración)
--   Tarjeta de variante:  14-15 (antes 10-11 -- habilitar, compartir-imagen)
--   Buscador:             16    (antes 12, ya corregido en migration_fix_orden_escanear_tienda.sql
--                                 -- escanear-codigo)
-- ============================================================

INSERT INTO accion_submenu (submenu_id, clave, etiqueta, descripcion, categoria, orden)
SELECT s.id, v.clave, v.etiqueta, v.descripcion, 'Filtros públicos', v.orden
FROM submenu s
CROSS JOIN (
    SELECT 'filtro-talla'  AS clave, 'Filtro público: Talla'  AS etiqueta,
           'Selector "Talla" en la barra de filtros públicos del catálogo, en Tienda.' AS descripcion, 10 AS orden UNION ALL
    SELECT 'filtro-color',     'Filtro público: Color',
           'Selector "Color" en la barra de filtros públicos del catálogo, en Tienda.', 11 UNION ALL
    SELECT 'filtro-marca',     'Filtro público: Marca',
           'Selector "Marca" en la barra de filtros públicos del catálogo, en Tienda.', 12 UNION ALL
    SELECT 'filtro-precio',    'Filtro público: Rango de precio',
           'Campos "$ mín" / "$ máx" en la barra de filtros públicos del catálogo, en Tienda.', 13
) v
WHERE s.ruta = 'tienda/buscar'
  AND NOT EXISTS (
    SELECT 1 FROM accion_submenu existente WHERE existente.submenu_id = s.id AND existente.clave = v.clave
  );

-- Recorre "Tarjeta de variante" y "Buscador" para dejar espacio a los 4 nuevos (10-13).
UPDATE accion_submenu a
JOIN submenu s ON s.id = a.submenu_id
SET a.orden = CASE a.clave
    WHEN 'habilitar'        THEN 14
    WHEN 'compartir-imagen' THEN 15
    WHEN 'escanear-codigo'  THEN 16
    END
WHERE s.ruta = 'tienda/buscar' AND a.clave IN ('habilitar', 'compartir-imagen', 'escanear-codigo');

-- Preserva el comportamiento actual: se le dan las 4 acciones nuevas a ROLE_ADMIN.
INSERT INTO rol_accion (rol_id, accion_submenu_id)
SELECT r.id, a.id
FROM roles r
CROSS JOIN accion_submenu a
JOIN submenu s ON s.id = a.submenu_id AND s.ruta = 'tienda/buscar'
WHERE r.nombre_rol = 'ROLE_ADMIN'
  AND a.clave IN ('filtro-talla', 'filtro-color', 'filtro-marca', 'filtro-precio')
  AND NOT EXISTS (
    SELECT 1 FROM rol_accion existente WHERE existente.rol_id = r.id AND existente.accion_submenu_id = a.id
  );

-- ============================================================
-- VERIFICACIÓN
-- ============================================================
-- SELECT a.clave, a.categoria, a.orden
-- FROM accion_submenu a
-- JOIN submenu s ON s.id = a.submenu_id
-- WHERE s.ruta = 'tienda/buscar'
-- ORDER BY a.orden;
