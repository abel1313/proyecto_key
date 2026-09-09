-- ============================================================
-- Migración: Boletos de rifa por acción en redes sociales
-- Un boleto por cada acción distinta (seguir, compartir, etc.) que hace
-- un concursante ya registrado -- el nombre/datos del participante NO se
-- repiten, se referencian una sola vez vía concursante_id.
-- ============================================================

CREATE TABLE boletos_rifa (
    id                      INT AUTO_INCREMENT PRIMARY KEY,
    concursante_id          INT         NOT NULL,
    motivo                  VARCHAR(200) NULL,
    fecha                   DATE        NOT NULL,
    url_perfil_red_social   VARCHAR(500) NULL,
    url_seguimiento         VARCHAR(500) NULL,
    CONSTRAINT fk_boletos_rifa_concursante
        FOREIGN KEY (concursante_id) REFERENCES concursantes(id)
);

-- Evidencia de varias publicaciones compartidas para un mismo boleto
CREATE TABLE boleto_rifa_url_compartido (
    boleto_rifa_id INT          NOT NULL,
    url            VARCHAR(500) NOT NULL,
    CONSTRAINT fk_boleto_rifa_url_boleto
        FOREIGN KEY (boleto_rifa_id) REFERENCES boletos_rifa(id) ON DELETE CASCADE
);

-- ============================================================
-- VERIFICACIÓN
-- ============================================================
-- SHOW CREATE TABLE boletos_rifa;
-- SHOW CREATE TABLE boleto_rifa_url_compartido;
