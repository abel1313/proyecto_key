-- ============================================================
-- MODULO: Fotos del ramo ya armado (variante "sombra" en ramo_armado)
-- ============================================================
-- Conectarse al servidor
-- sudo mysql -u root -p

-- Seleccionar la base segun el ambiente:
-- use inventario_key_qa;   <- dev y qa (misma BD)
-- use inventario_key;      <- main / prod
--
-- Agrega la variante "sombra" a ramo_armado para poder reusar el sistema de fotos de variantes
-- ya existente (POST /tienda/v1/guardarConImagenes, GET /tienda/v1/imagenes/{varianteId}) sin
-- construir nada nuevo. Esta variante nunca se vende ni aparece en un pedido -- solo sirve para
-- colgarle fotos del ramo ya armado. Contrato completo en CAMBIOS_FRONT.md.
--
-- Los ramos ya existentes en BD quedan con variante_id NULL hasta que se vuelvan a guardar
-- (POST/PUT) una vez desplegado este cambio -- ahi el back crea la variante sola.
-- ============================================================

ALTER TABLE ramo_armado
    ADD COLUMN variante_id INT NULL AFTER imagen_url,
    ADD CONSTRAINT fk_ramo_armado_variante FOREIGN KEY (variante_id) REFERENCES variantes(id);
