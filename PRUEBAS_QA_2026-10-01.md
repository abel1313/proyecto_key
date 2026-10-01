# Pruebas en QA — redes, páginas legales y pedidos unidos (2026-10-01)

Junta en un solo lugar el plan de pruebas y el estado de cada tema que se armaron el 2026-10-01.
Se sigue llenando aquí; lo que ya está en `PENDIENTES_2026-09-29.md` solo se referencia.

**Cómo leerlo:**
- `💬` = lo que escribió el dueño al probar, tal cual.
- `↳` = la respuesta o el cambio, debajo de cada comentario.
- Los nombres de botones y textos se sacaron del código del 2026-10-01 (el primer plan tenía
  nombres adivinados; ya se corrigieron).

---

## ▶ Pruebas pendientes — empezar aquí

Esta es **la lista de lo que te falta probar en QA** (actualizada 2026-10-01). Cada prueba tiene más
abajo su sección marcada con **🔴 PRUEBA PENDIENTE**, con:
- **Antes de empezar:** qué preparar (pedidos de prueba, sesión, etc.).
- **Pasos:** qué hacer, en orden, con los nombres reales de menús y botones.
- **✅ Debe pasar:** lo que tienes que ver.
- **⛔ No puede pasar:** lo que sería un error. Si ves algo de esto, anótalo con 💬 abajo del paso.

**Siempre, antes de probar:** entra a QA (`qa.shop.novedades-jade.com.mx`) y haz recarga forzada
(`Ctrl + Shift + R`) para no ver la versión vieja.

| Orden | Prueba | Estado | Qué revisa |
|---|---|---|---|
| 1 | **Prueba 7** — Apartado es sin dinero | 🔴 pendiente | Lo programado hoy: abonos en Apartado, cambiar a Ir pagando, grupos de Apartados, venta directa |
| 2 | **Prueba 8** — Botón Volver alineado | 🔴 pendiente | Que el botón quede en el mismo borde que la tarjeta en 13 pantallas |
| 3 | **Prueba 4** — Quitar, cambiar o agregar un artículo con abonos | 🔴 **reiniciar** (segunda vuelta, pedidos nuevos H4–H7) | La primera vuelta encontró un error al agregar; ya está corregido y se repite completa |
| 4 | **Prueba 5** — Cancelar un pedido con abonos | 🔴 pendiente | Stock y mensajes al cancelar Ir pagando y pedidos unidos |
| — | **Prueba 6** — Venta: "Falta entregarlo" | ⛔ todavía no se puede | No está programada; la prueba está escrita para cuando se haga |
| — | **Prueba 1** — Botón ← Regresar en páginas legales | ✅ hecha | — |
| — | **Prueba 2** — TikTok | ✅ hecha | — |
| — | **Prueba 3.7 y 3.7-bis** — Pedidos unidos | ✅ hechas | — |

## Estado de cada tema

| Tema | Dónde está | Estado |
|---|---|---|
| Botón ← Regresar en páginas legales | front `dev` y `qa` | ✅ probado (Prueba 1) |
| TikTok: conectar, quitar acceso, subir video | prod (con llaves de Sandbox) | ✅ probado |
| TikTok: revisión de la app de Production | developers.tiktok.com | ⏳ esperando respuesta (`PENDIENTES` E.2) |
| Pedidos unidos: saldos y abonos | back y front `dev`/`qa` | ✅ probado (3.7 y 3.7-bis) |
| Quitar / cambiar / agregar artículo con abonos | back y front `dev`/`qa` | 🔧 error al agregar corregido (BUG-KEY-14); 🔴 repetir Prueba 4 |
| Cancelar pedido con abonos | back y front `dev`/`qa` | ⏳ sin probar (Prueba 5) |
| Venta: lo que paga decide y "Falta entregarlo" | — | 🆕 por programar (Prueba 6) |
| Apartado es sin dinero | back y front `dev`/`qa` | 🔴 pendiente de probar (Prueba 7) |
| Botón Volver alineado con la tarjeta | front `dev` y `qa` | 🔴 pendiente de probar (Prueba 8) |
| "Saldo a favor" en el front | — | ⏳ falta hacerlo (`PENDIENTES` B) |
| Pagar Hosting-Mexico, orden 609155 | — | ⏳ antes del **4-oct-2026** (`PENDIENTES` C) |

---

## Prueba 1 — Botón ← Regresar en páginas legales

Páginas: `/privacidad`, `/eliminar-datos`, `/termConditions`. Son públicas (Meta y TikTok las abren
sin sesión), por eso el botón solo sale con sesión.

| Caso | Qué debe pasar |
|---|---|
| Sin sesión (ventana de incógnito), abrir cualquiera de las 3 | **No** sale el botón |
| Con sesión, entrar desde el pie de página (p. ej. desde Publicar en redes) | Sale **← Regresar** arriba del título |
| Clic en ← Regresar | Vuelve a la pantalla de la que se vino |
| Con sesión, abrir la página directo (pegando la URL) | Sale el botón y manda al inicio |
| Tema claro y tema oscuro | El botón se lee bien en los dos |

**Antes de probar:** recarga forzada (`Ctrl + Shift + R`, o en el celular cerrar la pestaña),
para no quedarse con la versión vieja guardada en el navegador.

### ✅ PRUEBA HECHA

Ya quedó probado (2026-10-01, captura del dueño): **con sesión, en `/privacidad` sale ← Regresar
arriba del título**. Falta lo demás.

**Pasos**
1. **Sin sesión:** abre una ventana de incógnito y entra a
   `qa.shop.novedades-jade.com.mx/privacidad`, luego `/eliminar-datos` y luego `/termConditions`.
   - ✅ Debe pasar: las 3 páginas se ven completas y **sin** el botón ← Regresar.
   - ⛔ No puede pasar: que salga el botón, o que te mande al login.
2. **Con sesión, desde una pantalla:** en tu sesión normal entra a **Marketing → Publicar en redes**,
   baja hasta el pie de página y da clic en **Eliminar mis datos**.
   - ✅ Debe pasar: sale **← Regresar** arriba del título; al darle clic regresas a Publicar en redes.
   - ⛔ No puede pasar: que regrese a otra pantalla, al inicio o a una página de fuera (TikTok, Meta).
3. Repite el paso 2 con **Términos y condiciones**.
4. **Con sesión, abriendo directo:** pega en la barra `qa.shop.novedades-jade.com.mx/termConditions`.
   - ✅ Debe pasar: sale el botón; al darle clic te manda al inicio de la tienda.
5. **Día y noche:** cambia el tema con el botón ☀️/🌙 del menú lateral y mira el botón en las 3 páginas.
   - ✅ Debe pasar: se lee bien en los dos.

- [x] Con sesión: aparece en `/privacidad`
- [x] Sin sesión: no aparece en `/privacidad`
- [ x] Sin sesión: no aparece en `/eliminar-datos` ni en `/termConditions`
- [ x] Con sesión: aparece en `/eliminar-datos` y `/termConditions` y regresa a la pantalla anterior
- [ x] Abierta directo: regresa al inicio
- [x ] Se ve bien de día y de noche

> 💬 *"No aparece el botón, pero si tengo sesión tampoco aparece"*
> 💬 *"Sigue igual, así hay que dejarlo"*

### 1.1 ↳ Por qué no salía, y qué se hizo

1. **El botón nunca había llegado a QA.** El componente existía, pero las 4 piezas que lo ponen en
   pantalla (las 3 páginas y `app.module.ts`) se quedaron sin subir en la sesión del 2026-09-30.
   Se subieron a `qa` el 2026-10-01.
2. **Por qué "sigue igual" después de subirlo:** se hicieron 2 pushes seguidos a `qa` del front y
   GitHub Actions corrió 2 deploys al mismo tiempo. El del commit **sin** el botón terminó 5 segundos
   después y pisó la imagen `front-jade-service:qa` (01:52:43 con botón → 01:52:48 sin botón).
   QA siguió sin botón.
3. **Arreglo:** se volvió a correr el deploy del último commit de `qa` (`e9eb447d`, run 524,
   intento 2); terminó bien a las 02:24 (UTC) del 2026-10-01, sin otro deploy encima. Y `dev`
   también quedó con las 4 piezas (antes solo las tenía `qa`).
4. El cambio a `AuthenticateService` (`sessionChange$`) que se subió esa misma noche **no era la
   causa**: el botón ya se recalculaba solo. Se deja porque no afecta nada.

**Para que no se repita:** no hacer dos push seguidos a `qa` del front; o agregar al workflow
`producto-actions-qa.yml` un `concurrency` que cancele el deploy viejo si llega uno nuevo
(propuesta, no hecha: es un cambio al CI/CD).

---

## Prueba 2 — TikTok ✅ cerrada

> 💬 *"Esto ya quedó listo, ya se hizo la prueba en prod y sí se quedó en notificación en el celular."*
> 💬 *"Se hizo la prueba de desconectar y conectar y funcionó, se hizo varias veces."*

↳ **Cerrada.** Probado en prod (con las llaves de Sandbox): **Conectar TikTok** → pantalla de
permisos → "Cuenta de TikTok conectada" con nombre y foto → **Quitar acceso** y volver a conectar,
varias veces → subir video con TikTok marcado → llega a TikTok → Bandeja de entrada →
Notificaciones del sistema.

- [x] Conectar / Quitar acceso / volver a conectar
- [x] Nombre y foto de la cuenta conectada
- [x] El video llega como notificación (no se publica solo)

Lo único que sigue abierto de TikTok es la respuesta de la revisión de Production
(`PENDIENTES_2026-09-29.md`, E.2: qué hacer si la aprueban o la rechazan).

---

## Prueba 3 — Pedidos unidos: saldos y abonos

### Cómo se ve hoy (sacado del código)

**Lista — Pedidos → Mis pedidos**
- De un grupo **solo sale la card del titular** (el que paga y recoge). Los demás se abren buscando
  su número.
- Card del grupo: "Unido con #…", **"Total de los N pedidos"**, abajo **"Este pedido: $x"**, y a la
  derecha el total del grupo. **"Falta $Y"** sale solo cuando ya hay al menos un abono y no está
  pagado. No tiene renglón de "Pagado".
- Card de un pedido suelto: **"Pagado $X"** y **"Falta $Y"** salen solo si es Apartado o Ir pagando,
  tiene al menos un abono y no está pagado.

**Detalle del titular (botón 👁 Detalle), de arriba a abajo**

| # | Qué hay | Botones |
|---|---|---|
| 1 | Encabezado: "Pedido #X", Apartado / Ir pagando, estado, **"Total de los N pedidos: $T"** y **"Este pedido: $x"** | 🖨️ Imprimir ticket · 📧 Reenviar ticket · 🔁 Cambiar forma de cobro · ➕ Agregar artículo |
| 2 | **"🔗 Unido en el grupo #G"**: quién paga y recoge, tabla *Pedido · Cliente · Estado · Total · Pagado · Saldo*, y totales *Total del grupo · Pagado · Saldo* | Cambiar quién recoge · 💵 Abonar al grupo · ➕ Agregar pedidos · ✂️ Separar pedidos |
| 3 | **"📋 Pagos registrados"** — solo los de **este** pedido | 💳 Registrar abono |
| 4 | Artículos de **este** pedido | en cada uno: **−** (quitar uno) y **⇄** (cambiar por otro) |
| 5 | **"🔗 N pedidos unidos — Este y N más · total $T"**: un renglón por cada otro pedido con su cliente, número de artículos y su total; al tocarlo se ven sus artículos | Abrir pedido #Y · y en cada artículo **−** y **⇄** |

### 3.1

> 💬 *"No veo estos nuevos (Pagado / Falta) en el pedido 1 ni en el 2."*

↳ El plan anterior estaba mal: decía que cada card muestra Debe / Pagado / Falta. Lo real está
arriba: unidos, solo sale la card del titular y el "Falta" aparece hasta que hay un abono. Lo
pagado y el saldo de **cada** pedido se ve en la tabla del bloque "🔗 Unido en el grupo" dentro del
Detalle. Con $0 abonado, que no salga nada en la card es lo esperado.

### 3.2

> 💬 *"Cuando se hace el pedido y se pone en Apartado o Ir pagando, ¿los pagos solo se hacen en
> abonos? Esta es una duda que no me has respondido."*

↳ **Sí.** En Apartado e Ir pagando todo el dinero entra como abono; no hay otro cobro. Por dónde entra:

| Dónde | Botón / campo | Qué hace |
|---|---|---|
| Venta directa, al crear el pedido | **💰 Pago inicial (enganche)** | Se registra como el primer abono justo después de crear el pedido |
| Detalle del pedido | **💳 Registrar abono** → 💾 Guardar abono | Abono a ese pedido |
| Pantalla Créditos / Abonos | Registrar abono | Igual que el anterior |
| Detalle de un pedido unido | **💵 Abonar al grupo** | Un abono para todo el grupo (ver 3.4) |
| Detalle → 🔁 Cambiar forma de cobro (de contado a Apartado o Ir pagando) | **¿Cobra algo ahora?** | Lo que se escriba queda como abono, con la nota de "¿Por qué cambia?" |

- El botón **Cobrar** de la card **no cobra** un Apartado / Ir pagando: avisa *"Este pedido se cobra
  registrando un abono, no desde este botón"* y ofrece **Ir a Créditos / Abonos**.
- Cuando lo abonado cubre el total, el pedido pasa solo a **PAGADO** y se crea su venta.
- **Apartado** = la mercancía se queda en la tienda hasta que termine de pagar.
  **Ir pagando** = el cliente se la lleva y va pagando.

### 3.3

> 💬 *"Buscar botón 'Agregar abono' o 'Registrar pago' — aquí debes de poner los nombres correctos,
> no adivinando."*

↳ Corregido. Los nombres reales son **💵 Abonar al grupo** (en el bloque "🔗 Unido en el grupo") y
**💳 Registrar abono** (abajo de "📋 Pagos registrados", guarda con **💾 Guardar abono**). Ojo: el
formulario de "Abonar al grupo" también termina en un botón que dice **Registrar abono**, lo que
suma a la confusión de 3.4. Todo este documento se revisó contra el código.

### 3.4

> 💬 *"Hay 2 botones de abonar, eso no me gusta, tiene que haber solo 1 botón de abonar. El abonar
> no significa que se está abonando a un pedido en específico sino a todos los pedidos de ese
> detalle. Cuando se vayan a separar los pedidos, ahí es donde se pone cuánto es para cada pedido."*

↳ Qué hace cada uno **hoy**:

- **💵 Abonar al grupo** — un solo monto para todo el grupo. Por dentro se anota llenando **primero
  el pedido más viejo** (regla R6 de `grupopedido/README.md`), **no** en partes iguales (el plan
  anterior decía "$75 a cada uno": estaba mal).
  Ejemplo: A = $200 (el más viejo) y B = $150. Abono de $150 → A queda con $150 pagado y B con $0.
  Abono de $250 → A $200 (queda Pagado) y B $50.
- **💳 Registrar abono** — abona **solo al pedido abierto**. Es el botón de siempre de un pedido
  suelto; sigue saliendo cuando está unido, y por eso se ven dos.
- **✂️ Separar pedidos** — ahí se escribe cuánto de lo que dio el cliente se queda cada pedido
  (columnas *Tiene hoy · Se queda con · Queda debiendo*; tiene que sumar exacto). No importa a qué
  pedido se anotó cada abono antes: al separar se reparte como se diga. O sea, la regla que pides
  ya se cumple al separar; lo que sobra es el segundo botón.

✅ **Hecho el 2026-10-01** (ver 3.7): unido, solo queda **💵 Abonar al grupo**.

**Por decidir junto con ese cambio:**
- [ ] "📋 Pagos registrados" hoy muestra solo los abonos de este pedido (un abono al grupo puede
      salir partido entre pedidos). ¿Que muestre los pagos del grupo, cada uno una vez y completo?
- [ ] La columna "Pagado" por pedido en la tabla del grupo: ¿se queda (es lo que tendría cada uno si
      se separa sin mover nada) o se deja solo el pagado del grupo?

### 3.5

> 💬 *"El primer pedido tiene un botón que dice cambiar: al dar cambiar permite cambiar el producto,
> ¿sí sabe a qué pedido irse? Y cuando se da agregar, ¿sí se sabe a qué pedido se va a ir? En el
> pedido 2 también tiene para cambiar el artículo: ¿sabe que si elijo otro artículo se iría a ese
> pedido?"*

↳ **Sí, los dos saben a qué pedido van:**
- **⇄** (cambiar por otro): el artículo nuevo se queda en el **mismo pedido** del artículo que se
  cambia. En los artículos de otro pedido del grupo, el buscador lo dice en el título:
  *"Cambiar por otro artículo — pedido #Y"*. En los del pedido abierto dice solo
  *"Cambiar por otro artículo"*.
- **➕ Agregar artículo** (arriba): si el pedido está unido, primero pregunta
  **"¿A qué pedido lo agregas?"** con la lista *"Pedido #X (este)"* / *"Pedido #Y — cliente"*.
  Si solo hay un pedido donde se pueda agregar, va directo a ese.
- **➕ Agregar pedidos** (en el bloque del grupo) es otra cosa: suma pedidos completos al grupo.
- Cada pedido guarda sus artículos: al separar, cada artículo se va con su pedido.

### 3.6

> 💬 *"El segundo pedido dice que es un total de $369, pero en el primero no decía eso. Y en el
> segundo dice el total y también 'Abrir pedido', y el primer pedido no dice eso. ¿Ahí qué está
> faltando, qué está pasando?"*

↳ El "primer pedido" es **el que tienes abierto** (el titular, al que entraste con 👁 Detalle). Su
total está en el encabezado de hasta arriba (*"Este pedido: $x"*) y no tiene "Abrir pedido" porque
ya estás dentro. Los otros salen en la sección *"🔗 N pedidos unidos"*, con su total y
**Abrir pedido #Y** (para ver o cambiar sus datos de entrega). No es un error de cuentas, pero se
ve disparejo.

✅ **Hecho el 2026-10-01** (ver 3.7): el pedido abierto ya sale con la misma cabecera que los otros.

### 3.7 ✅ Pedidos unidos: abonar al grupo y separar — probada en QA el 2026-10-01

Resultado: **pasó** (las 5 casillas marcadas). Tus notas y lo que se hizo con cada una:

> 💬 *"Esta no la entiendo, no pusiste los pasos."* (sobre el párrafo de arriba de la prueba)

↳ Ese párrafo no era un paso, era la explicación de por qué esta prueba usa pedidos de **Ir
pagando** y no Apartados: desde hoy un Apartado es sin dinero (Prueba 7), así que un grupo de
Apartados no acepta abonos sueltos. Ya se quitó y los pasos de abajo dicen exactamente qué elegir.

> 💬 *"Hace falta especificar si es con Ir pagando, hay que ser muy específico, y si se dio enganche o no."*

↳ Corregido abajo en **"Antes de empezar"**: los dos pedidos son **Ir pagando** y **sin enganche**
(Pago inicial en $0), con el botón exacto que hay que tocar.

> 💬 *"Veo 2 botones, uno que dice Abonar al grupo y otro que dice Registrar abono, ¿cuál es la
> diferencia, por qué están 2 botones?"*

↳ Eran dos caminos para lo mismo: **💵 Abonar al grupo** (para todo el grupo) y **💳 Registrar
abono** (el de un pedido suelto, que seguía saliendo aunque el pedido estuviera unido). **Ya se
quitó el segundo** cuando el pedido está unido: queda solo **💵 Abonar al grupo**, y donde estaba
el otro sale la nota *"Este pedido está unido: los pagos se registran para todo el grupo con 💵
Abonar al grupo, en el bloque de arriba"*. El botón que guarda dentro del formulario también se
llama ahora **💵 Abonar al grupo** (antes decía "Registrar abono" y por eso parecían dos cosas).

> 💬 *"Sigo viendo diferencias que ya te había comentado en los pedidos: en unos pedidos tiene
> cosas de más, en otro no."*

↳ Es lo de 3.6: el pedido abierto no tenía la misma cabecera que los otros en *"🔗 N pedidos
unidos"*. **Ya se agregó**: ahora sale primero *"Pedido #X · paga y recoge · cliente · N artículos ·
$total"* con la leyenda *"Es el que tienes abierto (sus artículos están arriba)"*, igual que los
demás (que conservan *Abrir pedido #Y* porque sus artículos se despliegan ahí mismo).

> 💬 *"Sí pasó la prueba, solo que en el modal que da el resultado los textos están disparejos:
> unos están en medio, otros a la derecha, y el botón de OK en el centro; otros a la izquierda. Hay
> que homologarlos."*

↳ **Corregido.** El aviso de *"Abono registrado"* ahora tiene todos los renglones centrados y con
el mismo formato: *Abono*, *Pagado del grupo*, *Falta* y, si hubo, *Cambio para el cliente*. Se
quitó la lista *"Se repartió así: Pedido #…"* (alineada a la izquierda): el dinero es del grupo y
lo de cada pedido se decide al separar. Si con el abono se termina de pagar, el título dice
*"Grupo pagado"*.

> 💬 *"Hasta arriba del detalle dice total 2 pedidos igual al total 400 y abajo dice 'Este pedido
> 200', pero no se entiende a qué pedido se refiere. Es ambiguo, ¿no?"* — y en Mis pedidos:
> *"debe decir solo el total 2 pedidos 400 y nada más"*.

↳ **Sí era ambiguo. Corregido en los dos lugares:**
- **Card de Mis pedidos:** solo *"Total de los 2 pedidos"* con **$400** (y *"Falta $X"* cuando ya
  hay abonos). Se quitó *"Este pedido: $200"*.
- **Encabezado del detalle:** solo *"Total de los 2 pedidos: $400"*. Abajo, en chico, ya no dice
  *"Este pedido"*: dice *"Pagado $100 · falta $300"* (solo si ya hay abonos).
- **Tabla del bloque del grupo:** cada pedido muestra solo su **Total**; se quitaron las columnas
  *Pagado* y *Saldo* por pedido (regla ya decidida en la skill `reglas-pedidos`, sección 4). Lo
  pagado y el saldo van abajo, del grupo completo, con la nota *"Lo pagado es de todo el grupo.
  Cuánto se queda cada pedido se decide al ✂️ Separar pedidos"*.

> 💬 *"En la tabla, G1 (el más viejo) muestra Pagado $100 y G2 $0 — es correcto esto."*

↳ Era correcto por dentro (el abono llena primero el pedido más viejo), pero por la regla de arriba
ya no se enseña por pedido: en la tabla solo verás los totales y abajo *Pagado $100 · Saldo $300*.

> 💬 *"[Abonar $500] es correcto, solo que lo correcto sería que abajo del monto se hiciera el
> cálculo o apareciera el aviso, porque estaría llenando más campos, más chamba, y al final no se
> puede, ¿entiendes?"*

↳ **Sí. Corregido:** en cuanto escribes el monto, debajo del campo sale:
- si es más que el saldo: en rojo *"Es más de lo que se debe: el saldo del grupo es de $300.00"*,
  el campo se pone rojo y el botón **💵 Abonar al grupo** se queda gris (no deja seguir);
- si está bien: *"Quedaría debiendo $X"*.

Lo mismo en el abono de un pedido suelto (**💳 Registrar abono** → campo *Monto*): *"Es más de lo
que se debe: el saldo es de $X"* y **💾 Guardar abono** gris.

### 3.7-bis ✅ Lo que se cambió por tus notas de la 3.7 — probada en QA el 2026-10-01

**Antes de empezar — preparar 2 pedidos de Ir pagando sin enganche**
1. Menú **Ventas → Venta directa**.
2. Agrega **1 artículo**. Anota su precio (en el ejemplo, **$200**).
3. En la forma de cobro toca **💳 Ir pagando**.
4. En **💰 Pago inicial (enganche)** deja **$0** (no escribas nada).
5. Toca **💰 Cobrar**. Anota el número de pedido que sale: es **G1**.
6. Espera 1 minuto (así G1 queda como el más viejo) y repite los pasos 1 a 5 con **otro artículo**
   (en el ejemplo, también de **$200**). Ese es **G2**.
7. **Pedidos → Mis pedidos** → en el buscador escribe el número de **G1** → **👁 Detalle** →
   **🔗 Unir con otros pedidos** → busca el número de **G2** → márcalo → **Unir 2 pedidos**.

En los pasos de abajo, si tus precios no son de $200, la cuenta es: *total del grupo = precio de
G1 + precio de G2*.

**Paso 1 — Un solo botón de abonar (detalle de G1)**
- ✅ Debe pasar: en el bloque *"🔗 Unido en el grupo #…"* está **💵 Abonar al grupo**. Más abajo,
  donde antes estaba **💳 Registrar abono**, ahora solo hay una nota: *"Este pedido está unido: los
  pagos se registran para todo el grupo con 💵 Abonar al grupo, en el bloque de arriba."*
- ⛔ No puede pasar: que siga apareciendo **💳 Registrar abono** en G1.
- Abre también el detalle de **G2** (en la tabla del grupo, clic en **#G2 ↗**): tampoco debe tener
  **💳 Registrar abono**.

**Paso 2 — Encabezado y tabla sin "Este pedido" (detalle de G1, todavía sin abonos)**
- ✅ Debe pasar: arriba a la derecha dice solo **"Total de los 2 pedidos: $400.00"**, sin ningún
  renglón chico debajo.
- ✅ Debe pasar: la tabla del grupo tiene 4 columnas: *Pedido · Cliente · Estado · Total* (G1
  $200, G2 $200). Abajo: *Total del grupo $400 · Pagado $0 · Saldo $400*.
- ⛔ No puede pasar: que diga *"Este pedido: $200"* en cualquier parte, o que la tabla tenga
  columnas *Pagado* / *Saldo* por pedido.

**Paso 3 — Aviso del monto al escribir**
1. Toca **💵 Abonar al grupo**. En **Monto** escribe **500** (no toques nada más).
   - ✅ Debe pasar: sin salir del campo, debajo aparece en rojo *"Es más de lo que se debe: el
     saldo del grupo es de $400.00."*, el campo se marca rojo y el botón **💵 Abonar al grupo**
     de abajo está gris (no se puede tocar).
   - ⛔ No puede pasar: que tengas que llenar *Forma de pago* o *¿Con cuánto paga?* para enterarte.
2. Borra y escribe **100**.
   - ✅ Debe pasar: el aviso rojo desaparece y debajo dice *"Quedaría debiendo $300.00"*. El botón
     se activa.

**Paso 4 — Aviso del resultado centrado**
1. Con Monto **100**, *Forma de pago* **Efectivo**, *¿Con cuánto paga?* **200** → **💵 Abonar al grupo**.
   - ✅ Debe pasar: sale *"Abono registrado"* y debajo, **todo centrado**, 4 renglones:
     *Abono: **$100.00*** · *Pagado del grupo: $100.00* · *Falta: **$300.00*** ·
     *Cambio para el cliente: **$100.00***. El botón **OK** centrado.
   - ⛔ No puede pasar: textos a la izquierda y otros al centro, o la lista *"Se repartió así: Pedido #…"*.
2. Cierra el aviso.
   - ✅ Debe pasar: el encabezado ahora dice *"Total de los 2 pedidos: $400.00"* y debajo, chico,
     *"Pagado $100.00 · falta $300.00"*. Debajo de la tabla: *Pagado $100 · Saldo $300* y la nota
     *"Lo pagado es de todo el grupo. Cuánto se queda cada pedido se decide al ✂️ Separar pedidos."*

**Paso 5 — Card de Mis pedidos**
1. Regresa a **Pedidos → Mis pedidos** y busca **G1**.
   - ✅ Debe pasar: la card dice **"Total de los 2 pedidos"** con **$400.00** a la derecha y
     **"Falta $300.00"** debajo. Nada más.
   - ⛔ No puede pasar: que diga *"Este pedido: $200"*, o que salga la card de G2 suelta.

**Paso 6 — Los pedidos del grupo se ven iguales (detalle de G1, hasta abajo)**
- ✅ Debe pasar: en *"🔗 2 pedidos unidos"* salen **dos** renglones con el mismo formato:
  *"• Pedido #G1 · paga y recoge · cliente · 1 artículo · $200.00 — Es el que tienes abierto (sus
  artículos están arriba)"* y *"▸ Pedido #G2 · cliente · 1 artículo · $200.00 — Abrir pedido #G2"*.
- ⛔ No puede pasar: que G1 no salga en esa lista (era lo que se veía disparejo).

**Paso 7 — Mismo aviso en un pedido suelto**
1. Haz un pedido de Ir pagando sin enganche como en "Antes de empezar" (pasos 1 a 5), **sin unirlo**.
   Ábrelo: **👁 Detalle** → **💳 Registrar abono** → en **Monto** escribe más que su total.
   - ✅ Debe pasar: debajo del monto, en rojo, *"Es más de lo que se debe: el saldo es de $X."* y
     **💾 Guardar abono** en gris.
   - ⛔ No puede pasar: que deje guardar.

- [x ] Paso 1: un solo botón de abonar en G1 y en G2
- [x ] Paso 2: encabezado y tabla sin "Este pedido" ni Pagado/Saldo por pedido
- [x ] Paso 3: el aviso de monto sale al escribir y bloquea el botón
- [x ] Paso 4: aviso del resultado centrado, sin "Se repartió así"
- [x ] Paso 5: card solo con "Total de los 2 pedidos" y "Falta"
- [x ] Paso 6: G1 sale en "2 pedidos unidos" igual que G2
- [ x] Paso 7: mismo aviso en un pedido suelto

**Lo que sigue pendiente de 3.4 (no se hizo hoy):** el botón **"Detalle de los pagos"** que
liste cada pago del grupo una sola vez y completo (decidido, skill `reglas-pedidos` sección 4).
Necesita un cambio en el back para juntar los pagos de todos los pedidos del grupo.

### 3.8 Formas de cobro y unir pedidos — reglas del dueño (2026-10-01)

> 💬 *"Cuando se paga en carrito / tienda al contado y efectivo, el pedido ya se hizo completamente:
> fue al local y ya se lo di. Está el otro, que paga con tarjeta, de una o a meses (¿ya metiste que
> cada producto se pueda pagar a MSI?); ahí también el cliente estaba en el local. La otra, que
> ahorita no se implementa: el cliente paga el pedido desde su cuenta con tarjeta, ahí no se lo
> entregamos, el cliente pone a dónde y cuándo se lo vamos a llevar. Apartado e Ir pagando no se
> pueden pagar ni con tarjeta ni con MSI. Más adelante: si lo aparta y va al local a recogerlo y
> quiere pagar con tarjeta y tiene la opción de MSI, que lo pueda hacer; mínimo $300 para MSI, con
> las reglas que ya te dije. Ir pagando no tiene MSI aunque el pedido cumpla."*

↳ Anotado en `.claude/skills/reglas-pedidos/SKILL.md` (sección 2 y 3) y lo nuevo en
`PLAN_PEDIDOS_VENTAS_ENTREGA.md` §10. Sobre MSI por producto: **no está hecho todavía**. Está
decidido y en el plan (Bloque 6: casilla por producto y por artículo, mínimo $300, solo si todos los
artículos lo aceptan), pero sin programar. Hoy los meses se eligen al cobrar con tarjeta en la
terminal, para toda la venta de contado, sin mínimo.

> 💬 *"Y la duda que dices de que si paga a crédito, ahí no sé a qué te refieres."*

↳ Fue una palabra mal usada: en el código, Apartado e Ir pagando se llaman "crédito". Quería decir
**Apartado o Ir pagando**. Ya no se usa "a crédito" en lo que se te escriba (quedó como regla en la
skill).

> 💬 *"Quiero que inicies a hacer una skill para que siempre haga lo que diga, por ejemplo cosas
> repetidas, cómo se reparte, para que lo anotes, porque ya habíamos quedado cómo era."*

↳ Hecha: `.claude/skills/reglas-pedidos/SKILL.md` en el back. Se usa antes de tocar o contestar
cualquier cosa de pedidos, ventas, cobro, abonos o grupos. Trae: cómo trabajar (nombres reales,
tus palabras, no volver a preguntar, anotar cada regla nueva), formas de cobro, MSI, unir y separar
con el ejemplo de abajo, y lo que falta confirmar.

> 💬 *"Apartado e Ir pagando sin abonos: no se pueden unir porque no tienen la misma forma de pago.
> Se puede cambiar la forma de pago de algún pedido para que los 2 sean iguales."*

↳ ✅ Así funciona: el buscador de unir solo muestra pedidos con la misma forma de cobro. Para unirlos,
primero se cambia uno con **🔁 Cambiar forma de cobro** en su detalle y luego se unen. Ya unidos ya no
se puede cambiar la forma de cobro. (Elegir "pasar todos a … y unir" desde la pantalla de unir está
en el plan, 6.7, sin hacer.)

> 💬 *"Si es Apartado, cuando lo va a recoger es porque lo va a pagar por completo, porque si va a
> estar dando abonos eso sería Ir pagando. Entonces si unimos 2 pedidos [apartados], aquí no aplican
> abonos."*

↳ ❓ **Esto choca con lo anotado antes y con el código, falta que confirmes:**
- El 2026-09-29 quedó: *"Apartado: si deja anticipo, se anota (efectivo o transferencia)"*.
- Hoy el sistema deja dar **enganche** al crear un Apartado y **abonos** después (también "Abonar al
  grupo" en Apartados unidos).

¿Cuál queda? **(a)** Apartado sin anticipo ni abonos: se paga completo al recogerlo (habría que
quitar el enganche y los abonos en Apartado). **(b)** Apartado sí puede dejar un anticipo, y el resto
se paga completo al recogerlo. **(c)** Se queda como está. No se cambia nada hasta que lo digas.

> 💬 *"Ir pagando: ahí sí es ir dando abonos, de lo que sea, y no caducan. Dos pedidos de $200 ($400),
> da $100 para los 2 y ya debe $300. Al separar: 50 y 50 → cada uno debe $150; si los 100 se van a
> uno solo, uno debe $100 y el otro $200."*

↳ ✅ Así funciona (Separar pide cuánto se queda cada uno y tiene que sumar exacto). El ejemplo quedó
en la skill como el ejemplo acordado.

> 💬 *"Agregar producto, que pregunte a qué pedido irá de los que tenemos en ese detalle; y cambiar
> artículo, por el pedido debería saber que es para ese pedido, ¿no?"*

↳ ✅ Los dos ya funcionan así (ver 3.5): **➕ Agregar artículo** pregunta *"¿A qué pedido lo agregas?"*
y **⇄** deja el artículo nuevo en el mismo pedido del que se cambia.

### 3.9 Apartado es sin dinero — decidido (2026-10-01)

> 💬 *"Actualmente el cliente no puede pagar con tarjeta, solo apartado. Aquí me equivoqué: el
> apartado es porque lo piden por Face o live o por mensaje, y ahí no me ha dado nada, solo lo pide.
> Cuando hago el pedido, al cobrar el cliente me puede hacer una transferencia o mandar a la prima,
> tía, etc. a dejarme 100 pesos; entonces lo tendría que poner en Ir pagando porque ya me dio un
> enganche, y en Apartado no podría entrar, por eso hay que corregirlo. Si ya hizo un apartado y el
> cliente viene y dice 'te voy a dar un enganche de 100', al dar el abono sería cambiar el pedido a
> Ir pagando. Hay que anotarlo bien para que la próxima vez que te diga 'hace falta que en apartados
> dejes dar un abono', me expliques que quedamos así, y me digas que para dar un abono tengo que
> cambiar el pedido a Ir pagando. Y si en venta elijo Apartado y después pongo que me dio 100 por
> transferencia, que muestre un modal y no deje seguir, explicando esto, para entender que lo tengo
> que cambiar a Ir pagando."*

↳ Anotado como regla en la skill (`reglas-pedidos` 2.1), con la indicación de recordártela si un
día pides dejar abonar en un Apartado. En el plan quedó la lista de cambios (`PLAN` §10.6):
- **Venta:** Apartado + pago inicial → aviso que no deja seguir y botón **Cambiar a Ir pagando**.
- **Apartado ya hecho:** "💳 Registrar abono" avisa *"Para dar un abono, cambia el pedido a Ir
  pagando"* y lleva a **🔁 Cambiar forma de cobro**.
- **El back también lo rechaza**, para que no se cuele por ninguna pantalla.
- **Grupos de Apartados:** sin "Abonar al grupo".
- **Apartados que ya tienen dinero** en QA y prod: se buscan y se decide contigo (lo natural:
  pasarlos a Ir pagando).

**Todavía no está programado**: hoy el sistema sigue aceptando enganche y abonos en Apartado.

❓ **Falta una cosa:** con esta regla, el Apartado se paga **completo** al recogerlo, pero hoy un
Apartado solo se puede liquidar con abonos (el botón Cobrar lo manda a Créditos / Abonos).
Propuesta: que **Cobrar** en la card de un Apartado cobre el total de una vez (efectivo o
transferencia; tarjeta y MSI cuando existan) y lo deje Pagado. ¿Así?

> 💬 *"Otra cosa que no me explicaste: ¿los abonos solo se dan en Abonos como tal? Porque ahorita los
> abonos los dejaba dar en el detalle del pedido. Es una duda que no me has contestado."*

↳ Se pueden dar en los dos lugares y **es exactamente lo mismo**: "💳 Registrar abono" en el
detalle del pedido y la pantalla **Créditos / Abonos** guardan el abono igual (el mismo registro en el
back; también el "Pago inicial (enganche)" de la venta). Da igual dónde lo registres: sale en
"📋 Pagos registrados", cuenta para el saldo y para el corte del día. El detalle es solo un atajo para
no tener que ir a la otra pantalla.

### 3.10 Cobrar el Apartado completo — decidido (2026-10-01)

> 💬 *"Sí, me gusta. Que si detecta que pongo en abono, por ejemplo, si son 200 y llega y me da 200,
> pongo 200 en abono, entonces detecte que lo pagó completo de una y no mandar a abono. Y por ejemplo
> si me da 500, lo mismo: detecta que está completo y muestra lo que tengo que devolverle. ¿Alguna
> otra duda?"*

↳ Anotado (skill `reglas-pedidos` 2.1 y `PLAN` §10.6, A5 y A8):
- **Cobrar** en un Apartado cobra el total de una vez y lo deja pagado.
- Si en un Apartado se escribe el total **en "Registrar abono"**, no se anota como abono: se detecta
  que es el pago completo y se cobra igual que con Cobrar.
- Si da **de más** (debe $200, da $500), se cobra completo y muestra **"Devolver: $300"**.
- Si da **menos**, sale el aviso de cambiarlo a Ir pagando (3.9).

Hoy el sistema hace otra cosa: un abono mayor a lo que se debe **se rechaza** (*"El monto excede el
saldo pendiente"*), y el cambio solo se calcula contra el abono escrito.

**❓ Dudas que quedan:**
1. **Ir pagando también:** si debe $150 y da $500, ¿igual se liquida, queda pagado y muestra
   "Devolver: $350"? (hoy se rechaza). Recomendado: **sí**, misma regla.
2. **Transferencia de más:** si transfiere $500 y debe $200, no hay cambio en efectivo que dar en el
   momento. ¿Se rechaza y se le pide transferir lo exacto, o se acepta y le devuelves $300 por fuera?
   Recomendado: **rechazar** y pedir lo exacto, para que el corte cuadre.
3. **Cómo queda al cobrarlo:** si paga el Apartado al recogerlo, ¿queda **Entregado** (pagado y se lo
   llevó), como el contado? Recomendado: **Entregado**. Si paga pero se lo llevan después, sería el
   "Ya pagó, falta entregarlo" del plan (V3).
4. **Apartados unidos:** al recogerlos, ¿un solo Cobrar por el total del grupo, igual que un grupo de
   contado? Recomendado: **sí**.
5. **Apartados que hoy ya tienen dinero** (en QA y prod): ¿se pasan todos a Ir pagando? Recomendado:
   **sí**, con un script que primero te muestre la lista.

### 3.11 Respuestas a las 5 dudas (2026-10-01)

> 💬 *"Entonces hay que dejarlo como está, que solo se pueda pagar en abonos, ¿no? Y si me voy a
> Créditos y Abonos, ¿ahí sí lo puedo pagar todo? Y si me da uno de 500, ¿me daría cuánto le
> regresaría? ¿O qué pasa si me debe 100 y me quiere dar 20 pesos y me da uno de 500? Quiero ver
> cuánto le tengo que regresar."*

↳ **Sí, se deja como está** (reemplaza lo de 3.10 de que Cobrar cobrara todo). En Créditos /
Abonos, y también en "💳 Registrar abono" del detalle, **sí puedes pagar el total**: el pedido queda
pagado. El cambio ya lo calcula hoy el campo **"💵 Monto recibido"** (solo en efectivo):
- Debe $100 y lo liquida con uno de $500 → Monto $100, Monto recibido $500 → *Cambio a devolver $400*.
- Debe $100, quiere dar $20 y paga con uno de $500 → Monto $20, Monto recibido $500 →
  *Cambio a devolver $480*.

Lo que no deja es escribir un abono **mayor** a lo que debe, y así se queda: el abono es lo que
cuenta para la deuda, el monto recibido es el billete. Con la regla de Apartado (3.9), en un Apartado
solo se acepta el total; si da menos, el aviso de cambiarlo a Ir pagando.

> 💬 *"2. Si por transferencia me da 500, es que se equivocó, porque por transferencia me puede dar
> exacto. No voy a poner 'me mandó 500 por transferencia y le tengo que regresar 300'."*

↳ ✅ De acuerdo y ya funciona así: en transferencia no hay "Monto recibido" ni cambio, y un abono
mayor a la deuda se rechaza.

> 💬 *"3. Si lo paga por transferencia cuando estoy haciendo la venta y digo que me pagó todo,
> entonces el pedido se va como pagado pero falta entregarlo. Ahí hay que ver cómo le hacemos para
> saber que lo pagó pero aún no lo recoge; no sé cómo hacerle, tú dime."* — y al final: *"cuando
> esté haciendo la venta, si detecta que es Apartado pero da el total, que aparezcan las opciones de
> pago… puede que tenga 2 botones, de pagado y entregado; si hizo la transferencia y pongo el total,
> por default se detecta pagado y solo queda el botón de si ya se entregó o no. Aquí no lo tengo
> claro aún."*

↳ **Propuesta** (skill 2.3, `PLAN` §10.6 A9):
1. **Lo que paga decide la forma de cobro**, no tienes que acertarle al botón:
   $0 → **Apartado** · menos del total → **Ir pagando** · el total → **pagado**.
2. Si pagó el total, queda una sola pregunta obligatoria, **sin ninguna marcada de inicio** (para que
   no se te pase): **"Ya se lo llevó"** → queda Entregado, como hoy · **"Falta entregarlo"** → queda
   *Pagado, falta entregar*, y pones si pasa por él o se lo llevan, y qué día.
3. En la lista de pedidos esos salen como **"Pagado · falta entregar"**, y cuando lo recoja le das
   **Entregar** (ya no se cobra nada).

¿Así te sirve? Si sí, se junta con el "Ya pagó, falta entregarlo" que ya estaba en el plan (V3).

> 💬 *"4. Cuando están unidos como Apartados tienen que tener el mismo estatus. Cuando viene a
> recogerlo lo tiene que liquidar para terminarlo como pagado y entregarlo. Y si dice 'te voy a dar
> un adelanto nada más', tendría que cambiar el tipo de pago a Ir pagando para poder dar abonos, ¿no?"*

↳ ✅ Sí, exacto. Ojo: hoy dentro de un grupo **no se puede cambiar la forma de cobro**; habría que
separar, cambiar cada uno a Ir pagando y volver a unir. Quedó anotado hacer **"cambiar la forma de
cobro del grupo entero"** (`PLAN` §10.6 A10).

> 💬 *"5. Sí, hay que pasarlos a pagados para que no haya fallas, y ya en prod así lo dejamos y los
> vamos sacando como se vayan pudiendo, ¿no?"*

↳ Anotado (`PLAN` A7): en **QA** un script pasa a **Ir pagando** los Apartados que ya tienen dinero
(primero te muestra la lista); en **prod** se quedan como están y se resuelven uno por uno. Entendí
"pagados" como **Ir pagando**: marcarlos como *Pagado* diría que ya no deben nada, y sí deben.

### 3.12 "Falta entregarlo" — decidido (2026-10-01)

> 💬 *"Entonces, si pagó todo el pedido, ¿solo se habilita cuando me da el total? ¿Qué pasa si lo
> manda con una conocida pero la conocida no lo recoge, o lo mismo por transferencia? En los 2 casos
> solo pagó el pedido pero no lo ha recogido. ¿Cómo sabría el sistema que no lo ha recogido?"*

↳ Por el botón que eliges al guardar la venta: **📦 Falta entregarlo** deja el pedido como
**Pagado · falta entregar**, con su etiqueta, el botón **Entregar** y el filtro **"Por entregar"**.

> 💬 *"¿Entonces sacarás un botón más para elegir 'falta entregarlo'? ¿Y qué pasa si me equivoco y le
> doy 'falta entregarlo' y aún no lo ha pagado?"*

↳ No es un paso más: el botón de guardar se vuelve dos y eliges uno. Solo salen si escribiste que te
dio dinero. Si te equivocas, se corrige con 🔁 Cambiar forma de cobro, Entregar, o (solo admin)
regresarlo a "Falta entregarlo". Todo detallado en la **Prueba 6**.

> 💬 *"Perfecto, me parece perfecto, pero en las pruebas hay que describirlo detalladamente para
> cuando lo inicie sepa cuándo sacarlo o cuándo no."*

↳ Hecho: **Prueba 6**. Se tomó como aprobado también que en **Ir pagando** se pregunte
"¿Ya se lo llevó?" (era la pregunta pendiente de ese mensaje); si no era así, se quita.

### 3.13 Tabla del grupo, pagos y cancelar — decidido (2026-10-01)

> 💬 *"La 1 no la entiendo. 2: tabla del grupo, hay que dejar lo que dices, pero una opción que
> diga 'detalle de los pagos' cuando aplique en Ir pagando, ¿no? 3: sí hay que mencionar cuánto
> devolverle, y también recuerda que debe salir el modal de si el producto está correcto o está
> dañado, para no regresar el stock en caso de que esté dañado, ¿no?"*

↳ **1 y 2 son lo mismo, y tu "Detalle de los pagos" lo resuelve.** La duda 1 era: si el cliente
da $100 al grupo, por dentro se puede anotar $60 en un pedido y $40 en otro, y la lista de pagos
de cada pedido lo mostraba partido. Con tu botón **"Detalle de los pagos"** (solo en grupos de
Ir pagando) se ve cada pago **una sola vez y completo**: *"$100 · 01/10 · efectivo"*. La tabla
queda con el total de cada pedido y abajo el Pagado y el Saldo del grupo. (`PLAN` A14)

**3: sí, las dos cosas.** Al cancelar, desde la card o desde Créditos / Abonos:
- dice **cuánto devolverle** (*"Saldo a favor: $X"*);
- pregunta por cada producto **"¿Viene bien o dañado?"**: bien → regresa al stock; dañado → no
  regresa y queda la nota "regresó dañado".

Lo de bien / dañado ya estaba en el plan (D1–D3); quedó ligado a cancelar desde la card. (`PLAN` A15)

---

## Prueba 4 — Quitar, cambiar o agregar un artículo en un pedido con abonos

> 💬 *"ESTA REGLA NO LA ENTIENDO, SÉ MÁS ESPECÍFICO PORQUE NO ENTIENDO. No sé qué hacer pues, sé más
> específico de favor en cada prueba."* (Caso 1)

↳ Reescrita completa abajo. La regla, en palabras:

**Cuando quitas o cambias un artículo de un pedido de Ir pagando, el sistema vuelve a sumar cuánto
cuesta el pedido y lo compara con lo que el cliente ya te dio en ese pedido.**
- Si lo que ya te dio **cubre** el nuevo total (es igual o más) → el pedido queda **Pagado**: ya no
  debe nada. Si te dio de más, la diferencia es **saldo a favor** del cliente (esa leyenda todavía
  no sale en pantalla).
- Si lo que te dio **no alcanza** → el pedido queda (o regresa) a **Ir pagando**, debiendo la
  diferencia.
- Solo cuenta **ese** pedido; no se mezcla con otros pedidos del cliente.

Ejemplo con los números de esta prueba: pedido de 2 artículos de $100 = **$200**; el cliente dio
**$150** de enganche → debe **$50**. Quitas 1 artículo → el pedido ahora cuesta **$100**, pero el
cliente ya había dado $150 → **no debe nada, queda Pagado**, y le sobran **$50** (saldo a favor).

También se corrigió algo que encontramos al escribir esta prueba: al tocar **−** la pantalla
bajaba el total pero el estado de arriba no cambiaba hasta salir y volver a entrar. Ahora, en
Apartado e Ir pagando, el detalle se vuelve a cargar solo y el estado cambia en ese momento.

### 🔴 PRUEBA PENDIENTE — segunda vuelta (empezar de cero, 2026-10-01)

**Por qué se reinicia:** en la primera vuelta (más abajo, con tus notas) encontraste un error del
back: al **agregar** un artículo a un pedido ya Pagado, el total no lo sumaba y el pedido seguía
diciendo Pagado. Ya está corregido. Como los pedidos **H1, H2 y H3** de la primera vuelta quedaron
con cuentas tocadas por ese error, **no los uses**: se hacen pedidos nuevos (**H4 a H7**).

**Antes de empezar**
1. Confirma que ya se desplegó el arreglo: en GitHub → **Actions**, el último *"QA"* de
   `proyecto_key` y de `producto_venta_online` en verde (tarda 1–2 minutos después de subir).
2. Entra a QA y haz recarga forzada (`Ctrl + Shift + R`).
3. Busca en **Tienda** 4 artículos **de modelos distintos** (no la misma prenda en otra talla/color:
   el botón **−** busca la línea por modelo y con dos del mismo modelo podría quitar el que no es):

   | Nombre en esta prueba | Precio de ejemplo | Anota el tuyo |
   |---|---|---|
   | **Art-A** | $100 | |
   | **Art-B** | $100 | |
   | **Art-200** | $200 | |
   | **Art-300** | $300 | |

   Si tus precios son otros, haz las cuentas con los tuyos: la fórmula va en cada paso.
4. Crea 4 pedidos. Cada uno: **Ventas → Venta directa** → agrega los artículos → toca **💳 Ir
   pagando** → en **💰 Pago inicial (enganche)** escribe lo que dice la tabla → **💰 Cobrar** →
   anota el número de pedido.

   | Pedido | Artículos | Total | 💰 Pago inicial | Debe al crearlo | Número |
   |---|---|---|---|---|---|
   | **H4** | Art-A + Art-B | $200 | **$150** | $50 | |
   | **H5** | Art-A + Art-B | $200 | **$150** | $50 | |
   | **H6** | Art-A + Art-B | $200 | **$150** | $50 | |
   | **H7** | Art-A | $100 | **$0** (no escribas nada) | $100 | |

Para abrir cada uno: **Pedidos → Mis pedidos** → en el buscador escribe su número → **👁 Detalle**.
Los controles están en cada tarjeta de artículo: **−** (quita una pieza, sin preguntar) y **⇄**
(cambiarlo por otro). **➕ Agregar artículo** está arriba, junto al encabezado del pedido.

**Caso 1 — Quitar un artículo y que lo ya pagado alcance (H4)**
1. Abre **H4**. Arriba: **Ir pagando**, *"Debe: $50"* (Total $200 · pagado $150).
2. Toca **−** en la tarjeta de **Art-B**.
   - ✅ El detalle se recarga solo: **Total $100**, estado **Pagado**, ya no dice "Debe".
     Cuenta: $200 − $100 = $100; pagó $150 ≥ $100 → Pagado.
   - ✅ *📋 Pagos registrados* sigue en **$150** (lo que entró no cambia).
   - ✅ En **Tienda**, el stock de Art-B subió 1.
   - ✅ No sale ningún número de los $50 de más (el "saldo a favor" todavía no está hecho).
   - ⛔ Que siga diciendo "Debe", o que tengas que salir y volver a entrar para ver Pagado.

**Caso 2 — A un pedido Pagado, cambiarle el artículo por uno más caro (H4)**
1. En **H4** (Pagado, solo Art-A) toca **⇄** en Art-A.
2. En *"Cambiar por otro artículo"* escribe 3 letras de **Art-200** y elígelo.
   - ✅ Sale *"Artículo cambiado"* y el detalle se recarga: **Total $200**, **Ir pagando**,
     *"Debe: $50"*. Cuenta: $200 − $150 = $50.
   - ✅ Vuelve a salir **💳 Registrar abono** y ya **no** sale *"✅ Este pedido ya está pagado por
     completo"*.
   - ✅ En **Tienda**: Art-A +1, Art-200 −1.
   - ✅ En **Reportes**, la venta que se creó cuando H4 quedó Pagado ya no aparece.
   - ⛔ Que siga diciendo Pagado, o Total $100.

**Caso 3 — A un pedido Pagado, AGREGARLE un artículo (H5) — el error de la primera vuelta**
1. Abre **H5** y toca **−** en **Art-B** → igual que el caso 1: Total $100, **Pagado**.
2. Toca **➕ Agregar artículo**, escribe 3 letras de **Art-300** y elígelo.
   - ✅ Sale *"Artículo agregado"* y el detalle se recarga: arriba **Total $400** (Art-A $100 +
     Art-300 $300), **Ir pagando**, *"Debe: $250"*. Cuenta: $400 − $150 = $250.
   - ✅ *📋 Pagos registrados* sigue en **$150**.
   - ✅ Vuelve a salir **💳 Registrar abono**; **no** sale *"Este pedido ya está pagado por
     completo"*.
   - ✅ En **Reportes**, la venta de H5 ya no aparece (volvió a deber).
   - ⛔ **Total $100** arriba, o que siga **Pagado** — es exactamente lo que fallaba antes.
3. Toca **💳 Registrar abono**, escribe **$250** y guárdalo.
   - ✅ Queda **Pagado**, *"Pagos registrados"* suma **$400**, y en **Reportes** aparece la venta de
     H5 por **$400** (no por $100).

**Caso 4 — Quitar y luego cambiar en el mismo pedido (H6)**
1. Abre **H6**, toca **−** en **Art-B** → Total $100, **Pagado**.
2. Toca **⇄** en Art-A y cámbialo por **Art-300**.
   - ✅ **Total $300**, **Ir pagando**, *"Debe: $150"*. Cuenta: $300 − $150 = $150.

**Caso 5 — Quitar el único artículo (H7)**
1. Abre **H7** (solo Art-A, *"Debe: $100"*) y toca **−**.
   - ✅ No lo deja: *"'<nombre>' es el ultimo articulo del pedido #H7. Para regresar todo, cancela
     el pedido"*. El pedido queda igual.
   - ⛔ Que quede un pedido sin artículos.

- [ ] Caso 1: quitar → Total $100, Pagado al momento; pagos $150; stock +1
- [ ] Caso 2: Pagado + ⇄ a uno de $200 → Total $200, Ir pagando, debe $50; venta fuera de Reportes
- [ ] Caso 3: Pagado + ➕ uno de $300 → Total $400, Ir pagando, debe $250; al abonar $250 la venta es de $400
- [ ] Caso 4: quitar y ⇄ a uno de $300 → Total $300, debe $150
- [ ] Caso 5: no deja quitar el último artículo

### Primera vuelta (2026-10-01) — ya hecha, se deja como historial

Con tus notas. Los pedidos **H1, H2, H3** no se vuelven a usar (ver arriba por qué).

Los controles están en el **👁 Detalle** del pedido, en cada tarjeta de artículo:
**−** (quita una pieza, sin preguntar) y **⇄** (cambiarlo por otro artículo).

**Antes de empezar — elige 2 artículos del mismo precio y prepara 3 pedidos**

Busca en **Tienda** dos artículos que cuesten lo mismo (en el ejemplo, **$100** cada uno) y uno más
caro (en el ejemplo, **$200**) y otro de ~**$300**. Anota sus precios: si no son esos, haz las cuentas
con los tuyos (la fórmula va en cada paso).

Cada pedido se hace así: menú **Ventas → Venta directa** → agrega los artículos → toca **💳 Ir
pagando** → en **💰 Pago inicial (enganche)** escribe lo que se indica → **💰 Cobrar** → anota el
número de pedido.

| Pedido | Artículos | Total | 💰 Pago inicial (enganche) | Debe al crearlo |
|---|---|---|---|---|
| **H1** | 2 artículos de $100 | $200 | **$150** | $50 |
| **H2** | 2 artículos de $100 | $200 | **$150** | $50 |
| **H3** | 1 artículo de $100 | $100 | **$0** (no escribas nada) | $100 |

Para abrir cada uno: **Pedidos → Mis pedidos** → en el buscador escribe su número → **👁 Detalle**.

**Caso 1 — Quitar un artículo y que lo que ya dio alcance (H1)**
1. Abre el detalle de **H1**. Arriba debe decir **Ir pagando** y *"Debe: $50"* (Total $200 · pagado $150).
2. En la tarjeta de uno de los 2 artículos toca **−** una vez.
   - ✅ Debe pasar: el pedido se recarga solo y ahora el total es **$100** y el estado **Pagado**
     (ya no dice "Debe"). Cuenta: total nuevo $100; ya dio $150; $150 ≥ $100 → Pagado.
   - ✅ Debe pasar: en **Tienda**, el stock de ese artículo subió en 1.
   - ⛔ No puede pasar: que siga diciendo *"Debe: $…"*, o que tengas que salir y volver a entrar
     para ver *Pagado*.
   - 💬 Anota qué ves de los **$50 de más** (hoy no debe salir nada: el aviso de saldo a favor está
     pendiente). Es para confirmar que no aparece ningún número raro.
es correcto y que pasa coin eso 50 en el historial?
   - ↳ **Respuesta:** el historial (*Pagos registrados*) enseña lo que **entró**, y entró $150: eso
     no cambia al quitar un artículo, por eso sigue diciendo $150 y está bien. Los **$50 de más no se
     guardan en ningún lado todavía**: no hay "saldo a favor" del cliente ni devolución registrada.
     Hoy solo se deducen restando (pagado $150 − total $100). Mostrarlos como *"Saldo a favor: $50"*
     y decidir qué se hace con ellos (devolverlos, o usarlos en otra compra) es el pendiente
     **"saldo a favor"**; ese no se tocó en esta corrección.
**Caso 2 — A ese pedido ya Pagado, cambiarle un artículo por uno más caro (H1)**
1. En el detalle de **H1** (que quedó Pagado con 1 artículo de $100), toca **⇄** en su tarjeta.
2. En el buscador *"Cambiar por otro artículo"* escribe al menos 3 letras del artículo de **$200** y elígelo.
   - ✅ Debe pasar: sale *"Artículo cambiado"*. El pedido regresa a **Ir pagando** y dice
     *"Debe: $50"*. Cuenta: total nuevo $200; ya dio $150; le falta $200 − $150 = **$50**.
   - ✅ Debe pasar: en **Reportes**, la venta que se había creado cuando quedó Pagado ya no cuenta
     (el pedido volvió a deber).
   - ⛔ No puede pasar: que siga diciendo **Pagado** cuando debe $50.
   - aqui hay error quite el producto y si ya decia pagado y decia que pago 150 a un articulo de 100 es decir que yo debia 50 el dueno pues, pero agregue
   - un producto de 300 y mira paso esto, total hast arriba donde esta el imprimir tiket etc dice total 100 en lugar de sumar el primer producto que quedo
   - y el ultimo producto que  se agrego eso esta mal
   - p[agos ]registrados dice 150 eso esta bien no pero mira este texto ✅ Este pedido ya está pagado por completo — no se pueden registrar más abonos.

esta mal porque ya agregue el nuevo producto de 300

   - ↳ **Respuesta: tenías razón, era un error del back y ya está corregido** (ramas `dev` y `qa`;
     ver la segunda vuelta de la Prueba 4). Qué pasaba: al agregar, el back primero lee el pedido para revisar que se
     pueda editar, guarda el artículo nuevo y después vuelve a leer el pedido para sacar el total.
     Esa segunda lectura usaba la copia de la primera (la que todavía no tenía el artículo de $300),
     así que el total salía **$100**. Con $150 pagados contra $100, el back concluía *"ya está
     cubierto"* y lo volvía a dejar **Pagado** — por eso arriba decía *Total $100* y abajo *"✅ Este
     pedido ya está pagado por completo"*. El artículo sí quedó guardado en el pedido (por eso lo
     ves en la lista); lo que estaba mal era la cuenta.
     - Lo mismo le pasaba a **quitar una promoción completa** y a **cambiar a un artículo fuera del
       combo con "Quitar la promoción"**: borraba las líneas pero el total las seguía sumando.
       Quedó corregido con lo mismo.
     - **Quitar con −** nunca tuvo este problema (por eso tu Caso 1 salió bien).
     - Prueba automática nueva que reproduce exactamente tu caso ($100 pagado con $150 + artículo de
       $300): antes del arreglo fallaba con *total $100*; ahora da **total $400, pagado $150,
       Ir pagando**.
   - ↳ **Cómo dejar H1 bien en QA** (quedó guardado con total $100 y una venta de $100 en Reportes),
     **después de que se despliegue el arreglo en QA**:
     1. En el detalle de H1 toca **−** en el artículo de **$300**. El total se recalcula con lo que
        queda: **$100**, sigue **Pagado** y la venta queda en $100, que es lo correcto para ese
        momento.
        - ⚠️ Si los dos artículos son **del mismo modelo** (misma prenda, distinta talla/color), no
          uses H1: el botón − busca la línea por modelo y podría quitar el de $100. Mejor crea un
          pedido nuevo igual a H1 (1 artículo de $100, abono de $150) y prueba ahí.
     2. Vuelve a agregar el artículo de **$300** (o repite el Caso 2 con ⇄).
   - ↳ **Lo que debe pasar ahora al agregar el de $300 a H1 Pagado ($100, pagado $150):**
     - ✅ Arriba: *Total $400 · pagado $150* y **"Debe: $250"**. Cuenta: $100 + $300 = $400;
       $400 − $150 = **$250**.
     - ✅ Estado **Ir pagando** (ya no *Pagado*), ya **no** sale *"Este pedido ya está pagado por
       completo"* y vuelve a aparecer **💳 Registrar abono**.
     - ✅ *Pagos registrados* sigue en **$150**.
     - ✅ En **Reportes** ya no cuenta la venta de $100 (el pedido volvió a deber); se crea otra
       hasta que vuelva a quedar pagado.
     - ⛔ No puede pasar: que siga diciendo Total $100, o *Pagado*.


**Caso 3 — Quitar y luego cambiar en el mismo pedido (H2)**
1. Abre el detalle de **H2** (*"Debe: $50"*). Toca **−** en uno de sus artículos.
   - ✅ Debe pasar: igual que el caso 1: total **$100**, queda **Pagado**.
2. Ahora toca **⇄** en el artículo que quedó y cámbialo por el de **~$300**.
   - ✅ Debe pasar: regresa a **Ir pagando** con *"Debe: $150"*. Cuenta: $300 − $150 = **$150**.

**Caso 4 — Quitar el único artículo (H3)**
1. Abre el detalle de **H3** (1 artículo, *"Debe: $100"*). Toca **−** en su tarjeta.
   - ✅ Debe pasar: no lo deja y sale el aviso *"'<nombre del artículo>' es el ultimo articulo del
     pedido #H3. Para regresar todo, cancela el pedido"*. El pedido queda igual.
   - ⛔ No puede pasar: que quede un pedido sin artículos.

- [x] Caso 1: quitar → Pagado al momento — ✅ salió bien
- [ ] Caso 2: en lugar de ⇄ se agregó uno de $300 → ❌ Total $100 y seguía Pagado (corregido; se
  repite como Caso 3 de la segunda vuelta)
- [ ] Caso 3 y Caso 4: no se llegaron a probar → pasan a la segunda vuelta

---

## Prueba 5 — Cancelar un pedido con abonos

### 🔴 PRUEBA PENDIENTE

Hay dos lugares para cancelar y **no dicen lo mismo** (hallazgo 5.1; ya decidido arreglarlo, todavía
no está programado):

| Dónde | Botón | Qué dice hoy al terminar |
|---|---|---|
| **Pedidos → Mis pedidos**, en la card | **Cancelar** → *"¿Por qué cancelas este pedido?"* (elige motivo) → **Cancelar pedido** | Solo *"Pedido cancelado correctamente"* |
| **Ventas → Créditos / Abonos** | **✖ Cancelar** | Pregunta *"¿Cancelar el crédito (ir pagando) de …?"* y al final el mensaje completo |

**Antes de empezar — preparar 3 pedidos de Ir pagando (Ventas → Venta directa)**
- **K1:** 1 artículo, enganche $50.
- **K2:** 1 artículo, enganche $50.
- **K3** y **K4:** 1 artículo cada uno, sin enganche; únelos (K3 titular) como en la Prueba 3.

Anota el **stock** de cada artículo antes de cancelar (en **Catálogo → Modelos** o en la tienda).

**Caso 1 — Cancelar Ir pagando desde Créditos / Abonos (K1)**
1. **Ventas → Créditos / Abonos** → K1 → **✖ Cancelar** → confirma **Sí, registrar como incobrable**.
   - ✅ Debe pasar: queda **Cancelado**; el mensaje dice que el stock **no** se devolvió y la deuda
     que quedó ($ que faltaba).
   - ⛔ No puede pasar: que el stock suba (la mercancía ya se la llevó).

**Caso 2 — Cancelar Ir pagando desde la card (K2)**
1. **Pedidos → Mis pedidos** → card de K2 → **Cancelar** → motivo **El cliente avisó** → **Cancelar pedido**.
   - ✅ Debe pasar: queda Cancelado y el stock **no** sube.
   - Anota con 💬: el mensaje **no** dice cuánto devolver ni la deuda (es lo que se va a arreglar).

**Caso 3 — Cancelar un pedido unido que no es el titular (K4)**
1. Busca K4 por su número → **Cancelar**.
   - ✅ Debe pasar: solo K4 queda cancelado; en el detalle de K3 el grupo ya no lo suma al total ni al
     saldo.
   - ⛔ No puede pasar: que se cancele también K3.

**Caso 4 — Cancelar con un usuario que no es administrador**
1. Entra con un usuario sin rol de administrador → intenta cancelar un pedido **Pagado**.
   - ⛔ No puede pasar: que lo deje. Solo un administrador cancela un pedido pagado (es devolución).

- [ ] Caso 1: Créditos / Abonos, stock no regresa, mensaje completo
- [ ] Caso 2: desde la card, stock no regresa (anotar que no dice montos)
- [ ] Caso 3: pedido unido, solo cae el que se cancela
- [ ] Caso 4: un no-admin no cancela un pagado

### 5.1 Hallazgo al preparar esta prueba

Desde la **card de Mis pedidos** no se ve cuánto devolver ni la deuda: el back de ese botón contesta
solo "Pedido cancelado correctamente". En Créditos / Abonos sí sale. **Ya decidido** (3.13): la card
también lo dirá y preguntará por producto si viene bien o dañado. Pendiente de programar.

---

## Prueba 6 — Venta: lo que paga decide y "Falta entregarlo" (🆕 por programar)

### ⛔ TODAVÍA NO SE PUEDE PROBAR

No está programada. La prueba está escrita para cuando se haga; **no la hagas todavía**.

Reglas: skill `reglas-pedidos` 2.1–2.3 y `PLAN` §10.6 (A9, A11–A13). **Todavía no está en el
código**: esta prueba es para cuando se programe. Todos los casos con un pedido de **$200**.

### 6.1 Cuándo salen los dos botones y cuándo no

| # | En la venta escribes | La pantalla dice | Botones para terminar | Queda en Mis pedidos como |
|---|---|---|---|---|
| a | Pago $0 | Quedará como: **Apartado** | **Uno solo** (guardar). **No** salen ✅ / 📦 | Apartado |
| b | Pago $100, efectivo | Quedará como: **Ir pagando** | **✅ Ya se lo llevó** · **📦 Todavía no se lo lleva** | Ir pagando, debe $100 |
| c | Pago $100 por transferencia (la manda la conocida, la tía…) | Quedará como: **Ir pagando** | Igual que b | Ir pagando, debe $100 |
| d | Pago $200, efectivo | Quedará como: **Pagado** | **✅ Ya se lo llevó** · **📦 Falta entregarlo** | Según el botón (6.2) |
| e | Pago $200 por transferencia | Quedará como: **Pagado** | Igual que d | Según el botón (6.2) |
| f | Pago $200 con tarjeta en la terminal | Quedará como: **Pagado** | Igual que d | Según el botón (6.2) |
| g | Pago $200 efectivo con un billete de $500 | Quedará como: **Pagado** · Cambio a devolver **$300** | Igual que d | Según el botón (6.2) |
| h | Pago $500 por transferencia | **No deja**: en transferencia es el monto exacto | — | — |
| i | Eliges **Apartado** y luego escribes $100 | Cambia solo a **Ir pagando**, con la nota *"Un Apartado es sin dinero; como te dio $100, queda como Ir pagando"* | Igual que b | Ir pagando |

Revisar en todos: ninguno de los dos botones viene marcado, y antes de guardar sale el resumen
(*"Cobrado: $200 por transferencia · Falta entregarlo"*).

### 6.2 Qué pasa con cada botón

| Caso | Botón | Esperado en Mis pedidos |
|---|---|---|
| Pagó $200 | ✅ Ya se lo llevó | **Entregado**, terminado (como hoy). Sin Cobrar ni Entregar |
| Pagó $200 | 📦 Falta entregarlo | Etiqueta **"Pagado · falta entregar"**, botón **Entregar** (no Cobrar), sale en el filtro **"Por entregar"** |
| Pagó $100 | ✅ Ya se lo llevó | Ir pagando, debe $100; si se cancela, el stock **no** regresa |
| Pagó $100 | 📦 Todavía no se lo lleva | Ir pagando, debe $100; si se cancela, el stock **sí** regresa |

### 6.3 Después de guardar

- [ ] "Pagado · falta entregar" → **Entregar** → queda Entregado y **no** pide dinero.
- [ ] "Pagado · falta entregar" con día de entrega ya pasado → sale **"⚠ Atrasado N días"**.
- [ ] "Pagado · falta entregar" **no** se cancela solo aunque pasen días (los de contado sin cobrar sí,
      a los 2 días).
- [ ] El filtro **"Por entregar"** muestra solo los pagados que no se han entregado.

### 6.4 Corregir errores

| # | Error al guardar | Cómo corregirlo | Esperado |
|---|---|---|---|
| 1 | "Falta entregarlo" pero no había pagado nada | Detalle → 🔁 Cambiar forma de cobro → **Apartado** | Se anula el cobro, se borra la venta, queda Apartado debiendo $200 |
| 2 | "Falta entregarlo" pero pagó solo $100 | 🔁 Cambiar forma de cobro → **Ir pagando**, "¿Cobra algo ahora?" $100 | Ir pagando, debe $100 |
| 3 | "Falta entregarlo" pero sí se lo llevó | **Entregar** en la card | Entregado |
| 4 | "Ya se lo llevó" pero no se lo llevó (con admin) | Botón para regresarlo a "Falta entregarlo" | Pagado · falta entregar |
| 5 | Lo mismo que 4, con un usuario que **no** es admin | — | El botón **no** sale |

---

## Prueba 7 — Apartado es sin dinero (en QA desde 2026-10-01)

### 🔴 PRUEBA PENDIENTE — es la primera que conviene hacer

Reglas: skill `reglas-pedidos` 2.1. Qué cambió: `CAMBIOS_FRONT.md` → "📦 Apartado es sin dinero".
**Antes de empezar:** en QA, recarga forzada (`Ctrl + Shift + R`) para no quedarte con la versión vieja.

### 7.0 Preparar los pedidos de prueba (una sola vez)

Todo en el menú **Ventas → Venta directa** (pantalla "Venta directa"). Usa artículos baratos de prueba y
anota el número de cada pedido que se crea (sale en el mensaje "✅ Apartado registrado · Pedido #…").

| Pedido | Cómo crearlo | Anota |
|---|---|---|
| **A** — Apartado sin dinero | 🔍 Buscar producto → agrega 1 artículo → **📦 Apartado** → deja **💰 Pago inicial (enganche)** vacío o en 0 → **💰 Cobrar** | número de A y su total |
| **B** — otro Apartado | Igual que A, con otro artículo | número de B |
| **C** — otro Apartado | Igual que A | número de C |
| **D** — otro Apartado | Igual que A | número de D |
| **E** — Ir pagando con dinero | Agrega 1 artículo → **💳 Ir pagando** → enganche **$50** → **💰 Cobrar** | número de E |
| **F** — Ir pagando sin dinero | Agrega 1 artículo → **💳 Ir pagando** → enganche en 0 → **💰 Cobrar** | número de F |

Para entrar al detalle de cualquiera: **Pedidos → Mis pedidos** → busca su número → **👁 Detalle**.

### 7.1 Apartado con un adelanto → se cambia a Ir pagando (pedido A)

1. Abre el detalle de **A**.
2. Abajo, clic en **💳 Registrar abono**.
   - ✅ El campo **Monto** ya trae el **total** del pedido.
   - ✅ Abajo del título dice: *"Es un Apartado: se paga completo ($…). Para dejar un adelanto, cámbialo a Ir pagando."*
3. Cambia el monto a **$50** y da **💾 Guardar abono**.
   - ✅ Sale el aviso **"Un Apartado se paga completo"** que explica que, si dejó un adelanto, primero se cambia a Ir pagando.
   - ✅ **No** se registró nada: en "📋 Pagos registrados" no aparece el abono.
4. En el aviso, clic en **🔁 Cambiar a Ir pagando**.
   - ✅ Se abre **"🔁 Cambiar la forma de cobro"** con **Ir pagando** ya marcado y **$50** en "¿Cobra algo ahora?".
5. En "¿Por qué cambia?" escribe *"dejó $50 de adelanto"* → **Guardar cambio**.
   - ✅ El pedido queda **💳 Ir pagando**, con **Pagado $50** y debe el resto.
   - ✅ En "📋 Pagos registrados" aparece el abono de $50 con la nota del cambio.

⛔ **No puede pasar:** que se registre un abono menor al total en un Apartado, o que al cambiar a Ir pagando se pierdan los $50.

- [ ] 7.1 completa

### 7.2 Apartado pagado completo, con cambio (pedido B)

1. Abre el detalle de **B** → **💳 Registrar abono**. El monto ya trae el total.
2. Deja el monto como está, forma de pago **Efectivo**, y en **💵 Monto recibido** pon **$500**.
   - ✅ Sale *"Cambio a devolver: $…"* (500 menos el total).
3. **💾 Guardar abono**.
   - ✅ Se registra sin aviso y el pedido queda **Pagado**.
   - ✅ Ya no aparece el botón "💳 Registrar abono"; dice *"Este pedido ya está pagado por completo"*.

⛔ **No puede pasar:** que salga el aviso "Un Apartado se paga completo" pagando el total, o que el cambio salga mal.

- [ ] 7.2 completa

### 7.3 Créditos y Abonos (pedido C)

1. Menú **Ventas → Créditos / Abonos** (pantalla "💳 Créditos y Abonos") → busca el pedido **C** → **+ Abono**.
   - ✅ El monto ya trae el total y abajo dice *"Es un Apartado: se paga completo…"*.
2. Cambia el monto a **$50** → registrar.
   - ✅ Aviso **"Un Apartado se paga completo"** con el botón **Ir al pedido**. No se registra nada.
3. Clic en **Ir al pedido**.
   - ✅ Se abre el pedido **C** en Mis pedidos.

⛔ **No puede pasar:** que Créditos / Abonos registre el abono de $50 en el Apartado.

- [ ] 7.3 completa

### 7.4 Cambiar forma de cobro (pedidos E y F)

1. Abre el detalle de **E** (Ir pagando con $50) → **🔁 Cambiar forma de cobro**.
   - ✅ El botón **Apartado** sale gris y no se puede elegir. Al pasar el mouse dice
     *"Ya dio dinero: un Apartado es sin dinero, queda como Ir pagando"*.
   - Da **Cancelar**.
2. Abre el detalle de **F** (Ir pagando sin dinero) → **🔁 Cambiar forma de cobro** → clic en **Apartado**.
   - ✅ **No** aparece "¿Cobra algo ahora?".
   - ✅ Sale la nota *"Un Apartado es sin dinero: el cliente lo paga completo cuando lo recoge…"*.
3. **Guardar cambio**.
   - ✅ **F** queda como **📦 Apartado**.
   - ✅ Las ayudas de los botones dicen: Apartado *"Sin dinero: lo paga completo al recogerlo"* e
     Ir pagando *"Ya dio algo y va abonando"*.

⛔ **No puede pasar:** poder elegir Apartado en un pedido que ya tiene dinero (E).

- [ ] 7.4 completa

### 7.5 Apartados unidos (pedidos C y D)

1. Abre el detalle de **C** → **🔗 Unir con otros pedidos** → busca **D** → elígelo → **Unir 2 pedidos**.
2. En el bloque **"🔗 Unido en el grupo #…"**:
   - ✅ El botón dice **💵 Pagar el grupo completo** (ya no "Abonar al grupo").
3. Clic en **💵 Pagar el grupo completo**.
   - ✅ El monto ya trae el **saldo del grupo** (C + D) y el texto dice *"Son Apartados: se pagan completos…"*.
4. Cambia el monto a **$20** → **Registrar abono**.
   - ✅ Aviso **"Los Apartados se pagan completos"**. No se registra nada.
5. Regresa el monto al saldo completo → **Registrar abono**.
   - ✅ **C** y **D** quedan **Pagado**; el saldo del grupo queda en $0.

⛔ **No puede pasar:** que el grupo de Apartados acepte un pago menor al saldo, o que diga "Abonar al grupo".

- [ ] 7.5 completa

### 7.6 Venta directa: Apartado con enganche se vuelve Ir pagando

1. **Ventas → Venta directa** → agrega 1 artículo → clic en **📦 Apartado**.
2. En **💰 Pago inicial (enganche)** escribe **100**.
   - ✅ El botón activo cambia solo a **💳 Ir pagando**.
   - ✅ Sale la nota *"Un Apartado es sin dinero: como te dio $100.00, queda como Ir pagando."*
3. **💰 Cobrar**.
   - ✅ El mensaje dice **"Ir pagando registrado"** con el enganche de $100.
   - ✅ En Mis pedidos ese pedido es **Ir pagando**, pagado $100.
4. Otra venta: 1 artículo → **📦 Apartado** → sin enganche → **💰 Cobrar**.
   - ✅ Se guarda como **Apartado**, como siempre.

⛔ **No puede pasar:** que se guarde un Apartado con enganche.

- [ ] 7.6 completa

### Si algo no sale igual

Anota debajo del paso, con 💬, qué hiciste, qué esperabas y qué salió (y el número de pedido). Lo
reviso y respondo abajo con ↳.

---

## Prueba 8 — Botón Volver alineado con la tarjeta

### 🔴 PRUEBA PENDIENTE

Qué cambió (2026-10-01): el botón **← Volver** (o el texto que tenga en cada pantalla) ya no queda en
la esquina; queda en el **mismo borde izquierdo de la tarjeta**, arriba de ella, como **← Regresar** en
Política de Privacidad. Su texto y lo que hace **no cambiaron**.

**Pasos — en cada pantalla de la lista**
1. Entra a la pantalla.
2. Mira el borde izquierdo del botón y el borde izquierdo de la tarjeta de abajo.
   - ✅ Debe pasar: los dos bordes están en la misma línea vertical.
   - ⛔ No puede pasar: que el botón quede pegado a la esquina o más a la izquierda/derecha que la tarjeta.
3. Hazle clic.
   - ✅ Debe pasar: regresas a la pantalla de antes (con los mismos filtros si venías de un buscador).
4. Repite en el celular (o haciendo angosta la ventana).
   - ✅ Debe pasar: sigue alineado con la tarjeta.

| Pantalla | Cómo llegar (nombres sacados del código) | ✔ |
|---|---|---|
| Agregar Modelo | **Catálogo → Agregar modelo** | [ ] |
| Actualizar Modelo | **Catálogo → Modelos** → en un modelo **✏️ Actualizar** | [ ] |
| Nuevo Producto | **Catálogo → Agregar producto** | [ ] |
| Actualizar producto (artículo) | **Tienda** → en un artículo **✏️ Editar** | [ ] |
| Cargar catálogo Excel | **Catálogo → Cargar Excel** | [ ] |
| Carrito | Agrega algo al carrito → botón **Carrito** del menú lateral | [ ] |
| Entregas por zona | **Envíos → Entregas por zona** | [ ] |
| Ver un cliente | **Clientes** → en un cliente **👁️ Ver/Editar** | [ ] |
| Nuevo cliente | No tiene botón en el menú: pega `qa.shop.novedades-jade.com.mx/clientes/agregar` | [ ] |
| Mis datos | Abajo en el menú lateral, debajo de tu nombre: **Mis datos** | [ ] |
| Cambiar contraseña | Mismo lugar: **Cambiar contraseña** | [ ] |
| Mi perfil | Mismo lugar: **Mi perfil** | [ ] |
| Agregar mi compra | Mismo lugar: **Agregar mi compra** | [ ] |


---

## Referencias

- `PENDIENTES_2026-09-29.md` — B (reglas de quitar/cambiar y cancelar), C (Hosting-Mexico), E.2 (TikTok).
- `src/main/java/com/ventas/key/hexagonal/grupopedido/README.md` — reglas R1–R18 de pedidos unidos.
- `TIKTOK_SETUP.md` — checklist para activar TikTok en un ambiente.
