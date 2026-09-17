-- ============================================================
-- Migración: ubicación del local (dirección + punto en el mapa)
--
-- Motivo: el login y el registro van a mostrar una miniatura del mapa con el local marcado y un
-- botón "Cómo llegar" que abre Google Maps trazando la ruta desde donde esté el cliente. Para eso
-- hacen falta la dirección (texto que se le muestra) y el punto exacto (lat/lng, con el que se
-- arma el link). `configuracion_negocio` ya guardaba horario y redes, pero nada de ubicación.
--
-- Se capturan desde Administración > Configuración del negocio, con el MISMO selector de mapa que
-- ya se usa para el punto de encuentro de las entregas (Leaflet + OpenStreetMap, sin API key).
--
-- Quedan en NULL a propósito: mientras no se capture la ubicación, login y registro no muestran
-- nada (no se inventa un punto por defecto — marcaría un local que no es el tuyo).
-- ============================================================

ALTER TABLE configuracion_negocio
    ADD COLUMN direccion VARCHAR(255) NULL,
    ADD COLUMN latitud   DOUBLE       NULL,
    ADD COLUMN longitud  DOUBLE       NULL;
