-- Migracion 2026-08-19 (segunda parte del bot de comentarios): agrega respuesta_comment_id a
-- comentario_social (ya creada en migration_comentario_social.sql) + tabla nueva comentario_pausa.
--
-- respuesta_comment_id: guarda el comment_id que devolvio Facebook de NUESTRA propia respuesta.
-- Sirve para reconocer, cuando ese mismo id vuelve por el webhook (Meta notifica cualquier
-- comentario nuevo, incluidos los que hacemos nosotros), que es un eco de nuestro propio bot y
-- no una respuesta manual del admin -- ver FacebookCommentBotService.
ALTER TABLE comentario_social ADD COLUMN respuesta_comment_id VARCHAR(100) NULL;

-- comentario_pausa: marca que un admin ya contesto manualmente (desde la app real de Facebook,
-- no desde el bot) a un cliente en un post especifico -- a partir de ahi el bot deja de
-- auto-contestarle a ESE cliente en ESE post (indefinido). Otros posts o clientes siguen normal.
CREATE TABLE IF NOT EXISTS comentario_pausa (
    id INT AUTO_INCREMENT PRIMARY KEY,
    autor_id VARCHAR(100) NOT NULL,
    post_id VARCHAR(100) NOT NULL,
    fecha DATETIME NOT NULL,
    UNIQUE KEY uk_comentario_pausa_autor_post (autor_id, post_id)
);
