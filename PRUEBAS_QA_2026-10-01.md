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
- [ ] **Hallazgo nuevo:** cancelar desde la card de Mis pedidos no dice el saldo a favor (5.1).
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

↳ **Sí.** A crédito todo el dinero entra como abono; no hay otro cobro. Por dónde entra:

| Dónde | Botón / campo | Qué hace |
|---|---|---|
| Venta directa, al crear el pedido | **💰 Pago inicial (enganche)** | Se registra como el primer abono justo después de crear el pedido |
| Detalle del pedido | **💳 Registrar abono** → 💾 Guardar abono | Abono a ese pedido |
| Pantalla Créditos / Abonos | Registrar abono | Igual que el anterior |
| Detalle de un pedido unido | **💵 Abonar al grupo** | Un abono para todo el grupo (ver 3.4) |
| Detalle → 🔁 Cambiar forma de cobro (de contado a crédito) | **¿Cobra algo ahora?** | Lo que se escriba queda como abono, con la nota de "¿Por qué cambia?" |

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
4. Esperado: cada pedido sale con su propia card y, si es a crédito con abonos, **"Pagado $X · Falta $Y"**.
   El que cubre su total queda **Pagado**.

- [ ] La card del grupo dice "Falta" después de abonar
- [ ] El abono al grupo llena primero el pedido más viejo
- [ ] Total / Pagado / Saldo del grupo cuadran (Total = Pagado + Saldo)
- [ ] Separar no deja si la suma no es exacta
- [ ] Al separar, cada pedido conserva lo que se le asignó

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
- **Apartado:** el stock regresa; lo abonado queda como saldo a favor del cliente.
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

## Referencias

- `PENDIENTES_2026-09-29.md` — B (reglas de quitar/cambiar y cancelar), C (Hosting-Mexico), E.2 (TikTok).
- `src/main/java/com/ventas/key/hexagonal/grupopedido/README.md` — reglas R1–R18 de pedidos unidos.
- `TIKTOK_SETUP.md` — checklist para activar TikTok en un ambiente.
