-- ============================================================
-- Renombre variante → artículo: los TEXTOS de la base (H4 y H6 de RENOMBRE_VARIANTE_A_ARTICULO.md)
-- Fecha: 2026-10-08 · Rama: rename/variante-a-articulo
--
-- Qué cambia (solo lo que el admin LEE):
--   * Menú Catálogo: "🧩 Agregar producto" → "🧩 Agregar artículo" (submenu.nombre de tienda/venta).
--   * Menú Rifas: "🎡 Rifa de productos" → "🎡 Rifa de artículos" (rifas/agregar: sus premios son artículos).
--   * Sistema → 🛡️ Gestión de roles: etiquetas, explicaciones (ℹ️) y categorías de los permisos que
--     decían "variante" ("Tarjeta de variante", "Habilitar / deshabilitar variante",
--     "Crear productos desde el modelo (🧩 "Productos"…)", "Excel sin productos"…).
--
-- Qué NO cambia: claves de las acciones ('crear-variantes'…), rutas, permisos de ningún rol.
-- Nadie gana ni pierde acceso; no hace falta volver a entrar, basta recargar la pantalla.
--
-- Columnas tocadas: tema_variable.etiqueta (solo la fila 'filtros-panel-bg'),
-- submenu.nombre / descripcion / descripcion_escritura y
-- accion_submenu.etiqueta / descripcion / categoria. Los reemplazos palabra por palabra no alargan
-- el texto ("variante" y "artículo" miden lo mismo); los textos fijos de la sección 1 que sí son más
-- largos quedan por debajo de 255 caracteres (el largo de la columna).
-- También corrige 4 explicaciones (ℹ️) que decían "producto" por artículo (Diagnóstico de imágenes,
-- Categorías, Reportes de ventas, Publicar en redes) y la del permiso 💲 de Tienda, que seguía
-- describiendo el precio por modelo de antes del 2026-09-29.
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

-- Menú Rifas → la pantalla rifas/agregar
UPDATE submenu SET nombre = 'Rifa de artículos'
 WHERE id > 0 AND ruta = 'rifas/agregar' AND nombre = 'Rifa de productos';

UPDATE submenu
   SET descripcion = 'Crear una rifa nueva y elegir qué artículos participan. Vive en el menú: Rifas → Rifa de artículos.'
 WHERE id > 0 AND ruta = 'rifas/agregar'
   AND descripcion = 'Crear una rifa nueva y elegir qué productos participan. Vive en el menú: Rifas → Rifa de productos.';

-- Explicaciones (ℹ️) de otras pantallas que trabajan con artículos (2026-10-08, segunda pasada)
UPDATE submenu
   SET descripcion = 'Herramienta para revisar por qué no aparece la imagen de un modelo o de un artículo. Vive en el menú: Sistema → Diagnóstico de imágenes.'
 WHERE id > 0 AND ruta = 'admin/diagnostico-imagenes'
   AND descripcion = 'Herramienta para revisar por qué no aparece la imagen de un producto/variante. Vive en el menú: Sistema → Diagnóstico de imágenes.';

UPDATE submenu
   SET descripcion = 'Catálogo de categorías/palabras clave usadas para clasificar y buscar modelos y artículos. Vive en el menú: Catálogo → Categorías.'
 WHERE id > 0 AND ruta = 'palabras-clave'
   AND descripcion = 'Catálogo de categorías/palabras clave usadas para clasificar y buscar productos. Vive en el menú: Catálogo → Categorías.';

UPDATE submenu
   SET descripcion = 'Reportes detallados de ventas, por fecha/artículo/vendedor. Vive en el menú: Reportes → Reportes de ventas.'
 WHERE id > 0 AND ruta = 'reportes'
   AND descripcion = 'Reportes detallados de ventas, por fecha/producto/vendedor. Vive en el menú: Reportes → Reportes de ventas.';

UPDATE submenu
   SET descripcion = 'Publicar artículos/promociones directo en Facebook/Instagram desde el sistema. Vive en el menú: Marketing → Publicar en redes.'
 WHERE id > 0 AND ruta = 'admin/facebook'
   AND descripcion = 'Publicar productos/promociones directo en Facebook/Instagram desde el sistema. Vive en el menú: Marketing → Publicar en redes.';

-- Tienda → Buscar, permiso 💲: desde el 2026-09-29 cambia el precio de UN artículo (ya no el de
-- todos los artículos del modelo); la explicación decía lo de antes. Sigue siendo el mismo permiso.
UPDATE accion_submenu a JOIN submenu s ON s.id = a.submenu_id
   SET a.descripcion = 'Botón 💲 de la tarjeta de Tienda. Cambia el precio normal y con descuento de ese artículo (solo de ese). También deja ver el precio con descuento (👁) en el carrito y en el detalle del pedido. Los pedidos ya hechos conservan su precio.'
 WHERE a.id > 0 AND s.ruta = 'tienda/buscar' AND a.clave = 'cambiar-precio'
   AND a.descripcion = 'Botón 💲 de la tarjeta de Tienda. Cambia el precio normal y el precio con descuento del producto, para todos sus artículos. Los pedidos ya hechos conservan su precio.';

-- Personalización: el color del recuadro de filtros nombraba la pantalla de Modelos como "Productos"
UPDATE tema_variable SET etiqueta = 'Fondo del recuadro de búsqueda y filtros (Tienda y Modelos)'
 WHERE id > 0 AND clave = 'filtros-panel-bg'
   AND etiqueta = 'Fondo del recuadro de búsqueda y filtros (Tienda y Productos)';

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
UPDATE submenu SET nombre = REPLACE(nombre, 'Rifa de productos', 'Rifa de artículos') WHERE id > 0 AND nombre LIKE '%Rifa de productos%';
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
UPDATE submenu SET descripcion = REPLACE(descripcion, 'Rifa de productos', 'Rifa de artículos') WHERE id > 0 AND descripcion LIKE '%Rifa de productos%';
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
UPDATE submenu SET descripcion_escritura = REPLACE(descripcion_escritura, 'Rifa de productos', 'Rifa de artículos') WHERE id > 0 AND descripcion_escritura LIKE '%Rifa de productos%';
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
UPDATE accion_submenu SET etiqueta = REPLACE(etiqueta, 'Rifa de productos', 'Rifa de artículos') WHERE id > 0 AND etiqueta LIKE '%Rifa de productos%';
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
UPDATE accion_submenu SET descripcion = REPLACE(descripcion, 'Rifa de productos', 'Rifa de artículos') WHERE id > 0 AND descripcion LIKE '%Rifa de productos%';
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
UPDATE accion_submenu SET categoria = REPLACE(categoria, 'Rifa de productos', 'Rifa de artículos') WHERE id > 0 AND categoria LIKE '%Rifa de productos%';
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

-- ── Verificación DESPUÉS: las tres primeras tienen que salir VACÍAS ─────────
SELECT 'submenu con variante' AS revisar, id, ruta, nombre, descripcion, descripcion_escritura
  FROM submenu
 WHERE nombre IN ('Agregar producto', 'Rifa de productos')
    OR CONCAT_WS(' ', descripcion, descripcion_escritura) LIKE '%Rifa de productos%'
    OR descripcion LIKE '%buscar productos.%'
    OR descripcion LIKE '%fecha/producto/%'
    OR descripcion LIKE '%Publicar productos/%'
    OR CONCAT_WS(' ', nombre, descripcion, descripcion_escritura) LIKE '%variante%'
    OR CONCAT_WS(' ', descripcion, descripcion_escritura) LIKE '%Agregar producto%';

SELECT 'accion con variante' AS revisar, a.id, s.ruta, a.clave, a.etiqueta, a.categoria, a.descripcion
  FROM accion_submenu a JOIN submenu s ON s.id = a.submenu_id
 WHERE CONCAT_WS(' ', a.etiqueta, a.descripcion, a.categoria) LIKE '%variante%'
    OR CONCAT_WS(' ', a.etiqueta, a.descripcion) LIKE '%"Productos"%'
    OR CONCAT_WS(' ', a.etiqueta, a.descripcion) LIKE '%sin productos%'
    OR (a.clave = 'cambiar-precio' AND a.descripcion LIKE '%para todos sus artículos%');

-- Esta también tiene que salir VACÍA
SELECT 'color con Productos' AS revisar, clave, etiqueta FROM tema_variable
 WHERE clave = 'filtros-panel-bg' AND etiqueta LIKE '%(Tienda y Productos)%';

-- Y esta tiene que devolver 2 filas: "Agregar artículo" y "Rifa de artículos"
SELECT id, ruta, nombre FROM submenu WHERE ruta IN ('tienda/venta', 'rifas/agregar');
