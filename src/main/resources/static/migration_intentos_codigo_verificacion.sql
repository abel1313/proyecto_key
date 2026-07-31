-- Hallazgo 2 (SEGURIDAD_AUTH.md) — contador de intentos fallidos del codigo de verificacion de correo.
--
-- Mismo mecanismo que intentos_codigo_reset (hallazgo 1), pero para el codigo que usan
-- /v1/auth/verificar-correo (publico) y /v1/auth/confirmar-cambio-correo. Sin el contador, el
-- codigo de 6 digitos se puede probar ilimitadamente durante sus 15 minutos de vida y permite
-- activar por fuerza bruta una cuenta registrada con el correo de otra persona.
-- Al llegar a 5 fallos el codigo se invalida y hay que pedir uno nuevo.

ALTER TABLE usuario_modificacion
    ADD COLUMN intentos_codigo_verificacion INT NOT NULL DEFAULT 0;
