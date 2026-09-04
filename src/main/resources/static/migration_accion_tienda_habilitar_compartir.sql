-- ============================================================
-- Migración: extender permisos finos (Fase 3) a "tienda/buscar" -- acciones
-- "habilitar" y "compartir-imagen"
-- (sigue a migration_filtros_granulares.sql, que ya migró los filtros de esta pantalla)
--
-- Motivo: "tienda/buscar" es el piloto elegido para llevar el sistema de acciones granulares
-- (ya usado en Modelos) más allá de los filtros. Faltaban 2 botones de la tarjeta de variante que
-- hoy dependen solo de isAdminUser en el front:
--   - "habilitar": el toggle de habilitar/deshabilitar variante (individual y en lote). Tiene
--     back real -- PUT /tienda/v1/{id}/habilitar y PUT /tienda/v1/admin/habilitar-lote -- así que
--     esta migración le agrega también el gate en SecurityConfig (ver accion("tienda/buscar",
--     "habilitar")), igual que ya existe para el equivalente en Modelos (productos/buscar).
--   - "compartir-imagen": el botón de compartir a WhatsApp/Facebook/descargar. Es 100% frontend
--     (CompartirService solo hace GET a la URL de imagen ya pública, no llama ningún endpoint
--     propio) -- no necesita gate en el back, solo el checkbox en Gestión de roles para
--     mostrar/ocultar el botón.
--
-- El "editar" (✏️) y el checkbox de selección en lote de esa misma tarjeta se quedan por ahora
-- bajo el isAdminUser general de la pantalla -- no se les da acción propia en esta migración.
-- El escáner de cámara (📷) se deja público/sin permiso a propósito -- no es una acción admin.
-- ============================================================

INSERT INTO accion_submenu (submenu_id, clave, etiqueta, descripcion, orden)
SELECT s.id, v.clave, v.etiqueta, v.descripcion, v.orden
FROM submenu s
CROSS JOIN (
    SELECT 'habilitar'         AS clave, 'Habilitar / deshabilitar variante' AS etiqueta,
           'Interruptor de habilitar/deshabilitar en la tarjeta de cada variante y en la barra de selección en lote, en Tienda.' AS descripcion, 16 AS orden UNION ALL
    SELECT 'compartir-imagen',     'Compartir imagen',
           'Botón "Compartir" (WhatsApp/Facebook/descargar) en la tarjeta de cada variante, en Tienda.', 17
) v
WHERE s.ruta = 'tienda/buscar'
  AND NOT EXISTS (
    SELECT 1 FROM accion_submenu existente WHERE existente.submenu_id = s.id AND existente.clave = v.clave
  );

-- ------------------------------------------------------------
-- Preserva el comportamiento actual: hoy esos 2 botones dependen solo de isAdminUser
-- (ROLE_ADMIN), así que se le dan las 2 acciones nuevas a ROLE_ADMIN para no perder nada.
-- ------------------------------------------------------------
INSERT INTO rol_accion (rol_id, accion_submenu_id)
SELECT r.id, a.id
FROM roles r
CROSS JOIN accion_submenu a
JOIN submenu s ON s.id = a.submenu_id AND s.ruta = 'tienda/buscar'
WHERE r.nombre_rol = 'ROLE_ADMIN'
  AND a.clave IN ('habilitar', 'compartir-imagen')
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
-- WHERE a.clave IN ('habilitar', 'compartir-imagen') AND s.ruta = 'tienda/buscar'
-- ORDER BY r.nombre_rol, a.orden;
