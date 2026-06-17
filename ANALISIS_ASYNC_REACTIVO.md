# Análisis de Migración a Programación Reactiva

**Proyecto:** mis-productos (Spring Boot 3.2.5)  
**Fecha:** 2026-05-23  
**Rama analizada:** qa  
**Objetivo:** Eliminar los `.block()` en `ImageneClienteDisco` adoptando un modelo reactivo,
evaluando el impacto en la cadena de dependencias (port, services, controllers, cache).

---

## 1. Estado actual — inventario de `.block()`

`ImageneClienteDisco` usa `WebClient` (ya reactivo internamente) pero bloquea el hilo llamador
con `.block()` en **6 métodos**:

| Método | Línea | Tipo de retorno actual | Qué bloquea |
|---|---|---|---|
| `save()` | 65 | `List<ImagenDto>` | POST multipart al micro |
| `getAll()` | 86 | `List<ImagenDto>` | GET batch de imágenes |
| `delete()` | 100 | `void` | DELETE al micro |
| `verificarExistentes()` | 115 | `List<Long>` | GET /imagenes/verificar |
| `deleteInagenesDisco()` | 129 | `void` | DELETE /imagenes/disco |
| `getOne()` | 149 | `ImagenDto` | GET una imagen |

El proyecto corre sobre **Spring MVC** (`spring-boot-starter-web`), usa **JPA/Hibernate** para
la BD, y **Redis** como cache sincrónica con `@Cacheable` estándar.  
WebFlux **no** es el servidor; solo se usa `WebClient` como cliente HTTP reactivo.

---

## 2. Cambios en `ImagenPort` (interfaz)

### Opción A — Cambio total a tipos reactivos

```java
public interface ImagenPort {
    Mono<List<ImagenDto>>  save(MultiValueMap<String, ?> multipartData);
    Mono<List<ImagenDto>>  getAll(List<Long> ids);
    Mono<ImagenDto>        getOne(Long id);
    Mono<Void>             delete(List<Long> ids);
    Mono<Void>             deleteInagenesDisco(List<String> ids);
    Mono<List<Long>>       verificarExistentes(List<Long> ids);
}
```

**Consecuencia directa:** cada lugar que llama a estos métodos debe manejar `Mono<...>`, y eso
arrastra cambios a los dos services y a todos los callers del controller.

### Opción B — Cambio parcial (solo los métodos que tienen timeout risk)

Cambiar solo `getAll()` y `verificarExistentes()` a `Mono<...>` porque son los que se llaman
dentro de métodos `@Cacheable` y dentro de flujos que ya tienen manejo de error.  
`save()` y `delete()` conservan retorno síncrono porque ya están dentro de `@Transactional`
y no aplica cachear el resultado.

**Recomendación:** Opción A es más limpia arquitectónicamente. Opción B es más segura para
migrar sin romper el servlet stack. Este análisis detalla **Opción A** como objetivo y señala
dónde aplica Opción B como paso intermedio.

---

## 3. Cambios en `ImageneClienteDisco`

### 3.1 Implementación reactiva de cada método

Todos los métodos ya construyen el `Mono` completo. El único cambio es **eliminar `.block()`
al final** y devolver el `Mono`:

```java
// ANTES:
public List<ImagenDto> getAll(List<Long> ids) {
    return webClient.get()
            .uri(...)
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<List<Imagen>>() {})
            .flatMap(flat -> Mono.just(flat.stream().map(...).toList()))
            .doOnError(...)
            .onErrorReturn(List.of())
            .block();   // <-- ELIMINAR
}

// DESPUÉS:
public Mono<List<ImagenDto>> getAll(List<Long> ids) {
    return webClient.get()
            .uri(...)
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<List<Imagen>>() {})
            .flatMap(flat -> Mono.just(flat.stream().map(...).toList()))
            .doOnError(...)
            .onErrorReturn(List.of());   // Mono<List<ImagenDto>>
}
```

Esto aplica de forma idéntica a los 6 métodos. Los `void` devuelven `Mono<Void>`:

```java
public Mono<Void> delete(List<Long> ids) {
    return webClient.delete()
            .uri(...)
            .retrieve()
            .toBodilessEntity()
            .doOnError(...)
            .then();   // convierte Mono<ResponseEntity<Void>> → Mono<Void>
}
```

**Punto crítico en `save()`:** El método usa `SecurityContextHolder.getContext().getAuthentication()`
de forma sincrónica (línea 49). En un contexto reactivo, el `SecurityContext` no se propaga
automáticamente a través de operadores reactivos sin `ReactorContextHolder`. Como el servicio
sigue siendo MVC (no WebFlux), se puede capturar el token **antes** de construir el pipeline:

```java
public Mono<List<ImagenDto>> save(MultiValueMap<String, ?> multipartData) {
    // Capturar token en el hilo MVC ANTES de entrar al pipeline reactivo
    String jwtToken = SecurityContextHolder.getContext()
                                           .getAuthentication()
                                           .getCredentials().toString();
    return webClient.post()
            .uri("/imagenes")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)
            ...
}
```

Lo mismo aplica a `getAll()`, `verificarExistentes()`, etc. que ya usan `AuthenticationUtils.jwtBearerToken()`.
Verificar que `AuthenticationUtils` es thread-safe en el contexto de llamada.

---

## 4. Cambios en los Services

### 4.1 El problema fundamental: JPA + Reactive en Spring MVC

El proyecto usa `spring-boot-starter-web` (Tomcat / servlet threads), JPA bloqueante,
y `@Cacheable` sincrónico. Los services son llamados desde el thread del request HTTP.

Cuando el port devuelva `Mono<...>`, hay **tres estrategias** para consumirlo en un service
sincrónico:

#### Estrategia 1 — `.block()` con timeout (cambio mínimo, equivalente al estado actual)

```java
List<ImagenDto> imagenes = imagenPort.getAll(ids)
        .timeout(Duration.ofSeconds(5))
        .onErrorReturn(List.of())
        .block();
```

Pros: cero cambios en el service ni en los controllers.  
Contras: sigue bloqueando; solo agrega timeout. No resuelve el problema de fondo.

#### Estrategia 2 — `subscribeOn(Schedulers.boundedElastic())` + `.block()`

```java
List<ImagenDto> imagenes = imagenPort.getAll(ids)
        .subscribeOn(Schedulers.boundedElastic())
        .timeout(Duration.ofSeconds(5))
        .onErrorReturn(List.of())
        .block();
```

`boundedElastic()` mueve la suscripción a un pool de threads diseñado para operaciones de I/O
bloqueante. Esto libera el thread de Tomcat durante la espera. Pero como `WebClient` ya es
no bloqueante internamente, agregar `subscribeOn` aquí no aporta beneficio real: Netty ya
maneja la I/O en sus propios event-loop threads y el `.block()` sigue ocupando un thread.

**Veredicto:** `subscribeOn` + `.block()` es un antipatrón en este contexto. Aporta
complejidad sin beneficio cuando el caller ya es un servlet thread.

#### Estrategia 3 — Devolver `Mono<...>` desde el service y hacer el controller reactivo

Esta es la migración completa. Se detalla en §4.4.

### 4.2 El problema de `@Cacheable` con tipos reactivos

`@Cacheable` de Spring Cache **no entiende** `Mono` ni `Flux`. Si un método anotado con
`@Cacheable` devuelve `Mono<List<...>>`, Spring cachea **el objeto `Mono`** (que es
el publisher, no el resultado), con efectos impredecibles.

**Ejemplo del problema:**

```java
// MALO — Spring cachea el Mono mismo, no la List
@Cacheable(value = "variantesImagenesCache", key = "'v2:' + #varianteId")
public Mono<List<ImagenUpdateDto>> getImagenesPorVarianteV2(Integer varianteId) {
    ...
}
```

Los métodos afectados en `VarianteServiceImpl` con este problema:

- `buscarPorProducto()` — cacheable, llama a repo JPA (no al port), no tiene `.block()`
- `getImagenesPorVariante()` — cacheable, no llama al port
- `getImagenesPorVarianteV2()` — cacheable, llama a `verificarExistentes()` (líneas ~263)
- `getImagenesPorVariantePaginado()` — cacheable, llama a `verificarExistentes()` (línea ~289)
- `buscarPorProductoPaginadoResumen()` — cacheable, llama a `buildResumenDtosBatch()` (no port)

En `ImagenServiceImpl`:
- `findImagenPrincipalPorProductoIdsV2()` — cacheable, llama a `imagenPort.getAll()` (línea ~107)

### 4.3 Opciones de cache reactiva

#### Opción A — `ReactiveRedisTemplate` con cache manual (recomendada)

Eliminar `@Cacheable` del método y reemplazarlo por acceso directo a Redis reactivo:

```java
// Dependencia ya incluida: spring-boot-starter-data-redis
// Solo hay que declarar el bean ReactiveRedisTemplate

@Bean
public ReactiveRedisTemplate<String, Object> reactiveRedisTemplate(
        ReactiveRedisConnectionFactory factory) {
    return new ReactiveRedisTemplate<>(factory, RedisSerializationContext.string());
}

// En el service:
public Mono<List<ImagenUpdateDto>> getImagenesPorVarianteV2(Integer varianteId) {
    String cacheKey = "variantesImagenesCache:v2:" + varianteId;
    return reactiveRedisTemplate.opsForValue().get(cacheKey)
            .switchIfEmpty(
                computeImagenesPorVarianteV2(varianteId)
                    .flatMap(result ->
                        reactiveRedisTemplate.opsForValue()
                                .set(cacheKey, result, Duration.ofMinutes(30))
                                .thenReturn(result)
                    )
            );
}
```

Pros: funciona correctamente con tipos reactivos, sin dependencias extra.  
Contras: más código, hay que replicar la lógica de TTL y eviction manualmente.

#### Opción B — `CacheMono` de reactor-extra

```xml
<!-- Dependencia adicional requerida -->
<dependency>
    <groupId>io.projectreactor.addons</groupId>
    <artifactId>reactor-extra</artifactId>
    <version>3.5.1</version>
</dependency>
```

```java
import reactor.cache.CacheMono;
import reactor.core.publisher.Signal;

public Mono<List<ImagenUpdateDto>> getImagenesPorVarianteV2(Integer varianteId) {
    String key = "variantesImagenesCache:v2:" + varianteId;
    return CacheMono.lookup(k -> reactiveRedisTemplate.opsForValue()
                    .get(k)
                    .map(Signal::next),
                    key)
            .onCacheMissResume(() -> computeImagenesPorVarianteV2(varianteId))
            .andWriteWith((k, signal) ->
                    reactiveRedisTemplate.opsForValue()
                            .set(k, signal.get(), Duration.ofMinutes(30))
                            .then()
            );
}
```

Pros: API expresiva, idiomática para Reactor.  
Contras: dependencia extra, más abstracta para el equipo.

#### Opción C — Spring Cache con soporte reactivo (Spring Boot 3.x)

Spring Boot 3.x con `spring-boot-starter-cache` + Redis **sí** tiene soporte para `Mono`/`Flux`
cuando el `CacheManager` es `RedisCacheManager` configurado con `ReactiveRedisCache`. Sin embargo,
este soporte **requiere** que el servidor sea WebFlux (no MVC). Con `spring-boot-starter-web`,
`@Cacheable` en métodos que devuelven `Mono` sigue sin funcionar correctamente.

**Conclusión de opciones de cache:** La Opción A (ReactiveRedisTemplate manual) es la más
controlada y no requiere dependencias nuevas. La Opción B (CacheMono) es más elegante si
el equipo ya trabaja con Reactor idiomático.

### 4.4 Plan de cambios en `VarianteServiceImpl`

Los únicos métodos que realmente llaman al port son:

**a) `getImagenesPorVarianteV2()` (línea ~256) — llama a `verificarExistentes()`**

Con migración completa:
```java
// SIN @Cacheable (se mueve a cache manual)
public Mono<List<ImagenUpdateDto>> getImagenesPorVarianteV2(Integer varianteId) {
    List<VarianteImagen> relaciones = iVarianteImagenRepository.findByVarianteId(varianteId);
    if (relaciones.isEmpty()) return Mono.just(List.of());
    List<Long> ids = relaciones.stream().map(vi -> vi.getImagen().getId()).toList();

    return imagenPort.verificarExistentes(ids)              // Mono<List<Long>>
            .map(existentes -> buildImagenUpdateDtos(
                    relaciones.stream()
                            .filter(vi -> existentes.contains(vi.getImagen().getId()))
                            .toList()
            ))
            .onErrorReturn(List.of());
}
```

La llamada JPA (`findByVarianteId`) sigue siendo sincrónica y se ejecuta en el thread MVC
antes de entrar al pipeline reactivo. Esto es correcto en Spring MVC.

**b) `getImagenesPorVariantePaginado()` (línea ~274) — llama a `verificarExistentes()`**

Mismo patrón: JPA sincrónico primero, luego `flatMap` sobre el `Mono` del port.

**c) `diagnosticarImagenesVariante()` (línea ~594) — llama a `getAll()`**

No tiene `@Cacheable`. El caller (`VarianteController.diagnosticarImagenesVariante()`) es un
endpoint admin, baja frecuencia. Se puede usar `.block(timeout)` aquí sin problema práctico.

**d) `subirImagenes()` / `guardarConImagenes()` — llaman a `imageneClienteDisco.save()`**

Están dentro de `@Transactional`. El port devolvería `Mono<List<ImagenDto>>`. En este
contexto transaccional/JPA, la opción más segura es:

```java
List<Long> imageIds = imagenPort.save(formData)
        .timeout(Duration.ofSeconds(30))
        .block();  // Aceptable dentro de @Transactional MVC
```

O bien encapsular todo en `Mono.fromCallable(() -> { ...jpa... }).subscribeOn(Schedulers.boundedElastic())`.
Pero dado que la transacción JPA ya requiere un thread concreto, el `.block()` con timeout
en este punto es perfectamente justificado.

### 4.5 Plan de cambios en `ImagenServiceImpl`

**`findImagenPrincipalPorProductoIdsV2()` (línea ~100) — llama a `imagenPort.getAll()`**

```java
// ANTES: imagenPort.getAll(todosIds) devuelve List<ImagenDto>
// DESPUÉS: devuelve Mono<List<ImagenDto>>

// Con migración completa (el método mismo devuelve Mono):
@Override
public Mono<PageableDto<List<ImagenProductoBase64>>> findImagenPrincipalPorProductoIdsV2(
        Integer productoId, int page, int size) {
    List<ImagenProductoDto> todas = iImagenRepository.findImagenPrincipalPorProductoIds55(productoId);
    if (todas.isEmpty()) return Mono.just(paginarEnMemoria(List.of(), page, size));

    List<Long> todosIds = todas.stream().map(ImagenProductoDto::getIdImagen).toList();
    return imagenPort.getAll(todosIds)
            .map(imagenes -> {
                Map<Long, byte[]> bytesById = imagenes.stream()
                        .filter(dto -> dto.getImagen() != null)
                        .collect(Collectors.toMap(ImagenDto::getId, ImagenDto::getImagen));
                List<ImagenProductoBase64> conImagen = todas.stream()
                        .filter(mpa -> bytesById.containsKey(mpa.getIdImagen()))
                        .map(mpa -> { ... })
                        .toList();
                return paginarEnMemoria(conImagen, page, size);
            })
            .onErrorReturn(paginarEnMemoria(List.of(), page, size));
}
```

La anotación `@Cacheable(value = "detalle-v2", ...)` debe eliminarse y reemplazarse por
cache manual con `ReactiveRedisTemplate`.

---

## 5. Cambios en Controllers

### 5.1 `ImageneController`

El método `getImagenByImagenIdV2()` llama a `imagenPort.getOne(imagenId)` directamente:

```java
// ANTES
ImagenDto imagenDto = imagenPort.getOne(imagenId);  // bloquea internamente

// DESPUÉS (si port devuelve Mono)
return imagenPort.getOne(imagenId)
        .map(imagenDto -> ResponseEntity.ok()
                .contentType(mediaType)
                .body(imagenDto.getImagen()))
        .defaultIfEmpty(ResponseEntity.noContent().build());
```

Pero el controller está en Spring MVC. Devolver `Mono<ResponseEntity<byte[]>>` en un
`@RestController` MVC **no** funciona igual que en WebFlux: Spring MVC serializa el `Mono`
como un objeto genérico. Hay dos salidas:

- **Mantener MVC:** Usar `.block(Duration.ofSeconds(5))` con timeout en el controller.
- **Migrar el controller a WebFlux:** Solo funciona si se elimina `spring-boot-starter-web`
  y se cambia a `spring-boot-starter-webflux`, lo cual es un cambio de arquitectura mayor
  incompatible con JPA/Hibernate tradicional y con la mayoría de la configuración actual
  (Security, WebSocket, etc.).

**Recomendación para los controllers:** Mantenerlos síncronos. El timeout al hacer `.block()`
en el controller es la solución pragmática:

```java
@GetMapping("/v2/file/{imagenId}")
public ResponseEntity<byte[]> getImagenByImagenIdV2(@PathVariable Long imagenId) {
    ImagenDto imagenDto = imagenPort.getOne(imagenId)
            .timeout(Duration.ofSeconds(5))
            .onErrorReturn(new ImagenDto())
            .block();
    if (imagenDto == null || imagenDto.getImagen() == null) {
        return ResponseEntity.noContent().build();
    }
    ...
}
```

### 5.2 `VarianteController`

Los endpoints que llaman a métodos que usan el port:

- `GET /v2/imagenes/{varianteId}` → `getImagenesPorVarianteV2()` — si el service devuelve
  `Mono<List<...>>`, el controller hace `.block(timeout)`.
- `GET /imagenes/{varianteId}/paginado` → `getImagenesPorVariantePaginado()` — mismo caso.
- `GET /admin/diagnostico-imagenes/{varianteId}` — admin, baja frecuencia, `.block()` aceptable.

Los endpoints `guardarConImagenes`, `guardarVariantesInicializarDesdeProducto`, y los DELETE
son operaciones de escritura con `@Transactional`. Mantenerlos síncronos con `.block(timeout)`
sobre las operaciones del port es la opción correcta.

---

## 6. Implicaciones en VPS / Docker / Infraestructura

### 6.1 Imagen Docker

**No requiere cambios.** La imagen Docker actual ya tiene:
- `spring-boot-starter-webflux` (ya en pom.xml, línea 103) — Netty y Reactor Core ya están disponibles.
- `spring-boot-starter-data-redis` — para `ReactiveRedisTemplate` solo se necesita declarar el bean.

No se necesita cambiar la imagen base ni agregar dependencias al Dockerfile.

### 6.2 Variables de entorno

No se requieren variables de entorno nuevas. La conexión Redis (`REDIS_HOST`, `redis.host`)
ya está configurada en qa y docker YML. `ReactiveRedisTemplate` usa el mismo
`ReactiveRedisConnectionFactory` que Spring autoconfigura.

### 6.3 Pool de conexiones de BD (Hikari)

**Riesgo latente:** Actualmente `maximum-pool-size: 5` en qa y docker.

Si se migran los services a devolver `Mono` y se usa `subscribeOn(Schedulers.boundedElastic())`
para envolver llamadas JPA, el pool de threads de `boundedElastic` (por defecto hasta 10×CPUs)
puede crear más threads que conexiones disponibles en Hikari, generando colas de espera o
`SQLTimeoutException`. 

**Recomendación:** Si se adopta `boundedElastic` para llamadas JPA, alinear el pool:
```yaml
hikari:
  maximum-pool-size: 10   # Subir si se usa boundedElastic con concurrencia real
```

O bien, mantener las llamadas JPA siempre en el thread MVC (sin `subscribeOn`) y solo
hacer reactivo el llamado al microservicio externo.

### 6.4 RabbitMQ

Sin impacto. RabbitMQ está configurado en qa/docker pero los métodos en cuestión no usan
mensajería. Los cambios reactivos son exclusivamente en la capa HTTP (WebClient → micro de imágenes).

### 6.5 Configuración de Timeout en WebClient

Actualmente no hay timeout configurado en `ImageneClienteDisco`. Como parte de la migración,
agregar timeout a nivel de WebClient es obligatorio para evitar que una llamada colgada
bloquee un thread indefinidamente:

```java
@PostConstruct
public void init() {
    HttpClient httpClient = HttpClient.create()
            .responseTimeout(Duration.ofSeconds(10));
    this.webClient = builder
            .baseUrl(endpointImg)
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .exchangeStrategies(strategies)
            .build();
}
```

Esta dependencia ya está disponible (transitiva de `spring-boot-starter-webflux`).

---

## 7. Tabla resumen de archivos a modificar

| Archivo | Líneas aprox. | Cambio necesario | Riesgo |
|---|---|---|---|
| `ImagenPort.java` | 11-16 | Cambiar firmas a `Mono<...>` / `Mono<Void>` | **Medio** — rompe compilación hasta actualizar implementación y callers |
| `ImageneClienteDisco.java` | 65, 86, 100, 115, 129, 149 | Eliminar `.block()` en 6 métodos; ajustar capture de JWT en `save()` | **Bajo** — cambio mecánico, ya tiene `onErrorReturn` |
| `ImageneClienteDisco.java` | 38-44 (`init()`) | Agregar `responseTimeout` al `HttpClient` | **Muy bajo** |
| `VarianteServiceImpl.java` | ~257-271 (`getImagenesPorVarianteV2`) | Consumir `Mono<List<Long>>` de `verificarExistentes()` con `flatMap`; eliminar `@Cacheable` o migrar a cache manual | **Alto** — afecta cache, lógica de filtrado, y signature |
| `VarianteServiceImpl.java` | ~276-311 (`getImagenesPorVariantePaginado`) | Mismo patrón que `getImagenesPorVarianteV2` | **Alto** |
| `VarianteServiceImpl.java` | ~594-635 (`diagnosticarImagenesVariante`) | Consumir `Mono<List<ImagenDto>>` de `getAll()` con `.block(timeout)` o `flatMap` | **Medio** |
| `VarianteServiceImpl.java` | ~383 (`subirImagenes`) y ~239 (`subirImagenesMultipart`) | `.block(timeout)` sobre `save()` dentro de `@Transactional` | **Bajo** — solo agrega timeout |
| `ImagenServiceImpl.java` | ~99-131 (`findImagenPrincipalPorProductoIdsV2`) | Consumir `Mono<List<ImagenDto>>` de `getAll()`; manejar cache manual | **Alto** — método cacheado con `@Cacheable`, afecta el controller correspondiente |
| `ImageneController.java` | ~100-110 (`getImagenByImagenIdV2`) | Consumir `Mono<ImagenDto>` de `getOne()` con `.block(timeout)` | **Bajo** |
| `CacheConfig.java` (nuevo bean) | — | Declarar `ReactiveRedisTemplate<String, Object>` | **Bajo** — solo un `@Bean` |
| `pom.xml` | — | Agregar `reactor-extra` si se elige `CacheMono` | **Muy bajo** |
| `application-qa.yml` / `application-docker.yml` | hikari `maximum-pool-size` | Evaluar subir de 5 a 10 si se usa `boundedElastic` para JPA | **Bajo** — decisión de capacidad |

---

## 8. Orden de migración recomendado

### Fase 0 (Hoy — sin migración, mejora inmediata): Agregar `.timeout()` antes del `.block()`

**Impacto:** 30 minutos de trabajo. Sin riesgo.  
**Beneficio:** Evita que un micro de imágenes caído cuelgue threads de Tomcat indefinidamente.

```java
// En ImageneClienteDisco — cada método:
.timeout(Duration.ofSeconds(8))
.onErrorReturn(List.of())   // o List.of() / null según el método
.block();
```

Y agregar `responseTimeout` al `HttpClient` en `init()`. Esta mejora es **independiente** de
cualquier migración reactiva y debe hacerse de todas formas.

### Fase 1: Cambiar `ImagenPort` e `ImageneClienteDisco` (sin tocar services aún)

1. Actualizar `ImagenPort` con firmas `Mono<...>`.
2. Actualizar `ImageneClienteDisco` eliminando `.block()` y retornando `Mono`.
3. En todos los callers actuales del port, agregar `.block(timeout)` para restaurar compilación.
4. El resultado funcional es idéntico al actual pero con timeout y sin `.block()` en el adaptador.

**Riesgo:** Medio. La compilación fallará hasta completar el paso 3. Ejecutar en rama separada.

### Fase 2: Migrar `ImagenServiceImpl.findImagenPrincipalPorProductoIdsV2()`

1. Eliminar `@Cacheable` del método.
2. Inyectar `ReactiveRedisTemplate` en el service.
3. Implementar cache manual con `ReactiveRedisTemplate.opsForValue()`.
4. Devolver `Mono<PageableDto<...>>` desde el service.
5. Ajustar `ImageneController.getDetalleV2()` para hacer `.block(timeout)`.

### Fase 3: Migrar `VarianteServiceImpl` (los dos métodos con `verificarExistentes()`)

1. `getImagenesPorVarianteV2()` — reemplazar try/catch con flatMap reactivo.
2. `getImagenesPorVariantePaginado()` — mismo patrón.
3. Mover cache a `ReactiveRedisTemplate` o simplemente eliminar `@Cacheable` y aceptar
   que estos métodos no se cachean (son endpoints de detalle, no de listado masivo).
4. `VarianteController` no requiere cambios si el service sigue devolviendo el tipo original
   con `.block(timeout)` interno.

### Fase 4 (Opcional): Migrar controllers a WebFlux

Esta fase requiere:
- Cambiar `spring-boot-starter-web` por `spring-boot-starter-webflux`.
- Reemplazar `spring-boot-starter-data-jpa` por `r2dbc` (BD reactiva).
- Migrar Spring Security a la versión WebFlux.
- Reescribir todos los repositories a `R2dbcRepository`.

**Recomendación: NO hacer la Fase 4.** El costo es una reescritura casi total del proyecto.
El beneficio en escalabilidad no justifica el riesgo para un servicio con carga moderada
(tienda de ropa, VPS con Hikari de 5 conexiones).

---

## 9. Recomendación final

### ¿Vale la pena la migración completa?

**No completamente.** La migración total (Fase 4) implica abandonar JPA/Hibernate, Spring MVC,
WebSocket y la configuración de Security actual — una reescritura de ~80% del código.

### ¿Qué sí vale la pena?

El camino pragmático con mayor ROI:

1. **Inmediato (Fase 0):** Agregar `.timeout(Duration.ofSeconds(8))` antes de cada `.block()`.
   Esto resuelve el riesgo real: threads de Tomcat bloqueados por un micro externo caído.
   - Archivos: solo `ImageneClienteDisco.java`.
   - Tiempo: ~30 minutos.
   - Sin riesgo de regresión.

2. **A mediano plazo (Fases 1-3):** Eliminar `.block()` del adaptador `ImageneClienteDisco`
   y propagar `Mono<...>` hasta los services, haciendo `.block(timeout)` en los services.
   Esto permite agregar lógica reactiva (reintentos, circuit-breaker) en el adaptador sin tocar
   los services.
   - Tiempo estimado: 2-3 días.
   - Riesgo: Medio (rompe compilación durante la transición).

3. **Manejar la cache con cuidado:** Los métodos `@Cacheable` que llaman al port deben:
   - Eliminar `@Cacheable` y aceptar sin cache en endpoints de baja frecuencia (diagnóstico).
   - Implementar cache manual con `ReactiveRedisTemplate` en los de alta frecuencia
     (`getImagenesPorVarianteV2`, `findImagenPrincipalPorProductoIdsV2`).
   - No se necesita `reactor-extra` / `CacheMono` si se usa `ReactiveRedisTemplate` directamente.

### Lo que definitivamente NO hacer

- No usar `subscribeOn(Schedulers.boundedElastic())` como justificación para no agregar timeout.
- No devolver `Mono<...>` desde métodos con `@Cacheable` sin reemplazar el mecanismo de cache.
- No migrar a WebFlux + R2DBC a menos que se planifique como proyecto independiente con
  tiempo suficiente para reescribir y probar toda la capa de datos.
