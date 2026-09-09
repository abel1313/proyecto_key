-- ============================================================
-- Migración: agrega --input-placeholder al catálogo de Personalización.
--
-- Al auditar qué tokens de formularios (.pk-input, design-system.scss) ya eran
-- editables desde Personalización se encontró que --input-text y --input-border
-- YA estaban cubiertos indirectamente vía el alias de app-text/app-border (ver
-- ALIAS_LEGACY en tema.model.ts, frontend), y --input-focus-border/--input-focus-shadow
-- ya derivan de --brand-1/--app-accent-rgb -- solo faltaba el color del texto de
-- placeholder, que seguía fijo en styles.scss sin fila en tema_variable.
--
-- Semilla: mismo valor que ya estaba a mano en los bloques body.theme-light /
-- body.theme-dark de styles.scss -- la primera carga no cambia nada visualmente.
-- ============================================================

INSERT INTO tema_variable (clave, etiqueta, grupo, tipo, valor_claro, valor_oscuro, orden)
SELECT 'input-placeholder', 'Color del texto de ejemplo (placeholder) en los campos', 'Formularios', 'color', '#9DBAAD', '#6E6E73', 3
WHERE NOT EXISTS (SELECT 1 FROM tema_variable WHERE clave = 'input-placeholder');

-- ============================================================
-- VERIFICACIÓN
-- ============================================================
-- SELECT clave, etiqueta, grupo, valor_claro, valor_oscuro FROM tema_variable WHERE clave = 'input-placeholder';
