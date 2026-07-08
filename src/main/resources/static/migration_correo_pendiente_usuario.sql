-- ============================================================
-- MÓDULO: Cambio de correo con verificación previa (admin y self-service) — PLAN_MEJORAS.md seccion 17
-- ============================================================
-- Conectarse al servidor
-- sudo mysql -u root -p

-- Seleccionar la base según el ambiente:
-- use inventario_key_qa;   ← dev y qa (misma BD)
-- use inventario_key;      ← main / prod
--
-- Correo pendiente de confirmar: el email real (usuario_modificacion.email) no cambia hasta que
-- el codigo enviado a correo_pendiente se valida correctamente. Si el codigo nunca se confirma
-- (o el admin/usuario cancela), correo_pendiente se descarta y el email real nunca se toco.
-- Reutiliza las columnas ya existentes codigo_verificacion / codigo_verificacion_expira
-- (migration_usuario_verificacion_correo.sql) — no hace falta duplicarlas.
-- ============================================================

ALTER TABLE usuario_modificacion
    ADD COLUMN correo_pendiente VARCHAR(150) NULL
        COMMENT 'Correo nuevo esperando verificacion; null si no hay cambio de correo en curso';
