-- ============================================================
-- MÓDULO: Verificación de correo del cliente (código de 6 dígitos)
-- ============================================================
-- Conectarse al servidor
-- sudo mysql -u root -p

-- Seleccionar la base según el ambiente:
-- use inventario_key_qa;   ← dev y qa (misma BD)
-- use inventario_key;      ← main / prod
--
-- Estado por ambiente (2026-07-02): pendiente de correr en los 3 ambientes.
-- ============================================================

ALTER TABLE clientes
    ADD COLUMN correo_verificado          TINYINT(1) NOT NULL DEFAULT 0
        COMMENT '0 = correo sin verificar, 1 = verificado con codigo',
    ADD COLUMN codigo_verificacion        VARCHAR(6)  NULL
        COMMENT 'Codigo de 6 digitos vigente, null si ya se verifico o no se ha solicitado',
    ADD COLUMN codigo_verificacion_expira DATETIME    NULL
        COMMENT 'Vencimiento del codigo (15 minutos desde el envio)';

-- Nota: correo_electronico y numero_telefonico ya existian como columnas NULLABLE
-- y siguen asi a nivel de BD (no se agrega NOT NULL) — la obligatoriedad se valida
-- en el back con @NotBlank en Cliente.java, solo aplica a registros nuevos/editados.
-- Los clientes existentes con esos campos vacios no se rompen, pero no podran
-- generar pedidos nuevos ni verificar su correo hasta completarlos.
