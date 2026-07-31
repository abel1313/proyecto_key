-- Hallazgo 1 (SEGURIDAD_AUTH.md) — contador de intentos fallidos del codigo de reset de password.
--
-- Sin este contador el codigo de 6 digitos se puede probar ilimitadamente durante sus 15 minutos
-- de vida (1,000,000 de combinaciones), lo que permite tomar el control de cualquier cuenta
-- conociendo unicamente su correo. Al llegar a 5 fallos el codigo se invalida y hay que pedir
-- uno nuevo por /v1/auth/olvide-password, que si esta limitado por IP.

ALTER TABLE usuario_modificacion
    ADD COLUMN intentos_codigo_reset INT NOT NULL DEFAULT 0;
