-- ============================================================
-- Migracion: códigos postales guardados (2026-10-14, dominio hexagonal codigopostal)
--
-- Mis datos y ver cliente llenan el municipio con el código postal, y cada vez se le preguntaba al
-- servicio externo (que a veces no contesta o rechaza la llave). Ahora se busca primero aquí y
-- solo si el código no está (o tiene más de un año) se le pregunta al externo y se guarda.
--
-- Backend: GET /v1/dipomex/getCodigoPostal/{cp} (CodigoPostalController). Entidad
-- CodigoPostalGuardado.
--
-- Sin esta tabla la app arranca igual (ddl-auto: none) y el código postal sigue funcionando
-- como antes, preguntándole al servicio externo cada vez (el log avisa que no pudo leer la tabla).
--
-- Idempotente: CREATE TABLE IF NOT EXISTS. Correr primero en inventario_key_qa (cubre dev y qa)
-- y despues en inventario_key (prod). No agrega permisos.
-- ============================================================

CREATE TABLE IF NOT EXISTS codigo_postal (
    id          INT          NOT NULL AUTO_INCREMENT,
    codigo      VARCHAR(5)   NOT NULL COMMENT 'Los 5 dígitos',
    municipio   VARCHAR(150) NOT NULL,
    estado      VARCHAR(150) NULL,
    colonias    TEXT         NOT NULL COMMENT 'Arreglo JSON de textos, por ejemplo ["Centro","Valle"]',
    consultado  DATETIME     NOT NULL COMMENT 'Cuándo se le preguntó al servicio externo; con más de un año se vuelve a preguntar',
    PRIMARY KEY (id),
    UNIQUE KEY uq_codigo_postal (codigo)
);

-- Verificacion: debe devolver 1 fila con la tabla.
SELECT table_name, create_time
FROM information_schema.tables
WHERE table_schema = DATABASE() AND table_name = 'codigo_postal';
