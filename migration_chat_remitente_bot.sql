-- ============================================================================
-- chat_mensaje: la base no permitia el remitente 'BOT'
-- Fecha: 2026-09-17
--
-- chat_schema.sql creo la tabla con CHECK (remitente IN ('USUARIO','ADMIN')): cuando
-- en el chat solo escribian el cliente y el dueno, alcanzaba. Desde P4 el bot tambien
-- guarda mensajes (remitente = 'BOT') y la base los rechaza con
-- "Check constraint 'chk_remitente' is violated" (MySQL error 3819).
--
-- Que rompe:
--   1. Chat en vivo: el bot SI contesta (OpenAI responde bien), pero al guardar la
--      respuesta truena. El rescate que deberia avisarle al cliente guarda por el mismo
--      camino, asi que tambien truena -> el cliente se queda sin respuesta y sin aviso.
--      Es exactamente el sintoma de "el bot no responde".
--   2. Widget publico (/chatbot/mensaje): la conversacion se guardaba a medias. La
--      pregunta del cliente entra ('USUARIO'), la respuesta del bot se rechaza y el
--      error se traga un log.warn. En chat directo se veian conversaciones mancas,
--      con las preguntas del cliente y sin las respuestas.
--
-- Sin esta migracion el bot no puede funcionar, no importa cuantos arreglos lleve el
-- Java: el insert lo rechaza la base.
--
-- No mueve ni borra datos: solo cambia que valores acepta la columna de aqui en
-- adelante. Los mensajes que ya estan guardados no se tocan.
-- ============================================================================

-- MySQL 8.0.16+. Si la restriccion no existe (base creada sin ella), este DROP marca
-- error 3940 "Check constraint ... is not found": en ese caso saltarselo y correr solo
-- el ADD de abajo.
ALTER TABLE chat_mensaje DROP CHECK chk_remitente;

ALTER TABLE chat_mensaje
    ADD CONSTRAINT chk_remitente CHECK (remitente IN ('USUARIO', 'ADMIN', 'BOT'));

-- ----------------------------------------------------------------------------
-- Verificar. Debe devolver la clausula con los TRES valores.
-- ----------------------------------------------------------------------------
SELECT CONSTRAINT_NAME, CHECK_CLAUSE
FROM information_schema.CHECK_CONSTRAINTS
WHERE CONSTRAINT_NAME = 'chk_remitente';

-- ----------------------------------------------------------------------------
-- Si hay que echarla para atras (deja al bot sin poder guardar otra vez):
-- ALTER TABLE chat_mensaje DROP CHECK chk_remitente;
-- ALTER TABLE chat_mensaje
--     ADD CONSTRAINT chk_remitente CHECK (remitente IN ('USUARIO', 'ADMIN'));
-- ----------------------------------------------------------------------------
