-- Migración 2026-08-14: pliegos por defecto configurables, como último respaldo cuando una
-- cantidad de flores no tiene fila registrada (ni pliegos explícito ni fórmula floresPorPliego).
-- Reemplaza el "1 pliego" implícito de antes -- el dueño pidió que el respaldo sea 2, y que se
-- pueda ajustar sin desplegar. Ver conversación con el front/dueño del negocio del 2026-08-14.

ALTER TABLE accesorio_ramo
    ADD COLUMN pliegos_por_defecto INT NULL COMMENT 'Solo aplica al accesorio marcado es_papel=true: ultimo respaldo de pliegos cuando ni el pliegos explicito de cantidad_flor_valida ni floresPorPliego estan configurados para esa cantidad. NULL = sin este respaldo, cae al precio fijo unico de siempre.';

-- ============================================================
-- IMPORTANTE -- acción manual pendiente después de correr esta migración:
-- ============================================================
-- Editar el accesorio marcado "es_papel=true" y ponerle "pliegos_por_defecto" = 2 (o el numero
-- que decida el dueño). Mientras nadie lo configure, no cambia nada del comportamiento actual.
