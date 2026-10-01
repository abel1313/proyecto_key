# Pruebas en QA — redes, páginas legales y pedidos unidos (2026-10-01)

Junta en un solo lugar el plan de pruebas y el estado de cada tema que se armaron el 2026-10-01.
Se sigue llenando aquí; lo que ya está en `PENDIENTES_2026-09-29.md` solo se referencia.

**Cómo leerlo:**
- `💬` = lo que escribió el dueño al probar, tal cual.
- `↳` = la respuesta o el cambio, debajo de cada comentario.
- Los nombres de botones y textos se sacaron del código del 2026-10-01 (el primer plan tenía
  nombres adivinados; ya se corrigieron).

---

## ▶ Dónde nos quedamos — empezar aquí

- [ ] **Prueba 1:** volver a probar el botón **← Regresar** (el deploy de QA se corrió de nuevo, ver 1.1).
- [ ] **Hacer:** un solo botón de abonar en pedidos unidos (3.4) — por hacer, no está en el código.
- [ ] **Hacer:** que el pedido abierto se vea igual que los otros del grupo (3.6) — por hacer.
- [x] ~~Cancelar desde la card no dice el saldo a favor~~ → decidido: que lo diga y pregunte bien/dañado (3.13)
- [x] ~~¿Apartado acepta anticipo / abonos?~~ → **No**: Apartado es sin dinero (3.9)
- [x] ~~¿El Apartado se cobra completo con Cobrar?~~ → **Sí**, y detecta pago completo y cambio (3.10)
- [x] ~~Las 5 dudas de 3.10~~ → contestadas (3.11)
- [x] ~~Venta que pagó todo pero no se lo lleva~~ → decidido (3.12); casos de prueba en la **Prueba 6**
- [ ] **Hacer:** Prueba 6 (lo que paga decide, "Falta entregarlo", Ir pagando "¿ya se lo llevó?")
- [x] ~~Hacer "Apartado = sin dinero"~~ → programado en `dev` y `qa` (falta el script de QA). **Probar: Prueba 7**
- [x] ~~Tabla del grupo y "Pagos registrados"~~ → decidido: solo totales + "Detalle de los pagos" (3.13)
- [ ] **Seguir probando:** Prueba 3 desde el paso 3 (3.7), luego Pruebas 4 y 5.

## Estado de cada tema

| Tema | Dónde está | Estado |
|---|---|---|
| Botón ← Regresar en páginas legales | front `dev` y `qa` | ⚠️ re-probar (1.1) |
| TikTok: conectar, quitar acceso, subir video | prod (con llaves de Sandbox) | ✅ probado |
| TikTok: revisión de la app de Production | developers.tiktok.com | ⏳ esperando respuesta (`PENDIENTES` E.2) |
| Pedidos unidos: saldos y abonos | back y front `dev`/`qa` | 🔄 probando (Prueba 3) |
| Quitar / cambiar artículo con abonos | back y front `dev`/`qa` | ⏳ sin probar (Prueba 4) |
| Cancelar pedido con abonos | back y front `dev`/`qa` | ⏳ sin probar (Prueba 5) |
| Venta: lo que paga decide y "Falta entregarlo" | — | 🆕 por programar (Prueba 6) |
| Apartado es sin dinero | back y front `dev`/`qa` | 🧪 listo para probar (Prueba 7) |
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

- [x] Sin sesión: no aparece en `/privacidad`
- [ ] Sin sesión: no aparece en `/eliminar-datos` y `/termConditions`
- [ ] Con sesión: aparece en las 3
- [ ] Regresa a la pantalla anterior / al inicio si se abrió directo
- [ ] Se ve bien en tema claro y oscuro

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

**Por hacer (siguiente sesión):** cuando el pedido está unido, esconder **💳 Registrar abono** y dejar
solo **💵 Abonar al grupo**.

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

**Por hacer (siguiente sesión):** ponerle al pedido abierto la misma cabecera que a los otros
(*Pedido #X · cliente · N artículos · $total · "este"*), para que los pedidos del grupo se vean
iguales.

### 3.7 Lo que falta de la Prueba 3 (con los nombres reales)

**Paso 3 — Abonar al grupo**
1. Mis pedidos → card del grupo → **👁 Detalle**.
2. En "🔗 Unido en el grupo #G" → **💵 Abonar al grupo** → *Monto*, *Forma de pago* (Efectivo o
   Transferencia), *¿Con cuánto paga?* si es efectivo → **Registrar abono**.
3. Esperado:
   - En la tabla, el pedido **más viejo** se llena primero (ver ejemplo de 3.4).
   - *Total del grupo · Pagado · Saldo* se actualizan.
   - De regreso en Mis pedidos, la card del grupo ya dice **"Falta $Y"**.

**Paso 4 — Separar**
1. **✂️ Separar pedidos** → marcar en *Sale* los que se separan.
2. En *Se queda con* escribir cuánto se queda cada uno; *Queda debiendo* se calcula mientras se escribe.
3. **Separar**. Si no suma exacto lo que dio el cliente, no deja.
4. Esperado: cada pedido sale con su propia card y, si es Apartado o Ir pagando con abonos, **"Pagado $X · Falta $Y"**.
   El que cubre su total queda **Pagado**.

- [ ] La card del grupo dice "Falta" después de abonar
- [ ] El abono al grupo llena primero el pedido más viejo
- [ ] Total / Pagado / Saldo del grupo cuadran (Total = Pagado + Saldo)
- [ ] Separar no deja si la suma no es exacta
- [ ] Al separar, cada pedido conserva lo que se le asignó

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

## Prueba 4 — Quitar o cambiar un artículo en un pedido con abonos (sin probar)

Se hace en el Detalle del pedido, en la card de cada artículo: **−** (quitar uno) y **⇄** (cambiar
por otro). Regla acordada (`PENDIENTES` B): la cuenta se hace **solo con ese pedido**; lo que sobre
es de ese cliente.

| Caso | Esperado |
|---|---|
| Pedido **Pagado** y se cambia un artículo por uno **más caro** | Regresa a Apartado / Ir pagando y se borra la venta que tenía |
| Se quita un artículo y lo abonado **cubre** el nuevo total | Pasa solo a **Pagado** |
| Lo abonado queda **mayor** que el nuevo total | Queda Pagado. El aviso "Saldo a favor del cliente" en pantalla **todavía no existe** (`PENDIENTES` B) |
| Se quiere quitar el **último** artículo | No deja: *"'<artículo>' es el ultimo articulo del pedido #N. Para regresar todo, cancela el pedido"* |

- [ ] Cambiar a uno más caro un pedido Pagado
- [ ] Quitar y que lo abonado cubra el total
- [ ] Quitar de más (anotar qué se ve, para el aviso de saldo a favor)
- [ ] Intentar quitar el último artículo

---

## Prueba 5 — Cancelar un pedido con abonos (sin probar)

Hay dos lugares para cancelar, y **no dicen lo mismo**:

| Dónde | Botón | Qué dice al terminar |
|---|---|---|
| Mis pedidos, en la card | **Cancelar** → *"¿Por qué cancelas este pedido?"* → **Cancelar pedido** | Solo *"Pedido cancelado correctamente"* |
| Créditos / Abonos | **✖ Cancelar** | El mensaje completo, p. ej. *"APARTADO cancelado. Stock devuelto. Saldo a favor del cliente: $X"* |

Qué debe pasar (igual en los dos):
- **Apartado:** el stock regresa. Hoy, si tiene abonos, quedan como saldo a favor; con la regla 3.9
  un Apartado ya no tendrá dinero, así que esto se va a quitar.
- **Ir pagando:** el stock **no** regresa (la mercancía ya se la llevó); lo que debía queda como
  deuda incobrable.
- **Pagado o Entregado:** es una devolución; solo la puede hacer un administrador.
- **Unido:** se cancela solo ese pedido; el grupo deja de contarlo. Si es el titular, primero
  **Cambiar quién recoge**.
- La devolución del dinero se hace a mano (efectivo o transferencia); el sistema no registra la
  salida (`PENDIENTES` B, pendiente de decidir).

### 5.1 Hallazgo al preparar esta prueba

Desde la **card de Mis pedidos**, al cancelar un Apartado con abonos **no se ve cuánto hay que
devolverle al cliente**: el back de ese botón contesta solo "Pedido cancelado correctamente". El
monto sí sale cancelando desde **Créditos / Abonos**.

**Por decidir:** que la card también muestre el saldo a favor (y el resto del mensaje), igual que
Créditos / Abonos.

- [ ] Cancelar un Apartado con abonos desde Créditos / Abonos (sale el saldo a favor)
- [ ] Cancelar el mismo caso desde la card (confirmar que no sale)
- [ ] Cancelar un Ir pagando (el stock no regresa)
- [ ] Cancelar un pedido unido que no es el titular

---

## Prueba 6 — Venta: lo que paga decide y "Falta entregarlo" (🆕 por programar)

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

## Prueba 7 — Apartado es sin dinero (✅ en QA desde 2026-10-01)

Reglas: skill `reglas-pedidos` 2.1. Qué cambió: `CAMBIOS_FRONT.md` → "📦 Apartado es sin dinero".
**Antes de empezar:** en QA, recarga forzada (`Ctrl + Shift + R`) para no quedarte con la versión vieja.

### 7.0 Preparar los pedidos de prueba (una sola vez)

Todo en **Tienda → Venta directa** (pantalla "Venta directa"). Usa artículos baratos de prueba y
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

- [ ] 7.1 completa

### 7.2 Apartado pagado completo, con cambio (pedido B)

1. Abre el detalle de **B** → **💳 Registrar abono**. El monto ya trae el total.
2. Deja el monto como está, forma de pago **Efectivo**, y en **💵 Monto recibido** pon **$500**.
   - ✅ Sale *"Cambio a devolver: $…"* (500 menos el total).
3. **💾 Guardar abono**.
   - ✅ Se registra sin aviso y el pedido queda **Pagado**.
   - ✅ Ya no aparece el botón "💳 Registrar abono"; dice *"Este pedido ya está pagado por completo"*.

- [ ] 7.2 completa

### 7.3 Créditos y Abonos (pedido C)

1. Menú **Créditos / Abonos** (pantalla "💳 Créditos y Abonos") → busca el pedido **C** → **+ Abono**.
   - ✅ El monto ya trae el total y abajo dice *"Es un Apartado: se paga completo…"*.
2. Cambia el monto a **$50** → registrar.
   - ✅ Aviso **"Un Apartado se paga completo"** con el botón **Ir al pedido**. No se registra nada.
3. Clic en **Ir al pedido**.
   - ✅ Se abre el pedido **C** en Mis pedidos.

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

- [ ] 7.5 completa

### 7.6 Venta directa: Apartado con enganche se vuelve Ir pagando

1. **Tienda → Venta directa** → agrega 1 artículo → clic en **📦 Apartado**.
2. En **💰 Pago inicial (enganche)** escribe **100**.
   - ✅ El botón activo cambia solo a **💳 Ir pagando**.
   - ✅ Sale la nota *"Un Apartado es sin dinero: como te dio $100.00, queda como Ir pagando."*
3. **💰 Cobrar**.
   - ✅ El mensaje dice **"Ir pagando registrado"** con el enganche de $100.
   - ✅ En Mis pedidos ese pedido es **Ir pagando**, pagado $100.
4. Otra venta: 1 artículo → **📦 Apartado** → sin enganche → **💰 Cobrar**.
   - ✅ Se guarda como **Apartado**, como siempre.

- [ ] 7.6 completa

### Si algo no sale igual

Anota debajo del paso, con 💬, qué hiciste, qué esperabas y qué salió (y el número de pedido). Lo
reviso y respondo abajo con ↳.

---

## Referencias

- `PENDIENTES_2026-09-29.md` — B (reglas de quitar/cambiar y cancelar), C (Hosting-Mexico), E.2 (TikTok).
- `src/main/java/com/ventas/key/hexagonal/grupopedido/README.md` — reglas R1–R18 de pedidos unidos.
- `TIKTOK_SETUP.md` — checklist para activar TikTok en un ambiente.
