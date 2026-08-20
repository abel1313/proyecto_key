-- Migracion 2026-08-20: bot de mensajes directos (DM) de Instagram.
-- Ejecutar manualmente en la BD de cada ambiente (ddl-auto: none).
-- Mismo proposito que comentario_social/comentario_pausa pero para mensajeria en vez de
-- comentarios publicos -- ver InstagramDirectMessageBotService.

-- mensaje_directo_social: registra cada DM procesado.
-- 1) Idempotencia: Meta puede reenviar el mismo evento de webhook mas de una vez; con mid UNIQUE
--    no se responde duplicado.
-- 2) Saber si es la "primera vez" que ese autor_id escribe (saludo de bienvenida obligatorio).
-- 3) respuesta_mid: guarda el mid que devolvio Instagram de NUESTRA propia respuesta -- sirve
--    para reconocer, cuando ese mismo mid vuelve por el webhook como "echo", que es un eco de
--    nuestro propio bot y no una respuesta manual del admin.
CREATE TABLE IF NOT EXISTS mensaje_directo_social (
    id INT AUTO_INCREMENT PRIMARY KEY,
    mid VARCHAR(100) NOT NULL UNIQUE,
    red_social VARCHAR(20) NOT NULL DEFAULT 'instagram',
    autor_id VARCHAR(100) NOT NULL,
    mensaje TEXT,
    respuesta TEXT,
    respuesta_mid VARCHAR(100) NULL,
    fecha DATETIME NOT NULL
);

CREATE INDEX idx_mensaje_directo_social_autor ON mensaje_directo_social(autor_id);

-- mensaje_directo_pausa: marca que un admin ya contesto manualmente (desde la app real de
-- Instagram, no desde el bot) a un cliente por DM -- a partir de ahi el bot deja de
-- auto-contestarle a ESE cliente (indefinido). A diferencia de comentario_pausa no hay post_id --
-- un DM no esta ligado a una publicacion, la pausa es por conversacion completa con ese autor.
CREATE TABLE IF NOT EXISTS mensaje_directo_pausa (
    id INT AUTO_INCREMENT PRIMARY KEY,
    autor_id VARCHAR(100) NOT NULL UNIQUE,
    fecha DATETIME NOT NULL
);
