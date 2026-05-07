-- ============================================================
-- MIGRACIÓN: inventario_key_qa  →  inventario_key (producción)
-- Ejecutar conectado al mismo servidor MySQL donde existen ambas bases.
-- ============================================================

SET FOREIGN_KEY_CHECKS = 0;

-- ------------------------------------------------------------
-- 1. codigo_barras
--    Valida por el VALOR del código de barras (no por id).
--    NO se fuerza el id para evitar colisiones con prod;
--    el paso de producto resuelve el id correcto por valor de barras.
-- ------------------------------------------------------------
INSERT INTO inventario_key.codigo_barras (codigo_barras)
SELECT q.codigo_barras
FROM inventario_key_qa.codigo_barras q
WHERE NOT EXISTS (
    SELECT 1
    FROM inventario_key.codigo_barras p
    WHERE p.codigo_barras = q.codigo_barras
);

-- ------------------------------------------------------------
-- 2. imagenes_copy
--    Valida por id.
-- ------------------------------------------------------------
INSERT INTO inventario_key.imagenes_copy (id, base_64, extension, nombre_imagen)
SELECT q.id, q.base_64, q.extension, q.nombre_imagen
FROM inventario_key_qa.imagenes_copy q
WHERE NOT EXISTS (
    SELECT 1
    FROM inventario_key.imagenes_copy p
    WHERE p.id = q.id
);

-- ------------------------------------------------------------
-- 3. producto
--    Valida por el código de barras asociado (negocio, no por id).
--    NO se fuerza el id para evitar colisiones con prod;
--    producto_imagen_copy y variantes resuelven producto_id por barras.
-- ------------------------------------------------------------
INSERT INTO inventario_key.producto (
    nombre, precio_costo, piezas, color,
    precio_venta, precio_rebaja, descripcion,
    stock, marca, contenido_neto, habilitado,
    codigo_barras_id
)
SELECT
    q.nombre,
    q.precio_costo,
    q.piezas,
    q.color,
    q.precio_venta,
    q.precio_rebaja,
    q.descripcion,
    q.stock,
    q.marca,
    q.contenido_neto,
    q.habilitado,
    (
        SELECT p_cb.id
        FROM inventario_key.codigo_barras p_cb
        WHERE p_cb.codigo_barras = q_cb.codigo_barras
        LIMIT 1
    ) AS codigo_barras_id
FROM inventario_key_qa.producto q
LEFT JOIN inventario_key_qa.codigo_barras q_cb ON q_cb.id = q.codigo_barras_id
WHERE NOT EXISTS (
    SELECT 1
    FROM inventario_key.producto p_prod
    JOIN inventario_key.codigo_barras p_cb ON p_cb.id = p_prod.codigo_barras_id
    WHERE p_cb.codigo_barras = q_cb.codigo_barras
);

-- ------------------------------------------------------------
-- 4. producto_imagen_copy
--    Valida por id.
--    producto_id se resuelve por código de barras del producto de QA.
-- ------------------------------------------------------------
INSERT INTO inventario_key.producto_imagen_copy (id, producto_id, imagen_id)
SELECT
    q.id,
    (
        SELECT p_prod.id
        FROM inventario_key.producto p_prod
        JOIN inventario_key.codigo_barras p_cb ON p_cb.id = p_prod.codigo_barras_id
        JOIN inventario_key_qa.producto q_prod ON q_prod.id = q.producto_id
        JOIN inventario_key_qa.codigo_barras q_cb ON q_cb.id = q_prod.codigo_barras_id
        WHERE p_cb.codigo_barras = q_cb.codigo_barras
        LIMIT 1
    ) AS producto_id,
    q.imagen_id
FROM inventario_key_qa.producto_imagen_copy q
WHERE NOT EXISTS (
    SELECT 1
    FROM inventario_key.producto_imagen_copy p
    WHERE p.id = q.id
);

-- ------------------------------------------------------------
-- 5. variantes
--    Valida por id.
--    producto_id se resuelve por código de barras del producto de QA.
-- ------------------------------------------------------------
INSERT INTO inventario_key.variantes (
    id, producto_id, talla, descripcion,
    color, presentacion, stock, marca, contenido_neto
)
SELECT
    q.id,
    (
        SELECT p_prod.id
        FROM inventario_key.producto p_prod
        JOIN inventario_key.codigo_barras p_cb ON p_cb.id = p_prod.codigo_barras_id
        JOIN inventario_key_qa.producto q_prod ON q_prod.id = q.producto_id
        JOIN inventario_key_qa.codigo_barras q_cb ON q_cb.id = q_prod.codigo_barras_id
        WHERE p_cb.codigo_barras = q_cb.codigo_barras
        LIMIT 1
    ) AS producto_id,
    q.talla, q.descripcion,
    q.color, q.presentacion, q.stock, q.marca, q.contenido_neto
FROM inventario_key_qa.variantes q
WHERE NOT EXISTS (
    SELECT 1
    FROM inventario_key.variantes p
    WHERE p.id = q.id
);

-- ------------------------------------------------------------
-- 6. variante_imagen
--    Valida por id.
-- ------------------------------------------------------------
INSERT INTO inventario_key.variante_imagen (id, variante_id, imagen_id)
SELECT q.id, q.variante_id, q.imagen_id
FROM inventario_key_qa.variante_imagen q
WHERE NOT EXISTS (
    SELECT 1
    FROM inventario_key.variante_imagen p
    WHERE p.id = q.id
);

SET FOREIGN_KEY_CHECKS = 1;