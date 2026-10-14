# Pruebas que faltan — paso a paso (al 2026-10-13)

Pediste el 2026-10-13: *"quita del doc las pruebas que ya pasaron y solo deja las que hacen falta,
porque me confundes; si quieres haz un nuevo documento"*. Este es ese documento: **solo lo que falta**.
El de antes (`PRUEBAS_PENDIENTES_2026-10-07.md`) se queda como historial, con tus 💬.

**Ya pasaron (no vienen aquí):** 1, 2, 3, 4, 5, 7, 8, 9, 10.2, 11 y 14 (en prod), y los casos 3.6 y 3.7.

**📍 Llevamos (se hacen una por una, con los datos que crea 🧪 Preparar prueba N):**

| Prueba | Estado | Cuándo |
|---|---|---|
| **24** — Unir un Ir pagando que ya tiene dinero (K4 / K6) | ✅ Pasó | 2026-10-13 |
| **22** — 🕓 Pendiente en Pago y Mis pedidos sin cancelados al abrir | ✅ Pasó | 2026-10-13 |
| **21** — La fecha de entrega en Pedidos y en Entregas por zona (y el nombre del cliente de la venta) | ✅ Pasó | 2026-10-13 |
| **20** — Cliente que no podía pedir, grupo, modelo agotado y Mis pedidos (20.2 y 20.3 con botón) | 👉 **Sigue**: 20.2 ✅ y 20.4 ✅ (2026-10-14). Falta **20.3** (pasos 1, 2 y 3 ✅; sigue en el 4: la ventana para agregar stock al modelo ya pasó, faltan la pregunta de cuántos artículos y los checks por artículo, ya subidos a QA) y **20.1** (sin reportar) | |
| **16** — 🕓 Pendiente, cambiarlo a Apartado, Entregas por zona, ⓘ | ⏳ Falta | |
| **25** — Mis pedidos: buscar y poner/quitar filtros ya no tarda | ✅ Pasó | 2026-10-14 |
| **26** — Mis pedidos con caché: rápido y sin datos viejos | ✅ Pasó | 2026-10-14 |

Las demás (12, 15, 14-QA, 17, 18, 19, 23, 13 y 6) siguen en la tabla de abajo, sin hacer.

**Lo nuevo de este documento:**
- **🧪 Preparar prueba N:** un botón que da de alta **todo** lo que necesita cada prueba: los pedidos y
  también los **modelos y artículos** (Art-A de $100, Art-200, M-OFF, el modelo agotado…). Ya no tienes
  que armarlos, buscarlos ni anotarlos tú. Y **🧪 Preparar todas las pruebas** deja listo todo el
  documento de un clic (ver "🧪 Preparar prueba", abajo).
- **Prueba 12** reescrita: cada cosa dice **en qué menú y en qué pantalla** se revisa.
- **Prueba 23** (nueva): los colores de noche que reportaste, **las listas que se abrían en blanco**, el chat de día y el botón **📍 Dirección**.
- **Prueba 24** (nueva): lo de K4 / K5 / K6.
- **2026-10-14:** el 🧪 también da de alta lo que no es catálogo ni pedidos: la **rifa con el plazo vencido** y los
  **códigos de Agregar mi compra** de la 19, y los **usuarios de prueba** con su rol (**qa.prueba17** y **qa.prueba18**).
  A esos usuarios les pones contraseña tú en **Sistema → Usuarios → Resetear contraseña**; nadie más la sabe.

Cada prueba trae:
- **Para qué es** — en una línea.
- **Antes de empezar** — qué preparar (casi siempre: tocar **🧪 Preparar prueba N**).
- **Mapa de impacto** (cuando aplica) — qué se movió, a qué le pega, qué daba **antes** y qué debe dar **después**.
- **Pasos** — a la izquierda lo que **haces**, a la derecha lo que **debes ver**.
- **❌ Está mal si…** — lo que sería un error.

**Cómo anotar:** si algo no sale como dice "Debes ver", escribe debajo de esa tabla `💬` y lo que
pasó (qué hiciste, qué esperabas, qué salió, número de pedido). Al terminar una prueba marca su
casilla `[x]`. Yo leo tus 💬 y contesto debajo con `↳`.

---

## Tus notas del 2026-10-13 y qué se hizo

💬 *"Para el 1, si pongo Apartado pero ingreso un dinero, lo cambia a Ir pagando: pasó. 2: ya agregué
artículos a los pedidos. Se pudo cancelar: pasó el 3. 4: ya veo Pendiente. 5: ya lo veo con el mensaje
de si ya lo entregó o no."*
↳ Pruebas 2, 3, 4, 7 y 11 marcadas como pasadas; ya no vienen en este documento.

💬 *"Revisa los colores, mira cómo se ve de feo de noche"* (ventana de cancelar), *"7: los fondos se ven
muy feos, y en el menú las opciones de los submenús, como son links y es de noche, se ven como azul o
negro, no se ven"* y *"cuando paso el mouse por un submenú se pone en color negro"*. *"Mira cuál estoy
usando para que lo revises, solo esos temas."*
↳ Revisé solo tus dos temas: **Día "personalizado a mano"** y **Noche "Jade"**. La causa: en el archivo
de colores de la noche había **dos comentarios sin cerrar** que se "comían" los colores que venían
después (el del texto del submenú, el del fondo al pasar el mouse y el de las tarjetas). Por eso el
navegador usaba los de fábrica: azul de enlace y negro. Además, la ventana de cancelar tenía colores
fijos de día, y la regla de letras de Jade pintaba encima de las casillas de los filtros. Arreglado en
el front; se revisa en la **Prueba 23** y en la **Prueba 13**.

💬 *"En pedido hay 2 Entrega: hay que cambiar el primero por Dirección o algo que tenga que ver con
mapa, para que los 2 no tengan el mismo nombre."*
↳ El botón **📍 Entrega** de la tarjeta ahora dice **📍 Dirección** (abre lo mismo: a quién, dónde y
cuándo se entrega). El de **📦 Entregar** no cambia. Hay un script de solo texto para que Gestión de
roles diga lo mismo: `migration_accion_editar_entrega_direccion.sql`. **Prueba 23.**

💬 *"12 solo dice lo legal, pero no dice la descripción en dónde."*
↳ La Prueba 12 ahora empieza con una tabla **"Dónde se revisa cada cosa"**, con el menú, la pantalla
y el botón de cada punto.

💬 *"Cuando un pedido es Ir pagando y doy un tanto cuando lo piden, después, cuando quiero unir el K4
con el K5, el K6 (con un tanto) no aparece en el buscador del detalle de un pedido para unir."*
↳ Lo revisé en una base MySQL de prueba con los mismos servicios de QA: **un Ir pagando con dinero sí
sale en el buscador de "🔗 Unir"** y se une sin problema (creé uno sin dinero y otro con $40: desde el
primero, el buscador encuentra al segundo y los une). El buscador solo deja fuera a los
**cancelados**, a los **cobrados de contado** (Entregado), a los que ya están **en otro grupo** y a los de
**otra forma de cobro**. K6, en la Prueba 4, se terminó de pagar y en la **4.6 se canceló** (devolución):
por eso ya no sale. Para que lo compruebes con pedidos nuevos: **Prueba 24** (con su botón).

💬 *"Revisa bien todo porque vi unos select que cuando salían todas las opciones aparecía en blanco y no
se veían; revisa bien y genera skill para cosas necesarias."*
↳ Lo medí en lugar de mirarlo: abrí cada pantalla con lista con los **5 diseños** de Personalización y
con los colores de fábrica, de día y de noche, y calculé el contraste entre la letra de las opciones y
el fondo del menú (lo legible es 4.5 o más). Con los 4 diseños de estilo "Clásico" (Clásico, Jade
profundo elevado, Neutros cálidos y Teal), **de noche 11 de 12 listas daban 1.1 a 1.2**: menú blanco con
letra casi blanca, lo que viste. Con Jade y de día salían bien. La causa: el menú lo dibuja el navegador
y tomaba el fondo, la letra y el modo claro/oscuro de tres lugares distintos. Ahora los tres salen de un
mismo trío que se calcula solo con tus colores de Personalización. Después del arreglo, el peor caso es
9.2, en los 5 diseños, de día y de noche. También aguanta un diseño armado a mano con fondo oscuro de
día. Quité 4 parches viejos que forzaban el menú blanco. De paso revisé **todo el texto** de 13
pantallas con tus temas: de noche nada; de día, **"Asistente Virtual"** en el encabezado del chat salía
blanco sobre claro y el **"$" verde de Reportes** casi no se leía. Arreglados. Skills nuevas:
`revision-colores` (cómo medir esto), `preparar-prueba` (los botones 🧪) y `probar-en-local`. Se
revisa en la **Prueba 23** (23.5 y 23.6).

💬 *"Hay que subir a dev y a qa, y para las pruebas, me refiero a los artículos, es decir que ya estén
los pedidos de las pruebas como los pedí; también genera una skill para esto de las pruebas: dar de alta
lo que se necesita para las pruebas."*
↳ Subido a `dev` y `qa`. Los botones 🧪 ahora también dan de alta los **modelos y artículos** de cada
prueba, ya en el estado que pide: **Art-200** y los dos tickets (12), el modelo con stock 3 (14-QA), los
artículos viejos deshabilitados que guardan stock (15), el modelo de los filtros, **Art-A** de $100, uno
con descuento que se vende a $100 y uno con stock 1 (17), **M-OFF** y **M-0** (18) y el modelo
**agotado** (20.3). Cada uno sale con su nombre y su código de barras (tócalo y se copia, para pegarlo en
el buscador). Hay un botón nuevo, **🧪 Preparar todas las pruebas**. Lo comprobé en una base MySQL de
prueba con el back completo: las 10 pruebas quedan listas y cada cosa queda en el estado que pide su
prueba (stock, habilitado, precio, tipo de pedido, abonos). La skill `preparar-prueba` quedó con la
regla: **ninguna prueba te pide que busques, anotes o armes datos**; si lo necesita, trae su botón.

💬 *"Lo que estoy pensando es que generes los scripts para generar las pruebas que necesitas; si
necesitas varios pedidos, los agregas, y yo hago las pruebas en pedidos. Cada prueba así hay que
hacerla, para no perder tiempo en generar pedidos. ¿Cómo ves?"*
↳ Hecho, pero con botones en lugar de scripts SQL: los pedidos salen de **los mismos servicios que la
pantalla** (venta directa, carrito del cliente, abono, cancelar), así se comportan igual que los
tuyos y no hay que correr nada en la base. Ver **🧪 Preparar prueba** abajo. Hoy hay botón para las
pruebas que necesitan pedidos: **16, 20, 21, 22 y 24**. Las demás no necesitan pedidos (o necesitan un
modelo o un dato que se elige en la pantalla). *(Ampliado en la nota de abajo: ahora también los
modelos y artículos.)*

---
## Antes de todo (una sola vez, 5 minutos)

| # | Haz esto | Debes ver |
|---|---|---|
| 1 | Abre QA: `qa.shop.novedades-jade.com.mx` | La tienda |
| 2 | Entra con tu usuario **administrador** | Tu nombre abajo en el menú lateral |
| 3 | Presiona `Ctrl + Shift + R` (recarga forzada) | La página se recarga. Hazlo **cada vez** que yo suba algo |
| 4 | (Ya se subió, 2026-10-13.) Corre en `inventario_key_qa` el script `migration_accion_editar_entrega_direccion.sql` (está en `src/main/resources/static/`). Al final trae una consulta | La consulta devuelve **1 fila** con *"Dirección (📍 en la tarjeta)"*. No hace falta volver a entrar |
| 5 | **Sistema → 🗑️ Limpiar caché** → **🧪 Datos de prueba** → **🧪 Preparar todas las pruebas** → **Sí, preparar todas** (ver "🧪 Preparar prueba", abajo). Ya **no** hay que buscar ni anotar Art-A ni Art-200: los da de alta el botón | Un recuadro **✅ Prueba N lista** por cada prueba (12), con sus pedidos, modelos y artículos. Déjala abierta en otra pestaña: de ahí sacas los números y los códigos |

**Para abrir un pedido:** menú **📋 Pedidos → Mis pedidos** → escribe su número en el buscador → en
su tarjeta toca **👁 Detalle**. (Desde **🧪 Preparar prueba**, el número del pedido ya es un enlace que
abre su detalle.)

### 🧪 Preparar prueba: los pedidos, modelos y artículos se crean solos

**Dónde:** menú lateral **Sistema → 🗑️ Limpiar caché** → tarjeta **🧪 Datos de prueba** (solo la ve el
administrador) → abajo, **Preparar una prueba**.

| # | Haz esto | Debes ver |
|---|---|---|
| 1 | Toca **🧪 Preparar prueba N** (la que vas a hacer) | El botón dice **Preparando…** unos segundos |
| 2 | — | Un recuadro **✅ Prueba N lista** con: los **modelos y artículos** (su **clave** como la usa la prueba: Art-A, M-OFF…; su nombre; su **código de barras**, que al tocarlo se copia, y qué es) y los **pedidos** (clave: P1, A, K4…; su **número**, que es un enlace al detalle; el **nombre con el que se ve en pantalla**, por ejemplo «Prueba 24 K4»; y qué es). Abajo, 👉 por dónde seguir |
| 3 | Si la prueba lo pide, **anota los números** en la tabla de "Antes de empezar" | — |
| 4 | **O todas de una vez:** **🧪 Preparar todas las pruebas** → **Sí, preparar todas** | El botón dice **Preparando todas…** y sale un recuadro por prueba. Si alguna falla, sale en **⚠️ rojo con el motivo** y las demás quedan listas |
| 5 | **Si algo salió mal en una prueba:** vuelve a tocar su **🧪 Preparar prueba N** | **Reinicia** la prueba: primero quita lo de antes y luego la crea otra vez. En su recuadro dice **♻️ Se quitó lo de antes: 2 pedidos cancelados y 1 modelo dado de baja** |
| 6 | **🗑 Quitar** (en cada prueba) o **🗑 Quitar todas** | Pide confirmación y **solo quita**, sin volver a crear. Avisa cuántos pedidos canceló y cuántos modelos dio de baja. Úsalo al terminar para dejar QA limpio |

**Nombres exactos (para que lo que dice este documento sea lo que ves):**
- **Modelos:** **"QA Prueba 17 Art-A"** (la prueba y la clave). Las pruebas de pedidos usan además un modelo
  **"QA Prueba 24 Artículo"** con **un** artículo de **$150** y stock 30. Todos con marca **"Prueba QA"**, código
  `2098…` y una foto de las que ya hay en QA.
- **Pedidos:** el **cliente** de cada pedido se llama como la prueba y su clave: el pedido **K4** de la Prueba 24
  es del cliente **«Prueba 24 K4»**; **T1** de la Prueba 12, «Prueba 12 T1»; y así todos. **Es lo que ves en la
  tarjeta del pedido y lo que buscas** en **Mis pedidos** o en **🔗 Unir**: escribe «Prueba 24 K4», o solo «Prueba 24»
  para traer todos los de esa prueba. También sirve el **número** del pedido.
- **Los buscadores siguen su regla:** de texto piden **3 letras o más** (por eso «Prueba 24», nunca «K4» solo) y de
  número de pedido basta 1 dígito.
- Los clientes de los 🕓 Pendiente tienen cuenta (un correo que termina en `.invalid`, así que **no le llega ningún
  correo a nadie**); los de venta directa no tienen correo ni teléfono. La zona, cuando la prueba la necesita, es
  **"Zona Prueba QA"** (envío $0; se crea la primera vez). Los pedidos llevan además la nota `[DATOS DE PRUEBA]`
  (no se ve en pantalla).

**Si algo sale mal, se reinicia.** Si una prueba te sale mal a la mitad, o ya vendiste, cancelaste o uniste lo
que tenías, vuelve a tocar **🧪 Preparar prueba N**: primero **quita lo de la vez anterior** (sus pedidos se
**cancelan**, el stock regresa, y sus modelos se **dan de baja**) y luego la **crea otra vez** con los mismos
nombres. **🗑 Quitar** solo quita. **Nada se borra:** los pedidos cancelados no salen en Mis pedidos al abrir (se
ven con ❌ Cancelado) y los modelos dados de baja quedan deshabilitados en Catálogo → 🔍 Modelos. Si buscas por
nombre y salen un modelo deshabilitado y uno habilitado, el bueno es el **habilitado**; con el código de barras
sale solo el que acabas de crear.

La lista del recuadro 🧪 **se queda guardada** en este navegador aunque cambies de pantalla, con la hora en que se
preparó cada prueba; se quita con *🧹 Quitar la lista* (solo de la pantalla: no borra nada de la base).

**Lo que escribes tú — y lo que NO.** Todo lo que una prueba necesita **de arranque** lo crea 🧪 con código:
modelos, artículos, precios, stock, clientes, pedidos, abonos, ventas y cancelados. **Tú no metes ningún dato de
arranque.** Las pruebas de **pedidos** (24, 22, 21, 20.2, 16 y los tickets de la 12) **no te piden escribir nada**:
tocas el botón y haces los pasos de pantalla (buscar, unir, cancelar…). Lo único que escribes es en las pantallas
que **son** la prueba: si el código las llenara, no se estaría probando la pantalla. Son pocas, y el nombre exacto
ya viene en cada paso:

| Prueba | Pantalla que se prueba | Qué escribes |
|---|---|---|
| 12.1 | ⚖️ Datos legales | nombre `Prueba 12 Negocio`, domicilio `Calle 1, Luvianos, Edo. Méx.`, teléfono `(55) 1234-5678`, horario `Lunes a sábado 10 a 19` |
| 12.5 y 20.1 | Crear una cuenta (necesita **un correo tuyo** para el código) | nombre `Prueba 12 Registro` / `Prueba 20 Cliente nuevo` |
| 15 (6, 6c) | Zonas de entrega | nombre `Prueba 15 Zona` / `Prueba 15 Estanco` |
| 17.4 | Cliente de Venta directa (los avisos de 1 o 2 letras) | `a`, `Lo`, `Jo`… (son las pruebas) y `Prueba 17 Ana` |
| 17.7 | Agregar modelo | `QA Prueba 17 Modelo nuevo`, precio `100`, stock `10`, color `Negro`, marca `Prueba QA`, descripción `Prueba`, primera categoría, una foto |
| 20.3 | Editar artículo y la ventana ➕ Agregar stock al modelo; 🧩 Agregar artículos con checks | artículo `1`, modelo `10`; 2 artículos: talla `S` stock `1` con descripción `Prueba 20 artículo S`, talla `L` stock `1` color `Rojo` y una foto cualquiera; `9` y otra vez `2` en ¿Cuántos artículos? |
| 20.3 | Agregar modelo con stock 0 (no debe dejar) | `QA Prueba 20 Modelo sin stock`, marca `Prueba QA`, precio de compra `50`, precio de venta `100`, stock `0`, descripción `Prueba`, **Generar código automático** prendido |
| 26 | Abono, cambiar forma de cobro, Entregar y Cancelar (lo que se prueba es que la lista lo enseñe al instante) | abono `30` y luego `70`, en efectivo |
| 19.1 | Participante de rifa (no debe dejar: el plazo ya venció) | nombre `Prueba`, apellido `Diecinueve`, teléfono `5512345678`, palabra clave `QAPRUEBA19` |
| 19.1 | Agregar mi compra | `codigo-que-no-existe` y el código de **CU** (se copia del recuadro 🧪) |
| 21.1 | Entregas por zona (fecha de entrega) | fecha, hora y punto de encuentro `Prueba 21 Punto` |

Los 🕓 Pendientes que crea el botón llevan la zona **"Zona Prueba QA"**: así no se cancelan solos a los
pocos días (los que recogen en el local sí se cancelan, 2 días después de su fecha para recoger). Puedes
regresar a una prueba días después y siguen ahí.

❌ **Está mal si:** sale *"No se pudo preparar la prueba N"* — copia el mensaje en tus 💬. En prod el
botón siempre dice *"…solo se pueden generar en la base de QA…: no se hizo nada"*, y está bien.

---

## Resumen: qué falta y cuándo se puede

Todo lo del 2026-10-13 (colores de noche, botón 📍 Dirección, 🧪 Preparar prueba con modelos y artículos,
y Preparar todas) **ya está en `dev` y `qa`**: todas se pueden hacer hoy. La columna 🧪 dice qué botón
prepara sus datos (con **Preparar todas** ya quedan todos).

| Orden | Prueba | Qué revisa | ¿Ya se puede? | 🧪 Botón | Tiempo | ✔ |
|---|---|---|---|---|---|---|
| 1 | **12 — Lo legal** | Datos del negocio, pie de página, Términos, Aviso de privacidad, registro, ticket, bot, Google — con dónde se revisa cada uno | Ya | 12 | 40 min | [ ] |
| 2 | **15 — Agregar producto, habilitar, Zonas de entrega** | Código y categoría del modelo, stock al habilitar, Zonas de entrega y Entregas por zona | Ya | 15 | 30 min | [ ] |
| 3 | **14-QA — El hotfix de prod en QA** | Que 🧩 Productos haga en QA lo mismo que en prod | Ya | 14-QA | 10 min | [ ] |
| 4 | **17 — Filtros de Tienda, Venta directa, buscadores y 🧩 Agregar artículos** | Filtros que se combinan, precio mín/máx, stock después de vender, nombres de 3 letras | Ya | 17 (también el usuario **qa.prueba17**) | 45 min | [ ] |
| 5 | **18 — Agregar artículo: todos los modelos** | Modelos sin stock o deshabilitados, ✅ Habilitar, Guardar en el modelo | Ya | 18 (también el usuario **qa.prueba18**) | 30 min | [ ] |
| 6 | **19 — Errores con su código, 404 y Home** | Avisos de error, 404 de verdad, Home y página 404 | Ya (el 🧪 19, al subir esta ronda) | 19 (y 18) | 25 min | [ ] |
| 7 | **23 — Colores de noche, listas desplegables y 📍 Dirección** 🆕 | Ventana de cancelar, submenús de noche, casillas de filtros, **listas que salían en blanco**, chat de día, botón Dirección | Ya | — (23.2: 22) | 20 min | [ ] |
| 8 | **13 — Fondo de los filtros y seguridad** | Recuadro de Tienda y Modelos (ahora también de noche), Personalización, encabezados de seguridad | Ya | — | 15 min | [ ] |
| 9 | **24 — Unir un Ir pagando que ya tiene dinero** 🆕 | Lo de K4 / K5 / K6 | Ya | 24 | 5 min | [x] |
| 10 | **22 — 🕓 Pendiente en Pago y sin cancelados al abrir** | Pendiente en Pago, Falta pagar sin Pendientes, sin cancelados al abrir | Ya | 22 | 10 min | [x] |
| 11 | **21 — La fecha de entrega en Pedidos y en Entregas por zona** | La fecha que pones en Entregas por zona sale en la card y en el detalle; **el nombre del cliente es el de la venta, no el de tu cuenta** | Ya (el nombre: al subir esta corrección) | 21 | 20 min | [x] |
| 12 | **20 — Cliente que no podía pedir, grupo, modelo agotado y Mis pedidos** | Carrito → Mis datos; cambiar forma de cobro de pedidos unidos; Actualizar Modelo en 0; Mis pedidos al abrir | Ya | 20 (20.2 y 20.3) | 35 min | [ ] |
| 13 | **16 — 🕓 Pendiente, cambiarlo a Apartado, Entregas por zona, ⓘ** | Pendiente en filtros y card, pasarlo a Apartado / Ir pagando, Entregas por zona, ⓘ | Ya | 16 | 25 min | [ ] |
| 14 | **25 — Mis pedidos: buscar y poner/quitar filtros ya no tarda** 🆕 | La búsqueda por texto responde en ~1 s (antes hasta 48 s con 6 000 pedidos); mismos resultados | Al subir el arreglo | — | 10 min | [x] |
| 15 | **26 — Mis pedidos con caché: rápido y sin datos viejos** 🆕 | Poner/quitar filtros y regresar a uno ya visto sale al instante; un abono, cambio de forma de cobro, Entregar o Cancelar se ve en la lista en ese momento | Ya (subida a QA el 2026-10-14) | 26 | 15 min | [x] |
| 16 | **6 — Datos de prueba con un botón** | Miles de modelos y pedidos de prueba | **Al final** | — | 15 min | [ ] |

**Por qué este orden:** primero lo que ya se podía hacer antes de la subida del 2026-10-13; después lo
que estrena esa subida. La **6** va al final porque mete miles de pedidos de prueba.

**Al terminar todas:** toca **🗑 Quitar todas** (en 🧪 Datos de prueba) para dejar QA limpio, y QA pasa a **prod** (ver "Lo que sigue" al final).

---
## Prueba 12 — Lo legal que ya se pudo resolver

**Para qué es:** de `LEGAL_PLAN_DE_ACCION.md`, lo que **no necesitaba tus datos**: un lugar para
capturarlos (y que salgan solos donde la ley pide), Términos y Aviso de privacidad nuevos, la casilla
de Términos al registrarse, el aviso de "sin intereses" en Ir pagando, el ticket y lo de Google.

💬 2026-10-13: *"solo dice lo legal pero no dice en dónde"*. ↳ Esta tabla dice dónde se revisa cada cosa;
los pasos de abajo la siguen en el mismo orden.

### Dónde se revisa cada cosa

| Parte | Qué revisas | Dónde está (menú → pantalla → botón) | Con qué usuario |
|---|---|---|---|
| **12.1** | Los datos legales del negocio (responsable, domicilio, teléfono, correo, horario, RFC) | Menú lateral **Sistema → 🏪 Negocio & Contactos** → baja hasta el recuadro **⚖️ Datos legales del negocio** → botón **💾 Guardar datos legales** | Administrador |
| **12.2** | Que esos datos salgan solos al pie de página | **Cualquier pantalla**, baja **hasta el final**: la franja de abajo con "© Novedades Jade", **Aviso de privacidad** y **Términos y condiciones** | Cualquiera, también sin sesión (ventana privada) |
| **12.3** | El texto de los Términos | En esa franja de abajo, el enlace **Términos y condiciones** (abre `/termConditions`) | Cualquiera |
| **12.4** | El texto del Aviso de privacidad | En esa franja de abajo, el enlace **Aviso de privacidad** (abre `/privacidad`) | Cualquiera |
| **12.5** | Las dos casillas al crear una cuenta | Abajo en el menú lateral **Salir** → en la pantalla de entrar, **Regístrate aquí** | Sin sesión |
| **12.6** | El aviso "sin intereses" de Ir pagando y Apartado | **Ventas → 💰 Venta directa** → agrega un artículo → toca **💳 Ir pagando** o **📦 Apartado**: el aviso sale **debajo del total**. También en el **carrito** de la tienda | Administrador |
| **12.7** | Tus datos y la garantía en el ticket | **📋 Pedidos → Mis pedidos** → en la tarjeta de un pedido ya cobrado, el botón con el **ícono de impresora** (🖨) · o en su **👁 Detalle**, **🖨️ Imprimir ticket** | Administrador |
| **12.8** | Que el bot no prometa lo que no dice el catálogo | La **burbuja 💬 de abajo a la derecha** de la tienda (el chat de la tienda) | Cualquiera |
| **12.9** | Lo de Google | La **pestaña del navegador** al abrir el detalle de un artículo, y la dirección `qa.shop.novedades-jade.com.mx/sitemap.xml` | Cualquiera |

### 12.0 Antes de empezar

`migration_datos_legales.sql` ya está corrida en QA (2026-10-07). **Tú:** `Ctrl + Shift + R`.

Para la 12.6 y la 12.7: **🧪 Preparar prueba 12** (o ya lo hiciste con **Preparar todas**). Da de alta:

| Clave | Qué es | Para |
|---|---|---|
| **Art-200** | Artículo de **$200** con stock 10 | 12.6: Venta directa |
| **T1** | 🛒 Venta de contado de Art-200, ya cobrada · cliente «Prueba 12 T1» | 12.7 paso 1: su ticket |
| **T2** | 💳 Ir pagando de Art-200 con un abono de **$50** · cliente «Prueba 12 T2» | 12.7 paso 2: el ticket de abono |

### Mapa de impacto

| # | Se movió | Le pega a | Antes | Después |
|---|---|---|---|---|
| 12.1 | Tabla y pantalla nuevas de **datos legales** | **Sistema → Negocio & Contactos** | No había dónde poner domicilio ni teléfono | Sección **⚖️ Datos legales del negocio** |
| 12.2 | El pie de página lee esos datos | **Todas** las pantallas (abajo) | Solo "© Novedades Jade" y los 3 enlaces | Además: responsable, domicilio, teléfono, correo y horario (lo que esté capturado) |
| 12.3 | Texto de **Términos y condiciones** | `/termConditions` (pie de página) | Decía "dentro de los siguientes días" (sin número); sin garantía ni derecho a cancelar | Garantía de 90 días, cancelar en 5 días hábiles, formas de pago, qué pasa con el dinero al cancelar, PROFECO |
| 12.4 | Texto del **Aviso de privacidad** | `/privacidad` | Sin responsable ni domicilio; una sola lista de para qué | Responsable, para qué (necesario / lo que tú decides), con quién y en qué país, cómo pedir tus datos y en cuántos días |
| 12.5 | **Registro**: aviso corto y casilla de Términos | Login → **Regístrate aquí** | Una casilla (privacidad) | Un párrafo corto + **dos** casillas (privacidad y Términos) |
| 12.6 | Aviso de **Ir pagando / Apartado** | **Ventas → 💰 Venta directa** y **Carrito** | Nada | "Ir pagando, sin intereses. Precio de contado $X · Total a pagar $X (CAT 0%)" |
| 12.7 | **Ticket** | Cualquier ticket (venta, abono, liquidado, cancelación) | Solo "NOVEDADES JADE" | Debajo, tus datos legales; en abonos "Abonos sin intereses (CAT 0%)"; abajo "Garantía de 90 días" |
| 12.8 | **Bot**: no prometer lo que no dice el catálogo | Chat de la tienda, chat en vivo, Instagram y Facebook | Podía decir "original" o "garantizado" | Solo lo del catálogo |
| 12.9 | **Google** | Detalle de un artículo, página que no existe, sitemap | Título igual en todo; sitemap a páginas rotas; fotos con "Imagen variante" | Título con el nombre del artículo; sitemap a la tienda; fotos con su nombre |
| — | **Lo que NO debe cambiar** | Venta, cobro, abonos, totales, entrar al sistema | — | **Lo mismo que antes** |

### 12.1 Datos legales en Negocio & Contactos

| # | Haz esto | Debes ver |
|---|---|---|
| 1 | **Sistema → Negocio & Contactos** → baja hasta **⚖️ Datos legales del negocio** | El correo `contacto@novedades-jade.com.mx` ya lleno y **"⚠️ Falta: Nombre del responsable, Domicilio, Teléfono"** |
| 2 | En RFC escribe `ABC123` y toca **💾 Guardar datos legales** | Mensaje del RFC (13 o 12 caracteres); no se guarda |
| 3 | En Teléfono escribe `12345` → Guardar | *"El teléfono tiene que tener 10 dígitos"* |
| 4 | Llena **datos de prueba** (no los reales todavía): nombre "Prueba 12 Negocio", domicilio "Calle 1, Luvianos, Edo. Méx.", teléfono `(55) 1234-5678`, horario "Lunes a sábado 10 a 19" → Guardar | *"¡Datos legales guardados!"* y "Ya se ven en el pie de página…". El aviso de "Falta" desaparece |
| 5 | Recarga la página | Los datos siguen; el teléfono aparece como `5512345678` |
| 6 | Entra con un usuario **sin** permiso de escribir en Negocio & Contactos | No puede guardar (o no ve la pantalla) |

### 12.2 Pie de página

| # | Haz esto | Debes ver |
|---|---|---|
| 1 | Recarga (`Ctrl + Shift + R`) cualquier pantalla y baja hasta el final | "Prueba 12 Negocio · Calle 1, Luvianos… · Tel. 55 1234 5678 · contacto@… · Atención: Lunes a sábado 10 a 19" |
| 2 | Sin sesión (ventana privada), en la tienda | Lo mismo |
| 3 | En el celular | Se acomoda en varias líneas, sin salirse de la pantalla |
| 4 | Toca el teléfono en el celular | Abre la llamada |
| 5 | **Al terminar la prueba:** borra los datos de prueba en Negocio & Contactos (deja solo el correo) o pon los reales | — |

### 12.3 y 12.4 Términos y Aviso de privacidad

| # | Haz esto | Debes ver |
|---|---|---|
| 1 | Pie de página → **Términos y condiciones** | Secciones nuevas: **Quién vende** (con tus datos), **Formas de pago**, **Cancelar tu compra (5 días hábiles)**, **Garantía… 90 días**, **Si cancelas un pedido en el que ya pagaste algo**, **Quejas y PROFECO**. Fecha: 7 de octubre de 2026. **Ya no** dice "dentro de los siguientes días" |
| 2 | Pie de página → **Aviso de privacidad** | Título "Aviso de privacidad"; **Quién es responsable de tus datos**; **Para qué los usamos** en dos partes; OVHcloud, Google, OpenAI y Mercado Pago con su país; **Tus derechos (ARCO)** con 5 y 15 días hábiles |
| 3 | Las dos, de noche y en celular | Se leen bien |

### 12.5 Registro

| # | Haz esto | Debes ver |
|---|---|---|
| 1 | Cierra sesión → **Regístrate aquí** | Un párrafo corto ("Novedades Jade usa tus datos para…") y **dos casillas**: aviso de privacidad y **Términos y condiciones** |
| 2 | Llena todo (nombre **Prueba 12 Registro**, con un correo tuyo para recibir el código), marca solo la de privacidad | **Registrarse** sigue gris |
| 3 | Marca también Términos → Registrarse | Se crea la cuenta como siempre (código al correo) |
| 4 | **Leer los Términos y condiciones** | Abre los Términos en otra pestaña |
| 5 | (Opcional, en la base) `SELECT username, acepto_terminos, fecha_acepto_terminos FROM usuario_modificacion ORDER BY id DESC LIMIT 1;` | `1` y la fecha de hoy |
| 6 | Entra con tu usuario de siempre | Entra normal (las cuentas de antes no se ven afectadas) |

### 12.6 Ir pagando y Apartado

| # | Haz esto | Debes ver |
|---|---|---|
| 1 | **Venta directa** → en **🔍 Buscar producto** escribe **Art-200** (o pega su código) → agrégalo → **💳 Ir pagando** | *"💳 Ir pagando, sin intereses. Precio de contado $200.00 · Total a pagar $200.00 (CAT 0%). Puede liquidar antes cuando quiera, sin cargo."* |
| 2 | Cambia a **📦 Apartado** | *"📦 Apartado, sin dinero ahora. Se paga completo ($200.00) al recogerlo."* |
| 3 | **💵 Contado** | Ninguno de los dos avisos |
| 4 | **Carrito** con artículos → Ir pagando / Apartado | Los mismos avisos con el total del carrito |
| 5 | **Mis pedidos** → escribe **Prueba 12 T2** | Es el 💳 Ir pagando con $50 que 🧪 ya hizo con la misma venta directa: dice **abonado $50.00** y **resta $150.00**. Es el "se crea igual que antes"; **no hace falta hacer otra venta** |

### 12.7 Ticket

| # | Haz esto | Debes ver |
|---|---|---|
| 1 | Imprime el ticket de **T1**, la venta de contado (con los datos de prueba de 12.1 capturados). En **Mis pedidos** escribe **Prueba 12 T1** (o ábrelo con el enlace de su número en el recuadro de 🧪) | Debajo de "NOVEDADES JADE": nombre, domicilio, teléfono, correo. Abajo: "Garantía de 90 días desde que lo recibes" |
| 2 | Ticket del **abono** de **T2** (en Mis pedidos escribe **Prueba 12 T2**; Ir pagando con $50) | Además: "Abonos sin intereses (CAT 0%)" |
| 3 | Totales, abonos y saldo | **Igual que antes** |

### 12.8 Bot

| # | Haz esto | Debes ver |
|---|---|---|
| 1 | En el chat de la tienda pregunta "¿esta bolsa es original?" de una bolsa cuya descripción no lo diga | No contesta "sí, es original": dice lo que trae el catálogo y ofrece que alguien del negocio confirme |
| 2 | "¿Este perfume quita las manchas?" | No promete efectos en la piel |

### 12.9 Google

| # | Haz esto | Debes ver |
|---|---|---|
| 1 | Abre el detalle de un artículo | La pestaña del navegador dice "<nombre del artículo> — Novedades Jade" |
| 2 | Regresa a la tienda | La pestaña vuelve a "Novedades Jade — Bolsas, Pantalones…" |
| 3 | Abre `qa.shop.novedades-jade.com.mx/sitemap.xml` | Solo `/tienda/buscar`, Términos y Privacidad (ya no `/variantes/buscar` ni `/login`) |
| 4 | (Opcional) Herramientas del navegador → Elementos, en el detalle, busca `ld+json` | Un bloque con el nombre, precio en MXN y disponibilidad |

💬 Notas:

- [ ] **Prueba 12 terminada**

---

## Prueba 15 — Agregar producto con código y categoría, stock al habilitar, Zonas de entrega y Entregas por zona

**Para qué es:** lo que pediste el 2026-10-07 al revisar QA.

**Antes de empezar:** **🧪 Preparar prueba 15** (o ya lo hiciste con **Preparar todas**). Da de alta:

| Clave | Qué es | Para |
|---|---|---|
| **M5** | Modelo con **stock 5**: talla **CH** habilitada con 4 | Pasos 2b, 2c y 3 |
| **M5-baja** | La talla **M** de M5, **deshabilitada, que guarda 3** (como los artículos viejos) | Paso 3: al habilitarla queda en **1** |
| **M10** | Modelo con **stock 10**, de **otra categoría** que M5: talla CH habilitada con 4 | Pasos 2c y 4 |
| **M10-baja** | La talla M de M10, deshabilitada, que guarda 3 | Paso 4: al habilitarla se queda con **3** |

Ya no hace falta la consulta en la base para encontrar un artículo viejo deshabilitado.

### Mapa de impacto

| # | Se movió | Le pega a | Antes | Después |
|---|---|---|---|---|
| 15.1 | Resultados de "Buscar modelo" muestran el código de barras | **Catálogo → Agregar producto** | Solo el nombre: 3 modelos "BOLSA" no se distinguían | "BOLSA · 7501234567890" en la lista y en el modelo elegido |
| 15.2 | Habilitar un artículo viejo deshabilitado que guarda stock | **Tienda**: 🔓 Habilitar de la card y 🔓 Habilitar seleccionadas | Volvía con su stock viejo aunque el modelo ya no tuviera libre (descuadre) | Se queda con lo libre del modelo y el mensaje dice el ajuste |
| 15.3 | "Recoger en tienda" es un interruptor | **Envíos → Zonas de entrega** | Checkbox del navegador | Interruptor (prendido / apagado) |
| 15.4 | Recuadro con fondo | **Envíos → Zonas de entrega** y **Envíos → Entregas por zona** | Formulario suelto sobre la página | Dentro de una card con el mismo fondo que **Agregar Modelo** |
| 15.8 | "Recoger en tienda" solo en una fila | **Envíos → Zonas de entrega** | Se podía prender en cualquier zona, en varias, con envío y horas | Al prenderlo se esconden Envío, Horas extra, Día y anillos; una segunda fila marcada se rechaza con el nombre de la que ya lo es |
| 15.9 | (mismo) | **Carrito** → "📍 Lugar de entrega" y **Envíos → Entregas por zona** | — | **Lo mismo que antes**: el local pide fecha de recogida; el local no sale como zona en Entregas por zona |
| 15.10 | Se quitó el "día de la semana" de la zona | **Envíos → Zonas de entrega** y **Envíos → Entregas por zona** | Select "Sin día fijo / 🚚 Lunes…" que solo prellenaba la fecha del viaje (y podía no coincidir con la fecha escogida) | Ya no existe; la fecha del viaje se escoge en Entregas por zona y esa fecha ya dice el día |
| 15.7 | Explicación en la pantalla | **Envíos → Zonas de entrega** | Título "Catálogo de lugares de entrega", sin explicación (solo el "?") | Título **"📍 Zonas de entrega"** y recuadro "ℹ️ Para qué sirve esta pantalla…" abierto: dónde se usa la lista, qué hace cada dato y qué no hace |
| 15.5 | La búsqueda de modelos trae la categoría (solo admin) | **Catálogo → Agregar producto** → Categoría | Vacía aunque el modelo la tuviera (el back sí se la ponía al guardar) | Sale llena con la del modelo; se puede cambiar |
| 15.6 | (mismo) | **Catálogo → Modelos**, Tienda y todo lo que lista modelos | — | **Lo mismo que antes** (es un campo más en la respuesta, nadie más lo lee) |
| — | **Lo que NO cambia** | Guardar artículo, deshabilitar (sigue dejando en 0), guardar lugar, programar entregas | — | **Lo mismo que antes** |

| # | Haz esto | Debes ver |
|---|---|---|
| 1 | **Catálogo → Agregar producto** → busca "bolsa" | Cada resultado con su código: "BOLSA · 750…" |
| 2 | Elige uno | Arriba sale "BOLSA · 750…" y los datos del modelo ya llenos (color, marca, descripción…) y "Repartido / Libre" |
| 2b | Elige un modelo que **tenga categoría**: **M5** (búscalo como *QA Prueba 15 M5*) | La casilla **Categoría** ya dice la del modelo. Cámbiala por otra y guarda: el artículo queda con la que escogiste |
| 2c | Elige **M5**, quítalo con ✕ y elige **M10** (otra categoría) | La Categoría cambia a la del segundo. Si la habías cambiado a mano, se queda la tuya |
| 2d | Modelo **sin** categoría | La casilla queda vacía, como antes |
| 3 | **M5-baja** (en M5 la talla CH tiene 4 y el modelo 5: le queda 1 libre) → **Tienda → Buscar** → busca *QA Prueba 15 M5* → filtro **No habilitadas** → marca la talla M → **🔓 Habilitar seleccionadas** | Mensaje *"…Se ajustó el stock… de 3 a 1"*; el artículo queda con 1. Con el **🔓 Habilitar** de la card también queda en 1 (ese botón no muestra el detalle) |
| 4 | Mismo caso con **M10-baja** (modelo en 10, le quedan 6 libres) | Se habilita con sus 3, sin ajuste |
| 5 | Deshabilita un artículo con stock 2 y vuelve a habilitarlo | Queda en 0 (igual que antes: al deshabilitar su stock vuelve al libre) |
| 6 | **Envíos → Zonas de entrega** → nuevo lugar **Prueba 15 Zona** | "🏬 Recoger en tienda" es un interruptor; prenderlo y guardar marca la fila con "🏬 recoger en tienda" |
| 6b | **Envíos → Zonas de entrega** | Arriba el recuadro "ℹ️ Para qué sirve esta pantalla…", abierto; se cierra y abre con clic. Se lee bien de día y de noche. El "?" dice lo mismo en corto |
| 6c | **Envíos → Zonas de entrega** → nuevo lugar **Prueba 15 Estanco** → prende **🏬 Recoger en tienda** | Se esconden Envío, Horas extra y Día de entrega; sale la nota "Esta fila es tu local…". Al **➕ Agregar**: si ya tienes la fila del local, error *"\"(tu local)\" ya es la fila de recoger en tienda. Solo puede haber una…"* y no se guarda |
| 6d | ✏️ en la fila del local que ya existe → **💾 Actualizar** | Se guarda. Si tenía envío u horas extra, ya no los muestra en la lista |
| 6e | ✏️ en una zona normal (Tejupilco) | Se ven Envío, Horas extra y, si tiene centro, los anillos, igual que antes |
| 6f | ✏️ en Tejupilco | **Ya no sale** el select "Sin día fijo / Lunes…": se quitó. Al **💾 Actualizar** se borra el día viejo que tuviera |
| 6g | **Envíos → Entregas por zona** → elige Tejupilco | Salen **todos** los de Tejupilco que faltan por entregar (ver Prueba 16). La **Fecha** del viaje sale **vacía**: la escoges tú (ya no se prellena con el día fijo) |
| 7 | **Envíos → Zonas de entrega** y **Envíos → Entregas por zona**, de día, de noche y en el celular | Todo dentro de un recuadro con borde y sombra; en el celular sin moverse de lado |

**Para el paso 3:** hoy, al deshabilitar un artículo su stock queda en 0, así que solo los viejos
tienen stock estando deshabilitados. Por eso el botón 🧪 los crea así (M5-baja y M10-baja). Si ya
habilitaste los dos y quieres repetir, vuelve a tocar **🧪 Preparar prueba 15**.

💬 Notas:

- [ ] **Prueba 15 terminada**

---

## Prueba 14-QA — El hotfix de prod, ya en QA

**Para qué es:** el arreglo de **🧩 Productos** (cuánto stock queda y dónde está) ya lo validaste en
**prod**. Cuando lo baje a `qa` y `dev` (va junto con tu "sube"), hay que confirmar que en QA hace lo
mismo y que no chocó con lo de QA (artículo nace con los datos del modelo, Agregar producto).

**Antes de empezar:** **🧪 Preparar prueba 14-QA** (o ya lo hiciste con **Preparar todas**). Da de alta
**M3**: un modelo con stock **3** y un artículo con stock **1** con foto. Al terminar el paso 4 ya no le
queda stock: para repetir, vuelve a tocar el botón.

| # | Haz esto | Debes ver |
|---|---|---|
| 1 | **Catálogo → 🔍 Modelos** → en el buscador escribe **QA Prueba 14** (o pega el código de barras de **M3**) → en su tarjeta **🧩 Productos** | *"Stock del modelo: 3 · En sus artículos: 1 · Puedes crear: 2"* |
| 2 | Pon **3** en cantidad → Crear | No deja: *"Puedes crear hasta 2"* |
| 3 | Pon **2**, elige una foto **sin** marcar "Misma imagen" → Crear | No deja: *"Para usar las fotos marca 'Misma imagen para todas las variantes'"* |
| 4 | Marca la casilla → Crear | "2 variante(s) creada(s)"; en **Tienda** salen con la foto y con el color, marca, descripción y categoría del modelo |
| 5 | Otra vez **🧩 Productos** del mismo modelo | No abre la ventana: *"No queda stock para artículos nuevos…"* |
| 6 | **Catálogo → Agregar producto** con ese modelo | *"Repartido: 3 · Libre: 0"*, igual que antes |

❌ **Está mal si:** sale *"Error al crear variantes — Intenta de nuevo"* (era el 400 de prod), o la
ventana deja pedir más de lo que dice "Puedes crear".

💬 Notas:

- [ ] **Prueba 14-QA terminada**

---

## Prueba 17 — Filtros de Tienda, Venta directa, buscadores y Agregar artículos (2026-10-08)

**Para qué es:** lo que reportaste el 2026-10-08 al validar en QA: los filtros de Tienda que no se
combinaban, el precio mín/máx que no encontraba los de $100, el buscador de Venta directa que no se
actualizaba después de vender, nombres de una letra guardados en Venta directa, y la ventana para dar
de alta los artículos de un modelo al guardarlo.

**Antes de empezar:** ✅ ya está en QA (2026-10-13); haz `Ctrl + Shift + R`. **🧪 Preparar prueba 17**
(o ya lo hiciste con **Preparar todas**). Da de alta:

| Clave | Qué es | Para |
|---|---|---|
| **Filtros** | Modelo con tallas **CH, M y G** habilitadas con 5 y **XL deshabilitada** (borde rojo) con 5 | 17.1 |
| **Art-A** | Artículo de **$100** con stock 10 | 17.2 |
| **Descuento** | Artículo de $130 que se vende al **descuento de $100** (la tarjeta dice $100) | 17.2 |
| **Stock-1** | Artículo con **stock 1** | 17.3 (después de venderlo queda en 0: para repetir, vuelve a tocar el botón) |

### Mapa de impacto

| # | Se movió | Le pega a | Dónde se ve | Antes | Después |
|---|---|---|---|---|---|
| 17.1 | Búsqueda de Tienda: filtros de admin y del catálogo en **una sola** llamada | **Tienda → Buscar** (escritorio, con sesión de admin) | Las tarjetas | "Con stock" + "Habilitadas" dejaba 1; quitar un filtro no siempre volvía a buscar | Salen todos los que cumplen los dos; al quitar uno se busca con los que quedan |
| 17.2 | Precio mín/máx compara el **precio que se cobra** | Tienda → Buscar (admin y catálogo de clientes) | Las tarjetas | Mín 100 / máx 100 no encontraba los de $100 | Encuentra los que la tarjeta dice $100, también con descuento |
| 17.3 | (mismo) | Tienda sin sesión (cliente) | Las tarjetas | Comparaba el precio normal | **Lo mismo que 17.2**; lo demás del catálogo, igual que antes |
| 17.4 | Venta directa vuelve a buscar después de cobrar | **Ventas → 💰 Venta directa** | Lista de **🔍 Buscar producto** | Seguía diciendo el stock viejo | El stock ya baja solo |
| 17.5 | Validación del cliente sin registro y de quien recibe | Venta directa → registrar cliente / Datos de entrega | Aviso rojo debajo del campo | Guardaba "a" | Pide 3 letras; correo y teléfono válidos si se escriben |
| 17.6 | Buscador de **Mis pedidos** desde la cuenta del cliente | Tienda → Mis pedidos (cliente) | Debajo del buscador | Letras = ventana emergente en cada tecla | Aviso debajo, sin ventana; el número de pedido con cualquier cantidad de dígitos |
| 17.7 | Buscadores que buscaban con 2 letras | Promociones (buscar artículo del combo), Publicar en Facebook (buscar producto), Reportes (buscar cliente) | La lista de resultados | Buscaba con 2 letras | Busca desde 3 (vacío limpia) |
| 17.8 | Ventana **🧩 Agregar artículos** | **Catálogo → Agregar modelo** (al guardar uno nuevo) | Ventana nueva | Solo "¡Producto guardado!" | Pregunta **cuántos** artículos quieres hacer; cada artículo con sus checks de lo que cambia (2026-10-14) |
| 17.9 | (misma ventana) | **Catálogo → 🔍 Modelos → 🧩 Productos** | Ventana | "Inicializar variantes": N artículos iguales con stock 1 y sin talla | Un formulario por artículo, con talla, stock, foto… |
| 17.10 | `guardarConImagenes` acepta foto propia por artículo | **Catálogo → 🧩 Agregar producto** con varias tallas y una foto | Las fotos de los artículos | La foto la llevan todas las tallas | **Lo mismo que antes** |

### 17.1 Filtros que se combinan (Tienda → Buscar, escritorio, como admin)

| # | Haz esto | Debes ver |
|---|---|---|
| 1 | **Tienda → Buscar** → escribe **QA Prueba 17 Filtros** → abre los filtros → marca **Con stock** | Los 4 artículos de **Filtros**: CH, M, G y XL (XL con borde rojo) |
| 2 | Marca también **Habilitadas** | CH, M y G. **XL ya no sale**; los otros 3 **sí** (antes quedaba 1) |
| 3 | Agrega un tercer filtro: **Talla G** | Solo G |
| 4 | Quita **Talla** | Vuelve el resultado del paso 2 sin tocar nada más |
| 5 | Quita **Habilitadas** | Vuelve el resultado del paso 1 |
| 6 | Quita **Con stock** | Todo el catálogo, como al entrar |

❌ **Está mal si:** al quitar un filtro la lista no cambia, o con dos filtros sale menos de lo que cumple los dos.

### 17.2 Precio mín / máx

| # | Haz esto | Debes ver |
|---|---|---|
| 1 | En los filtros escribe **Precio mín 100** y **máx 100** (espera un segundo sin escribir) | Los artículos que **en la tarjeta** dicen $100, incluidos los que llegan a $100 con descuento: entre ellos **Art-A** y **Descuento** |
| 2 | Cambia el máx a **99** | No sale ninguno de $100 (ni Art-A ni Descuento) |
| 3 | Borra los dos precios | Vuelve a todo |
| 4 | Cierra sesión (o abre la tienda en una ventana privada) y repite el paso 1 | Los mismos de $100 que tengan foto y stock (el catálogo de clientes solo muestra esos) |

❌ **Está mal si:** con 100 / 100 no sale un artículo cuya tarjeta dice $100.

### 17.3 Venta directa: el buscador se actualiza al vender

| # | Haz esto | Debes ver |
|---|---|---|
| 1 | **Ventas → 💰 Venta directa** → en **🔍 Buscar producto** escribe **Stock-1** (o pega su código) | **Stock-1** con **1** de stock |
| 2 | Agrégalo, cobra de **💵 Contado** y cierra el aviso | La venta se registra |
| 3 | Mira la lista del buscador (sin escribir nada) | Ese artículo ya dice **0** o ya no sale. **No** sigue diciendo 1 |

### 17.4 Venta directa: nombres de 3 letras, correo y teléfono

| # | Haz esto | Debes ver |
|---|---|---|
| 1 | Agrega un artículo → **💰 Cobrar** → *"¿Agregar cliente para la rifa?"* → **Sí** | El formulario del cliente |
| 2 | En **Nombre** escribe **a** | Debajo, en rojo: *"Escribe al menos 3 letras"*. **Guardar cliente** apagado |
| 3 | Cambia a **Prueba 17 Ana**; en **Apellido paterno** escribe **Lo** | Debajo de apellido: *"Si lo escribes, al menos 3 letras"*. Bórralo y el aviso se va (es opcional) |
| 4 | En **Correo** escribe **ana@gmail** | *"El correo no es válido (ejemplo: nombre@gmail.com)"*. Corrígelo a **ana@gmail.com** y se va |
| 5 | En **Número telefónico** escribe **55123** | *"El teléfono debe tener 10 dígitos"*. Con **55 1234 5678** se va |
| 6 | Cancela. En **📍 Datos de entrega → Nombre de quien recibe** escribe **Jo** | Debajo: *"Si lo escribes, al menos 3 letras"*, y junto a **💰 Cobrar**: *"Para cobrar, corrige el nombre de quien recibe…"*. **Cobrar** apagado |
| 7 | Cámbialo a **Prueba 17 José** (o bórralo) | **Cobrar** se vuelve a prender |

❌ **Está mal si:** se guarda un cliente o un "quien recibe" de 1 o 2 letras.

### 17.5 Mis pedidos desde la cuenta del cliente

| # | Haz esto | Debes ver |
|---|---|---|
| 1 | Entra con una cuenta de **cliente** que tenga pedidos → **Mis pedidos** | Sus pedidos |
| 2 | En el buscador escribe el número de uno de sus pedidos, aunque sea de **1 dígito** | Ese pedido |
| 3 | Escribe letras, por ejemplo **abc** | Debajo del buscador: *"Escribe solo el número de tu pedido (por ejemplo 15)."* **Sin** ventana emergente |
| 4 | Borra todo | Vuelven todos sus pedidos |

### 17.6 Buscadores que ahora piden 3 letras

| # | Haz esto | Debes ver |
|---|---|---|
| 1 | **Promociones** (pantalla de administrar promociones) → en el buscador de artículos del combo escribe **2 letras** | No busca. Con la 3.ª letra, sí |
| 2 | **Publicar en Facebook** → buscador del producto → 2 letras / 3 letras | Igual que el paso 1 |
| 3 | **Reportes** → buscador de cliente | El texto de ayuda dice *"Escribe al menos 3 letras…"* y busca desde la 3.ª |

### 17.7 Agregar modelo → "¿Cuántos artículos quieres hacer?" (cambiada el 2026-10-14)

Cambió con lo que pediste en la 20.3: la pregunta ahora es **cuántos** artículos, y en cada artículo marcas
**solo lo que cambia** (lo demás queda igual al modelo). Antes había casillas arriba para todos.

| # | Haz esto | Debes ver |
|---|---|---|
| 1 | **Catálogo → Agregar modelo** → llena: nombre **QA Prueba 17 Modelo nuevo**, precio **100**, **stock 10**, color **Negro**, marca **Prueba QA**, descripción **Prueba**, la primera categoría y una foto cualquiera → **💾 Guardar modelo** | Ventana **"✅ Modelo guardado"**: *"Stock del modelo: 10 · Libre: 10"*, *"¿Cuántos artículos quieres hacer de este modelo?"* con un **1** escrito y los botones **Ahora no** / **Continuar** |
| 2 | Escribe **2** → **Continuar** | Ventana **🧩 Agregar artículos** con **Artículo 1** y **Artículo 2**, abiertos (con 4 o más, del segundo en adelante salen plegados; se prueba en la 20.3): Talla, Stock y Presentación vacíos, y *"¿Qué cambia en este artículo?"* con 6 checks **sin marcar** (Foto, Descripción, Color, Marca, Contenido neto, Categoría). Abajo de cada uno: *"Igual al modelo: Foto: la del modelo · Descripción: Prueba · Color: Negro · Marca: Prueba QA · Contenido neto: (el modelo no tiene) · Categoría: …"* |
| 3 | En **Mismo stock para todos** escribe **4** → **Aplicar** | Los dos con stock 4. Abajo: *"Stock del modelo: 10 · Repartido: 8 · Te quedan 2"* |
| 4 | En el **Artículo 2** cambia el stock a **8** | Abajo en rojo: *"Te pasaste por 2"*. **Guardar 2 artículos** apagado |
| 5 | Regrésalo a **4**. Tallas: **CH** y **M**. En el Artículo 2 marca **Color** | Aparece el campo Color ya escrito con **Negro**; cámbialo a **Blanco**. El Artículo 1 sigue sin marcar (*"Igual al modelo: … Color: Negro …"*) |
| 6 | En el Artículo 2 marca **Foto** → **Subir foto** → elige una foto | Se ve la miniatura y los botones **Cambiar foto** / **Quitar** |
| 7 | En el Artículo 1 marca **Marca** y quítale la marca enseguida | El campo aparece y se va; en *"Igual al modelo"* vuelve a salir *"Marca: Prueba QA"* |
| 8 | Toca **Guardar 2 artículos** | *"2 artículos guardados"* → **Entendido** y se cierra |
| 9 | **Tienda** → busca el modelo | Dos artículos: **CH Negro** con la foto del modelo y **M Blanco** con su foto propia. Stock 4 cada uno. Los dos con marca **Prueba QA** y descripción **Prueba** (las del modelo) |

❌ **Está mal si:** se guarda uno solo de los dos, el Artículo 1 sale con la foto del 2, el CH sale sin marca o
sin descripción, o se puede guardar con *"Te pasaste"* o con un check marcado y vacío.

### 17.8 "Ahora no" y la tarjeta 🧩 Productos

| # | Haz esto | Debes ver |
|---|---|---|
| 1 | Da de alta otro modelo (stock 5) → en la pregunta toca **Ahora no** | Se cierra. El modelo **sí** quedó guardado (búscalo en **Catálogo → 🔍 Modelos**) y no tiene artículos |
| 2 | En su tarjeta toca **🧩 Productos** | La misma ventana **🧩 Agregar artículos**, **sin** la pregunta, con *"Libre: 5"* y los checks por artículo |
| 3 | Pide **1** artículo con stock **5**, talla **U** → **Guardar 1 artículo** | *"1 artículo guardado"*; la lista de modelos se recarga |
| 4 | Vuelve a tocar **🧩 Productos** en ese modelo | El aviso de siempre: *"No queda stock para artículos nuevos"* (ya repartiste los 5) |
| 5 | En el modelo del 17.7 toca **🧩 Productos** | *"En sus artículos: 8 · Libre: 2"* |

### 17.9 Lo que no debe cambiar: Agregar producto con varias tallas

| # | Haz esto | Debes ver |
|---|---|---|
| 1 | **Catálogo → 🧩 Agregar producto** → elige un modelo con stock libre → llena el formulario de arriba, sube **una** foto y agrega **2 tallas** abajo → guarda | **Lo mismo que antes:** se guardan los artículos y **todos** llevan la foto que subiste |

### 17.10 Color nuevo en Personalización (solo si corriste `migration_tema_modal.sql`)

| # | Haz esto | Debes ver |
|---|---|---|
| 1 | **Sistema → Personalización → Página** | Fila nueva **"Velo detrás de una ventana abierta"** |
| 2 | Cámbiale el color de día, guarda y abre la ventana de 🧩 Productos | El fondo detrás de la ventana usa el color nuevo |

💬 Notas:

### 17.11 Filtros de Tienda con un rol que no es admin (corregido antes de subir a QA)

**Antes de empezar:** el mismo **🧪 Preparar prueba 17** da de alta el usuario **qa.prueba17** con el rol
**ROLE_QA_PRUEBA_17**: solo **Tienda → Buscar** y el filtro **"Con stock"**, sin nada de Catálogo. Su contraseña no
la sabe nadie: ponle una tú en **Sistema → Usuarios** → busca **qa.prueba17** → **Resetear contraseña** (te
enseña una contraseña nueva; cópiala). Entra con ese usuario en otra ventana de incógnito (te pide cambiar la
contraseña la primera vez).

| # | Haz esto | Debes ver |
|---|---|---|
| 1 | **Tienda → Buscar** → filtros → marca **Con stock** | La lista con stock, **sin error** (antes daba error 403) |
| 2 | Quita **Con stock** y elige solo una **Talla** | Los de esa talla, sin error |
| 3 | Con tu usuario admin, lo mismo | Igual que en 17.1 |

❌ **Está mal si:** sale "No tienes permiso" o la lista se queda vacía con error.

- [ ] 17.1 · [ ] 17.2 · [ ] 17.3 · [ ] 17.4 · [ ] 17.5 · [ ] 17.6 · [ ] 17.7 · [ ] 17.8 · [ ] 17.9 · [ ] 17.10 · [ ] 17.11 — **Prueba 17 terminada**

---

## Prueba 18 — Agregar artículo: todos los modelos, habilitar y agregar stock (2026-10-08)

**Para qué es:** lo que pediste el 2026-10-08: en **Catálogo → 🧩 Agregar producto** (la pantalla de
agregar artículo) buscar **todos** los modelos, también los sin stock, deshabilitados o dados de baja; al
elegir uno, que diga en qué estado está, dejar **habilitarlo**, decir **cuántos artículos más se pueden
hacer**, y que al **agregarle stock** se vaya al modelo y pregunte *"¿Deseas agregar los artículos de una vez?"*.

**Antes de empezar:** ✅ ya está en QA (2026-10-13); haz `Ctrl + Shift + R`. **🧪 Preparar prueba 18**
(o ya lo hiciste con **Preparar todas**). Da de alta:

| Clave | Qué es | Para |
|---|---|---|
| **M-OFF** | Modelo **deshabilitado** (su artículo sigue habilitado) | 18.1 y 18.2 (al habilitarlo en 18.2 ya no está apagado: para repetir, vuelve a tocar el botón) |
| **M-0** | Modelo **habilitado** con stock 4, todo repartido en su artículo (**Libre 0**) | 18.1 y 18.3 |

Además:
- Para los pasos con otro rol: el mismo **🧪 Preparar prueba 18** da de alta el usuario **qa.prueba18** con el rol
  **ROLE_QA_PRUEBA_18**: **🧩 Agregar producto** con **"Ver todos los modelos"** y **sin** "Habilitar" de Modelos
  (`migration_accion_tienda_venta_ver_todos.sql` ✅ ya está corrida en QA). Ponle contraseña en **Sistema →
  Usuarios** → **qa.prueba18** → **Resetear contraseña** y entra con él en una ventana de incógnito.

### Mapa de impacto

| # | Se movió | Le pega a | Dónde se ve | Antes | Después |
|---|---|---|---|---|---|
| 18.1 | Buscador de modelos con `todos=true` | 🧩 Agregar producto (admin o con el permiso) | Lista del buscador | Un rol que no es admin no veía los sin stock ni deshabilitados | Salen todos, con etiqueta ⛔ Deshabilitado / Sin stock / Sin foto |
| 18.2 | (mismo endpoint, sin `todos`) | **Tienda**, Modelos y cualquier otro buscador de modelos | Sus listas | Lo de siempre | **Lo mismo que antes** |
| 18.3 | La disponibilidad se lee bien | 🧩 Agregar producto → recuadro de stock | Recuadro verde | **No aparecía nunca** (el front leía mal la respuesta) | Aparece: total, repartido, libre y "Puedes hacer hasta N artículos más" |
| 18.4 | **Guardar en el modelo** (al momento) | 🧩 Agregar producto | Botón junto a "Agregar (+) o quitar (−)" | El ajuste solo se guardaba junto con el artículo | Se guarda en el modelo al tocarlo; luego pregunta por los artículos |
| 18.5 | ✅ Habilitar modelo | 🧩 Agregar producto | Aviso rojo | No había forma desde aquí | Habilita el modelo (sus artículos quedan como estaban) |
| 18.6 | Ajuste dentro del guardado del artículo | 🧩 Agregar producto → 💾 Guardar | Stock del modelo | Funcionaba | **Lo mismo que antes** |

### 18.1 Ver todos los modelos

| # | Haz esto | Debes ver |
|---|---|---|
| 1 | Como **admin**: **Catálogo → 🧩 Agregar producto** → en el buscador escribe 3 letras de **M-OFF** | Sale **M-OFF** con la etiqueta **⛔ Deshabilitado** debajo del nombre (y **Sin stock** / **Sin foto** si aplica) |
| 2 | Busca **M-0** | Sale sin etiqueta de deshabilitado |
| 3 | Entra con **qa.prueba18** y busca **M-OFF** | También sale, con sus etiquetas |
| 4 | En **Gestión de roles**, a **ROLE_QA_PRUEBA_18** quítale **"Ver todos los modelos"**; con **qa.prueba18** sal, vuelve a entrar y busca **M-OFF** | **Ya no sale** (como antes). Para repetir, vuelve a tocar 🧪 18: le regresa el permiso |

### 18.2 Modelo deshabilitado → Habilitar

| # | Haz esto | Debes ver |
|---|---|---|
| 1 | Elige **M-OFF** | Aviso rojo **"⛔ Este modelo está deshabilitado"** con el botón **✅ Habilitar modelo**. El botón **💾 Guardar** está apagado y **Guardar en el modelo** también |
| 2 | Toca **✅ Habilitar modelo** | Pregunta *"¿Habilitar "M-OFF"?"* explicando que sus artículos dados de baja siguen de baja → **Sí, habilitar** |
| 3 | — | **"Modelo habilitado"** con *"Stock del modelo: N. Tiene X artículos con Y piezas. Puedes hacer hasta Z artículos más"* (o *"No le queda stock libre…"*). Se queda hasta **Entendido** |
| 4 | Mira la pantalla | El aviso rojo ya no está; se ve el recuadro de stock y **💾 Guardar** se prende |
| 5 | **Catálogo → 🔍 Modelos** → busca **M-OFF** | Ya está habilitado |
| 6 | Con **qa.prueba18** (no tiene "Habilitar" de Modelos) elige **M-OFF** (si ya lo habilitaste, vuelve a tocar 🧪 18) | En vez del botón: *"Pídele a alguien con permiso de Habilitar…"* |

### 18.3 Sin stock libre → Guardar en el modelo → artículos de una vez

| # | Haz esto | Debes ver |
|---|---|---|
| 1 | Elige **M-0** | Recuadro de stock: *"Stock total del modelo"*, *"Repartido · Libre 0"* y *"No le queda stock libre: agrégale stock al modelo para hacer artículos nuevos."* |
| 2 | En **Agregar (+) o quitar (−) al modelo** escribe **5** | Abajo: *"El modelo quedaría en N+5"*. Se prende **Guardar en el modelo** |
| 3 | Toca **Guardar en el modelo** | **"Se agregaron 5 al modelo"** con el resumen (*"Puedes hacer hasta 5 artículos más"*) y la pregunta **"¿Deseas agregar los artículos de una vez?"** → **Sí, agregar artículos** / **Después** |
| 4 | Toca **Sí, agregar artículos** | Se abre la ventana **🧩 Agregar artículos** con *"Libre: 5"* (la misma de la Prueba 17.7) |
| 5 | Pide **2** artículos con stock **2** cada uno → **Guardar 2 artículos** | *"2 artículos guardados"*. Al cerrar, el recuadro dice *"Puedes hacer hasta 1 artículo más"* |
| 6 | Escribe **−1** → **Guardar en el modelo** | *"Se quitaron 1 al modelo"*; Libre 0 |
| 7 | Escribe **−1** otra vez | Aviso rojo *"No se puede dejar el modelo en …: ya tiene … repartidos"* y el botón apagado |
| 8 | Con libre > 0, toca **🧩 Agregar varios artículos de una vez** | La misma ventana, sin la pregunta |

### 18.4 Lo que no debe cambiar

| # | Haz esto | Debes ver |
|---|---|---|
| 1 | En un modelo con libre 0, escribe **+3** en el campo **sin** tocar *Guardar en el modelo*, llena un artículo con stock **3** y toca **💾 Guardar** | **Lo mismo que antes:** se guarda el artículo y el modelo sube 3 (en una sola operación) |
| 2 | **Tienda → Buscar** y **Catálogo → 🔍 Modelos** | Igual que antes: lo que veías antes es lo que ves ahora |
| 3 | Sin sesión, busca en la tienda un modelo deshabilitado | No sale (el permiso nuevo no aplica sin sesión) |

❌ **Está mal si:** un rol sin el permiso ve modelos deshabilitados, se puede guardar un artículo de un modelo
deshabilitado, el stock agregado se reparte solo a algún artículo, o el recuadro de stock no aparece.

💬 Notas:

- [ ] 18.1 · [ ] 18.2 · [ ] 18.3 · [ ] 18.4 — **Prueba 18 terminada**

---

## Prueba 19 — Errores con su código, página 404 y Home (2026-10-08)

**Para qué es:** lo que pediste el 2026-10-08: que cada error salga con su código (antes casi todo era
400, y uno contestaba 200 aunque fallara), que una página que no existe dé un **404 de verdad**, y que
Home y la página 404 ya no carguen una foto de pantalla completa que tardaba y no dejaba leer el texto.

**Antes de empezar:** ✅ ya está en QA (2026-10-13); haz `Ctrl + Shift + R`. Para ver el código
de una respuesta: **F12 → Red (Network)**, columna **Estado (Status)**. Toca **🧪 Preparar prueba 19** (y la **18**,
que deja **M-0**). Da de alta:

| Clave | Qué es | Para |
|---|---|---|
| **RC** | Rifa #N (🧪 Prueba, diaria de hoy) con un premio, cuyo **plazo de registro ya venció**. El recuadro 🧪 dice su número | 19.1 paso 2 |
| **CU** | Código de **Agregar mi compra** que **ya se usó** (tócalo en el recuadro 🧪 para copiarlo) | 19.1 paso 4 |
| **CL** | Código que **no** se ha usado. No lo captures: solo es para comparar | — |

La rifa sigue abierta para entrar a ella hasta un día después; si ya pasó más, vuelve a tocar 🧪 19.

### Mapa de impacto

| # | Se movió | Le pega a | Dónde se ve | Antes | Después |
|---|---|---|---|---|---|
| 19.1 | 102 `catch` que contestaban 400 a todo | Todas las pantallas que muestran el error del back | El aviso rojo | El texto del error | **El mismo texto**; solo cambia el número en Red |
| 19.2 | Concursantes, rifas, Agregar mi compra, Gastos lanzaban un error "genérico" | Rifas (agregar participante), Agregar mi compra | El aviso | El texto | **El mismo texto** (sin el arreglo saldría "Error interno del servidor") |
| 19.3 | Imágenes de un artículo: error → 200 con lista vacía | Boletos de rifa (foto del premio) | La foto | Si fallaba decía "sin imágenes" | Si falla, sale el error con "reintentar" |
| 19.4 | URL que no existe | Cualquier link roto | Barra de direcciones + Red | Se veía "no disponible" con **200** | Cambia a `/no-encontrada` y Red dice **404** |
| 19.5 | Home y página 404 | Home (`/home`) y `/no-encontrada` | La pantalla | Foto de 3 MB, texto blanco encima | Logo y texto con los colores de Personalización, de día y de noche |
| 19.6 | `a.pk-btn` (botón que es enlace) | **Boletos de rifa** → botón "Ver ruleta" (secundario) | El color del texto | Azul (Nebular pintaba todos los enlaces) | Del color de los botones secundarios |

### 19.1 Los avisos de error dicen lo mismo

| # | Haz esto | Debes ver |
|---|---|---|
| 1 | ⚠️ **Sin prueba en pantalla.** El aviso *"Este cliente ya está registrado en esta rifa"* (409) solo sale si se registra **el mismo cliente de pedidos** dos veces, y la pantalla nunca lo manda: **+ Agregar** registra nombre y teléfono sin cliente, y **📅 Importar del mes** se salta a los que ya están (los cuenta como "omitidos"). Se revisó en el código el 2026-10-14 | — |
| 2 | Pantalla **🎰 Rifas** → **☀️ Diaria**, día de hoy → **Rifa #N** (la de RC, con 🧪 Prueba) → **🎡 Ir a ejecución** → en **Participantes**, **+ Agregar** → nombre **Prueba**, apellido **Diecinueve**, teléfono **5512345678**, en **Palabra clave** elige **QAPRUEBA19** → **Registrar** | *"El plazo de registro cerró el …"* (o *"Esta rifa ya fue sorteada o está inactiva"* si ya pasó un día). En Red: **409**. No se agrega |
| 3 | **Agregar mi compra** → escribe **codigo-que-no-existe** | *"No encontramos ese código, revisa que esté bien copiado."* En Red: **404** |
| 4 | Pega el código de **CU** (del recuadro 🧪) | *"Este código ya fue usado."* En Red: **409** |
| 5 | **Gestión de roles** → intenta quitarle a ROLE_ADMIN "Gestión de roles" | El aviso de siempre (*"No se le puede quitar a ROLE_ADMIN…"*). En Red: **409** |
| 6 | **Catálogo → 🧩 Agregar producto** → elige **QA Prueba 18 M-0** (libre 0) → llena un artículo con stock **1** → **💾 Guardar** | *"El modelo «…» no tiene piezas libres para darle a este artículo: tiene N libres y le pides M más…"* (hasta el 2026-10-14 decía *"Stock insuficiente para el producto…"*; las dos están bien). En Red: **422** (antes salía 404) |

### 19.2 Página que no existe

| # | Haz esto | Debes ver |
|---|---|---|
| 1 | Escribe en la barra `…/esta-pagina-no-existe` y Enter | La dirección cambia a **`/no-encontrada`**: *"Error 404 · Esta página no existe"* con **🛍️ Ir a la tienda** y **← Regresar**. En Red, el documento `no-encontrada`: **404** |
| 2 | **🛍️ Ir a la tienda** | Abre Tienda → Buscar |
| 3 | Abre la ficha de un artículo que no existe: `…/tienda/detalle/999999999` | *"Este producto ya no está disponible."* (antes un visitante sin sesión veía *"No pudimos abrir este producto desde un enlace directo"*) |
| 4 | Abre un link de ruleta de una rifa que no existe | La pantalla 404, con tu link en la barra |

### 19.3 Home y página 404 nuevas

| # | Haz esto | Debes ver |
|---|---|---|
| 1 | Abre **Home** de día y luego cambia a noche (☀️/🌙) | El logo en círculo, "Tienda en línea", **Novedades Jade** y "Bolsas, pantalones, blusas y perfumes". Todo se lee en los dos. Carga al instante |
| 2 | Sin sesión: Home | Solo **🛍️ Ver la tienda**. Con sesión: también **🎁 Promociones** |
| 3 | En el celular | El panel ocupa el ancho, botones uno debajo de otro |
| 4 | **Rifas → Boletos de rifa** → rifa pública → botón **Ver ruleta** | El texto ya no sale azul |

**❌ Está mal si…** un aviso de error cambió de texto o sale vacío; aparece *"Error interno del servidor"*
donde antes salía un mensaje claro; `/no-encontrada` dice 200 en Red; algún texto de Home no se lee de
noche.

---

## Prueba 23 — Colores de noche, listas desplegables y el botón 📍 Dirección (2026-10-13) 🆕

**Para qué es:** tus notas del 2026-10-13 (arriba): la ventana de cancelar fea de noche, los submenús
del menú lateral azules o negros de noche (y negros al pasar el mouse), las casillas de los filtros, los
dos botones con "Entrega" en la tarjeta de pedido, **las listas que se abrían en blanco**, y lo que salió
al revisar todo el texto (el chat de día y el "$" de Reportes).

**Antes de empezar:** el script del paso 4 de "Antes de todo" (ya está subido lo del 2026-10-13).
`Ctrl + Shift + R`. Usa tus temas de siempre: **Día "personalizado a mano"** y **Noche "Jade"**. Para
ver la noche: abajo en el menú lateral **🌙 Modo oscuro** (o espera a después de las 7 de la noche).
Para tener un pedido que cancelar sin tocar los tuyos: **🧪 Preparar prueba 22** y usa **AP**.

### Mapa de impacto

| # | Se movió | Le pega a | Dónde se ve | Antes | Después |
|---|---|---|---|---|---|
| 23.1 | Dos comentarios sin cerrar en los colores de noche (`styles.scss`) | **Todo** lo que usa esos colores de noche: menú lateral, tarjetas | Menú lateral y tarjetas, de noche | Submenús en azul de enlace o negro; al pasar el mouse, negro | Submenús con el color de texto del menú; al pasar el mouse, el fondo de "seleccionado" |
| 23.2 | Color de fondo de tarjeta de noche (`--card-bg`) | Tarjetas que lo usan (p. ej. 🗑️ Limpiar caché) | De noche | No existía: fondo transparente | Gris oscuro del tema |
| 23.3 | Botones de motivo de la ventana de cancelar | **Mis pedidos → ✖ Cancelar** y **Créditos / Abonos → ✖ Cancelar** | La ventana *"¿Por qué cancelas este pedido?"* | Colores fijos de día (blanco) también de noche | Los del tema, de día y de noche |
| 23.4 | Regla de letras de Jade ya no pinta las casillas de los filtros | **Tienda** y **Catálogo → 🔍 Modelos** → ⚙️ Filtros | Casillas, fechas | Opacas / mal contraste en Jade | Con su color de "cada filtro" (Personalización → Formularios) |
| 23.5 | Botón **📍 Entrega** → **📍 Dirección** | **Mis pedidos** (tarjeta) | El botón y su ventana | "📍 Entrega", junto a "📦 Entregar" | "📍 Dirección"; ventana *"📍 Dirección y fecha de entrega — Pedido #N"*; al guardar *"Dirección y fecha guardadas"* |
| 23.7 | Menú de opciones de **todas** las listas (`select`, `p-dropdown`, `datalist`) | Mis pedidos (Ordenar, cobro), Venta directa (forma de pago, lugar de entrega), Carrito, Mis datos, Clientes, Entregas por zona, Personalización, Rifas, Flores, ventanas con lista | Al abrir la lista | En los diseños Clásico de noche: menú blanco con letra blanca | Fondo sólido y letra que contrasta, en los 5 diseños, de día y de noche |
| 23.8 | Encabezado del chat de la burbuja 💬 | Todas las pantallas | De día | "Asistente Virtual" y la ✕ casi blancos sobre claro | Se leen |
| 23.9 | "$" verde de Reportes | **Reportes** | De día | Verde claro sobre la tarjeta | Verde más oscuro, se lee |
| 23.6 | Texto del permiso `editar-entrega` (script) | **Sistema → 🛡️ Gestión de roles** → Mis pedidos | La etiqueta del permiso | "📍 Entrega…" | *"Dirección (📍 en la tarjeta)"*. **Quién lo tiene no cambia** |
| — | **Lo que NO debe cambiar** | Login, Registro, colores de día, lo que hace cada botón | — | — | **Lo mismo que antes** |

### 23.1 Menú lateral de noche

| # | Haz esto | Debes ver |
|---|---|---|
| 1 | De noche, abre el grupo **📋 Pedidos** del menú lateral | Los submenús (**Mis pedidos**, etc.) en el mismo color claro que el resto del menú. **Ni azul ni negro** |
| 2 | Pasa el mouse sobre un submenú **que no es** el de la pantalla abierta | Se le pone un fondo suave (el de "seleccionado") y la letra se sigue leyendo. **No** se pone negro |
| 3 | El submenú de la pantalla abierta | Marcado como activo, se lee |
| 4 | Repite en **Ventas**, **Envíos** y **Sistema** | Igual |
| 5 | Cambia a **☀️ Modo claro** y repite 1 y 2 | Igual que antes de este cambio (tus colores de día) |

### 23.2 La ventana de cancelar

| # | Haz esto | Debes ver |
|---|---|---|
| 1 | De noche, **Mis pedidos** → en el buscador escribe **Prueba 22 AP** (el Apartado de la 🧪 Prueba 22) → en su tarjeta **✖ Cancelar** | *"¿Por qué cancelas este pedido?"* con los motivos como botones **oscuros con letra clara**, del color de la ventana (antes: botones blancos) |
| 2 | Pasa el mouse por un motivo y tócalo | Se marca con el color de acento del tema; se lee |
| 3 | Cierra la ventana **sin** cancelar (o cancela AP: es de prueba) | — |
| 4 | Lo mismo de día | Se ve bien con tus colores de día |
| 5 | **💳 Créditos / Abonos → ✖ Cancelar** en cualquier tarjeta, de noche, y ciérrala sin cancelar | Los mismos botones de motivo, del color del tema |

### 23.3 Las casillas de los filtros

| # | Haz esto | Debes ver |
|---|---|---|
| 1 | De noche, **Tienda** → **⚙️ Filtros** | Las casillas (**Con stock**, **Habilitadas**…), las fechas y **$ mín / $ máx** con su fondo de filtro, letra clara, se leen. Nada transparente u opaco |
| 2 | **Catálogo → 🔍 Modelos** → ⚙️ Filtros, de noche | Igual |
| 3 | Las dos de día | Igual que antes |

### 23.4 El botón 📍 Dirección

| # | Haz esto | Debes ver |
|---|---|---|
| 1 | **Mis pedidos** → cualquier tarjeta que no esté entregada | El botón dice **📍 Dirección** (antes "📍 Entrega"). Al pasar el mouse: *"A quién, dónde y cuándo se entrega: dirección, mapa, zona y fecha"*. **📦 Entregar** sigue igual |
| 2 | Tócalo | Ventana **"📍 Dirección y fecha de entrega — Pedido #N"**, con lo mismo de antes (a quién, dirección, mapa, zona, fecha) |
| 3 | Cambia algo y guarda | *"Dirección y fecha guardadas"* y se ve en la tarjeta |
| 4 | **Sistema → 🛡️ Gestión de roles** → un rol → busca **Mis pedidos** | El permiso dice **"Dirección (📍 en la tarjeta)"**. Los roles que lo tenían lo siguen teniendo |
| 5 | Entra con un usuario de un rol **sin** ese permiso | No ve el botón 📍 Dirección (igual que antes con "Entrega") |

### 23.5 Las listas desplegables (lo de "salían en blanco")

Hazlo de noche y de día, con tus temas de siempre. En cada lista: ábrela y mira **todas** las opciones.

| # | Haz esto | Debes ver |
|---|---|---|
| 1 | **📋 Pedidos → Mis pedidos** → la lista **Ordenar** (arriba de las tarjetas) | Todas las opciones se leen: fondo del color de tus campos y letra que contrasta. **Nada en blanco con letra blanca** |
| 2 | **Ventas → 💰 Venta directa** → agrega un artículo → **💵 Contado** → la lista de **forma de pago** (y la de meses, si eliges tarjeta) | El panel que se abre se lee |
| 3 | En la misma pantalla, la lista **Lugar de entrega** | Se lee |
| 4 | **🚚 Envíos → 📦 Entregas por zona** → la lista de zonas | Se lee |
| 5 | **Mis datos** (tu perfil) → la lista **Sexo** · **Sistema → 🎨 Personalización** → cualquier lista | Se leen |
| 6 | **Mis pedidos** → una tarjeta → **📍 Dirección** → la lista de zona dentro de la ventana | Se lee |
| 7 | (Opcional) En el **celular**, abre una de esas listas | El celular la muestra con su propio menú; se lee |

### 23.6 El chat de día y Reportes

| # | Haz esto | Debes ver |
|---|---|---|
| 1 | De día, toca la burbuja **💬** de abajo a la derecha | Arriba de la ventana del chat se lee **"Asistente Virtual"**, "En línea" y la ✕ / − para cerrar |
| 2 | De noche, lo mismo | Igual que antes (encabezado de color con letra clara) |
| 3 | De día, **Reportes** | Las cifras verdes con "$" de las tarjetas se leen |

❌ **Está mal si:** de noche algún submenú sigue azul o negro, o se pone negro al pasar el mouse; la
ventana de cancelar sale con botones blancos de noche; las casillas de los filtros no se leen; queda
algún botón que diga "📍 Entrega"; un rol pierde o gana el permiso; **alguna lista se abre en blanco o
con letras que no se leen**; "Asistente Virtual" no se ve de día.

💬 Notas:

- [ ] 23.1 · [ ] 23.2 · [ ] 23.3 · [ ] 23.4 · [ ] 23.5 · [ ] 23.6 — **Prueba 23 terminada**

---

## Prueba 13 — Fondo de los filtros desde Personalización y seguridad de la tienda

**Para qué es:** dos cosas del 2026-10-07.
1. En **Tienda** y en **Catálogo → 🔍 Modelos**, el recuadro de la búsqueda y los filtros se había
   quedado **sin fondo** (todo suelto sobre la página). Vuelve el recuadro, y su color y el de cada
   filtro se cambian en **Sistema → Personalización → Formularios**.
2. La tienda ahora manda **encabezados de seguridad** (`SEGURIDAD_DATOS.md` §6, S1).

**Por qué se había perdido el fondo:** la regla del 2026-10-06 que dejó los encabezados de pantalla
en una sola franja ("sin un div dentro de otro div") volvía transparente todo `*-header__content`.
En Tienda y en Modelos ese elemento **es** el recuadro (no hay franja afuera), así que se quedó sin
nada. Esas dos pantallas salen de esa regla; las demás siguen igual.

💬 2026-10-13: *"los fondos se ven muy feos"* (de noche). ↳ Era la misma causa que los submenús (Prueba
23): colores de noche que no llegaban, y la regla de letras de Jade encima de las casillas. Arreglado; el
paso 4 de 13.1 se revisa **después de la subida del 2026-10-13**, con tu tema **Noche "Jade"**.

### 13.0 Antes de empezar

1. ✅ `migration_tema_filtros.sql` corrida en `inventario_key_qa` (2026-10-07) y el front ya está en QA.
   El arreglo de noche llega con la subida del 2026-10-13.
2. **Tú:** `Ctrl + Shift + R`.

### Mapa de impacto

| # | Se movió | Le pega a | Antes | Después |
|---|---|---|---|---|
| 13.1 | La regla que vuelve transparente `*-header__content` ya no aplica a `vb-header__content` ni `pl-header__content` | **Tienda** | Título, búsqueda y filtros sueltos sobre la página | **Recuadro** blanco translúcido de día y gris oscuro de noche, con borde y sombra |
| 13.2 | (la misma) | **Catálogo → 🔍 Modelos** | Igual que Tienda: sin recuadro | Igual que Tienda: con recuadro |
| 13.3 | (la misma) | Las demás pantallas con encabezado: Créditos / Abonos, Clientes, Dashboard, Reportes, Gastos, Favoritos, Carga de imágenes, Buscar usuarios | Una sola franja con el color de Personalización | **Lo mismo que antes** |
| 13.4 | El subtítulo ya no lleva la clase `text-white-50` (blanco forzado) | "Todos nuestros productos" (Tienda) y "Catálogo de productos" (Modelos) | Gris | **Gris, igual** (sin el cambio, de día quedaba blanco sobre blanco) |
| 13.5 | 2 colores nuevos: `filtros-panel-bg` y `filtro-bg` | **Sistema → Personalización → Formularios** | No existían | "Fondo del recuadro de búsqueda y filtros (Tienda y Productos)" y "Fondo de cada filtro (casillas, fechas y precio)" |
| 13.6 | Las casillas, las fechas, **$ mín / $ máx** y **✕ Limpiar filtros** usan `filtro-bg` | Tienda y Modelos | Verde muy claro (el color de "seleccionado") | **El mismo verde** (mismo valor), pero ahora se cambia aparte |
| 13.7 | **Talla / Color / Marca** son selects | Tienda | Diseño de todos los selects (fondo de los campos) | **Lo mismo que antes**: siguen la regla de todos los selects del 2026-10-06 |
| 13.8 | Los 5 diseños predefinidos traen los 2 colores | **Personalización → Diseños predefinidos** | — | Al aplicar uno, los 2 colores cambian con él |
| 13.9 | Encabezados de seguridad en el nginx de la tienda (`default.conf`) | Todas las pantallas | No había ninguno | 6 encabezados (ver 13.4 de abajo) |
| 13.10 | La cámara y la ubicación quedan permitidas solo para la tienda | 📷 Escanear código de barras, 📡 Usar mi ubicación | Funcionaban | **Lo mismo que antes** |
| — | **Lo que NO debe cambiar** | Buscar, filtrar, guardar filtros, carrito, login | — | **Lo mismo que antes** |

### 13.1 El recuadro de Tienda y Modelos

| # | Haz esto | Debes ver |
|---|---|---|
| 1 | Abre **Tienda** de día | El título, la búsqueda, **⚙️ Filtros** y los filtros dentro de un **recuadro blanco** con borde y sombra |
| 2 | Abre **⚙️ Filtros** | Las casillas, las fechas y **$ mín / $ máx** con fondo verde claro, **dentro** del recuadro |
| 3 | Debajo de **Tienda** | "Todos nuestros productos" en gris, se lee |
| 4 | Cambia a noche (abajo en el menú lateral, **🌙 Modo oscuro**) | Recuadro gris oscuro con borde; **las casillas, fechas y $ mín / $ máx se leen** (después de la subida del 2026-10-13) |
| 5 | En el celular | El recuadro cabe sin mover la página de lado; los filtros, uno debajo del otro |
| 6 | **Catálogo → 🔍 Modelos**, de día y de noche | Lo mismo: recuadro, filtros dentro, "Catálogo de productos" se lee |
| 7 | **Créditos / Abonos**, **Clientes** y **Reportes** | Su encabezado **igual que antes** (una franja, sin recuadro adentro) |

❌ **Está mal si:** el recuadro no se ve de día; el subtítulo desaparece; Créditos / Abonos o Clientes
ahora tienen un recuadro dentro de la franja.

### 13.2 Cambiarlo desde Personalización

| # | Haz esto | Debes ver |
|---|---|---|
| 1 | **Sistema → Personalización** → abre **Formularios** | Dos colores nuevos: **Fondo del recuadro de búsqueda y filtros (Tienda y Productos)** y **Fondo de cada filtro (casillas, fechas y precio)** |
| 2 | Pon el del recuadro en un color fuerte (por ejemplo amarillo) para día → guarda | Abre **Tienda**: el recuadro cambió a ese color |
| 3 | Pon el de cada filtro en otro color → guarda | Las casillas, fechas y $ mín / $ máx cambian. **Talla / Color / Marca no** (siguen el fondo de los campos) |
| 4 | **Diseños predefinidos → Jade → Usar para día y noche** | Los dos regresan: recuadro blanco translúcido y filtros verde claro |

❌ **Está mal si:** los colores no aparecen en Formularios (falta el script 13.0); cambiar el color no
cambia nada en Tienda.

### 13.3 Los filtros siguen funcionando igual

| # | Haz esto | Debes ver |
|---|---|---|
| 1 | En **Tienda** marca **Con stock** y **Habilitadas**, elige una talla y pon $ mín 100 | La lista se filtra **igual que antes** y el contador dice "N activos" |
| 2 | Sal de la pantalla y vuelve | Los filtros se quedan guardados, como antes |
| 3 | **✕ Limpiar filtros** | Se quitan y vuelve la lista completa |

### 13.4 Encabezados de seguridad

| # | Haz esto | Debes ver |
|---|---|---|
| 1 | En la computadora abre `qa.shop.novedades-jade.com.mx`, `F12` → **Red** → recarga → toca la primera fila (la página) → **Encabezados de respuesta** | `x-frame-options: SAMEORIGIN`, `content-security-policy: frame-ancestors 'self'`, `x-content-type-options: nosniff`, `referrer-policy: strict-origin-when-cross-origin`, `permissions-policy: camera=(self), geolocation=(self), microphone=()`, `strict-transport-security: max-age=31536000` |
| 2 | (Opcional, en la VPS) `curl -sI https://qa.shop.novedades-jade.com.mx/ \| grep -iE "x-frame\|strict\|nosniff\|referrer\|permissions\|content-security\|server"` | Los mismos 6 y `Server: nginx` sin número de versión |
| 3 | En el celular: **Tienda** → **📷 Escanear código de barras** | Pide permiso de cámara y lee el código **igual que antes** |
| 4 | **Sistema → Negocio & Contactos** → en el mapa, **📡 Usar mi ubicación** | Pide permiso y pone el pin **igual que antes** |
| 5 | En el celular, en la card de un artículo de **Tienda**, toca **Compartir imagen** | Abre el menú de compartir del celular (o copia la imagen), como antes |
| 6 | Abre el link de la tienda desde Facebook o Instagram | Abre normal |
| 7 | **(Va con S2)** En la VPS: `curl -sI http://qa.shop.novedades-jade.com.mx/` | `301` con `Location: https://qa.shop…`. Si sale `200`, avísame (falta el redireccionamiento) |

❌ **Está mal si:** la cámara o la ubicación ya no piden permiso o marcan error; alguna imagen,
letra o pantalla deja de cargar.

💬 Notas:

- [ ] **Prueba 13 terminada**

---

## Prueba 24 — Unir un Ir pagando que ya tiene dinero (lo de K4 / K5 / K6) 🆕

**Para qué es:** tu nota del 2026-10-13: el K6 (Ir pagando con un tanto) no salía en el buscador de
"🔗 Unir" desde el detalle de K4. Lo revisé: el buscador **no** deja fuera a nadie por el dinero; K6 no
salía porque en la **Prueba 4.6 se canceló** (devolución), y los cancelados no se pueden unir. Esta
prueba lo comprueba con pedidos nuevos.

**Antes de empezar:** **🧪 Preparar prueba 24** (o ya lo hiciste con **Preparar todas**). Anota:

| Pedido | Qué es | Número |
|---|---|---|
| **K4** | 💳 Ir pagando sin dinero, debe $150 · cliente «Prueba 24 K4» | |
| **K6** | 💳 Ir pagando que ya dio $40, debe $110 · cliente «Prueba 24 K6» | |

(Los dos "ya se lo llevó", como en la Prueba 4. Cada uno es de su propio cliente, y **Unir** acepta pedidos de clientes distintos.)

| # | Haz esto | Debes ver |
|---|---|---|
| 1 | **📋 Pedidos → Mis pedidos** → escribe **Prueba 24 K4** → en su tarjeta **👁 Detalle** (o ábrelo con su enlace en el recuadro 🧪) → **🔗 Unir con otros pedidos** | El buscador de pedidos para unir |
| 2 | Escribe **Prueba 24 K6** (o el número de K6) | **Sale K6**, con *abonado $40.00* |
| 3 | Borra y escribe **Prueba 24** | Sale K6, el único otro Ir pagando abierto de la prueba (K4 es el pedido desde el que buscas). **No** salen Apartados ni cancelados |
| 4 | Elige **K6** → **Unir 2 pedidos** | Quedan unidos. El grupo suma **$300** y ya tiene **$40** pagados: debe **$260** |
| 5 | Abre el detalle de **K6** | Dice que está unido con K4 |
| 6 | (Contraprueba) Toca otra vez **🧪 Preparar prueba 24**: **reinicia** y te deja K4 y K6 **nuevos y sin unir** (los de antes quedan cancelados; sale ♻️). Cancela el K6 nuevo con **El cliente avisó**. Después, desde el K4 nuevo → 🔗 Unir → escribe **Prueba 24 K6** | **No** sale ninguno: los cancelados no se unen (eso le pasó a tu K6) |

❌ **Está mal si:** un Ir pagando con dinero, no cancelado y fuera de un grupo **no** sale en el buscador;
o un cancelado sí sale.

💬 Notas:

- [x] **Prueba 24 terminada** (2026-10-13: "esta prueba está bien")

---

## Prueba 22 — 🕓 Pendiente en Pago y Mis pedidos sin cancelados al abrir (2026-10-13)

**Para qué es:** tus respuestas del 2026-10-13.
- *"El filtro de Pendiente: como no hay un filtro de Pendientes, aparecen siempre y no deberían… debería
  ir en Pagos, donde está Falta pagar, Pagado, etc."* Antes, 🕓 Pendiente estaba en **Forma de cobro** y
  además los Pendientes salían **siempre** dentro de **💰 Falta pagar**. Ahora 🕓 Pendiente es un botón
  del bloque **Pago** y Falta pagar ya **no** los trae.
- *"Debe haber todos los filtros para volverlos a mostrar; todo lo que se pueda filtrar se tiene que
  hacer."* Al abrir Mis pedidos tampoco salen los **cancelados**; se ven con **❌ Cancelado**.

**Antes de empezar:** ✅ ya está en QA (2026-10-13); haz `Ctrl + Shift + R`. **🧪 Preparar prueba 22**
(o ya lo hiciste con **Preparar todas**). Crea:

| Pedido | Qué es | Número |
|---|---|---|
| **PE** | 🕓 Pendiente (en "Zona Prueba QA") · cliente «Prueba 22 PE» | |
| **AP** | 📦 Apartado que debe $150 · cliente «Prueba 22 AP» | |
| **CA** | Apartado cancelado ("el cliente avisó") · cliente «Prueba 22 CA» | |


### Mapa de impacto

| # | Se movió | Le pega a | Dónde se ve | Antes | Después |
|---|---|---|---|---|---|
| 22.1 | 🕓 Pendiente pasa de Forma de cobro a **Pago** | ⚙️ Filtros de Mis pedidos | Los botones | Forma de cobro: 🕓 Pendiente · 🛒 Contado · 📦 Apartado · 💳 Ir pagando | Forma de cobro: 🛒 Contado · 📦 Apartado · 💳 Ir pagando. Pago: **🕓 Pendiente** · 💰 Falta pagar · ✅ Pagado · ❌ Cancelado |
| 22.2 | 💰 Falta pagar ya no incluye Pendientes | ⚙️ Filtros → Pago | La lista | PE salía junto con AP | Solo AP (y los demás Apartados / Ir pagando que deben) |
| 22.3 | Vista de inicio sin cancelados | Mis pedidos al abrir | La lista y el aviso | CA salía | CA no sale; el aviso dice cómo verlo |
| 22.4 | Filtros guardados con 🕓 Pendiente en Forma de cobro | Mis pedidos al volver a entrar | El panel | — | El botón aparece prendido en **Pago** |
| 22.5 | (sin cambio) 📦 Apartado, 💳 Ir pagando, 🛒 Contado | ⚙️ Filtros → Forma de cobro | La lista | — | **Lo mismo que antes** |
| 22.6 | (sin cambio) Entrega, Dinero, Fecha de entrega | ⚙️ Filtros | La lista | — | **Lo mismo que antes** |

### 22.1 Los botones y lo que trae cada uno

| # | Haz esto | Debes ver |
|---|---|---|
| 1 | **Mis pedidos** → **⚙️ Filtros** | **Forma de cobro:** 🛒 Contado · 📦 Apartado · 💳 Ir pagando (ya sin 🕓 Pendiente). **Pago:** 🕓 Pendiente · 💰 Falta pagar · ✅ Pagado · ❌ Cancelado |
| 2 | Toca solo **🕓 Pendiente** | Sale PE y los demás Pendientes; **no** sale AP |
| 3 | Quita Pendiente y toca solo **💰 Falta pagar** | Sale AP; **no** sale PE (antes sí salía) |
| 4 | Prende los dos (**🕓 Pendiente** + **💰 Falta pagar**) | Salen PE y AP |
| 5 | Toca la **ⓘ** del bloque Pago | 🕓 Pendiente explica que lo pidió el cliente y nadie lo ha cobrado; 💰 Falta pagar dice que los Pendientes tienen su propio botón |
| 6 | **📦 Apartado** en Forma de cobro | Lo mismo que antes: solo Apartados |

### 22.2 Al abrir, sin cancelados

| # | Haz esto | Debes ver |
|---|---|---|
| 1 | **✕ Quitar filtros** y sal y vuelve a entrar a **Mis pedidos** | Bajo el buscador: *"No se muestran los pedidos ya pagados y entregados ni los cancelados…"*. Sale PE y AP; **no** sale CA |
| 2 | **⚙️ Filtros → ❌ Cancelado** | Sale CA y el aviso se va |
| 3 | **✕ Quitar filtros** y escribe **Prueba 22 CA** en el buscador | Sale CA |

**❌ Está mal si…** 🕓 Pendiente sigue en Forma de cobro; Falta pagar trae Pendientes; un cancelado sale al
abrir; con ❌ Cancelado no salen los cancelados; Apartado o Ir pagando dejan de traer lo de siempre.

💬 Notas:

- [x] **Prueba 22 terminada** (2026-10-13: pasaron las dos partes, 22.1 y 22.2, con los pedidos de 🧪 Preparar prueba 22)

---

## Prueba 21 — La fecha de entrega en Pedidos y en Entregas por zona (2026-10-13)

**Para qué es:** tu nota del 2026-10-13: *"cuando busco los pedidos por zona y les pongo la fecha, no me
aparece en pedidos la fecha en la que se va a entregar; y en pedidos hay una que dice fecha pero no sé de
qué es"*.

**Qué estaba pasando:**
- **Entregas por zona sí guardaba la fecha** en cada pedido de la lista (y mandaba los correos), pero la
  card de **Mis pedidos** solo enseñaba la fecha de entrega si el pedido **todavía debía dinero**. Un
  pedido ya pagado que faltaba entregar (los que justamente salen en Entregas por zona) la escondía.
  Ahora la card mira si **ya se lo llevó** (Entregado / Falta entregar), no si ya pagó.
- La fecha que decía **"Pedido"** en la card, **"Fecha"** en la tabla de Entregas por zona y la del
  🕒 del detalle es **el día en que se hizo el pedido**, no el de entrega. Ahora lo dicen así.

- **El nombre del cliente** (tu nota del 2026-10-13 al probar esto): *"esta semana sale el nombre de quien dio
  de alta el pedido, en lugar del nombre que se puso en la venta; ya habíamos quedado en tomar primero el
  nombre de la venta, y si no hay, el de quien hizo la venta"*. Pasaba porque la Venta directa, cuando **no**
  registras al cliente y solo escribes su nombre, guarda dos cosas en el pedido: **tu cuenta** (la de quien
  atiende) y **el nombre que escribiste**. Mis pedidos ya enseñaba el nombre escrito, pero **Entregas por zona
  tomaba primero la cuenta** y salías tú en toda la columna **Cliente**. Lo mismo pasaba en **Créditos / Abonos**,
  en el nombre del formulario de cobro y de los tickets (detalle del pedido), y 🔗 **Unir** ni siquiera buscaba
  por el nombre escrito. Ahora todas las pantallas usan **la misma regla que Mis pedidos**: primero el nombre
  de la venta; solo si no hay, el de la cuenta de quien la hizo.

**Dónde va cada fecha (respuesta a "¿va en pedidos o en el detalle?": en los dos):**

| Pantalla | Cuándo se hizo el pedido | Cuándo se entrega |
|---|---|---|
| **Mis pedidos** (card) | 📅 **Pedido hecho el** | 🚚 **Se entrega el** sáb 18 oct, 10:00 · Zona (o 🏢 **Recoge en el local el** …). Si es de una zona y todavía no se programa: **Se entrega: Sin fecha todavía · Zona** |
| **Detalle del pedido** | 🕒 **Pedido hecho el** 13/10/2026 14:30 hrs | En "Entrega": **Se entrega el:** 18/10/2026 a las 10:00, en (punto de encuentro) |
| **Entregas por zona** (tabla) | Columna **Pedido hecho el** | Columna nueva **Se entrega el** (o *Sin fecha todavía*) |

**Antes de empezar:** ✅ ya está en QA (2026-10-13); haz `Ctrl + Shift + R`. **🧪 Preparar prueba 21**
(o ya lo hiciste con **Preparar todas**; si ya la habías preparado, tócalo otra vez: **reinicia** y te deja
los pedidos nuevos, con el nombre bien guardado). Crea, en la zona **"Zona Prueba QA"** y sin fecha de entrega,
tres pedidos **vendidos como tú los vendes** (Venta directa sin registrar al cliente: el pedido queda con
**tu cuenta** y con el **nombre escrito**):

| Pedido | Qué es | Nombre escrito en la venta | Número |
|---|---|---|---|
| **P1** | 📦 Apartado que debe $150 | «Prueba 21 P1» | |
| **P2** | 💳 Ir pagando **ya pagado completo** que falta entregar | «Prueba 21 P2» | |
| **P3** | 📦 Apartado que debe $150 (el segundo Apartado, para probar 🔗 Unir) | «Prueba 21 P3» | |

**Ejemplo de lo que debes ver en Entregas por zona** (esta es la tabla de la 21.1, paso 1): en la columna
**Cliente** los tres nombres de arriba, **nunca el tuyo** (el de la cuenta con la que entraste):

| Pedido | Cliente | Total | Pedido hecho el | Se entrega el |
|---|---|---|---|---|
| #(número de P1) | **Prueba 21 P1** | $150.00 | (hoy) | Sin fecha todavía |
| #(número de P2) | **Prueba 21 P2** | $150.00 | (hoy) | Sin fecha todavía |
| #(número de P3) | **Prueba 21 P3** | $150.00 | (hoy) | Sin fecha todavía |


### Mapa de impacto

| # | Se movió | Le pega a | Dónde se ve | Antes | Después |
|---|---|---|---|---|---|
| 21.1 | Cuándo la card enseña la fecha de entrega: por **Entregado / Falta entregar**, ya no por el pago | Mis pedidos (admin y la del cliente) | Renglón 🚚 de la card | Solo si debía dinero | Siempre que falte entregar |
| 21.2 | (mismo) pedido que debe y tiene fecha | Mis pedidos | Renglón 🚚 | Salía | **Lo mismo que antes** |
| 21.3 | (mismo) pedido ya entregado | Mis pedidos | Renglón 🚚 | No salía | **Lo mismo que antes** (no sale) |
| 21.4 | (mismo) "⚠ Atrasado N días" | Mis pedidos | Junto a la fecha | Solo en los que debían | También en pagados que faltan entregar y ya pasó la fecha |
| 21.5 | Nombres de las fechas | Card, detalle y Entregas por zona | Las etiquetas | "Pedido", 🕒 sin nombre, "Fecha" | "Pedido hecho el", "Se entrega el" |
| 21.6 | `GET /v1/entregas-zona/{id}/pendientes` trae `fechaEntrega` y `horaEntrega` | Entregas por zona | Columna nueva | No existía | **Se entrega el** |
| 21.7 | **El cambio:** programar la entrega y verla | Entregas por zona → Mis pedidos → detalle | Las tres pantallas | La fecha solo llegaba en el correo (y en el detalle) | Sale en las tres |
| 21.8 | **El nombre del comprador:** primero el de la venta, y solo si no hay, el de la cuenta de quien la hizo | **Entregas por zona** (columna Cliente) | La tabla | Salía el nombre de quien dio de alta el pedido | Sale el nombre escrito en la venta |
| 21.9 | (mismo) | **Ventas → 💳 Créditos / Abonos** (tarjetas y formulario de abono) | El nombre de la tarjeta | Salía el de la cuenta | Sale el nombre escrito |
| 21.10 | (mismo) | Formularios de cobro (**Liquidar**, **Dar abono**) y tickets | El nombre arriba del formulario | Salía el de la cuenta | Sale el nombre escrito |
| 21.11 | (mismo) y 🔗 **Unir** también busca por el nombre escrito | **🔗 Unir con otros pedidos** | El buscador y la tarjeta | No encontraba por el nombre escrito (solo por el de la cuenta o el cliente sin registro) | Encuentra y enseña el nombre escrito |
| 21.12 | (sin cambio) Mis pedidos | **Mis pedidos** | La card | Ya enseñaba el nombre escrito | **Lo mismo que antes** |
| 21.13 | (mismo) correos de aviso y comprobantes | **Avisar a N cliente(s)** | El saludo del correo | Iba a la cuenta de quien atendió aunque la venta trajera un cliente sin registro | Va al cliente de la venta (si la venta solo trae tu cuenta, sigue llegando a ti) |

### 21.1 Antes de programar

| # | Haz esto | Debes ver |
|---|---|---|
| 1 | **🚚 Envíos → 📦 Entregas por zona** → en la lista **📍 Zona** elige **Zona Prueba QA** → toca el botón **Esta semana** | La tabla de arriba: columnas **Cliente**, **Pedido hecho el** y **Se entrega el**. P1, P2 y P3 dicen *Sin fecha todavía*. **Cliente** dice «Prueba 21 P1», «Prueba 21 P2» y «Prueba 21 P3» — **no tu nombre** |
| 2 | **Mis pedidos** → escribe **Prueba 21 P1** | 📅 **Pedido hecho el** con fecha y hora. 🚚 **Se entrega: Sin fecha todavía · (zona)** |
| 3 | Escribe **Prueba 21 P2** | Igual que P1: 🚚 **Se entrega: Sin fecha todavía · (zona)** (antes no salía nada) |
| 4 | Con el rango de **Esta semana** puesto, cambia la **📍 Zona** a otra y regresa a **Zona Prueba QA** | La tabla se vuelve a llenar con los mismos tres, con el mismo nombre en **Cliente** |

### 21.2 Programar y ver la fecha

| # | Haz esto | Debes ver |
|---|---|---|
| 1 | En **Entregas por zona**, llena 📅 Fecha de entrega (por ejemplo el sábado), hora y punto de encuentro **Prueba 21 Punto** → **✉️ Avisar a 3 cliente(s)** → **Sí, enviar avisos** | *"Se avisó por correo a N cliente(s). La fecha de entrega quedó guardada en los 3 pedido(s): se ve en Mis pedidos y en el detalle de cada uno."* Como estos pedidos llevan **tu cuenta** (la venta no trae cliente con correo), los correos te llegan **a ti**, al correo de tu cuenta: es normal. Si tu cuenta no tiene correo, N será 0 |
| 2 | La tabla se vuelve a cargar | P1, P2 y P3 dicen en **Se entrega el** la fecha y la hora que pusiste. **Cliente** sigue diciendo los mismos tres nombres |
| 3 | **Mis pedidos** → escribe **Prueba 21 P1** | 🚚 **Se entrega el** sáb (día) (mes), (hora) · (zona) |
| 4 | Escribe **Prueba 21 P2** (el ya pagado) | **También** sale 🚚 **Se entrega el** … (antes aquí no salía) |
| 5 | Abre el detalle de P2 | Arriba 🕒 **Pedido hecho el** (fecha y hora de cuando se hizo). En "Entrega": **Se entrega el:** (fecha) a las (hora), en (punto) |
| 6 | En la card de P2, **📦 Entregar** | El renglón 🚚 desaparece (ya se lo llevó) |
| 7 | Con **P1** (sigue sin entregar): vuelve a **Entregas por zona**, pon una fecha de entrega **ya pasada** (por ejemplo, ayer) → **Avisar** → **Mis pedidos** → escribe **Prueba 21 P1**. *(Si el calendario no te deja escoger un día pasado, anótalo en 💬: esta parte la reviso yo.)* | ⚠ **Atrasado N días** junto a la fecha |
| 8 | *(Opcional: «Prueba 21 P1» es un cliente sin cuenta, así que con los datos de 🧪 no se puede entrar como él. Solo si ya tienes una cuenta de cliente con un pedido programado.)* Con la cuenta del cliente → **Mis pedidos** | También ve 🚚 **Se entrega el** … |

### 21.3 El nombre del cliente en las demás pantallas (lo que reportaste)

Es la misma regla en todas partes: primero el nombre que se escribió en la venta; solo si no hay, el de la cuenta.

| # | Haz esto | Debes ver |
|---|---|---|
| 1 | **Ventas → 💳 Créditos / Abonos** → pestaña **📋 Cuentas por cobrar** → busca la tarjeta del Apartado P1 (debe $150) | La tarjeta dice **Prueba 21 P1** (no tu nombre). P3 igual |
| 2 | En esa tarjeta toca **+ Abono** y mira el nombre arriba del formulario; ciérralo sin guardar | Dice **Prueba 21 P1**. (Un Apartado se paga completo: no hace falta guardarlo) |
| 3 | **Mis pedidos** → escribe **Prueba 21 P1** → en su tarjeta toca **Liquidar** y mira el renglón de arriba con el número y el nombre; ciérralo con ✕ sin pagar | Dice **Prueba 21 P1** |
| 4 | **Mis pedidos** → abre el **👁 Detalle** de P1 → **🔗 Unir con otros pedidos** → escribe **Prueba 21 P3** | Sale **un solo pedido**: P3, y su tarjeta dice **Prueba 21 P3**. (P2 no sale: es Ir pagando y P1 es Apartado; solo se unen de la misma forma de cobro) |
| 5 | En ese mismo buscador borra y escribe el **número** de P3 | Sale P3 con el mismo nombre |
| 6 | (Contraprueba) Sin cerrar, escribe **Prueba 21 P2** | **No** sale ninguno: es de otra forma de cobro |

**❌ Está mal si…** un pedido programado en Entregas por zona no enseña la fecha en la card o en el
detalle; un pedido ya entregado sigue enseñando "Se entrega el"; la columna **Se entrega el** queda
vacía después de avisar; la fecha de "Pedido hecho el" cambia al programar la entrega; **en Entregas por zona, Créditos / Abonos o los formularios de cobro sale tu nombre en lugar de «Prueba 21 …»**; 🔗 Unir no encuentra a P3 escribiendo su nombre.

💬 Notas:

💬 *"Ya está el nombre correcto en Entregas y además ya se ve la fecha en Pedidos de cuándo se va a entregar."* (2026-10-13, ya con el arreglo en QA, commit `486c0b5`)
↳ Anotado: el nombre de la venta («Prueba 21 P1/P2/P3») sale en la columna **Cliente** de Entregas por zona, y la fecha de entrega programada sale en **Mis pedidos**. Confirmado por el dueño: 21.1 y 21.2. Sin falla reportada en 21.3 (Créditos / Abonos, formularios de cobro y 🔗 Unir); el paso 7 de 21.2 (⚠ Atrasado) quedó sin confirmar.

💬 *"21.1 Antes de programar ya quedó; 21.2 Programar y ver la fecha, ya se ve en Pedidos después de poner la fecha de entrega. Hay que revisar los filtros o las búsquedas de los pedidos porque tarda mucho para usar un filtro o quitarlo; no sé si lo estás paginando o qué está pasando."* (2026-10-14)
↳ 21.1 y 21.2 anotadas como pasadas. Lo de la lentitud se midió: la pantalla sí pagina (de 10 en 10); lo lento era el texto escrito en el buscador (48 s con 6 000 pedidos). Arreglado en el back y con su propia prueba: **Prueba 25**.

- [x] **Prueba 21 terminada** (2026-10-13)

---

## Prueba 20 — Cliente que no podía pedir, pedidos unidos, modelo agotado y Mis pedidos (2026-10-13)

**Para qué es:** tus notas del 2026-10-13. (1) Un cliente con el correo verificado llenaba sus datos y
el carrito se los volvía a pedir. (2) Dos pedidos 🕓 Pendiente unidos no se podían pasar a Ir pagando ni a
Apartado (*"deshaz el grupo antes…"*). (3) Un modelo agotado (stock 0) no se podía guardar en Actualizar
Modelo. (4) Mis pedidos debe abrir sin los pedidos que ya están pagados y entregados.

**Antes de empezar:** ✅ ya está en QA (2026-10-13); haz `Ctrl + Shift + R`. Para la 20.1 ten a la mano un
correo nuevo para crear una cuenta de cliente (esa no se puede preparar con botón: es justo lo que se prueba).
Para la 20.2 y la 20.3: **🧪 Preparar prueba 20** (o ya lo hiciste con **Preparar todas**).

### Mapa de impacto

| # | Se movió | Le pega a | Dónde se ve | Antes | Después |
|---|---|---|---|---|---|
| 20.1 | Al guardar el cliente se borra su copia guardada (caché) | Carrito, Mis datos, Mi perfil, admin → Clientes | Si pide datos otra vez | Seguía viendo los datos viejos hasta 10 min | Ve lo recién guardado |
| 20.2 | "Completa tus datos" lleva a **Mis datos** y regresa | Carrito, detalle de producto, Tienda → Buscar (♥), Mis pedidos, Flores, Favoritos | A dónde lleva el botón | **Nuevo cliente** vacío (pedía el correo otra vez) o `/mis-datos` (404) | Mis datos con tu correo ✅ Verificado; al guardar regresa |
| 20.3 | Teléfono obligatorio en Mis datos | Mis datos | Al guardar sin teléfono | Guardaba y el carrito seguía pidiendo datos | *"Faltan datos obligatorios"* con el teléfono en rojo |
| 20.4 | 🔁 Cambiar forma de cobro de un pedido unido | Detalle del pedido (unido) | El aviso al guardar | *"deshaz el grupo antes…"* | Cambia **todos** los del grupo |
| 20.5 | (mismo) en un pedido **no** unido | Detalle del pedido | — | Funcionaba | **Lo mismo que antes** |
| 20.6 | Stock 0 al editar un modelo | Catálogo → 🔍 Modelos → ✏️ Actualizar | Al guardar | *"El stock no debe de ser 0"* | Guarda |
| 20.7 | (mismo) alta de modelo nuevo | Catálogo → Agregar modelo con stock 0 | Al guardar | No dejaba | **Lo mismo que antes** (no deja) |
| 20.8 | Mis pedidos abre con `SIN_TERMINAR` | Mis pedidos (admin) | La lista al entrar | Todos, también los ya pagados y entregados | Sin esos; con cualquier filtro o búsqueda sale lo que pidas |

### 20.1 Cliente nuevo hace su primer pedido

| # | Haz esto | Debes ver |
|---|---|---|
| 1 | Crea una cuenta nueva (nombre **Prueba 20 Cliente nuevo**, con un correo tuyo), verifica el correo, acepta Términos y entra | Entras a la tienda |
| 2 | Agrega un artículo al carrito → **📋 Generar pedido** | *"Completa tus datos — necesitamos tu nombre, apellidos y teléfono. Tu correo ya está verificado."* → **Completar mis datos** |
| 3 | — | Abre **Mis datos** con el aviso 🛒 *"Para terminar tu pedido…"* y tu correo ya escrito con **✅ Verificado** |
| 4 | Guarda **sin** teléfono | *"Faltan datos obligatorios"* y el teléfono en rojo |
| 5 | Llena todo con * y **Guardar** | *"Datos guardados — Ya puedes terminar tu pedido"* → **🛒 Regresar al carrito** te regresa |
| 6 | **📋 Generar pedido** otra vez | Ya **no** pide datos: sale la confirmación y el pedido se crea |
| 7 | Vuelve a **Mis datos** | Se ven los datos que guardaste (antes se veían vacíos) |
| 8 | Con esa cuenta: **Favoritos** o **Mis pedidos** antes de llenar datos (repite con otra cuenta nueva) | **Completar mis datos** lleva a Mis datos y al guardar regresa a esa pantalla |

### 20.2 Pedidos unidos: cambiar la forma de cobro

**Antes de empezar:** **🧪 Preparar prueba 20**. Crea **P1** y **P2**:
dos 🕓 Pendiente de $150 cada uno (en "Zona Prueba QA"), de los clientes «Prueba 20 P1» y «Prueba 20 P2». Después, en el detalle de P1:
**🔗 Unir con otros pedidos** → escribe **Prueba 20 P2** → **Unir 2 pedidos**.

| # | Haz esto | Debes ver |
|---|---|---|
| 1 | Detalle de P1 → **🔁 Cambiar forma de cobro** | Arriba: *"🔗 Este pedido está unido con otros: el cambio se hace en todo el grupo (pedidos #P1, #P2)…"*. "De contado" bloqueado con su motivo |
| 2 | Elige **Ir pagando**, deja 0 → **Guardar cambio** | *"El grupo quedó como Ir pagando — Cambiaron los pedidos #P1, #P2"*. Los dos dicen Ir pagando |
| 3 | **🔁 Cambiar forma de cobro** → **Apartado** → Guardar | *"El grupo quedó como Apartado"* (no tiene dinero) |
| 4 | Cámbialo a **Ir pagando** escribiendo **$50** en "¿Cobra algo ahora?" | Queda Ir pagando y *"Se registró el abono del grupo de $50.00"*. En el grupo: Pagado $50 |
| 5 | Intenta pasarlo a **Apartado** | **Apartado** bloqueado: *"🔒 No se puede: ya dio $50.00 entre los pedidos del grupo…"* |
| 6 | Un pedido **no** unido: 🔁 Cambiar forma de cobro de Pendiente a Ir pagando | Igual que siempre (20.5) |

- [x] **20.2 ✅ Pasó (2026-10-14).**
💬 *"20.2 Pedidos unidos: cambiar la forma de cobro, quedo bien toda la prueba completa"*

### 20.3 Modelo agotado (reescrita otra vez el 2026-10-14, con lo que pediste)

**Antes de empezar:** vuelve a tocar **🧪 Preparar prueba 20**: deja **QA Prueba 20 Agotado** como
nuevo, con el modelo en stock 0 y su único artículo (talla **M**, **Negro**) también en 0. El modelo trae
foto, color, marca y descripción.

**Lo que hay que saber antes (porque confunde):** hay **dos** campos que se llaman "Actualizar stock" y no
hacen lo mismo.
- El del **modelo** (Catálogo → 🔍 Modelos → ✏️ Actualizar) **mete piezas nuevas** a la tienda.
- El del **artículo** (Tienda → Buscar → ✏️ Editar) **no mete piezas nuevas**: toma piezas
  **libres del modelo** y se las da a esa talla/color.

**Lo nuevo (lo que pediste el 2026-10-14):** si en el artículo pides más de lo que el modelo tiene libre,
ya no sale el error: se abre una ventana para **agregarle stock al modelo** sin salir del artículo. Lo que
pongas ahí (10, 100…) se guarda **solo en el modelo**; el artículo no cambia hasta que tú le pongas su stock
y guardes. Y al guardar un modelo con stock libre (también al **actualizarlo**), te pregunta **cuántos
artículos** quieres hacer; cada artículo nace **igual al modelo** y con checks marcas solo lo que cambia.

| # | Haz esto | Debes ver |
|---|---|---|
| 1 ✅ | **Catálogo → 🔍 Modelos** → en el buscador escribe **QA Prueba 20 Agotado** → en su tarjeta **✏️ Actualizar** → cambia solo la **Descripción** → **💾 Actualizar modelo** | Se guarda. **No sale ningún aviso de error**. La descripción nueva se queda. Como no tiene stock libre, **no** pregunta por artículos: sale *"¡Modelo actualizado!"* y regresa a Modelos. ✅ **Pasó el 2026-10-14** (con el aviso de antes, *"¡Producto actualizado!"*) |
| 2 ✅ | **Tienda → Buscar** → escribe **QA Prueba 20 Agotado** → en la tarjeta del artículo (M, Negro) **✏️ Editar** | En el recuadro de stock: *"Quedan 0 disponibles de 0"* y el botón **➕ Agregar stock al modelo** |
| 3 ✅ | En **Actualizar stock** escribe **1** → **💾 Actualizar artículo** (antes decía *"Actualizar producto"*) | **No** se guarda nada todavía: se abre la ventana **➕ Agregar stock al modelo**: *"El modelo «QA Prueba 20 Agotado» tiene 0 libres y a este artículo le pides 1 más. ¿Cuántas piezas le agregas al modelo?…"* con un **1** ya escrito. ✅ El 2026-10-14 aquí salió el aviso *"El modelo «QA Prueba 20 Agotado» no tiene piezas libres…"* — estaba bien, pero pediste esta ventana en su lugar |
| 4 | **Solo es una prueba de seguridad** (si no la quieres hacer, sáltala al 5). Estás en la ventana **➕ Agregar stock al modelo** del paso 3: el campo trae un **1**. Bórralo, escribe **0** y toca el botón **Agregar al modelo** | Debajo del campo sale en rojo: *"Escribe cuántas piezas: un número entero de 1 en adelante."* La ventana **se queda abierta** y no se guardó nada: el sistema no deja agregar 0 piezas |
| 5 | En ese mismo campo escribe **10** (las piezas que "llegaron") y toca **Agregar al modelo** | Se cierra esa ventana y se abre **otra** con palomita verde: *"Se agregaron 10 al modelo"* y debajo *"Ahora el modelo tiene 10 libres de 10. Este artículo sigue con 0. Ya alcanza para sumarle 1: da 💾 Actualizar artículo."* Toca **Entendido** |
| 6 | Ya no hay ventanas: estás otra vez en la pantalla de **editar el artículo**. **No toques nada**, solo mira. Arriba, el recuadro de stock; abajo, los campos **Stock** y **Actualizar stock** | El recuadro dice *"Quedan 10 disponibles de 10"* (el modelo ya tiene piezas). El campo **Stock** (gris, no se escribe) sigue en **0** y **Actualizar stock** sigue en **1**. Es lo importante: **los 10 se fueron al modelo, no al artículo** |
| 7 | Baja al final de la pantalla y toca el botón grande **💾 Actualizar artículo** | Un aviso corto *"¡Artículo actualizado!"* y regresa a **Tienda → Buscar**. Si buscas **QA Prueba 20 Agotado**, la tarjeta de la talla **M** dice stock **1** (no 10: solo se le dio 1) |
| 8 | Ahora a la pantalla del modelo: menú **Catálogo → 🔍 Modelos** → escribe **QA Prueba 20 Agotado**. Mira la tarjeta. Luego, en esa tarjeta, toca **🧩 Productos** | La tarjeta dice **Stock 10** (los 10 de la ventana; no 11). Al tocar **🧩 Productos** se abre una ventana grande **🧩 Agregar artículos** que dice *"Stock del modelo: 10 · En sus artículos: 1 · Libre: 9"*: de 10 piezas, 1 ya está en un artículo y quedan 9 sin repartir. Ciérrala con **Cancelar** (abajo a la izquierda); aquí no vamos a guardar nada |
| 9 | En la **misma tarjeta** del modelo toca **✏️ Actualizar**. Se abre la pantalla **Actualizar modelo** con los datos llenos. **No cambies nada** y toca el botón **💾 Actualizar modelo** (al final) | Se abre una ventana chica con título **"✅ Modelo actualizado"**, el nombre del modelo, *"Stock del modelo: 10 · En sus artículos: 1 · Libre: 9"* y la pregunta **"¿Cuántos artículos quieres hacer de este modelo?"** con un **1** escrito y debajo *"Puedes hacer de 1 a 9."* Dos botones: **Ahora no** y **Continuar**. (Antes de este cambio solo salía *"¡Producto actualizado!"* y no preguntaba nada) |
| 10 | **Otra prueba de seguridad.** En el campo de la pregunta borra el 1 y escribe **20** (son más de los 9 que caben) | Debajo sale en rojo *"De 1 a 9 (cada artículo necesita al menos 1 pieza)."* y el botón **Continuar** se pone gris: no deja seguir |
| 11 | Cambia el número a **2** y toca **Continuar** | Se abre la ventana grande **🧩 Agregar artículos**. Arriba: *"Stock del modelo… Libre: 9"* y el campo **¿Cuántos artículos?** con 2. Abajo, **dos tarjetas abiertas**: **Artículo 1** y **Artículo 2**. Cada una trae los campos **Talla**, **Stock** y **Presentación** (vacíos) y una fila *"¿Qué cambia en este artículo?"* con 6 casillas redondas **sin marcar**: **Foto, Descripción, Color, Marca, Contenido neto, Categoría**. Al final de cada tarjeta una línea *"Igual al modelo: Foto: la del modelo · Descripción: … · Color: … · Marca: …"*. Eso quiere decir: **si no marcas nada, el artículo sale idéntico al modelo** |
| 12 | En la tarjeta **Artículo 1**: en **Talla** escribe **S**, en **Stock** escribe **1**. Luego toca la casilla **Descripción** | Debajo aparece un campo **Descripción** que ya trae escrita la del modelo. Bórrala y escribe **Prueba 20 artículo S**. En la línea *"Igual al modelo"* ya **no** aparece Descripción (porque este artículo la tiene distinta) |
| 13 | En la tarjeta **Artículo 2**: **Talla** **L**, **Stock** **1**. Toca la casilla **Color**: aparece el campo Color con el del modelo. **Bórralo y déjalo vacío** | Debajo del campo, en rojo: *"Escribe el color o quita la marca de Color."* En la barra de la tarjeta: *"⚠️ Falta: Color"*. Abajo, el botón **Guardar 2 artículos** está **gris** y una línea dice *"⚠️ Al artículo 2 le falta algo."* Es la regla: **si marcas un dato, tienes que llenarlo** (o quitar la marca) |
| 14 | En el campo Color del Artículo 2 escribe **Rojo**. Después toca la casilla **Foto** de ese mismo Artículo 2 | Aparece **"Foto de este artículo"** con el botón **Subir foto** y en rojo *"Sube la foto o quita la marca de Foto."* Toca **Subir foto** y elige cualquier imagen: se ve una miniatura con los botones **Cambiar foto** / **Quitar** y se quita el rojo |
| 15 | Sube en la ventana hasta **¿Cuántos artículos?** (arriba del todo) y cámbialo de 2 a **9** | Aparecen **9 artículos**. Los **1 y 2 se quedan abiertos con lo que ya llenaste**. Del **3 al 9 salen cerrados**: cada uno es **una sola línea** del estilo *"▸ Artículo 3 · Igual al modelo · ⚠️ Falta: stock · Mostrar"*. Abajo: *"⚠️ A los artículos 3, 4, 5, 6, 7, 8 y 9 les falta algo. Abrir solo esos"* y **Guardar 9 artículos** gris |
| 16 | Sobre la lista hay dos botones: **Abrir todos** y **Cerrar todos**. Toca **Cerrar todos**. Luego toca la línea del **Artículo 2** | Los 9 quedan en una línea cada uno. La del 1 dice *"Talla S · Stock 1 · Cambia: Descripción"* y la del 2 *"Talla L · Stock 1 · Cambia: Foto, Color"*. Al tocar la línea del **Artículo 2** se abre **solo ese**, con su foto y su color Rojo. Tócala otra vez (dice **Ocultar**) y se cierra |
| 17 | Regresa **¿Cuántos artículos?** a **2** y toca **Abrir todos** | Desaparecen del 3 al 9. El 1 y el 2 se abren **con todo lo que llenaste** y ya no sale el aviso amarillo de abajo; **Guardar 2 artículos** vuelve a estar activo |
| 18 | Toca **Guardar 2 artículos** (abajo a la derecha) | Aviso *"2 artículos guardados — Ya se pueden vender. Stock repartido: 2 de 10."* Toca **Entendido**: se cierra la ventana y regresas a **Catálogo → 🔍 Modelos** |
| 19 | Menú **Tienda → Buscar** → escribe **QA Prueba 20 Agotado** | Salen **3 tarjetas**: la **M Negro** (stock 1, la del paso 7); la **S** con la descripción *Prueba 20 artículo S*, y con el color y la foto **del modelo** (stock 1); y la **L Rojo** con **su propia foto** y la descripción **del modelo** (stock 1) |
| 20 | **Prueba aparte, ya no es del modelo agotado:** menú **Catálogo → Agregar modelo**. Llena **Nombre** *QA Prueba 20 Modelo sin stock*, **Marca** *Prueba QA*, **Precio de compra** *50*, **Precio de venta** *100*, **Stock** *0*, **Descripción** *Prueba* y prende **Generar código automático** (el interruptor del código de barras). Toca **💾 Guardar modelo** | **No lo guarda**: sale el aviso *"El stock no debe de ser 0"*. Un modelo nuevo tiene que nacer con piezas. Si lo buscas en **Catálogo → 🔍 Modelos** no aparece |

**❌ Está mal si…** el paso 1 pide stock, da error o pregunta por artículos; en el paso 3 se guarda el
artículo o sale el error en vez de la ventana; en el paso 6 el artículo ya tiene los 10 (la ventana solo
toca el modelo); el modelo queda con 11 en el paso 8; en el paso 9 no pregunta; se puede continuar con 20
en el paso 10; se puede guardar con un check marcado y vacío; en el paso 19 el artículo S no trae la foto del
modelo o el L no trae su propia foto; en el paso 15 los nuevos salen abiertos o se pierde lo que ya llenaste
en el 1 y el 2; el paso 20 crea el modelo con stock 0.

**Mapa de impacto de lo nuevo (2026-10-14)** — la ventana 🧩 Agregar artículos es una sola y la abren 4
pantallas; las cuatro se prueban:

| # | Se movió | Le pega a | Dónde se ve | Antes | Después |
|---|---|---|---|---|---|
| a | Pedir más stock del libre en el artículo | Tienda → Buscar → ✏️ Editar | Al dar 💾 | Error *"no tiene piezas libres…"* | Ventana **➕ Agregar stock al modelo** (pasos 2–7) |
| b | Pregunta al guardar un modelo | Catálogo → 🔍 Modelos → ✏️ Actualizar | Al dar 💾 Actualizar modelo | Solo *"¡Producto actualizado!"* | Con stock libre: *"¿Cuántos artículos…?"*; sin libre: *"¡Modelo actualizado!"* (pasos 1 y 9) |
| c | (mismo) | Catálogo → Agregar modelo | Al guardar uno nuevo | *"¿Quieres agregar sus artículos ahora?"* (Después / Sí) | *"¿Cuántos artículos quieres hacer de este modelo?"* (Ahora no / Continuar) — Prueba 17.7 |
| d | Checks por artículo en vez de casillas para todos | Ventana 🧩 desde Agregar modelo, Actualizar modelo, 🧩 Productos y Agregar artículo → Guardar en el modelo | Los formularios | Casillas *"Usar del modelo en todos los artículos"* arriba y campos ya llenos | Cada artículo con sus 6 checks; lo no marcado queda igual al modelo — Pruebas 17.7, 17.8, 18 (pasos 3–4) y 20.3 |
| e | Lo que se guarda | El back (sin cambios) | El artículo guardado | El dato que venía escrito | Lo no marcado viaja vacío y el back pone el del modelo (como siempre que un dato llega vacío) |

- [ ] **20.3 — sigue en el paso 4.** Pasos **1, 2 y 3 ✅** (2026-10-14). Faltan del 4 al 20. Lo que dijiste:

💬 *"listo 2 y 3, la 4 no la entiendo, explícalo mejor… de la 4 hacia adelante no la entiendo, descríbela más"* (2026-10-14)
↳ Hecho: los pasos **4 a 20** se reescribieron diciendo qué tocar, dónde y qué debe aparecer. El 4 y el 10 son solo pruebas de seguridad (escribir 0 o 20 a propósito para ver el aviso en rojo): si no las quieres hacer, se saltan.

💬 *"20.3 Modelo agotado cambie solo la descripcion y si dejo actualizar, ahi como dices no deberia? no sale el mensaje que mencionas"*
↳ Está bien así: **debía guardar sin mensaje**. El texto de antes decía "Guarda sin el aviso *El stock no
debe de ser 0*" y se leía como si tuviera que salir ese aviso. El aviso era el error de antes (un modelo
agotado no se podía editar); ahora ya no sale. El paso 1 de arriba lo dice así: *"No sale ningún aviso de error"*.

💬 *"Error al actualizar Stock insuficiente para el producto 'QA Prueba 20 Agotado' (id=20677). Disponible: 0, Solicitado: 5, cuando actualizo de actualizar stock da este error tienda modelo buscar el producto agotado"*
↳ No es una falla. Lo hiciste desde **Tienda → Buscar → ✏️ Editar del artículo**, y ese "Actualizar stock" toma
piezas **libres del modelo**: con el modelo en 0, no hay de dónde. El paso 2 de la prueba vieja era el
"Actualizar stock" **del modelo** (Catálogo → 🔍 Modelos → ✏️ Actualizar), pero no lo decía. Se arregló de dos
formas: (1) la prueba ahora dice exactamente en qué pantalla va cada uno y prueba los dos (pasos 2 a 4), y
(2) el aviso ya no dice "Stock insuficiente para el producto… (id=…)". Ahora dice qué pasa y dónde se arregla
(paso 2). El aviso nuevo sale **después de subir** esta ronda.

💬 *"Agregar modelo nuevo (nombre QA Prueba 20 Modelo sin stock, marca Prueba QA) con stock 0 no dice donde ir, es decir no explica bien como hacer la prueba"*
↳ Tienes razón. El paso 5 de arriba ya dice el menú (**Catálogo → Agregar modelo**), cada campo con su valor y
el botón.

💬 *"si se guarda sin ningún error"* (paso 1) y *"Error al actualizar El modelo «QA Prueba 20 Agotado» no tiene piezas libres para darle a este artículo: tiene 0 libres y le pides 5 más… aquí se supone que debería darte la opción de actualizar el stock del producto base… debería abrir un modal en el cual debería decir quieres actualizar producto y el stock y ahí puedo poner 10 o 100 pero eso no quiere decir que el artículo… se le vayan a poner esos 10 o 100… y para el producto base… al dar guardar me debe salir cuántos artículos quiero hacer… con los check de que quiero actualizar, ejemplo la descripción o la imagen… la variante se va a hacer igual que lo que tiene el producto, todos sus campos, y solo diferente los check que cambie"* (2026-10-14)
↳ Hecho, tal cual. (1) En **editar artículo**, si el modelo no alcanza, al dar 💾 se abre **➕ Agregar stock al
modelo**: lo que escribas ahí se suma **solo al modelo**; el artículo sigue igual, y al cerrar le pones 1, 2… y
guardas (pasos 2–7). También hay un botón **➕ Agregar stock al modelo** en el recuadro de stock por si lo quieres
hacer antes. (2) Al **actualizar** un modelo con stock libre, te pregunta **cuántos artículos** quieres hacer (de 1
a lo libre) y te pone ese número de formularios (pasos 9–11); antes solo preguntaba al dar de alta. (3) Cada
artículo tiene sus checks **Foto, Descripción, Color, Marca, Contenido neto y Categoría**: lo que no marques queda
**igual al modelo**; al marcar uno aparece el campo ya escrito con el dato del modelo para que lo cambies (pasos
11–19). Y cada artículo se puede **plegar y desplegar** (pasos 15–17). Talla, presentación y stock son siempre de cada artículo. Los pasos que ya pasaste llevan ✅.

### 20.4 Mis pedidos al abrir

| # | Haz esto | Debes ver |
|---|---|---|
| 1 | Entra a **Mis pedidos** (admin) | Bajo el buscador: *"No se muestran los pedidos ya pagados y entregados ni los cancelados…"*. No sale ninguno pagado **y** entregado ni ningún cancelado (Prueba 22); sí los pagados que faltan entregar, los que deben y los Pendiente |
| 2 | **⚙️ Filtros** → Pago: **Pagado** + Entrega: **Entregado** | Ahora sí salen los terminados, y el aviso se va |
| 3 | **Quitar filtros** | Vuelve la vista de inicio con el aviso |
| 4 | Escribe el número de un pedido ya pagado y entregado | Sale |

- [x] **20.4 ✅ Pasó (2026-10-14).**
💬 *"20.4 Mis pedidos al abrir 1 listo, 2 listo, 3 listo, 4 listo"*

**❌ Está mal si…** el carrito vuelve a pedir datos ya guardados; Mis datos sale vacío después de
guardar; *"deshaz el grupo antes…"*; uno de los pedidos del grupo cambia y el otro no; Apartado se deja
elegir con dinero; un modelo agotado no se guarda; Mis pedidos abre con pedidos pagados y entregados.

---

## Prueba 16 — 🕓 Pendiente, cambiarlo a Apartado, Entregas por zona, íconos ⓘ y datos legales

**Para qué es:** lo que pediste el 2026-10-07: que el pedido que el cliente hace desde su cuenta
(**Pendiente**) se vea en los filtros, que se pueda pasar a Apartado (o Ir pagando) y que después
se vea como tal en todos lados, y el ícono ⓘ que explica cada opción.

### Mapa de impacto

| # | Se movió | Le pega a | Antes | Después |
|---|---|---|---|---|
| 16.1 | Filtro **🕓 Pendiente** (desde el 2026-10-13 en el bloque **Pago**) | **Pedidos → Mis pedidos → ⚙️ Filtros** | No existía: el pedido del cliente salía en "🛒 Contado" + "Falta pagar" | "🕓 Pendiente" muestra solo esos. **"🛒 Contado" ya no los incluye** (solo los que se cobraron completos) |
| 16.2 | (mismo) | Filtros guardados con "Contado" + "Falta pagar" | Daban los Pendientes | Dan 0: hay que cambiar a "🕓 Pendiente" |
| 16.3 | Etiqueta **🕓 Pendiente** en la card | **Mis pedidos** (card) | La card solo decía "Pendiente" abajo, sin forma de cobro arriba | Arriba, junto al número, "🕓 Pendiente" con contorno punteado |
| 16.4 | 🔁 **Cambiar forma de cobro** de un Pendiente → **Apartado** | Detalle del pedido, Mis pedidos, Créditos / Abonos, cancelación automática | Quedaba Apartado pero **por dentro seguía "Pendiente"**: el cancelador automático lo cancelaba igual a los 2 días de su fecha | Queda Apartado por dentro también: **ya no se cancela solo**; sale en Créditos / Abonos y en el filtro 📦 Apartado |
| 16.5 | 🔁 Pendiente → **Ir pagando** con adelanto | Detalle del pedido | Error *"es de tipo NORMAL y no tiene saldo que cobrar"* | Se registra el adelanto y queda Ir pagando |
| 16.6 | **Entregas por zona** trae todo lo que falta entregar | **Envíos → Entregas por zona** | Solo Pendientes y Apartados | También los **Ir pagando que no se ha llevado** y los **Pagados que faltan por entregar**. Siguen fuera: cancelados, entregados y ramos |
| 16.8 | Debajo de cada dato legal dice **de dónde sale** y **dónde se ve** | **Sistema → Negocio & Contactos → ⚖️ Datos legales del negocio** | Solo el nombre del campo y "Usa los mismos datos de tu constancia" (que para el domicilio era incorrecto) | Recuadro "📄 Antes de llenar…" y una línea por campo |
| 16.7 | Íconos **ⓘ** | Filtros de Mis pedidos (cada bloque) y título de Entregas por zona | No había | Al tocarlo sale qué significa cada opción. Solo lo ven el admin y los roles con **Ayuda contextual** |
| — | **Lo que NO cambia** | Cobrar desde la card, abonos, Venta directa, el carrito del cliente | — | **Lo mismo que antes** |

**Ojo, cambió el 2026-10-13 (Prueba 22):** 🕓 Pendiente ya no está en **Forma de cobro**, ahora es un
botón del bloque **Pago**. Los pasos de abajo ya lo dicen así.

### Antes de empezar

**🧪 Preparar prueba 16** (o ya lo hiciste con **Preparar todas**). Crea los 4 pedidos, todos en la zona
**"Zona Prueba QA"** (en vez de Tejupilco) y sin llevárselos:

| Pedido | Qué es | Número |
|---|---|---|
| **A** | 🕓 Pendiente · cliente «Prueba 16 A» (con cuenta) | |
| **B** | 🕓 Pendiente · cliente «Prueba 16 B» | |
| **C** | 💳 Ir pagando con $50, todavía no se lo lleva · cliente «Prueba 16 C» | |
| **D** | 🛒 Contado, todavía no se lo lleva · cliente «Prueba 16 D» | |

| # | Haz esto | Debes ver |
|---|---|---|
| 1 | **Mis pedidos** → escribe **Prueba 16 A** | La card dice arriba **🕓 Pendiente** (contorno punteado) |
| 2 | **⚙️ Filtros → Pago → 🕓 Pendiente** | Salen A y B. No salen C ni D |
| 3 | Quita ese filtro y marca **🛒 Contado** | Sale D (cobrado de contado). **No** salen A ni B |
| 4 | Toca la **ⓘ** junto a "Forma de cobro" | Se abre un recuadro con qué significa Contado, Apartado e Ir pagando (lo de 🕓 Pendiente está en la ⓘ de **Pago**). Se cierra con ✕, con Esc o tocando afuera. Prueba las ⓘ de Pago, Entrega, Dinero, Fecha de entrega, Dónde se entrega y Unidos y otros |
| 5 | Abre A → **🔁 Cambiar forma de cobro** | Dice "Ahora está como **🕓 Pendiente (lo pidió el cliente desde su cuenta, sin cobrar)**" |
| 6 | Elige **Apartado** → guarda | "Quedó como Apartado". La card ya dice **📦 Apartado**; sale en el filtro 📦 Apartado y en **Créditos / Abonos**; **ya no** sale en 🕓 Pendiente |
| 7 | Abre B → 🔁 → **Ir pagando**, en "¿Cobra algo ahora?" pon **$100** → guarda | Queda **💳 Ir pagando** con $100 abonados y "Falta $(total − 100)" |
| 8 | **🚚 Envíos → 📦 Entregas por zona** → **Zona Prueba QA** (o Tejupilco si los hiciste a mano), rango que incluya hoy | Salen **A, B, C y D** |
| 9 | Toca la **ⓘ** del título "📦 Entregas por zona" | Explica qué pedidos salen, el rango de fechas, qué hace "Programar" y el aviso de los 🕓 Pendientes |
| 9b | **Sistema → Negocio & Contactos** → baja a **⚖️ Datos legales del negocio** | Arriba el recuadro "📄 Antes de llenar, ten a la mano tu Constancia…" con el aviso de no copiar el domicilio de la constancia de hoy; debajo de cada campo "De dónde" y "Se ve en". Se lee bien de día y de noche |
| 10 | Quita **Ayuda contextual** a un rol (Sistema → Gestión de roles), entra con un usuario de ese rol | No ve ninguna ⓘ (ni el "?" de la pantalla) |

**❌ Está mal si…** al día siguiente de la fecha + 2 días el pedido A (ya Apartado) aparece cancelado;
"🛒 Contado" trae pedidos que nadie ha cobrado; la ⓘ tapa los filtros en el celular sin poder cerrarla.

💬 Notas:

- [ ] **Prueba 16 terminada**

---

## Prueba 25 — Mis pedidos: buscar y poner o quitar filtros ya no tarda (2026-10-14) 🆕

**Para qué es:** tu nota del 2026-10-14: *"hay que revisar los filtros o las búsquedas de los pedidos porque
tarda mucho para usar un filtro o quitarlo, no sé si lo estás paginando o qué está pasando"*.

**Qué pasaba (medido, no supuesto):** la pantalla sí pagina (pide de 10 en 10), espera 400 ms después de la
última tecla y descarta respuestas viejas. El problema estaba en el servidor: **cuando hay texto en el
buscador, cada pedido de la lista revisaba por su cuenta todos sus artículos y los de su grupo**. Con 6 000
pedidos de prueba, buscar un nombre tardaba **48 segundos**, y como la pantalla vuelve a buscar con **cada**
filtro que prendes o quitas, cada clic con texto escrito esperaba eso (justo lo que hacíamos en las pruebas 21
y 22: escribir «Prueba 2x» y luego tocar filtros). Sin texto, los filtros tardaban ~0.1 s con ese volumen.
Ahora el servidor calcula una sola vez qué pedidos coinciden con el texto: el mismo caso tarda ~0.12 s.
**Los resultados son los mismos de antes** (se compararon 490 búsquedas contra el código anterior).

**Antes de empezar:** ✅ después de que yo suba el arreglo a QA y el deploy termine (te aviso); `Ctrl + Shift + R`.
No necesita botón 🧪: usa los pedidos que ya hay en QA (cuantos más haya, más se nota la diferencia). Ten a la
mano un cronómetro (el del celular): **mide desde que tocas hasta que salen las tarjetas**.

### Mapa de impacto

| # | Se movió | Le pega a | Dónde se ve | Antes | Después |
|---|---|---|---|---|---|
| 25.1 | La búsqueda por texto (`GET /v1/pedidos/buscar?buscar=…`) | **Mis pedidos** (administrador): buscador | Segundos que tarda en salir la lista | Segundos o minutos con muchos pedidos | Cerca de 1 segundo o menos |
| 25.2 | (mismo) con filtros puestos | **⚙️ Filtros** mientras hay texto en el buscador | Cada clic | Cada clic esperaba lo mismo que la búsqueda | Cada clic ~1 segundo o menos |
| 25.3 | (sin cambio) filtros sin texto | ⚙️ Filtros, **✕ Quitar filtros**, páginas ‹ › | Segundos | Ya eran rápidos | **Lo mismo que antes** |
| 25.4 | (sin cambio) qué pedidos salen | Todos los de arriba | La lista | — | **Los mismos pedidos, en el mismo orden y con el mismo total que antes** |
| 25.5 | (sin cambio) pedidos unidos | Buscar por el nombre de un cliente que está **dentro** de un grupo | La tarjeta del titular | El titular sale | **Lo mismo** (sigue saliendo el titular, no el miembro) |

### 25.1 Qué tan rápido responde

| # | Haz esto | Debes ver |
|---|---|---|
| 1 | Entra a **Mis pedidos** y mide hasta que salgan las tarjetas | Menos de 2 segundos |
| 2 | En el buscador escribe **Prueba** (3 letras o más) y espera | Salen las tarjetas en 1 o 2 segundos, no 10 ni 30 |
| 3 | Con ese texto puesto: **⚙️ Filtros** → toca **🕓 Pendiente**, luego quítalo | Cada clic responde en 1 o 2 segundos; el spinner se quita solo |
| 4 | Con el texto puesto: toca **💰 Falta pagar**, luego **📦 Apartado**, luego quita los dos | Igual: cada clic en 1 o 2 segundos |
| 5 | Borra el texto y prende y quita **💳 Ir pagando**, **✅ Pagado** y **❌ Cancelado** | Cada cambio en 1 segundo o menos |
| 6 | **✕ Quitar filtros** | Vuelve la vista de inicio (con el aviso de pagados y entregados) en 1 o 2 segundos |
| 7 | Escribe una palabra del nombre de un artículo (por ejemplo **bolsa**) | Sale en 1 o 2 segundos |
| 8 | Escribe el número de un pedido que conozcas | Sale ese pedido al instante |

Si algún paso tarda **más de 3 segundos**, escribe en 💬 qué hiciste, cuántos pedidos crees que hay y cuánto
tardó (aproximado). Con eso sé si falta otra mejora.

### 25.2 Que sigan saliendo los mismos pedidos

| # | Haz esto | Debes ver |
|---|---|---|
| 1 | Escribe **Prueba 22** (si ya no tienes la 🧪 22 preparada, tócala) | Salen PE y AP (CA solo con el filtro ❌ Cancelado), igual que cuando hiciste la Prueba 22 |
| 2 | Escribe **Prueba 21 P1** | Sale solo P1 (el nombre que se escribió en la venta) |
| 3 | Escribe el nombre de un cliente que esté en un grupo de pedidos unidos (por ejemplo **Prueba 24 K6** si los uniste) | Sale la tarjeta **del titular del grupo**, no la del miembro |
| 4 | Escribe el **teléfono** o una parte (4 o más números) de un cliente | Salen sus pedidos |

**❌ Está mal si…** algún paso tarda más de 3 segundos con pocos pedidos; el spinner se queda dando vueltas;
falta un pedido que antes salía (o sale uno que no tiene que ver con lo escrito); un cliente de un grupo ya no
encuentra a su titular.

💬 Notas:

💬 *"25.1 Qué tan rápido responde: ya está más rápido, pero una duda: ¿no se maneja caché aquí? Y quitarla cuando sea requerido según la venta; es decir, si es Ir pagando solo se limpia la caché de Ir pagando, que es la que se dio de alta, y así con los demás; y cuando se pague algo solo se limpia la caché según lo que sea. 25.2 Que sigan saliendo los mismos pedidos: ya quedó."* (2026-10-14)
↳ 25.2 pasó; 25.1 pasó con "más rápido". Sobre el caché (respuesta actualizada el 2026-10-14, después de tu otra nota *"al final me pones la caché para pedidos, se va a poder o no, porque sigue tardando al seleccionar un filtro o quitarlo"*): **sí se pudo y ya está hecho** (sube con esta ronda). Lo que **no** conviene es limpiar "solo la de Ir pagando": un abono pasa un pedido de Ir pagando a Pagado, cambiar la forma de cobro lo pasa de Pendiente a Apartado, y la búsqueda por texto cruza todas las formas de cobro. Si se limpiara solo una, otra lista se quedaría con el saldo viejo. Por eso el caché lleva una **versión**: cualquier movimiento (venta, abono, cancelar, entregar, unir, cambiar forma de cobro, editar artículos, cambiar datos del cliente) la sube, y la lista se vuelve a calcular. Mientras no se mueva nada, poner y quitar filtros sale del caché. Medido con 6 000 pedidos: la primera vez ~0.26 s, la segunda ~0.008 s. Además, los filtros sin texto quedaron ~2× más rápidos. Se prueba en la **Prueba 26**. Detalle: `hexagonal/busquedapedido/README.md`, R18 y R19.

💬 *"duda: en pedidos ¿no traes los pedidos por paginación? ¿o traes todos los pedidos de una vez?"* (2026-10-14)
↳ Sí se pagina: la pantalla pide **10 por página** y el servidor solo manda esas 10 tarjetas. Lo que sí recorre **todos** los que cumplen el filtro es el **conteo** ("N pedidos", para saber cuántas páginas hay) y el **orden** (para saber cuáles son los primeros 10). Por eso la mejora fue en esa consulta y en el caché, no en la página.

- [x] **Prueba 25 terminada** (2026-10-14)

---

## Prueba 26 — Mis pedidos con caché: rápido y sin datos viejos (2026-10-14) 🆕

**Para qué es:** tus notas del 2026-10-14: *"¿no se maneja caché aquí? … y cuando se pague algo solo se limpia
la caché según lo que sea"* y *"sigue tardando al seleccionar un filtro o quitarlo"*.

**Qué cambió:**
- La lista de Mis pedidos ahora se guarda un rato (caché).
- Si vuelves a pedir un filtro que ya viste y no se movió nada, sale **al instante** sin volver a calcularse.
- **Cualquier** movimiento (vender, abonar, cancelar, entregar, unir, cambiar forma de cobro, editar artículos o datos
  del cliente) hace que la lista se vuelva a calcular. No se limpia "solo la de Ir pagando", porque un abono cambia
  al pedido de filtro y otra lista se quedaría con el saldo viejo.
- Los filtros sin texto también quedaron ~2× más rápidos.

**Lo más importante de esta prueba no es la velocidad: es que nunca veas un dato viejo.**

**Antes de empezar:** ✅ después de que yo suba esta ronda a QA y el deploy termine (te aviso); `Ctrl + Shift + R`.
Luego **🧪 Preparar prueba 26**. Crea 4 pedidos en "Zona Prueba QA":

| Clave | Qué es · cliente |
|---|---|
| **IP** | 💳 Ir pagando de $150 con $50 dados (debe $100) · «Prueba 26 IP» |
| **PE** | 🕓 Pendiente de $150 · «Prueba 26 PE» |
| **PF** | ✅ Pagado de contado ($150) que **falta entregar** · «Prueba 26 PF» |
| **AP** | 📦 Apartado de $150 sin dinero · «Prueba 26 AP» |

Si ya habías preparado la 26 antes, con «Prueba 26» también salen los de la vez anterior con **❌ Cancelado**: no
les hagas caso.

### Mapa de impacto

| # | Se movió | Le pega a | Dónde se ve | Antes | Después |
|---|---|---|---|---|---|
| 26.1 | La lista se guarda en caché | **Mis pedidos** (administrador): ⚙️ Filtros, buscador, páginas | Cuánto tarda | Cada clic se calculaba todo de nuevo | Un filtro ya visto sale al instante |
| 26.2 | Un abono sube la versión | 💳 Dar abono (tarjeta) y 💳 Registrar abono (detalle) | La tarjeta | Pagado / Falta al momento | **Lo mismo que antes**: al momento, sin `Ctrl + Shift + R` |
| 26.3 | (mismo) cambiar forma de cobro | 🔁 Cambiar forma de cobro (detalle) | La tarjeta | Al momento | **Lo mismo** |
| 26.4 | 📦 Entregar avisa al caché (el único que guarda sin pasar por la ruta normal) | 📦 Entregar (tarjeta) | La tarjeta | Al momento | **Lo mismo** |
| 26.5 | (mismo) cancelar | Cancelar (tarjeta) | La tarjeta | Al momento | **Lo mismo** |
| 26.6 | La versión es una sola para todos | Dos pestañas o dos personas | La otra pestaña | Al buscar otra vez | **Lo mismo** |
| 26.7 | Guardar los filtros **no** borra el caché | Mis pedidos: salir y volver a entrar | Los filtros que dejaste | Se quedaban | **Lo mismo** |
| 26.8 | Mensaje de stock del artículo (BUG-KEY-41) | Tienda → Buscar → ✏️ Editar → Actualizar stock | El aviso | *"Stock insuficiente para el producto…"* | Dice qué pasa y dónde se arregla (se prueba en la 20.3, paso 2) |

### 26.1 Rápido al poner y quitar filtros

| # | Haz esto | Debes ver |
|---|---|---|
| 1 | **Mis pedidos** → en el buscador escribe **Prueba 26** | Las 4 tarjetas: IP, PE, PF y AP |
| 2 | **⚙️ Filtros** → toca **💳 Ir pagando** | Solo IP |
| 3 | Quita **💳 Ir pagando** | Otra vez las 4 |
| 4 | Vuelve a tocar **💳 Ir pagando**, quítalo, y otra vez | Desde la segunda vez, **al instante** (esa combinación ya se había calculado) |
| 5 | Borra el texto. Prende y quita **✅ Pagado**, luego **📦 Apartado**, dos veces cada uno | La primera vez de cada uno, rápido (1 s o menos); la segunda, al instante |
| 6 | Pasa a la página **Siguiente →** y regresa con **← Anterior** | La página que ya viste regresa al instante |

### 26.2 a 26.5 — Cada movimiento se ve en ese momento

Hazlos **con «Prueba 26» escrito** en el buscador. Después de cada uno, **no** recargues la página.

| # | Haz esto | Debes ver |
|---|---|---|
| 1 | Tarjeta de **IP** → **💳 Dar abono** → Monto del abono **30**, efectivo → **💳 Registrar abono** | La tarjeta de IP ya dice **$80 pagado y falta $70**, sin recargar |
| 2 | Tarjeta de **PE** → **👁 Detalle** → **🔁 Cambiar forma de cobro** → **Apartado** → **Guardar cambio** → **← Volver** | La tarjeta de PE ya dice **📦 Apartado** (ya no 🕓 Pendiente) |
| 3 | Tarjeta de **PF** → **📦 Entregar** → **Sí, entregado** | La tarjeta de PF ya dice **Entregado** y el botón 📦 Entregar ya no sale |
| 4 | Tarjeta de **AP** → **Cancelar** → elige cualquier motivo → **Cancelar pedido** | La tarjeta de AP ya dice **❌ Cancelado** |
| 5 | Borra el texto (vista de inicio) | **AP no sale** (los cancelados no salen al abrir); **PF tampoco** (ya está pagado y entregado). IP y PE sí |

### 26.6 Dos pestañas

| # | Haz esto | Debes ver |
|---|---|---|
| 1 | Abre **otra pestaña** con **Mis pedidos** y escribe **Prueba 26 IP** en las dos | En las dos: IP con $80 pagado |
| 2 | En la pestaña 2: **💳 Dar abono** → **Liquidar todo ($70.00)** → **💳 Registrar abono** (si pregunta si ya se lo llevó, contesta lo que quieras) | En la pestaña 2, IP **pagado** |
| 3 | En la pestaña 1 **no** recargues: toca **⚙️ Filtros → 💳 Ir pagando** y quítalo | En la pestaña 1, IP ya sale **pagado** (no se quedó con $80) |

### 26.7 Los filtros guardados siguen igual

| # | Haz esto | Debes ver |
|---|---|---|
| 1 | Deja puesto **⚙️ Filtros → 📦 Apartado**, sal a otra pantalla y vuelve a **Mis pedidos** | El filtro sigue puesto, como antes |
| 2 | **✕ Quitar filtros** | La vista de inicio |

**❌ Está mal si…**
- cualquier tarjeta se queda con el dato de antes después de un movimiento (y hay que recargar para verlo);
- en la pestaña 1 IP sigue con $80 después del paso 26.6;
- un filtro que ya viste tarda lo mismo que la primera vez;
- sale un error o el spinner se queda dando vueltas;
- se pierden los filtros guardados.

💬 Notas:

💬 *"esta prueba sí está haciendo lo que dice en la prueba"* (2026-10-14; antes había escrito que no veía los datos del 🧪 y lo canceló)
↳ ✅ Pasó.

- [x] **Prueba 26 terminada** (2026-10-14)

---

## Prueba 6 — Datos de prueba con un botón (la última)

**Para qué es:** crear de un jalón miles de modelos, artículos y pedidos de prueba para no darlos de
alta a mano. Todo queda marcado como **"Prueba QA"** para distinguirlo de lo real.

**Antes de empezar:** termina **todas** las demás de este documento. Mientras esté generando, **no me pidas subir nada a
QA** (subir reinicia el servidor y corta la corrida).

| # | Haz esto | Debes ver |
|---|---|---|
| 1 | **Sistema → 🗑️ Limpiar caché** | Arriba la tarjeta de siempre (**Gestión de Caché**) y abajo **🧪 Datos de prueba** (la misma de los botones **🧪 Preparar prueba**: aquí se usa la parte de arriba, **🧪 Generar datos de prueba**) |
| 2 | Toca **🗑️ Limpiar toda la caché → Sí, limpiar** | *"¡N cachés limpiadas!"*, igual que siempre |
| 3 | En **🧪 Datos de prueba** deja los valores (Modelos **20000**, Artículos **1** a **4**, Pedidos **1000**) y toca **🧪 Generar datos de prueba → Sí, generar** | El botón dice **Generando…** y aparece una barra: *"Creando modelos y artículos · N%"* |
| 4 | Sal de la pantalla y vuelve a entrar | La barra sigue avanzando |
| 5 | Espera a que diga **Terminado · 100%** (unos minutos) | *Modelos: 20000 de 20000* y *Pedidos: 1000 de 1000*. Primero suben los modelos y **después** los pedidos |
| 6 | Toca otra vez **🗑️ Limpiar toda la caché → Sí, limpiar** | Para que la tienda muestre lo nuevo de inmediato |
| 7 | **Tienda** → busca **QA-1** | Artículos de prueba con foto (la foto es de otro producto real: es normal) |
| 8 | **Catálogo → 🔍 Modelos** → busca **QA-** | Modelos con marca **Prueba QA**, de 1 a 4 artículos cada uno |
| 9 | **Mis pedidos** → busca **Cliente Prueba QA** (también puedes filtrar por estado con ⚙️ Filtros) | Pedidos **Entregado**, **Apartado**, **Ir pagando** y **Pagado** |
| 10 | **💳 Créditos / Abonos** → **📋 Cuentas por cobrar** | Apartados e Ir pagando de "Cliente Prueba QA" |
| 11 | **Reportes** | Ventas de contado y de los pedidos que se terminaron de pagar |
| 12 | Regresa a **🗑️ Limpiar caché** → **Dar de baja los datos de prueba → Sí, dar de baja** | *"N artículos de prueba dados de baja (y sus modelos)"* |
| 13 | **Tienda** → busca **QA-1** otra vez (limpia la caché si todavía salen) | Ya no salen. Tus productos reales siguen igual. Los pedidos de prueba siguen en Mis pedidos como historial |

❌ **Está mal si:** la barra dice *"Se detuvo: …"* o *"con error: N"*. Copia el **Último error** en tus notas.

💬 Notas:

- [ ] **Prueba 6 terminada**

---

## Lo que sigue al terminar estas pruebas (acordado el 2026-10-13)

**Orden:** primero se terminan las pruebas de este documento → QA pasa a **prod** con lo que hay hoy →
**después** entra el cambio de "variante" a "artículo" a `dev` y `qa`. Así a prod no se lleva el
renombrado sin probar; solo lo que ya está en QA.

### Paso 1 — QA a prod (cuando termines las pruebas de este documento)

Lista completa y orden: `CLAUDE.md` → *"Pendiente para prod"* (8 scripts, se corren en `inventario_key`
**antes** del deploy del back) y `PASOS_SUBIDA_2026-10-07.md` Parte 2. Del 2026-10-13 hay **un** script
nuevo, solo de texto: `migration_accion_editar_entrega_direccion.sql` (el 8 de la lista). Los botones
**🧪 Preparar prueba** también llegan a prod, pero ahí siempre se niegan (solo funcionan en la base de QA). Después de los scripts de permisos, volver
a entrar.

### Paso 2 — "Variante" → "artículo" en QA (después de prod)

Revisado el 2026-10-13. Vive en la rama `rename/variante-a-articulo` (back y front); detalle completo en
`RENOMBRE_VARIANTE_A_ARTICULO.md` (en la rama).

**Lo único que hay que correr en la base:** `migration_renombre_articulo_etiquetas.sql`. Cambia **solo
textos** que ve el admin (menú *"🧩 Agregar producto"* → **"🧩 Agregar artículo"**, *"🎡 Rifa de
productos"* → **"🎡 Rifa de artículos"**, y las etiquetas de Gestión de roles que dicen "variante"). No
toca claves, rutas ni permisos: nadie gana ni pierde acceso y **no hay que volver a entrar**, basta
recargar. Se volvió a probar el 2026-10-13 en una base desechable con los textos que dejan las
migraciones, con "safe updates" prendido como en MySQL Workbench: la primera corrida cambió 33 textos,
la segunda ninguno, y las consultas de verificación salieron bien.

**Orden en QA:**
1. Traer a la rama lo de `dev` (hoy le faltan 5 cambios del back y 3 del front). Probado en una copia
   aparte: en el back chocan 6 archivos (`VarianteController`, `ConfigurarRifaVarianteController`,
   `VarianteServiceImpl`, `SecurityConfig` y dos documentos); en el front solo `TESTS_PENDIENTES.md`. Se
   resuelven con la lógica de `dev` y el texto de la rama, y se compila antes de subir.
2. **Back** a `dev` → `qa` (agrega `/v2/articulos` sin quitar `/v1/variantes`: el front de QA sigue igual).
3. **Front** a `dev` → `qa` (ya llama `/v2/articulos` y dice "artículo"). Nunca antes que el back: daría 404.
4. Correr `migration_renombre_articulo_etiquetas.sql` en `inventario_key_qa`. Al final trae 4 consultas:
   las 3 primeras tienen que salir **vacías** y la última tiene que devolver **Agregar artículo** y
   **Rifa de artículos**.
5. Pruebas R1–R19 de `RENOMBRE_VARIANTE_A_ARTICULO.md` §6 (con F12 → Red y el filtro `articulos`: todo
   200, nunca 401, 403 ni 404).
6. A prod igual: back, front y el mismo script en `inventario_key`.

## Lo que todavía no se prueba

- **El cambio de "variante" a "artículo"** (rutas `/v2/articulos`): rama aparte `rename/variante-a-articulo`. Entra a QA en el Paso 2 de arriba, después de pasar a prod.
- **"¿Viene bien o dañado?"** al cancelar o devolver y **registrar que el dinero ya se devolvió**: acordados, sin programar.
