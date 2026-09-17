-- ============================================================================
-- Chat en vivo atendido por el bot, con traspaso a humano (P4)
-- Fecha: 2026-09-15
--
-- Agrega el modo de atencion de cada conversacion del chat en vivo:
--   BOT     = contesta el prompt del chatbot (valor por defecto)
--   HUMANO  = el admin tomo la conversacion; el bot se queda callado
--
-- El modo es independiente de `estado` (ACTIVA / CERRADA / BOT): `estado` dice si la
-- conversacion sigue abierta, `modo` dice quien la esta atendiendo.
--
-- Cuando una sesion se cierra por silencio y el cliente vuelve a escribir, el modo
-- regresa solo a BOT: lo que vuelve es una conversacion nueva.
-- ============================================================================

ALTER TABLE chat_sesion
    ADD COLUMN modo VARCHAR(10) NULL DEFAULT 'BOT';

-- Las conversaciones que ya existian quedan en BOT para no dejar el campo en NULL.
UPDATE chat_sesion SET modo = 'BOT' WHERE modo IS NULL;

-- ----------------------------------------------------------------------------
-- Si hay que echarla para atras (solo se pierde el modo de atencion, nada mas):
-- ALTER TABLE chat_sesion DROP COLUMN modo;
-- ----------------------------------------------------------------------------
