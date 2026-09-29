# Dominio: precio

Cambiar el precio de un producto desde la tarjeta de Tienda (botón 💲), sin tener que armar una
promoción ni entrar a editar el producto completo.

## Por qué existe (2026-09-22)

A un producto le bajaron el precio. Como no había forma rápida de cambiarlo, se armó una promoción
solo para eso — y la promoción se registró como **pago en efectivo** cuando el cliente en realidad
iba a ir pagando. El pedido quedó "Entregado" de contado y no se le podían registrar abonos.

## Los tres precios (viven en el producto; un artículo puede tener los suyos)

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
- **R5** (2026-09-29) — Un artículo puede tener **precio propio** (normal + descuento, siempre los
  dos juntos, con R1 y R2). Sin precio propio usa los del producto. Cambiar el precio del producto
  no pisa a los artículos que tienen el suyo. Para leerlo: `Variantes.precioNormal()` y
  `precioDescuento()`, nunca `getProducto().getPrecioVenta()` directo.
- **R6** — "Usar el del producto" le quita el precio propio al artículo.
- **R7** (2026-09-29) — El descuento **nunca se cobra solo**: se cobra el normal, y el descuento
  únicamente si el admin lo elige (check "Usar" en el carrito, "Otro precio" en el pedido).

## Permiso

Acción `cambiar-precio` de `tienda/buscar` → `migration_accion_tienda_cambiar_precio.sql`.

## Endpoint

- `PUT /v1/precios/articulo/{varianteId}` — body `{ precioVenta, precioRebaja }`. Solo ese artículo.
- `DELETE /v1/precios/articulo/{varianteId}` — vuelve al precio del producto. **Solo en qa/dev**
  (en `main` no está expuesto; el caso de uso sí existe).
- `PUT /v1/precios/producto/{productoId}` — el del producto (el front ya no lo usa).

Columnas del precio propio: `variantes.precio_venta` / `precio_rebaja` (`migration_precio_variante.sql`).
