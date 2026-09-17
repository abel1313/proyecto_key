-- ============================================================================
-- OPCIONAL / LIMPIEZA DE DATOS VIEJOS — una conversacion por usuario
-- Fecha: 2026-09-15
--
-- El codigo ya quedo arreglado: de aqui en adelante cada usuario reusa SU MISMA
-- conversacion y no se vuelven a generar duplicados.
--
-- Esta migracion es SOLO para las conversaciones que YA quedaron partidas antes del
-- arreglo. Junta, por usuario, todos sus mensajes en una sola conversacion.
--
-- ⚠️ MUEVE Y BORRA FILAS. No se pierde ningun mensaje (se reapuntan), pero si se
--    borran las filas sobrantes de chat_sesion. Saca respaldo de chat_sesion y
--    chat_mensaje antes de correrla, y correla primero en QA.
--
-- Si no la corres no pasa nada grave: los duplicados viejos se quedan ahi como
-- conversaciones aparte y con el tiempo dejan de aparecer en el listado.
-- ============================================================================

-- ----------------------------------------------------------------------------
-- PASO 1 — Ver que se va a consolidar. NO cambia nada, es solo para revisar.
-- ----------------------------------------------------------------------------
SELECT usuario_id, COUNT(*) AS conversaciones_duplicadas
FROM chat_sesion
WHERE usuario_id IS NOT NULL AND estado <> 'BOT'
GROUP BY usuario_id
HAVING COUNT(*) > 1
ORDER BY conversaciones_duplicadas DESC;

-- ----------------------------------------------------------------------------
-- PASO 2 — Reapuntar los mensajes a la conversacion mas antigua de cada usuario.
-- Se usa MIN(id) como la que se queda: el id es unico, asi que nunca hay empate.
-- ----------------------------------------------------------------------------
UPDATE chat_mensaje m
JOIN chat_sesion s        ON s.sesion_id = m.sesion_id
JOIN (
        SELECT usuario_id, MIN(id) AS id_destino
        FROM chat_sesion
        WHERE usuario_id IS NOT NULL AND estado <> 'BOT'
        GROUP BY usuario_id
     ) p                  ON p.usuario_id = s.usuario_id
JOIN chat_sesion destino  ON destino.id = p.id_destino
SET m.sesion_id = destino.sesion_id
WHERE s.usuario_id IS NOT NULL
  AND s.estado <> 'BOT'
  AND s.id <> destino.id;

-- ----------------------------------------------------------------------------
-- PASO 3 — Borrar las filas de chat_sesion que quedaron sin mensajes.
-- ----------------------------------------------------------------------------
DELETE s FROM chat_sesion s
JOIN (
        SELECT usuario_id, MIN(id) AS id_destino
        FROM chat_sesion
        WHERE usuario_id IS NOT NULL AND estado <> 'BOT'
        GROUP BY usuario_id
     ) p ON p.usuario_id = s.usuario_id
WHERE s.usuario_id IS NOT NULL
  AND s.estado <> 'BOT'
  AND s.id <> p.id_destino;

-- ----------------------------------------------------------------------------
-- PASO 4 — Poner la ultima actividad segun el ultimo mensaje real, para que la
-- conversacion quede ordenada donde le toca en el listado del admin.
-- ----------------------------------------------------------------------------
UPDATE chat_sesion s
JOIN (
        SELECT sesion_id, MAX(timestamp) AS ultimo
        FROM chat_mensaje
        GROUP BY sesion_id
     ) m ON m.sesion_id = s.sesion_id
SET s.ultima_actividad = m.ultimo
WHERE s.usuario_id IS NOT NULL AND s.estado <> 'BOT';

-- ----------------------------------------------------------------------------
-- PASO 5 — Verificar. Ya no deberia devolver ninguna fila.
-- ----------------------------------------------------------------------------
SELECT usuario_id, COUNT(*) AS conversaciones_duplicadas
FROM chat_sesion
WHERE usuario_id IS NOT NULL AND estado <> 'BOT'
GROUP BY usuario_id
HAVING COUNT(*) > 1;
