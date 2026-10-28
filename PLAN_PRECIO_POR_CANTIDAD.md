# Plan — precio por cantidad (paquetes) y una fila por artículo

**Fecha:** 2026-10-28 · **Estado:** solo plan; se programa después de tarjeta y meses (decisión del dueño). Antes de programar se vuelve a revisar este documento con él.

## Lo que pidió el dueño
Poder configurar en cada **producto** una regla de precio por cantidad, y que en la tabla del pedido el **mismo artículo salga en una sola fila** con cantidad y subtotal calculado.

### Dos tipos de regla (se elige uno por producto)
- **PAQUETE:** "cada N piezas cuestan $X"; lo que sobra se cobra a precio normal.
  Ej. pantalón $200, paquete de 2 = $375 → 1 = $200 · 2 = $375 · 3 = $575 · 4 = $750.
- **DESDE:** "de N piezas en adelante cada una vale $Y".
  Ej. normal $200, desde 3 a $150 → 2 = $400 · 3 = $450 · 5 = $750.

### Qué cuenta
La cantidad se cuenta **por producto completo** (un pantalón talla 28 y otro talla 30 del mismo producto suman juntos).

## Lo que ya existe (investigado 2026-10-28)
- `hexagonal/precio` (`PreciosDeArticulo`, `ReglasDePrecio`): precio normal y descuento por artículo (`usarDescuento`). No hay precio por cantidad. R4: lo ya vendido conserva su precio. Es la base natural del dominio nuevo.
- Promociones/combos (`PROMOCIONES.md`): combo de piezas distintas, precio por pieza (decisión 9), solo de contado (decisión 8). **Son otra cosa**; conviven y no se mezclan.
- `DetallePedido` guarda `cantidad`, `precio_unitario`, `sub_total` y `promocion_id`. El subtotal lo calcula el back.
- `PedidoServiceImpl.validarPrecioCatalogo` (~L218) y `VentaServiceImpl` (~L395) solo aceptan precio normal o de rebaja × cantidad: hay que cambiarlos para aceptar el precio por regla **calculado por el back**, nunca el que mande el front.
- El carrito del front ya fusiona por artículo; `cantidad*precio` está repetido en `carrito-variante.service.ts`, `venta-variante`, `venta-directa`, `detalle-pedido`, `mis-pedidos` y `shared/total-pedido.pipe.ts`.

## Diseño propuesto
- Tabla `precio_cantidad_regla` (producto, tipo PAQUETE|DESDE, N, precio, activa) con migración; **una sola regla activa por producto**.
- Dominio puro en `hexagonal/precio` con pruebas de los ejemplos de arriba (sin Spring).
- El back calcula el subtotal por producto (suma de piezas de todos sus artículos) y lo reparte entre las líneas; `DetallePedido.sub_total` queda **ya calculado y congelado**; `precio_unitario` es el promedio solo para mostrar; se guarda un marcador de la regla aplicada.
- Front: una sola función `subtotalLinea`; mostrar "2 × $375 (paquete)"; campos de la regla en `productos/producto/add` y en el botón 💲 de la Tienda.

## Riesgos que hay que resolver antes de programar
1. **Editar la cantidad después** (`PedidoServiceImpl` ~L780, `PedidoArticuloJpaAdapter` L53-84): `precioUnitario * cantidad` rompe el paquete. Hay que recalcular con la regla vigente **al momento del pedido**.
2. **Total, abonos y grupos** dependen de `totalPedido` (`AbonoServiceImpl`, `grupopedido`): si el total cambia al editar, cambian saldos y el estado Pagado.
3. **Ganancia** se calcula con `sub_total` (`PedidoServiceImpl` ~L494-504).
4. **Precio desactualizado** en el front (`venta-directa` ~L955).
5. **Decidir con el dueño:** qué pasa con un Apartado hecho con regla vigente si la regla cambia; si la regla aplica también a ventas del admin con precio manual (💲); cómo se reparte el descuento entre tallas distintas con precio distinto.

## Pruebas (tabla con números)
Las de arriba en el dominio, más recorrer carrito → pedido → abono → grupo → venta y comprobar que total, saldo y ganancia coinciden.
