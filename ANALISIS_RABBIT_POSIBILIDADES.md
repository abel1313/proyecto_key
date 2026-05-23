# Análisis RabbitMQ — Estado actual y posibilidades

Fecha de análisis: 2026-05-22  
Proyectos analizados:
- **mis-productos** (proyecto_key_new) — context-path: `/mis-productos`, puerto QA: 9011, puerto docker: 9010
- **micro_imagenes** (imagenes) — context-path: `/mis-productos`, puerto QA y docker: 9096

---

## 1. Estado actual de RabbitMQ

### Dónde está configurado

Ambos micros tienen RabbitMQ configurado en sus YMLs de QA y docker:

| Propiedad | mis-productos | micro_imagenes |
|-----------|---------------|----------------|
| host | `rabbitmq` | `rabbitmq` |
| port | `5672` | `5672` |
| username | `${SPRING_RABBITMQ_USERNAME}` / `${RABBITMQ_USERNAME}` | `${RABBITMQ_USERNAME}` |
| password | `${SPRING_RABBITMQ_PASSWORD}` / `${RABBITMQ_PASSWORD}` | `${RABBITMQ_PASSWORD}` |

El perfil `dev` (application.yml) **no tiene RabbitMQ configurado** — solo tiene `spring.cache.type: simple`, lo que significa que en desarrollo local no se usa Rabbit.

### Exchanges declarados

| Nombre | Tipo | Declarado en |
|--------|------|--------------|
| `exchange.imagenes` | DirectExchange (durable) | mis-productos `RabbitMQConfig` (solo el exchange) |
| `exchange.imagenes` | DirectExchange (durable) | micro_imagenes `RabbitMQConfig` (exchange + cola + binding) |
| `dlx.imagenes` | DirectExchange (durable) | micro_imagenes `RabbitMQConfig` (Dead Letter Exchange) |

### Colas declaradas

| Cola | Tipo | Declarada en | Notas |
|------|------|--------------|-------|
| `queue.guardar.imagenes` | durable | micro_imagenes `RabbitMQConfig` | DLX configurado: `dlx.imagenes` |
| `dlq.guardar.imagenes` | durable | micro_imagenes `RabbitMQConfig` | Mensajes fallidos aterrizan aquí |

### Routing keys

| Routing Key | Exchange | Cola destino |
|-------------|----------|--------------|
| `guardar.imagen` | `exchange.imagenes` | `queue.guardar.imagenes` |
| `guardar.imagen` | `dlx.imagenes` | `dlq.guardar.imagenes` |

### Quién publica (producer)

**mis-productos** — clase `ImagenProductoClienteAWS` (infraestructura hexagonal), inyectada como bean `RabbitTemplate`.  
Archivo: `src/main/java/com/ventas/key/mis/productos/hexagonal/infraestructura/ImagenProductoClienteAWS.java`

También existe un endpoint de prueba en `AdminController`:
- `GET /mis-productos/admin/test-rabbit` — publica un mensaje dummy `[{productoId:999, imagenId:1}]` al exchange.

### Quién consume (consumer)

**micro_imagenes** — clase `ImagenRabbitConsumer` con `@RabbitListener(queues = "queue.guardar.imagenes")`.  
Archivo: `src/main/java/com/venta_bolsas/imagenes/infraestructura/messaging/ImagenRabbitConsumer.java`

Si el listener lanza excepción → NACK → el mensaje va a `dlq.guardar.imagenes` (no reencola infinitamente).

---

## 2. Endpoints que usan Rabbit

### mis-productos (producer)

#### Endpoint principal: guardar/actualizar producto con imágenes

| Campo | Detalle |
|-------|---------|
| Micro | mis-productos |
| POST | `POST /mis-productos/productos/save` |
| PUT | `PUT /mis-productos/productos/update` |
| Qué publica | Lista de `RequestProductoImagen` serializada como JSON string |
| Exchange | `exchange.imagenes` |
| Routing key | `guardar.imagen` |
| Cola destino | `queue.guardar.imagenes` (en micro_imagenes) |

**Nota importante:** en la implementación actual, este flujo **NO usa Rabbit directamente para la publicación de relaciones**. El método `relacionProductoImagen()` en `ProductosServiceImpl`:
1. Sube los archivos binarios al micro vía HTTP multipart: `imagenPort.save(builder.build())`  → `POST /mis-productos/imagenes` en micro_imagenes
2. Luego llama vía HTTP: `imagenProductoClienteAWS.saveAll(relaciones)` → `POST /mis-productos/producto-imagen/saveAll` en micro_imagenes

El `RabbitTemplate` inyectado en `ImagenProductoClienteAWS` **está disponible pero no se invoca** en el flujo de guardado de producción. Existe un TODO comentado en el código señalando esto:
```
// TODO: RabbitMQ — candidato para migrar a Rabbit cuando el micro tenga @RabbitListener en queue.guardar.imagenes
```

#### Endpoint de prueba admin

| Campo | Detalle |
|-------|---------|
| Micro | mis-productos |
| Método + ruta | `GET /mis-productos/admin/test-rabbit` |
| Qué publica | `[{productoId:999, imagenId:1}]` (mensaje de prueba hardcodeado) |
| Exchange | `exchange.imagenes` |
| Routing key | `guardar.imagen` |

Este endpoint sí usa `rabbitTemplate.convertAndSend()` directamente. Está documentado como temporal y para ser eliminado en producción.

### micro_imagenes (consumer)

#### Listener activo

| Campo | Detalle |
|-------|---------|
| Micro | micro_imagenes |
| Queue escuchada | `queue.guardar.imagenes` |
| Qué recibe | JSON string que deserializa a `List<RequestProductoImagen>` |
| Payload esperado | `[{"productoId": N, "imagenId": N, "principal": bool}, ...]` |
| Qué hace | Crea relaciones `ProductoImagen` en la BD de micro_imagenes |
| En caso de error | NACK → mensaje va a `dlq.guardar.imagenes` |

---

## 3. Flujo actual — de extremo a extremo

### Flujo de guardado de producto con imágenes (flujo actual — HTTP, no Rabbit)

```
[FRONT/ADMIN]
    │
    │ POST /mis-productos/productos/save (multipart o JSON con imágenes en base64)
    ▼
[mis-productos — ProductosControllerImpl]
    │
    │ llama saveProductoLote(productoDetalle)
    ▼
[mis-productos — ProductosServiceImpl.guardarProducto()]
    │
    ├─ 1. Guarda producto en BD local (MySQL)
    ├─ 2. Genera UUIDs para nombres de archivos
    ├─ 3. Escribe los bytes de imagen en disco local (/app/imagenes)
    ├─ 4. Guarda registros Imagen en BD local (tabla imagen_copy)
    │
    │ PASO HTTP-1: sube archivos al micro de imágenes
    │ imagenPort.save(multipart) → POST /mis-productos/imagenes (micro_imagenes)
    ▼
[micro_imagenes — ImagenController.save()]
    │ Guarda archivos en disco propio + registros en tabla imagenes
    │ Devuelve List<Imagen> con IDs asignados
    ▼
[mis-productos — ProductosServiceImpl (continúa)]
    │
    │ PASO HTTP-2: guarda relaciones producto-imagen
    │ imagenProductoClienteAWS.saveAll(relaciones)
    │ → POST /mis-productos/producto-imagen/saveAll (micro_imagenes)
    ▼
[micro_imagenes — ProductoImagenController.saveAll()]
    │ Guarda relaciones en tabla producto_imagen_copy
    │ Devuelve ResponseGeneric<ProductoImagen>
    ▼
[mis-productos — fin del flujo]
    │ Responde con el Producto guardado
    ▼
[FRONT]
```

### Flujo del listener Rabbit (activo pero solo invocado por el endpoint de prueba)

```
[mis-productos — AdminController.testRabbit() o futuro publisher]
    │
    │ rabbitTemplate.convertAndSend("exchange.imagenes", "guardar.imagen", List<RequestProductoImagen>)
    ▼
[RabbitMQ broker — exchange.imagenes]
    │ routing key "guardar.imagen" → queue.guardar.imagenes
    ▼
[micro_imagenes — ImagenRabbitConsumer.procesarGuardarImagenes()]
    │
    │ Deserializa JSON → List<RequestProductoImagen>
    │ Mapea a List<ProductoImagen> (solo productoId + imagenId)
    │
    ▼
[micro_imagenes — ProductoImagenCasoUso.createAll()]
    │ Guarda relaciones en BD
    ▼
[FIN — sin respuesta al publisher (fire-and-forget)]

Si falla → RuntimeException → NACK → dlq.guardar.imagenes
```

---

## 4. Análisis de posibilidades — dónde más usar RabbitMQ

### POSIBILIDAD A — Migrar el paso HTTP-2 a Rabbit (relaciones producto-imagen)

**Operación actual:** tras subir archivos al micro vía HTTP, mis-productos llama síncronamente a `POST /mis-productos/producto-imagen/saveAll` para guardar las relaciones. Si el micro está caído en ese momento, la relación se pierde.

**Con Rabbit:** mis-productos publicaría `List<RequestProductoImagen>` al exchange después de recibir los IDs del micro. El consumer ya existe (`ImagenRabbitConsumer`) y ya sabe procesar ese payload.

**Beneficio concreto:**
- Garantía de entrega: si micro_imagenes está caído al momento de guardar el producto, el mensaje queda en `queue.guardar.imagenes` y se procesa cuando el micro levanta.
- El producto se guarda aunque la relación en micro_imagenes falle temporalmente.
- El DLQ ya está configurado para capturar fallos permanentes sin bucle infinito.

**Cambio requerido:** en `ImagenProductoClienteAWS.saveAll()`, reemplazar la llamada HTTP por `rabbitTemplate.convertAndSend(EXCHANGE_IMAGENES, ROUTING_KEY_GUARDAR, relaciones)`. El `RabbitTemplate` ya está inyectado en esa clase.

**Riesgo:** los archivos binarios aún se suben vía HTTP (paso HTTP-1), porque Rabbit no es apropiado para transferir binarios grandes. Solo las relaciones (IDs enteros) se mueven a Rabbit.

---

### POSIBILIDAD B — Evento "imagen.eliminada" para eliminación asíncrona

**Operación actual:** `DELETE /mis-productos/imagen/v2/{idImagen}` y `DELETE /mis-productos/imagen/v2/{productoId}/imagenes` llaman síncronamente al micro para eliminar archivos en disco. Si el micro está caído, los archivos quedan huérfanos en el disco del micro.

El código ya tiene el TODO anotado:
```java
// TODO: RabbitMQ — candidato para publicar evento "imagen.eliminada" a exchange.imagenes
//   para que el micro procese la eliminación del archivo de forma asíncrona.
```

**Con Rabbit:** publicar `List<Long> ids` a un nuevo routing key `"eliminar.imagen"` en el mismo exchange. El micro consumiría el evento y borraría los archivos en background.

**Beneficio concreto:**
- El DELETE de mis-productos responde inmediatamente aunque el micro esté caído.
- Los archivos eventualmente se limpian cuando el micro levanta.
- Requiere agregar una nueva cola y binding (ej: `queue.eliminar.imagenes`) tanto en mis-productos como en micro_imagenes.

**Nuevo DTO necesario:** `List<Long>` de IDs o un `EliminacionImagenEvent { List<Long> ids }`.

---

### POSIBILIDAD C — Cache evict distribuida para multi-nodo

**Operación actual:** los endpoints de evicción de caché (`GET /imagen/v2/cache/limpiar`, `PUT /presentacion/v2/imagenes/{id}`) usan `@CacheEvict` local. En un despliegue con múltiples instancias de mis-productos, solo invalida el nodo que recibió la petición.

El código ya tiene el TODO anotado en dos lugares:
```java
// TODO: RabbitMQ — publicar evento a exchange.imagenes para que todos los nodos invaliden su caché local
// TODO: RabbitMQ — cuando se implemente PUT /presentacion/v2/imagenes/{id},
//   publicar evento a exchange.imagenes routing key "cache.evict.presentacion"
```

**Con Rabbit:** al actualizar una imagen de presentación o al eliminar una imagen de producto, mis-productos publicaría un evento a un exchange de tipo `fanout` (o un routing key dedicado). Cada instancia del servicio tendría su propia cola anónima suscrita a ese exchange, consumiría el evento y ejecutaría el evict local.

**Beneficio concreto:**
- Caché coherente en todos los nodos sin necesidad de llamar Redis directamente.
- Patrón estándar para cache invalidation distribuida.
- Solo aplica si hay más de una instancia del servicio corriendo simultáneamente.

**Implementación:** requiere un nuevo `FanoutExchange` (o routing key adicional en el exchange actual), colas anónimas auto-delete, y un `@RabbitListener` en mis-productos (que actualmente solo es producer).

---

### POSIBILIDAD D — Evento de stock bajo o agotado para notificaciones

**Operación actual:** no existe ningún mecanismo de notificación cuando un producto se queda sin stock. El campo `stock` se actualiza síncronamente pero no se notifica a nadie.

**Con Rabbit:** al actualizar el stock (en `ProductosServiceImpl.guardarProducto()` o en un endpoint dedicado), publicar un evento `stock.agotado` o `stock.bajo` si el stock cae a 0 o por debajo de un umbral. Un futuro micro de notificaciones o el propio mis-productos podría consumirlo para enviar alertas.

**Beneficio concreto:**
- Desacopla la lógica de negocio (actualizar stock) de la de notificación.
- Permite agregar un micro de alertas en el futuro sin modificar ProductosServiceImpl.
- Es una adición nueva, no una migración de algo existente.

---

### POSIBILIDAD E — Evento de venta completada (integración con MercadoPago)

**Operación actual:** mis-productos tiene configurado `mercadopago.access-token` en los YMLs, lo que indica que hay lógica de pagos. Tras una venta exitosa, la actualización de stock se hace síncronamente.

**Con Rabbit:** publicar evento `venta.completada` con `{productoId, cantidad, precioFinal}` al confirmarse un pago. Esto desacopla: reducción de stock, registro de venta, notificación al admin.

**Beneficio concreto:**
- Si el procesamiento de stock falla no afecta la confirmación del pago al cliente.
- Permite auditoría de ventas de forma asíncrona y extensible.

---

## 5. Conclusión — qué implementar y en qué orden

### Prioridad 1 — POSIBILIDAD A: migrar relaciones producto-imagen a Rabbit

**Por qué primero:** la infraestructura ya está 90% lista. El `RabbitTemplate` está inyectado en `ImagenProductoClienteAWS`, el consumer `ImagenRabbitConsumer` ya existe y acepta exactamente el payload necesario, el exchange y la cola ya están declarados, el DLQ ya está configurado. El cambio es de ~5 líneas: reemplazar la llamada HTTP `saveAll()` por `rabbitTemplate.convertAndSend(...)`.

**Esfuerzo:** bajo. **Impacto:** alto (resiliencia ante caída del micro al guardar).

### Prioridad 2 — POSIBILIDAD B: evento "imagen.eliminada"

**Por qué segundo:** complementa la prioridad 1 para cerrar el ciclo completo de imágenes (guardar y eliminar). Requiere una cola nueva pero el patrón es idéntico al que ya existe.

**Esfuerzo:** medio (nueva cola + nuevo consumer en micro_imagenes). **Impacto:** medio (evita archivos huérfanos en disco).

### Prioridad 3 — POSIBILIDAD C: cache evict distribuida

**Por qué tercero:** solo aplica cuando haya más de una instancia del servicio corriendo. En un despliegue de una sola instancia el `@CacheEvict` local ya es suficiente.

**Esfuerzo:** medio (nuevo exchange fanout + colas anónimas + listener en mis-productos). **Impacto:** variable según escala.

### Prioridad 4 — POSIBILIDAD D/E: eventos de negocio (stock, ventas)

**Por qué al final:** no hay infraestructura preexistente ni consumer listo. Son adiciones nuevas que requieren diseño de contrato de mensajes y posiblemente un nuevo micro consumidor.

**Esfuerzo:** alto. **Impacto:** alto a largo plazo si el sistema escala.

---

### Resumen visual

```
ESTADO ACTUAL:
mis-productos ──HTTP multipart──▶ micro_imagenes (POST /imagenes)
mis-productos ──HTTP JSON──────▶ micro_imagenes (POST /producto-imagen/saveAll)
mis-productos ──HTTP──────────▶ micro_imagenes (DELETE /imagenes)

LISTENER LISTO (sin invocar en producción):
RabbitMQ queue.guardar.imagenes ──▶ micro_imagenes ImagenRabbitConsumer ✓

PRIORIDAD 1 (cambio mínimo):
mis-productos ──HTTP multipart──▶ micro_imagenes (POST /imagenes)   [se mantiene]
mis-productos ──Rabbit──────────▶ queue.guardar.imagenes ──▶ micro_imagenes [NUEVO]
mis-productos ──HTTP──────────▶ micro_imagenes (DELETE /imagenes)   [se mantiene por ahora]

PRIORIDAD 2 (nueva cola):
mis-productos ──Rabbit──────────▶ queue.eliminar.imagenes ──▶ micro_imagenes [NUEVO]
```
