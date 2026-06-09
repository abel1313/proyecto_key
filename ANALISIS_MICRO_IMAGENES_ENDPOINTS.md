# Análisis: Micro-Servicios de Imágenes — Endpoints y Capacidades

## 1. URI Base del Micro

| Ambiente | URI base |
|----------|----------|
| Dev (local) | `http://localhost:9096/mis-productos` |
| QA / Docker | `http://<host>:9096/mis-productos` — el host externo publicado es `https://backend-imagenes.novedades-jade.com.mx/mis-productos` |

- **Puerto:** `9096`
- **Context-path:** `/mis-productos`
- **Fuente:** `application.yml` (base) + `application-qa.yml` + `application-dev.yml`, todos coinciden en `server.port: 9096` y `server.servlet.context-path: /mis-productos`

---

## 2. Endpoints por Controlador

> La ruta completa de cada endpoint es: `<context-path> + <@RequestMapping de clase> + <@Mapping de método>`
> Ejemplo: `/mis-productos` + `/imagenes` + `/file/{id}` = `/mis-productos/imagenes/file/{id}`

---

### 2.1 `ImagenController` — `@RequestMapping("/imagenes")`

Ruta base de clase: `/mis-productos/imagenes`

#### POST `/mis-productos/imagenes`
- **Qué hace:** Recibe uno o más archivos, los guarda en disco local y registra el metadato en BD (`imagenes_copy`). Devuelve los objetos `Imagen` con el ID asignado y la URL (nombre de archivo en disco).
- **Parámetros:** `multipart/form-data` — campo `files` (array de `MultipartFile`)
- **Respuesta:** `200 OK` → `List<Imagen>` (JSON). Cada objeto: `{ id, nombreImagen, urlImagen, contentType }`
- **Autenticación:** **Requiere JWT** (no está en la lista de rutas públicas)

#### GET `/mis-productos/imagenes`
- **Qué hace:** Devuelve la metadata de una lista de imágenes por sus IDs. Lee el archivo del disco y lo incluye como `byte[]` en el campo `imagen`.
- **Parámetros:** `?ids=1,2,3` (query param, `List<Long>`)
- **Respuesta:** `200 OK` → `List<Imagen>` — para cada ID devuelve `{ id, nombreImagen, imagen (bytes), urlImagen, contentType }`
- **Autenticación:** **Pública** (`permitAll` en SecurityConfig para `GET /imagenes`)

#### GET `/mis-productos/imagenes/{id}`
- **Qué hace:** Devuelve la metadata + bytes de una imagen única por su ID.
- **Parámetros:** `{id}` path param (`Long`)
- **Respuesta:** `200 OK` → `Imagen` (JSON con campo `imagen` como array de bytes)
- **Autenticación:** **Pública** (`GET /imagenes/**`)

#### GET `/mis-productos/imagenes/file/{imagenId}`
- **Qué hace:** Devuelve los bytes crudos de la imagen con el `Content-Type` correcto (image/jpeg, image/png, etc.). Es el endpoint que usa el front para renderizar imágenes directamente (`<img src="...">`).
- **Parámetros:** `{imagenId}` path param (`Long`)
- **Respuesta:** `200 OK` → `byte[]` con `Content-Type` resuelto desde la extensión/contentType guardado. Si no existe o hay error: `204 No Content`
- **Autenticación:** **Pública** (`GET /imagenes/**`)

#### GET `/mis-productos/imagenes/verificar`
- **Qué hace:** Verifica cuáles de los IDs dados tienen su archivo físico presente en disco. Filtra solo los IDs cuyo archivo realmente existe en `ruta_imagenes`.
- **Parámetros:** `?ids=1,2,3` (query param, `List<Long>`)
- **Respuesta:** `200 OK` → `List<Long>` — solo los IDs que tienen archivo en disco
- **Autenticación:** **Pública** (`GET /imagenes/**`)

#### DELETE `/mis-productos/imagenes`
- **Qué hace:** Elimina una o más imágenes por sus IDs: borra el archivo de disco y el registro en `imagenes_copy`. Si un ID falla, continúa con los demás (log de warning).
- **Parámetros:** `?ids=1,2,3` (query param, `List<Long>`)
- **Respuesta:** `204 No Content`
- **Autenticación:** **Requiere JWT**

#### DELETE `/mis-productos/imagenes/disco`
- **Qué hace:** Elimina archivos del disco por nombre de archivo (no por ID). Útil para limpieza directa.
- **Parámetros:** `?ids=nombre1.jpg,nombre2.png` (query param, `List<String>` — nombres de archivo)
- **Respuesta:** `204 No Content`
- **Autenticación:** **Requiere JWT**

---

### 2.2 `ProductoImagenController` — `@RequestMapping("/producto-imagen")`

Ruta base de clase: `/mis-productos/producto-imagen`

Este controlador gestiona la tabla de relación `producto_imagen_copy` (muchos-a-muchos entre productos e imágenes).

#### POST `/mis-productos/producto-imagen`
- **Qué hace:** Crea una relación producto-imagen única.
- **Parámetros:** Body JSON — `{ id, imagenId, productoId, principal }`
- **Respuesta:** `200 OK` → `ResponseGeneric<ProductoImagen>` — `{ codigo: 200, mensaje: "...", response: { id, imagenId, productoId, principal } }`
- **Autenticación:** **Requiere JWT**

#### POST `/mis-productos/producto-imagen/saveAll`
- **Qué hace:** Crea múltiples relaciones producto-imagen en lote.
- **Parámetros:** Body JSON — `List<{ id, imagenId, productoId, principal }>`
- **Respuesta:** `200 OK` → `ResponseGeneric<ProductoImagen>` (objeto vacío de confirmación)
- **Autenticación:** **Requiere JWT**

#### PUT `/mis-productos/producto-imagen`
- **Qué hace:** Actualiza una relación producto-imagen existente.
- **Parámetros:** Body JSON — `{ id, imagenId, productoId, principal }`
- **Respuesta:** `200 OK` → `ResponseGeneric<ProductoImagen>`
- **Autenticación:** **Requiere JWT**

#### DELETE `/mis-productos/producto-imagen/{id}`
- **Qué hace:** Elimina la relación producto-imagen con el ID dado. Si la imagen queda huérfana (no referenciada por ningún producto ni variante), también elimina el archivo físico y el registro en `imagenes_copy`.
- **Parámetros:** `{id}` path param (`Long`) — ID de la relación en `producto_imagen_copy`
- **Respuesta:** `200 OK` → `ResponseGeneric<ProductoImagen>`
- **Autenticación:** **Requiere JWT**

#### GET `/mis-productos/producto-imagen/{id}`
- **Qué hace:** Busca una relación producto-imagen por ID. Nota: la implementación llama al servicio pero devuelve un `ProductoImagen` vacío (el resultado real del `findById` no se retorna — posible bug).
- **Parámetros:** `{id}` path param (`Integer`)
- **Respuesta:** `200 OK` → `ResponseGeneric<ProductoImagen>`
- **Autenticación:** **Pública** (`GET /producto-imagen/**`)

#### GET `/mis-productos/producto-imagen/buscarImagenProducto/{id}`
- **Qué hace:** Busca la imagen principal de un producto. Itera los IDs de imagen del producto buscando el primero que tenga archivo en disco (hasta 5 intentos). Devuelve los bytes del archivo.
- **Parámetros:** `{id}` path param (`Integer`) — ID del producto
- **Respuesta:** `200 OK` → `Imagen` (JSON con campo `imagen` como array de bytes). Si no encuentra: `Imagen` vacío.
- **Autenticación:** **Pública** (`GET /producto-imagen/**`)

#### GET `/mis-productos/producto-imagen/listar/{productoId}`
- **Qué hace:** Lista paginada de todas las imágenes de un producto. Verifica existencia en disco de cada imagen antes de incluirla. Construye la URL de cada imagen usando el endpoint configurado en `api.imagenes`.
- **Parámetros:**
  - `{productoId}` path param (`Integer`)
  - `?pagina=1` (query param, default `1`)
  - `?size=8` (query param, default `8`)
- **Respuesta:** `200 OK` →
  ```json
  {
    "productoId": 123,
    "listaImagenes": [
      { "id": "456", "extension": "jpg", "nombreImagen": "foto.jpg", "urlImagen": "https://backend-imagenes.../mis-productos/imagenes/file/456", "principal": true }
    ],
    "pagina": 1,
    "totalPaginas": 2,
    "totalImagenes": 10
  }
  ```
- **Autenticación:** **Pública** (`GET /producto-imagen/**`)

#### PUT `/mis-productos/producto-imagen/{id}/principal`
- **Qué hace:** Marca la imagen con ese `id` de relación como la imagen principal del producto. Pone `principal=false` en todas las demás imágenes del mismo producto.
- **Parámetros:** `{id}` path param (`Integer`) — ID de la relación en `producto_imagen_copy`
- **Respuesta:** `200 OK` → `{ "mensaje": "Imagen marcada como principal correctamente" }`
- **Autenticación:** **Requiere JWT**

---

### 2.3 `CacheController` — `@RequestMapping("/cache")`

Ruta base de clase: `/mis-productos/cache`

#### DELETE `/mis-productos/cache/limpiar`
- **Qué hace:** Limpia toda la caché Redis (todos los cache names registrados: `imagenes`, `imagenOne`, `imagenes-producto`).
- **Parámetros:** Ninguno
- **Respuesta:** `200 OK` → `{ "mensaje": "Toda la cache fue limpiada correctamente" }`
- **Autenticación:** **Requiere JWT**

---

## 3. Modelo de Datos (Entidades JPA)

### Tabla: `imagenes_copy`
Entidad: `ImagenEntity`

| Columna | Tipo | Descripción |
|---------|------|-------------|
| `id` | `Long` (PK) | ID generado por UUID (`getMostSignificantBits & MAX_VALUE`) en la capa de servicio |
| `base_64` | `String` | Nombre de archivo en disco (ej: `uuid_foto.jpg`) — **campo mal nombrado, no es base64** |
| `extension` | `String` | Content-Type completo (ej: `image/jpeg`) o solo extensión |
| `nombre_imagen` | `String` | Nombre original del archivo subido |

### Tabla: `producto_imagen_copy`
Entidad: `ProductoImagenEntity` (extiende `BaseIdEntity` con `@Id` autoincremental)

| Columna | Tipo | Descripción |
|---------|------|-------------|
| `id` | `Integer` (PK) | Autoincremental |
| `imagen_id` | `Long` (FK → `imagenes_copy.id`) | Referencia a la imagen |
| `producto_id` | `Integer` | ID del producto en el micro de productos (referencia externa) |
| `principal` | `Boolean` | Si esta imagen es la imagen principal del producto |

### Tablas referenciadas (pero no manejadas por este micro)
- `variante_imagen` — tabla del micro de productos. El micro de imágenes solo cuenta referencias en ella para verificar si una imagen es huérfana.

---

## 4. Capacidades Actuales del Micro

### Lo que SÍ puede hacer

1. **Almacenamiento de archivos en disco local** — guarda archivos con nombre UUID único en la ruta configurada (`/app/imagenes` en QA/Docker, `D:\Imagenes` en dev).

2. **Registro de metadatos en BD** — mantiene la tabla `imagenes_copy` con ID, nombre de archivo en disco, extensión/content-type y nombre original.

3. **Servir imágenes como bytes crudos** — endpoint `GET /imagenes/file/{id}` devuelve bytes con Content-Type correcto para renderizado directo en `<img src>`.

4. **Listado de imágenes con metadata** — por lista de IDs o por ID único, devolviendo bytes en el campo `imagen`.

5. **Verificación de existencia en disco** — puede confirmar qué IDs tienen su archivo físico presente (`GET /imagenes/verificar`).

6. **Gestión de la relación producto-imagen** — tabla `producto_imagen_copy`: crear, actualizar, eliminar relaciones, marcar imagen principal, listar con paginación.

7. **Limpieza inteligente al borrar** — al eliminar una relación producto-imagen verifica si la imagen es huérfana (no referenciada en `variante_imagen` ni en `producto_imagen_copy`) antes de borrar el archivo físico.

8. **Caché Redis** — cachea resultados con nombres `imagenes`, `imagenOne`, `imagenes-producto`. Endpoint para limpiarla.

9. **Consumo de mensajes RabbitMQ** — escucha la cola `queue.guardar.imagenes` (exchange `exchange.imagenes`, routing key `guardar.imagen`) para crear relaciones producto-imagen de forma asíncrona. Tiene DLQ (`dlq.guardar.imagenes`) para mensajes fallidos.

10. **CORS configurado** — permite orígenes específicos de producción y `localhost:4200` para desarrollo.

11. **JWT estativo** — verifica tokens JWT en endpoints protegidos. El secret se lee de la variable de entorno `TOKEN_JWT` (mismo secret que el micro principal).

### Lo que NO puede hacer (limitaciones actuales)

1. **No maneja variantes** — solo tiene la tabla `producto_imagen_copy`. La tabla `variante_imagen` del micro de productos no tiene endpoints aquí; solo se consulta para detectar huérfanas.

2. **No tiene endpoint para listar imágenes de una variante** — si se quiere migrar la lógica de `GET /variantes/imagenes/{varianteId}` aquí, habría que crear nuevas tablas y endpoints.

3. **No almacena en S3/nube** — a pesar de que la clase se llama `ClienteS3`, el almacenamiento es 100% en disco local. No hay integración real con AWS S3.

4. **No tiene endpoint de diagnóstico** — no hay equivalente al `GET /productos/admin/diagnostico-imagenes/{productoId}` del micro principal.

5. **No gestiona productos directamente** — solo mantiene la relación imagen↔producto. No tiene datos del producto (nombre, precio, etc.).

6. **Paginación en `listarImagenesProducto` es en memoria** — carga todas las imágenes del producto, filtra las existentes en disco, y luego pagina en Java. No es paginación en BD.

7. **`GET /producto-imagen/{id}` devuelve objeto vacío** — el resultado de `findById` no se retorna al cliente (posible bug: el servicio se llama pero el resultado se descarta).

8. **`DELETE /producto-imagen/{id}` usa `imagenId` como clave** — el repositorio tiene `deleteByImageneId(Long imagenId)`, lo que significa que borra por `imagen_id` en la tabla, no por el `id` de la relación. Si un producto tiene múltiples relaciones apuntando a la misma imagen, todas se borrarían.

9. **No tiene endpoint para listar todas las imágenes sin filtrar por producto** — no hay un `GET /imagenes/listar` paginado general.

10. **Sin soporte para actualización de archivo** — el método `update` en `ClienteS3` elimina el archivo y vuelve a leerlo (sin reemplazarlo realmente). No hay un endpoint funcional de actualización de bytes.
