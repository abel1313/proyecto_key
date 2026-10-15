-- ============================================================
-- Migración: datos legales del negocio y aceptación de Términos (2026-10-07)
--
-- LEGAL_PLAN_DE_ACCION.md, puntos 4 y 16:
--   1. Tabla datos_legales_negocio (una sola fila, id = 1): nombre del responsable, RFC, domicilio,
--      teléfono, correo y horario de atención. La LFPC art. 76 bis III pide mostrarlos antes de
--      comprar. Los captura el dueño en Configuración del negocio → Datos legales. Nace con el
--      correo que ya se mostraba (contacto@novedades-jade.com.mx) y lo demás vacío.
--   2. usuario_modificacion (entidad Usuario): acepto_terminos + fecha_acepto_terminos, para
--      guardar cuándo aceptó los Términos al registrarse (igual que la privacidad).
--
-- Correr ANTES del back que la usa: la entidad Usuario ya mapea las columnas nuevas y sin ellas
-- falla el login. Idempotente. No cambia ningún dato existente (las cuentas viejas quedan en 0).
-- ============================================================

CREATE TABLE IF NOT EXISTS datos_legales_negocio (
    id                  INT          NOT NULL COMMENT 'Siempre 1: una sola fila',
    nombre_responsable  VARCHAR(150) NULL COMMENT 'Persona física o empresa que vende',
    rfc                 VARCHAR(13)  NULL,
    domicilio           VARCHAR(300) NULL COMMENT 'Domicilio físico para reclamaciones (LFPC 76 bis III)',
    telefono            VARCHAR(20)  NULL COMMENT 'Solo dígitos, 10',
    correo              VARCHAR(150) NULL,
    horario_atencion    VARCHAR(150) NULL,
    actualizado_en      DATETIME     NULL,
    PRIMARY KEY (id)
);

INSERT INTO datos_legales_negocio (id, correo, actualizado_en)
SELECT 1, 'contacto@novedades-jade.com.mx', NOW()
FROM (SELECT 1) AS dummy
WHERE NOT EXISTS (SELECT 1 FROM datos_legales_negocio WHERE id = 1);

SET @sql = IF((SELECT COUNT(*) FROM information_schema.columns
               WHERE table_schema = DATABASE() AND table_name = 'usuario_modificacion'
                 AND column_name = 'acepto_terminos') = 0,
    'ALTER TABLE usuario_modificacion ADD COLUMN acepto_terminos BIT(1) NOT NULL DEFAULT 0',
    'SELECT ''acepto_terminos ya existe''');
PREPARE st FROM @sql; EXECUTE st; DEALLOCATE PREPARE st;

SET @sql = IF((SELECT COUNT(*) FROM information_schema.columns
               WHERE table_schema = DATABASE() AND table_name = 'usuario_modificacion'
                 AND column_name = 'fecha_acepto_terminos') = 0,
    'ALTER TABLE usuario_modificacion ADD COLUMN fecha_acepto_terminos DATETIME NULL',
    'SELECT ''fecha_acepto_terminos ya existe''');
PREPARE st FROM @sql; EXECUTE st; DEALLOCATE PREPARE st;

-- ── Verificación ────────────────────────────────────────────────────────────────────────────
-- 1) Una fila con id = 1 y el correo:
SELECT id, nombre_responsable, domicilio, telefono, correo FROM datos_legales_negocio;
-- 2) Las dos columnas nuevas en usuario_modificacion (deben salir 2 filas):
SELECT column_name, column_type, is_nullable, column_default FROM information_schema.columns
WHERE table_schema = DATABASE() AND table_name = 'usuario_modificacion'
  AND column_name IN ('acepto_terminos', 'fecha_acepto_terminos');
