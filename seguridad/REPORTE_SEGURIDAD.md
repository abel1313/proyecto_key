# Reporte de Seguridad — Proyecto Key New
**Fecha:** 2026-05-19
**Spring Boot:** 3.2.5 | Java 17 | MySQL + Redis + RabbitMQ

---

## Resumen Ejecutivo

| Severidad | Cantidad | Estado |
|-----------|----------|--------|
| CRÍTICA   | 3        | 0 corregidas, 3 pendientes (requieren acción manual) |
| ALTA      | 7        | 0 corregidas, 7 pendientes |
| MEDIA     | 6        | 2 corregidas, 4 pendientes |
| BAJA      | 4        | 0 corregidas, 4 pendientes |

> Las vulnerabilidades CRÍTICAS deben atenderse **antes del próximo despliegue a producción**.

---

## Vulnerabilidades Encontradas

---

### [SEG-001] CRÍTICA — Credenciales de base de datos reales en Git

- **Categoría:** Credenciales en código fuente
- **Archivo:** `src/main/resources/application-dev.yml` líneas 34–37
- **Código vulnerable:**
```yaml
username: user_ventas_qa
password: Mexico#2026
# URL apunta a IP pública: jdbc:mysql://51.178.29.99:3306/inventario_key_qa
```
- **Impacto:** Cualquier persona con acceso al repositorio puede conectarse directamente a MySQL, leer todos los datos de clientes, pedidos y productos, o destruirlos.
- **Estado:** PENDIENTE — requiere acción manual urgente
- **Solución:**
  1. Invalidar la contraseña en MySQL: `ALTER USER 'user_ventas_qa'@'%' IDENTIFIED BY 'nueva_clave_aleatoria';`
  2. Reemplazar en `application-dev.yml`:
  ```yaml
  username: ${SPRING_DATASOURCE_USERNAME}
  password: ${SPRING_DATASOURCE_PASSWORD}
  ```
  3. Usar un archivo `.env` local (no versionado en Git) para desarrollo.
  Esto si hay que agregarlo par no volver a subir las credenciles?
---

### [SEG-002] CRÍTICA — Clave JWT hardcodeada en Git

- **Categoría:** Secreto criptográfico en código fuente
- **Archivos:** `application.yml` línea 67 y `application-dev.yml` línea 57
- **Código vulnerable:**
```yaml
clave-seguridad:
  clave: miClaveSuperSeguraDe32Caracteres
```
- **Impacto:** Con esta clave (que está en Git), cualquier atacante puede forjar JWTs con `ROLE_ADMIN` y acceder a todos los endpoints de administración sin credenciales.
- **Estado:** PENDIENTE — requiere acción manual urgente
- **Solución:**
  1. Generar clave aleatoria: `openssl rand -base64 64`
  2. Reemplazar en ambos archivos:
  ```yaml
  clave-seguridad:
    clave: ${TOKEN_JWT}
  ```
  3. Agregar `TOKEN_JWT=<nueva_clave>` en las variables de entorno del VPS (K8s Secrets).
  4. **Todos los tokens JWT existentes quedarán inválidos** (comportamiento esperado — todos los usuarios deberán volver a iniciar sesión).

---

### [SEG-003] CRÍTICA — Endpoint de prueba RabbitMQ público y sin autenticación

- **Categoría:** Control de acceso roto
- **Archivo:** `security/SecurityConfig.java`
- **Código vulnerable:**
```java
.requestMatchers(HttpMethod.GET, "/admin/test-rabbit").permitAll()
```
- **Impacto:** Cualquier visitante puede inundar la cola RabbitMQ y descubrir la arquitectura interna del sistema.
- **Estado:** PENDIENTE
- **Solución:** Eliminar la línea del `permitAll()`. El endpoint ya queda cubierto por la regla general `"/admin/**"` que requiere `ROLE_ADMIN`. Idealmente, eliminar el endpoint completo de `AdminController` ya que es solo de prueba.
- **Nota:** hay que eliminarlo, solo era para pruebas
  > **Respuesta:** Correcto. Se elimina el método completo de `AdminController` y la línea del `permitAll()` en SecurityConfig.

> **🔧 CAMBIO PENDIENTE:** Eliminar método `testRabbit()` de `AdminController.java` y eliminar su línea `permitAll()` de `SecurityConfig.java`. Solo era de prueba, ya no se usará.

---

### [SEG-004] ALTA — Registro público de usuarios sin verificación

- **Categoría:** Control de acceso roto
- **Archivo:** `security/SecurityConfig.java`
- **Detalle:** `/auth/registrar` es público. Cualquier bot puede crear miles de cuentas con acceso a endpoints `authenticated()`.
- **Estado:** PENDIENTE
- **DECISIÓN FINAL: (B) Verificación de email** — el cliente se registra solo, el backend manda un email con un código/link, la cuenta queda inactiva hasta confirmar. Se implementa con Spring Mail.

> **🔧 CAMBIO PENDIENTE:** Implementar opción B:
> - Agregar campo `emailVerificado boolean` a la entidad `Usuario` (default `false`).
> - Al registrar: guardar con `emailVerificado = false` y enviar email con token UUID.
> - Crear endpoint `/auth/verificar-email?token=XXX` que active la cuenta.
> - En el login: si `emailVerificado = false`, retornar 403 con mensaje "Debes verificar tu correo antes de iniciar sesión".
> - Crear tabla `email_verificacion_token (id, usuarioId, token, expiracion)`.

---

### [SEG-005] ALTA — Refresh token puede usarse como access token

- **Categoría:** Confusión de tokens JWT
- **Archivo:** `jwt/JwtUtil.java`
- **Detalle:** `validateToken()` no verifica el claim `type`. Un refresh token (7 días de vida) puede usarse como Bearer token en Authorization para autenticarse en cualquier endpoint.
- **Estado:** PENDIENTE
- **Solución:**
```java
public boolean validateToken(String token, UserDetails userDetails) {
    try {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSecretKey()).build()
                .parseClaimsJws(token).getBody();
        // Rechazar si es refresh token
        if ("refresh".equals(claims.get("type"))) return false;
        return claims.getSubject().equals(userDetails.getUsername());
    } catch (JwtException e) {
        return false;
    }
}
```
- **Nota:** hay que agregar la validacion para que ya no pase o que no sea el mismo token y sean diferentes como dices no?
  > **Respuesta:** Sí. Con esa validación el access token y el refresh token quedan funcionalmente separados — si alguien intenta usar el refresh token como si fuera un access token, el filtro lo rechaza.

> **🔧 CAMBIO PENDIENTE:** Agregar la validación `if ("refresh".equals(claims.get("type"))) return false;` en `JwtUtil.validateToken()`.

---

### [SEG-006] ALTA — Sin validación de tipo MIME ni tamaño en uploads de imágenes

- **Categoría:** Subida de archivos sin restricciones
- **Archivos:** `service/VarianteServiceImpl.java`, `service/ProductosServiceImpl.java`
- **Detalle:** Se acepta cualquier tipo de archivo y cualquier tamaño. Solo se verifica la extensión del nombre (bypasseable).
- **Estado:** PENDIENTE
- **Solución:**
```java
private static final long MAX_SIZE = 10 * 1024 * 1024; // 10 MB
private static final Set<String> TIPOS_VALIDOS = Set.of("image/jpeg","image/png","image/gif","image/webp");

private void validarImagen(MultipartFile file) {
    if (file.getSize() > MAX_SIZE)
        throw new IllegalArgumentException("Archivo '" + file.getOriginalFilename() + "' supera 10 MB");
    String tipo = file.getContentType();
    if (tipo == null || !TIPOS_VALIDOS.contains(tipo.toLowerCase()))
        throw new IllegalArgumentException("Tipo no permitido: " + tipo + ". Solo JPG, PNG, GIF, WebP");
}
```
- **Nota:** hay que agregar la validacion para que como maximo sean 10 mb como dices
  > **Respuesta:** Correcto, se agrega el método `validarImagen()` en los dos servicios mencionados. Limita a 10 MB y verifica el tipo MIME real del archivo, no solo la extensión del nombre.

> **📌 Nota — ¿Se puede usar guardado asíncrono para imágenes?**
>
> Sí, tiene sentido y es buena práctica. La validación (tipo MIME, tamaño) debe seguir siendo **síncrona** para poder devolver el error al usuario si manda un archivo inválido. Pero la parte lenta — subir el archivo al microservicio de imágenes — sí se puede hacer asíncrona con `@Async`:
> - El endpoint responde inmediatamente al admin que el producto se guardó.
> - En segundo plano, Spring lanza un hilo que sube las imágenes al micro.
> - Si falla la subida, se loguea el error (ya está funcionando así parcialmente en `ProductosServiceImpl` con el try-catch que no bloquea el guardado).
> El `@Async` ya existe en el proyecto (si hay `@EnableAsync`), solo habría que mover la llamada al micro dentro de un método `@Async`.

> **🔧 CAMBIO PENDIENTE:** Implementar `validarImagen()` en la carga de imágenes de `VarianteServiceImpl` y `ProductosServiceImpl`, antes de procesar cada archivo. Evaluar mover la llamada al micro externo a un método `@Async`.

---

### [SEG-007] ALTA — Sin límite en parámetros de paginación

- **Categoría:** Validación de entrada insuficiente
- **Archivos:** `ProductosControllerImpl.java`, `VarianteController.java`, `ClienteControllerImpl.java` y más
- **Código vulnerable:**
```java
@RequestParam int size, @RequestParam int page
// size=999999 ejecuta consulta con millones de registros
```
- **Impacto:** Una sola petición anónima con `size=999999` puede agotar la memoria del JVM.
- **Estado:** PENDIENTE
<<<<<<< HEAD
- **Solución:** En cada endpoint con paginación aplicar:
```java
int safeSize = Math.min(Math.max(size, 1), 100); // mín 1, máx 100
int safePage = Math.max(page, 1);
```
- **Nota:** hay que limitar a que sean de 10 en 10 ya que así es como estoy paginando como máximo
  > **Respuesta:** Entonces el límite queda en 10, no 100. El código sería:
  > ```java
  > int safeSize = Math.min(Math.max(size, 1), 10); // mín 1, máx 10
  > int safePage = Math.max(page, 1);
  > ```
=======

> **🔧 CAMBIO PENDIENTE:** La paginación del proyecto es de 10 en 10 como máximo. Aplicar en cada endpoint con paginación:
> ```java
> int safeSize = Math.min(Math.max(size, 1), 10); // mín 1, máx 10
> int safePage = Math.max(page, 0);
> ```
>>>>>>> dev

---

### [SEG-008] ALTA — Endpoint buscarClientePorIdUsuario público permite enumeración

- **Categoría:** IDOR / Exposición de información
- **Archivo:** `security/SecurityConfig.java`
- **Código vulnerable:**
```java
.requestMatchers("/usuarios/buscarClientePorIdUsuario/**").permitAll()
```
- **Impacto:** Cualquier visitante anónimo puede enumerar qué IDs de usuario tienen cliente asociado iterando IDs del 1 al 9999.
- **Estado:** PENDIENTE
<<<<<<< HEAD
- **Solución:** Cambiar de `permitAll()` a `authenticated()`.
- **Nota:** Entonces aquí solo para los que estén autenticados, ya que el cliente puede buscar su ID, ¿o eso también es peligroso ya que si está autenticado podría buscar cualquier ID no?
  > **Respuesta:** Exacto, tienes razón. Poner `authenticated()` es el primer paso pero no es suficiente — cualquier usuario autenticado podría buscar los datos de otro cliente cambiando el ID en la URL (eso se llama **IDOR**). La solución correcta es que el endpoint no reciba el ID como parámetro, sino que lo lea directamente del JWT:
  > ```java
  > String username = SecurityContextHolder.getContext().getAuthentication().getName();
  > ```
  > Así cada usuario solo puede ver sus propios datos, sin importar qué ID escriba.
> -seguimos con duas si, lo que quiero es que no porque este autenticado pueda ver los datos de los demas usuario ese cambio hay que hacerlo asi para que no pueda ver los datos de otros
=======

> **📌 Nota:** Sí, exactamente. Con `authenticated()`, el endpoint solo es accesible si el usuario ya inició sesión y tiene un JWT válido. Un usuario registrado y logueado sí puede buscar su propio ID de cliente. El cambio inmediato es pasar de `permitAll()` a `authenticated()`.
>
> Hay un segundo problema a futuro: un usuario logueado podría buscar el ID de **otro** usuario (IDOR). Para corregirlo completamente habría que verificar en el controlador que el `idUsuario` del request coincide con el del JWT, o que tiene `ROLE_ADMIN`. Pero como primer paso, el `authenticated()` ya elimina el acceso anónimo.

> **🔧 CAMBIO PENDIENTE:** Cambiar en `SecurityConfig.java` de `.permitAll()` a `.authenticated()` para `/usuarios/buscarClientePorIdUsuario/**`. Después evaluar agregar validación de que el usuario solo puede buscar su propio ID.
>>>>>>> dev

---

### [SEG-009] MEDIA — Logs del chatbot registran mensaje completo del usuario

- **Categoría:** Violación de privacidad / Datos sensibles en logs
- **Archivo:** `controller/ChatbotController.java`
- **Código vulnerable:**
```java
log.info("Chatbot - IP: {}, mensaje: {}", ip, request.getMensaje());
```
- **Solución:**
```java
log.info("Chatbot - IP: {}, longitud mensaje: {}", ip, request.getMensaje().length());
```
<<<<<<< HEAD
- **Nota:** hay que corregirlos
  > **Respuesta:** Sí. Se cambia esa línea en `ChatbotController.java` — queda registrada solo la longitud del mensaje, no el contenido privado del usuario.
=======
- **Estado:** PENDIENTE

> **🔧 CAMBIO PENDIENTE:** Cambiar el `log.info` en `ChatbotController.java` para que solo registre la longitud del mensaje, no el contenido.
>>>>>>> dev

---

### [SEG-010] MEDIA — Headers de seguridad HTTP no configurados explícitamente

- **Categoría:** Configuración de seguridad incompleta
- **Archivo:** `security/SecurityConfig.java`
<<<<<<< HEAD
- **Detalle:** No se configura CSP (Content-Security-Policy) ni HSTS (Strict-Transport-Security).
- **Solución:** En `filterChain()`, agregar:
```java
.headers(headers -> headers
    .frameOptions(opt -> opt.deny())
    .contentTypeOptions(Customizer.withDefaults())
    .httpStrictTransportSecurity(hsts -> hsts
        .includeSubDomains(true)
        .maxAgeInSeconds(31536000))
)
```
- **Nota:** esto no lo entiendo, ¿a qué se refiere?
  > **Respuesta:** Son instrucciones que el servidor envía al navegador para que se proteja:
  > - **HSTS:** Le dice al navegador "nunca uses HTTP para este dominio, siempre HTTPS". Evita que alguien intercepte la conexión bajando de HTTPS a HTTP.
  > - **frameOptions(deny):** Evita que tu app se muestre dentro de un `<iframe>` en otro sitio. Se usa en ataques de *clickjacking* donde hacen que el usuario haga clic en botones invisibles.
  > - **contentTypeOptions:** Evita que el navegador ejecute un archivo malicioso que fue subido disfrazado de imagen.
    -Hay que implementar ese cambio
=======
- **Estado:** PENDIENTE

> **📌 Nota — ¿Qué son estos headers HTTP de seguridad?**
>
> Son instrucciones que el backend manda en cada respuesta HTTP para decirle al navegador cómo protegerse. Spring Security ya activa algunos por defecto, pero estos son los que faltan:
>
> - **`frameOptions().deny()`** — Prohíbe que tu sitio se cargue dentro de un `<iframe>` en otra página. Evita ataques de *clickjacking* (una página maliciosa pone tu app en un iframe invisible y engaña al usuario para que haga clic sin saberlo).
> - **`contentTypeOptions`** — Le dice al navegador que no intente "adivinar" el tipo de un archivo que descarga. Sin este header, el navegador podría interpretar un archivo subido como HTML y ejecutar scripts. Con el header activado, respeta el `Content-Type` declarado.
> - **`HSTS (Strict-Transport-Security)`** — Le dice al navegador que para ese dominio siempre use HTTPS, aunque el usuario escriba `http://`. El navegador lo recuerda por 1 año (31536000 segundos). Evita ataques donde alguien intercepta el tráfico HTTP antes de que se redirija a HTTPS.
>
> La configuración a agregar en `SecurityConfig.java` dentro del `filterChain`:
> ```java
> .headers(headers -> headers
>     .frameOptions(opt -> opt.deny())
>     .contentTypeOptions(Customizer.withDefaults())
>     .httpStrictTransportSecurity(hsts -> hsts
>         .includeSubDomains(true)
>         .maxAgeInSeconds(31536000))
> )
> ```

> **🔧 CAMBIO PENDIENTE:** Agregar la configuración de headers en `SecurityConfig.java`.

>>>>>>> dev
---

### [SEG-011] MEDIA — AuthRequest sin límite de longitud en campos

- **Categoría:** Validación de entrada insuficiente
- **Archivo:** `src/main/java/com/ventas/key/mis/productos/models/AuthRequest.java`
<<<<<<< HEAD
- **Solución aplicada:** Agregados `@Size` en `userName` (3-100 chars), `password` (6-200 chars) y `@Email` + `@Size` en `email`.
- **Nota:** hay que agregar la validacion
  > **Respuesta:** Ya está aplicada en el código — se verificó que `@Size` y `@Email` están correctamente en `AuthRequest.java`. Este punto ya está resuelto.
=======
- **Estado:** PENDIENTE (documentado como corregido pero falta verificar la implementación y los mensajes)

> **🔧 CAMBIO PENDIENTE:** Verificar que las anotaciones `@Size` y `@Email` estén implementadas en `AuthRequest.java`. Agregar mensajes en español a todas las validaciones:
> ```java
> @Size(min = 3, max = 100, message = "El nombre de usuario debe tener entre 3 y 100 caracteres")
> private String userName;
>
> @Size(min = 6, max = 200, message = "La contraseña debe tener entre 6 y 200 caracteres")
> private String password;
>
> @Email(message = "El correo no tiene un formato válido")
> @Size(max = 150, message = "El correo no puede superar 150 caracteres")
> private String email;
> ```
>>>>>>> dev

---

### [SEG-012] MEDIA — ChatbotRequest sin límite en historial de mensajes

- **Categoría:** Validación de entrada / Ataque de costo
- **Archivo:** `chatbot/ChatbotRequest.java`
- **Detalle:** El historial puede tener mensajes ilimitados de longitud ilimitada, generando llamadas masivas y costosas a la API de OpenAI.
- **Estado:** PENDIENTE
- **Solución:**
```java
@Size(max = 20, message = "El historial no puede superar 20 mensajes")
private List<@Valid MensajeHistorial> historial;

// En MensajeHistorial:
@Size(max = 2000, message = "El mensaje no puede superar 2000 caracteres")
private String contenido;
```
- **Nota:** hay que agregar la validacion
  > **Respuesta:** Sí. Se agregan las anotaciones `@Size` en `ChatbotRequest` y en `MensajeHistorial`. Esto evita que alguien mande un historial enorme que genere una llamada costosa a OpenAI.

> **📌 Nota — ¿Cómo se probarían estos cambios?**
>
> Una vez implementadas las validaciones, las pruebas serían:
> 1. **Prueba 1:** Enviar al endpoint del chatbot un historial con 21 objetos → debe responder HTTP 400 con el mensaje "El historial no puede superar 20 mensajes".
> 2. **Prueba 2:** Enviar un mensaje con más de 2000 caracteres en `contenido` → debe responder HTTP 400 con "El mensaje no puede superar 2000 caracteres".
> 3. **Prueba 3 (happy path):** Enviar un historial con 5 mensajes cortos → debe funcionar normalmente con HTTP 200.
> Se pueden hacer desde Postman, Swagger o escribir un test de integración con `@SpringBootTest` + `MockMvc`.

> **🔧 CAMBIO PENDIENTE:** Implementar `@Size(max = 20)` en el historial de `ChatbotRequest` y `@Size(max = 2000)` en `contenido` de `MensajeHistorial`. Asegurarse de que `@Valid` esté en el parámetro del controller para que las validaciones se activen.

---

### [SEG-013] MEDIA — Swagger habilitado en perfil Docker (producción)

- **Categoría:** Exposición de información
- **Archivo:** `src/main/resources/application-docker.yml`
<<<<<<< HEAD
- **Solución:** Agregar al perfil docker:
```yaml
springdoc:
  swagger-ui:
    enabled: false
  api-docs:
    enabled: false
```
- **Nota:** no sé a qué se refiere, ¿me explicas?
  > **Respuesta:** Swagger es una interfaz web que se genera automáticamente y documenta toda tu API. Si alguien entra a `http://tu-servidor/swagger-ui.html` en producción, ve la lista completa de todos tus endpoints, parámetros y respuestas — básicamente un mapa del sistema para un atacante. Es útil en desarrollo pero hay que apagarlo en producción con esas dos líneas en el yml de docker.
    -hay que corregirlo
=======
- **Estado:** PENDIENTE

> **📌 Nota:** Sí, esa es la solución completa. Agregar esas dos propiedades al perfil `application-docker.yml` deshabilita tanto la interfaz visual de Swagger (`/swagger-ui.html`) como el JSON con la definición de la API (`/v3/api-docs`). Así nadie desde internet puede ver la documentación de los endpoints en producción. También se debería revisar si el perfil QA (`application-qa.yml`) tiene Swagger habilitado y decidir si se quiere deshabilitar también allí.

> **🔧 CAMBIO PENDIENTE:** Agregar en `application-docker.yml`:
> ```yaml
> springdoc:
>   swagger-ui:
>     enabled: false
>   api-docs:
>     enabled: false
> ```
> Evaluar si también se desactiva en `application-qa.yml`.

>>>>>>> dev
---

### [SEG-014] MEDIA — ExceptionGlobal catch-all exponía mensaje interno ✅ CORREGIDO

- **Categoría:** Exposición de información en errores
- **Solución aplicada:** El catch-all ahora devuelve "Error interno del servidor" genérico. El detalle completo solo va al log interno con `log.error`.

---

### [SEG-015] BAJA — Rate limiter de login en memoria (no persiste entre reinicios)

- **Categoría:** Riesgo arquitectural
- **Archivo:** `service/LoginRateLimiterService.java`
- **Detalle:** `ConcurrentHashMap` en memoria. Los contadores se resetean al reiniciar el pod. En multi-instancia cada pod tiene su propio contador.
- **Solución futura:** Migrar a Bucket4j con Redis como backend.
- **Nota:** no entiendo a qué se refiere, ¿me explicas?
  > **Respuesta:** El sistema que bloquea IPs cuando fallan el login varias veces guarda los contadores en la RAM del servidor. Tiene dos problemas:
  > 1. **Reinicio:** Si el servidor se reinicia, los contadores se borran y una IP bloqueada puede intentar de nuevo inmediatamente.
  > 2. **Múltiples instancias:** Si hay 2 pods en Kubernetes, un atacante puede hacer 4 intentos en el pod A y 4 en el pod B sin que ninguno lo bloquee, porque cada pod tiene su propio contador.
  > La solución es guardar los contadores en Redis, que es compartido entre todos los pods y persiste aunque el servidor se reinicie.
    - hay que agregar esa mejora
---

### [SEG-016] BAJA — Bloqueo de IPs del chatbot tampoco persiste

- **Categoría:** Riesgo arquitectural
<<<<<<< HEAD
- **Misma solución que SEG-015:** Migrar `ipInfoMap` a Redis.
- **Nota:** no sé a qué se refiere
  > **Respuesta:** Mismo problema que SEG-015 pero para el chatbot. El mapa que recuerda qué IPs están bloqueadas por enviar mensajes incomprensibles está en RAM. Al reiniciar el servidor, todas las IPs bloqueadas quedan libres automáticamente. La solución es moverlo a Redis igual que el rate limiter.
> Hay que corregirlo tambien
=======

> **📌 Nota — Explicación detallada:**
>
> El chatbot tiene una variable en memoria (`ipInfoMap`, un `ConcurrentHashMap`) que registra cuántos mensajes confusos mandó cada IP y si está bloqueada. El problema es que esa información existe solo mientras la aplicación está corriendo:
> - Si la aplicación se **reinicia** (nuevo deploy, caída del pod, etc.), ese mapa se vacía. Una IP que estaba bloqueada queda libre automáticamente.
> - Si hay **varios pods** corriendo al mismo tiempo (escalado horizontal en Kubernetes), cada pod tiene su propia copia del mapa. Entonces una IP bloqueada en el pod 1 no está bloqueada en el pod 2, y puede mandar mensajes sin límite cambiando de pod.
>
> La solución es guardar esa información en **Redis** con un TTL (tiempo de expiración). Redis es compartido por todos los pods y persiste entre reinicios. Así el bloqueo de 6 horas funciona correctamente sin importar cuántas instancias corran.

- **Solución futura:** Migrar `ipInfoMap` a Redis con TTL de 6 horas.
>>>>>>> dev

---

### [SEG-017] BAJA — Endpoint de simulación de pago sin doble capa de seguridad

- **Categoría:** Defensa en profundidad insuficiente
<<<<<<< HEAD
- **Solución:** Agregar `@PreAuthorize("hasRole('ADMIN')")` al método de simulación en `MercadoPagoController`.
- **Nota:** ¿cómo sería 2 capas de seguridad?
  > **Respuesta:**
  > - **Capa 1 (SecurityConfig):** La regla general en la configuración HTTP que dice que `/admin/**` requiere `ROLE_ADMIN`.
  > - **Capa 2 (@PreAuthorize en el método):** La anotación directamente en el método del controlador que también verifica el rol.
  > Si por error de un refactor el endpoint se mueve fuera de `/admin/`, la capa 2 sigue protegiendo. Son dos barreras independientes — si una falla, la otra cubre.
-hayq ue agregar esa capa de seguridad
=======

> **📌 Nota — ¿Qué es la doble capa de seguridad?**
>
> El sistema ya tiene una primera capa de seguridad en `SecurityConfig.java` que controla qué rutas requieren autenticación. La "doble capa" significa agregar `@PreAuthorize("hasRole('ADMIN')")` directamente en el método del controlador, como segunda barrera independiente.
>
> ¿Por qué es útil? Si alguien en el futuro agrega ese endpoint a una ruta pública por error en `SecurityConfig.java`, la anotación `@PreAuthorize` en el método lo seguiría bloqueando. Son dos cerraduras independientes: aunque abras una por error, la otra sigue cerrada. Es lo que se llama "defensa en profundidad" — no depender de un solo mecanismo de seguridad.

> **🔧 CAMBIO PENDIENTE:** Agregar `@PreAuthorize("hasRole('ADMIN')")` al método de simulación de pago en `MercadoPagoController`.

>>>>>>> dev
---

### [SEG-018] BAJA — JJWT versión 0.11.5 con API deprecated

- **Categoría:** Dependencia desactualizada
<<<<<<< HEAD
- **Solución:** Actualizar a JJWT 0.12.x en `pom.xml`.
- **Nota:** Hay que actualizar la dependencia que sea compatible
  > **Respuesta:** Sí. Se actualiza en `pom.xml` de `0.11.5` a `0.12.x`. La API cambió entre versiones, así que también hay que ajustar el código en `JwtUtil.java` (algunos métodos de 0.11 fueron eliminados en 0.12).
    -hay que hacer los cambios
=======
- **Estado:** PENDIENTE

> **🔧 CAMBIO PENDIENTE:** Actualizar JJWT a 0.12.x en `pom.xml`. La API cambió entre versiones — los cambios necesarios en el código serían:
> ```xml
> <!-- pom.xml: cambiar de 0.11.5 a -->
> <version>0.12.6</version>
> ```
> ```java
> // JwtUtil.java — cambios de API:
> // Antes: Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token)
> // Ahora: Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token)
>
> // Antes: Keys.hmacShaKeyFor(secret.getBytes())  →  sigue igual
> // Antes: .getBody()  →  ahora .getPayload()
> ```
> Todos los archivos que usen JJWT (`JwtUtil.java`, `JwtAuthenticationFilter.java`) necesitan actualizarse con la nueva API.

>>>>>>> dev
---

## Buenas Prácticas Detectadas ✅

El proyecto ya implementa correctamente:

1. **Cookies HttpOnly** — El refresh token usa `Set-Cookie` con `HttpOnly` y `Secure`, evitando robo vía XSS
2. **Rate limiting en login** — 5 intentos por IP cada 15 minutos con Bucket4j
3. **Bloqueo de IPs en chatbot** — 3 mensajes incomprensibles → bloqueo de 6 horas
4. **BCryptPasswordEncoder** — Contraseñas correctamente hasheadas
5. **@JsonIgnore en password** — La entidad Usuario nunca serializa la contraseña
6. **CORS restrictivo** — Lista explícita de 8 orígenes conocidos, sin wildcard, con `allowCredentials` correcto
7. **CSRF deshabilitado correctamente** — Stateless JWT + SameSite cookie, no se necesita CSRF
8. **Sin SQL Injection** — Todos los repositorios usan parámetros nombrados JPA (`:param`). Queries con `LIKE CONCAT('%', :buscar, '%')` correctamente parametrizadas
9. **Perfil prod usa 100% variables de entorno** — Ninguna credencial hardcodeada en perfiles QA/Docker
10. **SessionManagement STATELESS** — Sin HttpSession
11. **`@EnableMethodSecurity`** — Activa `@PreAuthorize` como segunda capa de seguridad

- **Nota:** ¿a qué se refiere con el número 5?
  > **Respuesta:** La entidad `Usuario` en Java tiene un campo `password` que guarda el hash BCrypt (algo como `$2a$10$...`). Sin `@JsonIgnore`, si algún endpoint devuelve un objeto `Usuario` como respuesta JSON, ese hash iría incluido en la respuesta. Con `@JsonIgnore` encima del campo, Spring lo excluye automáticamente de toda serialización — ningún endpoint puede filtrarlo accidentalmente.
    -hay que corregirlo
---

## Recomendaciones Adicionales

1. **JWT Blacklist en Redis al hacer logout**

> **📌 Nota — Explicación:**
> Actualmente cuando un usuario hace logout, el access token (válido 15 minutos) sigue funcionando hasta que expire naturalmente. No hay forma de invalidarlo antes. Con una blacklist en Redis:
> - Al hacer logout, el backend extrae el `jti` (ID único del token, que ya está en el claim del JWT) y lo guarda en Redis con un TTL igual al tiempo que le queda antes de expirar.
> - En `JwtAuthenticationFilter`, antes de aceptar cualquier token, se verifica si su `jti` está en la blacklist de Redis. Si está → rechazado, aunque la firma sea válida.
> - Resultado: el logout es inmediato y real, no hay ventana de 15 minutos donde el token robado siga funcionando.
>
> **🔧 CAMBIO PENDIENTE:** Implementar blacklist JWT en Redis: guardar `jti` al hacer logout, verificar en el filtro antes de autenticar.

2. **Trusted proxies para X-Forwarded-For**

> **📌 Nota — Explicación:**
> El rate limiter del login usa la IP del cliente para bloquear intentos fallidos. Esa IP se lee del header `X-Forwarded-For` que manda nginx/Traefik cuando actúa de proxy. El problema: cualquier cliente puede poner ese header manualmente (forjarlo) y evadir el bloqueo con IPs falsas.
>
> La solución segura es decirle a Spring que confíe en `X-Forwarded-For` **solo si la petición llega desde el proxy conocido** (nginx en el mismo servidor). Esto se hace con:
> ```yaml
> # application-docker.yml / application-qa.yml
> server:
>   forward-headers-strategy: NATIVE
> ```
> Y en nginx configurar que solo él ponga el header `X-Forwarded-For`. Con esto, un cliente externo no puede falsificar su IP porque nginx sobreescribe el header.
>
> **🔧 CAMBIO PENDIENTE:** Agregar `server.forward-headers-strategy: NATIVE` en los YMLs de producción y QA.

3. **Límite de sesión absoluta (30 días)**

> **📌 Nota — El cambio:**
> Actualmente un refresh token se puede renovar indefinidamente. Un usuario que inició sesión hace 2 años sigue con su sesión activa porque cada vez que el token de 7 días expira, el frontend pide uno nuevo. No hay un límite absoluto.
>
> La mejora: al generar el refresh token, incluir en sus claims la fecha de inicio de sesión (`sessionStart`). En el endpoint `/auth/refresh`, verificar:
> ```java
> Date sessionStart = claims.get("sessionStart", Date.class);
> if (System.currentTimeMillis() - sessionStart.getTime() > 30L * 24 * 3600 * 1000) {
>     throw new RuntimeException("Sesión expirada, vuelve a iniciar sesión");
> }
> ```
> Así aunque el usuario siga renovando el refresh token, a los 30 días de la sesión original se le pide que vuelva a autenticarse con usuario y contraseña.
>
> **🔧 CAMBIO PENDIENTE:** Agregar claim `sessionStart` al generar refresh token en `JwtUtil.java`. Verificar en `AuthController.refresh()` que no supere 30 días.

4. **Verificar que `PruebaControllerImpl` no esté expuesto en producción**

> **📌 Nota — Cómo se haría:**
> Buscar en el proyecto la clase `PruebaControllerImpl` (o `PruebaController`). Verificar:
> 1. Su `@RequestMapping` — ¿qué ruta maneja?
> 2. En `SecurityConfig.java` — ¿esa ruta está en `permitAll()` o en `authenticated()`?
> 3. Si está públicamente accesible y tiene lógica de prueba, agregar `@Profile("!docker")` a la clase para que no se cargue en producción, o eliminarla si ya no se usa.
>
> **🔧 CAMBIO PENDIENTE:** Revisar y asegurar que `PruebaControllerImpl` no esté accesible en el perfil Docker.

5. **Rotar API key de OpenAI** ✅ Validado — verificar que nunca se commiteó en Git.

- **Nota:** ¿a qué se refieren las recomendaciones o cómo se hacen o por qué las mencionaste?
  > **Respuesta:**
  > 1. **JWT Blacklist:** Al hacer logout hoy el access token sigue siendo válido 15 min aunque el usuario cerró sesión. Si alguien lo robó puede usarlo esos 15 min. Guardar el token en Redis al hacer logout lo invalida inmediatamente.
  > 2. **X-Forwarded-For:** Si tu app está detrás de nginx, la IP que llega al rate limiter es la de nginx, no la del cliente real. Podría terminar bloqueando a todos los usuarios en vez del atacante. Esa configuración en el yml hace que Spring lea la IP real.
  > 3. **Refresh token indefinido:** El refresh de 7 días se puede renovar para siempre. Si alguien roba el token y lo sigue renovando, tiene acceso de por vida. Un límite de 30 días cierra esa ventana.
  > 4. **PruebaControllerImpl:** Similar al endpoint de RabbitMQ — verificar que no esté accesible en producción.
  > 5. **API key OpenAI:** Si alguna vez se guardó la clave real en un commit, sigue en el historial de Git aunque se haya borrado del archivo. Hay que verificarlo y rotar la clave en OpenAI.
    - hay que realizar estos cambios para mejorarlo

---

## Archivos a Revisar y Modificar

| Archivo | Vulnerabilidades | Prioridad |
|---------|-----------------|-----------|
| `src/main/resources/application-dev.yml` | SEG-001, SEG-002 | 🔴 URGENTE |
| `src/main/resources/application.yml` | SEG-002 | 🔴 URGENTE |
| `security/SecurityConfig.java` | SEG-003, SEG-004, SEG-007, SEG-008, SEG-010, SEG-020 | 🟠 ALTA |
| `jwt/JwtUtil.java` | SEG-005, SEG-018 | 🟠 ALTA |
| `config/RedisConfig.java`, `config/CacheTtlConfig.java` | SEG-019 | 🟠 ALTA |
| `controller/AdminController.java` | SEG-003, SEG-020 | 🟠 ALTA |
| `service/VarianteServiceImpl.java`, `service/ProductosServiceImpl.java` | SEG-006 | 🟠 ALTA |
| `controller/ProductosControllerImpl.java`, `VarianteController.java` | SEG-007 | 🟠 ALTA |
| `models/AuthRequest.java` | SEG-011 | 🟡 MEDIA |
| `chatbot/ChatbotRequest.java` | SEG-012 | 🟡 MEDIA |
| `chatbot/ChatbotController.java` | SEG-009 | 🟡 MEDIA |
| `src/main/resources/application-docker.yml` | SEG-013 | 🟡 MEDIA |
| `controller/MercadoPagoController.java` | SEG-017 | 🟢 BAJA |
<<<<<<< HEAD
=======
| `pom.xml` | SEG-018 | 🟢 BAJA |

---

## Revisión de Seguridad — Rama `dev` (2026-05-26)

Revisión enfocada en los cambios recientes de la rama `dev`. Solo se reportan vulnerabilidades con confianza ≥ 80 %.

---

### [SEG-019] ALTA — Deserialización insegura de Jackson en Redis (RCE potencial)

- **Categoría:** `deserialization_rce`
- **Archivos:** `src/main/java/com/ventas/key/mis/productos/config/RedisConfig.java`, `config/CacheTtlConfig.java`
- **Confianza:** 8/10

**Descripción:**
Ambos `ObjectMapper` de Redis usan `activateDefaultTyping` con `LaissezFaireSubTypeValidator.instance` y `DefaultTyping.NON_FINAL`. Este validador acepta **cualquier nombre de clase sin restricción** — es funcionalmente equivalente al método `enableDefaultTyping()` que Jackson deprecó explícitamente como inseguro (CVE-2017-7525 y cadena relacionada). Cualquier JSON almacenado en Redis que incluya un campo `@class` apuntando a una clase gadget del classpath será instanciada durante la deserialización.

**Escenario de explotación:**
Redis no tiene contraseña configurada en ningún perfil de la aplicación. Un atacante con acceso de red al puerto 6379 (red local del VPS, firewall mal configurado o posición MITM) puede escribir directamente un payload manipulado con `@class` apuntando a una cadena de gadgets conocida (ej. `com.sun.org.apache.xalan.internal.xsltc.trax.TemplatesImpl`, disponible vía dependencias transitivas de Spring). En la siguiente lectura de caché, el deserializador instancia la clase del atacante y ejecuta comandos del sistema operativo con los permisos del proceso Java.

**Corrección — Parte 1: código Java**
```java
mapper.activateDefaultTyping(
    BasicPolymorphicTypeValidator.builder()
        .allowIfSubType("com.ventas.key.mis.productos") // solo clases del dominio
        .build(),
    ObjectMapper.DefaultTyping.NON_FINAL,
    JsonTypeInfo.As.PROPERTY
);
```

**Corrección — Parte 2: proteger Redis con contraseña**

> **📌 Nota — Cómo implementar contraseña en Redis:**
>
> **Paso 1 — En `docker-compose.yml`** (desarrollo local):
> ```yaml
> redis:
>   image: redis:7-alpine
>   command: redis-server --requirepass ${REDIS_PASSWORD}
> ```
>
> **Paso 2 — En `application-docker.yml`** (producción) y `application-qa.yml`:
> ```yaml
> spring:
>   data:
>     redis:
>       password: ${REDIS_PASSWORD}
> ```
> Spring Boot lee esa propiedad y se la pasa automáticamente a `LettuceConnectionFactory`. No hay que cambiar nada en el código Java.
>
> **Paso 3 — Variable de entorno:** Agregar `REDIS_PASSWORD=TuContraseñaSegura` en los secretos de Kubernetes / archivo `.env` local.
>
> **Importante:** al poner contraseña en Redis por primera vez, los datos en caché quedan inaccesibles. El sistema simplemente volverá a llenar la caché desde la BD, sin efectos adversos. Solo hay que reiniciar el servicio.

> **🔧 CAMBIO PENDIENTE:**
> 1. Reemplazar `LaissezFaireSubTypeValidator.instance` por `BasicPolymorphicTypeValidator` con lista blanca en `RedisConfig.java` y `CacheTtlConfig.java`.
> 2. Agregar `requirepass` en Redis y `spring.data.redis.password: ${REDIS_PASSWORD}` en los YMLs de QA y Docker.

---

### [SEG-020] ALTA — Endpoint de prueba RabbitMQ accesible sin autenticación

- **Categoría:** `unauthorized_message_injection`
- **Archivos:** `src/main/java/com/ventas/key/mis/productos/controller/AdminController.java` (líneas 41–53), `security/SecurityConfig.java` (línea 127)
- **Confianza:** 9/10

**Descripción:**
El endpoint `GET /admin/test-rabbit` está explícitamente marcado con `permitAll()` en `SecurityConfig.java`, anulando la regla general `hasRole("ADMIN")` que protege `/admin/**`. Cualquier usuario de internet, sin ninguna credencial, puede llamarlo. El handler publica un mensaje hardcodeado `RequestProductoImagen{productoId=999, imagenId=1}` en el exchange real de producción `EXCHANGE_IMAGENES` con la routing key `ROUTING_KEY_GUARDAR`. El propio comentario Swagger del endpoint dice `"ELIMINAR en produccion"`.

**Escenario de explotación:**
Un atacante envía peticiones `GET /mis-productos/admin/test-rabbit` repetidas sin autenticarse. Cada llamada encola un mensaje real en la cola de producción. El consumidor lo procesa y puede generar registros espurios en base de datos, disparar operaciones en el sistema de archivos o corromper asociaciones producto–imagen.

> **🔧 CAMBIO PENDIENTE:** Solo era de prueba, ya no se usará.
> 1. Eliminar el método `testRabbit()` de `AdminController.java`.
> 2. Eliminar la línea de `SecurityConfig.java`:
> ```java
> // Eliminar:
> .requestMatchers(HttpMethod.GET, "/admin/test-rabbit").permitAll()
> ```
>>>>>>> dev
