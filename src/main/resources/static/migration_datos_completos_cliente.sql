-- ============================================================
-- MÓDULO: Unificar verificación de correo Usuario/Cliente (mejora 15, PLAN_MEJORAS.md)
-- ============================================================
-- Conectarse al servidor
-- sudo mysql -u root -p

-- Seleccionar la base según el ambiente:
-- use inventario_key_qa;   ← dev y qa (misma BD)
-- use inventario_key;      ← main / prod
--
-- Estado por ambiente: back ya implementado en dev (2026-07-03) — pendiente correr en dev/qa/prod.
-- Ver PLAN_MEJORAS.md sección 15 para el diseño completo.
-- Correr junto con migration_usuario_verificacion_correo.sql (mismo módulo).
-- ============================================================

ALTER TABLE clientes
    ADD COLUMN datos_completos TINYINT(1) NOT NULL DEFAULT 0
        COMMENT '0 = falta nombre/apellido paterno/telefono (cliente auto-creado solo con correo al registrar usuario), 1 = perfil completo',
    ADD COLUMN correo_pendiente VARCHAR(150) NULL
        COMMENT 'Correo nuevo escrito por el cliente, esperando verificacion. correo_electronico NO cambia hasta que se verifique este valor.';

-- Backfill: clientes ya existentes (creados por el flujo viejo, antes de esta mejora)
-- que ya tienen los 4 campos obligatorios llenos quedan marcados como completos.
UPDATE clientes
SET datos_completos = 1
WHERE nombre_persona IS NOT NULL AND nombre_persona <> ''
  AND apeido_paterno IS NOT NULL AND apeido_paterno <> ''
  AND numero_telefonico IS NOT NULL AND numero_telefonico <> ''
  AND correo_electronico IS NOT NULL AND correo_electronico <> '';

-- Nota: NO hace falta ningún ALTER para apeido_materno (pasa a opcional, ver PLAN_MEJORAS.md) —
-- la columna ya es NULLABLE en BD desde siempre, la obligatoriedad era solo @NotBlank/@NotNull
-- en Cliente.java (back). Basta con quitar esas anotaciones en el código, sin tocar la tabla.
