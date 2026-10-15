-- ============================================================
-- Migración: fondo detrás de las ventanas (modales) configurable desde Personalización (2026-10-08)
--
-- Qué hace: da de alta 1 variable en tema_variable, grupo "Página":
--   - modal-backdrop  Velo oscuro detrás de una ventana abierta (por ahora, la ventana
--                     "🧩 Agregar artículos" de Agregar modelo y de 🧩 Productos)
--
-- Valores: los mismos que styles.scss, así que correrla no cambia nada en pantalla.
--
-- No cambia ninguna fila que ya exista. Idempotente: NOT EXISTS por clave y
-- FROM (SELECT 1) AS dummy, así no depende de ninguna fila ancla.
-- Sin correrla, el front usa el valor de styles.scss (no se rompe nada).
-- ============================================================

SET NAMES utf8mb4;

INSERT INTO tema_variable (clave, etiqueta, grupo, tipo, valor_claro, valor_oscuro, orden)
SELECT 'modal-backdrop', 'Velo detrás de una ventana abierta', 'Página', 'color',
       'rgba(22,24,38,0.55)', 'rgba(0,0,0,0.65)', 20
FROM (SELECT 1) AS dummy
WHERE NOT EXISTS (SELECT 1 FROM tema_variable WHERE clave = 'modal-backdrop');

-- Verificación: debe salir 1 fila.
-- SELECT clave, etiqueta, grupo, valor_claro, valor_oscuro, orden
-- FROM tema_variable WHERE clave = 'modal-backdrop';
