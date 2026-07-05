# API Contratos — Micro Imágenes + Proyecto Key

> ⚠️ **DOCUMENTO DESACTUALIZADO (detectado 2026-07-04).** La fuente de verdad actual para
> endpoints y cambios de contrato es **`CAMBIOS_FRONT.md`**. Este archivo puede tener
> información obsoleta (ejemplo confirmado: la sección de `/v1/auth/login` ya no coincide con el
> código real) — no usarlo para trabajo nuevo sin verificar contra el código o `CAMBIOS_FRONT.md`.

> Documento de referencia backend: request exacto, response shape, excepciones y llamadas entre micros.
>
> **URLs base**
> - **Proyecto-Key:** `http://localhost:9091/mis-productos`
> - **Micro Imágenes:** `http://localhost:9096/mis-productos`
>
> Última actualización: 2026-06-18

---

## FORMATO DE ERRORES

### Proyecto-Key (9091)
Todos los errores de negocio usan `ResponseGeneric`:
```json
{ "mensaje": "Texto del error en español", "code": 400, "data": null, "lista": null }
```
El HTTP status y el campo `code` siempre coinciden.

### Micro Imágenes (9096)
Los errores usan `MensajeError` (formato diferente, microservicio independiente):
```json
{ "code": 400, "message": "Error message", "fecha": "2026-06-18" }
```
> El campo se llama `message` (inglés), no `mensaje`.

---

## SECCIÓN 1 — MICRO IMÁGENES (puerto 9096)

Context-path: `/mis-productos`

---

### 1.1 Imágenes — `/v1/imagenes`

---

#### `POST /v1/imagenes` — Guardar imágenes en disco

**Request** `multipart/form-data`
```
files: MultipartFile[]   (uno o más archivos de imagen)
```

**Response 200**
```json
[
  {
    "id": 42,
    "nombreImagen": "abc123.jpg",
    "imagen": "<bytes base64>",
    "urlImagen": "http://...",
    "contentType": "image/jpeg"
  }
]
```

**Excepciones**
| HTTP | `message` |
|------|-----------|
| 400 | "Error al procesar archivo: ..." |
| 400 | Cualquier RuntimeException propagada |

**Comportamiento:** Guarda en disco, registra en BD, evicta caches `imagenes`, `imagenOne`, `imagenes-producto`.

---

#### `GET /v1/imagenes?ids=1,2,3` — Obtener múltiples imágenes con bytes

**Request** Query param: `ids` (List\<Long\>, requerido)

**Response 200**
```json
[
  {
    "id": 42,
    "nombreImagen": "abc123.jpg",
    "imagen": "<bytes base64>",
    "urlImagen": "http://...",
    "contentType": "image/jpeg"
  }
]
```

**Excepciones**
- IOException al leer disco → **silenciada**, retorna `[]`

---

#### `GET /v1/imagenes/{id}` — Obtener una imagen con metadata

**Request** Path variable: `id` (Long)

**Response 200** — misma forma que el objeto de la lista de arriba
**Response 204** — si no existe o hay IOException (sin body)

---

#### `GET /v1/imagenes/file/{imagenId}` — Descargar bytes crudos de imagen

**Request** Path variable: `imagenId` (Long)

**Response 200**
```
Content-Type: image/jpeg | image/png | image/gif | image/webp | application/octet-stream
Body: bytes de la imagen (NO JSON)
```
**Response 204** — si imagen no existe o archivo perdido (sin body)

> **Este es el endpoint que el front usa para mostrar imágenes en `<img src="...">`**

---

#### `GET /v1/imagenes/verificar?ids=1,2,3` — Verificar qué IDs existen en disco

**Request** Query param: `ids` (List\<Long\>)

**Response 200**
```json
[1, 3]
```
Solo devuelve los IDs que tienen archivo físico en disco. Si el micro no responde → `[]`.

---

#### `DELETE /v1/imagenes?ids=1,2,3` — Eliminar múltiples imágenes

**Request** Query param: `ids` (List\<Long\>)

**Response 204** (siempre, incluso si alguna falla — errores se loggean y se continúa)

---

#### `DELETE /v1/imagenes/disco?ids=nombre1.jpg,nombre2.jpg` — Eliminar archivos por nombre

**Request** Query param: `ids` (List\<String\>, nombres de archivo)

**Response 204**

**Excepciones**
| HTTP | `message` |
|------|-----------|
| 400 | IOException propagada |

---

### 1.2 Relación Producto-Imagen — `/v1/producto-imagen`

---

#### `POST /v1/producto-imagen` — Crear relación producto↔imagen

**Request** `application/json`
```json
{
  "imagenId": 42,
  "productoId": 10,
  "principal": true
}
```

**Response 200**
```json
{
  "mensaje": "La peticion fue exitosa",
  "code": 200,
  "data": {
    "id": 5,
    "imagenId": 42,
    "productoId": 10,
    "principal": true
  },
  "lista": null
}
```
**Response 404** — si resultado es null

**Excepciones**
| HTTP | `message` |
|------|-----------|
| 400 | Exception genérica |

---

#### `POST /v1/producto-imagen/saveAll` — Crear múltiples relaciones en batch

**Request** `application/json` — array de objetos igual al POST anterior

**Response 200** — `ResponseGeneric` con envelope vacío

---

#### `PUT /v1/producto-imagen` — Actualizar relación

**Request** — igual que POST

**Response 200** — `ResponseGeneric<ProductoImagen>`

**Excepciones**
| HTTP | `message` |
|------|-----------|
| 400 | "El producto que ingresó no existe" |
| 400 | Exception genérica |

---

#### `DELETE /v1/producto-imagen/{id}` — Eliminar relación (y posiblemente la imagen)

**Request** Path variable: `id` (Long)

**Response 200 / 404**

> **Lógica importante:** Si después de eliminar la relación la imagen queda huérfana (ningún otro producto ni variante la usa), también se elimina el archivo del disco. Si la imagen es compartida, solo se elimina la relación.

**Excepciones**
| HTTP | `message` |
|------|-----------|
| 400 | Exception al verificar huérfana o eliminar |

---

#### `GET /v1/producto-imagen/{id}` — Obtener relación por ID

**Response 200** — `ResponseGeneric<ProductoImagen>`
**Response 404** — si no existe

---

#### `GET /v1/producto-imagen/buscarImagenProducto/{productoId}` — Imagen principal del producto

**Request** Path variable: `productoId` (Integer)

**Response 200**
```json
{
  "id": 42,
  "nombreImagen": "abc123.jpg",
  "imagen": "<bytes base64>",
  "urlImagen": "http://...",
  "contentType": "image/jpeg"
}
```
Si no hay imagen o falla → `{}` (Imagen vacía, sin lanzar excepción)

> **Cacheable:** key = `productoId`. Si hay error, el error se cachea también.

---

#### `GET /v1/producto-imagen/listar/{productoId}?pagina=1&size=8` — Listar imágenes paginadas

**Request**
- Path: `productoId` (Integer)
- Query: `pagina` (int, default=1), `size` (int, default=8)

**Response 200**
```json
{
  "productoId": 10,
  "listaImagenes": [
    {
      "id": "42",
      "extension": "jpg",
      "nombreImagen": "abc123.jpg",
      "urlImagen": "http://localhost:9096/mis-productos/v1/imagenes/file/42",
      "principal": true
    }
  ],
  "pagina": 1,
  "totalPaginas": 3,
  "totalImagenes": 20
}
```
> `id` viene como **String**, no Long.

**Cacheable:** key = `"{productoId}:p{pagina}:s{size}"`
**Fallback:** si verificación en disco falla, usa BD local sin validar existencia física.

---

#### `PUT /v1/producto-imagen/{id}/principal` — Marcar imagen como principal

**Request** Path variable: `id` (Long — ID de la imagen, no de la relación)

**Response 200**
```json
{ "mensaje": "Imagen marcada como principal correctamente" }
```

**Excepciones**
| HTTP | `message` |
|------|-----------|
| 400 | Exception genérica |

---

#### `POST /v1/producto-imagen/admin/limpiar-duplicados` — Limpiar relaciones duplicadas en BD

**Response 200**
```json
{
  "productoImagenEliminadas": 5,
  "varianteImagenEliminadas": 3
}
```

---

### 1.3 Caché — `/v1/cache`

#### `DELETE /v1/cache/limpiar` — Vaciar toda la caché

**Response 200**
```json
{ "mensaje": "Toda la cache fue limpiada correctamente" }
```

---

## SECCIÓN 2 — CÓMO PROYECTO-KEY LLAMA A MICRO IMÁGENES

**Configuración por ambiente:**
- `dev`: `api.imagenes: http://localhost:9096/mis-productos`
- `qa/prod`: `api.imagenes: ${ENDPOINT_IMAGENES}` (variable de entorno)

**JWT automático:** Todas las llamadas incluyen `Authorization: Bearer {token}` vía filtro en `WebClientConfig`.

**Patrón de errores en llamadas salientes:**

| Tipo de llamada | Si falla o retorna vacío |
|---|---|
| `GET /v1/imagenes?ids=...` | Retorna `[]` silenciosamente (el producto se muestra sin imagen) |
| `GET /v1/imagenes/verificar?ids=...` | Retorna `[]` → fallback a BD local |
| `POST /v1/imagenes` (subir) | Loggea error — el producto/variante **se guarda igual** (best-effort) |
| `GET buscarImagenProducto/{id}` | Retorna objeto vacío `{}` |
| Eliminaciones | Via RabbitMQ (asíncrono) — no bloquea la respuesta HTTP |

---

### Flujo: Guardar producto con imágenes

```
Front → POST /v1/productos/save (con archivos)
  ↓
ProductosServiceImpl.relacionProductoImagen()
  ↓ (síncrono, timeout 30s)
  POST http://9096/v1/imagenes          → obtiene List<ImagenDto> con IDs
  ↓ (si subida OK → async via Rabbit)
  PUBLISH queue.guardar.imagenes        → micro crea relaciones en su BD
  ↓ (si falla subida → solo loggea)
Producto guardado en BD local siempre
```

### Flujo: Guardar variante con imágenes

```
Front → POST /v1/variantes/guardarConImagenes
  ↓
VarianteServiceImpl
  ↓ (síncrono, timeout 30s)
  POST http://9096/v1/imagenes          → IDs de imágenes
  ↓ (async via Rabbit)
  PUBLISH queue.guardar.imagenes        → relaciones variante_imagen
Variante guardada siempre
```

### Flujo: Listar productos (construcción de URLs)

```
GET /v1/productos/obtenerProductos
  ↓
Proyecto-Key NO llama al micro para obtener imágenes
Solo construye URLs que apuntan al micro:
  urlImagen = "${api.imagenes}/v1/imagenes/file/{imagenId}"
El FRONT es quien hace GET a esas URLs directamente
```

### Flujo: Eliminar producto/variante (imágenes)

```
DELETE /v1/productos/deleteBy/{id}
  ↓
Lee nombres de archivos de BD local
  ↓ (async via Rabbit)
  PUBLISH queue.eliminar.imagenes.disco → micro borra archivos del disco
```

### Flujo: Verificar imágenes en diagnóstico

```
GET /v1/variantes/admin/diagnostico-imagenes/{varianteId}
  ↓
VarianteServiceImpl.diagnosticarImagenesVariante()
  ↓ (síncrono, timeout 5s)
  GET http://9096/v1/imagenes?ids=...  → List<Imagen> (con bytes)
Identifica cuáles IDs de BD tienen archivo físico en el micro
```

---

### Endpoints de proyecto-key que actúan como proxy hacia el micro

Estos endpoints de proyecto-key (9091) internamente llaman al micro de imágenes (9096):

| Endpoint 9091 | Llama a 9096 | Comportamiento si falla |
|---|---|---|
| `GET /v1/imagen/{productoId}` | `GET /v1/producto-imagen/buscarImagenProducto/{id}` | Retorna imagen vacía |
| `GET /v1/imagen/file/{imagenId}` | `GET /v1/imagenes?ids={id}` → extrae bytes | 204 si falla |
| `DELETE /v1/imagen/{idImagen}` | `DELETE /v1/imagenes` via Rabbit | Async, no bloquea |
| `DELETE /v1/imagen/{productoId}/imagenes` | `DELETE /v1/imagenes` via Rabbit | Async, no bloquea |

---

## SECCIÓN 3 — PROYECTO-KEY: ENDPOINTS PRINCIPALES (9091)

Context-path: `/mis-productos` — prefijo: `/v1/`

### Formato de respuesta exitosa

```json
{ "mensaje": "La peticion fue exitosa", "code": 200, "data": { ... }, "lista": null }
```

---

### AUTH — `/v1/auth`

#### `POST /v1/auth/login`

**Request**
```json
{ "username": "usuario", "password": "clave" }
```

**Response 200**
```json
{
  "mensaje": "La peticion fue exitosa",
  "code": 200,
  "data": {
    "accessToken": "eyJ...",
    "role": "ROLE_ADMIN"
  },
  "lista": null
}
```
Access token: 15 min · Refresh token: 7 días (en cookie HttpOnly)

**Excepciones**
| HTTP | `mensaje` |
|------|-----------|
| 400 | "Usuario o contraseña incorrectos" |
| 400 | "Su cuenta ha sido bloqueada temporalmente por múltiples intentos fallidos" |

---

#### `POST /v1/auth/refresh`

**Request** — sin body (lee refresh token de cookie HttpOnly)

**Response 200** — igual que login (nuevo accessToken)

**Excepciones**
| HTTP | `mensaje` |
|------|-----------|
| 400 | "Refresh token inválido o expirado" |

---

#### `POST /v1/auth/logout`

**Response 200** — limpia cookie

---

### CONCURSANTE — `/v1/concursante`

#### `POST /v1/concursante/registrar[?forzar=false]`

**Request**
```json
{
  "nombre": "Abel",
  "apellidoPaterno": "Tiburcio",
  "telefono": "7223475214",
  "palabraClave": "VENTAS",
  "ordenDesde": 1,
  "configurarRifa": { "id": 39 }
}
```

**Response 200** — `ResponseGeneric<Concursante>` con el concursante guardado

**Excepciones**
| HTTP | `mensaje` |
|------|-----------|
| 400 | "El nombre es requerido" |
| 400 | "Debe indicar la configuración de rifa" |
| 400 | "Configuración de rifa no encontrada" |
| 400 | "Esta rifa ya fue sorteada o está inactiva" |
| 400 | "El plazo de registro cerró el 2026-06-18T12:13" |

> `?forzar=true` omite la validación de fecha límite.

---

#### `POST /v1/concursante/importarDePedidos`

**Request**
```json
{
  "configurarRifaId": 39,
  "palabraClave": "VENTAS",
  "ordenDesde": 1,
  "mes": "2026-06",
  "clientes": [
    { "clientePedidoId": 5, "nombre": "Ana", "apellidoPaterno": "Pérez", "telefono": "...", "sinRegistro": false }
  ]
}
```

**Response 200**
```json
{
  "importados": [ /* Concursante[] */ ],
  "omitidosYaRegistrados": [ { "clientePedidoId": 5, "nombre": "Ana" } ],
  "omitidosSinNombre": []
}
```

**Excepciones**
| HTTP | `mensaje` |
|------|-----------|
| 400 | "Configuración de rifa no encontrada" |
| 400 | "Esta rifa no está activa" |

---

#### `DELETE /v1/concursante/{id}`

**Excepciones**
| HTTP | `mensaje` |
|------|-----------|
| 400 | "Concursante no encontrado" |
| 400 | "No se puede eliminar: el concursante ya participó en un sorteo" |

---

#### `PUT /v1/concursante/{id}`

**Request** — campos opcionales:
```json
{ "nombre": "...", "apellidoPaterno": "...", "telefono": "...", "palabraClave": "...", "ordenDesde": 1 }
```

**Excepciones**
| HTTP | `mensaje` |
|------|-----------|
| 400 | "Concursante no encontrado" |

---

#### `GET /v1/concursante/porRifa/{configurarRifaId}` — sin errores (lista vacía si no hay)
#### `GET /v1/concursante/elegibles/{configurarRifaId}` — sin errores
#### `GET /v1/concursante/clientesPorMes?mes=2026-06` — sin errores

---

### CONFIGURAR RIFA — `/v1/configurarRifa`

#### `POST /v1/configurarRifa/save`

**Request**
```json
{
  "fechaHoraLimite": "2026-06-19T00:00:00",
  "activa": true,
  "tipo": "MENSUAL",
  "mesReferencia": "2026-06",
  "esPrueba": false
}
```
> `fechaHoraLimite` en formato 24h. Medianoche = `T00:00:00` del día siguiente.

**Response 200** — `ResponseGeneric<ConfigurarRifa>`

---

#### `PUT /v1/configurarRifa/{id}` — Editar configuración existente

**Request** (todos opcionales, solo enviar lo que cambia):
```json
{
  "fechaHoraLimite": "2026-06-19T00:00:00",
  "tipo": "DIARIA",
  "mesReferencia": "2026-06"
}
```

**Excepciones**
| HTTP | `mensaje` |
|------|-----------|
| 400 | "Configuración de rifa no encontrada" |
| 400 | "No se puede cambiar el tipo de rifa porque ya tiene variantes configuradas. Elimina las variantes primero." |

---

#### `PUT /v1/configurarRifa/{id}/esPrueba`

**Request**
```json
{ "esPrueba": false }
```
> Al pasar de `true` → `false`: limpia giros de demo y reactiva la rifa.

**Excepciones**
| HTTP | `mensaje` |
|------|-----------|
| 400 | "Configuración de rifa no encontrada" |

---

#### `GET /v1/configurarRifa/activas` — sin errores
#### `GET /v1/configurarRifa/activas/hoy` — sin errores
#### `GET /v1/configurarRifa/buscar?desde=&hasta=&tipo=&mesReferencia=` — sin errores (lista vacía si no hay)

---

### GANADOR RIFA — `/v1/ganadorRifa`

#### `POST /v1/ganadorRifa/sortear/{configurarRifaId}`

**Response 200**
```json
{
  "concursante": { "id": 5, "nombre": "Abel", "apellidoPaterno": "...", "boletos": 3 },
  "varianteActual": { "id": 2, "palabraClave": "VENTAS", "giroGanador": 3, "permitirNuevos": false },
  "descartado": false,
  "rifaTerminada": false
}
```

**Excepciones**
| HTTP | `mensaje` |
|------|-----------|
| 400 | "Configuración de rifa no encontrada" |
| 400 | "Esta rifa ya fue completada o está inactiva" |
| 400 | "La rifa no tiene variantes configuradas" |
| 400 | "Todas las variantes ya fueron sorteadas" |
| 400 | "No hay concursantes elegibles para la variante con palabraClave='VENTAS'" |

---

#### `POST /v1/ganadorRifa/continuarVariante/{configurarRifaId}?modo=RESTANTES`

Modos: `RESTANTES` (los no descartados pasan) · `CERO` (todos reinician) · `NUEVOS` (solo nuevos)

**Response 200** — `ResponseGeneric<SorteoEstadoDto>` (mismo shape que `/estado`)

**Excepciones**
| HTTP | `mensaje` |
|------|-----------|
| 400 | "Rifa no encontrada" |
| 400 | "No hay siguiente variante" |
| 400 | "Modo inválido: X. Usar RESTANTES, CERO o NUEVOS" |

---

#### `GET /v1/ganadorRifa/estado/{configurarRifaId}`

**Response 200**
```json
{
  "configurarRifa": { "id": 39, "fechaHoraLimite": "2026-06-19T00:00:00", "activa": true, "esPrueba": false, "tipo": "MENSUAL", "mesReferencia": "2026-06" },
  "totalConcursantes": 12,
  "totalVariantes": 2,
  "varianteNumeroActual": 1,
  "varianteActual": { "id": 5, "palabraClave": "VENTAS", "giroGanador": 3, "permitirNuevos": false },
  "giroActual": 1,
  "giroGanador": 3,
  "elegibles": [ /* Concursante[] */ ],
  "descartados": [ /* Concursante[] */ ],
  "historial": [],
  "rifaTerminada": false
}
```

**Excepciones**
| HTTP | `mensaje` |
|------|-----------|
| 400 | "Rifa no encontrada" |

---

#### `POST /v1/ganadorRifa/reiniciar/{configurarRifaId}?completo=false`

**Response 200**
- `completo=false`: `"Rifa reiniciada (concursantes conservados)"`
- `completo=true`: `"Rifa reiniciada completamente (concursantes eliminados)"`

**Excepciones**
| HTTP | `mensaje` |
|------|-----------|
| 400 | "Rifa no encontrada" |

---

### NOTAS IMPORTANTES

#### RabbitMQ — operaciones asíncronas de imágenes
Las eliminaciones de archivos van por Rabbit (no HTTP directo). Si Rabbit está caído, las imágenes físicas no se eliminan pero la operación HTTP responde OK. Queues:
- `queue.eliminar.imagenes` — eliminar por ID
- `queue.eliminar.imagenes.disco` — eliminar por nombre de archivo
- `queue.guardar.imagenes` — crear relaciones producto/variante↔imagen

#### Caché (proyecto-key)
- `@CacheEvict` al guardar/eliminar imágenes de producto y variante
- Los endpoints de listado de imágenes están cacheados en el micro de imágenes
- Limpiar caché del micro: `DELETE /v1/cache/limpiar` (9096)
- Limpiar caché del proyecto-key: `DELETE /v1/admin/cache` (9091)
