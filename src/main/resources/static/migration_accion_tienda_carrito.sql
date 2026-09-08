-- ============================================================
-- Migración: acciones puntuales para los botones de carrito en "tienda/buscar"
-- (Agregar / Quitar / Ver carrito)
--
-- Motivo: pedido explícito del dueño -- TODO lo que tiene la pantalla debe tener su propio
-- permiso separado, sin excepciones. Estos eran los últimos 3 elementos de la pantalla sin
-- ningún permiso: el ícono 🛒 del encabezado y los 3 botones de carrito de cada tarjeta
-- (➕ Agregar, ➖ Quitar, 🛒 Carrito) se mostraban siempre, a cualquiera.
--
-- Igual que con el escáner y los filtros públicos (migration_accion_tienda_escanear.sql,
-- migration_accion_tienda_filtros_publicos.sql): un visitante SIN sesión sigue viendo estos
-- botones siempre -- son la función central de compra de la tienda. La acción solo aplica a
-- cuentas CON sesión (cualquier rol, incluido ROLE_ADMIN), dada por defecto solo a ROLE_ADMIN
-- para preservar el comportamiento actual.
--
-- "Ver carrito" cubre tanto el ícono 🛒 del encabezado como el botón "Carrito" de la tarjeta --
-- es la misma acción (verCarrito()) en dos lugares de la pantalla.
--
-- Renumera orden para mantener cada categoría en un bloque contiguo (el front agrupa así, no
-- reordena por su cuenta):
--   Filtros (admin):      1-9   (sin cambios)
--   Filtros públicos:     10-13 (sin cambios)
--   Tarjeta de variante:  14-18 (agregar-carrito, quitar-carrito, ver-carrito, habilitar,
--                                 compartir-imagen -- antes 14-15 solo habilitar/compartir-imagen)
--   Buscador:             19    (antes 16 -- escanear-codigo)
-- ============================================================

INSERT INTO accion_submenu (submenu_id, clave, etiqueta, descripcion, categoria, orden)
SELECT s.id, v.clave, v.etiqueta, v.descripcion, 'Tarjeta de variante', v.orden
FROM submenu s
CROSS JOIN (
    SELECT 'agregar-carrito' AS clave, 'Agregar al carrito (➕)' AS etiqueta,
           'Botón "Agregar" en la tarjeta de cada variante, en Tienda.' AS descripcion, 14 AS orden UNION ALL
    SELECT 'quitar-carrito',     'Quitar del carrito (➖)',
           'Botón "Quitar" en la tarjeta de cada variante, en Tienda.', 15 UNION ALL
    SELECT 'ver-carrito',        'Ver carrito (🛒)',
           'Ícono 🛒 del encabezado y botón "Carrito" en la tarjeta de cada variante, en Tienda.', 16
) v
WHERE s.ruta = 'tienda/buscar'
  AND NOT EXISTS (
    SELECT 1 FROM accion_submenu existente WHERE existente.submenu_id = s.id AND existente.clave = v.clave
  );

-- Recorre "habilitar" / "compartir-imagen" / "escanear-codigo" para dejar espacio a los 3 nuevos
-- (14-16).
UPDATE accion_submenu a
JOIN submenu s ON s.id = a.submenu_id
SET a.orden = CASE a.clave
    WHEN 'habilitar'        THEN 17
    WHEN 'compartir-imagen' THEN 18
    WHEN 'escanear-codigo'  THEN 19
    END
WHERE s.ruta = 'tienda/buscar' AND a.clave IN ('habilitar', 'compartir-imagen', 'escanear-codigo');

-- Preserva el comportamiento actual: se le dan las 3 acciones nuevas a ROLE_ADMIN.
INSERT INTO rol_accion (rol_id, accion_submenu_id)
SELECT r.id, a.id
FROM roles r
CROSS JOIN accion_submenu a
JOIN submenu s ON s.id = a.submenu_id AND s.ruta = 'tienda/buscar'
WHERE r.nombre_rol = 'ROLE_ADMIN'
  AND a.clave IN ('agregar-carrito', 'quitar-carrito', 'ver-carrito')
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
