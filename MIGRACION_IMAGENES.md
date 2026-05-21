# Migración de Imágenes: proyecto-key → micro_imagenes

> Lista completa de todo lo relacionado con imágenes que debe moverse de `proyecto-key`
> al microservicio `imagenes`. Tachar cada item conforme se complete.

---

## ESTADO GENERAL

- [ ] Controllers/Endpoints (14)
- [ ] Servicios (5)
- [ ] Entidades y Repositorios (4 cada uno)
- [ ] DTOs y Modelos (13)
- [ ] Scheduler (1)
- [ ] Configuración
- [ ] Refactors en ProductosServiceImpl y VarianteServiceImpl

---

## 1. CONTROLLERS / ENDPOINTS

### ImageneController.java
`src/main/java/com/ventas/key/mis/productos/controller/ImageneController.java`

- [ ] `GET /imagen/{id}` — obtiene bytes de imagen en caché
- [ ] `GET /imagen/{id}/detalle` — obtiene detalle paginado de imagen
- [ ] `GET /imagen/file/{imagenId}` — obtiene imagen por ID
- [ ] `GET /imagen/{idProducto}/imagenes` — obtiene imágenes de un producto
- [ ] `DELETE /imagen/{idImagen}` — elimina imagen por ID
- [ ] `DELETE /imagen/{productoId}/imagenes` — elimina imágenes específicas de un producto
- [ ] `DELETE /imagen/producto` — elimina todas las imágenes de varios productos
- [ ] `GET /imagen/cache/imagen/limpiar` — limpia cache de imágenes

### ImagenPresentacionController.java
`src/main/java/com/ventas/key/mis/productos/controller/ImagenPresentacionController.java`

- [ ] `GET /presentacion/imagenes` — imágenes activas por tipo (LOGIN/REGISTRO)
- [ ] `GET /presentacion/imagenes/{id}/imagen` — bytes de imagen de presentación desde disco
- [ ] `GET /presentacion/imagenes/todas` — todas las imágenes de presentación
- [ ] `PUT /presentacion/imagenes/{id}` — actualiza imagen y metadatos de presentación

### VarianteController.java
`src/main/java/com/ventas/key/mis/productos/controller/VarianteController.java`

- [ ] `GET /variantes/imagenes/{varianteId}` — imágenes de una variante
- [ ] `GET /variantes/imagenes/{varianteId}/paginado` — imágenes paginadas de variante
- [ ] `DELETE /variantes/imagenes` — elimina imágenes de múltiples variantes
- [ ] `DELETE /variantes/{varianteId}/imagenes` — elimina imágenes específicas de variante
- [ ] `GET /variantes/admin/diagnostico-imagenes/{varianteId}` — diagnóstico de variante
- [ ] `PUT /variantes/imagenes/{varianteImagenId}/principal` — marca imagen como principal
- [ ] `POST /variantes/guardarConImagenes` — **REFACTOR** separar variante de imágenes
- [ ] `POST /variantes/inicializarDesdeProducto` — **REFACTOR** separar variante de imágenes

### ProductosControllerImpl.java
`src/main/java/com/ventas/key/mis/productos/controller/ProductosControllerImpl.java`

- [ ] `GET /productos/admin/diagnostico-imagenes/{productoId}` — diagnóstico de producto
- [ ] `POST /productos/compartir-imagenes-variantes` — comparte imágenes de producto a variantes

### AdminReconciliacionController.java
`src/main/java/com/ventas/key/mis/productos/controller/AdminReconciliacionController.java`

- [ ] `POST /admin/reconciliacion/imagenes` — reconcilia imágenes entre BD y disco
- [ ] `POST /admin/reconciliacion/imagenes/limpiar-bd` — limpia registros huérfanos de BD
- [ ] `GET /admin/reconciliacion/imagenes/resultado` — resultado de última reconciliación

---

## 2. SERVICIOS

### ImagenServiceImpl.java
`src/main/java/com/ventas/key/mis/productos/service/ImagenServiceImpl.java`
**MOVER COMPLETO**

- [ ] `saveAll()` — guarda múltiples imágenes
- [ ] `findIdsImagenesProducto()` — busca IDs de imágenes de productos
- [ ] `findImagenPrincipalPorProductoIds()` — lee bytes desde disco + cache
- [ ] `deleteById()` — elimina imagen y evicta caches
- [ ] `deleteByIds()` — elimina múltiples imágenes
- [ ] `findByIdImg()` — busca imagen por ID de producto
- [ ] `findByImagenId()` — busca imagen por ID de imagen

### ProductoImagenServiceImpl.java
`src/main/java/com/ventas/key/mis/productos/service/ProductoImagenServiceImpl.java`
**MOVER COMPLETO**

- [ ] `saveAll()` — guarda relaciones producto-imagen
- [ ] `findByProductoId()` — busca relaciones de producto-imagen
- [ ] `findByImagenesPorIdProducto()` — obtiene DTOs de imágenes del producto con URLs
- [ ] `eliminarImagenesEspecificas()` — elimina imágenes específicas, busca huérfanas
- [ ] `eliminarImagenesDeProductos()` — elimina imágenes de múltiples productos y variantes

### ImagenPresentacionService.java
`src/main/java/com/ventas/key/mis/productos/service/ImagenPresentacionService.java`
**MOVER COMPLETO**

- [ ] `getImagenesPorTipo()` — imágenes activas por tipo
- [ ] `getTodas()` — todas las imágenes
- [ ] `actualizar()` — actualiza imagen en disco y BD
- [ ] `getImagenBytes()` — lee bytes de archivo de disco
- [ ] `getMediaType()` — determina MIME type
- [ ] `eliminarArchivoEnDisco()` — elimina archivo de disco

### ReconciliacionImagenService.java
`src/main/java/com/ventas/key/mis/productos/service/ReconciliacionImagenService.java`
**MOVER COMPLETO**

- [ ] `reconciliarTodos()` — reconcilia todas las imágenes
- [ ] `reconciliarProducto()` — reconcilia imágenes de un producto
- [ ] `reconciliar()` — lógica principal de reconciliación
- [ ] `procesarProducto()` — lee disco, verifica existencia, re-envía
- [ ] `procesarVariante()` — lee disco, verifica existencia, re-envía
- [ ] `limpiarDiscoDia()` — limpia archivos huérfanos de disco
- [ ] `limpiarBdHuerfanos()` — limpia registros BD sin archivo en disco
- [ ] `getUltimoResultado()` — retorna resultado de última ejecución

### ProductosServiceImpl.java — métodos de imagen
`src/main/java/com/ventas/key/mis/productos/service/ProductosServiceImpl.java`
**REFACTOR** (no mover completo, solo separar lógica de imágenes)

- [ ] Línea ~491 `relacionProductoImagen()` — lee archivos de disco, construye FormData, publica a RabbitMQ
- [ ] Línea ~535 `mappImagenes()` — mapea DTOs a entidades, escribe a disco, genera UUID
- [ ] Línea ~571 `aplicarPrincipalProducto()` — marca imagen como principal
- [ ] Línea ~241 `deleteByIdProducto()` — refactorizar delegación de eliminación de imágenes
- [ ] Línea ~316 `compartirImagenesVarianteDto()` — mover completamente
- [ ] Línea ~632 `diagnosticarImagenesProducto()` — mover completamente

### VarianteServiceImpl.java — métodos de imagen
`src/main/java/com/ventas/key/mis/productos/service/VarianteServiceImpl.java`
**REFACTOR** (separar lógica de imagen de lógica de variante)

- [ ] Línea ~215 `subirImagenesMultipart()` — sube archivos a disco, llama cliente HTTP
- [ ] Línea ~237 `getImagenesPorVariante()` — obtiene imágenes de variante
- [ ] Línea ~243 `getImagenesPorVariantePaginado()` — imágenes paginadas
- [ ] Línea ~254 `buildImagenUpdateDtos()` — construye DTOs de imagen con URLs
- [ ] Línea ~266 `marcarImagenPrincipalVariante()` — marca imagen como principal
- [ ] Línea ~306 `subirImagenes()` — sube múltiples imágenes a disco
- [ ] Línea ~326 `vincularImagenes()` — vincula imágenes a variante
- [ ] Línea ~521 `eliminarImagenesEspecificas()` — elimina imágenes específicas
- [ ] Línea ~548 `diagnosticarImagenesVariante()` — diagnóstico de imágenes
- [ ] Línea ~592 `eliminarImagenesDeVariantes()` — elimina imágenes de variantes

---

## 3. ENTIDADES

| Archivo | Tabla | Acción |
|---------|-------|--------|
| `entity/Imagen.java` | `imagenes_copy` | MOVER COMPLETO |
| `entity/ProductoImagen.java` | `producto_imagen_copy` | MOVER COMPLETO |
| `entity/productoVariantes/VarianteImagen.java` | `variante_imagen` | MOVER COMPLETO |
| `entity/ImagenPresentacion.java` | `imagen_presentacion` | MOVER COMPLETO |

- [ ] `Imagen.java`
- [ ] `ProductoImagen.java`
- [ ] `VarianteImagen.java`
- [ ] `ImagenPresentacion.java`

---

## 4. REPOSITORIOS

- [ ] `IImagenRepository.java` — MOVER COMPLETO
- [ ] `IProductoImagenRepository.java` — MOVER COMPLETO
- [ ] `IVarianteImagenRepository.java` — MOVER COMPLETO
- [ ] `IImagenPresentacionRepository.java` — MOVER COMPLETO

---

## 5. DTOs Y MODELOS

- [ ] `models/ImagenDTO.java`
- [ ] `models/ImagenUpdateDto.java`
- [ ] `models/ProductoImagenDto.java`
- [ ] `models/ImagenProductoBase64.java`
- [ ] `models/ImagenProductoDto.java`
- [ ] `models/ImagenProductoResult.java`
- [ ] `models/DiagnosticoImagenProductoDto.java`
- [ ] `models/DiagnosticoImagenVarianteDto.java`
- [ ] `models/ImagenDiagnosticoItem.java`
- [ ] `models/CompartirImagenesVarianteDto.java`
- [ ] `dto/negocio/ImagenPresentacionUpdateDto.java`
- [ ] `models/ReconciliacionResultadoDto.java`
- [ ] `hexagonal/infraestructura/dto/ImagenDto.java`

---

## 6. ARQUITECTURA HEXAGONAL

- [ ] `hexagonal/aplicacion/ImagenesCasoUso.java` — MOVER COMPLETO
- [ ] `hexagonal/aplicacion/ImagenesProductoCasoUso.java` — MOVER COMPLETO
- [ ] `hexagonal/dominio/service/ImagenService.java` — MOVER COMPLETO
- [ ] `hexagonal/dominio/port/out/ImagenPort.java` — MOVER COMPLETO
- [ ] `hexagonal/dominio/port/out/ImagenProductoPort.java` — MOVER COMPLETO
- [ ] `hexagonal/infraestructura/ImageneClienteDisco.java` — MOVER COMPLETO
- [ ] `hexagonal/infraestructura/ImagenProductoClienteAWS.java` — MOVER COMPLETO
- [ ] `hexagonal/dominio/Imagen.java` — MOVER COMPLETO
- [ ] `hexagonal/dominio/ImagenProducto.java` — MOVER COMPLETO
- [ ] `hexagonal/dominio/ProductoImagen.java` — MOVER COMPLETO
- [ ] `hexagonal/dominio/mapper/RequestProductoImagen.java` — MOVER COMPLETO

---

## 7. SCHEDULER

- [ ] `scheduler/ImagenScheduler.java` — MOVER COMPLETO
  - Tarea medianoche: `reconciliarTodos()`
  - Tarea 4AM: `limpiarDiscoDia()`

---

## 8. CONFIGURACIÓN

- [ ] Remover de `application.yml` y `application-*.yml` la propiedad `guardar-imagenes.ruta_imagenes`
- [ ] Mantener `api.imagenes` (URL del microservicio imagenes) en proyecto-key

---

## 9. FLUJOS CLAVE A REFACTORIZAR

Estos flujos mezclan lógica de producto con lógica de imagen y deben separarse:

- [ ] **Guardar Producto con Imágenes** — separar guardado de producto del guardado de imágenes en `ProductosServiceImpl.saveProductoLote()`
- [ ] **Guardar Variante con Imágenes** — separar lógica de variante de lógica de imagen en `VarianteServiceImpl.guardarConImagenes()`
- [ ] **Eliminar Producto/Variante** — delegar eliminación de imágenes al microservicio en `deleteByIdProducto()`
- [ ] **Compartir Imágenes** — operación completamente de imagenes
- [ ] **Reconciliación** — operación completamente de imagenes

---

## ORDEN SUGERIDO DE MIGRACIÓN

1. Entidades + Repositorios (base de datos)
2. DTOs y modelos compartidos
3. Servicios independientes (`ImagenPresentacionService`, `ReconciliacionImagenService`)
4. Servicios acoplados con refactor (`ImagenServiceImpl`, `ProductoImagenServiceImpl`)
5. Controllers
6. Flujos mezclados en `ProductosServiceImpl` y `VarianteServiceImpl`
7. Scheduler
8. Limpieza de configuración
