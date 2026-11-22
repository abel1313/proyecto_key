# PLAN — Publicaciones y ventas por WhatsApp (bot "NovedadesJade")

> **Fecha:** 2026-11-18 · **Estado:** plan, sin código programado todavía.
> **Pedido del dueño (textual):** *"generar un endpoint para escribir las imágenes o el compartimiento de las imágenes en el bot, hay que notar que se explique como se hará, buscar la mejor manera y que no gaste muchos tokens"*, *"que solo comente en las publicaciones que el bot haga"*, *"una pantalla específica para publicaciones en whats"*, *"generar la venta si es en el grupo o se le manda directo a la persona que le interesa, pero mencionarle si lo hará por dm o personal"*, *"el bot se llamará novedadesJade"*, *"un chat nuevo con el nuevo prompt, se hace un nuevo método"*.

Documento para quien implemente el lado del **back** (este repo, Spring Boot) y el **front** (Angular). El bot de WhatsApp vive en el repo **`abel1313/whatsapp_bot`** (Python + Selenium) y ya está hecho en su parte básica; su documentación está en ese repo (`README.md`, `docs/`). Este documento es la copia para el equipo del back: la fuente original está en `whatsapp_bot/docs/BACK_WHATSAPP_PUBLICACIONES.md`. Aquí se describe **qué falta construir en este repo (back) y en el front, y por qué**.

> Lo que dice "ya existe" se verificó leyendo `proyecto_key` (rama principal). Lo marcado **(sin verificar)** es una suposición que quien implemente debe comprobar.

## 1. Objetivo
1. Publicar productos en grupos de WhatsApp desde una **pantalla del panel**, con foto, programables.
2. Que el bot conteste **solo lo relacionado con lo que él publicó**, usando pocos tokens.
3. Poder **generar la venta** desde el grupo o por mensaje privado.
4. El bot se presenta como **NovedadesJade** y usa un **chat nuevo, con prompt nuevo**.

## 2. Qué ya existe y se reutiliza
El back ya resuelve este mismo problema para Facebook e Instagram. WhatsApp debe ser **una tercera red** de ese módulo, no algo nuevo.

| Pieza existente | Para qué sirve aquí |
|---|---|
| `hexagonal/botredes` (`AtenderInteraccionService`, `CerebroPort`, `Canal`, `RedSocial`) | Orquesta reglas: dedupe (R6), pausa de 30 min cuando atiende una persona (R5), control de abuso (R7), escalar al admin por correo (R1). |
| `ProductoDePublicacionPort` + tabla `publicacion_social` | Ya liga una publicación con un producto (`variante_id`). Es la base de "solo contesto lo que publiqué". |
| `ChatbotBase.responderSobreProducto(...)` | Prompt corto con **solo los datos de un producto** (sin catálogo): el más barato en tokens. |
| `ChatbotFacebookService` / `ChatbotInstagramService` (extienden `ChatbotBase`) | Patrón para el chat nuevo de WhatsApp. |
| `PublicacionSocialScheduler`, `RedesSocialesController` | Publicación programada y pantalla de publicar en redes: modelo a copiar. |
| `ChatbotBlockService`, `PoliticaDeRespuesta` | Límites y filtro de respuestas con datos internos. |
| `/v1/clientes-sin-registro`, `/v1/pedidos/savePedido`, `/v1/abonos` | Cliente sin cuenta, pedido y apartado con abonos. |
| `COMO_DAR_ALTA_PANTALLA_Y_PERMISOS.md` | Pasos para dar de alta la pantalla nueva y sus permisos. |

Ojo: `POST /productos/compartir-imagenes-variantes` **no** es lo que se pide aquí. Ese endpoint asigna imágenes de un producto a sus variantes.

## 3. Modelo de datos
Reutilizar `publicacion_social` con `plataforma = 'whatsapp'`. Agregar columnas:

| Columna | Tipo | Uso |
|---|---|---|
| `grupo_whatsapp` | varchar | **Nombre exacto** del grupo (el bot lo busca por nombre). |
| `codigo_ref` | varchar único | Código corto visible, p. ej. `J42` (ver §5). |
| `modo_venta` | enum `GRUPO` / `PRIVADO` | Cómo se cierra la venta (ver §7). |
| `estado` | | Agregar `EN_PROCESO` además de los que ya tiene. |

`estado`: `PENDIENTE` → `EN_PROCESO` (cuando el bot la recoge) → `PUBLICADA` / `ERROR`; `CANCELADA` desde el panel. `intentos` y `ultimo_error` ya existen.

Agregar `WHATSAPP("whatsapp")` a `RedSocial` y revisar la columna `red_social` de las tablas de registro, pausas y abuso (migración SQL, como las `migration_*.sql` del repo).

## 4. Endpoints para el bot (header `X-Bot-Key`)
Ninguno puede ser `permitAll` sin llave: validar `X-Bot-Key` contra una propiedad del servidor (`bot.api-key`) con comparación en tiempo constante. La llave es la `PUBLICACIONES_API_KEY` del bot. Limitar por llave, **no por IP** (el bot sale siempre por la misma IP).

| Método y ruta | Qué hace |
|---|---|
| `GET /v1/bot/publicaciones/pendientes` | Devuelve `PENDIENTE` con `programada_para` vencida y las marca `EN_PROCESO` en la misma transacción (evita duplicados). `[{id, grupo, texto, imagenUrl, codigoRef}]`. |
| `GET /v1/bot/publicaciones/{id}/imagen` | **Entrega la imagen al bot** (ver §4.1). |
| `POST /v1/bot/publicaciones/{id}/publicada` | Marca `PUBLICADA`, guarda `fecha_publicacion`. |
| `POST /v1/bot/publicaciones/{id}/error` | `{detalle}`; suma `intentos`; vuelve a `PENDIENTE` hasta 3 intentos y luego `ERROR`. |
| `POST /v1/bot/whatsapp/mensaje` | El chat nuevo (ver §6). |
| `POST /v1/bot/whatsapp/venta` | Genera el pedido (ver §7). |

### 4.1 Imagen para el bot: la forma más barata
- **Las imágenes nunca pasan por el modelo de IA** (cero tokens). Solo se mueven bytes por HTTP.
- `GET /v1/bot/publicaciones/{id}/imagen` responde **binario** con `Content-Type: image/jpeg`, **no base64 en JSON** (base64 pesa ~33 % más).
- Redimensionar y comprimir en el back: lado mayor ≤ 1280 px, JPEG calidad ~80 (WhatsApp igual la comprime). Cachear el resultado por `imagen_id`.
- Origen: la imagen principal de la variante (`imagen_id`), pedida al micro de imágenes. La raíz del micro de QA responde 403 y no se debe depender de su URL pública. **(sin verificar)** qué rutas del micro son públicas.
- Si no hay imagen, `404`: el bot publica solo el texto.
- El cliente del bot ya acepta cualquier URL que devuelva `image/*` (máximo 5 MB), así que `imagenUrl` puede apuntar a este mismo endpoint.

### 4.2 El texto de la publicación **no usa IA**
Se arma con una plantilla a partir del producto: nombre, precio, tallas y colores disponibles, código de referencia y una llamada a la acción. Sale gratis, es predecible y se edita en la pantalla antes de publicar. Un botón opcional "mejorar con IA" puede quedar apagado por defecto.

## 5. Cómo sabe el bot que "esto lo publiqué yo" (y contestar solo eso)
Cada publicación lleva un **código corto al inicio del texto**, p. ej.:

```
*Novedades Jade · J42*
Bolsa tote beige 🛍️
$450 · quedan pocas
Responde a este mensaje o escribe J42 para apartarla
```

Va **al inicio** porque, cuando alguien responde (cita) un mensaje en WhatsApp, la cita solo muestra las primeras líneas. **(sin verificar)** exactamente cuánto texto expone la cita en WhatsApp Web; hay que probarlo con una publicación real.

El bot solo manda al back un mensaje de grupo si cumple **alguna** de estas condiciones:
1. Es una **respuesta (cita)** a un mensaje del bot y en la cita aparece un código `J\d+`.
2. El texto contiene un código `J\d+`.
3. Menciona `@NovedadesJade` y hay una sola publicación activa del grupo en las últimas 24 h (si hay varias, el back pide aclarar a cuál se refiere).

Todo lo demás en el grupo **se ignora en silencio**: no se contesta ni se escala. En el back, `POST /v1/bot/whatsapp/mensaje` vuelve a validar (fuente única de la regla): si en un grupo el código no corresponde a una publicación `PUBLICADA` en ese grupo, responde `accion: IGNORAR`.

En mensajes **privados** sí se contesta con el catálogo completo, como en R3 de `botredes/README.md`.

### 5.1 Decisión 2026-11-22: qué pasa cuando alguien comenta sin citar ni poner el código (PROPUESTA, el dueño la confirma)
WhatsApp no tiene «comentarios» como Facebook: el bot solo ve texto. Orden de confianza propuesto:
| Señal | Confianza | Qué hace el bot |
|---|---|---|
| Cita (responde a) el mensaje del bot y la cita trae el código `J42` | Alta | Contesta sobre esa publicación |
| El texto trae el código `J42` | Alta | Igual |
| Sin cita y **una sola** publicación activa en ese grupo en 24 h | Media | Contesta **diciendo de cuál habla** («Sobre la bolsa tote J42: …») para que un error se note |
| Sin cita y **varias** publicaciones activas | Baja | **No adivina**: pregunta «¿te refieres a J42 o J43?» |
| Sin cita, sin código y sin publicación activa | Ninguna | Se ignora en silencio |
El texto de la publicación pide «responde a este mensaje o escribe J42». Si en la práctica casi nadie cita, la salida es publicar **una sola cosa a la vez por grupo**.

## 6. Chat nuevo y prompt nuevo (NovedadesJade)
Crear `ChatbotWhatsappService extends ChatbotBase`, con un método por situación, igual que Instagram/Facebook:

| Método | Cuándo | Contexto que recibe |
|---|---|---|
| `responderSobreProductoWhatsapp(texto, variante, esPrimeraVez, enGrupo)` | Mensaje con código/cita de una publicación | Solo los datos de ese producto. |
| `responderMensajeDirectoWhatsapp(texto, esPrimeraVez)` | Chat privado | Catálogo completo (R3). |

Prompt nuevo `IDENTIDAD_WHATSAPP` (en vez de `IDENTIDAD_COMENTARIOS`):
- Se llama **NovedadesJade**; tono de vecina del pueblo, 1–3 líneas, 1 emoji como máximo; `REGLAS_DE_RESPETO` incluidas.
- En grupo: nunca datos de otros clientes, domicilios ni teléfonos; el stock exacto lo filtra `PoliticaDeRespuesta`.
- Se presenta como asistente automático **solo en el primer mensaje privado** (R4).
- Marcas nuevas: `##COMPRAR##` (quiere comprar o apartar) además de `##ESCALAR##` y `##GRACIAS##`.

### Cómo gastar pocos tokens
1. **Cero IA donde se pueda:** saludos, gracias y "precio?" / "hay en otro color?" se contestan con plantillas a partir de los datos del producto; el modelo solo se llama cuando hace falta.
2. **Prompt mínimo:** `responderSobreProducto` ya manda solo el producto, sin catálogo. No agregar políticas largas.
3. `max_tokens` de **~150** para WhatsApp (hoy `ChatbotBase` usa 500).
4. Historial corto: últimos 4 mensajes **por persona + publicación**, nunca el del grupo completo.
5. **Caché** de respuestas por `(publicacion, pregunta normalizada)`, TTL de 1 h: en un grupo muchas personas preguntan lo mismo.
6. Modelo barato (`gpt-4o-mini`, que ya es el default) y sin visión: las fotos nunca se envían al modelo.
7. Límites por persona (`ChatbotBlockService` con clave `whatsapp:<persona>`) y por publicación.

### Contrato `POST /v1/bot/whatsapp/mensaje`
Entrada:
```json
{ "canal": "GRUPO", "chat": "Grupo Familia", "remitente": "Ana", "texto": "cuánto cuesta?",
  "ref": "J42", "citaTexto": "Novedades Jade · J42 Bolsa tote…" }
```
`canal` es `GRUPO` o `PRIVADO`; `ref` y `citaTexto` son opcionales.

Salida:
```json
{ "accion": "RESPONDER", "respuesta": "Cuesta $450 💖", "publicacionId": 12, "modoVenta": "PRIVADO" }
```
`accion`: `RESPONDER`, `IGNORAR`, `ESCALADO` (el back ya avisó al admin y el bot solo saluda), `OFRECER_PRIVADO` (ver §7). Mapear a `AtenderInteraccionService`: `GRUPO` ≈ `Canal.COMENTARIO` con `publicacionId = codigo_ref`; `PRIVADO` ≈ `Canal.MENSAJE_DIRECTO`.

Identificar a la persona: Selenium solo ve el **nombre** (o el número si no está en contactos). Usar `autorId = chat + ":" + remitente` y avisar en la pantalla que dos personas con el mismo nombre se confunden. **(sin verificar)**

La pausa R5 ("atiende una persona") requiere que el bot avise cuando el admin contesta a mano desde el celular; es trabajo del bot (§9) y el back solo necesita `POST /v1/bot/whatsapp/pausa {chat, remitente}`.

## 7. Generar la venta
El bot **le avisa a la persona cómo será el trato** antes de pasar a privado. La pantalla decide por publicación (`modo_venta`).

| Modo | Qué hace el bot | Riesgo |
|---|---|---|
| **GRUPO** | Contesta y cierra en el grupo: "Te lo aparto, ¿cuántas piezas? ¿entrega o recoges?". Los datos personales **no** se piden en el grupo. | Datos a la vista de todos; solo sirve para apartar sin datos. |
| **PRIVADO** (recomendado) | En el grupo: "¡Claro, Ana! Escríbeme por privado y te lo aparto 💖 wa.me/<número>?text=Quiero%20J42". La persona escribe **primero** y el bot continúa en privado. | Bajo: la persona inició la conversación. |
| **PRIVADO PROACTIVO** (opcional, apagado) | En el grupo: "Te escribo por privado". El bot **inicia** el chat con la persona. | Alto: WhatsApp puede marcar mensajes a quien no te escribió. Si se activa: máximo ~5 por día, una vez por persona y publicación, solo si pidió comprar. **(sin verificar)** que el bot pueda abrir el chat de un miembro del grupo. |

Flujo de venta (en privado, `POST /v1/bot/whatsapp/venta`):
1. El modelo marca `##COMPRAR##`; el back lleva una pequeña máquina de estados por persona+publicación: cantidad → nombre → entrega (punto de encuentro o domicilio, con `lugares-entrega` / `entregas-zona`) → confirmación.
2. Al confirmar: crear/ubicar el cliente en `/v1/clientes-sin-registro` y el pedido con `savePedido`, con `origen = 'WHATSAPP'` y `publicacion_social_id` (para reportar ventas por publicación). **(sin verificar)** payload exacto de `savePedido` y de `PedidoDatosCliente`.
3. No se cobra dentro del chat: el pedido queda **pendiente de confirmar** y se avisa al admin con `AvisoAdminPort`. El pago/abono sigue por el flujo actual de `/v1/abonos`.
4. Validar stock al crear el pedido; si se agotó, el bot lo dice y ofrece avisar.

## 8. Pantalla del front: "Publicaciones de WhatsApp"
Dar de alta con `COMO_DAR_ALTA_PANTALLA_Y_PERMISOS.md` (menú, submenú y permisos de lectura/escritura). Tomar como modelo la pantalla de publicar en redes sociales.

- **Lista**: producto, grupo, fecha programada, estado (`PENDIENTE`/`EN_PROCESO`/`PUBLICADA`/`ERROR`/`CANCELADA`), código `J42`, errores, **reintentar** y **cancelar** (solo si sigue `PENDIENTE`).
- **Crear/programar**: buscar producto (artículo) → texto autogenerado y **editable** → imagen principal con vista previa → grupo (lista de grupos dados de alta en un catálogo, porque el nombre debe coincidir exacto) → modo de venta (`GRUPO`/`PRIVADO`) → publicar ahora o programar.
- **Vista previa** estilo burbuja de WhatsApp con el código incluido.
- **Resultados** por publicación: preguntas atendidas, escaladas, pedidos generados.
- Avisos en pantalla: el bot debe ser miembro del grupo (y admin si el grupo limita el envío); el bot solo contesta lo que publica desde aquí.

## 9. Cambios que quedan en el bot (`whatsapp_bot`), no en el back
Para no confundir: esto lo hace quien mantenga el bot, no el equipo del back.
1. Leer el **texto de la cita** y extraer el código `J\d+`; filtrar mensajes de grupo según §5.
2. Llamar a `POST /v1/bot/whatsapp/mensaje` (en vez de `/v1/chatbot/mensaje`) y actuar según `accion`.
3. Enviar la imagen desde `/v1/bot/publicaciones/{id}/imagen` (ya soportado por `bot/fotos.py`).
4. Detectar cuando el **admin contesta a mano** (mensajes salientes que el bot no envió) y avisar la pausa.
5. Modo privado proactivo, si se decide activarlo.
6. Configurar `BOT_NAME=NovedadesJade` y verificar los selectores en WhatsApp real.

## 10. Seguridad y privacidad
- `X-Bot-Key` en todo `/v1/bot/**`; fuera de `permitAll`; rotación documentada.
- Nunca mostrar en grupo datos de otros clientes. Los números y nombres de WhatsApp son datos personales: revisar `LEGAL_PLAN_DE_ACCION.md` y avisar en el grupo que hay un bot.
- Automatizar WhatsApp Web **no es oficial**: el número del bot puede ser bloqueado. Por eso se recomienda un número dedicado, volumen bajo y no escribir a desconocidos.

## 11. Decisiones por confirmar con el dueño
| Decisión | Recomendación |
|---|---|
| ¿`modo_venta` por defecto? | `PRIVADO` con enlace `wa.me`. |
| ¿Activar mensaje privado proactivo? | No al inicio. |
| ¿Mención real como `@NovedadesJade` o solo código `J42`? | Ambos; el código es lo que siempre funciona. |
| ¿Quién crea el catálogo de grupos? | El admin, desde la misma pantalla. |
| ¿La venta en grupo pide datos? | No: solo apartar; los datos se piden en privado. |

### 11.1 Dudas abiertas (2026-11-22)
| # | Duda | Quién la resuelve | Estado |
|---|---|---|---|
| 1 | ¿Se confirma la propuesta de §5.1 (sin cita y con varias publicaciones activas el bot pregunta de cuál habla, nunca adivina)? | Dueño | Pendiente |
| 2 | ¿Los clientes suelen **citar** el mensaje del bot o escriben suelto? Si casi nadie cita: una sola publicación activa por grupo. | Dueño (por experiencia con los grupos) | Pendiente |
| 3 | Generar `BOT_API_KEY` (larga y al azar), ponerla en el servidor de QA y en el bot (`PUBLICACIONES_API_KEY`). | Dueño | Pendiente |
| 4 | El repo `whatsapp_bot` no está conectado a la sesión: ¿se agrega para ajustar el bot a estos endpoints (`imagenPath` relativo, estados)? | Dueño | Pendiente |
| 5 | ¿Número de WhatsApp dedicado al bot? (automatizar WhatsApp Web no es oficial; el número puede bloquearse.) | Dueño | Pendiente |
| 6 | Verificar con una publicación real cuánto texto muestra la cita en WhatsApp Web (el código `J42` va en la primera línea por eso). | Prueba en QA | Pendiente |

## 12. Plan de trabajo sugerido
**Back**
- [ ] Migración SQL: columnas de `publicacion_social`, `WHATSAPP` en `red_social`, catálogo de grupos.
- [ ] Filtro `X-Bot-Key` para `/v1/bot/**`.
- [ ] `pendientes`, `imagen`, `publicada`, `error` (§4).
- [ ] `ChatbotWhatsappService` y prompt `IDENTIDAD_WHATSAPP` (§6), con plantillas sin IA, caché y `max_tokens` bajo.
- [ ] `POST /v1/bot/whatsapp/mensaje` conectado a `AtenderInteraccionService` (`RedSocial.WHATSAPP`).
- [ ] `POST /v1/bot/whatsapp/venta` y marca `##COMPRAR##` (§7).
- [ ] Tests, siguiendo `ChatbotRespetoTest`, `ChatbotFugaDatosClienteTest` y `AtenderInteraccionServiceTest`.

**Front**
- [ ] Pantalla de §8 con permisos.

**Criterios de aceptación**
- Una publicación programada sale una sola vez, con foto, en el grupo correcto, y queda `PUBLICADA` o `ERROR` con motivo.
- Un mensaje de grupo sin código ni cita se **ignora** (sin respuesta ni correo).
- Responder a la publicación con "¿cuánto cuesta?" contesta con el precio del producto correcto.
- Una pregunta que el producto no cubre se escala y el bot se aparta 30 min.
- "Lo quiero" genera un pedido pendiente con `origen = WHATSAPP` ligado a la publicación y avisa al admin.
- Ninguna respuesta en grupo muestra datos de otro cliente.
