-- ============================================================
-- Migración: acciones puntuales para el grupo Flores eternas (catalogos, entregas, ramos-admin,
-- frases) -- continuación del audit sistemático de permisos finos (2026-09-08).
--
-- "flores/ramos" (Vitrina) y "flores/configurar" (Arma tu ramo) son públicas, de cara al
-- cliente (ver ramo, pedir, WhatsApp) -- no tienen ninguna acción admin, se excluyen del audit.
-- ============================================================

-- flores/catalogos: 5 pestañas (tipos/colores/cantidades/accesorios/frases de listón), todas con
-- el mismo patrón agregar/editar/activar-desactivar/eliminar -- se separan "habilitar" y
-- "eliminar" para todo el catálogo (una acción cubre las 5 pestañas, mismo criterio que
-- Categorías/Modelos), en vez de una acción por pestaña -- son el mismo tipo de operación
-- repetida, no 5 funcionalidades distintas.
INSERT INTO accion_submenu (submenu_id, clave, etiqueta, descripcion, orden)
SELECT s.id, 'habilitar', 'Activar / desactivar (👁️🚫 en cada fila)',
       'Ícono para activar/desactivar un tipo de flor, color, cantidad, accesorio o frase de listón, en cualquiera de las 5 pestañas de Catálogos.',
       1
FROM submenu s
WHERE s.ruta = 'flores/catalogos'
  AND NOT EXISTS (SELECT 1 FROM accion_submenu e WHERE e.submenu_id = s.id AND e.clave = 'habilitar');

INSERT INTO accion_submenu (submenu_id, clave, etiqueta, descripcion, orden)
SELECT s.id, 'eliminar', 'Eliminar (🗑️ en cada fila)',
       'Ícono para eliminar un tipo de flor, color, cantidad, accesorio o frase de listón, en cualquiera de las 5 pestañas de Catálogos.',
       2
FROM submenu s
WHERE s.ruta = 'flores/catalogos'
  AND NOT EXISTS (SELECT 1 FROM accion_submenu e WHERE e.submenu_id = s.id AND e.clave = 'eliminar');

-- flores/entregas: "Quitar plazos" reinicia la configuración de una zona (destructivo).
INSERT INTO accion_submenu (submenu_id, clave, etiqueta, descripcion, orden)
SELECT s.id, 'eliminar', 'Quitar plazos (🗑️ en la fila de la zona)',
       'Botón 🗑️ que borra los plazos de entrega configurados para una zona, en Entregas.',
       1
FROM submenu s
WHERE s.ruta = 'flores/entregas'
  AND NOT EXISTS (SELECT 1 FROM accion_submenu e WHERE e.submenu_id = s.id AND e.clave = 'eliminar');

-- flores/ramos-admin: solo tiene activar/desactivar como acción puntual (no hay botón de borrar).
INSERT INTO accion_submenu (submenu_id, clave, etiqueta, descripcion, orden)
SELECT s.id, 'habilitar', 'Activar / desactivar ramo armado',
       'Botón que activa/desactiva un ramo ya armado para que deje de/vuelva a aparecer en la Vitrina, en Administrar ramos armados.',
       1
FROM submenu s
WHERE s.ruta = 'flores/ramos-admin'
  AND NOT EXISTS (SELECT 1 FROM accion_submenu e WHERE e.submenu_id = s.id AND e.clave = 'habilitar');

-- flores/frases: bandeja de moderación -- aprobar/rechazar son 2 decisiones distintas, con
-- sentido de negocio separarlas (ej. un rol que solo revisa y rechaza spam, sin poder aprobar).
INSERT INTO accion_submenu (submenu_id, clave, etiqueta, descripcion, orden)
SELECT s.id, 'aprobar', 'Aprobar frase (botón verde)',
       'Botón para aprobar una frase de listón pendiente, en Frases por aprobar.',
       1
FROM submenu s
WHERE s.ruta = 'flores/frases'
  AND NOT EXISTS (SELECT 1 FROM accion_submenu e WHERE e.submenu_id = s.id AND e.clave = 'aprobar');

INSERT INTO accion_submenu (submenu_id, clave, etiqueta, descripcion, orden)
SELECT s.id, 'rechazar', 'Rechazar frase (✕ Rechazar)',
       'Botón para rechazar una frase de listón pendiente, en Frases por aprobar.',
       2
FROM submenu s
WHERE s.ruta = 'flores/frases'
  AND NOT EXISTS (SELECT 1 FROM accion_submenu e WHERE e.submenu_id = s.id AND e.clave = 'rechazar');

-- Se dan a todo rol que hoy tiene Ver en cada pantalla, para preservar el comportamiento actual.
INSERT INTO rol_accion (rol_id, accion_submenu_id)
SELECT rs.rol_id, a.id
FROM rol_submenu rs
JOIN submenu s ON s.id = rs.submenu_id
  AND s.ruta IN ('flores/catalogos', 'flores/entregas', 'flores/ramos-admin', 'flores/frases')
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
-- WHERE s.ruta IN ('flores/catalogos', 'flores/entregas', 'flores/ramos-admin', 'flores/frases')
-- ORDER BY s.ruta, a.orden;
