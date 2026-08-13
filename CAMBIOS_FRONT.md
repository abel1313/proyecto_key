# Cambios de API para Frontend — Migración a micro_imagenes

## Regla general
- **proyecto-key (9091):** solo maneja lógica de negocio (productos, variantes, pedidos, etc.)
- **micro_imagenes (9096):** todo lo relacionado con archivos de imagen

Los endpoints deprecados en proyecto-key siguen funcionando pero el front debe apuntar a los nuevos.
Los endpoints que dicen `✅ micro_imagenes (9096)` el front los llama **directamente al micro**.
Los endpoints que dicen `✅ proyecto-key (9091)` no pudieron moverse al micro (mezclan datos de negocio).

---

## ⚠️ MIGRACIÓN DE VERSIONES DE URL — 2026-06-07 (acción requerida en el front)

Se normalizó el versionado de URLs en **ambos** backends (proyecto-key 9091 y micro_imagenes 9096) para que todo use `/v1/` como versión estable. Resumen para el front:

- **Lo que el front ya está usando como "v2"** → se renombró a **`/v1/`**. Es la versión activa/estable. **El front solo necesita agregar `/v1/`** donde antes no había versión, o cambiar `/v2/` por `/v1/` donde ya tenía `/v2/`.
- **Lo que el front YA NO usa** (la versión vieja, marcada `@Deprecated`) → se renombró a **`/v3/`**. Sigue funcionando por compatibilidad pero no se debe usar para nada nuevo.
- **micro_imagenes (9096)** no tenía versión en sus URLs — ahora **todas** sus rutas llevan el prefijo `/v1/`.

### Tabla de cambios — proyecto-key (9091)

> ✅ `imagenes.service.ts` y `producto.service.ts` ya actualizados (2026-06-17)

| Antes (front lo usa) | Ahora | Estado |
|---|---|---|
| `imagen/v2/{productoId}` | `imagen/v1/{productoId}` | ✅ `imagenes.service.ts` |
| `imagen/v2/{productoId}/detalle` | `imagen/v1/{productoId}/detalle` | ✅ `producto.service.ts` |
| `imagen/v2/file/{imagenId}` | `imagen/v1/file/{imagenId}` |
| `imagen/v2/{idProducto}/imagenes` | `imagen/v1/{idProducto}/imagenes` |
| `imagen/v2/{idImagen}` (DELETE) | `imagen/v1/{idImagen}` (DELETE) |
| `imagen/v2/{productoId}/imagenes` (DELETE) | `imagen/v1/{productoId}/imagenes` (DELETE) |
| `imagen/v2/producto` (DELETE) | `imagen/v1/producto` (DELETE) |
| `imagen/v2/cache/limpiar` | `imagen/v1/cache/limpiar` |
| `presentacion/v2/imagenes` | `presentacion/v1/imagenes` |
| `presentacion/v2/imagenes/{id}/imagen` | `presentacion/v1/imagenes/{id}/imagen` |
| `presentacion/v2/imagenes/todas` | `presentacion/v1/imagenes/todas` |
| `presentacion/v2/imagenes/{id}` (PUT) | `presentacion/v1/imagenes/{id}` (PUT) |
| `variantes/v2/imagenes/{varianteId}` | `variantes/v1/imagenes/{varianteId}` |
| `variantes/v2/imagenes` (DELETE) | `variantes/v1/imagenes` (DELETE) |
| `variantes/v2/{varianteId}/imagenes` (DELETE) | `variantes/v1/{varianteId}/imagenes` (DELETE) |

> Las rutas viejas sin versión (`imagen/{id}`, `presentacion/imagenes`, `variantes/imagenes/{varianteId}`, etc.) ahora viven bajo `/v3/` y están `@Deprecated` — el front **no** debe usarlas.

### Tabla de cambios — micro_imagenes (9096) — antes no tenía versión, ahora todo lleva `/v1/`

| Antes | Ahora |
|---|---|
| `imagenes/file/{imagenId}` | `v1/imagenes/file/{imagenId}` |
| `imagenes/{id}` | `v1/imagenes/{id}` |
| `imagenes` (POST/GET/DELETE) | `v1/imagenes` |
| `imagenes/verificar` | `v1/imagenes/verificar` |
| `imagenes/disco` (DELETE) | `v1/imagenes/disco` |
| `producto-imagen/...` (todas las rutas) | `v1/producto-imagen/...` |
| `cache/limpiar` (DELETE) | `v1/cache/limpiar` |

**Ejemplo concreto que dio el equipo:**
```
Antes: http://localhost:9096/mis-productos/imagenes/file/7305237692097776164
Ahora: http://localhost:9096/mis-productos/v1/imagenes/file/7305237692097776164
```

Los `urlImagen` / `imagenUrl` que devuelven los listados (productos, variantes, presentación) **ya vienen actualizados con `/v1/` desde el backend** — el front no tiene que construir esas URLs manualmente, solo usarlas tal cual llegan en el response.

---

## BUGS CORREGIDOS — Cambios de comportamiento que el front debe conocer

---

### [BUG-KEY-02] ✅ Fix: búsqueda de pedidos — endpoint migrado a query param
**Fecha:** 2026-06-05  
**Archivos corregidos:** `PedidoController.java:92`, `PedidoServiceImpl.java:250`

**Endpoint ANTERIOR (deprecado):**
```
GET /pedidos/buscarClientePedido/{buscar}?size=10&page=0
```

**Endpoint NUEVO:**
```
GET /pedidos/buscarClientePedido?buscar=juan&size=10&page=0
GET /pedidos/buscarClientePedido?size=10&page=0            ← sin buscar = todos los pedidos
```

**El front DEBE cambiar la llamada:**
- Quitar el segmento `/{buscar}` de la URL
- Pasar `buscar` como query param (opcional)
- Cuando el campo está vacío → omitir el param o mandarlo vacío `buscar=`; ambos devuelven todos los pedidos

**Por qué cambia:** un path variable nunca puede ser vacío en HTTP — el router de Spring no matchea la ruta si el segmento falta. El front usaba `"vacio"` como centinela, lo que provocaba que la búsqueda buscara un cliente llamado "vacio" y no devolviera nada. Con query param opcional el problema desaparece.

**Comportamiento:**
- `buscar` ausente o vacío → devuelve **todos** los pedidos paginados
- `buscar=juan` → filtra pedidos cuyo cliente contiene "juan"

**Códigos de respuesta:** 200 con `PageableDto`, 500 si hay error interno.

---

### [BUG-KEY-01] ✅ Fix: guardar/actualizar producto ahora tiene rollback real si algo falla
**Fecha:** 2026-06-04  
**Archivo corregido:** `ProductosServiceImpl.java:365` — `private` → `protected` en `guardarProducto()`

**Endpoints afectados:**
```
POST /productos/save
PUT  /productos/update
```

**Dónde verlo en el panel:**
> Menú lateral → **Productos** → botón **Agregar producto** o **Editar producto** → llenar el formulario y guardar.

**Comportamiento ANTES del fix (incorrecto):**
- Si fallaba cualquier paso después de guardar el producto en BD (por ejemplo el guardado de imágenes), el producto quedaba guardado pero sin imágenes — estado inconsistente, datos a medias sin rollback

**Comportamiento DESPUÉS del fix (correcto):**
- Si algo falla durante el guardado completo (producto + imágenes + relaciones), Spring hace rollback de todo
- O se guarda todo completo, o no se guarda nada

**El front no necesita cambiar nada** — mismo endpoint, mismo request, mismo response. El cambio es interno de integridad de datos.

---

### [BUG-KEY-03] ✅ Fix: compartir imágenes a variantes ya no genera duplicados ni errores aleatorios
**Fecha:** 2026-06-05  
**Archivo corregido:** `ProductosServiceImpl.java:352` — eliminado `parallelStream`, reemplazado por `saveAll()` en un solo batch

**Endpoint afectado:**
```
POST /productos/compartir-imagenes-variantes
```

**Dónde verlo en el panel:**
> Menú lateral → **Productos** → abrir un producto → sección de variantes → botón **Compartir imágenes a variantes** (asigna las imágenes del producto a todas sus variantes de una vez).

**Comportamiento ANTES del fix (incorrecto):**
- Se usaban dos `parallelStream` anidados para guardar cada relación variante-imagen en paralelo
- JPA no soporta múltiples hilos simultáneos sobre el mismo contexto de BD
- Podía resultar en: duplicados silenciosos, errores aleatorios tipo `ConcurrentModificationException`, o imágenes asignadas incorrectamente a variantes equivocadas

**Comportamiento DESPUÉS del fix (correcto):**
- Se arma toda la lista de relaciones variante-imagen en memoria y se persiste en **una sola llamada** `saveAll()`
- Sin hilos paralelos, sin riesgo de corrupción, más rápido porque es un solo INSERT en lote

**El front no necesita cambiar nada** — mismo endpoint, mismo request, mismo response.

---

### [BUG-KEY-04] ✅ Fix: reconciliación de imágenes ya no se bloquea permanentemente si hay un error
**Fecha:** 2026-06-05  
**Archivo corregido:** `ReconciliacionImagenService.java:68` — envuelto en `try/catch/finally`

**Endpoints afectados:**
```
POST /admin/reconciliacion/imagenes
POST /admin/reconciliacion/imagenes/limpiar-bd
GET  /admin/reconciliacion/imagenes/resultado
```

**Dónde verlo en el panel:**
> Menú lateral → **Administración** → sección **Reconciliación de imágenes** → botón **Iniciar reconciliación**.

**Comportamiento ANTES del fix (incorrecto):**
- Si durante la reconciliación ocurría cualquier error (BD caída, NPE, timeout), la bandera interna `enProceso` quedaba en `true` para siempre
- Desde ese momento todos los intentos de volver a reconciliar eran rechazados con "ya hay un proceso en curso"
- La única solución era reiniciar el servidor

**Comportamiento DESPUÉS del fix (correcto):**
- Si ocurre un error, se loguea, se guarda el resultado parcial que se alcanzó a procesar, y `enProceso` se libera en el bloque `finally` — siempre, pase lo que pase
- Se puede volver a lanzar la reconciliación inmediatamente sin reiniciar

**El front no necesita cambiar nada** — mismos endpoints, mismo response.

---

### [BUG-KEY-06] ✅ Fix: errores en endpoints de pedidos ya no devuelven pantalla en blanco
**Fecha:** 2026-06-05 | **Archivo:** `PedidoController.java`

**Endpoints afectados:**
```
GET    /pedidos/findPedido/{id}
GET    /pedidos/findPedido/{idPedido}/{idCliente}
GET    /pedidos/buscarClientePedido/{buscar}
DELETE /pedidos/delete/{id}
```
**Dónde verlo:** Menú → **Pedidos** → cualquier acción de consulta o eliminación.

**Antes:** si el backend tenía un error interno, retornaba `null` → el front recibía un 500 genérico sin body, causando pantalla en blanco o comportamiento indefinido.

**Después:** retorna `500 Internal Server Error` con body de error controlado que el front puede leer y mostrar mensaje al usuario.

**El front puede mejorar:** si el front ya maneja el caso de `response == null`, ahora puede leer `response.mensaje` para mostrar el error específico.

---

### [BUG-KEY-07] ✅ Fix: subida de imágenes ya no falla silenciosamente por JWT
**Fecha:** 2026-06-05 | **Archivo:** `ImageneClienteDisco.java:54`

**Endpoints afectados (indirectamente — flujo interno):**
```
POST /variantes/guardarConImagenes
POST /productos/save
PUT  /productos/update
```
**Dónde verlo:** Menú → **Productos** o **Variantes** → subir imágenes al guardar/actualizar.

**Antes:** si el contexto de seguridad no tenía credenciales JWT (ej. token expirado en ciertos flujos), lanzaba `NullPointerException` → 500 genérico sin mensaje claro.

**Después:** lanza `IllegalStateException` con mensaje descriptivo "No hay credenciales JWT en el contexto de seguridad" → más fácil de diagnosticar en logs.

**El front no necesita cambiar nada** — si ocurre, el error ahora llega con mensaje legible.

---

### [BUG-KEY-08] ✅ Fix: actualizar imagen de presentación ahora refresca la caché
**Fecha:** 2026-06-05 | **Archivo:** `ImagenPresentacionService.java`

**Endpoints afectados:**
```
PUT /presentacion/imagenes/{id}
PUT /presentacion/v1/imagenes/{id}
```
**Dónde verlo:** Menú → **Presentación** o **Inicio/Banner** → editar una imagen → guardar.

**Antes:** después de actualizar una imagen de presentación, la caché seguía sirviendo la imagen anterior hasta que expirara sola (podía tardar minutos).

**Después:** al actualizar, la caché se invalida automáticamente y todos los usuarios ven la imagen nueva de inmediato.

**El front no necesita cambiar nada.**

---

### [BUG-KEY-09] ✅ Fix: IDs de imagen ahora usan 128 bits — sin riesgo de colisión
**Fecha:** 2026-06-05 | **Archivo:** `ProductosServiceImpl.java`

**Endpoints afectados:**
```
POST /productos/save
PUT  /productos/update
```
**Dónde verlo:** Menú → **Productos** → agregar o editar producto con imágenes.

**Antes:** el ID de cada imagen se generaba con solo 63 bits de un UUID → con muchas imágenes acumuladas había riesgo de duplicados silenciosos o error de BD.

**Después:** usa XOR de las dos mitades del UUID (128 bits efectivos) → probabilidad de colisión prácticamente cero.

**El front no necesita cambiar nada.**

---

### [BUG-KEY-10] ✅ Fix: contrato de saveAll() ahora es correcto (void)
**Fecha:** 2026-06-05 | **Archivo:** `ImagenProductoClienteVPS.java` + `ImagenProductoPort.java`

**Flujo afectado (interno — no es endpoint público):**
Cuando se guarda un producto con imágenes, internamente se publica a RabbitMQ la relación producto-imagen.

**Antes:** el método declaraba retorno `ResponseGeneric<ProductoImagen>` pero siempre devolvía `null`. Engañoso y potencial NPE si algún caller usaba el retorno.

**Después:** retorno cambiado a `void` — el contrato refleja la realidad (fire-and-forget por Rabbit).

**El front no necesita cambiar nada.**

---

### [PERF-KEY-01] ✅ Fix: timeouts en llamadas HTTP al micro de imágenes
**Fecha:** 2026-06-05 | **Archivos:** `ImageneClienteDisco.java`, `ImagenProductoClienteVPS.java`

**Endpoints que mejoran (los que consultan imágenes al micro):**
```
GET /imagen/{id}
GET /imagen/v1/{productoId}
GET /variantes/buscar
GET /variantes/imagenes/{varianteId}
GET /productos/findById/{id}
```
**Dónde verlo:** cualquier pantalla que muestre imágenes de productos o variantes.

**Antes:** si el micro de imágenes tardaba o no respondía, el hilo de Tomcat quedaba bloqueado indefinidamente → con varios usuarios concurrentes el servidor dejaba de responder.

**Después:** timeout de 5 segundos en todas las llamadas HTTP. Si el micro no responde en 5s, libera el hilo y devuelve error controlado.

**El front puede notar:** en casos donde el micro de imágenes esté lento, ahora recibirá un error a los 5s en vez de esperar indefinidamente. Recomendable mostrar imagen placeholder si el response de imagen viene vacío.

---

### [PERF-KEY-02] ✅ Fix: listado de imágenes por producto ya pagina en SQL
**Fecha:** 2026-06-05 | **Archivo:** `ImagenServiceImpl.java`

**Endpoints afectados:**
```
GET /imagen/{id}/detalle?page=1&size=10
GET /imagen/v1/{productoId}/detalle?page=1&size=10
```
**Dónde verlo:** Menú → **Productos** → detalle de producto → galería de imágenes paginada.

**Antes:** cargaba TODAS las imágenes del producto en memoria (incluyendo leer los archivos del disco), luego recortaba por página en Java. Con muchas imágenes: memoria alta, respuesta lenta.

**Después:** la paginación ocurre en SQL — solo carga del disco las imágenes de la página actual. Respuesta significativamente más rápida y sin presión de memoria.

**El front no necesita cambiar nada** — mismo endpoint, mismo response, mismos parámetros `page` y `size`.

---

### [PERF-KEY-03] ✅ Fix: marcar imagen principal ya no hace N queries individuales
**Fecha:** 2026-06-05 | **Archivos:** `ProductosServiceImpl.java`, `IProductoImagenRepository.java`

**Endpoints afectados:**
```
POST /productos/save      (cuando se envía imagenPrincipalId)
PUT  /productos/update    (cuando se envía imagenPrincipalId)
```
**Dónde verlo:** Menú → **Productos** → guardar producto → el campo "imagen principal" del formulario.

**Antes:** por cada imagen del producto hacía 1 SELECT + N UPDATEs individuales (un UPDATE por imagen). Producto con 10 imágenes = 11 queries.

**Después:** 2 queries fijas sin importar cuántas imágenes tenga el producto: 1 UPDATE que desmarca todas + 1 UPDATE que marca la principal.

**El front no necesita cambiar nada.**

---

## ENDPOINTS MIGRADOS

---

### 1. Imagen principal de un producto en el listado

> El front **no necesita llamar a ningún endpoint adicional**. El response de `GET /productos/obtenerProductos` ya incluye el campo `urlImagen` que apunta directo a los bytes. El front solo hace `<img [src]="producto.imagen.urlImagen">`.

**`urlImagen` que viene en el listado de productos (a partir de ahora):**
```
http://localhost:9096/mis-productos/v1/imagenes/file/{imagenId}
```

**Response al llamar esa URL (micro_imagenes 9096):**
```
Content-Type: image/jpeg   (o image/png, image/gif)
Body: <bytes binarios>
```

**Response 204:** sin body — imagen no encontrada en disco (no explota con 500).

**Cambio respecto a la versión anterior:** antes la `urlImagen` apuntaba a `buscarImagenProducto/{productoId}` que devolvía JSON (no bytes). Ahora apunta directamente a `/imagenes/file/{imagenId}` — se puede usar directo como `src` del `<img>` sin ningún procesamiento.

---

### 2. Detalle paginado de imágenes de un producto

#### Version anterior — `GET /imagen/v3/{productoId}/detalle` ❌ Deprecated

| | |
|---|---|
| **Controlador** | `ImageneController` — `proyecto-key` — método `getDetalle()` |
| **Path param** | `productoId` (Integer) |
| **Query params** | `page` (int), `size` (int) |
| **Response 200** | `PageableDto` → lista de items: `{ idProducto, idImagen, name, price, inventoryStatus, extencion, image (bytes) }` |
| **RabbitMQ** | No aplica — lectura síncrona |
| **Acción front** | Sin cambio — sigue funcionando igual |

**Flujo interno:**
```
Front → proyecto-key ImageneController.getDetalle()
            └─► IImagenService.findImagenPrincipalPorProductoIds()
                      └─► consulta BD local (nombre, precio, stock, imagenId)
                      └─► por cada imagen: lee bytes del DISCO LOCAL de proyecto-key
```

---

#### Version nueva — `GET /imagen/v1/{productoId}/detalle` ✅ Usar esta — **proyecto-key (9091)** — se queda aquí

> Este endpoint **no puede moverse al micro** porque mezcla datos del producto (nombre, precio, stock) con bytes de imagen.

| | |
|---|---|
| **Controlador** | `ImageneController` — `proyecto-key` — método `getDetalleV2()` |
| **Path param** | `productoId` (Integer) — mismo que antes |
| **Query params** | `page` (int), `size` (int) — mismos que antes |
| **Response 200** | Misma estructura: `PageableDto` → lista de `{ idProducto, idImagen, name, price, inventoryStatus, extencion, image (bytes) }` |
| **RabbitMQ** | No aplica — lectura síncrona |
| **Acción front** | Cambiar URL de `/imagen/{id}/detalle` a `/imagen/v1/{id}/detalle` |

**Diferencia clave con la versión anterior:**
- `name`, `price`, `inventoryStatus`, `extencion` → siguen saliendo de la **BD local de proyecto-key** (el micro no tiene datos del producto)
- `image` (bytes) → ahora vienen del **microservicio de imágenes** en vez del disco local
- Si una imagen no existe en el micro → ese item llega con `image: null` + log en servidor (antes también podía ser null pero sin aviso)

**Flujo interno:**
```
Front → proyecto-key ImageneController.getDetalleV2()
            └─► IImagenService.findImagenPrincipalPorProductoIdsV2()
                      └─► consulta BD local (nombre, precio, stock, imagenId) ← igual que antes
                      └─► por cada imagen: ImagenPort.getOne(imagenId)
                                └─► HTTP → microservicio de imágenes → bytes del DISCO DEL MICRO
```

---

### 3. Obtener bytes de imagen por ID de imagen

#### Version anterior — `GET /imagen/v3/file/{imagenId}` ❌ Deprecated

| | |
|---|---|
| **Controlador** | `ImageneController` — `proyecto-key` — método `getImagenByImagenId()` |
| **Path param** | `imagenId` (Long) — ID de la imagen |
| **Response 200** | `byte[]` con header `Content-Type` |
| **Response error** | HTTP 500 si el archivo no existe en disco local |
| **RabbitMQ** | No aplica |
| **Acción front** | Sin cambio — sigue funcionando si el archivo está en disco local |

**Flujo interno:**
```
Front → proyecto-key ImageneController.getImagenByImagenId()
            └─► IImagenService.findByImagenId()
                      └─► busca en imagenes_copy → lee bytes del DISCO LOCAL de proyecto-key
```

---

#### Version nueva — `GET /imagenes/file/{imagenId}` ✅ Usar esta — **micro_imagenes (9096)**

| | |
|---|---|
| **Micro** | `micro_imagenes` — `ImagenController.getImagenBytes()` |
| **Path param** | `imagenId` (Long) — mismo que antes |
| **Response 200** | `byte[]` con header `Content-Type` |
| **Response sin imagen** | HTTP 204 No Content (antes daba 500) |
| **Acción front** | Cambiar URL a `GET http://localhost:9096/mis-productos/v1/imagenes/file/{imagenId}` |

**Request:**
```
GET http://localhost:9096/mis-productos/v1/imagenes/file/123
```

**Response 200:**
```
Content-Type: image/jpeg   (o image/png, image/gif)
Body: <bytes binarios>
```

**Response 204:** sin body — imagen no encontrada en disco.

**Diferencia clave:** el front llama directo al micro — proyecto-key ya no intermedia. Los bytes vienen del disco del micro.

**Flujo:**
```
Front → GET /mis-productos/imagenes/file/{imagenId}   ← micro_imagenes directo
            └─► imagenes_copy (BD compartida) → obtiene nombre de archivo
            └─► lee bytes del DISCO DEL MICRO
            ← byte[] + Content-Type
```

---

### 4. Listado de imágenes de un producto (metadata + URLs)

#### Version anterior — `GET /imagen/v3/{idProducto}/imagenes` ❌ Deprecated

| | |
|---|---|
| **Controlador** | `ImageneController` — `proyecto-key` — método `getImagenesPorProductoId()` |
| **Path param** | `idProducto` (Integer) |
| **Response 200** | `ProductoImagenDto` → `{ productoId, listaImagenes: [{ id, extension, nombreImagen, urlImagen, principal }] }` |
| **urlImagen apunta a** | `GET /imagen/v3/file/{imagenId}` — disco local |
| **RabbitMQ** | No aplica |
| **Acción front** | Sin cambio — sigue funcionando |

---

#### Version nueva — `GET /producto-imagen/listar/{productoId}` ✅ Usar esta — **micro_imagenes (9096)**

| | |
|---|---|
| **Micro** | `micro_imagenes` — `ProductoImagenController.listarImagenesProducto()` |
| **Path param** | `productoId` (Integer) — mismo que antes |
| **Response 200** | Misma estructura — `{ productoId, listaImagenes: [{id, extension, nombreImagen, urlImagen, principal}] }` |
| **urlImagen apunta a** | `GET /mis-productos/imagenes/file/{imagenId}` — micro_imagenes |
| **Acción front** | Cambiar URL a `GET http://localhost:9096/mis-productos/producto-imagen/listar/{productoId}` |

**Request:**
```
GET http://localhost:9096/mis-productos/producto-imagen/listar/10
Authorization: Bearer <token>
```

**Response 200:**
```json
{
  "productoId": 10,
  "listaImagenes": [
    { "id": 123, "extension": "jpg", "nombreImagen": "foto.jpg", "urlImagen": "/mis-productos/imagenes/file/123", "principal": true },
    { "id": 124, "extension": "png", "nombreImagen": "foto2.png", "urlImagen": "/mis-productos/imagenes/file/124", "principal": false }
  ]
}
```

**Diferencia clave:** el front llama directo al micro. La `urlImagen` ya apunta al endpoint de bytes del micro — el front no cambia cómo procesa la respuesta, solo la URL del request.

**Flujo:**
```
Front → GET /mis-productos/producto-imagen/listar/{productoId}   ← micro_imagenes directo
            └─► JOIN producto_imagen_copy + imagenes_copy (BD compartida)
            └─► urlImagen = /mis-productos/imagenes/file/{id}
            ← { productoId, listaImagenes:[...] }
```

---

### 5. Eliminar imagen por ID

#### Versión anterior — `DELETE /imagen/v3/{idImagen}` ❌ Deprecated (proyecto-key)

Solo borraba de la BD local — el archivo quedaba en disco del micro.

#### Versión final — `DELETE /producto-imagen/{id}` ✅ Usar esta — **micro_imagenes (9096)**

> `{id}` = el ID de la imagen (Long) — el mismo valor que antes se mandaba a proyecto-key.

**Request:**
```
DELETE http://localhost:9096/mis-productos/producto-imagen/123
Authorization: Bearer <token>
```

**Response 200:**
```json
{ "response": {} }
```

**Diferencia clave:** el front llama directo al micro. Borra el archivo del disco, el registro de `imagenes_copy` y la relación de `producto_imagen_copy` — todo en una sola llamada. Ya no pasa por proyecto-key.

**Flujo:**
```
Front → DELETE /mis-productos/producto-imagen/{imagenId}   ← micro_imagenes directo
            └─► busca relación por imagenId en producto_imagen_copy
            └─► borra archivo del disco del micro
            └─► borra registro de imagenes_copy
            └─► borra relación de producto_imagen_copy
            ← 200 OK
```

---

### 6. Eliminar imágenes específicas de un producto — **proyecto-key (9091)** — se queda aquí

> No puede moverse al micro porque necesita verificar `variante_imagen` que es tabla de proyecto-key.

| | `DELETE /imagen/v3/{productoId}/imagenes` ❌ Deprecated | `DELETE /imagen/v1/{productoId}/imagenes` ✅ Usar esta |
|---|---|---|
| **URL completa** | `http://localhost:9091/mis-productos/imagen/v3/{id}/imagenes` | `http://localhost:9091/mis-productos/imagen/v1/{id}/imagenes` |
| **Body** | `[imagenId1, imagenId2, ...]` (Long[]) | mismo |
| **Response** | HTTP 200 `{ message }` | HTTP 200 `{ message }` — mismo |

---

### 7. Eliminar todas las imágenes de varios productos — **proyecto-key (9091)** — se queda aquí

> Misma razón que el punto 6.

| | `DELETE /imagen/v3/producto` ❌ Deprecated | `DELETE /imagen/v1/producto` ✅ Usar esta |
|---|---|---|
| **URL completa** | `http://localhost:9091/mis-productos/imagen/v3/producto` | `http://localhost:9091/mis-productos/imagen/v1/producto` |
| **Body** | `[productoId1, productoId2, ...]` (Integer[]) | mismo |
| **Response** | HTTP 200 `{ message }` | HTTP 200 `{ message }` — mismo |

---

### 8. Limpiar caché de imágenes

| | `GET /imagen/v3/cache/imagen/limpiar` ❌ Deprecated | `GET /imagen/v1/cache/limpiar` ✅ Usar esta |
|---|---|---|
| **Controlador** | `ImageneController` — `limpiarTodaLaCacheDeImagenes()` | `ImageneController` — `limpiarCacheImagenesV2()` |
| **Response** | void | HTTP 204 No Content |
| **Diferencia** | Solo evicta caché `imagenes` | Evicta `imagenes`, `detalleImagen`, `detalle`, `detalle-v2`, `buscarImagenIdCache` |
| **RabbitMQ** | No aplica | TODO: publicar evento para invalidar caché en todos los nodos |
| **Acción front** | Sin cambio | Cambiar URL a `/imagen/v1/cache/limpiar` |

---

## ENDPOINTS MIGRADOS (continuación)

---

### 9. Imágenes activas de presentación por tipo (LOGIN / REGISTRO)

#### Versión anterior — `GET /presentacion/v3/imagenes?tipo=LOGIN` ❌ Deprecated

| | |
|---|---|
| **Controlador** | `ImagenPresentacionController` — `getImagenes()` |
| **Query param** | `tipo` (String: `LOGIN` \| `REGISTRO`) |
| **Response 200** | `ResponseGeneric<List<ImagenPresentacion>>` — entidad directa con `nombreArchivo` (ruta de disco interno) |
| **RabbitMQ** | No aplica |
| **Acción front** | Sin cambio — sigue funcionando |

**Request:**
```
GET /mis-productos/presentacion/v3/imagenes?tipo=LOGIN
```

**Response 200:**
```json
{
  "mensaje": "La peticion fue exitosa",
  "code": 200,
  "data": [
    {
      "id": 1,
      "tipo": "LOGIN",
      "orden": 1,
      "nombreArchivo": "uuid_banner.jpg",
      "extension": "jpg",
      "nombreOriginal": "banner.jpg",
      "descripcion": "Banner principal de login",
      "activo": true,
      "actualizadoEn": "2026-05-21T10:00:00"
    }
  ],
  "lista": null
}
```

**Flujo interno:**
```
Front → getImagenes()
    └─► ImagenPresentacionService.getImagenesPorTipo()
              └─► IImagenPresentacionRepository.findByTipoAndActivoOrderByOrden()
                        └─► BD local → devuelve entidad con nombreArchivo (disco local)
```

---

#### Versión nueva — `GET /presentacion/v1/imagenes?tipo=LOGIN` ✅ Usar esta

| | |
|---|---|
| **Controlador** | `ImagenPresentacionController` — `getImagenesV2()` |
| **Query param** | `tipo` (String: `LOGIN` \| `REGISTRO`) — mismo que antes |
| **Response 200** | `ResponseGeneric<List<ImagenPresentacionDto>>` — DTO con `urlImagen` calculada |
| **Response sin datos** | HTTP 200 con `data: []` (lista vacía) |
| **Cache** | `@Cacheable("presentacion-imagenes")` por `tipo` |
| **RabbitMQ** | **NO aplica** — lectura síncrona. TODO: cuando se implemente `PUT /presentacion/v1/imagenes/{id}`, publicar evento `cache.evict.presentacion` en `exchange.imagenes` para invalidar caché en todos los nodos |
| **Acción front** | Cambiar URL a `/presentacion/v1/imagenes?tipo=...` y usar `urlImagen` del DTO para cargar la imagen |

**Request:**
```
GET /mis-productos/presentacion/v1/imagenes?tipo=LOGIN
```

**Response 200:**
```json
{
  "mensaje": "La peticion fue exitosa",
  "code": 200,
  "data": [
    {
      "id": 1,
      "tipo": "LOGIN",
      "orden": 1,
      "extension": "jpg",
      "nombreOriginal": "banner.jpg",
      "descripcion": "Banner principal de login",
      "activo": true,
      "actualizadoEn": "2026-05-21T10:00:00",
      "urlImagen": "/presentacion/v1/imagenes/1/imagen"
    }
  ],
  "lista": null
}
```

**Diferencia clave con la versión anterior:**
- Ya **no expone** `nombreArchivo` (ruta de disco interno)
- Agrega `urlImagen` → apunta a `GET /presentacion/v1/imagenes/{id}/imagen` (bytes desde el micro)
- La respuesta se cachea — menor carga en BD en producción

**Flujo interno:**
```
Front → getImagenesV2()
    └─► ImagenPresentacionService.getImagenesPorTipoV2()   ← @Cacheable("presentacion-imagenes")
              └─► IImagenPresentacionRepository.findByTipoAndActivoOrderByOrden()
                        └─► BD local → mapea a ImagenPresentacionDto con urlImagen calculada
```

---

---

### 10. Bytes de imagen de presentación por ID

#### Versión anterior — `GET /presentacion/v3/imagenes/{id}/imagen` ❌ Deprecated

| | |
|---|---|
| **Controlador** | `ImagenPresentacionController` — `getImagen()` |
| **Path param** | `id` (Integer) — ID de la `ImagenPresentacion` |
| **Response 200** | `byte[]` con header `Content-Type: image/jpeg \| image/png \| ...` |
| **Response error** | HTTP 500 si el archivo no existe en disco |
| **RabbitMQ** | No aplica |
| **Acción front** | Sin cambio — sigue funcionando |

**Request:**
```
GET /mis-productos/presentacion/imagenes/1/imagen
```

**Response 200:**
```
Content-Type: image/jpeg   (o image/png, image/gif)
Body: <bytes binarios — usar directamente como src de <img> o blob>
```

**Response 500:** archivo no encontrado en disco.

---

#### Versión nueva — `GET /presentacion/v1/imagenes/{id}/imagen` ✅ Usar esta

| | |
|---|---|
| **Path param** | `id` (Integer) — mismo que antes |
| **Acción front** | Si ya usas `GET /presentacion/v1/imagenes?tipo=...`, el campo `urlImagen` de cada item ya apunta a esta URL — sin cambio adicional. Solo actualizar si tenías la URL hardcodeada. |

**Request:**
```
GET /mis-productos/presentacion/v1/imagenes/1/imagen
```

**Response 200:**
```
Content-Type: image/jpeg   (o image/png, image/gif según la imagen)
Body: <bytes binarios — usar directamente como src de <img> o blob>
```

**Response 204:** sin body — imagen no encontrada (ya no explota con 500).

**Diferencia clave:** igual que v1 pero devuelve **204** en vez de **500** cuando no existe el archivo.

---

---

### 11. Listar todas las imágenes de presentación (ADMIN)

#### Versión anterior — `GET /presentacion/v3/imagenes/todas` ❌ Deprecated

**Request:**
```
GET /mis-productos/presentacion/v3/imagenes/todas
Authorization: Bearer <token>
```

**Response 200:**
```json
{
  "data": [
    {
      "id": 1,
      "tipo": "LOGIN",
      "orden": 1,
      "nombreArchivo": "uuid_banner.jpg",
      "extension": "jpg",
      "nombreOriginal": "banner.jpg",
      "descripcion": "Banner principal",
      "activo": true,
      "actualizadoEn": "2026-05-21T10:00:00"
    }
  ]
}
```

---

#### Versión nueva — `GET /presentacion/v1/imagenes/todas` ✅ Usar esta

**Request:**
```
GET /mis-productos/presentacion/v1/imagenes/todas
Authorization: Bearer <token>
```

**Response 200:**
```json
{
  "data": [
    {
      "id": 1,
      "tipo": "LOGIN",
      "orden": 1,
      "extension": "jpg",
      "nombreOriginal": "banner.jpg",
      "descripcion": "Banner principal",
      "activo": true,
      "actualizadoEn": "2026-05-21T10:00:00",
      "urlImagen": "/presentacion/v1/imagenes/1/imagen"
    }
  ]
}
```

**Diferencia clave:** ya no expone `nombreArchivo` (ruta interna del servidor). Usar `urlImagen` para mostrar la imagen.

---

### 12. Actualizar imagen de presentación (ADMIN)

#### Versión anterior — `PUT /presentacion/v3/imagenes/{id}` ❌ Deprecated

**Request:**
```
PUT /mis-productos/presentacion/imagenes/1
Authorization: Bearer <token>
Content-Type: application/json

{
  "base64": "<bytes[]>",
  "extension": "jpg",
  "nombreImagen": "banner.jpg",
  "descripcion": "Banner principal",
  "activo": true
}
```

> `base64` es opcional — si no se envía, solo se actualizan `descripcion` y `activo`.

**Response 200:**
```json
{
  "data": {
    "id": 1,
    "tipo": "LOGIN",
    "orden": 1,
    "nombreArchivo": "uuid_banner.jpg",
    "extension": "jpg",
    "nombreOriginal": "banner.jpg",
    "descripcion": "Banner principal",
    "activo": true,
    "actualizadoEn": "2026-05-21T10:00:00"
  }
}
```

---

#### Versión nueva — `PUT /presentacion/v1/imagenes/{id}` ✅ Usar esta

**Request:** igual que v1 — mismo body, mismo token ADMIN.

```
PUT /mis-productos/presentacion/v1/imagenes/1
Authorization: Bearer <token>
Content-Type: application/json

{
  "base64": "<bytes[]>",
  "extension": "jpg",
  "nombreImagen": "banner.jpg",
  "descripcion": "Banner principal",
  "activo": true
}
```

**Response 200:**
```json
{
  "data": {
    "id": 1,
    "tipo": "LOGIN",
    "orden": 1,
    "extension": "jpg",
    "nombreOriginal": "banner.jpg",
    "descripcion": "Banner principal",
    "activo": true,
    "actualizadoEn": "2026-05-21T10:00:00",
    "urlImagen": "/presentacion/v1/imagenes/1/imagen"
  }
}
```

**Diferencia clave:**
- Ya no devuelve `nombreArchivo` (ruta interna del servidor)
- **Invalida automáticamente el caché** `presentacion-imagenes` — el próximo `GET /presentacion/v1/imagenes?tipo=...` devuelve datos frescos
- RabbitMQ: TODO para invalidar caché en multi-nodo (por ahora se invalida solo el nodo que recibe el PUT)

---

### 13. Imágenes de una variante por ID

#### Versión anterior — `GET /variantes/v3/imagenes/{varianteId}` ❌ Deprecated

**Request:**
```
GET /mis-productos/variantes/v3/imagenes/5
```

**Response 200:**
```json
{
  "data": [
    {
      "id": "123",
      "extension": "jpg",
      "nombreImagen": "foto.jpg",
      "urlImagen": "http://micro-imagenes/imagenes/123",
      "principal": true
    }
  ]
}
```

> Puede devolver `urlImagen` con URLs rotas si el archivo ya no existe en el micro.

---

#### Versión nueva — `GET /variantes/v1/imagenes/{varianteId}` ✅ Usar esta

**Request:**
```
GET /mis-productos/variantes/v1/imagenes/5
```

**Response 200:**
```json
{
  "data": [
    {
      "id": "123",
      "extension": "jpg",
      "nombreImagen": "foto.jpg",
      "urlImagen": "http://micro-imagenes/imagenes/123",
      "principal": true
    }
  ]
}
```

**Response 200 sin imágenes:** `{ "data": [] }`

**Diferencia clave:** antes de responder verifica en el micro cuáles archivos existen — solo devuelve imágenes con archivo real. Nunca devuelve URLs rotas.

---

### 14. Eliminar todas las imágenes de varias variantes (ADMIN)

| | `DELETE /variantes/v3/imagenes` ❌ Deprecated | `DELETE /variantes/v1/imagenes` ✅ Usar esta |
|---|---|---|
| **Auth** | Bearer token ADMIN | igual |
| **Body** | `[varianteId1, varianteId2, ...]` (Integer[]) | igual |
| **Response 200** | `{ "data": "Imágenes eliminadas correctamente" }` | igual |
| **Diferencia** | misma lógica | misma lógica — solo cambia la URL |

**Request:**
```
DELETE /mis-productos/variantes/v1/imagenes
Authorization: Bearer <token>
Content-Type: application/json

[1, 2, 3]
```

**Response 200:**
```json
{ "data": "Imágenes eliminadas correctamente" }
```

---

### 15. Eliminar imágenes específicas de una variante (ADMIN)

| | `DELETE /variantes/v3/{varianteId}/imagenes` ❌ Deprecated | `DELETE /variantes/v1/{varianteId}/imagenes` ✅ Usar esta |
|---|---|---|
| **Auth** | Bearer token ADMIN | igual |
| **Path param** | `varianteId` (Integer) | igual |
| **Body** | `[imagenId1, imagenId2, ...]` (Long[]) | igual |
| **Response 200** | `{ "data": "Imágenes eliminadas correctamente" }` | igual |
| **Diferencia** | misma lógica | misma lógica — solo cambia la URL |

**Request:**
```
DELETE /mis-productos/variantes/v1/5/imagenes
Authorization: Bearer <token>
Content-Type: application/json

[123, 456]
```

**Response 200:**
```json
{ "data": "Imágenes eliminadas correctamente" }
```

> Ambos DELETEs ya eliminaban de BD local **y** del micro en la versión anterior. El cambio es solo la URL.

## PENDIENTES DE MIGRAR

---

## CAMBIOS ADICIONALES EN MICRO_IMAGENES

---

### 16. Listar imágenes de un producto — ahora paginado

**Endpoint:** `GET /producto-imagen/listar/{productoId}` — **micro_imagenes (9096)**

> Este endpoint ya se documentó en el punto 4. Ahora se le agregó paginación.

**Request:**
```
GET http://localhost:9096/mis-productos/producto-imagen/listar/265?pagina=1&size=8
Authorization: Bearer <token>
```

| Param | Tipo | Obligatorio | Default | Descripción |
|---|---|---|---|---|
| `pagina` | Integer | No | `1` | Número de página (empieza en 1) |
| `size` | Integer | No | `8` | Imágenes por página |

**Response 200:**
```json
{
  "productoId": 265,
  "listaImagenes": [
    {
      "id": 123,
      "extension": "image/jpeg",
      "nombreImagen": "foto.jpg",
      "urlImagen": "http://localhost:9096/mis-productos/v1/imagenes/file/123",
      "principal": true
    }
  ],
  "pagina": 1,
  "totalPaginas": 3,
  "totalImagenes": 20
}
```

**Cómo navegar páginas desde el front:**
```
Primera página:   GET .../listar/265?pagina=1&size=8
Segunda página:   GET .../listar/265?pagina=2&size=8
Última página:    GET .../listar/265?pagina={totalPaginas}&size=8
```

**Diferencia clave:** antes devolvía todas las imágenes sin límite. Ahora devuelve 8 por página. Usar `totalPaginas` para renderizar los botones de paginación. Si no se mandan params, devuelve la primera página con 8 imágenes.

---

### 17. DetalleProductoComponent — imágenes del producto con URL en lugar de bytes

#### Versión anterior — `GET /imagen/v3/{productoId}/detalle` ❌ Deprecated (proyecto-key 9091)

Devolvía bytes embebidos en el response (pesado, lento).

**Request:**
```
GET http://localhost:9091/mis-productos/imagen/265/detalle?size=4&page=0
Authorization: Bearer <token>
```

**Response 200:**
```json
{
  "list": [
    {
      "idProducto": 265,
      "idImagen": 123,
      "name": "prod",
      "price": 1.0,
      "inventoryStatus": "INSTOCK",
      "extencion": "jpg",
      "image": "/9j/4AAQSkZJRgAB..."
    }
  ],
  "totalPaginas": 3
}
```

---

#### Versión nueva — `GET /producto-imagen/listar/{productoId}` ✅ Usar esta — **micro_imagenes (9096)**

Devuelve URLs — el front carga cada imagen con `<img [src]="imagen.urlImagen">`.

**Request:**
```
GET http://localhost:9096/mis-productos/producto-imagen/listar/265?pagina=1&size=8
Authorization: Bearer <token>
```

**Response 200:**
```json
{
  "productoId": 265,
  "listaImagenes": [
    {
      "id": "3855830153700593542",
      "extension": "image/jpeg",
      "nombreImagen": "foto.jpg",
      "urlImagen": "http://localhost:9096/mis-productos/v1/imagenes/file/3855830153700593542",
      "principal": true
    },
    {
      "id": "7565125362907238017",
      "extension": "image/jpeg",
      "nombreImagen": "foto2.jpg",
      "urlImagen": "http://localhost:9096/mis-productos/v1/imagenes/file/7565125362907238017",
      "principal": false
    }
  ],
  "pagina": 1,
  "totalPaginas": 2,
  "totalImagenes": 10
}
```

**Cómo mostrar cada imagen en el front:**
```html
<img [src]="imagen.urlImagen" />
```

**Cómo navegar páginas:**
```
GET .../listar/265?pagina=1&size=8   ← primera página
GET .../listar/265?pagina=2&size=8   ← siguiente página
```

**Diferencia clave:**
- Ya no vienen bytes embebidos (`image: "base64..."`) — el front usa `urlImagen` directamente
- El campo `id` es **string** (no number) — JS no puede representar estos IDs como Number sin perder precisión
- `principal: true` indica cuál es la imagen principal del producto

---

### 18. DetalleProductoComponent — eliminar imágenes

#### Versión anterior — `DELETE /imagen/v3/{productoId}/imagenes` ❌ Deprecated (proyecto-key 9091)

**Request:**
```
DELETE http://localhost:9091/mis-productos/imagen/265/imagenes
Authorization: Bearer <token>
Content-Type: application/json

["3855830153700593542", "7565125362907238017"]
```

**Response 200:**
```json
{ "data": "Imágenes eliminadas correctamente" }
```

---

#### Versión nueva — `DELETE /imagen/v1/{productoId}/imagenes` ✅ Usar esta (proyecto-key 9091)

**Request:**
```
DELETE http://localhost:9091/mis-productos/imagen/v1/265/imagenes
Authorization: Bearer <token>
Content-Type: application/json

["3855830153700593542", "7565125362907238017"]
```

**Response 200:**
```json
{ "data": "Imágenes eliminadas correctamente" }
```

**Diferencia clave:** verifica si la imagen es compartida con otras variantes antes de borrarla del disco — si la comparte, solo borra la relación del producto sin borrar el archivo.

> **Nota:** los IDs se mandan como strings (igual que vienen del `listar`).

---

---

## LISTADO DE VARIANTES — `/variantes/buscar`

**Endpoint:** `GET http://localhost:9091/mis-productos/variantes/buscar?termino=&pagina=1&size=10`

**Response 200:**
```json
{
  "data": {
    "pagina": 1,
    "totalPaginas": 3,
    "totalRegistros": 25,
    "t": [
      {
        "id": 5,
        "talla": "M",
        "descripcion": "Pantalón slim",
        "color": "Azul",
        "presentacion": null,
        "stock": 10,
        "marca": "...",
        "contenidoNeto": null,
        "imagenBase64": null,
        "imagenUrl": "http://localhost:9096/mis-productos/v1/imagenes/file/7305237692097776164",
        "precio": 99.99,
        "codigoBarras": "...",
        "nombreProducto": "Jeans Slim"
      }
    ]
  }
}
```

**Claves para el front:**
- La imagen viene en `data.t[i].imagenUrl` — no en `imagenBase64` (siempre `null`)
- `imagenUrl` es una URL completa a bytes directos: usar `<img [src]="variante.imagenUrl">`
- Si `imagenUrl` es `null` → la variante no tiene imagen asignada
- La lista de variantes está en `data.t` (no `data.content`, no `data.items`)
- Paginación: `data.pagina`, `data.totalPaginas`, `data.totalRegistros`
- El back selecciona la imagen marcada como principal; si ninguna lo es, usa la primera disponible

---

## RESUMEN POR COMPONENTE

> Referencia rápida para el equipo de front — qué endpoint usa cada componente.

---

### UpdateComponent (editar producto)

| Acción | Método | URL | Body / Params |
|---|---|---|---|
| Listar imágenes del producto | GET | `http://localhost:9096/mis-productos/v1/producto-imagen/listar/{productoId}?pagina=1&size=8` | — |
| Ver bytes de una imagen | GET | `http://localhost:9096/mis-productos/v1/imagenes/file/{imagenId}` | — |
| Eliminar una imagen | DELETE | `http://localhost:9096/mis-productos/v1/producto-imagen/{imagenId}` | — |
| Marcar imagen como principal | PUT | `http://localhost:9096/mis-productos/v1/producto-imagen/{id}/principal` | — |

> `imagenId` viene del campo `id` (string) del response de `listar`.

---

### DetalleProductoComponent (detalle y carrusel del producto)

| Acción | Método | URL | Body / Params |
|---|---|---|---|
| Listar imágenes del producto | GET | `http://localhost:9096/mis-productos/v1/producto-imagen/listar/{productoId}?pagina=1&size=8` | — |
| Ver bytes de una imagen | GET | usar `urlImagen` del response de `listar` directamente en `<img [src]>` | — |
| Eliminar imágenes seleccionadas (batch) | DELETE | `http://localhost:9091/mis-productos/imagen/v1/{productoId}/imagenes` | `["imagenId1", "imagenId2"]` |

---

### LoginFormComponent / AddUsuariosComponent (imágenes de login/registro)

| Acción | Método | URL | Body / Params |
|---|---|---|---|
| Listar imágenes por tipo | GET | `http://localhost:9091/mis-productos/presentacion/v1/imagenes?tipo=LOGIN` | — |
| Ver bytes de una imagen | GET | usar `urlImagen` del response directamente en `<img [src]>` | — |

---

### PresentacionImagenesComponent (admin — imágenes de presentación)

| Acción | Método | URL | Body / Params |
|---|---|---|---|
| Listar todas (activas e inactivas) | GET | `http://localhost:9091/mis-productos/presentacion/v1/imagenes/todas` | Bearer token ADMIN |
| Actualizar imagen/descripción | PUT | `http://localhost:9091/mis-productos/presentacion/v1/imagenes/{id}` | `{ base64, extension, nombreImagen, descripcion, activo }` |

---

### DetalleVarianteComponent / UpdateVarianteComponent (imágenes de variante)

| Acción | Método | URL | Body / Params |
|---|---|---|---|
| Listar imágenes de variante | GET | `http://localhost:9091/mis-productos/variantes/v1/imagenes/{varianteId}` | — |
| Eliminar imágenes específicas | DELETE | `http://localhost:9091/mis-productos/variantes/v1/{varianteId}/imagenes` | `[imagenId1, imagenId2]` |
| Marcar imagen como principal | PUT | `http://localhost:9091/mis-productos/variantes/imagenes/{imagenId}/principal` | — |

---

## GLOSARIO

- **@Deprecated**: el endpoint original, sin tocar, sigue funcionando
- **v1**: el endpoint activo/estable que delega al microservicio de imágenes (antes llamado "v2"; se renombró a `v1` el 2026-06-07 — ver sección "MIGRACIÓN DE VERSIONES DE URL")
- **v3**: el endpoint antiguo/deprecado (antes era la ruta sin versión, ahora vive bajo `/v3/` para no chocar con `/v1/`)
- **204 No Content**: no hay imagen disponible, no es un error
- **RabbitMQ — No aplica**: lectura síncrona, no hay eventos
- **RabbitMQ — TODO**: hay una oportunidad de usar Rabbit aquí pero aún no está implementado

---

---

## CAMBIOS DE BACKEND — 2026-05-22 — Acciones requeridas en el front

> Estos cambios ya están aplicados en el backend (rama `dev`). El front debe actualizar los componentes indicados.

---

### CAMBIO A — Listado de variantes: `imagenUrl` ahora siempre viene poblada

**Endpoint afectado:** `GET /mis-productos/variantes/buscar?termino=&pagina=1&size=10`

**Qué cambió en el back:**
Antes el back verificaba contra el microservicio de imágenes si el archivo existía en disco antes de incluir la URL. Si esa verificación fallaba (error de red, micro lento) la `imagenUrl` llegaba `null` aunque la variante tuviera imagen. Ahora el back asigna la URL directamente desde la base de datos, sin verificación extra.

**Comportamiento nuevo:**
- Si la variante tiene imágenes → `imagenUrl` siempre viene con valor
- Si la variante NO tiene ninguna imagen asignada → `imagenUrl` es `null`
- Si el archivo ya no existe en disco → el micro devuelve `204 No Content` al hacer `GET imagenes/file/{id}` (el `<img>` no muestra nada, no explota)
- La imagen seleccionada es la marcada como **principal**; si ninguna lo es, la de **id más bajo**

**Response (no cambia la estructura, cambia el valor):**
```json
{
  "data": {
    "pagina": 1,
    "totalPaginas": 3,
    "totalRegistros": 25,
    "t": [
      {
        "id": 5,
        "talla": "M",
        "descripcion": "Pantalón slim",
        "color": "Azul",
        "stock": 10,
        "marca": "Marca X",
        "imagenBase64": null,
        "imagenUrl": "http://localhost:9096/mis-productos/v1/imagenes/file/7305237692097776164",
        "precio": 99.99,
        "codigoBarras": "1234567890",
        "nombreProducto": "Jeans Slim"
      }
    ]
  }
}
```

**Acción requerida en el front:**
```html
<!-- Antes: el front no mostraba nada porque imagenUrl llegaba null -->
<!-- Ahora: usar directo como src -->
<img [src]="variante.imagenUrl" *ngIf="variante.imagenUrl" />
```

- **No usar** `imagenBase64` — siempre es `null`
- **No filtrar** por `principal` — el back ya eligió la imagen correcta
- La lista de variantes está en `response.data.t` (no `data.content` ni `data.items`)

**Componentes que deben actualizarse:**
- Cualquier componente que liste variantes con imagen (catálogo, búsqueda, etc.)

---

### CAMBIO B — Listado de productos: `urlImagen` ahora apunta directo a los bytes

**Endpoints afectados:**
- `GET /mis-productos/productos/obtenerProductos?page=1&size=10`
- `GET /mis-productos/productos/buscarNombreOrCodigoBarra?nombre=...&page=1&size=10`

**Qué cambió en el back:**
Antes `producto.imagen.urlImagen` apuntaba a `buscarImagenProducto/{productoId}` que devuelve un **JSON** (no bytes). El front tenía que llamar ese endpoint, extraer el `id` del JSON y luego llamar `/imagenes/file/{id}` para obtener los bytes.

Ahora `producto.imagen.urlImagen` apunta directamente a `/imagenes/file/{imagenId}` — **devuelve bytes**, se puede usar directo como `src` del `<img>`.

**Valor anterior de `urlImagen`:**
```
http://localhost:9096/mis-productos/producto-imagen/buscarImagenProducto/265
→ devolvía JSON: { id, imagen (base64), urlImagen (filename), contentType }
```

**Valor nuevo de `urlImagen`:**
```
http://localhost:9096/mis-productos/v1/imagenes/file/7305237692097776164
→ devuelve bytes directos (Content-Type: image/jpeg)
```

**Response de `obtenerProductos` (estructura no cambia, cambia el valor de `urlImagen`):**
```json
{
  "data": {
    "pagina": 1,
    "totalPaginas": 5,
    "totalRegistros": 48,
    "t": [
      {
        "idProducto": 265,
        "nombre": "Great Jeans",
        "color": "Azul",
        "precioVenta": 150.0,
        "descripcion": "...",
        "codigoBarras": "...",
        "stock": 10,
        "imagen": {
          "urlImagen": "http://localhost:9096/mis-productos/v1/imagenes/file/7305237692097776164"
        }
      }
    ]
  }
}
```

**Acción requerida en el front:**
```html
<!-- Antes: llamar buscarImagenProducto, extraer id, luego llamar /imagenes/file/{id} -->
<!-- Ahora: usar directo -->
<img [src]="producto.imagen?.urlImagen" *ngIf="producto.imagen?.urlImagen" />
```

- Si el producto **no tiene imagen asignada** → `imagen.urlImagen` es `null` (o `imagen` puede ser un objeto con `urlImagen: null`)
- Si el archivo no existe en disco → micro devuelve `204`, el `<img>` no muestra nada
- **Eliminar** toda lógica que llame `buscarImagenProducto` para obtener la imagen del listado

**Componentes que deben actualizarse:**
- Componente de listado/catálogo de productos
- Componente de búsqueda de productos
- Cualquier componente que use `obtenerProductos` o `buscarNombreOrCodigoBarra` y muestre imagen

---

### Resumen de acciones — tabla rápida

| Componente | Qué cambiar |
|---|---|
| Listado/catálogo de variantes | Usar `variante.imagenUrl` directo en `<img [src]>`. No filtrar por principal. |
| Listado/catálogo de productos | Usar `producto.imagen.urlImagen` directo en `<img [src]>`. Eliminar la llamada intermedia a `buscarImagenProducto`. |
| Búsqueda de productos (`buscarNombreOrCodigoBarra`) | Igual que listado de productos — misma estructura de response. |

---

### Lo que NO cambia

- Endpoints de detalle de imágenes de variante: `GET /variantes/v1/imagenes/{varianteId}` — sin cambios (renombrado de `v2` a `v1`, ver sección "MIGRACIÓN DE VERSIONES DE URL")
- Endpoints de imágenes de producto en detalle: `GET /producto-imagen/listar/{productoId}` — sin cambios
- Endpoints de eliminación y marcado de principal — sin cambios
- Estructura general del response (`data.t`, `data.pagina`, etc.) — sin cambios

---

## CAMBIOS INTERNOS — RabbitMQ + Caché (sin impacto en el contrato de API)

> **El front NO necesita cambiar nada.** Request, response y URLs son exactamente los mismos.
> Estos cambios son internos: ahora cualquier escritura invalida la caché en **todos los nodos** del back
> vía RabbitMQ, en lugar de solo en el nodo que procesó el request.

### Qué cambió internamente

Antes: cada método de escritura usaba `@CacheEvict` con una lista de caches específicas. Si el back corría en varios nodos, solo el nodo que recibía el request limpiaba su caché — los otros seguían sirviendo datos viejos.

Ahora: cualquier escritura hace dos cosas:
1. Llama a `CacheService.evictAll()` → limpia **todas** las caches del nodo actual
2. Publica un evento `cache.evict.all` a RabbitMQ → todos los demás nodos reciben el evento y también limpian sus caches

---

### Endpoints afectados (mismo contrato, nuevo comportamiento de caché)

#### Imágenes de producto

| Método | URL | Comportamiento visible para el front |
|--------|-----|--------------------------------------|
| `DELETE` | `/imagen/v1/{imagenId}` | Sin cambio — sigue eliminando la imagen y respondiendo 200 |
| `PUT` | `/presentacion/v1/imagenes/{id}` | Sin cambio — sigue actualizando y devolviendo `ImagenPresentacionDto` |
| `GET` | `/imagen/v1/cache/limpiar` | Sin cambio en response — ahora también notifica a los demás nodos vía Rabbit |

#### Productos

| Método | URL | Comportamiento visible para el front |
|--------|-----|--------------------------------------|
| `POST` | `/productos/save` | Sin cambio en request/response |
| `PUT` | `/productos/update` | Sin cambio en request/response |
| `DELETE` | `/productos/deleteBy/{id}` | Sin cambio en request/response |
| `PUT` | `/productos/{id}/habilitar?habilitar=` | Sin cambio en request/response |

#### Pedidos

| Método | URL | Comportamiento visible para el front |
|--------|-----|--------------------------------------|
| `POST` | `/pedidos/savePedido` | Sin cambio en request/response |
| `PUT` | `/pedidos/confirmar/{id}` | Sin cambio en request/response |
| `DELETE` | `/pedidos/delete/{id}?motivo=` | Sin cambio en request/response |
| `DELETE` | `/pedidos/{pedidoId}/detalle/{productoId}?cantidad=` | Sin cambio en request/response |

#### Ventas

| Método | URL | Comportamiento visible para el front |
|--------|-----|--------------------------------------|
| `POST` | `/ventas/save` | Sin cambio en request/response |

#### Palabras clave

| Método | URL | Comportamiento visible para el front |
|--------|-----|--------------------------------------|
| `POST` | `/palabras-clave/save` | Sin cambio en request/response |
| `PUT` | `/palabras-clave/update/{id}` | Sin cambio — el `save` del servicio base ahora evicta caché + Rabbit |
| `DELETE` | `/palabras-clave/delete` | Sin cambio — igual |

#### Admin — limpieza de caché

| Método | URL | Qué hace | Cambio |
|--------|-----|----------|--------|
| `DELETE` | `/admin/cache` | Limpia todas las caches de Spring | Ahora también notifica vía Rabbit a los demás nodos. Response sin cambio: devuelve lista de caches limpiadas. |

---

### Acción requerida por el front

**Ninguna.** Todos los endpoints mantienen el mismo método HTTP, URL, request body y response.

El único beneficio observable es que después de cualquier escritura, **todos los nodos** del back sirven datos actualizados — elimina el caso donde el front veía datos viejos al refrescar si era atendido por un nodo diferente.

---

## CAMBIOS INTERNOS — micro_imagenes ahora también evicta caché vía Rabbit

> **El front NO necesita cambiar nada.** Este cambio es interno a micro_imagenes (puerto 9096).

### Qué cambió

`micro_imagenes` ahora escucha el evento `cache.evict.all` de RabbitMQ.

Antes: cuando `proyecto-key` publicaba `cache.evict.all`, solo los nodos de `proyecto-key` limpiaban su caché. `micro_imagenes` no se enteraba y podía seguir sirviendo datos cacheados viejos (imágenes de productos que ya no existen, listas de imágenes desactualizadas).

Ahora: cuando se publica `cache.evict.all`:
1. Los nodos de `proyecto-key` limpian su caché (como antes)
2. Los nodos de `micro_imagenes` también limpian su caché (nuevo)

### Implementación

- **Cola nueva en micro_imagenes:** `queue.cache.evict.all.imagenes` — cola propia, separada de la de proyecto-key, vinculada al mismo `exchange.imagenes` con la misma routing key `cache.evict.all`. Esto garantiza que ambos servicios reciban el mismo mensaje (no compiten por él).
- **Listener:** `ImagenRabbitConsumer.evictAllCache()` — limpia todas las caches de Redis en el nodo de micro_imagenes que recibe el mensaje.

### Cuándo se dispara

Los mismos eventos que ya existían en proyecto-key (POST producto, PUT producto, DELETE producto, POST pedido, etc.) ahora también limpian la caché de micro_imagenes automáticamente.

---

## CAMBIOS INTERNOS — Guardar relaciones producto-imagen ahora es asíncrono vía Rabbit

> **El front NO necesita cambiar nada.** Request, response y URLs son exactamente los mismos.

### Qué cambió

Cuando se guarda o actualiza un producto con imágenes, el paso de registrar la relación `productoId → imagenId` en micro_imagenes ahora es **asíncrono vía RabbitMQ** en vez de una llamada HTTP síncrona.

**Flujo anterior:**
```
Front → POST /productos/save
    └─► sube bytes al micro (HTTP multipart) → obtiene imagenIds
    └─► POST producto-imagen/saveAll (HTTP síncrono) → micro_imagenes registra la relación
    ← 200 OK  (todo en la misma llamada)
```

**Flujo nuevo:**
```
Front → POST /productos/save
    └─► sube bytes al micro (HTTP multipart) → obtiene imagenIds
    └─► publica a queue.guardar.imagenes (Rabbit, fire-and-forget)
    ← 200 OK  (respuesta inmediata, sin esperar al micro)
              ...micro_imagenes recibe el mensaje y registra la relación en segundo plano
```

### Garantías
- Si micro_imagenes está caído cuando se guarda el producto, el mensaje **queda encolado** en Rabbit y se procesa cuando el micro levanta — no se pierde
- Si el procesamiento falla → NACK → va a `dlq.guardar.imagenes` (Dead Letter Queue) para inspección manual

### Dónde se ve el cambio en el front (cómo probarlo)

1. Ve al panel admin → crear nuevo producto → sube una imagen → guarda
2. El 200 OK llega **más rápido** que antes (ya no espera la confirmación del micro)
3. Espera 1-2 segundos → ve al listado de productos → la imagen ya aparece
4. **Caso de falla simulada:** si micro_imagenes está abajo al guardar, el producto se crea igual y la imagen aparece en cuanto micro_imagenes vuelve a estar activo

---

## CAMBIOS INTERNOS — Eliminar imágenes ahora es asíncrono vía Rabbit

> **El front NO necesita cambiar nada.** Mismos endpoints, mismo request, mismo response.

### Qué cambió

Las dos operaciones de eliminación de imágenes que antes hacían HTTP síncrono a micro_imagenes ahora publican a RabbitMQ:

| Operación | Queue | Qué hace micro_imagenes al recibirlo |
|---|---|---|
| Eliminar imágenes por ID | `queue.eliminar.imagenes` | Borra el archivo del disco + el registro de BD por cada ID |
| Eliminar archivos del disco | `queue.eliminar.imagenes.disco` | Borra solo los archivos del disco (sin tocar BD) |

Ambas colas tienen Dead Letter Queue (`dlq.eliminar.imagenes`, `dlq.eliminar.imagenes.disco`) — si el procesamiento falla, el mensaje va al DLQ en vez de perderse o reintentar infinitamente.

**Flujo anterior:**
```
Front → DELETE producto/variante
    └─► DELETE /imagenes?ids=... (HTTP síncrono a micro_imagenes)
    ← 200 OK  (espera a que el micro confirme la eliminación)
```

**Flujo nuevo:**
```
Front → DELETE producto/variante
    └─► publica ids a queue.eliminar.imagenes (Rabbit, fire-and-forget)
    ← 200 OK  (respuesta inmediata)
              ...micro_imagenes recibe el mensaje y elimina archivos + BD en segundo plano
```

### Dónde se ve el cambio en el front (cómo probarlo)

**Caso 1 — Eliminar imagen de un producto:**
1. Ve al panel admin → editar producto → elimina una imagen → guarda
2. El 200 OK llega más rápido que antes
3. Recarga el detalle del producto → la imagen ya no aparece

**Caso 2 — Eliminar un producto completo:**
1. Ve al panel admin → listado de productos → elimina un producto
2. El producto desaparece del listado inmediatamente
3. Las imágenes asociadas se eliminan del disco del micro en segundo plano — si entras al diagnóstico de imágenes del producto antes de que procese, puede que aún aparezcan brevemente

**Caso 3 — Eliminar imagen de una variante:**
1. Ve al panel admin → variantes → selecciona una variante → elimina imágenes → guarda
2. Las imágenes desaparecen del listado de esa variante en el siguiente request

### Dónde se ve el cambio en el front (cómo probarlo)

**Caso 1 — Imagen de producto:**
1. Ve al panel admin → editar producto → cambia o elimina la imagen principal → guarda
2. Ve al catálogo/listado de productos (sin recargar manualmente el front)
3. **Antes:** la imagen vieja seguía apareciendo hasta que expiraba el TTL de 30 min
4. **Ahora:** la imagen actualizada aparece de inmediato en el siguiente request al listado

**Caso 2 — Banner de login/registro:**
1. Ve al panel admin → Imágenes de presentación → selecciona el banner de LOGIN → cambia la imagen → guarda
2. Abre otra pestaña y ve a la pantalla de login
3. **Antes:** el banner viejo seguía apareciendo (caché de micro_imagenes no se limpiaba)
4. **Ahora:** el banner nuevo aparece de inmediato

**Caso 3 — Eliminar imagen de variante:**
1. Ve al panel admin → variantes → selecciona una variante → elimina una imagen → guarda
2. Ve al listado de variantes o al detalle de esa variante
3. **Antes:** la imagen eliminada podía seguir apareciendo en caché
4. **Ahora:** el listado ya no incluye esa imagen en el siguiente request

---

## Optimizaciones internas N+1 — 2026-06-09

### Qué se hizo
Se corrigieron problemas de N+1 en JPA/Hibernate en `proyecto_key_new`. Los contratos de API **no cambian** — mismo request, mismo response. Solo mejora el rendimiento y la estabilidad interna.

### Endpoints a probar (pruebas de regresión)

#### 1. Módulo Rifa — GanadorRifaController (`/v1/ganadorRifa`)

| Endpoint | Método | Qué probar |
|----------|--------|-----------|
| `/v1/ganadorRifa/sortear/{configurarRifaId}` | POST | Ejecutar un sorteo completo, verificar que devuelve ganador y variante |
| `/v1/ganadorRifa/continuarVariante/{configurarRifaId}?modo=RESTANTES` | POST | Continuar variante con modo RESTANTES/CERO/NUEVOS, verificar que el historial queda bien |
| `/v1/ganadorRifa/estado/{configurarRifaId}` | GET | Obtener estado de la rifa activa, verificar que trae variante actual, elegibles y descartados |
| `/v1/ganadorRifa/reiniciar/{configurarRifaId}` | POST | Reiniciar rifa con `completo=false` y `completo=true`, verificar que limpia ganadores e historial |

**Qué cambió internamente:**
- `sortear()` y `continuarVariante()`: las variantes de rifa ahora se cargan con sus variantes de producto y producto en una sola query (antes era 1+N+N)
- `continuarVariante()`: los ganadores anteriores se cargan con `concursante` y `configurarRifaVariante` en una sola query (antes era 1+N+N)
- `reiniciar()`: usa DELETE directo en BD en vez de cargar todos los registros y borrarlos uno a uno (antes era 1+N queries de SELECT + N de DELETE)

---

#### 2. Módulo Variantes de Rifa — ConfigurarRifaVarianteController (`/v1/configurarRifaVariante`)

| Endpoint | Método | Qué probar |
|----------|--------|-----------|
| `GET /v1/configurarRifaVariante/porRifa/{rifaId}` | GET | Listar variantes de una rifa, verificar que devuelve variante con nombre de producto incluido |

**Qué cambió internamente:**
- `listarPorRifa()`: carga variantes con su `Variante` y el `Producto` asociado en una sola query (antes era 1+N+N)

---

#### 3. Módulo Productos — ProductosController (`/v1/productos`)

| Endpoint | Método | Qué probar |
|----------|--------|-----------|
| `POST /v1/productos/compartir-imagenes-variantes` | POST | Compartir imágenes de un producto a todas sus variantes, verificar que todas las variantes reciben las imágenes |

**Qué cambió internamente:**
- `compartirImagenesVarianteDto()`: las imágenes del producto se cargan con JOIN FETCH incluyendo el objeto `Imagen` completo (antes era N queries extras en el loop doble)

---

### micro_imagenes — sin cambios
No se modificó ningún archivo de `micro_imagenes`. No requiere pruebas adicionales.

---

## Rifa Mensual — nuevos campos y endpoints (2026-06-11)

Diseño completo en `RIFA_MENSUAL_PROPUESTA.md`. Todos los endpoints son **ADMIN** (`/v1/configurarRifa/**`, `/v1/concursante/**`).

### 1. `ConfigurarRifa` — 3 campos nuevos (opcionales, no rompen lo existente)

Afecta a: `POST /v1/configurarRifa/save`, `PUT /v1/configurarRifa/update/{id}`, `GET /v1/configurarRifa/activas`, `GET /v1/configurarRifa/activas/hoy`, `GET /v1/configurarRifa/buscar`, `GET /v1/ganadorRifa/estado/{id}` (dentro de `configurarRifa`).

**Campos nuevos:**
- `tipo`: `"MENSUAL"` | `"DIARIA"` | `null` (rifas viejas quedan `null`)
- `mesReferencia`: `"YYYY-MM"` | `null` — solo informativo, de qué mes son los participantes
- `esPrueba`: `boolean`, default `false`

**Request** (`save`/`update`, campos nuevos opcionales):
```json
{
  "fechaHoraLimite": "2026-07-01T20:00:00",
  "activa": true,
  "tipo": "MENSUAL",
  "mesReferencia": "2026-06",
  "esPrueba": false
}
```

**Response** (`/activas`, `/activas/hoy`, `/buscar`) — 3 campos nuevos al final:
```json
{
  "id": 9,
  "fechaHoraLimite": "2026-07-01T20:00:00",
  "activa": true,
  "totalVariantes": 2,
  "variantesSorteadas": 0,
  "tipo": "MENSUAL",
  "mesReferencia": "2026-06",
  "esPrueba": false
}
```

Si `esPrueba: true`, el front debe mostrar un aviso tipo **"⚠️ Esta rifa es de prueba"**.

---

### 2. `PUT /v1/configurarRifa/{id}/esPrueba` — 🆕 toggle modo prueba

**Request:**
```json
{ "esPrueba": false }
```

**Response:** entidad `ConfigurarRifa` completa (incluye `id`, `esPrueba`, `activa`, `variantes`, etc.)

**⚠️ Efecto al pasar de `true` → `false`** (botón "Pasar a sorteo real"):
- Borra los giros de la demo (`ganador_rifa` + `historial_rifa_variante` de esa rifa)
- Todos los concursantes vuelven a estar elegibles (`descartado=false`), incluidos los agregados durante la prueba
- Reactiva la rifa (`activa=true`)

Al pasar de `false` → `true` (botón "Modo demo") solo cambia el flag, no borra nada.

**Error 400** si el `id` no existe: `{ "mensaje": "Configuración de rifa no encontrada" }`

---

### 3. `GET /v1/configurarRifa/buscar` — 🆕 nuevo endpoint

**Request:** `GET /v1/configurarRifa/buscar?desde=2026-06-25&hasta=2026-06-30` (rango de días por `fechaHoraLimite`)
o `GET /v1/configurarRifa/buscar?tipo=MENSUAL&mesReferencia=2026-06` (rifas mensuales de ese mes)
o combinaciones de `desde`, `hasta`, `tipo`, `mesReferencia`.

**Sin parámetros**: devuelve lo mismo que `/activas/hoy` (rifas activas con `fechaHoraLimite` de hoy).

**Response:** `List<ConfigurarRifaResumenDto>`, mismo formato que `/activas` (ver sección 1).

---

### 4. `Concursante` — campo nuevo `agregadoEnPrueba`

Afecta a: `GET /v1/concursante/porRifa/{id}`, `GET /v1/concursante/elegibles/{id}`, `GET /v1/ganadorRifa/estado/{id}` (dentro de `elegibles`/`descartados`).

**Campo nuevo:** `agregadoEnPrueba: boolean` — `true` si el concursante se registró mientras la rifa estaba en `esPrueba=true`.

Con esto el front puede mostrar **2 listas**:
- Participantes normales (`agregadoEnPrueba=false`)
- Agregados durante la prueba (`agregadoEnPrueba=true`)

Al pasar a sorteo real (toggle `esPrueba→false`, sección 2) estos concursantes **siguen participando** — el flag es solo informativo para el admin.

---

### 5. `POST /v1/concursante/importarDePedidos` — ⚠️ cambia el `response`

**Request:** sin cambios —
```json
{
  "configurarRifaId": 9,
  "palabraClave": "BOLSA",
  "ordenDesde": 1,
  "mes": "2026-06",
  "clientes": [
    { "clientePedidoId": 102, "nombre": "Carlos Ruiz", "telefono": "555...", "sinRegistro": false }
  ]
}
```

**Response — ANTES** era `List<Concursante>` directo. **AHORA:**
```json
{
  "importados": [
    { "id": 201, "nombre": "María López", "palabraClave": "BOLSA", "agregadoEnPrueba": false }
  ],
  "omitidosYaRegistrados": [
    { "clientePedidoId": 102, "nombre": "Carlos Ruiz", "telefono": "555...", "sinRegistro": false }
  ]
}
```

**Diferencia clave:** si un `clientePedidoId` ya estaba registrado como concursante en esa misma rifa
(ej. el admin dio clic 2 veces en "importar"), ya **no se duplica** — se omite y aparece en
`omitidosYaRegistrados` para que el front avise "estos N ya estaban registrados".

---

### 6. `DELETE /v1/concursante/{id}` — 🆕 nuevo endpoint

Reemplaza usar `DELETE /v1/concursante/delete` (genérico, requiere el id en el body) para este caso.

**Response OK (200):**
```json
{ "data": "Concursante eliminado" }
```

**Response error (400)** — si el concursante ya participó en algún giro (`ganador_rifa`):
```json
{ "mensaje": "No se puede eliminar: el concursante ya participó en un sorteo" }
```

---

### 7. `PUT /v1/concursante/{id}` — 🆕 nuevo endpoint (body parcial)

Reemplaza usar `PUT /v1/concursante/update/{id}` (genérico, exige el objeto `Concursante` completo) para este caso.

**Request** (todos los campos opcionales, solo se actualizan los que vengan):
```json
{
  "nombre": "Juan",
  "apellidoPaterno": "García",
  "telefono": "5551234567",
  "palabraClave": "BOLSA",
  "ordenDesde": 1
}
```

**Response (200):** entidad `Concursante` actualizada completa.

`boletos`, `boletosBase`, `descartado`, `agregadoEnPrueba`, `clientePedidoId` y `configurarRifa`
**no se pueden modificar** desde este endpoint.

---

### 8. Cambio interno — fórmula de "boletos" (sin cambio de contrato)

`boletosBase`/`boletos` (campos ya existentes en `Concursante`, visibles en `/porRifa`, `/elegibles`,
`/estado`) ahora se calculan por **cantidad de productos comprados** en el mes
(`SUM(detalle_pedidos.cantidad)` de pedidos `Entregado`), antes era por **número de pedidos**. No
cambia ningún endpoint ni nombre de campo — solo el valor numérico que puede llegar a tener un
concursante. No mostrar estos campos en pantallas proyectadas al público.

---

## Rifa por Día (`tipo="DIARIA"`) — reutiliza todo lo de arriba (2026-06-11)

Diseño en `RIFA_DIARIA_PROPUESTA.md`. **No hay endpoints nuevos.** La diaria usa el mismo backend que
la rifa mensual (sección anterior) — solo cambia el `tipo` y cómo se agregan los participantes.

### 1. Crear la sesión del día

`POST /v1/configurarRifa/save`
```json
{ "fechaHoraLimite": "2026-06-11T20:00:00", "activa": true, "tipo": "DIARIA", "esPrueba": false }
```
`mesReferencia` se deja `null` (no aplica para diaria).

---

### 2. Agregar participantes — uno por uno (no hay importación en bloque)

**Caso A — cliente ya registrado en la app:**
`GET /v1/clientes/buscar?nombre=Maria` (🟢 endpoint ya existente, no es de rifas) →
`ClienteBusquedaDto` con `nombrePersona`, `apeidoPaterno`, `numeroTelefonico`. El front toma esos
datos y los manda al paso siguiente.

**Caso B — persona sin registro:** el front captura los datos a mano.

En ambos casos:
`POST /v1/concursante/registrar`
```json
{ "nombre": "Maria", "apellidoPaterno": "Lopez", "telefono": "555...",
  "palabraClave": "BOLSA", "configurarRifa": { "id": 12 } }
```

**⚠️ Importante:** NO enviar `clientePedidoId` en la diaria → `boletos` queda en `1` para todos
(misma probabilidad para cada participante). Si se envía `clientePedidoId`, el back calculará
`boletos` por compras del mes (igual que en mensual) — no usar ese campo aquí salvo que se pida lo
contrario.

---

### 3. Resto del flujo — igual que mensual

- Editar / eliminar: `PUT` / `DELETE /v1/concursante/{id}` (sección 6 y 7 de arriba)
- Modo prueba: `PUT /v1/configurarRifa/{id}/esPrueba` (sección 2 de arriba) — mismo banner
  "⚠️ Esta rifa es de prueba"
- Ver participantes / separar en 2 listas: `GET /v1/concursante/porRifa/{id}` → `agregadoEnPrueba`
  (sección 4 de arriba)
- Traer la rifa de hoy: `GET /v1/configurarRifa/activas/hoy` — ya devuelve **cualquier** `tipo`
  activo hoy, incluida la diaria, sin que el front tenga que filtrar
- Buscar una rifa diaria de otro día: `GET /v1/configurarRifa/buscar?tipo=DIARIA&desde=&hasta=`
- Sorteo: `sortear` / `continuarVariante` / `estado` — mismo motor que mensual

---

## Rifa — modo prueba ya no se "cierra" tras el sorteo (2026-06-13)

### Qué cambió
- **Antes:** al sortear el ganador de la última variante, el backend ponía `activa=false` en la rifa
  **sin importar `esPrueba`**. Eso rompía el flujo de pruebas: para repetir la prueba había que
  `reiniciar` y, además, si se volvía a mandar `POST /configurarRifaVariante/save` con la misma
  `palabraClave`, daba error `"La palabraClave 'X' ya existe en esta rifa"`.
- **Ahora:**
    - Si `esPrueba: true`, la rifa **se mantiene `activa: true`** aunque ya se haya sorteado el
      ganador de la última variante. `rifaTerminada` (en `/sortear` y `/estado`) sigue marcando
      correctamente cuándo terminó el ciclo — no depende de `activa`.
    - `POST /v1/configurarRifaVariante/save`: si `esPrueba: true` y la `palabraClave` ya existe en
      esa rifa, **ya no rechaza** — actualiza la configuración existente (`giroGanador`, `orden`,
      `permitirNuevos`, y la variante/stock si se cambió de variante). Mismo `request`/`response`
      de siempre.
    - Si `esPrueba: false` (rifa real), el comportamiento **no cambia**: al terminar se pone
      `activa: false`, y reusar una `palabraClave` ya configurada en esa rifa sigue dando
      `"ya existe en esta rifa"`.

### Qué debe hacer el front
- **Nada obligatorio, es retrocompatible.** Mientras `esPrueba: true`, el admin puede:
    - Repetir `sortear` tras `POST /v1/ganadorRifa/reiniciar/{id}?completo=true|false` cuantas veces
      quiera, sin que la rifa se "cierre" (`activas`/`activas/hoy` la sigue listando).
    - Re-mandar `POST /configurarRifaVariante/save` con la misma `palabraClave` para "recargar" la
      config de la variante de prueba — ya no da error.
- Cuando el admin haga `PUT /v1/configurarRifa/{id}/esPrueba` con `{ "esPrueba": false }`
  ("Pasar a sorteo real"), la `ConfigurarRifaVariante` y su `palabraClave` configuradas durante las
  pruebas **se conservan** y se usan tal cual para el sorteo real (no hay que volver a crearlas).
  A partir de ahí aplica el comportamiento de rifa real descrito arriba.

---

## Catálogo de errores — endpoints de Rifas (2026-06-13)

### Formato de error
Todos los endpoints de Rifas que validan reglas de negocio (todos excepto los `GET` simples)
responden, cuando algo falla:

```
HTTP 400 Bad Request
{
  "mensaje": "<texto del error, mostrar tal cual al usuario>",
  "code": 404,
  "data": null,
  "lista": null
}
```

⚠️ **`code: 404` es un valor fijo** del helper `ResponseGeneric` (no significa "no encontrado" en
sentido HTTP). Para detectar error el front debe usar el **status HTTP 400** y/o `data === null`,
y mostrar el texto de `mensaje`.

### `POST /v1/configurarRifaVariante/save`
| `mensaje` | Causa |
|---|---|
| `Rifa no encontrada` | `configurarRifaId` no existe |
| `La rifa no está activa` | `activa=false` (rifa real ya cerrada) |
| `La palabraClave 'X' ya existe en esta rifa` | solo si `esPrueba=false` y otra variante de la rifa ya usa esa `palabraClave` |
| `Variante no encontrada` | `varianteId` no existe |
| `La variante no tiene stock disponible` | `stock < 1` en la variante |

### `DELETE /v1/configurarRifaVariante/{id}`
| `mensaje` | Causa |
|---|---|
| `Configuración de variante no encontrada` | `id` no existe |

### `PUT /v1/configurarRifaVariante/{id}/palabraClave`
| `mensaje` | Causa |
|---|---|
| `Configuración de variante no encontrada` | `id` no existe |
| `La palabraClave ya existe en esta rifa` | otra variante de la misma rifa ya usa esa `palabraClave` |

### `PUT /v1/configurarRifa/{id}/esPrueba`
| `mensaje` | Causa |
|---|---|
| `Configuración de rifa no encontrada` | `id` no existe |

### `POST /v1/concursante/registrar?forzar=`
| `mensaje` | Causa |
|---|---|
| `El nombre es requerido` | falta `nombre` (validación de campo) |
| `Debe indicar la configuración de rifa` | falta `configurarRifa.id` en el body |
| `Configuración de rifa no encontrada` | `configurarRifa.id` no existe |
| `Esta rifa ya fue sorteada o está inactiva` | `activa=false` |
| `El plazo de registro cerró el {fechaHoraLimite}` | ya pasó `fechaHoraLimite` y `forzar=false` (default) — reintentar con `?forzar=true` si el admin quiere forzar el registro |
| `Este cliente ya está registrado en esta rifa` | **NUEVO (2026-07-13)** — `clientePedidoId` ya tiene un concursante en esta misma rifa. Antes este endpoint no lo validaba (solo lo validaba `/importarDePedidos`), así que un mismo cliente se podía registrar varias veces si se usaba el registro individual. `forzar=true` **no** evita este error — es una regla de integridad, no de plazo. |

### `POST /v1/concursante/importarDePedidos`
| `mensaje` | Causa |
|---|---|
| `Configuración de rifa no encontrada` | `configurarRifaId` no existe |
| `Esta rifa no está activa` | `activa=false` |

### `DELETE /v1/concursante/{id}`
| `mensaje` | Causa |
|---|---|
| `Concursante no encontrado` | `id` no existe |
| `No se puede eliminar: el concursante ya participó en un sorteo` | tiene un registro en `ganador_rifa` |

### `PUT /v1/concursante/{id}`
| `mensaje` | Causa |
|---|---|
| `Concursante no encontrado` | `id` no existe |

### `POST /v1/ganadorRifa/sortear/{configurarRifaId}`
| `mensaje` | Causa |
|---|---|
| `Configuración de rifa no encontrada` | `configurarRifaId` no existe |
| `Esta rifa ya fue completada o está inactiva` | `activa=false` |
| `La rifa no tiene variantes configuradas` | la rifa no tiene ninguna `configurarRifaVariante` |
| `Todas las variantes ya fueron sorteadas` | ya hay un ganador declarado por cada variante |
| `No hay concursantes elegibles para la variante con palabraClave='X'` | nadie con esa `palabraClave` y `descartado=false` |

### `POST /v1/ganadorRifa/continuarVariante/{configurarRifaId}?modo=`
| `mensaje` | Causa |
|---|---|
| `Rifa no encontrada` | `configurarRifaId` no existe |
| `No hay siguiente variante` | ya se sortearon todas las variantes |
| `Modo inválido: X. Usar RESTANTES, CERO o NUEVOS` | `modo` no es uno de los 3 valores válidos |

### `GET /v1/ganadorRifa/estado/{configurarRifaId}`
| `mensaje` | Causa |
|---|---|
| `Rifa no encontrada` | `configurarRifaId` no existe |

### `POST /v1/ganadorRifa/reiniciar/{configurarRifaId}?completo=`
| `mensaje` | Causa |
|---|---|
| `Rifa no encontrada` | `configurarRifaId` no existe |

**Response OK (200)** de `reiniciar`:
```json
{ "data": "Rifa reiniciada completamente (concursantes eliminados)" }
```
o, con `completo=false`:
```json
{ "data": "Rifa reiniciada (concursantes conservados)" }
```

---

## Autenticación — token expirado/ausente ahora responde 401 (antes 403) (2026-06-13)

**Causa del bug:** `SecurityConfig` no tenía configurado un `AuthenticationEntryPoint`, así que
Spring Security usaba el fallback por defecto (`Http403ForbiddenEntryPoint`). Esto hacía que
**cualquier request sin autenticación válida** (token ausente, corrupto o **expirado**) devolviera
**403 Forbidden** en vez de **401 Unauthorized**. Si el interceptor del front solo dispara el
refresh ante un **401**, nunca se enteraba de que el access token expiró — el request fallaba con
un 403 "seco" y ahí quedaba.

**Cambio:**
- **401 Unauthorized** → no autenticado: token ausente, inválido o **expirado**.
  Body: `{ "mensaje": "Token inválido o expirado", "code": 404, "data": null, "lista": null }`
  → el front debe intentar `/v1/auth/refresh` y reintentar el request original.
- **403 Forbidden** → autenticado correctamente pero sin el rol requerido (ej. usuario sin
  `ROLE_ADMIN` llamando a un endpoint de admin).
  Body: `{ "mensaje": "No tiene permisos para acceder a este recurso", "code": 404, "data": null, "lista": null }`
  → el front **no** debe reintentar con refresh aquí (el token es válido, solo falta permiso).

**Acción para el front:** revisar el interceptor — el flujo de `/v1/auth/refresh` debe dispararse
ante **401**, no ante 403. Si antes "funcionaba" reintentando en 403, eso era un parche al bug
descrito arriba; ahora la expiración de token llega correctamente como 401.

---

## `POST /v1/concursante/importarDePedidos` — nuevo campo `omitidosSinNombre` (2026-06-13)

**Causa del bug:** si `clientes[]` traía una entrada `sinRegistro: true` con `nombre: ""`
(vacío), el backend intentaba guardar el `Concursante` y la validación `@NotBlank` de Hibernate
lanzaba un `ConstraintViolationException` cuyo mensaje crudo (técnico) se devolvía tal cual en
`mensaje`, y **abortaba todo el batch** — ningún concursante se importaba, ni siquiera los
válidos.

**Cambio:** las entradas sin `nombre` (vacío o solo espacios) ya **no rompen el batch**: se omiten
y se devuelven en un nuevo arreglo `omitidosSinNombre`, igual que ya pasaba con
`omitidosYaRegistrados`.

**Response — ahora:**
```json
{
  "importados": [
    { "id": 201, "nombre": "María López", "palabraClave": "BOLSA", "agregadoEnPrueba": false }
  ],
  "omitidosYaRegistrados": [
    { "clientePedidoId": 102, "nombre": "Carlos Ruiz", "telefono": "555...", "sinRegistro": false }
  ],
  "omitidosSinNombre": [
    { "clientePedidoId": null, "nombre": "", "telefono": "", "sinRegistro": true }
  ]
}
```

**Acción para el front:** si `omitidosSinNombre` no viene vacío, avisar al admin algo como
"N participante(s) sin registro no se importaron porque no tienen nombre". Si la UI permite
agregar filas de "cliente sin registro" a mano, idealmente exigir `nombre` antes de enviar para
que no terminen en este arreglo.

---

## Pitfall técnico resuelto — @Query + Page<> con subquery JPQL (2026-06-18)

**Síntoma:** endpoint de historial devuelve `{ mensajes: [], totalMensajes: 0 }` aunque en BD hay filas con datos correctos.

**Causa:** cuando `@Query` usa una subconsulta JPQL (`IN (SELECT ...)`) y el tipo de retorno es `Page<T>`, Spring Data JPA no puede derivar el COUNT automáticamente. Sin `countQuery` explícito asume `totalElements = 0` y nunca ejecuta la query real.

**Regla:** siempre que haya un `@Query` que devuelva `Page<T>` y contenga subqueries, agregar `countQuery` sin el `ORDER BY`:
```java
@Query(
        value = "SELECT m FROM ... WHERE m.sesionId IN (SELECT s.sesionId FROM ...) ORDER BY m.timestamp DESC",
        countQuery = "SELECT COUNT(m) FROM ... WHERE m.sesionId IN (SELECT s.sesionId FROM ...)"
)
Page<ChatMensaje> findBy...(Pageable pageable);
```

---

## CHAT EN VIVO — Panel Admin (acción requerida en el front) — 2026-06-17

### Problema actual
Cuando el admin selecciona una sesión en el panel, **solo ve los mensajes nuevos** que llegan en tiempo real (WebSocket). Los mensajes anteriores de esa sesión no aparecen porque el front no los está cargando.

### Endpoints de historial — paginado tipo Messenger (scroll hacia arriba carga más)

Ambos endpoints (admin y cliente) aceptan `pagina` y `size`. La carga inicial trae los últimos 20 mensajes. Cuando el usuario hace scroll arriba se pide la siguiente página.

#### Admin
```
GET /mis-productos/v1/chat/admin/historial/{sesionId}?pagina=0&size=20
Authorization: Bearer <token admin>
```

#### Cliente (público)
```
GET /mis-productos/v1/chat/historial/{sesionId}?pagina=0&size=20
```

| Param | Default | Descripción |
|---|---|---|
| `pagina` | `0` | Página a cargar. `0` = mensajes más recientes |
| `size` | `20` | Mensajes por página |

**Response** — leer `response.data`:
```json
{
  "code": 200,
  "data": {
    "mensajes": [
      { "remitente": "USUARIO", "contenido": "Hola, tengo una pregunta", "timestamp": "2026-06-17T10:00:00" },
      { "remitente": "ADMIN",   "contenido": "Claro, ¿en qué te ayudo?", "timestamp": "2026-06-17T10:00:05" }
    ],
    "pagina": 0,
    "totalPaginas": 3,
    "totalMensajes": 45,
    "hayMasAntiguos": true
  }
}
```

- `mensajes` viene ordenado **cronológico ascendente** (el más antiguo primero) — listo para renderizar de arriba a abajo
- `hayMasAntiguos: true` → mostrar botón/spinner de "cargar más" al inicio del scroll
- `id` y `sesionId` no aparecen en cada mensaje (`@JsonIgnore`)
- `remitente` es exactamente `"USUARIO"` o `"ADMIN"`
- El endpoint de cliente devuelve **403** si el `sesionId` no existe en BD

**Flujo scroll tipo Messenger:**
```typescript
// Carga inicial (mensajes más recientes)
cargarHistorial(sesionId, pagina = 0) {
  GET .../historial/{sesionId}?pagina=0&size=20
  this.mensajes = res.data.mensajes;        // renderizar
  this.hayMasAntiguos = res.data.hayMasAntiguos;
}

// Usuario hace scroll arriba → cargar página siguiente
cargarMasAntiguos() {
  if (!this.hayMasAntiguos) return;
  GET .../historial/{sesionId}?pagina={paginaActual + 1}&size=20
  this.mensajes = [...res.data.mensajes, ...this.mensajes]; // prepend
  this.hayMasAntiguos = res.data.hayMasAntiguos;
}
```

**Acción para el front — PENDIENTE:** Llamar este endpoint en DOS lugares:
1. **Panel admin:** cuando el admin hace clic en una sesión, cargar `pagina=0` y renderizar antes de recibir eventos WebSocket.
2. **Chat del cliente:** al inicializar, usar el endpoint por `clienteId` (ver sección siguiente) para ver TODA la historia entre sesiones.

---

### Historial completo del cliente a través de sesiones — `clienteId` persistente

El `sesionId` cambia cada vez que la sesión expira (5 min de inactividad). Para que el cliente vea mensajes de sesiones anteriores, el front genera un `clienteId` fijo guardado en `localStorage`.

**Generar y guardar el `clienteId` una sola vez:**
```typescript
if (!localStorage.getItem('chat_cliente_id')) {
  localStorage.setItem('chat_cliente_id', crypto.randomUUID());
}
const clienteId = localStorage.getItem('chat_cliente_id');
```

**Enviarlo al conectar** — payload de `/app/chat.conectar`:
```json
{ "tempId": "uuid-temporal", "nombreUsuario": "Juan", "clienteId": "uuid-persistente" }
```

**Endpoint de historial completo** — todas las sesiones del cliente:
```
GET /mis-productos/v1/chat/historial/cliente/{clienteId}?pagina=0&size=20
```
Público, sin token. Devuelve mensajes de **todas las sesiones** vinculadas a ese `clienteId` ordenados cronológicamente. Mismo formato de response que el historial por `sesionId` (`{ mensajes, pagina, totalPaginas, totalMensajes, hayMasAntiguos }`).

**Flujo correcto al inicializar el chat del cliente:**
```typescript
ngOnInit() {
  // 1. clienteId persiste en localStorage entre sesiones y recargas
  if (!localStorage.getItem('chat_cliente_id'))
    localStorage.setItem('chat_cliente_id', crypto.randomUUID());
  this.clienteId = localStorage.getItem('chat_cliente_id');

  // 2. Cargar toda la historia del cliente (todas las sesiones pasadas)
  this.http.get(`/v1/chat/historial/cliente/${this.clienteId}?pagina=0&size=20`)
    .subscribe(res => {
      this.mensajes = res.data.mensajes ?? [];
      this.hayMasAntiguos = res.data.hayMasAntiguos;
    });

  // 3. Conectar WebSocket mandando clienteId para vincular la nueva sesión
  this.conectarWebSocket();
}
```

> **Resumen de storage:**
> - `clienteId` → **`localStorage`** — persiste aunque se cierre el navegador, une todas las sesiones (usuarios anónimos)
> - `sesionId` → **`sessionStorage`** — solo dura la pestaña, identifica la sesión WebSocket activa

---

### Historial por usuario registrado — `usuarioId` (vinculado a la cuenta)

Para usuarios que tienen cuenta en el sistema, se puede vincular la sesión de chat a su `usuarioId` real (Integer) en lugar de un UUID anónimo. Esto permite recuperar todos sus mensajes históricos de forma confiable.

**Enviar `usuarioId` al conectar** — payload de `\app\chat.conectar`:
```json
{
  "tempId": "uuid-temporal",
  "nombreUsuario": "Juan",
  "clienteId": "uuid-persistente-localStorage",
  "usuarioId": 42
}
```
- `usuarioId` es opcional (null si el usuario no está autenticado → solo se usa `clienteId`)
- `usuarioId` es el `id` (Integer) del usuario en `usuario_modificacion`

**Endpoint de historial por usuarioId** — todas las sesiones del usuario registrado:
```
GET /mis-productos/v1/chat/historial/usuario/{usuarioId}?pagina=0&size=20
```
Público, sin token. Devuelve mensajes de **todas las sesiones** vinculadas a ese `usuarioId`, mismo formato que historial por sesión.

**Response:** igual al historial paginado:
```json
{
  "mensaje": "La peticion fue exitosa",
  "code": 200,
  "data": {
    "mensajes": [ { "remitente": "USUARIO", "contenido": "Hola", "timestamp": "2026-06-17T10:00:00" } ],
    "pagina": 0,
    "totalPaginas": 3,
    "totalMensajes": 45,
    "hayMasAntiguos": true
  }
}
```

**Flujo recomendado para usuario autenticado:**
```typescript
ngOnInit() {
  // usuarioId viene del token decodificado o del perfil del usuario autenticado
  this.usuarioId = this.authService.getCurrentUser()?.id ?? null;

  // Cargar historial del usuario (todas sus sesiones pasadas)
  if (this.usuarioId) {
    this.http.get(`/v1/chat/historial/usuario/${this.usuarioId}?pagina=0&size=20`)
      .subscribe(res => {
        this.mensajes = (res as any).data?.mensajes ?? [];
        this.hayMasAntiguos = (res as any).data?.hayMasAntiguos ?? false;
      });
  } else {
    // usuario anónimo: usar clienteId de localStorage
    const clienteId = localStorage.getItem('chat_cliente_id');
    if (clienteId) {
      this.http.get(`/v1/chat/historial/cliente/${clienteId}?pagina=0&size=20`)
        .subscribe(res => {
          this.mensajes = (res as any).data?.mensajes ?? [];
          this.hayMasAntiguos = (res as any).data?.hayMasAntiguos ?? false;
        });
    }
  }

  // Conectar WebSocket
  this.conectarWebSocket();
}
```

**UX de expiración de sesión (SESION_CERRADA):**
1. Recibir `{ tipo: "SESION_CERRADA" }` en `/topic/chat.usuario.{sesionId}`
2. Limpiar `mensajes` del componente (y `sesionId` de sessionStorage)
3. Cuando el usuario envía el siguiente mensaje:
    - Llamar de nuevo a `\app\chat.conectar` con el `usuarioId` (o `clienteId`) → recibir nuevo `sesionId`
    - Llamar al endpoint de historial (`pagina=0, size=20`) para cargar los últimos mensajes
    - Renderizar esos mensajes — el scroll hacia arriba carga páginas anteriores (`pagina=1`, `pagina=2`...)

---

---

### Endpoint de sesiones activas (para el listado del panel)

**Request:**
```
GET /mis-productos/v1/chat/admin/sesiones
Authorization: Bearer <token admin>
```

**Response:** envuelto en `ResponseGeneric` — leer `response.data`:
```json
{
  "mensaje": "La peticion fue exitosa",
  "code": 200,
  "data": [
    {
      "sesionId": "f1d0db6f-496f-4000-9dd1-234efdc51f06",
      "nombreUsuario": "chat",
      "estado": "ACTIVA",
      "fechaInicio": "2026-06-17T10:00:00",
      "ultimaActividad": "2026-06-17T10:02:00",
      "ultimoMensaje": "Hola, tengo una pregunta"
    },
    {
      "sesionId": "37cb781e-f39f-4fa4-b825-52a7f0b9ab0c",
      "nombreUsuario": "Visitante",
      "estado": "CERRADA",
      "fechaInicio": "2026-06-17T09:00:00",
      "ultimaActividad": "2026-06-17T09:05:00",
      "ultimoMensaje": "Buen dia"
    }
  ],
  "lista": null
}
```

- Devuelve **todas las sesiones de las últimas 24 horas** (ACTIVA y CERRADA), ordenadas por `ultimaActividad` descendente
- Campo nuevo `estado`: `"ACTIVA"` o `"CERRADA"` — mostrar indicador visual (ej. punto verde / gris)
- `ultimoMensaje` puede ser `null` si el usuario conectó pero no envió ningún mensaje
- El admin puede hacer clic en cualquier sesión para ver el historial — incluso las cerradas
- `estado === 'CERRADA'` → solo lectura (no tiene sentido responder, la sesión ya expiró)

```typescript
// ✅ Correcto
this.sesiones = (response as any).data ?? [];
// Indicador visual sugerido:
// sesion.estado === 'ACTIVA'  → punto verde, puede responder
// sesion.estado === 'CERRADA' → punto gris, solo ver historial
```

---

### Endpoint para cerrar sesión manualmente

**Request:**
```
POST /mis-productos/v1/chat/admin/cerrar/{sesionId}
Authorization: Bearer <token admin>
```

**Response:** 204 No Content

---

### Comportamiento del email de notificación

El backend manda email al admin en el **primer mensaje de cada sesión**, sin importar si el admin está en el panel o no.

- Visitante se conecta → sin email
- Visitante manda primer mensaje → email con el contenido del mensaje
- Visitante manda más mensajes en la misma sesión → sin email (ya fue notificado)
- Sesión expira por inactividad (5 min) → visitante manda nuevo mensaje → nueva sesión → nuevo email

El front **no necesita hacer nada especial** para controlar los emails. Solo asegurarse de que el visitante llame `/chat.conectar` para crear sesión antes de mandar mensajes.

---

### Notificación en el panel cuando el admin ESTÁ en la app — ✅ IMPLEMENTADO (2026-06-17)

Cuando el admin está en el panel y llega un mensaje de un visitante, el backend publica el evento por WebSocket en `/topic/chat.admin`.

**Lo que se implementó:**
- Sonido beep via Web Audio API al llegar mensaje en sesión no activa (sin archivo externo)
- Highlight rojo en la sesión del listado con clase `ca-session-item--unread`, se quita al hacer clic
- Eliminada propiedad `env.buscarImagenProducto` (URL deprecada, no se usaba en templates)

El campo del mensaje en el evento es `contenido` (no `mensaje`):
```json
{
  "tipo": "MENSAJE",
  "sesionId": "f1d0db6f-...",
  "nombreUsuario": "Juan",
  "contenido": "Hola, tengo una pregunta",
  "timestamp": "2026-06-17T10:00:05"
}
```

**Para eventos `NUEVA_SESION`**, `contenido` viene `null` — es correcto, no hay mensaje aún.

---

### Protección anti-bot

El backend limita los emails a uno por sesión. Si un bot manda mensajes continuamente dentro de la misma sesión, solo llega 1 email. Si crea sesiones nuevas continuamente, puede generar emails repetidos — se puede agregar rate limiting por IP en una iteración futura si se detecta el problema.

**Timeout de sesión:** 5 minutos sin actividad de ninguno de los dos lados (ni usuario ni admin) → la sesión se cierra y el cliente recibe `{ "tipo": "SESION_CERRADA" }` en `/topic/chat.usuario.{sesionId}`. Cualquier mensaje de cualquiera de los dos reinicia el contador.

---

### Confirmado correcto por el front — sin cambios requeridos (2026-06-17)

Lo siguiente ya estaba bien implementado y no requiere ninguna acción:

| Ítem | Estado |
|---|---|
| `buscarClientePedido` con query params | ✅ correcto |
| micro_imagenes con prefijo `/v1/` | ✅ correcto |
| Imágenes de productos y variantes usando `urlImagen` directa del response | ✅ correcto |
| Interceptor maneja 401 (token expirado) y 403 (sin permiso) correctamente | ✅ correcto |
| `omitidosSinNombre?.` con optional chaining | ✅ correcto |

---

## CHAT EN VIVO — Referencia completa para el front (2026-06-18)

> **Por qué no aparecen los mensajes al recargar la página**
>
> Se confirmó en BD que los mensajes SÍ se guardan correctamente. El problema es que el componente del chat **no está llamando al endpoint de historial al inicializar (`ngOnInit`)**.
> Al recargar la página la conexión WebSocket se reinicia (nueva sesión) y si el front no consulta el historial antes de conectar, la pantalla arranca vacía aunque existan mensajes previos en la BD.
> La sesión más reciente en BD tiene `cliente_id = e8ea8611-ca0a-48e1-8619-d754923e2885` con mensajes de USUARIO y ADMIN guardados — el backend funciona. Solo falta que el front haga el `GET /historial/cliente/{clienteId}` al iniciar.

---

### Mapa completo de endpoints de chat

#### 1. WebSocket — conectar nueva sesión

**Cuándo usarlo:** al montar el componente de chat (ngOnInit), antes de enviar mensajes. Genera el `sesionId` que identifica esta sesión.

**Suscribirse primero en:**
```
/topic/chat.inicio.{tempId}
```

**Publicar en:**
```
/app/chat.conectar
```

**Payload:**
```json
{
  "tempId": "uuid-generado-en-el-front",
  "nombreUsuario": "Juan",
  "clienteId": "uuid-de-localStorage",
  "usuarioId": 42
}
```
- `tempId`: UUID que el front genera en el momento para recibir la respuesta (no se guarda)
- `clienteId`: UUID guardado en `localStorage` — se crea la primera vez y persiste siempre (usuarios anónimos o como fallback)
- `usuarioId`: el `id` del usuario autenticado (Integer) — enviar `null` si no está logueado

**Response que llega en `/topic/chat.inicio.{tempId}`:**
```json
{ "sesionId": "94bb63c0-a3fe-4d7c-b4a9-ecd2a72c871c" }
```
→ Guardar `sesionId` en `sessionStorage`. Se usa para enviar mensajes y recibir respuestas del admin.

---

#### 2. WebSocket — enviar mensaje del cliente

**Cuándo usarlo:** cuando el usuario escribe y presiona enviar.

**Publicar en:**
```
/app/chat.mensaje
```

**Payload:**
```json
{ "sesionId": "uuid-de-sessionStorage", "contenido": "Hola, tengo una pregunta" }
```

**No hay response directo.** El admin recibe el mensaje en `/topic/chat.admin`. Si la sesión está expirada el mensaje se descarta — el front debe reconectar primero.

---

#### 3. WebSocket — recibir eventos del backend (canal del cliente)

**Suscribirse en:**
```
/topic/chat.usuario.{sesionId}
```

**Evento: mensaje del admin**
```json
{ "tipo": "MENSAJE", "remitente": "ADMIN", "contenido": "Hola, ¿en qué te ayudo?", "timestamp": "2026-06-18T02:35:47" }
```

**Evento: sesión expirada** (5 min sin actividad)
```json
{ "tipo": "SESION_CERRADA" }
```
→ Al recibir `SESION_CERRADA`: limpiar `sesionId` de sessionStorage y limpiar la lista de mensajes del componente. La próxima vez que el usuario envíe un mensaje, reconectar (`/app/chat.conectar`) y luego cargar historial.

---

#### 4. REST — historial del cliente por `clienteId` (usuarios anónimos o fallback)

**Cuándo usarlo:** en `ngOnInit`, ANTES de conectar el WebSocket. Carga todos los mensajes de todas las sesiones anteriores.

**Request:**
```
GET /mis-productos/v1/chat/historial/cliente/{clienteId}?pagina=0&size=20
```
Sin token. `clienteId` viene de `localStorage.getItem('chat_cliente_id')`.

**Response:** `ResponseGeneric` — leer `response.data`:
```json
{
  "mensaje": "La peticion fue exitosa",
  "code": 200,
  "data": {
    "mensajes": [
      { "remitente": "USUARIO", "contenido": "Hola", "timestamp": "2026-06-18T02:35:16" },
      { "remitente": "ADMIN",   "contenido": "Como estas", "timestamp": "2026-06-18T02:35:47" }
    ],
    "pagina": 0,
    "totalPaginas": 1,
    "totalMensajes": 2,
    "hayMasAntiguos": false
  }
}
```
→ Leer: `(response as any).data.mensajes` — **NO** `response as any[]`.

**Scroll hacia arriba — cargar más antiguos:**
```
GET /mis-productos/v1/chat/historial/cliente/{clienteId}?pagina=1&size=20
```
Cuando `hayMasAntiguos === true`, al hacer scroll al tope cargar `pagina + 1` y **prepend** al array actual:
```typescript
this.mensajes = [...nuevosMensajes, ...this.mensajes];
```

---

#### 5. REST — historial del cliente por `usuarioId` (usuarios registrados)

**Cuándo usarlo:** igual que el anterior, pero cuando el usuario está autenticado. Tiene la ventaja de ser robusto aunque el `localStorage` se borre.

**Request:**
```
GET /mis-productos/v1/chat/historial/usuario/{usuarioId}?pagina=0&size=20
```
Sin token. `usuarioId` es el `id` Integer del usuario autenticado.

**Response:** mismo formato que el endpoint por `clienteId` (ver arriba).

---

#### 6. REST — historial de una sesión específica (para el panel admin)

**Cuándo usarlo:** en el panel admin, cuando el admin hace clic en una sesión del listado para ver su historial.

**Request:**
```
GET /mis-productos/v1/chat/admin/historial/{sesionId}?pagina=0&size=20
Authorization: Bearer <token admin>
```

**Response:** mismo formato paginado que los anteriores.

---

#### 7. REST — listado de sesiones para el panel admin

**Cuándo usarlo:** al cargar el panel de admin, para ver todas las sesiones de las últimas 24 h (activas y cerradas).

**Request:**
```
GET /mis-productos/v1/chat/admin/sesiones
Authorization: Bearer <token admin>
```

**Response:** `ResponseGeneric` — leer `response.data`:
```json
{
  "data": [
    {
      "sesionId": "94bb63c0-...",
      "nombreUsuario": "chat",
      "estado": "ACTIVA",
      "fechaInicio": "2026-06-18T02:35:07",
      "ultimaActividad": "2026-06-18T02:35:47",
      "ultimoMensaje": "Como estas"
    }
  ]
}
```
→ Leer: `(response as any).data` — **NO** `response as any[]`.
- `estado` puede ser `"ACTIVA"` o `"CERRADA"`

---

#### 8. REST — cerrar sesión manualmente (panel admin)

**Cuándo usarlo:** botón "Cerrar sesión" en el panel admin.

**Request:**
```
POST /mis-productos/v1/chat/admin/cerrar/{sesionId}
Authorization: Bearer <token admin>
```
**Response:** 204 No Content.

---

#### 9. WebSocket — panel admin (recibir eventos y responder)

**Suscribirse en** (para recibir mensajes de todos los clientes):
```
/topic/chat.admin
```

**Eventos posibles:**

Nueva sesión conectada:
```json
{ "tipo": "NUEVA_SESION", "sesionId": "...", "nombreUsuario": "Juan", "contenido": null, "timestamp": null }
```

Mensaje del cliente:
```json
{ "tipo": "MENSAJE", "sesionId": "...", "nombreUsuario": "Juan", "contenido": "Hola", "timestamp": "2026-06-18T02:35:16" }
```

**Publicar para responder al cliente:**
```
/app/chat.admin.responder
```
```json
{ "sesionId": "uuid-del-cliente", "contenido": "Hola, ¿en qué te ayudo?" }
```

**Publicar para marcar que el admin está en el panel** (suspende emails):
```
/app/chat.admin.conectado
```
Sin payload.

---

### Flujo completo del componente de chat del cliente — código de referencia

> **Decisión 2026-06-18:** el chat es solo para usuarios logueados. Se eliminó el `clienteId` (localStorage UUID). El único identificador es `usuarioId` (Integer del usuario autenticado).

```typescript
// usuarioId viene del usuario autenticado (Integer)
// Solo mostrar el chat si el usuario está logueado
const usuarioId = this.authService.getCurrentUser()?.id;

ngOnInit() {
  if (!usuarioId) return; // no mostrar chat a usuarios no autenticados

  // PASO 1: cargar historial ANTES de conectar el WebSocket
  this.http.get(`/v1/chat/historial/usuario/${usuarioId}?pagina=0&size=20`)
    .subscribe(res => {
      this.mensajes       = (res as any).data?.mensajes      ?? [];
      this.hayMasAntiguos = (res as any).data?.hayMasAntiguos ?? false;
      this.paginaActual   = 0;
    });

  // PASO 2: conectar WebSocket
  this.conectarWebSocket();
}

conectarWebSocket() {
  const tempId = crypto.randomUUID();

  // suscribirse ANTES de publicar
  this.stompClient.subscribe(`/topic/chat.inicio.${tempId}`, frame => {
    const data = JSON.parse(frame.body);
    this.sesionId = data.sesionId;
    sessionStorage.setItem('chat_sesion_id', this.sesionId);

    // suscribirse al canal de respuestas del admin
    this.stompClient.subscribe(`/topic/chat.usuario.${this.sesionId}`, frame2 => {
      const evento = JSON.parse(frame2.body);
      if (evento.tipo === 'MENSAJE') {
        this.mensajes = [...this.mensajes, evento];
      } else if (evento.tipo === 'SESION_CERRADA') {
        sessionStorage.removeItem('chat_sesion_id');
        this.sesionId = null;
        // NO limpiar mensajes — dejarlos visibles
        // Al siguiente envío reconectar y recargar historial
      }
    });
  });

  this.stompClient.publish({
    destination: '/app/chat.conectar',
    body: JSON.stringify({ tempId, nombreUsuario: this.nombre, usuarioId })
  });
}

cargarMasAntiguos() {
  if (!this.hayMasAntiguos) return;
  this.paginaActual++;
  this.http.get(`/v1/chat/historial/usuario/${usuarioId}?pagina=${this.paginaActual}&size=20`)
    .subscribe(res => {
      const antiguos = (res as any).data?.mensajes ?? [];
      this.mensajes       = [...antiguos, ...this.mensajes]; // prepend al inicio
      this.hayMasAntiguos = (res as any).data?.hayMasAntiguos ?? false;
    });
}

enviarMensaje(contenido: string) {
  if (!this.sesionId) {
    // sesión expirada → reconectar y recargar historial
    this.conectarWebSocket();
    this.http.get(`/v1/chat/historial/usuario/${usuarioId}?pagina=0&size=20`)
      .subscribe(res => {
        this.mensajes       = (res as any).data?.mensajes      ?? [];
        this.hayMasAntiguos = (res as any).data?.hayMasAntiguos ?? false;
        this.paginaActual   = 0;
      });
    return;
  }
  this.stompClient.publish({
    destination: '/app/chat.mensaje',
    body: JSON.stringify({ sesionId: this.sesionId, contenido })
  });
  // agregar optimistamente al array local
  this.mensajes = [...this.mensajes, { remitente: 'USUARIO', contenido, timestamp: new Date().toISOString() }];
}
```

---

## MÓDULO: Pagos parciales — Apartado y Fiado (2026-06-27)

> Backend: proyecto-key (9091) — todos los endpoints requieren `ROLE_ADMIN` (token JWT en cookie)

### Concepto

| Tipo | Flujo |
|---|---|
| `APARTADO` | Cliente aparta producto → va dando abonos → al liquidar se le entrega |
| `FIADO` | Se entrega producto de entrada → cliente va pagando → al liquidar cierra |

El tipo se define al crear el pedido. Una vez creado no cambia.

---

### 1. Crear pedido con tipo de crédito

Campo nuevo en el body de `POST /v1/pedidos/savePedido`:

```
tipoPedido: "APARTADO" | "FIADO" | "NORMAL"  (default: "NORMAL")
```

El back calcula `totalPedido` automáticamente sumando los `subTotal` del detalle.
`totalPagado` inicia en `0`.

**Request igual al existente + campo nuevo:**
```json
{
  "cliente": { "id": 10 },
  "fechaPedido": "2026-06-27",
  "tipoPedido": "APARTADO",
  "estadoPedido": "APARTADO",
  "observaciones": "Pantalón azul talla M",
  "detalles": [
    { "productoId": 5, "varianteId": 12, "cantidad": 1, "precioUnitario": 350.00, "subTotal": 350.00 }
  ]
}
```

**Response:** igual al response actual de pedido (incluye los nuevos campos `tipoPedido`, `totalPedido`, `totalPagado`).

---

### 1b. Venta directa con crédito — `POST /v1/ventas/save` (MODIFICADO)

El endpoint ya existía para venta inmediata. Ahora acepta el campo opcional `tipoPedido`.

**Diferencia clave vs v1 anterior:**
- Si `tipoPedido` es `"APARTADO"` o `"FIADO"` → **no se crea Venta**, solo se crea el Pedido con estado = `tipoPedido`. El response devuelve `pedidoId` y `ventaId` es `null`.
- Si `tipoPedido` es `null` / `"NORMAL"` → comportamiento igual al actual (Pedido + Venta cerrados en un shot).

**Request (campos relevantes, igual al existente + `tipoPedido` + `observaciones`):**
```json
{
  "usuarioId": 1,
  "clienteId": 10,
  "pagosYMesesId": 1,
  "tipoPedido": "APARTADO",
  "observaciones": "Pantaloneta negra talla M",
  "detalles": [
    { "varianteId": 42, "cantidad": 1, "precioVenta": 350.00, "subTotal": 350.00 }
  ]
}
```

> `pagosYMesesId` sigue siendo requerido en el request pero **no se usa** en el flujo crédito (no hay Venta ni cargos).

**Response 200 — flujo crédito (ventaId = null, pedidoId presente):**
```json
{
  "response": {
    "ventaId": null,
    "tipoPago": null,
    "requiereTerminal": false,
    "totalVenta": 350.00,
    "meses": null,
    "descripcionPago": null,
    "intentId": null,
    "pedidoId": 55
  }
}
```

**Response 200 — flujo normal (sin cambios):**
```json
{
  "response": {
    "ventaId": 23,
    "tipoPago": "Efectivo",
    "requiereTerminal": false,
    "totalVenta": 350.00,
    "meses": null,
    "descripcionPago": "Efectivo / Transferencia",
    "intentId": null,
    "pedidoId": null
  }
}
```

**Lógica para el front:**
```ts
if (response.pedidoId) {
  // crédito → redirigir a /abonos con el pedidoId
} else {
  // venta inmediata → flujo normal
}
```

---

### 2. Registrar un abono

```
POST /v1/abonos/{pedidoId}
```

**Request:**
```json
{
  "monto": 100.00,
  "fechaPago": "2026-06-27",
  "metodoPago": "EFECTIVO",
  "nota": "primer abono"
}
```
- `fechaPago`: opcional, default = hoy
- `metodoPago`: `"EFECTIVO"` | `"TRANSFERENCIA"` | `"TARJETA"` (default `"EFECTIVO"`)
- `nota`: opcional

**Response 200:**
```json
{
  "mensaje": "La peticion fue exitosa",
  "code": 200,
  "data": {
    "id": 1,
    "monto": 100.00,
    "fechaPago": "27/06/2026",
    "metodoPago": "EFECTIVO",
    "nota": "primer abono"
  }
}
```

**Response 400** si el pedido ya está `PAGADO`, `cancelado`, o es de tipo `NORMAL`.

> **Auto-cierre:** cuando `totalPagado >= totalPedido` el back cambia `estadoPedido` a `"PAGADO"` automáticamente.
> Para `APARTADO` además guarda `fechaRecogida = hoy` (fecha de entrega del producto).

---

### 3. Historial de abonos de un pedido

```
GET /v1/abonos/{pedidoId}
```

**Response 200:**
```json
{
  "code": 200,
  "data": [
    { "id": 1, "monto": 100.00, "fechaPago": "27/06/2026", "metodoPago": "EFECTIVO", "nota": "primer abono" },
    { "id": 2, "monto": 200.00, "fechaPago": "05/07/2026", "metodoPago": "TRANSFERENCIA", "nota": null }
  ]
}
```

---

### 4. Reporte: estado de cuenta (pedidos pendientes de liquidar)

```
GET /v1/abonos/reporte/estado-cuenta
```

Devuelve todos los pedidos `APARTADO` o `FIADO` que **aún no están pagados**.

**Response 200:**
```json
{
  "code": 200,
  "data": [
    {
      "pedidoId": 45,
      "tipoPedido": "FIADO",
      "estadoPedido": "FIADO",
      "cliente": "María López",
      "telefono": "5512345678",
      "totalPedido": 350.00,
      "totalPagado": 100.00,
      "saldo": 250.00,
      "fechaPedido": "27/06/2026",
      "abonos": [
        { "id": 1, "monto": 100.00, "fechaPago": "27/06/2026", "metodoPago": "EFECTIVO", "nota": null }
      ]
    }
  ]
}
```

---

### 5. Reporte: pedidos liquidados

```
GET /v1/abonos/reporte/pagados
```

Devuelve todos los pedidos `APARTADO` o `FIADO` con `estadoPedido = "PAGADO"`.

**Response 200:**
```json
{
  "code": 200,
  "data": [
    {
      "pedidoId": 40,
      "tipoPedido": "APARTADO",
      "cliente": "Ana García",
      "telefono": "5598765432",
      "totalPedido": 500.00,
      "fechaPedido": "10/06/2026",
      "fechaUltimoPago": "27/06/2026",
      "abonos": [
        { "id": 3, "monto": 200.00, "fechaPago": "15/06/2026", "metodoPago": "EFECTIVO", "nota": null },
        { "id": 7, "monto": 300.00, "fechaPago": "27/06/2026", "metodoPago": "TRANSFERENCIA", "nota": "liquidación" }
      ]
    }
  ]
}
```

---

### Resumen de endpoints nuevos

| Método | URL | Descripción |
|---|---|---|
| `POST` | `/v1/ventas/save` | **MODIFICADO** — acepta `tipoPedido`; si es APARTADO/FIADO devuelve `pedidoId` en vez de `ventaId` |
| `POST` | `/v1/pedidos/savePedido` | Ya existía — ahora acepta `tipoPedido` |
| `POST` | `/v1/abonos/{pedidoId}` | Registrar abono |
| `GET` | `/v1/abonos/{pedidoId}` | Historial de abonos |
| `GET` | `/v1/abonos/reporte/estado-cuenta` | Pedidos con saldo pendiente |
| `GET` | `/v1/abonos/reporte/pagados` | Pedidos liquidados |

### Estados posibles de `estadoPedido`

| Estado | Significado |
|---|---|
| `PENDIENTE` | Pedido normal sin confirmar (flujo existente) |
| `APARTADO` | Reservado, abonando, sin entregar |
| `FIADO` | Entregado, abonando, sin liquidar |
| `PAGADO` | Liquidado (cierre de APARTADO o FIADO) |
| `Entregado` | Confirmado por flujo normal de venta (ya existía) |
| `cancelado` | Cancelado (ya existía) |

---

## Chatbot — Tarjetas de productos (2026-07-01)

### Qué cambia

El chatbot ahora puede mostrar productos como **tarjetas visuales** cuando el cliente pide ver
productos por categoría o marca. El response del chatbot incluye campos nuevos opcionales.
La paginación ("ver más") se hace con un endpoint separado **sin pasar por la IA** (0 tokens extra).

---

### 1. POST /v1/chatbot/mensaje — response extendido

**Sin cambio en el request** — sigue igual que antes.

**Response cuando el bot quiere mostrar productos:**
```json
{
  "respuesta": "¡Claro, aquí te muestro! 👜",
  "bloqueado": false,
  "segundosEspera": 0,
  "productos": [
    {
      "varianteId": 12,
      "nombre": "Bolsa Coach Café",
      "marca": "Coach",
      "talla": "única",
      "color": "café",
      "precio": 850.0,
      "stock": 5,
      "descripcion": "Bolsa de piel genuina, correa ajustable",
      "codigoBarras": "ABC123"
    },
    {
      "varianteId": 13,
      "nombre": "Bolsa Coach Negra",
      "marca": "Coach",
      "talla": "única",
      "color": "negra",
      "precio": 900.0,
      "stock": 3,
      "descripcion": null,
      "codigoBarras": "DEF456"
    }
  ],
  "hayMas": true,
  "busquedaQuery": "Coach",
  "busquedaOffset": 2
}
```

**Response cuando el bot responde texto normal (sin productos):**
```json
{
  "respuesta": "Hola, ¿en qué te puedo ayudar? 😊",
  "bloqueado": false,
  "segundosEspera": 0
}
```
Los campos `productos`, `hayMas`, `busquedaQuery` y `busquedaOffset` **solo aparecen** cuando
el bot quiere mostrar tarjetas. Si no están en el response, simplemente no renderizar tarjetas.

**Campos nuevos 2026-07-02 — respuesta a BUG-CB-03:** `descripcion` (puede ser `null` si la
variante no tiene una cargada) y `codigoBarras` (solo aparece en el JSON si el producto tiene
código de barras registrado — si no, el campo no viene). Úsenlos para diferenciar visualmente
tarjetas que comparten nombre/marca/precio idénticos (ej. mostrar el código de barras chiquito
debajo del nombre cuando `talla`/`color` vengan ambos `null`).

---

### 2. GET /v1/chatbot/buscar — "Ver más" sin IA

Llamar este endpoint cuando el usuario hace clic en el botón **"Ver más"**.
No llama a OpenAI, solo consulta la BD. Muy rápido y sin costo de tokens.

**Request:**
```
GET /mis-productos/v1/chatbot/buscar?q=Coach&offset=2
```
| Param | Tipo | Descripción |
|---|---|---|
| `q` | string | La misma búsqueda del response anterior (`busquedaQuery`) |
| `offset` | number | El valor de `busquedaOffset` del response anterior |

**Response:**
```json
{
  "productos": [ ... ],
  "hayMas": false,
  "busquedaQuery": "Coach",
  "busquedaOffset": 4
}
```

---

### 3. Cómo obtener la imagen de cada tarjeta

Cada producto tiene `varianteId`. Usar el endpoint ya existente (⚠️ corregido 2026-07-02 — la URL
tenía el `/v1/` en la posición equivocada):

```
GET /mis-productos/variantes/v1/imagenes/{varianteId}
```

**⚠️ Corrección 2026-07-02:** NO tomar el primer elemento del array a secas — tomar el elemento
con **`"principal": true`**. Si ninguno viene marcado como principal, ahí sí usar el primero como
fallback. Si el array está vacío, mostrar imagen placeholder.
```js
const imagenes = await fetch(`/mis-productos/variantes/v1/imagenes/${varianteId}`).then(r => r.json());
const imagen = imagenes.data.find(img => img.principal) || imagenes.data[0];
```

---

### 4. Flujo completo para el front

```
1. Usuario escribe "tienes bolsas?"
2. Front → POST /v1/chatbot/mensaje
3. Response tiene productos[] y hayMas=true
4. Front muestra:
   - Burbuja de chat con respuesta.respuesta
   - 2 tarjetas de producto debajo (con imagen de /variantes/imagenes/{id})
   - Botón "Ver más" si hayMas=true

5. Usuario hace clic en "Ver más"
6. Front → GET /v1/chatbot/buscar?q={busquedaQuery}&offset={busquedaOffset}
7. Response trae 2 productos más
8. Front AGREGA las tarjetas nuevas debajo de las anteriores (no reemplaza)
9. Si el nuevo hayMas=false, ocultar el botón "Ver más"

10. Usuario escoge un producto → lo puede agregar al carrito normalmente
```

---

### 5. Diseño sugerido de tarjeta

```
┌─────────────────────┐
│   [imagen 150x150]  │
├─────────────────────┤
│ Bolsa Coach Café    │
│ Marca: Coach        │
│ Color: café         │
│ Talla: única        │
│ $850.00             │
│ Stock: 5 pzas       │
│  [Ver detalle]      │
└─────────────────────┘
```

El botón "Ver detalle" puede abrir el modal/página de producto existente
usando `varianteId` para hacer el fetch de detalle.

---

### 6. Notas importantes

- `marca`, `talla`, `color` pueden ser `null` — mostrar solo los que tengan valor.
- `hayMas` es `false` cuando ya no hay más resultados — ocultar el botón.
- El botón "Ver más" siempre usa `busquedaQuery` y `busquedaOffset` del **último response**.
- Si el usuario hace una nueva pregunta después de ver tarjetas, el historial del chat continúa normalmente.

---

## Ticket / Comprobante — implementación FRONT (2026-07-01)

> El back solo manda datos. El front genera el HTML, aplica estilos de impresión y llama `window.print()`.
> El correo se implementó en el back (ver sección "Correo — cómo lo hace el front" más abajo).
> WhatsApp automático al cliente quedó descartado (ver `PLAN_MEJORAS.md`) — en su lugar el ticket
> lleva un QR de "contáctanos por WhatsApp" (ver siguiente sección).

---

### QRs del ticket (2026-07-01)

La generación del QR es **100% front** (librería JS, ej. `npm install qrcode` o `angularx-qrcode`).
Los *datos* que van dentro de cada QR salen de dos fuentes distintas: la URL de la tienda es fija
(`environment.ts`) y los links de WhatsApp/Facebook del negocio salen de un endpoint nuevo del back
(`GET /v1/negocio/contactos`), **no se arman a mano ni se hardcodea ningún número**.

#### Endpoint nuevo — `GET /v1/negocio/contactos` (público, 2026-07-01)

```
GET /mis-productos/v1/negocio/contactos
```

Response:
```json
{
  "data": {
    "whatsappUrl": "https://wa.me/52XXXXXXXXXX",
    "facebookUrl": "https://facebook.com/novedadesjade"
  }
}
```

- Público, no requiere login.
- **Diferencia con `GET /v1/negocio/estado`:** ese endpoint también trae `whatsappUrl`/`facebookUrl`,
  pero los devuelve en `null` mientras el negocio está **abierto** (a propósito, para otro caso de
  uso). Este endpoint nuevo (`/contactos`) siempre los devuelve, sin importar si está abierto o
  cerrado — por eso es el que hay que usar para el ticket, que se genera justo durante la venta
  (negocio abierto).
- Cualquiera de los dos campos puede venir `null`/vacío si el admin no los configuró — en ese caso
  no mostrar ese QR (ver "Cuántos QRs mostrar" abajo).
- Los valores ya son URLs completas y listas para usar (`https://wa.me/...`, `https://facebook.com/...`)
  — el front solo las mete en el QR, no arma nada.

#### QR 1 — Link a la tienda

Apunta a la URL pública de la tienda, sacada de `environment.ts` (`environment.tiendaUrl` o la que
ya exista para CORS/base de la app), NO de este endpoint:
- Prod: `https://shop.novedades-jade.com.mx`
- QA: `https://qa.shop.novedades-jade.com.mx`

#### QR 2 — "Contáctanos por WhatsApp" (click-to-chat)

Usa directo el `whatsappUrl` que regresa `GET /v1/negocio/contactos` (ya es un link `wa.me/...`
armado por el admin desde el panel — no hay que construirlo ni pedir el número por separado).

- Al escanear/tocar, abre el WhatsApp de quien escanea con un chat ya armado **hacia el negocio**
  (es al revés de "el negocio le manda algo al cliente" — aquí el cliente es quien envía, con un
  solo tap en "Enviar").
- El número no aparece como texto en el ticket, solo va codificado dentro del QR.
- Si se quiere texto precargado (ej. `"Hola, tengo una duda sobre mi compra folio #42"`), se le
  agrega `?text=<mensaje url-encoded>` al final del `whatsappUrl` recibido antes de generar el QR.

#### QR 3 — Facebook del negocio

Usa directo el `facebookUrl` que regresa el mismo endpoint. Mismo tratamiento que el de WhatsApp.

#### ✅ Cuántos QRs mostrar — RESUELTO: los 3 fijos siempre, sin rotación

Se confirmó mostrar los 3 QRs (tienda, WhatsApp, Facebook) fijos siempre — sin rotación
aleatoria, ya implementado y funcionando del lado del front.

---

### Tipos de ticket y de dónde salen los datos

#### A) Ticket de Venta Directa (NORMAL)

**Cuándo mostrarlo:** después del éxito de `POST /mis-productos/v1/ventas/save`
cuando `tipoPedido = "NORMAL"` (o cuando el pedido no es crédito).

**De dónde salen los datos:**

| Campo del ticket | Fuente |
|---|---|
| Nombre cliente | Estado local del form (el front ya lo tiene seleccionado) |
| Artículos, cantidades, precios | Estado local del carrito |
| Total | `res.data.totalVenta` |
| Método de pago | Estado local del form |
| Monto entregado (dado) | Estado local del form |
| Cambio | Calculado en el front: `montoDado - totalVenta` |
| Fecha | `new Date()` en el momento de la venta |
| # Venta | `res.data.ventaId` |

**Ticket generado:**
```
╔══════════════════════════════╗
║       NOVEDADES JADE         ║
╠══════════════════════════════╣
║ Venta #1042   01/07/2026     ║
║ Cliente: María López         ║
╠══════════════════════════════╣
║ 1x Pantalón Negro M  $350.00 ║
║ 1x Blusa Floral S    $180.00 ║
╠══════════════════════════════╣
║ TOTAL              $530.00   ║
║ MÉTODO: EFECTIVO             ║
║ ENTREGÓ:           $600.00   ║
║ CAMBIO:             $70.00   ║
╚══════════════════════════════╝
```

---

#### B) Ticket de Abono

**Cuándo mostrarlo:** después del éxito de `POST /mis-productos/v1/abonos/{pedidoId}`

**De dónde salen los datos:**

Primero el front ya tiene el pedido en pantalla. Al registrar el abono, el response trae:

```json
{
  "data": {
    "id": 5,
    "monto": 150.00,
    "fechaPago": "01/07/2026",
    "metodoPago": "EFECTIVO",
    "nota": "segundo abono",
    "montoDado": 200.00,
    "cambio": 50.00,
    "estadoPedido": "APARTADO",
    "saldoRestante": 100.00
  }
}
```

Para completar el ticket (nombre cliente, artículos, total del apartado) el front hace:

```
GET /mis-productos/v1/pedidos/{pedidoId}/detalle
```

Response que necesitas:
```json
{
  "data": {
    "pedidoId": 42,
    "tipoPedido": "APARTADO",
    "estadoPedido": "APARTADO",
    "totalPedido": 350.00,
    "totalPagado": 250.00,
    "saldoPendiente": 100.00,
    "fechaPedido": "2026-06-15",
    "clienteNombre": "María López",
    "clienteTelefono": "7221234567",
    "detalles": [
      {
        "varianteId": 12,
        "productoNombre": "Pantalón Negro",
        "talla": "M",
        "color": "negro",
        "cantidad": 1,
        "precioUnitario": 350.00,
        "subTotal": 350.00
      }
    ]
  }
}
```

**Ticket generado:**
```
╔══════════════════════════════╗
║  NOVEDADES JADE — ABONO      ║
╠══════════════════════════════╣
║ Apartado #42  01/07/2026     ║
║ Cliente: María López         ║
╠══════════════════════════════╣
║ Pantalón Negro M     $350.00 ║
╠══════════════════════════════╣
║ Total apartado:      $350.00 ║
║ Ya pagado:           $250.00 ║
║ Abono de hoy:        $150.00 ║
║ Saldo pendiente:     $100.00 ║
╠══════════════════════════════╣
║ MÉTODO: EFECTIVO             ║
║ ENTREGÓ:             $200.00 ║
║ CAMBIO:               $50.00 ║
╚══════════════════════════════╝
```

> Si `metodoPago = "TRANSFERENCIA"`, no mostrar las filas ENTREGÓ y CAMBIO (serán `null`).

---

#### C) Ticket de Liquidación (pedido PAGADO)

**Cuándo mostrarlo:** cuando el response del abono trae `estadoPedido = "PAGADO"`.
Mismo flujo que el ticket de abono — solo cambia el encabezado y no hay saldo pendiente.

**Ticket generado:**
```
╔══════════════════════════════╗
║ NOVEDADES JADE — ¡LIQUIDADO! ║
╠══════════════════════════════╣
║ Apartado #42  01/07/2026     ║
║ Cliente: María López         ║
╠══════════════════════════════╣
║ Pantalón Negro M     $350.00 ║
╠══════════════════════════════╣
║ Total pagado:        $350.00 ║
║ ✅ PAGADO COMPLETAMENTE      ║
╚══════════════════════════════╝
```

---

#### D) Ticket de Cancelación

**Cuándo mostrarlo:** después del éxito de `PUT /mis-productos/v1/abonos/{pedidoId}/cancelar`

Response de cancelación:
```json
{
  "data": {
    "pedidoId": 42,
    "tipoPedido": "APARTADO",
    "estadoPedido": "cancelado",
    "totalPagado": 100.00,
    "totalPendiente": 250.00,
    "stockDevuelto": true,
    "mensaje": "Pedido cancelado correctamente"
  }
}
```

El front también necesita llamar `GET /mis-productos/v1/pedidos/{id}/detalle` para obtener
`clienteNombre`, `motivoCancelacion` y los artículos.

**Ticket generado:**
```
╔══════════════════════════════╗
║  NOVEDADES JADE — CANCELADO  ║
╠══════════════════════════════╣
║ Apartado #42  01/07/2026     ║
║ Cliente: María López         ║
╠══════════════════════════════╣
║ Pantalón Negro M     $350.00 ║
╠══════════════════════════════╣
║ Motivo: NO SE PRESENTÓ       ║
║ Abonos realizados:   $100.00 ║
║ (saldo a favor del cliente)  ║
╚══════════════════════════════╝
```

---

### Dónde aparece el botón de imprimir

| Pantalla | Cuándo mostrar el botón |
|---|---|
| Venta directa | Al cerrar el modal/toast de "Venta exitosa" — mostrar botón **🖨️ Imprimir ticket** |
| Registrar abono | En el toast/modal de confirmación del abono |
| Liquidación (PAGADO) | En el toast/modal — ticket distinto al de abono normal |
| Cancelación | En el modal de confirmación de cancelación |

---

### Cómo imprimir

```javascript
function imprimirTicket(htmlTicket) {
  const ventana = window.open('', '_blank', 'width=400,height=600');
  ventana.document.write(`
    <html>
      <head>
        <title>Ticket</title>
        <style>
          body {
            font-family: 'Courier New', monospace;
            font-size: 12px;
            width: 280px;
            margin: 0 auto;
            padding: 8px;
          }
          .titulo    { text-align: center; font-weight: bold; font-size: 14px; }
          .linea     { border-top: 1px dashed #000; margin: 4px 0; }
          .fila      { display: flex; justify-content: space-between; }
          .total     { font-weight: bold; }
          .centro    { text-align: center; }
          @media print {
            body { width: 100%; }
          }
        </style>
      </head>
      <body>
        ${htmlTicket}
        <script>window.print(); window.close();<\/script>
      </body>
    </html>
  `);
  ventana.document.close();
}
```

---

### Estructura HTML sugerida del ticket

```javascript
function generarHtmlTicket({ tipo, numero, fecha, cliente, articulos,
                              total, totalPagado, saldoPendiente, abonoHoy,
                              metodoPago, montoDado, cambio, motivo }) {
  const formatPeso = (n) => n != null ? `$${n.toFixed(2)}` : '';
  const hoy = fecha || new Date().toLocaleDateString('es-MX');

  let encabezado = '';
  if (tipo === 'venta')        encabezado = 'COMPROBANTE DE VENTA';
  if (tipo === 'abono')        encabezado = 'COMPROBANTE DE ABONO';
  if (tipo === 'liquidado')    encabezado = '¡APARTADO LIQUIDADO!';
  if (tipo === 'cancelacion')  encabezado = 'CANCELACIÓN DE PEDIDO';

  const filasArticulos = articulos.map(a => `
    <div class="fila">
      <span>${a.cantidad}x ${a.productoNombre}${a.talla ? ' ' + a.talla : ''}</span>
      <span>${formatPeso(a.subTotal)}</span>
    </div>
  `).join('');

  const filaPago = metodoPago === 'EFECTIVO' ? `
    <div class="fila"><span>ENTREGÓ:</span><span>${formatPeso(montoDado)}</span></div>
    <div class="fila"><span>CAMBIO:</span><span>${formatPeso(cambio)}</span></div>
  ` : `<div class="fila"><span>MÉTODO:</span><span>TRANSFERENCIA</span></div>`;

  return `
    <div class="titulo">NOVEDADES JADE</div>
    <div class="titulo">${encabezado}</div>
    <div class="linea"></div>
    <div class="fila"><span>Folio #${numero}</span><span>${hoy}</span></div>
    <div>Cliente: ${cliente}</div>
    <div class="linea"></div>
    ${filasArticulos}
    <div class="linea"></div>
    ${total        ? `<div class="fila total"><span>TOTAL:</span><span>${formatPeso(total)}</span></div>` : ''}
    ${totalPagado  ? `<div class="fila"><span>Ya pagado:</span><span>${formatPeso(totalPagado)}</span></div>` : ''}
    ${abonoHoy     ? `<div class="fila"><span>Abono de hoy:</span><span>${formatPeso(abonoHoy)}</span></div>` : ''}
    ${saldoPendiente != null && saldoPendiente > 0
        ? `<div class="fila"><span>Saldo pendiente:</span><span>${formatPeso(saldoPendiente)}</span></div>` : ''}
    ${tipo === 'liquidado' ? `<div class="centro">✅ PAGADO COMPLETAMENTE</div>` : ''}
    ${motivo ? `<div>Motivo: ${motivo}</div>` : ''}
    <div class="linea"></div>
    ${filaPago}
    <div class="linea"></div>
    <div class="centro">¡Gracias por tu compra!</div>
  `;
}
```

---

### Correo (y WhatsApp EN PAUSA) — cómo lo hace el front

> **DECISIÓN 2026-07-01:** por ahora solo se implementa el envío por **correo**. WhatsApp queda
> en pausa (ver "DECISIÓN PENDIENTE" en `PLAN_MEJORAS.md`) — CallMeBot (gratis) solo le avisa al
> negocio, no al cliente, y Twilio (que sí notificaría al cliente) es de pago y requiere alta de
> cuenta + código nuevo que no se justifica por ahora. **El front NO debe mostrar el checkbox de
> WhatsApp.** El campo `notificacion.enviarWhatsapp` se puede omitir/mandar `false` siempre; el
> back lo soporta pero no hay forma de que le llegue nada real al cliente todavía.

El front genera el ticket (ya lo hace para imprimir). Si el usuario marcó el checkbox de correo,
**incluye el ticket en el mismo request** que registra la acción. El back lo recibe y lo envía.

#### Checkbox en el UI

Mostrar en el form de abono, venta directa y cancelación:

```html
<label>
    <input type="checkbox" [(ngModel)]="enviarCorreo" />
    Enviar ticket al correo del cliente
</label>
```

- Pre-marcar correo si el cliente tiene email registrado.
- Si el cliente no tiene correo → deshabilitar el checkbox.

---

#### (Referencia, no implementar por ahora) Cómo armar el ticketTexto para WhatsApp

Queda documentado por si más adelante se retoma con Twilio — no construir esto en el front hoy.
WhatsApp no soporta HTML — mandar texto plano. Generar con una función separada:

```javascript
function generarTextoWhatsapp({ tipo, numero, fecha, cliente, articulos,
                                 total, abonoHoy, saldoPendiente, metodoPago,
                                 montoDado, cambio, motivo }) {
  const fmt = (n) => n != null ? `$${n.toFixed(2)}` : '';
  const hoy = fecha || new Date().toLocaleDateString('es-MX');

  let lineas = [
    '🛍️ NOVEDADES JADE',
    tipo === 'venta'       ? 'Comprobante de venta' :
    tipo === 'abono'       ? 'Comprobante de abono' :
    tipo === 'liquidado'   ? '✅ Apartado liquidado' :
                             '❌ Cancelación de pedido',
    `Folio #${numero} — ${hoy}`,
    `Cliente: ${cliente}`,
    '─────────────────────',
    ...articulos.map(a =>
      `• ${a.cantidad}x ${a.productoNombre}${a.talla ? ' ' + a.talla : ''} — ${fmt(a.subTotal)}`
    ),
    '─────────────────────',
  ];

  if (total)          lineas.push(`Total: ${fmt(total)}`);
  if (abonoHoy)       lineas.push(`Abono de hoy: ${fmt(abonoHoy)}`);
  if (saldoPendiente) lineas.push(`Saldo pendiente: ${fmt(saldoPendiente)}`);
  if (tipo === 'liquidado') lineas.push('✅ PAGADO COMPLETAMENTE');
  if (motivo)         lineas.push(`Motivo cancelación: ${motivo}`);

  lineas.push('─────────────────────');
  lineas.push(`Método: ${metodoPago}`);
  if (metodoPago === 'EFECTIVO' && montoDado) {
    lineas.push(`Entregó: ${fmt(montoDado)}`);
    lineas.push(`Cambio: ${fmt(cambio)}`);
  }
  lineas.push('¡Gracias por tu compra! 🙏');

  return lineas.join('\n');
}
```

---

#### Campos que se agregan al request cuando hay correo/WhatsApp

Aplicar en: `POST /v1/abonos/{pedidoId}`, `POST /v1/ventas/save`,
`PUT /v1/abonos/{pedidoId}/cancelar`.

**IMPORTANTE — implementación final:** los campos van anidados dentro de un objeto
`"notificacion"`, no planos en la raíz del body (`NotificacionRequest.java`):

```json
{
  "monto": 150.00,
  "metodoPago": "EFECTIVO",
  "montoDado": 200.00,

  "notificacion": {
    "enviarCorreo":   true,
    "enviarWhatsapp": false,
    "ticketHtml":     "<html>...ticket generado por el front...</html>",
    "correo":         "escrito-en-el-modal@ejemplo.com"
  }
}
```

- **Por ahora el front solo maneja `enviarCorreo`, `ticketHtml` y `correo`.** `enviarWhatsapp` se
  manda siempre `false` (o se omite) y `ticketTexto` no hace falta construirlo — ver nota de
  "WhatsApp EN PAUSA" arriba.
- **`correo` (nuevo, 2026-07-01) — para el modal post-venta:** si el cliente no tiene correo
  registrado, se muestra un modal pidiéndolo manualmente; ese valor va en este campo. Si viene con
  valor, el back lo usa como destino en vez del correo de la BD. Si se omite o va vacío, se usa el
  correo registrado (comportamiento normal, sin cambios).
- Si no se quiere enviar nada → no mandar el campo `notificacion` (o mandar `null`). El back solo intenta notificar si `notificacion != null`.
- Si `enviarCorreo = false` → no hace falta mandar `ticketHtml`.

---

#### Qué devuelve el back (campos nuevos en el response)

El back agrega al response normal tres campos extra: `correoEnviado`, `whatsappEnviado`, `erroresEnvio`
(todos `null`/omitidos si no se pidió notificación — `@JsonInclude(NON_NULL)`).

- **Abono** (`POST /v1/abonos/{pedidoId}`) y **cancelación** (`PUT /v1/abonos/{pedidoId}/cancelar`)
  devuelven envuelto en `ResponseGeneric` (campo `data`):

```json
{
  "data": {
    "id": 5,
    "monto": 150.00,
    "estadoPedido": "APARTADO",
    "saldoRestante": 100.00,

    "correoEnviado":    true,
    "whatsappEnviado":  false,
    "erroresEnvio":     []
  }
}
```

- **Venta directa** (`POST /v1/ventas/save`) **NO** usa `ResponseGeneric` — el back devuelve
  `VentaDirectaResponse` directo, sin envolver en `data`:

```json
{
  "ventaId": 10,
  "tipoPago": "EFECTIVO",
  "requiereTerminal": false,
  "total": 350.00,
  "correoEnviado":   true,
  "whatsappEnviado": false,
  "erroresEnvio":    []
}
```

| Campo | Tipo | Descripción |
|---|---|---|
| `correoEnviado` | boolean | `true` si el correo se envió con éxito |
| `whatsappEnviado` | boolean | `true` si el WhatsApp se envió con éxito |
| `erroresEnvio` | string[] | Lista de errores si algún envío falló (puede estar vacío) |

#### Cómo mostrar el resultado en el UI

```
✅ Abono registrado correctamente
✅ Correo enviado a maria@gmail.com
❌ WhatsApp no se pudo enviar — intentar después
```

- Si `correoEnviado = false` y el usuario lo pidió → mostrar aviso (no es error fatal).
- El abono/venta ya quedó guardado aunque falle el envío — no bloquear el flujo.

---

### Resumen de endpoints que usa el ticket (todos ya existen)

| Tipo de ticket | Endpoints necesarios |
|---|---|
| Venta directa | Estado local del carrito + `res` (sin wrapper) del `POST /v1/ventas/save` |
| Abono | `res.data` del `POST /v1/abonos/{pedidoId}` + `GET /v1/pedidos/{id}/detalle` |
| Liquidado | Igual que abono |
| Cancelación | `res.data` del `PUT /v1/abonos/{pedidoId}/cancelar` + `GET /v1/pedidos/{id}/detalle` |

**El ticket HTML/texto lo genera el front. El back solo lo recibe y lo transporta por correo/WhatsApp.**

---

## EP-T1 y EP-T2 — Detalle de pedido enriquecido + reenviar comprobante (2026-07-02)

### EP-T1 — `GET /v1/pedidos/{id}/detalle` (endpoint que ya usabas — mismo path, mismo auth)

**Qué cambia:** se agregaron 4 campos nuevos a la respuesta. Nada de lo que ya consumías cambió.

**Response — campos nuevos (además de los que ya recibías):**
```json
{
  "data": {
    "clienteCorreo": "juan@email.com",
    "metodoPago": "EFECTIVO",
    "montoDado": 350.00,
    "abonos": [
      { "id": 10, "monto": 200.00, "fechaPago": "2026-07-01", "metodoPago": "EFECTIVO", "nota": "Enganche", "montoDado": 220.00 }
    ]
  }
}
```

| Campo | Tipo | Cuándo viene |
|---|---|---|
| `clienteCorreo` | string \| null | Si el cliente tiene correo registrado |
| `metodoPago` | string \| null | **Solo en ventas NORMAL al contado.** `null` en créditos (APARTADO/FIADO) — ver `abonos[]` |
| `montoDado` | number \| null | Solo ventas NORMAL, y **solo si el front lo mandó** al crear la venta (ver acción requerida abajo). En pedidos vendidos antes de este cambio siempre es `null` |
| `abonos` | array | Historial de pagos del crédito. Lista vacía `[]` en ventas NORMAL |

**⚠️ Acción requerida — sin esto `montoDado` nunca llega:** `montoDado` no se guardaba antes en el back para ventas de contado (solo existía para abonos). Para que el ticket pueda mostrar "ENTREGÓ / CAMBIO" en ventas nuevas, el front debe **agregar el campo `montoDado` al body de `POST /v1/ventas/save`**:
```json
{
  "usuarioId": 1,
  "clienteId": 5,
  "detalles": [ ... ],
  "montoDado": 350.00
}
```
- Mandarlo solo cuando el método de pago sea EFECTIVO (igual que ya calculas el cambio localmente hoy, nada más ahora también se lo mandas al back).
- Los pedidos vendidos **antes** de que el front implemente esto se quedan con `montoDado: null` para siempre — no hay forma de recuperarlo, el ticket de esos pedidos viejos simplemente no muestra esa línea.

---

### EP-T2 — `POST /v1/pedidos/{id}/notificar` (endpoint nuevo)

**Request:** `POST /mis-productos/v1/pedidos/{id}/notificar` — requiere rol ADMIN (Bearer token).
```json
{
  "correo": "cliente@email.com",
  "ticketHtml": "<html>...ticket generado por el front...</html>"
}
```

**Response 200:**
```json
{ "data": "Comprobante enviado correctamente a cliente@email.com" }
```
(va envuelto en `ResponseGeneric` como el resto del proyecto — el mensaje de éxito queda en `data`, no en `mensaje` como en el ejemplo original que se pidió)

**Response 400:**
```json
{ "mensaje": "No se pudo enviar el correo. Verifica la dirección." }
```

**Qué hace:** reenvía tal cual el `ticketHtml` recibido por correo (asunto `"Comprobante de tu pedido #{id} — Novedades Jade"`). No genera nada nuevo — el HTML ya lo arma el front (con sus QR de tienda/WhatsApp/Facebook incluidos, como en el resto de tickets).

**Uso:** botón "reenviar por correo" en cualquier pantalla de detalle de pedido, sin depender de que sea justo al momento de la venta/abono.

---

### Preguntas del front — confirmadas 2026-07-02

El front reportó no ver el QR de Facebook y preguntó 4 cosas puntuales. Respuestas verificadas
contra el código (no supuestas):

1. **`GET /v1/negocio/contactos` va envuelto en `ResponseGeneric`** — leer `response.data.whatsappUrl`
   / `response.data.facebookUrl`, NO `response.whatsappUrl` directo. Esto es lo que causaba que
   el QR de Facebook (y probablemente el de WhatsApp) no aparecieran — ya estaba documentado así
   arriba, pero se confirma explícito por si se leyó mal.
2. **No existe `tiendaUrl` en el back** — nunca se implementó. La intención original (ver "QR 1"
   arriba) es que el front lo resuelva con `environment.ts` / `window.location.origin`, no del
   back. **Pendiente de confirmar con el front:** si `window.location.origin` no sirve en su caso
   (ej. el ticket se genera en un contexto sin ese origin correcto), avisar y se agrega como campo
   nuevo a este mismo endpoint — no implementado todavía, a la espera de esa confirmación.
3. **`GET /v1/pedidos/{id}/detalle` va envuelto:** `{ "data": { pedidoId, detalles[], clienteCorreo,
   metodoPago, montoDado, abonos[] } }` — ya documentado arriba en EP-T1, confirmado sin cambios.
4. **`POST /v1/pedidos/{id}/notificar`:** éxito → `data` trae el texto de confirmación; error →
   `mensaje` trae el motivo (dos campos distintos según si fue éxito o error, revisar el `code`/HTTP
   status para saber cuál leer) — ya documentado arriba en EP-T2, confirmado sin cambios.

---

## Reportes de ventas (2026-07-02) — endpoints nuevos

> Todos requieren rol ADMIN (Bearer token). Todos van envueltos en `ResponseGeneric`
> (`{ "data": {...} }` o `{ "data": [...] }`), mismo patrón que el resto del proyecto.

### `GET /v1/reportes/ventas/diario?fecha=YYYY-MM-DD`

**Request:** `GET /mis-productos/v1/reportes/ventas/diario?fecha=2026-07-02`

**Response 200:**
```json
{
  "data": {
    "fecha": "2026-07-02",
    "totalVenta": 4350.00,
    "totalGanancia": 1200.00,
    "cantidadVentas": 12
  }
}
```
Si no hubo ventas ese día: `totalVenta`/`totalGanancia` vienen en `0.0`, `cantidadVentas` en `0` (no error, no null).

---

### `GET /v1/reportes/ventas/mensual?mes=YYYY-MM`

**Request:** `GET /mis-productos/v1/reportes/ventas/mensual?mes=2026-07`

**Response 200:**
```json
{
  "data": {
    "mes": "2026-07",
    "totalVenta": 45000.00,
    "totalGanancia": 12500.00,
    "cantidadVentas": 130,
    "porDia": [
      { "fecha": "2026-07-01", "totalVenta": 4350.00, "totalGanancia": 1200.00, "cantidadVentas": 12 },
      { "fecha": "2026-07-02", "totalVenta": 3100.00, "totalGanancia": 900.00, "cantidadVentas": 8 }
    ]
  }
}
```
- `porDia` solo trae los días que tuvieron al menos una venta (no rellena con ceros los días sin ventas — si necesitan la gráfica con todos los días del mes, hay que completar los huecos en el front).

**Response 400** (formato de `mes` inválido, ej. mandaron `2026-13` o `julio-2026`):
```json
{ "mensaje": "Formato de mes invalido, usar yyyy-MM" }
```

---

### `GET /v1/reportes/ventas/cliente/{clienteId}`

**Request:** `GET /mis-productos/v1/reportes/ventas/cliente/5`

**Response 200:**
```json
{
  "data": {
    "clienteId": 5,
    "clienteNombre": "María López",
    "totalCompras": 7,
    "totalGastado": 3200.00,
    "ventas": [
      { "ventaId": 42, "fechaVenta": "2026-07-01T14:30:00", "totalVenta": 530.00, "gananciaTotal": 150.00 }
    ]
  }
}
```
- Si el cliente existe pero no tiene compras → `totalCompras: 0`, `ventas: []` (no error).
- Solo cuenta ventas de contado (`Venta`), no incluye créditos/abonos — para eso usar el reporte de abonos que ya existe en `GET /v1/abonos/reporte/*`.

**Response 400** (cliente no existe):
```json
{ "mensaje": "Cliente no encontrado: 5" }
```

---

### `GET /v1/reportes/ventas/productos-mas-vendidos?desde=YYYY-MM-DD&hasta=YYYY-MM-DD&limite=10`

**Request:** `GET /mis-productos/v1/reportes/ventas/productos-mas-vendidos?desde=2026-07-01&hasta=2026-07-31&limite=10`

- `limite` es opcional, default `10`.

**Response 200:**
```json
{
  "data": [
    { "varianteId": 12, "productoNombre": "Pantalón clásico negro", "talla": "M", "color": "Negro", "cantidadVendida": 34, "totalVendido": 11900.00 },
    { "varianteId": 8, "productoNombre": "Blusa floral", "talla": "S", "color": "Rosa", "cantidadVendida": 21, "totalVendido": 3780.00 }
  ]
}
```
Ordenado de mayor a menor por `cantidadVendida`. Lista vacía `[]` si no hubo ventas en el rango (no error).

---

**Archivos nuevos en el back:** `ReporteVentasController.java`, `ReporteVentasServiceImpl.java`,
`IReporteVentasService.java`, DTOs en `models/reportes/` (`ReporteDiarioDto`, `ReporteMensualDto`,
`ReporteClienteDto`, `VentaResumenItem`, `ProductoMasVendidoDto`). Sin migración de BD — usa
tablas y columnas que ya existían.

---

## ⚠️ Problema conocido — Chatbot muestra "el mismo producto" repetido (2026-07-02)

### Qué está pasando

Al buscar un producto en el chatbot (ej. "Mochila"), a veces varias tarjetas se ven idénticas —
mismo nombre, mismo precio, sin talla/color que las distinga — como si fuera el mismo producto
mostrado varias veces.

### Diagnóstico — verificado en vivo contra QA, no es bug de front ni de back

Se probó directo contra el servidor real:
```
GET /v1/chatbot/buscar?q=Mochila&offset=0
→ varianteId 117 y 165, ambos "Mochila Prada", $400, sin talla, sin color
GET /v1/chatbot/buscar?q=Mochila&offset=2
→ varianteId 213 y 277, mismos datos otra vez
```

**La búsqueda y la paginación del back funcionan correctamente** — sí trae 4 registros distintos
(`varianteId` 117, 165, 213, 277). El problema es que **esas 4 filas están duplicadas en la base
de datos**: se cargó "Mochila Prada" 4 veces con exactamente los mismos datos, en vez de una sola
vez con más stock, o con talla/color que las diferenciara. Por eso el chatbot no tiene manera de
mostrar "cosas diferentes" — no hay 4 productos diferentes, hay 1 producto repetido 4 veces en la
tabla `variantes`.

**Extra:** las 4 variantes también dan error 500 al pedir su imagen
(`GET /variantes/v1/imagenes/{varianteId}`) — probablemente ninguna tiene una imagen real cargada.

### Qué lo puede solucionar

No es algo que el front pueda arreglar con código — es limpieza de datos. Opciones (pendiente de
decisión del negocio, no se tocó nada todavía):
1. **Borrar las 3 filas sobrantes** desde el panel de admin de variantes y dejar solo 1, con el
   stock correcto sumado (ej. si cada una tenía `stock: 1`, la que quede debería tener `stock: 4`).
2. **Diferenciarlas** si en realidad SÍ son productos distintos (ej. colores/tallas distintos que
   no se llenaron al crearlas) — habría que editarlas para agregar talla/color a cada una.
3. Si se prefiere, se puede pedir un script de limpieza al back una vez que el negocio confirme
   cuál de las dos opciones anteriores aplica — no se debe hacer sin esa confirmación porque borrar
   filas es una acción destructiva.

### Dos correcciones de documentación relacionadas (ya corregidas arriba, en la sección 3 del chatbot)

Mientras se investigaba esto se encontraron 2 errores en la doc que el front ya tenía, que también
podían afectar que la imagen mostrada fuera la incorrecta:
- La URL tenía el `/v1/` mal puesto: era `/v1/variantes/imagenes/{varianteId}`, la correcta es
  `/variantes/v1/imagenes/{varianteId}`.
- Decía "tomar el primer elemento" del array de imágenes — debe ser el elemento con
  `"principal": true` (el primero como fallback solo si ninguno viene marcado).

---

## Dashboard con métricas (2026-07-02) — endpoint nuevo

### `GET /v1/dashboard/resumen`

**Request:** `GET /mis-productos/v1/dashboard/resumen` — requiere rol ADMIN (Bearer token).

**Response 200:**
```json
{
  "data": {
    "ventasHoy": 4350.00,
    "ventasMes": 45000.00,
    "gananciaMes": 12500.00,
    "gastosMes": 3200.00,
    "gananciaNetaMes": 9300.00,
    "pedidosPendientesEntregar": 5,
    "creditosActivos": 12,
    "montoPorCobrar": 8400.00,
    "productosStockBajo": 7
  }
}
```

| Campo | Qué significa |
|---|---|
| `ventasHoy` / `ventasMes` | Total vendido (ventas de contado), hoy y en lo que va del mes |
| `gananciaMes` | Ganancia de las ventas del mes (sin restar gastos) |
| `gastosMes` | Total de gastos registrados en el mes |
| `gananciaNetaMes` | `gananciaMes - gastosMes` |
| `pedidosPendientesEntregar` | Solo cuenta **APARTADO** activos (no pagados, no cancelados) — el producto no se entrega hasta pagarse completo. **FIADO no cuenta aquí** porque ese producto ya se entregó, solo falta cobrarlo |
| `creditosActivos` | APARTADO + FIADO activos (no pagados, no cancelados) — cuenta de pedidos |
| `montoPorCobrar` | Suma de `totalPedido - totalPagado` de esos mismos créditos activos |
| `productosStockBajo` | Variantes con `0 < stock < 5` (no incluye stock=0, eso es "sin stock", otro caso) |

**⚠️ Falta "Clientes nuevos este mes" del plan original** — no se implementó porque `Cliente`
no tiene ninguna columna de fecha de registro/creación, ni siquiera a nivel de tabla base. Sin
eso no hay forma de saber cuáles son "nuevos" vs "de siempre". Si se necesita, avisar y se agrega
la columna (con migración SQL, y solo contará clientes dados de alta después de agregarla — los
existentes no tienen ese dato retroactivamente, mismo caso que pasó con `montoDado`).

**Archivos nuevos en el back:** `DashboardController.java`, `DashboardServiceImpl.java`,
`IDashboardService.java`, `DashboardResumenDto.java`. Sin migración de BD.

---

## Guía de gráficas para reportes (2026-07-02)

Son **2 cosas distintas**, para que no se mezclen:

### 1. Corrección de algo que dijimos mal antes

Cuando se respondió la duda de `ng2-charts` vs Chart.js directo, se dijo que el **Dashboard**
(`GET /v1/dashboard/resumen`) iba a necesitar "varias gráficas más". **Eso estaba mal** — ya se
implementó el dashboard y es solo números sueltos en cards (ventas hoy, stock bajo, etc.), sin
ninguna serie de datos. **El dashboard NO lleva gráficas, solo cards de números.** La única razón
real para tener `ng2-charts` instalado es el punto 2 de abajo.

### 2. Qué gráficas SÍ se pueden armar, y con qué endpoint

Esto es nuevo — una guía de qué gráficas arma cada endpoint de **reportes** (no del dashboard),
usando datos que ya existen, sin pedir nada nuevo al back:

| Gráfica | Endpoint | Campos a usar | Tipo sugerido |
|---|---|---|---|
| Ventas por día del mes | `GET /v1/reportes/ventas/mensual?mes=` | `porDia[].fecha` + `porDia[].totalVenta` | Barras |
| Ventas vs Ganancia por día | Mismo endpoint | `porDia[].totalVenta` + `porDia[].totalGanancia` (ya vienen juntos) | Combinada: barras (venta) + línea (ganancia) |
| Top productos vendidos | `GET /v1/reportes/ventas/productos-mas-vendidos?desde=&hasta=` | Ya viene ordenado desc por `cantidadVendida` | Barras horizontales, o dona con top 5 + "otros" agrupado |
| Comparar mes actual vs mes anterior | Llamar `mensual` **dos veces** (una por cada mes) y combinar en el front — no hay endpoint que regrese los 2 meses juntos | `totalVenta` de cada llamada | Barras agrupadas (2 series) |
| Gasto histórico de un cliente | `GET /v1/reportes/ventas/cliente/{clienteId}` | `ventas[].fechaVenta` + `ventas[].totalVenta`, agrupar por mes en el front (el back regresa venta por venta, no agrupado por mes) | Línea de tendencia |

**Lo que NO tiene dato para gráfica:** `GET /v1/dashboard/resumen` (números sueltos, ver punto 1)
y `GET /v1/reportes/ventas/diario` (es un solo número del día que se pida, no una serie).

---

## Filtros producto/variante por rol (2026-07-02)

### 1. Cambio de comportamiento en los listados públicos (sin acción del front)

Los endpoints de catálogo que ya usa el front **ahora exigen una condición más** para clientes
normales (no ADMIN): además de `stock > 0` y `habilitado`, el producto/variante también debe
**tener al menos una imagen**. Antes solo se exigía stock + habilitado. No cambia el contrato
(mismos campos, mismo formato) — solo puede que aparezcan **menos resultados** que antes si hay
productos sin foto todavía. No requiere ningún cambio en el front, es automático según el rol del
token.

Afecta a: `GET /v1/productos/obtenerProductos`, `GET /v1/productos/buscarNombreOrCodigoBarra`,
`GET /variantes/v1/buscar`, `GET /variantes/v1/getAll`.

**Nota:** para ADMIN no cambia nada — sigue viendo todo el catálogo sin este filtro.

### 2. Endpoints nuevos — filtros de admin (acción requerida en el front)

Antes había endpoints sueltos por cada filtro (`admin/sin-stock`, `admin/no-habilitados`, que
siguen existiendo y funcionando igual). Ahora hay un endpoint único con parámetro `filtro` para
elegir entre 4 vistas, pensado para un dropdown/select en el panel de admin:

```
GET /mis-productos/v1/productos/admin/filtrar?filtro=SIN_STOCK&size=10&page=1
GET /mis-productos/v1/productos/admin/filtrar?filtro=CON_STOCK&size=10&page=1
GET /mis-productos/v1/productos/admin/filtrar?filtro=CON_IMAGENES&size=10&page=1
GET /mis-productos/v1/productos/admin/filtrar?filtro=CON_STOCK_Y_IMAGENES&size=10&page=1

GET /mis-productos/variantes/v1/admin/filtrar?filtro=SIN_STOCK&pagina=1&size=10
GET /mis-productos/variantes/v1/admin/filtrar?filtro=CON_STOCK&pagina=1&size=10
GET /mis-productos/variantes/v1/admin/filtrar?filtro=CON_IMAGENES&pagina=1&size=10
GET /mis-productos/variantes/v1/admin/filtrar?filtro=CON_STOCK_Y_IMAGENES&pagina=1&size=10
```

`filtro` es un enum de texto — valores válidos: `SIN_STOCK`, `CON_STOCK`, `CON_IMAGENES`,
`CON_STOCK_Y_IMAGENES`. Ambos requieren rol ADMIN (Bearer token). Ojo: productos usa `page`,
variantes usa `pagina` (ya era así en el resto de los endpoints de cada uno, se mantiene la misma
convención).

**Response productos — mismo formato que `admin/sin-stock`:**
```json
{
  "pagina": 1,
  "totalPaginas": 3,
  "totalRegistros": 25,
  "t": [
    {
      "idProducto": 42,
      "nombre": "Mochila Prada",
      "color": "negro",
      "precioVenta": 400.0,
      "precioCosto": 200.0,
      "precioRebaja": null,
      "descripcion": "Mochila para mostrar",
      "codigoBarras": "cod1230981",
      "stock": 0,
      "marca": "PRADA",
      "contenido": "1 pieza",
      "habilitado": "1",
      "imagen": { "urlImagen": "https://.../v1/imagenes/file/123" }
    }
  ]
}
```

**Response variantes — mismo formato que `admin/sin-stock` (resumen):**
```json
{
  "pagina": 1,
  "totalPaginas": 2,
  "totalRegistros": 15,
  "t": [
    {
      "id": 117,
      "talla": "s",
      "descripcion": "Mochila para mostrar",
      "color": null,
      "presentacion": "bolsa",
      "stock": 5,
      "marca": null,
      "contenidoNeto": "1 pieza",
      "imagenUrl": "https://.../v1/imagenes/file/456",
      "precio": 300.0,
      "codigoBarras": "cod1230981",
      "nombreProducto": "Mochila para mostrar"
    }
  ]
}
```

**Qué significa cada filtro** (aplica igual a productos y variantes, admin ve TODO el catálogo,
sin exigir habilitado — a diferencia del listado público):
- `SIN_STOCK` → `stock = 0`
- `CON_STOCK` → `stock > 0`
- `CON_IMAGENES` → tiene al menos una imagen cargada (sin importar stock)
- `CON_STOCK_Y_IMAGENES` → `stock > 0` **y** tiene al menos una imagen cargada (combina los dos
  anteriores — es la misma condición que ya se exige al cliente normal, pero aquí sin exigir
  `habilitado`, para que el admin pueda ver también los que están deshabilitados)

**Archivos nuevos/tocados en el back:** `FiltroCatalogoEnum.java` (nuevo),
`IProductosRepository.java`, `IVarianteRepository.java`, `ProductosServiceImpl.java`,
`VarianteServiceImpl.java`, `ProductosControllerImpl.java`, `VarianteController.java`. Sin
migración de BD — usa las tablas de imágenes que ya existían.

## Verificación de correo del cliente (2026-07-02) — acción requerida en el front

### 1. Correo y teléfono ahora son obligatorios en `Cliente`

`POST /v1/clientes/save` y `PUT /v1/clientes/update/{id}` ahora exigen `correoElectronico` y
`numeroTelefonico` (antes eran opcionales, sin ninguna validación). Si faltan o el formato es
inválido, responde **400** con `mensaje` describiendo el error (mismo patrón que ya usan
`nombrePersona`/`apeidoPaterno`/`apeidoMaterno`):
- `correoElectronico`: obligatorio, formato de email válido.
- `numeroTelefonico`: obligatorio, exactamente 10 dígitos (sin espacios, guiones ni lada
  internacional — ej. `"5512345678"`).

**No aplica** a venta directa sin cuenta (`ClienteSinRegistroDto`) — esos campos siguen
opcionales, es una venta de mostrador supervisada por personal.

También ahora `POST /v1/auth/registrar` exige `email` (antes era opcional, solo se validaba el
formato si venía). El endpoint pasó de usar `AuthRequest` a un DTO nuevo `RegistroRequest` con
los mismos 3 campos (`userName`, `password`, `email`) — sin cambio de contrato para el front,
solo ahora `email` es requerido. **`POST /v1/auth/login` no cambia** — sigue sin pedir email.

### 2. Nuevo flujo: verificar el correo con un código de 6 dígitos

Antes de que un cliente **con cuenta** pueda generar un pedido (`POST /pedidos/savePedido`) o
recibir el ticket automático en su correo registrado (venta directa, abono, cancelación de
pedido), su correo debe estar verificado.

```
POST /v1/clientes/{id}/enviar-codigo-verificacion
POST /v1/clientes/{id}/verificar-correo
Body: { "codigo": "123456" }
```

- `enviar-codigo-verificacion`: genera un código de 6 dígitos, lo manda por correo (vence en 15
  minutos) y responde `200` con `{ "data": "Codigo enviado al correo registrado" }`. Si el
  cliente no existe o no tiene correo registrado, responde `400`.
- `verificar-correo`: valida el código contra el que se envió. Si es correcto y no venció, marca
  el cliente como verificado y responde `200`. Si el código es incorrecto o ya venció, responde
  `400` con el mensaje correspondiente (`"Codigo de verificacion invalido"` /
  `"El codigo de verificacion expiro, solicita uno nuevo"`) — en ese caso hay que dejar que el
  usuario pida un código nuevo (`enviar-codigo-verificacion` otra vez).
- Si ya estaba verificado, `verificar-correo` no hace nada y responde `200` igual (idempotente).

**Qué pasa si el cliente NO está verificado:**
- `POST /pedidos/savePedido` responde `400` con mensaje `"Debes verificar tu correo antes de
  generar un pedido"` — no se crea el pedido.
- En venta directa / abono / cancelación de pedido, si se pidió `enviarCorreo: true` en la
  notificación y el cliente no está verificado, el ticket **no se envía** — el response trae
  `correoEnviado: false` y en `erroresEnvio` aparece `"El correo del cliente no esta verificado,
  no se envio el ticket"`. **Excepción:** si en el modal post-venta se escribe un correo manual
  (`notificacion.correo`) para esa notificación puntual, se envía ahí sin exigir verificación —
  ese campo es un envío puntual, no depende de la cuenta del cliente.

**Sugerencia de UX para el front:** tras crear/actualizar el `Cliente` (o al detectar
`correoVerificado: false` en el objeto `Cliente`), mostrar un paso de "verifica tu correo" con un
input de 6 dígitos y botón de reenviar código, antes de dejar avanzar al carrito/pedido.

**Nota operativa:** los clientes que ya existían antes de este cambio quedan con
`correoVerificado = false` por default (no hay migración retroactiva) — van a tener que
verificar su correo la primera vez que intenten generar un pedido, aunque su cuenta sea antigua.

**Archivos tocados en el back:** `Cliente.java` (3 campos nuevos + validaciones), `AuthRequest.java`
(sin campo obligatorio, no cambia), `RegistroRequest.java` (nuevo), `VerificarCorreoRequest.java`
(nuevo), `ClienteServiceImpl.java`, `ClienteControllerImpl.java`, `EmailService.java`,
`PedidoServiceImpl.java`, `VentaServiceImpl.java`, `AbonoServiceImpl.java`, `AuthController.java`.
Migración: `migration_verificacion_correo.sql` (agrega 3 columnas a `clientes`, pendiente de
correr en dev/qa/prod).

### 3. Estado de verificación visible en la búsqueda de clientes

`GET /v1/clientes/buscar` ahora incluye `correoVerificado` en cada elemento de la lista
(`ClienteBusquedaDto`) — útil para que el panel admin muestre un badge de "verificado" / "sin
verificar" junto a cada cliente.

### 4. Endpoint de soporte/pruebas — resetear verificación (solo ADMIN)

```
DELETE /v1/clientes/{id}/verificacion-correo
```

Regresa el cliente a `correoVerificado: false` y borra cualquier código pendiente. Requiere rol
ADMIN (mismo criterio que el resto de `DELETE /v1/clientes/**`). Pensado para soporte/QA — no es
parte del flujo normal del cliente, sirve para poder re-probar la verificación sin tener que
crear una cuenta nueva cada vez.

## Deshabilitar productos/variantes en lote (2026-07-02) — acción requerida en el front

Pensado para ocultar productos o variantes de prueba sin borrarlos: el admin busca (paginado,
usando `admin/filtrar` o la búsqueda normal), selecciona varios de la lista con checkboxes, y
manda un solo request con todos los IDs.

```
PUT /v1/productos/admin/habilitar-lote
PUT /variantes/v1/admin/habilitar-lote
Body: { "ids": [12, 15, 20], "habilitar": false }
```

- `ids`: lista de IDs de producto o de variante (según el endpoint) — no puede venir vacía.
- `habilitar`: `false` para ocultar, `true` para volver a mostrar (mismo endpoint sirve para
  ambas direcciones).
- Requiere rol ADMIN. Responde `200` con `{ "data": "Productos deshabilitados correctamente" }`
  (o el mensaje equivalente para variantes/habilitar). Los IDs que no existan simplemente se
  ignoran (no truena, solo actualiza los que sí encuentra).
- Después de deshabilitar, esos productos/variantes **dejan de aparecer de inmediato** en los
  listados públicos (cliente normal) — la caché se limpia automáticamente. El admin los sigue
  viendo igual en sus búsquedas/filtros (para poder rehabilitarlos después).

### Novedad importante: la variante ahora tiene SU PROPIO campo `habilitado`

Antes una variante solo era visible/oculta según el `habilitado` del producto padre — no había
forma de ocultar una variante suelta (ej. una talla de prueba) dejando visibles las demás del
mismo producto. Ahora `Variantes` tiene su propio campo `habilitado`, independiente del producto:
para que una variante sea visible al cliente normal se necesitan **ambos** en `'1'` (producto
habilitado Y variante habilitada). El campo `habilitado` de la variante ya viene incluido en las
respuestas donde antes venían el resto de sus campos (mismo objeto `Variantes`).

**Archivos tocados en el back:** `Variantes.java` (campo nuevo), `HabilitarLoteRequest.java`
(nuevo, reutilizado en ambos endpoints), `IVarianteRepository.java` (las 5 queries públicas ahora
también exigen `v.habilitado = '1'`), `VarianteServiceImpl.java`, `VarianteController.java`,
`ProductosServiceImpl.java`, `ProductosControllerImpl.java`. Migración:
`migration_habilitado_variantes.sql` (agrega columna a `variantes`, default `'1'` para no afectar
datos existentes — pendiente de correr en dev/qa/prod).

## Restablecer contraseña olvidada (2026-07-03) — acción requerida en el front

Mismo patrón que la verificación de correo: código de 6 dígitos por correo, vence en 15 minutos.
Dos pasos, dos endpoints:

```
POST /v1/auth/olvide-password
Body: { "email": "cliente@correo.com" }

POST /v1/auth/restablecer-password
Body: { "email": "cliente@correo.com", "codigo": "123456", "nuevaPassword": "miNuevaClave" }
```

**Paso 1 — `olvide-password`:** manda el código al correo. **Siempre responde `200`**, exista o
no una cuenta con ese correo — es intencional, para no revelar si un correo está registrado en el
sistema (protección contra enumeración de cuentas). El front debe mostrar el mismo mensaje
("revisa tu correo") sin importar el resultado, no puede usar la respuesta para saber si el
correo existe.

**Paso 2 — `restablecer-password`:** valida el código y, si es correcto y no venció, actualiza la
contraseña. Responde `200` en éxito, `400` con mensaje `"Codigo invalido o expirado"` si el
código está mal, venció, o no hay cuenta con ese correo (mismo mensaje genérico en los 3 casos,
misma razón de seguridad que el paso 1).

**Sobre el flujo de UX que describiste (código primero, campo de nueva contraseña después):** no
hay un endpoint separado para "solo validar el código" — el back valida y cambia la contraseña en
el mismo request. El front puede armar la pantalla en dos pasos visuales (mostrar el campo de
"nueva contraseña" recién cuando el usuario terminó de escribir los 6 dígitos) sin necesidad de
otra llamada al back; si el código resulta incorrecto, el error sale hasta que se manda el
formulario completo (mismo comportamiento que cualquier validación de formulario).

**Nota de seguridad:** esto NO cierra las sesiones activas del usuario — si tenía un access/refresh
token válido en otro dispositivo, sigue funcionando hasta que expire naturalmente (15 min / 7
días). No hay revocación de tokens implementada todavía; avisar si esto es un problema para
retomarlo.

**Archivos tocados en el back:** `Usuario.java` (2 campos nuevos), `IUsuarioRepository.java`,
`OlvidePasswordRequest.java` (nuevo), `RestablecerPasswordRequest.java` (nuevo),
`PasswordResetService.java` (nuevo), `EmailService.java`, `AuthController.java`,
`SecurityConfig.java` (los 2 endpoints nuevos son públicos, como `/login`). Migración:
`migration_reset_password.sql` (agrega 2 columnas a `usuario_modificacion` — pendiente de correr
en dev/qa/prod).

### Cambiar contraseña estando logueado — endpoint distinto, sin código por correo

```
PUT /v1/auth/cambiar-password
Header: Authorization: Bearer {accessToken}
Body: { "passwordActual": "claveVieja", "nuevaPassword": "claveNueva" }
```

Requiere sesión válida (JWT) — no manda `username` ni `email` en el body, el back identifica al
usuario por el token. Pide la contraseña actual en vez de código por correo porque el usuario ya
está autenticado (re-autenticar con la contraseña actual es la protección estándar para que una
sesión abierta/robada no pueda cambiar la contraseña sin más).

- `200` con `"Contrasena actualizada correctamente"`.
- `400` con `"La contrasena actual es incorrecta"` si `passwordActual` no coincide.
- `401` si el token no es válido/expiró (igual que cualquier endpoint protegido).

Va en la pantalla de "mi cuenta"/perfil, no en el login — ese caso sigue siendo
`olvide-password` + `restablecer-password` de la sección anterior.

**Archivos:** `CambiarPasswordRequest.java` (nuevo), `PasswordResetService.java`,
`AuthController.java`. No requiere migración (usa las columnas de `password` que ya existían).

## Unificar verificación de correo Usuario/Cliente (2026-07-03) — acción requerida en el front

> ✅ **Back ya está en QA** (2026-07-04) — merge `dev → qa` hecho y pusheado, migraciones
> `migration_usuario_verificacion_correo.sql` y `migration_datos_completos_cliente.sql` ya
> corridas en `inventario_key_qa`. `correo_verificado` nace en `0` para todos sin excepción (sin
> grandfathering, decisión de diseño — ver migración); `datos_completos` sí hace backfill contra
> los datos reales del cliente. El front puede empezar a integrar esta sección. Diseño completo en
> `PLAN_MEJORAS.md` mejora 15.

### 1. Registro ahora exige verificar el correo antes de poder loguearse

`POST /v1/auth/registrar` no cambia de contrato, pero el `Usuario` que crea queda **sin poder
loguearse** hasta verificar su correo (antes podía loguearse de inmediato).

```
POST /v1/auth/enviar-codigo-verificacion
Body: { "userName": "juanperez" }      // acepta username O correo, cualquiera de los dos

POST /v1/auth/verificar-correo
Body: { "userName": "juanperez", "codigo": "123456" }
```

Mismo patrón que ya conocen de la verificación de `Cliente` (vencimiento 15 minutos, código de 6
dígitos). Ambos responden `200` con texto plano en éxito, `400` con el mensaje de error en texto
plano si falla (`"Usuario no encontrado"`, `"El correo ya esta verificado"`,
`"Codigo de verificacion invalido"`, `"El codigo de verificacion expiro, solicita uno nuevo"`).
`enviar-codigo-verificacion` también puede responder `429` si se pide demasiadas veces seguidas
(rate-limit propio, independiente del de login/registro).

**Flujo front sugerido:** justo después de `POST /v1/auth/registrar`, llamar
`enviar-codigo-verificacion` automáticamente y mostrar la pantalla de "ingresa el código de 6
dígitos", con botón de reenviar. Recién cuando `verificar-correo` responde `200`, mandar al login
normal (`POST /v1/auth/login`).

### 2. `POST /v1/auth/login` ahora puede rechazar por correo sin verificar

Nueva respuesta posible, además de las que ya existían:

- **`403`** con body `"Debes verificar tu correo antes de iniciar sesión"` — el `Usuario` existe,
  la contraseña es correcta, pero `correoVerificado` sigue en `false`. El front debe mandar a la
  pantalla de "ingresa el código" (mismos 2 endpoints del punto 1) en vez de mostrar un error
  genérico de credenciales.
- `401` (credenciales inválidas) y `429` (rate-limit) siguen igual que antes, sin cambios.

**Excepción — rol ADMIN:** los usuarios con rol `ROLE_ADMIN` **no** requieren correo verificado
para hacer login, sin importar el valor de `correoVerificado` en BD. El chequeo de verificación se
salta por completo para ese rol y nunca reciben este `403`. El front no necesita ninguna lógica
especial para esto: simplemente el admin nunca va a recibir el `403` de arriba, entra normal con
`200` aunque nunca haya pasado por la pantalla de verificación.

**Usuarios que ya existían antes de este cambio (no admin):** todos quedan con
`correoVerificado = false` por default (sin excepción para roles no-admin, no hay "pase
automático") — al primer intento de login después de que esto se despliegue, van a recibir el
mismo `403` de arriba y tendrán que verificar su correo por primera vez, aunque su cuenta sea
antigua. Sesiones ya activas (con un access/refresh token válido) NO se ven afectadas — solo un
login nuevo dispara esta validación.

**Flujo exacto que debe implementar el front (no hay endpoint de "revisar si está verificado antes"
— todo se resuelve con la respuesta del propio `login`):**

```
1. Usuario escribe userName + password → una sola petición:
   POST /v1/auth/login  Body: { "userName": "...", "password": "..." }

2. Reaccionar según el código de esa misma respuesta:
   - 200                                          → guardar accessToken/refreshToken, entrar
                                                     al sistema normal (dashboard/productos/
                                                     variantes). Sin cambios.
   - 401 (credenciales inválidas)                 → error de siempre. Sin cambios.
   - 429 (rate-limit)                             → mensaje de siempre. Sin cambios.
   - 403 "Debes verificar tu correo antes de
     iniciar sesión"                              → NUEVO. No mostrar error genérico, no
                                                     guardar token, no entrar al sistema.
                                                     Ir al paso 3.

3. Si vino ese 403 puntual:
   a) Navegar a la pantalla de código (la misma de F-19 usada en registro).
   b) Disparar automático: POST /v1/auth/enviar-codigo-verificacion { "userName": "..." }
   c) Usuario escribe el código de 6 dígitos.
   d) POST /v1/auth/verificar-correo { "userName": "...", "codigo": "..." }
        - 400 → mostrar error, permitir reintentar o reenviar código.
        - 200 → correo verificado, pero AÚN NO hay sesión iniciada (este endpoint no
                 devuelve tokens).
   e) Volver a llamar POST /v1/auth/login con el mismo userName/password.
        - Ahora responde 200 → recién aquí se entra al sistema.
```

**Importante:** distinguir este `403` puntual (por el texto del mensaje o un código de error
propio, si el back lo agrega) de cualquier otro `403` genérico que la app ya use para "no
autorizado" — no deben compartir el mismo manejador en el front.

---

### [BUG-KEY-11] ✅ Fix: contraseña incorrecta ya no se confundía con "correo sin verificar"
**Fecha:** 2026-07-04 | **Archivos:** `Usuario.java`, `AuthController.java`

**Antes (incorrecto):** Spring Security evalúa `isEnabled()` **antes** de comparar la contraseña.
Como `isEnabled()` dependía de `correoVerificado`, un usuario sin verificar recibía el `403`
"Debes verificar tu correo..." **sin importar si la contraseña era correcta o incorrecta** — la
contraseña nunca llegaba a compararse. Esto rompía el caso de contraseña mal escrita: en vez de
`401 "Credenciales inválidas"` salía el `403` de verificación, dando información confusa/errónea
al usuario.

**Después (correcto):** `isEnabled()` ya no depende de `correoVerificado` (vuelve a depender solo
del flag `enabled`, como antes de mejora 15). El chequeo de correo verificado se hace aparte, en
`AuthController.login()`, **después** de que `authManager.authenticate()` ya confirmó la
contraseña. Orden real ahora: 1) usuario existe, 2) contraseña correcta → si no, `401` sin
excepción, 3) correo verificado o rol ADMIN → si no, `403`.

**El front no necesita cambiar nada de lo ya documentado arriba** — mismos endpoints, mismos
códigos de respuesta. Solo que ahora `401` y `403` salen en el caso correcto cada uno.

---

### 3. Al verificar, se auto-crea el `Cliente` — nuevo campo `datosCompletos`

Cuando `verificar-correo` (punto 1) tiene éxito por primera vez, el back crea automáticamente un
`Cliente` vinculado a ese `Usuario`, con el correo ya copiado y verificado, pero **sin nombre,
apellidos ni teléfono todavía** — nuevo campo `Cliente.datosCompletos: false`.

**`POST /pedidos/savePedido` ahora valida dos cosas por separado, con mensajes distintos:**
- `400` `"Debes verificar tu correo antes de generar un pedido"` — ya existía (mejora 12), sigue
  igual.
- `400` `"Debes completar tus datos (nombre, apellido paterno, telefono) antes de generar un
  pedido"` — **nuevo**. El front debe distinguir este mensaje del anterior para saber si mandar a
  la pantalla de "verifica tu correo" o a la de "completa tu perfil" (nombre, apellido paterno,
  teléfono — el correo ya viene prellenado, no hace falta volver a pedirlo ni verificarlo aquí).

Se guarda con el mismo endpoint de siempre: `POST /v1/clientes/save` /
`PUT /v1/clientes/update/{id}`.

**Apellido materno ahora es opcional** (antes obligatorio, mejora 12) — si el formulario del front
tenía `Validators.required` en ese campo, hay que quitarlo.

### 4. Cambiar el correo de un cliente ya no se aplica de inmediato

Al actualizar un `Cliente` (`POST/PUT /v1/clientes/...`) con un `correoElectronico` distinto al
que ya tenía guardado:

- Los demás campos del formulario (nombre, apellidos, teléfono, direcciones) se guardan siempre,
  sin condición.
- El correo **no cambia todavía** — el objeto `Cliente` que devuelve el response sigue trayendo el
  correo **anterior** (el ya verificado), no el que se acaba de escribir.
- El back dispara automáticamente el envío de un código de verificación al correo nuevo (mismo
  mecanismo de siempre: `POST /v1/clientes/{id}/enviar-codigo-verificacion` ya se llama solo, el
  front no necesita invocarlo aparte en este caso).
- El front debe comparar el `correoElectronico` que mandó vs. el que regresó el response: si son
  distintos, mostrar un aviso tipo *"Guardamos tus datos. Te enviamos un código a tu correo nuevo
  para confirmarlo — mientras no lo confirmes, seguirás recibiendo notificaciones en tu correo
  anterior."* y ofrecer el input de 6 dígitos (`POST /v1/clientes/{id}/verificar-correo`, ya
  existente). Si el cliente nunca verifica, no pasa nada malo — simplemente el correo anterior
  sigue siendo el vigente indefinidamente.
- **Excepción — un ADMIN editando el cliente desde el panel:** el correo se aplica directo, sin
  disparar nada de esto. Se distingue por el rol de la sesión que hace el request, no por ningún
  campo del body — el front del panel admin no necesita hacer nada especial aquí, ya funciona así
  automáticamente.

### 5. Nada nuevo para soporte — ya funcionaba

El caso de "el cliente no puede verificar su correo solo, un admin lo ayuda por teléfono" **no
requirió cambios** — `POST /v1/clientes/{id}/enviar-codigo-verificacion` y
`POST /v1/clientes/{id}/verificar-correo` ya eran accesibles por cualquier usuario autenticado
(incluido ADMIN) para cualquier `clienteId`, no solo el dueño de la cuenta. Si el front quiere una
pantalla de soporte en el panel admin (buscar cliente → botón reenviar código → input para
capturar el código que el cliente dicte), puede armarla ya con estos 2 endpoints existentes.

**Archivos tocados en el back:** `Usuario.java` (3 campos nuevos), `Cliente.java` (`datosCompletos`,
`correoPendiente`, apellido materno ya no obligatorio), `UsuarioVerificacionService.java` (nuevo),
`EnviarCodigoVerificacionUsuarioRequest.java` / `VerificarCorreoUsuarioRequest.java` (nuevos),
`ClienteServiceImpl.java`, `ClienteControllerImpl.java`, `AuthController.java`,
`SecurityConfig.java`, `PedidoServiceImpl.java`. Migraciones:
`migration_usuario_verificacion_correo.sql` y `migration_datos_completos_cliente.sql` — **ya
corridas en QA (2026-07-04)**.

---

## [SEC-KEY-01] ✅ Fix: control de acceso — un usuario ya no podía ver/editar datos de otro (2026-07-04)

**Hallazgo:** `POST /v1/clientes/save`, `PUT /v1/clientes/update/{id}` y
`GET /v1/clientes/buscarPorIdCliente/{id}` solo exigían estar autenticado, sin verificar que el
`id`/`usuario.id` del request correspondiera al usuario dueño de la sesión. Cualquier cliente
logueado podía leer o sobreescribir los datos de **otro** cliente con solo mandar su `id`. Lo
mismo pasaba con `/v1/usuarios/**` (gestión de cuentas/roles/permisos): solo pedía estar
autenticado, no ser ADMIN — un usuario cualquiera podía, por ejemplo, asignarse el rol `ADMIN` vía
`PUT /v1/usuarios/{usuarioId}/rol/{rolId}`.

**Fix aplicado:**
- `/v1/usuarios/**` (excepto `buscarClientePorIdUsuario`, que ya era público) ahora requiere
  `hasRole("ADMIN")` en `SecurityConfig` — toda esa gestión es exclusiva de admin, no había caso
  de autoservicio legítimo.
- `GET /v1/clientes/buscar` (búsqueda por nombre, expone correo/teléfono) ahora también requiere
  `hasRole("ADMIN")` — antes cualquier cliente autenticado podía buscar los datos de otros.
- `POST /v1/clientes/save`, `PUT /v1/clientes/update/{id}` y
  `GET /v1/clientes/buscarPorIdCliente/{id}` ahora comparan el usuario del JWT contra el
  `usuario.id`/`idCliente` de la petición — si no coincide y quien llama no es ADMIN, responden
  `403`. Un ADMIN sigue pudiendo operar sobre cualquier cliente (panel admin no se ve afectado).
- `PUT /v1/clientes/update/{id}` antes ignoraba el `{id}` de la URL y hacía un guardado crudo sin
  pasar por la lógica de correo pendiente/mejora 15 — ahora reutiliza exactamente la misma lógica
  que `save()`, así que ambos se comportan igual.

**El front no necesita cambiar nada si ya mandaba el `usuario.id`/`idCliente` correctos (el
propio, no el de otro)** — solo verá un `403` nuevo si por error intentaba operar sobre un id que
no le pertenece, cosa que antes se permitía silenciosamente.

**Acción específica del front para `/v1/usuarios/**` y `GET /v1/clientes/buscar`:** antes
funcionaban para cualquier usuario logueado; ahora dan `403` si quien llama no es ADMIN. Si alguna
pantalla que NO es del panel admin (ej. "mi perfil" de un cliente normal) llegaba a llamar alguno
de estos endpoints, hay que quitarle esa llamada — no van a volver a funcionar para no-admins. El
panel admin no se ve afectado (siempre llama estos endpoints ya logueado como ADMIN).

**Archivos:** `SecurityConfig.java`, `ClienteControllerImpl.java`, `AuthenticationUtils.java`
(nuevo método `currentUsuario()`). No requiere migración.

---

## Reseteo de contraseña por ADMIN — contraseña temporal fija (2026-07-04)

Pensado para cuando un usuario olvida su contraseña y el correo que registró es falso/no revisa
(el flujo normal de `olvide-password` no le sirve porque nunca va a recibir el código). El admin
lo resetea a una contraseña generada al azar y se la pasa al usuario por el medio que sea
(teléfono, en persona, etc.).

```
PUT /v1/usuarios/{id}/resetear-password
```

- Requiere rol ADMIN (cae dentro de `/v1/usuarios/**`, ver `SEC-KEY-01` arriba).
- No lleva body — solo el `id` del usuario (el mismo que usarías para `updateUsuario/{id}`).
- Genera una contraseña aleatoria de 8 caracteres (letras mayúsculas/minúsculas + dígitos, sin
  `0/O/1/l/I` para no confundir al dictarla), se la asigna al usuario y marca internamente
  `passwordTemporal = true`.

> **[BUG-KEY-12] ✅ Fix (2026-07-04):** al probar este endpoint, el response llegaba vacío
> `{ "mensaje": null, "code": 0, "data": null, "lista": null }` a pesar de responder `200`. Causa:
> el constructor de 2 argumentos de `ResponseGeneric` (`ResponseGeneric.java`) solo llenaba los
> campos cuando `data` era `null` — el caso de éxito (con datos reales) nunca los asignaba. Era un
> bug preexistente en una clase muy usada en todo el back; nadie lo había notado porque hasta hoy
> todos los demás usos de ese constructor pasaban `null` a propósito (casos de error). Ya
> corregido — el `data`/`mensaje`/`code` ahora sí llegan bien en la respuesta de este endpoint (y
> de cualquier otro que use ese mismo constructor con datos reales en el futuro).
- Responde `200` con `{ "data": "aB3dEfG9", "mensaje": "Contrasena reseteada. Comparte esta
  contrasena con el usuario; debera cambiarla en su siguiente login." }` — **el front debe
  mostrarle esa contraseña (`data`) al admin en pantalla** para que se la pueda dar al usuario;
  el back no la vuelve a mostrar después, solo queda el hash.

**Cambio en el login — nuevo campo `debeCambiarPassword`:**

`POST /v1/auth/login` ahora devuelve, además de `accessToken`:

```json
{ "accessToken": "...", "debeCambiarPassword": true }
```

- `true` solo si la contraseña actual fue puesta por un reseteo de admin y el usuario **todavía
  no la ha cambiado**. En cualquier otro caso viene `false`.
- **El front debe revisar este flag después de un login exitoso** (200): si viene `true`, no
  dejar navegar al sistema normal — forzar la pantalla de "cambia tu contraseña" (reusar
  `PUT /v1/auth/cambiar-password`, ya documentado arriba, pidiendo como "actual" la contraseña
  temporal que el admin le dio, y la nueva que el usuario elija).
- En cuanto el usuario cambia su contraseña con éxito (por `cambiar-password` o por
  `restablecer-password` del flujo de "olvidé mi contraseña"), el flag se limpia solo — el
  próximo login ya viene con `debeCambiarPassword: false`.

**Archivos:** `Usuario.java` (`passwordTemporal`), `UsuarioServiceImpl.java`
(`resetearPasswordAleatoria`), `UsuarioController.java`, `AuthResponse.java`, `AuthController.java`,
`PasswordResetService.java`. Migración: `migration_password_temporal.sql` — **pendiente de correr
en dev/qa/prod**.

### Verificar el correo de un Usuario desde el panel de admin

No es un endpoint nuevo — la pantalla de detalle/edición de un `Usuario` en el panel puede usar
los mismos 2 endpoints ya documentados arriba (sección "Unificar verificación de correo
Usuario/Cliente", punto 1):

```
POST /v1/auth/enviar-codigo-verificacion   Body: { "userName": "..." }
POST /v1/auth/verificar-correo             Body: { "userName": "...", "codigo": "..." }
```

Son públicos (cualquiera los puede llamar, no piden rol) porque un usuario recién registrado
todavía no tiene sesión cuando los usa por primera vez — así que el panel admin también puede
dispararlos para cualquier `userName`, sin restricción adicional. Flujo sugerido en el panel: botón
"Reenviar código de verificación" → dispara `enviar-codigo-verificacion` → input para que el admin
capture el código que el usuario le dicte por teléfono → `verificar-correo`.

### Si el admin edita el correo de un Usuario (no Cliente), se aplica directo

`PUT /v1/usuarios/updateUsuario/{id}` (ahora solo ADMIN, ver `SEC-KEY-01`) ya aplicaba — y sigue
aplicando — el correo nuevo de inmediato, sin pedir verificación ni dejar nada pendiente. Mismo
criterio que ya existe para `Cliente` cuando lo edita un ADMIN (mejora 15, punto 4): se confía en
el admin, no hay paso intermedio. No fue necesario cambiar código para esto, ya funcionaba así.

---

## ✅ Promociones por variante / combos (2026-07-05+) — implementado en dev/qa/main, pruebas en curso

> **Implementado en el backend y en producción (`main`).** Migración `migration_promociones.sql` ya
> corrida en dev/qa/main. Pruebas end-to-end en curso (filtro, listar promos, compra con combo).
> Diseño completo en `PROMOCIONES.md` en la raíz del repo. El front consume los endpoints ya desde
> hace varias semanas, pero puede haber campos/comportamientos nuevos que se agregaron después de
> la primera integración — revisar esta sección completa, especialmente el punto sobre
> `codigoBarras` (2026-07-13).

**Qué es:** un combo de 1 o más variantes ya existentes (pueden ser productos distintos entre sí)
que se venden juntas con precio rebajado por pieza. Cada pieza conserva su propio precio de oferta
(no hay precio único de paquete) — así que en pedidos/ventas cada pieza viaja como una línea normal,
solo con un campo nuevo `promocionId` para agruparlas.

**Endpoints planeados:**
- `POST /v1/promociones` (ADMIN) — crear
- `PUT /v1/promociones/{id}` (ADMIN) — editar (reemplaza detalles completos)
- `PUT /v1/promociones/{id}/activo` (ADMIN) — activar/desactivar
- `GET /v1/promociones/admin?pagina=&size=` (ADMIN) — listado completo, incluye vencidas/inactivas
- `GET /v1/promociones/activas?pagina=&size=` (cualquier usuario logueado) — catálogo, trae
  `instanciasDisponibles` ya calculado y el desglose de piezas (variante, talla, color, precio
  normal vs promo, imagen)

> **No existe endpoint DELETE para promociones.** "Eliminar" una promo desde el panel admin es
> llamar `PUT /v1/promociones/{id}/activo` con `{ "activo": false }` — la promoción no se borra,
> se apaga: deja de salir en `/v1/promociones/activas` pero sigue existiendo (con su historial)
> en `/v1/promociones/admin`. Si el front pone un botón de "eliminar" en la lista de admin, debe
> llamar a este endpoint, no esperar un DELETE que no existe.

**Cambios que vendrán en endpoints existentes:**
- `POST /pedidos/savePedido` y venta directa: cada detalle gana campo opcional `promocionId`.
- `GET /pedidos/findPedido/{id}`: cada línea del detalle gana `promocionId` +
  `promocionDescripcion` (null en líneas normales) para que el front agrupe el combo visualmente.
- Ticket/comprobante: se agrupa por `promocionId` igual que el detalle de pedido.

**Regla de negocio clave para el checkout:** si el carrito trae al menos una promoción, **todo el
pedido se fuerza a pago de contado** — el front debe ocultar/deshabilitar "Apartar" y "Fiado" para
el pedido completo (no solo la promo) y mostrar aviso. El back rechazará con `400` si de todos
modos llega un pedido con promoción y `tipoPedido` distinto de `NORMAL`.

Ver `PROMOCIONES.md` para los JSON de request/response completos de cada endpoint y el flujo UX
sugerido (catálogo, detalle de la promo, carrito, countdown de vencimiento calculado en el front).

**Novedad (2026-07-13): campo `codigoBarras` en `GET /v1/promociones/activas` (solo ADMIN)**

El endpoint `GET /v1/promociones/activas?pagina=&size=` ahora incluye en cada detalle del combo
un campo opcional `codigoBarras` (null para clientes, poblado solo si el solicitante es ADMIN).
Se agregó para que el admin pueda distinguir variantes "hermanas" de un mismo producto al armar
las promociones (ej. "Jean Slim M" vs "Jean Slim L" — mismo nombre de producto, distinto código
y talla). El cliente normal nunca ve este campo.

```json
{
  "instanciasDisponibles": 15,
  "variante": { ... },
  "detalles": [
    {
      "varianteId": 245,
      "productoNombre": "Jean Slim",
      "talla": "M",
      "color": "Azul",
      "cantidad": 1,
      "precioNormal": 250.00,
      "precioEnPromocion": 220.00,
      "imagenUrl": "...",
      "codigoBarras": "JEAN-SLIM-M-AZUL"  // ← nuevo, solo para ADMIN
    }
  ]
}
```

**Cambio en el front — Gestión de Promociones (2026-07-15): buscador de variantes**

La pantalla `gestion-promociones.component.ts` cambió el endpoint para buscar variantes al armar
un combo:
- **Antes:** `GET /variantes/v1/buscar?termino=...` (público, con cascada en el back)
- **Ahora:** `GET /variantes/v1/admin/filtrar?nombreOCodigo=...&conStock=true` (admin, OR en un
  solo query)

**Por qué:** el endpoint admin combina búsqueda de texto (nombre/código) con filtro de stock en
un solo AND, en vez de la cascada vieja del buscador público que podía ocultar resultados. Además,
para promociones **queremos solo variantes con stock** (de lo contrario una promo se quedaría
inviable apenas se agote una de sus piezas). El filtro `conStock=true` asegura eso.

**No es un cambio de contrato** — el response sigue siendo la misma lista paginada de variantes.
Es solo dónde y cómo el front las pide.

---

## [SEC-KEY-02] ✅ Fix: precio de línea ahora se valida contra catálogo (2026-07-05)

**Antes:** `POST /pedidos/savePedido` y la venta directa (`VentaDirectaRequest`) aceptaban el
`precioUnitario`/`precioVenta` y `subTotal` de cada línea tal cual los mandara el request, sin
comparar contra nada — solo se validaba stock. Cualquier usuario autenticado (no solo ADMIN, ya
que `savePedido` está abierto a `authenticated()`) podía editar el request antes de enviarlo
(DevTools, Postman, etc.) y pagar el precio que quisiera por un producto normal.

**Después:** en una línea **sin** `promocionId`, el back ahora exige que `precioUnitario`
(`precioVenta` en venta directa) coincida con el precio de catálogo actual del producto
(`Producto.precioVenta`), y que `subTotal` sea `precioUnitario * cantidad` (tolerancia de 1
centavo por redondeo). Si no coincide, responde `400` con `"El precio de {nombre} no es valido"` o
`"El subtotal de {nombre} no es valido"` y no crea el pedido/venta.

**Qué debe hacer el front:** nada nuevo si ya arma el carrito con el precio que el back le dio en
el listado del producto/variante (`GET /variantes/buscar`, etc.) — ese sigue siendo el precio
válido. El único caso que ahora falla es si el carrito quedó con un precio **desactualizado**
(ej. el admin cambió el precio del producto mientras el cliente tenía el carrito abierto desde
hace rato) — en ese caso el front debe mostrar el error del `400` y sugerir refrescar el carrito
antes de reintentar, en vez de reintentar con el mismo precio viejo.

**Las líneas con `promocionId` no cambian:** su precio rebajado sigue siendo válido — se valida
aparte contra `promocion_detalle` (ver sección de Promociones arriba), no contra el precio de
catálogo.

**Archivos:** `PedidoServiceImpl.java` (`validarPrecioCatalogo`), `VentaServiceImpl.java`
(`validarPrecioCatalogo`).

---

## ⚠️ Revisar en el FRONT — segunda llamada a `/v1/promociones/admin` se queda colgada indefinidamente (2026-07-05)

**Síntoma reportado:** al cargar el panel de admin de promociones, salen (casi) dos llamadas
seguidas a `GET /v1/promociones/admin?pagina=1&size=10`. La primera termina bien (200, con los
datos). La segunda se queda "cargando" **para siempre** (varios minutos, nunca resuelve ni falla).

**Ya se descartó que sea el backend.** Se probó el endpoint directo (fuera del front) tres veces
seguidas y respondió en menos de 1.1s cada vez, sin colgarse. Además, si fuera un bloqueo de MySQL
(por ejemplo dejado por las `ALTER TABLE` de la migración de promociones), el pool de conexiones
(Hikari, `connection-timeout: 20000`) habría fallado con error a los ~20-25 segundos — no se
quedaría colgado de forma indefinida. Un hang indefinido (no un timeout) apunta a algo del lado
del cliente/Angular, no de la base de datos ni del servidor.

**Qué debe revisar el front — sospecha concreta: el interceptor de refresh de token.**
Ya hubo un bug ahí antes (ver ticket del bug de `response.response.accessToken` documentado
arriba, sección JWT). El patrón que explica exactamente este síntoma:

1. Salen 2 requests casi al mismo tiempo hacia un endpoint protegido.
2. Alguno de los dos (o ambos) dispara el flujo de refresh de token en el interceptor HTTP.
3. El interceptor debe hacer que **todas** las requests que estaban esperando ese refresh se
   reanuden cuando el token nuevo esté listo — típicamente compartiendo un
   `BehaviorSubject<string | null>` (o similar) donde las requests en espera hacen algo como
   `filter(token => token !== null)` sobre ese subject.
4. **Si en cambio se usa un `Subject` (no `BehaviorSubject`), o solo se resuelve una promesa/
   observable de un solo uso, o solo la request que "ganó la carrera" y disparó el refresh
   recibe la notificación** — la segunda request se queda suscrita a algo que ya emitió y nunca
   vuelve a emitir, o a algo que nunca la tiene en cuenta. Se queda esperando para siempre.

**Qué pedirle al desarrollador del front que verifique puntualmente:**
- Ubicar el interceptor HTTP que maneja 401 / refresh de token.
- Confirmar cómo maneja **llamadas concurrentes** que necesitan el mismo refresh: ¿usa un
  `BehaviorSubject` (o equivalente) que emite el token nuevo a **todos** los suscriptores en
  espera, o solo resuelve para la request que originó el refresh?
- Reproducir disparando 2 llamadas al mismo endpoint protegido casi al mismo tiempo (ej. desde la
  consola o abriendo la pantalla de promociones admin) y confirmar si el bug ocurre solo cuando
  hay una condición de carrera en el refresh, o también sin refresh de por medio (en ese caso la
  causa sería otra, ej. una duplicación de la llamada en el propio componente/servicio Angular que
  vale la pena revisar aparte — dos suscripciones al mismo observable sin compartir, un resolver +
  un `ngOnInit` llamando dos veces, etc.).

**No es un cambio de contrato de API** — no hay nada nuevo que el front tenga que mandar o
interpretar distinto en la respuesta; es una investigación de un bug de concurrencia en el cliente.

> ✅ **Resuelto en el front (2026-07-06).** Confirmado por el equipo de front — era el interceptor
> de refresh de token, como se sospechaba arriba. Cerrado, no requiere nada más del backend.

---

## ⚠️ Diagnóstico temporal en `PUT /variantes/v1/admin/habilitar-lote` (2026-07-06)

**Bug reportado:** al deshabilitar/habilitar variantes en lote, el endpoint responde 200 con el
mensaje de éxito, pero en la base de datos las variantes no cambian de estado. Sospecha: el
`findAllById(ids)` del backend ignora en silencio los ids que no existan como `Variantes.id` — si
el front está mandando ids equivocados (por ejemplo `producto.id` en vez de `variante.id`), el
endpoint "tiene éxito" sin actualizar nada, porque no hay ninguna variante real que coincida.

**Actualización 2026-07-06 (misma sesión):** con datos reales de QA (ids `2, 3, 4`) los 3 salieron
`encontradoEnBD: true` — sí existen como `Variantes.id`, así que se descarta el mismatch de ids.
Se agregó una segunda verificación: tras el `saveAll`, el backend hace `flush()` +
`entityManager.clear()` y vuelve a leer esas mismas variantes directo de la BD (sin caché de
Hibernate de por medio) para confirmar si el `UPDATE` realmente se aplicó, dentro de la misma
transacción.

**Cambio (temporal, solo para diagnosticar — no es el fix final):** el campo `data` de la
respuesta, que antes era solo el texto `"Variantes deshabilitadas correctamente"` /
`"Variantes habilitadas correctamente"`, ahora viene con un diagnóstico concatenado:

```json
{
  "mensaje": "La peticion fue exitosa",
  "code": 200,
  "data": "Variantes deshabilitadas correctamente. {\"idsEnviados\":[2, 3, 4],\"resultado\":[{\"id\":2,\"encontradoEnBD\":true,\"habilitadoTrasGuardar\":\"0\"},{\"id\":3,\"encontradoEnBD\":true,\"habilitadoTrasGuardar\":\"0\"},{\"id\":4,\"encontradoEnBD\":true,\"habilitadoTrasGuardar\":\"0\"}]}",
  "lista": null
}
```

- `idsEnviados`: los ids tal cual los mandó el front en el `request.ids`.
- `resultado`: por cada id, si existe (`encontradoEnBD: true`) o no (`false`) como `Variantes.id`
  real en la base, y `habilitadoTrasGuardar`: el valor de la columna `habilitado` releído
  directamente de la BD después de guardar (`"1"` = habilitado, `"0"` = deshabilitado).
- Si `habilitadoTrasGuardar` ya sale correcto (`"0"` al deshabilitar) pero al consultar la tabla
  con otra herramienta (DBeaver, consola MySQL, etc.) todavía se ve `"1"`, el problema no es del
  backend — es una lectura obsoleta de esa herramienta (transacción/conexión abierta desde antes
  con aislamiento `REPEATABLE READ`, o apuntando a un host/réplica distinto). Hay que cerrar y
  reabrir la conexión de esa herramienta antes de volver a consultar.
- También se loguea del lado del servidor (`log.info`) el mismo diagnóstico.

**⚠️ Si el front hace algo con ese string además de mostrarlo tal cual** (comparación exacta contra
`"Variantes deshabilitadas correctamente"`, parseo, etc.), va a dejar de matchear porque ahora trae
texto extra al final. Si solo se muestra el mensaje en un toast/snackbar sin comparar el contenido,
no requiere ningún cambio del front — solo van a ver un texto más largo temporalmente.

**Pendiente:** con este diagnóstico en logs/respuesta, confirmar si los ids que manda el front para
esta pantalla (`variantes/v1/admin/habilitar-lote`) realmente corresponden a `variante.id` o si por
error de la pantalla se están mandando otros ids (ej. `producto.id`). Una vez confirmada la causa,
se quita este diagnóstico y se aplica el fix definitivo (que puede ser en front, si el bug es que
se arma mal el arreglo de ids antes de llamar al endpoint).

---

## ✅ RESUELTO (2026-07-07): diagnóstico temporal quitado de `habilitar-lote`

Como ya se confirmó (sección de arriba, "Causa real encontrada") que el `UPDATE` en BD siempre
funcionó bien, se quitó el JSON de diagnóstico del campo `data`. El endpoint vuelve al mensaje
limpio de siempre:

```json
{ "mensaje": "La peticion fue exitosa", "code": 200, "data": "Variantes deshabilitadas correctamente.", "lista": null }
```

Mismo para `"Variantes habilitadas correctamente."`. El diagnóstico (ids/resultado) sigue
generándose pero solo va al log del servidor (`log.debug`), ya no viaja en la respuesta HTTP. Si el
front había agregado algún manejo temporal para el texto largo con JSON embebido, ya se puede
quitar — el `data` vuelve a ser el string corto de antes.

---

## ✅ Causa real encontrada y arreglada (2026-07-06): variantes SÍ se deshabilitaban, pero nunca se veía

Con el diagnóstico de arriba se confirmó en QA que `habilitar-lote` **sí actualiza la BD**
correctamente (`habilitadoTrasGuardar` salía con el valor correcto). El problema real era otro: los
endpoints de búsqueda/listado de variantes para admin (`GET /variantes/v1/buscar`,
`GET /variantes/v1/porProducto/{productoId}`, el filtro admin, "sin stock deshabilitadas", etc.)
**nunca incluían el campo `habilitado` en su respuesta** — a diferencia de productos, donde ese
campo sí viaja. Por eso, aunque la variante ya estaba deshabilitada en la BD, cualquier pantalla
que la buscara/listara no tenía forma de saberlo y seguía mostrándola como habilitada.

**Cambio de contrato — nuevo campo `habilitado` (char, `'1'`/`'0'`) agregado a:**
- El objeto de cada variante en `GET /variantes/v1/buscar` (búsqueda por nombre/código/palabra
  clave, resumen paginado) — clase `VarianteResumenDto`.
- El objeto de cada variante en `GET /variantes/v1/porProducto/{productoId}` (listado simple, no
  paginado) — clase `VarianteDto`.

Mismo formato que ya usa `Producto.habilitado`: `'1'` = habilitada, `'0'` = deshabilitada. El front
debe empezar a leer este campo en esas pantallas para reflejar correctamente el estado, igual que
ya lo hace con productos.

**Aún pendiente de correr en producción** — este fix (junto con el diagnóstico de arriba) solo
está en `dev`/`qa` por ahora; falta subir a `main` cuando se confirme que todo funciona bien en QA.

---

## ✅ Fix (2026-07-06): búsqueda de cliente por nombre completo no encontraba resultados

**Bug:** `GET /clientes/buscar?nombre=...` buscaba el texto contra `nombrePersona`,
`apeidoPaterno` y `apeidoMaterno` **por separado** (OR). Si buscabas solo "Abel" sí encontraba al
cliente (matchea `nombrePersona`), pero si buscabas "Abel Tiburcio" (nombre + apellido juntos) no
encontraba nada, porque ningún campo individual contiene esa cadena completa.

**Fix:** la query ahora concatena `nombrePersona + apeidoPaterno + apeidoMaterno` y busca el texto
contra el nombre completo. Sigue funcionando buscar por una sola palabra (nombre solo, o apellido
solo) y ahora también funciona buscar "nombre apellido" junto, en ese orden. **No cambia el
contrato** (mismo endpoint, mismo request/response) — solo corrige los resultados.

---

## ⚠️ Cambio de comportamiento (2026-07-06): errores de validación ya NO regresan 500

**Contexto:** al guardar una venta directa con una promoción
(`POST /v1/ventas/save`, líneas con `promocionId`), el front reportó `{"code":500,"data":null,
"mensaje":"Error interno del servidor"}` — sin ninguna pista de qué estaba mal. La causa inmediata
era que el request mandaba `"cantidad": null` en las líneas de la promo (el backend no validaba
eso y tronaba con un error interno al hacer una comparación numérica). **El front debe mandar
`cantidad` con el número real de piezas en cada línea de detalle**, incluidas las de promoción
(no puede ir `null`).

**Pero el hallazgo más importante fue de fondo:** el backend tiene decenas de validaciones de
negocio (stock insuficiente, precio inválido, promoción vencida o no disponible, "las promociones
solo se pueden comprar de contado", etc.) que se lanzan internamente como una excepción genérica.
El manejador global de errores no tenía un caso para ese tipo de excepción, así que **todas esas
validaciones terminaban devolviendo `code: 500` con el mensaje genérico `"Error interno del
servidor"`**, ocultando el mensaje real (p. ej. "Stock insuficiente en variante id 5. Disponible:
2, solicitado: 10").

**Fix:** ahora esas validaciones de negocio devuelven `code: 400` con el mensaje real y específico
en `mensaje`/`data`, igual que ya pasaba con otras validaciones (`404`, `409`, `422`, etc.).

**Lo que el front necesita revisar:**
- Si en algún lado el front distingue `500` vs `400` para decidir qué mostrarle al usuario (p. ej.
  "algo salió mal, intenta de nuevo" para 500 vs. mostrar el mensaje tal cual para 400), muchos
  errores que antes caían en la rama de "500 genérico" ahora van a caer en la rama de "400 con
  mensaje específico" — en general esto es una mejora (mensajes más útiles), pero si hay lógica
  específica atada al código 500 en particular, revisarla.
- Ya se puede mostrar directamente el mensaje de `data`/`mensaje` en la mayoría de los errores de
  venta/pedido/promoción — antes esa información no llegaba nunca.
- Además se agregó validación explícita de `cantidad` (obligatoria y mayor a 0) en
  `POST /v1/ventas/save` y `POST /pedidos/savePedido` — si falta o es 0/negativa, ahora regresa
  400 con `"La cantidad es obligatoria y debe ser mayor a 0..."` en vez de tronar.

**Aún pendiente de correr en producción** — igual que los cambios anteriores, esto solo está en
`dev`/`qa` por ahora.

---

## ✅ Cambio de contrato (2026-07-06, front actualizado 2026-07-07): filtro admin combinado de productos/variantes + fix paginación por defecto

**1. `GET /productos/*` sin página/tamaño por defecto (bug, ya corregido).** Varios endpoints de
`ProductosControllerImpl` (`obtenerProductos`, `buscarNombreOrCodigoBarra`, `admin/no-habilitados`,
`admin/sin-stock`, `admin/filtrar`) exigían `size`/`page` como obligatorios — si el front entraba a
un componente sin mandarlos, el backend rechazaba la petición en vez de asumir página 1 / 10
registros (a diferencia de `VarianteController`, que sí tenía default). Ahora todos tienen
`page` por defecto `1` y `size` por defecto `10`, igual que variantes. **No rompe nada** — si ya
mandabas esos params, sigue funcionando igual.

**2. `GET /productos/admin/filtrar` y `GET /variantes/v1/admin/filtrar` — filtro combinado
(rompe contrato, hay que actualizar el front).**

Antes: un solo parámetro `filtro` (enum `SIN_STOCK` / `CON_STOCK` / `CON_IMAGENES` /
`CON_STOCK_Y_IMAGENES`), sin poder combinarlo con búsqueda por nombre/código.

Ahora, **se quitó el parámetro `filtro`** y se reemplazó por 4 parámetros independientes, todos
opcionales, que se combinan entre sí con AND:

| Parámetro | Tipo | Significado |
|---|---|---|
| `nombreOCodigo` | string, opcional | Busca en nombre del producto/variante y en código de barras a la vez (como ya funciona en las búsquedas públicas) |
| `conStock` | boolean, opcional | `true` = con stock, `false` = sin stock, **omitido** = cualquiera |
| `conImagenes` | boolean, opcional | `true` = con imágenes, `false` = sin imágenes, **omitido** = cualquiera |
| `habilitado` | boolean, opcional | `true` = habilitado, `false` = deshabilitado, **omitido** = cualquiera |
| `page`/`pagina`, `size` | int | Igual que antes (default 1 y 10 si no se mandan) |

Ejemplo: buscar "pantalon" con stock, sin importar si tiene imágenes o no, solo habilitados:
```
GET /productos/admin/filtrar?nombreOCodigo=pantalon&conStock=true&habilitado=true&page=1&size=10
```

Ejemplo: solo deshabilitados, sin ningún otro filtro:
```
GET /variantes/v1/admin/filtrar?habilitado=false&pagina=1&size=10
```

**Reglas de uso:**
- Cada uno de los 3 filtros (`conStock`, `conImagenes`, `habilitado`) es de un solo estado a la
  vez — no tiene sentido pedir "con imágenes" y "sin imágenes" al mismo tiempo, por eso cada uno
  es un solo booleano (no un arreglo). Si no se manda el parámetro, no se filtra por esa dimensión.
  `nombreOCodigo` sí se puede combinar libremente con cualquier combinación de los otros 3.
- En variantes, `habilitado` filtra por el estado de la **variante** (`v.habilitado`), no del
  producto padre — coincide con el fix documentado arriba de `habilitar-lote`.
- **✅ Implementado en el front (2026-07-07):** `variante.service.ts` y `producto.service.ts`
  traducen internamente el enum al nuevo formato de parámetros. Los componentes que llaman a
  `adminFiltrar(...)` no cambian — la traducción ocurre dentro del servicio.

---

## ✅ Fix (2026-07-07): mensajes de error de promociones ahora son específicos

**Reportado:** al agregar una promoción al carrito y confirmar la venta/pedido, el back rechazaba
la operación con `400` y el mismo mensaje genérico `"La promocion '...' ya no esta disponible"` sin
importar cuál era el problema real (línea faltante, precio distinto, cantidad inválida, etc.) — esto
hacía imposible saber, desde el front, qué corregir.

**Causa:** `PromocionServiceImpl.validarLineasPromocion()` usaba el mismo mensaje para 4
validaciones distintas. Ya se separaron — el mensaje ahora dice exactamente cuál fue el problema:

| Situación | Mensaje nuevo |
|---|---|
| Faltan o sobran líneas del combo (el front debe mandar **una línea por cada variante** de la promoción, ver `PROMOCIONES.md` punto 7) | `"La promocion '{descripcion}' requiere N linea(s) (una por cada variante del combo), se recibieron M"` |
| Una `varianteId` mandada no pertenece a esa promoción | `"La variante {id} no pertenece a la promocion '{descripcion}'"` |
| El `precioUnitario` mandado no coincide con `precioEnPromocion` de esa variante en BD | `"El precio de la variante {id} en la promocion '{descripcion}' no coincide. Esperado: X, recibido: Y"` |
| La `cantidad` mandada no es múltiplo de la cantidad del detalle (ej. detalle pide de 1 en 1 y llegó 3 en una promo que solo permite llevar combos completos) | `"La cantidad de la variante {id} en la promocion '{descripcion}' debe ser multiplo de N, se recibio M"` |
| Promoción vencida o desactivada | `"La promocion '{descripcion}' ya no esta disponible"` (sin cambios) |
| Se intenta apartar/dar a crédito una promoción | `"Las promociones solo se pueden comprar de contado, no se pueden apartar ni dar a credito"` (sin cambios) |

**No cambia el contrato** (mismo `400`, mismo formato de response) — solo el texto del mensaje es
más específico. Si el front tenía un caso de prueba fallando "por promociones" sin saber por qué,
usar este mensaje nuevo para identificar cuál de las 4 validaciones está chocando (lo más común:
el front manda la promoción como **una sola línea** en vez de una línea por cada variante que la
compone — ver contrato en `PROMOCIONES.md`, sección 7).

---

## ✅ Fix (2026-07-21): `GET /variantes/v1/admin/filtrar?habilitado=...` ahora considera también el habilitado del producto padre

**Reportado:** un producto deshabilitado (p. ej. un borrador de carga rápida, buscándolo por su
código) no aparecía en
`GET /mis-productos/variantes/v1/admin/filtrar?nombreOCodigo=369&habilitado=false&pagina=1&size=10`.

**Antes (qué fallaba):** el filtro `habilitado` solo miraba el flag de la **variante**
(`v.habilitado`). Los borradores de carga rápida nacen con el **producto** deshabilitado (`'0'`)
pero su variante en `'1'`, así que:
- con `habilitado=false` el borrador **no salía** (su variante está en `'1'`), y
- con `habilitado=true` el borrador **sí salía** como si estuviera habilitado, aunque el producto
  no lo está.

Lo mismo aplicaba a cualquier producto deshabilitado desde el módulo de productos cuyas variantes
siguieran en `'1'`.

**Ahora:** el filtro usa el estado **efectivo** (variante Y producto):
- `habilitado=true` → variante habilitada **y** producto habilitado.
- `habilitado=false` → variante deshabilitada **o** producto deshabilitado (cualquiera de los dos
  basta para considerarla "no habilitada").
- omitido → sin filtro, igual que antes.

**Además cambia el campo `habilitado` del response** (`VarianteResumenDto`, aplica a todos los
listados de variantes que devuelven ese DTO): ahora refleja el estado efectivo — es `'1'` solo si
la variante **y** su producto están habilitados. Antes un borrador podía llegar con
`habilitado: '1'` aunque el producto estuviera deshabilitado; ya no.

**Request y demás parámetros no cambian.** Esto reemplaza la regla documentada en la sección del
2026-07-06 que decía "en variantes, `habilitado` filtra por el estado de la variante, no del
producto padre" — esa regla ya no aplica.

**Solo en `dev`/`qa` por ahora** — pendiente de subir a `main`.

---

## ✅ Nuevo (2026-07-21): parámetro `codigoGenerado` en los filtros admin de productos y variantes

Se agregó un 5.º parámetro opcional a los dos filtros combinados de admin, para poder listar los
productos que siguen con el **código de barras autogenerado** de la carga rápida (es decir, a los
que todavía no se les asigna el código real vía `/completar`):

**Request:**
```
GET /mis-productos/productos/admin/filtrar?codigoGenerado=true&page=1&size=10
GET /mis-productos/variantes/v1/admin/filtrar?codigoGenerado=true&pagina=1&size=10
```

| Parámetro | Tipo | Significado |
|---|---|---|
| `codigoGenerado` | boolean, opcional | `true` = solo productos con código de barras autogenerado (borradores de carga rápida sin código real); `false` = solo productos con código real (incluye todos los productos normales, que nunca pasaron por carga rápida); **omitido** = cualquiera |

- Se combina con AND con los otros 4 (`nombreOCodigo`, `conStock`, `conImagenes`, `habilitado`),
  igual que hasta ahora. Ejemplo típico para la pantalla de "pendientes de completar":
  `?codigoGenerado=true&habilitado=false`.
- En variantes filtra por el flag del **producto padre** (el código de barras vive en el producto).

**Comportamiento con el filtro omitido y los `NULL` (importante para el front):** en BD, los
productos normales (que nunca pasaron por carga rápida) tienen `codigo_barras_generado = NULL`.
El backend ya lo maneja — el front no tiene que tratar el `NULL` como caso aparte:

```
# solo texto, sin codigoGenerado → devuelve TODOS los que matcheen "369"
# (autogenerados, código real y NULL — no se filtra por esa dimensión)
GET /mis-productos/variantes/v1/admin/filtrar?nombreOCodigo=369&pagina=1&size=10

# texto + solo los de código autogenerado
GET /mis-productos/variantes/v1/admin/filtrar?nombreOCodigo=369&codigoGenerado=true&pagina=1&size=10

# texto + solo los de código real — los NULL caen de este lado (cuentan como código real)
GET /mis-productos/variantes/v1/admin/filtrar?nombreOCodigo=369&codigoGenerado=false&pagina=1&size=10
```
- **El response no cambia** — mismos DTOs de siempre. Los DTOs de listado no incluyen el flag
  `codigoBarrasGenerado`; si el front lo llega a necesitar como badge en un listado mixto (sin
  filtrar), se puede agregar después.

**Solo en `dev`/`qa` por ahora** — pendiente de subir a `main`.

---

## ✅ Fix (2026-07-07): campo `cantidad` en detalles de promoción activa

**Causa raíz del bug "cantidad obligatoria":** `GET /v1/promociones/activas` devolvía los detalles
de cada promo sin el campo `cantidad` (cuántas unidades de esa variante consume un combo). Cuando el
front armaba la solicitud de venta hacía `d.cantidad * cantidadCombos`, y al ser `d.cantidad`
`undefined`, el resultado era `NaN` → `null` en el JSON → el back rechazaba con *"La cantidad es
obligatoria y debe ser mayor a 0"*.

**Fix:** `PromocionDetalleActivaDto` ahora incluye `cantidad`. El front no necesita cambiar nada
en `venta-directa.component.ts` — el cálculo ya era correcto, solo faltaba el dato del back.

**Respuesta actualizada de `GET /v1/promociones/activas` — cada detalle ahora incluye `cantidad`:**
```json
{
  "varianteId": 12,
  "nombreProducto": "Jean Slim",
  "talla": "M",
  "color": "Azul",
  "cantidad": 1,
  "precioNormal": 300.00,
  "precioEnPromocion": 220.00,
  "imagenUrl": "..."
}
```

**Archivos cambiados:** `PromocionDetalleActivaDto.java`, `PromocionServiceImpl.java`
(método `toDetalleActivaDto`).

---

## ✅ Nuevo (2026-07-07): `existencias` por variante en `GET /v1/promociones/admin`

**Qué es:** el endpoint `GET /v1/promociones/admin?pagina=&size=` ahora devuelve en cada detalle
el stock actual (`existencias`) de la variante. Útil para que el panel admin muestre cuántos combos
se pueden vender actualmente sin tener que ir a buscar el stock variante por variante.

**Campo nuevo en cada detalle de la respuesta admin:**
```json
{
  "varianteId": 12,
  "nombreProducto": "Jean Slim",
  "talla": "M",
  "color": "Azul",
  "cantidad": 1,
  "precioEnPromocion": 220.00,
  "imagenUrl": "...",
  "existencias": 8
}
```
`existencias` es el stock actual de esa variante. Para calcular cuántos combos completos se pueden
vender: `Math.floor(existencias / cantidad)` por cada detalle → tomar el mínimo de todos.

**El endpoint de clientes (`GET /v1/promociones/activas`) NO cambia:** sigue devolviendo
`instanciasDisponibles` ya calculado en el back. El `existencias` crudo es solo para el panel admin.

**Archivos cambiados:** `PromocionDetalleResponseDto.java` (campo `existencias` agregado),
`PromocionServiceImpl.java` (método `toDetalleResponseDto` pasa `variante.getStock()`).

**Cambios en el front (ya aplicados en esta sesión):**
- `promocion.model.ts` — `IPromocionDetalle` tiene campo opcional `existencias?: number`.
- Panel admin `gestion-promociones.component.html` — cada detalle muestra `(N en stock)` y el
  encabezado de la tarjeta calcula `N combos disponibles` (mínimo entre las piezas).

---

## ✅ Nuevo (2026-07-07): fecha+hora completa y `productoId` en endpoints de pedidos

**Motivo:** en `mis-pedidos` no se podía mostrar la hora de la compra porque el back nunca la
guardaba (`pedidos.fecha_pedido` es columna `DATE`, sin hora). Se agregó una columna nueva
`fecha_hora_registro` (`DATETIME`, aditiva, no reemplaza `fecha_pedido`) que se llena en cada
pedido nuevo. **Pedidos creados antes de este cambio no tienen hora real** — el back rellena con
medianoche (`00:00`) como fallback, no lo interpretes como que la compra fue a esa hora.

**1. `GET /v1/pedidos/{id}/detalle` (`PedidoDetalleResponse`)** — dos campos nuevos:
```json
{
  "fechaPedido": "2026-07-07",
  "fechaHoraRegistro": "2026-07-07T14:32:10",
  "detalles": [
    { "id": 1, "productoId": 45, "varianteId": 12, "productoNombre": "Jean Slim", "talla": "M", "color": "Azul", "promocionId": 3, "promocionDescripcion": "Combo verano" }
  ]
}
```
- `fechaHoraRegistro`: ISO `LocalDateTime` (fecha+hora completa) — úsalo en vez de `fechaPedido`
  para mostrar/formatear la hora de la compra en ticket y detalle.
- `detalles[].productoId`: id real del producto (ya resuelto por el back incluso en líneas de
  promoción/variante) — úsalo para armar la URL de imagen: `GET /imagen/v1/{productoId}`.
  Antes este campo no existía en `detalles[]`, solo `varianteId`.

**2. `GET /v1/pedidos/findPedido/{id}`, `findPedido/{idPedido}/{idCliente}`, `buscarClientePedido`**
(la respuesta paginada que arma la lista de `mis-pedidos`, campo `pedido.detalles[].producto` /
`pedido.fecha_pedido`): **NO cambia de forma (sigue siendo el mismo JSON con los mismos nombres de
campo)**, solo cambia el **contenido** del string `fecha_pedido`: antes `"07/07/2026"`, ahora
`"07/07/2026 14:32"` (agregó `HH:mm`). Si el front parsea esta fecha con un split fijo por `/`
asumiendo solo `dd/mm/yyyy` (como el pipe `FechaEspanolPipe`), hay que actualizarlo para no rompa
con el sufijo de hora.

**Archivos cambiados:** `Pedido.java` (campo `fechaHoraRegistro`), `PedidoDetalleResponse.java`,
`DetalleItemResponse.java` (campo `productoId`), `PedidoServiceImpl.java` (los 4 puntos donde se
crea un pedido + `getDetallePedido()`), `VentaServiceImpl.java`, `AbonoServiceImpl.java`,
`IPedidoRepository.java` (los 4 queries nativos), migración
`migration_pedido_fecha_hora.sql` (**pendiente de aplicar en la BD** `inventario_key_qa`).
- Panel admin `gestion-promociones.component.ts` — método `combosDisponibles(p)` calcula el
  mínimo de `Math.floor(existencias / cantidad)` entre todas las piezas del combo.

---

## ✅ Fix (2026-07-07): `POST /variantes/v1/inicializarDesdeProducto` — el checkbox "misma imagen para todas" no funcionaba

**Endpoint:** `POST /variantes/v1/inicializarDesdeProducto` (botón "Variantes" en la card de
`/productos/buscar` admin).

**Bug reportado:** con `imagenParaTodas: true`, tanto sin subir archivos como subiendo un archivo
nuevo, el back respondía error y no se creaban variantes con imagen. Solo funcionaba el caso sin
checkbox y sin archivos (variantes sin imagen).

**Causa:** el código solo manejaba el caso "checkbox marcado + archivo nuevo". Si el checkbox
estaba marcado pero no se mandaba ningún archivo, el bloque de imágenes se saltaba por completo —
nunca buscaba la imagen ya existente del producto, así que las variantes se creaban sin imagen aunque
el checkbox estuviera marcado. Aparte, la subida al microservicio de imágenes no tenía manejo de
error, así que si ese servicio fallaba o tardaba, el error no traía info útil.

### Request (sin cambios respecto a lo que el front ya manda)

`multipart/form-data` con 2 partes:

```
Part 1 → nombre: "request"
         Content-Type: application/json
         Body: {
           "productoId":        <número>,
           "cantidadVariantes": <número>,
           "imagenParaTodas":   <boolean>   ← viene del checkbox
         }

Part 2 → nombre: "files[]"    ← solo si el usuario seleccionó archivos
         Content-Type: image/*
         Body: <archivo(s) seleccionados>
```

### Response — éxito (201)

```json
{ "mensaje": "La peticion fue exitosa", "code": 200, "data": "Variantes", "lista": null }
```

`data` es el string literal `"Variantes"`, **no** un arreglo de las variantes creadas (esto ya era
así antes del fix, solo estaba mal documentado). Si el front necesita las variantes recién creadas
(con sus imágenes) para refrescar la UI, tiene que volver a pedir
`GET /variantes/porProducto/{productoId}` después de este POST.

### Los 3 flujos — comportamiento ya corregido

| Flujo | `imagenParaTodas` | `files[]` | Resultado |
|---|---|---|---|
| A | `false` | sin archivos | Crea variantes sin imagen. (sin cambios) |
| B | `true` | sin archivos | Busca la imagen **principal** ya vinculada al producto (o la primera si ninguna está marcada como principal) y la vincula a **todas** las variantes creadas. |
| C | `true` | con 1+ archivos | Sube el/los archivo(s) al microservicio de imágenes y vincula esa(s) imagen(es) nueva(s) a **todas** las variantes creadas. Si el producto no tenía imagen propia, también se la asigna a él. |

### Response — error (nuevos casos)

Todos llegan en el mismo campo que el front ya lee (`err.error.mensaje`), no cambia el manejo
en el interceptor/handler genérico:

| Caso | HTTP | `mensaje` |
|---|---|---|
| Flujo B, **producto sin ninguna imagen** para copiar | `404` | `"El producto {id} no tiene una imagen para copiar a las variantes. Sube una imagen o desmarca la casilla de 'misma imagen para todas'."` |
| Flujo C, **falla la subida al microservicio de imágenes** | `404` | `"No se pudo subir la imagen al servicio de imagenes, intenta de nuevo"` |
| Stock insuficiente (ya existía, sin cambios) | `404` | `"Stock insuficiente para crear N variantes del producto X. Stock disponible: Y"` |

**Front:** no requiere cambios de código — la petición que ya arman coincide con este contrato y
el error handler genérico ya muestra estos mensajes nuevos en el Swal. Es contrato de referencia
para que sepan qué esperar al probar los 3 flujos y no se sorprendan con el `404` nuevo del caso B.

**Estado:** subiendo a `dev` para pruebas de QA. Falta subir a `main` siguiendo el flujo normal
(`dev → qa → main`).

---

## 🔴 Fix crítico (2026-07-08): `PUT /v1/usuarios/updateUsuario/{id}` destruía la contraseña real del usuario

**Bug reportado:** al editar el correo de un usuario desde el panel admin (`usuarios/update`) y dar
"Actualizar", el back sobrescribía la contraseña real del usuario aunque el front no haya tocado
ese campo — dejando la cuenta con una contraseña inservible.

**Causa:** `UsuarioServiceImpl.updateUserDto()` hacía
`existe.setPassword(passwordEncoder.encode(usuarioDto.getPassword()))` **sin validar** si
`usuarioDto.getPassword()` venía `null`/vacío. Si el front no incluía el campo `password` en el
body (el caso normal al editar solo correo/username/enabled), el back igual encriptaba ese valor
vacío/null y lo guardaba como la contraseña real — efectivamente reseteándola sin que nadie lo
pidiera.

**Fix:** `updateUserDto()` **ya no toca el campo password en absoluto**, sin importar qué venga en
el body. El único endpoint que puede cambiar la contraseña de un usuario sigue siendo
`PUT /v1/usuarios/{id}/resetear-password` (genera una password aleatoria de 8 caracteres y la
devuelve en la respuesta para que el admin se la comparta al usuario).

**Acción requerida en el front:**
- El campo `password` en el body de `PUT /v1/usuarios/updateUsuario/{id}` ya no tiene ningún
  efecto — el back lo ignora. Se puede dejar de mandar.
- **El formulario de edición de usuario (admin) no debe mostrar ningún campo de contraseña.** El
  admin solo tiene 2 acciones válidas sobre la contraseña de otro usuario: el botón
  "Restablecer contraseña" (`PUT /v1/usuarios/{id}/resetear-password`) y nada más — no puede
  fijar una contraseña arbitraria directamente.
- El campo `username` en ese mismo formulario debe mostrarse **deshabilitado** (solo lectura) para
  el admin — el update sigue aceptándolo en el body por compatibilidad, pero la UI no debería
  permitir editarlo desde esta pantalla.

**CORRECCIÓN (2026-07-08, segunda vuelta) — el diseño de correo de arriba cambió por completo.**
La primera versión de este fix guardaba el correo de inmediato y solo reseteaba
`correoVerificado`. El diseño real que se pidió es **verificar ANTES de guardar**: el correo NO se
actualiza hasta que el código sea correcto; si el código falla/expira/se cancela, el correo real
**nunca cambió** (no hace falta "revertir" nada porque nunca se tocó). Se agregó una columna nueva
`usuario_modificacion.correo_pendiente` para esto — ver migración
`migration_correo_pendiente_usuario.sql`, **pendiente de correr en dev/qa/prod**.

> ⚠️ **No confundir con `Cliente.correoPendiente`** (perfil del cliente, `mis-datos`, ya existía de
> antes) — es una columna distinta en otra tabla, con otra regla: ahí el admin SÍ puede aplicar el
> correo directo sin verificar. Aquí (cuenta de login/`Usuario`), el admin también verifica.

### 🐛 BUG CONFIRMADO EN QA (2026-07-08) — el front en `usuarios/update` llama al endpoint equivocado

**Síntoma:** al cambiar el correo de otro usuario desde el panel admin, no llega ningún código al
correo nuevo.

**Causa confirmada con curl real:** el front está llamando
`POST /v1/auth/enviar-codigo-verificacion` con body `{ "userName": "pedro" }` — **ese es el
endpoint viejo** (verificación única post-registro, ver arriba). Ese endpoint:
- No recibe ningún correo nuevo, solo `userName`.
- Manda el código al correo que **ya está guardado**, no a uno nuevo.
- Si ese correo ya está verificado (el caso normal para cualquier cuenta activa), responde `400`
  con `"El correo ya esta verificado"` y no manda nada — por eso "no llega el correo".

**Corrección necesaria en el front — reemplazar esa llamada:**

| ❌ Está llamando (incorrecto para cambio de correo) | ✅ Debe llamar |
|---|---|
| `POST /v1/auth/enviar-codigo-verificacion` `{ "userName": "..." }` | `POST /v1/usuarios/{id}/solicitar-cambio-correo` `{ "correoNuevo": "..." }` (admin, `{id}` = id del usuario que se está editando, **no** el id del admin) |
| `POST /v1/auth/verificar-correo` `{ "userName": "...", "codigo": "..." }` | `POST /v1/usuarios/{id}/confirmar-cambio-correo` `{ "codigo": "..." }` (admin) |

Los endpoints `enviar-codigo-verificacion`/`verificar-correo` **solo sirven para la verificación
única post-registro** — nunca para cambiar un correo ya existente, ni desde el panel admin ni
desde self-service. Para self-service (el propio usuario cambia su correo) es la misma tabla pero
con las rutas `/v1/auth/solicitar-cambio-correo` / `confirmar-cambio-correo` (sin `{id}`, ver el
contrato completo abajo).

**4 endpoints nuevos (2 admin, 2 self-service) — reemplazan el uso de
`enviar-codigo-verificacion`/`verificar-correo` para este caso** (esos 2 endpoints viejos siguen
existiendo tal cual, pero solo para la verificación inicial post-registro, no para cambios de
correo posteriores):

```
# Admin — cambia el correo de OTRO usuario (por id)
POST /v1/usuarios/{id}/solicitar-cambio-correo   Body: { "correoNuevo": "..." }
POST /v1/usuarios/{id}/confirmar-cambio-correo   Body: { "codigo": "123456" }

# Self-service — el propio usuario cambia SU correo (identificado por el JWT, sin id)
POST /v1/auth/solicitar-cambio-correo            Body: { "correoNuevo": "..." }
POST /v1/auth/confirmar-cambio-correo            Body: { "codigo": "123456" }
```

**Corrección de contrato (2026-07-08):** la primera versión de estos 4 endpoints devolvía texto
plano (`"Codigo enviado al correo nuevo"`) en vez de JSON. Eso rompía el front porque el
`HttpClient` esperaba JSON y tronaba al parsear un body que no lo era — el back sí mandaba el
correo, pero el front mostraba error igual. Ya está corregido: ahora responden `ResponseGeneric<String>`,
igual que el resto del API.

- `solicitar-cambio-correo` → `200` con body `{ "mensaje": "La peticion fue exitosa", "code": 200, "data": "Codigo enviado al correo nuevo", "lista": null }`
  (el código se manda a la dirección **nueva**, no a la actual). Leer el mensaje para mostrar desde `data` (o `mensaje` en el caso de error).
  `400` con body `{ "mensaje": "<detalle del error>", "code": 404, "data": null, "lista": null }` si `correoNuevo` viene vacío o es igual al actual.
  - **Nuevo (2026-07-08):** si ya había un código vigente (no expirado) para ese mismo correo nuevo — ej. el usuario le dio doble click al botón, o cerró el modal y volvió a intentar antes de que pasaran los 15 min — el back **ya no reenvía un correo nuevo**, reutiliza el código que ya mandó (evita que el usuario reciba varios correos con códigos distintos donde el último invalida a los anteriores). En ese caso `data` viene con el mensaje
    `"Ya tienes un codigo vigente enviado a ese correo, revisa tu bandeja"` en vez de `"Codigo enviado al correo nuevo"` — sigue siendo `200`, el front puede mostrar cualquiera de los dos como texto informativo y abrir el modal del código igual en ambos casos.
- `confirmar-cambio-correo` → `200` con body `{ "mensaje": "La peticion fue exitosa", "code": 200, "data": "Correo actualizado correctamente", "lista": null }`
  — **solo en este momento** se actualiza el `email` real. `400` con `{ "mensaje": "<detalle del error>", "code": 404, "data": null, "lista": null }`
  si el código es inválido/expiró — en ese caso el correo real sigue siendo el de antes, no hay que
  hacer nada para "revertir" el campo en el front, solo mostrar `mensaje` y dejar el valor viejo.
- El código **nunca viaja en la respuesta de la API** — solo llega por correo. El modal siempre
  necesita un input para que el usuario/admin lo escriba.
- Nota sobre el `code` del body en caso de error: viene `404` aunque el HTTP status real sea `400`
  — es una particularidad de `ResponseGeneric` que ya existía en otros endpoints del API, no es
  nuevo de esta corrección. Para detectar error en el front, usar el status HTTP (`400`), no el
  campo `code` del body.

**Front — flujo para las 2 pantallas (admin y self-service), idéntico salvo el endpoint:**
1. Si el correo ingresado en el form es **igual** al actual → no pasa nada especial, se guarda
   junto con los demás campos del form normal (o ni se manda, según cómo armes el form).
2. Si es **distinto** → abrir modal, llamar `solicitar-cambio-correo` (con `{id}` si es admin, sin
   nada si es self-service), mostrar input para el código, llamar `confirmar-cambio-correo`.
3. Si `confirmar-cambio-correo` responde `200` → refrescar el campo `email` en pantalla con el
   valor nuevo. Si responde `400` → mostrar el mensaje, dejar el campo como estaba, permitir
   reintentar o cancelar.

**Endpoint self-service — `PUT /v1/auth/mi-perfil` (ya NO incluye `email`):**

```
PUT /v1/auth/mi-perfil
Authorization: Bearer <token del propio usuario>
Content-Type: application/json

Body: { "username": "string, requerido" }
```
Response éxito (200): `"Perfil actualizado correctamente"`. El correo se maneja exclusivamente con
los 2 endpoints de arriba, nunca con este. Identifica la cuenta por el JWT — no hay que mandar
ningún id.

**Contraseña self-service — sin cambios respecto a la primera vuelta:** `PUT /v1/auth/cambiar-password`
(`{ "passwordActual", "nuevaPassword" }`, ya existía antes de esta sesión) sigue siendo el único
camino. Front: al detectar que el usuario está escribiendo en los campos de nueva contraseña, se
debe mostrar el **mismo validador de reglas que usa el formulario de registro** (reusar ese
componente), y solo permitir guardar si pasa esa validación y se ingresó la contraseña actual.

**Resumen para el front — 2 pantallas distintas:**

| Pantalla | Quién | Password | Username | Email |
|---|---|---|---|---|
| Admin edita a otro usuario (`usuarios/update`) | Solo ADMIN | Sin campo de password en el form. Solo botón "Restablecer contraseña" (`PUT /v1/usuarios/{id}/resetear-password`) | Deshabilitado (solo lectura) | Modal verificar-antes-de-guardar (`solicitar`/`confirmar-cambio-correo` con `{id}`) |
| Usuario edita su propia cuenta ("Mi perfil") | Cualquier autenticado | Validador de registro + contraseña actual obligatoria, vía `PUT /v1/auth/cambiar-password` | Editable, `PUT /v1/auth/mi-perfil` | Modal verificar-antes-de-guardar (`solicitar`/`confirmar-cambio-correo` sin id) |

**Pendiente:** correr `migration_correo_pendiente_usuario.sql` en todos los ambientes antes de que
el flujo de correo funcione.

**Solo en `dev` por ahora**, pendiente de subir a `qa`/`main`.

---

## 🆕 Consultar cambio de correo pendiente (2026-07-08) — reemplaza guardar estado en el navegador

**Motivo:** se detectó que la implementación actual en el front (`mi-perfil.component.ts`) guarda
el correo pendiente en `sessionStorage` (`cambio_correo_self`) para sobrevivir a un refresh de
página mientras el código de verificación sigue vigente (15 min). Esto funciona pero tiene un bug:
la clave de `sessionStorage` no distingue usuario — si el usuario A pide un cambio de correo y
cierra sesión sin confirmar/cancelar, y el usuario B inicia sesión en la **misma pestaña**, al
cargar `mi-perfil` se restaura el correo pendiente de A en el formulario de B. Además, el front
adivina el tiempo de expiración (15 min contados desde el navegador) en vez de usar el real del
back.

**Se agregaron 2 endpoints GET para que el front deje de usar `sessionStorage`/`localStorage` para
esto y consulte el estado real al back** (que ya lo persistía en BD, columna
`usuario.correo_pendiente` + `usuario.codigo_verificacion_expira`):

```
GET /v1/auth/cambio-correo-pendiente              (self-service, identifica por JWT)
GET /v1/usuarios/{id}/cambio-correo-pendiente     (admin, de OTRO usuario por id)
```

**Response (200), igual en ambos:**
```json
{
  "mensaje": "La peticion fue exitosa",
  "code": 200,
  "data": {
    "pendiente": true,
    "correoPendiente": "nuevo@correo.com",
    "expiraEn": "2026-07-08T14:35:00"
  },
  "lista": null
}
```
- `pendiente: false` (con `correoPendiente`/`expiraEn` en `null`) si no hay cambio en curso, **o si
  el código ya expiró** — en ese caso el front debe tratarlo como "no hay nada pendiente" (no
  reabrir el modal), aunque el dato siga en BD hasta el próximo `solicitar-cambio-correo`.
- `expiraEn` es la fecha/hora real de expiración (formato ISO local, sin zona) — úsala para mostrar
  cuenta regresiva o decidir si vale la pena reabrir el modal, en vez de una regla fija de 15 min
  del lado del front.

**Acción pendiente para el front (no implementada por el back, es cambio de front):**
- Reemplazar la lógica de `sessionStorage.getItem('cambio_correo_self')` en
  `mi-perfil.component.ts` (`ngOnInit` → `restaurarCambioCorreoPendiente()`) por una llamada a
  `GET /v1/auth/cambio-correo-pendiente` al cargar el componente. Si `pendiente: true`, mostrar el
  banner/estado de "verificación en curso" con `correoPendiente`; si `false`, no mostrar nada — ya
  no hace falta leer ni escribir `sessionStorage` para esto.
- Aplicar el mismo cambio en la pantalla admin de edición de usuario (`usuarios/update`), usando
  `GET /v1/usuarios/{id}/cambio-correo-pendiente` en vez de cualquier storage local equivalente que
  tenga esa pantalla.
- Ya no es necesario limpiar manualmente ninguna key de `sessionStorage`/`localStorage` al
  confirmar o cancelar — simplemente dejar de mostrar el banner tras la respuesta del backend
  (`confirmar-cambio-correo` exitoso, o el usuario cancela en el front sin llamar a nada, ya que el
  back no expone un endpoint de "cancelar" — el pendiente se sobreescribe solo la próxima vez que
  se llame `solicitar-cambio-correo`, o expira solo a los 15 min).

**En `dev`, pendiente de subir a `qa`/`main`.**

---

## 🆕 Independizar una variante en su propio producto (2026-07-07)

**Caso de uso:** el admin capturó mal el código de barras de un producto con varias variantes, o
simplemente decide que una variante ya merece ser su propio producto. La variante conserva toda su
info (talla, color, imagen, stock) — la operación crea un producto nuevo a partir de ella, con su
propio código de barras.

```
POST /variantes/v1/{varianteId}/independizar
Authorization: Bearer <token admin>
Content-Type: application/json
```

**Request** — mismo shape que crear un producto normal, más el código de barras nuevo obligatorio.
El front prellena estos campos abriendo el mismo formulario de "crear producto":

```json
{
  "nombre": "string, requerido",
  "descripcion": "string",
  "marca": "string",
  "color": "string",
  "contenido": "string",
  "piezas": 0.0,
  "precioCosto": 0.0,
  "precioVenta": 0.0,
  "precioRebaja": 0.0,
  "palabraClaveId": 1,
  "codigoBarras": "string, requerido, debe ser nuevo (no existir ya en otro producto)",
  "imagenPrincipalId": 123
}
```

**Precarga de campos en el front** (el back solo recibe lo que venga en el body, no le importa de
dónde lo sacó el front):

| Campo | Prioridad 1 | Si viene null/vacío, cae a |
|---|---|---|
| `nombre` | — | **Producto origen** (la variante no tiene `nombre`) |
| `descripcion` | Variante | Producto origen |
| `marca` | Variante | Producto origen |
| `color` | Variante | Producto origen |
| `contenido` | Variante (`contenidoNeto`) | Producto origen (`contenido`) |
| `piezas` | — | **Producto origen** (la variante no tiene `piezas`, es `NOT NULL` en BD) |
| `precioCosto`/`precioVenta`/`precioRebaja` | — | Producto origen (la variante no tiene precio propio) |
| `palabraClaveId` | Variante | Producto origen |
| `codigoBarras` | — | **Siempre vacío** — es el dato nuevo que captura el admin |

- `imagenPrincipalId` opcional — solo si la variante tenía más de una imagen y el admin quiere
  elegir cuál queda como principal. Con 1 sola imagen el back la usa automático.
- **No se manda `stock`** — se calcula solo, a partir del stock de la variante (se resta del
  producto origen y se asigna al producto nuevo, sin duplicar ni perder unidades).
- El campo `stock` no se muestra editable en el modal; si se quiere mostrar informativo, usar el
  stock actual de la variante.

**Response (éxito, 201):**
```json
{
  "mensaje": "La peticion fue exitosa",
  "code": 200,
  "data": {
    "productoNuevoId": 456,
    "codigoBarras": "cod-nuevo-123",
    "stockProductoOrigenRestante": 2
  }
}
```

**Errores esperados:**
| Caso | HTTP | Mensaje |
|---|---|---|
| `varianteId` no existe | `404` | `"No existe la variante con id: {id}"` |
| Código de barras vacío/no enviado | `404` | `"El codigo de barras es requerido"` |
| Código de barras ya usado por otro producto | `409` | `"El codigo de barras {codigo} ya esta en uso por otro producto"` |

**Flujo en el front:**
1. Botón "Independizar" en el detalle de una variante (solo admin).
2. Abre el formulario de "crear producto" prellenado según la tabla de arriba, todo editable.
3. Campo obligatorio adicional: código de barras nuevo (distinto al del producto origen). El front
   puede validar que no venga vacío; la validación de "no duplicado" la hace el back.
4. Al confirmar, llama al endpoint. Si responde `409`/`400` por código duplicado, muestra el
   mensaje tal cual y deja el formulario abierto — no se tocó stock ni la variante en ese caso.
5. Tras éxito (`201`): refrescar el producto origen (usar `data.stockProductoOrigenRestante`, no
   hace falta volver a pedir el producto completo), refrescar la lista de variantes del producto
   origen (la variante ya no debe aparecer ahí), y navegar/mostrar el producto nuevo
   (`data.productoNuevoId` + `data.codigoBarras`).

**Solo ADMIN** (mismo matcher genérico de `/variantes/**`). Contrato completo también en
`PLAN_MEJORAS.md` sección 16. **En `dev`, pendiente de subir a `qa`/`main`.**

---

## 🆕 Reporte de promociones — cuántos combos se han vendido y ganancia por promoción (2026-07-13)

**Endpoint nuevo, no existía nada parecido antes:**

```
GET /v1/reportes/ventas/promociones?desde=2026-07-01&hasta=2026-07-31
Authorization: Bearer <token admin>
```

`desde` y `hasta` son **opcionales** (`yyyy-MM-dd`). Sin ellos, trae el histórico completo desde
que existe la promoción. Si se manda solo uno de los dos, filtra solo por ese límite.

**Solo ADMIN** — mismo matcher genérico ya existente (`/v1/reportes/**` → `hasRole("ADMIN")`), no
requirió tocar `SecurityConfig`.

**Response 200:**
```json
{
  "data": [
    {
      "promocionId": 7,
      "descripcion": "Combo Jean + Blusa",
      "combosVendidos": 14,
      "numeroTransacciones": 9,
      "ventaTotal": 4900.00,
      "gananciaTotal": 1750.00,
      "ultimaVenta": "2026-07-12"
    },
    {
      "promocionId": 3,
      "descripcion": "Combo Verano",
      "combosVendidos": 0,
      "numeroTransacciones": 0,
      "ventaTotal": 0.0,
      "gananciaTotal": 0.0,
      "ultimaVenta": null
    }
  ]
}
```
Ordenado por `combosVendidos` descendente (los más vendidos primero). **Incluye promociones sin
ninguna venta** (aparecen con todo en 0 y `ultimaVenta: null`) — así el admin ve también las que no
han pegado, no solo las exitosas.

**Qué significa cada campo:**
- `combosVendidos`: número de combos completos vendidos, **no piezas sueltas**. Si el combo es
  Jean+Blusa y se vendieron 14 combos, son 28 filas de venta por dentro (14 jeans + 14 blusas), pero
  el campo ya reporta 14 — el cálculo evita contar de más cuando el combo tiene varias piezas.
- `numeroTransacciones`: en cuántas ventas/pedidos distintos apareció esta promoción (un cliente que
  compra 2 combos en un solo ticket cuenta como 1 transacción con 2 combos).
- `ventaTotal` / `gananciaTotal`: suma real de lo vendido y la ganancia de esa promoción en el rango
  de fechas — viene directo de los registros de venta ya guardados (`detalle_venta_variantes`), no
  es una estimación.
- `ultimaVenta`: fecha (sin hora) de la venta más reciente que incluyó esta promoción.

**No es un cambio de contrato de nada existente** — es un endpoint nuevo, no toca `/promociones/**`
ni ningún flujo de venta/carrito ya documentado.

**Falta hacer:**
- ⏳ No hay pantalla en el front para esto todavía — hay que armar una vista nueva (ej. dentro de
  "🎁 Gestión Promociones" o como pestaña "Reportes"), no reemplaza ni modifica ninguna pantalla
  existente. El endpoint ya está en dev/qa/main.
- Sugerido para la vista: tabla con las columnas de arriba, filtro de rango de fechas (opcional,
  puede arrancar sin filtro mostrando todo), ordenado ya viene del back por más vendidos.

**Archivos back:** `PromocionReporteDto.java` (nuevo), `IPromocionRepository.java` (query
`reportePromociones`), `ReporteVentasServiceImpl.java` / `IReporteVentasService.java`,
`ReporteVentasController.java`.

---

## 🆕 Filtros de búsqueda en el catálogo público (2026-07-13)

**Primera de 3 mejoras acordadas para la página pública** (filtros → favoritos → reseñas, se van
agregando una por una). **No requiere correr ningún SQL** — no se tocó ninguna tabla, son queries
nuevas sobre columnas que ya existen.

### 1. Catálogo filtrado

```
GET /variantes/v1/buscar-filtrado?termino=&precioMin=&precioMax=&talla=&color=&marca=&pagina=1&size=10
```

Pública (no requiere login), igual que `/variantes/v1/buscar`. **Todos los parámetros son
opcionales** — mandar solo los que el usuario haya elegido, el resto se omite o se manda vacío:

| Parámetro | Tipo | Notas |
|---|---|---|
| `termino` | string | Busca en nombre de producto, marca, palabra clave y código de barras (como hoy) |
| `precioMin` / `precioMax` | number | Filtra por `producto.precioVenta`. Se puede mandar solo uno de los dos |
| `talla` | string | **Match exacto** (no `LIKE`) — pensado para venir de un dropdown, no de texto libre |
| `color` | string | Match exacto, mismo criterio que talla |
| `marca` | string | Match exacto, mismo criterio que talla |
| `pagina` / `size` | int | Igual que el resto de endpoints paginados |

Todos los filtros se combinan con **AND** (ej. `talla=M&color=Azul` → solo variantes M Y azules).

**Diferencia importante con `/variantes/v1/buscar` (el buscador de texto que ya existe):**
`/buscar` hace una cascada (busca por código → si no hay nada por palabra clave → si no hay nada
por nombre) y **lanza error 404 si no encuentra nada**. `/buscar-filtrado` es un único query con
todos los filtros combinados y **devuelve lista vacía `"t": []`** si no hay resultados — no hay que
capturar un error para el caso "sin resultados", solo revisar si `t` viene vacío. Uno no reemplaza
al otro: `/buscar` sigue igual para el buscador de texto simple; `/buscar-filtrado` es para cuando
el usuario además aplica filtros.

**Response 200** — mismo shape que `/variantes/v1/buscar` (no cambia nada de `VarianteResumenDto`):
```json
{
  "data": {
    "pagina": 1,
    "totalPaginas": 3,
    "totalRegistros": 27,
    "t": [
      {
        "id": 12, "talla": "M", "descripcion": "...", "color": "Azul", "presentacion": "...",
        "stock": 8, "marca": "Levi's", "contenidoNeto": null, "imagenUrl": "...",
        "precio": 300.00, "codigoBarras": "GLPD-066", "nombreProducto": "Jean Slim", "habilitado": "1"
      }
    ]
  }
}
```

**Mismas reglas de visibilidad que el resto del catálogo público:** solo variantes con
`stock > 0`, producto habilitado, variante habilitada y con al menos una imagen — igual que
`/variantes/v1/buscar` para clientes no-admin.

### 2. Valores disponibles para armar los filtros (dropdowns/slider)

```
GET /variantes/v1/filtros-disponibles
```

Pública, sin parámetros. Devuelve los valores que **realmente existen** en el catálogo visible
ahora mismo, para que el front no tenga que adivinar qué mostrar en los dropdowns ni mostrar
opciones que no van a dar resultados:

```json
{
  "data": {
    "tallas": ["CH", "M", "G", "32", "34"],
    "colores": ["Azul", "Negro", "Rojo"],
    "marcas": ["Levi's", "Zara", "Bershka"],
    "precioMin": 89.0,
    "precioMax": 1250.0
  }
}
```
`precioMin`/`precioMax` son el rango real del catálogo — úsalo para los límites del slider de
precio. Si el catálogo estuviera vacío, las listas vienen vacías y los precios vienen `null`.

**Sugerencia de flujo en el front:** al entrar a la pantalla de catálogo, llamar primero a
`filtros-disponibles` para pintar los controles (dropdowns de talla/color/marca + slider de
precio con esos límites), y usar `buscar-filtrado` cada vez que el usuario cambie algún filtro.

**Archivos back:** `FiltrosDisponiblesDto.java` (nuevo), `IVarianteRepository.java`
(`buscarVariantesPublicoFiltrado`, `findTallasDisponiblesPublico`, `findColoresDisponiblesPublico`,
`findMarcasDisponiblesPublico`, `findRangoPreciosPublico`), `VarianteServiceImpl.java`,
`VarianteController.java` (`/v1/buscar-filtrado`, `/v1/filtros-disponibles`).

**⏳ Pendiente:** subir de `dev` a `qa` (por ahora solo en `dev`, sin push todavía).

---

## 🆕 Favoritos (2026-07-13)

**Segunda de las 3 mejoras acordadas para la página pública.** Tabla nueva `favorito`.

**⚠️ Requiere correr SQL antes de probar** — `src/main/resources/static/migration_favoritos_resenas.sql`
(crea `favorito` y `resena`, ver sección de reseñas abajo). Correr en dev/qa/prod según se vaya
subiendo cada ambiente.

**Todo bajo `/v1/favoritos/**` requiere estar logueado.** Además, el usuario logueado necesita
tener un `Cliente` asociado (no basta con tener cuenta de `Usuario`) — si el registro no se
completó, cualquier llamada regresa `400` con `"Tu cuenta todavia no tiene un perfil de cliente
completo"`. Es el mismo caso ya documentado para otros flujos de "datosCompletos".

### 1. Agregar a favoritos

```
POST /v1/favoritos/{varianteId}
Authorization: Bearer <token>
```
Sin body. Si ya estaba en favoritos, no truena ni duplica — simplemente no hace nada (idempotente).

**Response 200:**
```json
{ "mensaje": "Agregado a favoritos", "code": 200, "data": "Agregado a favoritos" }
```
**Response 400:** `"No existe la variante con id: {id}"` si el id no existe.

### 2. Quitar de favoritos

```
DELETE /v1/favoritos/{varianteId}
Authorization: Bearer <token>
```
Idempotente también — si no estaba en favoritos, no truena.

### 3. Listar mis favoritos (paginado, con datos completos de la variante)

```
GET /v1/favoritos?pagina=1&size=10
Authorization: Bearer <token>
```

**Response 200** — mismo `VarianteResumenDto` que ya usa `/variantes/v1/buscar`, ordenado por fecha
en que se agregó (más reciente primero):
```json
{
  "data": {
    "pagina": 1, "totalPaginas": 1, "totalRegistros": 3,
    "t": [
      { "id": 12, "talla": "M", "color": "Azul", "stock": 8, "marca": "Levi's",
        "imagenUrl": "...", "precio": 300.00, "codigoBarras": "GLPD-066",
        "nombreProducto": "Jean Slim", "habilitado": "1" }
    ]
  }
}
```

### 4. Solo los IDs (para marcar el corazón en el catálogo sin pedir todo el objeto)

```
GET /v1/favoritos/ids
Authorization: Bearer <token>
```

**Response 200:**
```json
{ "data": [12, 45, 89] }
```
**Uso sugerido:** al entrar a cualquier pantalla de catálogo, pedir esta lista una vez y guardarla
en memoria del front; comparar cada `varianteId` visible contra este array para pintar el corazón
lleno/vacío, en vez de preguntarle al back "¿es favorito?" variante por variante.

**Archivos back:** `Favorito.java` (entidad nueva), `IFavoritoRepository.java`,
`FavoritoServiceImpl.java`, `FavoritoController.java`, `resumenPorIds()` agregado a
`VarianteServiceImpl.java` (reutiliza el armado de imágenes/precio que ya usa `/buscar`).

---

## 🆕 Reseñas y calificaciones (2026-07-13)

**Tercera de las 3 mejoras.** Tabla nueva `resena`. **Mismo SQL que favoritos** (arriba) — un solo
archivo crea las 2 tablas.

**Regla de negocio clave: solo se puede reseñar lo que ya se compró.** El back valida que exista un
registro de venta real (`detalle_venta_variantes`, mismas tablas que usa el reporte de ventas) del
cliente logueado para esa variante — no basta con tenerla en el carrito ni con un pedido sin pagar.
Si no compró, `POST` regresa `400` con `"Solo puedes resenar productos que hayas comprado"`.

**Moderación: publicación inmediata, sin cola de aprobación.** La reseña se ve en el catálogo en
cuanto se crea. El dueño puede editarla o borrarla cuando quiera; un ADMIN puede borrar cualquier
reseña (mismo endpoint `DELETE`, el back decide el permiso según quién llama) — es la forma de
quitar contenido inapropiado, no hay pantalla de "pendientes por aprobar". Si más adelante
prefieren aprobación previa en vez de esto, avisen antes de que el front dependa de que todo se
publique al instante.

**Un cliente = una reseña por variante** (no puede dejar 5 reseñas del mismo producto) — para
cambiar de opinión usa `PUT` (editar), no crear otra.

### 1. Crear reseña

```
POST /v1/resenas
Authorization: Bearer <token>
Content-Type: application/json

{ "varianteId": 12, "calificacion": 5, "comentario": "Me encantó, talla exacta" }
```
`comentario` es opcional (puede ir `null` o vacío, solo calificación). `calificacion` es
obligatorio, entero 1-5.

**Response 200:**
```json
{
  "data": {
    "id": 34,
    "varianteId": 12,
    "calificacion": 5,
    "comentario": "Me encantó, talla exacta",
    "fechaCreacion": "2026-07-13T18:40:00",
    "nombreCliente": "Ana G.",
    "esPropia": true
  }
}
```
`nombreCliente` ya viene recortado a nombre + inicial del apellido paterno (privacidad) — no
mandar el nombre completo del cliente en ningún lado del front para esto.

**Response 400:**
- `"La calificacion debe ser un numero entre 1 y 5"`
- `"Solo puedes resenar productos que hayas comprado"`
- `"Ya dejaste una resena para este producto, puedes editarla en vez de crear otra"`
- `"Tu cuenta todavia no tiene un perfil de cliente completo"`

### 2. Editar mi reseña

```
PUT /v1/resenas/{id}
Authorization: Bearer <token>
Content-Type: application/json

{ "calificacion": 4, "comentario": "Actualizo: la talla me quedó algo grande" }
```
Solo el dueño puede editar la suya — `400` con `"No puedes editar la resena de otro cliente"` si
se intenta con el id de otro. `varianteId` no se manda (no se puede reasignar una reseña a otra
variante).

**Response 200:** mismo shape que crear.

### 3. Eliminar reseña

```
DELETE /v1/resenas/{id}
Authorization: Bearer <token>
```
El dueño borra la suya. Un ADMIN puede borrar cualquiera (moderación) — mismo endpoint, el back
distingue por rol. Si un cliente normal intenta borrar la de otro: `400` con `"No puedes eliminar
la resena de otro cliente"`.

**Response 200:**
```json
{ "mensaje": "Resena eliminada", "code": 200, "data": "Resena eliminada" }
```

### 4. Listar reseñas de un producto (pública, no requiere login)

```
GET /v1/resenas/variante/{varianteId}?pagina=1&size=10
```
Sin `Authorization`, funciona igual — pero si se manda el token, cada reseña trae `esPropia: true`
en la que corresponde al usuario logueado (para mostrarle botones de editar/borrar solo en esa).
Sin token, todas vienen con `esPropia: false`.

**Response 200:**
```json
{
  "data": {
    "pagina": 1, "totalPaginas": 1, "totalRegistros": 2,
    "t": [
      { "id": 34, "varianteId": 12, "calificacion": 5, "comentario": "Me encantó, talla exacta",
        "fechaCreacion": "2026-07-13T18:40:00", "nombreCliente": "Ana G.", "esPropia": true },
      { "id": 31, "varianteId": 12, "calificacion": 4, "comentario": null,
        "fechaCreacion": "2026-07-10T12:00:00", "nombreCliente": "Luis M.", "esPropia": false }
    ]
  }
}
```
Ordenado por más reciente primero.

### 5. Resumen — promedio y conteo por estrella (para la ficha del producto)

```
GET /v1/resenas/variante/{varianteId}/resumen
```
Pública, sin parámetros de paginación (es un solo objeto).

**Response 200:**
```json
{
  "data": {
    "varianteId": 12,
    "promedio": 4.5,
    "totalResenas": 2,
    "conteoPorEstrella": { "1": 0, "2": 0, "3": 0, "4": 1, "5": 1 }
  }
}
```
`conteoPorEstrella` siempre trae las 5 llaves (1 a 5) aunque no haya reseñas de esa calificación —
no hay que validar `undefined` en el front, si no hay ninguna la clave existe con valor `0`.
`promedio` viene `0.0` (no `null`) cuando `totalResenas` es `0` — mostrar el estado "sin reseñas
todavía" cuando `totalResenas === 0`, no cuando `promedio === 0`.

**Sugerencia de flujo:** llamar a `/resumen` al cargar la ficha del producto (para las estrellitas
junto al precio) y a `/variante/{id}` (sin `/resumen`) solo cuando el usuario abre la sección de
reseñas completa — son 2 llamadas separadas a propósito, para no traer todos los comentarios si
solo se va a mostrar el promedio.

### 6. Mis reseñas (requiere login)

```
GET /v1/resenas/mis-resenas?pagina=1&size=10
Authorization: Bearer <token>
```
Mismo shape que el listado por variante, pero solo las del cliente logueado, de cualquier
producto. Útil para una pantalla "Mis reseñas" en el perfil del cliente.

**Archivos back:** `Resena.java` (entidad nueva), `IResenaRepository.java`, DTOs en
`models/resenas/` (`ResenaRequestDto`, `ResenaEditarDto`, `ResenaResponseDto`, `ResenaResumenDto`),
`ResenaServiceImpl.java`, `ResenaController.java`. También se agregó
`existsByVariante_IdAndVenta_Cliente_Id` a `IDetalleVentaVarianteRepository.java` (valida la
compra) y `currentUsuarioOpt()` a `AuthenticationUtils.java` (para que el listado público sepa
"es mío" sin reventar cuando no hay token).

**⏳ Pendiente:** correr `migration_favoritos_resenas.sql` en el ambiente que corresponda antes de
probar, y subir de `dev` a `qa`.

---

## 🐛 Fix (2026-07-13): búsqueda por código de barras era EXACTA, no parcial — reportado por el usuario

**Síntoma reportado:** buscar `glpd` en el buscador de productos no traía nada, aunque existe el
producto "Mochila Prada" con código de barras `GLPD-066`. En variantes pasaba lo mismo con el
buscador normal (`/variantes/v1/buscar`, usado también dentro del buscador de variantes de
"Gestión Promociones") — pero el filtro admin "con stock" **sí** encontraba las variantes, aunque
según el reporte "la promoción decía que no había productos" cuando en realidad el stock existía
(1 de cada variante).

**Causa raíz (una sola, repetida en 3 lugares):** el "paso 1" del buscador (código de barras) en
`ProductosServiceImpl.findNombreOrCodigoBarra` y en `VarianteServiceImpl.buscarPorCodigoBarrasPaginado`
(camino ADMIN) usaba métodos de Spring Data con **coincidencia EXACTA** (`= :codigoBarras`, o el
derived method `findByProductoCodigoBarrasCodigoBarras` sin `Containing`) en vez de `LIKE
%texto%`. Es decir: escribir `glpd` nunca iba a encontrar `GLPD-066` porque no son *iguales*, solo
un texto que *contiene* al otro. Solo el nombre (paso 3) ya usaba `LIKE`, pero como "glpd" tampoco
está en el nombre "Mochila Prada", tampoco aparecía por ahí — de ahí que pareciera que la búsqueda
completa no funcionaba, cuando en realidad solo fallaba el primer paso (código) sin caer
correctamente a nada más.

**Por qué el filtro "con stock" (`/variantes/v1/admin/filtrar` y el equivalente de productos) sí
funcionaba:** esos endpoints usan una query distinta (`buscarVariantesAdmin` / `buscarProductosAdmin`)
que **siempre** fue `LIKE` — nunca tuvieron el bug. Por eso la variante con stock=1 sí aparecía ahí
pero no en el buscador normal ni en el buscador de "Gestión Promociones" (que reutiliza
`/variantes/v1/buscar`): dos implementaciones de "buscar" con comportamiento distinto para el mismo
caso de uso.

**Fix aplicado:** el paso 1 (código de barras) de ambos buscadores ahora usa `LIKE
%texto%` igual que el paso 3 (nombre) y que los filtros admin — un código de barras completo
(escaneado) sigue encontrando el match exacto igual que antes (`LIKE '%GLPD-066%'` también es
`true` para el texto exacto), pero ahora **además** funciona escribir solo una parte.

**No cambia el contrato** (mismos endpoints, mismo shape de response) — cambia el comportamiento:
ahora estos 2 endpoints pueden regresar **más de un resultado** cuando antes el "paso 1" solo podía
regresar 0 o exactamente 1 (coincidencia exacta). Si el front tenía lógica que asumía "si
encontró por código, es un solo producto", hay que revisarla — ahora es una lista paginada normal
como los otros pasos.

**Archivos:** `IProductosRepository.java` (`findByCodigoBarrasContainingAdmin`,
`findByCodigoBarrasPublicoContaining` nuevos), `ProductosServiceImpl.java`
(`findNombreOrCodigoBarra`), `IVarianteRepository.java`
(`findByProductoCodigoBarrasCodigoBarrasContainingIgnoreCase` nuevo), `VarianteServiceImpl.java`
(`buscarPorCodigoBarrasPaginado`). Los métodos de coincidencia exacta **no se tocaron** — siguen
existiendo y se usan a propósito en otros lugares (validar duplicados al guardar, escaneo de
código de barras en venta directa) donde sí se necesita exacto, no parcial.

**⚠️ Importante para probar el fix:** estos buscadores están cacheados (`@Cacheable`,
`buscarNombreOrCodigoBarrasCache` / `variantesCodigoBarrasCache`). Si ya buscaste `glpd` antes del
fix y quedó un resultado vacío en caché, puede que sigas viendo "sin resultados" hasta que se
limpie. Limpiar con `DELETE /v1/admin/cache` (ADMIN) después de desplegar, antes de volver a
probar.

---

## 🆕 Reclamo de venta de mostrador — para que el cliente aparezca en la rifa (2026-07-13)

**Problema que resuelve:** un cliente compra en mostrador pero, por cualquier razón, la venta se
registra con `ClienteSinRegistro` en vez de con su cuenta real. Esa venta sí genera un Pedido por
detrás (todo venta directa normal crea uno), pero queda ligado al registro "sin cuenta", no al
cliente real — por lo que no se puede vincular su historial de compras a su perfil. Ahora el
cliente puede "reclamar" esa venta desde la app y quedar vinculado.

**Flujo:**
1. Al guardar una venta directa (`POST /v1/ventas/save`) con `clienteSinRegistroDto` que trae
   `correo_Electronico`, el backend genera un código UUID y lo **envía por correo** a esa
   dirección (asunto "Reclama tu compra — Novedades Jade"). No se expone en la respuesta del save.
2. El cliente inicia sesión en la app y captura ese código.

### `POST /v1/ventas/reclamar` — NUEVO, requiere estar autenticado (cualquier cliente, no ADMIN)
**Request:**
```json
{ "codigo": "3fa85f64-5717-4562-b3fc-2c963f66afa6" }
```
**Response 200:**
```json
{ "data": "Compra vinculada a tu cuenta" }
```

**Errores (400, `mensaje`):**
| `mensaje` | Causa |
|---|---|
| `Debes indicar el código` | body sin `codigo` o vacío |
| `Código inválido` | no existe ninguna venta con ese `codigoReclamo` |
| `Este código ya fue utilizado` | esa venta ya fue reclamada antes (un código solo sirve **una vez**) |
| `Este código ya expiró: solo es válido durante el mes de tu compra` | **NUEVO** — el código solo se puede usar dentro del **mes calendario** de la venta, no son N días desde la compra: si la venta fue el 29 de enero, el código expira el 31 de enero a las 23:59:59, igual que si hubiera sido el 1 de enero. Al llegar el 1 de febrero ya no sirve, aunque nunca se haya usado. (Este límite **no aplica** al fallback de asignación manual del admin — sección siguiente — que no tiene vencimiento.) |
| `Tu cuenta todavía no tiene un perfil de cliente completo` | el usuario logueado no tiene `Cliente` asociado |
| `El correo de tu cuenta no coincide con el de esta compra` | el correo de la cuenta logueada no es el mismo al que se envió el código (capa extra de seguridad — evita reclamar con una cuenta distinta si el código se reenvía) |

**Qué cambia en el backend al reclamar:** la `Venta.cliente` y el `Pedido.cliente` que la
respalda quedan asignados al cliente autenticado (antes solo tenían `clienteSinRegistro`). Esto
es lo que hace que, al armar una rifa con `GET /v1/concursante/clientesPorMes?mes=YYYY-MM`, ese
cliente aparezca en la lista de compradores de ese mes — esa consulta lee de `pedidos`, no de
`ventas`, por eso se propaga también al pedido. La rifa a la que puede entrar queda **limitada al
mes de la venta**: si compró en enero, ese cliente solo aparece en `clientesPorMes?mes=2026-01`,
no en meses posteriores.

### `POST /v1/ventas/{ventaId}/asignarCliente` — NUEVO, solo ADMIN

Fallback para cuando el cliente nunca captura el UUID que le llegó por correo (se fue a spam, no
quiso loguearse, etc.). El admin busca al cliente real y lo vincula manualmente a la venta, sin
necesitar el código — el admin ya validó identidad al elegir al cliente en el buscador.

**Request:**
```json
{ "clienteId": 123 }
```
**Response 200:**
```json
{ "data": "Cliente vinculado a la venta" }
```

**Errores (400, `mensaje`):**
| `mensaje` | Causa |
|---|---|
| `Venta no encontrada` | `ventaId` no existe |
| `Esta venta ya tiene un cliente asignado` | la venta ya tiene `cliente_id` (ya sea porque se vendió con cliente real desde el inicio, o porque ya fue reclamada/asignada antes) — no se puede reasignar por este medio |
| `Cliente no encontrado` | `clienteId` no existe |

Tiene el mismo efecto que el auto-reclamo del cliente: vincula `Venta.cliente` **y**
`Pedido.cliente`, para que aparezca en `clientesPorMes` del mes correspondiente.

### ⚠️ Naming — no usar la palabra "reclamo" de cara al cliente

El endpoint y los métodos internos se llaman `reclamar`/`reclamo` porque es el término técnico
más corto, pero **en la UI del cliente no debe aparecer esa palabra** — en español "reclamo" se
lee como queja/reclamación, no como "esta compra es mía, agrégala a mi cuenta". El correo que ya
se envía usa el texto "Agregar mi compra"; el front debe seguir esa misma línea:

| Elemento | Texto sugerido |
|---|---|
| Nombre de la pantalla/opción de menú | "Agregar mi compra" |
| Botón de acción | "Agregar compra" |
| Campo de captura | "Código de tu compra" (el UUID que llegó por correo) |
| Mensaje de éxito | "Tu compra quedó agregada a tu cuenta" |
| Error: código ya usado | "Este código ya fue usado" |
| Error: código inválido | "No encontramos ese código, revisa que esté bien copiado" |
| Error: correo no coincide | "Este código pertenece a otra cuenta" |

### 📝 Flujo de UI sugerido (pendiente — para cuando se construya el front)

Esto **todavía no está construido en el front**, queda anotado aquí para retomarlo:

1. **Dónde vive:** dentro de "Mi cuenta" / perfil del cliente, como una opción más de menú
   ("Agregar mi compra"), no como una pantalla que se muestre sola — el cliente entra ahí solo
   cuando recibió el correo y decide capturar el código.
2. **Pantalla:** un solo campo de texto para pegar/escribir el código + botón "Agregar compra".
   No hace falta mostrar nada más (ni monto, ni productos) porque el backend no expone el detalle
   de la venta en este endpoint, solo confirma o rechaza.
3. **Alternativa a evaluar más adelante:** en vez de que el cliente tenga que ir a buscar la
   opción en el menú, se le podría notificar dentro de la app (banner/notificación push) cuando
   detecte que tiene un correo de este tipo pendiente — no implementado, es solo una idea a
   futuro, no bloquea el llegar a construir la versión simple del punto 2.
4. **Después de agregar exitosamente:** no hay nada más que mostrarle al cliente en el momento
   (no ve si ganó o no rifa, eso es otro flujo, del admin) — solo el mensaje de éxito.

---

## 🆕 Carga rápida de imágenes — producto + variante borrador por foto (2026-07-20)

**Problema que resuelve:** hoy, para dar de alta un producto hay que llenar todo el formulario
(nombre, precio, código de barras, etc.) **y** subir la imagen en el mismo guardado. Si se
tarda llenando el formulario y el token expira, o cualquier otra cosa falla a la mitad, se pierde
todo — incluida la imagen que ya se tenía lista. Este flujo nuevo separa las dos cosas: **primero
se sube la imagen y el backend crea automáticamente un producto+variante "borrador"** (solo con
stock=1, sin nombre/precio/nada más), y **después** se va llenando ese borrador campo por campo,
tantas veces como haga falta, sin volver a tocar la imagen ni arriesgar perderla.

Pensado para una pantalla de captura en lote: el usuario va tomando/seleccionando fotos una tras
otra, cada una dispara su propio producto borrador en el backend, y luego el usuario entra a cada
uno (o a una lista de "borradores pendientes de llenar") para completarlo con calma.

**No hay una tabla/entidad nueva de seguimiento.** El estado de la imagen vive directo en la
fila de `producto` (columnas `estado_imagen` / `mensaje_error_imagen`) — el producto y la
variante se crean **de inmediato**, sincrónico, apenas se llama al endpoint; lo único que corre
en segundo plano es la subida de la imagen al microservicio de imágenes (la parte lenta de red).
Por eso el front recibe `productoId`/`varianteId` reales desde la primera respuesta, no un id de
seguimiento aparte.

**⚠️ Requiere correr `migration_carga_imagenes.sql` (carpeta `src/main/resources/static/`) antes
de usar estos endpoints** — agrega a `producto` las columnas `codigo_barras_generado`,
`estado_imagen` y `mensaje_error_imagen`. No se ejecuta solo (ddl-auto: none), hay que correrlo a
mano en cada BD.

**Todos los endpoints de esta sección requieren rol ADMIN — y la pantalla también.** No es
una pantalla de cliente: es una herramienta de captura para quien da de alta el catálogo. El
backend ya rechaza estos endpoints con 403 si el usuario no es ADMIN, pero **eso no basta**: el
front debe además ocultar/bloquear la ruta y el ítem de menú para usuarios no-admin, igual que ya
hace con el resto del panel de productos/variantes (mismo guard de ruta que usan `/productos` y
`/variantes` hoy). Que el backend rechace la llamada no debe ser la única barrera — si un cliente
normal llega a ver el botón o la ruta, es un bug de UX aunque el request final falle igual.

**Dónde verlo:** Menú (panel admin) → **Productos** → nueva opción **Carga rápida de imágenes**
(o como se llame en el menú actual de Productos/Variantes) → pantalla de captura en lote descrita
en la sección "Notas para la pantalla nueva" más abajo.

### 1. Subir una imagen → crea el borrador YA

```
POST /v1/carga-imagenes/subir-imagen
Content-Type: multipart/form-data
```
| Parte | Tipo | Notas |
|---|---|---|
| `imagen` | file | Una sola imagen por request. Si el usuario selecciona 10 fotos, el front hace 10 requests (uno por foto), no un solo request con 10 archivos. |

**Sí soporta subir muchas imágenes seguidas** — el front puede disparar todos los
`POST /subir-imagen` de la sesión de captura sin esperar uno a uno (cada llamada responde casi de
inmediato porque solo crea el producto+variante; la subida real de la imagen sigue en segundo
plano). En el backend, esas subidas en segundo plano corren en un pool acotado (máx. 6 en
paralelo, el resto se encola) para no saturar el servidor ni bombardear al microservicio de
imágenes si se mandan 50-100 fotos de un jalón — no hace falta que el front limite cuántas manda
a la vez, el backend ya absorbe eso.

**Response 201** — el producto y la variante YA existen en la base al recibir esta respuesta:
```json
{
  "data": {
    "productoId": 812,
    "varianteId": 1503,
    "estadoImagen": "PENDIENTE",
    "imagenId": null,
    "urlImagen": null,
    "mensajeErrorImagen": null
  }
}
```

Lo único que sigue en `PENDIENTE` es la imagen (se está subiendo al microservicio de imágenes en
segundo plano, para no dejar la pantalla congelada si el usuario sube muchas fotos seguidas).
Guarda `productoId` — es lo que se usa para preguntar el estado, completar el producto o
reintentar la imagen si falla.

### 2. Consultar el estado de una o varias imágenes (polling)

```
GET /v1/carga-imagenes/estado?productoIds=812&productoIds=813
```
(o `?productoIds=812,813` — Spring acepta ambas formas)

**Response 200** — un elemento por cada `productoId` consultado:
```json
{
  "data": [
    {
      "productoId": 812,
      "varianteId": 1503,
      "estadoImagen": "EXITOSO",
      "imagenId": 9041,
      "urlImagen": "https://.../v1/imagenes/file/9041",
      "mensajeErrorImagen": null
    },
    {
      "productoId": 813,
      "varianteId": 1504,
      "estadoImagen": "FALLIDO",
      "imagenId": null,
      "urlImagen": null,
      "mensajeErrorImagen": "No se pudo subir la imagen al servicio de imagenes, intenta de nuevo"
    }
  ]
}
```

**Valores de `estadoImagen`:**
| Estado | Qué significa | Qué hacer en el front |
|---|---|---|
| `PENDIENTE` | La imagen todavía se está subiendo | Seguir preguntando (ej. cada 2-3 segundos) hasta que cambie |
| `EXITOSO` | Imagen enlazada al producto+variante | Mostrar la miniatura (`urlImagen`) y dejar que el usuario entre a completar el producto (`productoId`) |
| `FALLIDO` | La subida de la imagen falló | Mostrar el error y dar opción de **reintentar con otra/la misma foto** (ver punto 3) |

**Cómo saber que una imagen ya terminó (y dejar de preguntar por ella):** el propio
`estadoImagen` de la respuesta es la señal — mientras es `PENDIENTE` sigue en proceso; en cuanto
la respuesta trae `EXITOSO` o `FALLIDO` para ese `productoId`, ya terminó (bien o mal) y no hace
falta volver a consultarlo. El front debe llevar un set/array de "`productoId` todavía
pendientes" e ir sacando de ahí cada uno que deje de venir en `PENDIENTE`, guardando el resultado
(`urlImagen` si fue `EXITOSO`, `mensajeErrorImagen` si fue `FALLIDO`) para pintarlo en la grilla.
Cuando el set de pendientes queda vacío, se detiene el `setInterval`/polling por completo —no
hay que seguir llamando a `GET /estado` de fondo indefinidamente.

Pseudocódigo del loop:
```js
let pendientes = new Set(idsSubidos); // todos los productoId que se acaban de subir
const resultados = new Map();

const intervalo = setInterval(async () => {
  if (pendientes.size === 0) { clearInterval(intervalo); return; }

  const { data } = await getEstado([...pendientes]);
  for (const item of data) {
    if (item.estadoImagen !== 'PENDIENTE') {
      resultados.set(item.productoId, item); // EXITOSO o FALLIDO: ya terminó
      pendientes.delete(item.productoId);     // se omite en la siguiente consulta
      actualizarTarjetaEnLaGrilla(item);
    }
  }
}, 2500);
```

**Sugerencia de flujo general:** el front dispara todas las subidas de la sesión de captura,
guarda la lista de `productoId` en memoria (el set `pendientes` de arriba), y hace polling de
`GET /estado` solo con los IDs que sigan pendientes cada pocos segundos, hasta vaciar el set.

### 3. Reintentar la imagen de un borrador que falló

```
POST /v1/carga-imagenes/{productoId}/reintentar-imagen
Content-Type: multipart/form-data
```
Mismo `imagen` como parte del form. A diferencia de subir una foto nueva, esto **reutiliza el
mismo producto y variante** que ya existían (no crea un borrador duplicado) — pone
`estadoImagen` de vuelta en `PENDIENTE` y vuelve a intentar la subida. Responde 202 con el mismo
shape que el punto 2.

### 4. Ver borradores con imagen fallida (por si el front perdió la lista de `productoId`)

```
GET /v1/carga-imagenes/fallidas
```
Sin parámetros. Devuelve todos los productos con `estadoImagen = FALLIDO`, más recientes primero
— mismo shape que el punto 2. Pensado como red de seguridad: si el usuario cierra la app/recarga
la página antes de que terminara el polling y perdió la lista de `productoId` en memoria, esta
pantalla le permite ver "qué se quedó pendiente" sin tener que adivinar cuáles fotos sí entraron.

### 5. Completar el producto borrador (ir llenando campos de a poco)

```
PUT /v1/carga-imagenes/{productoId}/completar
```
**Request** — todos los campos son opcionales, manda solo lo que el usuario ya llenó en ese
momento (cada campo no nulo pisa el valor actual; no hace falta reenviar el objeto completo cada
vez):
```json
{
  "nombre": "Pantalón de mezclilla slim",
  "precioCosto": 250.0,
  "piezas": 1,
  "color": "Azul",
  "precioVenta": 450.0,
  "precioRebaja": null,
  "descripcion": "...",
  "marca": "Levi's",
  "contenido": null,
  "palabraClaveId": 4,
  "codigoBarras": null,
  "habilitar": false
}
```

**Response 200:** el `Producto` actualizado (entidad completa, incluye `id`, `stock`, etc.).

**Sobre `codigoBarras`:** el producto borrador nace con un código de barras **temporal
autogenerado** (formato `BRD-XXXXXXXXXXXX`), invisible para el usuario — es solo para que el
producto sea válido en la base de datos mientras no tiene el código real. **No mostrar ni dejar
editar ese código placeholder en el front.** Cuando el usuario finalmente escanee/capture el
código de barras real, mándalo en `codigoBarras` en esta misma llamada: el backend detecta que el
producto todavía tenía el código autogenerado, crea el código real, lo asigna, **y borra el
placeholder anterior** — no hay que hacer nada extra desde el front para esa limpieza. Si el
código de barras ya pertenece a otro producto, responde 400 con `mensaje`:
`"El codigo de barras <código> ya esta en uso por otro producto"`.

**Sobre `habilitar`:** el producto borrador nace **deshabilitado** (`habilitado='0'`) para que no
aparezca roto en el catálogo público mientras le faltan datos — un producto con imagen y stock
pero sin nombre ni precio no debe ser visible a un cliente. Manda `"habilitar": true` en esta
misma llamada cuando el producto ya esté completo y listo para publicarse. El backend **rechaza**
habilitarlo en dos casos (400 con `mensaje` explicando cuál):
- Todavía tiene el código de barras autogenerado (no mandaste `codigoBarras` real todavía, o lo
  mandaste en esta misma llamada — en ese caso sí se puede, se procesa el código antes de validar).
- La imagen no quedó en `estadoImagen = EXITOSO` (sigue `PENDIENTE` o quedó `FALLIDO`) — no se
  puede publicar un producto sin imagen funcionando.

Si el front prefiere separar "guardar" de "publicar", también puede usar el endpoint que ya
existía, `PUT /v1/productos/{id}/habilitar?habilitar=true`, una vez que el código de barras ya
sea el real y la imagen esté en `EXITOSO`.

**Por qué es un endpoint nuevo y no el `PUT /v1/productos/update` de siempre:** ese endpoint
existente funciona por "upsert vía código de barras" (busca si ya existe un producto con ese
código para decidir si crea o actualiza) y tiene una rama vieja pensada para lotes que, si el
producto no tiene un código de barras "normal" todavía, puede terminar ignorando nombre/color/
descripción o creando registros de lote inesperados. Este endpoint nuevo actualiza **directo por
`productoId`**, sin esa lógica de por medio — está pensado específicamente para ir completando un
borrador campo por campo sin sorpresas. No reemplaza `/v1/productos/update`, que sigue siendo el
que se usa para el alta/edición normal de productos que ya nacen con su código de barras real.

### Notas para la pantalla nueva (sugerencia de flujo)

1. Pantalla de captura: el usuario selecciona/toma varias fotos. Cada una dispara
   `POST /subir-imagen` de inmediato (no espera a que el usuario termine de elegir todas).
2. El front muestra una grilla con las fotos y su estado (spinner mientras `PENDIENTE`, miniatura
   cuando `EXITOSO`, ícono de error + botón "reintentar" cuando `FALLIDO`, que llama a
   `POST /{productoId}/reintentar-imagen`), haciendo polling de `GET /estado` con todos los
   `productoId` de la sesión.
3. Al tocar una foto ya `EXITOSO`, se abre el formulario normal de edición de producto
   (`productoId`), pero guardando con `PUT /{productoId}/completar` en vez del guardado de
   siempre — así cada campo que el usuario llena se persiste al momento (por ejemplo al perder el
   foco de cada input, o con un botón "Guardar avance"), sin esperar a que el formulario esté
   100% lleno.
4. Cuando el usuario termina de llenar todo y ya tiene el código de barras real, el último
   guardado manda `codigoBarras` + `"habilitar": true` para publicar el producto.
5. Los productos borrador que se queden sin terminar quedan deshabilitados y no contaminan el
   catálogo — se pueden encontrar más tarde vía `GET /v1/productos/admin/no-habilitados` (ya
   existía) para retomarlos.

**Archivos back:** `CargaImagenesController.java`, `CargaImagenesServiceImpl.java`,
`ICargaImagenService.java` (reescritos), `EstadoCargaProductoDto.java` / `CompletarProductoDto.java`
(dtos nuevos), `Producto.java` (campos `codigoBarrasGenerado`, `estadoImagen`,
`mensajeErrorImagen`), `IProductosRepository.java` / `IVarianteRepository.java` (queries nuevas de
soporte), `migration_carga_imagenes.sql` (nuevo, pendiente de correr en dev/qa).

**🐛 Bug corregido (2026-07-21):** al probar en QA, las fotos fallaban con
`Column 'nombre' cannot be null` — la tabla `producto` tiene varias columnas `NOT NULL`
preexistentes (`nombre`, `precio_costo`, `piezas`, `precio_venta`, `precio_rebaja`; la migración
de este flujo no las tocó) y `crearBorrador()` las dejaba en null porque el borrador nace
intencionalmente vacío. Fix: el borrador ahora nace con placeholders — strings vacíos (`nombre`,
`color` = `""`) y ceros (`precioCosto`, `piezas`, `precioVenta`, `precioRebaja` = `0`) — que se
pisan en cuanto el front manda los datos reales via `PUT /completar`. **No cambia el contrato** —
el front no tiene que hacer nada distinto; solo tener en cuenta que un borrador recién creado
trae `nombre` vacío y precios en `0` (no null) si llega a pintarlos antes de completarlo.

**🐛 Bug corregido #2 (2026-07-21):** ya con los borradores creándose bien, la subida de la
imagen quedaba en `FALLIDO` con `403 Forbidden from POST .../v1/imagenes` — el backend le pasa
el JWT del admin al microservicio de imágenes leyéndolo del contexto de seguridad del hilo del
request, pero la subida corre en un hilo del pool async, que no hereda ese contexto → la llamada
salía **sin** header `Authorization` y el micro la rechazaba con 403. Fix: el pool ahora propaga
el contexto de seguridad del request al hilo async (`DelegatingSecurityContextAsyncTaskExecutor`),
así la subida en segundo plano viaja con el mismo token del admin que subió la foto. **Sin cambio
de contrato** — el front no hace nada distinto; las fotos que quedaron `FALLIDO` por este 403 se
reintentan con `POST /{productoId}/reintentar-imagen` normalmente.

**🐛 Bug corregido #3 (2026-07-21):** el borrador recién creado no aparecía en
`GET /v1/productos/admin/no-habilitados` — ese listado está cacheado y crear el borrador no
limpiaba la caché (solo se limpiaba cuando la imagen terminaba de subir con éxito; si la subida
fallaba, el borrador no aparecía nunca hasta expirar la caché). Ahora crear el borrador también
evicta la caché, así el listado de no-habilitados lo refleja de inmediato. Recordatorio: **los
borradores no salen en el listado público/normal de productos** (nacen deshabilitados a
propósito) — para verlos son `admin/no-habilitados` o `GET /v1/carga-imagenes/estado`. Si hace
falta forzar la limpieza a mano en QA: `DELETE /v1/admin/cache` (ADMIN).

**⚠️ Aclaración importante para el front (2026-07-21): no usar `POST /v1/productos/save` ni
`PUT /v1/productos/update` para completar un borrador de carga rápida.** Se probó en QA editando
un producto borrador (nace con 1 stock, 1 variante y código autogenerado `BRD-XXXXXXXXXXXX`) desde
`POST /productos/save` mandando los datos reales + un código de barras nuevo. Resultado: **no
actualizó el borrador, creó un producto duplicado nuevo** con su propio `id` y su propio
`codigo_barras`, dejando el borrador original intacto (mismo código autogenerado, campos vacíos).

Motivo: `/productos/save` y `/productos/update` (ambos llaman al mismo `saveProductoLote()`) buscan
el producto a actualizar **por coincidencia exacta de código de barras**, nunca por `id` — es un
upsert vía código de barras, pensado para alta/edición de productos que ya nacen con su código real
(carga manual, Excel). Si mandas un código de barras que no existe todavía en la base (como el
código real de un borrador, que aún no está registrado), el backend concluye que es un producto
nuevo y lo crea, en vez de actualizar el borrador.

**Regla para el front:** si el producto que se está editando todavía tiene código autogenerado
(`codigoBarrasGenerado: true` en la respuesta de `GET /v1/carga-imagenes/estado` o del `Producto`
devuelto), el guardado de esa pantalla **siempre** debe ir a `PUT /v1/carga-imagenes/{productoId}/completar`
(ver punto 5 arriba) — nunca a `save`/`update`. Recién cuando el producto ya tiene su código real
asignado (`codigoBarrasGenerado: false`) se puede volver a editar con el flujo normal de
`save`/`update`.

---

**🐛 Bug corregido (2026-07-21): editar un producto normal (no borrador) cambiando su código de
barras creaba un duplicado en vez de actualizarlo.** Es el mismo problema del punto anterior pero
para la edición normal de catálogo (fuera del flujo de carga rápida): `POST /productos/save` y
`PUT /productos/update` buscaban el producto a actualizar **solo por coincidencia exacta de código
de barras**. Si el usuario editaba un producto y le cambiaba el código de barras junto con el resto
de los datos, el backend no encontraba ningún producto con ese código nuevo, concluía que era un
producto nuevo y lo creaba — dejando el producto original intacto, sin los cambios.

**Qué cambió:** ahora `guardarProducto()` primero busca el producto **por `id`** (si el front lo
manda en el body). Si lo encuentra y el código de barras viene distinto al que ya tenía, crea el
código de barras nuevo, lo asigna a ese mismo producto, y **elimina el código de barras anterior**
(la relación producto↔código de barras es 1 a 1 única, así que el anterior siempre queda huérfano,
nunca se pierde nada real). Si el código nuevo ya está en uso por otro producto, responde 400 con
`ExceptionDuplicado`. Si el front no manda `id`, se mantiene el comportamiento anterior (búsqueda
por código de barras) para no romper la carga por Excel.

**Importante para el front:** a partir de ahora, el body de `save`/`update` **debe incluir el
campo `id` del producto** cuando se está editando uno existente (antes se ignoraba). Sin `id`,
si se cambia el código de barras se sigue creando un producto duplicado como antes.

**Esto NO reemplaza el punto anterior sobre borradores de carga rápida.** Aunque técnicamente ya
no se duplicaría el producto, `save`/`update` **no** resetean `codigoBarrasGenerado` a `false` ni
validan el estado de la imagen — un borrador guardado por esta vía quedaría con el código real ya
asignado pero con `codigoBarrasGenerado: true` inconsistente (bloquea habilitar, ensucia el filtro
`codigoGenerado`). Para borradores sigue aplicando la regla de arriba: usar siempre
`PUT /v1/carga-imagenes/{productoId}/completar`.

**⏳ Pendiente:** probar el flujo end-to-end de nuevo en QA con el fix, y push a `qa`.


## ❓ CONSULTA AL BACK — falta endpoint para descartar un borrador de carga rápida (2026-07-21)

### Endpoints que usa hoy `/carga-imagenes` (todos en `carga-imagenes.service.ts`)

| Método | Endpoint | Para qué |
|---|---|---|
| `POST` | `/v1/carga-imagenes/subir-imagen` | Sube una foto → crea el producto+variante borrador |
| `GET` | `/v1/carga-imagenes/estado?productoIds=` | Polling del estado de la imagen (PENDIENTE/EXITOSO/FALLIDO) |
| `POST` | `/v1/carga-imagenes/{productoId}/reintentar-imagen` | Reintenta la imagen de un borrador que falló |
| `GET` | `/v1/carga-imagenes/fallidas` | Lista los borradores con imagen FALLIDA (sin paginación, sin filtro de fecha) |
| `PUT` | `/v1/carga-imagenes/{productoId}/completar` | Guarda los datos del producto (nombre, precio, código real, etc.) |

**No existe ningún endpoint de borrado** para esta pantalla — es la raíz del problema de abajo.

**Reportado por el admin:** subió varias fotos en `/carga-imagenes`; algunas quedaron listas para
"Completar datos" (`EXITOSO`) y otras fallaron (`FALLIDO`, con botón "Reintentar"). Le dio "✕" a
las fallidas esperando que desaparecieran para siempre. Al volver a entrar a la pantalla:
- Las que le dio "✕" **volvieron a aparecer** con el botón "Reintentar".
- Las que estaban listas para completar **ya no aparecen por ningún lado**.

**Diagnóstico (confirmado en el código del front, no es un bug de UI aislado — es que falta una
pieza del backend):**

1. El botón "✕" (`quitarTarjeta()` en `carga-imagenes.component.ts`) **nunca llama a ningún
   endpoint** — solo saca la tarjeta del array local en memoria. No existe ningún `DELETE` en
   `carga-imagenes.service.ts` para eliminar un borrador de verdad. Como el producto+variante
   sigue existiendo en BD con `estadoImagen: FALLIDO`, la próxima vez que se entra a la pantalla,
   `GET /v1/carga-imagenes/fallidas` lo vuelve a traer — por eso "siempre aparece".

2. `ngOnInit()` solo llama a `GET /v1/carga-imagenes/fallidas` como red de seguridad al recargar
   — **no existe ningún endpoint para recuperar los borradores que ya subieron bien pero aún no
   se completaron** (`EXITOSO`, sin `PUT /completar` todavía). Esas tarjetas solo viven en el
   estado del componente Angular mientras la pantalla sigue abierta; al navegar a otra ruta o
   recargar la página, se pierden de la vista — aunque el producto+variante sigue vivo en BD,
   deshabilitado, con su imagen ya subida.

**Lo que necesitamos del back — dos preguntas concretas:**

1. **¿Existe (o se puede agregar) un endpoint para descartar/eliminar por completo un borrador de
   carga rápida?** Pensado para cuando el admin decide que esa foto/producto no vale la pena
   completar (ej. imagen borrosa, producto repetido, foto de prueba). Algo tipo
   `DELETE /v1/carga-imagenes/{productoId}` que borre el producto, la variante y la imagen
   asociada (si ya se subió). Sin esto, el front no tiene forma de que el "✕" sea permanente.

2. **¿Cómo recupera el front, al recargar la pantalla, los borradores `EXITOSO` que aún no se
   completaron (no solo los `FALLIDO`)?** ¿Ya existe un filtro que sirva para esto en
   `GET /v1/productos/admin/filtrar` (ej. `codigoGenerado=true&habilitado=false`, sección de
   arriba) que el front pueda usar para repoblar TODA la lista de pendientes al entrar a
   `/carga-imagenes` (fallidos + exitosos-sin-completar), en vez de usar solo
   `GET /v1/carga-imagenes/fallidas`? O si hace falta un endpoint dedicado, avisar.

**Mientras no haya respuesta, el front seguirá con el gap:** los borradores fallidos "resucitan"
cada vez que se recarga la pantalla (aunque se hayan descartado con "✕"), y los borradores listos
para completar solo son visibles durante la misma sesión de captura en la que se subieron.

### Dudas adicionales, mismo tema (no bloquean, pero conviene resolverlas antes de que crezca el uso)

3. **`GET /fallidas` no tiene paginación ni filtro de fecha** — "devuelve todos los productos con
   `estadoImagen = FALLIDO`, más recientes primero", sin límite. Si nunca se implementa el borrado
   del punto 1, esta lista va a crecer indefinidamente con el uso real (pruebas, fotos borrosas,
   productos que ya no interesan) y cada vez que un admin entre a `/carga-imagenes` va a cargar
   más y más tarjetas viejas. ¿Conviene agregar paginación o un filtro `desde`/`hasta`, o el plan
   es que el borrado del punto 1 mantenga la lista corta de forma natural?

4. **¿Qué pasa con la imagen ya subida al microservicio de imágenes si el borrador se elimina?**
   Si se implementa el `DELETE` del punto 1, ¿también borra la imagen del micro de imágenes
   (9096), o solo el producto/variante en proyecto-key y la imagen queda huérfana allá?

5. **¿Hay límite de reintentos en `POST /{productoId}/reintentar-imagen`?** Si una imagen falla
   siempre (ej. archivo corrupto, formato no soportado), ¿el front debería dejar de ofrecer
   "Reintentar" después de N intentos, o el backend ya limita/rechaza en algún punto? Hoy el
   botón de reintentar no tiene tope, el admin puede darle indefinidamente.

6. **¿Debería haber una limpieza automática de borradores abandonados** (nunca completados,
   con semanas de antigüedad)? Con el tiempo estos van a acumularse en `admin/no-habilitados` y
   en el filtro `codigoGenerado=true` sin que nadie los complete ni los borre — un TTL o un job
   de limpieza periódico evitaría que esos listados se llenen de basura.

---

## ✅ RESPUESTA DEL BACK a la consulta de arriba (2026-07-21)

### 1. Nuevo endpoint: `DELETE /v1/carga-imagenes/{productoId}` — descarta el borrador para siempre

**Request:** `DELETE /v1/carga-imagenes/{productoId}` (sin body).

**Response 200:**
```json
{ "response": "Borrador eliminado correctamente" }
```

**Qué hace:** borra de verdad — no es el soft-delete de `deleteBy/{id}` de productos normales.
Elimina el producto, su(s) variante(s), las relaciones `producto_imagen`/`variante_imagen`, la
imagen (fila local + intenta borrarla también en el microservicio de imágenes — si esa llamada
falla, no bloquea el borrado, solo queda un log de warning) y el `codigo_barras` placeholder
(`BRD-...`). Ahora el botón "✕" del front puede llamar a este endpoint y la tarjeta no va a
"resucitar" nunca más.

**Seguridad — solo borra borradores de verdad:** si el producto ya tiene código de barras real
(`codigoBarrasGenerado: false`, o sea ya se le hizo `PUT /completar` con un código real), este
endpoint responde **400** y no borra nada — es a propósito, para que un uso accidental de este
botón no pueda borrar un producto real ya completado/habilitado. Mensaje de error en ese caso:
`"El producto {id} ya tiene codigo de barras real asignado, no se puede descartar como borrador..."`.

**404 / error:** si el `productoId` no existe, 400 con `"No existe el producto borrador con id: {id}"`.

### 2. Cómo recuperar TODOS los pendientes (fallidos + exitosos sin completar) al recargar

**No hace falta ningún endpoint nuevo — ya se puede armar con dos llamadas que ya existen:**

1. `GET /v1/productos/admin/filtrar?codigoGenerado=true&habilitado=false&size=100&page=1` — trae
   **todos** los productos que siguen siendo borrador (`codigoBarrasGenerado: true`), sin importar
   si su imagen quedó `PENDIENTE`, `EXITOSO` o `FALLIDO`. Ya está paginado (ver pregunta 3). De ahí
   sacas los `idProducto` de todos los borradores vivos.
2. `GET /v1/carga-imagenes/estado?productoIds=1,2,3,...` con esos IDs — devuelve el `estadoImagen`,
   `mensajeErrorImagen` y la URL de imagen de cada uno. Con ese campo el front arma los buckets:
   `FALLIDO` → tarjetas con botón "Reintentar", `EXITOSO` → tarjetas con botón "Completar datos",
   `PENDIENTE` → todavía subiendo.

**Aclaración importante:** el `ProductoDTO` que devuelve `admin/filtrar` **no** trae `estadoImagen`
ni `codigoBarrasGenerado` en el JSON (son campos internos de `Producto`, no están mapeados ahí) —
por eso hace falta el segundo llamado a `/estado` para clasificar las tarjetas. Si en algún momento
esto genera demasiadas llamadas o el front prefiere un único endpoint que ya traiga todo junto,
avisen y se agrega, pero con el volumen actual las dos llamadas combinadas ya resuelven el punto 2
sin cambios de backend.

**🐛 Caso real confirmado en QA (2026-07-21):** un admin subió una imagen en `/carga-imagenes`,
vio aparecer la tarjeta con "Completar registro" (`estadoImagen: EXITOSO`), navegó fuera de la
pantalla y volvió a entrar — esa tarjeta **ya no aparecía en ningún lado**, mientras que otras
tarjetas que sí habían fallado (`FALLIDO`) sí seguían visibles. Es justo el gap descrito arriba:
`ngOnInit()` solo vuelve a pedir `GET /fallidas`, nunca las `EXITOSO` sin completar.

**El producto NO se perdió** — se confirmó que sigue en la base, deshabilitado, con su imagen ya
subida correctamente. Mientras el front no implemente la solución del punto 2 (repoblar con
`admin/filtrar?codigoGenerado=true&habilitado=false` + `/estado` al entrar a la pantalla), la forma
de recuperar manualmente un borrador "perdido de vista" es:
```
GET /v1/productos/admin/filtrar?codigoGenerado=true&habilitado=false&size=50&page=1
```
(o `GET /v1/productos/admin/no-habilitados`) y de ahí tomar el `idProducto` para completarlo con
`PUT /v1/carga-imagenes/{productoId}/completar` como siempre. **Este es el mismo bug para todos los
admins, no algo puntual de una sesión** — va a repetirse cada vez que alguien recargue o navegue
fuera de `/carga-imagenes` después de subir una imagen exitosa, hasta que se implemente el punto 2.

### 3. Paginación en `/fallidas`

`GET /v1/carga-imagenes/fallidas` se queda **sin paginar por ahora** (no se le tocó nada). Pero con
la respuesta del punto 2, **recomendamos migrar el front para dejar de usarlo** y en su lugar usar
`admin/filtrar?codigoGenerado=true&habilitado=false` (que sí pagina con `size`/`page`) + `/estado`
para clasificar. Eso resuelve el crecimiento indefinido de la lista sin tener que tocar `/fallidas`.
Si prefieren seguir usando `/fallidas` tal cual, avisen y se le agrega paginación aparte.

### 4. Imagen en el microservicio al borrar un borrador

Sí — `DELETE /v1/carga-imagenes/{productoId}` (punto 1) también intenta borrar la imagen en el
microservicio de imágenes (puerto 9096), no solo el producto/variante local. Es un best-effort: si
el microservicio no responde, el borrado local igual se completa (para no dejar el borrador
atascado) y solo queda un warning en el log del back para revisar manualmente esa imagen huérfana.

### 5. Límite de reintentos en `reintentar-imagen`

Confirmado: **no hay límite** hoy en `POST /{productoId}/reintentar-imagen`, se puede reintentar
indefinidamente. No se agregó tope en esta sesión — si se quiere limitar (ej. deshabilitar el botón
en el front después de N intentos, o que el back rechace después de N), es una mejora aparte, avisen
si la priorizan.

### 6. Limpieza automática / TTL de borradores abandonados

No implementado todavía — queda como pendiente de backlog, no bloquea nada de lo de arriba. Con el
nuevo `DELETE /v1/carga-imagenes/{productoId}` (punto 1) al menos ya hay una forma manual de
limpiarlos desde el front; un job automático (ej. borrar borradores con más de N días sin completar)
se puede evaluar después si el volumen lo justifica.

---

## ✅ Confirmación del front (2026-07-22): ya implementados los 2 puntos de la respuesta de arriba

Con la respuesta de los puntos 1 y 2, se implementaron los dos cambios pendientes en
`/carga-imagenes`:

1. **El botón "✕" ahora llama a `DELETE /v1/carga-imagenes/{productoId}`** (con un Swal de
   confirmación antes, porque pasó de ser una acción cosmética a una permanente). Si el back
   responde `400` (producto ya con código real), se muestra ese mensaje en un Swal de error y la
   tarjeta se queda tal cual.
2. **Al entrar/recargar la pantalla, ya no se pierde de vista lo que quedó `EXITOSO` sin
   completar.** Se reemplazó `GET /fallidas` por el combo que confirmaron en el punto 2:
   `GET /v1/productos/admin/filtrar?codigoGenerado=true&habilitado=false&size=100&page=1` para
   traer todos los borradores vivos, y con esos `idProducto` un `GET /v1/carga-imagenes/estado`
   para clasificarlos por `estadoImagen` (arranca el polling si alguno sigue `PENDIENTE`).

Ya no se usa `GET /fallidas` en ningún lado del front — se quitó del servicio.

**Pendiente de nuestro lado:** probarlo en vivo contra el ambiente donde esté desplegado este fix
(dev/qa). Cualquier caso raro que salga en la prueba lo anotamos aquí mismo.

---

## ❓ CONSULTA AL BACK — mis-pedidos: cancelar sin afectar rifa, cobrar créditos, cliente sin registro duplicado (2026-07-22)

> Revisado el código actual de `mis-pedidos`, `detalle-pedido`, `venta-directa` y `variantes/carrito`
> antes de escribir esto, para no preguntar algo que ya está resuelto. Resultado: 3 puntos son
> pregunta real para el back, 2 ya están implementados (solo se anotan para que quede registro), y
> 2 son 100% front (se van a hacer sin esperar respuesta).

### 1. ❓ Cancelar un pedido por error del ADMIN sin afectar al cliente en la rifa

**Contexto:** el admin puede equivocarse al capturar un pedido (producto/cliente incorrecto). Hoy
la cancelación usa `DELETE /v1/pedidos/delete/{id}?motivo=...` con dos motivos ya soportados en el
front: `NO_SE_PRESENTO` y `CLIENTE_AVISO` (ambos implican que la falta fue del cliente).

**Pregunta:** ¿el `motivo` de cancelación tiene HOY algún efecto sobre la elegibilidad del cliente
en una rifa (ej. lo descarta, le resta boletos, cuenta como falta al importar participantes desde
`clientesPorMes`/`importarDePedidos`)? Si sí — necesitamos un motivo nuevo (ej. `ERROR_ADMIN` o
`ERROR_CAPTURA`) que el back trate como "no cuenta en contra del cliente", porque el error fue
nuestro al capturar, no del cliente.

**Endpoint usado hoy:** `DELETE /v1/pedidos/delete/{id}?motivo=...` (`PedidosService.cancelarConMotivo()`).

### 2. ❓ Cliente sin registro con el mismo nombre — ¿riesgo de confusión en la rifa?

**Contexto:** en `variantes/venta-directa`, "Agregar cliente sin registro" solo manda
`nombre_persona`, `apellido_paterno`, `correo_Electronico`, `numero_Telefonico` por cada venta —
sin ningún identificador único. Si dos personas DISTINTAS compran por separado y ambas quedan
registradas como, por ejemplo, "Raul" (sin correo/teléfono, o con datos parecidos):

**Pregunta:** ¿cómo distingue el back a estos dos "Raul" al importar participantes de una rifa
(`clientesPorMes` / `importarDePedidos`)? ¿Hay riesgo de que:
- se combinen los boletos de las dos personas en un solo participante, o
- se descarte a una de las dos por "duplicado" aunque sean personas diferentes?

Si el back matchea por nombre+apellido a secas, es un riesgo real con nombres comunes. Si ya usa
algún id de venta/pedido por participante, no hay problema — solo queremos confirmarlo antes de
que pase en una rifa real.

### 3. ❓ `GET /v1/pedidos/{id}/detalle` — ¿falla o devuelve vacío cuando un producto no tiene imagen?

**Reportado:** al abrir "Detalle" de un pedido, a veces se ve como si no tuviera productos ("No hay
productos en este pedido"), y se sospecha que pasa con pedidos que tienen algún producto sin
imagen.

**Ya revisamos el front:** la imagen individual de cada producto ya tiene fallback
(`<img (error)="onImgError($event)">` → `assets/img/no-image.png`), así que una imagen rota NO
debería tumbar el render de la card. Pero si el problema está en la respuesta COMPLETA del
endpoint (no en una imagen individual), el handler de error de `cargarDetalleCompleto()` en el
front hoy es silencioso (`error: () => { this.cargandoDetalle = false; }`) — si el back respondiera
error o un objeto vacío por esta causa, el usuario vería el estado "sin productos", que es
engañoso.

**Pregunta:** ¿hay algún caso conocido donde `GET /v1/pedidos/{id}/detalle` falle o devuelva
`detalles: []` específicamente por falta de imagen en alguno de los productos del pedido? Si es
así, necesitamos que no falle — el detalle (nombre, cantidad, precio) debe mostrarse igual, con o
sin imagen.

**Mientras se confirma:** el front va a mostrar el error real en pantalla en vez de la vacía
silenciosa (no depende del back, se hace de todas formas).

---

### ✅ Ya implementado (solo para que quede registro, no es pregunta)

- **Badge de tipo de pedido en la card de `mis-pedidos`** (📦 Apartado / 💳 Ir pagando) — ya existe
  en `mis-pedidos.component.html` desde la sesión del 2026-07-01 (sección NF-2 del changelog
  interno). Si el usuario no lo está viendo en QA, probablemente es tema de bundle no
  actualizado, no de código faltante.
- **Botón "Ver imagen" en `/variantes/carrito`** — ya está deshabilitado cuando no hay imagen
  (`[disabled]="!item.imagenUrl"` + guard en `verImagen()`). Si en vivo se ve habilitado sin
  imagen de todas formas, probablemente el back está mandando `imagenUrl` como string vacío no-nulo
  o una URL rota en vez de `null` — para diagnosticarlo necesitamos el producto/variante puntual
  donde pasó, con el valor exacto de `imagenUrl` que trae.

### 🔧 100% front, no depende del back — se implementa sin esperar respuesta

1. **Cobrar un pedido FIADO/APARTADO desde `mis-pedidos` da error 404.** El botón "Cobrar" de la
   card SIEMPRE abre el diálogo de "Confirmar cobro" (`PUT /v1/pedidos/confirmar/{id}`), sin
   importar el `tipoPedido` — ese endpoint es para ventas NORMAL y correctamente rechaza
   FIADO/APARTADO ("se liquidan mediante abonos, no por esta vía"). Ya existe abono completo en
   `detalle-pedido` (formulario de registrar abono, mismo endpoint `POST /v1/abonos/{pedidoId}`
   que usa `/abonos`). Plan: si `tipoPedido` es `APARTADO`/`FIADO`, el botón "Cobrar" ya no abre
   ese diálogo — en su lugar lleva directo al detalle (que ya tiene el formulario de abono) o
   muestra un aviso con acceso directo a `/abonos`. Ningún endpoint nuevo.
2. **Historial de abonos en la pantalla de Detalle del pedido.** Hoy `detalle-pedido` solo tiene
   el formulario para registrar un abono NUEVO — no muestra los abonos ya hechos. Buena noticia:
   **el dato ya viene en la misma respuesta que usa la pantalla** —
   `PedidoDetalleResponse.abonos?: AbonoDetalleItem[]` (`GET /v1/pedidos/{id}/detalle`) ya incluye
   el arreglo de abonos, el front solo no lo está pintando todavía. No hace falta ninguna llamada
   nueva.

**Ambos ya implementados (2026-07-22).**

---

### 💬 Nota — "campo nuevo para saber tipo de pedido" (confirmado, no es nada nuevo)

El usuario mencionó de pasada que iba a haber "un campo nuevo en los pedidos para saber si ir
pagando, apartado o de una" y preguntó si eso afectaba los 2 puntos de arriba. Confirmado en
código: ese campo **ya existe** — `IPedidoQuery.tipoPedido?: string` (valores `NORMAL` /
`APARTADO` / `FIADO`), ya viene en la respuesta de listado de pedidos desde la sesión del
2026-07-01 (NF-2) y es justo lo que usamos para el fix del punto 1 de arriba (redirigir "Cobrar" a
la pantalla de abono cuando el pedido es a crédito). No hay nada pendiente del back en esto — solo
lo dejamos anotado por si el mensaje se refería a otra cosa que no quedó clara y hace falta
aclarar de su lado.

---

## ✅ Respuestas back a la consulta de mis-pedidos / rifas (2026-07-21)

Respuestas verificadas en el código actual a los 3 puntos "❓" de la consulta de arriba.

### R-1 — El `motivo` de cancelación SÍ afecta la rifa, pero solo dos valores puntuales

`motivo` es un `String` libre (no un enum cerrado), default `"NO_SE_PRESENTO"` en el endpoint
`DELETE /v1/pedidos/delete/{id}?motivo=...`. Cualquiera que sea el valor, `estadoPedido` siempre
queda en `"cancelado"` — el `motivo` no cambia el estado, solo se guarda en `motivoCancelacion`.

Donde sí pesa: `calcularScore` (usado para calcular boletos de la rifa) resta boletos únicamente
cuando `motivoCancelacion` es `'TIMEOUT'` o `'NO_SE_PRESENTO'`. **Cualquier otro texto que mande el
front en `motivo` (ej. `ERROR_ADMIN`, `ERROR_CAPTURA`) ya NO penaliza al cliente** — no hace falta
que el back agregue nada nuevo, el front puede usar hoy mismo cualquier motivo distinto a esos dos
para el caso "error del admin al capturar".

Aparte: el import de participantes de la rifa (`clientesPorMes`/`importarDePedidos`) no excluye
pedidos cancelados por su `estadoPedido` — solo excluye `APARTADO`/`FIADO` que no estén `PAGADO`.
Un pedido NORMAL cancelado sigue contando como cliente candidato, solo cambia su peso en el score.

**Acción para el front:** al cancelar por error del admin, mandar `motivo=ERROR_ADMIN` (o
cualquier texto que no sea `TIMEOUT`/`NO_SE_PRESENTO`) — ya funciona hoy, sin cambios en el back.

### R-2 — Cliente sin registro con nombre repetido: NO se confunden

El back arma la lista de participantes usando `COALESCE(c.id, csr.id)` como `clientePedidoId` — es
decir, usa el **id de la fila** en `clientes_sin_registro`, no el nombre. Dos personas distintas
que compren ambas como "Raul" sin correo/teléfono generan cada una su propio registro con su propio
`id`, así que quedan como concursantes separados. No hay agrupación por nombre+apellido en el flujo
de import de rifa (`clientesPorMes`/`importarDePedidos`); ese matching por nombre solo existe en un
flujo distinto (`copiarDeRifa`, para copiar participantes entre dos rifas ya existentes) y no aplica
aquí. **No hay riesgo de fusión ni de descarte por duplicado en este flujo.**

### R-3 — El detalle de pedido NO falla por falta de imagen — porque no maneja imágenes

Revisado `getDetallePedido` (el método que arma el response de `GET /v1/pedidos/{id}/detalle`):
**no llama a ningún servicio de imágenes ni de variantes.** Solo copia campos ya cargados por JPA
(id, cantidad, precio, talla, color, descripción, promoción, `varianteId`). El DTO de cada detalle
no tiene ningún campo de imagen — como ya está documentado en la sección 20.3, la imagen se pide
aparte con `GET /v1/variantes/imagenes/{varianteId}` por cada detalle.

**Conclusión:** el escenario "el producto no tiene imagen y por eso el detalle sale vacío" no puede
pasar en el back — no hay ninguna llamada a imágenes dentro de este endpoint que pueda fallar. Si en
vivo se sigue viendo "No hay productos en este pedido", la causa es otra (error real del endpoint,
pedido sin `DetallePedido` asociado, o un problema en el front al leer el response) — no la imagen.
Con el cambio ya anotado arriba (mostrar el error real en vez del estado vacío silencioso) debería
verse el motivo real la próxima vez que pase.

---

## 🔎 Aclaración adicional — cliente sin registro repetido + cómo se envía `motivo` hoy (2026-07-21)

Dos dudas de seguimiento a R-2 y R-1, verificadas contra el código actual de ambos repos (back y
`producto_venta_online`). **Análisis únicamente — no se implementó nada todavía**, queda para la
siguiente sesión.

### R-2 (extensión) — Cancelar un "cliente sin registro" no lo reutiliza ni lo borra

Escenario planteado: se registra una venta con "Abel Tiburcio" (persona A), se cancela ese pedido,
luego se registra otra venta con "Abel Tiburcio" (puede ser la persona A repitiendo, o una persona B
distinta que solo coincide en nombre).

- **Cancelar el pedido NO toca la tabla `clientes_sin_registro` para nada.**
  `PedidoServiceImpl.deletePedidoById` (líneas 344-373) solo cambia el `Pedido`
  (`estadoPedido="cancelado"`, `motivoCancelacion`, `fechaCancelacion`) y devuelve stock. No hay
  ninguna llamada a `IClienteSinRegistroRepository` — el registro de "Abel Tiburcio" persona A queda
  intacto, con su `id` original, sea el pedido cancelado o no.
- **Cada venta sin registro crea SIEMPRE una fila nueva.** `VentaServiceImpl.java:143-144` hace
  `iClienteSinRegistroRepository.save(...)` de un `ClienteSinRegistro` recién construido — no busca
  antes por nombre. `IClienteSinRegistroRepository` no tiene ningún `findByNombre` ni lógica de
  "encontrar o crear".

**Conclusión:** volver a agregar "Abel Tiburcio" (sea la misma persona A o una persona B distinta)
genera una fila nueva e independiente, con `id` propio, sin importar si el registro anterior fue
cancelado o no. **No hay riesgo de mezclar a dos personas**, pero tampoco el sistema reconoce que es
"la misma persona" recurrente — cada compra sin registro es un registro aislado. Esto es consistente
con R-2 de arriba (el import de rifa usa el `id` de la fila, no el nombre).

### R-1 (extensión) — El front YA tiene un select para `motivo`, no manda texto libre

Duda: ¿cómo sabe el front qué texto exacto mandar en `motivo` para que el back lo identifique bien?

Ya está resuelto en el front, no hace falta inventar nada: `mis-pedidos.component.ts` (líneas
100-117), al cancelar, abre un modal (SweetAlert2) con **`input: 'radio'`** — dos opciones fijas:

```ts
inputOptions: {
  NO_SE_PRESENTO: 'No se presentó',
  CLIENTE_AVISO:  'El cliente avisó'
}
```

y el valor elegido se manda como **parámetro de query separado**, no concatenado en observaciones:
`pedidos.service.ts:40-42` → `DELETE /v1/pedidos/delete/{id}?motivo={valorExacto}`.

- `NO_SE_PRESENTO` sí coincide con el valor que el back penaliza en el score de la rifa.
- `CLIENTE_AVISO` ya se manda tal cual, pero el back **no lo penaliza** (solo penaliza
  `TIMEOUT`/`NO_SE_PRESENTO` — ver R-1 arriba), así que hoy mismo "el cliente avisó" ya se comporta
  como algo que no cuenta en contra, sin cambios pendientes.

**Lo que falta para el caso "error del admin al capturar" (R-1):** agregar una tercera opción al
mismo `inputOptions` que ya existe, por ejemplo:

```ts
inputOptions: {
  NO_SE_PRESENTO: 'No se presentó',
  CLIENTE_AVISO:  'El cliente avisó',
  ERROR_ADMIN:    'Error al capturar (admin)'
}
```

El mecanismo de envío (query param exacto, no texto libre) ya existe y no cambia — solo se agrega la
tercera entrada al objeto de opciones del radio. El back ya no penaliza ningún valor que no sea
`TIMEOUT`/`NO_SE_PRESENTO`, así que `ERROR_ADMIN` queda automáticamente sin penalización, sin tocar
nada del back.

### ✅ Pendiente para la siguiente sesión (front, 100% en `producto_venta_online`)

- [ ] Agregar la opción `ERROR_ADMIN: 'Error al capturar (admin)'` al `inputOptions` de
  `mis-pedidos.component.ts` (líneas ~106-109).
- [ ] Confirmar con el usuario si el texto visible ("Error al capturar (admin)") es el que quiere
  mostrar al admin en el modal, o prefiere otra redacción.

---

## 🔎 Aclaración adicional — cómo se identifica al ganador "sin registro" y si sus pedidos cuentan en la rifa (2026-07-21)

Dos dudas más de seguimiento, verificadas contra el código actual. **Análisis únicamente — nada
implementado todavía.**

### ¿Los pedidos de "cliente sin registro" SÍ participan en la rifa del mes? — Confirmado que SÍ, sin excepción

Recorrido el pipeline completo, sin ningún filtro oculto que los excluya:

- `findClientesUnicosPorMes`/`findTodosClientesConCompras` (`IPedidoRepository.java:240`) solo
  exigen `(cliente_id IS NOT NULL OR cliente_sin_registro_id IS NOT NULL)` — nunca exigen
  `cliente_id IS NOT NULL` a secas.
- `importarDePedidos` (`ConcursanteServiceImpl.java:144-203`) solo descarta por "ya registrado
  antes" o "nombre vacío" — ningún chequeo distingue cliente real vs sin registro.
- El cálculo de boletos (`calcularBoletos`/`calcularScore`) trata ambos casos de forma simétrica
  vía el flag `sinRegistro`, sin restarles nada por el simple hecho de no estar registrados.
- El sorteo final (`GanadorRifaServiceImpl`) elige entre todos los `Concursante` guardados, sin
  filtrar por si tienen `Cliente` asociado.

**Conclusión: un pedido de cliente sin registro pesa exactamente igual que uno de cliente
registrado en toda la rifa — boletos, elegibilidad y sorteo.**

### ¿Cómo se identifica al ganador si no está en el sistema? — Nombre y teléfono sí quedan, correo no

Cuando se hace el import de participantes, el back **congela una copia** de los datos de contacto
en la propia tabla `Concursante` (no queda como referencia viva a `Cliente`/`ClienteSinRegistro`):
`nombre`, `apellidoPaterno`, `telefono` (`ConcursanteServiceImpl.java:186-188`), tomados de
`COALESCE(cliente.nombre, clienteSinRegistro.nombre)` ya resuelto desde el listado de
`clientesPorMes`.

**Si un cliente sin registro gana:** el admin sí puede ver su **nombre y teléfono** en el
`GanadorRifa` (vía `Concursante`) para poder localizarlo y entregarle el premio — no depende de que
tenga cuenta en el sistema.

**Lo que NO queda disponible:**
- **Correo electrónico** — ni la query de `clientesPorMes` lo selecciona, ni `Concursante` tiene
  columna para guardarlo. Si se necesita contactar por correo (ej. para notificar el premio), hoy
  no hay forma de recuperarlo desde el ganador — habría que ir manualmente a la tabla
  `clientes_sin_registro` con el `clientePedidoId` guardado, si es que se conserva.
- **Certeza de si el ganador es cliente registrado o sin registro** — `Concursante` no guarda ese
  flag como columna persistida (la query sí lo calcula al vuelo, pero no se guarda). Como
  `clientes.id` y `clientes_sin_registro.id` son secuencias independientes, un mismo número de
  `clientePedidoId` podría corresponder a cualquiera de las dos tablas sin que quede registrado cuál
  fue — actualmente solo el nombre/teléfono congelados permiten identificar a la persona real, no
  hace falta saber en qué tabla vive para contactarla, pero si se necesita auditar después ("¿este
  ganador era cliente registrado?") no hay cómo saberlo con certeza desde `Concursante`.

### ✅ Pendiente para decidir (no es urgente, solo queda anotado)

- [ ] Definir si hace falta capturar correo del cliente sin registro para el caso de ganador (hoy no
  se guarda en ningún punto del flujo de rifa).
- [ ] Definir si vale la pena persistir el flag "sin registro" en `Concursante` para poder auditar
  después qué tipo de cliente ganó cada rifa.

---

## 📋 PLAN — Verificación real de correo para cliente sin registro + elegibilidad de rifa (2026-07-21, actualizado)

**✅ Implementado en el back (dev, sin commitear) — ver la sección "ESPECIFICACIÓN FINAL" más abajo,
que es la referencia con los endpoints y bodies exactos, ya implementados tal cual.**

### ⚠️ Confirmado en el front actual — hoy "Agregar cliente" NO llama al back

El modal "Agregar cliente sin registro" en `venta-directa.component.ts` (`obtenerDatosClienteSinRegistro()`,
líneas 163-164) hoy **solo asigna el formulario a una variable en memoria**
(`this.clienteSinRegistroModal = this.clienteForm.value`) — no hay ningún HTTP call ahí. El
`ClienteSinRegistro` se guarda en la BD hasta el final, en el mismo request que la venta
(`venta-directa.component.ts:649`, `clienteSinRegistroDto: this.clienteSinRegistroModal`, que
`VentaServiceImpl` guarda de un jalón dentro de `POST /v1/ventas/save`).

**El cambio que se pide es justo introducir un HTTP call ahí:** que "Agregar cliente" sí cree el
registro en el back (con `correoVerificado = false`), permita mandar/verificar el código en ese
mismo modal, y solo hasta cerrar el modal (verificado o aceptando seguir sin verificar) se habilite
"Generar venta" — la cual pasa a usar el `clienteSinRegistroId` ya creado en vez de mandar el DTO
completo otra vez. Si el admin cierra/cancela la venta después de agregar el cliente, queda una fila
huérfana en `clientes_sin_registro` sin pedido asociado — inofensivo (no afecta la rifa, que solo
cuenta filas ligadas a un pedido real), solo un dato de más en la tabla.

### Flujo confirmado con el usuario (venta presencial, admin captura los datos)

1. Admin hace la venta y captura al cliente sin registro. Le pide correo.
2. Si el cliente no quiere dar correo → admin pide teléfono en su lugar. Cualquiera de los dos casos
   permite generar la venta con normalidad.
3. **Si el cliente SÍ da un correo**, antes de terminar la venta el admin manda un código de
   verificación a ese correo (el cliente lo revisa ahí mismo, en su teléfono, y se lo dice al admin o
   lo captura él mismo en la pantalla) — igual que ya pasa hoy cuando alguien se registra en el
   sistema y no puede entrar hasta verificar su correo (`Cliente`/`Usuario`, mismo patrón, ver
   `ClienteServiceImpl.enviarCodigoVerificacionCorreo`/`verificarCorreo`,
   `service/ClienteServiceImpl.java:72-117`). Solo después de confirmar el código se termina de
   generar la venta.
4. Si el cliente no quiere dar ni correo ni teléfono, el registro `ClienteSinRegistro` **se guarda
   igual** (para reportes, como ya pasa hoy) — simplemente ese cliente no cuenta para la rifa del mes.
5. Regla de elegibilidad para la rifa: participa si **correo verificado** (`correoVerificado = true`)
   **O** teléfono presente (no vacío — el teléfono no se puede verificar, no existe SMS/OTP en el
   proyecto, así que ahí sí basta con que no venga vacío).

### Por qué no se puede verificar "después" de guardar la venta (detalle técnico)

Hoy `VentaServiceImpl` (líneas 143-144) crea un `ClienteSinRegistro` **nuevo siempre** dentro del
mismo `POST /v1/ventas/save` — no hay un paso intermedio con un `id` propio antes de eso. Pero el
patrón de verificación de `Cliente` (`enviarCodigoVerificacionCorreo`/`verificarCorreo`) necesita un
`id` ya persistido para guardarle el código y su expiración. Como el usuario quiere verificar el
correo **antes** de terminar la venta (no después), hace falta partir el guardado en dos pasos:

1. Se crea el `ClienteSinRegistro` primero (con `correoVerificado = false`), se manda el código.
2. Se verifica el código contra ese `id` ya existente (igual que `Cliente`).
3. `POST /v1/ventas/save` pasa a aceptar un `clienteSinRegistroId` ya existente (además de poder
   seguir creando uno nuevo inline si no hubo correo que verificar), para enlazar la venta al
   registro que ya se verificó.

### Plan propuesto (pendiente de aprobar antes de tocar código)

1. **Back — nuevos campos en `ClienteSinRegistro`** (mismo patrón que `Cliente`):
   `correoVerificado: Boolean` (default `false`), `codigoVerificacion: String`,
   `codigoVerificacionExpira: LocalDateTime` + migración SQL.
2. **Back — nuevo endpoint para crear el registro temprano + enviar código:**
   ej. `POST /v1/clientes-sin-registro` (crea la fila, sin venta todavía, devuelve el `id`) y
   `POST /v1/clientes-sin-registro/{id}/enviar-codigo` (genera código de 6 dígitos, igual que
   `Cliente`, reutilizando `EmailService`).
3. **Back — nuevo endpoint para verificar:** `POST /v1/clientes-sin-registro/{id}/verificar-codigo`
   `{ codigo }` → marca `correoVerificado = true` (mismo patrón que `ClienteServiceImpl.verificarCorreo`).
4. **Back — `POST /v1/ventas/save`** acepta `clienteSinRegistroId` (de un registro ya creado/verificado)
   como alternativa al DTO embebido actual (que sigue funcionando igual para el caso "no dio nada" o
   "solo dio teléfono").
5. **Back — ajustar la consulta de elegibilidad de la rifa** (`findClientesUnicosPorMes` /
   `findTodosClientesConCompras`, `IPedidoRepository.java`) para exigir, en clientes sin registro:
   `correo_verificado = true` **o** `numero_telefonico` no vacío.
6. **Front — pantalla de venta directa:** tras capturar correo, botón "Enviar código" → campo para
   capturarlo → botón "Verificar" → solo entonces se habilita "Generar venta". Si el cliente no dio
   correo (solo teléfono, o nada), se salta este paso y se genera la venta normal.
7. **Back — agregar `correo` a `Concursante`** (falta hoy) para poder notificar al ganador por correo
   si resulta ser un cliente sin registro, y **enviar correo automático al ganador** al sortear
   (reutilizando `EmailService`, mismo patrón que la sección 25). Este punto es independiente de la
   verificación — es la mejora de "avisarle a quien ganó".

### ❓ Preguntas — YA RESUELTAS (ver especificación final más abajo)

- ~~¿El filtro aplica solo a `ClienteSinRegistro` o también a `Cliente`?~~ → **Solo a
  `ClienteSinRegistro`.** `Cliente` registrado ya verifica su correo al registrarse (y al cambiarlo,
  vía `correoPendiente` — mejora 15); si no lo verifica, se queda con el correo anterior ya
  verificado. No hace falta ningún filtro nuevo para clientes registrados.
- ~~¿Se implementa el correo al ganador ahora o después?~~ → **Ahora, en esta misma sesión**, junto
  con todo lo demás.
- ~~¿Si no se verifica, se guarda el correo o se descarta?~~ → **Se guarda tal cual**, con
  `correoVerificado = false`. Sirve para reportes/contacto manual aunque no cuente para la rifa.

---

## ✅ ESPECIFICACIÓN FINAL — Verificar correo de cliente sin registro + elegibilidad de rifa + notificar al ganador (2026-07-21)

**Esta sección es la referencia única y completa para implementar este cambio — reemplaza en
detalle todo el análisis/plan de arriba.**

**✅ Implementado en el back (dev, sin commitear/pushear todavía; compila OK — `mvn compile` verde).**
Falta correr la migración SQL (`migration_verificacion_cliente_sin_registro.sql`) en dev/qa, y
falta todo el lado del front. Los endpoints, requests y responses de abajo ya son exactamente
como quedaron implementados, no un borrador.

### Resumen en una frase

Al agregar un cliente sin registro con correo, ese correo se verifica con un código de 6 dígitos
**antes** de poder generar la venta (el admin puede seguir sin verificar si el cliente no puede/no
quiere); un cliente sin registro solo participa en la rifa del mes si su correo quedó verificado **o**
si dio teléfono (el teléfono no se puede verificar — no existe SMS/OTP en el proyecto — así que ahí
basta con que no venga vacío); y al elegir ganador de una rifa, si tiene correo disponible se le
manda un correo automático avisando que ganó.

### 1. Cambios de datos (back)

**`ClienteSinRegistro`** — 3 columnas nuevas (mismo patrón que ya existe en `Cliente`):
| Campo | Tipo | Default |
|---|---|---|
| `correoVerificado` | boolean | `false` |
| `codigoVerificacion` | string (6 dígitos) | `null` |
| `codigoVerificacionExpira` | datetime | `null` |

**`Concursante`** — 1 columna nueva:
| Campo | Tipo | Default |
|---|---|---|
| `correo` | string | `null` |

**Migración:** `migration_verificacion_cliente_sin_registro.sql` (raíz de `resources/static`) —
**✅ ya corrida en dev, qa Y prod** (2026-07-21) — `inventario_key_qa` (dev/qa) e `inventario_key`
(prod). No queda ningún ambiente pendiente para esta migración.

Se llena al importar participantes de la rifa (`COALESCE(cliente.correo, clienteSinRegistro.correo)`),
igual que ya se hace hoy con `nombre`/`telefono`.

### 2. Endpoints nuevos — flujo de "Agregar cliente sin registro"

Reemplaza el modal actual, que hoy solo guarda el formulario en memoria
(`venta-directa.component.ts:163-164`) sin llamar al back.

#### 2.1 Crear el registro

```
POST /mis-productos/v1/clientes-sin-registro
```
**Request** (mismos campos que hoy manda el formulario, sin cambios de nombre):
```json
{
  "nombre_persona": "Abel Tiburcio",
  "segundo_nombre": "",
  "apeido_Paterno": "",
  "apeido_Materno": "",
  "fecha_Nacimiento": "",
  "sexo": "",
  "correo_Electronico": "abel@correo.com",
  "numero_Telefonico": ""
}
```
**Response (tal cual quedó implementado — regresa la entidad completa, no solo el id):**
```json
{
  "data": {
    "id": 501,
    "nombrePersona": "Abel Tiburcio",
    "segundoNombre": "",
    "apeidoPaterno": "",
    "apeidoMaterno": "",
    "fechaNacimiento": null,
    "sexo": "",
    "correoElectronico": "abel@correo.com",
    "numeroTelefonico": "",
    "correoVerificado": false,
    "codigoVerificacion": null,
    "codigoVerificacionExpira": null
  }
}
```
El front solo necesita quedarse con `id` (para los pasos siguientes) y `correoVerificado` (para
saber si mostrar el paso de verificación o ya venir en `true`/`false`). Se llama al confirmar el
formulario del modal (botón que hoy dice "Agregar" o similar). Si `correo_Electronico` viene vacío,
`id` igual se crea — simplemente no hay nada que verificar y el front pasa directo al paso 4.

#### 2.2 Enviar código de verificación

```
POST /mis-productos/v1/clientes-sin-registro/{id}/enviar-codigo
```
Sin body. Genera un código de 6 dígitos, lo guarda con expiración (mismos minutos que usa `Cliente`
hoy), y lo envía al `correo_Electronico` guardado en el paso 2.1.

**Response 200:** `{ "data": "Codigo enviado" }`
**Response 400:** si el registro no tiene correo — `"El cliente no tiene correo registrado"`.

#### 2.3 Verificar código

```
POST /mis-productos/v1/clientes-sin-registro/{id}/verificar-codigo
{ "codigo": "123456" }
```
**Response 200** (código correcto): `{ "data": "Correo verificado correctamente" }` — string, no
objeto. El front debe volver a marcar su propio estado local `correoVerificado = true` al recibir
un 200 aquí (el body no repite el objeto completo).
**Response 400** — código incorrecto: `"Codigo de verificacion invalido"`
**Response 400** — código vencido: `"El codigo de verificacion expiro, solicita uno nuevo"` (el front
debe ofrecer "Reenviar código", que vuelve a llamar 2.2)

El admin puede cerrar el modal en cualquier momento sin haber llamado 2.2/2.3 — el registro ya
existe (paso 2.1) con `correoVerificado = false`, y así se queda si nunca se verifica.

### 3. Cambio en `POST /v1/ventas/save`

**Nuevo campo opcional en el request:** `clienteSinRegistroId` (int). Cuando viene, el back usa el
registro ya creado en 2.1 (no crea uno nuevo). El campo `clienteSinRegistroDto` embebido (el que se
manda hoy) queda como fallback por compatibilidad, pero el flujo nuevo del front **siempre** debe
mandar `clienteSinRegistroId` en vez del DTO completo, una vez armado el paso 2.

```json
{
  "clienteSinRegistroId": 501,
  "clienteId": null,
  "...": "resto del body de venta sin cambios"
}
```

### 4. Flujo completo para el front (paso a paso)

1. Admin abre el modal "Agregar cliente sin registro", llena nombre + correo y/o teléfono.
2. Al confirmar el formulario → `POST /v1/clientes-sin-registro` (2.1) → guardar el `id` devuelto en
   el estado del componente (ya no se guarda el DTO completo, se guarda el `id`).
3. Si el registro tiene correo:
   - Mostrar botón **"Enviar código de verificación"** → llama 2.2.
   - Mostrar campo para capturar el código + botón **"Verificar"** → llama 2.3.
   - Si OK → mostrar check/badge "Correo verificado ✅".
   - Si falla → mostrar el error y permitir reintentar o reenviar código.
   - El admin puede omitir este paso y cerrar el modal igual — no es obligatorio, solo afecta si el
     cliente entra o no a la rifa.
4. Cerrar el modal (verificado o no) — el chip de cliente en pantalla ya no necesita mostrar nada
   nuevo, sigue igual que hoy.
5. Al presionar "Generar venta" → `POST /v1/ventas/save` con `clienteSinRegistroId` (del paso 2) en
   vez de `clienteSinRegistroDto`.

### 5. Elegibilidad de rifa (100% back, informativo para el front)

Un pedido de cliente sin registro participa en la rifa del mes solo si, al momento de la compra,
ese `ClienteSinRegistro` tiene `correoVerificado = true` **o** `numeroTelefonico` no vacío. Si
ninguno de los dos, el pedido se guarda igual (para reportes) pero no cuenta para esa rifa. Clientes
registrados (`Cliente`) no cambian — su correo ya se verifica en el registro.

### 6. Notificación automática al ganador

Al elegir ganador (`GanadorRifaServiceImpl`), si el `Concursante` ganador tiene `correo` (columna
nueva del punto 1), se le manda un correo automático avisando que ganó, reutilizando `EmailService`
(mismo mecanismo que el correo de "agrega tu compra" de la sección 25). Si no hay correo disponible
(el participante solo dio teléfono), no se manda nada automático — el admin contacta manualmente
usando el nombre/teléfono ya visibles en `GanadorRifa`.

### Resumen de endpoints — tabla rápida

| Endpoint | Método | Cuándo se llama |
|---|---|---|
| `/v1/clientes-sin-registro` | POST | Al confirmar el formulario del modal "Agregar cliente" |
| `/v1/clientes-sin-registro/{id}/enviar-codigo` | POST | Al presionar "Enviar código de verificación" |
| `/v1/clientes-sin-registro/{id}/verificar-codigo` | POST | Al presionar "Verificar" con el código capturado |
| `/v1/ventas/save` | POST | Al presionar "Generar venta" — ahora manda `clienteSinRegistroId` |

**Nada de esto rompe el flujo actual de clientes registrados (`clienteId`)** — solo cambia cómo se
maneja el caso de cliente sin registro.

---

## ✅ Confirmación del front (2026-07-22): ya implementado el flujo completo

Implementado tal cual la especificación final, sin necesitar ninguna aclaración adicional:

1. **Motivo `ERROR_ADMIN`** agregado al modal de cancelar en `mis-pedidos` (R-1 extensión) — sin
   cambios de back, como ya confirmaron.
2. **Modal "Agregar cliente sin registro" en `venta-directa`** reescrito a 2 pasos:
   - Paso 1 (form) → `POST /v1/clientes-sin-registro`, guarda el `id` devuelto.
   - Paso 2 (solo si dio correo sin verificar) → botón enviar código
     (`POST /v1/clientes-sin-registro/{id}/enviar-codigo`) + campo capturar código
     (`POST /v1/clientes-sin-registro/{id}/verificar-codigo`), con opción de omitir en cualquier
     momento.
3. **`POST /v1/ventas/save`** ahora manda `clienteSinRegistroId` en vez de
   `clienteSinRegistroDto` embebido, tal como pide la especificación.

**⚠️ Pendiente de nuestro lado:** no se pudo probar en vivo — no quedó claro en la especificación
si el código del back (los 3 endpoints nuevos) ya está commiteado/pusheado/desplegado en algún
ambiente, solo que "compila OK" en local y que la migración SQL ya corrió en dev/qa/prod. Avisen
cuando esté desplegado para probar el flujo de punta a punta.

**✅ Respuesta del back (2026-07-22):** ya está pusheado — commit en `dev` y merge `dev → qa` ya
en `origin/qa`. Listo para probar el flujo de punta a punta contra QA.

---

## ✅ Ajustes tras probar en vivo (2026-07-22) + ❓ una pregunta opcional

Tres cosas más, encontradas al probar el flujo de "Cobrar" en `mis-pedidos` en QA:

1. **"Cobrar" en un crédito ahora manda directo a `/abonos`** (antes mandaba al detalle del
   pedido, que también tiene abono pero no era lo esperado) — con `?pedidoId=N`, que abre
   automáticamente la card de ese pedido en Créditos/Abonos. 100% front, sin cambios de back.
2. **"Fiado" → "Ir pagando"** en la pantalla de Créditos/Abonos (`/abonos`) — se nos había pasado
   en el rename de julio, solo tocamos `venta-variante` en ese momento.
3. **Imprimir/enviar ticket ya no se puede antes de que haya algún pago** — en `mis-pedidos` y en
   el detalle del pedido. 100% front, usando el mismo `GET /v1/pedidos/{id}/detalle` que ya se
   pedía antes de imprimir (trae `estadoPedido`, `totalPagado`, `abonos`).

### ❓ Pregunta opcional (no bloqueante) — flag de "ya tiene pagos" en la lista de pedidos

En `mis-pedidos` (la lista de cards, `GET /v1/pedidos/...`) no tenemos forma de saber si un
pedido a crédito (`APARTADO`/`FIADO`) ya tiene al menos un abono sin pedir el detalle completo de
CADA card — así que ahí el botón de imprimir/enviar se queda visualmente habilitado y la
validación real ocurre al hacer clic (pedimos el detalle, y si no hay pagos, avisamos y no
generamos nada). Funciona, pero no se ve deshabilitado de entrada como si sería lo ideal.

¿Sería mucho pedir que la lista de pedidos incluya algo simple tipo `totalPagado` o `tienePagos`
por pedido? Con eso el front podría deshabilitar el botón ahí mismo, sin tener que esperar al
clic. No es urgente — el comportamiento actual ya evita el problema real (imprimir sin pago), solo
falta el detalle visual.

**✅ Respuesta del back (2026-07-22):** confirmado en código — `PedidoQuery` (el DTO detrás de
`GET /v1/pedidos/buscarClientePedido` y `GET /v1/pedidos/findPedido/{id}`) hoy **solo** trae `id`,
`fecha_pedido`, `estado_pedido` y `detalles`. `totalPagado` **no** viene en la lista hoy — solo en
`GET /v1/pedidos/{id}/detalle`. Agregarlo es sencillo (una columna más en las queries nativas +
un campo en el DTO), pero es trabajo nuevo, no algo que ya exista. Como ustedes mismos dicen que
no es urgente, queda anotado como pendiente — avisen cuando quieran que lo agreguemos.

---

## ✅ Fix (2026-07-22): badge de estado duplicaba el badge de tipo en pedidos a crédito

**Encontrado en vivo** en `mis-pedidos` y `detalle-pedido`: en la cabecera de un pedido
`APARTADO` se veía `📦 Apartado` seguido, justo abajo, de `APARTADO` en mayúsculas — parecía
información repetida/rota.

**Causa (dato para que lo tengan presente, no es que esté mal):** confirmamos que
`estado_pedido`/`estadoPedido` para un pedido a crédito es literalmente `'APARTADO'`/`'FIADO'`
(el mismo string que `tipoPedido`) hasta que se liquida, momento en el que cambia a `'PAGADO'`.
El front tenía un badge de **tipo** (📦 Apartado / 💳 Ir pagando) y, aparte, un badge de
**estado** que solo interpolaba ese campo tal cual — para un crédito sin pagar, mostraba
literalmente el mismo texto dos veces.

**Fix — 100% front, sin cambios de back:** el badge de estado ahora muestra el estado de pago
en vez del valor crudo: **"Por cobrar"** (nada pagado todavía) o **"Pagado"**
(`estadoPedido === 'PAGADO'`). NORMAL/Cancelado siguen mostrando `estado_pedido` tal cual, sin
cambios ahí.

---

## 🧹 Nuevo (back, 2026-07-22): limpieza automática de "cliente sin registro" huérfano

**Contexto:** con el flujo nuevo (`POST /v1/clientes-sin-registro` se llama ANTES de generar la
venta), si el admin agrega un cliente en el modal y luego lo quita/reemplaza sin llegar a generar
la venta, ese registro queda huérfano en `clientes_sin_registro` (creado, pero ningún `Pedido` lo
referencia).

**Solución — job automático a medianoche** (`ClienteSinRegistroLimpiezaScheduler`, cron
`0 0 0 * * *`): borra los registros de `clientes_sin_registro` que:
- No tienen ningún `Pedido` que los referencie (huérfanos), **y**
- Fueron creados hace más de **6 horas** (margen de seguridad — nunca borra algo que se esté
  capturando esa misma noche).

**Para el front, esto es 100% transparente — no cambia ningún contrato ni requiere nada nuevo.**
Solo un detalle a tener presente: si el admin crea un cliente sin registro (paso 1 del modal) y
por alguna razón la pantalla queda abierta/pausada por **más de 6 horas** sin terminar de generar
la venta, ese `clienteSinRegistroId` ya no existirá y `POST /v1/ventas/save` respondería
`"Cliente sin registro no encontrado"` — un caso extremo, no un flujo normal de uso.

**Migración:** `migration_limpieza_cliente_sin_registro.sql` — **✅ ya corrida en dev, qa y prod**
(2026-07-22). No queda ningún ambiente pendiente para esta migración.

---

## ❓ Confirmado: sí queremos `totalPagado` en la lista de pedidos — spec exacta

Con esto sí adelantamos (confirmado con el usuario). Para no ir y venir con el nombre/formato,
esto es exactamente lo que esperamos — si lo agregan tal cual, lo conectamos sin tener que
preguntar nada más:

**Dónde:** el DTO `PedidoQuery` que ya mencionaron (el que arma `GET /v1/pedidos/buscarClientePedido`
y `GET /v1/pedidos/findPedido/{id}`) — mismo objeto que hoy trae `id`, `fecha_pedido`,
`estado_pedido`, `tipoPedido`, `detalles`.

**Campo nuevo, nombre exacto:** `totalPagado` (camelCase, igual que `tipoPedido` en ese mismo
DTO — no `total_pagado`).

**Tipo:** `number` (decimal), igual que `PedidoDetalleResponse.totalPagado` en
`GET /v1/pedidos/{id}/detalle` — mismo significado: suma de abonos ya registrados para ese
pedido. Para pedidos `NORMAL` puede venir `0` o `null`, no lo usamos ahí.

**Shape esperado del objeto `pedido` dentro de cada item de la lista:**
```json
{
  "id": 89,
  "fecha_pedido": "22/07/2026 00:04",
  "estado_pedido": "APARTADO",
  "tipoPedido": "APARTADO",
  "totalPagado": 150.00,
  "detalles": [ ... ]
}
```

**Cómo lo vamos a usar (ya está el código listo del lado front, solo falta el dato):**
`mis-pedidos.component.ts` → `puedeGenerarTicket(item)` va a cambiar de "siempre `true` para
crédito" a `item.pedido.tipoPedido no es credito || (item.pedido.totalPagado ?? 0) > 0` — el
mismo criterio que ya usa `detalle-pedido` con el detalle completo.

**No es urgente** — el comportamiento actual ya es correcto (la validación al hacer clic no deja
generar el ticket sin pago), esto es solo para que el botón se vea deshabilitado desde que carga
la card. Avisen cuando esté listo y lo conectamos de una vez.

---

## ✅ Front: unificado el motivo de cancelación en mis-pedidos y abonos (2026-07-22)

Cambio 100% de front, no bloquea nada — lo anotamos igual porque puede afectar cómo llega
el campo `motivo` a los 2 endpoints de cancelar.

Antes: `mis-pedidos` (`DELETE /v1/pedidos/delete/{id}?motivo=...`) pedía el motivo con una
lista de opciones fijas (radio), pero `/abonos` (`PUT /v1/abonos/{pedidoId}/cancelar`) pedía
un texto libre opcional (input, máx 30 caracteres, podía ir vacío). Se unificaron las dos
pantallas para que ambas usen la MISMA selección de 3 opciones fijas — mismos valores
literales en ambas:

- `NO_SE_PRESENTO` → "No se presentó"
- `CLIENTE_AVISO` → "El cliente avisó"
- `ERROR_ADMIN` → "Error al capturar (fue el admin, no el cliente)"

## ❓ CONSULTA AL BACK — ¿`motivo` en `PUT /v1/abonos/{pedidoId}/cancelar` tiene el mismo
## efecto sobre el score de rifa que en `/v1/pedidos/delete/{id}`?

Ya nos confirmaron antes (para `DELETE /v1/pedidos/delete/{id}`) que `motivo` es texto libre y
que el score de rifa solo se penaliza cuando el valor es exactamente `TIMEOUT` o
`NO_SE_PRESENTO` — cualquier otro valor (como `CLIENTE_AVISO`/`ERROR_ADMIN`) no afecta al
cliente.

Como ahora `/abonos` también manda uno de esos 3 valores fijos (antes mandaba texto libre u
opcional/vacío), necesitamos confirmar:

1. ¿`PUT /v1/abonos/{pedidoId}/cancelar` usa `motivo` para algo más que guardarlo como texto —
   por ejemplo el mismo scoring de rifa que `/v1/pedidos/delete/{id}`?
2. Si sí, ¿aplica la misma regla (solo `TIMEOUT`/`NO_SE_PRESENTO` penalizan)?
3. Antes este campo podía llegar `null`/vacío (el input era opcional) — ahora SIEMPRE va a
   llegar uno de los 3 valores de la lista de arriba (ya no hay opción de dejarlo vacío).
   ¿Eso rompe algo del lado del back que esperaba poder recibirlo vacío?

No es urgente — el front ya está implementado y funcionando con estos 3 valores fijos
independientemente de la respuesta; es solo para saber si hace falta ajustar algo del scoring
de la rifa en ese endpoint específico.

---

## ✅ Back: elegibilidad de rifa vuelve a ser "compró este mes", sin requisito de correo/teléfono (2026-07-22)

Revertido el filtro que agregó `verificación de correo para cliente sin registro + elegibilidad
de rifa` (commit `0391fe9`). Ese cambio exigía que un cliente sin registro tuviera
`correo_verificado = TRUE` o un teléfono no vacío para poder entrar a la rifa. Se quita esa
condición: ahora cualquier cliente (registrado o sin registro) que haya comprado en el mes
entra a la rifa sin importar si tiene correo o teléfono capturado — como era antes de ese
commit.

Afecta:
- `IPedidoRepository.findClientesUnicosPorMes` (listado de clientes elegibles por mes)
- `IPedidoRepository.findTodosClientesConCompras` (listado de clientes con compras, sin filtro de mes)

No cambia el contrato de ningún endpoint (mismo shape de respuesta), solo cambia qué clientes
aparecen en la lista de elegibles. La notificación por correo al ganador (cuando gana alguien
sin correo capturado) sigue sin enviarse — eso no cambió, solo la elegibilidad para participar.

---

## ✅ Front: "Cobrar" en crédito seguía fallando en QA — ya está corregido (2026-07-22)

100% front, sin cambios de back. Se probó en vivo (con hard-refresh, no era el bug de caché) y
"Cobrar" en un pedido APARTADO/FIADO seguía abriendo el diálogo normal de forma de pago en vez
de mandar al redirect a `/abonos` de la sección anterior — al confirmar, el back lo rechazaba
(`PUT /v1/pedidos/confirmar/{id}` no acepta crédito) y el usuario veía un error genérico.

**Causa:** el redirect decidía con `item.pedido.tipoPedido`, que viene de la **lista**
(`GET /v1/pedidos/buscarClientePedido`). Ese campo llegaba `undefined` para pedidos de crédito
en ese endpoint — ver la pregunta de abajo.

**Fix:** `cobrarAdmin()` ya no confía solo en el campo de la lista. Antes de decidir, pide
`GET /v1/pedidos/{id}/detalle` (que sí trae `tipoPedido` de forma confiable) y decide con ese
dato. Si el detalle falla por algo (red), cae al diálogo normal como antes, para no bloquear un
pedido NORMAL por un problema de conectividad. Además, si el back de todos modos rechaza el
cobro con un mensaje que mencione "abono"/"apartado"/"fiado", ahora se ofrece el mismo redirect
en vez de un error genérico — red de seguridad extra.

## ❓ CONSULTA AL BACK — `tipoPedido` ¿sí viene o no en `buscarClientePedido`/`findPedido`?

Encontramos algo que no cuadra entre dos respuestas suyas en este mismo documento:

- Arriba, en "✅ Respuesta del back (2026-07-22)" sobre `totalPagado`, dijeron que `PedidoQuery`
  (el DTO de `buscarClientePedido`/`findPedido`) **hoy solo trae** `id`, `fecha_pedido`,
  `estado_pedido` y `detalles` — sin mencionar `tipoPedido`.
- Pero en la pregunta de `totalPagado` que mandamos después, nosotros mismos escribimos que ese
  mismo DTO "hoy trae `id`, `fecha_pedido`, `estado_pedido`, `tipoPedido`, `detalles`" — dando
  por hecho que `tipoPedido` ya estaba, porque lo agregamos al modelo del front desde el
  2026-07-01 (para el badge "📦 Apartado" de la card) y nunca nos habían dicho que faltara.

Con el bug de arriba ("Cobrar" no redirigía) sospechamos que la versión correcta es la primera:
`tipoPedido` **tampoco** viene en la lista, y el badge de tipo en la card probablemente lleva
tiempo sin mostrarse (bug silencioso, nadie lo reportó porque no truena, solo no se ve el ícono).

**¿Nos confirman si `tipoPedido` está o no en la respuesta de `buscarClientePedido`/
`findPedido` hoy?** Si NO está, ¿lo pueden agregar junto con `totalPagado` (mismo DTO, mismo
viaje) cuando lo hagan? Con eso el front deja de necesitar la llamada extra a `/detalle` para
decidir el redirect de "Cobrar", y el badge de tipo en la lista vuelve a funcionar.

No es urgente — el fix de arriba ya hace que "Cobrar" funcione correctamente sin depender de
esto (pide el detalle si hace falta), es solo para simplificar y arreglar el badge visual.
---

## ✅ Respuesta a la consulta pendiente — `motivo` en `PUT /v1/abonos/{pedidoId}/cancelar` (2026-07-23)

Confirmado: `motivo` en ese endpoint se guarda en la **misma columna** (`pedidos.motivo_cancelacion`)
que usa `DELETE /v1/pedidos/delete/{id}`, y ambos alimentan la **misma regla** de score de rifa —
solo `TIMEOUT` y `NO_SE_PRESENTO` penalizan, cualquier otro valor no afecta al cliente. De tus 3
valores fijos actuales: `NO_SE_PRESENTO` sí penaliza (correcto, es la intención), `CLIENTE_AVISO`
y `ERROR_ADMIN` no penalizan. No hace falta ajustar nada de tu lado por esto.

Sobre que ahora siempre llega uno de los 3 valores (antes podía ir vacío): no rompe nada, el back
ya tenía un fallback (`motivo == null → "CANCELADO"`) que sigue funcionando igual si algún caso
nuevo llegara a mandar vacío.

## ✅ Nuevo (2026-07-23): cancelar pedidos ya entregados/pagados (devolución), datos de entrega y fix de totales

### 1. Columnas ampliadas — ya corrido en qa y prod

`pedidos.observaciones` ahora es `TEXT` (antes `VARCHAR(100)`), `pedidos.motivo_cancelacion` ahora
es `VARCHAR(150)` (antes `VARCHAR(30)`). Ya no hay riesgo de truncamiento en observaciones o
motivos de cancelación normales.

### 2. NUEVO: cancelar un pedido ya entregado o ya pagado = devolución

Antes, `DELETE /v1/pedidos/delete/{id}?motivo=...` (pedidos normales) y
`PUT /v1/abonos/{pedidoId}/cancelar` (créditos APARTADO/FIADO) bloqueaban por completo cancelar
un pedido en estado `Entregado`/`PAGADO`. Ahora sí se permite — es una devolución real (el
cliente ya tenía el producto y lo está regresando), con estas reglas nuevas:

- Requiere que el usuario logueado sea **ADMIN** (`ROLE_ADMIN`). Si no lo es, 400 con:
  *"Solo un administrador puede cancelar un pedido que ya fue entregado o pagado"* (créditos:
  *"...ya pagado"*).
- El `motivo` **no puede ser** `TIMEOUT` ni `NO_SE_PRESENTO` aquí — si se manda, 400 con:
  *"Ese motivo es para pedidos que no se recogieron, no aplica para un pedido ya entregado"*.
  Razón: esos 2 motivos bajan el score de rifa, y aquí el cliente sí cumplió, solo devuelve el
  producto — no se le debe penalizar. Usa `CLIENTE_AVISO`, `ERROR_ADMIN`, o cualquier otro texto.
- El stock se regresa igual que en una cancelación normal (producto + variante).
- Si el pedido tenía una `Venta` asociada (la tienen todos los `Entregado`/`PAGADO`), se marca
  internamente como `"Devuelta"` — deja de contar en los reportes de ingresos por fecha/rango.
  No cambia el shape de esos reportes, solo el contenido (menos ventas si hay devoluciones).
- Excepción — `FIADO` **activo** (estado `"FIADO"`, todavía no llegó a `"PAGADO"`): ahí el stock
  NO se regresa (la mercancía ya se entregó desde el inicio y el cliente solo dejó de pagar; sigue
  tratándose como deuda incobrable, `stockDevuelto: false`), y no requiere ser ADMIN — esto no
  cambió respecto a antes.

**No cambia la URL ni el shape del request/response de ninguno de los 2 endpoints** — mismo
`DELETE /v1/pedidos/delete/{id}?motivo=...` y mismo `PUT /v1/abonos/{pedidoId}/cancelar` con
`{ "motivo": "...", "notificacion": {...} }`. Solo cambió qué estados ahora aceptan y quién los
puede llamar.

**Qué necesita el front:** si `estadoPedido` es `Entregado` o `PAGADO`, mostrar la acción de
cancelar solo si el usuario logueado es ADMIN, y no ofrecer `NO_SE_PRESENTO` como motivo ahí (usa
`CLIENTE_AVISO`/`ERROR_ADMIN`, o agrega una opción nueva tipo "Devolución" — el back acepta
cualquier texto que no sea `TIMEOUT`/`NO_SE_PRESENTO`).

### 3. Fix: el total del pedido no se actualizaba al quitar una línea

`DELETE /v1/pedidos/{pedidoId}/detalle/{productoId}?cantidad=N` — antes, al quitar o reducir una
línea, `totalPedido` se quedaba con el valor de antes del cambio (bug). Ahora se recalcula
correctamente sumando lo que queda. Si el front guardaba `totalPedido` localmente después de esta
llamada en vez de volver a pedir el detalle, ya no hace falta ese workaround.

### 4. NUEVO: datos de entrega en el pedido (a quién, dónde, cuándo)

Campos nuevos en `Pedido`: `nombreReceptor`, `direccionEntrega`. Se reutiliza el campo
`fechaRecogida` que ya existía — ahora representa la fecha en que se va a entregar el pedido
(antes en venta al contado se autoasignaba a "hoy" internamente, sin poder elegirla).

**a) Al crear la venta — `POST /v1/ventas/save` (`VentaDirectaRequest`)** — 3 campos nuevos, todos
opcionales:
```json
{
  "usuarioId": 1,
  "clienteId": 10,
  "detalles": [ "..." ],
  "observaciones": "Encargado por Facebook, buscar 'María Jade Boutique'",
  "nombreReceptor": "María López",
  "direccionEntrega": "Calle Reforma 123, Zacazonapan",
  "fechaEntrega": "2026-07-26"
}
```
- Si no se mandan, quedan vacíos y se pueden completar después (ver punto b).
- **Fix incluido:** antes, en venta de **contado**, `observaciones` se ignoraba siempre (el back
  lo forzaba a vacío sin importar lo que mandara el front). Ya no — si se manda, se guarda.
- `fechaEntrega`: si no se manda en venta de contado, se autoasigna a hoy (igual que antes); en
  crédito (APARTADO/FIADO) queda vacía si no se manda.

**b) Editar después — nuevo `PUT /v1/pedidos/{id}/entrega`:**
```json
// Request — todos los campos opcionales, solo se actualiza lo que se mande (null = no tocar)
{
  "nombreReceptor": "María López",
  "direccionEntrega": "Calle Reforma 123, Zacazonapan",
  "fechaEntrega": "2026-07-26",
  "observaciones": "Encargado por Facebook, buscar 'María Jade Boutique'"
}
```
```json
// Response 200 — mismo shape que GET /v1/pedidos/{id}/detalle (PedidoDetalleResponse)
{
  "pedidoId": 55,
  "estadoPedido": "APARTADO",
  "nombreReceptor": "María López",
  "direccionEntrega": "Calle Reforma 123, Zacazonapan",
  "fechaRecogida": "2026-07-26",
  "observaciones": "Encargado por Facebook, buscar 'María Jade Boutique'"
}
```
- No toca líneas, precios ni stock — solo estos 4 campos de "metadata" del pedido.
- Se puede llamar en cualquier momento después de creado el pedido, en cualquier estado excepto
  `"cancelado"` (ahí responde 400: *"No se pueden editar los datos de entrega de un pedido
  cancelado"*).
- No requiere ser ADMIN.
- `GET /v1/pedidos/{id}/detalle` (`PedidoDetalleResponse`) ya devuelve `nombreReceptor` y
  `direccionEntrega` en la respuesta (campos nuevos, mismo endpoint de siempre).

**Cuándo usar cada uno:** si el usuario captura estos datos al momento de vender, van en
`POST /v1/ventas/save`. Si no los captura ahí (o se equivocó y quiere corregirlos), se usa
`PUT /v1/pedidos/{id}/entrega` en cualquier momento posterior — mismos campos, mismo efecto final.

**Archivos cambiados:** `Pedido.java` (columnas nuevas), `PedidoDetalleResponse.java`,
`VentaDirectaRequest.java`, `PedidosDTOPedido.java`, `EditarEntregaPedidoRequest.java` (nuevo),
`VentaServiceImpl.java`, `PedidoServiceImpl.java`, `AbonoServiceImpl.java`, `PedidoController.java`,
`IPedidoService.java`, `IVentaRepository.java` (reportes excluyen `Devuelta`), migraciones
`migration_pedido_observaciones_motivo_ampliado.sql` (**ya corrida en qa y prod**) y
`migration_pedido_datos_entrega.sql` (**pendiente de correr en qa y prod**).
---

## ✅ Front: implementado "cancelar pedido ya entregado/pagado = devolución" (2026-07-24)

Ya conectado del lado front, según su respuesta del 2026-07-23:

- `mis-pedidos` (pedidos NORMAL): botón "Cancelar" ya no se deshabilita para admin cuando el
  pedido está `Entregado` (antes se deshabilitaba siempre en ese estado, sin importar el rol).
  Sigue deshabilitado para clientes normales. El motivo `NO_SE_PRESENTO` se excluye del selector
  cuando el pedido ya está entregado.
- `/abonos` → pestaña "Liquidados": no existía ningún botón de cancelar ahí, se agregó de cero.
  Como toda la ruta ya es admin-only, no hicimos chequeo de rol extra. Mismo filtro sin
  `NO_SE_PRESENTO`.
- Usamos las 2 opciones de motivo que ya existían (`CLIENTE_AVISO`/`ERROR_ADMIN`) para este caso,
  no agregamos una etiqueta nueva tipo "Devolución".

Aún no lo hemos probado en vivo contra el back. Cualquier cosa que no cuadre (mensaje de error
inesperado, 400 no documentado, etc.) lo anotamos aquí cuando lo probemos.

---

## ✅ Front: implementado "datos de entrega" + fix de total desactualizado (2026-07-24)

Ya conectado del lado front, según su respuesta del 2026-07-23:

- **Fix propio (no era del back):** `detalle-pedido` no refrescaba el total mostrado tras
  quitar una línea, aunque ustedes ya lo recalculaban bien server-side — el front nunca volvía
  a pedirlo. Ya corregido (se recalcula localmente sumando los subtotales que quedan).
- **`venta-directa`:** agregamos los 3 campos opcionales (`nombreReceptor`, `direccionEntrega`,
  `fechaEntrega`) al crear la venta, y movimos "Observaciones" para que se muestre/mande
  **siempre** (antes solo aparecía en crédito) — para aprovechar que ya no lo ignoran en
  contado.
- **`mis-pedidos`:** nuevo botón "📍 Entrega" en cada card → abre un modal para capturar/editar
  esos 4 campos en cualquier momento, llama `PUT /v1/pedidos/{id}/entrega`. Deshabilitado si el
  pedido está cancelado.
- **`detalle-pedido`:** muestra esos datos (solo lectura) si ya hay algo capturado.

⚠️ Todavía no lo hemos podido probar en vivo — según su propio doc, la migración
`migration_pedido_datos_entrega.sql` seguía pendiente de correr en qa/prod al momento de
escribir esto. Avísenos cuando ya esté corrida para probarlo de nuestro lado.

---

## ✅ NUEVO (2026-07-24): catálogo de "lugares de entrega" + link de Facebook por pedido

Dos campos nuevos, pensados para poder filtrar pedidos por zona de entrega (ej. "Zacazonapan")
en vez de buscar en el texto libre de `direccionEntrega`, y para guardar el link al perfil de
Facebook de quien hizo cada pedido (útil sobre todo en ventas de mostrador con
`ClienteSinRegistro`, para poder ubicar/contactar a la persona).

**Por qué van en `Pedido` y no en `Cliente`/`ClienteSinRegistro`:** igual que `nombreReceptor`/
`direccionEntrega`, quién recibe y desde dónde compró puede variar de un pedido a otro — y con
`ClienteSinRegistro` en particular no hay garantía de que sea la misma persona real la próxima
vez. Guardar el link en el pedido evita que quede pegado a un registro que puede no reflejar
correctamente quién hizo esa compra específica.

### 1. Catálogo nuevo — CRUD genérico `/v1/lugares-entrega`

Mismo patrón que otros catálogos simples del proyecto (`/v1/palabras-clave`, `/v1/pagos/*`).

| Método | URL | Quién | Body / respuesta |
|--------|-----|-------|-------------------|
| `GET` | `/v1/lugares-entrega/getAll?page=0&size=50` | Cualquier autenticado | `{ "data": [ {"id":1,"nombre":"Zacazonapan"}, ... ] }` (⚠️ ver corrección más abajo — no es `{ t: [...] }`) |
| `GET` | `/v1/lugares-entrega/getOne/{id}` | Cualquier autenticado | `{ "data": {"id":1,"nombre":"Zacazonapan"} }` |
| `POST` | `/v1/lugares-entrega/save` | ADMIN | Body: `{ "nombre": "Zacazonapan" }` → Response: el registro creado con `id` |
| `PUT` | `/v1/lugares-entrega/update/{id}` | ADMIN | Body: `{ "nombre": "Zacazonapan" }` → Response: el registro actualizado |
| `DELETE` | `/v1/lugares-entrega/delete` | ADMIN | Body: `1` (el id, como número JSON crudo — **no** `{ "id": 1 }`) |

`nombre` es único — si se repite, el back responde con el mensaje genérico de duplicado del CRUD
base ("El codigo postal ya existe, ingrese uno diferente" — mensaje heredado del CRUD genérico,
mejorarlo es aparte).

**⚠️ Corrección (2026-07-24):** el `DELETE` inicialmente se documentó con `Body: { "id": 1 }`,
que es **incorrecto** — así truena con `JSON parse error: Cannot deserialize value of type
'java.lang.Integer' from Object value`. El body correcto es el id **solo**, como valor JSON
crudo (`1`), sin envolver en objeto — es el mismo patrón que usan los demás catálogos genéricos
del proyecto (`/v1/palabras-clave/delete`, etc.). Ejemplo fetch/axios:
```js
fetch(`${base}/v1/lugares-entrega/delete`, {
  method: 'DELETE',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify(id) // "1", no { id }
});
```
También se corrigió un bug del back: el `delete()` genérico no borraba nada de verdad (bug
preexistente en el CRUD base, ya corregido — ver `LugarEntregaServiceImpl.delete`). Antes de este
fix, aunque el body fuera correcto, el endpoint respondía 200 sin eliminar el registro.

**Front:** pantalla nueva de catálogo (alta/edición de lugares), igual que cualquier otro catálogo
admin que ya tengan armado. El select de "lugar de entrega" en venta directa y en editar-entrega
consume `GET /v1/lugares-entrega/getAll` para poblar las opciones.

### 2. Campos nuevos en `Pedido`: `lugarEntregaId` + `urlFacebook`

**a) Al crear la venta — `POST /v1/ventas/save` (`VentaDirectaRequest`)** — 2 campos nuevos,
ambos opcionales, mismo lugar que `nombreReceptor`/`direccionEntrega`/`fechaEntrega`:
```json
{
  "usuarioId": 1,
  "clienteId": 10,
  "detalles": [ "..." ],
  "nombreReceptor": "María López",
  "direccionEntrega": "Calle Reforma 123",
  "lugarEntregaId": 1,
  "urlFacebook": "https://facebook.com/maria.lopez.jade",
  "fechaEntrega": "2026-07-26"
}
```
- `lugarEntregaId` debe existir en el catálogo — si no, 400 con *"Lugar de entrega no encontrado:
  {id}"*.
- `urlFacebook` no se valida formato en el back (cualquier string hasta 300 caracteres) — el front
  puede validar que sea una URL antes de mandarla si quiere evitar basura.

**b) Al crear pedido propio del cliente — `POST /v1/pedidos/savePedido` (`PedidosDTOPedido`)** —
mismos 2 campos nuevos, opcionales, mismo criterio (el cliente elige su lugar de entrega en su
propio checkout).

**c) Editar después — `PUT /v1/pedidos/{id}/entrega` (`EditarEntregaPedidoRequest`)** — mismos 2
campos agregados, opcionales (null = no tocar), junto a `nombreReceptor`/`direccionEntrega`:
```json
{
  "lugarEntregaId": 1,
  "urlFacebook": "https://facebook.com/maria.lopez.jade"
}
```

**d) Respuesta — `GET /v1/pedidos/{id}/detalle` (`PedidoDetalleResponse`)** — 3 campos nuevos:
```json
{
  "pedidoId": 55,
  "nombreReceptor": "María López",
  "direccionEntrega": "Calle Reforma 123",
  "lugarEntregaId": 1,
  "lugarEntregaNombre": "Zacazonapan",
  "urlFacebook": "https://facebook.com/maria.lopez.jade"
}
```
`lugarEntregaId`/`lugarEntregaNombre` solo aparecen si el pedido tiene lugar asignado (response
usa `@JsonInclude(NON_NULL)`, igual que el resto de este DTO).

**e) Listado/búsqueda — `GET /v1/pedidos/buscarClientePedido`** — nuevo query param opcional
`lugarEntregaId` para filtrar exacto por lugar (no es texto libre, es el id del catálogo):
```
GET /v1/pedidos/buscarClientePedido?lugarEntregaId=1&size=10&page=0
GET /v1/pedidos/buscarClientePedido?buscar=juan&lugarEntregaId=1&size=10&page=0
```
Se puede combinar con `buscar` (nombre/correo/teléfono) o usar solo. Si se omite, no filtra por
lugar (como antes). El JSON de cada pedido en la respuesta ahora también trae `lugarEntregaId`,
`lugarEntregaNombre` y `urlFacebook` (mismo campo `pedido` del objeto que ya devuelve `id`,
`estado_pedido`, `tipoPedido`, `totalPagado`, etc.).

### 3. Qué necesita el front — resumen de pantallas

- **Catálogo de lugares** (pantalla nueva, admin): alta/edición/listado simple, un campo de texto
  (`nombre`).
- **Venta directa** (form de creación): agregar select "Lugar de entrega" (poblado desde el
  catálogo) y campo de texto "Link de Facebook", ambos opcionales, junto a los campos de entrega
  que ya existen ahí.
- **Checkout del cliente** (`savePedido`): mismo select de lugar de entrega, opcional. El link de
  Facebook normalmente no aplica aquí (es el cliente comprando para sí mismo), se puede omitir del
  formulario público aunque el campo exista en el request.
- **Editar datos de entrega de un pedido**: agregar el mismo select + campo de texto al formulario
  que ya llama a `PUT /v1/pedidos/{id}/entrega`.
- **Detalle de pedido**: mostrar `lugarEntregaNombre` como texto y `urlFacebook` como link
  clickeable (`target="_blank"`) si viene en la respuesta.
- **Listado/búsqueda de pedidos**: agregar el mismo select como filtro adicional (aparte del
  buscador de texto que ya existe), mandando `lugarEntregaId` en la query.

**Archivos cambiados:** `LugarEntrega.java` (entity nueva), `ILugarEntregaRepository.java`,
`LugarEntregaServiceImpl.java`, `LugarEntregaController.java` (CRUD genérico, todos nuevos),
`Pedido.java` (columnas `lugar_entrega_id`, `url_facebook`), `PedidosDTOPedido.java`,
`EditarEntregaPedidoRequest.java`, `PedidoDetalleResponse.java`, `PedidoQuery.java`,
`VentaDirectaRequest.java`, `PedidoServiceImpl.java`, `VentaServiceImpl.java`,
`IPedidoRepository.java` (join a `lugares_entrega` + filtro en `buscarPedidosPorCliente`/
`buscarTodosLosPedidos`), `IPedidoService.java`, `PedidoController.java`, `SecurityConfig.java`,
migración `migration_lugar_entrega.sql` (**pendiente de correr en dev/qa/prod**).

---

## ✅ Front: implementado "lugares de entrega" + link de Facebook por pedido (2026-07-24)

Ya conectado del lado front, según su respuesta del 2026-07-24:

- Pantalla nueva de catálogo (alta/edición/eliminación) en `/lugares-entrega`, solo admin.
- `venta-directa`: select de lugar + input de link de Facebook.
- Checkout del cliente (`savePedido`): select de lugar (sin Facebook, como sugirieron).
- `mis-pedidos`: filtro por lugar (autocomplete local sobre el catálogo cargado), card muestra
  el nombre de quien recibe, y el modal de "Entrega" ya tiene los 2 campos nuevos.
- `detalle-pedido`: muestra el nombre del lugar y el link de Facebook (clickeable).

⚠️ Todavía no lo hemos probado en vivo — según su propio doc, `migration_lugar_entrega.sql`
seguía pendiente de correr en dev/qa/prod al momento de escribir esto. Avísennos cuando ya esté
corrida.

**Duda aparte:** en `mis-pedidos` agregamos que la card muestre `nombreReceptor` — pero no
tenemos confirmado si ese campo viene en `GET /v1/pedidos/buscarClientePedido` (la lista), solo
está confirmado que viene en `GET /v1/pedidos/{id}/detalle`. Si no viene en la lista, esa fila
simplemente no se muestra (el campo queda `undefined`), no rompe nada — pero si es fácil de
agregar ahí también, nos ahorraríamos tener que pedir el detalle de cada pedido para mostrarlo.

## ✅ Respuesta a la duda — `nombreReceptor` ya viene en la lista (2026-07-24)

Confirmado que no venía y era fácil de agregar — ya se agregó. `GET /v1/pedidos/buscarClientePedido`
(y también `buscarTodosLosPedidos`, mismo endpoint cuando `buscar` viene vacío) ahora incluye
`nombreReceptor` en el objeto `pedido` de cada resultado, junto a `tipoPedido`, `totalPagado`,
`lugarEntregaId`, `lugarEntregaNombre` y `urlFacebook`:
```json
{
  "cliente": { "id": 10, "nombreCliente": "...", "...": "..." },
  "pedido": {
    "id": 55,
    "fecha_pedido": "24/07/2026 10:30",
    "estado_pedido": "Entregado",
    "tipoPedido": "NORMAL",
    "totalPagado": 450.0,
    "nombreReceptor": "María López",
    "lugarEntregaId": 1,
    "lugarEntregaNombre": "Zacazonapan",
    "urlFacebook": "https://facebook.com/maria.lopez.jade",
    "detalles": [ "..." ]
  }
}
```
Puede venir `null` si el pedido nunca capturó ese dato (pedidos viejos, o si no se llenó al
crear la venta/pedido) — mismo criterio que los demás campos opcionales de este objeto. Ya no
hace falta pedir el detalle de cada pedido solo para mostrar el nombre del receptor en la lista.

**Archivo cambiado:** `IPedidoRepository.java` (agregado a `buscarPedidosPorCliente` y
`buscarTodosLosPedidos`), `PedidoQuery.java`.

---

## ✅ Front: aplicadas las 2 correcciones (2026-07-24)

- `DELETE /v1/lugares-entrega/delete` → ya manda el id crudo (`1`), no `{ id: 1 }`.
- `mis-pedidos`: `puedeGenerarTicket()` para crédito ya usa `totalPagado` real de la lista en
  vez de dejar el botón siempre habilitado. Gracias por confirmar que `nombreReceptor` también
  quedó en la lista — no hicimos falta más cambios ahí, el binding ya estaba listo, solo faltaba
  el dato.

---

## ❓ CONSULTA AL BACK — GET /v1/lugares-entrega/getAll parece no traer nada en QA

Probando en vivo (`pedidos/mis-pedidos` → botón "📍 Entrega"): el select "Lugar de entrega" del
modal aparece **vacío** (solo "Sin especificar"), aunque ya se agregaron lugares desde el
catálogo (`/lugares-entrega`). Reportado como que "parece que hay errores" al recargar esa
pantalla — todavía no tenemos el mensaje de error exacto ni una captura de la pestaña Network,
se las pasamos en cuanto las tengamos.

**El front implementó `GET /v1/lugares-entrega/getAll?page=0&size=50` tal cual lo documentaron**
(`LugarEntregaService.getAll()`), esperando:
```json
{ "data": { "t": [ {"id":1,"nombre":"..."} ], "pagina":0, "totalPaginas":1, "totalRegistros":1 } }
```
y leyendo `res.data.t`. Si la respuesta real trae otro shape (por ejemplo `data` como array
plano, o el campo no se llama `t`), el front simplemente lo interpreta como lista vacía sin
tronar — por eso no vemos ningún error en consola del lado nuestro, solo el select vacío.

**¿Nos pueden confirmar?**
1. ¿`GET /v1/lugares-entrega/getAll` está respondiendo 200 con datos reales en QA ahora mismo?
   (probamos con `curl` sin token y sí responde 401 "Token inválido o expirado" — o sea el
   endpoint existe y responde, pero no pudimos probarlo autenticados desde aquí).
2. ¿La migración `migration_lugar_entrega.sql` ya está corrida en QA? Es la única duda real que
   quedaba pendiente de su respuesta original.
3. Si ya corrió y el endpoint responde bien, ¿nos pueden pasar un ejemplo real de la respuesta
   (con al menos 1 lugar) para comparar contra el shape que documentaron?

No es urgente resolverlo en el momento — esperamos su respuesta antes de seguir investigando de
este lado.

## ✅ Respuesta a la consulta — el shape documentado estaba mal (2026-07-24)

Confirmado, el problema es del lado de la documentación, no del código: **su implementación es
correcta, lo que está mal es lo que les dijimos que esperaran.**

`GET /v1/lugares-entrega/getAll` usa el CRUD genérico (`AbstractController.findAll`), que **sí**
pagina internamente con `page`/`size`, pero **no** envuelve el resultado en `PginaDto` — solo
devuelve el arreglo plano de esa página, sin `pagina`/`totalPaginas`/`totalRegistros`:
```json
{ "data": [ {"id":1,"nombre":"Zacazonapan"}, {"id":2,"nombre":"Tejupilco"} ], "code":200, "mensaje":"..." }
```
(El shape `{ "t": [...], "pagina":... }` que documentamos es el de `PginaDto`, que usan otros
endpoints como `buscarPorNombre` — nos confundimos de patrón al escribir la tabla la primera vez.)

Con esto se responde también sus 3 preguntas:
1. Sí, el endpoint responde 200 con datos reales en QA — el problema era leer `res.data.t`
   (`undefined`, por eso caía a lista vacía sin tronar) en vez de `res.data` directo.
2. Sí, `migration_lugar_entrega.sql` ya corrió en QA (confirmado antes de esta sesión).
3. Ejemplo real con 1 lugar ya agregado ("Zacazonapan"):
   ```json
   { "data": [ {"id":1,"nombre":"Zacazonapan"} ], "code":200, "mensaje":"La peticion fue exitosa" }
   ```

**Fix necesario del lado front:** en `LugarEntregaService.getAll()`, cambiar `res.data.t` por
`res.data` directo (ya es el arreglo). Como el catálogo es chico (nombres de zonas/pueblos), para
traer "todos" en un solo viaje para el select alcanza con pedir un `size` grande en una sola
llamada, ej. `GET /v1/lugares-entrega/getAll?page=0&size=200` — no hace falta armar paginación
real en la UI para esto.

**Tabla corregida arriba** (sección "1. Catálogo nuevo") para reflejar el shape real.

### Aclaración — 2 usos distintos del mismo `GET /getAll`, con criterio de paginación diferente

`GET /v1/lugares-entrega/getAll` es un solo endpoint, pero se consume desde 2 pantallas con
necesidades distintas — no confundir una con la otra:

1. **Pantalla admin del catálogo (`/lugares-entrega`, alta/edición/borrado de lugares):** esta
   **sí debe paginar de verdad**, con tabla + controles de página, igual que cualquier otro
   catálogo/listado admin del proyecto (`page`/`size` normales, avanzar de página en página). Con
   pocos lugares hoy no se nota, pero si el catálogo crece (más zonas/colonias con el tiempo) esta
   pantalla sí necesita paginación real para no cargar cientos de registros de un tirón.

2. **Select de "lugar de entrega" en venta directa / editar-entrega / filtro de búsqueda de
   pedidos:** aquí **no hay ni debe haber paginación** — es un `<select>` que necesita **todas**
   las opciones disponibles de una vez para que el usuario elija. Aquí es donde aplica lo de
   pedir `size` grande en una sola llamada (`?page=0&size=200`) y usar el arreglo completo tal
   cual, sin controles de "siguiente página".

Mismo endpoint, mismo shape de respuesta (`{ "data": [...] }`) — la diferencia es solo cómo lo
consume cada pantalla: una pagina de verdad (catálogo admin), la otra pide todo de un jalón
(select).
---

## ✅ Front: aplicado el fix de shape + catálogo con paginación real (2026-07-24)

- `LugarEntregaService.getAll()` ya lee `res.data` directo (no `res.data.t`), `size=200` por
  default para las pantallas que necesitan todo el catálogo de un jalón (selects).
- Catálogo admin (`/lugares-entrega`) rediseñado con paginación real: tabla + "← Anterior" /
  "Siguiente →", `size=10` por página. Como `getAll` no trae total de registros, "hay
  siguiente" se infiere con `length === size`.

Gracias por la respuesta rápida y por confirmar que la migración ya estaba corrida — era 100%
lectura de shape del lado nuestro.

---

## ❓ CONSULTA AL BACK — filtro por tipo de pedido en `buscarClientePedido` + inventario de endpoints de `mis-pedidos`

### Inventario — endpoints que usa la pantalla `pedidos/mis-pedidos` hoy

| Método | URL | Para qué |
|---|---|---|
| `GET` | `/v1/pedidos/findPedido/{idCliente}?size=&page=` | Cliente no-admin: lista sus propios pedidos (infinite scroll) |
| `GET` | `/v1/pedidos/findPedido/{idPedido}/{idCliente}?size=&page=` | Cliente busca un pedido propio por número |
| `GET` | `/v1/pedidos/buscarClientePedido?size=&page=&buscar=&lugarEntregaId=` | Admin: lista/búsqueda general (texto + filtro de lugar, ya conectado) |
| `PUT` | `/v1/pedidos/confirmar/{id}` | Cobrar pedido NORMAL |
| `DELETE` | `/v1/pedidos/{pedidoId}/detalle/{productoId}?cantidad=` | Quitar/reducir una línea del detalle |
| `DELETE` | `/v1/pedidos/delete/{id}?motivo=` | Cancelar pedido |
| `GET` | `/v1/pedidos/{id}/detalle` | Detalle completo (incluye datos de entrega, lugar, Facebook, abonos) |
| `PUT` | `/v1/pedidos/{id}/entrega` | Editar nombreReceptor/direccionEntrega/fechaEntrega/lugarEntregaId/urlFacebook/observaciones |
| `POST` | `/v1/pedidos/{id}/notificar` | Reenviar comprobante por correo |
| `PUT` | `/v1/abonos/{pedidoId}/cancelar` | Cancelar crédito (vía `/abonos`, pantalla hermana) |

### ❓ Pregunta — filtro por tipo de pedido en `buscarClientePedido`

Agregamos en el front 2 botones "📦 Apartados" / "💳 Ir pagando" en `mis-pedidos` (admin),
independientes del filtro de lugar (se combinan con AND si ambos están activos: lugar +
Apartados = apartados de ese lugar; solo uno de los dos = solo ese filtro).

Mandamos el tipo como query param **repetido**, convención Spring `@RequestParam List<String>`:
```
GET /v1/pedidos/buscarClientePedido?size=10&page=0&tipoPedido=APARTADO&tipoPedido=FIADO
GET /v1/pedidos/buscarClientePedido?size=10&page=0&lugarEntregaId=1&tipoPedido=APARTADO
```
Si ningún checkbox está marcado, no se manda el parámetro (como hoy, sin filtro de tipo).

**¿`GET /v1/pedidos/buscarClientePedido` ya soporta filtrar por `tipoPedido` así, o hay que
agregarlo?** Si el nombre/formato del parámetro que esperan es distinto, avísennos y ajustamos
el front — mientras tanto no rompe nada, un query param que el back no reconoce simplemente se
ignora.

No es urgente — el resto de la pantalla (paginación real admin, filtro de lugar) ya funciona
sin depender de esto.

---

## ✅ Front: resumen visible de filtros activos en mis-pedidos (2026-07-24)

100% front, no necesita nada de su lado — lo anotamos igual por la regla de dejar registro de
todo lo que se implementa en esta pantalla.

Con 3 filtros combinables ahora en `mis-pedidos` admin (texto, lugar, tipo de pedido), se agregó
un chip debajo de los filtros que resume qué está activo, ej. `"Buscando: texto "123" + lugar
"Zacazonapan" + Apartados"` — solo visible si hay al menos un filtro aplicado.

---

## ✅ Respuesta a la consulta — filtro `tipoPedido` ya agregado (2026-07-24)

No existía, ya se agregó a `GET /v1/pedidos/buscarClientePedido` tal cual lo mandan ustedes —
mismo formato, query param repetido:
```
GET /v1/pedidos/buscarClientePedido?size=10&page=0&tipoPedido=APARTADO&tipoPedido=FIADO
GET /v1/pedidos/buscarClientePedido?size=10&page=0&lugarEntregaId=1&tipoPedido=APARTADO
GET /v1/pedidos/buscarClientePedido?size=10&page=0   ← sin tipoPedido = sin filtro de tipo, como hoy
```
- Se combina con `AND` con `buscar` y `lugarEntregaId`, exactamente como ya lo tenían asumido.
- Valores esperados: `"NORMAL"`, `"APARTADO"`, `"FIADO"` (mismos strings que ya usan en
  `tipoPedido` de la respuesta). Un valor que no sea ninguno de esos tres simplemente no
  matchea nada — no truena, solo no encuentra resultados para ese valor.
- El JSON de cada pedido en la respuesta no cambia (ya traía `tipoPedido`) — el filtro solo
  afecta qué resultados vienen, no el shape de cada uno.

**Archivos cambiados:** `IPedidoRepository.java` (`buscarPedidosPorCliente` y
`buscarTodosLosPedidos`), `PedidoServiceImpl.java`, `IPedidoService.java`, `PedidoController.java`.

---

## ⚠️ Aviso al front — falta un 3er checkbox de tipo (`NORMAL`) en `mis-pedidos`

Probando en vivo con el filtro ya conectado: hoy la pantalla solo tiene 2 checkboxes
("📦 Apartados" / "💳 Ir pagando" → `APARTADO`/`FIADO`). El back ya soporta el tercer valor sin
ningún cambio adicional — falta agregar el checkbox correspondiente en el front:

```
GET /v1/pedidos/buscarClientePedido?tipoPedido=NORMAL
```

Con esto los 3 checkboxes cubren los 3 valores reales de `tipoPedido` (`NORMAL`, `APARTADO`,
`FIADO`). Ninguno marcado sigue significando "sin filtro" (todos los tipos), no hay que mandar
los 3 explícitamente para ese caso.

## ⚠️ Aclaración — `buscar` NO busca por id/número de pedido (todavía)

Probando `buscar=1` contra un pedido real: encontró resultados, pero **por coincidencia** — el
teléfono del cliente (`7223475214`) contiene un "1", y `buscar` hace `LIKE '%valor%'` contra
nombre/correo/teléfono del cliente, nunca contra `pedidos.id`. Si el número que se busca no
coincide por casualidad con algo del cliente, no encuentra el pedido aunque el id exista.

**Pendiente de confirmar con ustedes:** ¿agregamos búsqueda por id de pedido al mismo campo
`buscar` (ej. `OR p.id = :buscar` cuando el valor es numérico), combinable con `lugarEntregaId`/
`tipoPedido` como todo lo demás? O si el filtro de "id de pedido" en la pantalla es un campo
separado que no debe tocar este endpoint, avísennos y lo dejamos como está.

## ✅ Resuelto — `buscar` ya también busca por id de pedido (2026-07-24)

Confirmado: en este sistema el "número de pedido" que ve el admin **es** `pedido.id` (no hay un
folio separado) — mismo id que usan en `/pedidos/{id}/detalle`, etc. Con eso, se agregó al mismo
campo `buscar`:

```
GET /v1/pedidos/buscarClientePedido?buscar=46           ← encuentra el pedido #46 por id, exista o no coincidencia con el cliente
GET /v1/pedidos/buscarClientePedido?buscar=juan          ← sigue buscando por nombre/correo/telefono, igual que antes
GET /v1/pedidos/buscarClientePedido?buscar=46&lugarEntregaId=1&tipoPedido=APARTADO  ← todo combinable
```

**Regla:** si `buscar` es **puramente numérico** (`^[0-9]+$`), además de la búsqueda de texto ya
existente, también compara contra `pedido.id` exacto (con `OR`, no reemplaza la búsqueda de
texto — un número podría coincidir con ambos criterios a la vez y no hay problema). Si `buscar`
trae letras, sigue comportándose exactamente igual que antes (solo texto).

**Nota:** ya no hace falta que interpreten el match anterior de `buscar=1` (que encontró un
pedido por coincidencia del teléfono) como bug — ahora con id 1 dígito buscaría explícitamente
`pedido.id = 1` también, además de seguir matcheando teléfonos que contengan "1".

**Archivo cambiado:** `IPedidoRepository.java` (`buscarPedidosPorCliente`).

---

## ✅ Front: agregado el checkbox "Normal" faltante (2026-07-24)

Ya está el 3er checkbox "🛒 Normal" junto a "Apartados"/"Ir pagando" en `mis-pedidos`. Gracias
por confirmar que `buscar` ya encuentra por id de pedido — no hizo falta ningún cambio en el
front para eso, ya mandábamos el texto tal cual al parámetro `buscar`.

---

## ✅ Front: verificado — no queda ningún "Fiado" visible en pantalla (2026-07-24)

100% front, sin acción del back — es solo texto de UI, el valor interno `tipoPedido: 'FIADO'`
sigue siendo el mismo que ya manejan.

El usuario pidió revisar si quedaba algún "Fiado" visible sin renombrar a "Ir pagando" (el
rename se hizo en una sesión anterior). Se hizo un grep exhaustivo de `Fiado`/`fiado` en todo
`src/app` (HTML + TS) — no queda ningún texto visible al usuario, todas las pantallas
(`mis-pedidos`, `detalle-pedido`, `/abonos`, `venta-variante`) ya dicen "Ir pagando". Lo único
que sigue con "fiado" son cosas internas sin impacto visual: el valor del enum `'FIADO'`,
nombres de clases CSS (`--fiado`) y variables (`esFiado`).

---

## ✅ Front: URL de la tienda cambió de /variantes a /tienda (2026-07-24)

100% front, sin acción del back — es solo la ruta del router de Angular, **no** toca las
llamadas al back (`/variantes/v1/...` sigue exactamente igual, ese es su path del backend, no
del front). Lo anotamos solo para que sepan que si ven links viejos a `/variantes/buscar` en
capturas o docs anteriores, ahora es `/tienda/buscar`.

---

## ❓ CONSULTA AL BACK — renombrar el endpoint `/variantes` a `/tienda` (necesita cambio de su lado)

Además del cambio de URL del navegador (front-only, ya avisado arriba), el usuario pidió que el
endpoint **real** del backend también deje de decir "variantes" — de `/variantes/...` a
`/tienda/...`. A diferencia de lo anterior, **esto sí necesita que ustedes hagan el mismo cambio**
— si solo lo cambiamos del lado front, todo lo relacionado a variantes (buscar, guardar,
imágenes, independizar, etc.) empezaría a dar 404.

### Ejemplo concreto (antes → después)

```
Antes:  GET  /variantes/1                          → traer la variante con id 1
Ahora:  GET  /tienda/1                              → misma función, mismo id, prefijo nuevo

Antes:  GET  /variantes/v1/buscar?termino=blusa
Ahora:  GET  /tienda/v1/buscar?termino=blusa

Antes:  POST /variantes/save
Ahora:  POST /tienda/save

Antes:  GET  /variantes/v1/imagenes/{varianteId}
Ahora:  GET  /tienda/v1/imagenes/{varianteId}

Antes:  POST /variantes/1/independizar
Ahora:  POST /tienda/1/independizar
```

**Regla exacta:** es un cambio de **prefijo únicamente** — todo lo que hoy empieza con
`/variantes` (sea `/variantes/1`, `/variantes/v1/buscar`, `/variantes/admin/...`, etc.) pasa a
empezar con `/tienda`, conservando exactamente el resto de la ruta, los query params y el shape
de request/response tal cual están hoy. No es un rename de campos ni de nada más — solo el
primer segmento de la URL.

**Ya implementado del lado front** (branch `dev`, `variante.service.ts` y los ~25 métodos que
dependen de su URL base, más `chatbot.service.ts` y `rifa.service.ts`) — pero **todavía NO
promovido a `qa`**, para no romper nada mientras ustedes no tengan el cambio espejo desplegado.
Avísennos cuando ya esté listo de su lado (y en qué ambiente — dev/qa/prod) y ahí sincronizamos
el merge a `qa` de nuestro lado para que coincidan.

**Nota:** confirmamos que **no** hay que tocar `/admin/sin-variantes/reporte` ni
`/compartir-imagenes-variantes` (del controlador de productos/Modelo) — esos "variantes" son
parte del nombre de esa ruta específica, no el prefijo `/variantes` que se está renombrando.

## ✅ Respuesta — ya aplicado en `dev`, análisis de impacto (2026-07-24)

Confirmado el rename, aplicado tal cual lo pidieron: **solo el primer segmento de la URL**, resto
de la ruta/query params/shape intactos. `VarianteController` pasó de `@RequestMapping("variantes")`
a `@RequestMapping("tienda")` — como es el prefijo base, cubre automáticamente los ~25 endpoints
(`/v1/buscar`, `/v1/save`, `/v1/imagenes/{id}`, `/v1/admin/**`, `/v1/{id}/independizar`, etc.),
sin tocar ninguno individualmente. Coincide con `/admin/sin-variantes/reporte` y
`/compartir-imagenes-variantes` — confirmado que esos NO cambian (son nombres de ruta de otro
controlador, no el prefijo).

**Análisis de impacto — no afecta nada más allá de este micro:**
- **micro_imagenes:** no llama a `/variantes/...` desde código — solo lo menciona en su propia
  documentación (`FLUJO_ENDPOINTS.md`), sin dependencia real. Cero cambios necesarios ahí.
- **nginx:** el `default.conf` de este micro es un proxy catch-all sin ruteo por path — no
  distingue `/variantes` de nada más, así que no hay nada que ajustar en infraestructura.
- **Otros consumidores internos:** ningún otro controlador/service del micro construye URLs
  hardcodeadas hacia `/variantes` (se revisó con grep en todo `src/`).

**Lo único que sí había que tocar en conjunto** (mismo aprendizaje que la migración a `/v1/` de
meses atrás — cambiar solo el `@RequestMapping` no basta):
- `SecurityConfig.java`: los 3 `requestMatchers` que protegían `/variantes/**` ahora protegen
  `/tienda/**`. Si no se actualizaban en conjunto, los GETs públicos de la tienda hubieran caído
  en `anyRequest().authenticated()` (rompiendo el catálogo público) y los matchers de
  `/variantes/admin/**` hubieran dejado de proteger nada.

**Estado:** confirmado que su cambio de front ya está en `qa` — ya mergeamos `dev → qa` de este
lado también. El rename `/variantes` → `/tienda` (y todo lo demás de esta sesión) ya está en
`qa` en ambos lados, coordinado.

---

## ✅ Front: promovido a QA — ambos lados ya coinciden (2026-07-24)

Ya subimos el rename a `qa` (deploy confirmado exitoso). Verificamos con `curl` antes de avisar:
`GET /tienda/1` responde 401 (protegido, funcionando) y `/variantes/1` (el viejo) ya no
responde bien — o sea ustedes ya tenían el cambio desplegado en QA también, no solo en dev
como habían dicho, así que no hubo ninguna ventana de caída del catálogo. Todo sincronizado.
---

## ✅ Front: fix móvil — filtros traslapados + cards de 2 en 2 (2026-07-24)

100% front, sin acción del back. Reportado con capturas: en `productos/buscar` y
`variantes/buscar` ("Tienda"), los checkboxes de filtro se veían con texto encimado en anchos
tipo tablet/celular grande (faltaba un breakpoint intermedio entre 576px y el punto donde ya
caben 4 columnas cómodas), y las cards de producto se veían de 1 en 1 en móvil en vez de 2 en
2 (pedido explícito). Ambos corregidos — detalle completo en `CLAUDE.md` de este repo, sección
"FIX MÓVIL — FILTROS TRASLAPADOS EN TABLET/CELULAR GRANDE + CARDS DE 2 EN 2".

---

## ✅ Fix: cancelar pedido — el back ya validaba, ahora también informa el motivo (2026-07-27)

**Pregunta del front:** al cancelar un pedido, ¿el front debe ocultar/deshabilitar el botón según
el estado, o el back valida?

**Respuesta: ambos.** El back **ya validaba** (no es un cambio de reglas, ya existía):
- No se puede cancelar un pedido que ya está en estado `cancelado`.
- Si el pedido ya está `Entregado` o `PAGADO` (crédito liquidado), cancelar es en realidad una
  **devolución** — solo un usuario con rol ADMIN puede hacerlo, y no se permite mandar
  `motivo=TIMEOUT` ni `motivo=NO_SE_PRESENTO` en ese caso (esos motivos son para pedidos que
  nunca se recogieron, no para uno que ya se entregó).

**Lo que sí estaba mal y se corrigió:** cuando el back rechazaba la cancelación, el endpoint
devolvía **500 vacío, sin mensaje** — el front no tenía forma de saber por qué falló.

```
Request: DELETE /v1/pedidos/delete/{id}?motivo=NO_SE_PRESENTO
```

**Antes:**
- Éxito → `200`, sin body.
- Rechazo → `500`, sin body.

**Ahora:**
- Éxito → `200`, body `{ "response": "Pedido cancelado correctamente" }`.
- Rechazo → `400`, body `{ "mensaje": "..." }` con el motivo exacto, por ejemplo:
  - `"No se puede cancelar un pedido en estado: cancelado"`
  - `"Solo un administrador puede cancelar un pedido que ya fue entregado o pagado"`
  - `"Ese motivo es para pedidos que no se recogieron, no aplica para un pedido ya entregado"`

**Recomendación de UX (no obligatoria, el back ya bloquea el caso):** usar el `estadoPedido` que
ya viene en `GET /v1/pedidos/{id}/detalle` (`PedidoDetalleResponse.estadoPedido`) para ocultar o
deshabilitar el botón de cancelar cuando el estado sea `cancelado`, o mostrarlo distinto (como
"solicitar devolución") cuando sea `Entregado`/`PAGADO` y el usuario no sea admin. Esto es solo
para mejor experiencia — el back sigue siendo la fuente de verdad y rechaza cualquier intento
igual, ahora con mensaje claro en el 400.

---

## ✅ Fix: cancelar un FIADO desde la pantalla de Pedidos devolvía stock indebido (2026-07-27)

Pregunta del front: si el mismo pedido de crédito se puede cancelar tanto desde **Pedidos**
(`DELETE /v1/pedidos/delete/{id}`) como desde **Abonos** (`PUT /v1/abonos/{pedidoId}/cancelar`),
¿queda igual de protegido en ambos lados?

**Sí, es la misma fila en BD** (mismo `estadoPedido`), así que cancelar desde cualquiera de las
dos pantallas se refleja de inmediato en la otra en cuanto se vuelva a pedir el dato — y un
segundo intento de cancelar (desde cualquiera de las dos) ahora se rechaza con `400` y mensaje
claro (antes, del lado de Pedidos, tiraba `500` vacío — ver sección anterior).

**Pero encontramos un bug de negocio real, no solo de mensajes:** para un pedido `FIADO` **activo**
(ya entregado al cliente, todavía pagando), la regla correcta es que **cancelar NO devuelve stock**
(la mercancía ya salió, queda como deuda incobrable — así lo hace `/v1/abonos/{pedidoId}/cancelar`
desde siempre). El endpoint general `/v1/pedidos/delete/{id}` no conocía esa excepción y devolvía
stock siempre, sin importar el tipo — si un FIADO activo se cancelaba desde la pantalla de
**Pedidos** en vez de la de **Abonos**, el stock se restauraba aunque el cliente se hubiera
quedado con el producto. Ya corregido: `/v1/pedidos/delete/{id}` ahora replica la misma regla
(no devuelve stock si `tipoPedido = FIADO` y todavía no es una devolución real, es decir, no está
en `Entregado`/`PAGADO`). `APARTADO` no cambia — siempre devolvía stock y sigue siendo correcto,
porque en `APARTADO` la mercancía nunca se entregó.

**Recomendación de UX (no obligatoria):** si la pantalla de Pedidos va a seguir mostrando el botón
de cancelar para pedidos de crédito, ya no hay riesgo de inconsistencia de stock al usarlo — pero
sigue sin mostrar el desglose de saldo a favor / deuda pendiente que sí trae la respuesta de
`/v1/abonos/{pedidoId}/cancelar` (`saldoAFavor`, `deudaPendiente`, mensaje). Si el front quiere ese
detalle para el usuario, sigue siendo mejor dirigir la cancelación de APARTADO/FIADO a la pantalla
de Abonos.

---

## ✅ Front: revisado y confirmado — ya funcionaba, más una mejora chica (2026-07-27)

Revisamos ambos fixes que documentaron (400+mensaje al cancelar, stock de FIADO). Sin dudas:

- **El manejo de error ya estaba listo** — `cancelarPedido()` en `mis-pedidos` ya leía
  `err?.error?.mensaje`, así que el mensaje nuevo del 400 se muestra automático, sin tocar
  código.
- **El fix de stock en FIADO es 100% backend** — no requirió nada de nuestro lado.
- **Sí aplicamos la recomendación opcional**: el botón "Cancelar" ahora también se deshabilita
  cuando el pedido ya está cancelado (antes solo consideraba "Entregado" + no-admin).

Gracias por el detalle de los 3 mensajes de error exactos, ayudó a confirmar rápido que ya
estábamos leyendo el campo correcto.

---

## ✅ Perf: imágenes ahora se cachean en el navegador (2026-07-27)

Contexto: las listas de productos/variantes con imagen tardaban en aparecer. El análisis mostró
que proyecto-key ya arma la lista con una sola query (sin N+1) — el cuello de botella estaba en
`micro_imagenes`, que servía cada imagen sin ningún header de caché: el navegador re-descargaba
las mismas fotos en cada búsqueda o repaginado, aunque no hubieran cambiado.

```
Request: GET {endpointImagenes}/v1/imagenes/file/{imagenId}
```

**No cambia el contrato** (misma URL, mismo status, mismo body) — solo se agregaron headers:
- `Cache-Control: public, max-age=31536000, immutable`
- `ETag: "{imagenId}"`

**No requiere ningún cambio en el front.** El navegador va a empezar a cachear cada imagen sola
por hasta 1 año la primera vez que la descargue — en listas/búsquedas repetidas debería notarse
la diferencia de inmediato porque deja de volver a pedir imágenes ya vistas. Como el id de una
imagen nunca se reutiliza (reemplazar = subir un id nuevo, eliminar = se borra) es seguro
cachearla como inmutable indefinidamente.

Quedan pendientes de análisis (no implementados aún): generar miniaturas para las listas en vez
de servir el archivo original completo, y un endpoint por lote para pedir varias imágenes en una
sola llamada en vez de una por producto.

---

## ✅ Perf: miniaturas en listado/búsqueda de productos y variantes (2026-07-28)

Segundo paso para bajar el tiempo de carga de imágenes (el primero fue el caché del navegador,
sección anterior). Ahora la lista/búsqueda de productos y de variantes ya no manda la imagen
original completa — manda una **miniatura** más liviana (máx. 400px de ancho, mismo alto
proporcional).

**No requiere ningún cambio en el front.** El campo de imagen en la respuesta (`urlImagen` /
`imagenUrl`) sigue siendo un string con una URL completa, igual que antes — solo que ahora esa URL
apunta a un endpoint distinto según el contexto:

- **Listado/búsqueda de productos y variantes** (`GET /productos/obtenerProductos`,
  `GET /productos/buscarNombreOrCodigoBarra`, `GET /variantes/buscar` y equivalentes de
  admin/filtros) → la URL ahora es:
  ```
  GET {endpointImagenes}/v1/imagenes/thumbnail/{imagenId}
  ```
  Devuelve los mismos bytes de imagen (mismo `Content-Type`, mismo `Cache-Control`/`ETag` de 1 año
  que el original), solo que redimensionada. Si `noContent (204)`, es el mismo caso de siempre:
  imagen no encontrada en disco.

- **Detalle de producto/variante** (`GET /productos/findById/{id}`, galería de imágenes) → sigue
  usando la imagen completa sin cambios:
  ```
  GET {endpointImagenes}/v1/imagenes/file/{imagenId}
  ```

El front no tiene que armar ninguna de las dos URLs manualmente — ya vienen completas en la
respuesta, así que este cambio es transparente mientras no se haya hardcodeado en ningún lado la
ruta `/v1/imagenes/file/` esperando que sea siempre esa.

Pendiente de análisis: caché en memoria de bytes calientes y/o endpoint por lote, para bajar
todavía más el tiempo que tarda la *primera* carga de una búsqueda nueva.

**Estado de despliegue:** ya está en `dev` y `qa` de ambos repos (proyecto-key y micro_imagenes).
**Todavía no está en producción (`main`/`master`)** — el caché de navegador de la sección anterior
tampoco. Avisamos cuando se suba a main para que puedan validar en QA mientras tanto.

### 🔍 Reporte de prueba en QA (2026-07-28): variantes se ven chicas, productos se siguen viendo grandes

Al probar en `qa.shop.novedades-jade.com.mx`: en la pantalla de **variantes** las imágenes ya se
ven chicas (esperado), pero en la pantalla de **productos → buscar** se siguen viendo grandes.

**Verificado directo contra el servidor de QA (sin pasar por el navegador), backend está bien:**
- `GET /v1/productos/obtenerProductos` y `GET /v1/productos/buscarNombreOrCodigoBarra` en QA ya
  devuelven `urlImagen` apuntando a `/v1/imagenes/thumbnail/{id}`, no a `/file/{id}`.
- Se probó bajar una imagen real de un producto de QA: el original pesa 232 KB (960x1280 px), la
  miniatura pesa 56 KB (400x533 px) — el redimensionado sí funciona correctamente.
- La caché de Redis de esa búsqueda (`buscarNombreOrCodigoBarrasCache`, TTL 2h) ya fue limpiada a
  mano (`DELETE /v1/admin/cache`) durante esta sesión — no era (o ya no es) la causa.

**Conclusión:** el backend de productos en QA está devolviendo la miniatura correcta y más liviana.
Si en la pantalla de productos sigue viéndose "grande", la causa más probable está del lado del
front/navegador, no del backend:
- Caché del navegador con la página/imágenes viejas — probar en ventana de incógnito.
- O que "grande" se refiera al tamaño con que se dibuja el `<img>` en pantalla (controlado por
  CSS/layout del front), no al peso del archivo descargado — el fix de miniaturas reduce el peso y
  tiempo de descarga, no el tamaño visual del recuadro en la página.

**Pendiente para retomar si el problema no se resuelve solo:** confirmar en el navegador (F12 →
Network) si la imagen que carga la pantalla de productos → buscar es realmente `/thumbnail/` (y de
qué peso), y si el problema es de velocidad o solo de tamaño visual en pantalla.

---

## ✅ Confirmado del lado del front — "productos → buscar" nunca iba a verse más chico (2026-07-28)

Revisamos el reporte de la sección anterior. Confirmamos con el código:

1. **El pipe `imagenSrc` del front (usado en TODAS las listas, `productos/buscar` y
   `variantes/buscar` por igual) no reescribe la URL que ya viene armada del back.** Solo tiene un
   regex que convierte `/imagenes/{id}` (id pelón al final) → `/imagenes/file/{id}` — eso solo
   aplica a las URLs viejas de detalle. Una URL que ya llega como `/v1/imagenes/thumbnail/{id}` no
   matchea ese regex (no termina en solo dígitos) y se usa tal cual, sin tocarla. Confirmado que
   ambas pantallas comparten exactamente el mismo código para consumir la imagen — no hay ninguna
   ruta alterna en `productos/buscar` que fuerce `/file/`.

2. **La causa real de que se vea "grande" es CSS, no el peso del archivo — y es diseño de siempre,
   no una regresión de este fix.** El recuadro de imagen en las cards de `productos/buscar` es un
   contenedor con `height: 180px` fijo + `object-fit: cover` — el navegador SIEMPRE dibuja la
   imagen a ese tamaño de caja sin importar si descargó el original de 960×1280 o la miniatura de
   400×533; `object-fit: cover` solo cambia cuánto tarda en llegar y cuánta memoria usa, no el
   tamaño en pantalla. Es decir: aunque el fix de miniaturas funcione perfecto (y por lo que
   verificaron del lado del back, sí funciona), esta pantalla **nunca** iba a "verse más chica" —
   eso solo se nota en pantallas donde el layout de la card cambia de tamaño según el contenido, no
   en esta.

**Conclusión:** no hay ningún bug ni cambio pendiente del front para esto. El fix de miniaturas
está funcionando (menos peso, menos tiempo de descarga) — simplemente no había ninguna expectativa
válida de que el recuadro visual cambiara de tamaño en esta pantalla en particular. Gracias por
dejar la verificación directa contra el servidor (bytes/tamaño real) — ayudó a descartar rápido
que fuera caché de Redis o un problema del backend antes de que lo revisáramos del lado del front.

---

## 🎨 Cambio de imagen del front — paleta jade (2026-07-30)

Solo para que estén enterados: **el front cambió de color de marca**. Ya está desplegado en `dev`
y `qa`. **No requiere ningún cambio de su lado ni afecta ningún endpoint** — es 100% CSS
(variables de tema y hojas de estilo de componentes). Lo anotamos aquí porque si abren QA se van
a topar con una app que se ve distinta y no queremos que parezca un problema de despliegue.

**Qué cambió:** el acento pasó de azul/morado (`#007AFF` / `#5856D6`) a **verde jade** —
`#00875A` en modo claro y `#00D97E` en modo oscuro (el nombre viene de la tienda, Novedades
Jade). Los grises y fondos, que eran azul marino, se inclinaron a un neutro verdoso para que
todo se vea de la misma familia.

**Cero impacto en la API:** no se tocó ningún `.service.ts`, ninguna URL, ningún contrato de
request/response. Si algo se ve raro en QA, es de estilos, no de datos.

**Un detalle que quizá les interese** (por si les toca algo parecido del lado de sus pantallas):
tres bugs de esta migración **no los detectó el compilador** — sólo aparecieron al levantar la
app y mirarla en capturas:
1. Los botones de Bootstrap seguían azules porque `bootstrap.min.css` se carga después de
   nuestros estilos y ganaba por orden de cascada.
2. Texto blanco sobre el verde brillante quedaba ilegible en modo oscuro.
3. Dos verdes distintos terminaron significando cosas distintas: al volverse verde la marca,
   los badges de "Apartado"/"Ir pagando" dejaron de distinguirse del verde de "Pagado". Ahora
   los estados usan colores semánticos independientes del color de marca:
   **Apartado = ámbar, Ir pagando = azul, Pagado = verde, Cancelado = rojo.**

Ese último punto sí es visible para el usuario final, así que si en algún reporte o correo que
genere el back se usan colores por estado de pedido, vale la pena homologarlos con esa tabla
para que el cliente vea lo mismo en la app y en el correo. Si quieren, nos dicen y les pasamos
los hex exactos.

---

## 🔐 CORRECCIONES DE SEGURIDAD EN AUTENTICACIÓN — 2026-07-31 (acción requerida en el front)

Tanda de correcciones sobre `AuthController` y toda la capa de autenticación (16 de 18 hallazgos
de `SEGURIDAD_AUTH.md`). **Sin desplegar todavía — está en `dev`, sin commitear.**

La mayoría son internas y el front no las nota. Pero hay **tres cambios de comportamiento** que sí
afectan al front, y uno que requiere que el front agregue un header.

### 1. ⚠️ `passwordTemporal` ahora se fuerza en el backend

**Antes:** el login devolvía `passwordTemporal: true` (contraseña puesta por un ADMIN) pero el
token venía con permisos completos. Si el front ignoraba el flag, el usuario podía navegar y operar
con normalidad.

**Ahora:** con `passwordTemporal = true` el backend responde **403** en **todos** los endpoints
salvo estos cuatro:

| Método | URL |
|---|---|
| PUT | `/mis-productos/v1/auth/cambiar-password` |
| POST | `/mis-productos/v1/auth/logout` |
| POST | `/mis-productos/v1/auth/refresh` |
| GET | `/mis-productos/v1/auth/validar` |

**Response del 403:**
```json
{
  "mensaje": "Debes cambiar tu contrasena temporal antes de continuar",
  "code": 404,
  "data": null,
  "lista": null
}
```

**Qué debe hacer el front:** al recibir `passwordTemporal: true` en el login, redirigir sí o sí a
la pantalla de cambio de contraseña. Si no, el usuario verá 403 en todo lo demás.

### 2. ⚠️ Cambiar la contraseña cierra TODAS las sesiones (incluida la propia)

Aplica a los tres caminos: `PUT /v1/auth/cambiar-password`, `POST /v1/auth/restablecer-password`
y el reseteo que hace un ADMIN desde el módulo de usuarios.

**Antes:** cambiar la contraseña no invalidaba nada; el refresh token seguía vivo 7 días.

**Ahora:** el refresh token muere en el instante. **Después de un cambio de contraseña exitoso, el
front debe mandar al usuario al login** — el siguiente `POST /v1/auth/refresh` va a responder 401.

Es intencional: es lo que hace que el caso "me entraron a la cuenta, cambio la contraseña"
realmente expulse al atacante.

### 3. `POST /v1/auth/logout` ahora invalida el token del lado del servidor

**Antes:** sólo borraba la cookie del navegador; el refresh token seguía siendo válido 7 días.
**Ahora:** además elimina la sesión en BD. No cambia el contrato (misma URL, mismo 200), pero el
logout ahora sí corta el acceso de verdad.

Efecto relacionado: **el refresh token rota de verdad**. Si el front llegara a reusar un refresh
token viejo (uno que ya fue rotado), el backend lo interpreta como token robado y **cierra la
sesión completa** → 401 y hay que volver a iniciar sesión. El interceptor no debe reintentar el
refresh con un token que ya usó.

### 4. 🚩 `X-Requested-With` en refresh y logout — pendiente de coordinar

Para cerrar el hueco de CSRF, `POST /v1/auth/refresh` y `POST /v1/auth/logout` pueden exigir el
header `X-Requested-With: XMLHttpRequest`. Sin él responden **403**.

**Viene APAGADO por defecto**, así que hoy no rompe nada. Se activa con
`seguridad.exigir-header-refresh: true` en el YML del ambiente.

**Acción para el front:** agregar el header a esas dos llamadas y avisar cuando esté desplegado.
Recién ahí se enciende en el backend — primero QA, después producción. Si se enciende antes, todos
los usuarios pierden la sesión a los 15 minutos (cuando expira el access token).

### 5. Cambios menores que el front puede notar

| Qué | Antes | Ahora |
|---|---|---|
| Contraseña mínima al **registrar / cambiar / restablecer** | 3 caracteres (el mensaje decía 6) | **8 caracteres**, mensaje corregido |
| Contraseña mínima en el **login** | 3 | sigue en 3 (no rompe a usuarios con contraseñas viejas) |
| `POST /v1/auth/restablecer-password` | sin límite de intentos | **429** tras 5 intentos por IP o por correo; el código se invalida a los 5 fallos |
| `POST /v1/auth/verificar-correo` | sin límite de intentos | **429** tras 5 intentos por IP o por usuario; el código se invalida a los 5 fallos |
| `POST /v1/auth/confirmar-cambio-correo` | sin límite | **429** tras 5 intentos; el código se invalida a los 5 fallos |
| `POST /v1/auth/enviar-codigo-verificacion` | límite sólo por usuario | ahora también por IP |
| `GET /v1/auth/validar` con un **refresh** token | respondía 200 "Token válido" | responde **401** (un refresh token no sirve para autenticar) |
| `POST /v1/auth/refresh` de un usuario deshabilitado o sin correo verificado | renovaba igual | **401** y limpia la cookie |

Los mensajes de error de código inválido/expirado ahora son **genéricos y iguales en todos los
casos**, para no revelar si un correo o un username existe. El front no debe intentar distinguir
"código incorrecto" de "correo no registrado" a partir del texto.

### 6. Al desplegar: todos los usuarios se deslogean una vez

Los refresh tokens actuales no tienen los datos nuevos (`jti` y `sessionId`), así que no se pueden
renovar: el primer `POST /v1/auth/refresh` después del despliegue responde 401 y hay que volver a
iniciar sesión. Es **de una sola vez**, pero conviene desplegar en horario de poco movimiento y
que el front maneje ese 401 mandando al login sin mostrar un error feo.

### ✅ Checklist para el front — qué tienen que hacer y cuándo

Resumen accionable de lo de arriba. Nada de esto está desplegado todavía (el backend está en `dev`,
sin commitear), así que hoy **nada se rompe**. Esto es para que lo tengan listo antes.

| ⬜ | Qué hacer | Urgencia | Qué pasa si no se hace |
|---|---|---|---|
| ⬜ | Al recibir `passwordTemporal: true` en el login, **redirigir sí o sí** a cambiar contraseña | 🔴 Antes del despliegue | El usuario recibe **403** en todos los endpoints salvo cambiar contraseña, y la app se ve rota |
| ⬜ | Después de un cambio de contraseña exitoso, **mandar al login** | 🔴 Antes del despliegue | El siguiente refresh da 401 y el usuario queda en una pantalla que no responde |
| ⬜ | Manejar el **401 del primer refresh tras el despliegue** mandando al login sin error feo | 🔴 Antes del despliegue | Todos los usuarios ven un error la primera vez (los tokens viejos ya no sirven) |
| ⬜ | Que el interceptor **no reintente** el refresh con un token que ya usó | 🟠 Antes del despliegue | El backend lo interpreta como token robado y **cierra la sesión completa** |
| ⬜ | Agregar el header `X-Requested-With: XMLHttpRequest` a `POST /v1/auth/refresh` y `POST /v1/auth/logout` | 🟡 Cuando puedan, y **avisar** | Nada por ahora: el backend lo tiene apagado hasta que confirmen |
| ⬜ | Revisar que la validación de contraseña en registro/cambio/reset pida **mínimo 8** caracteres | 🟡 Cuando puedan | El backend rechaza con 400 y el mensaje "La contrasena debe tener entre 8 y 200 caracteres" |

**Sobre el header `X-Requested-With`:** el orden importa. Primero el front lo despliega, luego avisa,
y recién ahí el backend lo empieza a exigir. Si se enciende antes, todos los usuarios pierden la
sesión a los 15 minutos (cuando expira su access token). No hay prisa.

**Lo que NO cambia:** las URLs, los shapes de request/response y el flujo de login siguen igual. Todo
lo de arriba es comportamiento, no contrato.

---

## ⚡ RENDIMIENTO DE BÚSQUEDAS Y CACHÉ DE STOCK — 2026-07-31 (informativo, sin acción obligatoria)

Tanda de optimización sobre los endpoints de catálogo y búsqueda. **Ningún cambio de contrato:**
mismas URLs, mismos parámetros, mismos responses. Pero hay un comportamiento que conviene que el
front conozca, porque **surgió de una pregunta concreta: "¿qué pasa si el cliente ve stock 1 pero
ya se vendió?"**.

### 1. El stock que devuelve el catálogo puede venir de caché

Esto **no es nuevo** (ya era así), pero nunca se había documentado y ahora aplica también a
`/tienda/v1/buscar`, que antes era el único que siempre pegaba a la base.

| Endpoint | Caché | Duración |
|---|---|---|
| `GET /mis-productos/v1/productos/obtenerProductos` | sí | hasta **1 h** |
| `GET /mis-productos/v1/productos/buscarNombreOrCodigoBarra` | sí | hasta **2 h** |
| `GET /mis-productos/v1/productos/findById/{id}` | sí | hasta **6 h** |
| `GET /mis-productos/tienda/v1/buscar` | **sí (nuevo)** | hasta **1 h** |
| `GET /mis-productos/tienda/v1/buscar-filtrado` | sí | hasta **1 h** |

**El caché se limpia automáticamente** cuando el admin crea, edita o elimina un producto o
variante, cuando cambian las imágenes, y cuando se mueve stock (venta, pedido, cancelación y
—desde hoy— también abonos). En uso normal el dato está fresco; la ventana de desfase aparece
sobre todo si el stock cambió por una vía que no pasa por esos flujos.

### 2. Lo importante: el stock mostrado es orientativo, el válido es el del pedido

**No hay riesgo de sobreventa.** Al crear un pedido, el backend bloquea la fila y revalida el stock
contra la base dentro de la transacción. El stock nunca queda negativo y nunca se vende algo que no
existe.

Lo que sí puede pasar es que **un cliente vea disponible algo que ya se agotó** y, al confirmar,
reciba:

```
400 — "Stock insuficiente en variante id 123. Disponible: 0, solicitado: 1"
```

**Recomendación para el front:** tratar ese 400 como un caso esperado, no como un error inesperado.
Lo ideal es mostrar un mensaje claro del tipo *"Este producto acaba de agotarse"* y refrescar la
vista del producto, en vez de un error genérico. El mensaje del backend ya trae la cantidad
disponible real, por si se quiere mostrar.

### 3. Nada que cambiar en el código del front

Las búsquedas deberían responder más rápido, sobre todo `/tienda/v1/buscar`, que antes no cacheaba
por un bug y pegaba a la base en cada llamada. No hay que tocar nada.

### 4. ¿Y si el front necesita el stock exacto en tiempo real?

Hoy no hay un endpoint sin caché para eso. Si en alguna pantalla hace falta (por ejemplo, un
detalle de producto justo antes de confirmar la compra), **avísennos y lo agregamos** — es un
cambio chico del lado del backend. Mientras tanto, la validación al crear el pedido es la garantía
real.

### Estado de despliegue

Desplegado en `dev` y `qa` el 2026-07-31. **Pendiente `main`** (producción).

---

## 🧪 GUÍA DE PRUEBAS DEL FRONT EN QA — antes de promover a producción (2026-07-30)

Lo que sigue está **desplegado en `qa` y todavía NO en producción** (producción sigue con la
versión del 23 de julio). Son 16 cambios del front del 24 al 30 de julio. Lo anotamos aquí por dos
razones: para que sepan qué hay en QA si entran a probar algo suyo, y por si quieren validar de su
lado los puntos donde el front consume algo que ustedes cambiaron.

**⚠️ Antes de probar:** abrir QA en ventana de incógnito o refrescar con `Ctrl+Shift+R`. Una
pestaña que ya estaba abierta no vuelve a pedir los archivos nuevos — es la causa habitual de
"ya lo subiste pero no lo veo".

### Resumen de lo que cambió

| Área | Cambio |
|---|---|
| Apariencia | Paleta de marca de azul/morado a **verde jade** (61 archivos). Ver sección anterior. |
| Estados de pedido | "Ir pagando" pasa de verde a **azul** (chocaba con "Pagado" al volverse verde la marca) |
| Pedidos | Paginación real para admin, filtro por tipo, filtro por lugar, resumen de filtros activos |
| Pedidos | Datos de entrega (receptor, dirección, fecha, lugar, Facebook) desde la tarjeta |
| Pedidos | Cancelar pedidos ya entregados/pagados (devolución) — solo admin |
| Pedidos | Historial de pagos en el detalle; total se recalcula al quitar una línea |
| Catálogo nuevo | Lugares de entrega (CRUD admin con paginación) |
| Tienda | Ruta y prefijo de API de `/variantes` a `/tienda` (ya sincronizado con ustedes) |
| Carrito | El stock visible baja al agregar al carrito |
| Móvil | Filtros ya no se traslapan; productos de 2 en 2 |

### Pruebas — las críticas primero

Marcadas 🔴 las que, si fallan, bloquean la promoción a producción.

**Apariencia**
- 🔴 Ningún botón azul suelto en Tienda, Pedidos, Venta directa, Créditos/Abonos ni Usuarios.
- 🔴 Nada de texto ilegible: en modo oscuro el verde es brillante, así que las letras encima van
  oscuras (revisar botones y el número de página resaltado de las tablas).
- Modo claro y modo oscuro con el botón 🌙/☀️ del menú.
- Los 4 estados se distinguen: **Apartado ámbar · Ir pagando azul · Pagado verde · Cancelado rojo**.
- El login sigue azul/morado **a propósito** — no es un descuido.

**Pedidos** (`/pedidos/mis-pedidos`, requiere admin)
- 🔴 La paginación funciona. Antes como admin solo se veían los primeros 10 pedidos y no había
  forma de ver el resto.
- Filtro por tipo (Normal / Apartados / Ir pagando), solo o combinado con el de lugar.
- Filtro por lugar de entrega (autocomplete).
- El resumen de filtros activos aparece cuando hay filtros y desaparece cuando no.
- Buscar un pedido por su número — **esto usa el cambio suyo** de buscar por `pedido.id` cuando el
  término es numérico.
- Datos de entrega: llenar, guardar y reabrir. Debe persistir todo y mostrar "Recibe: …" en la
  tarjeta.
- Cobrar un pedido a crédito ofrece ir a Créditos/Abonos, **no** abre el diálogo de forma de pago
  (que terminaba en el 404 de "se liquidan mediante abonos").
- Cancelar un pedido ya Entregado como admin (devolución): debe permitirlo, pedir motivo sin
  ofrecer "No se presentó", y devolver el stock. **Esto usa el cambio suyo** del 400 + mensaje.
- El total del detalle se corrige al quitar una línea.
- Historial de pagos visible en el detalle de un pedido a crédito con abonos.
- Imprimir/enviar comprobante deshabilitado mientras no haya ningún pago.

**Lugares de entrega** (Inventario → Lugares de entrega)
- Alta, edición y borrado. **Confirmar que el borrado de verdad elimina** y no reaparece al
  recargar — hubo dos correcciones seguidas ahí (el body del DELETE y el shape del getAll).
- 🔴 Que la lista salga **completa** en Venta directa y en el modal de Entrega, no solo los
  primeros. El bug del shape dejaba la lista vacía sin ningún error visible.
- La tabla del catálogo pagina.

**Tienda** (`/tienda/buscar`)
- 🔴 El catálogo carga y busca por nombre y por código. El renombre de `/variantes` a `/tienda`
  tocó **todas** las llamadas de ese módulo, así que es la prueba de humo del cambio coordinado.
- Detalle de producto, edición, imágenes y venta directa responden con normalidad.
- El stock visible baja al agregar al carrito.
- La tabla del carrito y su paginador ya toman los colores del sistema (antes el pie salía blanco).

**Móvil** (probar en teléfono real, no solo achicando la ventana)
- Los 8 filtros de admin se ven completos, sin texto encimado ni cortado.
- Los productos se ven de 2 en 2 por fila.

### Qué haremos si algo falla

Se corrige en `qa` y se vuelve a probar; producción no se toca hasta que esté limpio. Si el
problema resulta ser de su lado, lo anotamos aquí como siempre.

---

## ✅ Front: tanda de fixes de UI reportados en QA (2026-08-01)

Ronda de 9 bugs reportados de un jalón al probar QA. 7 eran 100% front (colores, paginación,
UX de inputs, cálculo mostrado en un ticket) — ya corregidos y en `dev`/`qa`. Quedan 2 puntos
que sí necesitan algo de su lado, abajo en detalle.

**Resumen de lo ya corregido (sin acción de su lado):**
- Los modales de confirmación (SweetAlert2) salían con el botón morado por defecto de la
  librería en vez del verde jade de la marca — nunca se había sobreescrito ese color
  específico. Corregido de forma global.
- Botón "Tomar foto" (carga de imágenes) con texto blanco ilegible sobre el verde brillante del
  modo oscuro — mismo patrón de contraste ya conocido, faltaba aplicarlo ahí.
- Catálogo de categorías (`palabras-clave`) no tenía paginación — clonado el mismo patrón que
  ya usa "Lugares de entrega".
- Botón azul suelto en la pantalla de Gastos — quedó fuera de las migraciones de paleta
  anteriores.
- Inputs de precio/monto ahora seleccionan su contenido completo al enfocarlos, para no tener
  que borrar el "0" a mano antes de escribir.

## ❓ CONSULTA AL BACK — `POST /v1/abonos/{pedidoId}`: ¿`saldoRestante` refleja el saldo antes o
## después del abono que se acaba de registrar?

**Reportado por el usuario, con un ejemplo concreto:** pedido con total $300. Ya se habían
abonado $100 antes. Se registra un abono nuevo de $100 hoy. El ticket impreso mostró:

```
TOTAL: $300.00
Abonos previos: $100.00
Abono de hoy: $100.00
Saldo pendiente: $200.00     ← debería ser $100.00 (300 - 100 - 100)
```

**Lo que encontramos revisando el front:** el código construye ese ticket usando
`data.saldoRestante` (el campo que ustedes devuelven en la respuesta de
`POST /v1/abonos/{pedidoId}`) para actualizar el saldo mostrado. El número que salió en el
ticket ($200) coincide exactamente con lo que habría sido el saldo **antes** de este abono
(300 - 100 = 200) — nunca con el saldo real después (100). Eso encaja con que
`saldoRestante` esté llegando calculado sobre el estado previo a persistir el abono, en vez de
sobre el estado ya actualizado.

**No estamos 100% seguros de que el problema esté de su lado** — no descartamos que el usuario
haya probado contra una versión vieja cacheada del front (nos ha pasado antes). Por eso ya
corregimos el front para que **no dependa de este campo para el cálculo** — ahora el saldo
mostrado en el ticket se calcula siempre en el front (saldo que ya teníamos cargado, menos el
monto que se acaba de abonar), y `saldoRestante` del back ya no se usa para el número, solo
`estadoPedido` para saber si quedó liquidado. Así que **no es bloqueante para nosotros**.

Aun así, si `saldoRestante` sí está devolviendo el estado previo en vez del posterior, vale la
pena que lo revisen — puede estar afectando otras pantallas o reportes que sí confíen
directamente en ese número tal cual lo mandan.

## ❓ CONSULTA AL BACK — filtro por estado (Pagado/Cancelado) en `buscarClientePedido`

En `mis-pedidos` ya tenemos filtro por tipo de pedido (Normal/Apartado/Ir pagando, vía
`&tipoPedido=`) y por lugar de entrega. El usuario pidió agregar también un filtro por
**estado** — específicamente para ver de un vistazo los pedidos ya **Pagados** y los
**Cancelados**, junto a los filtros que ya existen.

`GET /v1/pedidos/buscarClientePedido` hoy no tiene ningún parámetro para filtrar por
`estado_pedido`. Como la pantalla ya usa paginación real del servidor, no es viable resolverlo
filtrando en el front lo que ya llegó de una página — daría resultados incompletos.

**¿Podrían agregar un parámetro nuevo, mismo patrón que `tipoPedido`?** Por ejemplo
`&estadoPedido=PAGADO` / `&estadoPedido=Cancelado` (repetible si hace falta combinar). En
cuanto exista, conectarlo del lado del front es inmediato — el patrón de botones toggle ya está
armado, solo hace falta el parámetro nuevo.

No es urgente — es una mejora, no bloquea nada de lo que ya funciona.

### Precisión del pedido (confirmado con el usuario, 2026-08-01)

Para que quede sin ambigüedad qué esperamos del lado del front una vez que exista el parámetro:

- **Dos botones nuevos** en la barra de filtros de `mis-pedidos`, junto a los que ya existen
  (Normal / Apartados / Ir pagando):
  - **"✅ Pagados"** → pide `estado_pedido = PAGADO`
  - **"❌ Cancelados"** → pide `estado_pedido = Cancelado`
- Se combinan con **AND** con los filtros que ya existen (tipo de pedido + lugar de entrega),
  igual que ya funciona hoy entre esos dos — ej. "Apartados" + "Cancelados" a la vez → apartados
  que fueron cancelados.
- Mismo estilo visual de pastilla/toggle que los filtros actuales, nada nuevo de diseño.

Con el parámetro `&estadoPedido=` confirmado (nombre exacto, valores esperados —
`PAGADO`/`Cancelado`, tal como ya usa el campo `estado_pedido` hoy — o si prefieren otro valor,
avisen), lo conectamos de inmediato.

---

## ✅ RESPUESTA DEL BACK — `saldoRestante` en `POST /v1/abonos/{pedidoId}` es **posterior** al abono

**Respuesta corta: es el saldo DESPUÉS de aplicar el abono que se acaba de registrar.** Ese es el
contrato y no cambia. Para el ejemplo que pusieron (total $300, $100 abonado antes, $100 hoy) el
back debe devolver `saldoRestante: 100.0`, nunca 200.

El cálculo vive en `AbonoServiceImpl.registrarAbono` y ocurre así, todo dentro de la **misma
transacción**:

1. Lee el `total_pagado` que el pedido ya traía (los $100 previos).
2. Suma el abono nuevo → `nuevoTotalPagado` ($200) y lo guarda en el pedido.
3. Si con eso se cubre el total, marca el pedido `PAGADO` y genera la venta.
4. Hasta el final calcula `saldoRestante = total_pedido - nuevoTotalPagado`, con piso en 0 (nunca
   devuelve negativo aunque el redondeo quedara justo).

O sea que el número sale del estado **ya actualizado**, no del previo. Además, el endpoint valida
antes de guardar que el monto no exceda el saldo pendiente, así que un abono que dejaría saldo
negativo se rechaza con 400 en vez de devolver un saldo raro.

### Entonces, ¿de dónde salió el $200 del ticket?

Revisamos las dos únicas rutas del back que crean abonos (registrar y transferir) y las dos
mantienen `total_pagado` en sincronía. Con el código actual, ese $200 solo se explica de dos formas:

1. **Front cacheado** — la hipótesis que ustedes mismos plantearon. Es la más probable.
2. **Dato desfasado en BD** — que ese pedido específico tuviera `total_pagado = 0` aunque ya
   existiera el renglón del abono previo de $100 (por ejemplo, un abono insertado por SQL a mano, o
   un pedido anterior a la migración del módulo de abonos). En ese caso el back habría leído
   `total_pagado = 0`, y $300 − $100 = **$200**: cuadra exacto con lo que vieron.

Para descartar el caso 2 dejamos del lado del back un script de diagnóstico
(`diagnostico_total_pagado_vs_abonos.sql`) que lista los pedidos donde `total_pagado` no cuadra con
la suma real de sus abonos. Lo corremos en QA y producción; si aparece algo, lo corregimos por dato
y lo anotamos aquí. **No requiere nada de su parte.**

### Sobre su fix del front

El cambio que hicieron (calcular el saldo del ticket en el front y usar `saldoRestante` solo para
`estadoPedido`) **está bien y no hay que revertirlo** — nos parece correcto que el ticket no dependa
de un solo campo. Ojo con un detalle: si el saldo lo calculan contra el que tenían cargado en
pantalla, ese valor también puede estar viejo si el pedido se movió en otra sesión. `saldoRestante`
del back sí es siempre el valor recién persistido, así que si en algún momento quieren volver a
usarlo como fuente, pueden — es confiable.

---

## ✅ RESPUESTA DEL BACK — filtro por estado en `buscarClientePedido`: **ya está, úsenlo**

Lo agregamos con el mismo patrón que `tipoPedido`, tal como lo pidieron. Ya está implementado y
compilando en `dev`; **todavía no está desplegado en QA** — avisamos aquí en cuanto suba a `qa` y
después a producción. Vayan armando los botones, el contrato de abajo ya es el definitivo.

**Request:**

```
GET /mis-productos/v1/pedidos/buscarClientePedido
    ?buscar=
    &size=10
    &page=0
    &estadoPedido=PAGADO
    &estadoPedido=CANCELADO      ← repetible, igual que tipoPedido
    &tipoPedido=APARTADO         ← opcional, se combina con AND
    &lugarEntregaId=3            ← opcional, se combina con AND
```

- **Nombre del parámetro:** `estadoPedido` (confirmado — el que propusieron).
- **Repetible:** sí. Varios valores del mismo parámetro se combinan con **OR entre ellos**
  (`estadoPedido=PAGADO&estadoPedido=CANCELADO` = pagados *o* cancelados).
- **Combinación con los otros filtros:** **AND**, exactamente como pidieron. "Apartados" +
  "Cancelados" → apartados que fueron cancelados.
- **Si se omite:** no filtra por estado. Comportamiento idéntico al de hoy — el cambio es
  retrocompatible, no tienen que tocar nada si no quieren el filtro.
- **Response:** el mismo `PageableDto` de siempre, sin campos nuevos. La paginación sigue siendo
  del servidor y ya cuenta solo los pedidos que pasan el filtro.

### Valores — no se preocupen por mayúsculas/minúsculas

En la BD el campo `estado_pedido` quedó con mayúsculas inconsistentes según qué parte del sistema
escribió el pedido (`PAGADO`, `cancelado`, `Entregado`, `APARTADO`, `FIADO`). Para que eso no se les
vuelva un problema, **la comparación es case-insensitive**: `CANCELADO`, `Cancelado` y `cancelado`
traen lo mismo. Manden el valor como lo tengan a la mano.

Valores que existen hoy y qué significa cada uno:

| Valor | Qué es |
|---|---|
| `PAGADO` | Crédito (apartado o fiado) ya liquidado |
| `CANCELADO` | Pedido cancelado, sea por no recogerse o por devolución |
| `ENTREGADO` | Venta al contado ya entregada |
| `APARTADO` | Apartado activo, todavía con saldo |
| `FIADO` | "Ir pagando" activo, todavía con saldo |

Para los dos botones que describieron: **"✅ Pagados"** → `estadoPedido=PAGADO`;
**"❌ Cancelados"** → `estadoPedido=CANCELADO`.

Una precisión sobre "Cancelados": el estado `CANCELADO` no distingue el motivo (no se presentó vs.
devolución de un pedido ya pagado). Los dos caen en el mismo estado. Si más adelante quieren
separarlos en la pantalla, el pedido ya guarda `motivo_cancelacion` y `fecha_cancelacion` —
díganos y los exponemos en el listado.

---

## ✅ Front: confirmado — filtro `estadoPedido` conectado y probado en QA (2026-08-01)

Cerramos el loop de la consulta anterior. Conectamos los botones "✅ Pagados"/"❌ Cancelados" en
`mis-pedidos` contra `&estadoPedido=PAGADO`/`&estadoPedido=CANCELADO` con el contrato que
confirmaron (repetible, OR entre sí, AND contra tipo/lugar, case-insensitive) y el usuario ya lo
probó en QA — funciona correctamente. Sin pendientes de nuestro lado en este punto.

De paso, encontramos y corregimos un bug relacionado en el front (100% nuestro, sin acción de su
lado): el botón "Cobrar" de la card de `mis-pedidos` seguía habilitado en pedidos ya con
`estado_pedido = PAGADO` — el `[disabled]` solo comparaba contra `'Entregado'` (venta normal), sin
contemplar el estado de un crédito ya liquidado. Al hacer clic mandaba a `/abonos`, que
correctamente respondía "este pedido ya está pagado" — el bug era que el botón nunca debió estar
clickeable ahí. Ya corregido: el botón se deshabilita también cuando el pedido está `PAGADO` o
`Cancelado`.

---

## ❓ Ayuda — botón "Tomar foto" (Carga rápida de imágenes) sigue sin verse bien, no encontramos
## nada del lado del front que lo explique

**Pantalla:** `/carga-imagenes` (admin) → botón "📷 Tomar foto" (abre la cámara en celular vía
`<input type="file" capture="environment">`, junto a "🖼️ Elegir de galería o PC").

**Lo que ya intentamos (2 rondas), sin resultado:** el reporte inicial era de contraste — texto
blanco fijo sobre el verde jade brillante del modo oscuro, ilegible. Se corrigió usando la
variable de contraste ya establecida en el proyecto (`var(--app-accent-ink)`, que da blanco en
modo claro y texto oscuro en modo oscuro — mismo patrón ya usado en otros botones). Verificado en
el código que el fix está en la rama desplegada. El usuario probó de nuevo y sigue reportando
"no se ve como tal Tomar foto" — sin poder confirmar todavía si es de nuevo contraste, el emoji
📷 no renderizando en su navegador/SO, o algo funcional (en escritorio ese input no abre cámara,
solo el selector de archivos normal — capture="environment" es soporte de navegador/dispositivo,
no algo que nuestro código controle).

**Por qué lo anotamos aquí en vez de solo en el front:** revisamos el HTML/CSS del botón
completo — es un `<label>` con un `<input type="file" hidden>` adentro, sin ninguna llamada al
back ni condición que dependa de datos del servidor. **No encontramos ninguna forma en que esto
dependa de algo de su lado** — lo anotamos por transparencia y por si ustedes ven algo que a
nosotros se nos esté escapando (¿alguna respuesta de otro endpoint de esta pantalla que pudiera
estar rompiendo el render de la página completa, por ejemplo?), no porque tengamos una pista
concreta de que sea un tema de backend.

**Seguimos sin poder reproducirlo nosotros** — pendiente de una captura de pantalla del usuario
para diagnosticarlo con precisión en vez de seguir adivinando.

---

## ⏸️ PAUSADO — todo lo de Facebook se sacó de `dev`/`qa` (2026-08-05)

**Los endpoints `POST /v1/redes-sociales/facebook/publicar` y `POST /v1/redes-sociales/facebook/publicar-video`
YA NO EXISTEN en `dev` ni en `qa` a partir de ahora.** Se decidió pausar el feature completo —
código, config y tabla `publicacion_social` — mientras se resuelve la configuración de la app de
Meta (¡gracias por resolver lo de la Política de Privacidad, ver la sección de ustedes más abajo
para las preguntas pendientes de ese punto — siguen vigentes, esto no las cambia!).

**Nada se perdió**: todo el código quedó respaldado en la rama `backup/facebook-redes-sociales`
de `proyecto_key`, listo para retomarse cuando se reactive el trabajo. Si mientras tanto conectan
algo contra estos endpoints en QA, van a ver 404 — no es un bug, es que efectivamente no está
desplegado.

Dejamos toda la documentación de abajo (contrato de los endpoints, flujo de pantalla, respuestas
a sus preguntas, y la sección de ustedes sobre la Política de Privacidad) **tal cual**, como
referencia para cuando se retome — no hace falta rehacerla, solo va a volver a aplicar cuando el
código regrese a `dev`/`qa`.

---

## 📘 Endpoint nuevo — Publicar variante en Facebook (2026-08-05, actualizado 2026-08-05)

Primer paso de la integración con redes sociales: publicar la foto de una variante en la página
de Facebook del negocio. Solo Facebook por ahora — Instagram y TikTok quedan para después (Instagram
comparte casi el mismo trámite de permisos de Meta, así que se agrega con poco esfuerzo cuando se
necesite; TikTok es un ecosistema aparte).

**Alcance actual — solo foto, solo Facebook feed:** este endpoint únicamente publica una **foto**
en el feed normal de la página (`tipoPublicacion` siempre sale `"foto"` en la respuesta).
**Video, Historias y Reels NO están implementados todavía** — la Graph API los maneja con flujos
completamente distintos (subida de video por partes/resumable, las historias ni siquiera aceptan
comentarios públicos). Si se necesitan, es trabajo aparte — avisen para dimensionarlo.

**Solo ADMIN.**

**⚠️ Cambió el Content-Type: ahora es `multipart/form-data`, no JSON** (se necesitaba para poder
mandar un archivo de imagen en el mismo request — ver `imagenNueva` abajo).

**Request:**
```
POST /v1/redes-sociales/facebook/publicar
Authorization: Bearer <token>
Content-Type: multipart/form-data

varianteId: 270
descripcion: Mochila Prada, talla única, color negro. Código: 7501234567890
imagenId:                     (opcional, texto vacío = omitir el part)
scheduledPublishTime:         (opcional)
imagenNueva:                  (opcional, part de tipo archivo)
```
- `varianteId` (requerido): variante del catálogo a publicar.
- `descripcion` (requerido): el texto que termina como caption del post en Facebook, **tal cual
  se manda, sin ningún procesamiento del back**. Ver la sección de código de barras más abajo.
- `imagenId` (opcional): usar una imagen específica **ya guardada** de esa variante (distinta a
  la principal). Se ignora si se manda `imagenNueva`.
- `imagenNueva` (opcional, **nuevo**): archivo de imagen que el admin selecciona en el momento
  (de PC, galería o cámara) **solo para esta publicación** — no se guarda en la galería de la
  variante ni pasa por el microservicio de imágenes, se manda tal cual a Facebook. Ver sección de
  calidad abajo. Si se manda, gana sobre `imagenId`/imagen principal.
- Si no se manda ni `imagenId` ni `imagenNueva`: se usa la imagen principal ya guardada de la
  variante.
- `scheduledPublishTime` (opcional, `LocalDateTime` ISO): si se omite, se publica de inmediato.
  Si se manda, programa la publicación en Facebook (mínimo 10 minutos, máximo 6 meses a futuro —
  fuera de esa ventana responde 400).

**Límite de tamaño:** el micro acepta hasta **25 MB** por archivo/request (`imagenNueva` incluida)
— configurado así a propósito para no capar fotos de cámara a resolución completa.

**Response 200:**
```json
{
  "mensaje": "La peticion fue exitosa",
  "code": 200,
  "data": {
    "id": 1,
    "varianteId": 270,
    "plataforma": "facebook",
    "tipoPublicacion": "foto",
    "descripcionPublicada": "Mochila Prada, talla única, color negro. Código: 7501234567890",
    "postIdFacebook": "122100000000000_987654321",
    "scheduledPublishTime": null,
    "fechaPublicacion": "2026-08-05T18:30:00",
    "estado": "PUBLICADA"
  }
}
```
`estado` es `"PUBLICADA"` cuando se publicó de inmediato o `"PROGRAMADA"` cuando se mandó
`scheduledPublishTime`.

**Errores:**
- **400** — la variante no tiene ninguna imagen guardada (y no se mandó `imagenNueva`), la
  variante/imagen no existe, la ventana de `scheduledPublishTime` es inválida, el archivo excede
  25 MB, o Facebook rechazó la publicación (credenciales no configuradas, token vencido, o la
  página no tiene los permisos `pages_manage_posts` aprobados por Meta todavía — mientras la app
  de Meta esté en modo desarrollo, Facebook solo acepta publicar en páginas donde el usuario
  dueño del token esté agregado como Admin/Developer/Tester de la app).

### Sobre la calidad de la imagen (duda que surgió: "en variantes se le quita calidad")

Se revisó el pipeline completo (este micro + microservicio de imágenes) y **el archivo original
nunca se comprime ni se redimensiona al guardarse** — se escribe a disco tal cual llega. Lo único
que se redimensiona es un **thumbnail aparte** (cacheado en una carpeta separada), usado
únicamente para las listas/búsquedas por velocidad — el original queda intacto y es al que este
endpoint de Facebook accede siempre (tanto con `imagenId` de una imagen ya guardada como con
`imagenNueva`). Si en la pantalla de variantes se percibe una foto más "pesada"/pixelada de lo
esperado, no es el back bajándole calidad al guardar — revisen si el propio navegador/celular
comprime la imagen **antes** de subirla (común en inputs de cámara), eso está fuera del alcance
de este endpoint. Para este flujo de Facebook específicamente, usar `imagenNueva` es la forma de
garantizar 100% que se manda exactamente el archivo que el admin seleccionó, sin que pase por
ningún guardado intermedio.

### Código de barras y descripción — cómo se usa

El campo `descripcion` es **texto libre, sin ningún parseo ni validación especial del back** —
lo que llegue ahí es exactamentre lo que Facebook muestra como caption del post. El código de
barras **no es un campo aparte**: es simplemente parte de ese texto por convención (para que un
cliente pueda escribirlo y pedir ese producto exacto), así que si quieren incluirlo lo concatenan
ustedes al armar el valor de `descripcion` antes de mandarlo (ver sugerencia en el flujo de
pantalla abajo). Tengan presente que como es texto plano en un post público, el código de barras
quedaría visible para cualquiera que vea la publicación — si no quieren eso, simplemente no lo
incluyan al armar la descripción.

Como el alcance actual es solo "foto" (feed normal), no hay todavía una distinción de "qué
descripción lleva cada tipo de contenido" — cuando se agregue video/reel/historia, cada una va a
tomar el mismo criterio (un `message`/caption de texto libre), pero cada tipo tiene su propio
límite de caracteres y comportamiento en Facebook que hay que confirmar en su momento (las
historias, por ejemplo, no muestran caption de la misma forma que un post normal).

### Flujo sugerido de la pantalla — "Publicar en Facebook" (admin)

No es un endpoint nuevo de por sí, es cómo se pensó que se arme la pantalla con lo que ya existe
más el endpoint de arriba:

1. **Buscar variante** — reutilizar el buscador que ya existe:
   `GET /tienda/v1/buscar?...` (paginado, con imagen incluida). No hace falta ningún endpoint
   nuevo para esto.
2. **Al elegir una variante** — precargar:
   - Imagen principal, para el preview (la que ya se muestra hoy en cualquier tarjeta de variante).
   - Un textarea con la descripción sugerida = `variante.descripcion` + " Código: " +
     `producto.codigoBarras` (si tiene). **Editable libremente** — lo que quede ahí es lo que se
     manda como `descripcion` al publicar, tal cual (ver sección de arriba).
3. **Elegir la imagen** — tres opciones para el admin:
   - Usar la principal (default, no mandar nada).
   - Elegir otra ya guardada de esa variante, de las que trae `GET /tienda/v1/imagenes/{varianteId}`
     → mandar su id como `imagenId`.
   - **Subir una nueva** (botón "usar otra foto solo para esta publicación") → adjuntarla como
     `imagenNueva`. Útil cuando quieren una foto a mejor calidad/ángulo que la que ya está
     guardada en el catálogo, sin tener que agregarla permanentemente a la variante.
4. **Publicar ahora vs. Programar** — un toggle simple:
   - Ahora (default): no mandar `scheduledPublishTime`.
   - Programar: un date-time picker; validar en el front que sea al menos 10 minutos en el
     futuro y no más de 6 meses (el back también lo valida y devuelve 400 si se pasan, pero es
     mejor no dejar mandar la petición si ya se sabe que va a fallar).
5. **Botón "Publicar"** → `POST /v1/redes-sociales/facebook/publicar` (multipart) con los campos
   de arriba.
6. **Resultado:**
   - Éxito con `estado: "PUBLICADA"` → mostrar confirmación; si quieren armar un link directo al
     post, es `https://www.facebook.com/{postIdFacebook}`.
   - Éxito con `estado: "PROGRAMADA"` → mostrar "Se publicará el {scheduledPublishTime}".
   - Error 400 → mostrar el `mensaje` del `ResponseGeneric` tal cual, ya viene en español y
     explica la causa (sin imagen, Facebook rechazó, archivo muy grande, etc.), no hace falta
     traducirlo.

**Lo que NO existe todavía** (por si lo dan por hecho): no hay endpoint para listar publicaciones
ya hechas de una variante ni para editarlas/borrarlas de Facebook desde acá — cada llamada a
`/facebook/publicar` crea una nueva. Tampoco hay Historia/Reel (ver endpoint de video abajo, es
lo único agregado además de foto). Si quieren Historia o Reel, avisen y se dimensiona aparte —
son procesos de la Graph API en 2 pasos, más trabajo que foto/video.

---

## 📘 Endpoint nuevo — Publicar VIDEO de una variante en Facebook (2026-08-05)

Segunda pieza de la integración con redes sociales, hermano del endpoint de foto de arriba.
Publica un video en el feed normal de la página (`POST /{page-id}/videos` de la Graph API).

**Diferencia importante con el de foto:** el catálogo **no guarda video de variantes** — no
existe "video principal" al que caer como con las fotos. Por eso el archivo es **obligatorio en
cada llamada**, y nunca se guarda en el microservicio de imágenes ni en ningún lado del catálogo,
es exclusivo de esa publicación.

**Solo ADMIN.**

**Request:**
```
POST /v1/redes-sociales/facebook/publicar-video
Authorization: Bearer <token>
Content-Type: multipart/form-data

varianteId: 270
descripcion: Mochila Prada, talla única, color negro. Código: 7501234567890
scheduledPublishTime:         (opcional)
video:                        (requerido, part de tipo archivo)
```
- `varianteId` (requerido): variante del catálogo a la que se asocia el video (para el registro
  interno de auditoría; el video en sí no queda ligado a la variante en ningún otro lado).
- `descripcion` (requerido): caption del video, mismo criterio que en el de foto — texto libre,
  sin parseo del back.
- `video` (requerido): el archivo. Se manda a Facebook tal cual, sin comprimir ni convertir.
- `scheduledPublishTime` (opcional, igual que en foto: mínimo 10 min, máximo 6 meses a futuro).

**Límite de tamaño:** el micro ahora acepta hasta **200 MB** por archivo/request (subimos el
límite general del micro de 25 MB a 200 MB específicamente para poder soportar video a calidad
completa; aplica también al endpoint de foto, sin problema, las fotos no se van a acercar a eso).

**Response 200:** mismo shape que el de foto —
```json
{
  "mensaje": "La peticion fue exitosa",
  "code": 200,
  "data": {
    "id": 2,
    "varianteId": 270,
    "plataforma": "facebook",
    "tipoPublicacion": "video",
    "descripcionPublicada": "Mochila Prada, talla única, color negro. Código: 7501234567890",
    "postIdFacebook": "9876543210",
    "scheduledPublishTime": null,
    "fechaPublicacion": "2026-08-05T19:10:00",
    "estado": "PUBLICADA"
  }
}
```
Ojo: para video, `postIdFacebook` es el **id del video**, no un `post_id` de post normal —
Facebook no siempre devuelve un `post_id` separado para publicaciones de video. Para armar un
link, `https://www.facebook.com/{postIdFacebook}` también funciona con el id del video.

**Errores:**
- **400** — falta el archivo `video`, la variante no existe, ventana de `scheduledPublishTime`
  inválida, archivo excede 200 MB, o Facebook rechazó el video (mismas causas que en foto:
  credenciales, permisos, formato no soportado).

**A tener en cuenta:** subir un video pesado puede tardar bastante — el back espera hasta
**5 minutos** antes de dar timeout hacia Facebook. Si tienen spinner/loading en el botón de
publicar, que aguante ese tiempo sin asumir que se colgó.

---

## ✅ Front: implementada la pantalla "Publicar en Facebook" (2026-08-05)

Conectados los **2 endpoints** de las secciones de arriba. Pantalla nueva en
**`/admin/facebook`** (solo ADMIN, link "📘 Publicar en Facebook" dentro del menú 🛠️ Sistema).

### Qué quedó

1. **Buscar producto** — reutiliza `GET /tienda/v1/buscar` como sugirieron, sin endpoint nuevo.
2. **Descripción sugerida y editable** — se arma con nombre, descripción, talla/color/marca,
   precio y **código de barras**. Lo dejamos incluido por defecto (es lo que le sirve al cliente
   para pedir el producto exacto), pero la pantalla avisa que queda público y el admin lo puede
   borrar antes de publicar.
3. **Elegir imagen** — las 3 opciones: la principal, otra ya guardada
   (`GET /tienda/v1/imagenes/{varianteId}` → se manda su id como `imagenId`), o subir una nueva
   (`imagenNueva`, con botones separados para galería y cámara).
4. **Video** — archivo obligatorio, con preview antes de publicar.
5. **Ahora vs. Programar** — la fecha se valida en el front con los mismos límites de ustedes
   (mín. 10 min, máx. 6 meses) para no mandar peticiones que ya se sabe que van a dar 400.
6. **Resultado** — botón "Ver en Facebook" con `https://www.facebook.com/{postIdFacebook}`, y
   para `PROGRAMADA` se muestra la fecha. Los 400 se muestran con su `mensaje` tal cual, como
   sugirieron (ya viene en español).

### Detalles de implementación por si les sirve saberlo

- **Los campos opcionales se OMITEN del multipart, no se mandan vacíos.** Entendimos que mandar
  `imagenId` con string vacío haría que el back lo tome como valor y descarte la imagen
  principal. **Si en realidad ustedes ya tratan el string vacío como "no vino", avísennos** —
  igual funciona como lo dejamos, es solo para confirmar el supuesto.
- **Barra de progreso real de subida**, y una fase aparte para cuando el archivo ya llegó
  completo pero ustedes todavía lo están mandando a Facebook (esos hasta 5 minutos que
  mencionaron). Ahí la barra se queda al 100% con el texto "Enviándolo a Facebook…" para que no
  parezca colgado.
- Estas 2 llamadas quedaron excluidas de nuestro overlay global de carga — si no, la app entera
  se bloqueaba varios minutos subiendo un video.

### ❓ 2 preguntas

1. **¿Ya está aprobado `pages_manage_posts` en la app de Meta, o sigue en modo desarrollo?**
   Lo preguntamos porque ustedes mismos lo advirtieron: mientras esté en desarrollo, Facebook
   solo acepta publicar en páginas donde el dueño del token esté agregado como
   Admin/Developer/Tester. **Necesitamos saberlo para poder probar esto en QA** — si todavía no,
   lo único que vamos a ver es el 400 y no podemos validar el camino feliz.

2. **¿El `scheduledPublishTime` lo interpretan en la zona horaria del servidor?** Mandamos un
   `LocalDateTime` sin offset (ej. `2026-08-05T18:30:00`), tomado de la hora local del navegador
   del admin. Si el servidor corre en UTC, una publicación "a las 6 pm" se programaría 6 horas
   corridas. Confírmennos qué zona asumen y, si hace falta, les mandamos la hora ya convertida.

**Estado:** en `dev`, compila sin errores. **No probado en vivo todavía** — depende de que su
lado esté desplegado y del punto 1 de arriba.

---

## ✅ Front: cerrado el checklist de seguridad de autenticación (2026-08-05)

Atendidos los **6 puntos** de la sección 🔐 del 2026-07-31. Ya está en `dev`, compila sin
errores. Resumen de cada uno:

| # | Punto | Estado |
|---|---|---|
| 1 | Forzar cambio con contraseña temporal | ✅ Hecho — **pero ver la pregunta de abajo** |
| 2 | Tras cambiar contraseña → al login | ✅ Hecho (estaban mal 3 de 4 lugares) |
| 3 | 401 del primer refresh → login sin error feo | ✅ Hecho |
| 4 | No reintentar el refresh con un token ya usado | ✅ Reforzado |
| 5 | Header `X-Requested-With` | ✅ Ya lo mandamos — **falta que lo enciendan, ver abajo** |
| 6 | Mínimo 8 caracteres | ✅ Ya estaba, verificado en los 5 formularios |

### 🚩 1. `POST /v1/auth/refresh` y `/logout` YA reciben `X-Requested-With: XMLHttpRequest`

Como pidieron, respetamos el orden: **ya lo desplegamos nosotros primero**. Cuando esto llegue
a QA/producción pueden encender `seguridad.exigir-header-refresh: true` cuando quieran.
Avísennos y lo confirmamos del lado del navegador.

### ❓ 2. ¿El login devuelve `passwordTemporal` o `debeCambiarPassword`?

En su documento del **2026-07-04** el campo se llamaba `debeCambiarPassword`, y en el del
**2026-07-31** aparece como `passwordTemporal`. No sabemos si lo renombraron o si son dos
campos distintos que conviven.

**Por ahora leemos los dos** (`passwordTemporal ?? debeCambiarPassword`), así que funciona
pase lo que pase. Pero nos gustaría confirmarlo para quitar el que sobre — no es cosmético: si
el front se queda con el nombre equivocado, el usuario recibe **403 en todos los endpoints** y
la app se ve completamente rota sin ninguna pista de la causa.

### ℹ️ 3. Detalle de lo que corregimos en el punto 2, por si les sirve

Encontramos que **3 de los 4 lugares** donde se cambia la contraseña dejaban al usuario dentro
de la app. El peor era el modal forzado del login: tras el cambio entraba directo al catálogo
con la sesión ya muerta. Ahora los 4 cierran sesión y mandan al login con el mensaje "Vuelve a
iniciar sesión con tu nueva contraseña".

### ℹ️ 4. Sobre el 401 masivo del despliegue

Lo manejamos silenciando el error, no solo redirigiendo: antes cada pantalla mostraba su
"Error al cargar…" justo mientras mandábamos al usuario al login. Ahora la petición se corta
sin ruido y lo único que ve es la pantalla de login.

**Cuando vayan a desplegar su lado, avísennos con tiempo** para tener esto ya en producción —
si su despliegue llega antes que el nuestro, el 401 masivo sí se va a ver feo.

### ⚠️ Lo que NO pudimos probar

Nada de esto está probado en vivo: su lado sigue sin desplegar, así que no hay forma de
reproducir ni el 403 por contraseña temporal ni el 401 masivo del refresh. Lo probamos en
cuanto tengan algo en QA.

---

## 🙋 Pedido al front — Política de Privacidad para la app de Meta (2026-08-05)

Para terminar de configurar la app de Facebook (la que se necesita para poder publicar en la
página desde el sistema — endpoints de arriba), Meta exige en **Configuración → Básico** una
**URL de Política de Privacidad pública**. Sin eso, ni siquiera deja hacer login de prueba en el
Graph API Explorer para sacar las credenciales (token, etc.) — es un bloqueo total, no solo un
detalle cosmético.

**Lo que se necesita de ustedes:** si el sitio de Novedades Jade no tiene todavía una página de
Política de Privacidad publicada, crearla y hospedarla (puede ser algo simple/genérico — qué
datos se recaban, para qué se usan, contacto) y pasarnos la URL final. Si ya existe una en el
sitio, con pasarnos esa URL alcanza, no hace falta crear nada nuevo.

No es urgente-urgente para el catálogo en sí, pero sí es lo único que tiene bloqueada ahora mismo
la parte de credenciales de Facebook — sin esa URL no se puede avanzar con las pruebas reales de
publicar en Facebook (foto/video, endpoints ya documentados arriba).

Este documento (`CAMBIOS_FRONT.md`) también sirve como canal para pasarse documentos/archivos
entre back y front cuando haga falta — no solo contratos de endpoints. Si tienen algo que
compartir de su lado (capturas, specs, lo que sea), puede ir aquí también.

---

## 🔧 Respuestas del back — preguntas del front del 2026-08-05

### Sobre "Publicar en Facebook"

1. **Estado de `pages_manage_posts` en la app de Meta: todavía en modo desarrollo, ni siquiera
   tenemos el Page Access Token todavía.** Estamos a mitad de la configuración de la app en Meta
   for Developers — nos topamos con un bloqueo (Meta exige URL de Política de Privacidad para
   habilitar el login de prueba, ver el pedido de arriba) y no hemos podido generar el token
   todavía. **Por ahora no se puede probar el camino feliz en QA** — solo van a ver 400 hasta que
   esto se resuelva. Avisamos en cuanto tengamos credenciales reales.

2. **`scheduledPublishTime` se interpreta en la zona horaria del servidor, y el servidor corre en
   `America/Mexico_City`** (`ENV TZ=America/Mexico_City` en el Dockerfile, aplica a qa y prod).
   Si el admin que programa la publicación está en esa misma zona horaria (caso normal, es un
   negocio mexicano), **no hace falta convertir nada** — manden el `LocalDateTime` tal cual sale
   del date-time picker del navegador.

3. **Su supuesto de omitir el part en vez de mandar string vacío es correcto y necesario** —
   así se debe quedar. Si mandan `imagenId` como string vacío (`""`), el back **no lo trata como
   "no vino"**: intenta convertir `""` a `Long`, falla, y por ahora eso cae en el manejador
   genérico de excepciones → **500 feo**, no un 400 claro. Quedó anotado como pendiente de mejora
   de nuestro lado (que un string vacío se trate igual que ausente), pero mientras tanto sigan
   omitiendo el part cuando no aplique, como ya lo dejaron.

### Sobre el checklist de seguridad de auth

4. **El campo del login es `debeCambiarPassword` — es el único que existe en la respuesta hoy.**
   Revisado en el código: `AuthResponse.java` solo tiene `accessToken` y `debeCambiarPassword`.
   `passwordTemporal` es el nombre de un campo interno de la entidad `Usuario` (uso solo del
   back, para decidir cuándo poner `debeCambiarPassword=true`) — nunca viaja al front con ese
   nombre. El documento del 2026-07-04 que menciona `debeCambiarPassword` es el vigente; pueden
   quitar el fallback a `passwordTemporal`, no hace falta.

5. **`seguridad.exigir-header-refresh` — sigue en `false` (apagado) en todos los ambientes,
   todavía no lo prendimos.** Confirmado en el código (`AuthController.java`, default `false`,
   no está seteado a `true` en ningún yml de ningún ambiente). Lo dejamos así hasta confirmar con
   el usuario cuándo conviene encenderlo — nada roto de su lado, es una decisión pendiente
   nuestra, no un olvido silencioso.

---

## ✅ Front: Política de Privacidad lista + confirmaciones (2026-08-05)

### 1. 🔓 Política de Privacidad — desbloqueado

No existía ninguna página así en el sistema (lo verificamos), así que la creamos de cero. Ruta
pública **`/privacidad`**, **sin guards a propósito**: Meta la abre con un bot anónimo, y si se
topa con un redirect al login la da por inválida.

**URL para pegar en Configuración → Básico de la app de Meta:**

```
https://qa.shop.novedades-jade.com.mx/privacidad     ← QA
https://shop.novedades-jade.com.mx/privacidad        ← producción
```

⚠️ **Confírmennos cuál de los dos dominios usar** — Meta acepta uno solo. Si la app se va a
dejar apuntando a producción, hay que esperar a promover; en QA ya queda disponible en cuanto
se despliegue `dev` → `qa`. **Avísennos y lo promovemos**, es lo único que falta para que
puedan seguir con el token.

⚠️ **Falta confirmar el correo de contacto.** La página dice hoy
`contacto@novedades-jade.com.mx`. Es a donde van a escribir los clientes que pidan acceder,
corregir o eliminar sus datos, así que tiene que ser una cuenta que alguien lea de verdad. Si es
otro, nos dicen y lo cambiamos en un minuto.

El contenido está redactado sobre lo que el sistema realmente recaba (cuenta, contacto, pedidos,
datos de entrega, chat), no es una plantilla genérica. Incluye una sección explícita de redes
sociales aclarando que **solo se publican productos del catálogo y nunca datos de clientes** —
justo lo que Meta revisa en estos casos.

### 2. Sobre sus respuestas — todo confirmado, un solo cambio

- **Zona horaria:** perfecto, ya lo mandábamos así (hora local del navegador, sin convertir).
  Sin cambios.
- **Omitir el part vacío:** confirmado que era lo correcto. Buen dato lo del 500 — se queda
  como está.
- **`debeCambiarPassword`:** gracias por revisarlo en el código. **Ya quitamos el fallback a
  `passwordTemporal`**, ahora leemos solo `debeCambiarPassword`.
- **`exigir-header-refresh`:** de acuerdo en dejarlo apagado. Nosotros ya mandamos el header,
  así que del lado del front no hay prisa — solo pedimos que **cuando lo vayan a encender, sea
  después de que esto llegue a producción**, no antes.

### 3. Lo que necesitamos que nos pasen cuando tengan el token

Para que la pantalla de publicar funcione, de su lado hacen falta 4 datos configurados:
**App ID**, **App Secret**, **ID de la página** y un **Page Access Token de larga duración**
(el que da el Graph API Explorer por defecto es de ~1 hora — si se usa ese, las publicaciones
empiezan a fallar solas al rato sin razón aparente).

Nosotros no necesitamos ninguno de esos valores en el front; solo avísennos cuando estén
cargados para probar el camino feliz en QA.

---

## ✅ Front: alineado con la pausa de Facebook (2026-08-05)

Recibido. Del lado del front había un problema que su aviso destapó: **la pantalla de publicar ya
estaba mergeada a `qa` y su link visible** en el menú (🛠️ Sistema → "📘 Publicar en Facebook").
Cualquier admin que entrara iba a chocar con el 404 sin entender por qué.

**Ya lo ocultamos.** Comentamos únicamente el link del menú; la ruta `/admin/facebook`, el
componente, el servicio y los modelos **se quedan intactos** en el código — igual que ustedes
conservaron el suyo en `backup/facebook-redes-sociales`. Reactivarlo es descomentar una línea.

**No hace falta que nos avisen dos veces cuando lo retomen:** con que nos digan que los endpoints
volvieron a `dev`/`qa`, descomentamos y probamos el mismo día.

### Dónde quedó la configuración de la app de Meta

Por si sirve cuando se retome — la app **no** quedó a medias, quedó bloqueada en un solo punto:

- App `novedadesJade`, ID `1017171384561253`, en modo **Publicada**.
- Caso de uso **"Administrar todos los aspectos de tu página"** ya agregado, con
  `pages_manage_posts` y `pages_read_engagement` en **"Listo para la prueba"**.
- ✅ **La Política de Privacidad ya dejó de ser el bloqueo** — la publicamos (ver sección
  anterior) y la URL ya está cargada en Configuración → Básica.
- ⛔ **El bloqueo real es otro:** no se pudo generar el **Page Access Token**. El popup de
  consentimiento de Facebook (`facebook.com/privacy/consent/?flow=user_cookie_choice_v2`) entra
  en bucle infinito con `ERR_TOO_MANY_REDIRECTS`, en Brave **y** en Chrome. Ya se descartaron:
  cookies de terceros (están permitidas globalmente) y cookies viejas (se borraron). La sospecha
  que queda, sin confirmar, es alguna **extensión del navegador** rompiendo ese flujo.
- ℹ️ El aviso *"Currently ineligible for submission — Ícono de la app (1024×1024)"* **no era el
  bloqueo**: solo impide mandar la app a revisión de Meta, trámite que no hace falta para
  publicar en la página propia siendo admin de la app.

O sea: cuando se retome, **el único paso pendiente es generar el token de página**. Todo lo demás
de la configuración de Meta ya está.

### Lo que NO tocamos

La página pública **`/privacidad`** se queda como está — sigue haciendo falta para la app de Meta
cuando se reactive, y de todos modos conviene tenerla publicada.

---

## ✅ Front: el feature de Facebook también salió de `dev`/`qa` (2026-08-05)

Corrección de nuestro mensaje anterior: primero solo ocultamos el link del menú, pero eso dejaba
~1300 líneas de código muerto apuntando a endpoints que ya no existen. **Lo sacamos completo**,
igual que ustedes.

**Respaldado en la rama `backup/facebook-redes-sociales`** del repo del front — usamos **el mismo
nombre que ustedes** en `proyecto_key`, a propósito, para que las dos ramas se encuentren juntas
cuando se retome.

Se eliminaron: el modelo, el servicio, el componente con su HTML/SCSS, la ruta `/admin/facebook`,
su declaración en el módulo admin, el link del menú y la excepción del interceptor de carga.

**La página pública `/privacidad` se queda** — no depende del feature, sigue haciendo falta para
la app de Meta y de todos modos conviene tenerla publicada.

### Para cuando se retome

Con que nos avisen que los endpoints volvieron a `dev`/`qa`, hacemos merge de la rama de respaldo
y probamos el mismo día. El contrato que documentaron sigue vigente tal cual — no hay que rehacer
nada.

Y recuerden lo de la sección anterior: del lado de la app de Meta **el único paso que faltaba era
generar el Page Access Token** (bloqueado por el bucle de `ERR_TOO_MANY_REDIRECTS` en el popup de
consentimiento). Todo lo demás de esa configuración ya está hecho.

---

## 🚀 Front: promovido a PRODUCCIÓN (2026-08-05)

`qa` → `master` (commit `136ffa5`). Todo lo acumulado desde la última promoción ya está en prod.

### ⚠️ Lo que les toca saber a ustedes

**1. `X-Requested-With` ya está en PRODUCCIÓN.** Como pidieron, respetamos el orden: primero lo
desplegamos nosotros, ahora les avisamos. **`POST /v1/auth/refresh` y `POST /v1/auth/logout` ya
llegan con el header** desde el navegador, en qa y en prod. Pueden encender
`seguridad.exigir-header-refresh: true` cuando quieran, sin riesgo de tirar sesiones.

**2. El manejo del 401 masivo ya está listo del lado del front.** Cuando desplieguen su tanda de
seguridad, el primer refresh de cada usuario va a dar 401 — el front ya lo maneja mandando al
login sin mostrar ningún error. **Pero avísennos antes de desplegar**, para estar pendientes.

**3. Verificamos el rename `/variantes` → `/tienda` contra el backend de PRODUCCIÓN antes de
promover**, porque el front hace 25 llamadas con ese prefijo y un desfase habría tumbado el
catálogo completo: `GET /tienda/1` responde **401** (existe y está protegido) ✅. Todo alineado.

**4. La página `/privacidad` ya está en producción**, así que la URL que Meta tiene configurada
(`https://shop.novedades-jade.com.mx/privacidad`) ahora sí resuelve de verdad. Antes solo existía
en QA.

**5. NO va el feature de Publicar en Facebook** — quedó fuera de dev/qa/prod, respaldado en la
rama `backup/facebook-redes-sociales`, como acordamos.

### Qué más entró (resumen)

Paleta jade en ambos temas, rediseño del login, taxonomía de nombres (Tienda / Inventario /
Modelos), los 6 puntos del checklist de seguridad de auth, catálogo de lugares de entrega, datos
de entrega en pedidos, filtro Pagados/Cancelados, paginación real de admin en `mis-pedidos`,
historial de abonos en el ticket, y la tanda de fixes de UI reportados en QA.

También entró un fix de infraestructura: **`Cache-Control` en nginx** (index.html sin caché,
assets hasheados con caché de un año). Eso explica retroactivamente los "ya subiste el cambio
pero no se ve" que aparecieron varias veces en este documento — el servidor nunca le decía al
navegador que dejara de confiar en su copia vieja.

**Verificado antes de promover:** `ng build --configuration=production` sin errores.

---

## 🚀 Back: merge a `main` ya hecho (2026-08-05) — falta el deploy real al servidor

`qa` → `main` de `proyecto_key` ya está mergeado y pusheado (commit `411ca0a`). **Todavía NO se
desplegó al servidor de producción** — según lo que pidieron, avisamos antes de ese paso, no
después. Cuando decidamos desplegar de verdad, se los confirmamos aparte para que estén pendientes
del 401 masivo del primer refresh.

Va todo lo que ya sabían que veníamos armando: los 16 hallazgos de `SEGURIDAD_AUTH.md` (incluido
`X-Requested-With`, que confirman que ya tienen en prod — cuando desplieguen esto de nuestro lado
vamos a encender `seguridad.exigir-header-refresh: true`), rendimiento de búsquedas, lugares de
entrega, datos de entrega en pedidos, cancelación como devolución, y el resto de lo que ya está
documentado arriba en este archivo. **Sin Facebook** — sigue fuera, respaldado en
`backup/facebook-redes-sociales` como acordamos.

Un detalle de CORS que salió al hacer el merge, por transparencia: `main` nunca había tenido un
candado de orígenes permitidos activo en el perfil que de verdad corre en el contenedor
(`application-docker.yml` ya lo tenía bien, con `shop.novedades-jade.com.mx` y los demás dominios
reales — no hubo que tocar nada ahí). Quedó verificado antes de pushear.

---

## ✅ Front: verificado EN VIVO en producción — vía libre para su deploy (2026-08-05)

Recibido lo del merge a `main`. Antes de que desplieguen, fuimos a comprobar **contra el sitio de
producción real** que nuestro despliegue sí hubiera corrido — porque nuestro pipeline es conocido
por no dispararse solo, y si ustedes encendieran `exigir-header-refresh` con un bundle viejo
arriba, **todos los usuarios perderían la sesión a los 15 minutos**. No queríamos que eso
dependiera de un supuesto.

Descargamos el bundle que está sirviendo `shop.novedades-jade.com.mx` (`main.b7489b600cab8b68.js`,
3.3 MB) y lo revisamos por dentro:

| Qué buscamos | Resultado |
|---|---|
| `X-Requested-With` en el bundle | ✅ presente |
| Ruta `/privacidad` | ✅ presente, y `GET /privacidad` responde **200** |
| Prefijo `/tienda` | ✅ presente |
| `Cache-Control` de `index.html` | ✅ `no-cache, no-store, must-revalidate` |
| `Cache-Control` de los assets hasheados | ✅ `public, max-age=31536000, immutable` |

**Conclusión: el despliegue del front sí corrió y el bundle nuevo es el que está en vivo.**

### 🟢 Pueden desplegar y encender `seguridad.exigir-header-refresh: true`

De nuestro lado no hay nada pendiente. Solo dos peticiones:

1. **Avísennos el día/hora del deploy**, para estar pendientes del 401 masivo del primer refresh
   (nuestro front ya lo maneja mandando al login sin error feo, pero queremos ver que se comporte
   como esperamos con usuarios reales).
2. **Enciendan el header en el mismo deploy o después, nunca antes.** Ya está en prod de nuestro
   lado, así que el orden que pidieron se cumplió — pero mejor no adelantarlo por si acaso.

Gracias por el detalle del CORS, anotado. Nos deja tranquilos saber que `application-docker.yml`
ya tenía `shop.novedades-jade.com.mx` — es justo el origen desde el que va a pegar todo esto.

---

## 🐛 Back: 2 endpoints públicos caían con 400 en producción — ya corregidos y desplegados (2026-08-06)

Tras el deploy del 2026-08-05, dos endpoints del catálogo público quedaron rotos en `main`
(producción) por un mismo tipo de bug de mapeo JPA. Tomamos la sesión para auditar el resto del
código en busca del mismo patrón — encontramos y arreglamos los dos únicos casos que existían.
Ya está en `dev`/`qa`/`main`, pusheado y compilado sin errores.

### [BUG-KEY-12] ✅ Fix: `GET /tienda/v1/filtros-disponibles` respondía 400
**Fecha:** 2026-08-06
**Archivo corregido:** `IVarianteRepository.java` (`findRangoPreciosPublico`)

**Endpoint:**
```
GET /mis-productos/tienda/v1/filtros-disponibles
```

**Comportamiento ANTES del fix (roto):** siempre respondía `400` con
`"class [Ljava.lang.Object; cannot be cast to class java.lang.Number"` — el endpoint de filtros
del catálogo (tallas, colores, marcas, rango de precio) no funcionaba en absoluto.

**Comportamiento DESPUÉS del fix (correcto):** `200` con el mismo contrato de siempre —
`{ tallas: string[], colores: string[], marcas: string[], precioMin: number, precioMax: number }`.

**El front no necesita cambiar nada** — mismo endpoint, mismo request, mismo response. Era un bug
interno de mapeo (Spring Data anidaba mal el resultado de una query de agregación con una sola
fila), no un cambio de contrato.

---

### [BUG-KEY-13] ✅ Fix: `GET /v1/resenas/variante/{varianteId}/resumen` respondía 400
**Fecha:** 2026-08-06
**Archivo corregido:** `IResenaRepository.java` (`resumenPorVariante`)

**Endpoint:**
```
GET /mis-productos/v1/resenas/variante/{varianteId}/resumen
```

**Comportamiento ANTES del fix (roto):** siempre respondía `400` (mismo error de casteo que
BUG-KEY-12) — el resumen de calificaciones (promedio + conteo por estrellas) de cualquier
variante fallaba. Probablemente pasaba desapercibido porque el detalle de producto no lo muestra
de forma prominente, pero cualquier pantalla que lo consuma estaba rota.

**Comportamiento DESPUÉS del fix (correcto):** `200` con el mismo contrato de siempre —
`{ varianteId: number, promedio: number, totalResenas: number, conteoPorEstrella: {1: n, 2: n, 3: n, 4: n, 5: n} }`.

**El front no necesita cambiar nada** — mismo bug interno que el anterior, mismo tipo de fix.

---

## 🚀 Back: micro_imagenes — 5 commits que estaban solo en `qa` ya llegaron a producción (2026-08-06)

Al investigar un 400 en `GET /v1/imagenes/thumbnail/{imagenId}` (`"No static resource"`)
encontramos que el código de miniaturas nunca había llegado a `master` de `micro_imagenes` — se
quedó en `qa` desde que se implementó. Hicimos el merge `qa → master` (commit `7de7f83`), que
dispara el deploy automático (GitHub Actions). Va todo lo que estaba pendiente, no solo el fix del
día:

- **Miniaturas para listas:** `GET /mis-productos/v1/imagenes/thumbnail/{imagenId}` — ya responde
  en producción. Es la que usan los `imagenUrl` de listados/búsqueda (ver sección de arriba); el
  detalle de producto sigue usando `/v1/imagenes/file/{imagenId}` (imagen completa).
- **Cache del navegador en `/v1/imagenes/file/{imagenId}`:** ahora trae `Cache-Control` (1 año,
  inmutable) y `ETag`. No requiere cambios en el front, solo hace que la segunda carga de la misma
  imagen sea instantánea.
- Un fix interno de rendimiento (N+1 al verificar existencia de imágenes) y uno de seguridad
  (credenciales AWS hardcodeadas que ya no se usaban, quitadas del yml). Ninguno de los dos cambia
  contrato ni comportamiento visible para el front.

**El front no necesita cambiar nada** en esta sección — son fixes de disponibilidad/rendimiento de
endpoints que ya existían en el contrato.

---

## ❓ CONSULTA AL BACK — endpoint para la "cinta de promociones" que corre arriba de la pantalla (2026-08-10)

### Qué es

Se agregó una **cinta de texto que corre de derecha a izquierda** en la parte superior de la app
(estilo noticiero: "BOLSAS ✦ BLUSAS ✦ PERFUMES 10 ML ✦ ENVÍOS A TODO MÉXICO ✦ …"). Ya está en
`qa`, se ve en todas las pantallas menos las de autenticación (`/login`, `/usuarios/registrar`,
`/verificar-correo`, `/olvide-password`, `/privacidad`).

Hay además una pantalla de administración (`/admin/cinta`, solo ADMIN) donde el dueño escribe,
reordena, oculta y borra esas frases.

### ⚠️ Hoy NO tiene backend — vive en `localStorage`

Se hizo así a propósito para poder afinar diseño y comportamiento antes de pedirles nada. **La
consecuencia es que lo que el admin edita se guarda solo en SU navegador**: otro usuario, otra
computadora o modo incógnito ven las frases de fábrica. Ya no queremos dejarlo así — es una cinta
comercial, tiene que verla igual todo el mundo.

### Lo que pedimos

Es un catálogo chiquito, idéntico en forma a `lugares-entrega` o `palabras-clave`. Si les sirve
reusar el **CRUD genérico** que ya tienen, por nosotros perfecto — solo con dos matices (puntos 1
y 4 de abajo).

**Entidad sugerida** (`cinta_promocion` o como prefieran nombrarla):

| Campo | Tipo | Notas |
|---|---|---|
| `id` | Long | PK |
| `texto` | String (máx. 120) | Lo que se ve corriendo. Se muestra tal cual (el front lo pone en mayúsculas por CSS, no hace falta guardarlo en mayúsculas) |
| `activo` | boolean | `false` = se conserva en la pantalla del admin pero no sale en la cinta |
| `orden` | int | Posición en la cinta. La pantalla de admin tiene botones subir/bajar |

> 🏷️ **Un favor con el nombre: llámenle "cinta", no "ticker".** En inglés el término para este
> letrero corredizo es *ticker*, y así lo habíamos nombrado nosotros — pero se lee casi igual que
> **"ticket"**, que en este sistema ya significa otra cosa muy distinta (el comprobante de venta
> que se imprime y se manda por correo, `POST /v1/pedidos/{id}/notificar`). El dueño se confundió
> con solo verlo en el menú, así que del lado del front ya lo renombramos todo a **cinta** antes
> de que esto llegara a producción. Les pedimos lo mismo en tabla y endpoints para que no queden
> dos nombres para la misma cosa.

**Endpoints:**

1. **`GET /v1/cinta/activos`** — ⚠️ **el único que NO es admin-only.** Lo llama la cinta, que se
   pinta para cualquier usuario logueado (cliente incluido). Debe devolver **solo** los `activo =
   true`, **ya ordenados** por `orden` ascendente. Si pueden, con `@Cacheable` — se pide en cada
   carga de la app y el contenido cambia muy de vez en cuando.
   ```json
   { "data": [ { "id": 1, "texto": "Bolsas", "activo": true, "orden": 1 } ] }
   ```
2. **`GET /v1/cinta/getAll`** — ADMIN. Todas, activas y no activas, ordenadas por `orden`. Sin
   paginar, o con `size` grande por default: son pocas frases y la pantalla las muestra todas
   juntas para poder reordenarlas.
3. **`POST /v1/cinta/save`** — ADMIN. Body `{ texto, activo, orden }`.
4. **`PUT /v1/cinta/update/{id}`** — ADMIN. **Acá va el otro matiz:** reordenar cambia el `orden`
   de dos filas a la vez (la que sube y la que baja). Si su `update` genérico ya acepta que le
   manden solo los campos a tocar, nos alcanza con dos llamadas. Si prefieren, un
   **`PUT /v1/cinta/orden`** con `[{id, orden}, ...]` de un jalón también nos sirve y nos evita
   dejar el orden a medias si una de las dos llamadas falla. Ustedes decidan cuál les acomoda.
5. **`DELETE /v1/cinta/delete`** — ADMIN. Nos da igual si el id va en el path o como body crudo
   (como `lugares-entrega`); solo díganos cuál para no equivocarnos otra vez.

**Datos iniciales que traeríamos como semilla** (hoy son los defaults del front): Bolsas, Blusas,
Pantalones, Perfumes 10 ml, Envíos a todo México, Promos de temporada.

### Del lado del front ya está listo para el cambio

`CintaService` expone `items$` (todas) y `activos$` (las que corren). Ni la cinta ni la pantalla
de admin conocen `localStorage` — cuando exista el endpoint, se reemplaza el cuerpo de los 5
métodos públicos por llamadas HTTP y **no se toca ningún componente**.

### Pregunta puntual

¿Les late así, o prefieren otra forma? Lo único que de verdad nos importa que salga bien es el
punto 1: que `GET /v1/cinta/activos` **no exija rol ADMIN**, porque si no, el cliente entra a la
tienda y la cinta le sale vacía (o con un 403 en consola en cada carga).

### 🔗 A dónde lleva cada frase al hacerle clic

Las frases no deberían ser solo decorativas: "Blusas" tiene que llevar al catálogo de blusas.
**Pero les pedimos que NO guarden la URL del front** (`"/tienda/buscar?..."`). Ya nos mordió una
vez: cuando renombramos `/variantes` → `/tienda`, cualquier ruta guardada como texto en la base
se habría roto en silencio, sin que nadie se entere hasta que un cliente cayera en un 404.

En vez de eso, guarden **qué** mostrar y nosotros sabemos **dónde** vive. Dos campos más:

| `destinoTipo` | `destinoValor` | El front lo manda a |
|---|---|---|
| `NINGUNO` (default) | `null` | no es clickeable, solo texto |
| `PROMOCIONES` | `null` | `/promociones` (la pantalla de combos activos) |
| `BUSQUEDA` | `"BLUSAS"` | el catálogo con ese texto en el buscador |
| `PRODUCTO` | `"482"` | la ficha de esa variante |
| `EXTERNO` | url completa | se abre en pestaña nueva (Facebook, WhatsApp) |

**Nota sobre `BUSQUEDA`** — no necesitamos ningún endpoint nuevo para esto. El buscador del
catálogo ya hace cascada **código de barras → palabra clave → nombre**, y las categorías del
sistema (`palabraClave`) son justo "BLUSAS", "BOLSAS", etc. Así que mandar `"BLUSAS"` como texto
de búsqueda cae en el paso de palabra clave y devuelve todo lo de esa categoría. Es exactamente
lo mismo que si el cliente lo escribiera a mano. Por eso tampoco pedimos un tipo `CATEGORIA`
aparte: terminaría en la misma llamada.

### 💡 Opcional (ustedes dicen) — que las promociones vigentes se agreguen solas

Hoy, si el admin escribe "2x1 en blusas" a mano y la promo se vence, la frase se queda ahí
mintiendo hasta que alguien se acuerde de borrarla.

Si les parece bien, `GET /v1/cinta/activos` podría **anexar al final** de la lista las
promociones vigentes que ya tienen en su tabla (las mismas de `GET /v1/promociones/activas`),
armando la frase con su descripción y con `destinoTipo: "PROMOCIONES"`. Entran y salen solas
según su vigencia, sin que nadie toque la pantalla de administración.

No es bloqueante — si prefieren dejarlo todo manual por ahora, el front funciona igual. Solo
díganos qué prefieren para no asumirlo mal.

---

## ✅ NUEVO (2026-08-10): catálogo "cinta de promociones" — `/v1/cinta`

Respuesta a la consulta de arriba. **Para esta primera versión el back solo guarda y sirve las
frases — sin destino clickeable todavía** (nada de `destinoTipo`/`destinoValor`, `BUSQUEDA`,
`PRODUCTO`, etc.). Es puramente de muestra: alta, edición, borrado y orden. Fue decisión nuestra
simplificar el alcance de lo que propusieron para esta primera entrega — si más adelante se
agrega el clic, será una migración aditiva sobre esta misma tabla (columnas nuevas), no un cambio
de contrato de lo que ya está aquí. El punto de `BUSQUEDA` reusando la cascada del buscador y el
de las promociones vigentes anexadas quedan anotados para cuando se retome esa parte.

Mismo patrón que otros catálogos simples del proyecto (`/v1/lugares-entrega`, `/v1/palabras-clave`).

| Método | URL | Quién | Body / respuesta |
|--------|-----|-------|-------------------|
| `GET` | `/v1/cinta/activos` | **Público, sin auth** (ni siquiera login) | `{ "data": [ {"id":1,"texto":"Bolsas","activo":true,"orden":0}, ... ] }` — solo las activas, ya ordenadas por `orden` ascendente |
| `GET` | `/v1/cinta/getAll?page=0&size=50` | ADMIN | `{ "data": [ {...}, ... ] }` — todas, activas y apagadas |
| `GET` | `/v1/cinta/getOne/{id}` | ADMIN | `{ "data": {"id":1,"texto":"Bolsas","activo":true,"orden":0} }` |
| `POST` | `/v1/cinta/save` | ADMIN | Body: `{ "texto": "Bolsas", "activo": true, "orden": 0 }` → Response: el registro creado con `id` |
| `PUT` | `/v1/cinta/update/{id}` | ADMIN | Body: el objeto completo (igual que save, con `id`) → Response: el registro actualizado |
| `DELETE` | `/v1/cinta/delete` | ADMIN | Body: `1` (el id, como número JSON crudo — **no** `{ "id": 1 }`, mismo patrón que `/v1/lugares-entrega/delete`) |

**Respondiendo puntos puntuales de la consulta:**
- **Punto 1 (que `/activos` no exija rol ADMIN):** hecho — es el único GET de este catálogo que no
  requiere login siquiera, ni rol. El resto (`getAll`, `getOne`, `save`, `update`, `delete`) es ADMIN.
- **Caché en `/activos`:** sí, con `@Cacheable` (TTL 1h), invalidado automáticamente en cada
  `save`/`update`/`delete`.
- **Paginación de `getAll`:** el CRUD genérico exige `page`/`size` en la URL, no tiene defaults —
  no hay forma de omitirlos. Para traerlas todas de un jalón, usen un `size` grande
  (`?page=0&size=200`), no hace falta paginar de verdad en la pantalla de admin.
- **Reordenar (punto 4):** no armamos el `PUT /v1/cinta/orden` en lote — les toca resolverlo con
  dos llamadas sueltas a `update`, una por cada fila que intercambia posición. Si en la práctica
  ven que falla a medias muy seguido, avisen y lo armamos.
- **Formato del `DELETE` (punto 5):** id crudo en el body (`1`), no `{ id: 1 }` — mismo patrón que
  `/v1/lugares-entrega/delete`.
- **`texto`** (string, máx. 120, requerido): si se manda vacío o de más de 120 caracteres,
  `save`/`update` responden `400` con el mensaje de validación en `mensaje`.
- **Datos semilla:** no los cargamos nosotros — la tabla nace vacía. Denla de alta ustedes mismos
  desde la pantalla de admin cuando el endpoint esté arriba (son los mismos 6 textos default que
  ya tienen del lado del front).

**Pendiente de nuestro lado:** correr `migration_cinta_promocion.sql` en QA (crea la tabla
`cinta_promocion`, vacía). Avisamos cuando ya esté corrida.

---

## ✅ FRONT — cinta conectada a `/v1/cinta` (2026-08-10)

Recibido y conectado. Ya no queda nada en `localStorage`: tanto la cinta como la pantalla de
administración (`/admin/cinta`) leen del backend.

**Cómo quedó del lado del front:**

- **Dos listas separadas, a propósito.** La cinta consume `GET /activos` (el público) y la
  pantalla de admin consume `GET /getAll`. No las unificamos aunque hubiera "ahorrado una
  llamada": si la cinta colgara de `getAll`, a cualquier cliente le saldría 403 en cada carga y
  la vería vacía. Gracias por dejar `/activos` sin auth — era el punto que más nos preocupaba.
- **`/activos` falla en silencio.** Se pide en el arranque de la app, en todas las pantallas y
  para cualquier visitante. Si no responde, la lista queda vacía y la cinta simplemente no se
  pinta: ni Swal, ni throw, ni ruido en la consola de un cliente por un adorno. También la
  sacamos del overlay global de carga, para que no tape la pantalla mientras responde.
- **Reordenar: renumeramos, no intercambiamos.** Como no hay endpoint en lote, mandamos un
  `update` por cada fila que cambia de lugar (normalmente dos, en paralelo). Pero en vez de
  intercambiar los dos `orden` entre sí, **renumeramos la lista completa por posición**. El
  intercambio se rompe si dos filas comparten el mismo `orden` — que es justo lo que pasaría si
  un reordenamiento anterior quedó a medias, el riesgo que ustedes mismos mencionaron: ahí el
  intercambio no movería nada y el botón se vería muerto. Renumerar por índice deja la lista
  consistente siempre.
- **`page`/`size` siempre presentes** en `getAll` (`?page=0&size=200`) — ya sabemos que el CRUD
  genérico no tiene defaults.
- **`DELETE` con el id crudo** en el body, no `{ id }`. Anotado.
- **Tabla vacía:** en vez de sembrarla por fuera, la pantalla de admin tiene un botón
  **"✨ Cargar frases sugeridas"** con los 6 textos. **Solo aparece cuando la lista está vacía**
  — si estuviera siempre visible, un clic de más las duplicaría, porque cada carga hace `POST` y
  no reemplaza nada.

**Sobre recortar el alcance (sin destino clickeable en v1):** de acuerdo, no bloquea nada. Hoy
las frases se pintan como texto no clickeable, que es exactamente lo que devuelve el contrato.
Cuando se retome, del lado nuestro faltarían dos cosas que ya tenemos identificadas: agregarle al
catálogo el parámetro de búsqueda en la URL (hoy solo acepta `?productoId=`) y un botón "probar"
en la pantalla de admin que corra la búsqueda y diga cuántos resultados da, para no publicar una
frase que lleve a un catálogo vacío.

### ⏳ Lo único que falta para poder probarlo

Correr **`migration_cinta_promocion.sql` en QA**. Mientras no esté, los endpoints no responden y
la cinta no se va a ver — que es justo el comportamiento esperado (falla en silencio), así que no
se asusten si entran a QA y no aparece nada. **Avísennos cuando la corran** y lo verificamos en
vivo del lado del front.

---

## 🔒 BACK — endpoints que dejan de ser públicos + mejoras de rendimiento en catálogo (2026-08-11)

Revisión de seguridad y rendimiento del micro. **Ningún contrato de response cambia** — no hay que
tocar mappings ni interfaces. Lo que sí cambia es *quién* puede llamar dos grupos de endpoints, y
qué tan rápido responden las búsquedas.

### 1. `/tienda/getAll`, `/tienda/v1/getAll`, `/tienda/getOne/{id}`, `/tienda/v1/getOne/{id}` → ahora exigen ADMIN

**Antes:** cualquiera sin token podía llamarlos.
**Ahora:** 401 sin token, 403 con token de cliente normal. Con token de ADMIN siguen igual que siempre.

**Por qué:** son el CRUD genérico heredado (`AbstractController`), devuelven la entidad `Variantes`
cruda, o sea que arrastran el `Producto` completo — **incluido `precioCosto` y `precioRebaja`** — y
además no aplican el filtro del catálogo público (listaban también variantes deshabilitadas y sin
stock). Con `GET /tienda/getAll?page=0&size=1000` y sin login se podía sacar el margen completo de
la tienda.

**Qué tiene que hacer el front:** en principio **nada** — la tienda usa `/tienda/v1/buscar`,
`/tienda/v1/buscar-filtrado` y `/tienda/v1/porProducto/...`, que siguen públicos e intactos. Si en
alguna pantalla quedó una llamada suelta a `getAll`/`getOne` sin login, ahí va a empezar a dar 401:
avísennos y vemos si conviene exponer un equivalente sin precio de costo. Las pantallas de admin no
se ven afectadas (ya mandan token de ADMIN).

### 2. `/actuator/**` → ahora exige ADMIN (salvo `health`)

**Antes:** caía en el "cualquiera autenticado", así que un cliente logueado de la tienda podía
listar y **vaciar los cachés** del micro (`DELETE /actuator/caches`) y dejarlo lento a voluntad.
**Ahora:** solo ADMIN. `GET /actuator/health` sigue abierto porque lo usa el probe de Kubernetes.
El front no llama actuator, así que esto no debería notarse.

### 3. Búsquedas: pueden aparecer resultados que antes se perdían

En `/tienda/v1/buscar` y `/tienda/v1/buscar-filtrado`, la condición del código de barras generaba
un `INNER JOIN` implícito: las variantes cuyo **producto no tiene código de barras** quedaban fuera
del resultado *aunque hubieran coincidido por nombre, marca o palabra clave*. Ya se corrigió (join
explícito `LEFT`). Efecto visible: **el mismo término puede devolver más resultados que antes**, y
`totalRegistros` puede subir. No es un bug nuevo, es el que se corrigió.

> En la BD de QA hoy no hay ningún producto público sin código de barras, así que **en QA el
> resultado no debería cambiar**. En producción sí puede, si allá existen productos así.

### 4. Rendimiento del catálogo (transparente para el front)

Las listas de productos y variantes hacían una consulta por cada fila y por cada relación
(producto, código de barras, palabra clave, imagen): una página de 20 variantes eran ~80 consultas.
Ahora se traen agrupadas — la misma página son ~4. **La respuesta es idéntica campo por campo**, no
hay que cambiar nada; solo debería sentirse más rápido, sobre todo en la primera carga (cuando el
caché está frío).

### ⚠️ Pendiente del lado del back, aún NO aplicado

Las tablas de relación imagen↔producto/variante tienen filas repetidas en cantidad seria: en QA,
`producto_imagen_copy` tiene **13,095 filas para 81 relaciones reales** (el producto 265 solo tiene
11,032 filas para 25 imágenes) y `variante_imagen` **12,607 filas para 1,795 reales**. Eso es parte
de por qué las imágenes del listado se sienten lentas. Ya está escrito el script de limpieza
(`migracion_dedup_relaciones_imagenes.sql`) pero **no se ha corrido**. Cuando se corra, avisamos —
no cambia ningún contrato, pero la mejora sí se debería notar del lado de ustedes.

---

## 🆕 BACK — respuesta del ADMIN a reseñas + historial de acceso para el dashboard (2026-08-11)

**Implementado en `dev`, pendiente correr migración** en dev/qa/prod:
`src/main/resources/static/migration_respuesta_resena_historial_acceso.sql`.

### 1. ADMIN responde a una reseña

```
PUT /v1/resenas/{id}/responder
Authorization: Bearer <token de ADMIN>
Content-Type: application/json

{ "respuesta": "Gracias por tu comentario, ya revisamos el detalle del color." }
```

Solo ADMIN (401 sin token, 403 con token de cliente normal). Una sola respuesta por reseña, no un
hilo — si se vuelve a llamar, **sobrescribe** la respuesta anterior (no hay historial de versiones).

**Response 200** — mismo `ResenaResponseDto` que ya usan `/v1/resenas/variante/{id}` y
`/v1/resenas/mis-resenas`, con 2 campos nuevos al final:
```json
{
  "data": {
    "id": 12,
    "varianteId": 340,
    "calificacion": 4,
    "comentario": "Buena calidad, pero llegó un poco tarde",
    "fechaCreacion": "2026-08-01T10:15:00",
    "nombreCliente": "María L.",
    "esPropia": false,
    "respuestaAdmin": "Gracias por tu comentario, ya revisamos el detalle del color.",
    "fechaRespuesta": "2026-08-11T18:30:00"
  }
}
```
`respuestaAdmin`/`fechaRespuesta` vienen `null` en cualquier reseña sin respuesta todavía — **ya
salen así en todos los endpoints existentes** (`/v1/resenas/variante/{id}`, `/mis-resenas`), no
hace falta ningún cambio en las pantallas que ya consumen esos endpoints salvo pintar la respuesta
si no es null.

**Response 400:** `"La respuesta no puede estar vacia"` o `"No existe la resena con id: {id}"`.

### 2. Historial de acceso (para el dashboard del ADMIN)

Registra cada login y, mientras la sesión sigue activa, se actualiza sola — sin que el front tenga
que hacer nada nuevo. **Importante sobre la precisión:** la "última actividad" se actualiza cada
vez que el refresh token rota (aprox. cada 15 min mientras el usuario sigue usando la app), no en
cada clic. La duración que se ve es una aproximación en bloques de ~15 min, no un cronómetro exacto
— una visita muy corta que nunca llega a refrescar el token se ve con duración ~0.

```
GET /v1/dashboard/accesos?desde=2026-08-01&hasta=2026-08-11&pagina=1&size=20
Authorization: Bearer <token de ADMIN>
```
`desde`/`hasta` opcionales (`yyyy-MM-dd`) — sin ellos trae todo el histórico paginado. Ya está bajo
`/v1/dashboard/**`, que ya es ADMIN-only, no requiere nada nuevo de seguridad.

**Response 200:**
```json
{
  "data": {
    "pagina": 1,
    "totalPaginas": 3,
    "totalRegistros": 47,
    "t": [
      {
        "usuarioId": 8,
        "username": "maria.lopez",
        "fechaLogin": "2026-08-11T09:02:11",
        "ultimaActividad": "2026-08-11T09:41:00",
        "duracionMinutosAprox": 39
      }
    ]
  }
}
```

**No existe todavía** (por si lo necesitan después): conteo de visitantes anónimos sin cuenta —
esto solo registra logins de usuarios con cuenta, no tráfico público sin login.

---

## ✅ Respuesta del back — dudas de reseñas + historial de accesos (2026-08-11)

### 1. ¿Cuándo corren la migración?

**Todavía no se ha desplegado nada de esto a ningún ambiente compartido** — el código solo existe
como commit local en `dev`, sin push. Por eso los endpoints no responden: literalmente no están
ahí todavía, no es un problema de la migración sola.

**⚠️ Orden obligatorio, y esto es más serio de lo que parecía al escribir el contrato original:**
correr la migración **antes** de desplegar el código, nunca después. Revisamos qué pasa si se
invierte el orden:

- `SesionRefreshService.crearSesion()` ahora inserta también en `historial_acceso`, **en la misma
  transacción** que abre la sesión de login. Si esa tabla no existe todavía, ese `INSERT` truena,
  la transacción entera se revierte (ni siquiera se crea la sesión), y `AuthController.login()` cae
  en su `catch` genérico → **`POST /v1/auth/login` responde 500 para todo el mundo**, no solo para
  quien toque reseñas.
- Lo mismo con las columnas nuevas de `Resena` (`respuesta_admin`, `fecha_respuesta`): en cuanto el
  código se despliega, **cualquier lectura de reseñas** (no solo el `PUT /responder` nuevo) genera
  un `SELECT` que las incluye. Si no existen en la tabla, `/v1/resenas/variante/{id}` y
  `/mis-resenas` — que ya usan hoy — empiezan a responder 500.

O sea: el riesgo no es "el feature nuevo no funciona", es "se cae el login y las reseñas que ya
tenían andando" si el deploy le gana a la migración por error. Vamos a avisarles con tiempo antes
de pushear a `dev`/`qa`, y correr la migración primero.

### 2. `respuestaAdmin`/`fechaRespuesta` — ¿dependen de la migración?

Sí, dependen — y no solo el campo, **el endpoint entero de lectura** (ver punto 1). Una vez que la
migración corrió y el código está desplegado, ahí sí: van a salir `null` en **todas** las reseñas
existentes desde el primer momento, sin que ningún admin haya usado el `PUT` todavía — pueden
empezar a pintar el bloque de "respuesta del admin" (oculto si es `null`) desde que confirmemos que
ya está arriba, no hace falta esperar a que exista una respuesta real para probar el layout.

### 3. Historial de accesos

Confirmado, sin nada que agregar a lo que ya preguntaron — les avisamos en cuanto esté arriba.

---

**Sobre el otro tema de este mismo mensaje (`getOne` rompiendo la ficha de producto):** lo vimos,
es real y coincide con lo que reportan — trabajo aparte, respondemos por separado.

---

## ✅ Recibido — `getOne` rompiendo la ficha de producto, ya lo estamos atendiendo (2026-08-11)

Confirmado: **no promovemos `qa → main`** con el cierre de `/tienda/getOne`/`/tienda/v1/getOne`
hasta que ustedes avisen que su fix ya está arriba. Producción sigue como está mientras tanto, sin
riesgo. Ya estamos trabajando en el endpoint público que pidieron para cubrir el caso de link
directo — en cuanto esté listo, se los pasamos aquí mismo con el request/response definitivo antes
de que lo usen, no después.

---

## ✅ Respuesta del back — checklist del 2026-08-12

### 1. 🔴 Nuevo endpoint público — `varianteId → productoId`

Va la versión mínima, tal como pidieron:

```
GET /tienda/v1/variante/{varianteId}/producto-id
```

Público (mismo `permitAll` de `GET /tienda/**`, no requiere token). **No aplica ningún filtro de
visibilidad** (stock, habilitado) — mismo criterio que ya tiene `/variantes/v1/porProducto/{id}`,
que es el endpoint al que llaman después con este dato.

**Response 200:**
```json
{ "data": { "productoId": 265 } }
```

**Response 400:** `"No existe la variante con id: {id}"` si el id no existe.

Está en `dev`, compiló limpio y arrancó el contexto sin errores (validado contra la BD real de
QA — `varianteId 1 → productoId 265` es un dato real, no de prueba). **Lo único que no pude
verificar es la llamada HTTP en sí**, por un problema aparte que ya veníamos arrastrando: el cache
local fuerza Redis sin importar el perfil y no hay Redis corriendo en mi entorno de desarrollo —
en cuanto lo empuje a `qa` (que sí tiene Redis) lo probamos con curl y confirmamos aquí antes de
que ustedes lo conecten.

**Con esto ya no falta nada de nuestro lado para el caso del link directo.** En cuanto lo prueben
y confirmen, avisen y promovemos el cierre de `getOne` a producción como quedamos.

### 2. 🟠 Reseñas + historial de accesos

Ya está en `dev` y `qa` (push hecho), y la migración ya corrió — confirmado por nuestro lado
revisando directo la BD de QA: las columnas `respuesta_admin`/`fecha_respuesta` existen en
`resena`, y `historial_acceso` existe con el esquema correcto.

**Ojo, esto sí lo necesito de ustedes:** no he podido confirmar el flujo completo porque nadie ha
iniciado sesión en QA desde que se desplegó (la tabla `historial_acceso` sigue en 0 filas — no es
error, es que falta una prueba real). ¿Alguien puede hacer un login de prueba en QA? En cuanto
vea la fila nueva se los confirmo aquí y ya pueden conectar todo con confianza.

### 3. 🟠 `migracion_dedup_relaciones_imagenes.sql`

**Ya corrió y está verificada en QA**, desde antes de este checklist:
- `producto_imagen_copy`: 13,095 filas → **81** (pares reales)
- `variante_imagen`: 12,607 filas → **1,795** (pares reales)
- Verificado que ningún producto/variante se quedó sin imagen por la limpieza (0 casos)
- UNIQUE + índices agregados en ambas tablas, y el código ya evita que se vuelva a acumular
  basura (`vincularImagenes`/`compartirImagenesVarianteDto` filtran pares ya existentes antes de
  insertar)

**Prod:** vamos a confirmar con el usuario si ya corrió ahí también y les avisamos.

### 4. 🟡 Cinta — falta el clic

Anotado, no urgente. Cuando se retome, coordinamos el parámetro de búsqueda en la URL del
catálogo que van a necesitar.

---

## ✅ Respuesta del back — `getOne` se queda cerrado (no se revierte) + cierre de temas (2026-08-12)

### `getOne` ADMIN-only se mantiene

Surgió la duda de si cerrarlo rompía las vistas previas de WhatsApp/Facebook al compartir un link
de producto (esa era la razón por la que originalmente era público). Lo investigamos contra el
código real del front antes de decidir:

- El build es 100% cliente (`@angular-devkit/build-angular:browser`, sin `@angular/ssr` ni
  `@nguniversal`) — los bots de WhatsApp/Facebook **no ejecutan JavaScript**, así que nunca llegan
  a disparar ninguna llamada a la API al generar la vista previa. `getOne` nunca fue el mecanismo
  detrás de eso, público o no.
- `index.html` tiene el `og:image` **fijo** (`/assets/og-image.jpg`) — el mismo para todos los
  productos. Hoy, compartir el link de cualquier producto por WhatsApp muestra el logo genérico de
  la tienda, nunca la foto del producto. Eso no cambió con este fix, porque nunca dependió de él.

**Conclusión: `getOne` se queda como ADMIN-only.** No hay ninguna funcionalidad real que dependiera
de que fuera público — la fuga de `precioCosto`/`precioRebaja` que motivó el cierre sigue siendo el
problema real, y ya está cubierto el único caso legítimo que faltaba (el link directo, con el
resolver `varianteId → productoId` de la sección anterior).

**Dato aparte, no es un bug, es una feature que nunca existió:** si en algún momento quieren que
compartir un producto por WhatsApp muestre *esa* foto específica (no el logo genérico), hace falta
server-side rendering o un servicio que detecte al bot y le sirva HTML con meta tags dinámicos por
producto — trabajo nuevo, avísennos si lo quieren y lo planeamos.

**Ya pueden promover el cierre de `getOne` a producción** en cuanto confirmen que su fix de la
ficha (el que ya está en su `dev`/`qa`) sigue funcionando con el resolver nuevo.

### Migraciones — confirmado corridas en QA y producción

`migration_respuesta_resena_historial_acceso.sql` y `migracion_dedup_relaciones_imagenes.sql` ya
corrieron en ambos ambientes. El tema de imágenes duplicadas queda cerrado — sin más pendientes de
nuestro lado ahí.

---

## 🆕 BACK — nuevo módulo "Flores eternas": catálogos + motor de cálculo (2026-08-12)

Primera etapa de un módulo nuevo (ramos de flores eternas configurables). Ya está en `dev`/`qa` y
la migración (`migration_flores_eternas.sql`) ya corrió en **QA y producción**.

### ⚠️ Alcance de esta entrega — qué SÍ y qué NO

- **SÍ:** catálogos de administración (tipos de flor, cantidades válidas, accesorios, frases de
  listón, ramos preconfigurados) + un motor de cálculo público para cotizar un ramo en vivo.
- **NO todavía:** no existe endpoint para "confirmar" un ramo cotizado como un pedido real — falta
  decidir cómo se engancha con `Pedido`/`DetallePedido` de nuestro lado. El front ya puede armar
  toda la pantalla de configuración y mostrar el precio en vivo con lo que hay aquí, pero el botón
  final de "confirmar pedido" todavía no tiene a dónde pegarle. Avisamos en cuanto esté.

### Catálogos simples — CRUD genérico (mismo patrón que `/v1/lugares-entrega` y `/v1/cinta`)

GET (`getAll`/`getOne`) es **público** (sin login) en los 4 catálogos de abajo — el cliente
configura su ramo sin necesitar sesión. `save`/`update`/`delete` son **ADMIN**.

Mismas reglas ya conocidas del CRUD genérico: `getAll` exige `page` (base-0) y `size` en la URL
sin default; `delete` recibe el id crudo en el body (`1`, no `{ id: 1 }`); `save`/`update` reciben
el objeto de la entidad completo, no un DTO envuelto.

| Catálogo | Base URL | Campos |
|---|---|---|
| Tipos de flor | `/v1/tipos-flor` | `id`, `nombre`, `precioPorFlor`, `activo` |
| Cantidades válidas | `/v1/cantidades-flor` | `id`, `tipoFlor` (objeto anidado), `cantidad`, `activo` |
| Accesorios del ramo | `/v1/accesorios-ramo` | `id`, `nombre`, `precio`, `admiteTextoLibre`, `esPapel`, `activo` |
| Frases de listón predefinidas | `/v1/frases-liston` | `id`, `texto`, `precio`, `activo` |

**Nota sobre `cantidad-flor-valida`:** es "qué cantidades de flores sí forman bien el círculo",
por tipo de flor — ej. para "Rosa eterna" las válidas podrían ser 18, 20, 28, 32, 34, 48, 52. Al
guardar/editar, el body lleva el tipo de flor **anidado por id**, no hace falta mandar sus demás
campos:
```json
{ "tipoFlor": { "id": 1 }, "cantidad": 32, "activo": true }
```
El `GET` sí devuelve el objeto `tipoFlor` completo (igual que `Concursante` con `configurarRifa`,
si ya conocen ese patrón de rifas).

**`esPapel`** en accesorios: marca cuál accesorio es "el papel" para la regla del umbral (ver
motor de cálculo abajo). Debería haber como máximo un accesorio activo con `esPapel: true` a la
vez.

### Ramos preconfigurados (`/v1/ramos-armados`) — CRUD custom, no genérico

A diferencia de los catálogos de arriba, este no usa el patrón `/getAll`/`/getOne`/`/save`/`/update`
— tiene sus propias rutas (mismo estilo que `/v1/promociones`):

| Método | URL | Quién | Qué hace |
|---|---|---|---|
| `POST` | `/v1/ramos-armados` | ADMIN | Crear |
| `PUT` | `/v1/ramos-armados/{id}` | ADMIN | Editar |
| `PUT` | `/v1/ramos-armados/{id}/activo` | ADMIN | Activar/desactivar — body `{ "activo": true }` |
| `GET` | `/v1/ramos-armados/admin?pagina=1&size=10` | ADMIN | Lista todos (activos e inactivos) |
| `GET` | `/v1/ramos-armados/activos?pagina=1&size=10` | **Público** | Solo los activos, para la tienda |

**Ojo:** aquí `pagina` es **base-1** (como en `/v1/promociones`), no base-0 como el CRUD genérico
de arriba — es una inconsistencia que ya existe en el proyecto entre módulos, no es nueva de esto.

Body para crear/editar:
```json
{
  "nombre": "Ramo grande 48 rosas",
  "tipoFlorId": 1,
  "cantidadFlorValidaId": 5,
  "accesorios": [ { "accesorioId": 3, "cantidad": 1 } ],
  "activo": true
}
```
Response (creado/editado, y en las dos listas):
```json
{
  "id": 10,
  "nombre": "Ramo grande 48 rosas",
  "tipoFlorId": 1,
  "tipoFlorNombre": "Rosa eterna",
  "cantidad": 48,
  "precioFlores": 960.0,
  "papelIncluido": true,
  "precioPapel": 40.0,
  "accesorios": [ { "accesorioId": 3, "nombre": "Corona", "cantidad": 1, "precioUnitario": 80.0, "subtotal": 80.0 } ],
  "precioTotal": 1080.0,
  "activo": true
}
```
`papelIncluido`/`precioPapel` se calculan **automático** en el back si `cantidad > 10` (no hace
falta que el front lo mande ni lo agregue a `accesorios`) — ver la regla completa abajo.

### `/v1/lugares-entrega` ahora trae `costoEnvio`

Mismo endpoint de siempre, un campo nuevo en la respuesta: `costoEnvio` (número, puede venir
`null` si ese lugar no tiene costo de envío configurado — trátenlo como recoger en el local /
sin costo, no como error).

### Motor de cálculo — público, sin login (`POST /v1/flores/...`)

Estos dos no son CRUD, son cálculo puro (no guardan nada todavía, ver nota de alcance arriba).

**1. `POST /v1/flores/validar-cantidad`** — el cliente escribe libremente cuántas flores quiere;
esto dice si esa cantidad forma bien el círculo o no, y si no, ofrece alternativas.

Request:
```json
{ "tipoFlorId": 1, "cantidadSolicitada": 30 }
```
Response:
```json
{
  "cantidadSolicitada": 30,
  "precioCantidadSolicitada": 600.0,
  "valida": false,
  "mensaje": "Con 30 flores el circulo puede no quedar bien formado.",
  "alternativaMenor": 28,
  "precioAlternativaMenor": 560.0,
  "alternativaMayor": 32,
  "precioAlternativaMayor": 640.0
}
```
Si `valida: true`, las 4 alternativas vienen en `null` (no hace falta mostrarlas). Si la cantidad
pedida es menor a la más chica configurada en el catálogo (ej. pide 1 o 2), también responde
`valida: true` directo — es la venta "por unidad", no aplica el ajuste de círculo.

**2. `POST /v1/flores/calcular-precio`** — con la cantidad ya decidida, arma el desglose completo
y el total (accesorios, papel automático, listón, envío).

Request:
```json
{
  "tipoFlorId": 1,
  "cantidadFinal": 32,
  "accesorios": [ { "accesorioId": 3 }, { "accesorioId": 3 } ],
  "listones": [
    { "fraseListonPredefinidaId": 2 },
    { "fraseListonPersonalizada": "Te amo mamá" }
  ],
  "lugarEntregaId": 5,
  "recogerEnLocal": false
}
```
Nota sobre `accesorios`: **repetir la misma entrada tantas veces como unidades se quieran** — en
el ejemplo de arriba, dos entradas con `accesorioId: 3` = 2 unidades de ese accesorio, cobradas
cada una. Igual con `listones`: cada entrada es UN listón; cada uno trae **o** una frase
predefinida (`fraseListonPredefinidaId`) **o** una personalizada (`fraseListonPersonalizada`),
nunca las dos ni ninguna.

Response:
```json
{
  "cantidadFinal": 32,
  "precioBase": 640.0,
  "papelObligatorioAplicado": true,
  "precioPapel": 40.0,
  "accesoriosCalculados": [
    { "accesorioId": 3, "nombre": "Corona", "cantidad": 2, "precioUnitario": 80.0, "subtotal": 160.0, "agregadoAutomaticoPorRegla": false }
  ],
  "subtotalAccesorios": 160.0,
  "listonesCalculados": [
    { "texto": "Felicidades", "tipo": "PREDEFINIDA", "precio": 50.0 },
    { "texto": "Te amo mamá", "tipo": "PERSONALIZADA_PENDIENTE", "precio": null }
  ],
  "subtotalListones": 50.0,
  "tieneListonPendienteValidacion": true,
  "requiereAnticipo50Porciento": true,
  "montoAnticipoSugerido": 445.0,
  "avisoNoReembolso": "Este ramo incluye una frase personalizada pendiente de validar. Se requiere un anticipo del 50% para producirlo. Una vez entregado el ramo no hay reembolsos ni cancelaciones.",
  "recogerEnLocal": false,
  "costoEnvio": 30.0,
  "total": 890.0
}
```

**Reglas de negocio detrás de estos números (para que la UI tenga sentido):**
- **Papel automático:** si `cantidadFinal > 10`, el papel se cobra solo — no hay que preguntarle
  al cliente ni mandarlo en `accesorios`. Con 10 o menos, el papel es un accesorio opcional más
  (mándenlo en `accesorios` como cualquier otro si el cliente lo quiere).
- **`tieneListonPendienteValidacion: true`** → el `total` es **provisional** (no incluye el precio
  de la frase personalizada, todavía no existe). Hay que mostrarle al cliente el
  `montoAnticipoSugerido` y el `avisoNoReembolso` tal cual, antes de que confirme — es política
  de negocio, no redacción libre del front.
- **Envío:** o se manda `lugarEntregaId` (de la lista de `/v1/lugares-entrega`, **no texto
  libre**) o se manda `recogerEnLocal: true`. Si no se manda ninguno de los dos, `costoEnvio`
  vuelve `null` (todavía no decidido) y no cuenta en el `total`.

**Errores:** `400` con el motivo en `mensaje` (ej. tipo de flor sin `id`, cantidad ≤ 0, accesorio
inactivo, listón sin frase). `404` si el `tipoFlorId`/`accesorioId`/`fraseListonPredefinidaId`/
`lugarEntregaId` no existe. `500` solo ante error interno inesperado.

---

## 🌹 FRONT — catálogos de flores eternas conectados + un 401 que hay que revisar (2026-08-13)

Recibido el módulo. Ya están en `dev`/`qa` del front las **pantallas de administración de los
catálogos** (tipos de flor, cantidades válidas, accesorios y frases de listón), en una sola
pantalla con 4 pestañas — `/flores/catalogos`, menú 🌹 Flores eternas.

**Lo que NO hicimos todavía, a propósito:** la pantalla del cliente (configurador del ramo con
precio en vivo). Como ustedes mismos anotaron, no existe endpoint para confirmar el ramo cotizado
como pedido, así que esa pantalla terminaría en un botón sin destino. Preferimos esperar a que nos
digan cómo se engancha con `Pedido`/`DetallePedido`. El servicio del front ya tiene conectados
`validar-cantidad` y `calcular-precio`, listos para usarse en cuanto exista el cierre del flujo.

### ⚠️ Los GET que documentaron como públicos responden 401 en QA

Probado hoy sin token contra QA:

```
GET /v1/tipos-flor/getAll?page=0&size=5        → 401
GET /v1/accesorios-ramo/getAll?page=0&size=5   → 401
GET /v1/frases-liston/getAll?page=0&size=5     → 401
POST /v1/flores/validar-cantidad               → 401
GET /v1/ramos-armados/activos?pagina=1&size=10 → 401
```

**Aclaramos que el 401 por sí solo no prueba que falte el `permitAll`:** una ruta inventada
(`/v1/no-existe-nada/getAll`) también responde 401, así que ese código parece ser la respuesta
genérica para todo lo que no está explícitamente permitido — no distingue "no desplegado" de
"requiere token". Lo que sí es comparable: `GET /v1/cinta/activos`, que es público y sí está
desplegado, responde **200** en el mismo ambiente y sin token.

Entonces, o el módulo todavía no está en QA, o le faltó el `permitAll` a esas rutas. ¿Nos
confirman cuál de las dos? Lo preguntamos porque:

- **No nos bloquea hoy:** las pantallas de catálogos son de admin y mandan token, así que
  funcionarán en cuanto el módulo esté arriba.
- **Sí bloquearía la pantalla del cliente**, que es justo la que depende de que esos GET y el
  motor de cálculo sean públicos (el cliente arma su ramo sin sesión).

### Dudas concretas del contrato

1. **`esPapel`:** entendemos que debe haber **máximo un accesorio activo** con `esPapel: true`.
   La pantalla ya impide marcar un segundo, pero ¿el back lo valida también, o si por BD quedaran
   dos activos el cálculo elegiría uno arbitrario?
2. **Cantidad menor a la más chica del catálogo:** dicen que responde `valida: true` directo
   ("venta por unidad"). ¿Eso significa que el precio es `cantidad × precioPorFlor` sin ningún
   ajuste, y que tampoco se le agrega papel aunque supere las 10? (por si alguien pide 12 pero no
   hay una cantidad válida configurada tan baja).
3. **`costoEnvio` en `/v1/lugares-entrega`:** ya lo vemos documentado como campo nuevo. ¿Ese
   costo aplica **solo** a flores, o también debería empezar a mostrarse/cobrarse en los pedidos
   normales de la tienda? Hoy el front no lo usa en ningún flujo existente; si aplica a todo,
   habría que meterlo en venta directa y en el checkout, y eso es trabajo aparte que preferimos
   no asumir por nuestra cuenta.
4. **Ramos preconfigurados:** al crearlos mandamos `cantidadFlorValidaId`. ¿Ese id es el de la
   tabla de cantidades válidas (`/v1/cantidades-flor`), verdad? Lo asumimos así porque el nombre
   coincide, pero no viene explícito en el ejemplo.

### 🚧 Lo que nos hace falta para poder seguir — y por qué

Complemento de las dudas de arriba (que son de contrato). Esto es lo que **bloquea la siguiente
etapa**: la pantalla donde el cliente arma su ramo. Ninguna de estas es urgente para lo que ya
entregamos, pero sin resolverlas esa pantalla no se puede hacer completa, y preferimos no
construirla a medias y tener que rehacerla.

**1. Cómo se convierte un ramo cotizado en un pedido real.** Es el bloqueo principal, ya lo
anotaron ustedes. Sin esto, el configurador termina en un botón que no puede hacer nada.
Concretamente necesitamos saber: ¿se crea un `Pedido` normal con un `DetallePedido` especial, o
es una entidad aparte? ¿Y qué se manda — la configuración completa del ramo (flores, accesorios,
listones) o solo un total ya calculado? Lo preguntamos porque de eso depende si el ramo se puede
mezclar en el mismo pedido con productos de la tienda o si va siempre solo.

**2. Los GET públicos (el 401 de arriba).** Sin eso el cliente tendría que iniciar sesión para
ver siquiera los precios, y por lo que entendemos la idea es justo la contraria.

**3. Cómo se cobra el anticipo del 50%.** El motor ya devuelve `requiereAnticipo50Porciento` y
`montoAnticipoSugerido` cuando hay frase personalizada, pero no hay nada que diga **cómo se
registra ese pago**. ¿Se apoya en el módulo de abonos que ya existe (crear el pedido como
APARTADO y registrar el anticipo como primer abono), o es un flujo nuevo? Si es lo primero, del
lado del front ya está casi todo hecho y sería cuestión de enlazarlo; si es nuevo, hay que
diseñarlo.

**4. Quién valida la frase personalizada, y dónde.** El cálculo la deja como
`PERSONALIZADA_PENDIENTE` con precio `null`. Falta la otra mitad: ¿el admin ve en algún lado la
lista de frases pendientes, les pone precio y eso actualiza el total del pedido? Si va a existir
esa pantalla, es front que todavía no está ni planeado — avísennos con tiempo.

**5. ¿Las flores tienen inventario?** En el catálogo actual todo tiene stock y el sistema impide
vender de más. Aquí no vimos nada de existencias: un cliente podría pedir 200 rosas aunque no
haya. ¿Es intencional (se producen sobre pedido) o falta esa parte? No es lo mismo para la
pantalla: si hay stock, hay que mostrarlo y bloquear cantidades.

**6. Dónde vive esto para el cliente.** ¿Es una sección aparte del menú, o las flores aparecen
también dentro de la tienda junto a bolsas y blusas? Y los **ramos preconfigurados**, ¿se ven
como productos normales del catálogo o solo dentro del configurador? Esto define si hay que
tocar el catálogo actual o no.

**7. ¿El ramo pasa por el carrito?** Hoy el carrito guarda variantes y promociones. Un ramo es
otra cosa (una configuración, no un producto con id). Si debe convivir en el mismo carrito, hay
que extenderlo; si va directo a "confirmar pedido" sin pasar por ahí, es más simple. Nos sirve
saberlo antes de empezar, porque cambia bastante la estructura.

**Nota de método:** no asumimos ninguna de estas por nuestra cuenta a propósito. Ya nos pasó en
este proyecto que dar por hecho una regla de negocio (los precios de las promociones, el orden de
las migraciones) sale más caro que preguntar. Con que nos contesten 1 y 2 podemos avanzar bastante;
las demás se pueden ir resolviendo sobre la marcha.

---

## ✅ Respuesta del back — flores eternas: el 401, las dudas, y el flujo completo de pedido (2026-08-13)

### ⚠️ El 401: no era un bug, el módulo nunca había llegado a QA

Confirmado. Todo el trabajo de flores eternas (catálogos + motor de cálculo) se quedó en
nuestro `dev` local sin subir hasta ahora — nunca llegó a `qa`, por eso el 401 parejo en todo,
incluyendo rutas que ya estaban documentadas como `permitAll`. **Ya está commiteado y pusheado a
`dev`** (commit `d12dea8`). En cuanto lo suban a `qa` con su merge normal, esos GET van a
responder 200 sin token — no hubo que tocar `SecurityConfig`, el `permitAll` que documentamos ya
estaba bien desde el principio, simplemente no existía en el ambiente donde probaron.

### Dudas concretas del contrato

**1. `esPapel` — ahora sí se valida en el back.** Guardamos un accesorio con `esPapel: true` y
`activo: true` mientras ya existe otro también activo con `esPapel: true` → `400` con mensaje
explicando cuál es el que hay que desactivar primero. Ya no puede quedar más de uno activo ni por
error de BD ni por dos pestañas del admin guardando a la vez.

**2. Cantidad menor a la más chica del catálogo (venta "por unidad") — confirmado como
asumieron, con una precisión:** el precio es `cantidad × precioPorFlor` sin ningún ajuste, **pero
el papel automático sigue aplicando si `cantidadFinal > 10`**, sin importar que esa cantidad haya
entrado por el camino de "sin catálogo configurado". Son dos reglas independientes: la de
"cantidad válida" solo decide si se sugiere una alternativa; la del papel solo mira el número
final. Ejemplo con su caso (12 flores, sin cantidad válida tan baja configurada): `validar-cantidad`
responde `valida: true` sin alternativas, pero al llamar `calcular-precio` con `cantidadFinal: 12`,
`papelObligatorioAplicado` sale `true` igual.

**3. `costoEnvio` — solo aplica a flores eternas, no a nada más.** Reutilizamos `LugarEntrega`
únicamente para no crear un catálogo de "zonas" paralelo (es exactamente lo mismo: lista fija de
lugares configurados), pero **ningún código del checkout normal de la tienda lee `costoEnvio`
hoy** — ni venta directa, ni `PedidoServiceImpl`, ni el checkout del cliente. Es un campo nuevo
que solo consume el motor de cálculo de flores (`POST /v1/flores/calcular-precio`). No hace falta
que lo muestren ni lo cobren en ningún flujo existente — de hecho, por favor no lo hagan sin que
lo pidamos explícito, porque cobrar envío en la tienda general es una decisión de negocio aparte
que no se ha tomado.

**4. `cantidadFlorValidaId` en ramos armados — sí, confirmado.** Es el `id` de
`/v1/cantidades-flor` (la entidad `CantidadFlorValida`). Nombrado igual a propósito para que no
quedara duda.

### 🚧 Las 7 cosas que bloqueaban la pantalla del cliente — respondidas

**1. Cómo se convierte un ramo cotizado en un pedido real — esta es la pieza grande que
armamos esta sesión.** Respuesta corta: **no hay un endpoint nuevo para "confirmar el ramo"** —
se usa el que ya existe, `POST /v1/pedidos/savePedido`, exactamente igual que para bolsas o
blusas. La única diferencia es de dónde sale el `varianteId` de cada línea.

Cómo queda el flujo completo:

1. El cliente configura el ramo con `POST /v1/flores/validar-cantidad` y
   `POST /v1/flores/calcular-precio` (sin cambios, esos dos ya los tienen conectados).
2. **`calcular-precio` ahora también devuelve el `varianteId` de cada componente con precio
   conocido** — es lo que agregamos hoy:
   - `tipoFlorVarianteId` → la línea de las flores (cantidad = `cantidadFinal`, precioUnitario =
     `precioBase / cantidadFinal`, subTotal = `precioBase`).
   - `papelVarianteId` → si `papelObligatorioAplicado` es `true` (cantidad 1, precio =
     `precioPapel`).
   - `accesoriosCalculados[].varianteId` → uno por cada accesorio distinto elegido (cantidad =
     la que ya viene en `accesoriosCalculados[].cantidad`).
   - `listonesCalculados[].varianteId` → solo en los de `tipo: "PREDEFINIDA"` (los
     `PERSONALIZADA_PENDIENTE` no tienen variante todavía, ver punto 4 más abajo).
   - `envioVarianteId` → si hay `costoEnvio` (null si `recogerEnLocal` o si el lugar no tiene
     costo configurado).
3. Con esos `varianteId`, arman el `POST /v1/pedidos/savePedido` de siempre: **una línea en
   `detalles` por cada componente** (mismo contrato de siempre: `varianteId`, `cantidad`,
   `precioUnitario`, `subTotal` — nada nuevo, es el mismo body que ya usan para un carrito
   normal). El `precioUnitario`/`subTotal` que manden tiene que coincidir exacto con el que
   devolvió `calcular-precio` (la validación de precio de catálogo que ya existe en
   `savePedido` aplica igual — no es una regla nueva para flores, es la misma de siempre).
4. **Respuesta a "¿se puede mezclar con productos normales de la tienda en el mismo pedido?":
   sí, sin ningún problema.** Cada línea de flor/accesorio/envío es una línea de `DetallePedido`
   como cualquier otra — pueden convivir en el mismo array `detalles` junto con bolsas, blusas,
   lo que sea. No hicimos ninguna entidad "Pedido de flores" aparte.
5. **No hicimos ningún cambio a `PedidoServiceImpl`.** Cero riesgo de regresión en el checkout
   existente — desde su perspectiva, un pedido con flores es indistinguible de uno con productos
   normales.

**Cómo lo logramos (por si les sirve de contexto):** cada catálogo de flores
(`TipoFlor`/`AccesorioRamo`/`FraseListonPredefinida`/`LugarEntrega`) ahora tiene, por dentro, una
variante "sombra" con su producto detrás, que se crea/actualiza sola cuando el ADMIN guarda el
catálogo (nunca se edita a mano, no aparece como acción separada en ningún lado). Se las
mencionamos porque van a ver nombres tipo `[Flores eternas] Rosa eterna` si alguna vez consultan
`/v1/productos` o `/v1/variantes` directo — es intencional, no lo borren ni lo editen desde esas
pantallas.

**2. Los GET públicos (el 401).** Resuelto arriba — ya está en `dev`, falta su merge a `qa`.

**3. Cómo se cobra el anticipo del 50% — decidimos reutilizar abonos, con un matiz
importante.** No se puede simplemente crear el pedido completo de flores como `APARTADO`,
porque las líneas con precio conocido (flores, papel, accesorios, listón predefinido, envío) ya
se cobran de una vez con `savePedido` normal — meterlas a crédito solo por culpa de una frase sin
precio mezclaría dos cosas distintas. En vez de eso:

- El pedido de flores se crea **siempre `NORMAL`** (o lo que corresponda según cómo pague el
  cliente el resto), igual que cualquier otro.
- Si hay una frase personalizada pendiente, se guarda aparte en `RamoPedidoDetalle` (ver punto 4).
- **Cuando el ADMIN aprueba esa frase y le pone precio** (`PUT
  /v1/flores/pedidos/detalle/{id}/validar-frase`), el back automáticamente:
  1. Crea un producto/variante "sombra" solo para esa frase (con el precio que el admin le puso).
  2. Crea un **`Pedido` nuevo y separado**, del mismo cliente, `tipoPedido: "APARTADO"`, con una
     sola línea: esa frase.
  3. Devuelve `pedidoAnticipoId` (el id de ese pedido nuevo) en la respuesta.
- El front toma ese `pedidoAnticipoId` y registra el anticipo con el flujo de abonos que **ya
  tienen hecho**: `POST /v1/abonos/{pedidoAnticipoId}` con el monto (la respuesta de
  `validar-frase` también trae `montoAnticipo`, ya calculado al 50% del precio asignado).
- Todo el tracking de "cuánto se ha pagado", el auto-cierre a `PAGADO` cuando se completa, y la
  cancelación, es el módulo de abonos de siempre — no inventamos nada nuevo ahí.

Por qué separado y no el pedido original: así el pedido "de verdad" (el que ya se cobró
completo) no quede con `tipoPedido: APARTADO` de forma incorrecta, y el reporte de crédito solo
muestre lo que realmente es crédito (la frase), no el ramo completo.

**4. Quién valida la frase personalizada, y dónde — ya existe el endpoint, falta la pantalla.**
```
PUT /v1/flores/pedidos/detalle/{id}/validar-frase   (ADMIN)
Body: { "aprobar": true, "precioAsignado": 80.0, "anticipoPagado": false }
```
Aprueba (con precio) o rechaza (`aprobar: false`) la frase, y opcionalmente marca si ya se pagó
el anticipo. Para listar las pendientes: no hay todavía un endpoint de "dame todas las frases
`PENDIENTE_VALIDACION` de todos los pedidos" — hoy solo existe
`GET /v1/flores/pedidos/{pedidoId}/detalle` (por pedido puntual). Si quieren una pantalla tipo
"bandeja de frases pendientes", avísennos y armamos el endpoint de listado global — no lo hicimos
porque no sabíamos si iba a hacer falta.

**5. ¿Las flores tienen inventario? Sí, real, ya implementado.** `TipoFlor` tiene un campo
`stock` (flores sueltas disponibles) que edita el admin directo en el catálogo de tipos de flor
— no hay que ir a ningún lado más. Se descuenta de verdad al vender (vía la línea real de
`savePedido`, exactamente igual que el stock de cualquier variante). Si intentan vender más
flores de las que hay, `savePedido` responde el mismo error de "stock insuficiente" que ya
conocen de productos normales. Los accesorios/listones/envío **no** controlan inventario
(stock fijo interno, nunca se agotan) — eso sí fue decisión nuestra, avisen si en algún momento
quieren limitarlos también.

**6. Dónde vive esto para el cliente — decisión de negocio: sección aparte del menú.** Y los
ramos preconfigurados **sí se navegan como catálogo normal** (`GET /v1/ramos-armados/activos`
para el listado público) — técnicamente ya encajan porque, como cualquier flor, ya tienen
variante real detrás. El configurador "a la medida" es la parte que vive solo dentro de la
sección de flores, no mezclada con bolsas/blusas.

**7. ¿El ramo pasa por el carrito?** Como cada línea (flor, papel, accesorio, listón, envío) es
una variante real, técnicamente sí puede pasar por el mismo carrito que ya tienen — es una
decisión de front, no hay ninguna restricción de back. Lo único que cambia es que un "ramo" en el
carrito probablemente se vea como **varias líneas agrupadas visualmente bajo una tarjeta**, no
como una sola línea con un solo `id` de producto (parecido a como ya manejan el agrupado de una
promoción, según entendimos de esa sección del documento).

### Resumen de lo nuevo que pueden empezar a usar

| Endpoint | Qué cambió |
|---|---|
| `POST /v1/flores/calcular-precio` | Ahora devuelve `varianteId` en cada componente (ver punto 1 arriba) |
| `POST /v1/pedidos/savePedido` | Sin cambios — así se confirma un ramo cotizado como pedido real |
| `POST /v1/flores/pedidos/{pedidoId}/detalle` | Nuevo — guarda el "ticket de producción" del ramo (frase, contacto, entrega) después de crear el pedido con `savePedido` |
| `GET /v1/flores/pedidos/{pedidoId}/detalle` | Nuevo — consulta ese ticket |
| `PUT /v1/flores/pedidos/detalle/{id}/validar-frase` | Nuevo, ADMIN — aprueba/rechaza la frase personalizada, devuelve `pedidoAnticipoId` y `montoAnticipo` para registrar el abono |
| `TipoFlor.stock` | Nuevo — inventario real de flores sueltas |

**Pendiente de nuestro lado:** correr `migration_flores_eternas_pedido.sql` en QA (la primera,
`migration_flores_eternas.sql`, ya corrió en QA y producción). Avisamos cuando esté.

---

## ✅ QA listo — ya pueden probar sin 401 (2026-08-13)

Cerrado lo que quedaba pendiente:

- **Ambas migraciones ya corrieron en QA y producción** (`migration_flores_eternas.sql` y
  `migration_flores_eternas_pedido.sql`).
- **El código ya está mergeado a `qa` y pusheado** — el push disparó el deploy automático
  (build + SSH a QA) igual que cualquier otro merge a esa rama. Denle unos minutos si acaban de
  ver esto y todavía les da 401; si después de 10-15 min sigue igual, avísennos.

Con esto, los GET que documentamos como públicos deberían responder 200 sin token en QA:
```
GET /v1/tipos-flor/getAll?page=0&size=5
GET /v1/accesorios-ramo/getAll?page=0&size=5
GET /v1/frases-liston/getAll?page=0&size=5
POST /v1/flores/validar-cantidad
GET /v1/ramos-armados/activos?pagina=1&size=10
```

Y ya pueden probar de punta a punta el flujo completo que documentamos arriba: `calcular-precio`
→ `savePedido` con los `varianteId` → `POST /v1/flores/pedidos/{pedidoId}/detalle` →, si hay
frase personalizada, `validar-frase` → `POST /v1/abonos/{pedidoAnticipoId}`.

Si al probar encuentran algo que no cuadra con lo documentado, o alguna de las 7 dudas que
respondimos quedó coja, avisen — preferimos que lo digan ahora que arrancar la pantalla del
cliente con un supuesto equivocado.

---

## ❓ FRONT — dudas tras leer el contrato completo de flores (2026-08-13)

Primero: **confirmado que QA ya responde**. Probado sin token hace un momento —
`tipos-flor/getAll`, `accesorios-ramo/getAll`, `frases-liston/getAll` y `ramos-armados/activos`
responden **200**. `validar-cantidad` responde 400 "Tipo de flor no encontrado: 1", que es lo
correcto porque los catálogos están vacíos. Gracias por el despliegue.

Ahora las dudas que nos quedaron. La primera es la que más nos preocupa.

### 1. 🔴 Las variantes "sombra": ¿aparecen en el catálogo público y en los buscadores?

Entendemos y nos gusta la solución (por eso `savePedido` funciona sin cambios). Pero no queda
claro qué tanto se asoman esas variantes al resto del sistema. En concreto:

- **¿Salen en `GET /tienda/v1/buscar` y `buscar-filtrado`?** Si sí, un cliente navegando la
  tienda entre bolsas y blusas se va a encontrar `[Flores eternas] Rosa eterna` como producto
  suelto — y podría **agregar una sola rosa al carrito** por fuera del configurador, sin
  accesorios ni papel ni nada. Eso sería un problema de negocio, no solo estético.
- **¿Salen en los buscadores de admin?** (`/v1/productos/admin/filtrar`,
  `/tienda/v1/admin/filtrar`). Ahí no sería tan grave, pero también aparecerían en: el selector
  de variantes al armar una **promoción**, el de **rifas**, la pantalla de **carga rápida de
  imágenes**, y el **reporte de productos más vendidos** (donde "Rosa eterna" competiría contra
  productos reales, distorsionando el ranking).

Si hoy salen, ¿pueden marcarlas de alguna forma para excluirlas — un flag, una palabra clave
reservada, `habilitado: false` con una excepción interna, lo que les acomode? De nuestro lado
podemos filtrarlas por el prefijo `[Flores eternas]` del nombre, pero preferimos **no** hacerlo:
filtrar por texto es frágil y se rompe el día que alguien renombre el prefijo.

**Ligado a esto — el stock:** dicen que `TipoFlor.stock` se edita en el catálogo de flores, pero
esa variante sombra **también tiene stock editable desde la pantalla de variantes**. ¿Están
sincronizados en ambos sentidos, o si un admin lo cambia desde variantes se desincroniza y el
catálogo de flores muestra otro número?

### 2. 🟠 Falta el body de `POST /v1/flores/pedidos/{pedidoId}/detalle`

Lo listan en la tabla de endpoints nuevos ("guarda el ticket de producción del ramo: frase,
contacto, entrega") pero **no viene el request en ningún lado** — ni campos, ni cuáles son
obligatorios, ni la respuesta. Sin eso no lo podemos llamar. ¿Nos lo pasan?

Aprovechando: ¿es **obligatorio** llamarlo después de cada `savePedido` con flores, o solo cuando
hay frase personalizada? Lo preguntamos porque cambia el flujo: si es obligatorio siempre, hay que
manejar el caso de "el pedido se creó pero el ticket falló" (pedido huérfano sin datos de
producción).

### 3. 🟠 El anticipo del 50%: hay dos montos distintos, y un problema de momento

Encontramos dos cifras con el mismo nombre y valor muy diferente:

- `calcular-precio` → `montoAnticipoSugerido` = **50% del total provisional del ramo completo**
  (en su propio ejemplo: 445.0 sobre un total de 890.0).
- `validar-frase` → `montoAnticipo` = **50% del precio que el admin le asignó a la frase**
  (si la frase vale 80, serían 40).

No son lo mismo ni de cerca. ¿Cuál es el que realmente se cobra?

Y el problema de fondo es **cuándo**: por el flujo que describen, el precio de la frase lo pone el
admin *después* de que el pedido ya se creó y se cobró. Para entonces el cliente ya se fue. Pero
`calcular-precio` devuelve el aviso de anticipo **al cotizar**, o sea antes — como si se cobrara
en ese momento.

¿Cómo es en la práctica?
- **(a)** El cliente paga el 50% del ramo completo al hacer el pedido, y lo de `validar-frase` es
  otra cosa (¿el saldo?).
- **(b)** El cliente paga todo lo de precio conocido normal, se va, y después se le cobra aparte
  el anticipo de la frase cuando el admin la aprueba (¿por WhatsApp? ¿vuelve al local?).
- **(c)** Otra.

Lo preguntamos porque de esto depende qué le mostramos al cliente en pantalla **antes** de que
confirme. Hoy el `avisoNoReembolso` habla de un anticipo que, si es (b), todavía no se le puede
cobrar en ese momento — le estaríamos pidiendo algo que no se puede pagar ahí.

### 4. 🟡 Sí queremos el listado global de frases pendientes

Tomándoles la palabra: **sí nos hace falta.** Sin él, para encontrar una frase por aprobar el
admin tendría que ir abriendo pedido por pedido a ver si tiene alguna — no es una pantalla usable.

Lo que nos serviría: `GET /v1/flores/pedidos/frases-pendientes?pagina=&size=` (ADMIN), con lo
mínimo para armar la bandeja — `detalleId` (para mandarlo a `validar-frase`), `pedidoId`, el texto
de la frase, nombre del cliente y fecha del pedido. Si le pueden agregar un filtro por estado
(pendientes / aprobadas / rechazadas), mejor, pero con las pendientes nos basta para empezar.

### 5. 🟡 Lo del carrito lo decidimos nosotros

Anotado que no hay restricción de su lado. Es decisión de negocio del dueño (si un ramo se puede
mezclar con bolsas en el mismo carrito o va en un flujo aparte); lo consultamos y les avisamos
solo si termina necesitando algo del back.

### Mientras tanto

No arrancamos la pantalla del cliente hasta tener 2 y 3 — sobre todo 3, porque es la que puede
hacernos construir un flujo de pago equivocado. Los catálogos de admin ya están en `qa` y los
vamos a probar en cuanto carguemos datos de prueba; si algo no cuadra con lo documentado, les
avisamos por aquí.

### ✅ Decisión del dueño sobre el punto 1: flores en sección aparte, y fuera de la tienda general

Consultado con el dueño, y coincide con lo que ustedes proponían en su punto 6: **las rosas
eternas van en su propia sección, no mezcladas con la tienda.** Eso deja de ser una duda abierta.

Lo que sí queda como petición firme, porque la pantalla aparte **no lo resuelve sola**: las
variantes sombra viven en la misma tabla que alimenta el buscador del catálogo, así que aunque el
cliente compre flores en otra pantalla, `GET /tienda/v1/buscar` las va a seguir encontrando si
nadie las excluye.

**Necesitamos que no aparezcan en la tienda general** — ni en el buscador público, ni en los
filtros. Como decíamos, del lado del front podríamos filtrarlas por el prefijo `[Flores eternas]`
del nombre, pero **preferimos no hacerlo**: se rompe el día que alguien renombre ese prefijo, y
además el filtrado quedaría duplicado en cada pantalla que consulte variantes. Nos sirve
cualquier marca estable del lado de ustedes (un flag, una categoría reservada, lo que les
acomode) — con que sea algo que podamos preguntar sin adivinar por texto.

Nota aparte: el dueño confirmó que **no se venden flores por unidad** — solo ramos. Así que si
hoy esas variantes son comprables sueltas desde la tienda, eso también hay que cerrarlo.

---

## 🌹 FRONT — el flujo del cliente según el dueño, y dos cosas que no cuadran con el contrato (2026-08-13)

Sentamos con el dueño a que nos describiera **cómo quiere que el cliente arme su ramo**. Lo
anotamos aquí tal como lo dijo, porque es la referencia de negocio para la pantalla — y porque al
contrastarlo con lo que ya está implementado salieron dos diferencias, una de ellas seria.

### El flujo, en palabras del dueño

1. El cliente va **eligiendo las flores** — y aquí lo importante: *"puede que quiera de varios
   colores"*.
2. Escribe cuántas quiere. Si pide 10 y con 10 el ramo no queda bien formado, el sistema le
   avisa: *"este ramo no quedaría con 10 flores, deberían ser 8 o 12 para que quede formado
   correctamente"*, y **el cliente decide**.
3. Según la cantidad, **el pliego (papel) ya va incluido en el cobro** — el back lo agrega, el
   front no pregunta nada.
4. Si son pocas flores (1, 2, 3), se le **pregunta** si quiere papel; si dice que sí, se suma
   — y **se cobra una sola vez**, no por flor.
5. Se le pregunta si quiere listón: se le muestra la **lista de frases disponibles**, más la
   opción de **escribir una frase propia**.
6. Con todo eso se recalcula el total.

Los puntos 2, 4 (lo de cobrar el papel una sola vez), 5 y 6 ya están cubiertos por
`validar-cantidad` y `calcular-precio` tal como están. Los otros dos, no:

### 1. 🔴 Un ramo de varios colores no se puede expresar hoy

En el catálogo, "Rosa roja" y "Rosa blanca" son dos `TipoFlor` distintos. Pero tanto
`validar-cantidad` como `calcular-precio` aceptan **un solo tipo de flor por ramo**:

```json
{ "tipoFlorId": 1, "cantidadFinal": 32, ... }
```

No hay forma de pedir "6 rojas y 6 blancas en el mismo ramo", que es justo lo que el dueño
describe como caso normal. Tal como está, el cliente solo puede armar ramos de un color.

**Lo que necesitaríamos:** que ambos endpoints acepten una **lista** de flores, algo como
`"flores": [ { "tipoFlorId": 1, "cantidad": 6 }, { "tipoFlorId": 2, "cantidad": 6 } ]`, y que la
respuesta desglose una línea (con su `varianteId`) por cada tipo — igual que ya hacen con los
accesorios.

Dos decisiones que les tocan a ustedes y nos condicionan la pantalla:

- **¿La validación del círculo se hace sobre el total?** O sea, ¿12 flores en total forman bien
  el círculo aunque sean de dos colores, o cada tipo tiene que ser por sí solo una cantidad
  válida? Nuestra lectura del negocio es que **importa el total** (el círculo lo forman todas
  las flores juntas), pero no queremos asumirlo.
- **Si las cantidades válidas están configuradas por tipo de flor**, ¿contra cuál se valida
  cuando hay varios tipos mezclados?

No arrancamos la pantalla del cliente sin esto: es estructural, no un ajuste cosmético. Si tienen
que cambiar la forma del request, preferimos escribir el front una sola vez.

### 2. 🟠 El umbral del papel no coincide

- **Implementado:** el papel se agrega automático cuando `cantidadFinal > 10`. De 10 para abajo
  es un accesorio opcional que el front manda si el cliente lo pide.
- **Lo que describe el dueño:** con 1 flor se le pregunta, pero a partir de **2 o 3 flores** el
  papel ya va incluido de todos modos.

O sea, con lo que está hecho hoy, un cliente que pide 4 rosas **no** paga papel salvo que lo
pida explícitamente — y según el dueño, ahí ya debería ir incluido.

El dueño está definiendo el número exacto (si es a partir de 2, 3 o 4) y se los pasamos en
cuanto lo confirme. Pero aprovechamos para preguntar algo que nos parece más importante que el
número en sí:

**¿Ese umbral puede quedar configurable en vez de fijo en el código?** Hoy está fijo, así que
cada vez que el dueño lo quiera mover hay que pedírselo a ustedes y esperar un despliegue.
Si viviera como un campo del catálogo (o una configuración del módulo, junto al accesorio marcado
`esPapel`), él lo cambiaría solo y nosotros no tendríamos que molestarlos. Lo mismo aplicaría, si
les parece, al criterio de "cuántas flores son 'pocas'" para preguntar en vez de cobrar directo.

### Lo demás sigue igual

Las dudas del mensaje anterior (variantes sombra fuera de la tienda, body de
`POST /v1/flores/pedidos/{pedidoId}/detalle`, el doble monto del anticipo, y el listado global de
frases pendientes) siguen abiertas y son independientes de esto.

---

## ✅ Respuesta del back — multicolor, variantes sombra excluidas, anticipo aclarado (2026-08-13)

Todo lo de los dos mensajes anteriores quedó resuelto. Va por partes.

### 1. 🔴 Ramo multicolor — implementado, con un cambio de modelo que conviene que conozcan

No adoptamos literalmente `flores: [{tipoFlorId, cantidad}]` como lo plantearon, porque hoy
"Rosa roja" y "Rosa blanca" siendo dos `TipoFlor` distintos era justo el síntoma del problema, no
la solución — habría obligado a duplicar precio y cantidades válidas por cada color. En vez de
eso, separamos el concepto en dos niveles:

- **`TipoFlor`** ahora es la **especie** (ej. "Rosa eterna") — un solo precio por flor, una sola
  tabla de cantidades válidas, sin importar el color.
- **`ColorFlor`** (catálogo nuevo) es un **color vendible de esa especie** (ej. "Rosa eterna" +
  "Rojo") — tiene su propio stock real y su propia variante interna, pero **no** su propio precio
  ni sus propias cantidades válidas (hereda las de la especie).

Responde directo sus dos preguntas:
- **La validación del círculo es por el total de la especie**, sin importar cómo se reparta entre
  colores — confirmado, era lo que ustedes ya sospechaban.
- **Contra cuál cantidad válida se valida cuando hay varios colores:** contra la de la especie
  (`TipoFlor`), una sola tabla — no hay ambigüedad porque ya no hay una tabla por color.

**Catálogo nuevo:**

| Método | URL | Quién | Campos |
|---|---|---|---|
| CRUD genérico | `/v1/colores-flor` | GET público, resto ADMIN | `id`, `tipoFlor` (anidado por id), `nombre`, `stock`, `activo` |
| `GET /v1/colores-flor/por-tipo-flor/{tipoFlorId}` | — | Público | Lista de colores activos de esa especie — úsenlo para pintar el selector de color después de que el cliente ya fijó la cantidad |

**Flujo actualizado para el cliente:**
1. Elige la especie (`tipoFlorId`) y escribe la cantidad → `POST /v1/flores/validar-cantidad`
   (sin cambios de contrato, sigue siendo por especie).
2. Cantidad ya fijada → `GET /v1/colores-flor/por-tipo-flor/{tipoFlorId}` para mostrar los colores
   disponibles de esa especie.
3. El cliente reparte la cantidad entre uno o varios colores → `POST /v1/flores/calcular-precio`.

**`calcular-precio` — contrato nuevo** (reemplaza `tipoFlorId` + `cantidadFinal` por una lista):
```json
{
  "colores": [
    { "colorFlorId": 1, "cantidad": 6 },
    { "colorFlorId": 2, "cantidad": 6 }
  ],
  "accesorios": [ { "accesorioId": 3 } ],
  "listones": [ { "fraseListonPredefinidaId": 2 } ],
  "lugarEntregaId": 5,
  "recogerEnLocal": false
}
```
Un solo color es simplemente una lista de una entrada — no cambia nada para el caso simple.
**Regla:** todos los colores de la lista deben ser de la misma especie, si no, `400`.

**Response — ya no hay un solo `tipoFlorVarianteId`, hay una línea por color:**
```json
{
  "cantidadFinal": 12,
  "precioBase": 240.0,
  "coloresCalculados": [
    { "colorFlorId": 1, "colorNombre": "Rojo", "cantidad": 6, "precioUnitario": 20.0, "subtotal": 120.0, "varianteId": 101 },
    { "colorFlorId": 2, "colorNombre": "Blanco", "cantidad": 6, "precioUnitario": 20.0, "subtotal": 120.0, "varianteId": 102 }
  ],
  "papelObligatorioAplicado": true,
  "precioPapel": 40.0,
  "papelVarianteId": 55,
  "...": "resto igual (accesoriosCalculados, listonesCalculados, costoEnvio, total, etc.)"
}
```
Al armar `POST /v1/pedidos/savePedido`, en vez de una sola línea "de flores" mandan **una línea
por cada entrada de `coloresCalculados`** (mismo patrón que ya tenían para accesorios: cada una
con su propio `varianteId`, `cantidad`, `precioUnitario`, `subTotal`).

**`RamoArmado` también cambió:** ahora referencia un `colorFlorId` (no `tipoFlorId`) — un ramo
preconfigurado es un color específico, no una especie genérica. Ver punto 5 para el resto de los
cambios de ese catálogo (imagen).

### 2. 🟠 Body de `POST /v1/flores/pedidos/{pedidoId}/detalle` — aquí está

```json
{
  "ramoArmadoId": null,
  "colores": [
    { "colorFlorId": 1, "cantidad": 6 },
    { "colorFlorId": 2, "cantidad": 6 }
  ],
  "fraseListonPredefinidaId": null,
  "fraseListonPersonalizada": "Feliz cumpleaños Mamá",
  "lugarEntregaId": 5,
  "recogerEnLocal": false,
  "telefonoContacto": "3111234567",
  "correoContacto": "cliente@correo.com",
  "comentarioAccesorioNoDisponible": null
}
```
Todos los campos son opcionales excepto `colores` (el mismo desglose que mandaron a
`calcular-precio`, para que el ticket de producción sepa la mezcla exacta). `fraseListonPredefinidaId`
**o** `fraseListonPersonalizada` (nunca ambos). Response: el mismo objeto con `id`, `pedidoId`,
`fraseListonEstado`, etc. — ver el punto 3 para el detalle de esos campos.

**¿Es obligatorio llamarlo siempre?** No. Solo hace falta cuando hay algo que guardar que no está
ya en las líneas de `savePedido`: frase (predefinida o personalizada), zona de entrega/recoger en
local, contacto distinto al del perfil, o el comentario de accesorio no disponible. Si el ramo no
tiene nada de eso (caso raro, pero posible), pueden omitir la llamada — el pedido ya quedó
completo y cobrado correctamente con `savePedido` solo. No hay riesgo de "pedido huérfano": el
pedido es válido y está cobrado exista o no el ticket de producción.

### 3. 🟠 El anticipo — aclarado, y corregimos un bug real que encontraron

Tenían razón: `montoAnticipoSugerido` en `calcular-precio` era un número inventado (50% del total
del ramo completo, no de la frase) que no representaba nada real. **Lo quitamos.** Ahora
`calcular-precio` solo devuelve `tieneListonPendienteValidacion` (booleano) y
`avisoFrasePendiente` (texto para mostrarle al cliente) — sin monto, porque en ese momento
todavía no existe ningún monto que cobrar.

**Cómo es en la práctica — su opción (b), con una corrección:** el cliente paga **todo lo que
tiene precio conocido** (flores, papel, accesorios, listón predefinido, envío) de una sola vez con
`savePedido`, exactamente igual que cualquier pedido normal. La frase personalizada **no se cobra
en ese momento** porque no tiene precio todavía. Cuando el admin la revisa y le asigna un precio
(`PUT /v1/flores/pedidos/detalle/{id}/validar-frase`), **ahí y solo ahí** nace el monto real: el
back crea automáticamente un pedido `APARTADO` nuevo y separado (mismo cliente, una sola línea:
esa frase) y devuelve `pedidoAnticipoId` + `montoAnticipo` (50% de lo que el admin asignó). Ustedes
registran ese pago con `POST /v1/abonos/{pedidoAnticipoId}` (el flujo de abonos que ya tienen
hecho) — típicamente por WhatsApp o cuando el cliente vuelve, como sospechaban.

**En resumen: un solo monto de anticipo real en todo el sistema, y vive únicamente en la respuesta
de `validar-frase`.** El texto que le muestren al cliente al cotizar (`avisoFrasePendiente`) no
debe mencionar ningún número — nosotros ya lo redactamos así:
> *"Esta frase personalizada necesita ser aprobada por el equipo. Una vez asignado su precio, se
> les contactará para pagar el anticipo del 50%. Una vez entregado el ramo no hay reembolsos ni
> cancelaciones."*

### 4. 🟡 Listado global de frases pendientes — nuevo endpoint

```
GET /v1/flores/pedidos/frases-pendientes?pagina=1&size=10   (ADMIN)
```
Response — paginado, con exactamente lo que pidieron:
```json
{
  "data": {
    "pagina": 1, "totalPaginas": 1, "totalRegistros": 2,
    "t": [
      { "detalleId": 8, "pedidoId": 42, "fraseTexto": "Feliz cumpleaños Mamá", "clienteNombre": "Ana López", "fechaPedido": "2026-08-13" }
    ]
  }
}
```
`detalleId` es el que mandan a `validar-frase`. No incluye filtro por estado todavía (solo trae
pendientes) — si en la práctica les hace falta ver aprobadas/rechazadas también, avisen y lo
agregamos.

### 5. 🟢 Umbral del papel — ya es configurable, y ramos armados ya tienen foto

- **Umbral configurable:** `AccesorioRamo` tiene un campo nuevo, `umbralActivacion` (número,
  editable desde la pantalla de accesorios). El accesorio marcado `esPapel` se agrega solo cuando
  `cantidadFinal > umbralActivacion` — el dueño lo cambia él mismo desde ahí, sin pedirnos nada.
  `null` = nunca se agrega solo (queda como opcional siempre). En cuanto el dueño confirme el
  número exacto, alguien con acceso ADMIN lo edita en `/v1/accesorios-ramo` y ya.
- **Foto del ramo armado:** `RamoArmado` tiene `imagenUrl` (string) — el admin sube la imagen por
  fuera (no pasa por micro_imagenes todavía, es un link plano) y lo pega ahí al crear/editar el
  ramo. Si más adelante quieren que la imagen se suba directo desde la pantalla de admin (con
  preview, etc.), avisen y lo enganchamos con micro_imagenes — por ahora es lo mínimo funcional.

### 6. 🔴 Variantes sombra — ya excluidas de todo lo que mencionaron

Agregamos un flag (`Producto.esCatalogoInterno`) y lo aplicamos en:
- Buscador público de la tienda (`/tienda/v1/buscar`, `/tienda/v1/buscar-filtrado`,
  `/tienda/v1/filtros-disponibles`).
- Buscador/filtro de admin (`/tienda/v1/admin/filtrar`, `/v1/productos/admin/filtrar`).
- Listado general de admin (`GET /v1/productos/obtenerProductos`, `findAllNew` de variantes) —
  eran `findAll()` genéricos sin ningún filtro, tuvieron su propia query nueva.
- Buscador del **chatbot** (no lo habían preguntado, lo encontramos igual de expuesto).
- **Reporte de productos más vendidos** — confirmado, sin el flag iban a aparecer ahí compitiendo
  contra productos reales.
- Selectores de **promoción** y **rifa**: no tienen query propia, reusan el buscador general de
  variantes — quedan cubiertos automáticamente con lo de arriba.
- Carga rápida de imágenes: revisamos y no hace falta tocarla — ese flujo crea productos nuevos,
  nunca lista los existentes, así que las variantes sombra no se cruzan con esa pantalla.

**Sobre el stock desincronizado que preguntaron:** ya no aplica el escenario que les preocupaba —
`TipoFlor` ya no tiene variante propia (ver punto 1, ahora es `ColorFlor` el que la tiene). Pero
la duda de fondo seguía siendo válida: si alguien edita el stock de esa variante interna desde la
pantalla normal de variantes, sí se desincroniza con lo que `ColorFlor` muestra en su catálogo
(no hay sincronización en ese sentido). Como ahora estas variantes ya no aparecen en ningún
buscador/selector normal (punto de arriba), la posibilidad de que un admin llegue a editarlas por
accidente desde ahí baja mucho, pero si igual les preocupa que alguien las edite a propósito
sabiendo el id, avisen y le puede poner un candado aparte.

**Confirmado también:** no se venden flores por unidad sueltas desde la tienda general — nunca
fue posible (las variantes sombra ya devolvían 401/nunca aparecían navegables incluso antes de
este fix, porque flores eternas siempre vivió en su propia sección), y ahora con el flag queda
blindado en todos los buscadores además.

### Migración ya corrida

**`migration_flores_eternas_multicolor.sql` ya corrió en QA y producción** (confirmado
2026-08-13) — igual que las dos anteriores. `ColorFlor`, `umbralActivacion`, `imagenUrl` y
`esCatalogoInterno` ya existen en ambas bases. Pueden probar multicolor cuando gusten.

## 🔧 BACK — fix: los errores de `save`/`update` del CRUD genérico ya no ocultan el motivo real (2026-08-13)

Aplica a **todos** los catálogos que usan el CRUD genérico (`/v1/colores-flor`, `/v1/tipos-flor`,
`/v1/cantidades-flor`, `/v1/accesorios-ramo`, `/v1/frases-liston`, `/v1/lugares-entrega`,
`/v1/cinta`, etc.) — cualquier endpoint `POST .../save` o `PUT .../update/{id}`.

**Antes:** si `save`/`update` fallaba por cualquier motivo de negocio (ej. mandar un id que no
existe en una relación anidada, como `tipoFlor.id` inexistente), el back siempre respondía
`500` con el mismo mensaje genérico `"Error al guardar el registro"` / `"Error al actualizar el
registro"`, sin importar la causa real. El campo interno `code` del body además podía marcar
`404` aunque el status HTTP fuera `500` — ese `code` no reflejaba nada del error real, solo que
`data` venía `null`.

**Ahora:** el error real se propaga y el status HTTP + mensaje sí reflejan la causa:
- `404` + mensaje con el detalle (ej. `"Tipo de flor no encontrado: 5"`) cuando el problema es que
  algo referenciado no existe.
- `400` + mensaje con el detalle cuando es un error de validación de negocio.
- `500` solo para errores realmente no esperados (ej. caída de la base de datos).

No cambia el contrato del request ni el de la respuesta exitosa — solo mejora `mensaje`/status en
los casos de error, que antes siempre eran genéricos. Si el front ya mostraba `mensaje` tal cual
en pantalla, ahora el usuario va a ver el motivo real en vez de un texto fijo.

## 🔴 BACK — fix: `save` (alta) fallaba con 500/400 en colores de flor, accesorios, frases y lugares de entrega (2026-08-13)

Aplica al alta (creación, no edición) de los catálogos de "flores eternas" que necesitan poder
venderse como línea de pedido: `POST /v1/colores-flor/save`, `POST /v1/accesorios-ramo/save`,
`POST /v1/frases-liston/save`, `POST /v1/lugares-entrega/save`.

**Qué pasaba:** al dar de alta un registro nuevo, el back intenta crear por dentro un
`Producto`/`Variante` interno (nunca visible en el catálogo público — ver el punto "Variantes
sombra" más arriba) para poder venderlo como una línea real de pedido. Ese insert siempre fallaba
porque no se le asignaba código de barras, y esa columna es obligatoria en la base de datos. El
endpoint respondía con el mensaje real del error de SQL, algo como:
`"could not execute statement [Column 'codigo_barras_id' cannot be null] ..."`.

**Por qué pasó:** todos los demás flujos que crean un `Producto` en el sistema (alta normal, carga
rápida de imágenes, etc.) generan primero un código de barras — real o temporal — antes de
guardar. El único servicio que no lo hacía era el que arma estos productos "sombra"
(`ProductoSombraServiceImpl`, compartido por los 4 endpoints de arriba). No se detectó antes
porque recién se está probando el alta real desde el front de estos catálogos.

**Fix:** si el producto sombra no trae código de barras, ahora se le genera uno temporal
(`SOMBRA-XXXXXXXXXXXX`) antes de guardarlo — mismo mecanismo que ya usaba la carga rápida de
imágenes. No cambia el contrato del request ni el de la respuesta exitosa; el `save` de estos 4
endpoints simplemente ya funciona. La edición (`update`) de registros existentes no estaba
afectada por este bug.
