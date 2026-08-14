-- Migración 2026-08-14: bloqueo por anticipación insuficiente + cargo por urgencia, según lo que
-- el dueño ya sabe que puede o no puede armar a tiempo. Ver conversación con el front/dueño del
-- negocio del 2026-08-14.

ALTER TABLE cantidad_flor_valida
    ADD COLUMN horas_minimas_anticipacion INT NULL COMMENT 'Minimo de horas de anticipacion (sin contar la zona de entrega) que el dueno necesita para armar este tamano de ramo. Si la fecha/hora de entrega pedida no da ese margen, el pedido se rechaza. NULL = sin validar anticipacion para esta cantidad.',
    ADD COLUMN precio_urgencia DOUBLE NULL COMMENT 'Extra que se cobra cuando el pedido cae justo en el limite de horas_minimas_anticipacion (no con mucha anticipacion). Se suma directo al total, sin aparecer como accesorio. NULL = sin cargo de urgencia para esta cantidad.';

ALTER TABLE lugares_entrega
    ADD COLUMN horas_extra_anticipacion INT NULL COMMENT 'Horas extra que se suman al minimo de anticipacion requerido por el tamano del ramo, por estar esta zona mas lejos/complicada. NULL = 0, no suma nada.';

-- No requiere acción manual: todos los campos nacen vacíos (NULL), que es el estado esperado --
-- mientras nadie los configure, no cambia nada del comportamiento actual (no se valida ni se
-- bloquea ni se cobra nada nuevo).
