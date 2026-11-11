# Reporte por categoría / productos elegidos y fletes por lote (2026-11-10)

Documento para retomar. Lo de la **Parte A** ya está construido; lo de la **Parte B** (fletes por lote) es el diseño que el dueño describió y **todavía no se construye**.

---

## Parte A — Reporte por categoría (construido, solo ROLE_ADMIN «en duro»)

### Decisiones del dueño
1. Cuenta **solo lo vendido**; si una venta se cancela (queda «Devuelta») ya no cuenta (el stock regresa, salvo que se marque lo contrario). Mismo criterio que los reportes Diario y Mensual.
2. Filtros: **fecha inicio y fin** + **categoría**. La categoría es un filtro: una categoría, **«Otros»** (productos sin ninguna categoría) o vacío = **todo** (con o sin categoría), **paginado** en pantalla.
3. Además de por categoría, el administrador puede **elegir productos concretos**; si un producto se vendió varias veces, sus **cantidades se suman**.
4. Por producto, el reporte trae: nombre, código de barras, precio de compra, precio de venta, precio de rebaja, precio de rebaja **de la campaña**, piezas vendidas, ventas, total, costo, ganancia, % calculado, % real, flete, ¿flete?, stock por vender, ganancia por vender y ganancia total. **Totales de cada columna al final.**
5. Tres vistas: solo lo **vendido**, solo lo **no vendido** o **todo**, para ver la ganancia real, la potencial (lo que falta por vender) y la de todo.
6. **Porcentaje por producto:** el del **último cálculo de precio guardado** (si se actualiza el producto, el cálculo conserva ese último valor). Si el producto no tiene cálculo (ejemplo: costó 100 y se vende en 200 → 50 %), se calcula **sin flete**, se **marca «SIN FLETE»** y **se guarda** en el historial de cálculos (`producto_calculo_precio`) para tenerlo listo. Solo el reporte generado en segundo plano guarda; la pantalla nunca escribe.
7. **Asíncrono:** al pedir el Excel/PDF el back responde al instante y lo arma en segundo plano (hilo propio `reportesExecutor`, **no RabbitMQ**: `main` no lo tiene configurado). El archivo terminado se guarda en la tabla `reporte_generado` (**binario**, no base64) y se descarga cuando está `LISTO`; se borra solo a los 7 días.
8. Excel con **Apache POI**; PDF con **Apache PDFBox 3.0.3** (el dueño pidió librerías de Apache; Apache POI solo hace Excel).
9. **Solo administrador, «hecho en duro»:** `SecurityConfig` (`/v1/reportes/ventas/por-categoria/**` → `hasRole("ADMIN")`) y la pestaña en el front solo con `isAdminService`. **Pendiente configurable:** pasarlo a una pantalla/acción del menú (Gestión de roles) cuando el dueño lo pida, como ya está «Corte de caja».

### Dónde está
- Back: `hexagonal/reportecategoria/**`, `ReporteGeneradoEntidad`, `migration_reporte_categoria.sql`, `AsyncConfig.reportesExecutor`, contrato en `CAMBIOS_FRONT.md`.
- Notas para el siguiente que lo toque: el precio de campaña que se muestra es el de la **campaña más reciente** que trae al producto; «precio de venta» y «rebaja» son los de hoy; el costo vendido usa el costo guardado en cada línea de venta (no el de hoy).

---

## Parte B — Fletes por lote de compra (diseño pendiente)

### Qué pidió el dueño (sus palabras, ordenadas)
- Hoy el flete se captura **por producto** en la calculadora (campo «Transporte por pieza» de `producto_calculo_precio`). **De momento se trabaja por producto (juguete), no por categoría.**
- Después quiere que **cada vez que se agregue un producto se relacione con la compra que lo trajo**: «hoy fui a comprar, el flete me costó $100 y solo traje 2». Ese flete se identifica con **un nombre y su fecha** (un **lote de compra**).
- Al dar de alta el producto A (costó $500) se le asigna **parte del flete** del lote (ej. $100) y un porcentaje (ej. 50 %) → sale su precio (ej. $1,000): **ahí el flete queda cubierto**.
- El producto B del mismo lote se puede vender igual en $1,000 con los mismos datos, pero **como el flete ya se cubrió, esa parte es ganancia** (el flete que se usó «para sacar el precio» ya no cuenta como costo).
- Si **vuelve a comprar lo mismo con otro flete**, el reporte debe tomar **el otro flete**, porque el primero ya se vendió.
- Si **lo último que compró no se vendió** y compra otro igualito: hay que decidir cómo se manejan los fletes de los dos lotes. El flete puede **subir o bajar** entre compras.
- Cada artículo debe quedar **marcado con su lote** para saber cuál se vendió: el del primer flete, el del segundo, etc.
- Para **los cálculos por juguete** se sigue usando el flete aunque ya esté cubierto, pero **en el reporte de ganancia** lo cubierto deja de contar como costo.

### Modelo propuesto (a validar con el dueño antes de construir)
- `compra_lote` (id, nombre, fecha, flete_total, nota).
- `producto_lote` (producto_id, lote_id, piezas_compradas, costo_unitario, flete_asignado) — un producto puede tener **varios lotes** (resurtidos).
- `flete_cubierto` = suma de lo asignado a los productos del lote; **falta** = `flete_total − asignado`; al llegar a 0 el lote está **cubierto** y el siguiente producto del lote se calcula con flete 0 (su ganancia sube).
- Cada **artículo/pieza vendida** se liga a un lote. Regla propuesta: **PEPS** (primero en entrar, primero en salir): se venden primero las piezas del lote más viejo; la línea de venta guarda el `lote_id` y el flete por pieza **de ese lote** para que el reporte histórico no cambie si después se corrige.
- Lote con piezas sin vender y otro lote nuevo: conviven; el reporte separa por lote (ganancia por lote, flete recuperado vs. pendiente).
- Reporte de rentabilidad: por producto y por lote → inversión, flete, ventas, ganancia **sin flete / con flete / neta de comisión**, separando con tarjeta y sin tarjeta (idea ya anotada en el plan de fletes).

### Preguntas abiertas para cuando se retome
1. ¿El flete de un producto es **por pieza** (se multiplica por las piezas) o un **total** por producto del lote?
2. Con varios lotes vivos del mismo producto, ¿se vende **PEPS** (el más viejo primero) o el dueño **elige** de qué lote sale?
3. Ventas ya hechas antes de que existan los lotes: se reportan con el costo que ya tenían guardado; ¿el flete asignado después solo afecta lo que se venda en adelante o también se **recalcula** lo vendido?
4. Cuando el flete cambia (sube o baja) entre compras: ¿el precio de venta ya puesto se **recalcula** o solo se avisa?
5. ¿El lote lleva también otros gastos (bolsas, etiquetas) además del flete?

Orden de entrega sugerido: (1) lotes + asignar flete al dar de alta/actualizar el producto y en la calculadora, (2) marcar el lote en cada artículo y en la línea de venta, (3) reporte de rentabilidad por lote. Solo `dev`/`qa` primero.

---

## Parte B — DECISIONES DEL DUEÑO sobre fletes por lote (2026-11-10) — LISTO PARA RETOMAR

Contexto: el asesor (Fable 5.2, solo lectura) propuso el modelo de lotes y 5 preguntas; el dueño las respondió así. **No se ha construido nada de la Parte B.** Cuando el dueño diga «retomamos», se empieza por la fase 1 con estas reglas.

### Respuestas del dueño (sus palabras, ordenadas)
1. **¿Cuándo se da por pagado el flete?** El flete se termina **hasta que se llega al total del flete**. Si cada producto se pone con flete, **después de pagado el flete lo que siga es ganancia, aunque la pieza todavía «tenga» flete**.
2. **¿Cómo se liga cada pieza?** **Cada pieza tiene su propio flete** (el del lote del que vino). No es un flete por producto: es por pieza y por lote.
3. **Ventas viejas:** el dueño no entendió por qué quedarían «sin flete» (ver la explicación abajo). Pendiente de que le quede claro.
4. **Flete nuevo y producto viejo:** cuando llega un flete nuevo se registran los productos; si un producto viejo ya tenía el «flete 1», las piezas que llegan en el nuevo viaje se agregan a su stock con el «flete 2». Si en ese flete llegaron más piezas, **todas esas son «flete 2»** y las que quedaban son «flete 1». Eso aplica **mientras el flete aún no se cubre**; si el flete 1 **ya se cumplió**, las piezas viejas que sobren **son solo ganancia** (su flete ya no importa).
5. **Otros gastos del viaje** (gasolina, comida, bolsas, etiquetas…): que se puedan registrar pero **opcionales**, y que **se puedan calcular** (que sumen al costo del viaje solo si el dueño quiere).

### Cómo queda la regla (interpretación, a confirmar al construir)
- **Lote** = un viaje de compra: nombre, fecha, **flete total**, gastos opcionales (concepto + monto, cada uno marcable «cuenta para cubrir» sí/no) y nota. «Costo a cubrir» = flete total + los gastos marcados.
- Cada **pieza** pertenece a un lote y lleva su **flete asignado por pieza** (el que se incluyó al calcular su precio; puede ser el total ÷ piezas o más).
- **Recuperado del lote** = suma del flete asignado de las piezas **ya vendidas**, **con tope en el costo a cubrir**: `recuperado = min(costo a cubrir, Σ flete de lo vendido)`. **Falta por cubrir** = costo a cubrir − recuperado.
- **Flete pagado** = falta por cubrir = 0. Desde ahí, el flete asignado de las piezas que sigan vendiéndose **cuenta como ganancia** (aunque su precio lo incluya).
- Ejemplo: flete total $100; 4 piezas con $50 de flete asignado cada una. Al vender 2 piezas se recuperan $100 → **pagado**. Las otras 2 piezas dejan sus $50 como **ganancia**.
- **Orden de venta (PEPS):** sale primero la pieza del lote más viejo. Con «flete 1» pendiente y llegada de «flete 2», se agota primero el 1. Si el flete 1 ya está pagado, lo que quede del lote 1 es ganancia pura.
- Lo que no se vendió de un lote viejo **no se pierde**: sigue con su lote hasta venderse.
- El precio **no se cambia solo** cuando el flete sube o baja: el sistema avisa y el dueño decide.

### Aclaración de la duda 3 (ventas viejas)
No quedan «sin flete»: quedan **sin lote**. Las ventas ya hechas guardan el costo y la ganancia de ese día, **pero no guardan de qué viaje de compra salió cada pieza**, y no hay forma confiable de reconstruirlo. Por eso, para esas ventas el reporte sigue mostrando el **flete estimado** que ya muestra hoy (flete por pieza del último cálculo × piezas vendidas). Solo las ventas **nuevas**, hechas después de activar los lotes, se marcan con su lote y su flete real. **Opcional a futuro:** una pantalla para asignar a mano un lote a ventas viejas si el dueño quiere recuperar ese historial.

### Aviso importante antes de construir la fase 2 (ventas)
La calculadora de precio tiene la casilla «Poner también el costo total en Precio de compra». Si el dueño la marca, el **costo guardado ya incluye el flete**; entonces, al descontar el flete aparte, se contaría **doble**. Antes de tocar el flujo de venta hay que revisar cómo se guarda `precio_costo` en cada línea de venta y decidir si el flete se separa del costo.

### Modelo mínimo propuesto (del asesor, ajustado a las respuestas)
- `compra_lote`: id, nombre, fecha_compra, flete_total, nota, estado (abierto/cubierto/cerrado).
- `compra_lote_gasto`: id, lote_id, concepto, monto, cuenta_para_cubrir (opcional).
- `compra_lote_producto`: id, lote_id, producto_id, variante_id (si aplica), piezas_compradas, piezas_restantes, costo_unitario, flete_por_pieza.
- `venta_detalle_lote`: detalle_venta_id, lote_producto_id, cantidad, flete_aplicado, flete_contado_como_ganancia (para el reporte histórico).
- Lote especial «Inventario inicial» (flete $0) para el stock que ya existe hoy.
- Migración mínima: crear las tablas y cargar el inventario inicial; no se modifica ninguna tabla ni dato existente.

### Fases (solo `dev`/`qa` primero; producción con orden expresa)
1. **Registrar lotes** (solo juguetes): pantalla para crear el viaje (flete total, gastos opcionales) y los productos/piezas que trajo. No toca ventas.
2. **Marcar la venta con su lote** (PEPS), guardar el flete aplicado y el que ya cuenta como ganancia; devoluciones regresan la pieza a su lote y liberan su flete.
3. **Reporte por lote** (y columnas nuevas en el reporte por categoría): costo a cubrir, recuperado, falta por cubrir, estado, ganancia con y sin flete.
4. Ligar la calculadora de precio al lote y extenderlo a todas las categorías.

### Pendiente por confirmar con el dueño cuando se retome
- Que el «flete asignado por pieza» sea el que usó al calcular el precio (puede sumar más que el flete total; el exceso es ganancia).
- ¿Quiere la pantalla opcional para asignar lote a ventas viejas?
- Si se activa la casilla «poner costo total en Precio de compra», ¿se separa el flete del costo para no contarlo doble?

