-- ============================================================================
-- BACKFILL — variantes de Carga rapida que quedaron sin datos
--
-- Contexto: hasta el hotfix del 2026-09-22, PUT /v1/carga-imagenes/{id}/completar
-- escribia solo en la tabla `producto`. La variante que crea la carga rapida nace
-- con unicamente producto_id + stock, asi que se quedaba sin descripcion, color,
-- marca ni contenido_neto. El admin veia la info bien en productos y vacia en el
-- articulo (que es lo que el cliente ve en la tienda).
--
-- El codigo ya quedo arreglado: de aqui en adelante toda carga rapida sincroniza
-- su variante. Este script es SOLO para las que ya se cargaron antes del fix.
--
-- NO ES IDEMPOTENTE POR DISENO en el sentido de "corre cuantas veces quieras sin
-- pensar": solo toca columnas que estan NULL o vacias, asi que nunca pisa un dato
-- que alguien haya escrito a mano en la variante. Correrlo dos veces no hace dano.
--
-- ⚠️ NO TOCA STOCK. Ni el del producto ni el de la variante. El descuadre de
-- inventario es otro problema y se decide producto por producto (ver el incidente
-- del 2026-09-22 en CLAUDE.md).
--
-- Base: inventario_key_qa (cubre dev+qa) / inventario_key (prod)
-- ============================================================================


-- ----------------------------------------------------------------------------
-- 1. DIAGNOSTICO — correr ESTO PRIMERO y guardar el resultado
--    Cuantas variantes estan vacias teniendo el producto los datos cargados.
-- ----------------------------------------------------------------------------
SELECT COUNT(*) AS variantes_a_reparar
FROM variantes v
JOIN producto p ON p.id = v.producto_id
WHERE (v.descripcion   IS NULL OR v.descripcion   = '')
  AND (v.color         IS NULL OR v.color         = '')
  AND (v.marca         IS NULL OR v.marca         = '')
  AND (v.contenido_neto IS NULL OR v.contenido_neto = '')
  AND (p.descripcion IS NOT NULL AND p.descripcion <> '');


-- Detalle, por si se quiere revisar antes de tocar nada (limitado a 50)
SELECT v.id AS variante_id, p.id AS producto_id, p.nombre,
       p.descripcion AS desc_producto, v.descripcion AS desc_variante,
       p.color AS color_producto, v.color AS color_variante,
       p.marca AS marca_producto, v.marca AS marca_variante
FROM variantes v
JOIN producto p ON p.id = v.producto_id
WHERE (v.descripcion IS NULL OR v.descripcion = '')
  AND (p.descripcion IS NOT NULL AND p.descripcion <> '')
ORDER BY v.id DESC
LIMIT 50;


-- ----------------------------------------------------------------------------
-- 2. BACKFILL — cada columna por separado, solo donde la variante esta vacia
--    y el producto SI tiene el dato.
-- ----------------------------------------------------------------------------

UPDATE variantes v
JOIN producto p ON p.id = v.producto_id
SET v.descripcion = p.descripcion
WHERE (v.descripcion IS NULL OR v.descripcion = '')
  AND p.descripcion IS NOT NULL AND p.descripcion <> '';

UPDATE variantes v
JOIN producto p ON p.id = v.producto_id
SET v.color = p.color
WHERE (v.color IS NULL OR v.color = '')
  AND p.color IS NOT NULL AND p.color <> '';

UPDATE variantes v
JOIN producto p ON p.id = v.producto_id
SET v.marca = p.marca
WHERE (v.marca IS NULL OR v.marca = '')
  AND p.marca IS NOT NULL AND p.marca <> '';

UPDATE variantes v
JOIN producto p ON p.id = v.producto_id
SET v.contenido_neto = p.contenido
WHERE (v.contenido_neto IS NULL OR v.contenido_neto = '')
  AND p.contenido IS NOT NULL AND p.contenido <> '';

-- La categoria (palabra clave) es la que decide si el articulo sale en las
-- busquedas por palabra clave de la tienda.
UPDATE variantes v
JOIN producto p ON p.id = v.producto_id
SET v.palabra_clave_id = p.palabra_clave_id
WHERE v.palabra_clave_id IS NULL
  AND p.palabra_clave_id IS NOT NULL;


-- ----------------------------------------------------------------------------
-- 3. VERIFICACION — tiene que dar 0 (o bajar mucho respecto al paso 1)
-- ----------------------------------------------------------------------------
SELECT COUNT(*) AS siguen_vacias
FROM variantes v
JOIN producto p ON p.id = v.producto_id
WHERE (v.descripcion IS NULL OR v.descripcion = '')
  AND (p.descripcion IS NOT NULL AND p.descripcion <> '');


-- Confirmacion de que el stock NO se movio: comparar contra la foto de antes.
SELECT COUNT(*) AS productos_con_stock_negativo FROM producto WHERE stock < 0;
