-- ============================================================
-- Catálogo de logos subidos por el admin (LogoService/LogoController).
-- Ejecutar manualmente en la BD de cada ambiente (ddl-auto: none).
-- Un solo logo puede estar activo=1 a la vez (se fuerza a nivel de aplicación en
-- LogoService.activar(), no con constraint de BD).
-- ============================================================

CREATE TABLE logo (
    id               INT NOT NULL AUTO_INCREMENT,
    nombre_archivo   VARCHAR(300) NOT NULL,
    extension        VARCHAR(10)  NULL,
    nombre_original  VARCHAR(200) NULL,
    activo           TINYINT(1)   NOT NULL DEFAULT 0,
    creado_en        DATETIME     NULL,
    PRIMARY KEY (id)
);

CREATE INDEX idx_logo_activo ON logo (activo);
