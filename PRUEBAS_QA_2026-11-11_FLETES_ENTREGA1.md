# Pruebas en QA — Fletes entrega 1 (2026-11-11)

Requisitos: back y front de QA ya desplegados (GitHub Actions), `migration_flete_lote.sql` ya corrida, y **correr ahora `migration_flete_venta.sql`** en `inventario_key_qa` (después, volver a entrar).
Nota: **esta entrega NO trae todavía el botón «🧪 Preparar prueba»** (los datos se arman a mano con las pantallas de siempre); queda pendiente para la entrega 2.

## Mapa de impacto
| # | Se movió | Le pega a | Antes | Después |
|---|---|---|---|---|
| 1 | Al guardar una venta se ligan sus piezas a un viaje | Venta directa de contado | se guardaba y listo | **lo mismo** (más las filas de viaje); mismos totales, estado y stock |
| 2 | (igual) | Pedido cobrado desde Mis pedidos | igual | **lo mismo** |
| 3 | (igual) | Abono que liquida un Apartado / Ir pagando | igual | **lo mismo** |
| 4 | (igual) | Cobro con tarjeta aprobado | igual | **lo mismo** |
| 5 | Editar / borrar un viaje | Catálogo → Fletes → Editar / Eliminar | se podía todo | con piezas vendidas no deja quitar producto, bajar piezas ni borrar |
| 6 | Reporte por viaje | `GET /v1/fletes/{id}/reporte` | no existía | trae recuperado, falta, estado |

## Prueba 1 — Venta de un producto que está en un viaje
**Antes de empezar:** en Catálogo → Fletes crea el viaje «QA Flete A»: fecha de hoy, flete total $100, producto de juguetes P con 5 piezas, costo $50, flete por pieza $20. (P debe tener en existencia al menos esas 5 piezas.)
**Pasos:** 1. Venta directa de 3 piezas de P, de contado. 2. En MySQL: `SELECT * FROM venta_detalle_lote ORDER BY id DESC LIMIT 5;`.
**✅ Debe pasar:** la venta sale igual que siempre; hay **una fila** con `cantidad` 3 y `flete_por_pieza` 20.
**⛔ No puede pasar:** error al cobrar, total o stock distinto del esperado, o que no haya fila (si P tenía más piezas que el viaje, las más viejas salen primero y puede que no haya fila: revisa el stock de P).

## Prueba 2 — Venta que cruza dos viajes
**Antes de empezar:** crea «QA Flete B» con fecha posterior, P con 5 piezas, flete por pieza $30.
**Pasos:** vende 4 piezas de P (quedan 2 del viaje A y 5 del B).
**✅ Debe pasar:** dos filas: 2 de A a $20 y 2 de B a $30.

## Prueba 3 — Reporte del viaje
**Pasos:** abre `GET /mis-productos/v1/fletes/{id del viaje A}/reporte` (con tu sesión de administrador, en el navegador o en Postman).
**✅ Debe pasar:** viaje A: `costoACubrir` 100, `fleteRecuperado` 100 (3×20 + 2×20), `faltaPorCubrir` 0, `estado` `VENDIDO_POR_COMPLETO`, `piezasVendidas` 5. Viaje B: `costoACubrir` 150, `fleteRecuperado` 60, `faltaPorCubrir` 90, `estado` `ABIERTO`, `piezasRestantes` 3.

## Prueba 4 — Proteger un viaje con ventas
**Pasos:** en Catálogo → Fletes edita el viaje A: (a) quítale el producto P y guarda; (b) bájale las piezas de P a 4 y guarda; (c) intenta Eliminarlo; (d) cambia solo el nombre y el flete total y guarda.
**✅ Debe pasar:** (a), (b) y (c) responden con un aviso claro («ya se vendieron…», «no puede tener menos de…», «no se puede eliminar…») y no cambian nada; (d) guarda y las ventas siguen ligadas (repite el reporte de la prueba 3: mismos números del flete recuperado con el flete nuevo si lo cambiaste).

## Prueba 5 — Devolución
**Pasos:** devuelve la venta de la prueba 2 (Mis pedidos → cancelar/devolver como siempre) y repite el reporte de B.
**✅ Debe pasar:** B vuelve a `piezasVendidas` 0 y `faltaPorCubrir` 150; las piezas vuelven al viaje.

## Prueba 6 — Los otros tres flujos no cambian
**Pasos:** (a) un pedido hecho como Apartado y liquidado en un solo pago; (b) un Ir pagando con dos abonos; (c) un cobro con tarjeta aprobado (si tienes la terminal de pruebas). Cada uno con un producto que esté en un viaje.
**✅ Debe pasar:** mismos totales, estados y stock que antes de esta entrega; al liquidarse aparece su fila en `venta_detalle_lote`.
**⛔ No puede pasar:** que algún cobro falle o quede a medias por el viaje (si pasa, avisa: la asignación nunca debe tumbar una venta).

## Prueba 7 — Sin migración no se rompe (opcional, solo si tienes una base sin la tabla)
Con una base que no tenga `venta_detalle_lote`: las ventas se guardan igual y el reporte del viaje responde con 0 piezas vendidas.

---

# Entrega 3 — ligar ventas anteriores, calculadora y bloque en el producto

## Prueba 8 — Ligar ventas anteriores a un viaje
**Antes de empezar:** un producto P con un viaje «QA Flete C» de 5 piezas (flete 20 por pieza) y **dos ventas anteriores** de P hechas antes del viaje (o con P sin viaje), de 2 y 4 piezas (si ya tienes ventas viejas de P, sirven).
**Pasos:** 1. Catálogo → Fletes → en «QA Flete C» toca «Ligar ventas anteriores». 2. Elige el producto P. 3. Toca «Llenar de la más vieja a la más nueva» y «Ligar al viaje».
**✅ Debe pasar:** salen las ventas de P no devueltas con piezas sin ligar; se ligan máximo las piezas que le quedan al viaje (5); al guardar sale «Listo: se ligaron N pieza(s)»; en la tarjeta del viaje «Vendidas» sube y «Recuperado» crece con $20 por pieza; las ventas no cambian de total ni de ganancia.
**⛔ No puede pasar:** que deje ligar más piezas de las que tiene la venta o las que le quedan al viaje; que aparezcan ventas devueltas.

## Prueba 9 — Calculadora con el flete del viaje
**Pasos:** Productos → Buscar → Actualizar el producto P (que está en un viaje) → «🧮 Calcular precio».
**✅ Debe pasar:** el aviso «🚚 ya tiene su flete registrado» y debajo botones «Usar el flete de un viaje»; al tocar uno, «Transporte por pieza» se llena con el flete de ese viaje. La casilla «Poner también el costo total» arranca desmarcada; si la marcas, el aviso dice que el flete se contará dos veces.
**⛔ No puede pasar:** que «Usar este precio» cambie el «Precio de compra» sin que marques la casilla.

## Prueba 10 — Bloque «Viajes de compra» en Actualizar producto
**Pasos:** abre Actualizar del producto P.
**✅ Debe pasar:** aparece el bloque «🚚 Viajes de compra» con cada viaje (fecha, nombre, piezas, costo y flete por pieza). En un producto que no está en ningún viaje, el bloque no aparece. Es solo lectura.
