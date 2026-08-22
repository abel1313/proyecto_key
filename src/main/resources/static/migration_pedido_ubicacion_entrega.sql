-- Migracion 2026-08-22: ubicacion exacta de entrega en el pedido (peticion del front).
-- Distinto de LugarEntrega (que es la zona/pueblo, ej. Tejupilco) -- esto es el punto exacto
-- de la casa de CADA cliente, capturado en un mapa (Leaflet+OpenStreetMap del lado del front).
-- Los 3 campos son opcionales -- direccion_entrega (texto libre, ya existente) sigue siendo
-- el dato principal, esto es un complemento para poder trazar la ruta exacta al entregar.
ALTER TABLE pedidos ADD COLUMN latitud DOUBLE NULL;
ALTER TABLE pedidos ADD COLUMN longitud DOUBLE NULL;
ALTER TABLE pedidos ADD COLUMN referencias VARCHAR(255) NULL;
