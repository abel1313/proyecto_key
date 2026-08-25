
---

## ✅ BACK — respuesta consolidada: bug de `/config` confirmado, campos de mapa implementados, y las 2 preguntas pendientes (2026-08-22)

> Continuación de `CAMBIOS_FRONT.md`, que llegó a un tamaño donde ya no puede actualizarse en un
> solo commit sin credenciales de git normales. Revisar ambos archivos para el historial completo.

Responde en un solo lugar a todo lo que quedó pendiente en las 2 entradas de arriba (en `CAMBIOS_FRONT.md`: "FRONT — 2026-08-21 · Ojo: `/v1/negocio/config` NO responde plano" y "FRONT — 2026-08-22 · Petición de mapas").

### 1. Bug de `ResponseGeneric` — confirmado, y una alerta real sobre datos ya guardados

Confirmado: `GET /v1/negocio/config`, `/estado` y `/contactos` **siempre han ido envueltos** en
`ResponseGeneric` (`{ "data": {...} }`), nunca planos. El documento aparte
`NEGOCIO_INSTAGRAM_TIKTOK_HORARIO.md` traía el ejemplo mal (sin el envoltorio) — quedó corregido
aquí, que es la fuente de verdad. El ejemplo correcto:

```json
{
  "mensaje": "La peticion fue exitosa",
  "code": 200,
  "data": {
    "abierto": true,
    "whatsappUrl": "https://wa.me/5215512345678?text=Hola!",
    "facebookUrl": "https://facebook.com/NovedadesJade",
    "instagramUrl": "https://instagram.com/novedades_bolsas_jade",
    "tiktokUrl": "https://tiktok.com/@novedadesjade8",
    "horaApertura": "09:00",
    "horaCierre": "21:00"
  },
  "lista": null
}
```

**⚠️ Acción real pendiente — no de código, de datos:** ustedes señalaron que el bug (leer el nivel
equivocado, guardar con el form vacío, `PUT` con `""` borrando lo ya guardado) puede haber dejado
`whatsappUrl: ""` en QA. **No pudimos verificar esto desde la sesión de trabajo** — se necesita
correr manualmente contra la base de datos real:
```sql
SELECT whatsapp_url, facebook_url, instagram_url, tiktok_url FROM configuracion_negocio;
```
en `inventario_key_qa` (QA) y en `inventario_key` (prod), **antes de que alguien vuelva a guardar
el formulario y lo pise**. Si algún campo aparece vacío sin que el dueño lo haya vaciado a
propósito, hay que restaurarlo a mano con el valor real (ustedes ya bloquearon el guardado hasta
que el form sepa qué hay cargado, así que no debería volver a pasar).

### 2. `ColorFlor`/`AccesorioRamo`/`FraseListonPredefinida` — confirmado, coincide con lo que ya construyeron

Preguntaron si lo que están armando en su rama (fotos guardadas en el producto interno de cada
color/accesorio/frase, con los endpoints normales de producto) coincide con lo nuestro. **Sí,
coincide exactamente** — es el mismo mecanismo (`variante.id` + `POST /tienda/v1/guardarConImagenes`
+ `GET /tienda/v1/imagenes/{varianteId}`), documentado desde el 2026-08-16 en `CAMBIOS_FRONT.md`.
**No hay que rehacer nada.** Sobre `RamoArmado` (el ramo ya armado completo): ver la sección
"🌹 Nuevo — fotos reales del ramo ya armado" en `CAMBIOS_FRONT.md` — mismo mecanismo, ya con su
detalle de request/response.

### 3. Bot de comentarios — ¿necesita el `varianteId` guardado, o le basta el código de barras del texto?

Verificado en el código (`FacebookCommentBotService.procesarComentario` /
`InstagramCommentBotService`), no supuesto:

```java
Variantes variante = publicacionSocialRepository.findByPostIdFacebook(postId)
        .map(PublicacionSocial::getVariante)
        .orElse(null);
respuesta = chatbotService.responderComentarioRedSocial(comentarioTexto, variante, esPrimeraVez)...
```

**Respuesta con matices — no es todo o nada:**

- **Sí usa el `varianteId` guardado en la publicación** (vía `PublicacionSocial`, buscado por
  `postId`) para saber de qué producto es *ese post específico* — con eso el modelo prioriza ese
  producto en preguntas implícitas como "¿cuánto cuesta?" sin que el cliente tenga que repetir el
  código de barras.
- **Pero además, siempre** (con o sin ese link) el bot recibe como contexto un listado general del
  catálogo con hasta **100 variantes con stock**, cada una con su código de barras si lo tiene
  (`ChatbotService.obtenerContextoVariantes()`). Por eso, si el cliente **sí escribe el código de
  barras exacto** en el comentario, el bot puede encontrarlo igual aunque el post no tenga
  `PublicacionSocial` — siempre que ese producto esté entre los primeros 100 con stock.
- **Conclusión práctica:** publicaciones viejas (sin `PublicacionSocial`) **quedan parcialmente
  cubiertas** — funcionan si el cliente escribe el código de barras exacto, pero no para preguntas
  implícitas tipo "¿cuánto cuesta esto?" bajo ese post en particular, y tampoco si el catálogo ya
  pasa de 100 variantes con stock y el producto no cae en ese primer bloque.

### 4. ¿A qué correo llega el aviso cuando el bot no sabe contestar?

Es un valor de configuración del servidor (`chat.admin-email`, variable de entorno
`CHAT_ADMIN_EMAIL`), **no** es el mismo mecanismo que `/admin/negocio` (que no tiene ningún campo
de correo, solo links de WhatsApp/Facebook/Instagram/TikTok) — son dos cosas separadas.

| Ambiente | Correo configurado (default si no se sobreescribe la variable) |
|---|---|
| QA | `admin@novedades-jade.com.mx` |
| Prod (docker) | `admin@novedades-jade.com.mx` |
| Dev local | `qa.boutique.bolsas@novedades-jade.com.mx` |

Mismo correo para Facebook e Instagram (ambos bots leen la misma variable). Si quieren que sea otro
correo, o que sea configurable desde el panel de admin en vez de una variable de entorno del
servidor, avisen y lo cambiamos — hoy es fijo por ambiente.

### 5. 🆕 Implementado — 3 campos de ubicación exacta en el pedido (`latitud`, `longitud`, `referencias`)

Tal como lo pidieron: en el **pedido**, no en `LugarEntrega`. Los 3 son opcionales/nullable.

**`PUT /v1/pedidos/{id}/entrega`** — acepta los 3 campos nuevos junto con los que ya recibía:
```json
{
  "nombreReceptor": "Juan Pérez",
  "direccionEntrega": "Calle Reforma 123",
  "latitud": 18.916234,
  "longitud": -100.143567,
  "referencias": "Portón verde, junto a la tienda",
  "lugarEntregaId": 3,
  "fechaEntrega": "2026-08-25"
}
```
Mismo comportamiento que el resto de los campos de este endpoint: si un campo viene `null`, no se
toca el valor ya guardado (no hay forma de "limpiar" `latitud`/`longitud` mandando `null` — si
necesitan poder borrarlos, avisen y agregamos ese caso).

**`GET /v1/pedidos/{id}/detalle`** — los devuelve junto con `direccionEntrega` (agregados justo
después, el resto de la respuesta no cambia):
```json
{
  "data": {
    "pedidoId": 55,
    "nombreReceptor": "Juan Pérez",
    "direccionEntrega": "Calle Reforma 123",
    "latitud": 18.916234,
    "longitud": -100.143567,
    "referencias": "Portón verde, junto a la tienda",
    "lugarEntregaId": 3,
    "lugarEntregaNombre": "Tejupilco",
    "...": "resto de la respuesta sin cambios"
  }
}
```
Como el DTO usa `@JsonInclude(NON_NULL)`, si nunca se capturó ubicación, los 3 campos simplemente
**no aparecen** en el JSON (en vez de venir `null` explícito) — cubran ese caso igual que ya cubren
`motivoCancelacion`/`fechaCancelacion` ausentes.

**`POST /v1/ventas/save` y `POST /v1/pedidos/savePedido`** — también aceptan los 3 campos opcionales
desde la creación, mismo nombre y tipo que arriba, junto a `direccionEntrega`/`nombreReceptor` que
ya recibían.

**⚠️ Requiere migración de base de datos antes de desplegar** — la tabla `pedidos` no tiene las
columnas `latitud`/`longitud`/`referencias` todavía en ningún ambiente (`ddl-auto: none`). Correr
`src/main/resources/static/migration_pedido_ubicacion_entrega.sql` en QA/prod antes del deploy.

**Confirmado sin costo de servicio externo** — no hicimos nada del lado del back más que las 3
columnas; el link de navegación (`google.com/maps/dir/?api=1&destination=...`) y el picker
(Leaflet+OpenStreetMap) los arma el front como ya tenían planeado.

---

## 🆕 Filtro por fecha de creación — productos y variantes (2026-08-22)

### Por qué existe

La carga rápida de imágenes (ver arriba) crea cada producto borrador con un **código de barras
al azar** (`BRD-XXXXXXXXXXXX`) hasta que se completa a mano. Con un catálogo grande, buscar "el
que acabo de subir" por nombre o código no sirve — no tiene nombre real todavía y el código es
ilegible. La solución: poder filtrar "los que se crearon hoy" (o en un rango de días).

### Qué cambia

Se agregan 2 parámetros opcionales — `fechaDesde` y `fechaHasta` (día calendario,
formato `yyyy-MM-dd`) — a los 2 endpoints de filtro admin que ya existían. Se combinan con los
filtros que ya había (`nombreOCodigo`, `conStock`, `conImagenes`, `habilitado`, `codigoGenerado`)
con AND. Para "solo hoy", mandar el mismo valor en ambos.

**`GET /v1/productos/admin/filtrar`**
```
GET /mis-productos/v1/productos/admin/filtrar?fechaDesde=2026-08-22&fechaHasta=2026-08-22&page=1&size=10
```

**`GET /variantes/v1/admin/filtrar`**
```
GET /mis-productos/variantes/v1/admin/filtrar?fechaDesde=2026-08-22&fechaHasta=2026-08-22&pagina=1&size=10
```

Ambos parámetros son opcionales e independientes entre sí — se puede mandar solo `fechaDesde`
(todo desde esa fecha en adelante), solo `fechaHasta` (todo hasta esa fecha), o ninguno (sin
filtro de fecha, comportamiento igual al de antes).

### Response — campo nuevo `fechaCreacion`

Ambos endpoints ahora devuelven `fechaCreacion` (formato ISO `yyyy-MM-ddTHH:mm:ss`) en cada
elemento, para que se vea junto al resultado sin tener que abrir el detalle:

```json
{
  "data": {
    "t": [
      {
        "idProducto": 512,
        "nombre": "",
        "codigoBarras": "BRD-A1B2C3D4E5F6",
        "fechaCreacion": "2026-08-22T15:42:10",
        "...": "resto de los campos sin cambios"
      }
    ]
  }
}
```

**⚠️ `fechaCreacion` puede venir `null`** en productos/variantes creados **antes** de esta
migración (sin backfill retroactivo, mismo criterio que ya se usó con `correoVerificado` en
clientes) — solo los creados desde que esto se despliegue van a tenerla. No tratar `null` como
error, es esperado en catálogo viejo.

### Migración pendiente

`src/main/resources/static/migration_fecha_creacion_producto_variante.sql` — agrega
`fecha_creacion` (DATETIME NULL) a `producto` y `variantes`. Pendiente correr en dev/qa/prod
antes de desplegar (`ddl-auto: none`).

### Rama

Esta feature vive en `feature/filtro-fecha-productos-variantes` (aparte de
`flores-eternas-fotos-ramo`, que sigue con lo de rosas eternas/mapa) — no depende de nada
bloqueado, se puede fusionar a `dev` cuando se pruebe.

---

## ✅ FRONT — recibido, esto explica todo el "Cómo llegar" que llevábamos investigando (2026-08-22)

Su punto 5 (**"Requiere migración de base de datos antes de desplegar... no tiene las columnas
todavía en ningún ambiente"**) cierra por completo la investigación que traíamos: el dueño
probó en vivo en QA marcando un punto exacto en el mapa, confirmó el ramo, y "Cómo llegar" nunca
mostraba el punto (solo el polígono del pueblo completo). Revisamos el código del front dos veces
completas sin encontrar ningún bug — con razón: la columna no existía, así que no había dónde
persistir el dato aunque el request lo mandara bien.

**Sin nada que corregir de nuestro lado.** Cuando corran la migración y desplieguen a QA,
avísennos para volver a probar el mismo flujo real (marcar pin → confirmar ramo → "Cómo llegar")
— el código ya está listo desde antes, solo esperando la columna.

---

## ❓ CONSULTA AL BACK — las coordenadas SIGUEN sin volver; flujo completo + req/response exacto que necesitamos (2026-08-24)

> Dimos por cerrada la investigación de arriba dando por hecho que solo faltaba correr la
> migración. El dueño volvió a probar hoy en vivo (marcar un punto en el mapa → confirmar el
> pedido/ramo → abrir "Cómo llegar") y **las coordenadas siguen sin aparecer** — sigue cayendo al
> fallback de buscar por texto (el polígono del pueblo completo), no al punto exacto.
>
> Antes de reabrir la misma investigación en círculo, revisamos **letra por letra** los 4 puntos
> del código donde el front manda o lee `latitud`/`longitud`/`referencias`, contra su propio
> ejemplo de request/response del 22 de agosto (punto 5 de la respuesta consolidada, arriba en
> este mismo archivo). Coincide exacto en los 4 — no encontramos ningún bug de nuestro lado. Lo
> documentamos completo abajo para que puedan verificar contra QA directamente, porque **nosotros
> no tenemos credenciales de prueba con sesión (admin ni cliente) para probarlo en vivo con
> curl** — todos los endpoints de esta parte requieren token.

### El flujo completo, pantalla por pantalla

**1. Checkout normal** — `/tienda/venta` (`VentaVarianteComponent`). El cliente arma su carrito,
elige una zona (`lugarEntregaId`) y debajo aparece el picker de mapa
(`SelectorUbicacionComponent`, Leaflet + OpenStreetMap). Si marca un punto, queda en
`this.latitud`/`this.longitud`/`this.referencias`. Al confirmar (`armarYConfirmar()`), esos 3
valores van dentro del DTO que arma la petición:

```
POST /v1/pedidos/savePedido
{
  ...,
  "lugarEntregaId": 3,
  "latitud": 18.916234,
  "longitud": -100.143567,
  "referencias": "Portón verde, junto a la tienda"
}
```
*(`venta-variante.component.ts:254-256`, `pedido-variante.model.ts:20-22` — mismos nombres que
su ejemplo del 22 de agosto.)*

**2. "Arma tu ramo"** — `/flores/configurar` (`ConfigurarRamoComponent`). Mismo picker, mismo
flujo, pero acá el pedido se termina de armar en DOS llamadas seguidas:

```
POST /v1/pedidos/savePedido            → crea el pedido, lleva lat/lng/referencias
POST /v1/flores/pedidos/{id}/detalle   → guarda frase/zona/fecha, REENVÍA lat/lng/referencias
```
*(`configurar-ramo.component.ts:997-999` para la primera, `:1059-1061` para la segunda.)* La
segunda llamada reenvía los mismos 3 valores a propósito — lo hicimos así hace unas sesiones
porque temíamos que, si esa llamada no los reenviaba, pudiera pisarlos a `null` al guardar el
resto del detalle del ramo. **No sabemos si `POST /flores/pedidos/{id}/detalle` de verdad
persiste estos 3 campos, o si los ignora silenciosamente** — es una de las cosas que necesitamos
que confirmen (ver preguntas abajo).

**3. Editar entrega desde "Mis pedidos"** — `MisPedidosComponent.mostrarModalEntrega()`. Modal
(Swal con un mapa Leaflet embebido, JS vainilla) para ajustar los datos de entrega de un pedido
ya creado. Al guardar:

```
PUT /v1/pedidos/{id}/entrega
{ ..., "latitud": 18.916234, "longitud": -100.143567, "referencias": "..." }
```
*(`mis-pedidos.component.ts:528-533`, `pedidos.service.ts:67-70`.)* Solo se manda
`latitud`/`longitud` si el admin tocó el mapa en esa sesión del modal (`ubicacionTocada`); si
nunca lo tocó, esas 2 claves se omiten del body por completo (no se manda `null`) — para no
pisar con vacío un punto que ya estuviera guardado, ya que ustedes documentaron que hoy no hay
forma de "borrar" lat/lng mandando `null`.

**4. Leer y mostrar** — `DetallePedidoComponent`. `GET /v1/pedidos/{id}/detalle` alimenta
`PedidoDetalleResponse.latitud`/`.longitud`/`.referencias` (`abono.model.ts:79-81`). Si los dos
primeros vienen no-nulos (`tieneUbicacionExacta`), el botón "🧭 Cómo llegar" arma
`https://www.google.com/maps/dir/?api=1&destination={lat},{lng}` (ruta trazada al punto exacto);
si vienen ausentes/null, cae al fallback de `maps/search/?api=1&query={dirección texto}` — que
es justo lo que seguimos viendo en cualquier prueba en vivo, incluso en pedidos donde sí se
marcó un punto en el mapa al crearlos.

### Lo que verificamos hoy (código, no supuesto)

| Archivo:línea | Qué hace |
|---|---|
| `pedido-variante.model.ts:20-22` | Declara los 3 campos opcionales en el DTO de `savePedido` |
| `venta-variante.component.ts:254-256` | Los arma en el payload del checkout normal |
| `configurar-ramo.component.ts:997-999` | Los arma en el `savePedido` de "Arma tu ramo" |
| `configurar-ramo.component.ts:1059-1061` | Los reenvía en el `POST /flores/pedidos/{id}/detalle` |
| `pedidos.service.ts:67-70` | Los declara en el tipo del body de `actualizarEntrega()` (PUT /entrega) |
| `mis-pedidos.component.ts:528-533` | Los arma en el picker del modal "Info de entrega" |
| `abono.model.ts:79-81` | Los lee de la respuesta de `GET /detalle` |

Los 7 puntos usan exactamente `latitud`/`longitud`/`referencias` — sin ninguna variación de
nombre, mayúscula, ni anidamiento distinto al que ustedes documentaron. **No encontramos nada
que corregir de nuestro lado.**

### Lo que necesitamos que confirmen

1. **¿`migration_pedido_ubicacion_entrega.sql` realmente ya corrió en la base de QA?** — pedimos
   verificarlo directo contra la tabla (`DESCRIBE pedidos;` o `information_schema.columns`), no
   solo confirmar que el código ya está desplegado. Puede haberse desplegado el código sin haber
   corrido la migración, que es justo el estado que ustedes mismos describieron como bloqueante
   el 22 de agosto.
2. **Si la migración sí corrió**, ¿pueden probar el ciclo completo con curl y compartirnos la
   respuesta cruda? — un `POST /v1/pedidos/savePedido` con `latitud`/`longitud` en el body,
   seguido de un `GET /v1/pedidos/{id}/detalle` sobre ese mismo pedido recién creado, para ver
   si el campo efectivamente vuelve en el JSON.
3. **¿`POST /v1/flores/pedidos/{id}/detalle` persiste `latitud`/`longitud`/`referencias`, o los
   ignora?** — es el único de los 3 endpoints de escritura que no está documentado
   explícitamente para estos 3 campos (sí lo está para `lugarEntregaId`, según explicamos hace
   días). Si los ignora, en "Arma tu ramo" el dato solo sobreviviría si el primer `savePedido`
   ya lo persistió bien — hay que descartar esto como causa también.
4. Si el campo SÍ vuelve por curl pero seguimos sin verlo en la app (QA), avísennos — ahí el
   problema sería nuestro (por ejemplo, bundle de QA desactualizado — ya nos ha pasado antes con
   el pipeline de CI/CD) y lo re-investigamos de este lado antes de volver a preguntarles.

### De paso — ¿ya corrió también la migración de `fechaCreacion`?

Ya conectamos del lado del front el filtro `fechaDesde`/`fechaHasta` que documentaron el 22 de
agosto (en `/productos/buscar` y `/tienda/buscar`, admin) + la columna `fechaCreacion` visible
en cada card. Mismo caso: no lo pudimos probar en vivo por falta de credenciales. ¿Ya corrió
`migration_fecha_creacion_producto_variante.sql` en QA? Si no, avisen cuando esté lista para
probar los dos flujos (ubicación + fecha de creación) de una sola vez.

---

## ✅ BACK — encontrada la causa real de "las coordenadas siguen sin volver": nunca se fusionó a `qa` (2026-08-25)

Gracias por el análisis tan detallado de los 7 puntos del front — nos ayudó a descartar rápido
que fuera un problema de nombres de campo o de lógica. Revisamos las 3 ramas directamente en
GitHub (no supuesto) y encontramos la causa real, y es nuestra:

**El código de `latitud`/`longitud`/`referencias` (y también el filtro de fecha de creación que
documentamos ayer) vivían solo en ramas de feature — nunca se habían fusionado a `dev` ni a `qa`.**
No importaba si la migración había corrido o no: el `.jar` desplegado en QA nunca tuvo el código
que lee/escribe esos campos. La confirmación que les dimos el 22 de agosto de que "ya funciona en
dev/qa" fue un error nuestro — no lo habíamos verificado contra las ramas reales.

**Ya está corregido:** ambas features (`flores-eternas-fotos-ramo` y el filtro de fecha) se
fusionaron a `dev` y de ahí a `qa` — ya están desplegadas, listas para volver a probar.

### Sobre `POST /v1/flores/pedidos/{id}/detalle` (su pregunta 3)

Confirmado en el código: este endpoint **no toca para nada** `latitud`/`longitud`/`referencias`
del `Pedido` — ni los lee del request (`RamoPedidoDetalleRequestDto` no tiene esos campos) ni
vuelve a guardar el `Pedido` en ningún punto de `adjuntar()`. Solo lee el `Pedido` para
enganchar la relación con el nuevo `RamoPedidoDetalle`. **No hacía falta reenviarlos ahí** — ese
endpoint es inofensivo respecto a esos 3 campos, pueden confiar en que el primer `savePedido`
es el único que los persiste.

### Checklist antes de volver a probar

1. **Verificar que la migración de ubicación sí corrió en QA** —
   `src/main/resources/static/migration_pedido_ubicacion_entrega.sql` (agrega `latitud`,
   `longitud`, `referencias` a `pedidos`). Si el código ya estaba desplegado sin la migración,
   el guardado habría fallado con error 500 (columna inexistente) — si `savePedido` les
   respondía 200 sin la migración corrida, avísennos porque sería otro síntoma a investigar.
2. **Verificar que la migración de fecha de creación también corrió** —
   `migration_fecha_creacion_producto_variante.sql` (agrega `fecha_creacion` a `producto` y
   `variantes`).
3. Con ambas migraciones corridas y el código ya en QA, prueben de nuevo el ciclo completo:
   marcar pin → confirmar pedido/ramo → "Cómo llegar" — debería mostrar el punto exacto.

Avísennos qué encuentran.

---

## ✅ Confirmado — las 2 migraciones ya corrieron en QA y prod (2026-08-25)

Cierra los 2 puntos del checklist de arriba: el dueño confirma que
`migration_pedido_ubicacion_entrega.sql` y `migration_fecha_creacion_producto_variante.sql` ya se
ejecutaron en ambos ambientes. Con el código ya fusionado a `qa` (ver el punto de arriba) y las
migraciones corridas, el ciclo completo (marcar pin → confirmar pedido/ramo → "Cómo llegar") ya
debería funcionar de punta a punta. Prueben de nuevo y avisen qué encuentran.

---

## 🚨 URGENTE — reenviamos: lat/lng por zona en `LugarEntrega`, sigue sin respuesta desde el 22 de agosto

> Esta consulta se mandó hace 3 días (`CAMBIOS_FRONT.md`, sección "❓ CONSULTA AL BACK — lat/lng
> por zona...") y no llegó respuesta ni en el corte del 22 ni en el del 25 — se las reenviamos
> completa para que no se pierda entre los demás hilos, el dueño la marcó como prioridad.

**El problema:** el picker de mapa (checkout y "Arma tu ramo") siempre arranca centrado en un
punto fijo (Tejupilco), sin importar qué zona elija el cliente en el `<select>` de
`LugarEntrega`. Elegir "Zacazonapan" no mueve el mapa — sigue mostrando otro pueblo, y el cliente
tiene que buscar manualmente con el buscador de texto que agregamos como parche (Nominatim,
gratis, pero no resuelve el fondo).

**Lo que se necesita:** que `LugarEntrega` (`/v1/lugares-entrega`) tenga su propio centroide:

```
latitud   Double  (nullable — zonas viejas sin configurar simplemente no recentran)
longitud  Double  (nullable)
```

Del lado del front ya está el gancho hecho — `SelectorUbicacionComponent` ya reacciona a un
cambio de `centroDefault`, solo falta el dato real por zona en vez del genérico que usamos hoy.
Mismo criterio que ya usaron para `costoEnvio`/`horasExtraAnticipacion` en ese mismo modelo:
opcional, no afecta al checkout general de la tienda.

**Confirmen por favor:**
1. Si es viable, cuándo lo pueden agregar (aunque sea aproximado).
2. Si prefieren otro enfoque (por ejemplo, que el front calcule el centroide localmente con
   Nominatim buscando el nombre de la zona, sin tocar el back) — abierto a sugerencias, lo que
   importa es cerrar el punto.

---

## ✅ BACK — implementado: `LugarEntrega` ya tiene `latitud`/`longitud` (2026-08-25)

Perdón la demora — se responde ahora, agregado tal cual lo pidieron.

**`latitud`/`longitud`** (`Double`, nullable) agregados a la entidad `LugarEntrega`. Como este
recurso usa el CRUD genérico (`AbstractController`, sin DTO propio), ya quedan disponibles sin
más cambios en los 4 endpoints existentes:

- `POST /v1/lugares-entrega/save` y `PUT /v1/lugares-entrega/update` — aceptan los 2 campos
  opcionales junto a `nombre`/`costoEnvio`/`horasExtraAnticipacion` que ya recibían.
- `GET /v1/lugares-entrega/obtener...` (listado) y `GET /v1/lugares-entrega/findById/{id}` — los
  devuelven junto al resto de campos de la entidad.

```json
{
  "id": 3,
  "nombre": "Zacazonapan",
  "costoEnvio": 50.0,
  "horasExtraAnticipacion": 2,
  "latitud": 18.652,
  "longitud": -100.219
}
```

**Zonas viejas sin configurar** → `latitud`/`longitud` vienen `null` (no hay backfill retroactivo,
mismo criterio que `fechaCreacion` y `correoVerificado`) — el `SelectorUbicacionComponent` debe
seguir usando su centro por defecto cuando vengan null, tal como ya lo tienen previsto.

**⚠️ Requiere migración antes de desplegar** —
`src/main/resources/static/migration_lugar_entrega_centroide.sql` (agrega `latitud`/`longitud`
a `lugares_entrega`). Pendiente correr en dev/qa/prod (`ddl-auto: none`).

### Rama

Como no depende de nada bloqueado, va directo a `dev` (no en rama de feature aparte) — se fusiona
a `qa` en cuanto se pruebe, siguiendo el flujo normal.
