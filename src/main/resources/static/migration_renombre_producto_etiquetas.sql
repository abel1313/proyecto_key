-- ============================================================
-- Renombre «Modelo» → «Producto» (el producto BASE): los TEXTOS de la base
-- Fecha: 2026-10-20 · Rama: rename/modelo-a-producto · Documento: RENOMBRE_MODELO_A_PRODUCTO.md
--
-- Qué cambia (solo lo que el admin LEE):
--   * Catálogo: «🔍 Modelos» → «🔍 Productos» y «➕ Agregar modelo» → «➕ Agregar producto»
--     (submenu.nombre de productos/buscar y productos/agregar).
--   * Sistema → 🛡️ Gestión de roles: etiquetas, explicaciones (ℹ️) y categorías de los permisos que decían
--     «modelo» («Tarjeta de modelo», “Ver todos los modelos”, «Crear artículos desde el modelo»…).
--   * La etiqueta del color «Fondo del recuadro de búsqueda y filtros (Tienda y Modelos)».
--   * La acción «eliminar» de tienda/buscar decía «modelo» pero la tarjeta de Tienda es un ARTÍCULO:
--     esa fila pasa a «artículo», no a «producto».
--
-- Qué NO cambia: rutas, claves de las acciones ('ver-todos-los-modelos', 'crear-variantes'…), permisos de
-- ningún rol. Nadie gana ni pierde acceso; no hace falta volver a entrar, basta recargar la pantalla.
--
-- ⚠️ ORDEN: este script va DESPUÉS de migration_renombre_articulo_etiquetas.sql. Ese otro script reemplaza
-- «Agregar producto» por «Agregar artículo» en todas las filas: si se vuelve a correr DESPUÉS de este, pisaría el
-- renombrado nuevo. No lo vuelvas a correr.
--
-- Se localiza SIEMPRE por ruta / clave, nunca por el texto. Si el dueño editó un texto a mano desde
-- Sistema → Menús y submenús, la fila igual se actualiza SOLO en la palabra «modelo» (el resto del texto se respeta).
-- Un texto que con «producto» (2 letras más por cada «modelo») pasara de 255 caracteres se deja SIN tocar y la
-- verificación del final lo lista.
--
-- Idempotente: la segunda corrida no encuentra «modelo» y no cambia nada.
-- Correr primero en inventario_key_qa (cubre dev y qa); en inventario_key (prod) solo después de validar en QA.
-- ============================================================

SET NAMES utf8mb4;

-- ── 0. ¿Estás en la base correcta? ──────────────────────────────────────────────
-- Tiene que decir inventario_key_qa (dev/qa) o inventario_key (prod). Si dice NULL, haz doble clic en la base
-- (que quede en negritas) o corre antes: USE inventario_key_qa;  Córrelo COMPLETO con el rayo ⚡
-- "Execute (All or Selection)" (Ctrl+Shift+Enter) sin nada seleccionado.
SELECT DATABASE() AS base_en_uso;

-- ── 1. Respaldo (solo la primera vez: si ya existe, no se pisa) ────────────────
CREATE TABLE IF NOT EXISTS submenu_bkp_modelo_producto        AS SELECT * FROM submenu;
CREATE TABLE IF NOT EXISTS accion_submenu_bkp_modelo_producto AS SELECT * FROM accion_submenu;
CREATE TABLE IF NOT EXISTS tema_variable_bkp_modelo_producto  AS SELECT * FROM tema_variable;

-- ── 2. Diagnóstico ANTES (solo SELECT): lo que se va a cambiar ─────────────────
SELECT 'submenu' AS tabla, id, ruta, nombre, descripcion, descripcion_escritura
  FROM submenu
 WHERE ruta IN ('productos/buscar', 'productos/agregar', 'tienda/venta', 'admin/diagnostico-imagenes', 'palabras-clave')
   AND CONCAT_WS(' ', nombre, descripcion, descripcion_escritura) LIKE BINARY '%odelo%';

SELECT 'accion' AS tabla, a.id, s.ruta, a.clave, a.etiqueta, a.categoria, a.descripcion
  FROM accion_submenu a JOIN submenu s ON s.id = a.submenu_id
 WHERE CONCAT_WS(' ', a.etiqueta, a.descripcion, a.categoria) LIKE BINARY '%odelo%';

-- ── 3. La acción «eliminar» de la TARJETA DE TIENDA es un ARTÍCULO (no un producto) ──
UPDATE accion_submenu a JOIN submenu s ON s.id = a.submenu_id
   SET a.etiqueta    = REPLACE(REPLACE(a.etiqueta,    'modelo', 'artículo'), 'Modelo', 'Artículo'),
       a.descripcion = REPLACE(REPLACE(a.descripcion, 'modelo', 'artículo'), 'Modelo', 'Artículo')
 WHERE a.id > 0 AND s.ruta = 'tienda/buscar' AND a.clave = 'eliminar'
   AND CONCAT_WS(' ', a.etiqueta, a.descripcion) LIKE BINARY '%odelo%';

-- ── 4. Menú Catálogo: el nombre de las dos pantallas ───────────────────────────
UPDATE submenu SET nombre = 'Productos'
 WHERE id > 0 AND ruta = 'productos/buscar' AND nombre IN ('Modelos', 'Modelo');

UPDATE submenu SET nombre = 'Agregar producto'
 WHERE id > 0 AND ruta = 'productos/agregar' AND nombre IN ('Agregar modelo', 'Agregar Modelo');

-- ── 5. Barrido palabra por palabra, limitado a las filas conocidas (por ruta / clave) ──
-- Orden: plurales primero, luego singulares; con y sin mayúscula inicial. Las claves no se tocan.
-- Una columna solo se actualiza si el texto nuevo cabe en 255 caracteres.

-- 5.a submenu.descripcion
UPDATE submenu
   SET descripcion = REPLACE(REPLACE(REPLACE(REPLACE(descripcion, 'Modelos', 'Productos'), 'modelos', 'productos'), 'Modelo', 'Producto'), 'modelo', 'producto')
 WHERE id > 0
   AND ruta IN ('productos/buscar', 'productos/agregar', 'tienda/venta', 'admin/diagnostico-imagenes', 'palabras-clave')
   AND descripcion LIKE BINARY '%odelo%'
   AND CHAR_LENGTH(REPLACE(REPLACE(REPLACE(REPLACE(descripcion, 'Modelos', 'Productos'), 'modelos', 'productos'), 'Modelo', 'Producto'), 'modelo', 'producto')) <= 255;

-- 5.b submenu.descripcion_escritura
UPDATE submenu
   SET descripcion_escritura = REPLACE(REPLACE(REPLACE(REPLACE(descripcion_escritura, 'Modelos', 'Productos'), 'modelos', 'productos'), 'Modelo', 'Producto'), 'modelo', 'producto')
 WHERE id > 0
   AND ruta IN ('productos/buscar', 'productos/agregar', 'tienda/venta')
   AND descripcion_escritura LIKE BINARY '%odelo%'
   AND CHAR_LENGTH(REPLACE(REPLACE(REPLACE(REPLACE(descripcion_escritura, 'Modelos', 'Productos'), 'modelos', 'productos'), 'Modelo', 'Producto'), 'modelo', 'producto')) <= 255;

-- 5.c accion_submenu.etiqueta
UPDATE accion_submenu a JOIN submenu s ON s.id = a.submenu_id
   SET a.etiqueta = REPLACE(REPLACE(REPLACE(REPLACE(a.etiqueta, 'Modelos', 'Productos'), 'modelos', 'productos'), 'Modelo', 'Producto'), 'modelo', 'producto')
 WHERE a.id > 0
   AND ( (s.ruta = 'productos/buscar'  AND a.clave IN ('crear-variantes', 'descargar-excel', 'habilitar', 'eliminar', 'compartir-imagen', 'escanear-codigo'))
      OR (s.ruta = 'productos/agregar' AND a.clave = 'escanear-codigo')
      OR (s.ruta = 'tienda/venta'      AND a.clave = 'ver-todos-los-modelos')
      OR (s.ruta = 'pedidos/mis-pedidos' AND a.clave = 'cambiar-articulo') )
   AND a.etiqueta LIKE BINARY '%odelo%'
   AND CHAR_LENGTH(REPLACE(REPLACE(REPLACE(REPLACE(a.etiqueta, 'Modelos', 'Productos'), 'modelos', 'productos'), 'Modelo', 'Producto'), 'modelo', 'producto')) <= 255;

-- 5.d accion_submenu.descripcion
UPDATE accion_submenu a JOIN submenu s ON s.id = a.submenu_id
   SET a.descripcion = REPLACE(REPLACE(REPLACE(REPLACE(a.descripcion, 'Modelos', 'Productos'), 'modelos', 'productos'), 'Modelo', 'Producto'), 'modelo', 'producto')
 WHERE a.id > 0
   AND ( (s.ruta = 'productos/buscar'  AND a.clave IN ('crear-variantes', 'descargar-excel', 'habilitar', 'eliminar', 'compartir-imagen', 'escanear-codigo'))
      OR (s.ruta = 'productos/agregar' AND a.clave = 'escanear-codigo')
      OR (s.ruta = 'tienda/venta'      AND a.clave = 'ver-todos-los-modelos')
      OR (s.ruta = 'pedidos/mis-pedidos' AND a.clave = 'cambiar-articulo') )
   AND a.descripcion LIKE BINARY '%odelo%'
   AND CHAR_LENGTH(REPLACE(REPLACE(REPLACE(REPLACE(a.descripcion, 'Modelos', 'Productos'), 'modelos', 'productos'), 'Modelo', 'Producto'), 'modelo', 'producto')) <= 255;

-- 5.e accion_submenu.categoria: el grupo «Tarjeta de modelo» de Gestión de roles pasa a «Tarjeta de producto»
UPDATE accion_submenu a JOIN submenu s ON s.id = a.submenu_id
   SET a.categoria = 'Tarjeta de producto'
 WHERE a.id > 0 AND s.ruta = 'productos/buscar' AND a.categoria = 'Tarjeta de modelo';

-- 5.f La etiqueta del color de los filtros
UPDATE tema_variable
   SET etiqueta = REPLACE(etiqueta, '(Tienda y Modelos)', '(Tienda y Productos)')
 WHERE clave = 'filtros-panel-bg' AND etiqueta LIKE BINARY '%(Tienda y Modelos)%';

-- ── 6. Verificación DESPUÉS ─────────────────────────────────────────────────────
-- Las dos primeras listan lo que TODAVÍA dice «modelo». Pueden salir filas, y no son error:
--   * pantallas que no estaban en la lista (por ejemplo Reportes: «reporte por modelo»): se revisan una por una
--     y se dice si hay que cambiarlas;
--   * un texto escrito a mano en Menús y submenús, o uno que con «producto» pasaba de 255 caracteres: se
--     corrige a mano desde Sistema → Menús y submenús.
-- Lo que NO debe aparecer ahí son las pantallas y acciones de Catálogo, Tienda y Gestión de roles de la lista de arriba.
SELECT 'submenu con modelo' AS revisar, id, ruta, nombre, descripcion, descripcion_escritura
  FROM submenu
 WHERE CONCAT_WS(' ', nombre, descripcion, descripcion_escritura) LIKE BINARY '%odelo%';

SELECT 'accion con modelo' AS revisar, a.id, s.ruta, a.clave, a.etiqueta, a.categoria, a.descripcion
  FROM accion_submenu a JOIN submenu s ON s.id = a.submenu_id
 WHERE CONCAT_WS(' ', a.etiqueta, a.descripcion, a.categoria) LIKE BINARY '%odelo%';

-- Esta también tiene que salir VACÍA
SELECT 'color con Modelos' AS revisar, clave, etiqueta FROM tema_variable
 WHERE clave = 'filtros-panel-bg' AND etiqueta LIKE BINARY '%(Tienda y Modelos)%';

-- Y esta tiene que devolver 3 filas: «Productos», «Agregar producto» y «Agregar artículo»
SELECT id, ruta, nombre FROM submenu WHERE ruta IN ('productos/buscar', 'productos/agregar', 'tienda/venta');

-- Las claves NO cambian: tiene que devolver 1 fila con 'ver-todos-los-modelos'
SELECT s.ruta, a.clave, a.etiqueta FROM accion_submenu a JOIN submenu s ON s.id = a.submenu_id
 WHERE s.ruta = 'tienda/venta' AND a.clave = 'ver-todos-los-modelos';

-- ============================================================
-- PARA VOLVER ATRÁS (solo si se pide; no correr junto con lo de arriba)
-- ============================================================
-- SET @su = @@SQL_SAFE_UPDATES; SET SQL_SAFE_UPDATES = 0;
-- UPDATE submenu s JOIN submenu_bkp_modelo_producto b ON b.id = s.id
--    SET s.nombre = b.nombre, s.descripcion = b.descripcion, s.descripcion_escritura = b.descripcion_escritura;
-- UPDATE accion_submenu a JOIN accion_submenu_bkp_modelo_producto b ON b.id = a.id
--    SET a.etiqueta = b.etiqueta, a.descripcion = b.descripcion, a.categoria = b.categoria;
-- UPDATE tema_variable t JOIN tema_variable_bkp_modelo_producto b ON b.clave = t.clave SET t.etiqueta = b.etiqueta;
-- SET SQL_SAFE_UPDATES = @su;
