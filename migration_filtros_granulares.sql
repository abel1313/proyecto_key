-- ============================================================
-- Migración: separar el permiso único "filtros-admin" en un checkbox por filtro
-- (Fase 3 de permisos, sigue a migration_accion_submenu.sql)
--
-- Motivo: "filtros-admin" era UN solo permiso que mostraba/ocultaba TODA la barra de filtros de
-- Modelos y de Tienda (Con stock, Sin stock, Con imágenes, ..., rango de fecha) en bloque. El
-- usuario pidió poder darle a un rol, por ejemplo, solo "Con stock" y "Habilitados" sin los
-- demás -- así que cada checkbox pasa a ser su propia acción, configurable por separado desde
-- Gestión de roles. También se agrega `descripcion` (columna nueva en accion_submenu) para que
-- cada checkbox en Gestión de roles traiga un tooltip que diga en qué pantalla y en qué parte de
-- la barra de filtros aparece -- no solo un nombre corto y ya.
--
-- Aplica a las 2 pantallas que hoy tienen esta barra de filtros: "productos/buscar" (Modelos,
-- ya estaba en el sistema de acciones) y "tienda/buscar" (Tienda/Variantes, hoy controlado solo
-- por isAdminUser en el front -- esta migración lo suma también al sistema de acciones, sin
-- tocar el resto de isAdminUser en esa pantalla, que sigue pendiente aparte).
-- ============================================================

ALTER TABLE accion_submenu ADD COLUMN descripcion VARCHAR(255) NULL;

-- ------------------------------------------------------------
-- Salvaguarda: "tienda/buscar" (la vitrina pública) no tenía por qué tener guard de pantalla, así
-- que puede que ningún rol la tenga marcada como Ver explícitamente pese a que ROLE_ADMIN la
-- recibió en la siembra original (migration_rol_submenu_usuario_submenu.sql). Las acciones
-- requieren Ver como prerequisito (RolesServiceImpl.agregarAccion), así que se asegura acá antes
-- de sembrar las acciones de esa pantalla -- mismo patrón que migration_fix_submenu_gestion_menu.sql.
-- ------------------------------------------------------------
INSERT INTO rol_submenu (rol_id, submenu_id)
SELECT r.id, s.id
FROM roles r
CROSS JOIN submenu s
WHERE r.nombre_rol = 'ROLE_ADMIN'
  AND s.ruta = 'tienda/buscar'
  AND NOT EXISTS (
    SELECT 1 FROM rol_submenu rs WHERE rs.rol_id = r.id AND rs.submenu_id = s.id
  );

-- ------------------------------------------------------------
-- Nuevas acciones -- una por checkbox, en "productos/buscar" (Modelos) y "tienda/buscar" (Tienda)
-- ------------------------------------------------------------
INSERT INTO accion_submenu (submenu_id, clave, etiqueta, descripcion, orden)
SELECT s.id, v.clave, v.etiqueta, v.descripcion, v.orden
FROM submenu s
CROSS JOIN (
    SELECT 'filtro-con-stock'      AS clave, 'Filtro: Con stock'                       AS etiqueta,
           'Casilla "Con stock" en la barra de filtros, arriba de la lista.'           AS descripcion, 7 AS orden UNION ALL
    SELECT 'filtro-sin-stock',         'Filtro: Sin stock',
           'Casilla "Sin stock" en la barra de filtros, arriba de la lista.',           8 UNION ALL
    SELECT 'filtro-con-imagenes',      'Filtro: Con imágenes',
           'Casilla "Con imágenes" en la barra de filtros, arriba de la lista.',        9 UNION ALL
    SELECT 'filtro-sin-imagenes',      'Filtro: Sin imágenes',
           'Casilla "Sin imágenes" en la barra de filtros, arriba de la lista.',        10 UNION ALL
    SELECT 'filtro-habilitados',       'Filtro: Habilitados',
           'Casilla "Habilitados" en la barra de filtros, arriba de la lista.',         11 UNION ALL
    SELECT 'filtro-no-habilitados',    'Filtro: No habilitados',
           'Casilla "No habilitados" en la barra de filtros, arriba de la lista.',      12 UNION ALL
    SELECT 'filtro-codigo-generado',   'Filtro: Código generado',
           'Casilla "Código generado" en la barra de filtros, arriba de la lista.',     13 UNION ALL
    SELECT 'filtro-codigo-real',       'Filtro: Código real',
           'Casilla "Código real" en la barra de filtros, arriba de la lista.',         14 UNION ALL
    SELECT 'filtro-fecha-creacion',    'Filtro: Rango de fecha de creación',
           'Campos "Creado desde" / "Creado hasta" en la barra de filtros.',            15
) v
WHERE s.ruta IN ('productos/buscar', 'tienda/buscar');

-- ------------------------------------------------------------
-- Preserva el comportamiento actual de Modelos: todo rol que ya tenía "filtros-admin" (veía TODA
-- la barra) recibe automáticamente las 9 acciones nuevas -- desde Gestión de roles se le pueden
-- quitar individualmente después.
-- ------------------------------------------------------------
INSERT INTO rol_accion (rol_id, accion_submenu_id)
SELECT ra.rol_id, nueva.id
FROM rol_accion ra
JOIN accion_submenu vieja ON vieja.id = ra.accion_submenu_id AND vieja.clave = 'filtros-admin'
JOIN accion_submenu nueva ON nueva.submenu_id = vieja.submenu_id
    AND nueva.clave IN ('filtro-con-stock','filtro-sin-stock','filtro-con-imagenes','filtro-sin-imagenes',
                         'filtro-habilitados','filtro-no-habilitados','filtro-codigo-generado',
                         'filtro-codigo-real','filtro-fecha-creacion');

-- Se quita el permiso viejo (ya cumplió su función arriba) -- el cascade de rol_accion ya corrió
-- en el INSERT anterior, esto solo limpia el catálogo para no dejar un checkbox fantasma.
DELETE FROM accion_submenu WHERE clave = 'filtros-admin';

-- ------------------------------------------------------------
-- Preserva el comportamiento actual de Tienda: hoy la barra de filtros ahí depende solo de
-- isAdminUser (ROLE_ADMIN a secas), así que se le da a ROLE_ADMIN las 9 acciones de
-- "tienda/buscar" para que no pierda nada con el cambio.
-- ------------------------------------------------------------
INSERT INTO rol_accion (rol_id, accion_submenu_id)
SELECT r.id, a.id
FROM roles r
CROSS JOIN accion_submenu a
JOIN submenu s ON s.id = a.submenu_id AND s.ruta = 'tienda/buscar'
WHERE r.nombre_rol = 'ROLE_ADMIN'
  AND a.clave LIKE 'filtro-%'
  AND NOT EXISTS (
    SELECT 1 FROM rol_accion existente WHERE existente.rol_id = r.id AND existente.accion_submenu_id = a.id
  );

-- ============================================================
-- VERIFICACIÓN
-- ============================================================
-- SELECT s.ruta, a.clave, a.etiqueta, r.nombre_rol
-- FROM rol_accion ra
-- JOIN roles r ON r.id = ra.rol_id
-- JOIN accion_submenu a ON a.id = ra.accion_submenu_id
-- JOIN submenu s ON s.id = a.submenu_id
-- WHERE a.clave LIKE 'filtro-%'
-- ORDER BY s.ruta, r.nombre_rol, a.orden;
