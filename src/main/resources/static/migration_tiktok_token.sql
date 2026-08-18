-- Migracion 2026-08-18: tabla tiktok_token -- fila unica (id=1) para guardar el access_token +
-- refresh_token de la cuenta de TikTok del negocio. A diferencia del token de Facebook (no
-- expira), el de TikTok dura ~24h y el refresh token rota en cada uso -- por eso vive en una
-- tabla que el back puede actualizar solo, no en un yml estatico. Se llena la primera vez con
-- POST /v1/redes-sociales/tiktok/autorizar (ver TikTokGraphClient.autorizarConCode), despues de
-- que el admin haga el login OAuth en el navegador (paso 4 de TIKTOK_SETUP.md).
CREATE TABLE IF NOT EXISTS tiktok_token (
    id INT PRIMARY KEY,
    access_token VARCHAR(500),
    refresh_token VARCHAR(500),
    open_id VARCHAR(100),
    access_token_expira_en DATETIME,
    refresh_token_expira_en DATETIME,
    actualizado_en DATETIME
);
