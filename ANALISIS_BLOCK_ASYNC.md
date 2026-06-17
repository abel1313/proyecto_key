# Análisis de llamadas bloqueantes `.block()` — proyecto_key_new

> Fecha: 2026-05-23
> Rama analizada: `qa`
> Objetivo: catalogar todos los usos de `.block()` en WebClient/Reactor, relacionarlos con
> los endpoints REST del backend que los accionan, verificar si están documentados en el front
> y dar una valoración de si vale la pena migrar a programación reactiva.

---

## Resumen ejecutivo

Se encontraron **2 archivos** con llamadas `.block()` activas y **2 archivos** sin `.block()`
(ya reactivos o migrados).

| Archivo | Llamadas `.block()` | Criticidad |
|---|---|---|
| `ChatbotService.java` | ~~1~~ **0 — MIGRADO** ✅ (ver PROGRESO_SESION.md §7) | — |
| `ImageneClienteDisco.java` | 6 | Alta — operaciones de imagen de variantes |
| `ImagenProductoClienteMicro.java` | 6 | Media — operaciones de imagen de productos |
| `DopoMexService.java` | 0 | Ya reactivo — no requiere cambios |

---

## 1. ChatbotService.java

**Ruta:** `src/main/java/com/ventas/key/mis/productos/chatbot/ChatbotService.java`

### Llamada `.block()`

| Línea | Método | URL construida |
|---|---|---|
| 90 | `chat(ChatbotRequest)` | `POST https://api.openai.com/v1/chat/completions` |

**Contexto:** el servicio construye el WebClient con `baseUrl("https://api.openai.com/v1")` (línea 32)
y realiza `.uri("/chat/completions")`. El `.block()` en línea 90 espera la respuesta de OpenAI
de forma sincrónica.

### Endpoint REST del backend que lo expone

`POST /chatbot/mensaje` — `ChatbotController.enviarMensaje()`

### Presencia en documentación del front

**Documentado.** Aparece en `ENDPOINTS.md` sección CHATBOT:
```
POST /chatbot/mensaje — ChatbotComponent.enviar()
```

### Valoración de migración a reactivo

**Alta prioridad.** Razones:
- Cada mensaje de usuario bloquea un hilo del servidor esperando la respuesta de OpenAI
  (latencia típica: 1–5 segundos según contexto y modelo).
- El chatbot es público (no requiere auth), por lo que puede recibir carga simultánea
  de múltiples usuarios desconocidos.
- La migración es directa: el controlador ya puede recibir `Mono<ResponseEntity>` y
  `ChatbotService.chat()` puede retornar `Mono<String>` sin `.block()`.
- **Riesgo si no se migra:** bajo carga simultánea, el pool de hilos del servidor puede
  agotarse esperando respuestas de OpenAI, degradando todo el servicio.

---

## 2. ImageneClienteDisco.java

**Ruta:** `src/main/java/com/ventas/key/mis/productos/hexagonal/infraestructura/ImageneClienteDisco.java`
**Implementa:** `ImagenPort`
**URL base:** `${api.imagenes}` (apunta al microservicio `micro_imagenes`, puerto 9096)

### Llamadas `.block()`

| Línea | Método | HTTP | URL en micro_imagenes |
|---|---|---|---|
| 65 | `save(MultiValueMap)` | POST | `${api.imagenes}/imagenes` |
| 86 | `getAll(List<Long>)` | GET | `${api.imagenes}/imagenes?ids=...` |
| 100 | `delete(List<Long>)` | DELETE | `${api.imagenes}/imagenes?ids=...` |
| 115 | `verificarExistentes(List<Long>)` | GET | `${api.imagenes}/imagenes/verificar?ids=...` |
| 129 | `deleteInagenesDisco(List<String>)` | DELETE | `${api.imagenes}/imagenes/disco?ids=...` |
| 149 | `getOne(Long)` | GET | `${api.imagenes}/imagenes?ids={id}` |

### Endpoints REST del backend que los accionan

Cada método de `ImagenPort` es invocado desde varios puntos. La siguiente tabla mapea
los `.block()` hacia los endpoints REST que los desencadenan:

| Método `.block()` | Llamado desde | Endpoint REST expuesto |
|---|---|---|
| `save()` (línea 65) | `VarianteServiceImpl` → `guardarConImagenes`, `inicializarDesdeProducto` | `POST /variantes/guardarConImagenes`, `POST /variantes/inicializarDesdeProducto` |
| `save()` (línea 65) | `ProductosServiceImpl` → `relacionProductoImagen()` → llamado por `save` y `update` de producto | `POST /productos/save`, `PUT /productos/update` |
| `getAll()` (línea 86) | `ImagenServiceImpl.findImagenPrincipalPorProductoIdsV2()` | `GET /imagen/v2/{productoId}/detalle` |
| `getAll()` (línea 86) | `VarianteServiceImpl` → `buscarVariantes()` | `GET /variantes/buscar` |
| `delete()` (línea 100) | `ImagenServiceImpl.deleteByIdV2()` | `DELETE /imagen/v2/{imagenId}` |
| `delete()` (línea 100) | `VarianteServiceImpl` → eliminar imágenes de variante | `DELETE /variantes/v2/{varianteId}/imagenes`, `DELETE /variantes/v2/imagenes` |
| `delete()` (línea 100) | `ProductoImagenServiceImpl` → eliminar imágenes de producto | `DELETE /imagen/v2/{productoId}/imagenes`, `DELETE /imagen/v2/producto` |
| `verificarExistentes()` (línea 115) | `VarianteServiceImpl` → `buscarVariantes()` y `obtenerImagenesPorVarianteV2()` | `GET /variantes/buscar`, `GET /variantes/v2/imagenes/{varianteId}` |
| `deleteInagenesDisco()` (línea 129) | `ProductosServiceImpl` → eliminación de producto | `DELETE /productos/deleteBy/{id}` |
| `getOne()` (línea 149) | `ImageneController.getImagenByImagenIdV2()` | `GET /imagen/v2/file/{imagenId}` |

### Presencia en documentación del front

| Endpoint | En ENDPOINTS.md / CAMBIOS_FRONTEND.md |
|---|---|
| `POST /variantes/guardarConImagenes` | Documentado — `POST /variantes/guardarConImagenes` |
| `POST /variantes/inicializarDesdeProducto` | No aparece explícitamente en ENDPOINTS.md (endpoint interno/admin) |
| `POST /productos/save` | Documentado — `POST /productos/save` |
| `PUT /productos/update` | Documentado (mencionado en flujo de update) |
| `GET /imagen/v2/{productoId}/detalle` | Documentado — `GET /imagen/v2/{id}/detalle` |
| `GET /variantes/buscar` | Documentado — `GET /variantes/buscar?termino=&pagina=&size=` |
| `DELETE /imagen/v2/{imagenId}` | Documentado — flujo eliminación imagen |
| `DELETE /variantes/v2/{varianteId}/imagenes` | Documentado en CAMBIOS_FRONTEND.md §15 |
| `DELETE /variantes/v2/imagenes` | Documentado en CAMBIOS_FRONTEND.md §14 |
| `DELETE /imagen/v2/{productoId}/imagenes` | Documentado en CAMBIOS_FRONTEND.md §6 y §18 |
| `DELETE /imagen/v2/producto` | Documentado en CAMBIOS_FRONTEND.md §7 |
| `DELETE /productos/deleteBy/{id}` | Documentado |
| `GET /imagen/v2/file/{imagenId}` | Documentado en CAMBIOS_FRONTEND.md §3 como endpoint del micro (apunta a 9096 directamente; este endpoint en 9091 es legacy/interno) |

### Valoración de migración a reactivo

**Prioridad media-alta, pero con complejidad significativa.**

- `getAll()` es el más crítico: se llama en `GET /variantes/buscar` que es público y paginado.
  Bloquea el hilo mientras pide bytes de imagen a `micro_imagenes` por cada página de resultados.
  Una migración a `Mono`/`Flux` en `buscarVariantes()` daría el mayor beneficio de throughput.
- `save()` y `delete()` se usan en operaciones de escritura (guardar/eliminar variantes y productos)
  que son menos frecuentes que las lecturas pero bloquean durante transferencias de archivos
  (hasta 40 MB configurado en el buffer). Son candidatos secundarios.
- `verificarExistentes()` se llama junto con `getAll()` — si se migra `getAll()` conviene
  migrar este también.
- `getOne()` es llamado directamente desde `ImageneController` — migración directa y sencilla
  cambiando el tipo de retorno del controller a `Mono<ResponseEntity<byte[]>>`.
- **Nota:** `ImageneClienteDisco` usa `@PostConstruct` para inicializar el `WebClient` con
  el baseUrl leído del `@Value` — patrón correcto, no hay problema de inicialización al migrar.

---

## 3. ImagenProductoClienteMicro.java

**Ruta:** `src/main/java/com/ventas/key/mis/productos/hexagonal/infraestructura/ImagenProductoClienteMicro.java`
**Implementa:** `ImagenProductoPort`
**URL base:** `${api.imagenes}` (mismo microservicio `micro_imagenes`, puerto 9096)

### Llamadas `.block()`

| Línea | Método | HTTP | URL en micro_imagenes |
|---|---|---|---|
| 61 | `save(RequestProductoImagen)` | POST | `${api.imagenes}/imagenes` |
| 76 | `saveAll(List<RequestProductoImagen>)` | POST | `${api.imagenes}/producto-imagen/saveAll` |
| 87 | `update(RequestProductoImagen)` | PUT | `${api.imagenes}/imagenes` |
| 96 | `update(Integer id)` — método mal nombrado, hace DELETE | DELETE | `${api.imagenes}/imagenes/{id}` |
| 105 | `findById(Integer id)` | GET | `${api.imagenes}/imagenes/{id}` |
| 117 | `buscarImagenProducto(Integer id)` | GET | `${api.imagenes}/producto-imagen/buscarImagenProducto/{id}` |

**Nota técnica:** el método `update(Integer id)` en línea 91–97 usa `webClient.delete()` pero
se llama `update` — esto parece un defecto de diseño en la interfaz `ImagenProductoPort`
(posiblemente legacy). La URL construida en línea 93 tiene una barra final:
`"/imagenes/"` + id, lo que puede producir URLs incorrectas (`.uri("/imagenes/", id)` en
WebClient no interpola el parámetro — debería ser `.uri("/imagenes/{id}", id)`).

### Endpoints REST del backend que los accionan

| Método `.block()` | Llamado desde | Endpoint REST expuesto |
|---|---|---|
| `save()` (línea 61) | No hay referencias activas en el código analizado (método declarado en interfaz, posible uso futuro) | No activo actualmente |
| `saveAll()` (línea 76) | `ProductosServiceImpl.relacionProductoImagen()` | `POST /productos/save`, `PUT /productos/update` |
| `saveAll()` (línea 76) | `ReconciliacionImagenService` | `POST /admin/reconciliacion/imagenes` |
| `update(RequestProductoImagen)` (línea 87) | No hay referencias activas encontradas | No activo actualmente |
| `update(Integer)` (línea 96) | No hay referencias activas encontradas | No activo actualmente |
| `findById(Integer)` (línea 105) | No hay referencias activas encontradas | No activo actualmente |
| `buscarImagenProducto(Integer)` (línea 117) | `ProductosServiceImpl` → `diagnosticoImagenesProducto()` | `GET /productos/admin/diagnostico-imagenes/{productoId}` |

### Presencia en documentación del front

| Endpoint | En ENDPOINTS.md / CAMBIOS_FRONTEND.md |
|---|---|
| `POST /productos/save` | Documentado |
| `PUT /productos/update` | Documentado (flujo de update) |
| `POST /admin/reconciliacion/imagenes` | Documentado en ENDPOINTS.md sección ADMIN |
| `GET /productos/admin/diagnostico-imagenes/{productoId}` | Documentado en ENDPOINTS.md sección PRODUCTOS |

### Valoración de migración a reactivo

**Prioridad baja-media.**

- Los métodos `save()`, `update(RequestProductoImagen)`, `update(Integer)` y `findById(Integer)`
  **no tienen referencias activas** en el código Java — son letra muerta o código preparado para
  uso futuro. Si no se llaman, no hay beneficio inmediato de migrarlos.
- `saveAll()` se llama en el flujo de guardado de productos. Es una operación de escritura
  poco frecuente (admin), pero bloquea durante el POST al micro. Migración recomendada cuando
  se refactorice el flujo de productos.
- `buscarImagenProducto()` se llama solo desde el endpoint de diagnóstico (admin),
  uso muy infrecuente. Prioridad baja.
- **Bug pendiente:** el método `update(Integer id)` construye la URL con
  `.uri("/imagenes/", id)` — en WebClient esto no sustituye el parámetro. La URL
  resultante sería `/imagenes/` sin el ID. Si alguna vez se activa este método fallará.
  Corregir a `.uri("/imagenes/{id}", id)` si se retoma.

---

## 4. DopoMexService.java — Ya reactivo, sin `.block()`

**Ruta:** `src/main/java/com/ventas/key/mis/productos/service/DopoMexService.java`

- Usa WebClient con `baseUrl("https://api.tau.com.mx/dipomex/v1/")` y
  `baseUrl("https://api.copomex.com/query/")`.
- Los métodos retornan `Mono<CodigoPostalResponse>` y `Mono<Object>` — **no hay `.block()`**.
- El controlador `DopoMexController` expone `GET /dipomex/getCodigoPostal/{codigoPostal}`
  retornando `Mono<>` directamente, lo que es correcto en un stack reactivo/no-bloqueante.
- **Documentado** en ENDPOINTS.md sección CLIENTES: `GET /dipomex/getCodigoPostal/{codigoPostal}`.
- **No requiere ninguna acción.**

---

## Tabla resumen global

| # | Archivo | Línea | Método Java | URL HTTP | Endpoint REST backend | En doc front | Prioridad migración |
|---|---|---|---|---|---|---|---|
| 1 | `ChatbotService` | 90 | `chat()` | POST `api.openai.com/v1/chat/completions` | `POST /chatbot/mensaje` | Sí | ~~**Alta**~~ ✅ MIGRADO |
| 2 | `ImageneClienteDisco` | 65 | `save()` | POST `${api.imagenes}/imagenes` | `POST /variantes/guardarConImagenes`, `POST /productos/save` | Sí | Media |
| 3 | `ImageneClienteDisco` | 86 | `getAll()` | GET `${api.imagenes}/imagenes?ids=...` | `GET /variantes/buscar`, `GET /imagen/v2/{id}/detalle` | Sí | **Alta** |
| 4 | `ImageneClienteDisco` | 100 | `delete()` | DELETE `${api.imagenes}/imagenes?ids=...` | `DELETE /imagen/v2/...`, `DELETE /variantes/v2/...` | Sí | Media |
| 5 | `ImageneClienteDisco` | 115 | `verificarExistentes()` | GET `${api.imagenes}/imagenes/verificar?ids=...` | `GET /variantes/buscar`, `GET /variantes/v2/imagenes/{id}` | Sí | Media |
| 6 | `ImageneClienteDisco` | 129 | `deleteInagenesDisco()` | DELETE `${api.imagenes}/imagenes/disco?ids=...` | `DELETE /productos/deleteBy/{id}` | Sí | Baja |
| 7 | `ImageneClienteDisco` | 149 | `getOne()` | GET `${api.imagenes}/imagenes?ids={id}` | `GET /imagen/v2/file/{imagenId}` | Sí (v2) | Media |
| 8 | `ImagenProductoClienteMicro` | 61 | `save()` | POST `${api.imagenes}/imagenes` | Sin uso activo | — | Baja |
| 9 | `ImagenProductoClienteMicro` | 76 | `saveAll()` | POST `${api.imagenes}/producto-imagen/saveAll` | `POST /productos/save`, `PUT /productos/update`, `POST /admin/reconciliacion/imagenes` | Sí | Baja |
| 10 | `ImagenProductoClienteMicro` | 87 | `update(RequestProductoImagen)` | PUT `${api.imagenes}/imagenes` | Sin uso activo | — | Baja |
| 11 | `ImagenProductoClienteMicro` | 96 | `update(Integer)` — hace DELETE | DELETE `${api.imagenes}/imagenes/{id}` ⚠️ URL posiblemente incorrecta | Sin uso activo | — | Baja + corregir bug URL |
| 12 | `ImagenProductoClienteMicro` | 105 | `findById()` | GET `${api.imagenes}/imagenes/{id}` | Sin uso activo | — | Baja |
| 13 | `ImagenProductoClienteMicro` | 117 | `buscarImagenProducto()` | GET `${api.imagenes}/producto-imagen/buscarImagenProducto/{id}` | `GET /productos/admin/diagnostico-imagenes/{productoId}` | Sí (admin) | Baja |

---

## Recomendaciones por orden de prioridad

### ~~Prioridad 1 — `ChatbotService.chat()` (línea 90)~~ ✅ COMPLETADO
Migrado a `Mono<String>` con `.timeout(20s)`. `ChatbotController` ya retorna `Mono<ResponseEntity<...>>`.
Bloqueo de hilos durante llamadas a OpenAI eliminado. Ver PROGRESO_SESION.md §7.

### Prioridad 2 — `ImageneClienteDisco.getAll()` (línea 86)
Se llama en `GET /variantes/buscar` que es el endpoint público de búsqueda de catálogo.
Migrar `ImagenPort.getAll()` a `Mono<List<ImagenDto>>` y propagar el `Mono` hasta
`VarianteServiceImpl.buscarVariantes()`. Es la lectura más frecuente que tiene `.block()`.

### Prioridad 3 — `ImageneClienteDisco.verificarExistentes()` (línea 115)
Mismo flujo que `getAll()` — se puede migrar en la misma tarea.

### Prioridad 4 — `ImageneClienteDisco.getOne()` (línea 149) + `ImageneController.getImagenByImagenIdV2()`
Migración directa: `getOne()` retorna `Mono<ImagenDto>`, el controller retorna
`Mono<ResponseEntity<byte[]>>`. Pocos cambios, buen ejemplo de migración incremental.

### Prioridad 5 — Operaciones de escritura (`save`, `delete`, `saveAll`)
Son menos frecuentes. Migrar en una segunda fase o cuando se refactorice el flujo
de guardado de productos/variantes por otra razón.

### No migrar ahora
Los métodos `save()`, `update()` y `findById()` de `ImagenProductoClienteMicro` que no
tienen referencias activas — no aportan beneficio de rendimiento. Limpiarlos o
documentarlos como código inactivo.

---

## Bug identificado (no relacionado con async)

**`ImagenProductoClienteMicro.update(Integer id)` línea 91–97:**
```java
return webClient.delete()
    .uri("/imagenes/", id)   // ← id NO se sustituye — URL incorrecta
```
Debería ser `.uri("/imagenes/{id}", id)`. Si este método se activa en producción,
la petición DELETE irá a `/imagenes/` sin el ID, causando un error HTTP 4xx/5xx.
