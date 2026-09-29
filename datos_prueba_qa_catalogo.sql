-- =====================================================================================
-- DATOS DE PRUEBA DEL CATÁLOGO — SOLO PARA inventario_key_qa (dev y qa). NUNCA EN PROD.
-- =====================================================================================
-- Qué crea:
--   * 200 productos de prueba y 400 artículos (2 por producto).
--     Bolsas 40, Pantalones 40, Faldas 35, Blusas 30, Vestidos 30, Accesorios 25.
--   * Cada producto tiene stock 10, repartido entre sus 2 artículos (5+5, 6+4, 7+3, 8+2, 9+1,
--     10+0, 4+6, 3+7). 25 artículos quedan en 0 para probar "agotado".
--   * Precios en pesos (99 a 749). 67 productos traen "Otro precio" (rebaja menor al
--     precio normal); el resto lleva rebaja = precio normal, igual que el alta desde el admin.
--   * 20 artículos con precio propio (💲 por artículo); 10 de ellos con rebaja propia.
--   * Categorías (palabra_clave) Bolsas, Pantalones, Faldas, Blusas, Vestidos y Accesorios: si ya
--     existen se reusan, si no se crean.
--   * Imagen: cada artículo se liga a una imagen que YA existe en QA (de productos reales), para
--     que aparezca en la tienda pública. La foto no corresponde al artículo: es solo para pruebas.
--     No se crea ni se borra ninguna imagen, y dar de baja un artículo de prueba no borra la foto
--     real (solo se borran imágenes que ya nadie usa).
--
-- Cómo se reconocen: código de barras 2099000000001 a 2099000000200 (rango 20-29 de uso interno,
-- no choca con códigos reales) y marca "Prueba QA".
--
-- Seguridad:
--   * Cada INSERT lleva DATABASE() = 'inventario_key_qa': en cualquier otra base no inserta nada.
--   * Se puede correr dos veces: la segunda no duplica nada.
--   * No toca ningún producto, artículo ni imagen existente.
--   * Para quitarlos: limpiar_datos_prueba_qa_catalogo.sql (baja lógica, no DELETE). Después de
--     limpiar, volver a correr este script NO los reactiva: ya existen, dados de baja.
--
-- Probado antes de entregar (2026-09-29) en MySQL 8.0 con el esquema de las entidades más los
-- NOT NULL reales de producto, con sql_mode de MySQL 8, dos corridas seguidas, y en una base con
-- otro nombre (0 filas). Ver ESPECIFICACIONES_AMBIENTES.md.
--
-- Nota: la alerta diaria de stock bajo (umbral 5) va a listar muchos de estos artículos.
-- =====================================================================================

SET NAMES utf8mb4;

SELECT IF(DATABASE() = 'inventario_key_qa', 'OK: base inventario_key_qa',
          CONCAT('ALTO: estás en ', IFNULL(DATABASE(), '(ninguna)'), ' - este script no va a insertar nada')) AS base_actual;

START TRANSACTION;

-- -------------------------------------------------------------------------------------
-- 1. Categorías
-- -------------------------------------------------------------------------------------
INSERT INTO palabra_clave (nombre)
SELECT 'Bolsas' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM palabra_clave WHERE nombre = 'Bolsas');
INSERT INTO palabra_clave (nombre)
SELECT 'Pantalones' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM palabra_clave WHERE nombre = 'Pantalones');
INSERT INTO palabra_clave (nombre)
SELECT 'Faldas' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM palabra_clave WHERE nombre = 'Faldas');
INSERT INTO palabra_clave (nombre)
SELECT 'Blusas' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM palabra_clave WHERE nombre = 'Blusas');
INSERT INTO palabra_clave (nombre)
SELECT 'Vestidos' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM palabra_clave WHERE nombre = 'Vestidos');
INSERT INTO palabra_clave (nombre)
SELECT 'Accesorios' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM palabra_clave WHERE nombre = 'Accesorios');

-- -------------------------------------------------------------------------------------
-- 2. Códigos de barras (uno por producto)
-- -------------------------------------------------------------------------------------
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000001' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000001');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000002' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000002');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000003' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000003');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000004' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000004');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000005' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000005');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000006' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000006');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000007' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000007');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000008' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000008');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000009' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000009');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000010' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000010');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000011' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000011');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000012' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000012');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000013' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000013');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000014' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000014');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000015' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000015');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000016' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000016');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000017' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000017');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000018' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000018');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000019' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000019');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000020' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000020');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000021' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000021');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000022' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000022');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000023' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000023');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000024' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000024');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000025' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000025');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000026' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000026');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000027' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000027');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000028' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000028');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000029' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000029');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000030' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000030');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000031' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000031');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000032' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000032');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000033' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000033');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000034' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000034');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000035' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000035');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000036' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000036');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000037' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000037');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000038' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000038');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000039' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000039');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000040' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000040');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000041' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000041');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000042' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000042');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000043' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000043');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000044' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000044');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000045' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000045');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000046' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000046');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000047' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000047');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000048' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000048');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000049' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000049');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000050' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000050');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000051' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000051');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000052' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000052');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000053' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000053');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000054' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000054');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000055' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000055');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000056' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000056');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000057' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000057');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000058' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000058');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000059' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000059');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000060' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000060');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000061' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000061');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000062' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000062');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000063' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000063');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000064' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000064');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000065' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000065');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000066' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000066');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000067' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000067');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000068' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000068');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000069' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000069');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000070' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000070');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000071' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000071');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000072' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000072');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000073' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000073');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000074' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000074');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000075' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000075');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000076' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000076');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000077' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000077');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000078' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000078');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000079' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000079');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000080' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000080');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000081' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000081');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000082' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000082');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000083' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000083');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000084' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000084');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000085' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000085');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000086' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000086');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000087' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000087');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000088' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000088');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000089' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000089');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000090' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000090');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000091' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000091');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000092' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000092');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000093' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000093');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000094' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000094');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000095' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000095');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000096' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000096');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000097' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000097');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000098' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000098');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000099' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000099');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000100' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000100');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000101' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000101');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000102' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000102');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000103' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000103');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000104' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000104');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000105' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000105');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000106' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000106');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000107' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000107');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000108' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000108');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000109' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000109');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000110' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000110');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000111' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000111');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000112' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000112');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000113' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000113');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000114' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000114');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000115' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000115');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000116' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000116');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000117' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000117');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000118' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000118');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000119' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000119');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000120' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000120');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000121' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000121');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000122' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000122');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000123' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000123');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000124' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000124');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000125' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000125');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000126' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000126');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000127' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000127');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000128' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000128');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000129' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000129');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000130' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000130');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000131' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000131');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000132' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000132');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000133' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000133');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000134' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000134');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000135' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000135');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000136' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000136');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000137' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000137');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000138' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000138');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000139' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000139');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000140' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000140');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000141' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000141');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000142' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000142');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000143' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000143');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000144' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000144');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000145' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000145');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000146' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000146');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000147' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000147');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000148' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000148');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000149' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000149');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000150' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000150');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000151' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000151');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000152' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000152');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000153' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000153');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000154' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000154');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000155' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000155');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000156' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000156');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000157' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000157');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000158' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000158');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000159' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000159');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000160' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000160');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000161' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000161');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000162' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000162');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000163' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000163');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000164' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000164');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000165' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000165');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000166' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000166');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000167' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000167');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000168' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000168');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000169' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000169');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000170' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000170');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000171' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000171');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000172' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000172');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000173' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000173');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000174' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000174');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000175' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000175');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000176' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000176');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000177' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000177');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000178' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000178');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000179' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000179');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000180' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000180');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000181' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000181');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000182' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000182');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000183' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000183');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000184' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000184');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000185' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000185');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000186' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000186');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000187' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000187');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000188' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000188');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000189' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000189');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000190' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000190');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000191' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000191');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000192' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000192');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000193' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000193');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000194' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000194');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000195' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000195');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000196' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000196');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000197' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000197');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000198' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000198');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000199' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000199');
INSERT INTO codigo_barras (codigo_barras) SELECT '2099000000200' FROM DUAL
WHERE DATABASE() = 'inventario_key_qa' AND NOT EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000200');

-- -------------------------------------------------------------------------------------
-- 3. Productos
-- -------------------------------------------------------------------------------------
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Bolsa tote de piel sintética', 'Bolsa tote de piel sintética. Con cierre y forro interior. Artículo de prueba para QA.', 'Negro', 'Prueba QA', 159, 289, 249, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000001'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Bolsas'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000001')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000001');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Bolsa tote de lona', 'Bolsa tote de lona. Con cierre y forro interior. Artículo de prueba para QA.', 'Blanco', 'Prueba QA', 186, 339, 339, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000002'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Bolsas'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000002')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000002');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Bolsa tote tejida', 'Bolsa tote tejida. Con cierre y forro interior. Artículo de prueba para QA.', 'Beige', 'Prueba QA', 219, 399, 399, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000003'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Bolsas'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000003')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000003');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Bolsa tote acolchada', 'Bolsa tote acolchada. Con cierre y forro interior. Artículo de prueba para QA.', 'Camel', 'Prueba QA', 247, 449, 379, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000004'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Bolsas'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000004')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000004');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Bolsa tote de charol', 'Bolsa tote de charol. Con cierre y forro interior. Artículo de prueba para QA.', 'Café', 'Prueba QA', 274, 499, 499, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000005'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Bolsas'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000005')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000005');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Bolsa crossbody de piel sintética', 'Bolsa crossbody de piel sintética. Con cierre y forro interior. Artículo de prueba para QA.', 'Rojo', 'Prueba QA', 302, 549, 549, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000006'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Bolsas'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000006')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000006');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Bolsa crossbody de lona', 'Bolsa crossbody de lona. Con cierre y forro interior. Artículo de prueba para QA.', 'Vino', 'Prueba QA', 335, 609, 519, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000007'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Bolsas'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000007')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000007');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Bolsa crossbody tejida', 'Bolsa crossbody tejida. Con cierre y forro interior. Artículo de prueba para QA.', 'Rosa', 'Prueba QA', 164, 299, 299, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000008'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Bolsas'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000008')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000008');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Bolsa crossbody acolchada', 'Bolsa crossbody acolchada. Con cierre y forro interior. Artículo de prueba para QA.', 'Azul marino', 'Prueba QA', 192, 349, 349, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000009'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Bolsas'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000009')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000009');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Bolsa crossbody de charol', 'Bolsa crossbody de charol. Con cierre y forro interior. Artículo de prueba para QA.', 'Azul cielo', 'Prueba QA', 225, 409, 349, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000010'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Bolsas'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000010')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000010');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Bolsa clutch de piel sintética', 'Bolsa clutch de piel sintética. Con cierre y forro interior. Artículo de prueba para QA.', 'Verde olivo', 'Prueba QA', 252, 459, 459, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000011'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Bolsas'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000011')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000011');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Bolsa clutch de lona', 'Bolsa clutch de lona. Con cierre y forro interior. Artículo de prueba para QA.', 'Gris', 'Prueba QA', 280, 509, 509, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000012'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Bolsas'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000012')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000012');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Bolsa clutch tejida', 'Bolsa clutch tejida. Con cierre y forro interior. Artículo de prueba para QA.', 'Mostaza', 'Prueba QA', 307, 559, 479, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000013'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Bolsas'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000013')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000013');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Bolsa clutch acolchada', 'Bolsa clutch acolchada. Con cierre y forro interior. Artículo de prueba para QA.', 'Lila', 'Prueba QA', 340, 619, 619, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000014'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Bolsas'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000014')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000014');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Bolsa clutch de charol', 'Bolsa clutch de charol. Con cierre y forro interior. Artículo de prueba para QA.', 'Negro', 'Prueba QA', 170, 309, 309, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000015'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Bolsas'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000015')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000015');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Bolsa mochila de piel sintética', 'Bolsa mochila de piel sintética. Con cierre y forro interior. Artículo de prueba para QA.', 'Blanco', 'Prueba QA', 197, 359, 309, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000016'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Bolsas'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000016')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000016');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Bolsa mochila de lona', 'Bolsa mochila de lona. Con cierre y forro interior. Artículo de prueba para QA.', 'Beige', 'Prueba QA', 230, 419, 419, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000017'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Bolsas'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000017')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000017');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Bolsa mochila tejida', 'Bolsa mochila tejida. Con cierre y forro interior. Artículo de prueba para QA.', 'Camel', 'Prueba QA', 258, 469, 469, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000018'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Bolsas'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000018')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000018');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Bolsa mochila acolchada', 'Bolsa mochila acolchada. Con cierre y forro interior. Artículo de prueba para QA.', 'Café', 'Prueba QA', 285, 519, 439, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000019'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Bolsas'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000019')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000019');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Bolsa mochila de charol', 'Bolsa mochila de charol. Con cierre y forro interior. Artículo de prueba para QA.', 'Rojo', 'Prueba QA', 318, 579, 579, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000020'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Bolsas'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000020')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000020');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Bolsa bandolera de piel sintética', 'Bolsa bandolera de piel sintética. Con cierre y forro interior. Artículo de prueba para QA.', 'Vino', 'Prueba QA', 346, 629, 629, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000021'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Bolsas'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000021')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000021');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Bolsa bandolera de lona', 'Bolsa bandolera de lona. Con cierre y forro interior. Artículo de prueba para QA.', 'Rosa', 'Prueba QA', 175, 319, 269, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000022'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Bolsas'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000022')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000022');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Bolsa bandolera tejida', 'Bolsa bandolera tejida. Con cierre y forro interior. Artículo de prueba para QA.', 'Azul marino', 'Prueba QA', 208, 379, 379, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000023'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Bolsas'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000023')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000023');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Bolsa bandolera acolchada', 'Bolsa bandolera acolchada. Con cierre y forro interior. Artículo de prueba para QA.', 'Azul cielo', 'Prueba QA', 236, 429, 429, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000024'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Bolsas'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000024')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000024');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Bolsa bandolera de charol', 'Bolsa bandolera de charol. Con cierre y forro interior. Artículo de prueba para QA.', 'Verde olivo', 'Prueba QA', 263, 479, 409, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000025'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Bolsas'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000025')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000025');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Bolsa satchel de piel sintética', 'Bolsa satchel de piel sintética. Con cierre y forro interior. Artículo de prueba para QA.', 'Gris', 'Prueba QA', 291, 529, 529, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000026'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Bolsas'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000026')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000026');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Bolsa satchel de lona', 'Bolsa satchel de lona. Con cierre y forro interior. Artículo de prueba para QA.', 'Mostaza', 'Prueba QA', 324, 589, 589, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000027'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Bolsas'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000027')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000027');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Bolsa satchel tejida', 'Bolsa satchel tejida. Con cierre y forro interior. Artículo de prueba para QA.', 'Lila', 'Prueba QA', 351, 639, 539, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000028'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Bolsas'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000028')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000028');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Bolsa satchel acolchada', 'Bolsa satchel acolchada. Con cierre y forro interior. Artículo de prueba para QA.', 'Negro', 'Prueba QA', 181, 329, 329, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000029'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Bolsas'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000029')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000029');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Bolsa satchel de charol', 'Bolsa satchel de charol. Con cierre y forro interior. Artículo de prueba para QA.', 'Blanco', 'Prueba QA', 214, 389, 389, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000030'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Bolsas'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000030')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000030');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Bolsa hobo de piel sintética', 'Bolsa hobo de piel sintética. Con cierre y forro interior. Artículo de prueba para QA.', 'Beige', 'Prueba QA', 241, 439, 369, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000031'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Bolsas'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000031')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000031');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Bolsa hobo de lona', 'Bolsa hobo de lona. Con cierre y forro interior. Artículo de prueba para QA.', 'Camel', 'Prueba QA', 269, 489, 489, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000032'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Bolsas'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000032')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000032');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Bolsa hobo tejida', 'Bolsa hobo tejida. Con cierre y forro interior. Artículo de prueba para QA.', 'Café', 'Prueba QA', 296, 539, 539, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000033'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Bolsas'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000033')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000033');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Bolsa hobo acolchada', 'Bolsa hobo acolchada. Con cierre y forro interior. Artículo de prueba para QA.', 'Rojo', 'Prueba QA', 329, 599, 509, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000034'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Bolsas'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000034')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000034');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Bolsa hobo de charol', 'Bolsa hobo de charol. Con cierre y forro interior. Artículo de prueba para QA.', 'Vino', 'Prueba QA', 159, 289, 289, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000035'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Bolsas'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000035')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000035');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Bolsa bucket de piel sintética', 'Bolsa bucket de piel sintética. Con cierre y forro interior. Artículo de prueba para QA.', 'Rosa', 'Prueba QA', 186, 339, 339, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000036'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Bolsas'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000036')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000036');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Bolsa bucket de lona', 'Bolsa bucket de lona. Con cierre y forro interior. Artículo de prueba para QA.', 'Azul marino', 'Prueba QA', 219, 399, 339, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000037'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Bolsas'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000037')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000037');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Bolsa bucket tejida', 'Bolsa bucket tejida. Con cierre y forro interior. Artículo de prueba para QA.', 'Azul cielo', 'Prueba QA', 247, 449, 449, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000038'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Bolsas'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000038')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000038');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Bolsa bucket acolchada', 'Bolsa bucket acolchada. Con cierre y forro interior. Artículo de prueba para QA.', 'Verde olivo', 'Prueba QA', 274, 499, 499, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000039'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Bolsas'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000039')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000039');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Bolsa bucket de charol', 'Bolsa bucket de charol. Con cierre y forro interior. Artículo de prueba para QA.', 'Gris', 'Prueba QA', 307, 559, 479, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000040'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Bolsas'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000040')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000040');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Pantalón de mezclilla skinny tiro alto', 'Pantalón de mezclilla skinny tiro alto. Tela con buena caída y corte cómodo. Artículo de prueba para QA.', 'Mostaza', 'Prueba QA', 203, 369, 369, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000041'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Pantalones'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000041')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000041');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Pantalón de mezclilla skinny tiro medio', 'Pantalón de mezclilla skinny tiro medio. Tela con buena caída y corte cómodo. Artículo de prueba para QA.', 'Lila', 'Prueba QA', 230, 419, 419, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000042'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Pantalones'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000042')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000042');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Pantalón de mezclilla skinny stretch', 'Pantalón de mezclilla skinny stretch. Tela con buena caída y corte cómodo. Artículo de prueba para QA.', 'Negro', 'Prueba QA', 263, 479, 409, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000043'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Pantalones'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000043')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000043');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Pantalón de mezclilla skinny deslavado', 'Pantalón de mezclilla skinny deslavado. Tela con buena caída y corte cómodo. Artículo de prueba para QA.', 'Blanco', 'Prueba QA', 291, 529, 529, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000044'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Pantalones'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000044')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000044');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Pantalón de mezclilla skinny básico', 'Pantalón de mezclilla skinny básico. Tela con buena caída y corte cómodo. Artículo de prueba para QA.', 'Beige', 'Prueba QA', 318, 579, 579, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000045'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Pantalones'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000045')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000045');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Pantalón de mezclilla recto tiro alto', 'Pantalón de mezclilla recto tiro alto. Tela con buena caída y corte cómodo. Artículo de prueba para QA.', 'Camel', 'Prueba QA', 203, 369, 309, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000046'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Pantalones'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000046')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000046');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Pantalón de mezclilla recto tiro medio', 'Pantalón de mezclilla recto tiro medio. Tela con buena caída y corte cómodo. Artículo de prueba para QA.', 'Café', 'Prueba QA', 236, 429, 429, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000047'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Pantalones'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000047')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000047');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Pantalón de mezclilla recto stretch', 'Pantalón de mezclilla recto stretch. Tela con buena caída y corte cómodo. Artículo de prueba para QA.', 'Rojo', 'Prueba QA', 263, 479, 479, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000048'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Pantalones'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000048')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000048');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Pantalón de mezclilla recto deslavado', 'Pantalón de mezclilla recto deslavado. Tela con buena caída y corte cómodo. Artículo de prueba para QA.', 'Vino', 'Prueba QA', 291, 529, 449, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000049'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Pantalones'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000049')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000049');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Pantalón de mezclilla recto básico', 'Pantalón de mezclilla recto básico. Tela con buena caída y corte cómodo. Artículo de prueba para QA.', 'Rosa', 'Prueba QA', 324, 589, 589, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000050'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Pantalones'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000050')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000050');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Pantalón wide leg tiro alto', 'Pantalón wide leg tiro alto. Tela con buena caída y corte cómodo. Artículo de prueba para QA.', 'Azul marino', 'Prueba QA', 208, 379, 379, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000051'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Pantalones'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000051')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000051');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Pantalón wide leg tiro medio', 'Pantalón wide leg tiro medio. Tela con buena caída y corte cómodo. Artículo de prueba para QA.', 'Azul cielo', 'Prueba QA', 236, 429, 359, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000052'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Pantalones'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000052')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000052');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Pantalón wide leg stretch', 'Pantalón wide leg stretch. Tela con buena caída y corte cómodo. Artículo de prueba para QA.', 'Verde olivo', 'Prueba QA', 263, 479, 479, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000053'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Pantalones'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000053')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000053');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Pantalón wide leg deslavado', 'Pantalón wide leg deslavado. Tela con buena caída y corte cómodo. Artículo de prueba para QA.', 'Gris', 'Prueba QA', 296, 539, 539, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000054'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Pantalones'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000054')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000054');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Pantalón wide leg básico', 'Pantalón wide leg básico. Tela con buena caída y corte cómodo. Artículo de prueba para QA.', 'Mostaza', 'Prueba QA', 181, 329, 279, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000055'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Pantalones'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000055')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000055');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Pantalón palazzo tiro alto', 'Pantalón palazzo tiro alto. Tela con buena caída y corte cómodo. Artículo de prueba para QA.', 'Lila', 'Prueba QA', 208, 379, 379, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000056'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Pantalones'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000056')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000056');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Pantalón palazzo tiro medio', 'Pantalón palazzo tiro medio. Tela con buena caída y corte cómodo. Artículo de prueba para QA.', 'Negro', 'Prueba QA', 241, 439, 439, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000057'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Pantalones'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000057')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000057');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Pantalón palazzo stretch', 'Pantalón palazzo stretch. Tela con buena caída y corte cómodo. Artículo de prueba para QA.', 'Blanco', 'Prueba QA', 269, 489, 419, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000058'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Pantalones'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000058')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000058');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Pantalón palazzo deslavado', 'Pantalón palazzo deslavado. Tela con buena caída y corte cómodo. Artículo de prueba para QA.', 'Beige', 'Prueba QA', 296, 539, 539, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000059'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Pantalones'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000059')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000059');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Pantalón palazzo básico', 'Pantalón palazzo básico. Tela con buena caída y corte cómodo. Artículo de prueba para QA.', 'Camel', 'Prueba QA', 186, 339, 339, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000060'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Pantalones'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000060')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000060');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Pantalón cargo tiro alto', 'Pantalón cargo tiro alto. Tela con buena caída y corte cómodo. Artículo de prueba para QA.', 'Café', 'Prueba QA', 214, 389, 329, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000061'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Pantalones'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000061')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000061');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Pantalón cargo tiro medio', 'Pantalón cargo tiro medio. Tela con buena caída y corte cómodo. Artículo de prueba para QA.', 'Rojo', 'Prueba QA', 241, 439, 439, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000062'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Pantalones'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000062')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000062');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Pantalón cargo stretch', 'Pantalón cargo stretch. Tela con buena caída y corte cómodo. Artículo de prueba para QA.', 'Vino', 'Prueba QA', 274, 499, 499, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000063'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Pantalones'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000063')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000063');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Pantalón cargo deslavado', 'Pantalón cargo deslavado. Tela con buena caída y corte cómodo. Artículo de prueba para QA.', 'Rosa', 'Prueba QA', 302, 549, 469, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000064'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Pantalones'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000064')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000064');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Pantalón cargo básico', 'Pantalón cargo básico. Tela con buena caída y corte cómodo. Artículo de prueba para QA.', 'Azul marino', 'Prueba QA', 186, 339, 339, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000065'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Pantalones'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000065')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000065');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Pantalón jogger tiro alto', 'Pantalón jogger tiro alto. Tela con buena caída y corte cómodo. Artículo de prueba para QA.', 'Azul cielo', 'Prueba QA', 214, 389, 389, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000066'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Pantalones'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000066')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000066');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Pantalón jogger tiro medio', 'Pantalón jogger tiro medio. Tela con buena caída y corte cómodo. Artículo de prueba para QA.', 'Verde olivo', 'Prueba QA', 247, 449, 379, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000067'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Pantalones'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000067')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000067');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Pantalón jogger stretch', 'Pantalón jogger stretch. Tela con buena caída y corte cómodo. Artículo de prueba para QA.', 'Gris', 'Prueba QA', 274, 499, 499, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000068'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Pantalones'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000068')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000068');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Pantalón jogger deslavado', 'Pantalón jogger deslavado. Tela con buena caída y corte cómodo. Artículo de prueba para QA.', 'Mostaza', 'Prueba QA', 302, 549, 549, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000069'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Pantalones'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000069')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000069');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Pantalón jogger básico', 'Pantalón jogger básico. Tela con buena caída y corte cómodo. Artículo de prueba para QA.', 'Lila', 'Prueba QA', 192, 349, 299, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000070'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Pantalones'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000070')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000070');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Pantalón de vestir tiro alto', 'Pantalón de vestir tiro alto. Tela con buena caída y corte cómodo. Artículo de prueba para QA.', 'Negro', 'Prueba QA', 219, 399, 399, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000071'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Pantalones'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000071')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000071');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Pantalón de vestir tiro medio', 'Pantalón de vestir tiro medio. Tela con buena caída y corte cómodo. Artículo de prueba para QA.', 'Blanco', 'Prueba QA', 247, 449, 449, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000072'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Pantalones'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000072')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000072');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Pantalón de vestir stretch', 'Pantalón de vestir stretch. Tela con buena caída y corte cómodo. Artículo de prueba para QA.', 'Beige', 'Prueba QA', 274, 499, 419, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000073'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Pantalones'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000073')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000073');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Pantalón de vestir deslavado', 'Pantalón de vestir deslavado. Tela con buena caída y corte cómodo. Artículo de prueba para QA.', 'Camel', 'Prueba QA', 307, 559, 559, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000074'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Pantalones'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000074')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000074');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Pantalón de vestir básico', 'Pantalón de vestir básico. Tela con buena caída y corte cómodo. Artículo de prueba para QA.', 'Café', 'Prueba QA', 192, 349, 349, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000075'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Pantalones'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000075')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000075');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Pantalón acampanado tiro alto', 'Pantalón acampanado tiro alto. Tela con buena caída y corte cómodo. Artículo de prueba para QA.', 'Rojo', 'Prueba QA', 219, 399, 339, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000076'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Pantalones'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000076')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000076');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Pantalón acampanado tiro medio', 'Pantalón acampanado tiro medio. Tela con buena caída y corte cómodo. Artículo de prueba para QA.', 'Vino', 'Prueba QA', 252, 459, 459, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000077'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Pantalones'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000077')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000077');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Pantalón acampanado stretch', 'Pantalón acampanado stretch. Tela con buena caída y corte cómodo. Artículo de prueba para QA.', 'Rosa', 'Prueba QA', 280, 509, 509, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000078'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Pantalones'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000078')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000078');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Pantalón acampanado deslavado', 'Pantalón acampanado deslavado. Tela con buena caída y corte cómodo. Artículo de prueba para QA.', 'Azul marino', 'Prueba QA', 307, 559, 479, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000079'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Pantalones'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000079')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000079');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Pantalón acampanado básico', 'Pantalón acampanado básico. Tela con buena caída y corte cómodo. Artículo de prueba para QA.', 'Azul cielo', 'Prueba QA', 197, 359, 359, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000080'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Pantalones'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000080')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000080');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Falda lápiz satinada', 'Falda lápiz satinada. Cintura con cierre lateral. Artículo de prueba para QA.', 'Verde olivo', 'Prueba QA', 192, 349, 349, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000081'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Faldas'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000081')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000081');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Falda lápiz de mezclilla', 'Falda lápiz de mezclilla. Cintura con cierre lateral. Artículo de prueba para QA.', 'Gris', 'Prueba QA', 219, 399, 339, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000082'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Faldas'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000082')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000082');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Falda lápiz estampada', 'Falda lápiz estampada. Cintura con cierre lateral. Artículo de prueba para QA.', 'Mostaza', 'Prueba QA', 252, 459, 459, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000083'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Faldas'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000083')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000083');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Falda lápiz lisa', 'Falda lápiz lisa. Cintura con cierre lateral. Artículo de prueba para QA.', 'Lila', 'Prueba QA', 153, 279, 279, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000084'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Faldas'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000084')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000084');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Falda lápiz con abertura', 'Falda lápiz con abertura. Cintura con cierre lateral. Artículo de prueba para QA.', 'Negro', 'Prueba QA', 181, 329, 279, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000085'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Faldas'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000085')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000085');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Falda plisada satinada', 'Falda plisada satinada. Cintura con cierre lateral. Artículo de prueba para QA.', 'Blanco', 'Prueba QA', 208, 379, 379, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000086'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Faldas'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000086')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000086');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Falda plisada de mezclilla', 'Falda plisada de mezclilla. Cintura con cierre lateral. Artículo de prueba para QA.', 'Beige', 'Prueba QA', 241, 439, 439, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000087'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Faldas'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000087')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000087');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Falda plisada estampada', 'Falda plisada estampada. Cintura con cierre lateral. Artículo de prueba para QA.', 'Camel', 'Prueba QA', 142, 259, 219, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000088'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Faldas'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000088')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000088');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Falda plisada lisa', 'Falda plisada lisa. Cintura con cierre lateral. Artículo de prueba para QA.', 'Café', 'Prueba QA', 170, 309, 309, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000089'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Faldas'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000089')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000089');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Falda plisada con abertura', 'Falda plisada con abertura. Cintura con cierre lateral. Artículo de prueba para QA.', 'Rojo', 'Prueba QA', 203, 369, 369, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000090'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Faldas'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000090')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000090');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Falda midi satinada', 'Falda midi satinada. Cintura con cierre lateral. Artículo de prueba para QA.', 'Vino', 'Prueba QA', 230, 419, 359, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000091'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Faldas'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000091')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000091');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Falda midi de mezclilla', 'Falda midi de mezclilla. Cintura con cierre lateral. Artículo de prueba para QA.', 'Rosa', 'Prueba QA', 258, 469, 469, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000092'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Faldas'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000092')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000092');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Falda midi estampada', 'Falda midi estampada. Cintura con cierre lateral. Artículo de prueba para QA.', 'Azul marino', 'Prueba QA', 164, 299, 299, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000093'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Faldas'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000093')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000093');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Falda midi lisa', 'Falda midi lisa. Cintura con cierre lateral. Artículo de prueba para QA.', 'Azul cielo', 'Prueba QA', 192, 349, 299, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000094'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Faldas'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000094')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000094');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Falda midi con abertura', 'Falda midi con abertura. Cintura con cierre lateral. Artículo de prueba para QA.', 'Verde olivo', 'Prueba QA', 219, 399, 399, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000095'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Faldas'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000095')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000095');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Falda maxi satinada', 'Falda maxi satinada. Cintura con cierre lateral. Artículo de prueba para QA.', 'Gris', 'Prueba QA', 247, 449, 449, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000096'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Faldas'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000096')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000096');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Falda maxi de mezclilla', 'Falda maxi de mezclilla. Cintura con cierre lateral. Artículo de prueba para QA.', 'Mostaza', 'Prueba QA', 153, 279, 239, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000097'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Faldas'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000097')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000097');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Falda maxi estampada', 'Falda maxi estampada. Cintura con cierre lateral. Artículo de prueba para QA.', 'Lila', 'Prueba QA', 181, 329, 329, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000098'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Faldas'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000098')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000098');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Falda maxi lisa', 'Falda maxi lisa. Cintura con cierre lateral. Artículo de prueba para QA.', 'Negro', 'Prueba QA', 208, 379, 379, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000099'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Faldas'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000099')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000099');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Falda maxi con abertura', 'Falda maxi con abertura. Cintura con cierre lateral. Artículo de prueba para QA.', 'Blanco', 'Prueba QA', 241, 439, 369, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000100'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Faldas'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000100')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000100');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Falda mini satinada', 'Falda mini satinada. Cintura con cierre lateral. Artículo de prueba para QA.', 'Beige', 'Prueba QA', 142, 259, 259, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000101'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Faldas'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000101')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000101');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Falda mini de mezclilla', 'Falda mini de mezclilla. Cintura con cierre lateral. Artículo de prueba para QA.', 'Camel', 'Prueba QA', 170, 309, 309, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000102'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Faldas'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000102')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000102');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Falda mini estampada', 'Falda mini estampada. Cintura con cierre lateral. Artículo de prueba para QA.', 'Café', 'Prueba QA', 197, 359, 309, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000103'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Faldas'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000103')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000103');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Falda mini lisa', 'Falda mini lisa. Cintura con cierre lateral. Artículo de prueba para QA.', 'Rojo', 'Prueba QA', 230, 419, 419, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000104'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Faldas'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000104')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000104');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Falda mini con abertura', 'Falda mini con abertura. Cintura con cierre lateral. Artículo de prueba para QA.', 'Vino', 'Prueba QA', 258, 469, 469, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000105'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Faldas'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000105')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000105');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Falda línea A satinada', 'Falda línea A satinada. Cintura con cierre lateral. Artículo de prueba para QA.', 'Rosa', 'Prueba QA', 159, 289, 249, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000106'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Faldas'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000106')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000106');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Falda línea A de mezclilla', 'Falda línea A de mezclilla. Cintura con cierre lateral. Artículo de prueba para QA.', 'Azul marino', 'Prueba QA', 192, 349, 349, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000107'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Faldas'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000107')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000107');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Falda línea A estampada', 'Falda línea A estampada. Cintura con cierre lateral. Artículo de prueba para QA.', 'Azul cielo', 'Prueba QA', 219, 399, 399, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000108'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Faldas'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000108')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000108');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Falda línea A lisa', 'Falda línea A lisa. Cintura con cierre lateral. Artículo de prueba para QA.', 'Verde olivo', 'Prueba QA', 247, 449, 379, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000109'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Faldas'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000109')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000109');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Falda línea A con abertura', 'Falda línea A con abertura. Cintura con cierre lateral. Artículo de prueba para QA.', 'Gris', 'Prueba QA', 153, 279, 279, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000110'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Faldas'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000110')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000110');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Falda cruzada satinada', 'Falda cruzada satinada. Cintura con cierre lateral. Artículo de prueba para QA.', 'Mostaza', 'Prueba QA', 181, 329, 329, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000111'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Faldas'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000111')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000111');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Falda cruzada de mezclilla', 'Falda cruzada de mezclilla. Cintura con cierre lateral. Artículo de prueba para QA.', 'Lila', 'Prueba QA', 208, 379, 319, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000112'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Faldas'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000112')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000112');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Falda cruzada estampada', 'Falda cruzada estampada. Cintura con cierre lateral. Artículo de prueba para QA.', 'Negro', 'Prueba QA', 241, 439, 439, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000113'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Faldas'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000113')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000113');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Falda cruzada lisa', 'Falda cruzada lisa. Cintura con cierre lateral. Artículo de prueba para QA.', 'Blanco', 'Prueba QA', 142, 259, 259, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000114'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Faldas'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000114')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000114');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Falda cruzada con abertura', 'Falda cruzada con abertura. Cintura con cierre lateral. Artículo de prueba para QA.', 'Beige', 'Prueba QA', 170, 309, 259, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000115'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Faldas'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000115')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000115');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Blusa manga larga de gasa', 'Blusa manga larga de gasa. Tela ligera, ideal para el día. Artículo de prueba para QA.', 'Camel', 'Prueba QA', 170, 309, 309, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000116'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Blusas'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000116')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000116');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Blusa manga larga de algodón', 'Blusa manga larga de algodón. Tela ligera, ideal para el día. Artículo de prueba para QA.', 'Café', 'Prueba QA', 203, 369, 369, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000117'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Blusas'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000117')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000117');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Blusa manga larga satinada', 'Blusa manga larga satinada. Tela ligera, ideal para el día. Artículo de prueba para QA.', 'Rojo', 'Prueba QA', 120, 219, 189, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000118'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Blusas'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000118')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000118');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Blusa manga larga de encaje', 'Blusa manga larga de encaje. Tela ligera, ideal para el día. Artículo de prueba para QA.', 'Vino', 'Prueba QA', 148, 269, 269, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000119'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Blusas'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000119')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000119');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Blusa manga larga estampada', 'Blusa manga larga estampada. Tela ligera, ideal para el día. Artículo de prueba para QA.', 'Rosa', 'Prueba QA', 181, 329, 329, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000120'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Blusas'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000120')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000120');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Blusa manga corta de gasa', 'Blusa manga corta de gasa. Tela ligera, ideal para el día. Artículo de prueba para QA.', 'Azul marino', 'Prueba QA', 208, 379, 319, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000121'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Blusas'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000121')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000121');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Blusa manga corta de algodón', 'Blusa manga corta de algodón. Tela ligera, ideal para el día. Artículo de prueba para QA.', 'Azul cielo', 'Prueba QA', 126, 229, 229, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000122'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Blusas'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000122')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000122');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Blusa manga corta satinada', 'Blusa manga corta satinada. Tela ligera, ideal para el día. Artículo de prueba para QA.', 'Verde olivo', 'Prueba QA', 153, 279, 279, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000123'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Blusas'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000123')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000123');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Blusa manga corta de encaje', 'Blusa manga corta de encaje. Tela ligera, ideal para el día. Artículo de prueba para QA.', 'Gris', 'Prueba QA', 186, 339, 289, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000124'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Blusas'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000124')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000124');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Blusa manga corta estampada', 'Blusa manga corta estampada. Tela ligera, ideal para el día. Artículo de prueba para QA.', 'Mostaza', 'Prueba QA', 214, 389, 389, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000125'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Blusas'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000125')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000125');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Blusa sin mangas de gasa', 'Blusa sin mangas de gasa. Tela ligera, ideal para el día. Artículo de prueba para QA.', 'Lila', 'Prueba QA', 131, 239, 239, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000126'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Blusas'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000126')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000126');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Blusa sin mangas de algodón', 'Blusa sin mangas de algodón. Tela ligera, ideal para el día. Artículo de prueba para QA.', 'Negro', 'Prueba QA', 164, 299, 249, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000127'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Blusas'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000127')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000127');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Blusa sin mangas satinada', 'Blusa sin mangas satinada. Tela ligera, ideal para el día. Artículo de prueba para QA.', 'Blanco', 'Prueba QA', 192, 349, 349, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000128'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Blusas'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000128')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000128');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Blusa sin mangas de encaje', 'Blusa sin mangas de encaje. Tela ligera, ideal para el día. Artículo de prueba para QA.', 'Beige', 'Prueba QA', 219, 399, 399, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000129'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Blusas'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000129')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000129');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Blusa sin mangas estampada', 'Blusa sin mangas estampada. Tela ligera, ideal para el día. Artículo de prueba para QA.', 'Camel', 'Prueba QA', 142, 259, 219, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000130'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Blusas'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000130')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000130');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Blusa off shoulder de gasa', 'Blusa off shoulder de gasa. Tela ligera, ideal para el día. Artículo de prueba para QA.', 'Café', 'Prueba QA', 170, 309, 309, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000131'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Blusas'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000131')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000131');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Blusa off shoulder de algodón', 'Blusa off shoulder de algodón. Tela ligera, ideal para el día. Artículo de prueba para QA.', 'Rojo', 'Prueba QA', 197, 359, 359, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000132'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Blusas'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000132')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000132');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Blusa off shoulder satinada', 'Blusa off shoulder satinada. Tela ligera, ideal para el día. Artículo de prueba para QA.', 'Vino', 'Prueba QA', 230, 419, 359, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000133'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Blusas'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000133')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000133');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Blusa off shoulder de encaje', 'Blusa off shoulder de encaje. Tela ligera, ideal para el día. Artículo de prueba para QA.', 'Rosa', 'Prueba QA', 148, 269, 269, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000134'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Blusas'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000134')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000134');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Blusa off shoulder estampada', 'Blusa off shoulder estampada. Tela ligera, ideal para el día. Artículo de prueba para QA.', 'Azul marino', 'Prueba QA', 175, 319, 319, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000135'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Blusas'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000135')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000135');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Blusa cuello V de gasa', 'Blusa cuello V de gasa. Tela ligera, ideal para el día. Artículo de prueba para QA.', 'Azul cielo', 'Prueba QA', 203, 369, 309, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000136'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Blusas'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000136')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000136');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Blusa cuello V de algodón', 'Blusa cuello V de algodón. Tela ligera, ideal para el día. Artículo de prueba para QA.', 'Verde olivo', 'Prueba QA', 126, 229, 229, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000137'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Blusas'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000137')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000137');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Blusa cuello V satinada', 'Blusa cuello V satinada. Tela ligera, ideal para el día. Artículo de prueba para QA.', 'Gris', 'Prueba QA', 153, 279, 279, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000138'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Blusas'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000138')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000138');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Blusa cuello V de encaje', 'Blusa cuello V de encaje. Tela ligera, ideal para el día. Artículo de prueba para QA.', 'Mostaza', 'Prueba QA', 181, 329, 279, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000139'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Blusas'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000139')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000139');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Blusa cuello V estampada', 'Blusa cuello V estampada. Tela ligera, ideal para el día. Artículo de prueba para QA.', 'Lila', 'Prueba QA', 214, 389, 389, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000140'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Blusas'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000140')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000140');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Blusa crop de gasa', 'Blusa crop de gasa. Tela ligera, ideal para el día. Artículo de prueba para QA.', 'Negro', 'Prueba QA', 131, 239, 239, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000141'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Blusas'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000141')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000141');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Blusa crop de algodón', 'Blusa crop de algodón. Tela ligera, ideal para el día. Artículo de prueba para QA.', 'Blanco', 'Prueba QA', 159, 289, 249, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000142'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Blusas'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000142')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000142');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Blusa crop satinada', 'Blusa crop satinada. Tela ligera, ideal para el día. Artículo de prueba para QA.', 'Beige', 'Prueba QA', 186, 339, 339, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000143'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Blusas'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000143')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000143');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Blusa crop de encaje', 'Blusa crop de encaje. Tela ligera, ideal para el día. Artículo de prueba para QA.', 'Camel', 'Prueba QA', 219, 399, 399, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000144'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Blusas'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000144')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000144');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Blusa crop estampada', 'Blusa crop estampada. Tela ligera, ideal para el día. Artículo de prueba para QA.', 'Café', 'Prueba QA', 137, 249, 209, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000145'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Blusas'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000145')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000145');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Vestido midi floral', 'Vestido midi floral. Tela suave con forro. Artículo de prueba para QA.', 'Rojo', 'Prueba QA', 280, 509, 509, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000146'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Vestidos'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000146')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000146');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Vestido midi liso', 'Vestido midi liso. Tela suave con forro. Artículo de prueba para QA.', 'Vino', 'Prueba QA', 313, 569, 569, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000147'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Vestidos'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000147')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000147');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Vestido midi satinado', 'Vestido midi satinado. Tela suave con forro. Artículo de prueba para QA.', 'Rosa', 'Prueba QA', 340, 619, 529, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000148'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Vestidos'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000148')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000148');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Vestido midi de punto', 'Vestido midi de punto. Tela suave con forro. Artículo de prueba para QA.', 'Azul marino', 'Prueba QA', 368, 669, 669, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000149'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Vestidos'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000149')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000149');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Vestido midi de lino', 'Vestido midi de lino. Tela suave con forro. Artículo de prueba para QA.', 'Azul cielo', 'Prueba QA', 401, 729, 729, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000150'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Vestidos'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000150')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000150');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Vestido maxi floral', 'Vestido maxi floral. Tela suave con forro. Artículo de prueba para QA.', 'Verde olivo', 'Prueba QA', 230, 419, 359, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000151'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Vestidos'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000151')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000151');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Vestido maxi liso', 'Vestido maxi liso. Tela suave con forro. Artículo de prueba para QA.', 'Gris', 'Prueba QA', 258, 469, 469, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000152'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Vestidos'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000152')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000152');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Vestido maxi satinado', 'Vestido maxi satinado. Tela suave con forro. Artículo de prueba para QA.', 'Mostaza', 'Prueba QA', 285, 519, 519, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000153'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Vestidos'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000153')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000153');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Vestido maxi de punto', 'Vestido maxi de punto. Tela suave con forro. Artículo de prueba para QA.', 'Lila', 'Prueba QA', 318, 579, 489, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000154'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Vestidos'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000154')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000154');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Vestido maxi de lino', 'Vestido maxi de lino. Tela suave con forro. Artículo de prueba para QA.', 'Negro', 'Prueba QA', 346, 629, 629, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000155'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Vestidos'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000155')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000155');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Vestido corto floral', 'Vestido corto floral. Tela suave con forro. Artículo de prueba para QA.', 'Blanco', 'Prueba QA', 373, 679, 679, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000156'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Vestidos'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000156')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000156');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Vestido corto liso', 'Vestido corto liso. Tela suave con forro. Artículo de prueba para QA.', 'Beige', 'Prueba QA', 406, 739, 629, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000157'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Vestidos'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000157')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000157');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Vestido corto satinado', 'Vestido corto satinado. Tela suave con forro. Artículo de prueba para QA.', 'Camel', 'Prueba QA', 236, 429, 429, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000158'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Vestidos'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000158')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000158');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Vestido corto de punto', 'Vestido corto de punto. Tela suave con forro. Artículo de prueba para QA.', 'Café', 'Prueba QA', 263, 479, 479, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000159'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Vestidos'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000159')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000159');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Vestido corto de lino', 'Vestido corto de lino. Tela suave con forro. Artículo de prueba para QA.', 'Rojo', 'Prueba QA', 296, 539, 459, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000160'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Vestidos'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000160')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000160');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Vestido camisero floral', 'Vestido camisero floral. Tela suave con forro. Artículo de prueba para QA.', 'Vino', 'Prueba QA', 324, 589, 589, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000161'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Vestidos'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000161')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000161');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Vestido camisero liso', 'Vestido camisero liso. Tela suave con forro. Artículo de prueba para QA.', 'Rosa', 'Prueba QA', 351, 639, 639, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000162'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Vestidos'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000162')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000162');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Vestido camisero satinado', 'Vestido camisero satinado. Tela suave con forro. Artículo de prueba para QA.', 'Azul marino', 'Prueba QA', 384, 699, 589, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000163'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Vestidos'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000163')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000163');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Vestido camisero de punto', 'Vestido camisero de punto. Tela suave con forro. Artículo de prueba para QA.', 'Azul cielo', 'Prueba QA', 412, 749, 749, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000164'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Vestidos'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000164')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000164');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Vestido camisero de lino', 'Vestido camisero de lino. Tela suave con forro. Artículo de prueba para QA.', 'Verde olivo', 'Prueba QA', 241, 439, 439, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000165'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Vestidos'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000165')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000165');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Vestido tubo floral', 'Vestido tubo floral. Tela suave con forro. Artículo de prueba para QA.', 'Gris', 'Prueba QA', 269, 489, 419, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000166'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Vestidos'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000166')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000166');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Vestido tubo liso', 'Vestido tubo liso. Tela suave con forro. Artículo de prueba para QA.', 'Mostaza', 'Prueba QA', 302, 549, 549, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000167'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Vestidos'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000167')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000167');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Vestido tubo satinado', 'Vestido tubo satinado. Tela suave con forro. Artículo de prueba para QA.', 'Lila', 'Prueba QA', 329, 599, 599, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000168'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Vestidos'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000168')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000168');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Vestido tubo de punto', 'Vestido tubo de punto. Tela suave con forro. Artículo de prueba para QA.', 'Negro', 'Prueba QA', 357, 649, 549, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000169'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Vestidos'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000169')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000169');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Vestido tubo de lino', 'Vestido tubo de lino. Tela suave con forro. Artículo de prueba para QA.', 'Blanco', 'Prueba QA', 390, 709, 709, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000170'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Vestidos'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000170')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000170');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Vestido cruzado floral', 'Vestido cruzado floral. Tela suave con forro. Artículo de prueba para QA.', 'Beige', 'Prueba QA', 219, 399, 399, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000171'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Vestidos'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000171')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000171');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Vestido cruzado liso', 'Vestido cruzado liso. Tela suave con forro. Artículo de prueba para QA.', 'Camel', 'Prueba QA', 247, 449, 379, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000172'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Vestidos'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000172')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000172');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Vestido cruzado satinado', 'Vestido cruzado satinado. Tela suave con forro. Artículo de prueba para QA.', 'Café', 'Prueba QA', 274, 499, 499, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000173'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Vestidos'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000173')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000173');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Vestido cruzado de punto', 'Vestido cruzado de punto. Tela suave con forro. Artículo de prueba para QA.', 'Rojo', 'Prueba QA', 307, 559, 559, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000174'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Vestidos'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000174')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000174');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Vestido cruzado de lino', 'Vestido cruzado de lino. Tela suave con forro. Artículo de prueba para QA.', 'Vino', 'Prueba QA', 335, 609, 519, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000175'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Vestidos'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000175')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000175');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Cinturón trenzado', 'Cinturón trenzado. Ideal para regalo. Artículo de prueba para QA.', 'Rosa', 'Prueba QA', 137, 249, 249, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000176'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Accesorios'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000176')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000176');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Cinturón de hebilla dorada', 'Cinturón de hebilla dorada. Ideal para regalo. Artículo de prueba para QA.', 'Azul marino', 'Prueba QA', 65, 119, 119, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000177'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Accesorios'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000177')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000177');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Cinturón ancho elástico', 'Cinturón ancho elástico. Ideal para regalo. Artículo de prueba para QA.', 'Azul cielo', 'Prueba QA', 93, 169, 139, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000178'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Accesorios'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000178')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000178');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Cinturón delgado de charol', 'Cinturón delgado de charol. Ideal para regalo. Artículo de prueba para QA.', 'Verde olivo', 'Prueba QA', 120, 219, 219, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000179'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Accesorios'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000179')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000179');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Cinturón con argollas', 'Cinturón con argollas. Ideal para regalo. Artículo de prueba para QA.', 'Gris', 'Prueba QA', 153, 279, 279, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000180'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Accesorios'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000180')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000180');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Cartera larga con cierre', 'Cartera larga con cierre. Ideal para regalo. Artículo de prueba para QA.', 'Mostaza', 'Prueba QA', 76, 139, 119, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000181'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Accesorios'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000181')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000181');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Cartera compacta', 'Cartera compacta. Ideal para regalo. Artículo de prueba para QA.', 'Lila', 'Prueba QA', 104, 189, 189, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000182'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Accesorios'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000182')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000182');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Cartera tipo sobre', 'Cartera tipo sobre. Ideal para regalo. Artículo de prueba para QA.', 'Negro', 'Prueba QA', 131, 239, 239, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000183'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Accesorios'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000183')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000183');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Cartera acolchada', 'Cartera acolchada. Ideal para regalo. Artículo de prueba para QA.', 'Blanco', 'Prueba QA', 60, 109, 89, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000184'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Accesorios'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000184')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000184');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Cartera con correa', 'Cartera con correa. Ideal para regalo. Artículo de prueba para QA.', 'Beige', 'Prueba QA', 87, 159, 159, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000185'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Accesorios'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000185')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000185');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Monedero de piel sintética', 'Monedero de piel sintética. Ideal para regalo. Artículo de prueba para QA.', 'Camel', 'Prueba QA', 115, 209, 209, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000186'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Accesorios'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000186')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000186');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Monedero con broche', 'Monedero con broche. Ideal para regalo. Artículo de prueba para QA.', 'Café', 'Prueba QA', 148, 269, 229, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000187'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Accesorios'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000187')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000187');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Monedero bordado', 'Monedero bordado. Ideal para regalo. Artículo de prueba para QA.', 'Rojo', 'Prueba QA', 71, 129, 129, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000188'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Accesorios'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000188')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000188');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Monedero de charol', 'Monedero de charol. Ideal para regalo. Artículo de prueba para QA.', 'Vino', 'Prueba QA', 98, 179, 179, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000189'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Accesorios'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000189')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000189');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Monedero tejido', 'Monedero tejido. Ideal para regalo. Artículo de prueba para QA.', 'Rosa', 'Prueba QA', 131, 239, 199, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000190'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Accesorios'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000190')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000190');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Bufanda tejida', 'Bufanda tejida. Ideal para regalo. Artículo de prueba para QA.', 'Azul marino', 'Prueba QA', 54, 99, 99, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000191'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Accesorios'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000191')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000191');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Bufanda de punto grueso', 'Bufanda de punto grueso. Ideal para regalo. Artículo de prueba para QA.', 'Azul cielo', 'Prueba QA', 82, 149, 149, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000192'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Accesorios'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000192')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000192');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Bufanda a cuadros', 'Bufanda a cuadros. Ideal para regalo. Artículo de prueba para QA.', 'Verde olivo', 'Prueba QA', 109, 199, 169, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000193'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Accesorios'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000193')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000193');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Bufanda ligera', 'Bufanda ligera. Ideal para regalo. Artículo de prueba para QA.', 'Gris', 'Prueba QA', 142, 259, 259, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000194'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Accesorios'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000194')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000194');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Bufanda con flecos', 'Bufanda con flecos. Ideal para regalo. Artículo de prueba para QA.', 'Mostaza', 'Prueba QA', 65, 119, 119, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000195'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Accesorios'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000195')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000195');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Mascada de seda estampada', 'Mascada de seda estampada. Ideal para regalo. Artículo de prueba para QA.', 'Lila', 'Prueba QA', 93, 169, 139, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000196'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Accesorios'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000196')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000196');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Mascada lisa', 'Mascada lisa. Ideal para regalo. Artículo de prueba para QA.', 'Negro', 'Prueba QA', 126, 229, 229, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000197'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Accesorios'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000197')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000197');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Mascada de lunares', 'Mascada de lunares. Ideal para regalo. Artículo de prueba para QA.', 'Blanco', 'Prueba QA', 153, 279, 279, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000198'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Accesorios'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000198')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000198');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Mascada cuadrada', 'Mascada cuadrada. Ideal para regalo. Artículo de prueba para QA.', 'Beige', 'Prueba QA', 76, 139, 119, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000199'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Accesorios'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000199')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000199');
INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja, piezas, stock,
                      habilitado, es_catalogo_interno, codigo_barras_generado, codigo_barras_id, palabra_clave_id, fecha_creacion)
SELECT 'Mascada larga', 'Mascada larga. Ideal para regalo. Artículo de prueba para QA.', 'Camel', 'Prueba QA', 109, 199, 199, 10, 10,
       '1', 0, 0,
       (SELECT MIN(id) FROM codigo_barras WHERE codigo_barras = '2099000000200'),
       (SELECT MIN(id) FROM palabra_clave WHERE nombre = 'Accesorios'), NOW()
FROM DUAL
WHERE DATABASE() = 'inventario_key_qa'
  AND EXISTS (SELECT 1 FROM codigo_barras WHERE codigo_barras = '2099000000200')
  AND NOT EXISTS (SELECT 1 FROM producto pr JOIN codigo_barras cb ON cb.id = pr.codigo_barras_id WHERE cb.codigo_barras = '2099000000200');

-- -------------------------------------------------------------------------------------
-- 4. Artículos (variantes): 2 por producto, stock que suma 10
-- -------------------------------------------------------------------------------------
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'Única', 'Negro', p.descripcion, p.marca, 5, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000001' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'Única' AND x.color = 'Negro');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'Única', 'Camel', p.descripcion, p.marca, 5, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000001' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'Única' AND x.color = 'Camel');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'Única', 'Blanco', p.descripcion, p.marca, 6, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000002' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'Única' AND x.color = 'Blanco');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'Única', 'Azul marino', p.descripcion, p.marca, 4, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000002' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'Única' AND x.color = 'Azul marino');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'Única', 'Beige', p.descripcion, p.marca, 7, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000003' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'Única' AND x.color = 'Beige');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'Única', 'Lila', p.descripcion, p.marca, 3, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000003' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'Única' AND x.color = 'Lila');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'Única', 'Camel', p.descripcion, p.marca, 8, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000004' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'Única' AND x.color = 'Camel');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'Única', 'Café', p.descripcion, p.marca, 2, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000004' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'Única' AND x.color = 'Café');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'Única', 'Café', p.descripcion, p.marca, 9, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000005' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'Única' AND x.color = 'Café');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'Única', 'Azul cielo', p.descripcion, p.marca, 1, '1', p.palabra_clave_id, 549, 509, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000005' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'Única' AND x.color = 'Azul cielo');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'Única', 'Rojo', p.descripcion, p.marca, 10, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000006' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'Única' AND x.color = 'Rojo');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'Única', 'Negro', p.descripcion, p.marca, 0, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000006' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'Única' AND x.color = 'Negro');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'Única', 'Vino', p.descripcion, p.marca, 4, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000007' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'Única' AND x.color = 'Vino');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'Única', 'Rojo', p.descripcion, p.marca, 6, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000007' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'Única' AND x.color = 'Rojo');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'Única', 'Rosa', p.descripcion, p.marca, 3, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000008' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'Única' AND x.color = 'Rosa');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'Única', 'Verde olivo', p.descripcion, p.marca, 7, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000008' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'Única' AND x.color = 'Verde olivo');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'Única', 'Azul marino', p.descripcion, p.marca, 5, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000009' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'Única' AND x.color = 'Azul marino');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'Única', 'Blanco', p.descripcion, p.marca, 5, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000009' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'Única' AND x.color = 'Blanco');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'Única', 'Azul cielo', p.descripcion, p.marca, 6, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000010' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'Única' AND x.color = 'Azul cielo');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'Única', 'Vino', p.descripcion, p.marca, 4, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000010' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'Única' AND x.color = 'Vino');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'Única', 'Verde olivo', p.descripcion, p.marca, 7, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000011' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'Única' AND x.color = 'Verde olivo');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'Única', 'Gris', p.descripcion, p.marca, 3, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000011' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'Única' AND x.color = 'Gris');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'Única', 'Gris', p.descripcion, p.marca, 8, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000012' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'Única' AND x.color = 'Gris');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'Única', 'Beige', p.descripcion, p.marca, 2, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000012' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'Única' AND x.color = 'Beige');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'Única', 'Mostaza', p.descripcion, p.marca, 9, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000013' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'Única' AND x.color = 'Mostaza');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'Única', 'Rosa', p.descripcion, p.marca, 1, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000013' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'Única' AND x.color = 'Rosa');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'Única', 'Lila', p.descripcion, p.marca, 10, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000014' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'Única' AND x.color = 'Lila');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'Única', 'Mostaza', p.descripcion, p.marca, 0, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000014' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'Única' AND x.color = 'Mostaza');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'Única', 'Negro', p.descripcion, p.marca, 4, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000015' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'Única' AND x.color = 'Negro');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'Única', 'Camel', p.descripcion, p.marca, 6, '1', p.palabra_clave_id, 359, 359, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000015' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'Única' AND x.color = 'Camel');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'Única', 'Blanco', p.descripcion, p.marca, 3, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000016' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'Única' AND x.color = 'Blanco');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'Única', 'Azul marino', p.descripcion, p.marca, 7, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000016' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'Única' AND x.color = 'Azul marino');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'Única', 'Beige', p.descripcion, p.marca, 5, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000017' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'Única' AND x.color = 'Beige');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'Única', 'Lila', p.descripcion, p.marca, 5, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000017' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'Única' AND x.color = 'Lila');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'Única', 'Camel', p.descripcion, p.marca, 6, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000018' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'Única' AND x.color = 'Camel');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'Única', 'Café', p.descripcion, p.marca, 4, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000018' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'Única' AND x.color = 'Café');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'Única', 'Café', p.descripcion, p.marca, 7, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000019' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'Única' AND x.color = 'Café');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'Única', 'Azul cielo', p.descripcion, p.marca, 3, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000019' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'Única' AND x.color = 'Azul cielo');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'Única', 'Rojo', p.descripcion, p.marca, 8, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000020' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'Única' AND x.color = 'Rojo');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'Única', 'Negro', p.descripcion, p.marca, 2, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000020' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'Única' AND x.color = 'Negro');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'Única', 'Vino', p.descripcion, p.marca, 9, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000021' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'Única' AND x.color = 'Vino');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'Única', 'Rojo', p.descripcion, p.marca, 1, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000021' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'Única' AND x.color = 'Rojo');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'Única', 'Rosa', p.descripcion, p.marca, 10, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000022' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'Única' AND x.color = 'Rosa');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'Única', 'Verde olivo', p.descripcion, p.marca, 0, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000022' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'Única' AND x.color = 'Verde olivo');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'Única', 'Azul marino', p.descripcion, p.marca, 4, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000023' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'Única' AND x.color = 'Azul marino');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'Única', 'Blanco', p.descripcion, p.marca, 6, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000023' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'Única' AND x.color = 'Blanco');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'Única', 'Azul cielo', p.descripcion, p.marca, 3, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000024' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'Única' AND x.color = 'Azul cielo');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'Única', 'Vino', p.descripcion, p.marca, 7, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000024' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'Única' AND x.color = 'Vino');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'Única', 'Verde olivo', p.descripcion, p.marca, 5, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000025' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'Única' AND x.color = 'Verde olivo');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'Única', 'Gris', p.descripcion, p.marca, 5, '1', p.palabra_clave_id, 529, 489, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000025' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'Única' AND x.color = 'Gris');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'Única', 'Gris', p.descripcion, p.marca, 6, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000026' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'Única' AND x.color = 'Gris');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'Única', 'Beige', p.descripcion, p.marca, 4, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000026' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'Única' AND x.color = 'Beige');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'Única', 'Mostaza', p.descripcion, p.marca, 7, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000027' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'Única' AND x.color = 'Mostaza');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'Única', 'Rosa', p.descripcion, p.marca, 3, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000027' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'Única' AND x.color = 'Rosa');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'Única', 'Lila', p.descripcion, p.marca, 8, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000028' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'Única' AND x.color = 'Lila');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'Única', 'Mostaza', p.descripcion, p.marca, 2, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000028' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'Única' AND x.color = 'Mostaza');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'Única', 'Negro', p.descripcion, p.marca, 9, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000029' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'Única' AND x.color = 'Negro');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'Única', 'Camel', p.descripcion, p.marca, 1, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000029' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'Única' AND x.color = 'Camel');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'Única', 'Blanco', p.descripcion, p.marca, 10, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000030' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'Única' AND x.color = 'Blanco');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'Única', 'Azul marino', p.descripcion, p.marca, 0, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000030' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'Única' AND x.color = 'Azul marino');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'Única', 'Beige', p.descripcion, p.marca, 4, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000031' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'Única' AND x.color = 'Beige');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'Única', 'Lila', p.descripcion, p.marca, 6, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000031' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'Única' AND x.color = 'Lila');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'Única', 'Camel', p.descripcion, p.marca, 3, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000032' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'Única' AND x.color = 'Camel');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'Única', 'Café', p.descripcion, p.marca, 7, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000032' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'Única' AND x.color = 'Café');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'Única', 'Café', p.descripcion, p.marca, 5, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000033' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'Única' AND x.color = 'Café');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'Única', 'Azul cielo', p.descripcion, p.marca, 5, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000033' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'Única' AND x.color = 'Azul cielo');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'Única', 'Rojo', p.descripcion, p.marca, 6, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000034' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'Única' AND x.color = 'Rojo');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'Única', 'Negro', p.descripcion, p.marca, 4, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000034' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'Única' AND x.color = 'Negro');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'Única', 'Vino', p.descripcion, p.marca, 7, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000035' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'Única' AND x.color = 'Vino');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'Única', 'Rojo', p.descripcion, p.marca, 3, '1', p.palabra_clave_id, 339, 339, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000035' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'Única' AND x.color = 'Rojo');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'Única', 'Rosa', p.descripcion, p.marca, 8, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000036' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'Única' AND x.color = 'Rosa');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'Única', 'Verde olivo', p.descripcion, p.marca, 2, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000036' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'Única' AND x.color = 'Verde olivo');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'Única', 'Azul marino', p.descripcion, p.marca, 9, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000037' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'Única' AND x.color = 'Azul marino');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'Única', 'Blanco', p.descripcion, p.marca, 1, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000037' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'Única' AND x.color = 'Blanco');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'Única', 'Azul cielo', p.descripcion, p.marca, 10, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000038' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'Única' AND x.color = 'Azul cielo');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'Única', 'Vino', p.descripcion, p.marca, 0, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000038' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'Única' AND x.color = 'Vino');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'Única', 'Verde olivo', p.descripcion, p.marca, 4, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000039' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'Única' AND x.color = 'Verde olivo');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'Única', 'Gris', p.descripcion, p.marca, 6, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000039' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'Única' AND x.color = 'Gris');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'Única', 'Gris', p.descripcion, p.marca, 3, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000040' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'Única' AND x.color = 'Gris');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'Única', 'Beige', p.descripcion, p.marca, 7, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000040' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'Única' AND x.color = 'Beige');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, '28', 'Mostaza', p.descripcion, p.marca, 5, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000041' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = '28' AND x.color = 'Mostaza');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, '30', 'Mostaza', p.descripcion, p.marca, 5, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000041' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = '30' AND x.color = 'Mostaza');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, '30', 'Lila', p.descripcion, p.marca, 6, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000042' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = '30' AND x.color = 'Lila');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, '32', 'Lila', p.descripcion, p.marca, 4, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000042' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = '32' AND x.color = 'Lila');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, '32', 'Negro', p.descripcion, p.marca, 7, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000043' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = '32' AND x.color = 'Negro');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, '34', 'Negro', p.descripcion, p.marca, 3, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000043' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = '34' AND x.color = 'Negro');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, '34', 'Blanco', p.descripcion, p.marca, 8, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000044' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = '34' AND x.color = 'Blanco');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, '36', 'Blanco', p.descripcion, p.marca, 2, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000044' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = '36' AND x.color = 'Blanco');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, '28', 'Beige', p.descripcion, p.marca, 9, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000045' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = '28' AND x.color = 'Beige');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, '32', 'Beige', p.descripcion, p.marca, 1, '1', p.palabra_clave_id, 629, 589, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000045' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = '32' AND x.color = 'Beige');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, '28', 'Camel', p.descripcion, p.marca, 10, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000046' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = '28' AND x.color = 'Camel');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, '30', 'Camel', p.descripcion, p.marca, 0, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000046' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = '30' AND x.color = 'Camel');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, '30', 'Café', p.descripcion, p.marca, 4, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000047' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = '30' AND x.color = 'Café');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, '32', 'Café', p.descripcion, p.marca, 6, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000047' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = '32' AND x.color = 'Café');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, '32', 'Rojo', p.descripcion, p.marca, 3, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000048' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = '32' AND x.color = 'Rojo');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, '34', 'Rojo', p.descripcion, p.marca, 7, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000048' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = '34' AND x.color = 'Rojo');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, '34', 'Vino', p.descripcion, p.marca, 5, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000049' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = '34' AND x.color = 'Vino');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, '36', 'Vino', p.descripcion, p.marca, 5, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000049' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = '36' AND x.color = 'Vino');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, '28', 'Rosa', p.descripcion, p.marca, 6, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000050' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = '28' AND x.color = 'Rosa');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, '32', 'Rosa', p.descripcion, p.marca, 4, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000050' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = '32' AND x.color = 'Rosa');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, '28', 'Azul marino', p.descripcion, p.marca, 7, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000051' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = '28' AND x.color = 'Azul marino');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, '30', 'Azul marino', p.descripcion, p.marca, 3, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000051' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = '30' AND x.color = 'Azul marino');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, '30', 'Azul cielo', p.descripcion, p.marca, 8, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000052' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = '30' AND x.color = 'Azul cielo');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, '32', 'Azul cielo', p.descripcion, p.marca, 2, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000052' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = '32' AND x.color = 'Azul cielo');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, '32', 'Verde olivo', p.descripcion, p.marca, 9, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000053' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = '32' AND x.color = 'Verde olivo');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, '34', 'Verde olivo', p.descripcion, p.marca, 1, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000053' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = '34' AND x.color = 'Verde olivo');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, '34', 'Gris', p.descripcion, p.marca, 10, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000054' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = '34' AND x.color = 'Gris');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, '36', 'Gris', p.descripcion, p.marca, 0, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000054' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = '36' AND x.color = 'Gris');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, '28', 'Mostaza', p.descripcion, p.marca, 4, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000055' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = '28' AND x.color = 'Mostaza');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, '32', 'Mostaza', p.descripcion, p.marca, 6, '1', p.palabra_clave_id, 379, 379, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000055' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = '32' AND x.color = 'Mostaza');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, '28', 'Lila', p.descripcion, p.marca, 3, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000056' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = '28' AND x.color = 'Lila');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, '30', 'Lila', p.descripcion, p.marca, 7, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000056' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = '30' AND x.color = 'Lila');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, '30', 'Negro', p.descripcion, p.marca, 5, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000057' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = '30' AND x.color = 'Negro');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, '32', 'Negro', p.descripcion, p.marca, 5, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000057' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = '32' AND x.color = 'Negro');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, '32', 'Blanco', p.descripcion, p.marca, 6, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000058' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = '32' AND x.color = 'Blanco');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, '34', 'Blanco', p.descripcion, p.marca, 4, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000058' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = '34' AND x.color = 'Blanco');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, '34', 'Beige', p.descripcion, p.marca, 7, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000059' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = '34' AND x.color = 'Beige');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, '36', 'Beige', p.descripcion, p.marca, 3, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000059' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = '36' AND x.color = 'Beige');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, '28', 'Camel', p.descripcion, p.marca, 8, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000060' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = '28' AND x.color = 'Camel');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, '32', 'Camel', p.descripcion, p.marca, 2, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000060' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = '32' AND x.color = 'Camel');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, '28', 'Café', p.descripcion, p.marca, 9, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000061' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = '28' AND x.color = 'Café');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, '30', 'Café', p.descripcion, p.marca, 1, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000061' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = '30' AND x.color = 'Café');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, '30', 'Rojo', p.descripcion, p.marca, 10, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000062' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = '30' AND x.color = 'Rojo');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, '32', 'Rojo', p.descripcion, p.marca, 0, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000062' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = '32' AND x.color = 'Rojo');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, '32', 'Vino', p.descripcion, p.marca, 4, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000063' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = '32' AND x.color = 'Vino');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, '34', 'Vino', p.descripcion, p.marca, 6, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000063' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = '34' AND x.color = 'Vino');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, '34', 'Rosa', p.descripcion, p.marca, 3, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000064' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = '34' AND x.color = 'Rosa');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, '36', 'Rosa', p.descripcion, p.marca, 7, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000064' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = '36' AND x.color = 'Rosa');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, '28', 'Azul marino', p.descripcion, p.marca, 5, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000065' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = '28' AND x.color = 'Azul marino');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, '32', 'Azul marino', p.descripcion, p.marca, 5, '1', p.palabra_clave_id, 389, 349, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000065' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = '32' AND x.color = 'Azul marino');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, '28', 'Azul cielo', p.descripcion, p.marca, 6, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000066' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = '28' AND x.color = 'Azul cielo');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, '30', 'Azul cielo', p.descripcion, p.marca, 4, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000066' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = '30' AND x.color = 'Azul cielo');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, '30', 'Verde olivo', p.descripcion, p.marca, 7, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000067' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = '30' AND x.color = 'Verde olivo');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, '32', 'Verde olivo', p.descripcion, p.marca, 3, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000067' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = '32' AND x.color = 'Verde olivo');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, '32', 'Gris', p.descripcion, p.marca, 8, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000068' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = '32' AND x.color = 'Gris');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, '34', 'Gris', p.descripcion, p.marca, 2, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000068' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = '34' AND x.color = 'Gris');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, '34', 'Mostaza', p.descripcion, p.marca, 9, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000069' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = '34' AND x.color = 'Mostaza');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, '36', 'Mostaza', p.descripcion, p.marca, 1, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000069' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = '36' AND x.color = 'Mostaza');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, '28', 'Lila', p.descripcion, p.marca, 10, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000070' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = '28' AND x.color = 'Lila');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, '32', 'Lila', p.descripcion, p.marca, 0, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000070' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = '32' AND x.color = 'Lila');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, '28', 'Negro', p.descripcion, p.marca, 4, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000071' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = '28' AND x.color = 'Negro');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, '30', 'Negro', p.descripcion, p.marca, 6, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000071' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = '30' AND x.color = 'Negro');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, '30', 'Blanco', p.descripcion, p.marca, 3, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000072' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = '30' AND x.color = 'Blanco');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, '32', 'Blanco', p.descripcion, p.marca, 7, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000072' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = '32' AND x.color = 'Blanco');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, '32', 'Beige', p.descripcion, p.marca, 5, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000073' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = '32' AND x.color = 'Beige');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, '34', 'Beige', p.descripcion, p.marca, 5, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000073' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = '34' AND x.color = 'Beige');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, '34', 'Camel', p.descripcion, p.marca, 6, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000074' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = '34' AND x.color = 'Camel');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, '36', 'Camel', p.descripcion, p.marca, 4, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000074' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = '36' AND x.color = 'Camel');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, '28', 'Café', p.descripcion, p.marca, 7, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000075' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = '28' AND x.color = 'Café');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, '32', 'Café', p.descripcion, p.marca, 3, '1', p.palabra_clave_id, 399, 399, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000075' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = '32' AND x.color = 'Café');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, '28', 'Rojo', p.descripcion, p.marca, 8, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000076' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = '28' AND x.color = 'Rojo');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, '30', 'Rojo', p.descripcion, p.marca, 2, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000076' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = '30' AND x.color = 'Rojo');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, '30', 'Vino', p.descripcion, p.marca, 9, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000077' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = '30' AND x.color = 'Vino');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, '32', 'Vino', p.descripcion, p.marca, 1, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000077' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = '32' AND x.color = 'Vino');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, '32', 'Rosa', p.descripcion, p.marca, 10, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000078' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = '32' AND x.color = 'Rosa');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, '34', 'Rosa', p.descripcion, p.marca, 0, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000078' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = '34' AND x.color = 'Rosa');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, '34', 'Azul marino', p.descripcion, p.marca, 4, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000079' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = '34' AND x.color = 'Azul marino');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, '36', 'Azul marino', p.descripcion, p.marca, 6, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000079' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = '36' AND x.color = 'Azul marino');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, '28', 'Azul cielo', p.descripcion, p.marca, 3, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000080' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = '28' AND x.color = 'Azul cielo');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, '32', 'Azul cielo', p.descripcion, p.marca, 7, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000080' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = '32' AND x.color = 'Azul cielo');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'CH', 'Verde olivo', p.descripcion, p.marca, 5, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000081' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'CH' AND x.color = 'Verde olivo');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'M', 'Verde olivo', p.descripcion, p.marca, 5, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000081' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'M' AND x.color = 'Verde olivo');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'M', 'Gris', p.descripcion, p.marca, 6, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000082' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'M' AND x.color = 'Gris');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'G', 'Gris', p.descripcion, p.marca, 4, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000082' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'G' AND x.color = 'Gris');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'G', 'Mostaza', p.descripcion, p.marca, 7, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000083' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'G' AND x.color = 'Mostaza');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'EG', 'Mostaza', p.descripcion, p.marca, 3, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000083' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'EG' AND x.color = 'Mostaza');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'CH', 'Lila', p.descripcion, p.marca, 8, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000084' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'CH' AND x.color = 'Lila');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'G', 'Lila', p.descripcion, p.marca, 2, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000084' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'G' AND x.color = 'Lila');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'CH', 'Negro', p.descripcion, p.marca, 9, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000085' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'CH' AND x.color = 'Negro');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'M', 'Negro', p.descripcion, p.marca, 1, '1', p.palabra_clave_id, 379, 339, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000085' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'M' AND x.color = 'Negro');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'M', 'Blanco', p.descripcion, p.marca, 10, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000086' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'M' AND x.color = 'Blanco');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'G', 'Blanco', p.descripcion, p.marca, 0, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000086' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'G' AND x.color = 'Blanco');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'G', 'Beige', p.descripcion, p.marca, 4, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000087' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'G' AND x.color = 'Beige');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'EG', 'Beige', p.descripcion, p.marca, 6, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000087' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'EG' AND x.color = 'Beige');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'CH', 'Camel', p.descripcion, p.marca, 3, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000088' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'CH' AND x.color = 'Camel');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'G', 'Camel', p.descripcion, p.marca, 7, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000088' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'G' AND x.color = 'Camel');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'CH', 'Café', p.descripcion, p.marca, 5, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000089' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'CH' AND x.color = 'Café');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'M', 'Café', p.descripcion, p.marca, 5, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000089' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'M' AND x.color = 'Café');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'M', 'Rojo', p.descripcion, p.marca, 6, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000090' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'M' AND x.color = 'Rojo');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'G', 'Rojo', p.descripcion, p.marca, 4, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000090' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'G' AND x.color = 'Rojo');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'G', 'Vino', p.descripcion, p.marca, 7, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000091' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'G' AND x.color = 'Vino');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'EG', 'Vino', p.descripcion, p.marca, 3, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000091' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'EG' AND x.color = 'Vino');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'CH', 'Rosa', p.descripcion, p.marca, 8, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000092' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'CH' AND x.color = 'Rosa');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'G', 'Rosa', p.descripcion, p.marca, 2, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000092' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'G' AND x.color = 'Rosa');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'CH', 'Azul marino', p.descripcion, p.marca, 9, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000093' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'CH' AND x.color = 'Azul marino');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'M', 'Azul marino', p.descripcion, p.marca, 1, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000093' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'M' AND x.color = 'Azul marino');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'M', 'Azul cielo', p.descripcion, p.marca, 10, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000094' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'M' AND x.color = 'Azul cielo');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'G', 'Azul cielo', p.descripcion, p.marca, 0, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000094' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'G' AND x.color = 'Azul cielo');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'G', 'Verde olivo', p.descripcion, p.marca, 4, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000095' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'G' AND x.color = 'Verde olivo');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'EG', 'Verde olivo', p.descripcion, p.marca, 6, '1', p.palabra_clave_id, 449, 449, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000095' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'EG' AND x.color = 'Verde olivo');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'CH', 'Gris', p.descripcion, p.marca, 3, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000096' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'CH' AND x.color = 'Gris');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'G', 'Gris', p.descripcion, p.marca, 7, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000096' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'G' AND x.color = 'Gris');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'CH', 'Mostaza', p.descripcion, p.marca, 5, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000097' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'CH' AND x.color = 'Mostaza');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'M', 'Mostaza', p.descripcion, p.marca, 5, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000097' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'M' AND x.color = 'Mostaza');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'M', 'Lila', p.descripcion, p.marca, 6, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000098' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'M' AND x.color = 'Lila');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'G', 'Lila', p.descripcion, p.marca, 4, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000098' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'G' AND x.color = 'Lila');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'G', 'Negro', p.descripcion, p.marca, 7, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000099' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'G' AND x.color = 'Negro');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'EG', 'Negro', p.descripcion, p.marca, 3, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000099' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'EG' AND x.color = 'Negro');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'CH', 'Blanco', p.descripcion, p.marca, 8, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000100' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'CH' AND x.color = 'Blanco');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'G', 'Blanco', p.descripcion, p.marca, 2, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000100' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'G' AND x.color = 'Blanco');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'CH', 'Beige', p.descripcion, p.marca, 9, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000101' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'CH' AND x.color = 'Beige');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'M', 'Beige', p.descripcion, p.marca, 1, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000101' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'M' AND x.color = 'Beige');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'M', 'Camel', p.descripcion, p.marca, 10, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000102' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'M' AND x.color = 'Camel');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'G', 'Camel', p.descripcion, p.marca, 0, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000102' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'G' AND x.color = 'Camel');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'G', 'Café', p.descripcion, p.marca, 4, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000103' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'G' AND x.color = 'Café');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'EG', 'Café', p.descripcion, p.marca, 6, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000103' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'EG' AND x.color = 'Café');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'CH', 'Rojo', p.descripcion, p.marca, 3, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000104' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'CH' AND x.color = 'Rojo');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'G', 'Rojo', p.descripcion, p.marca, 7, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000104' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'G' AND x.color = 'Rojo');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'CH', 'Vino', p.descripcion, p.marca, 5, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000105' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'CH' AND x.color = 'Vino');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'M', 'Vino', p.descripcion, p.marca, 5, '1', p.palabra_clave_id, 519, 479, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000105' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'M' AND x.color = 'Vino');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'M', 'Rosa', p.descripcion, p.marca, 6, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000106' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'M' AND x.color = 'Rosa');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'G', 'Rosa', p.descripcion, p.marca, 4, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000106' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'G' AND x.color = 'Rosa');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'G', 'Azul marino', p.descripcion, p.marca, 7, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000107' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'G' AND x.color = 'Azul marino');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'EG', 'Azul marino', p.descripcion, p.marca, 3, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000107' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'EG' AND x.color = 'Azul marino');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'CH', 'Azul cielo', p.descripcion, p.marca, 8, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000108' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'CH' AND x.color = 'Azul cielo');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'G', 'Azul cielo', p.descripcion, p.marca, 2, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000108' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'G' AND x.color = 'Azul cielo');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'CH', 'Verde olivo', p.descripcion, p.marca, 9, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000109' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'CH' AND x.color = 'Verde olivo');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'M', 'Verde olivo', p.descripcion, p.marca, 1, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000109' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'M' AND x.color = 'Verde olivo');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'M', 'Gris', p.descripcion, p.marca, 10, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000110' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'M' AND x.color = 'Gris');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'G', 'Gris', p.descripcion, p.marca, 0, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000110' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'G' AND x.color = 'Gris');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'G', 'Mostaza', p.descripcion, p.marca, 4, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000111' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'G' AND x.color = 'Mostaza');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'EG', 'Mostaza', p.descripcion, p.marca, 6, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000111' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'EG' AND x.color = 'Mostaza');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'CH', 'Lila', p.descripcion, p.marca, 3, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000112' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'CH' AND x.color = 'Lila');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'G', 'Lila', p.descripcion, p.marca, 7, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000112' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'G' AND x.color = 'Lila');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'CH', 'Negro', p.descripcion, p.marca, 5, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000113' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'CH' AND x.color = 'Negro');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'M', 'Negro', p.descripcion, p.marca, 5, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000113' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'M' AND x.color = 'Negro');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'M', 'Blanco', p.descripcion, p.marca, 6, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000114' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'M' AND x.color = 'Blanco');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'G', 'Blanco', p.descripcion, p.marca, 4, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000114' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'G' AND x.color = 'Blanco');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'G', 'Beige', p.descripcion, p.marca, 7, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000115' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'G' AND x.color = 'Beige');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'EG', 'Beige', p.descripcion, p.marca, 3, '1', p.palabra_clave_id, 359, 359, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000115' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'EG' AND x.color = 'Beige');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'CH', 'Camel', p.descripcion, p.marca, 8, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000116' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'CH' AND x.color = 'Camel');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'G', 'Camel', p.descripcion, p.marca, 2, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000116' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'G' AND x.color = 'Camel');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'CH', 'Café', p.descripcion, p.marca, 9, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000117' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'CH' AND x.color = 'Café');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'M', 'Café', p.descripcion, p.marca, 1, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000117' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'M' AND x.color = 'Café');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'M', 'Rojo', p.descripcion, p.marca, 10, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000118' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'M' AND x.color = 'Rojo');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'G', 'Rojo', p.descripcion, p.marca, 0, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000118' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'G' AND x.color = 'Rojo');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'G', 'Vino', p.descripcion, p.marca, 4, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000119' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'G' AND x.color = 'Vino');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'EG', 'Vino', p.descripcion, p.marca, 6, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000119' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'EG' AND x.color = 'Vino');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'CH', 'Rosa', p.descripcion, p.marca, 3, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000120' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'CH' AND x.color = 'Rosa');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'G', 'Rosa', p.descripcion, p.marca, 7, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000120' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'G' AND x.color = 'Rosa');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'CH', 'Azul marino', p.descripcion, p.marca, 5, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000121' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'CH' AND x.color = 'Azul marino');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'M', 'Azul marino', p.descripcion, p.marca, 5, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000121' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'M' AND x.color = 'Azul marino');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'M', 'Azul cielo', p.descripcion, p.marca, 6, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000122' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'M' AND x.color = 'Azul cielo');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'G', 'Azul cielo', p.descripcion, p.marca, 4, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000122' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'G' AND x.color = 'Azul cielo');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'G', 'Verde olivo', p.descripcion, p.marca, 7, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000123' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'G' AND x.color = 'Verde olivo');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'EG', 'Verde olivo', p.descripcion, p.marca, 3, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000123' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'EG' AND x.color = 'Verde olivo');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'CH', 'Gris', p.descripcion, p.marca, 8, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000124' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'CH' AND x.color = 'Gris');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'G', 'Gris', p.descripcion, p.marca, 2, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000124' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'G' AND x.color = 'Gris');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'CH', 'Mostaza', p.descripcion, p.marca, 9, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000125' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'CH' AND x.color = 'Mostaza');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'M', 'Mostaza', p.descripcion, p.marca, 1, '1', p.palabra_clave_id, 439, 399, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000125' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'M' AND x.color = 'Mostaza');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'M', 'Lila', p.descripcion, p.marca, 10, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000126' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'M' AND x.color = 'Lila');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'G', 'Lila', p.descripcion, p.marca, 0, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000126' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'G' AND x.color = 'Lila');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'G', 'Negro', p.descripcion, p.marca, 4, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000127' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'G' AND x.color = 'Negro');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'EG', 'Negro', p.descripcion, p.marca, 6, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000127' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'EG' AND x.color = 'Negro');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'CH', 'Blanco', p.descripcion, p.marca, 3, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000128' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'CH' AND x.color = 'Blanco');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'G', 'Blanco', p.descripcion, p.marca, 7, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000128' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'G' AND x.color = 'Blanco');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'CH', 'Beige', p.descripcion, p.marca, 5, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000129' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'CH' AND x.color = 'Beige');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'M', 'Beige', p.descripcion, p.marca, 5, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000129' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'M' AND x.color = 'Beige');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'M', 'Camel', p.descripcion, p.marca, 6, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000130' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'M' AND x.color = 'Camel');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'G', 'Camel', p.descripcion, p.marca, 4, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000130' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'G' AND x.color = 'Camel');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'G', 'Café', p.descripcion, p.marca, 7, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000131' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'G' AND x.color = 'Café');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'EG', 'Café', p.descripcion, p.marca, 3, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000131' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'EG' AND x.color = 'Café');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'CH', 'Rojo', p.descripcion, p.marca, 8, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000132' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'CH' AND x.color = 'Rojo');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'G', 'Rojo', p.descripcion, p.marca, 2, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000132' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'G' AND x.color = 'Rojo');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'CH', 'Vino', p.descripcion, p.marca, 9, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000133' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'CH' AND x.color = 'Vino');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'M', 'Vino', p.descripcion, p.marca, 1, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000133' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'M' AND x.color = 'Vino');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'M', 'Rosa', p.descripcion, p.marca, 10, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000134' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'M' AND x.color = 'Rosa');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'G', 'Rosa', p.descripcion, p.marca, 0, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000134' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'G' AND x.color = 'Rosa');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'G', 'Azul marino', p.descripcion, p.marca, 4, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000135' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'G' AND x.color = 'Azul marino');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'EG', 'Azul marino', p.descripcion, p.marca, 6, '1', p.palabra_clave_id, 369, 369, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000135' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'EG' AND x.color = 'Azul marino');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'CH', 'Azul cielo', p.descripcion, p.marca, 3, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000136' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'CH' AND x.color = 'Azul cielo');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'G', 'Azul cielo', p.descripcion, p.marca, 7, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000136' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'G' AND x.color = 'Azul cielo');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'CH', 'Verde olivo', p.descripcion, p.marca, 5, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000137' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'CH' AND x.color = 'Verde olivo');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'M', 'Verde olivo', p.descripcion, p.marca, 5, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000137' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'M' AND x.color = 'Verde olivo');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'M', 'Gris', p.descripcion, p.marca, 6, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000138' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'M' AND x.color = 'Gris');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'G', 'Gris', p.descripcion, p.marca, 4, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000138' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'G' AND x.color = 'Gris');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'G', 'Mostaza', p.descripcion, p.marca, 7, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000139' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'G' AND x.color = 'Mostaza');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'EG', 'Mostaza', p.descripcion, p.marca, 3, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000139' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'EG' AND x.color = 'Mostaza');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'CH', 'Lila', p.descripcion, p.marca, 8, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000140' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'CH' AND x.color = 'Lila');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'G', 'Lila', p.descripcion, p.marca, 2, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000140' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'G' AND x.color = 'Lila');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'CH', 'Negro', p.descripcion, p.marca, 9, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000141' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'CH' AND x.color = 'Negro');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'M', 'Negro', p.descripcion, p.marca, 1, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000141' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'M' AND x.color = 'Negro');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'M', 'Blanco', p.descripcion, p.marca, 10, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000142' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'M' AND x.color = 'Blanco');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'G', 'Blanco', p.descripcion, p.marca, 0, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000142' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'G' AND x.color = 'Blanco');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'G', 'Beige', p.descripcion, p.marca, 4, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000143' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'G' AND x.color = 'Beige');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'EG', 'Beige', p.descripcion, p.marca, 6, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000143' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'EG' AND x.color = 'Beige');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'CH', 'Camel', p.descripcion, p.marca, 3, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000144' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'CH' AND x.color = 'Camel');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'G', 'Camel', p.descripcion, p.marca, 7, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000144' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'G' AND x.color = 'Camel');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'CH', 'Café', p.descripcion, p.marca, 5, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000145' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'CH' AND x.color = 'Café');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'M', 'Café', p.descripcion, p.marca, 5, '1', p.palabra_clave_id, 299, 259, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000145' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'M' AND x.color = 'Café');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'M', 'Rojo', p.descripcion, p.marca, 6, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000146' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'M' AND x.color = 'Rojo');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'G', 'Rojo', p.descripcion, p.marca, 4, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000146' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'G' AND x.color = 'Rojo');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'G', 'Vino', p.descripcion, p.marca, 7, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000147' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'G' AND x.color = 'Vino');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'EG', 'Vino', p.descripcion, p.marca, 3, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000147' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'EG' AND x.color = 'Vino');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'CH', 'Rosa', p.descripcion, p.marca, 8, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000148' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'CH' AND x.color = 'Rosa');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'G', 'Rosa', p.descripcion, p.marca, 2, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000148' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'G' AND x.color = 'Rosa');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'CH', 'Azul marino', p.descripcion, p.marca, 9, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000149' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'CH' AND x.color = 'Azul marino');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'M', 'Azul marino', p.descripcion, p.marca, 1, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000149' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'M' AND x.color = 'Azul marino');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'M', 'Azul cielo', p.descripcion, p.marca, 10, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000150' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'M' AND x.color = 'Azul cielo');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'G', 'Azul cielo', p.descripcion, p.marca, 0, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000150' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'G' AND x.color = 'Azul cielo');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'G', 'Verde olivo', p.descripcion, p.marca, 4, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000151' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'G' AND x.color = 'Verde olivo');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'EG', 'Verde olivo', p.descripcion, p.marca, 6, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000151' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'EG' AND x.color = 'Verde olivo');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'CH', 'Gris', p.descripcion, p.marca, 3, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000152' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'CH' AND x.color = 'Gris');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'G', 'Gris', p.descripcion, p.marca, 7, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000152' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'G' AND x.color = 'Gris');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'CH', 'Mostaza', p.descripcion, p.marca, 5, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000153' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'CH' AND x.color = 'Mostaza');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'M', 'Mostaza', p.descripcion, p.marca, 5, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000153' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'M' AND x.color = 'Mostaza');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'M', 'Lila', p.descripcion, p.marca, 6, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000154' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'M' AND x.color = 'Lila');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'G', 'Lila', p.descripcion, p.marca, 4, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000154' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'G' AND x.color = 'Lila');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'G', 'Negro', p.descripcion, p.marca, 7, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000155' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'G' AND x.color = 'Negro');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'EG', 'Negro', p.descripcion, p.marca, 3, '1', p.palabra_clave_id, 679, 679, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000155' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'EG' AND x.color = 'Negro');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'CH', 'Blanco', p.descripcion, p.marca, 8, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000156' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'CH' AND x.color = 'Blanco');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'G', 'Blanco', p.descripcion, p.marca, 2, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000156' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'G' AND x.color = 'Blanco');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'CH', 'Beige', p.descripcion, p.marca, 9, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000157' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'CH' AND x.color = 'Beige');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'M', 'Beige', p.descripcion, p.marca, 1, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000157' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'M' AND x.color = 'Beige');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'M', 'Camel', p.descripcion, p.marca, 10, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000158' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'M' AND x.color = 'Camel');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'G', 'Camel', p.descripcion, p.marca, 0, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000158' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'G' AND x.color = 'Camel');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'G', 'Café', p.descripcion, p.marca, 4, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000159' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'G' AND x.color = 'Café');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'EG', 'Café', p.descripcion, p.marca, 6, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000159' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'EG' AND x.color = 'Café');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'CH', 'Rojo', p.descripcion, p.marca, 3, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000160' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'CH' AND x.color = 'Rojo');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'G', 'Rojo', p.descripcion, p.marca, 7, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000160' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'G' AND x.color = 'Rojo');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'CH', 'Vino', p.descripcion, p.marca, 5, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000161' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'CH' AND x.color = 'Vino');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'M', 'Vino', p.descripcion, p.marca, 5, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000161' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'M' AND x.color = 'Vino');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'M', 'Rosa', p.descripcion, p.marca, 6, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000162' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'M' AND x.color = 'Rosa');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'G', 'Rosa', p.descripcion, p.marca, 4, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000162' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'G' AND x.color = 'Rosa');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'G', 'Azul marino', p.descripcion, p.marca, 7, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000163' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'G' AND x.color = 'Azul marino');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'EG', 'Azul marino', p.descripcion, p.marca, 3, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000163' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'EG' AND x.color = 'Azul marino');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'CH', 'Azul cielo', p.descripcion, p.marca, 8, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000164' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'CH' AND x.color = 'Azul cielo');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'G', 'Azul cielo', p.descripcion, p.marca, 2, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000164' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'G' AND x.color = 'Azul cielo');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'CH', 'Verde olivo', p.descripcion, p.marca, 9, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000165' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'CH' AND x.color = 'Verde olivo');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'M', 'Verde olivo', p.descripcion, p.marca, 1, '1', p.palabra_clave_id, 489, 449, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000165' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'M' AND x.color = 'Verde olivo');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'M', 'Gris', p.descripcion, p.marca, 10, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000166' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'M' AND x.color = 'Gris');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'G', 'Gris', p.descripcion, p.marca, 0, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000166' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'G' AND x.color = 'Gris');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'G', 'Mostaza', p.descripcion, p.marca, 4, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000167' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'G' AND x.color = 'Mostaza');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'EG', 'Mostaza', p.descripcion, p.marca, 6, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000167' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'EG' AND x.color = 'Mostaza');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'CH', 'Lila', p.descripcion, p.marca, 3, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000168' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'CH' AND x.color = 'Lila');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'G', 'Lila', p.descripcion, p.marca, 7, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000168' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'G' AND x.color = 'Lila');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'CH', 'Negro', p.descripcion, p.marca, 5, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000169' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'CH' AND x.color = 'Negro');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'M', 'Negro', p.descripcion, p.marca, 5, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000169' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'M' AND x.color = 'Negro');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'M', 'Blanco', p.descripcion, p.marca, 6, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000170' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'M' AND x.color = 'Blanco');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'G', 'Blanco', p.descripcion, p.marca, 4, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000170' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'G' AND x.color = 'Blanco');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'G', 'Beige', p.descripcion, p.marca, 7, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000171' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'G' AND x.color = 'Beige');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'EG', 'Beige', p.descripcion, p.marca, 3, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000171' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'EG' AND x.color = 'Beige');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'CH', 'Camel', p.descripcion, p.marca, 8, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000172' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'CH' AND x.color = 'Camel');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'G', 'Camel', p.descripcion, p.marca, 2, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000172' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'G' AND x.color = 'Camel');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'CH', 'Café', p.descripcion, p.marca, 9, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000173' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'CH' AND x.color = 'Café');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'M', 'Café', p.descripcion, p.marca, 1, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000173' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'M' AND x.color = 'Café');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'M', 'Rojo', p.descripcion, p.marca, 10, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000174' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'M' AND x.color = 'Rojo');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'G', 'Rojo', p.descripcion, p.marca, 0, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000174' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'G' AND x.color = 'Rojo');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'G', 'Vino', p.descripcion, p.marca, 4, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000175' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'G' AND x.color = 'Vino');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'EG', 'Vino', p.descripcion, p.marca, 6, '1', p.palabra_clave_id, 659, 659, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000175' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'EG' AND x.color = 'Vino');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'Única', 'Rosa', p.descripcion, p.marca, 3, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000176' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'Única' AND x.color = 'Rosa');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'Única', 'Verde olivo', p.descripcion, p.marca, 7, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000176' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'Única' AND x.color = 'Verde olivo');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'Única', 'Azul marino', p.descripcion, p.marca, 5, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000177' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'Única' AND x.color = 'Azul marino');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'Única', 'Blanco', p.descripcion, p.marca, 5, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000177' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'Única' AND x.color = 'Blanco');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'Única', 'Azul cielo', p.descripcion, p.marca, 6, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000178' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'Única' AND x.color = 'Azul cielo');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'Única', 'Vino', p.descripcion, p.marca, 4, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000178' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'Única' AND x.color = 'Vino');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'Única', 'Verde olivo', p.descripcion, p.marca, 7, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000179' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'Única' AND x.color = 'Verde olivo');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'Única', 'Gris', p.descripcion, p.marca, 3, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000179' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'Única' AND x.color = 'Gris');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'Única', 'Gris', p.descripcion, p.marca, 8, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000180' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'Única' AND x.color = 'Gris');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'Única', 'Beige', p.descripcion, p.marca, 2, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000180' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'Única' AND x.color = 'Beige');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'Única', 'Mostaza', p.descripcion, p.marca, 9, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000181' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'Única' AND x.color = 'Mostaza');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'Única', 'Rosa', p.descripcion, p.marca, 1, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000181' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'Única' AND x.color = 'Rosa');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'Única', 'Lila', p.descripcion, p.marca, 10, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000182' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'Única' AND x.color = 'Lila');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'Única', 'Mostaza', p.descripcion, p.marca, 0, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000182' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'Única' AND x.color = 'Mostaza');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'Única', 'Negro', p.descripcion, p.marca, 4, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000183' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'Única' AND x.color = 'Negro');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'Única', 'Camel', p.descripcion, p.marca, 6, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000183' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'Única' AND x.color = 'Camel');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'Única', 'Blanco', p.descripcion, p.marca, 3, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000184' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'Única' AND x.color = 'Blanco');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'Única', 'Azul marino', p.descripcion, p.marca, 7, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000184' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'Única' AND x.color = 'Azul marino');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'Única', 'Beige', p.descripcion, p.marca, 5, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000185' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'Única' AND x.color = 'Beige');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'Única', 'Lila', p.descripcion, p.marca, 5, '1', p.palabra_clave_id, 209, 169, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000185' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'Única' AND x.color = 'Lila');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'Única', 'Camel', p.descripcion, p.marca, 6, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000186' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'Única' AND x.color = 'Camel');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'Única', 'Café', p.descripcion, p.marca, 4, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000186' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'Única' AND x.color = 'Café');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'Única', 'Café', p.descripcion, p.marca, 7, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000187' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'Única' AND x.color = 'Café');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'Única', 'Azul cielo', p.descripcion, p.marca, 3, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000187' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'Única' AND x.color = 'Azul cielo');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'Única', 'Rojo', p.descripcion, p.marca, 8, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000188' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'Única' AND x.color = 'Rojo');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'Única', 'Negro', p.descripcion, p.marca, 2, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000188' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'Única' AND x.color = 'Negro');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'Única', 'Vino', p.descripcion, p.marca, 9, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000189' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'Única' AND x.color = 'Vino');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'Única', 'Rojo', p.descripcion, p.marca, 1, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000189' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'Única' AND x.color = 'Rojo');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'Única', 'Rosa', p.descripcion, p.marca, 10, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000190' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'Única' AND x.color = 'Rosa');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'Única', 'Verde olivo', p.descripcion, p.marca, 0, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000190' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'Única' AND x.color = 'Verde olivo');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'Única', 'Azul marino', p.descripcion, p.marca, 4, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000191' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'Única' AND x.color = 'Azul marino');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'Única', 'Blanco', p.descripcion, p.marca, 6, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000191' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'Única' AND x.color = 'Blanco');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'Única', 'Azul cielo', p.descripcion, p.marca, 3, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000192' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'Única' AND x.color = 'Azul cielo');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'Única', 'Vino', p.descripcion, p.marca, 7, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000192' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'Única' AND x.color = 'Vino');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'Única', 'Verde olivo', p.descripcion, p.marca, 5, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000193' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'Única' AND x.color = 'Verde olivo');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'Única', 'Gris', p.descripcion, p.marca, 5, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000193' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'Única' AND x.color = 'Gris');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'Única', 'Gris', p.descripcion, p.marca, 6, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000194' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'Única' AND x.color = 'Gris');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'Única', 'Beige', p.descripcion, p.marca, 4, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000194' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'Única' AND x.color = 'Beige');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'Única', 'Mostaza', p.descripcion, p.marca, 7, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000195' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'Única' AND x.color = 'Mostaza');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'Única', 'Rosa', p.descripcion, p.marca, 3, '1', p.palabra_clave_id, 169, 169, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000195' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'Única' AND x.color = 'Rosa');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'Única', 'Lila', p.descripcion, p.marca, 8, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000196' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'Única' AND x.color = 'Lila');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'Única', 'Mostaza', p.descripcion, p.marca, 2, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000196' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'Única' AND x.color = 'Mostaza');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'Única', 'Negro', p.descripcion, p.marca, 9, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000197' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'Única' AND x.color = 'Negro');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'Única', 'Camel', p.descripcion, p.marca, 1, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000197' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'Única' AND x.color = 'Camel');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'Única', 'Blanco', p.descripcion, p.marca, 10, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000198' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'Única' AND x.color = 'Blanco');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'Única', 'Azul marino', p.descripcion, p.marca, 0, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000198' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'Única' AND x.color = 'Azul marino');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'Única', 'Beige', p.descripcion, p.marca, 4, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000199' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'Única' AND x.color = 'Beige');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'Única', 'Lila', p.descripcion, p.marca, 6, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000199' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'Única' AND x.color = 'Lila');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'Única', 'Camel', p.descripcion, p.marca, 3, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000200' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'Única' AND x.color = 'Camel');
INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, palabra_clave_id,
                       precio_venta, precio_rebaja, fecha_creacion)
SELECT p.id, 'Única', 'Café', p.descripcion, p.marca, 7, '1', p.palabra_clave_id, NULL, NULL, NOW()
FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras = '2099000000200' AND DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variantes x WHERE x.producto_id = p.id AND x.talla = 'Única' AND x.color = 'Café');

-- -------------------------------------------------------------------------------------
-- 5. Imágenes: se reusan hasta 100 imágenes de productos reales habilitados, repartidas en orden
-- -------------------------------------------------------------------------------------
DROP TEMPORARY TABLE IF EXISTS tmp_prueba_imagen;
CREATE TEMPORARY TABLE tmp_prueba_imagen (rn INT AUTO_INCREMENT PRIMARY KEY, imagen_id BIGINT NOT NULL);
INSERT INTO tmp_prueba_imagen (imagen_id)
SELECT DISTINCT vi.imagen_id
FROM variante_imagen vi
JOIN variantes v ON v.id = vi.variante_id
JOIN producto p ON p.id = v.producto_id
LEFT JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE vi.imagen_id IS NOT NULL AND v.habilitado = '1' AND p.habilitado = '1'
  AND (cb.codigo_barras IS NULL OR cb.codigo_barras NOT LIKE '2099000%')
ORDER BY vi.imagen_id
LIMIT 100;

DROP TEMPORARY TABLE IF EXISTS tmp_prueba_variante;
CREATE TEMPORARY TABLE tmp_prueba_variante (k INT AUTO_INCREMENT PRIMARY KEY, variante_id INT NOT NULL);
INSERT INTO tmp_prueba_variante (variante_id)
SELECT v.id
FROM variantes v JOIN producto p ON p.id = v.producto_id JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras LIKE '2099000%' AND LENGTH(cb.codigo_barras) = 13
ORDER BY v.id;

SELECT COUNT(*) INTO @n_img FROM tmp_prueba_imagen;

INSERT INTO variante_imagen (variante_id, imagen_id, principal)
SELECT t.variante_id, i.imagen_id, 1
FROM tmp_prueba_variante t
JOIN tmp_prueba_imagen i ON i.rn = MOD(t.k - 1, @n_img) + 1
WHERE DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM variante_imagen x WHERE x.variante_id = t.variante_id);

-- Miniatura del listado de productos: la imagen del primer artículo
INSERT INTO producto_imagen_copy (producto_id, imagen_id, principal)
SELECT v.producto_id, MIN(vi.imagen_id), 1
FROM tmp_prueba_variante t
JOIN variantes v ON v.id = t.variante_id
JOIN variante_imagen vi ON vi.variante_id = v.id
WHERE DATABASE() = 'inventario_key_qa'
  AND NOT EXISTS (SELECT 1 FROM producto_imagen_copy x WHERE x.producto_id = v.producto_id)
GROUP BY v.producto_id;

DROP TEMPORARY TABLE IF EXISTS tmp_prueba_variante;
DROP TEMPORARY TABLE IF EXISTS tmp_prueba_imagen;

COMMIT;

-- =====================================================================================
-- VERIFICACIÓN — cada consulta dice qué número se espera
-- =====================================================================================

-- Esperado: productos = 200, articulos = 400
SELECT COUNT(DISTINCT p.id) AS productos, COUNT(v.id) AS articulos
FROM producto p
JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
LEFT JOIN variantes v ON v.producto_id = p.id
WHERE cb.codigo_barras LIKE '2099000%' AND LENGTH(cb.codigo_barras) = 13;

-- Esperado: 0 filas (todo producto con stock 10 = suma de sus 2 artículos)
SELECT p.id, p.nombre, p.stock, SUM(v.stock) AS suma_articulos, COUNT(v.id) AS articulos
FROM producto p
JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
LEFT JOIN variantes v ON v.producto_id = p.id
WHERE cb.codigo_barras LIKE '2099000%' AND LENGTH(cb.codigo_barras) = 13
GROUP BY p.id, p.nombre, p.stock
HAVING p.stock <> 10 OR SUM(v.stock) <> 10 OR COUNT(v.id) <> 2;

-- Esperado: agotados = 25, con_precio_propio = 20, sin_imagen = 0
-- (si sin_imagen = 400, QA no tiene ninguna imagen de producto real que reusar)
SELECT SUM(v.stock = 0)                AS agotados,
       SUM(v.precio_venta IS NOT NULL) AS con_precio_propio,
       SUM(NOT EXISTS (SELECT 1 FROM variante_imagen vi WHERE vi.variante_id = v.id)) AS sin_imagen
FROM variantes v
JOIN producto p ON p.id = v.producto_id
JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras LIKE '2099000%' AND LENGTH(cb.codigo_barras) = 13;

-- Esperado: 375 (mismas 4 condiciones que la tienda pública)
SELECT COUNT(*) AS visibles_en_tienda
FROM variantes v
JOIN producto p ON p.id = v.producto_id
JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
WHERE cb.codigo_barras LIKE '2099000%' AND LENGTH(cb.codigo_barras) = 13
  AND v.stock > 0 AND p.habilitado = '1' AND v.habilitado = '1' AND p.es_catalogo_interno = 0
  AND EXISTS (SELECT 1 FROM variante_imagen vi WHERE vi.variante_id = v.id);
