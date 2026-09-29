-- ============================================================
-- Migracion: filtros guardados por usuario (2026-09-29, dominio hexagonal preferenciafiltro)
--
-- tienda/buscar y productos/buscar recordaban los filtros solo en memoria: se perdian al
-- recargar, cerrar sesion o cambiar de dispositivo. Ahora cada persona del personal (no los
-- clientes) tiene los suyos guardados, uno por pantalla.
--
-- Backend: /v1/preferencias-filtro/{pantalla} (PreferenciaFiltroController).
--
-- Sin esta tabla la app arranca igual (ddl-auto: none), pero guardar/leer filtros responde 500
-- y la pantalla sigue funcionando solo con la memoria, como antes.
--
-- Idempotente: CREATE TABLE IF NOT EXISTS. Correr primero en inventario_key_qa (cubre dev y qa)
-- y despues en inventario_key (prod). No agrega permisos: usa el de ver cada pantalla.
--
-- OJO: la entidad Usuario mapea a usuario_modificacion, no a usuarios.
-- ============================================================

CREATE TABLE IF NOT EXISTS preferencia_filtro (
    id           INT          NOT NULL AUTO_INCREMENT,
    usuario_id   INT          NOT NULL,
    pantalla     VARCHAR(40)  NOT NULL COMMENT 'tienda-buscar | productos-buscar',
    filtros      TEXT         NOT NULL COMMENT 'Objeto JSON con los filtros, sin texto buscado ni pagina',
    actualizado  DATETIME     NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_preferencia_filtro (usuario_id, pantalla),
    CONSTRAINT fk_preferencia_filtro_usuario FOREIGN KEY (usuario_id)
        REFERENCES usuario_modificacion (id) ON DELETE CASCADE
);

-- Verificacion: debe devolver 1 fila con la tabla.
SELECT table_name, create_time
FROM information_schema.tables
WHERE table_schema = DATABASE() AND table_name = 'preferencia_filtro';
