-- ============================================================
-- Migración: accion puntual para clientes/buscar -- cierre del audit sistemático de permisos
-- finos (2026-09-08). Última pantalla del catálogo con acciones admin separables.
--
-- "favoritos" es la lista personal del propio cliente (quitar de favoritos, agregar/quitar
-- carrito) -- no es una pantalla admin, sin acciones que separar, queda fuera.
-- ============================================================

INSERT INTO accion_submenu (submenu_id, clave, etiqueta, descripcion, orden)
SELECT s.id, 'verificar-correo', 'Verificar / reiniciar verificación de correo',
       'Botones para marcar el correo de un cliente como verificado o reiniciar su verificación, en Clientes.',
       1
FROM submenu s
WHERE s.ruta = 'clientes/buscar'
  AND NOT EXISTS (SELECT 1 FROM accion_submenu e WHERE e.submenu_id = s.id AND e.clave = 'verificar-correo');

INSERT INTO rol_accion (rol_id, accion_submenu_id)
SELECT rs.rol_id, a.id
FROM rol_submenu rs
JOIN submenu s ON s.id = rs.submenu_id AND s.ruta = 'clientes/buscar'
JOIN accion_submenu a ON a.submenu_id = s.id
WHERE NOT EXISTS (
    SELECT 1 FROM rol_accion e WHERE e.rol_id = rs.rol_id AND e.accion_submenu_id = a.id
  );

-- ============================================================
-- VERIFICACIÓN
-- ============================================================
-- SELECT a.clave, a.etiqueta FROM accion_submenu a JOIN submenu s ON s.id = a.submenu_id WHERE s.ruta = 'clientes/buscar';
