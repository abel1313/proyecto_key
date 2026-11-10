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
