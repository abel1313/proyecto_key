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
4. ✅ Adaptador del pedido (`PedidoACobrarPort`, `ActualizarPedidoPort`) → `PedidoCobroAdapter`.
5. ⏳ Terminal real (`PointClient`) y Checkout Pro (`external_reference`, `expiration_date_to`, tope de cuotas, firma del webhook).
6. ✅ Controllers + permisos + `CAMBIOS_FRONT.md` (`PagoTarjetaController`, `/v1/pagos-tarjeta`; módulo apagado por `pagos.tarjeta.habilitado`).
7. ⏳ Front (casilla MSI, botón de pagar, estados) y pruebas en QA con la pasarela simulada.

---

## Plan actualizado (2026-10-28) — decisiones del dueño y orden

**Hallazgo:** hoy la terminal real se cobra por un camino **viejo** (`/v1/mp/*`, `MercadoPagoService` + `PointClient`) que es el que usa el front (`pedidos/pago.service.ts`, diálogo "Cobrar pedido" en `mis-pedidos` y `venta-directa`). El módulo nuevo (`/v1/pagos-tarjeta`) todavía no tiene front ni pasarela real. Dos caminos para lo mismo = riesgo de doble cobro; el viejo no tiene `external_reference`, devoluciones ni reconciliación, y su webhook no valida firma.

**Decisiones:**
1. La terminal **pasa al módulo nuevo**; un solo camino. Mientras convivan, `/v1/mp/iniciar` y `/v1/pagos-tarjeta` se rechazan entre sí si ya hay un cobro activo del mismo pedido o de la misma terminal.
2. **MSI:** casilla "Acepta meses sin intereses" por **producto** (aplica a todos sus artículos) y por **artículo** (NULL = hereda, 0/1 = propia), apagada por default. Si el pedido trae un artículo que no acepta, no hay meses y se explica cuál. Mínimo $300; planes 3/6/9/12; nunca en Apartado ni Ir pagando.
3. **Precio por cantidad** (Etapa de después, ver `PLAN_PRECIO_POR_CANTIDAD.md`).
4. **Lo que depende de datos de Mercado Pago va AL FINAL** (credenciales, terminal real, Checkout Pro, webhook firmado, producción).

**Orden aprobado:**
1. Anotar todo (este documento, `PLAN_PRECIO_POR_CANTIDAD.md`, skill `reglas-pedidos`).
2. Casilla de meses en producto y artículo (back + front + chips MSI).
3. ✅ (2026-10-28, ramas `feature/cobro-tarjeta` back y front) Botones de cobrar con tarjeta (terminal y en línea) funcionando con la pasarela **simulada**; los dos caminos de la terminal se rechazan entre sí; `venta-directa` y los grupos siguen por el camino anterior.
4. Pendientes de seguridad: bloquear abonos / agregar artículos / unir en grupo con cobro activo; pool de hilos de `@Scheduled`; mensaje claro si se enciende sin pasarela; freno iniciar→cancelar; lista de `COBRADO_SIN_APLICAR`; hotfix de `simular-pago` en producción (decisión del dueño).
5. Precio por cantidad.
6. **Al final:** adaptador real de la terminal (`PointClient`, `external_reference`, reembolso), Checkout Pro (`external_reference`, `expiration_date_to`, tope de cuotas, `back_urls`), webhook con firma (T5), filas de 6/9/12 meses en `pagos_y_meses` con tarifas reales, migración en producción y llevar a `main` por cherry-pick.

**Prerrequisito de producción (crítico):** mapear `aceptaMsi` en las entidades sin la columna en `inventario_key` tumba todo el catálogo (`Unknown column 'acepta_msi'`). La migración `migration_pago_tarjeta.sql` se corre en producción **antes** de llevar cualquier cosa a `main`.

**Datos que el dueño debe conseguir (solo para el paso 6; nunca por chat, van como secretos en Kubernetes):** credenciales de prueba y producción de Mercado Pago, usuarios y tarjetas de prueba, Checkout Pro habilitado, modelo y `device-id` de la terminal de producción, URL y secreto del webhook, tarifas reales por plan.

**Decisiones del dueño aún abiertas:** quién paga la comisión (la fórmula actual copia la de ventas y puede estar mal); si devolver afecta pedido/venta/stock; qué se hace con `COBRADO_SIN_APLICAR`; planes de meses habilitados; entrega del pago en línea (a dónde y cuándo).
