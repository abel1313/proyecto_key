-- Migración 2026-08-14: pliegos de papel configurables POR RAMO (por cada CantidadFlorValida),
-- en vez de derivarse solo de la fórmula floresPorPliego. Ver conversación con el front/dueño.

ALTER TABLE cantidad_flor_valida
    ADD COLUMN pliegos INT NULL COMMENT 'Cuantos pliegos de papel lleva este ramo especifico, configurado a mano por el dueno (no es proporcional a la cantidad de flores). NULL = todavia no configurado; en ese caso se usa como respaldo AccesorioRamo.floresPorPliego (formula) y, si tampoco esta, el precio fijo unico anterior.';

-- No requiere accion manual: el campo nace vacio (NULL) para todas las cantidades ya registradas,
-- que es exactamente el estado esperado -- el dueno lo va llenando conforme sepa cuantos pliegos
-- gasta cada ramo. Mientras tanto todo sigue funcionando igual que hoy (formula o precio fijo).
