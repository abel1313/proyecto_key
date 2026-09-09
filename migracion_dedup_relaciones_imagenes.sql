-- =====================================================================================
-- Saneamiento de las tablas de relacion producto/variante <-> imagen
--
-- PROBLEMA MEDIDO EN QA (inventario_key_qa, 2026-08-11):
--   producto_imagen_copy : 13,095 filas  ->  81 pares (producto_id, imagen_id) distintos
--                          el producto 265 solo tiene 11,032 filas para 25 imagenes reales
--   variante_imagen      : 12,607 filas  ->  1,795 pares distintos
--                          las variantes 552-556 tienen 778 filas cada una para 16 imagenes
--
--   O sea: >99% de producto_imagen_copy y ~86% de variante_imagen son filas repetidas.
--   Cada listado de catalogo lee y ordena esas filas para quedarse con UNA imagen por fila
--   del listado, asi que la basura se paga en cada request de la tienda.
--
-- ORDEN DE EJECUCION: 1) respaldo  2) limpieza  3) UNIQUE  4) indices  5) verificacion
--
-- Correr primero en QA (inventario_key_qa) y solo despues en prod (inventario_key).
-- =====================================================================================


-- -------------------------------------------------------------------------------------
-- 1) RESPALDO (obligatorio: el paso 2 borra filas y no hay vuelta atras sin esto)
-- -------------------------------------------------------------------------------------
CREATE TABLE producto_imagen_copy_bkp_20260811 AS SELECT * FROM producto_imagen_copy;
CREATE TABLE variante_imagen_bkp_20260811     AS SELECT * FROM variante_imagen;

-- Conteo previo, para comparar al final
SELECT 'ANTES producto_imagen_copy' etiqueta, COUNT(*) filas,
       COUNT(DISTINCT CONCAT(producto_id,':',imagen_id)) pares FROM producto_imagen_copy
UNION ALL
SELECT 'ANTES variante_imagen', COUNT(*),
       COUNT(DISTINCT CONCAT(variante_id,':',imagen_id)) FROM variante_imagen;


-- -------------------------------------------------------------------------------------
-- 2) LIMPIEZA — de cada par duplicado se conserva UNA sola fila.
--    Criterio de cual se conserva: la fila con principal = 1 si alguna lo tiene
--    (para no perder cual es la imagen principal); en caso de empate, el id mas chico.
--    Es el mismo criterio de orden que usa el listado, asi que la miniatura que ve el
--    front no cambia.
-- -------------------------------------------------------------------------------------
DELETE pi FROM producto_imagen_copy pi
JOIN (
    SELECT producto_id, imagen_id,
           MIN(CASE WHEN principal = 1 THEN id END) AS id_principal,
           MIN(id) AS id_min
    FROM producto_imagen_copy
    GROUP BY producto_id, imagen_id
) conservar
  ON  pi.producto_id = conservar.producto_id
  AND pi.imagen_id   = conservar.imagen_id
  AND pi.id <> COALESCE(conservar.id_principal, conservar.id_min);

DELETE vi FROM variante_imagen vi
JOIN (
    SELECT variante_id, imagen_id,
           MIN(CASE WHEN principal = 1 THEN id END) AS id_principal,
           MIN(id) AS id_min
    FROM variante_imagen
    GROUP BY variante_id, imagen_id
) conservar
  ON  vi.variante_id = conservar.variante_id
  AND vi.imagen_id   = conservar.imagen_id
  AND vi.id <> COALESCE(conservar.id_principal, conservar.id_min);


-- -------------------------------------------------------------------------------------
-- 3) UNIQUE — es lo que impide que el problema vuelva. El codigo ya filtra los pares
--    existentes antes de insertar (VarianteServiceImpl.vincularImagenes y
--    ProductosServiceImpl.compartirImagenesVarianteDto), pero eso es la defensa de
--    aplicacion; esta es la del motor, y aplica tambien a cualquier ruta que se agregue
--    despues o a inserciones manuales.
--
--    Si alguno de estos ALTER falla por "Duplicate entry", el paso 2 no limpio todo:
--    revisar con las consultas del paso 5 antes de insistir.
-- -------------------------------------------------------------------------------------
ALTER TABLE producto_imagen_copy
    ADD CONSTRAINT uk_producto_imagen UNIQUE (producto_id, imagen_id);

ALTER TABLE variante_imagen
    ADD CONSTRAINT uk_variante_imagen UNIQUE (variante_id, imagen_id);


-- -------------------------------------------------------------------------------------
-- 4) INDICES para la consulta de "primera imagen" del listado
--    (findIdsPrimeraImagenByProductoIdIn / findIdsPrimeraImagenByVarianteIdIn):
--        WHERE producto_id IN (...) ORDER BY principal DESC, imagen_id ASC
--    Hoy solo existe el indice de la FK (producto_id / variante_id) suelto, asi que el
--    ORDER BY se resuelve con filesort. Con estos, la consulta se sirve entera del indice.
-- -------------------------------------------------------------------------------------
CREATE INDEX idx_pi_producto_principal_imagen
    ON producto_imagen_copy (producto_id, principal, imagen_id);

CREATE INDEX idx_vi_variante_principal_id
    ON variante_imagen (variante_id, principal, id);


-- -------------------------------------------------------------------------------------
-- 5) VERIFICACION — filas debe quedar igual a pares en ambas tablas
-- -------------------------------------------------------------------------------------
SELECT 'DESPUES producto_imagen_copy' etiqueta, COUNT(*) filas,
       COUNT(DISTINCT CONCAT(producto_id,':',imagen_id)) pares FROM producto_imagen_copy
UNION ALL
SELECT 'DESPUES variante_imagen', COUNT(*),
       COUNT(DISTINCT CONCAT(variante_id,':',imagen_id)) FROM variante_imagen;

-- Ninguna de estas dos debe devolver filas:
SELECT producto_id, imagen_id, COUNT(*) FROM producto_imagen_copy
  GROUP BY producto_id, imagen_id HAVING COUNT(*) > 1;
SELECT variante_id, imagen_id, COUNT(*) FROM variante_imagen
  GROUP BY variante_id, imagen_id HAVING COUNT(*) > 1;

-- Y ningun producto/variante debio quedarse sin imagenes por la limpieza
-- (compara contra el respaldo del paso 1):
SELECT 'productos que perdieron todas sus imagenes' etiqueta, COUNT(*) FROM (
    SELECT DISTINCT producto_id FROM producto_imagen_copy_bkp_20260811
    WHERE producto_id NOT IN (SELECT producto_id FROM producto_imagen_copy)
) t
UNION ALL
SELECT 'variantes que perdieron todas sus imagenes', COUNT(*) FROM (
    SELECT DISTINCT variante_id FROM variante_imagen_bkp_20260811
    WHERE variante_id NOT IN (SELECT variante_id FROM variante_imagen)
) t;


-- -------------------------------------------------------------------------------------
-- 6) Cuando QA lleve unos dias estable, borrar los respaldos:
--      DROP TABLE producto_imagen_copy_bkp_20260811;
--      DROP TABLE variante_imagen_bkp_20260811;
-- -------------------------------------------------------------------------------------
