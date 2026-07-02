# Bugs del Chatbot — Reporte para el Equipo Backend

**Fecha:** 2026-07-02  
**Contexto:** se encontraron durante pruebas en vivo del chatbot en `/variantes/chatbot` y revisión del CAMBIOS_FRONT.md.

**Estado 2026-07-02:** BUG-CB-02 corregido (causa raíz encontrada y arreglada). BUG-CB-03
implementado. BUG-CB-01 sigue siendo un problema de datos, no de código — ver detalle abajo.

---

## BUG-CB-01 — El chatbot trae un producto/variante DISTINTO al que se venía platicando (no es solo la imagen)

**Estado: CORREGIDO 2026-07-02 (segunda causa raíz encontrada).** El diagnóstico anterior
("es dato incorrecto en BD, no se puede arreglar con código") era **incompleto**. Hay un
segundo bug, de código, que explica el caso reportado el 2026-07-02: el bot confunde
"Mochila para mostrar" (código de barras `cod1230981`, $300) con "Mochila Prada" ($400) — son
productos DIFERENTES, no la misma variante con imagen mal vinculada.

**Causa raíz real:** en `ChatbotService.java` el prompt de sistema le decía a la IA que, para
buscar la imagen (`##BUSCAR[término,offset]##`), **si el producto se identificó por código de
barras usara el NOMBRE del producto** como término de búsqueda. El problema: varios productos
comparten nombre genérico ("Mochila" aparece en "Mochila para mostrar" y en "Mochila Prada").
Al buscar por `##BUSCAR[Mochila,0]##`, el back hace `LIKE '%Mochila%'` contra TODOS los
productos con esa palabra en el nombre (`IVarianteRepository.buscarParaChatbot`) y devuelve
cualquiera de los que matchean — no necesariamente el que se acababa de identificar por código
de barras. El nombre es ambiguo; el código de barras no.

**Fix aplicado:** se corrigió el prompt para que, cuando el producto de la conversación se
identificó por código de barras, la IA use ESE código de barras como término de `##BUSCAR##`
(la query ya soportaba buscar por código de barras, solo que el prompt le decía que no lo
hiciera). Con eso el back trae el producto exacto, no uno con nombre parecido.

**Nota para front:** no requiere cambios — el fix es 100% de prompt/backend. Si se puede,
sería bueno volver a probar el flujo "preguntar por código de barras → pedir foto" para
confirmar que ahora trae la tarjeta correcta.

**Limitación que sigue existiendo (no es bug, es comportamiento esperado):** cuando el cliente
pide fotos SIN haber mencionado un código de barras (ej. "muéstrame bolsas", "tienes Mochila?")
la búsqueda sigue siendo por nombre/marca y puede traer varios productos distintos que
comparten esa palabra — para eso existe la paginación (`hayMas`) para ver más opciones. Esto es
esperado cuando el cliente no fue específico.

**Prioridad:** Alta

**Síntoma:**  
Al buscar un producto en el chatbot (ej. "Mochila"), las tarjetas cargan y muestran una imagen, pero **no es la imagen de ese producto** — es la imagen de otro producto distinto.

**Pasos para reproducir:**
1. Abrir el chatbot en el sistema
2. Escribir "Mochila" (o cualquier producto que tenga imagen subida)
3. Observar la imagen que aparece en la tarjeta
4. Comparar contra la imagen real de esa variante en el panel de admin (`/variantes/buscar` o `/variantes/editar`)

**Endpoint afectado:**
```
GET /mis-productos/variantes/v1/imagenes/{varianteId}
```

**Posibles causas (para que el back investigue):**
- El campo `principal: true` no está marcado en la imagen correcta para esas variantes — el front ahora lo usa como criterio de selección (fix del 2026-07-02), pero si el flag está en la imagen equivocada, seguirá mostrando mal
- Las imágenes en la BD están vinculadas al `varianteId` incorrecto (la foto de variante A quedó asociada a variante B)

**Lo que el front ya corrigió (2026-07-02):**  
El front ahora selecciona `imgs.find(i => i.principal) ?? imgs[0]` en vez de tomar siempre el primer elemento. Esta corrección está hecha. Si el problema persiste, la causa está en los datos del back.

**Datos de prueba reportados:**  
`varianteId` 117, 165, 213, 277 (variantes "Mochila Prada")

---

## BUG-CB-02 — Error 500 al pedir imágenes de ciertas variantes

**Estado: CORREGIDO 2026-07-02.** Causa raíz encontrada: cuando una fila de `variante_imagen`
apunta a una `Imagen` que ya no existe (o `imagen_id` es null), `vi.getImagen()` regresa `null`
y el código tronaba con `NullPointerException` al llamar `.getId()` sobre ese null — de ahí el
500. Se agregó un filtro (`filtrarRelacionesConImagen` en `VarianteServiceImpl.java`) que
descarta esas relaciones huérfanas antes de procesarlas (con un log de advertencia para poder
detectarlas), en vez de tronar. También se agregó try/catch en el controller como respaldo.
Ahora una variante sin imágenes válidas responde `200 { data: [] }` como se pedía.

**Prioridad:** Media

**Síntoma:**  
Para algunas variantes, el endpoint de imágenes devuelve HTTP 500 en vez de 200 con lista vacía o 200 con las imágenes.

**Endpoint afectado:**
```
GET /mis-productos/variantes/v1/imagenes/{varianteId}
```

**Comportamiento actual:** `500 Internal Server Error`

**Comportamiento esperado:**
- Si la variante tiene imágenes → `200 { data: [ { urlImagen, principal, ... } ] }`
- Si la variante **no** tiene imágenes → `200 { data: [] }` (lista vacía, no error)

**Impacto en el front:**  
El front ya captura el error con `catchError(() => of(null))` y muestra el placeholder 📦, así que no crashea. Pero el error 500 en consola es ruido y podría esconder otros problemas.

**Datos de prueba reportados:**  
Los mismos `varianteId` de BUG-CB-01 (117, 165, 213, 277) también presentaban 500 al pedir su imagen.

---

## BUG-CB-03 — Variantes con mismo código de barras aparecen indistinguibles en el chatbot

**Estado: IMPLEMENTADO 2026-07-02.** Se agregaron 2 campos nuevos al response de
`GET /v1/chatbot/buscar` (y también a `POST /v1/chatbot/mensaje` cuando dispara `##BUSCAR##`,
comparten el mismo método): `descripcion` y `codigoBarras` (este último solo si el producto
tiene código de barras registrado). Ver detalle completo en `CAMBIOS_FRONT.md`.

**Prioridad:** Baja / Mejora

**Síntoma:**  
Hay variantes que comparten el mismo código de barras y tienen el mismo nombre y precio, pero son variantes diferentes con stock propio. En el chatbot aparecen como tarjetas visualmente idénticas porque el response de `/v1/chatbot/buscar` no incluye ningún campo que las diferencie.

**El front muestra correctamente** lo que el back manda — si el back no manda `talla`, `color` ni ningún campo diferenciador, el front no puede distinguirlas.

**Petición:**  
¿Es posible agregar al response de `GET /v1/chatbot/buscar` algún campo adicional que diferencie estas variantes? Por ejemplo:
- `descripcion` (si existe en la entidad)
- `observaciones`
- `codigoBarras` (para que el admin pueda identificarla visualmente)
- O cualquier campo que las haga distinguibles

**Endpoint afectado:**
```
GET /mis-productos/v1/chatbot/buscar?q=Mochila&offset=0
```

**Response actual (ambas tarjetas idénticas):**
```json
{ "varianteId": 117, "nombre": "Mochila Prada", "precio": 400, "talla": null, "color": null, "stock": 1 }
{ "varianteId": 165, "nombre": "Mochila Prada", "precio": 400, "talla": null, "color": null, "stock": 1 }
```

**Response esperado (algo que las distinga):**
```json
{ "varianteId": 117, "nombre": "Mochila Prada", "precio": 400, "talla": null, "color": null, "stock": 1, "codigoBarras": "ABC123" }
{ "varianteId": 165, "nombre": "Mochila Prada", "precio": 400, "talla": null, "color": null, "stock": 1, "codigoBarras": "DEF456" }
```

---

## Resumen

| Bug | Endpoint | Prioridad | Estado |
|---|---|---|---|
| BUG-CB-01 — Trae producto/variante distinto al platicado | `POST /v1/chatbot/mensaje` | Alta | ✅ Corregido — prompt usaba nombre ambiguo en vez de código de barras para re-buscar |
| BUG-CB-02 — Error 500 en imágenes | `GET /variantes/v1/imagenes/{varianteId}` | Media | ✅ Corregido — filtra relaciones huérfanas, ya no truena |
| BUG-CB-03 — Tarjetas indistinguibles | `GET /v1/chatbot/buscar` | Baja | ✅ Implementado — se agregaron `descripcion` y `codigoBarras` al response |

**Correcciones ya aplicadas en el front (2026-07-02):**
- URL corregida: `/v1/variantes/imagenes/` → `/variantes/v1/imagenes/` (ya estaba corregido de sesión anterior)
- Selección de imagen: ahora usa `principal: true` en vez de tomar siempre el primer elemento del array

---

## Caso reportado 2026-07-02 — resuelto, explicación para front

**Reporte original:** al preguntar por la variante con código de barras `cod1230981`
("Mochila para mostrar", $300, 5 unidades), el bot confirmaba bien el producto en texto, pero
al pedir la foto regresaba la tarjeta de "Mochila Prada" ($400) — un producto totalmente
distinto, no solo una imagen mal ligada.

**Diagnóstico:** ver BUG-CB-01 arriba — era el prompt del chatbot usando el nombre genérico
("Mochila") en vez del código de barras exacto para la segunda búsqueda (la de la imagen),
y como hay más de un producto con "Mochila" en el nombre, el back regresaba el que fuera.

**Fix:** solo en `ChatbotService.java` (prompt), ningún endpoint ni contrato cambió.

**¿Front necesita hacer algo?** No. No cambia ningún request/response. Sí ayuda volver a
probar en vivo el flujo "preguntar por código de barras → pedir ver foto" para confirmar que
ahora trae la tarjeta correcta — si se puede reproducir con el mismo `cod1230981` sería la
mejor prueba.

**Si vuelve a pasar con otro producto:** lo más útil para diagnosticar es mandar, en este
mismo archivo o por Slack: el mensaje exacto del usuario, la respuesta del bot, y el array
`productos` que regresó `POST /v1/chatbot/mensaje` (no hace falta implementar logging nuevo en
front — con eso alcanza para rastrear qué término de búsqueda uso la IA).