-- ============================================================
-- Migración: re-agrega card-header-bg y card-footer-bg al catálogo de
-- Personalización, esta vez conectadas de verdad a las cards reales.
--
-- Corrige la migración anterior (migration_tema_variable_limpiar_card_header_footer.sql):
-- ahí se borraron porque no controlaban ninguna card real, bajo el supuesto de que "Color
-- de marca" (brand-1/brand-2) era suficiente. El dueño aclaró (2026-09-01, repetidas veces)
-- que quiere que la card, como componente, tenga sus PROPIOS controles de header/body/footer
-- independientes del color de marca -- no reutilizar brand-1/brand-2.
--
-- Semilla: mismo verde que ya se ve hoy en las cards reales (viene de brand-1), para que
-- esta migración no cambie nada visualmente -- el dueño ajusta el color desde acá cuando
-- quiera separarlo del de marca.
-- ============================================================

INSERT INTO tema_variable (clave, etiqueta, grupo, tipo, valor_claro, valor_oscuro, orden)
SELECT 'card-header-bg', 'Color del encabezado de las tarjetas', 'Card', 'color', '#00875A', '#00875A', 1
WHERE NOT EXISTS (SELECT 1 FROM tema_variable WHERE clave = 'card-header-bg');

INSERT INTO tema_variable (clave, etiqueta, grupo, tipo, valor_claro, valor_oscuro, orden)
SELECT 'card-footer-bg', 'Color del pie de las tarjetas', 'Card', 'color', '#FFFFFF', '#0C0C0C', 4
WHERE NOT EXISTS (SELECT 1 FROM tema_variable WHERE clave = 'card-footer-bg');

-- Reordena el resto del grupo "Card" para que header/footer queden junto a body/border,
-- en vez de al final.
UPDATE tema_variable SET orden = 2 WHERE clave = 'card-body-bg';
UPDATE tema_variable SET orden = 3 WHERE clave = 'card-border';
UPDATE tema_variable SET orden = 5 WHERE clave = 'card-radius';
UPDATE tema_variable SET orden = 6 WHERE clave = 'card-shadow';

-- ============================================================
-- VERIFICACIÓN
-- ============================================================
-- SELECT clave, etiqueta, grupo, orden FROM tema_variable WHERE grupo = 'Card' ORDER BY orden;
