-- ============================================================
-- Migración: reportes generados en segundo plano (2026-11-10) — reporte por categoría / por productos elegidos.
--
-- El administrador pide el reporte (Excel o PDF), el back lo arma en segundo plano y lo deja guardado aquí, ya terminado, para descargarlo.
-- Se borra solo cuando vence (7 días, job nocturno ReporteLimpiezaScheduler). El archivo se guarda en binario (LONGBLOB).
-- Idempotente: se puede correr dos veces. Sin esta tabla el reporte en pantalla funciona; solo falla pedir el Excel/PDF.
-- ============================================================

CREATE TABLE IF NOT EXISTS reporte_generado (
    id             INT          NOT NULL AUTO_INCREMENT,
    usuario        VARCHAR(100) NOT NULL,
    estado         VARCHAR(15)  NOT NULL,
    formato        VARCHAR(5)   NOT NULL,
    descripcion    VARCHAR(500) NULL,
    nombre_archivo VARCHAR(150) NULL,
    contenido      LONGBLOB     NULL,
    tamano         BIGINT       NULL,
    error          VARCHAR(500) NULL,
    creado         DATETIME     NOT NULL,
    terminado      DATETIME     NULL,
    expira         DATETIME     NOT NULL,
    PRIMARY KEY (id),
    KEY idx_reporte_generado_usuario (usuario, id),
    KEY idx_reporte_generado_expira (expira)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Verificación:
-- SHOW CREATE TABLE reporte_generado;
