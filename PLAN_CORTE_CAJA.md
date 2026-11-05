# Plan — Corte de caja por zona de entrega (2026-11-04)

**Pedido del dueño:** una pantalla, **solo para el administrador**, que muestre los pedidos abiertos agrupados por la **zona de entrega** que se
elige al hacer la venta (Luvianos, Tejupilco…), para saber cuánto hay que cobrar y cuánto se gana.

## Qué pedidos entran (decidido)
- **Sin pagar y sin entregar:** pedidos con `entregado = false`, que no estén cancelados y a los que todavía se les debe dinero
  (`total_pedido − total_pagado > 0`). Los ya pagados completos o ya entregados **no** salen.
- Los pedidos **sin zona** (se recogen en tienda) salen en un grupo aparte **«Sin zona»** al final y también suman al total general.
- Se puede elegir una sola zona o ver todas.

## La pantalla (lo que se ve)
Para cada zona, una tabla; debajo, el subtotal de la zona; al final, el total general.

| Pedido | Cliente | Artículos | Total del pedido | Pagos | Por cobrar |
|---|---|---|---|---|---|
| #12 | María López | 3 | $650.00 | 1 pago (anticipo $200.00) | $450.00 |
| #15 | Juan Pérez | 1 | $100.00 | sin pagos | $100.00 |
| | | | | **Subtotal Luvianos** | **$550.00** |

- **Artículos:** la suma de `cantidad` de sus renglones (`detalle_pedidos`).
- **Pagos:** cuántos abonos tiene (`abono_pedido`) y cuánto lleva pagado; si dio un anticipo se dice «anticipo». *Por cobrar* = total − pagado.

## Ganancia (oculta; solo se ve en esta pantalla de administrador)
Tres números por pedido y por zona, **en una sección aparte que el administrador despliega**:

| | Qué es |
|---|---|
| **A costo** | lo que costó la mercancía de ese pedido (`precio_costo` × cantidad) |
| **A precio de venta** | los artículos al precio normal de venta |
| **Con descuento** | lo que realmente se cobra, si el artículo tenía descuento |
| **Ganancia** | venta − costo, y con descuento − costo |

Ni el costo ni la ganancia salen en ningún endpoint que no sea de administrador, ni en el chat, ni en la tienda.

## Lo que encontré en los datos (importa)
1. **El costo solo existe en el producto** (`producto.precio_costo`), **no por artículo ni guardado en el pedido**. Si el costo del producto cambia después,
   el corte de pedidos viejos usaría el costo nuevo. Para ganancia exacta habría que **guardar el costo en cada renglón del pedido desde ahora**
   (una columna nueva + migración); los pedidos anteriores seguirían con el costo actual del producto.
2. **El renglón del pedido guarda el precio al que se cobró** (`precio_unitario`), pero no el precio normal ni el de descuento de ese momento. El «precio de venta
   normal» y el «descuento» saldrían de los precios **actuales** del artículo, y lo «realmente cobrado» del renglón.
3. **Los pagos** sí están completos: `abono_pedido` (monto, fecha, método, nota).

## Decisiones por tomar
Ver la conversación del 2026-11-04: qué columnas de ganancia, de dónde sale el costo, cómo se muestran los pagos y si el corte lleva fechas.

## Fases (cuando se decidan)
1. **Back:** dominio `cortecaja` (hexagonal), endpoint `GET /v1/reportes/corte-caja` solo `ROLE_ADMIN`, pruebas con H2. *Ya hecho un borrador sin ganancia ni pagos
   (sin subir): agrupa por zona, saldo, subtotal y total, con pruebas de cálculo.*
2. **Back:** artículos, pagos y ganancia (según lo decidido); migración solo si se decide guardar el costo en el pedido.
3. **Front:** pestaña «Corte de caja» en Reportes (solo administrador), con la ganancia oculta hasta desplegarla; imprimir o copiar el corte.
4. **Documentar** en `CAMBIOS_FRONT.md` y el README del dominio. **Probar en QA** con pedidos de dos zonas, uno con anticipo y uno con descuento.
