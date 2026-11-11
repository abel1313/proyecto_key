# Guía de pruebas en QA — Fletes (viajes de compra)

Para: el dueño. Escrita sin términos técnicos. Cada prueba dice **dónde**, **qué haces**, **qué debes ver** y **qué NO debe pasar**.
Fecha: 2026-11-11. Todo lo de esta guía está en **QA y dev**. **Nada de esto está en producción todavía.**

---

## 1. Qué es esto, en una frase

Hoy el flete (lo que cuesta traer la mercancía) solo se escribía a mano en la calculadora de precio y nadie sabía **cuándo ya se pagó** ni **de qué compra salió cada pieza que vendes**.
Con "Fletes" registras cada **viaje de compra** (cuánto costó el flete, qué trajiste y cuántas piezas), y el sistema, **cada vez que vendes**, anota **de qué viaje salió la pieza**. Así puede decirte:

- cuánto flete **ya recuperaste** con lo que has vendido,
- cuánto **te falta** por recuperar,
- desde qué venta el flete **ya se pagó** (y lo que se venda después **ya es ganancia**),
- y cuántas piezas **quedan** de cada viaje.

### Palabras que vas a ver

| Palabra | Qué significa |
|---|---|
| **Viaje** | Una compra: «hoy fui a comprar, el flete me costó $100 y traje estos juguetes». Tiene nombre y fecha. |
| **Costo a cubrir** | Lo que las piezas del viaje deben **recuperar**: el flete total **más** los gastos extra que tú marques como «cuenta para cubrir» (gasolina, bolsas…). |
| **Flete por pieza** | Cuánto flete lleva **cada pieza** de un producto de ese viaje. Normal: costo a cubrir ÷ piezas del viaje (lo puedes cambiar). |
| **Recuperado** | Lo que ya se recuperó del costo a cubrir con las piezas **vendidas** (flete por pieza × piezas vendidas), con tope en el costo a cubrir. |
| **Falta por cubrir** | Costo a cubrir − recuperado. |
| **Ya es ganancia** | El flete de las piezas que vendes **después** de que el viaje ya se cubrió: ya no es costo, es ganancia. |
| **Sin viaje** | Piezas vendidas que no salieron de ningún viaje (ventas de antes de los viajes, o stock de antes). Su flete sigue siendo el **estimado** de la calculadora. |
| **Real vs. estimado** | Real = el flete del viaje de donde salió la pieza. Estimado = el flete por pieza del último cálculo de precio (para lo que no tiene viaje y para lo que sigue en stock). |

Estados de un viaje: **Abierto** (el flete aún no se cubre) → **Flete cubierto** (lo recuperado ya llegó al costo a cubrir) → **Vendido por completo** (ya no quedan piezas).

### Reglas que el sistema sigue (decididas por ti)

1. Salen **primero las piezas que ya había antes de los viajes**; después las del **viaje más viejo**; luego las del siguiente.
2. Si una venta toma piezas de **dos viajes**, se parte (ej. 4 piezas = 2 del viaje A y 2 del B).
3. El costo que se guarda de cada producto **no lleva el flete**: el flete va **aparte**, en el viaje. Por eso la calculadora te **avisa** si estás por contar el flete dos veces.
4. El **precio no se cambia solo** si el flete sube o baja.
5. Una venta **devuelta** libera sus piezas: vuelven al viaje y dejan de contar.
6. Si el sistema **no puede** ligar una venta a un viaje (por lo que sea), **la venta se hace igual** y esa pieza queda «sin viaje». Una venta nunca se cae por esto.

---

## 2. Antes de empezar

1. **Migraciones en la base de QA** (las dos ya las corriste; confírmalo):
   `SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name IN ('compra_lote','compra_lote_gasto','compra_lote_producto','venta_detalle_lote');` → debe salir **4**.
2. **Espera los deploys** de QA (back y front) en GitHub Actions y **vuelve a entrar** (cierra sesión y entra) para que aparezca el menú.
3. En el navegador **Ctrl+F5**.
4. Entra como **administrador**. Todo lo de Fletes es solo del administrador.
5. Para las pruebas usa un **juguete de prueba** (que no sea un producto importante) con **stock exacto de 10 piezas**. A ese lo llamo **«P»**. Si el stock es otro, ajusta los números.

---

## 3. Las pantallas, una por una

### 3.1 Catálogo → 🚚 Fletes (la pantalla nueva)

**Qué es:** la lista de tus viajes de compra y el lugar donde los creas, los editas y ves cómo va el flete de cada uno.
**Qué esperas ver:**
- Arriba: título «Fletes», una explicación corta y el botón **＋ Nuevo viaje**.
- Una **tarjeta por viaje** con: nombre, fecha, nota; flete total, costo a cubrir, compra (sin flete), productos/piezas; una **etiqueta** (Abierto / Flete cubierto / Vendido por completo); una **barra** «Recuperado $X de $Y» (con «falta», «ya es ganancia», piezas vendidas y las que quedan); y botones **⬇ Excel**, **⬇ PDF**, **Ligar ventas anteriores**, **Editar**, **Eliminar**.
- **Ver productos** abre una tablita por producto: compradas, vendidas, quedan y cuánto se vendió.

### 3.2 Nuevo viaje / Editar viaje

**Qué es:** el formulario donde registras el viaje.
**Qué esperas ver:** nombre, fecha (hoy por defecto), flete total, nota; **otros gastos** (cada uno con «Cuenta para cubrir»); un **buscador de productos** (por nombre o código de barras) y, por cada producto, **piezas, costo c/u y flete por pieza**; el botón **Repartir el flete por igual**; y un **resumen en vivo**: Costo a cubrir / Flete repartido en las piezas / Falta por repartir (se pone de color si falta) / Flete de más.
**Importante:** esto **solo registra**. No cambia el stock, ni los precios, ni las ventas.
**Protección:** si un viaje ya tiene piezas vendidas, **no te deja** quitarle un producto, bajarle las piezas por debajo de lo vendido, ni borrarlo. Sí puedes cambiar nombre, fecha, flete y gastos.

### 3.3 Fletes → «Ligar ventas anteriores»

**Qué es:** para las ventas hechas **antes** de que existieran los viajes (o antes de registrar el viaje). El sistema no sabe de qué compra salieron; aquí se lo dices.
**Qué esperas ver:** eliges el **producto** del viaje (y, si quieres, fechas) → **Buscar ventas** → una tabla con las ventas de ese producto que tienen piezas **sin ligar** (venta, fecha, piezas, cuántas faltan por ligar, precio) → escribes cuántas piezas de cada venta salieron de este viaje → **Ligar al viaje**. El botón **Llenar de la más vieja a la más nueva** lo hace solo sin pasarse de lo que le queda al viaje.
**Qué NO hace:** no cambia la venta (total, ganancia, stock). Solo guarda de qué viaje salió y a cuánto flete por pieza.

### 3.4 Los botones ⬇ Excel y ⬇ PDF de cada viaje

**Qué es:** el reporte de **un viaje**: sus datos, cómo va el flete, los productos y **las ventas que se ligaron**, en el orden en que se fue recuperando el flete (con cuánto de cada venta fue «recuperado» y cuánto ya «ganancia»). Sirve para sacar reportes **por flete en lugar de por categoría**.
**Qué esperas ver:** se descarga un archivo con la banda verde de Novedades Jade, tarjetas (flete total, costo a cubrir, recuperado, falta, piezas, total vendido, ganancia vendida, flete ya ganancia), tabla de productos y tabla de ventas con su fila de **TOTAL**.

### 3.5 Reportes → Por categoría (cambió)

**Qué es:** el reporte de siempre por categoría/producto, ahora con el flete **real**.
**Qué esperas ver (nuevo):** en la tabla, dos columnas después de «¿Flete?»: **Flete real (viajes)** y **Piezas sin viaje**; en la tarjeta «Flete», «Vendido $X **(real $Y)** · stock $Z». El flete de lo vendido ahora es **real** (de los viajes) **más** el **estimado** solo de las piezas sin viaje. El Excel y el PDF traen las mismas columnas.

### 3.6 Productos → Actualizar producto → 🧮 Calcular precio

**Qué es:** la calculadora de precio de siempre.
**Qué esperas ver (nuevo) si el producto está en un viaje:** un aviso «🚚 ya tiene su flete registrado en «viaje» (… por pieza)» y botones **«Usar el flete de un viaje»** que llenan «Transporte por pieza» (solo para calcular el precio). La casilla **«Poner también el costo total en Precio de compra»** arranca **desmarcada**; si la marcas, el aviso dice que **el flete se contará dos veces**. En un producto sin viaje **todo es igual que antes**.

### 3.7 Productos → Actualizar producto → bloque «🚚 Viajes de compra»

**Qué es:** una lista **solo para ver** de los viajes en los que llegó ese producto (fecha, nombre, piezas, costo y flete por pieza).
**Qué esperas ver:** aparece solo en un producto que está en algún viaje. En un producto sin viajes **no aparece**.

### 3.8 Las ventas (Venta directa, Mis pedidos, abonos, tarjeta)

**Qué es:** las ventas de siempre. **No cambian** (mismos totales, estados y stock), pero por detrás cada una **liga sus piezas a su viaje**.
**Qué esperas ver:** **nada distinto** en pantalla. Se nota en la tarjeta del viaje (suben «vendidas» y «recuperado») y en la base de datos.

---

## 4. Pruebas (en este orden)

> Usa el producto **P** (stock 10). Los números de abajo son los que debes ver. Si algo no coincide, anota **qué paso, qué viste y qué esperabas**.

### Prueba 1 — Registrar dos viajes
**Dónde:** Catálogo → Fletes → ＋ Nuevo viaje.
**Haz:**
1. **Viaje A:** nombre «QA Flete A», fecha de **hace unos días**, flete total **$100**, agrega un gasto «Gasolina» **$40** con «Cuenta para cubrir» marcado. Busca **P**, agrégalo, ponle **5 piezas**, costo el que tenga, y toca **Repartir el flete por igual**. Guarda.
2. **Viaje B:** nombre «QA Flete B», fecha **de hoy**, flete total **$150**, sin gastos, **P con 5 piezas**. Toca «Repartir el flete por igual» y **después cambia tú el flete por pieza de P a $40** (así el viaje reparte $200 aunque solo debe cubrir $150: sobran $50 que serán ganancia). Guarda.

**Debes ver:**
- Viaje A: **costo a cubrir $140**, flete repartido **$140**, falta **$0**, **flete por pieza $28**.
- Viaje B: costo a cubrir **$150**, flete por pieza **$40**, flete repartido **$200** y el resumen dice **«Flete de más $50»** (el sobrante es ganancia).
- Las dos tarjetas en la lista con etiqueta **Abierto**, barra en 0 %.
**NO debe pasar:** que cambie el stock de P o su precio.

### Prueba 2 — Una venta toma del viaje más viejo
**Dónde:** Venta directa → contado.
**Haz:** vende **3 piezas de P**.
**Debes ver:** la venta sale **igual que siempre**. En Fletes, tarjeta de «QA Flete A»: **Recuperado $84** de $140 (3 × $28), **falta $56**, **Vendidas 3 de 5 · quedan 2**.
**NO debe pasar:** error al cobrar, o que el total/stock sean distintos de lo normal.

### Prueba 3 — Una venta que cruza dos viajes
**Haz:** vende **4 piezas más de P**.
**Debes ver:** 2 salen de A y 2 de B.
- **A:** Recuperado **$140** de $140 → etiqueta **Vendido por completo**, falta $0, «cubierto el [hoy]».
- **B:** Recuperado **$80** (2 × $40) de $150, **falta $70**, etiqueta **Abierto**, vendidas 2 de 5 · quedan 3.

### Prueba 4 — «Ya es ganancia»
**Haz:** vende **1 pieza** de P y revisa la tarjeta de «QA Flete B»; repite **una pieza a la vez** hasta vender las 3 que le quedan.
**Debes ver:**
- 1.ª pieza: B recuperado **$120**, falta **$30**.
- 2.ª pieza: lo recuperado **llega a $150** (no pasa de ahí): etiqueta **Flete cubierto** y «ya es ganancia **$10**» (de $160 de flete de esas piezas, $10 sobran).
- 3.ª pieza: «ya es ganancia **$50**» en total y la etiqueta pasa a **Vendido por completo** (ya no quedan piezas).

### Prueba 5 — Ver productos y descargar el reporte del viaje
**Haz:** en la tarjeta de «QA Flete A» toca **Ver productos**, luego **⬇ Excel** y **⬇ PDF**.
**Debes ver:** la tablita (P: compradas 5, vendidas 5, quedan 0). Se descargan los dos archivos: con tarjetas (flete total $100, costo a cubrir $140, recuperado $140, falta $0…), tabla de productos y tabla de **ventas ligadas** con su fila **TOTAL** (piezas 5, flete recuperado $140).
**NO debe pasar:** archivo vacío, que no abra, o números distintos a los de la pantalla.

### Prueba 6 — El reporte por categoría usa el flete real
**Dónde:** Reportes → Por categoría → elige la categoría de P y las fechas de hoy → **Ver reporte**.
**Debes ver:** en la fila de P, **Flete real (viajes)** = lo vendido de P (piezas × flete de su viaje: 3×$28 + 2×$28 + 2×$30 + …) y **Piezas sin viaje = 0**. En la tarjeta «Flete»: «Vendido $… (real $…)». Baja el **Excel** y el **PDF**: traen esas dos columnas.
**NO debe pasar:** que «Flete real» salga en 0 con ventas hechas después de registrar los viajes.

### Prueba 7 — El stock anterior a los viajes sale primero
**Prepara:** un juguete **Q con stock 12**; crea un viaje «QA Flete Q» con **solo 5 piezas** de Q (las otras 7 son «de antes»).
**Haz:** vende **3 piezas** de Q; luego **6 piezas** de Q.
**Debes ver:** las primeras 3 **no** descuentan nada del viaje (son de las 7 de antes). En las 6: **4** son de las de antes y **2** salen del viaje. Tarjeta del viaje: **vendidas 2 de 5**.

### Prueba 8 — Proteger un viaje con ventas
**Haz** (en «QA Flete A», que ya vendió): (a) quítale P y guarda; (b) bájale las piezas de P a 4 y guarda; (c) toca **Eliminar**; (d) cambia solo el **nombre** y el **flete total** y guarda.
**Debes ver:** (a), (b) y (c) te dan un aviso claro («ya se vendieron…», «no puede tener menos de…», «no se puede eliminar…») y **no cambian nada**. (d) sí guarda y las ventas siguen ligadas.

### Prueba 9 — Devolución
**Haz:** devuelve (cancela) la **venta de 4 piezas de la prueba 3** como siempre.
**Debes ver:** esas piezas **vuelven a sus viajes** y dejan de contar. «QA Flete A»: vendidas **3**, recuperado **$84**, falta $56, etiqueta **Abierto** otra vez. «QA Flete B»: vendidas **3**, recuperado **$120**, falta $30.

### Prueba 10 — Ligar ventas anteriores
**Prepara:** un producto **R** con **ventas viejas** (de antes de registrar un viaje) y un viaje «QA Flete R» con piezas de R.
**Haz:** Fletes → «QA Flete R» → **Ligar ventas anteriores** → elige R → **Buscar ventas** → **Llenar de la más vieja a la más nueva** → **Ligar al viaje**.
**Debes ver:** salen las ventas de R no devueltas con piezas sin ligar; liga **máximo** lo que le queda al viaje; «Listo: se ligaron N pieza(s)»; en la tarjeta suben **vendidas** y **recuperado**. Intenta escribir más piezas de las que tiene una venta o más de las que le quedan al viaje: **no debe dejarte** (aviso claro).
**NO debe pasar:** que la venta cambie de total o de ganancia.

### Prueba 11 — Calculadora y bloque en el producto
**Haz:** Productos → Buscar → **Actualizar** el producto P → mira el bloque; luego **🧮 Calcular precio**.
**Debes ver:** el bloque **«🚚 Viajes de compra»** con «QA Flete A» y «QA Flete B» (piezas, costo, flete por pieza). En la calculadora: el aviso del flete registrado, los botones **«Usar el flete de un viaje»** (llenan el transporte), y la casilla del costo total **desmarcada**; si la marcas, el aviso de que se contará dos veces.
**NO debe pasar:** que «Usar este precio» cambie el «Precio de compra» sin que marques la casilla.

### Prueba 12 — Los otros tipos de venta no cambian
**Haz:** con un producto que esté en un viaje: (a) un **Apartado** pagado completo de una vez; (b) un **Ir pagando** con dos abonos; (c) si tienes terminal de pruebas, un **cobro con tarjeta aprobado**.
**Debes ver:** mismos totales, estados y stock que siempre; al liquidarse, el viaje muestra las piezas vendidas.
**NO debe pasar:** que algún cobro falle o quede a medias. **Si pasa, avísame al instante:** el sistema nunca debe tumbar una venta por el viaje.

---

## 4.1 Prueba extra — buscar por categoría, «Otros» e importar un viaje en Campañas

1. **Catálogo → Fletes → Nuevo viaje.** En «Productos que llegaron en el viaje» elige una categoría (p. ej. JUGUETES) sin escribir nada: debe listar sus productos, con páginas de 12.
2. Elige **«Otros (sin categoría)»**: solo salen productos que no tienen categoría.
3. Agrega un producto a mano y después pulsa **«＋ Agregar los N de esta búsqueda»**: se agregan todos los demás y el aviso dice cuántos se agregaron y cuántos se saltaron (el que ya estaba). No debe haber ningún producto repetido en la tabla.
4. **Catálogo → Campañas → Editar:** en el selector de categoría del catálogo existe **«Otros (sin categoría)»** y filtra bien.
5. En el mismo panel, elige un viaje en **«Importar los productos de un viaje (flete)»** y pulsa **Importar**: sus productos entran a la campaña (los que ya estaban no se repiten); quita uno con ✕ y agrega otro a mano: funciona igual que siempre.
⛔ No puede pasar: productos duplicados, error al importar, «Otros» mostrando productos con categoría.

## 4.2 Prueba extra — deshacer una liga manual

1. En **Catálogo → Fletes**, abre «Ligar ventas anteriores» de un viaje y liga una venta (como en la prueba de ligar).
2. Debajo aparece la tabla **«Ligas hechas a mano en este viaje»** con esa venta.
3. Pulsa **↩ Deshacer** y confirma: la liga desaparece, la venta vuelve a salir en «Buscar ventas» con sus piezas pendientes y la barra de «recuperado» del viaje baja.
⛔ No puede pasar: que aparezcan ligas hechas automáticamente al vender, o que cambie el total o la ganancia de la venta.

## 4.3 Prueba extra — flete guardado, campaña y «Llenar el flete que falta» (con datos listos)

**Antes de empezar:** en **Sistema → 🗑️ Limpiar caché → 🧪 Datos de prueba** toca **«Preparar prueba F»**. Crea 6 productos «QA Prueba F …» (A, B y C con flete guardado de $20, $15 y $10 por pieza; D, E y F sin flete guardado), una campaña **apagada** «QA Prueba F Campaña de fletes» con los 6, y dos ventas de «Con flete A» (V1 de 2 piezas y V2 de 3) anteriores a cualquier viaje. No toca tu campaña real. Se puede tocar las veces que quieras: reinicia todo.

1. **Catálogo → Fletes → Nuevo viaje.** Pon flete total $100 y un gasto «gasolina» de $40 que cuenta para cubrir (a cubrir $140).
2. En «Traer los productos de una campaña» elige la de la prueba F y pulsa **Traer**. Salen los 6 con 1 pieza y costo $60. **A, B y C** llegan con su flete ($20, $15, $10); **D, E y F** con $0 y la marca «sin flete guardado». El aviso dice «3 con su flete guardado y 3 sin flete guardado».
3. Vuelve a pulsar **Traer**: dice que los 6 ya estaban y no se duplica nada.
4. Arriba aparece «3 producto(s) sin flete guardado. Faltan por cubrir $…» (cubrir $140 − $45 de A, B y C = **$95**). Pulsa **Llenar el flete que falta**: D, E y F quedan en **$31.67** por pieza y la marca desaparece; A, B y C no cambian.
5. Prueba el otro camino con un viaje nuevo: trae la campaña y **no** pulses el botón: D, E y F se quedan en $0.
6. Guarda el viaje. En su tarjeta abre **Ligar ventas anteriores**, elige «Con flete A» y liga **V1** y **V2** (5 piezas): es lo mismo que la prueba de ligar, sin crear ventas a mano.
⛔ No puede pasar: productos repetidos, que A, B o C cambien su flete al llenar, o que se mueva algún flete solo al cambiar piezas.

## 4.4 Prueba extra — un solo flete entre el viaje y «Calcular precio»

**Antes de empezar:** haz la prueba 4.3 hasta guardar el viaje con los productos de la prueba F (A trae $20 por pieza). Si no la hiciste, toca «Preparar prueba F» y crea el viaje trayendo la campaña de la prueba.

1. **Productos → Buscar** y abre **Actualizar** de «QA Prueba F Con flete A». Pulsa **🧮 Calcular precio**.
2. Pon el costo de compra en $60. **Transporte por pieza sale solo en $20**, con «🚚 Tomado del viaje «…». Puedes cambiarlo.
3. Cambia el transporte a **$30**: aparece el aviso «El viaje … dice $20.00 de flete por pieza y aquí pusiste $30.00. Al guardar el producto te preguntaré…».
4. Pulsa **Usar este precio** y **Guardar** el producto. Sale **«¿Actualizar también el viaje?»**.
   - **Sí, actualizar el viaje:** en **Catálogo → Fletes → Editar** el flete por pieza de «Con flete A» ahora es **$30**; los otros productos del viaje no cambian.
   - **No, dejar el viaje como está:** el flete en el viaje sigue en $20 (el cálculo del producto quedó en $30).
5. Repite el paso 1 y no cambies el transporte (déjalo en lo que salió): al guardar **no pregunta nada**.
6. **Varios viajes:** crea otro viaje (Nuevo viaje, sin campaña) con solo «Con flete A» a $25 y fecha posterior. Abre otra vez «Calcular precio» de A: toma el del viaje **más viejo que todavía tiene piezas por vender** (el primero), no el nuevo.
7. **No pisa lo que escribes:** abre «Calcular precio», escribe tú el transporte antes de que cargue (o bórralo y escribe $18): se queda en $18 y sale el aviso de diferencia.
⛔ No puede pasar: que el viaje cambie sin que respondas «Sí», que cambien otros productos del viaje, o que cambien el total o la ganancia de las ventas ya ligadas.

## 5. Cómo ver lo que el sistema anotó (opcional, en MySQL de QA)

```sql
-- De qué viaje salió cada pieza vendida (las últimas 20)
SELECT v.id, v.detalle_venta_variante_id AS linea, l.nombre AS viaje, v.cantidad, v.flete_por_pieza, v.origen
FROM venta_detalle_lote v
JOIN compra_lote_producto p ON p.id = v.compra_lote_producto_id
JOIN compra_lote l ON l.id = p.lote_id
ORDER BY v.id DESC LIMIT 20;
```
`origen` = AUTO (lo hizo el sistema al vender) o MANUAL (lo ligaste tú en «Ligar ventas anteriores»).

---

## 6. Lo que todavía NO hace (para que no lo esperes)

- **El reporte por categoría no se filtra por viaje**; para ver un viaje usa la tarjeta de Fletes o su Excel/PDF.
- **Las pruebas todavía no se arman solas** (el botón «🧪 Preparar prueba» no está hecho para fletes): los datos se crean a mano como arriba.
- **El stock de antes de los viajes es una aproximación:** el sistema no guarda de qué viaje es cada pieza física. Si ajustas el stock a mano o hay piezas que salen sin venta (rifas, bajas), la cuenta de «piezas sin viaje» puede salir distinta de la real.
- **Una devolución o una edición puede mover «desde qué venta se pagó el flete»** en meses pasados (se calcula siempre con toda la historia del viaje).
- **Nada de esto está en producción.** Para llevarlo hay que correr las dos migraciones en prod y subir back y front, solo con tu orden.

## 7. Si algo sale mal

Avísame con: **la prueba (número), qué hiciste, qué viste y qué esperabas** (una captura ayuda). Lo más importante a vigilar: **que ninguna venta falle** y que los números de «recuperado» cuadren con piezas × flete por pieza.
