# Hilos y concurrencia — guía completa + plan para este proyecto

**Estado:** ⏳ PENDIENTE — decidido que se va a implementar, aún no arrancado.
**Origen:** se detectó que el envío de correos (tickets, códigos de verificación, reset de
contraseña) corre 100% síncrono en el hilo del request — si el SMTP tarda o se cae, el usuario se
queda esperando hasta el timeout configurado (5 segundos en QA) antes de recibir respuesta.

Este documento tiene dos partes:
1. **Explicación completa de hilos en Java/Spring** — desde lo básico hasta lo más nuevo (virtual
   threads), para que quede como referencia sin tener que buscar en otro lado.
2. **Auditoría concreta de este proyecto** — qué endpoints/métodos conviene paralelizar o hacer
   asíncronos, cuáles no, y por qué en cada caso.

---

## Parte 1 — Hilos en Java, de cero a lo más nuevo

### 1.1 Qué es un hilo

Un hilo (`Thread`) es una línea de ejecución independiente dentro de un mismo proceso. Todos los
hilos de una aplicación Java comparten el mismo heap (memoria de objetos), pero cada uno tiene su
propio **stack** (variables locales, llamadas a métodos) y su propio **program counter**.

Una app Spring Boot típica arranca con:
- 1 hilo principal (`main`) que solo levanta el contexto y termina.
- Un **pool de hilos del servidor** (Tomcat embebido, por defecto ~200 hilos) — cada request HTTP
  entrante se atiende en uno de esos hilos, tomado prestado del pool y devuelto al terminar.
- Hilos de librerías (pool de conexiones de BD via Hikari, hilos de Redis, hilos de RabbitMQ, el
  scheduler de `@Scheduled`, etc.) — cada uno con su propio pool separado.

**Punto clave:** si el código de un controller hace algo lento y bloqueante (ej. esperar una
respuesta SMTP), el hilo de Tomcat que atiende ese request queda **ocupado todo ese tiempo** — no
puede atender otro request mientras tanto. Con suficientes requests lentos simultáneos, se agotan
los hilos del pool y la aplicación deja de responder aunque el CPU esté ocioso.

### 1.2 `Thread` vs `Runnable` vs `Callable`

```java
// Opción 1: extender Thread (rara vez se usa en apps reales)
class MiHilo extends Thread {
    public void run() { /* trabajo */ }
}
new MiHilo().start();

// Opción 2: Runnable — no devuelve nada, no lanza excepciones checked
Runnable tarea = () -> System.out.println("trabajo");
new Thread(tarea).start();

// Opción 3: Callable — devuelve un valor, puede lanzar excepciones checked
Callable<String> tarea2 = () -> {
    return "resultado";
};
```

`start()` crea un hilo real del sistema operativo y ejecuta `run()` en paralelo. Llamar `run()`
directo (sin `start()`) es un error común — eso ejecuta el código en el hilo actual, sin
paralelismo real.

### 1.3 Ciclo de vida de un hilo

```
NEW → RUNNABLE → (BLOCKED / WAITING / TIMED_WAITING) → TERMINATED
```

- **NEW**: se creó el objeto `Thread` pero no se llamó `start()`.
- **RUNNABLE**: corriendo o listo para correr (el SO decide cuándo le toca CPU).
- **BLOCKED**: esperando entrar a un bloque `synchronized` que otro hilo tiene ocupado.
- **WAITING / TIMED_WAITING**: pausado esperando una señal (`wait()`, `join()`,
  `Thread.sleep()`, o un `Future.get()` con timeout).
- **TERMINATED**: el método `run()` terminó (normal o por excepción no capturada).

### 1.4 El problema real: condiciones de carrera (race conditions)

Cuando dos hilos leen y escriben la misma variable sin coordinación:

```java
private int contador = 0;
public void incrementar() { contador++; }   // NO es atómico: leer, sumar, escribir = 3 pasos
```

Si dos hilos llaman `incrementar()` al mismo tiempo, pueden leer el mismo valor antes de que
cualquiera escriba, y se pierde un incremento. Esto es una **condición de carrera**.

En este proyecto ya existe un ejemplo real de este problema resuelto correctamente: al descontar
stock en una venta/pedido, dos ventas simultáneas del mismo producto podrían leer el mismo stock
y ambas restar, dejando el stock mal. Por eso `IProductosRepository` e `IVarianteRepository`
tienen:

```java
// IProductosRepository.java y IVarianteRepository.java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT p FROM Producto p WHERE p.id = :id")
Optional<Producto> findByIdWithLock(@Param("id") Integer id);
```

Esto le pide a MySQL un `SELECT ... FOR UPDATE` — bloquea esa fila hasta que la transacción
actual termine, así ninguna otra venta puede leer ese mismo producto hasta que se libere. Es
concurrencia a nivel de **base de datos**, no de hilos de Java, pero resuelve el mismo problema.
Se usa en `PedidoServiceImpl.savePedido()` y en los flujos de venta/abono.

### 1.5 Herramientas de sincronización en Java puro

| Herramienta | Para qué sirve | Cuándo usarla |
|---|---|---|
| `synchronized` | Bloque de código exclusivo — solo un hilo a la vez | Casos simples, pocos hilos |
| `volatile` | Garantiza que todos los hilos vean el valor más reciente de una variable (visibilidad, no atomicidad) | Flags de estado simples (ver 1.6) |
| `ReentrantLock` | Como `synchronized` pero con más control (`tryLock`, timeout, lock justo) | Cuando `synchronized` no alcanza |
| `AtomicInteger`/`AtomicLong`/`AtomicReference` | Operaciones atómicas sin bloquear (compare-and-swap) | Contadores, flags concurrentes |
| `ConcurrentHashMap` | Mapa seguro para concurrencia sin bloquear todo el mapa | Cachés en memoria compartidas entre hilos |

**Ejemplo real ya en este proyecto** (`LoginRateLimiterService.java`):
```java
private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
```
Usa `ConcurrentHashMap` porque varios requests de login llegan en paralelo y todos leen/escriben
ese mapa — con un `HashMap` normal esto podría corromperse o dar resultados inconsistentes bajo
carga.

**Ejemplo real de `volatile`** (`ReconciliacionImagenService.java`):
```java
private volatile ReconciliacionResultadoDto ultimoResultado;
private volatile boolean enProceso = false;
```
El proceso de reconciliación corre en un hilo `@Async` (ver 1.7), y otro endpoint público
consulta `isEnProceso()` desde el hilo del request HTTP. Sin `volatile`, el hilo que consulta
podría ver un valor "viejo" cacheado en su propio núcleo de CPU y nunca enterarse de que el
proceso ya terminó.

### 1.6 Pools de hilos — `ExecutorService`

Crear un `Thread` nuevo por cada tarea es caro (cada uno consume memoria de stack, ~1MB por
default) y no escala. La solución clásica es un **pool de hilos reciclables**:

```java
ExecutorService executor = Executors.newFixedThreadPool(10); // 10 hilos fijos, reciclados

executor.submit(() -> enviarCorreo(destinatario, asunto, html));

// Con resultado (Callable + Future)
Future<Boolean> resultado = executor.submit(() -> emailService.enviarTicket(correo, asunto, html));
boolean ok = resultado.get(5, TimeUnit.SECONDS); // espera hasta 5s, o lanza TimeoutException

executor.shutdown(); // SIEMPRE cerrar el pool cuando ya no se necesita
```

Tipos de pool más comunes (`Executors.*`):
- `newFixedThreadPool(n)` — n hilos fijos, cola ilimitada de tareas pendientes. El más usado.
- `newCachedThreadPool()` — crea hilos según demanda, los mata si están inactivos 60s. Peligroso
  bajo carga alta sin límite (puede crear miles de hilos).
- `newSingleThreadExecutor()` — 1 solo hilo, tareas en orden estricto (útil para colas
  secuenciales).
- `newScheduledThreadPool(n)` — para tareas repetidas/con delay (alternativa de bajo nivel a
  `@Scheduled` de Spring).

**Nota moderna:** desde Java 19, `Executors.newVirtualThreadPerTaskExecutor()` crea un hilo
virtual por tarea (ver 1.9) — sin límite práctico y sin el costo de memoria de hilos de SO reales.

### 1.7 `CompletableFuture` — componer tareas asíncronas

Es la forma moderna (desde Java 8) de encadenar trabajo asíncrono sin bloquear, y de correr
varias tareas en paralelo y esperar a que todas terminen.

```java
// Correr en paralelo y combinar resultados — ideal para un dashboard con varias queries
CompletableFuture<Long> ventasHoy = CompletableFuture.supplyAsync(() -> ventaService.ventasHoy());
CompletableFuture<Long> gastosHoy = CompletableFuture.supplyAsync(() -> gastoService.gastosHoy());
CompletableFuture<Long> stockBajo = CompletableFuture.supplyAsync(() -> varianteRepo.countStockBajo(5));

CompletableFuture.allOf(ventasHoy, gastosHoy, stockBajo).join(); // espera a que TODAS terminen

DashboardDto dto = new DashboardDto(ventasHoy.join(), gastosHoy.join(), stockBajo.join());
```

Si estas 3 queries tardan 200ms cada una y se corren **secuenciales** (como está hoy en
`DashboardServiceImpl`, casi seguro), el endpoint tarda ~600ms. Corriéndolas en paralelo con
`CompletableFuture`, tarda lo que tarde la más lenta de las 3 (~200ms) — porque las 3 esperan al
mismo tiempo, no una tras otra.

Métodos más usados de `CompletableFuture`:
- `supplyAsync(Supplier<T>)` — arranca una tarea que devuelve un valor.
- `runAsync(Runnable)` — arranca una tarea sin valor de retorno (ej. mandar un correo).
- `thenApply(Function)` — transforma el resultado cuando esté listo (sin bloquear).
- `thenCompose(Function)` — encadena otro `CompletableFuture` (evita futures anidados).
- `thenCombine(otro, BiFunction)` — combina el resultado de dos futures independientes.
- `exceptionally(Function)` — maneja errores sin `try/catch` tradicional.
- `.get()` / `.join()` — bloquea y espera el resultado (usar solo cuando ya no hay más trabajo
  async que hacer, típicamente al final).

### 1.8 Hilos en Spring Boot — `@Async` y `@Scheduled`

Spring envuelve los hilos de bajo nivel en anotaciones declarativas, así el código de negocio no
maneja `Thread`/`ExecutorService` directamente.

**`@Async`** — el método corre en otro hilo, el que lo llama sigue de inmediato sin esperar:
```java
@Async
public void enviarCorreoAsync(String destinatario, String asunto, String html) {
    emailService.enviarTicket(destinatario, asunto, html);
}
```
Requisitos para que funcione:
1. La clase debe ser un bean de Spring (`@Service`, `@Component`, etc.).
2. Debe estar activado `@EnableAsync` en algún `@Configuration` (en este proyecto ya está, en
   `MisProductosApplication.java`).
3. **El método debe ser llamado DESDE OTRO BEAN** — si un método de la misma clase llama a un
   método `@Async` de sí misma (`this.enviarCorreoAsync(...)`), Spring NO lo intercepta y corre
   síncrono igual. Esto pasa porque `@Async` funciona con un proxy que envuelve al bean, y
   llamarse a sí mismo (`this.algo()`) evita el proxy por completo. Es el error más común con
   esta anotación.
4. Puede devolver `void`, o `CompletableFuture<T>`/`Future<T>` si se necesita el resultado más
   adelante.

**`@Scheduled`** — corre un método en intervalos fijos o por cron, en un hilo aparte del pool de
Tomcat. Ya se usa bastante en este proyecto:

| Archivo | Configuración | Qué hace |
|---|---|---|
| `ChatSesionScheduler.java` | `fixedDelay = 300_000` (cada 5 min) | Limpieza de sesiones de chat |
| `ImagenScheduler.java` | `cron = "0 0 0 * * *"` (medianoche) | Tarea diaria de imágenes |
| `ImagenScheduler.java` | `cron = "0 0 4 * * *"` (4am) | Otra tarea diaria de imágenes |
| `NegocioAutoCierreScheduler.java` | `cron = "0 * * * * *"` (cada minuto) | Auto-cierre del negocio |
| `PedidoCancelacionScheduler.java` | `cron = "0 0 8 * * *"` (8am) | Cancelación automática de pedidos |
| `RifaScheduler.java` | `cron = "0 0 2 * * *"` (2am) | Procesamiento de rifa |

Diferencia clave `@Async` vs `@Scheduled`: `@Async` se dispara **on-demand** (alguien lo llama),
`@Scheduled` se dispara **solo, por tiempo**, sin que nadie lo invoque.

**Configurar el pool de hilos de `@Async`** — punto importante que hoy NO está configurado en
este proyecto: sin un `TaskExecutor` propio, Spring usa `SimpleAsyncTaskExecutor` por default,
que **crea un hilo nuevo por cada llamada, sin límite ni reciclaje**. Bajo carga alta esto puede
generar cientos de hilos simultáneos y tumbar el servidor. Se soluciona con un bean:

```java
@Configuration
public class AsyncConfig {
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(15);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("async-");
        executor.initialize();
        return executor;
    }
}
```

### 1.9 Lo más nuevo — Virtual Threads (Project Loom)

Desde **Java 21** (LTS, disponible desde 2023), existen los **hilos virtuales**: hilos manejados
por la JVM (no por el sistema operativo), extremadamente livianos — se pueden crear **millones**
sin agotar memoria, a diferencia de los hilos de SO tradicionales (limitados a miles).

```java
// Un hilo virtual por tarea, sin límite práctico
try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
    executor.submit(() -> emailService.enviarTicket(correo, asunto, html));
}
```

En Spring Boot 3.2+ (este proyecto ya está en **3.2.5**), se activa con una sola línea en el
`application.yml`, sin tocar código:
```yaml
spring:
  threads:
    virtual:
      enabled: true
```
Con esto, **cada request HTTP se atiende en un hilo virtual** en vez de un hilo real de Tomcat.
Si el código bloquea esperando el SMTP o una llamada HTTP a otro servicio, el hilo virtual se
"estaciona" sin ocupar un hilo de SO real — el servidor puede atender miles de requests
simultáneos bloqueantes sin agotar el pool, sin tener que reescribir el código a estilo reactivo
(`Mono`/`Flux`).

**⚠️ Requisito que hoy NO se cumple en este proyecto:** los hilos virtuales necesitan **Java 21**
como mínimo. El `pom.xml` actual fija `<java.version>17</java.version>` — para usar esto habría
que subir el JDK del proyecto (y del contenedor/servidor donde corre) a 21, probar que las
dependencias (Hibernate, RabbitMQ client, etc.) sean compatibles, y recién ahí activar la
propiedad de arriba. Es la opción más moderna, pero implica un cambio de plataforma, no solo de
código — anotarlo aparte como una mejora de infraestructura, no algo para "ya".

**Nota sobre "pinning":** hay un detalle a vigilar con hilos virtuales — si el código usa un
bloque `synchronized` (no `ReentrantLock`) alrededor de una operación bloqueante, el hilo virtual
queda "clavado" a un hilo real de SO durante ese bloque, perdiendo la ventaja. Java 24 corrigió
esto en gran parte, pero en 21-23 conviene revisar los `synchronized` existentes antes de migrar.

### 1.10 Ya existe un ejemplo de I/O no bloqueante real en este proyecto

`ChatbotService.java` usa `WebClient` (cliente HTTP reactivo de Spring) para llamar a la API de
OpenAI, **sin bloquear** con `.block()`:
```java
private Mono<String> llamarOpenAI(List<Map<String, String>> mensajes) { ... }
public Mono<String> chat(ChatbotRequest request) { ... }
```
Y el controller expone esto directamente:
```java
// ChatbotController.java
public Mono<ResponseEntity<Map<String, Object>>> enviarMensaje(...)
```
Esto significa que mientras se espera la respuesta de OpenAI (que puede tardar varios segundos),
el hilo de Tomcat **no queda bloqueado** — Spring libera el hilo y lo re-asigna cuando la
respuesta reactiva (`Mono`) esté lista. Es el mismo beneficio que dan los hilos virtuales, pero
logrado con programación reactiva en vez de con la JVM. Vale la pena usar este mismo patrón
(`WebClient` + `Mono`, sin `.block()`) para las llamadas al microservicio de imágenes, que hoy sí
son bloqueantes (ver Parte 2).

---

## Parte 2 — Qué endpoints de este proyecto conviene tocar, y cuáles no

### ✅ Buenos candidatos para hilos/async

| Dónde | Por qué SÍ conviene |
|---|---|
| `EmailService.enviarTicket/enviarCodigoVerificacion/enviarCodigoResetPassword` | Es I/O puro (llamada de red al SMTP), no depende de nada más en la transacción de BD, y el usuario no necesita esperar a que el correo salga para seguir usando la app. Es el caso más claro de todo el proyecto. |
| `WhatsappService.enviarMensaje` (CallMeBot) | Mismo argumento que el correo — llamada HTTP externa, resultado no crítico para continuar el flujo. |
| Dashboard (`GET /v1/dashboard/resumen`) | Si internamente hace varias queries independientes (ventas, gastos, stock bajo, créditos, etc.) de forma secuencial, correrlas con `CompletableFuture.allOf(...)` reduce la latencia total al tiempo de la query más lenta, no la suma de todas. |
| Reportes (`GET /v1/reportes/**`) | Mismo argumento que el dashboard si combinan varias agregaciones independientes. |
| Llamadas al microservicio de imágenes (`imageneClienteDisco`, `imagenPort`, `verificarExistentes`) | Es I/O de red a otro servicio. Hoy es bloqueante; convertirlo a `WebClient` reactivo (como ya se hizo en `ChatbotService`) liberaría el hilo mientras se espera la respuesta. Más laborioso que los otros casos porque el resultado sí se usa después en el mismo método (necesita orquestación con `Mono`/`CompletableFuture`, no es "fire and forget"). |
| `ReconciliacionImagenService` | Ya está bien hecho — correcto ejemplo de `@Async` para un proceso largo en background, con `volatile` para que otro hilo pueda consultar el estado (`isEnProceso()`) de forma segura. No tocar, usar como plantilla. |

### 🚫 NO conviene tocar

| Dónde | Por qué NO conviene |
|---|---|
| `PedidoServiceImpl.savePedido`, ventas, abonos (todo lo que descuenta stock) | Ya usan `@Lock(PESSIMISTIC_WRITE)` para evitar condiciones de carrera en el stock. Meterle async/paralelismo aquí sin cuidado rompería justo la garantía que ese lock protege — dos hilos podrían intentar tomar el lock de formas que generen deadlocks, o peor, que el descuento de stock se dispare fuera de la transacción que lo protege. Si algo se hace async aquí, tendría que ser DESPUÉS de que la transacción principal ya cerró (ej. solo el envío de notificación, no el guardado). |
| `LoginRateLimiterService` (Bucket4j en memoria) | Ya es rapidísimo (operación en memoria, sin I/O) — no hay nada que ganar paralelizando, y el rate-limit debe evaluarse síncrono antes de decidir si se permite continuar con el login. |
| Verificación de código (`ClienteServiceImpl.verificarCorreo`, `PasswordResetService.restablecerPassword`) | Son 1-2 queries de BD simples y rápidas — el overhead de coordinar un hilo aparte sería mayor que el tiempo que ahorra. Además el resultado (código válido o no) se necesita de inmediato para la respuesta HTTP, no hay nada que hacer "mientras tanto". |
| CRUDs simples (`ClienteControllerImpl`, `ProductosControllerImpl` guardar/actualizar/buscar) | Ya son rápidos (una o pocas queries), no hacen llamadas de red externas, y el cliente necesita el resultado inmediato para saber si su guardado tuvo éxito. |
| Login (`AuthController.login`) | Necesita el resultado síncrono (token JWT) para responder — no hay nada "de fondo" que hacer, y agregar async aquí solo sumaría complejidad sin beneficio. |

### Regla general para decidir

Antes de meterle hilos a algo, preguntarse:
1. **¿Es I/O-bound (red, disco, otro servicio) o CPU-bound (cálculo puro)?** — I/O-bound se
   beneficia mucho de async/hilos virtuales. CPU-bound casi nunca (salvo que se pueda paralelizar
   en varios núcleos, como el caso del dashboard con queries independientes).
2. **¿El que llama necesita el resultado para responder al cliente YA?** — si sí (login,
   verificar código, guardar y confirmar), no tiene sentido hacerlo async puro; como mucho se
   paraleliza internamente (`CompletableFuture.allOf`) pero se sigue esperando el conjunto.
3. **¿Ya hay una transacción de BD con locks (`@Transactional`, `PESSIMISTIC_WRITE`) alrededor?**
   — si sí, tener mucho cuidado: async dentro de una transacción no hereda el contexto
   transaccional del hilo original (otro gotcha real de `@Async` + `@Transactional` combinados).

---

## Próximos pasos sugeridos (sin implementar todavía)

1. Configurar un `TaskExecutor` propio (`ThreadPoolTaskExecutor`, ver 1.8) antes de usar más
   `@Async` — hoy cualquier `@Async` nuevo usaría el executor default sin límite de hilos.
2. Hacer `@Async` los 3 métodos de `EmailService` (o un wrapper que los llame async) — es el caso
   más claro y de menor riesgo.
3. Revisar si el dashboard/reportes ya paralelizan sus queries internas; si no, evaluar
   `CompletableFuture.allOf`.
4. Dejar Virtual Threads (1.9) anotado como mejora de infraestructura a futuro — requiere subir
   el proyecto de Java 17 a Java 21 primero, no es un cambio de una sola línea todavía.
