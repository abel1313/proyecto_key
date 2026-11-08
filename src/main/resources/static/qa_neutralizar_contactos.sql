-- ============================================================
-- Script (NO es migración, SOLO QA, NUNCA PROD): después de copiar prod a QA, quita los correos y teléfonos de personas reales
-- para que nada de QA (tickets, promociones, códigos de campaña, WhatsApp) les llegue por error.
-- Deja intactos los tuyos: pon tus correos en @mis_correos. Las contraseñas y los usuarios para entrar NO se tocan
-- (el correo del usuario solo se usa para avisos; el login es por username).
-- Revisa primero las cuentas con SELECT antes de correr los UPDATE.
-- ============================================================

SET @mis_correos = 'abeltrece@gmail.com';     -- <-- tus correos separados por coma, sin espacios

-- VISTA PREVIA: cuántos contactos se van a neutralizar
SELECT (SELECT COUNT(*) FROM clientes WHERE correo_electronico IS NOT NULL AND NOT FIND_IN_SET(correo_electronico, @mis_correos)) AS clientes,
       (SELECT COUNT(*) FROM clientes_sin_registro WHERE correo_electronico IS NOT NULL AND NOT FIND_IN_SET(correo_electronico, @mis_correos)) AS sin_registro,
       (SELECT COUNT(*) FROM usuario_modificacion WHERE email IS NOT NULL AND NOT FIND_IN_SET(email, @mis_correos)) AS usuarios,
       (SELECT COUNT(*) FROM concursantes WHERE correo IS NOT NULL AND NOT FIND_IN_SET(correo, @mis_correos)) AS concursantes;

-- APLICAR (correos: qa+<id>@example.com; teléfonos: se borran)
UPDATE clientes SET correo_electronico = CONCAT('qa+c', id, '@example.com'), correo_pendiente = NULL
 WHERE correo_electronico IS NOT NULL AND NOT FIND_IN_SET(correo_electronico, @mis_correos);
UPDATE clientes SET numero_telefonico = NULL WHERE correo_electronico LIKE 'qa+c%@example.com';

UPDATE clientes_sin_registro SET correo_electronico = CONCAT('qa+s', id, '@example.com')
 WHERE correo_electronico IS NOT NULL AND NOT FIND_IN_SET(correo_electronico, @mis_correos);
UPDATE clientes_sin_registro SET numero_telefonico = NULL WHERE correo_electronico LIKE 'qa+s%@example.com';

UPDATE usuario_modificacion SET email = CONCAT('qa+u', id, '@example.com'), correo_pendiente = NULL
 WHERE email IS NOT NULL AND NOT FIND_IN_SET(email, @mis_correos);

UPDATE concursantes SET correo = CONCAT('qa+k', id, '@example.com'), telefono = NULL
 WHERE correo IS NOT NULL AND NOT FIND_IN_SET(correo, @mis_correos);

UPDATE ventas SET correo_reclamo = NULL WHERE correo_reclamo IS NOT NULL AND NOT FIND_IN_SET(correo_reclamo, @mis_correos);

-- Si ya existen personas de campaña (tabla campana_persona), también:
-- UPDATE campana_persona SET correo = CONCAT('qa+p', id, '@example.com'), telefono = NULL WHERE correo IS NOT NULL AND NOT FIND_IN_SET(correo, @mis_correos);
