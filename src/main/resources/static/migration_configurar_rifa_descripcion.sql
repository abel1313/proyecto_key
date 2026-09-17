-- ============================================================
-- MODULO: Descripción/Reglas en configuración de rifa
-- ============================================================
-- Conectarse al servidor
-- sudo mysql -u root -p

-- Seleccionar la base segun el ambiente:
-- use inventario_key_qa;   <- dev y qa (misma BD)
-- use inventario_key;      <- main / prod
--
-- Campo TEXT para guardar la descripción, reglas o términos de la rifa
-- que se mostrarán en la página pública de la ruleta bajo la imagen del premio.
-- ============================================================

ALTER TABLE configurar_rifa
    ADD COLUMN descripcion TEXT NULL AFTER es_prueba;
