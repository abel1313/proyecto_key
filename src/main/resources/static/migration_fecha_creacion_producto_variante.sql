-- Migracion 2026-08-22: fecha_creacion en producto y variantes, para poder buscar por fecha
-- en el admin (admin/filtrar). Necesario porque la carga rapida de imagenes asigna un codigo
-- de barras al azar (BRD-XXXXXXXXXXXX) al crear el borrador, asi que con muchos productos no
-- hay forma de encontrar el que se acaba de crear por nombre/codigo -- buscar "los de hoy" es
-- la unica via practica.
-- Sin backfill retroactivo (mismo criterio que correo_verificado): los registros existentes
-- quedan con fecha_creacion NULL, solo los nuevos (desde que se despliegue esto) la tendran.
ALTER TABLE producto ADD COLUMN fecha_creacion DATETIME NULL;
ALTER TABLE variantes ADD COLUMN fecha_creacion DATETIME NULL;
