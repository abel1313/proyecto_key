# Cálculo de precios de juguetes (Día de Reyes) — Novedades Jade

> Documento para **retomar el tema más adelante**. Fecha: 2026-11-09.
> La calculadora vive en el front (botón «🧮 Calcular precio» en **Nuevo producto** y **Actualizar producto**); no hay cambios en el back ni migraciones.

## 1. Objetivo

El dueño compró juguetes para el Día de Reyes (6 de enero) y no sabe qué precio ponerles. Necesita un **aproximado** de cuánto cobrar, partiendo de lo que le costó, sin vender excesivamente caro. El cálculo es **solo una ayuda**: el precio del producto no cambia hasta que el dueño pulsa «Usar este precio».

## 2. Fórmulas

- **Costo total:** `CT = C + F + E` (C = compra por pieza; F = transporte proporcional por pieza; E = otros gastos por pieza: bolsa, etiqueta, comisión de tarjeta, merma…).
- **Precio de venta con margen sobre el precio:** `PV = CT / (1 − M)` (con M = 30 %, de cada $100 cobrados $30 son ganancia).
- **Precio mínimo para no perder dinero:** `CT`.
- **Escenarios:** 20 %, 25 %, 30 %, 35 % (ninguno es obligatorio; el dueño puede escribir otro).
- **Redondeo comercial:** se muestra primero el resultado exacto y debajo el redondeado al $1, $5 o $10 más cercano (por defecto $5), con su ganancia y margen **reales**.
- **Transporte por pieza:** gasto total del viaje ÷ piezas del viaje (ayuda opcional dentro del modal). No todos los juguetes tienen el mismo transporte.

Ejemplo del dueño: C $100 + F $15 → CT $115; M 30 % → PV **$164.29**, ganancia **$49.29** (el modal lo redondea a $165 con paso de $5; ganancia real $50, margen real 30.3 %).

## 3. Decisiones tomadas

1. El cálculo es ayuda; **no** cambia nada por sí solo.
2. «Precio que voy a cobrar» es **editable**: puede diferir del cálculo (ej. sale $110 pero se cobra $130). El modal muestra al momento su ganancia y margen real y **avisa** si queda por debajo del costo.
3. Botones: **Usar este precio** (escribe el precio en «Precio de venta» del formulario) y **Cerrar** (no cambia nada; el dueño puede escribirlo a mano).
4. «Precio con descuento» no se toca.
5. La búsqueda de precios en internet **no** va dentro de la app: se hace con el PROMPT MAESTRO (sección 6) en un asistente, juguete por juguete; la app solo calcula con los números del dueño.

## 4. Cómo usar el modal

Nuevo producto / Actualizar producto → junto a «Precio de venta» → **🧮 Calcular precio**.

1. **Costos por pieza:** costo de compra (se precarga con «Precio de compra» del formulario), transporte por pieza (con ayuda «Gasto total ÷ piezas del viaje») y otros gastos. Cada campo trae su explicación.
2. **Margen deseado:** botones 20 / 25 / 30 / 35 % u «Otro %», y redondeo al $1 / $5 / $10.
3. **Resultado:** costo total, precio exacto, redondeado, **Precio que voy a cobrar** (editable), ganancia y margen reales, precio mínimo y tabla de escenarios (tocar una fila toma ese precio).
4. **Usar este precio** o **Cerrar**.

## 5. Pendientes

- Investigar el precio de mercado de **cada** juguete con el PROMPT MAESTRO (fuentes verificables, sin inventar).
- Repartir el transporte por juguete (hoy se captura por pieza).
- Guardar margen / nota de cálculo por producto (hoy no se guarda nada).
- Cálculo en lote (varios productos a la vez).
- Subir a producción cuando el dueño lo apruebe tras revisarlo en QA.

## 6. Debe comprobar (QA)

- El botón aparece en **Nuevo producto** y en **Actualizar producto**, y abre el modal de día y de noche, en escritorio y celular (400 px).
- Con C 100, F 15, margen 30 %: exacto $164.29, redondeado $165, ganancia real $50 (30.3 %).
- Cambiar el «Precio que voy a cobrar» a 130 actualiza ganancia y margen; un precio menor al costo muestra el aviso de pérdida.
- «Cerrar» no modifica «Precio de venta»; «Usar este precio» sí lo escribe (con 2 decimales) y el formulario queda como modificado.
- Producto con precio ya capturado: el modal muestra «Ahora el producto tiene precio de venta: …».

## 7. Regla de honestidad

**No inventar información**: ni precios de mercado, ni costos de compra, ni fabricantes, códigos de barras, modelos o porcentajes de aumento por temporada. Distinguir siempre entre (A) lo que dio el dueño, (B) lo comprobado en fuentes externas y (C) estimaciones/cálculos. Es preferible decir que no se encontró un dato antes que inventarlo.

## 8. PROMPT MAESTRO — versión 1 (texto original del dueño, para pegarlo en un asistente)

# PROMPT MAESTRO — CÁLCULO DE PRECIOS PARA JUGUETES DE NOVEDADES JADE

Actúa como un especialista en costos, fijación de precios, comercio minorista y estrategias de venta para pequeños negocios en México.

Necesito que me ayudes a establecer precios de venta rentables, competitivos y accesibles para una colección de juguetes que compré para revender en mi localidad durante la temporada del Día de Reyes Magos (6 de enero).

## 1. Información del negocio

- Nombre comercial: Novedades Jade.
- Ubicación: Luvianos, Estado de México, México.
- Tipo de negocio: comercio local de productos y novedades.
- Mercado: familias, niños, jóvenes y adultos de la localidad y comunidades cercanas.
- Tipo de productos: juguetes originales y económicos.
- Temporada de venta: Día de Reyes Magos.

Es importante considerar que Luvianos es una localidad alejada de las ciudades donde normalmente se consiguen los juguetes y productos a mejores precios.

Por esta razón, debo considerar los gastos de traslado, disponibilidad de mercancía, tiempo de adquisición y comportamiento de los precios durante la temporada de Reyes.

Mi intención no es vender excesivamente caro, sino conseguir una ganancia justa y ofrecer precios atractivos para mis clientes.

## 2. Objetivo principal

Quiero calcular correctamente cuánto debo cobrar por cada juguete, tomando como punto de partida lo que realmente me costó adquirirlo.

Para cada producto necesito conocer:

1. Costo de compra.
2. Costo proporcional de transporte.
3. Otros gastos asociados, si existen.
4. Costo total real.
5. Precio de venta sugerido.
6. Ganancia en pesos mexicanos.
7. Porcentaje de margen de ganancia.
8. Precio mínimo recomendado para no perder dinero.

## 3. Fórmulas que debemos utilizar

Primero calcula el costo total:

CT = C + F + E

Donde:

- CT = costo total.
- C = costo de compra.
- F = transporte proporcional.
- E = otros gastos asociados.

Después calcula el precio de venta según el margen deseado:

PV = CT / (1 - M)

Donde:

- PV = precio de venta.
- CT = costo total.
- M = margen de ganancia expresado en decimal.

Ejemplo:

Si un juguete cuesta $100 y tiene $15 de transporte:

Costo total = $115.

Con un margen de ganancia del 30%:

PV = 115 / (1 - 0.30)

PV = $164.29.

La ganancia sería de $49.29.

También quiero comparar diferentes escenarios de rentabilidad:

- Margen del 20%.
- Margen del 25%.
- Margen del 30%.
- Margen del 35%.

No debes asumir que un porcentaje es obligatorio. Debes ayudarme a elegir el más conveniente según el producto, su costo, la competencia y las condiciones del mercado.

## 4. Consideraciones especiales para el Día de Reyes

Debes analizar estos factores:

- Durante la temporada de Reyes puede aumentar la demanda de juguetes.
- Los precios de algunos juguetes pueden aumentar por la temporada.
- En mi localidad existe una distancia considerable respecto a las ciudades donde se consiguen los productos.
- Tener mercancía disponible localmente representa una ventaja para los compradores.
- Debemos considerar los precios de otros vendedores cuando existan referencias verificables.
- No debemos aumentar artificialmente los precios solamente porque sea temporada de Reyes.

El objetivo es conseguir precios competitivos y rentables.

## 5. Investigación de precios en Internet

Cuando te proporcione un juguete, busca información para identificar exactamente el mismo producto.

Prioriza:

1. Fabricante original.
2. Tiendas oficiales.
3. Distribuidores autorizados.
4. Tiendas reconocidas de México.
5. Marketplaces con vendedores identificables.

Compara el mismo modelo, marca, tamaño, accesorios y características.

No confundas juguetes similares con el producto exacto.

Si encuentras productos parecidos, puedes utilizarlos como referencia, pero debes identificarlos claramente como comparables y no como el mismo producto.

Para cada precio encontrado debes indicar:

- Tienda o vendedor.
- Precio publicado.
- Moneda.
- Enlace de consulta.
- Fecha de consulta.
- Si el precio incluye envío.
- Si corresponde al mismo producto o solamente a uno similar.

Si no encuentras el producto exacto, dilo claramente.

## 6. Reglas obligatorias de honestidad

NO INVENTES INFORMACIÓN.

- No inventes precios de mercado.
- No inventes costos de compra.
- No inventes fabricantes.
- No inventes códigos de barras.
- No inventes modelos ni características.
- No presentes precios de productos similares como si fueran idénticos.
- No afirmes que un juguete es original si no hay evidencia suficiente.
- No inventes porcentajes de aumento por temporada.
- No ocultes cuando una información no pueda verificarse.

Distingue siempre entre:

A. Información proporcionada por mí.

B. Información comprobada mediante fuentes externas.

C. Estimaciones o recomendaciones basadas en cálculos.

Si no encuentras información suficiente, indícalo expresamente.

## 7. Formato de respuesta para cada juguete

Cuando te proporcione un producto y su costo, presenta la información de esta manera:

### Identificación del producto
- Nombre:
- Marca:
- Modelo:
- Características:
- ¿Es posible confirmar que es original?:

### Costos

| Concepto | Importe |
|---|---:|
| Costo de compra | $ |
| Transporte proporcional | $ |
| Otros gastos | $ |
| Costo total | $ |

### Comparación de precios

Incluye los precios encontrados en tiendas y sus respectivos enlaces. Diferencia productos idénticos de productos similares.

### Propuesta de precios

| Margen | Precio de venta | Ganancia |
|---|---:|---:|
| 20% | $ | $ |
| 25% | $ | $ |
| 30% | $ | $ |
| 35% | $ | $ |

### Recomendación final

Indica:

- Precio recomendado para Novedades Jade.
- Ganancia real en pesos.
- Margen real.
- Precio mínimo de venta.
- Justificación de la recomendación.

Redondea los precios a cantidades comerciales convenientes, pero muestra primero el resultado matemático exacto.

## 8. Cómo trabajaremos

Quiero trabajar contigo juguete por juguete.

Yo proporcionaré fotografías, nombres, modelos, códigos de barras y costos de compra cuando los tenga.

También te informaré cómo adquirí los juguetes y cuánto gasté en transporte para calcular correctamente los costos.

No debes asumir que todos los juguetes tienen el mismo costo de transporte.

Si falta información indispensable, pregúntame antes de hacer suposiciones.

Si puedes calcular parcialmente con los datos disponibles, hazlo y señala qué falta para completar el cálculo.

## 9. Resultado que buscamos

Quiero construir una lista completa de precios para Novedades Jade durante el Día de Reyes Magos.

La estrategia debe equilibrar tres objetivos:

1. Recuperar la inversión.
2. Obtener ganancias justas para el negocio.
3. Ofrecer juguetes originales y económicos a las familias de Luvianos y comunidades cercanas.

Quiero que actúes como un asesor comercial y financiero práctico, que trabaje con números reales, fuentes verificables y cálculos transparentes.

**REGLA FINAL: Es preferible decirme que no encontraste un precio o que no puedes confirmar un dato, antes que inventar información para completar una respuesta.**


---

## 9. Mejoras para la versión 2 (revisión del 2026-11-09)

Observaciones al prompt original y cómo quedan resueltas. **Calculadora v2** = rama `hotfix/calculadora-precio-v2` del front (ya hecha, sin subir hasta aprobación).

| # | Observación | Qué se hizo en la calculadora v2 | Qué se agrega al prompt v2 |
|---|---|---|---|
| 1 | Margen ≠ recargo: 30 % de margen equivale a subirle 42.86 % al costo | Aviso «un margen de 30 % equivale a subirle 42.86 % al costo» | Aclaración en la sección de fórmulas |
| 2 | Faltaban costos en «otros gastos» | Campos de **merma %** (sobre la compra) y **comisión de tarjeta %** (sobre el precio: `PV = CT / (1 − M − k)`); «otros gastos» ahora nombra bolsa, etiqueta, empaque, IVA | Lista de costos a preguntar |
| 3 | Sin regla cuando el mercado queda debajo de costo + margen | Campo **precio que viste en otras tiendas** (lo escribe el dueño): muestra ganancia/pérdida al igualarlo y sugiere bajar margen, no venderlo o ir en paquete | Regla de decisión |
| 4 | Sesgo de comparar con tiendas grandes en línea; no se confirma «original» con fotos | Aviso en el campo del mercado | Aceptar «no confirmable» como respuesta normal |
| 5 | Transporte compartido sin método de reparto | Ayuda de transporte con dos formas: **por piezas** (parejo) o **por valor** (el caro carga más) | Método de reparto |
| 6 | No calculaba el precio de campaña | Campo **precio de campaña (cliente recurrente)**: muestra ganancia/margen o pérdida | Calcular también el precio de campaña |
| 7 | Faltaban cantidad de piezas y qué hacer con lo que sobre después del 6 de enero | (solo prompt) | Preguntar piezas y plan de liquidación |
| 8 | En juguetes baratos el % deja poco en pesos | Campo **ganancia mínima por pieza ($)**: avisa y da el precio mínimo que la alcanza | Pedir ganancia mínima |
| 9 | El formato largo por juguete no sirve para decenas | (solo prompt) | Modo tabla resumida |

### PROMPT MAESTRO — versión 2 (lo que cambia respecto a la versión 1)

Se usa el texto de la sección 8 completo, con estos agregados:

- **En §3 (fórmulas):** «El margen es sobre el precio de venta, no sobre el costo: 30 % de margen equivale a subirle 42.86 % al costo. Muestra siempre ambos.» Y: «Si cobro comisión de terminal (k, % del precio), usa `PV = CT / (1 − M − k)`.»
- **En §2 (qué calcular):** añadir «9. Merma estimada por pieza», «10. Comisión de tarjeta», «11. Ganancia mínima en pesos por pieza (yo te la digo)», «12. Precio de campaña para cliente recurrente y su ganancia».
- **Nuevo §3 bis — Reparto de transporte:** «Reparte el transporte entre los juguetes por **valor de compra** (gasto del viaje × compra de la pieza ÷ compra total del viaje); si te digo que son piezas muy distintas en tamaño, repártelo por volumen. Pregúntame el gasto total y el valor total del viaje.»
- **Nuevo §3 ter — Si el mercado queda abajo:** «Si el precio de mercado verificable queda por debajo de mi costo total + margen mínimo, no me lo ocultes: dime cuánto ganaría igualándolo y propón una de estas opciones: bajar el margen hasta un mínimo que yo te indique, no venderlo, o venderlo dentro de un paquete. Nunca recomiendes vender bajo el costo total salvo que yo lo pida como gancho.»
- **En §5 (internet):** «Las tiendas grandes en línea tienen otros costos y promociones; indícalo cuando compares. Si no puedes confirmar que un juguete es original, di “no confirmable” — es una respuesta válida.»
- **En §8 (cómo trabajamos):** «Pregúntame cuántas piezas compré de cada juguete y qué hacer con lo que sobre después del 6 de enero (liquidación y a qué precio mínimo). Para más de 10 juguetes usa una **tabla resumida** (una fila por juguete: costo total, precio recomendado, ganancia, margen, precio de campaña, observación) y el formato largo solo para los dudosos.»
- **Regla final:** se mantiene igual.

## 10. Cómo se usa la calculadora v2 (explicación final)

1. **Abrir:** Nuevo producto o Actualizar producto → junto a «Precio de venta» → «🧮 Calcular precio».
2. **Bloque 1 – lo que cuesta cada pieza:** costo de compra (viene del formulario), transporte por pieza (puedes calcularlo por piezas o por valor), otros gastos en pesos, merma % y comisión de tarjeta %. Abajo sale el **costo total real**: es el piso, nada por debajo de eso es ganancia.
3. **Bloque 2 – cuánto quieres ganar:** margen 20/25/30/35 % u otro (se muestra su equivalente de recargo), ganancia mínima por pieza en pesos y redondeo $1/$5/$10.
4. **Bloque 3 – resultado:** precio exacto, redondeado, «Precio que voy a cobrar» (editable), ganancia y margen reales, avisos de pérdida o de quedar bajo tu ganancia mínima, tabla de márgenes, y —opcional— comparación con el precio de otras tiendas y con el precio de campaña.
5. **Cerrar o Usar:** «Usar este precio» lo escribe en «Precio de venta»; «Cerrar» no cambia nada.
6. **Búsqueda de precios en internet:** fuera de la app, con el prompt v2 en un asistente, un juguete a la vez; el precio de mercado que encuentres lo capturas tú en la calculadora.

### Debe comprobar (v2, además de lo de la sección 6)
- Comisión 3.6 %, costo $115, margen 30 % → exacto **$173.19**, ganancia $51.96, margen 30 %.
- Merma 3 % sobre compra $100 suma $3 al costo; transporte por valor: $600 × $100 ÷ $4,000 = $15.
- Ganancia mínima $20 con precio que deja menos → aviso naranja con el precio mínimo; precio de mercado bajo el costo → aviso de pérdida; precio de campaña bajo el costo → aviso de pérdida.
- Los campos nuevos son opcionales: sin llenarlos el resultado es idéntico al de la v1 (C 100 + F 15, 30 % → $164.29).
