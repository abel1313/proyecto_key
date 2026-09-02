-- Aviso de privacidad al registrarse (Usuario) + preferencia de correos no transaccionales (Cliente).
-- Ver INVESTIGACION_NUEVAS_FEATURES_2026-09-02.md, seccion 4.

ALTER TABLE usuario_modificacion
    ADD COLUMN acepto_privacidad BIT(1) NOT NULL DEFAULT 0,
    ADD COLUMN fecha_acepto_privacidad DATETIME NULL;

ALTER TABLE clientes
    ADD COLUMN recibir_correos BIT(1) NOT NULL DEFAULT 1;
