-- ============================================================
-- Migración: descripción propia para el checkbox "Editar" de Tienda (tienda/buscar)
-- (sigue a migration_submenu_descripcion_escritura.sql)
--
-- Motivo: bug reportado por el usuario con capturas (2026-09-08) -- el checkbox "✏️ Editar" de
-- Tienda en Gestión de roles estaba marcado para un rol (ROLE_USUARIO) pero el botón ✏️ Editar
-- de la tarjeta de variante en esa misma pantalla nunca aparecía para ese rol, solo para
-- ROLE_ADMIN. Causa: el botón (buscar.component.ts, editarVariante() -> POST
-- /tienda/v1/guardarConImagenes) leía el permiso de escritura de OTRA pantalla
-- ("tienda/venta"), y el backend (SecurityConfig./tienda/**) tampoco aceptaba el permiso de
-- "tienda/buscar" para ese endpoint -- así que ni marcando el checkbox correcto había forma de
-- que el botón funcionara. Fix: front ahora lee tieneEscritura('tienda/buscar') y el back acepta
-- ese mismo permiso en pantallaEscribir(...) del /tienda/**. Esta descripción deja explícito
-- DÓNDE aparece el efecto del checkbox, que es lo que faltaba (el texto anterior no lo decía).
-- ============================================================

UPDATE submenu SET descripcion_escritura =
    'Controla el botón ✏️ Editar que aparece en cada tarjeta de producto/variante, en la pantalla '
    'de Tienda (Buscar). Sin este permiso, el usuario puede ver y buscar variantes pero no le '
    'aparece el botón para editarlas.'
WHERE ruta = 'tienda/buscar';

-- ============================================================
-- VERIFICACIÓN
-- ============================================================
-- SELECT ruta, nombre, descripcion_escritura FROM submenu WHERE ruta = 'tienda/buscar';
