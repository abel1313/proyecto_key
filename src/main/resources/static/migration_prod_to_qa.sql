-- ============================================================
-- MIGRACIÓN: inventario_key (producción)  →  inventario_key_qa
-- Ejecutar conectado al mismo servidor MySQL donde existen ambas bases.
-- ============================================================

SET FOREIGN_KEY_CHECKS = 0;

-- ------------------------------------------------------------
-- 1. codigo_barras
--    Valida por el VALOR del código de barras (no por id).
-- ------------------------------------------------------------
INSERT INTO inventario_key_qa.codigo_barras (codigo_barras)
SELECT p.codigo_barras
FROM inventario_key.codigo_barras p
WHERE NOT EXISTS (
    SELECT 1
    FROM inventario_key_qa.codigo_barras q
    WHERE q.codigo_barras = p.codigo_barras
);

-- ------------------------------------------------------------
-- 2. imagenes_copy
--    Valida por id.
-- ------------------------------------------------------------
INSERT INTO inventario_key_qa.imagenes_copy (id, base_64, extension, nombre_imagen)
SELECT p.id, p.base_64, p.extension, p.nombre_imagen
FROM inventario_key.imagenes_copy p
WHERE NOT EXISTS (
    SELECT 1
    FROM inventario_key_qa.imagenes_copy q
    WHERE q.id = p.id
);

-- ------------------------------------------------------------
-- 3. producto
--    Valida por el código de barras asociado (negocio, no por id).
--    NO se fuerza el id para evitar colisiones con QA;
--    producto_imagen_copy y variantes resuelven producto_id por barras.
-- ------------------------------------------------------------
INSERT INTO inventario_key_qa.producto (
    nombre, precio_costo, piezas, color,
    precio_venta, precio_rebaja, descripcion,
    stock, marca, contenido_neto, habilitado,
    codigo_barras_id
)
SELECT
    p.nombre,
    p.precio_costo,
    p.piezas,
    p.color,
    p.precio_venta,
    p.precio_rebaja,
    p.descripcion,
    p.stock,
    p.marca,
    p.contenido_neto,
    p.habilitado,
    (
        SELECT q_cb.id
        FROM inventario_key_qa.codigo_barras q_cb
        WHERE q_cb.codigo_barras = p_cb.codigo_barras
        LIMIT 1
    ) AS codigo_barras_id
FROM inventario_key.producto p
LEFT JOIN inventario_key.codigo_barras p_cb ON p_cb.id = p.codigo_barras_id
WHERE NOT EXISTS (
    SELECT 1
    FROM inventario_key_qa.producto q_prod
    JOIN inventario_key_qa.codigo_barras q_cb ON q_cb.id = q_prod.codigo_barras_id
    WHERE q_cb.codigo_barras = p_cb.codigo_barras
);

-- ------------------------------------------------------------
-- 4. producto_imagen_copy
--    Valida por id.
--    producto_id se resuelve por código de barras del producto de prod.
-- ------------------------------------------------------------
INSERT INTO inventario_key_qa.producto_imagen_copy (id, producto_id, imagen_id)
SELECT
    p.id,
    (
        SELECT q_prod.id
        FROM inventario_key_qa.producto q_prod
        JOIN inventario_key_qa.codigo_barras q_cb ON q_cb.id = q_prod.codigo_barras_id
        JOIN inventario_key.producto p_prod ON p_prod.id = p.producto_id
        JOIN inventario_key.codigo_barras p_cb ON p_cb.id = p_prod.codigo_barras_id
        WHERE q_cb.codigo_barras = p_cb.codigo_barras
        LIMIT 1
    ) AS producto_id,
    p.imagen_id
FROM inventario_key.producto_imagen_copy p
WHERE NOT EXISTS (
    SELECT 1
    FROM inventario_key_qa.producto_imagen_copy q
    WHERE q.id = p.id
);

-- ------------------------------------------------------------
-- 5. variantes
--    Valida por id.
--    producto_id se resuelve por código de barras del producto de prod.
-- ------------------------------------------------------------
INSERT INTO inventario_key_qa.variantes (
    id, producto_id, talla, descripcion,
    color, presentacion, stock, marca, contenido_neto
)
SELECT
    p.id,
    (
        SELECT q_prod.id
        FROM inventario_key_qa.producto q_prod
        JOIN inventario_key_qa.codigo_barras q_cb ON q_cb.id = q_prod.codigo_barras_id
        JOIN inventario_key.producto p_prod ON p_prod.id = p.producto_id
        JOIN inventario_key.codigo_barras p_cb ON p_cb.id = p_prod.codigo_barras_id
        WHERE q_cb.codigo_barras = p_cb.codigo_barras
        LIMIT 1
    ) AS producto_id,
    p.talla, p.descripcion,
    p.color, p.presentacion, p.stock, p.marca, p.contenido_neto
FROM inventario_key.variantes p
WHERE NOT EXISTS (
    SELECT 1
    FROM inventario_key_qa.variantes q
    WHERE q.id = p.id
);

-- ------------------------------------------------------------
-- 6. variante_imagen
--    Valida por id.
-- ------------------------------------------------------------
INSERT INTO inventario_key_qa.variante_imagen (id, variante_id, imagen_id)
SELECT p.id, p.variante_id, p.imagen_id
FROM inventario_key.variante_imagen p
WHERE NOT EXISTS (
    SELECT 1
    FROM inventario_key_qa.variante_imagen q
    WHERE q.id = p.id
);

SET FOREIGN_KEY_CHECKS = 1;