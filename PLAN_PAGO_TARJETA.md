# Plan — pago con tarjeta (terminal del local y tienda en línea)

**Fecha:** 2026-10-21 · **Rama:** `feature/pago-tarjeta` (desde `dev`) · **Estado:** dominio, persistencia, planes MSI y pasarela simulada hechos y revisados dos veces (reconstruidos el 2026-10-21 tras perder el trabajo no subido; ahora la rama está en GitHub) · pendiente: adaptador del pedido, pasarelas reales, controllers y front.

Reglas, principios y contratos: `src/main/java/com/ventas/key/hexagonal/pagotarjeta/README.md`.

## Decisiones del dueño
- La terminal sigue en la API actual (no se migra); las pruebas son con una pasarela simulada en QA, sin mover dinero.
- Tarjeta solo cuando el cliente llega al local o cuando hace el pedido y paga él mismo. Apartado e Ir pagando: efectivo o transferencia.
- MSI solo en algunos productos: casilla por producto/artículo (apagada por default), mínimo $300, planes 3/6/9/12.
- El cambio se lleva solo a `dev` y `qa`, nunca a `main`, cuando esté probado y el dueño lo pida.

## Fases
1. ✅ Dominio + servicios + pruebas.
2. ✅ Persistencia (intentos, devoluciones, planes, transacción) + migración en QA.
3. ✅ Pasarela simulada (dev/qa).
4. ⏳ Adaptador del pedido (`PedidoACobrarPort`, `ActualizarPedidoPort`).
5. ⏳ Terminal real (`PointClient`) y Checkout Pro (`external_reference`, `expiration_date_to`, tope de cuotas, firma del webhook).
6. ⏳ Controllers + permisos + `CAMBIOS_FRONT.md`.
7. ⏳ Front (casilla MSI, botón de pagar, estados) y pruebas en QA con la pasarela simulada.
