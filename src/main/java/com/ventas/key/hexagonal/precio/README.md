# Dominio: precio

Cambiar el precio de un producto desde la tarjeta de Tienda (botón 💲), sin tener que armar una
promoción ni entrar a editar el producto completo.

## Por qué existe (2026-09-22)

A un producto le bajaron el precio. Como no había forma rápida de cambiarlo, se armó una promoción
solo para eso — y la promoción se registró como **pago en efectivo** cuando el cliente en realidad
iba a ir pagando. El pedido quedó "Entregado" de contado y no se le podían registrar abonos.

## Los tres precios (viven en el producto, los artículos los heredan)

| Campo | Para qué |
|---|---|
| `precio_costo` | Lo que costó. Nunca se vende a esto. Aquí solo sirve para avisar. |
| `precio_venta` | El normal al cliente. |
| `precio_rebaja` | El precio con descuento. 0 = sin descuento. Es precio de catálogo válido para cobrar. |

## Reglas

- **R1** — El precio normal es obligatorio y mayor a 0.
- **R2** — El descuento va de 0 (sin descuento) hasta el normal. Para cobrar **más**, se sube el
  normal; el descuento nunca puede quedar arriba del normal.
- **R3** — Venderlo por debajo del costo **se permite** (rematar es decisión del negocio), pero la
  respuesta lo marca (`vendeBajoCosto`) para que la pantalla avise.
- **R4** — Cambia el catálogo, no lo ya vendido: pedidos y ventas existentes conservan el precio
  con el que se hicieron.

## Permiso

Acción `cambiar-precio` de `tienda/buscar` → `migration_accion_tienda_cambiar_precio.sql`.

## Endpoint

`PUT /v1/precios/producto/{productoId}` — body `{ precioVenta, precioRebaja }`.
