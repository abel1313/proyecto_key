-- Migración 2026-08-14: configuración de entregas por tamaño de ramo (reemplaza en la práctica a
-- horas_minimas_anticipacion/precio_urgencia -- esos dos NO se borran, quedan sin usar). El
-- dueño da de alta, por cada tamaño, cuánto tarda entregarlo normal y, opcionalmente, si se puede
-- apurar y con qué cargo. Ver conversación con el front/dueño del negocio del 2026-08-14.

ALTER TABLE cantidad_flor_valida
    ADD COLUMN dias_normal INT NULL COMMENT 'Plazo normal en dias para este tamano de ramo. NULL = todavia sin configurar.',
    ADD COLUMN hora_entrega_normal TIME NULL COMMENT 'Hora del dia en que se entrega con el plazo normal.',
    ADD COLUMN dias_urgente INT NULL COMMENT 'Plazo urgente en dias. NULL = este tamano NO se puede apurar por ningun motivo.',
    ADD COLUMN hora_entrega_urgente TIME NULL COMMENT 'Hora de entrega del plazo urgente.',
    ADD COLUMN hora_limite_pedido TIME NULL COMMENT 'Hora limite del dia para que el pago cuente para el plazo urgente de hoy -- despues de esta hora, el plazo urgente se corre a partir de manana.',
    ADD COLUMN cargo_urgente DOUBLE NULL COMMENT 'Cargo extra cuando aplica el plazo urgente. Se suma directo al total, sin aparecer como accesorio.';

-- Para revalidar el pago tardio sin tocar el endpoint generico de abonos -- ver
-- RamoPedidoDetalleServiceImpl.revalidarAntesDePagar.
ALTER TABLE ramo_pedido_detalle
    ADD COLUMN fecha_hora_entrega DATETIME NULL COMMENT 'Fecha/hora de entrega que eligio el cliente (mismo dato mandado a calcular-precio y fechas-disponibles).',
    ADD COLUMN es_urgente TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'Si el cliente eligio el plazo urgente para este pedido.',
    ADD COLUMN fecha_limite_pago DATETIME NULL COMMENT 'Momento exacto limite para que el pago cuente con el precio normal. NULL si es_urgente=0.',
    ADD COLUMN cargo_urgente_monto DOUBLE NULL COMMENT 'Cuanto se cobraria si el pago llega despues de fecha_limite_pago (copiado de CantidadFlorValida.cargo_urgente al momento de adjuntar).',
    ADD COLUMN cargo_urgente_aplicado TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'true una vez que el cargo ya se agrego como linea al pedido -- evita duplicarlo.';

-- No requiere acción manual: todos los campos nacen vacíos (NULL) para todo lo ya registrado, que
-- es el estado esperado -- el dueño los va llenando por tamaño. Mientras un tamaño no tenga
-- dias_normal/hora_entrega_normal configurados, /v1/flores/fechas-disponibles responde con
-- mensaje de "todavía no configurado" en vez de una fecha.
