-- ============================================================
-- MODULO: Instagram y TikTok en contactos del negocio
-- ============================================================
-- Conectarse al servidor
-- sudo mysql -u root -p

-- Seleccionar la base segun el ambiente:
-- use inventario_key_qa;   <- dev y qa (misma BD)
-- use inventario_key;      <- main / prod
--
-- Contrato completo en CAMBIOS_FRONT.md. Mismo tratamiento que whatsapp_url/facebook_url ya
-- tienen hoy: texto libre nullable, sin formato forzado.
-- ============================================================

ALTER TABLE configuracion_negocio
    ADD COLUMN instagram_url VARCHAR(500) NULL AFTER facebook_url,
    ADD COLUMN tiktok_url VARCHAR(500) NULL AFTER instagram_url;
