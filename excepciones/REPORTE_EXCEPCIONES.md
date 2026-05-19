# Reporte de Manejo de Excepciones — Proyecto Key New

**Fecha:** 2026-05-19

---

## Estado antes de las correcciones

El proyecto tenía los siguientes problemas en el manejo de excepciones:

| # | Problema | Severidad |
|---|----------|-----------|
| 1 | `ExceptionDataNotFound` devolvía HTTP 404 pero el campo `code` en el body era `400` | MEDIO |
| 2 | El catch-all `Exception` devolvía HTTP 400 en lugar de 500 — los errores del servidor parecían errores del cliente | ALTO |
| 3 | El catch-all usaba `log.info` en lugar de `log.error` — los errores de servidor no aparecían en alertas | MEDIO |
| 4 | `ExceptionDuplicado` devolvía HTTP 400 en lugar de 409 (Conflict) | BAJO |
| 5 | Dos `@ControllerAdvice` activos: `ExceptionGlobal` y `GlobalExceptionHandler` en `handleExeption/` | MEDIO |
| 6 | `ErrorResponse.java` sin getters — Jackson no podía serializarla y devolvía `{}` vacío | ALTO |
| 7 | Llamada a `rabbitTemplate.convertAndSend()` sin try-catch — si RabbitMQ cae, falla el guardado del producto | ALTO |
| 8 | `CrudAbstractServiceImpl.update()` lanzaba `new Exception(e.getMessage())` perdiendo el stack trace original | BAJO |
| 9 | No existía excepción específica para stock insuficiente — se usaba `RuntimeException` genérica | BAJO |
| 10 | No existían handlers para `MethodArgumentNotValidException` ni `MaxUploadSizeExceededException` | MEDIO |

---

## Cambios Realizados

### Archivos Modificados

#### `ExceptionGlobal.java`
- **`ExceptionDataNotFound`**: corregido `code` de `400` → `404`. Log cambiado de `info` a `warn`.
- **`ExceptionDuplicado`**: cambiado HTTP status de `400` → `409 CONFLICT`. Code en body actualizado a `409`.
- **`ExceptionErrorInesperado`**: log cambiado de `info` a `warn`.
- **Catch-all `Exception`**: cambiado HTTP status de `400` → `500 INTERNAL_SERVER_ERROR`. Code en body actualizado a `500`. Log cambiado de `info` a `error`. El mensaje devuelto al cliente ahora es genérico ("Error interno del servidor") — no expone detalles internos.
- **Agregados nuevos handlers**:
  - `MethodArgumentNotValidException` → 400 con detalle de campos inválidos
  - `MaxUploadSizeExceededException` → 400 con mensaje claro
  - `ExceptionStockInsuficiente` → 422 UNPROCESSABLE_ENTITY

#### `handleExeption/GlobalExceptionHandler.java`
- Actualizado para usar el campo `codigo` de `GenericException` y devolver el HTTP status correcto:
  - `codigo = 1062` (duplicado SQL) → HTTP 409 CONFLICT
  - `codigo = 500` → HTTP 500 INTERNAL_SERVER_ERROR
  - Cualquier otro → HTTP 400 BAD_REQUEST
- Cambiado para devolver `ErrorResponse` (JSON) en lugar de `String` plano.

#### `handleExeption/ErrorResponse.java`
- Agregado `@Getter` de Lombok — Jackson ahora puede serializar la clase correctamente.

#### `hexagonal/infraestructura/ImagenProductoClienteAWS.java`
- Método `saveAll()`: envuelto `rabbitTemplate.convertAndSend()` en try-catch para `Exception`.
- Si RabbitMQ está caído, el error se loguea pero **el guardado del producto no falla**.
- El mensaje en cola se recuperará cuando RabbitMQ vuelva a estar disponible.

#### `service/CrudAbstractServiceImpl.java`
- Método `update()`: cambiado `throw new Exception(e.getMessage())` → `throw new Exception(e.getMessage(), e)` para preservar el stack trace original (facilita el diagnóstico en producción).

### Archivos Creados

#### `exeption/ExceptionStockInsuficiente.java`
- Nueva excepción específica para cuando no hay suficiente stock para completar una venta.
- Se lanza con HTTP 422 (UNPROCESSABLE_ENTITY) — indica que la solicitud es válida pero no se puede procesar por el estado del negocio.

---

## Mapa de Excepciones Actual

| Excepción | HTTP Status | Code en Body | Cuándo se lanza |
|-----------|-------------|--------------|-----------------|
| `ExceptionDataNotFound` | 404 NOT_FOUND | 404 | Entidad no encontrada por ID |
| `ExceptionErrorInesperado` | 400 BAD_REQUEST | 400 | Error de negocio esperado |
| `ExceptionDuplicado` | 409 CONFLICT | 409 | Entidad duplicada (violación de unicidad) |
| `ExceptionStockInsuficiente` | 422 UNPROCESSABLE_ENTITY | 422 | Stock insuficiente para venta |
| `GenericException(1062, ...)` | 409 CONFLICT | — | Error SQL de clave duplicada |
| `GenericException(500, ...)` | 500 INTERNAL_SERVER_ERROR | — | Error de BD genérico |
| `MethodArgumentNotValidException` | 400 BAD_REQUEST | 400 | Falla de @Valid en RequestBody |
| `MaxUploadSizeExceededException` | 400 BAD_REQUEST | 400 | Archivo demasiado grande |
| `Exception` (catch-all) | 500 INTERNAL_SERVER_ERROR | 500 | Cualquier error no controlado |

---

## Pendiente de Revisión Manual

Los siguientes casos usan `RuntimeException` genérica en services pero requieren revisión del equipo antes de cambiar (lógica de negocio compleja):

| Archivo | Línea aprox. | Situación |
|---------|-------------|-----------|
| `VentaServiceImpl.java` | ~136, ~143 | Stock insuficiente → cambiar a `ExceptionStockInsuficiente` |
| `PedidoServiceImpl.java` | ~115, ~129, ~160 | Pedido no existe → cambiar a `ExceptionDataNotFound` |
| `GanadorRifaServiceImpl.java` | Múltiples | Reglas de rifa → cambiar a `ExceptionErrorInesperado` |
| `ImagenPresentacionService.java` | ~43, ~70 | Imagen no encontrada → cambiar a `ExceptionDataNotFound` |