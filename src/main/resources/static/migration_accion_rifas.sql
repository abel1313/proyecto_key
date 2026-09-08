-- ============================================================
-- Migración: acciones puntuales para el grupo Rifas (rifas/agregar, rifas/mes, rifas/buscar) --
-- continuación del audit sistemático de permisos finos (2026-09-08).
--
-- Motivo: cada pantalla tiene botones de borrado/ejecución puntuales, hoy abiertos a cualquiera
-- con Ver, mismo caso ya resuelto en Modelos/Categorías/Lugares de entrega (separar "eliminar")
-- y ahora también en "ejecutar"/"recuperar" (acciones discretas y consecuentes, no solo consulta).
--
-- El resto de cada pantalla (configurar la rifa activa, agregar variante/participante manual,
-- importar, copiar de rifa anterior, modo prueba) es un único flujo de "administrar la rifa en
-- curso" -- no se separa, queda bajo el Ver/Editar general.
-- ============================================================

INSERT INTO accion_submenu (submenu_id, clave, etiqueta, descripcion, orden)
SELECT s.id, 'eliminar', 'Eliminar variante de la rifa (✕ en la tarjeta)',
       'Botón ✕ para quitar una variante ya agregada al sorteo, en Rifa de productos.',
       1
FROM submenu s
WHERE s.ruta = 'rifas/agregar'
  AND NOT EXISTS (SELECT 1 FROM accion_submenu e WHERE e.submenu_id = s.id AND e.clave = 'eliminar');

-- "Rifa de productos" (rifas/agregar) también permite dar de alta concursantes (mismo flujo que
-- rifa-mes) -- es un tipo de dato distinto al de la variante de arriba, se separa aparte.
INSERT INTO accion_submenu (submenu_id, clave, etiqueta, descripcion, orden)
SELECT s.id, 'eliminar-concursante', 'Eliminar concursante (✕ en la lista)',
       'Botón ✕ para quitar un concursante ya agregado al sorteo, en Rifa de productos.',
       2
FROM submenu s
WHERE s.ruta = 'rifas/agregar'
  AND NOT EXISTS (SELECT 1 FROM accion_submenu e WHERE e.submenu_id = s.id AND e.clave = 'eliminar-concursante');

INSERT INTO accion_submenu (submenu_id, clave, etiqueta, descripcion, orden)
SELECT s.id, 'eliminar', 'Eliminar concursante (✕ en la lista)',
       'Botón ✕ para quitar un concursante ya agregado al sorteo mensual, en Rifa mensual.',
       1
FROM submenu s
WHERE s.ruta = 'rifas/mes'
  AND NOT EXISTS (SELECT 1 FROM accion_submenu e WHERE e.submenu_id = s.id AND e.clave = 'eliminar');

INSERT INTO accion_submenu (submenu_id, clave, etiqueta, descripcion, orden)
SELECT s.id, 'ejecutar-rifa', 'Ejecutar sorteo (botón en la tarjeta de rifa activa)',
       'Botón que lleva a la pantalla de ejecución/sorteo de una rifa activa, en Ver rifas activas.',
       1
FROM submenu s
WHERE s.ruta = 'rifas/buscar'
  AND NOT EXISTS (SELECT 1 FROM accion_submenu e WHERE e.submenu_id = s.id AND e.clave = 'ejecutar-rifa');

INSERT INTO accion_submenu (submenu_id, clave, etiqueta, descripcion, orden)
SELECT s.id, 'recuperar-rifa', 'Recuperar rifa mensual cerrada (botón en la tarjeta)',
       'Botón "Recuperar" que reabre una rifa mensual ya cerrada, en Ver rifas activas.',
       2
FROM submenu s
WHERE s.ruta = 'rifas/buscar'
  AND NOT EXISTS (SELECT 1 FROM accion_submenu e WHERE e.submenu_id = s.id AND e.clave = 'recuperar-rifa');

-- Se dan a todo rol que hoy tiene Ver en cada pantalla, para preservar el comportamiento actual
-- (eran botones abiertos a cualquiera con acceso a la pantalla).
INSERT INTO rol_accion (rol_id, accion_submenu_id)
SELECT rs.rol_id, a.id
FROM rol_submenu rs
JOIN submenu s ON s.id = rs.submenu_id AND s.ruta IN ('rifas/agregar', 'rifas/mes', 'rifas/buscar')
JOIN accion_submenu a ON a.submenu_id = s.id
WHERE NOT EXISTS (
    SELECT 1 FROM rol_accion e WHERE e.rol_id = rs.rol_id AND e.accion_submenu_id = a.id
  );

-- ============================================================
-- VERIFICACIÓN
-- ============================================================
-- SELECT s.ruta, a.clave, a.etiqueta, a.orden
-- FROM accion_submenu a
-- JOIN submenu s ON s.id = a.submenu_id
-- WHERE s.ruta IN ('rifas/agregar', 'rifas/mes', 'rifas/buscar')
-- ORDER BY s.ruta, a.orden;
