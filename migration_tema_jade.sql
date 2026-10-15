-- ============================================================
-- Migración: diseño "Jade" como diseño de fábrica (2026-10-01)
--
-- Qué hace:
--   0) Respalda tema_variable completa en tema_variable_bkp_20261001 (solo la primera vez:
--      CREATE TABLE IF NOT EXISTS, así una segunda corrida no pisa el respaldo con valores Jade).
--      Con ese respaldo se puede volver al diseño que había antes, fila por fila.
--   1) Da de alta las 32 variables nuevas (letra, tamaños, botones, dorado, cristal,
--      sombras...). Idempotente: cada INSERT lleva NOT EXISTS por clave y FROM (SELECT 1) AS dummy,
--      así no depende de ninguna fila ancla (lección de migration_submenu_ayuda_contextual).
--   2) Pone las 27 variables que ya existían en los valores del diseño Jade.
--      ⚠️ Esto SÍ cambia cómo se ve la app: es el objetivo (Jade queda por default). Volver a
--      correrla después de haber escogido otro diseño en Personalización lo regresa a Jade.
--
-- Valores: generados del mismo catálogo que usa el front (presets-diseno.ts, diseño "jade"),
-- así que coinciden 1:1 con el botón "Usar para día y noche" de Jade en Personalización.
--
-- Correr ANTES o junto con el deploy del front de la rama feature/tema-jade-articulo:
-- con un front viejo las filas nuevas no hacen nada, pero el paso 2 cambia los colores.
-- ============================================================

SET NAMES utf8mb4;

-- 0) Respaldo
CREATE TABLE IF NOT EXISTS tema_variable_bkp_20261001 AS SELECT * FROM tema_variable;

-- 1) Variables nuevas
INSERT INTO tema_variable (clave, etiqueta, grupo, tipo, valor_claro, valor_oscuro, orden)
SELECT 'estilo', 'Estilo de letra, campos y botones (jade o clasico)', 'Estilo y letra', 'seleccion', 'jade', 'jade', 1
FROM (SELECT 1) AS dummy
WHERE NOT EXISTS (SELECT 1 FROM tema_variable WHERE clave = 'estilo');

INSERT INTO tema_variable (clave, etiqueta, grupo, tipo, valor_claro, valor_oscuro, orden)
SELECT 'font-family', 'Tipo de letra', 'Estilo y letra', 'texto', '"Inter", system-ui, sans-serif', '"Inter", system-ui, sans-serif', 2
FROM (SELECT 1) AS dummy
WHERE NOT EXISTS (SELECT 1 FROM tema_variable WHERE clave = 'font-family');

INSERT INTO tema_variable (clave, etiqueta, grupo, tipo, valor_claro, valor_oscuro, orden)
SELECT 'title-weight', 'Grosor de los títulos (500 = sin negrita)', 'Estilo y letra', 'texto', '500', '500', 3
FROM (SELECT 1) AS dummy
WHERE NOT EXISTS (SELECT 1 FROM tema_variable WHERE clave = 'title-weight');

INSERT INTO tema_variable (clave, etiqueta, grupo, tipo, valor_claro, valor_oscuro, orden)
SELECT 'h1-size', 'Tamaño del título de pantalla (px)', 'Estilo y letra', 'numero', '40', '40', 4
FROM (SELECT 1) AS dummy
WHERE NOT EXISTS (SELECT 1 FROM tema_variable WHERE clave = 'h1-size');

INSERT INTO tema_variable (clave, etiqueta, grupo, tipo, valor_claro, valor_oscuro, orden)
SELECT 'h2-size', 'Tamaño del título de sección (px)', 'Estilo y letra', 'numero', '28', '28', 5
FROM (SELECT 1) AS dummy
WHERE NOT EXISTS (SELECT 1 FROM tema_variable WHERE clave = 'h2-size');

INSERT INTO tema_variable (clave, etiqueta, grupo, tipo, valor_claro, valor_oscuro, orden)
SELECT 'h3-size', 'Tamaño del subtítulo (px)', 'Estilo y letra', 'numero', '20', '20', 6
FROM (SELECT 1) AS dummy
WHERE NOT EXISTS (SELECT 1 FROM tema_variable WHERE clave = 'h3-size');

INSERT INTO tema_variable (clave, etiqueta, grupo, tipo, valor_claro, valor_oscuro, orden)
SELECT 'body-size', 'Tamaño del texto normal (px)', 'Estilo y letra', 'numero', '14', '14', 7
FROM (SELECT 1) AS dummy
WHERE NOT EXISTS (SELECT 1 FROM tema_variable WHERE clave = 'body-size');

INSERT INTO tema_variable (clave, etiqueta, grupo, tipo, valor_claro, valor_oscuro, orden)
SELECT 'small-size', 'Tamaño de las notas (px)', 'Estilo y letra', 'numero', '12', '12', 8
FROM (SELECT 1) AS dummy
WHERE NOT EXISTS (SELECT 1 FROM tema_variable WHERE clave = 'small-size');

INSERT INTO tema_variable (clave, etiqueta, grupo, tipo, valor_claro, valor_oscuro, orden)
SELECT 'label-size', 'Tamaño de los labels de los campos (px)', 'Estilo y letra', 'numero', '12', '12', 9
FROM (SELECT 1) AS dummy
WHERE NOT EXISTS (SELECT 1 FROM tema_variable WHERE clave = 'label-size');

INSERT INTO tema_variable (clave, etiqueta, grupo, tipo, valor_claro, valor_oscuro, orden)
SELECT 'input-size', 'Tamaño de la letra dentro de los campos (px)', 'Estilo y letra', 'numero', '15', '15', 10
FROM (SELECT 1) AS dummy
WHERE NOT EXISTS (SELECT 1 FROM tema_variable WHERE clave = 'input-size');

INSERT INTO tema_variable (clave, etiqueta, grupo, tipo, valor_claro, valor_oscuro, orden)
SELECT 'input-height', 'Alto mínimo de los campos (px)', 'Estilo y letra', 'numero', '44', '44', 11
FROM (SELECT 1) AS dummy
WHERE NOT EXISTS (SELECT 1 FROM tema_variable WHERE clave = 'input-height');

INSERT INTO tema_variable (clave, etiqueta, grupo, tipo, valor_claro, valor_oscuro, orden)
SELECT 'app-accent-text', 'Precios, enlaces e ícono activo', 'Marca', 'color', '#2d7560', '#aee6cf', 5
FROM (SELECT 1) AS dummy
WHERE NOT EXISTS (SELECT 1 FROM tema_variable WHERE clave = 'app-accent-text');

INSERT INTO tema_variable (clave, etiqueta, grupo, tipo, valor_claro, valor_oscuro, orden)
SELECT 'app-accent-hover', 'Color de marca al pasar el mouse', 'Marca', 'color', '#2f7f67', '#7fd2b2', 6
FROM (SELECT 1) AS dummy
WHERE NOT EXISTS (SELECT 1 FROM tema_variable WHERE clave = 'app-accent-hover');

INSERT INTO tema_variable (clave, etiqueta, grupo, tipo, valor_claro, valor_oscuro, orden)
SELECT 'app-tint', 'Fondo de lo seleccionado o al pasar el mouse', 'Marca', 'color', 'rgba(45,117,96,0.10)', 'rgba(91,185,154,0.14)', 7
FROM (SELECT 1) AS dummy
WHERE NOT EXISTS (SELECT 1 FROM tema_variable WHERE clave = 'app-tint');

INSERT INTO tema_variable (clave, etiqueta, grupo, tipo, valor_claro, valor_oscuro, orden)
SELECT 'app-gold', 'Dorado: títulos pequeños y detalles', 'Marca', 'color', '#94712a', '#d4b36a', 8
FROM (SELECT 1) AS dummy
WHERE NOT EXISTS (SELECT 1 FROM tema_variable WHERE clave = 'app-gold');

INSERT INTO tema_variable (clave, etiqueta, grupo, tipo, valor_claro, valor_oscuro, orden)
SELECT 'app-gold-line', 'Línea dorada divisoria', 'Marca', 'color', 'rgba(148,113,42,0.50)', 'rgba(212,179,106,0.55)', 9
FROM (SELECT 1) AS dummy
WHERE NOT EXISTS (SELECT 1 FROM tema_variable WHERE clave = 'app-gold-line');

INSERT INTO tema_variable (clave, etiqueta, grupo, tipo, valor_claro, valor_oscuro, orden)
SELECT 'badge-bg', 'Fondo de las etiquetas (ej. "3 unidades")', 'Marca', 'color', '#d5ece2', '#1f5244', 10
FROM (SELECT 1) AS dummy
WHERE NOT EXISTS (SELECT 1 FROM tema_variable WHERE clave = 'badge-bg');

INSERT INTO tema_variable (clave, etiqueta, grupo, tipo, valor_claro, valor_oscuro, orden)
SELECT 'badge-text', 'Texto de las etiquetas', 'Marca', 'color', '#16362e', '#eefaf5', 11
FROM (SELECT 1) AS dummy
WHERE NOT EXISTS (SELECT 1 FROM tema_variable WHERE clave = 'badge-text');

INSERT INTO tema_variable (clave, etiqueta, grupo, tipo, valor_claro, valor_oscuro, orden)
SELECT 'app-bg-fx', 'Degradado del fondo (none = sin degradado)', 'Página', 'texto', 'radial-gradient(120% 60% at 0% 0%, #e3ede7 0%, #f6f3ec 55%)', 'radial-gradient(120% 60% at 0% 0%, #1b2a2a 0%, #161826 55%)', 5
FROM (SELECT 1) AS dummy
WHERE NOT EXISTS (SELECT 1 FROM tema_variable WHERE clave = 'app-bg-fx');

INSERT INTO tema_variable (clave, etiqueta, grupo, tipo, valor_claro, valor_oscuro, orden)
SELECT 'app-text-soft', 'Texto de descripciones', 'Página', 'color', '#4d5160', '#cfd3e5', 6
FROM (SELECT 1) AS dummy
WHERE NOT EXISTS (SELECT 1 FROM tema_variable WHERE clave = 'app-text-soft');

INSERT INTO tema_variable (clave, etiqueta, grupo, tipo, valor_claro, valor_oscuro, orden)
SELECT 'app-text-faint', 'Texto de ayuda e íconos apagados', 'Página', 'color', '#6f7384', '#9397ab', 7
FROM (SELECT 1) AS dummy
WHERE NOT EXISTS (SELECT 1 FROM tema_variable WHERE clave = 'app-text-faint');

INSERT INTO tema_variable (clave, etiqueta, grupo, tipo, valor_claro, valor_oscuro, orden)
SELECT 'app-hairline', 'Línea fina entre filas', 'Página', 'color', 'rgba(28,42,38,0.10)', 'rgba(233,233,237,0.10)', 8
FROM (SELECT 1) AS dummy
WHERE NOT EXISTS (SELECT 1 FROM tema_variable WHERE clave = 'app-hairline');

INSERT INTO tema_variable (clave, etiqueta, grupo, tipo, valor_claro, valor_oscuro, orden)
SELECT 'app-glass', 'Cristal de barras flotantes', 'Página', 'color', 'rgba(246,243,236,0.72)', 'rgba(22,24,38,0.60)', 9
FROM (SELECT 1) AS dummy
WHERE NOT EXISTS (SELECT 1 FROM tema_variable WHERE clave = 'app-glass');

INSERT INTO tema_variable (clave, etiqueta, grupo, tipo, valor_claro, valor_oscuro, orden)
SELECT 'app-glass-strong', 'Cristal fuerte (encabezados, barra inferior)', 'Página', 'color', 'rgba(246,243,236,0.90)', 'rgba(22,24,38,0.82)', 10
FROM (SELECT 1) AS dummy
WHERE NOT EXISTS (SELECT 1 FROM tema_variable WHERE clave = 'app-glass-strong');

INSERT INTO tema_variable (clave, etiqueta, grupo, tipo, valor_claro, valor_oscuro, orden)
SELECT 'form-card-radius', 'Redondeo de la tarjeta de los formularios (px)', 'Card', 'numero', '14', '14', 8
FROM (SELECT 1) AS dummy
WHERE NOT EXISTS (SELECT 1 FROM tema_variable WHERE clave = 'form-card-radius');

INSERT INTO tema_variable (clave, etiqueta, grupo, tipo, valor_claro, valor_oscuro, orden)
SELECT 'shadow-md', 'Sombra media (al pasar el mouse, menús)', 'Card', 'texto', '0 0 0 1px #d6cfc0, 0 6px 18px rgba(60,50,30,0.12)', '0 0 0 1px #595d6c, 0 6px 18px rgba(0,0,0,0.55)', 9
FROM (SELECT 1) AS dummy
WHERE NOT EXISTS (SELECT 1 FROM tema_variable WHERE clave = 'shadow-md');

INSERT INTO tema_variable (clave, etiqueta, grupo, tipo, valor_claro, valor_oscuro, orden)
SELECT 'shadow-lg', 'Sombra fuerte (ventanas y avisos)', 'Card', 'texto', '0 0 0 1px #d6cfc0, 0 16px 40px rgba(60,50,30,0.20)', '0 0 0 1px #9397ab, 0 16px 40px rgba(0,0,0,0.65)', 10
FROM (SELECT 1) AS dummy
WHERE NOT EXISTS (SELECT 1 FROM tema_variable WHERE clave = 'shadow-lg');

INSERT INTO tema_variable (clave, etiqueta, grupo, tipo, valor_claro, valor_oscuro, orden)
SELECT 'btn-primary-bg', 'Botón principal: fondo', 'Botones', 'texto', 'transparent', 'transparent', 1
FROM (SELECT 1) AS dummy
WHERE NOT EXISTS (SELECT 1 FROM tema_variable WHERE clave = 'btn-primary-bg');

INSERT INTO tema_variable (clave, etiqueta, grupo, tipo, valor_claro, valor_oscuro, orden)
SELECT 'btn-primary-text', 'Botón principal: texto', 'Botones', 'texto', 'var(--app-accent-text)', 'var(--app-accent-text)', 2
FROM (SELECT 1) AS dummy
WHERE NOT EXISTS (SELECT 1 FROM tema_variable WHERE clave = 'btn-primary-text');

INSERT INTO tema_variable (clave, etiqueta, grupo, tipo, valor_claro, valor_oscuro, orden)
SELECT 'btn-primary-border', 'Botón principal: borde', 'Botones', 'texto', 'var(--app-accent)', 'var(--app-accent)', 3
FROM (SELECT 1) AS dummy
WHERE NOT EXISTS (SELECT 1 FROM tema_variable WHERE clave = 'btn-primary-border');

INSERT INTO tema_variable (clave, etiqueta, grupo, tipo, valor_claro, valor_oscuro, orden)
SELECT 'btn-primary-hover-bg', 'Botón principal: fondo al pasar el mouse', 'Botones', 'texto', 'var(--app-tint)', 'var(--app-tint)', 4
FROM (SELECT 1) AS dummy
WHERE NOT EXISTS (SELECT 1 FROM tema_variable WHERE clave = 'btn-primary-hover-bg');

INSERT INTO tema_variable (clave, etiqueta, grupo, tipo, valor_claro, valor_oscuro, orden)
SELECT 'btn-primary-hover-text', 'Botón principal: texto al pasar el mouse', 'Botones', 'texto', 'var(--app-accent-hover)', 'var(--app-accent-hover)', 5
FROM (SELECT 1) AS dummy
WHERE NOT EXISTS (SELECT 1 FROM tema_variable WHERE clave = 'btn-primary-hover-text');

-- 2) Variables que ya existían -> valores Jade
UPDATE tema_variable SET valor_claro = '#2d7560', valor_oscuro = '#5bb99a' WHERE clave = 'brand-1';
UPDATE tema_variable SET valor_claro = '#2f7f67', valor_oscuro = '#7fd2b2' WHERE clave = 'brand-2';
UPDATE tema_variable SET valor_claro = '#1f5244', valor_oscuro = '#1f5244' WHERE clave = 'brand-3';
UPDATE tema_variable SET valor_claro = '#f6f3ec', valor_oscuro = '#161826' WHERE clave = 'app-accent-ink';
UPDATE tema_variable SET valor_claro = '#f6f3ec', valor_oscuro = '#161826' WHERE clave = 'app-bg';
UPDATE tema_variable SET valor_claro = '#1c2a26', valor_oscuro = '#e9e9ed' WHERE clave = 'app-text';
UPDATE tema_variable SET valor_claro = '#5f6373', valor_oscuro = '#b2b6ca' WHERE clave = 'app-text-muted';
UPDATE tema_variable SET valor_claro = 'rgba(28,42,38,0.16)', valor_oscuro = 'rgba(233,233,237,0.16)' WHERE clave = 'app-border';
UPDATE tema_variable SET valor_claro = '#ebe6da', valor_oscuro = '#232532' WHERE clave = 'card-header-bg';
UPDATE tema_variable SET valor_claro = '#1c2a26', valor_oscuro = '#e9e9ed' WHERE clave = 'card-header-text';
UPDATE tema_variable SET valor_claro = '#ebe6da', valor_oscuro = '#232532' WHERE clave = 'card-body-bg';
UPDATE tema_variable SET valor_claro = '#ebe6da', valor_oscuro = '#232532' WHERE clave = 'card-footer-bg';
UPDATE tema_variable SET valor_claro = 'rgba(28,42,38,0.16)', valor_oscuro = 'rgba(233,233,237,0.16)' WHERE clave = 'card-border';
UPDATE tema_variable SET valor_claro = '8', valor_oscuro = '8' WHERE clave = 'card-radius';
UPDATE tema_variable SET valor_claro = 'linea', valor_oscuro = 'linea' WHERE clave = 'card-shadow';
UPDATE tema_variable SET valor_claro = 'transparent', valor_oscuro = 'transparent' WHERE clave = 'table-header-bg';
UPDATE tema_variable SET valor_claro = '#6f7384', valor_oscuro = '#9397ab' WHERE clave = 'table-header-text';
UPDATE tema_variable SET valor_claro = 'rgba(45,117,96,0.10)', valor_oscuro = 'rgba(91,185,154,0.14)' WHERE clave = 'table-row-hover';
UPDATE tema_variable SET valor_claro = 'rgba(28,42,38,0.10)', valor_oscuro = 'rgba(233,233,237,0.10)' WHERE clave = 'table-border';
UPDATE tema_variable SET valor_claro = '#ebe6da', valor_oscuro = '#232532' WHERE clave = 'sb-header-bg';
UPDATE tema_variable SET valor_claro = '#ebe6da', valor_oscuro = '#232532' WHERE clave = 'sb-body-bg';
UPDATE tema_variable SET valor_claro = '#ebe6da', valor_oscuro = '#232532' WHERE clave = 'sb-footer-bg';
UPDATE tema_variable SET valor_claro = '#1c2a26', valor_oscuro = '#e9e9ed' WHERE clave = 'sb-text';
UPDATE tema_variable SET valor_claro = 'rgba(28,42,38,0.10)', valor_oscuro = 'rgba(233,233,237,0.10)' WHERE clave = 'sb-border';
UPDATE tema_variable SET valor_claro = '#f6f3ec', valor_oscuro = '#1c1e2c' WHERE clave = 'form-section-bg';
UPDATE tema_variable SET valor_claro = '#ebe6da', valor_oscuro = '#232532' WHERE clave = 'input-bg';
UPDATE tema_variable SET valor_claro = '#6f7384', valor_oscuro = '#9397ab' WHERE clave = 'input-placeholder';

-- ============================================================
-- VERIFICACIÓN (las tres deben dar lo indicado)
-- ============================================================
-- a) Las 59 claves del diseño existen -> debe dar 59
SELECT COUNT(*) AS claves_jade FROM tema_variable WHERE clave IN ('estilo', 'font-family', 'title-weight', 'h1-size', 'h2-size', 'h3-size', 'body-size', 'small-size', 'label-size', 'input-size', 'input-height', 'app-accent-text', 'app-accent-hover', 'app-tint', 'app-gold', 'app-gold-line', 'badge-bg', 'badge-text', 'app-bg-fx', 'app-text-soft', 'app-text-faint', 'app-hairline', 'app-glass', 'app-glass-strong', 'form-card-radius', 'shadow-md', 'shadow-lg', 'btn-primary-bg', 'btn-primary-text', 'btn-primary-border', 'btn-primary-hover-bg', 'btn-primary-hover-text', 'brand-1', 'brand-2', 'brand-3', 'app-accent-ink', 'app-bg', 'app-text', 'app-text-muted', 'app-border', 'card-header-bg', 'card-header-text', 'card-body-bg', 'card-footer-bg', 'card-border', 'card-radius', 'card-shadow', 'table-header-bg', 'table-header-text', 'table-row-hover', 'table-border', 'sb-header-bg', 'sb-body-bg', 'sb-footer-bg', 'sb-text', 'sb-border', 'form-section-bg', 'input-bg', 'input-placeholder');

-- b) El estilo quedó en jade -> debe dar 1 fila: estilo | jade | jade
SELECT clave, valor_claro, valor_oscuro FROM tema_variable WHERE clave = 'estilo';

-- c) El respaldo existe y tiene las filas de antes -> debe dar más de 0
SELECT COUNT(*) AS filas_respaldo FROM tema_variable_bkp_20261001;

-- ============================================================
-- PARA VOLVER AL DISEÑO DE ANTES (solo si se pide; no correr junto con lo de arriba)
-- ============================================================
-- Probado 2026-10-01 en MySQL 8 local con --safe-updates: deja las 39 filas de antes idénticas
-- (misma huella MD5 que antes de la migración).
-- SET @su = @@SQL_SAFE_UPDATES; SET SQL_SAFE_UPDATES = 0;   -- el UPDATE con JOIN da error 1175 sin esto
-- UPDATE tema_variable t JOIN tema_variable_bkp_20261001 b ON b.clave = t.clave
--    SET t.valor_claro = b.valor_claro, t.valor_oscuro = b.valor_oscuro;
-- UPDATE tema_variable SET valor_claro = 'clasico', valor_oscuro = 'clasico' WHERE clave = 'estilo';
-- SET SQL_SAFE_UPDATES = @su;
-- (las filas nuevas pueden quedarse: con estilo = clasico la letra y los botones son los de antes)
