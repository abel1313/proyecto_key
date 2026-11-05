# cortecaja — Corte de caja por zona de entrega (2026-11-04)

Pantalla **solo para el administrador** (Reportes → Corte de caja). Plan y decisiones: `PLAN_CORTE_CAJA.md`.

## Reglas
- **R1 — Qué pedidos entran:** sin entregar (`entregado = false`), sin cancelar y que no estén en estado `Entregado`, y con algo por cobrar (`total_pedido − total_pagado > 0.005`). Los pagados completos, los entregados y los cancelados no salen.
- **R2 — Por zona:** se agrupan por la zona de entrega del pedido (`lugar_entrega_id`). Zonas por nombre; los pedidos sin zona (recoger en tienda) van en **«Sin zona»**, al final, y también suman al total general. `?zonaId=` deja una sola zona.
- **R3 — Lo que se cobra:** cada pedido aporta su **saldo** (total − pagado, nunca negativo); subtotal por zona y total general son la suma de los saldos.
- **R4 — Piezas y pagos:** `articulos` = suma de `cantidad` de sus renglones; `pagos` = número de abonos; con **un solo pago** se le llama anticipo (`dioAnticipo`).
- **R5 — Ganancia (oculta en la pantalla, solo admin):** `costo` = costo ACTUAL del producto × cantidad (no se guarda en el pedido; si el costo cambia, cambia en pedidos viejos); `ventaNormal` = precio normal del artículo × cantidad; `conDescuento` = con su descuento si lo tiene (si no, igual al normal; `hayDescuento` avisa si alguno lo tiene). Ganancia = venta − costo, con cada precio.
- **R6 — Sin fechas:** es el corte del momento (todo lo abierto hoy).
- Todo es **solo lectura** y el endpoint `GET /v1/reportes/corte-caja` es `hasRole('ADMIN')` en `SecurityConfig`.

## Dónde está cada cosa
| Pieza | Archivo |
|---|---|
| Reglas | `dominio/modelo/` (`PedidoPorCobrar`, `CorteDeZona`, `CorteDeCaja`, `MontosDeGanancia`) |
| Caso de uso | `aplicacion/servicio/ArmarCorteDeCajaService` |
| Lectura de datos | `infraestructura/salida/persistencia/PedidosAbiertosJpaAdapter` |
| Endpoint y JSON | `infraestructura/entrada/rest/` |
| Pruebas | `ArmarCorteDeCajaServiceTest`, `PedidosAbiertosJpaAdapterTest` (H2), `CorteDeCajaSoloAdminTest` (seguridad real) |
