-- ============================================================
-- Renombre variante → artículo: los TEXTOS de la base (H4 y H6 de RENOMBRE_VARIANTE_A_ARTICULO.md)
-- Fecha: 2026-10-08 · Rama: rename/variante-a-articulo
--
-- Qué cambia (solo lo que el admin LEE):
--   * Menú Catálogo: "🧩 Agregar producto" → "🧩 Agregar artículo" (submenu.nombre de tienda/venta).
--   * Sistema → 🛡️ Gestión de roles: etiquetas, explicaciones (ℹ️) y categorías de los permisos que
--     decían "variante" ("Tarjeta de variante", "Habilitar / deshabilitar variante",
--     "Crear productos desde el modelo (🧩 "Productos"…)", "Excel sin productos"…).
--
-- Qué NO cambia: claves de las acciones ('crear-variantes'…), rutas, permisos de ningún rol.
-- Nadie gana ni pierde acceso; no hace falta volver a entrar, basta recargar la pantalla.
--
-- Columnas tocadas: submenu.nombre / descripcion / descripcion_escritura y
-- accion_submenu.etiqueta / descripcion / categoria. Ningún reemplazo alarga el texto
-- ("variante" y "artículo" miden lo mismo), así que no se pasa del largo de la columna.
--
-- Idempotente: correrla dos veces no cambia nada la segunda (ya no encuentra lo que reemplaza).
-- Orden: primero en inventario_key_qa (cubre dev y qa), luego en inventario_key (prod), y solo
-- cuando el front de la rama (que ya dice "artículo" en pantalla) esté en ese ambiente.
-- ============================================================

SET NAMES utf8mb4;

-- ── Diagnóstico ANTES (opcional): lo que se va a cambiar ─────────────────────
-- SELECT 'submenu' AS tabla, id, ruta, nombre, descripcion, descripcion_escritura
--   FROM submenu
--  WHERE nombre = 'Agregar producto'
--     OR CONCAT_WS(' ', nombre, descripcion, descripcion_escritura) LIKE '%variante%'
--     OR CONCAT_WS(' ', descripcion, descripcion_escritura) LIKE '%Agregar producto%';
-- SELECT a.id, s.ruta, a.clave, a.etiqueta, a.categoria, a.descripcion
--   FROM accion_submenu a JOIN submenu s ON s.id = a.submenu_id
--  WHERE CONCAT_WS(' ', a.etiqueta, a.descripcion, a.categoria) LIKE '%variante%'
--     OR CONCAT_WS(' ', a.etiqueta, a.descripcion) LIKE '%"Productos"%'
--     OR CONCAT_WS(' ', a.etiqueta, a.descripcion) LIKE '%sin productos%';

-- ── 1. Textos fijos (se escriben completos: el reemplazo palabra por palabra no los deja bien) ──

-- Menú Catálogo → la pantalla tienda/venta
UPDATE submenu SET nombre = 'Agregar artículo'
 WHERE id > 0 AND ruta = 'tienda/venta' AND nombre = 'Agregar producto';

UPDATE submenu
   SET descripcion = 'Formulario para agregar un artículo (talla/color) a partir de un modelo ya creado. Vive en el menú: Catálogo → Agregar artículo.'
 WHERE id > 0 AND ruta = 'tienda/venta'
   AND descripcion = 'Formulario para agregar un Producto (una variante concreta: talla/color) a partir de un Modelo ya creado. Vive en el menú: Catálogo → Agregar producto.';

-- Modelos (productos/buscar): el botón 🧩 de la tarjeta ahora dice "Artículos"
UPDATE accion_submenu a JOIN submenu s ON s.id = a.submenu_id
   SET a.etiqueta = 'Crear artículos desde el modelo (🧩 "Artículos" en la tarjeta)'
 WHERE a.id > 0 AND s.ruta = 'productos/buscar' AND a.clave = 'crear-variantes'
   AND a.etiqueta = 'Crear productos desde el modelo (🧩 "Productos" en la tarjeta)';

UPDATE accion_submenu a JOIN submenu s ON s.id = a.submenu_id
   SET a.descripcion = 'Deja crear artículos (tallas/colores) a partir de un modelo. Aparece en Modelos (productos/buscar): el botón 🧩 "Artículos" en cada tarjeta de modelo.'
 WHERE a.id > 0 AND s.ruta = 'productos/buscar' AND a.clave = 'crear-variantes'
   AND a.descripcion = 'Deja crear variantes (tallas/colores) en lote a partir de un modelo. Aparece en Modelos (productos/buscar): el botón para crear variantes en cada tarjeta de producto.';

UPDATE accion_submenu a JOIN submenu s ON s.id = a.submenu_id
   SET a.etiqueta = 'Descargar Excel sin artículos (📥 en la barra de filtros)'
 WHERE a.id > 0 AND s.ruta = 'productos/buscar' AND a.clave = 'descargar-excel'
   AND a.etiqueta = 'Descargar Excel sin productos (📥 en la barra de filtros)';

-- ── 2. Lo demás: reemplazo de frases, de la más larga a la más corta ──────────
-- REPLACE distingue mayúsculas, por eso van "Variante" y "variante" por separado. Las frases con
-- artículo ("de la variante" → "del artículo") van antes de la palabra sola para que no quede
-- "de el artículo".

-- submenu.nombre
UPDATE submenu SET nombre = REPLACE(nombre, 'Excel sin productos', 'Excel sin artículos') WHERE id > 0 AND nombre LIKE '%Excel sin productos%';
UPDATE submenu SET nombre = REPLACE(nombre, 'Crear variantes ("Productos")', 'Crear artículos (🧩 "Artículos")') WHERE id > 0 AND nombre LIKE '%Crear variantes ("Productos")%';
UPDATE submenu SET nombre = REPLACE(nombre, 'Descargar Excel de productos sin variantes', 'Descargar Excel de modelos sin artículos') WHERE id > 0 AND nombre LIKE '%Descargar Excel de productos sin variantes%';
UPDATE submenu SET nombre = REPLACE(nombre, '🧩 "Productos"', '🧩 "Artículos"') WHERE id > 0 AND nombre LIKE '%🧩 "Productos"%';
UPDATE submenu SET nombre = REPLACE(nombre, 'Agregar producto', 'Agregar artículo') WHERE id > 0 AND nombre LIKE '%Agregar producto%';
UPDATE submenu SET nombre = REPLACE(nombre, 'no tienen producto/variante creada', 'no tienen artículos') WHERE id > 0 AND nombre LIKE '%no tienen producto/variante creada%';
UPDATE submenu SET nombre = REPLACE(nombre, 'producto/variante', 'artículo') WHERE id > 0 AND nombre LIKE '%producto/variante%';
UPDATE submenu SET nombre = REPLACE(nombre, 'de la variante', 'del artículo') WHERE id > 0 AND nombre LIKE '%de la variante%';
UPDATE submenu SET nombre = REPLACE(nombre, 'a la variante', 'al artículo') WHERE id > 0 AND nombre LIKE '%a la variante%';
UPDATE submenu SET nombre = REPLACE(nombre, 'la variante', 'el artículo') WHERE id > 0 AND nombre LIKE '%la variante%';
UPDATE submenu SET nombre = REPLACE(nombre, 'La variante', 'El artículo') WHERE id > 0 AND nombre LIKE '%La variante%';
UPDATE submenu SET nombre = REPLACE(nombre, 'las variantes', 'los artículos') WHERE id > 0 AND nombre LIKE '%las variantes%';
UPDATE submenu SET nombre = REPLACE(nombre, 'Las variantes', 'Los artículos') WHERE id > 0 AND nombre LIKE '%Las variantes%';
UPDATE submenu SET nombre = REPLACE(nombre, 'unas variantes', 'unos artículos') WHERE id > 0 AND nombre LIKE '%unas variantes%';
UPDATE submenu SET nombre = REPLACE(nombre, 'una variante', 'un artículo') WHERE id > 0 AND nombre LIKE '%una variante%';
UPDATE submenu SET nombre = REPLACE(nombre, 'Una variante', 'Un artículo') WHERE id > 0 AND nombre LIKE '%Una variante%';
UPDATE submenu SET nombre = REPLACE(nombre, 'esta variante', 'este artículo') WHERE id > 0 AND nombre LIKE '%esta variante%';
UPDATE submenu SET nombre = REPLACE(nombre, 'esa variante', 'ese artículo') WHERE id > 0 AND nombre LIKE '%esa variante%';
UPDATE submenu SET nombre = REPLACE(nombre, 'Variantes', 'Artículos') WHERE id > 0 AND nombre LIKE '%Variantes%';
UPDATE submenu SET nombre = REPLACE(nombre, 'Variante', 'Artículo') WHERE id > 0 AND nombre LIKE '%Variante%';
UPDATE submenu SET nombre = REPLACE(nombre, 'variantes', 'artículos') WHERE id > 0 AND nombre LIKE '%variantes%';
UPDATE submenu SET nombre = REPLACE(nombre, 'variante', 'artículo') WHERE id > 0 AND nombre LIKE '%variante%';

-- submenu.descripcion
UPDATE submenu SET descripcion = REPLACE(descripcion, 'Excel sin productos', 'Excel sin artículos') WHERE id > 0 AND descripcion LIKE '%Excel sin productos%';
UPDATE submenu SET descripcion = REPLACE(descripcion, 'Crear variantes ("Productos")', 'Crear artículos (🧩 "Artículos")') WHERE id > 0 AND descripcion LIKE '%Crear variantes ("Productos")%';
UPDATE submenu SET descripcion = REPLACE(descripcion, 'Descargar Excel de productos sin variantes', 'Descargar Excel de modelos sin artículos') WHERE id > 0 AND descripcion LIKE '%Descargar Excel de productos sin variantes%';
UPDATE submenu SET descripcion = REPLACE(descripcion, '🧩 "Productos"', '🧩 "Artículos"') WHERE id > 0 AND descripcion LIKE '%🧩 "Productos"%';
UPDATE submenu SET descripcion = REPLACE(descripcion, 'Agregar producto', 'Agregar artículo') WHERE id > 0 AND descripcion LIKE '%Agregar producto%';
UPDATE submenu SET descripcion = REPLACE(descripcion, 'no tienen producto/variante creada', 'no tienen artículos') WHERE id > 0 AND descripcion LIKE '%no tienen producto/variante creada%';
UPDATE submenu SET descripcion = REPLACE(descripcion, 'producto/variante', 'artículo') WHERE id > 0 AND descripcion LIKE '%producto/variante%';
UPDATE submenu SET descripcion = REPLACE(descripcion, 'de la variante', 'del artículo') WHERE id > 0 AND descripcion LIKE '%de la variante%';
UPDATE submenu SET descripcion = REPLACE(descripcion, 'a la variante', 'al artículo') WHERE id > 0 AND descripcion LIKE '%a la variante%';
UPDATE submenu SET descripcion = REPLACE(descripcion, 'la variante', 'el artículo') WHERE id > 0 AND descripcion LIKE '%la variante%';
UPDATE submenu SET descripcion = REPLACE(descripcion, 'La variante', 'El artículo') WHERE id > 0 AND descripcion LIKE '%La variante%';
UPDATE submenu SET descripcion = REPLACE(descripcion, 'las variantes', 'los artículos') WHERE id > 0 AND descripcion LIKE '%las variantes%';
UPDATE submenu SET descripcion = REPLACE(descripcion, 'Las variantes', 'Los artículos') WHERE id > 0 AND descripcion LIKE '%Las variantes%';
UPDATE submenu SET descripcion = REPLACE(descripcion, 'unas variantes', 'unos artículos') WHERE id > 0 AND descripcion LIKE '%unas variantes%';
UPDATE submenu SET descripcion = REPLACE(descripcion, 'una variante', 'un artículo') WHERE id > 0 AND descripcion LIKE '%una variante%';
UPDATE submenu SET descripcion = REPLACE(descripcion, 'Una variante', 'Un artículo') WHERE id > 0 AND descripcion LIKE '%Una variante%';
UPDATE submenu SET descripcion = REPLACE(descripcion, 'esta variante', 'este artículo') WHERE id > 0 AND descripcion LIKE '%esta variante%';
UPDATE submenu SET descripcion = REPLACE(descripcion, 'esa variante', 'ese artículo') WHERE id > 0 AND descripcion LIKE '%esa variante%';
UPDATE submenu SET descripcion = REPLACE(descripcion, 'Variantes', 'Artículos') WHERE id > 0 AND descripcion LIKE '%Variantes%';
UPDATE submenu SET descripcion = REPLACE(descripcion, 'Variante', 'Artículo') WHERE id > 0 AND descripcion LIKE '%Variante%';
UPDATE submenu SET descripcion = REPLACE(descripcion, 'variantes', 'artículos') WHERE id > 0 AND descripcion LIKE '%variantes%';
UPDATE submenu SET descripcion = REPLACE(descripcion, 'variante', 'artículo') WHERE id > 0 AND descripcion LIKE '%variante%';

-- submenu.descripcion_escritura
UPDATE submenu SET descripcion_escritura = REPLACE(descripcion_escritura, 'Excel sin productos', 'Excel sin artículos') WHERE id > 0 AND descripcion_escritura LIKE '%Excel sin productos%';
UPDATE submenu SET descripcion_escritura = REPLACE(descripcion_escritura, 'Crear variantes ("Productos")', 'Crear artículos (🧩 "Artículos")') WHERE id > 0 AND descripcion_escritura LIKE '%Crear variantes ("Productos")%';
UPDATE submenu SET descripcion_escritura = REPLACE(descripcion_escritura, 'Descargar Excel de productos sin variantes', 'Descargar Excel de modelos sin artículos') WHERE id > 0 AND descripcion_escritura LIKE '%Descargar Excel de productos sin variantes%';
UPDATE submenu SET descripcion_escritura = REPLACE(descripcion_escritura, '🧩 "Productos"', '🧩 "Artículos"') WHERE id > 0 AND descripcion_escritura LIKE '%🧩 "Productos"%';
UPDATE submenu SET descripcion_escritura = REPLACE(descripcion_escritura, 'Agregar producto', 'Agregar artículo') WHERE id > 0 AND descripcion_escritura LIKE '%Agregar producto%';
UPDATE submenu SET descripcion_escritura = REPLACE(descripcion_escritura, 'no tienen producto/variante creada', 'no tienen artículos') WHERE id > 0 AND descripcion_escritura LIKE '%no tienen producto/variante creada%';
UPDATE submenu SET descripcion_escritura = REPLACE(descripcion_escritura, 'producto/variante', 'artículo') WHERE id > 0 AND descripcion_escritura LIKE '%producto/variante%';
UPDATE submenu SET descripcion_escritura = REPLACE(descripcion_escritura, 'de la variante', 'del artículo') WHERE id > 0 AND descripcion_escritura LIKE '%de la variante%';
UPDATE submenu SET descripcion_escritura = REPLACE(descripcion_escritura, 'a la variante', 'al artículo') WHERE id > 0 AND descripcion_escritura LIKE '%a la variante%';
UPDATE submenu SET descripcion_escritura = REPLACE(descripcion_escritura, 'la variante', 'el artículo') WHERE id > 0 AND descripcion_escritura LIKE '%la variante%';
UPDATE submenu SET descripcion_escritura = REPLACE(descripcion_escritura, 'La variante', 'El artículo') WHERE id > 0 AND descripcion_escritura LIKE '%La variante%';
UPDATE submenu SET descripcion_escritura = REPLACE(descripcion_escritura, 'las variantes', 'los artículos') WHERE id > 0 AND descripcion_escritura LIKE '%las variantes%';
UPDATE submenu SET descripcion_escritura = REPLACE(descripcion_escritura, 'Las variantes', 'Los artículos') WHERE id > 0 AND descripcion_escritura LIKE '%Las variantes%';
UPDATE submenu SET descripcion_escritura = REPLACE(descripcion_escritura, 'unas variantes', 'unos artículos') WHERE id > 0 AND descripcion_escritura LIKE '%unas variantes%';
UPDATE submenu SET descripcion_escritura = REPLACE(descripcion_escritura, 'una variante', 'un artículo') WHERE id > 0 AND descripcion_escritura LIKE '%una variante%';
UPDATE submenu SET descripcion_escritura = REPLACE(descripcion_escritura, 'Una variante', 'Un artículo') WHERE id > 0 AND descripcion_escritura LIKE '%Una variante%';
UPDATE submenu SET descripcion_escritura = REPLACE(descripcion_escritura, 'esta variante', 'este artículo') WHERE id > 0 AND descripcion_escritura LIKE '%esta variante%';
UPDATE submenu SET descripcion_escritura = REPLACE(descripcion_escritura, 'esa variante', 'ese artículo') WHERE id > 0 AND descripcion_escritura LIKE '%esa variante%';
UPDATE submenu SET descripcion_escritura = REPLACE(descripcion_escritura, 'Variantes', 'Artículos') WHERE id > 0 AND descripcion_escritura LIKE '%Variantes%';
UPDATE submenu SET descripcion_escritura = REPLACE(descripcion_escritura, 'Variante', 'Artículo') WHERE id > 0 AND descripcion_escritura LIKE '%Variante%';
UPDATE submenu SET descripcion_escritura = REPLACE(descripcion_escritura, 'variantes', 'artículos') WHERE id > 0 AND descripcion_escritura LIKE '%variantes%';
UPDATE submenu SET descripcion_escritura = REPLACE(descripcion_escritura, 'variante', 'artículo') WHERE id > 0 AND descripcion_escritura LIKE '%variante%';

-- accion_submenu.etiqueta
UPDATE accion_submenu SET etiqueta = REPLACE(etiqueta, 'Excel sin productos', 'Excel sin artículos') WHERE id > 0 AND etiqueta LIKE '%Excel sin productos%';
UPDATE accion_submenu SET etiqueta = REPLACE(etiqueta, 'Crear variantes ("Productos")', 'Crear artículos (🧩 "Artículos")') WHERE id > 0 AND etiqueta LIKE '%Crear variantes ("Productos")%';
UPDATE accion_submenu SET etiqueta = REPLACE(etiqueta, 'Descargar Excel de productos sin variantes', 'Descargar Excel de modelos sin artículos') WHERE id > 0 AND etiqueta LIKE '%Descargar Excel de productos sin variantes%';
UPDATE accion_submenu SET etiqueta = REPLACE(etiqueta, '🧩 "Productos"', '🧩 "Artículos"') WHERE id > 0 AND etiqueta LIKE '%🧩 "Productos"%';
UPDATE accion_submenu SET etiqueta = REPLACE(etiqueta, 'Agregar producto', 'Agregar artículo') WHERE id > 0 AND etiqueta LIKE '%Agregar producto%';
UPDATE accion_submenu SET etiqueta = REPLACE(etiqueta, 'no tienen producto/variante creada', 'no tienen artículos') WHERE id > 0 AND etiqueta LIKE '%no tienen producto/variante creada%';
UPDATE accion_submenu SET etiqueta = REPLACE(etiqueta, 'producto/variante', 'artículo') WHERE id > 0 AND etiqueta LIKE '%producto/variante%';
UPDATE accion_submenu SET etiqueta = REPLACE(etiqueta, 'de la variante', 'del artículo') WHERE id > 0 AND etiqueta LIKE '%de la variante%';
UPDATE accion_submenu SET etiqueta = REPLACE(etiqueta, 'a la variante', 'al artículo') WHERE id > 0 AND etiqueta LIKE '%a la variante%';
UPDATE accion_submenu SET etiqueta = REPLACE(etiqueta, 'la variante', 'el artículo') WHERE id > 0 AND etiqueta LIKE '%la variante%';
UPDATE accion_submenu SET etiqueta = REPLACE(etiqueta, 'La variante', 'El artículo') WHERE id > 0 AND etiqueta LIKE '%La variante%';
UPDATE accion_submenu SET etiqueta = REPLACE(etiqueta, 'las variantes', 'los artículos') WHERE id > 0 AND etiqueta LIKE '%las variantes%';
UPDATE accion_submenu SET etiqueta = REPLACE(etiqueta, 'Las variantes', 'Los artículos') WHERE id > 0 AND etiqueta LIKE '%Las variantes%';
UPDATE accion_submenu SET etiqueta = REPLACE(etiqueta, 'unas variantes', 'unos artículos') WHERE id > 0 AND etiqueta LIKE '%unas variantes%';
UPDATE accion_submenu SET etiqueta = REPLACE(etiqueta, 'una variante', 'un artículo') WHERE id > 0 AND etiqueta LIKE '%una variante%';
UPDATE accion_submenu SET etiqueta = REPLACE(etiqueta, 'Una variante', 'Un artículo') WHERE id > 0 AND etiqueta LIKE '%Una variante%';
UPDATE accion_submenu SET etiqueta = REPLACE(etiqueta, 'esta variante', 'este artículo') WHERE id > 0 AND etiqueta LIKE '%esta variante%';
UPDATE accion_submenu SET etiqueta = REPLACE(etiqueta, 'esa variante', 'ese artículo') WHERE id > 0 AND etiqueta LIKE '%esa variante%';
UPDATE accion_submenu SET etiqueta = REPLACE(etiqueta, 'Variantes', 'Artículos') WHERE id > 0 AND etiqueta LIKE '%Variantes%';
UPDATE accion_submenu SET etiqueta = REPLACE(etiqueta, 'Variante', 'Artículo') WHERE id > 0 AND etiqueta LIKE '%Variante%';
UPDATE accion_submenu SET etiqueta = REPLACE(etiqueta, 'variantes', 'artículos') WHERE id > 0 AND etiqueta LIKE '%variantes%';
UPDATE accion_submenu SET etiqueta = REPLACE(etiqueta, 'variante', 'artículo') WHERE id > 0 AND etiqueta LIKE '%variante%';

-- accion_submenu.descripcion
UPDATE accion_submenu SET descripcion = REPLACE(descripcion, 'Excel sin productos', 'Excel sin artículos') WHERE id > 0 AND descripcion LIKE '%Excel sin productos%';
UPDATE accion_submenu SET descripcion = REPLACE(descripcion, 'Crear variantes ("Productos")', 'Crear artículos (🧩 "Artículos")') WHERE id > 0 AND descripcion LIKE '%Crear variantes ("Productos")%';
UPDATE accion_submenu SET descripcion = REPLACE(descripcion, 'Descargar Excel de productos sin variantes', 'Descargar Excel de modelos sin artículos') WHERE id > 0 AND descripcion LIKE '%Descargar Excel de productos sin variantes%';
UPDATE accion_submenu SET descripcion = REPLACE(descripcion, '🧩 "Productos"', '🧩 "Artículos"') WHERE id > 0 AND descripcion LIKE '%🧩 "Productos"%';
UPDATE accion_submenu SET descripcion = REPLACE(descripcion, 'Agregar producto', 'Agregar artículo') WHERE id > 0 AND descripcion LIKE '%Agregar producto%';
UPDATE accion_submenu SET descripcion = REPLACE(descripcion, 'no tienen producto/variante creada', 'no tienen artículos') WHERE id > 0 AND descripcion LIKE '%no tienen producto/variante creada%';
UPDATE accion_submenu SET descripcion = REPLACE(descripcion, 'producto/variante', 'artículo') WHERE id > 0 AND descripcion LIKE '%producto/variante%';
UPDATE accion_submenu SET descripcion = REPLACE(descripcion, 'de la variante', 'del artículo') WHERE id > 0 AND descripcion LIKE '%de la variante%';
UPDATE accion_submenu SET descripcion = REPLACE(descripcion, 'a la variante', 'al artículo') WHERE id > 0 AND descripcion LIKE '%a la variante%';
UPDATE accion_submenu SET descripcion = REPLACE(descripcion, 'la variante', 'el artículo') WHERE id > 0 AND descripcion LIKE '%la variante%';
UPDATE accion_submenu SET descripcion = REPLACE(descripcion, 'La variante', 'El artículo') WHERE id > 0 AND descripcion LIKE '%La variante%';
UPDATE accion_submenu SET descripcion = REPLACE(descripcion, 'las variantes', 'los artículos') WHERE id > 0 AND descripcion LIKE '%las variantes%';
UPDATE accion_submenu SET descripcion = REPLACE(descripcion, 'Las variantes', 'Los artículos') WHERE id > 0 AND descripcion LIKE '%Las variantes%';
UPDATE accion_submenu SET descripcion = REPLACE(descripcion, 'unas variantes', 'unos artículos') WHERE id > 0 AND descripcion LIKE '%unas variantes%';
UPDATE accion_submenu SET descripcion = REPLACE(descripcion, 'una variante', 'un artículo') WHERE id > 0 AND descripcion LIKE '%una variante%';
UPDATE accion_submenu SET descripcion = REPLACE(descripcion, 'Una variante', 'Un artículo') WHERE id > 0 AND descripcion LIKE '%Una variante%';
UPDATE accion_submenu SET descripcion = REPLACE(descripcion, 'esta variante', 'este artículo') WHERE id > 0 AND descripcion LIKE '%esta variante%';
UPDATE accion_submenu SET descripcion = REPLACE(descripcion, 'esa variante', 'ese artículo') WHERE id > 0 AND descripcion LIKE '%esa variante%';
UPDATE accion_submenu SET descripcion = REPLACE(descripcion, 'Variantes', 'Artículos') WHERE id > 0 AND descripcion LIKE '%Variantes%';
UPDATE accion_submenu SET descripcion = REPLACE(descripcion, 'Variante', 'Artículo') WHERE id > 0 AND descripcion LIKE '%Variante%';
UPDATE accion_submenu SET descripcion = REPLACE(descripcion, 'variantes', 'artículos') WHERE id > 0 AND descripcion LIKE '%variantes%';
UPDATE accion_submenu SET descripcion = REPLACE(descripcion, 'variante', 'artículo') WHERE id > 0 AND descripcion LIKE '%variante%';

-- accion_submenu.categoria
UPDATE accion_submenu SET categoria = REPLACE(categoria, 'Excel sin productos', 'Excel sin artículos') WHERE id > 0 AND categoria LIKE '%Excel sin productos%';
UPDATE accion_submenu SET categoria = REPLACE(categoria, 'Crear variantes ("Productos")', 'Crear artículos (🧩 "Artículos")') WHERE id > 0 AND categoria LIKE '%Crear variantes ("Productos")%';
UPDATE accion_submenu SET categoria = REPLACE(categoria, 'Descargar Excel de productos sin variantes', 'Descargar Excel de modelos sin artículos') WHERE id > 0 AND categoria LIKE '%Descargar Excel de productos sin variantes%';
UPDATE accion_submenu SET categoria = REPLACE(categoria, '🧩 "Productos"', '🧩 "Artículos"') WHERE id > 0 AND categoria LIKE '%🧩 "Productos"%';
UPDATE accion_submenu SET categoria = REPLACE(categoria, 'Agregar producto', 'Agregar artículo') WHERE id > 0 AND categoria LIKE '%Agregar producto%';
UPDATE accion_submenu SET categoria = REPLACE(categoria, 'no tienen producto/variante creada', 'no tienen artículos') WHERE id > 0 AND categoria LIKE '%no tienen producto/variante creada%';
UPDATE accion_submenu SET categoria = REPLACE(categoria, 'producto/variante', 'artículo') WHERE id > 0 AND categoria LIKE '%producto/variante%';
UPDATE accion_submenu SET categoria = REPLACE(categoria, 'de la variante', 'del artículo') WHERE id > 0 AND categoria LIKE '%de la variante%';
UPDATE accion_submenu SET categoria = REPLACE(categoria, 'a la variante', 'al artículo') WHERE id > 0 AND categoria LIKE '%a la variante%';
UPDATE accion_submenu SET categoria = REPLACE(categoria, 'la variante', 'el artículo') WHERE id > 0 AND categoria LIKE '%la variante%';
UPDATE accion_submenu SET categoria = REPLACE(categoria, 'La variante', 'El artículo') WHERE id > 0 AND categoria LIKE '%La variante%';
UPDATE accion_submenu SET categoria = REPLACE(categoria, 'las variantes', 'los artículos') WHERE id > 0 AND categoria LIKE '%las variantes%';
UPDATE accion_submenu SET categoria = REPLACE(categoria, 'Las variantes', 'Los artículos') WHERE id > 0 AND categoria LIKE '%Las variantes%';
UPDATE accion_submenu SET categoria = REPLACE(categoria, 'unas variantes', 'unos artículos') WHERE id > 0 AND categoria LIKE '%unas variantes%';
UPDATE accion_submenu SET categoria = REPLACE(categoria, 'una variante', 'un artículo') WHERE id > 0 AND categoria LIKE '%una variante%';
UPDATE accion_submenu SET categoria = REPLACE(categoria, 'Una variante', 'Un artículo') WHERE id > 0 AND categoria LIKE '%Una variante%';
UPDATE accion_submenu SET categoria = REPLACE(categoria, 'esta variante', 'este artículo') WHERE id > 0 AND categoria LIKE '%esta variante%';
UPDATE accion_submenu SET categoria = REPLACE(categoria, 'esa variante', 'ese artículo') WHERE id > 0 AND categoria LIKE '%esa variante%';
UPDATE accion_submenu SET categoria = REPLACE(categoria, 'Variantes', 'Artículos') WHERE id > 0 AND categoria LIKE '%Variantes%';
UPDATE accion_submenu SET categoria = REPLACE(categoria, 'Variante', 'Artículo') WHERE id > 0 AND categoria LIKE '%Variante%';
UPDATE accion_submenu SET categoria = REPLACE(categoria, 'variantes', 'artículos') WHERE id > 0 AND categoria LIKE '%variantes%';
UPDATE accion_submenu SET categoria = REPLACE(categoria, 'variante', 'artículo') WHERE id > 0 AND categoria LIKE '%variante%';

-- ── Verificación DESPUÉS: las dos tienen que salir VACÍAS ───────────────────
SELECT 'submenu con variante' AS revisar, id, ruta, nombre, descripcion, descripcion_escritura
  FROM submenu
 WHERE nombre = 'Agregar producto'
    OR CONCAT_WS(' ', nombre, descripcion, descripcion_escritura) LIKE '%variante%'
    OR CONCAT_WS(' ', descripcion, descripcion_escritura) LIKE '%Agregar producto%';

SELECT 'accion con variante' AS revisar, a.id, s.ruta, a.clave, a.etiqueta, a.categoria, a.descripcion
  FROM accion_submenu a JOIN submenu s ON s.id = a.submenu_id
 WHERE CONCAT_WS(' ', a.etiqueta, a.descripcion, a.categoria) LIKE '%variante%'
    OR CONCAT_WS(' ', a.etiqueta, a.descripcion) LIKE '%"Productos"%'
    OR CONCAT_WS(' ', a.etiqueta, a.descripcion) LIKE '%sin productos%';

-- Y esta tiene que devolver 1 fila: el menú ya dice "Agregar artículo"
SELECT id, ruta, nombre FROM submenu WHERE ruta = 'tienda/venta';
