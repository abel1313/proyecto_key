
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

## ⚠️ Recordatorio — "subí la foto y desapareció del front" es el gap ya documentado desde 2026-07-21 (2026-08-22)

Se reportó de nuevo el mismo síntoma: se sube una foto en `/carga-imagenes`, el back la procesa
bien (`estadoImagen: EXITOSO`, producto+variante+imagen guardados), pero al salir/recargar la
pantalla la tarjeta ya no aparece en ningún lado.

**No es un bug nuevo ni de backend** — es el gap que ya se documentó arriba en `CAMBIOS_FRONT.md`
("❓ CONSULTA AL BACK — falta endpoint para descartar un borrador de carga rápida", 2026-07-21):
`ngOnInit()` de la pantalla solo vuelve a pedir `GET /v1/carga-imagenes/fallidas` al recargar —
nunca repobla los que quedaron `EXITOSO` sin completar. El producto **no se pierde**, sigue en la
base de datos, deshabilitado, con su imagen ya subida — solo se pierde de la vista del front.

**La solución sigue siendo la misma de esa vez, sin cambios de backend:**
```
GET /v1/productos/admin/filtrar?codigoGenerado=true&habilitado=false&size=50&page=1
GET /v1/carga-imagenes/estado?productoIds=1,2,3,...
```

**🆕 Ahora es más fácil todavía** gracias al filtro de fecha que se agregó (ver sección de arriba
"Filtro por fecha de creación") — para encontrar justo el que se subió hoy, sin traer todos los
borradores viejos:
```
GET /v1/productos/admin/filtrar?codigoGenerado=true&habilitado=false&fechaDesde=2026-08-22&fechaHasta=2026-08-22
```

**Para verificar ahora mismo que el producto de la prueba de hoy sí se guardó** (sin depender del
front): correr esa misma URL contra el ambiente donde se probó, con la fecha de hoy — si aparece
en la respuesta, el back funcionó correctamente y el pendiente es implementar esto en el front.
