-- ============================================================
-- Migración: fondo de los filtros configurable desde Personalización (2026-10-07)
--
-- Qué hace: da de alta 2 variables en tema_variable, grupo "Formularios":
--   - filtros-panel-bg  Fondo del recuadro de búsqueda y filtros (Tienda → Buscar y
--                       Productos → Buscar)
--   - filtro-bg         Fondo de cada filtro (casillas, fechas y precio)
--
-- Valores: los del diseño Jade (presets-diseno.ts). De día el recuadro queda blanco
-- translúcido, como antes de la homologación de encabezados; de noche no cambia.
-- Talla, Color y Marca son selects: siguen el diseño de todos los selects (input-bg).
--
-- No cambia ninguna fila que ya exista. Idempotente: NOT EXISTS por clave y
-- FROM (SELECT 1) AS dummy, así no depende de ninguna fila ancla.
-- Con el front viejo las filas nuevas no hacen nada.
-- ============================================================

SET NAMES utf8mb4;

INSERT INTO tema_variable (clave, etiqueta, grupo, tipo, valor_claro, valor_oscuro, orden)
SELECT 'filtros-panel-bg', 'Fondo del recuadro de búsqueda y filtros (Tienda y Productos)', 'Formularios', 'color',
       'rgba(255,255,255,0.70)', '#1c1e2c', 4
FROM (SELECT 1) AS dummy
WHERE NOT EXISTS (SELECT 1 FROM tema_variable WHERE clave = 'filtros-panel-bg');

INSERT INTO tema_variable (clave, etiqueta, grupo, tipo, valor_claro, valor_oscuro, orden)
SELECT 'filtro-bg', 'Fondo de cada filtro (casillas, fechas y precio)', 'Formularios', 'color',
       'rgba(45,117,96,0.10)', 'rgba(91,185,154,0.14)', 5
FROM (SELECT 1) AS dummy
WHERE NOT EXISTS (SELECT 1 FROM tema_variable WHERE clave = 'filtro-bg');

-- Verificación: deben salir 2 filas.
-- SELECT clave, etiqueta, grupo, valor_claro, valor_oscuro, orden
-- FROM tema_variable WHERE clave IN ('filtros-panel-bg', 'filtro-bg');
