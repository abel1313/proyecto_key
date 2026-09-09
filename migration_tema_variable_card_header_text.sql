-- ============================================================
-- Migración: agrega card-header-text al catálogo de Personalización.
--
-- El texto del encabezado de las tarjetas estaba fijo en blanco en el código
-- -- si el dueño pone un color de encabezado muy claro, el texto blanco deja
-- de leerse. Se agrega este campo para poder ajustar el color del texto por
-- separado.
--
-- Semilla: blanco, el mismo valor que ya estaba fijo en el código -- la
-- primera carga no cambia nada visualmente.
-- ============================================================

INSERT INTO tema_variable (clave, etiqueta, grupo, tipo, valor_claro, valor_oscuro, orden)
SELECT 'card-header-text', 'Texto del encabezado de las tarjetas', 'Card', 'color', '#FFFFFF', '#FFFFFF', 2
WHERE NOT EXISTS (SELECT 1 FROM tema_variable WHERE clave = 'card-header-text');

UPDATE tema_variable SET orden = 3 WHERE clave = 'card-body-bg';
UPDATE tema_variable SET orden = 4 WHERE clave = 'card-border';
UPDATE tema_variable SET orden = 5 WHERE clave = 'card-footer-bg';
UPDATE tema_variable SET orden = 6 WHERE clave = 'card-radius';
UPDATE tema_variable SET orden = 7 WHERE clave = 'card-shadow';

-- ============================================================
-- VERIFICACIÓN
-- ============================================================
-- SELECT clave, etiqueta, grupo, orden FROM tema_variable WHERE grupo = 'Card' ORDER BY orden;
