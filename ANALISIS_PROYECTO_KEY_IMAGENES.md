# Análisis de Endpoints de Imágenes — proyecto_key_new

Fecha: 2026-05-22

---

## 1. URI base del micro

```
http://host:9011/mis-productos
```

Puerto: **9011** | Context-path: **/mis-productos**
Fuente: `src/main/resources/application-qa.yml`

---

## 2. Lista completa de endpoints de imágenes

> Todas las rutas son relativas al context-path `/mis-productos`.
> "Público" = `permitAll()` sin token. "ADMIN" = requiere JWT con `ROLE_ADMIN`. "Autenticado" = cualquier JWT válido.

### 2.1 ImageneController — `/imagen`

| # | Método HTTP + Ruta completa | Qué hace | Auth |
|---|---|---|---|
| 1 | `GET /mis-productos/imagen/{id}` | **DEPRECATED** — Obtiene bytes de imagen por ID de producto (lee del disco local) | Público |
| 2 | `GET /mis-productos/imagen/v2/{productoId}` | Obtiene bytes de imagen principal del producto (delega al micro_imagenes via `ImagenProductoPort.buscarImagenProducto`) | Público |
| 3 | `GET /mis-productos/imagen/{id}/detalle` | **DEPRECATED** — Lista paginada de imágenes con datos base64 para un producto (lee disco local) | Público |
| 4 | `GET /mis-productos/imagen/v2/{productoId}/detalle` | Lista paginada de imágenes de un producto obteniendo bytes en lote del micro_imagenes | Público |
| 5 | `GET /mis-productos/imagen/file/{imagenId}` | **DEPRECATED** — Bytes de imagen por ID de imagen (lee disco local) | Público |
| 6 | `GET /mis-productos/imagen/v2/file/{imagenId}` | Bytes de imagen por ID de imagen (delega a micro_imagenes via `ImagenPort.getOne`) | Público |
| 7 | `GET /mis-productos/imagen/{idProducto}/imagenes` | **DEPRECATED** — Lista de imagenes del producto con URL apuntando a `/imagen/file/` (ruta interna disco) | Público |
| 8 | `GET /mis-productos/imagen/v2/{idProducto}/imagenes` | Lista de imágenes del producto con URL apuntando a `/imagen/v2/file/` (ruta del micro) | Público |
| 9 | `GET /mis-productos/imagen/cache/limpiar` | **DEPRECATED** — Limpia cache "imagenes" | **ADMIN** |
| 10 | `GET /mis-productos/imagen/v2/cache/limpiar` | Limpia caches: imagenes, detalleImagen, detalle, detalle-v2, buscarImagenIdCache | **ADMIN** |
| 11 | `DELETE /mis-productos/imagen/{idImagen}` | **DEPRECATED** — Elimina imagen de la BD local (tabla imagenes_copy) sin tocar micro | **ADMIN** |
| 12 | `DELETE /mis-productos/imagen/v2/{idImagen}` | Elimina imagen del micro_imagenes y luego de BD local | **ADMIN** |
| 13 | `DELETE /mis-productos/imagen/{productoId}/imagenes` | **DEPRECATED** — Elimina imagenes específicas de un producto (BD local, luego micro si huérfanas) | **ADMIN** |
| 14 | `DELETE /mis-productos/imagen/v2/{productoId}/imagenes` | Elimina imágenes específicas de un producto (igual que v1; el servicio ya llama al micro) | **ADMIN** |
| 15 | `DELETE /mis-productos/imagen/producto` | **DEPRECATED** — Elimina todas las imágenes de una lista de productos + variantes | **ADMIN** |
| 16 | `DELETE /mis-productos/imagen/v2/producto` | Elimina todas las imágenes de una lista de productos + variantes (igual lógica que v1) | **ADMIN** |

---

### 2.2 ImagenPresentacionController — `/presentacion`

| # | Método HTTP + Ruta completa | Qué hace | Auth |
|---|---|---|---|
| 17 | `GET /mis-productos/presentacion/imagenes?tipo=LOGIN` | **DEPRECATED** — Lista imágenes de presentación activas por tipo (entidad expuesta) | Público |
| 18 | `GET /mis-productos/presentacion/v2/imagenes?tipo=LOGIN` | Lista imágenes de presentación activas por tipo con DTO (urlImagen calculada, con cache) | Público |
| 19 | `GET /mis-productos/presentacion/imagenes/{id}/imagen` | **DEPRECATED** — Bytes de imagen de presentación por ID (lee disco local) | Público |
| 20 | `GET /mis-productos/presentacion/v2/imagenes/{id}/imagen` | Bytes de imagen de presentación por ID (actualmente lee el mismo disco local; pendiente de migrar al micro) | Público |
| 21 | `GET /mis-productos/presentacion/imagenes/todas` | **DEPRECATED** — Lista todas las imágenes de presentación (activas e inactivas), expone nombreArchivo de disco | **ADMIN** |
| 22 | `GET /mis-productos/presentacion/v2/imagenes/todas` | Lista todas las imágenes de presentación con DTO limpio (urlImagen calculada) | **ADMIN** |
| 23 | `PUT /mis-productos/presentacion/imagenes/{id}` | **DEPRECATED** — Actualiza imagen de presentación (guarda bytes en disco, no invalida cache) | **ADMIN** |
| 24 | `PUT /mis-productos/presentacion/v2/imagenes/{id}` | Actualiza imagen de presentación (guarda bytes en disco, invalida cache "presentacion-imagenes") | **ADMIN** |

---

### 2.3 VarianteController — `/variantes` (solo endpoints de imágenes)

| # | Método HTTP + Ruta completa | Qué hace | Auth |
|---|---|---|---|
| 25 | `GET /mis-productos/variantes/buscar?termino=X&pagina=1&size=10` | Búsqueda paginada de variantes incluyendo URL de imagen (apunta a micro_imagenes) | Público |
| 26 | `GET /mis-productos/variantes/imagenes/{varianteId}` | **DEPRECATED** — Lista imágenes de la variante sin verificar existencia en micro | Público |
| 27 | `GET /mis-productos/variantes/v2/imagenes/{varianteId}` | Lista imágenes de la variante verificando existencia real en micro_imagenes | Público |
| 28 | `GET /mis-productos/variantes/imagenes/{varianteId}/paginado` | Lista paginada de imágenes de variante con verificación de existencia en micro | Público |
| 29 | `POST /mis-productos/variantes/guardarConImagenes` | Guarda variantes con imágenes (sube al micro, vincula en BD local) | **ADMIN** |
| 30 | `POST /mis-productos/variantes/inicializarDesdeProducto` | Crea variantes en lote para un producto con imágenes opcionales (multipart) | **ADMIN** |
| 31 | `DELETE /mis-productos/variantes/imagenes` | **DEPRECATED** — Elimina imágenes de múltiples variantes | **ADMIN** |
| 32 | `DELETE /mis-productos/variantes/v2/imagenes` | Elimina imágenes de múltiples variantes (igual lógica; borra micro si huérfanas) | **ADMIN** |
| 33 | `DELETE /mis-productos/variantes/{varianteId}/imagenes` | **DEPRECATED** — Elimina imágenes específicas de una variante | **ADMIN** |
| 34 | `DELETE /mis-productos/variantes/v2/{varianteId}/imagenes` | Elimina imágenes específicas de una variante (borra micro si huérfanas) | **ADMIN** |
| 35 | `PUT /mis-productos/variantes/imagenes/{varianteImagenId}/principal` | Marca una imagen como principal para su variante | **ADMIN** |
| 36 | `GET /mis-productos/variantes/admin/diagnostico-imagenes/{varianteId}` | ADMIN: diagnostica inconsistencias de imágenes de una variante entre BD local y micro | **ADMIN** |

---

### 2.4 ProductosControllerImpl — `/productos` (solo endpoints relacionados a imágenes)

| # | Método HTTP + Ruta completa | Qué hace | Auth |
|---|---|---|---|
| 37 | `POST /mis-productos/productos/save` | Guarda producto; si incluye imágenes, las escribe en disco local y sube al micro_imagenes | **ADMIN** |
| 38 | `PUT /mis-productos/productos/update` | Actualiza producto; idem para imágenes nuevas | **ADMIN** |
| 39 | `GET /mis-productos/productos/admin/diagnostico-imagenes/{productoId}` | ADMIN: diagnostica inconsistencias de imágenes de un producto entre BD local y micro | **ADMIN** |
| 40 | `POST /mis-productos/productos/compartir-imagenes-variantes` | Copia las imágenes de un producto a todas sus variantes (solo vincula en BD local) | **ADMIN** |
| 41 | `DELETE /mis-productos/productos/deleteBy/{id}` | Elimina producto: borra variantes, desvincula imágenes del micro (deleteInagenesDisco) y BD | **ADMIN** |

---

## 3. Análisis por controlador

### 3.1 ImageneController

**Qué hace:**
Gestiona el ciclo de vida de imágenes de productos/variantes en la BD local (`imagenes_copy`, `producto_imagen`). Expone bytes de imagen, listados paginados, eliminciones y limpieza de cache. Las versiones v2 ya delegan la obtención de bytes al micro_imagenes (`ImagenPort`/`ImagenProductoPort`). Las v1 son deprecadas y leen del disco local mediante `rutaImagenes`.

**Dependencias de tablas propias de proyecto_key:**
- `imagenes_copy` (entidad `Imagen`) — registros con nombre de archivo en disco y metadata
- `producto_imagen` (relación `ProductoImagen`) — join entre producto e imagen
- `variante_imagen` (entidad `VarianteImagen`) — join entre variante e imagen
- `productos` y `variantes` — para consultas que filtran por producto/variante

**¿Puede moverse a micro_imagenes?**
Parcialmente. Los endpoints de **lectura de bytes** (v2/file/{imagenId}, v2/{productoId}) ya son proxies delgados hacia micro_imagenes — tienen sentido moverse. Los endpoints de **eliminación** y **listados** no se pueden mover directamente porque:
1. La lógica de huérfanos (`findOrphanIds`) cruza entre `producto_imagen` y `variante_imagen`, ambas tablas de este micro.
2. Las eliminaciones desencadenan invalidación de caches de Redis que pertenecen al micro de productos.
3. El listado `/imagen/v2/{productoId}/detalle` combina datos de `producto_imagen` (relación) con bytes del micro.

**Qué se necesitaría para moverlo:**
- micro_imagenes debería exponer un endpoint `DELETE /imagenes/batch?ids=...` que ya existe (lo usa `ImagenPort.delete`).
- La lógica de "huérfanos" (`findOrphanIds`) requeriría que micro_imagenes conozca las relaciones producto-imagen y variante-imagen, que actualmente viven aquí.
- Si las tablas `producto_imagen` y `variante_imagen` se mueven al micro, entonces sí sería posible. Si permanecen aquí, no.

---

### 3.2 ImagenPresentacionController

**Qué hace:**
Gestiona imágenes de presentación de la aplicación (banners en pantalla de login/registro). Las imágenes se guardan en disco local (`rutaImagenes`). Las versiones v2 retornan DTOs con URLs calculadas en vez de exponer campos internos. La actualización (PUT) ya invalida cache con `@CacheEvict`.

**Dependencias de tablas propias de proyecto_key:**
- `imagen_presentacion` — tabla completamente independiente de productos/variantes/pedidos. Solo tiene: id, tipo, orden, nombreArchivo (ruta en disco), extension, descripcion, activo, actualizadoEn.

**¿Puede moverse a micro_imagenes?**
Sí, es el candidato **más independiente** de todos. La tabla `imagen_presentacion` no tiene FK hacia `productos`, `variantes`, `clientes` ni ninguna otra entidad de negocio. Es un dominio de imagen puro.

**Qué se necesitaría para moverlo:**
1. Crear en micro_imagenes los endpoints equivalentes: `GET /presentacion/imagenes?tipo=X`, `GET /presentacion/imagenes/{id}/imagen`, `GET /presentacion/imagenes/todas`, `PUT /presentacion/imagenes/{id}`.
2. Migrar la tabla `imagen_presentacion` a la BD del micro.
3. El endpoint `PUT /presentacion/v2/imagenes/{id}` actualmente guarda bytes en el disco local de este micro (`rutaImagenes`). Al migrar, el micro_imagenes deberá gestionar ese disco (o su equivalente en S3/almacenamiento externo).
4. Actualizar la config de seguridad en SecurityConfig para redirigir si los endpoints se sirven desde otro origen, o eliminar las reglas de aquí.
5. Invalidar la cache "presentacion-imagenes" desde el micro (Rabbit o HTTP).

---

### 3.3 VarianteController (endpoints de imágenes)

**Qué hace:**
- Lectura: obtiene imágenes asociadas a variantes con verificación de existencia en micro_imagenes (v2).
- Escritura: sube imágenes al micro_imagenes via `ImageneClienteDisco.save()`, luego guarda relaciones en `variante_imagen` local.
- Eliminación: borra de `variante_imagen`, detecta huérfanos, borra de `imagenes_copy` y llama a `ImagenPort.delete()` en el micro.
- Diagnóstico: cruza datos de `variante_imagen` (BD local) con respuesta del micro.

**Dependencias de tablas propias de proyecto_key:**
- `variante_imagen` — join entre variante e imagen
- `imagenes_copy` — metadata de imagen
- `variantes` — para validar stock y existencia del padre
- `productos` — para validar stock disponible al crear variantes con imágenes
- `producto_imagen` — consultado al crear variantes desde producto (para no duplicar imágenes del producto)

**¿Puede moverse a micro_imagenes?**
No en su estado actual. Los motivos son:
1. La validación de stock (`guardarConImagenes`, `inicializarDesdeProducto`) lee tablas `productos` y `variantes` que son el núcleo de este micro. Habría que separar esa lógica o exponer un endpoint de validación.
2. La tabla `variante_imagen` actúa como punto de unión entre el dominio "variante" (de este micro) y el dominio "imagen" (del micro_imagenes). No puede vivir en el micro_imagenes sin tener acceso a la tabla `variantes`.
3. Las eliminaciones encadenan invalidación de caches (`variantesImagenesCache`, `variantesProductoCache`, `detalleImagen`) que son caches de negocio del micro de productos.

**Qué se necesitaría para moverlo parcialmente:**
- Los endpoints de **lectura de bytes** (`/variantes/v2/imagenes/{varianteId}`) son delegados simples al micro; podrían servirse directamente desde micro_imagenes si el cliente llama al micro_imagenes directamente, eliminando este intermediario.
- Los endpoints de **escritura con imágenes** requieren un contrato en micro_imagenes que reciba `varianteId` + archivos, pero la validación de stock debería quedar en este micro (evento previo o llamada previa al micro de productos).

---

### 3.4 ProductosControllerImpl (endpoints de imágenes)

**Qué hace:**
Al guardar/actualizar un producto: escribe los bytes en disco local (`rutaImagenes`), registra en `imagenes_copy` y `producto_imagen`, luego sube al micro_imagenes via `ImagenPort.save()` y registra la relación en el micro via `ImagenProductoClienteMicro.saveAll()`.
Al eliminar un producto: borra variantes con sus imágenes, llama a `ImagenPort.deleteInagenesDisco()` para borrar archivos en el micro.
El endpoint de diagnóstico cruza `producto_imagen` (BD local) con la respuesta del micro.
El endpoint `compartir-imagenes-variantes` solo opera en `producto_imagen` y `variante_imagen` local, sin tocar el micro.

**Dependencias de tablas propias de proyecto_key:**
- `productos` — entidad central de negocio
- `producto_imagen` — relación producto-imagen
- `variante_imagen` — al eliminar producto se borran también variantes con sus imágenes
- `imagenes_copy` — metadata de imágenes
- `variantes`, `codigo_barras`, `lotes_productos`, `palabras_clave` — parte del núcleo de negocio

**¿Puede moverse a micro_imagenes?**
No. Los endpoints de imágenes en `ProductosControllerImpl` están entrelazados con la lógica central de negocio (guardar/actualizar/eliminar producto). La gestión de imágenes es un efecto secundario del CRUD de productos. Separarlos requeriría descomponer las transacciones `@Transactional` que cubren tanto la persistencia del producto como la de sus imágenes.

**Qué se necesitaría para avanzar:**
- Los endpoints v2 de escritura de imágenes (`guardarProducto` interno) ya hacen HTTP al micro. El siguiente paso sería publicar un evento Rabbit ("producto.guardado" con imageIds) en vez de HTTP síncrono, para desacoplar el flujo. El código ya tiene el TODO anotado.
- `compartir-imagenes-variantes` únicamente crea registros en `variante_imagen` local; si esa tabla migrara al micro, este endpoint también debería migrar o convertirse en una llamada al micro.

---

## 4. Conclusión general

### Qué se puede mover al micro_imagenes

| Controlador / Funcionalidad | ¿Se puede mover? | Condición |
|---|---|---|
| `ImagenPresentacionController` completo | **Sí, prioritario** | La tabla `imagen_presentacion` es independiente; no hay FK a negocio |
| Lectura de bytes (`/imagen/v2/file/{id}`, `/imagen/v2/{productoId}`) | **Sí, como proxies** | Ya son wrappers de llamadas al micro; podrían eliminarse y el front llamaría directamente al micro_imagenes |
| `VarianteController` — endpoints de lectura de imágenes | **Parcialmente** | Solo si el front llama directamente al micro_imagenes; requiere que micro_imagenes implemente `/variantes/imagenes/{id}` |

### Qué no se puede mover sin refactor mayor

| Controlador / Funcionalidad | Por qué no |
|---|---|
| `ImageneController` — eliminaciones y listados | Lógica de huérfanos cruza `producto_imagen` + `variante_imagen` de este micro; invalidación de caches de negocio |
| `VarianteController` — escritura con imágenes | Validación de stock requiere acceso a `productos` y `variantes` de este micro en la misma transacción |
| `ProductosControllerImpl` — save/update/delete | Las imágenes son un efecto secundario de la transacción de negocio central del producto; separar rompe la atomicidad |
| `compartir-imagenes-variantes` | Opera sobre `variante_imagen` que es una tabla de join entre dominio negocio y dominio imagen |

### Orden recomendado de migración

1. **`ImagenPresentacionController`** — mover primero. Sin dependencias de negocio. Requiere solo: crear tabla en micro_imagenes, migrar datos, crear endpoints equivalentes, actualizar SecurityConfig aquí para quitar las reglas de `/presentacion/**`.

2. **Eliminar proxies de lectura de bytes** — los endpoints `/imagen/v2/file/{id}` y `/imagen/v2/{productoId}` son envoltorios delgados del micro. El front puede llamar directamente a micro_imagenes. Deprecar y eliminar con ventana de compatibilidad.

3. **Migrar tablas `producto_imagen` y `variante_imagen` al micro_imagenes** — este es el paso más disruptivo y habilita todo lo demás. Requiere:
   - Exponer en micro_imagenes endpoints que acepten `productoId`/`varianteId` como claves externas (sin FK real, solo valor de referencia).
   - Cambiar este micro para que todas las escrituras en esas tablas se hagan via HTTP o Rabbit al micro_imagenes.
   - Mover la lógica de huérfanos al micro_imagenes.

4. **Una vez migradas las tablas relacionales**, los endpoints de `ImageneController` y `VarianteController` relativos a imágenes pueden mover su lógica al micro_imagenes. Los de validación de stock quedan en este micro como paso previo.

5. **`ProductosControllerImpl` — imágenes** — último. Solo cuando el micro_imagenes soporte las relaciones y el flujo de eliminación masiva. El TODO de RabbitMQ ya está identificado en el código; publicar evento "producto.imagenes.subidas" desacopla sin romper atomicidad.

---

## Tablas de BD involucradas (resumen)

| Tabla | Entidad Java | ¿Dónde debería vivir? |
|---|---|---|
| `imagenes_copy` | `Imagen` | micro_imagenes (repositorio marcado `@Deprecated`) |
| `producto_imagen` | `ProductoImagen` | micro_imagenes (repositorio marcado `@Deprecated`) |
| `variante_imagen` | `VarianteImagen` | micro_imagenes (repositorio marcado `@Deprecated`) |
| `imagen_presentacion` | `ImagenPresentacion` | micro_imagenes (repositorio marcado `@Deprecated`) |

Los cuatro repositorios ya están marcados con `@Deprecated` y con el comentario "Migrar a micro_imagenes. No agregar nueva lógica aquí."
