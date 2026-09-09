-- ============================================================
-- Migración: elimina card-header-bg y card-footer-bg del catálogo de
-- Personalización.
--
-- Al auditar el grupo "Card" se encontró que estas dos filas nunca controlaron
-- ninguna card real de la app: el único lugar que leía var(--card-header-bg) y
-- var(--card-footer-bg) era la propia vista previa de la pantalla de
-- Personalización (gestion-personalizacion.component.scss). El header real de
-- las cards (pedidos, productos, variantes, etc.) usa el degradado de marca
-- --brand-2/--brand-1 (pedido 2026-09-01), y el footer real hereda el mismo
-- fondo que el body (--card-body-bg, ya editable y con alias real a --card-bg).
--
-- Resultado antes de esta migración: el dueño podía cambiar "Fondo del
-- encabezado"/"Fondo del pie de las tarjetas" y ver el cambio en la vista
-- previa, pero ninguna card real de la app cambiaba -- un control que parecía
-- funcionar pero no hacía nada fuera de esa pantalla. Se decidió borrarlas en
-- vez de dejarlas huérfanas (confirmado con el dueño 2026-09-01).
--
-- card-body-bg, card-border, card-radius, card-shadow NO se tocan -- esos sí
-- controlan cards reales.
-- ============================================================

DELETE FROM tema_variable WHERE clave IN ('card-header-bg', 'card-footer-bg');

-- ============================================================
-- VERIFICACIÓN
-- ============================================================
-- SELECT clave, etiqueta, grupo FROM tema_variable WHERE grupo = 'Card' ORDER BY orden;
