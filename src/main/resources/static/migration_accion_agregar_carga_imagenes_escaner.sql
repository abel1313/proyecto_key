-- ============================================================
-- Migración: acciones puntuales para "Agregar modelo" (productos/agregar) y "Carga rápida de
-- imágenes" (carga-imagenes) -- continuación del audit sistemático de permisos finos iniciado en
-- Modelos/Tienda/Envíos (2026-09-08).
--
-- Motivo: ambas pantallas tienen un botón de cámara para escanear código de barras (mismo caso
-- ya resuelto en Modelos con migration_accion_modelos_etiquetas_y_escaner.sql -- "el escáner es
-- público, cualquiera con acceso a la pantalla lo usa, sin permiso propio"), y "Carga rápida de
-- imágenes" además tiene un botón 🎲 para generar un código de barras aleatorio, igual de abierto.
-- Ninguna de las dos pantallas tenía accion_submenu todavía.
--
-- El resto de los botones de estas 2 pantallas (guardar, subir foto, tomar foto con cámara,
-- quitar imagen) son parte del mismo flujo de creación -- no tiene sentido de negocio separarlos
-- (no hay caso de "puede crear un modelo pero no puede tomarle foto"), así que se quedan bajo el
-- Ver general de la pantalla, sin acción puntual.
-- ============================================================

INSERT INTO accion_submenu (submenu_id, clave, etiqueta, descripcion, orden)
SELECT s.id, 'escanear-codigo', 'Escanear código de barras (📷)',
       'Botón 📷 junto al campo de código de barras, en Agregar modelo. Abre la cámara para leer el código y rellenar el campo automáticamente.',
       1
FROM submenu s
WHERE s.ruta = 'productos/agregar'
  AND NOT EXISTS (
    SELECT 1 FROM accion_submenu existente WHERE existente.submenu_id = s.id AND existente.clave = 'escanear-codigo'
  );

INSERT INTO accion_submenu (submenu_id, clave, etiqueta, descripcion, orden)
SELECT s.id, 'escanear-codigo', 'Escanear código de barras (📷)',
       'Botón 📷 junto al campo "Código de barras real" del formulario, en Carga rápida de imágenes. Abre la cámara para leer el código y rellenar el campo automáticamente.',
       1
FROM submenu s
WHERE s.ruta = 'carga-imagenes'
  AND NOT EXISTS (
    SELECT 1 FROM accion_submenu existente WHERE existente.submenu_id = s.id AND existente.clave = 'escanear-codigo'
  );

INSERT INTO accion_submenu (submenu_id, clave, etiqueta, descripcion, orden)
SELECT s.id, 'generar-codigo-barras', 'Generar código automático (🎲)',
       'Botón 🎲 junto al campo "Código de barras real" del formulario, en Carga rápida de imágenes. Genera un código aleatorio (fecha + números) sin necesidad de escanear.',
       2
FROM submenu s
WHERE s.ruta = 'carga-imagenes'
  AND NOT EXISTS (
    SELECT 1 FROM accion_submenu existente WHERE existente.submenu_id = s.id AND existente.clave = 'generar-codigo-barras'
  );

-- Se le dan a todo rol que hoy tenga Ver en cada pantalla, para preservar el comportamiento
-- actual (los botones eran públicos para cualquiera con acceso a la pantalla, sin permiso
-- propio). A partir de aquí, un rol nuevo que se le dé la pantalla ya NO trae estos botones por
-- default -- hay que asignárselos aparte desde Gestión de roles.
INSERT INTO rol_accion (rol_id, accion_submenu_id)
SELECT rs.rol_id, a.id
FROM rol_submenu rs
JOIN submenu s ON s.id = rs.submenu_id AND s.ruta IN ('productos/agregar', 'carga-imagenes')
JOIN accion_submenu a ON a.submenu_id = s.id
WHERE NOT EXISTS (
    SELECT 1 FROM rol_accion existente WHERE existente.rol_id = rs.rol_id AND existente.accion_submenu_id = a.id
  );

-- ============================================================
-- VERIFICACIÓN
-- ============================================================
-- SELECT s.ruta, a.clave, a.etiqueta, a.descripcion, a.orden
-- FROM accion_submenu a
-- JOIN submenu s ON s.id = a.submenu_id
-- WHERE s.ruta IN ('productos/agregar', 'carga-imagenes')
-- ORDER BY s.ruta, a.orden;
