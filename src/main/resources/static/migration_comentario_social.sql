-- Migracion 2026-08-19: tabla comentario_social -- registra cada comentario de redes sociales
-- que el bot proceso (empezando por Facebook). Sirve para 2 cosas:
-- 1) Idempotencia: Meta a veces reenvia el mismo evento de webhook mas de una vez; con comment_id
--    UNIQUE se evita procesar/responder el mismo comentario dos veces.
-- 2) Saber si es la "primera vez" que ese autor_id comenta (para el saludo de bienvenida
--    obligatorio en el primer contacto) -- ver FacebookCommentBotService.
CREATE TABLE IF NOT EXISTS comentario_social (
    id INT AUTO_INCREMENT PRIMARY KEY,
    comment_id VARCHAR(100) NOT NULL UNIQUE,
    post_id VARCHAR(100),
    red_social VARCHAR(20) NOT NULL DEFAULT 'facebook',
    autor_id VARCHAR(100) NOT NULL,
    mensaje TEXT,
    respuesta TEXT,
    fecha DATETIME NOT NULL
);

CREATE INDEX idx_comentario_social_autor ON comentario_social(autor_id);
