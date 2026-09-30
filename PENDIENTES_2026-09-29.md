# Pendientes y dudas — sesión del 2026-09-29

Todo lo que se preguntó o quedó abierto hoy, para no perderlo. Se tacha cuando se cierre.

---

## A. Pedidos unidos — saldo al separar ✅ (hecho, falta validar)

**Duda original:** uní 2 pedidos, di un abono de $200, al separarlos les dejé $100 a cada uno, pero
cada pedido seguía debiendo su total completo en lugar de descontar los $100.

**Qué era:** el back sí repartía bien los abonos (se comprobó con un test,
`SepararConAbonosJpaTest`); la pantalla solo mostraba el total, no lo pagado ni lo que falta.

**Qué se hizo** (en `dev` y `qa`, back y front):
- Card de pedidos: muestra "Pagado $X" y "Falta $Y".
- Detalle: el encabezado dice "Debe: $Y" (con total y pagado abajo).
- Separar: columna "Queda debiendo" por pedido mientras se escriben los montos.

- [ ] Validar en QA que se vea bien en los 3 lugares.
- [ ] Decidir cuándo sube a `main` (hoy solo está en `dev`/`qa`).

---

## B. Pedidos — quitar o cambiar un producto con abonos (en definición)

**Regla acordada:** cuando se quita o cambia un producto, la cuenta se hace **solo con ese pedido**.
Lo que sobre es de ese cliente; **no** se pasa a los otros pedidos del grupo.

**Cómo funciona hoy:** el total del pedido se recalcula y los abonos se quedan donde están. Eso está
bien. Lo que falta:

- [x] Si lo abonado ya cubre el nuevo total → el pedido pasa solo a **Pagado** (back, 2026-09-29).
- [ ] Si lo abonado es **más** que el total → mostrar **"Saldo a favor del cliente $X"** en el front
      (pendiente; el back ya deja el pedido Pagado).
- [x] **Bug corregido (back):** un pedido ya Pagado se podía editar; si se cambia un producto por uno más caro sigue
      diciendo Pagado aunque deba, y la venta registrada queda con los productos viejos. Debe
      regresar a Apartado / Ir pagando y borrarse esa venta.
- [x] El botón viejo de quitar producto ya no deja un pedido **sin productos**: pide cancelar.
- [x] **Decidido:** si regresan **todos** los productos de un pedido, no se quitan uno por uno:
      **se cancela el pedido**. El sistema no debe dejar quitar el último producto; debe decir
      "para regresar todo, cancela el pedido".

### Cancelar un pedido que está unido (cómo funciona hoy — no requiere cambios)

Caso: A, B y C están unidos y el cliente de C dice "ya no lo quiero".

- Se cancela **solo C**, con el botón de cancelar de ese pedido. A y B no se tocan.
- El grupo deja de contar a C: su total, lo que pagó y lo que debía ya no suman (regla R8 del
  grupo). Al separar, C tampoco entra al reparto.
- Los productos de C regresan al inventario (en "Ir pagando" no, porque la mercancía ya se la
  llevó; ahí lo que debía queda como deuda que no se cobró).
- Lo que C había abonado se queda registrado en C, y el mensaje de la cancelación dice
  **"Saldo a favor del cliente: $X"**: ese es el dinero a devolverle.
- **La devolución del dinero la haces tú** (efectivo o transferencia). El sistema la avisa, pero
  hasta donde se revisó no registra la salida del dinero.
- Si el cliente no quiere que le devuelvan el dinero sino dejarlo para otra compra, ya existe
  **transferir el abono a un pedido nuevo**.
- Si un pedido ya estaba **Pagado**, cancelarlo es una devolución: solo lo puede hacer un
  administrador.
- Ojo: si el que cancela es el **titular** (el que paga y recoge), primero cambiar "quién recoge"
  a otro pedido del grupo.

- [ ] Revisar si hace falta registrar la salida del dinero devuelto (para que el corte cuadre).

Ejemplo acordado (A, B, C con 2 productos de $100 cada uno):

| Paso | A | B | C |
|---|---|---|---|
| Unidos, dan $150, se separan con $50 c/u y se vuelven a unir | debe 150 | debe 150 | debe 150 |
| C regresa 1 producto ($100) | debe 150 | debe 150 | total 100, **debe 50** |
| Siguen pagando $300 | **Pagado** | **Pagado** | debe 50 |
| C paga sus $50 | Pagado | Pagado | **Pagado** |

Solo hay "saldo a favor" si a un pedido le quitan más de lo que le falta pagar. Ejemplo: A ya pagó
$200 y regresa un producto de $100 → A queda en $100 con $200 pagados → se le deben $100 a ese cliente.

---

## C. Dominio, DNS y correo (en investigación)

Detalle completo en `DOMINIO_DNS_CORREO.md`.

**Duda original:** Hosting-Mexico cobra el alojamiento (orden 609155); ¿qué es eso?, ¿no era el
dominio? ¿Puedo quedarme con el dominio y la VPS (shop, backend, QA) y cambiar solo el correo a un
proveedor más barato?

**Lo que ya se sabe:**
- El dominio ya se renovó. Lo que falta pagar es el alojamiento, donde viven los buzones de correo
  (el primer año venía en la compra).
- La tienda, el backend y QA están en la VPS, no en Hosting-Mexico.
- **El DNS vive dentro del alojamiento** (`ns1-hapi`/`ns2-hapi`). Si se deja vencer sin más, lo más
  probable es que también se caiga la tienda.
- Sí se puede quedarse con dominio + VPS y cambiar solo el correo, pero primero hay que sacar el DNS
  del alojamiento (sección 6 del documento).

- [ ] Enviar la pregunta a Hosting-Mexico (texto listo en la sección 7) y pegar la respuesta.
- [ ] Paso 2 en la VPS: respaldo de todos los registros DNS.
- [ ] Revisar el panel de Hosting-Mexico: precios, vencimientos, buzones, exportar zona DNS.
- [ ] Comparar precios de correo y decidir.
- [x] Respuesta de Hosting-Mexico: sin hospedaje se cae todo; fecha límite de pago **4-oct-2026**.
- [x] **Decisión: se paga la renovación** y durante el año se prepara la salida (plan en la sección
      10 de `DOMINIO_DNS_CORREO.md`).
- [ ] **Pagar la orden 609155 antes del 4-oct-2026.**

---

## D. Otros pendientes que ya estaban

- [x] Correr `backfill_estado_pedido_tipo.sql` en prod (`inventario_key`) antes de subir a `main`
      la corrección de `cambiarTipoPedido` — corrida 2026-09-30, 0 filas desfasadas.

---

## E. TikTok — la app de Production volvió a salir rechazada (2026-09-29)

Historial completo (capturas 60–66, primer rechazo por el ícono, guion del video, guías de revisión):
sección **4.11** de `ALTA_NEGOCIO_META_TIKTOK.md` en la rama `claude/admin-negocio-horario-redes-iss9wb`.

**App:** `novedadesJade` · id `7675270500029089812` · developers.tiktok.com
**Productos:** Login Kit + Content Posting API (solo "Upload", Direct Post apagado) · **Scopes:**
`user.info.basic`, `video.upload` · **Sandbox:** target user `novedadesjade8`.

### Lo que dice el formulario de Production que pegó el dueño hoy

| Campo | Qué tiene | ¿Bien? |
|---|---|---|
| Nombre / categoría | `novedadesJade` / Others | ✅ |
| Descripción | 105/120 → parece la nueva ("Tienda Novedades Jade: la dueña publica…") | ✅ (confirmar) |
| Ícono | no se ve en el texto pegado | ❓ confirmar que sea el logo "NOVEDADES BOLSAS JADE", no el viejo "JADA" |
| Términos / Privacidad | `/termConditions` y `/privacidad` de shop | ✅ existen y son públicas |
| Web URL | `https://shop.novedades-jade.com.mx` | ✅ |
| Redirect URI | `https://shop.novedades-jade.com.mx/tiktok/callback` | ✅ |
| **Explicación de App review** | **68/1000** — es la explicación vieja y corta | ❌ **casi seguro motivo de rechazo** |
| **Video demo** | **`videoTikTokPrueba.mp4`** — el video viejo | ❌ **casi seguro motivo de rechazo** |

La bitácora del 2026-09-24 ya decía que para reenviar había que **cambiar la explicación y el video**.
En Production siguen los de antes, así que el reenvío salió con lo mismo que ya habían rechazado.

### Lo que falta, en orden

- [ ] **1. Copiar el motivo exacto del rechazo** ("See why" / correo de TikTok) y pegarlo aquí. Sin eso
      solo se puede suponer.
- [ ] **2. Pie de página con enlaces visibles en la tienda.** La guía de TikTok dice que Privacidad y
      Términos deben verse en la página principal **sin abrir un menú**. Hoy la tienda no tiene pie de
      página (revisado en `master`, `qa` y `dev` del front): solo existen las rutas. Agregar en todas las
      páginas públicas: **Aviso de privacidad · Términos y condiciones · Eliminar mis datos**. Tiene que
      estar en **producción** (`shop.`), porque esa es la URL que revisan.
- [ ] **3. Poder publicar en TikTok desde producción.** El video tiene que mostrar el dominio
      `shop.novedades-jade.com.mx`, pero la cuenta autorizada y sus tokens están solo en la base de
      **QA**. En prod falta cargar `TIKTOK_CLIENT_KEY`/`TIKTOK_CLIENT_SECRET` (del **Sandbox**), confirmar
      que exista la tabla `tiktok_token` y autorizar `novedadesjade8` ahí.
- [ ] **4. Confirmar que el video subido llega a TikTok.** Llega como **notificación en la Bandeja de
      entrada** de `novedadesjade8`, no en Borradores. Hay que verlo antes de grabar, porque el video
      demo tiene que terminar mostrándolo.
- [ ] **5. Grabar el video nuevo** con el guion de la bitácora (4.11): autorizar con TikTok → regresa a
      `shop…/tiktok/callback` → "Cuenta conectada" → Publicar en redes → TikTok → abrir TikTok y mostrar
      la notificación/borrador. Tiene que verse cada scope (`user.info.basic`: la cuenta conectada;
      `video.upload`: el video subido como borrador).
- [ ] **6. Explicación nueva en inglés** (hasta 1000 caracteres), que diga qué hace cada producto y
      scope, y **no** decir "uso personal" ni "solo la dueña": es una herramienta de la tienda para su
      cuenta de negocio (la guía dice que no aprueban apps de uso privado).
- [ ] **7. En Production:** Return to Draft → ícono y descripción iguales al Sandbox → pegar la
      explicación nueva → **quitar `videoTikTokPrueba.mp4`** y subir el nuevo → Submit for review.

### Borrador de la explicación (para pegar en "App review", revisar antes)

> Novedades Jade (https://shop.novedades-jade.com.mx) is an online store that sells handbags,
> perfumes and clothing. Store staff with the admin role use the store's back office to share
> product videos to the store's TikTok business account.
>
> Login Kit / user.info.basic: an admin connects the store's TikTok account from the back office.
> TikTok redirects to https://shop.novedades-jade.com.mx/tiktok/callback and we read the account's
> open id, display name and avatar to show which TikTok account is connected.
>
> Content Posting API / video.upload: from "Publicar en redes", the admin picks a product video and
> sends it to TikTok with the upload (draft) flow (FILE_UPLOAD). The creator receives an inbox
> notification in TikTok to finish editing and post it. We do not use Direct Post and we never post
> without the creator's action.
>
> The demo video shows the full flow in the Sandbox with the target user novedadesjade8.

(Ajustar el texto a lo que de verdad se vea en el video.)

### Qué video subir a TikTok (guion)

Un solo video, **grabado en el Sandbox**, que muestre el flujo completo en
`https://shop.novedades-jade.com.mx` (el mismo dominio que se da de alta en la app). MP4 o MOV,
máximo 50 MB, 1080p, 1–3 minutos, sin cortes que escondan pasos.

Antes de grabar: el pie de página ya en producción; en el navegador, sesión de TikTok con
`novedadesjade8`; la URL de autorización armada con el **Client key del Sandbox**; **nunca** mostrar
el Client secret.

1. **Abrir la tienda** `shop.novedades-jade.com.mx`: que se vean el logo en la pestaña, el nombre y
   el pie de página con Privacidad y Términos (bajar hasta el pie y abrir los dos enlaces).
2. **Entrar al panel** con un usuario administrador.
3. **Conectar TikTok (Login Kit):** abrir la URL de autorización → pantalla de TikTok con el ícono
   de la app y los permisos (`user.info.basic`, `video.upload`) → **Autorizar**.
4. Regresa a `shop.novedades-jade.com.mx/tiktok/callback` → "Cuenta de TikTok conectada", mostrando
   **qué cuenta** quedó conectada (nombre o avatar = `user.info.basic`).
5. **Publicar en redes → TikTok:** elegir un producto y su video → Publicar. Que se vea el mensaje
   de que TikTok lo recibe como borrador.
6. **Abrir TikTok** (web o celular) con `novedadesjade8` → **Bandeja de entrada** → notificación
   "tu video está listo" → abrirla y ver el video en el editor (`video.upload`). Si es en celular,
   que se vea cómo se abre la app.
7. Terminar ahí (no hace falta publicarlo).

Referencia de lo legal que se revisó para el sitio: `CUMPLIMIENTO_LEGAL_TIENDA.md`.
