# Subida del 2026-10-07 — Entregado aparte del pago, pantallas homologadas y diseño Jade

Paso a paso para llevarlo a **QA** (hoy) y después a **prod**. Cada paso dice quién lo hace y cómo
se comprueba. Lo que ya se hizo está marcado ✅.

## Qué sube

| Parte | Back (`proyecto_key`) | Front (`producto_venta_online`) |
|---|---|---|
| **Entregado aparte del pago** | Columna `pedidos.entregado`, `POST/DELETE /v1/pedidos/{id}/entrega`, `entregado` en listas y detalle, filtros Pago / Entrega, venta directa con `entregado`, cancelar Ir pagando que no se lo llevó regresa stock | Dos etiquetas en la card, 📦 Entregar y ↺, "¿Ya se lo llevó?" al cobrar, filtros Pago / Entrega |
| **Agregar artículo con stock del modelo** | `ajusteStockModelo` en `guardarConImagenes` | Stock total bloqueado + campo para agregar o quitar |
| **Gastos** | Permisos del administrador (script) | Texto sin permiso |
| **Pantallas homologadas** | — | Ancho de Agregar modelo, encabezados con el color de Personalización, tablas, selects, "Volver" |
| **Diseño Jade** | Script `migration_tema_jade.sql` (ya estaba en `dev`) | Jade ya estaba en `dev` desde el 2026-10-06 |
| **Hotfix de prod "Crear artículos"** | Se baja de `main` a `dev` y `qa` | — |

Detalle del contrato con el front: `CAMBIOS_FRONT.md`, **[BUG-KEY-19]**.

---

## Parte 1 — QA

| # | Paso | Quién | Cómo se comprueba | Estado |
|---|---|---|---|---|
| 1 | `migration_entrega_pedido.sql` en `inventario_key_qa` | Tú | 2 columnas; `entregar` = 21 y `regresar-entrega` = 22 con admin | ✅ 2026-10-07 |
| 2 | `migration_accion_gastos_admin.sql` en `inventario_key_qa` | Tú | 3 acciones con admin = 1 | ✅ 2026-10-07 |
| 3 | `migration_tema_jade.sql` en `inventario_key_qa` | Tú | `estilo` = jade; respaldo con 39 filas | ✅ 2026-10-07 |
| 4 | Back: commit en `dev`, bajar el hotfix de `main` (merge) y push a `dev` | Yo | `mvn compile` sin errores | ✅ |
| 5 | Back: merge `dev` → `qa` y push (dispara `producto-actions-qa.yml`) | Yo | GitHub Actions en verde | ✅ |
| 6 | Front: commit en `dev`, push, merge `dev` → `qa` y push (deploy del front de QA) | Yo | `ng build` sin errores y Actions en verde | ✅ 2026-10-07, tarde (front `dev` `f81b84c1`, `qa` `f45d2c57`; ver Incidente 2) |
| 7 | Cerrar sesión, volver a entrar y `Ctrl + Shift + R` | Tú | Ves las dos etiquetas en Mis pedidos | ⏳ |
| 8 | **Prueba 11** de `GUIA_DE_PRUEBAS_QA.md` (11.1 a 11.8) | Tú | Cada paso como dice la columna "Debes ver" | ⏳ |
| 9 | Lo que salga distinto: 💬 debajo del paso, con número de pedido | Tú | Yo contesto con ↳ | ⏳ |
| 10 | `migration_tema_filtros.sql` en `inventario_key_qa` (fondo de los filtros en Personalización) | Tú | La consulta del final da 2 filas | ✅ 2026-10-07 |
| 11 | Front: recuadro de filtros con fondo + encabezados de seguridad → `dev` → `qa` | Yo, cuando digas "sube" | `ng build` sin errores; `git log -1 origin/qa` con el commit; Actions en verde | ⏳ |
| 12 | **Prueba 13** de `GUIA_DE_PRUEBAS_QA.md` | Tú | Cada paso como dice "Debes ver" | ⏳ |

Volver a entrar es obligatorio: los permisos nuevos (Entregar, Regresar, Gastos) viajan dentro de la
sesión, y una sesión vieja no los trae.

---

## Parte 2 — Prod (cuando QA dé el visto bueno)

### Antes de empezar

0. **El back con `server.max-http-request-header-size: 64KB` tiene que estar en `main` antes de correr
   los scripts de permisos** (o en la misma subida, con el back primero). En QA, al correr los scripts,
   el token del admin pasó de 8 KB y Tomcat respondía 400 a todo (2026-10-07, ver "Incidente" abajo).
   Y en la VPS: `large_client_header_buffers 4 32k;` en el bloque `server` de
   `/etc/nginx/sites-available/backend`, luego `sudo nginx -t && sudo systemctl reload nginx`.
1. **Revisar qué trae `qa` que `main` no tiene:** `git log --oneline main..qa` (back y front). Si
   aparece algo que todavía no debe ir a prod (una feature bloqueada), no se hace merge completo: se
   llevan con `git cherry-pick` solo los commits de esta subida (regla de `CLAUDE.md`, "Feature que
   no va a llegar a main junto con el resto").
2. **Respaldo** de las tablas que tocan los scripts:
   ```bash
   mysqldump inventario_key pedidos accion_submenu rol_accion tema_variable > respaldo_2026-10-07.sql
   ```
3. Elegir una hora con poco movimiento: entre el paso 1 y el paso 5 hay 2–3 minutos en los que el
   back viejo convive con las columnas nuevas (no pasa nada: `entregado` nace en 0).

### Pasos

| # | Paso | Quién | Cómo se comprueba |
|---|---|---|---|
| 1 | `migration_entrega_pedido.sql` en `inventario_key` — **antes** del back | Tú | Las 3 consultas del final del script: 2 columnas; resumen por tipo y estado (Apartados abiertos y contados pendientes en 0); `entregar` = 21 y `regresar-entrega` = 22 con admin |
| 2 | `migration_accion_gastos_admin.sql` en `inventario_key` | Tú | 3 acciones con admin = 1 |
| 3 | `migration_tema_jade.sql` en `inventario_key` — solo si el front con Jade sube en esta misma vez | Tú | La consulta (a) da 59; `estilo` = jade; el respaldo tiene filas |
| 3b | `migration_tema_filtros.sql` en `inventario_key` — junto con el 3 | Tú | 2 filas |
| 4 | Back: merge `qa` → `main` y push (dispara `producto-actions.yml`) | Yo | Actions en verde; `kubectl logs` sin errores al arrancar |
| 5 | Front: merge `qa` → `master` y push | Yo | Actions en verde |
| 6 | Cerrar sesión, volver a entrar y `Ctrl + Shift + R` | Tú | — |
| 7 | Revisión rápida en prod (ver abajo) | Tú | Todo como en QA |
| 8 | Marcar los 3 scripts como corridos en prod en `CLAUDE.md` y vaciar "Pendiente para prod" | Yo | — |

### Revisión rápida en prod (10 minutos)

| # | Haz esto | Debes ver |
|---|---|---|
| 1 | **Mis pedidos** | Cada card con dos etiquetas: Pagado / Falta pagar y Entregado / Falta entregar. Totales iguales que antes |
| 2 | Un contado cobrado de hoy | Pagado + Entregado |
| 3 | Un Apartado abierto | Falta pagar + Falta entregar, sin 📦 |
| 4 | **⚙️ Filtros** → Pagado + Falta entregar | Sale la lista sin error |
| 5 | **Catálogo → 🧩 Agregar producto** → elige un modelo | Stock total del modelo bloqueado y el campo para agregar o quitar |
| 6 | **Gastos** | Botón para agregar gasto |
| 7 | Cualquier pantalla de formulario, de día y de noche | Mismo ancho y encabezado con el color de Personalización |
| 8 | **Tienda** de día | Título, búsqueda y filtros dentro de un recuadro blanco |
| 9 | `F12` → Red → la página → Encabezados de respuesta | `x-frame-options`, `strict-transport-security` y los demás de la Prueba 13.4 |
| 10 | En la VPS: `curl -sI http://shop.novedades-jade.com.mx/` | `301` a `https://` (S2 de `SEGURIDAD_DATOS.md` §6) |

### Si algo sale mal

| Qué | Cómo se regresa |
|---|---|
| El back nuevo falla | `git revert -m 1 <merge>` en `main` y push. Las columnas nuevas **no** le estorban al back viejo (`entregado` tiene default 0), así que el script no se deshace |
| El front nuevo falla | `git revert -m 1 <merge>` en `master` y push |
| No gusta el diseño Jade | Consultas "PARA VOLVER AL DISEÑO DE ANTES" al final de `migration_tema_jade.sql` (dejan las filas idénticas a antes, probado) |
| Los permisos nuevos | No estorban al back viejo; se pueden dejar |

---

## Incidente en QA del 2026-10-07 — 400 en todo después de correr los scripts

**Qué se vio:** al entrar a QA todo fallaba con `400 (Bad Request)` y "blocked by CORS policy" en la
consola. Prod funcionaba. `curl` desde la VPS al mismo endpoint daba 200.

**Causa:** el access token lleva todas las pantallas y acciones del usuario. Con las acciones nuevas de
los scripts, el del admin pasó de 8 KB, que es el límite de encabezados por default de Tomcat. Tomcat
rechazaba la petición antes de llegar a Spring (por eso sin cabecera CORS). Lo que salía sin token (los
colores al arrancar) daba 200; lo que salía con token, 400 de 435 bytes en `/var/log/nginx/access.log`.
No era el código ni un SQL mal hecho.

**Arreglo:** `server.max-http-request-header-size: 64KB` en `application.yml` (todos los ambientes).
Recomendado además: `large_client_header_buffers 4 32k;` en el nginx de la VPS (QA y prod).

### Cómo se resolvió (paso a paso, para repetirlo)

1. En el navegador: Herramientas → Red. Fallaba todo con 400 y "blocked by CORS policy".
2. En la VPS, `curl -i https://qa.backend.novedades-jade.com.mx/mis-productos/v1/tema-variable/activo`
   → 200. La app y el nginx estaban bien.
3. `sudo grep '" 400 ' /var/log/nginx/access.log | grep mis-productos | tail` → los 400 eran del
   navegador, todos de **435 bytes** (página de error de Tomcat) y solo en lo que salía **con token**;
   lo que salía sin token (`tema-variable/activo` al arrancar) daba 200. Eso apuntó al tamaño del token.
4. Back: `server.max-http-request-header-size: 64KB` en `application.yml`, commit `d96d5fc` en `dev`,
   merge a `qa` (`4dda53f`). Con el deploy, QA volvió a responder.
5. nginx de la VPS: al agregar `large_client_header_buffers 4 32k;`, `sudo nginx -t` marcó
   `"client_max_body_size" directive is duplicate in /etc/nginx/sites-enabled/backend:4` (prod):
   la línea `client_max_body_size 200M;` estaba **dos veces** en el mismo bloque `server`. Se dejó
   una sola, se agregó `large_client_header_buffers 4 32k;`, `sudo nginx -t` en verde y
   `sudo systemctl reload nginx`. **Ojo:** con un `nginx -t` en rojo, el reload no se aplica y nginx
   sigue con la configuración vieja, pero si el servidor se reinicia nginx no arranca y se cae prod.
   Nunca dejarlo así.

---

## Incidente 2 del 2026-10-07 — el front de QA no tenía los cambios del front

**Qué se vio:** en QA no aparecía el diseño Jade (todo beige, sin verde), Agregar modelo y Cargar
catálogo Excel con un corte vertical en el encabezado que tapa el texto (en celular no se lee), las
pantallas sin el ancho homologado y Clientes sin tabla.

**Causa:** el back sí subió a `qa`, el front no. El 2026-10-07 el front de QA seguía en
`7e672fbf` (hotfix de Mis datos del 2026-10-06):
- Diseño Jade: en `dev` desde el 2026-10-06 (`1d9a5206`), nunca se mergeó a `qa`.
- Entregado aparte del pago, stock del modelo y pantallas homologadas: commit `2acf56b0` solo local, sin push.
- Lo legal (pie de página, Términos, Aviso, registro, ticket, SEO): sin commit.
QA corría el back nuevo (con los colores Jade en `tema_variable`) con el front viejo (sin la capa
Jade): de ahí el beige sin verde. El corte del encabezado es el `.…__header-glow` del front viejo
(un círculo de 280 px a la derecha que queda **encima** del texto, porque el título tenía `z-index`
sin `position`).

**Cómo se resolvió:** commit del front en `dev` (`f81b84c1`), merge `dev` → `qa` (`f45d2c57`) y push;
el deploy de QA ya trae Jade, lo legal, las pantallas homologadas y el arreglo del brillo del
encabezado. Al validarlo salió que el recuadro de Tienda no tenía fondo: ver Prueba 13.

**Ojo con `origin/qa` local:** en el worktree del front, `origin/qa` se quedó en `7e672fbf` aunque
GitHub ya tenía `a76aac95`. Para saber qué hay de verdad en GitHub usar
`git ls-remote origin qa`, no solo `git log origin/qa`.

**Regla que sale de esto:** antes de marcar ✅ un paso de deploy, comprobar con
`git log -1 origin/qa` en **los dos** repos que el commit esperado está ahí, y que Actions del front
terminó en verde.

---

## Incidente 3 del 2026-10-07 — prod: 🧩 Productos del modelo respondía 400 "Intenta de nuevo"

**Qué se vio:** en **Catálogo → 🔍 Modelos → 🧩 Productos** ("maleta de viaje", stock 3), crear 2
artículos salía *"Error al crear variantes — Intenta de nuevo"*. Subirle stock al modelo no cambiaba nada.

**Cómo se encontró (para repetirlo):**
1. Herramientas del navegador → Red: `POST …/v1/variantes/inicializarDesdeProducto` → **400** sin mensaje.
2. `kubectl logs deployment/proyecto-key-deployment -n default --since=2h | grep -E " WARN | ERROR "`:
   **ninguna** línea de `ExceptionGlobal` a esa hora → la petición no llegó a la app.
3. `sudo grep "inicializarDesdeProducto" /var/log/nginx/access.log | tail -3` → `400 435` a las 19:48 y
   20:09 UTC. **435 bytes = página de error de Tomcat**: encabezados de más de 8 KB.
4. Se descartaron antes el stock (modelo 418 con 4 y artículos en 0) y la foto del modelo (tenía 1).

**Causa:** el mismo incidente que QA (arriba). El token del admin de prod creció con
`migration_accion_pedidos_filtros_y_cobro.sql` (corrida en prod el 2026-10-06) y `main` seguía con el
límite de Tomcat de 8 KB. Las demás pantallas pasaban por poco; la subida con fotos manda unos bytes
más de encabezado (`Content-Type: multipart/form-data; boundary=…`) y cruzaba el límite.

**Arreglo:** `server.max-http-request-header-size: 64KB` en el `application.yml` de `main` (las mismas
3 líneas que `d96d5fc` en dev/qa). El nginx de prod ya tenía `large_client_header_buffers 4 32k`.

**Subida:** back `main` `282a530` y front `master` `161c0d4f` (este además trae la ventana de 🧩
que dice "Puedes crear N" y no tira las fotos). GitHub Actions en verde, 2026-10-07 ~20:20 UTC.
**✅ Validado por el dueño en prod el 2026-10-07:** "con eso quedó" — ya se crean los artículos de
"maleta de viaje".

**Falta:** bajar el hotfix a `qa` y `dev` (el límite de 64 KB ya estaba ahí; baja lo de la ventana
de 🧩 y el mensaje del stock).

**Lección:** cuando un script de permisos se corre en prod, el back de `main` ya tiene que traer el
límite de 64 KB. Un 400 sin mensaje y sin línea en el log del back = revisar primero
`access.log` de nginx: si el tamaño es 435, es Tomcat rechazando encabezados.

