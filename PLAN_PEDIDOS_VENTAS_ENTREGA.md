# Plan — ventas y pedidos (dev y qa)

**Estado: EN DEFINICIÓN, SIN PROGRAMAR.** Versión 3 (2026-09-30; la sección 6 manda sobre lo anterior). Versión 2 (2026-09-29, noche): reglas que pasó el dueño +
revisión del código punto por punto. Alcance: **solo `dev` y `qa`**.

Leyenda: ✅ ya existe · 🐞 falla hoy · 🆕 hay que hacerlo · ❓ falta decidir · 🔮 para después

---

## 1. Reglas que dio el dueño

**Estados del pedido:**
- **PENDIENTE** = se hizo el pedido y todavía no se entrega (y puede que no se haya pagado).
- **ENTREGADO** = ya se entregó **y** ya se pagó. Es el estado final.
- Si no se cumple alguna de las dos (no se entregó / no se pagó), tiene que quedar un campo que
  diga **qué pasó / por qué no se entregó**.

**Formas de cobro:**
1. **Contado en el local:** efectivo, transferencia o tarjeta (crédito, débito). Si se ofrece
   "3 meses sin intereses" tiene que ser algo que el dueño **activa** en algún lado.
2. **Apartado:** lo deja encargado (por ejemplo para llevarlo a Zacazonapan). El pedido queda
   pendiente como apartado. Si deja anticipo, se anota (efectivo o transferencia).
3. **Ir pagando:** **no caduca nunca**; se paga en efectivo o transferencia.

---

## 2. Lo que se encontró en el código

| # | Tema | Cómo está hoy | |
|---|---|---|---|
| H1 | Volver a "Contado" en la venta después de elegir Apartado | No hay botón "Contado": hay que volver a picar el mismo botón de Apartado para quitarlo. Por eso parecía que había que recargar. | 🐞 |
| H2 | Meses sin intereses (3 MSI, etc.) | Las opciones salen de las tablas `pagos_y_meses` / `meses_intereses`. **No hay pantalla** para activarlas o desactivarlas: solo editando la base. | 🆕 |
| H3 | Anticipo en Apartado / Ir pagando | ✅ "Pago inicial (enganche) — opcional", efectivo o transferencia, con cambio si es efectivo. | ✅ |
| H4 | "Fecha de entrega" en la venta | Se guarda como la fecha de entrega del pedido. La fecha de la venta se pone sola (hoy). Al vender casi nunca se sabe cuándo se va a entregar. | ❓ |
| H5 | ¿Qué se cancela solo? | Solo pedidos de **contado en Pendiente** con fecha, 2 días después de esa fecha. **Apartado e Ir pagando nunca.** | ✅ |
| H6 | "Entregas por zona" y la cancelación automática | Al programar un viaje, a cada pedido de la zona (Pendiente **y** Apartado) le pone la fecha del viaje. Los de **contado** que no se cobren en 2 días después de esa fecha **se cancelan solos y regresan stock**. Los apartados no. | ❓ |
| H7 | "Ya pagó, falta entregarlo" | **No existe.** Lo que existe es "Entregas por zona": programa día, hora y punto de encuentro y avisa por correo; **no marca pagos**. | 🆕 |
| H8 | Cobrar falla a la mitad | El cobro es una sola transacción: si algo falla no se guarda nada (no descuenta, no crea venta) y el pedido sigue Pendiente. Se puede reintentar. | ✅ |
| H9 | Card: "Fecha" | Es la **fecha en que se hizo el pedido**, pero no lo dice. | 🆕 |
| H10 | Card: botón "Entrega" | Edita **un solo pedido**: quién recibe, dirección, fecha, lugar y notas. Lo usa el admin (permiso `editar-entrega`) y también el **cliente en su propio pedido**; el cliente ve esos datos en su detalle. **No manda correo.** "Entregas por zona" hace lo mismo **para todos los de una zona** y sí manda correo. | ❓ |
| H11 | Cambiar forma de cobro con pedidos unidos | Hoy se **bloquea**: "deshaz el grupo antes de cambiar su forma de cobro". | 🆕 |
| H12 | Detalle: "Cómo llegar" | **Nunca lleva al local.** Va al punto de encuentro del viaje, a las coordenadas de la casa del cliente o busca la dirección escrita. El "Cómo llegar" del login sí usa la ubicación del local (por eso ahí sí funciona). | 🐞 |
| H13 | Registrar abono desde el detalle (pedido no unido) | ✅ Botón "Registrar abono" en el detalle. | ✅ |
| H14 | Unir, abonar, separar repartiendo, volver a unir | ✅ Hecho y probado: cada abono se queda en su pedido; al separar se decide cuánto va a cada uno; al volver a unir se suma lo de todos (incluye abonos hechos mientras estaban separados). | ✅ |
| H15 | Quitar / cambiar productos con abonos | ✅ (2026-09-29) La cuenta es por pedido; si lo abonado cubre, queda Pagado; no deja vaciar un pedido. | ✅ |
| H16 | Unir: capturar los números de pedido | Es un campo de texto ("102, 105"); solo avisa si algo está mal **al darle Unir**. No busca ni muestra de quién es cada pedido. | 🆕 |
| H17 | Cancelar "Ir pagando" | El stock **nunca** regresa. No pregunta nada. | 🆕 |
| H18 | Dinero devuelto | El sistema dice "saldo a favor $X", pero **no registra** que se devolvió; el corte del día del abono lo sigue contando. | 🆕 |

---

## 3. Lo que hay que hacer

### Ventas (pantalla de venta en el local)

| # | Qué | Estado |
|---|---|---|
| V1 | Botón **"Contado"** junto a Apartado / Ir pagando para cambiar de uno a otro sin recargar (H1) | 🐞 |
| V2 | Pantalla para **activar / desactivar meses sin intereses** y ver su comisión (H2) | 🆕 |
| V3 | **"Ya pagó, falta entregarlo"** en la venta: forma de pago, nota, y entrega "pasa por él" o "se lo llevo" (día, lugar/zona, dirección) (H7) | 🆕 |
| V4 | Fecha de entrega en la venta: quitarla u opcional (ver ❓2) (H4) | ❓ |

### Pedidos — card y detalle

| # | Qué | Estado |
|---|---|---|
| P1 | **"Ya pagó"** en un pedido Pendiente (el que el cliente hizo desde su cuenta): forma de pago + nota + datos de entrega → **"Pagado, falta entregar"** (H7) | 🆕 |
| P2 | Botón **"Entregado"** para cerrar, en contado **y** en crédito (hoy crédito se queda en "Pagado" sin saber si se entregó) | 🆕 |
| P3 | **"No se entregó"** con motivo (no estaba, no contestó, cambió de opinión, otro…) (ver ❓4) | 🆕 |
| P4 | En grupos, "Entregado" desde el titular marca **cada pedido** del grupo como pagado y entregado | 🆕 |
| P5 | Card: "Fecha" → **"Pedido el …"**, y si tiene fecha de entrega, **"Entrega: sáb 10:00"** (H9) | 🆕 |
| P6 | Cambiar forma de cobro de pedidos **unidos**: se cambia **todo el grupo junto** y sigue unido (H11) (ver ❓6) | 🆕 |
| P7 | **Cómo llegar** (H12): recoger en tienda → al **local**; entrega en zona → punto de encuentro; a domicilio → dirección del cliente. Si el local no tiene ubicación configurada, el botón sale **deshabilitado** con "Configura la ubicación del local" (ver ❓7) | 🐞 |
| P8 | **Unir:** buscador de pedidos (por número o nombre) que muestre cliente, forma de cobro y total, y avise al momento si no se puede unir (H16). El diseño lo busca el dueño. | 🆕 |

### Devoluciones y cancelaciones

| # | Qué | Estado |
|---|---|---|
| D1 | **Regresar productos** de un pedido (unido o no): elegir el pedido, los productos y cuántos. Por cada uno: **¿viene bien?** Bien → regresa al stock. Mal → **no** regresa al stock y queda nota "regresó dañado". Se descuenta del total del pedido. | 🆕 |
| D2 | **Si regresa todo**: en vez de quitar uno por uno, **cancelar el pedido** con la misma lista de productos para marcar bien / mal | 🆕 |
| D3 | **Cancelar "Ir pagando"**: preguntar si regresó la mercancía (D1/D2 aplican) (H17) | 🆕 |
| D4 | **"Registrar devolución"** en un pedido ya cancelado cuya mercancía regresa después: por producto, bien / mal, **una sola vez** por pieza | 🆕 |
| D5 | **¿Le devolviste el dinero?** Sí / No al cancelar o devolver, y si sí: **registrar la devolución** con fecha, monto y forma (efectivo / transferencia) para que el corte cuadre (H18) | 🆕 |

### Para después (anotado, no ahora)
- 🔮 **Pago mixto** (parte efectivo, parte transferencia) — al terminar todo lo de este plan.
- 🔮 **Pagos desde la aplicación** (que el cliente pague en línea) — tenerlo en mente.
- 🔮 Rediseño del detalle del pedido (el dueño busca diseños).

---

## 4. ❓ Dudas para el dueño

1. **Estados.** Hoy el pedido guarda la forma de cobro revuelta con el estado (un apartado tiene
   estado "APARTADO"). Propuesta: **no cambiar lo de adentro** (lo usan la cancelación automática,
   Entregas por zona, reportes y grupos) y en pantalla mostrar **dos cosas separadas**:
   - Forma de cobro: Contado · Apartado · Ir pagando
   - Estado: **Pendiente** · **Pagado, falta entregar** · **Entregado** (pagado y entregado) · **Cancelado**
   ¿De acuerdo?
2. **Fecha de entrega en la venta.** Como al vender no se sabe cuándo se entrega: ¿la **quito** de
   la venta y se pone después (Entregas por zona o el botón Entrega), o la dejo **opcional** por si
   ya se sabe? (recomendado: opcional).
3. **Cancelación automática con Entregas por zona (H6).** Hoy un pedido de contado de una zona que
   no se cobra 2 días después del viaje **se cancela solo** y regresa el stock. ¿Así lo quieres, o
   que la cancelación automática sea **solo para "recoger en tienda"** y lo de zonas se maneje con
   "No se entregó" + motivo? (recomendado: solo recoger en tienda).
4. **"No se entregó".** Cuando se marca con su motivo, ¿el pedido sigue Pendiente para intentarlo
   otro día, o se cancela? (recomendado: sigue Pendiente con el motivo en su historial; cancelar es
   otro botón).
5. **Botón "Entrega" de la card (H10).** ¿Se queda para casos de un solo pedido (a domicilio,
   corregir un dato) o lo quito y todo se hace desde Entregas por zona? (recomendado: se queda,
   renombrado a **"Datos de entrega"**). Nota: hoy el **cliente** también lo puede editar en su propio
   pedido. ¿Está bien?
6. **Cambiar forma de cobro.** Escribiste "si el pedido es 1 solo no se puede cambiar a apartado
   porque no se puede unir con otro pedido". No me queda claro: un pedido solo, ¿sí se puede cambiar
   a Apartado / Ir pagando / Contado como hoy? Entiendo que lo nuevo es que **en un grupo se cambien
   todos juntos**. ¿Es así?
7. **Cómo llegar.** ¿Estás de acuerdo con las tres reglas de P7 (tienda → local, zona → punto de
   encuentro, domicilio → casa del cliente)? ¿O quieres que **siempre** lleve al local?

---

## 5. Orden de trabajo propuesto (cuando se cierren las dudas)

1. **Arreglos rápidos:** V1 (botón Contado), P5 (etiquetas de fecha), P7 (Cómo llegar).
2. **Estados y entrega:** P1, P2, P3, P4, V3, V4 + ❓1, ❓3.
3. **Devoluciones y dinero:** D1–D5.
4. **Grupos y unir:** P6, P8.
5. **Meses sin intereses:** V2.

Cada bloque: pruebas, `CAMBIOS_FRONT.md`, migraciones anotadas en `CLAUDE.md` (permisos nuevos →
hay que volver a iniciar sesión), y `dev` → `qa`.


---

## 6. Respuestas del dueño y decisiones (v3)

### 6.1 Cómo llegar (revisado otra vez en el código)
"Entregas por zona" **sí guarda** el punto de encuentro en cada pedido, y el detalle **sí lo usa**…
pero **solo si se marcó el punto en el mapa** (es opcional). Si solo se escribió el texto del punto
de encuentro, el detalle se salta ese texto y manda a **las coordenadas de la casa del cliente**
(las del checkout). Ese es el error. Además, "Entregas por zona" solo programa pedidos **Pendiente o
Apartado** hechos **dentro del rango de fechas** de la pantalla (no Ir pagando, no ramos).

Reglas nuevas del botón (en este orden):
1. Viaje programado con punto en el mapa → al punto.
2. Viaje programado con punto **escrito** sin mapa → buscar ese texto en Maps (hoy va a la casa del cliente 🐞).
3. **Recoge en el local** → a la ubicación del local (la misma del login). Si el local no tiene
   ubicación configurada → botón deshabilitado: "Configura la ubicación del local".
4. A domicilio → la dirección / coordenadas del cliente.

### 6.2 Cancelación automática — regla final
- **Solo** se cancelan solos los pedidos donde el cliente eligió **recoger en el local** con un día,
  y no pasó: a los X días (hoy 2, `pedidos.dias-limite-recogida`), regresa el stock.
- Los pedidos con viaje de **Entregas por zona** ya **no** se cancelan solos (hoy sí, los de contado 🐞).
- **Apartados** (por ejemplo de redes, "recoge en el local", con o sin fecha): **no** se cancelan
  solos; en pedidos se ve la **fecha de entrega** y **cuántos días lleva atrasado**, y el admin decide
  cancelar a mano. ❓ ¿Además cancelación automática después de muchos días? (ver 6.8)

### 6.3 Fechas en la card (P5)
- **"Pedido: 29 sep 2026"** (cuándo se hizo).
- **"Entrega: sáb 4 oct, 10:00 · Zacazonapan"** si ya tiene fecha, o **"Recoge en el local: …"**.
- Si la fecha ya pasó y no se entregó: **"⚠ Atrasado 5 días"**.

### 6.4 Fecha en la venta (V4)
Se **quita**, salvo cuando es **"recoge en el local"**: ahí queda **opcional** como "¿Qué día pasa por
él?" (caso redes sociales cuando el cliente sí dice el día). Para zona o domicilio la fecha se pone
después en Entregas por zona.

### 6.5 "No se entregó" (P3)
Sigue **Pendiente**, se guarda el motivo con fecha en su historial, y en pedidos se ve el atraso
(6.3). Cancelar es aparte (manual).

### 6.6 Botón "Entrega" de la card (H10) — cambios del cliente "por confirmar"
De dónde salen los datos de entrega hoy: (a) el **checkout** del cliente (lugar, dirección, día si
recoge en el local), (b) la **venta** del admin, (c) **Entregas por zona** (día, hora, punto), (d) este
botón (admin o el propio cliente).

Reglas nuevas para el **cliente**:
- **No** puede cambiar la zona ni la fecha/hora de un viaje programado (se entrega en grupo, fines de semana).
- Puede elegir **"Mejor paso al local"** con un día → queda **"Cambio del cliente — por confirmar"**,
  te llega **correo**, y tú lo **confirmas o rechazas**.
- Puede dejar una **nota** ("no estoy el sábado") → también te avisa por correo.
- El **admin** sigue pudiendo editar todo.

### 6.7 Unir pedidos con distinta forma de cobro (P6)
Ejemplo: 2 pedidos **Apartado** unidos y llega uno de **Ir pagando**. En la pantalla de unir, si las
formas no coinciden: elegir **a qué forma pasan todos** ("Pasar todos a Ir pagando y unir"). Si cambia
el grupo, cambian **todos sus pedidos**; al separarse **se quedan** con la forma nueva. Un pedido solo
se puede cambiar de forma de cobro como hoy.

### 6.8 Meses sin intereses (V2 ampliado)
- **Pantalla de configuración**: activar / desactivar cada plan (3, 6, 9, 12 MSI) cuando quiera, con su comisión.
- **Por artículo**: casilla **"Acepta meses sin intereses"**, **apagada por defecto**:
  - en el **producto** → aplica a **todos sus artículos**;
  - en cada **artículo** → para prenderla o apagarla solo en ese.
- En la venta, las opciones de MSI solo aparecen si **aplican** (ver ❓ abajo).
- Aprovechando: en las pantallas que se toquen, "variante" → **"artículo"** en los textos (regla de `CLAUDE.md`).

### 6.9 ❓ Dudas que quedan
1. **Apartados atrasados:** ¿solo cancelación manual, o también automática después de N días (¿cuántos?)?
   Recomendado: solo manual, porque casi siempre tienen anticipo y hay que decidir qué pasa con ese dinero.
2. **MSI con carrito mezclado:** si un artículo acepta MSI y otro no, ¿no se ofrece MSI en esa
   venta (recomendado), o se permite y solo se cobra a meses lo que sí acepta?
3. **MSI monto mínimo:** los bancos suelen pedir un mínimo de compra para meses. ¿Pongo un **mínimo
   por plan** (ej. 3 MSI desde $300), opcional?
4. **Cómo llegar en QA:** ¿con qué número de pedido lo probaste y marcaste el punto en el mapa? Para
   confirmar si fue el caso 6.1-2 o que el pedido no entró en la programación.


---

## 7. Respuestas del dueño (v4, 2026-09-30)

### 7.1 Apartados atrasados — ✅ decidido
- **Cancelación automática a los 10 días** de atraso (configurable, como `pedidos.dias-limite-recogida`),
  además de poder cancelarlo a mano antes.
- 🆕 **En la búsqueda de artículos (solo admin)**: si un artículo está apartado en pedidos, que salga
  aunque no tenga stock libre, con una marca **"En pedido #123 · 5 días sin entregar"** que lleve
  directo a ese pedido, para cancelarlo rápido si alguien más lo quiere. Sirve aunque no se recuerde en
  qué pedido está. El **cliente no ve** nada de esto: su tienda sigue igual.

### 7.2 MSI con carrito mezclado — ✅ decidido
Solo se ofrecen meses sin intereses si **todos** los artículos de la venta los aceptan. Si alguno no,
se muestra un aviso ("Este artículo no acepta meses sin intereses") y se paga en efectivo,
transferencia o tarjeta normal.

### 7.3 MSI monto mínimo — ✅ decidido
Mínimo de compra **$300** para ofrecer meses sin intereses (configurable en la pantalla de MSI), y
además **todos** los artículos tienen que aceptarlo (7.2).

### 7.4 "Cómo llegar" en QA — aclaración
La pregunta era solo para reproducir el error: con qué **número de pedido** se probó y si al programar
la entrega por zona se **marcó el punto en el mapa**. No bloquea: el arreglo de 6.1 cubre los dos casos.

---

## 8. 🆕 Guardar los filtros en la base (tienda y productos/buscar)

**Antecedente:** 2026-09-02 se pidió que `tienda/buscar` y `productos/buscar` recordaran los filtros. Se
hizo **solo en memoria** (`VarianteService.filtrosCache`, `ProductoService.prodFiltrosCache`): se
conservan al navegar dentro de la app, pero **se pierden al recargar, cerrar sesión o cambiar de
dispositivo**.

**Propuesta:**
- Tabla nueva `preferencia_filtro` (usuario, pantalla, filtros en JSON, fecha de actualización), dominio
  hexagonal nuevo. Anotarla en la lista de tablas de `CLAUDE.md` + migración.
- `GET /v1/preferencias-filtro/{pantalla}` al entrar a la pantalla; `PUT` (con espera corta) cada vez que
  se cambia un filtro; "Limpiar" la borra.
- Al entrar: primero la memoria (como hoy, instantáneo); si no hay, lo guardado en la base.

**❓ Dudas:**
1. ¿Por usuario (cada quien los suyos) para admin y empleados? (recomendado)
2. ¿Y los **clientes** en la tienda? Con sesión se podría guardar en la base; sin sesión solo en el
   navegador. ¿O solo para el admin?
3. ¿Qué se guarda: solo los **filtros** (casillas, talla, color, marca, precio, fechas) o también el
   **texto buscado** y la **página**? (recomendado: solo filtros)
4. ¿Solo esas dos pantallas, o también los filtros de **Mis pedidos** (estado, lugar)?


### 8.1 Filtros — ✅ decidido (2026-09-30)
- Solo para usuarios **con permisos** (admin / empleados), cada quien los suyos. Los **clientes no**: su
  tienda ya trae sus filtros por defecto.
- Se guardan **solo los filtros** (casillas, talla, color, marca, precio, fechas), **no** el texto buscado
  ni la página.
- Solo **tienda/buscar** y **productos/buscar**.

---

## 9. Plan final de trabajo (todo en `dev` → `qa`)

En cada archivo que se toque, los textos de pantalla y mensajes pasan de "variante" a **"artículo"**
(regla de `CLAUDE.md`; rutas, tablas, clases y campos del contrato se quedan).

| Bloque | Qué | Secciones |
|---|---|---|
| 1. Arreglos rápidos | Botón "Contado" en la venta · fechas explícitas y atraso en la card · "Cómo llegar" con las 4 reglas | V1, 6.3, 6.1 |
| 2. Filtros guardados | Tabla + endpoints + tienda y productos | 8, 8.1 |
| 3. Estados y entrega | "Ya pagó, falta entregar" (venta y pedido) · "Entregado" (también crédito y grupos) · "No se entregó" + motivo · fecha solo para "recoge en el local" · cancelación automática: recoger en local (2 días) y apartados (10 días), zonas ya no | P1–P4, V3, 6.2, 6.4, 6.5, 7.1 |
| 4. Devoluciones y dinero | Regresar productos bien/mal · cancelar con lista · Ir pagando "¿regresó?" · registrar devolución · registrar dinero devuelto | D1–D5 |
| 5. Grupos y cliente | Cambiar forma de cobro de todo el grupo al unir · buscador para unir · cambios del cliente "por confirmar" · artículos apartados en la búsqueda del admin | 6.6, 6.7, P8, 7.1 |
| 6. Meses sin intereses | Pantalla de planes + mínimo $300 · casilla por producto/artículo · regla de carrito | 6.8, 7.2, 7.3 |

**Avance (2026-09-29):** Bloque 1 ✅ en `dev` y `qa` (back y front). Bloque 2 ✅ código y pruebas
listos; `migration_preferencia_filtro.sql` ya corrida en qa y prod.

---

## 10. Reglas nuevas del dueño (2026-10-01)

Resumen de todas las reglas de cobro en `.claude/skills/reglas-pedidos/SKILL.md`; aquí solo lo nuevo.

| # | Regla | Estado |
|---|---|---|
| 10.1 | **Pago en línea:** el cliente paga con tarjeta desde su cuenta en la tienda; el pedido queda pagado pero **sin entregar**, y el cliente pone **a dónde** y **qué día** se lo llevan. Se junta con "Ya pagó, falta entregarlo" (H7 / V3) | 🔮 |
| 10.2 | **Apartado al recogerlo:** poder liquidarlo en el local con tarjeta y, si cumple las reglas de MSI (mínimo $300, todos los artículos lo aceptan), a meses | 🔮 (Bloque 6) |
| 10.3 | **Ir pagando nunca tiene MSI**, aunque el pedido cumpla las reglas | regla para el Bloque 6 |
| 10.4 | Apartado e Ir pagando hoy: **ni tarjeta ni MSI** (efectivo o transferencia) | ✅ así está |
| 10.5 | ✅ **Decidido: Apartado = sin dinero.** Es el pedido que el cliente hace por Facebook, live o mensaje y no ha dado nada. Si dio cualquier cosa (enganche, transferencia, un familiar que trajo $100) es **Ir pagando**. Reemplaza lo del §1 ("si deja anticipo, se anota"). Regla completa: skill `reglas-pedidos` 2.1 | 🆕 por programar (10.6) |

### 10.6 Qué hay que cambiar para "Apartado = sin dinero" (🆕, sin programar)

| # | Dónde | Cambio |
|---|---|---|
| A1 | Back, `AbonoServiceImpl.registrarAbono` | Rechazar abonos a un Apartado: *"Para dar un abono, cambia el pedido a Ir pagando"*. Cubre de un tiro el detalle, Créditos / Abonos, el enganche de la venta y "Abonar al grupo", porque todos pasan por ahí |
| A2 | Back, venta directa | Rechazar un Apartado con pago inicial mayor a 0 (por si el front no lo frena) |
| A3 | Back, `PedidoServiceImpl.cambiarTipoPedido` | No pasar a Apartado si se cobra algo en ese momento o si el pedido ya tiene abonos |
| A4 | Front, venta directa | Apartado + "Pago inicial (enganche)" mayor a 0 → aviso que no deja seguir, explica la regla y ofrece **Cambiar a Ir pagando** |
| A5 | Front, detalle del pedido y Créditos / Abonos | En un Apartado, "💳 Registrar abono": si el monto es **menor** al total, aviso *"Para dar un abono, cambia el pedido a Ir pagando"* con botón a **🔁 Cambiar forma de cobro**; si es **el total**, se acepta como pago completo. El cambio sale de "Monto recibido", como hoy |
| A6 | Front, pedidos unidos | Sin "💵 Abonar al grupo" en un grupo de Apartados |
| A7 | Datos en QA y prod | **QA:** script que pasa a Ir pagando los Apartados que ya tienen abonos o enganche (primero muestra la lista; probado antes de entregarlo). **Prod:** se dejan como están y se van resolviendo uno por uno |
| A8 | Cobrar al recogerlo | ✅ **Decidido 2026-10-01: se deja como está.** El Apartado se liquida en el formulario de abono de siempre (detalle o Créditos / Abonos) pagando el total; Cobrar sigue mandando a Créditos / Abonos. Un abono nunca es mayor a la deuda; el cambio sale de "Monto recibido" (efectivo). Transferencia siempre exacta |
| A9 | ❓ Venta: pagó todo pero no se lo lleva | Propuesta (skill 2.3): lo que paga decide la forma de cobro ($0 Apartado · parcial Ir pagando · total pagado) y, si pagó todo, pregunta obligatoria **"Ya se lo llevó"** / **"Falta entregarlo"**. Se junta con V3 / H7. Falta aprobación |
| A10 | Pedidos unidos | **Cambiar la forma de cobro del grupo entero** (p. ej. Apartados unidos que dejan un adelanto → todos a Ir pagando). Hoy está bloqueado dentro de un grupo (R9, H11) |

Consecuencias: cancelar un Apartado ya no deja "saldo a favor" (no tiene dinero), y la Prueba 5 de
`PRUEBAS_QA_2026-10-01.md` cambia en ese punto.
