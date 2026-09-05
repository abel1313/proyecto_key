-- ============================================================
-- Migración: acción "escanear-codigo" para "tienda/buscar" (Tienda)
-- (mismo patrón que migration_accion_modelos_etiquetas_y_escaner.sql en Modelos)
--
-- Motivo: el escáner de código de barras (📷, botón del buscador + botón de arriba en vista
-- móvil) en Tienda hoy es incondicional para cualquiera que abra la pantalla en modo admin
-- (logueado). El usuario pidió que TODO lo que tiene la pantalla tenga su propio permiso
-- separado, sin excepciones -- esto incluye el escáner.
--
-- Distinto de Modelos en un punto importante: Tienda es la vitrina PÚBLICA -- un visitante
-- anónimo (sin sesión) también usa el escáner para buscar productos. Esta migración NO cambia
-- eso: el front sigue mostrando el botón sin condición a cualquiera sin sesión. Esta acción solo
-- aplica quien SÍ tiene una cuenta (cualquier rol, incluido ROLE_ADMIN) -- se le da por defecto
-- solo a ROLE_ADMIN para preservar su comportamiento actual.
-- ============================================================

INSERT INTO accion_submenu (submenu_id, clave, etiqueta, descripcion, categoria, orden)
SELECT s.id, 'escanear-codigo', 'Escanear código de barras (📷)',
       'Botón 📷 del buscador (a la derecha del campo de texto) y botón 📷 "Escanear código de barras" que aparece arriba del buscador en vista móvil, en Tienda. Abre la cámara para leer el código de barras y buscar automáticamente por él. NO afecta a visitantes sin cuenta -- ellos siempre lo ven; esta acción solo aplica a cuentas con sesión.',
       'Buscador', 2
FROM submenu s
WHERE s.ruta = 'tienda/buscar'
  AND NOT EXISTS (
    SELECT 1 FROM accion_submenu existente WHERE existente.submenu_id = s.id AND existente.clave = 'escanear-codigo'
  );

-- Se lo damos a ROLE_ADMIN para no perder el comportamiento actual -- cualquier otro rol que hoy
-- lo use (porque el botón era incondicional para cualquier cuenta logueada) lo deja de ver hasta
-- que se le asigne desde Gestión de roles.
INSERT INTO rol_accion (rol_id, accion_submenu_id)
SELECT r.id, a.id
FROM roles r
CROSS JOIN accion_submenu a
JOIN submenu s ON s.id = a.submenu_id AND s.ruta = 'tienda/buscar'
WHERE r.nombre_rol = 'ROLE_ADMIN'
  AND a.clave = 'escanear-codigo'
  AND NOT EXISTS (
    SELECT 1 FROM rol_accion existente WHERE existente.rol_id = r.id AND existente.accion_submenu_id = a.id
  );

-- ============================================================
-- VERIFICACIÓN
-- ============================================================
-- SELECT a.clave, a.etiqueta, a.categoria
-- FROM accion_submenu a
-- JOIN submenu s ON s.id = a.submenu_id
-- WHERE s.ruta = 'tienda/buscar'
-- ORDER BY a.orden;
