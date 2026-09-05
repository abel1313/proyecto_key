-- ============================================================
-- Migración: descripción propia para el checkbox "Editar" en Gestión de roles
-- (sigue a migration_accion_submenu_categoria.sql)
--
-- Motivo: a diferencia de "Ver" (columna `descripcion`, ya existente) y de cada acción puntual
-- (accion_submenu.descripcion), el checkbox "✏️ Editar" no tenía botón ℹ️ propio -- caso real
-- que lo disparó: el usuario marcó "Editar" en Modelos y nunca vio que hiciera nada ahí, porque
-- Modelos es solo buscar/listar (sin formulario propio) -- el permiso de escritura de esa
-- pantalla en realidad está COMPARTIDO por OR con Agregar modelo y Agregar producto
-- (SecurityConfig.pantallaEscribir("productos/buscar", "productos/agregar", "tienda/venta")),
-- algo que no se puede adivinar mirando solo la pantalla de Modelos.
-- ============================================================

ALTER TABLE submenu ADD COLUMN descripcion_escritura VARCHAR(255) NULL;

UPDATE submenu SET descripcion_escritura =
    'Permiso de escritura COMPARTIDO entre "Modelos", "Agregar modelo" y "Agregar producto" -- '
    'basta con tenerlo marcado en CUALQUIERA de las 3 para poder crear/editar/borrar en las 3. '
    'Modelos en sí es solo buscar/listar (no tiene formulario propio), así que marcarlo AQUÍ no '
    'habilita nada visible en esta pantalla -- el efecto se nota al entrar a Agregar modelo o '
    'Agregar producto. También habilita subir fotos a variantes desde Flores/Catálogos y Ramos armados.'
WHERE ruta = 'productos/buscar';

UPDATE submenu SET descripcion_escritura =
    'Permiso de escritura COMPARTIDO entre "Modelos", "Agregar modelo" y "Agregar producto" -- '
    'basta con tenerlo marcado en CUALQUIERA de las 3 para poder crear/editar/borrar en las 3.'
WHERE ruta = 'productos/agregar';

UPDATE submenu SET descripcion_escritura =
    'Permiso de escritura COMPARTIDO entre "Modelos", "Agregar modelo" y "Agregar producto" -- '
    'basta con tenerlo marcado en CUALQUIERA de las 3 para poder crear/editar/borrar en las 3.'
WHERE ruta = 'tienda/venta';

UPDATE submenu SET descripcion_escritura =
    'Permiso de escritura COMPARTIDO entre las 3 pantallas de Rifas ("Rifa de productos", "Rifa '
    'mensual", "Ver rifas activas") -- basta con tenerlo marcado en CUALQUIERA de las 3.'
WHERE ruta IN ('rifas/agregar', 'rifas/mes', 'rifas/buscar');

UPDATE submenu SET descripcion_escritura =
    'Permiso de escritura COMPARTIDO entre "Publicar en Facebook" y "Hashtags" -- basta con '
    'tenerlo marcado en CUALQUIERA de las 2.'
WHERE ruta IN ('admin/facebook', 'admin/hashtags');

-- ============================================================
-- VERIFICACIÓN
-- ============================================================
-- SELECT ruta, nombre, descripcion_escritura FROM submenu WHERE descripcion_escritura IS NOT NULL;
