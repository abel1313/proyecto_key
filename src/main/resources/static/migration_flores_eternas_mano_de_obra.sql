-- Migración 2026-08-14: mano de obra de armado, configurable por tamaño de ramo. Decisión del
-- dueño: el cliente ve UN SOLO precio de ramo, sin línea desglosada -- por eso NO es un accesorio
-- nuevo, es un monto que vive junto a "pliegos" en cantidad_flor_valida y se suma directo al
-- total. Ver conversación con el front/dueño del negocio del 2026-08-14.

ALTER TABLE cantidad_flor_valida
    ADD COLUMN mano_de_obra DOUBLE NULL COMMENT 'Costo de mano de obra de armar este tamaño de ramo, puesto a mano por el dueño (ej. un ramo de 62 lleva mas trabajo que uno de 20). Se suma directo a precio_total sin aparecer como accesorio. NULL = sin costo de mano de obra para esta cantidad.';

ALTER TABLE ramo_armado
    ADD COLUMN precio_mano_de_obra DOUBLE NULL COMMENT 'Congelado al guardar el ramo, igual que precio_flores/precio_papel -- ya esta sumado en precio_total.';

-- No requiere acción manual: el campo nace vacío (NULL) para todo lo ya registrado, que es el
-- estado esperado -- el dueño lo va llenando conforme decida cuánto cobrar por tamaño de ramo.
