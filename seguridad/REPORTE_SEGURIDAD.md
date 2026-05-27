# Reporte de Seguridad — Proyecto Key New
**Fecha:** 2026-05-19
**Spring Boot:** 3.2.5 | Java 17 | MySQL + Redis + RabbitMQ

---

## Resumen Ejecutivo

| Severidad | Cantidad | Estado |
|-----------|----------|--------|
| CRÍTICA   | 3        | 0 corregidas, 3 pendientes (requieren acción manual) |
| ALTA      | 5        | 0 corregidas, 5 pendientes |
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
- **Impacto:** Cualquier visitante puede inundar la cola RabbitMQ (DoS) y descubrir la arquitectura interna del sistema.
- **Estado:** PENDIENTE
- **Solución:** Eliminar la línea del `permitAll()`. El endpoint ya queda cubierto por la regla general `"/admin/**"` que requiere `ROLE_ADMIN`. Idealmente, eliminar el endpoint completo de `AdminController` ya que es solo de prueba.
- **Nota:** hay que eliminarlo, solo era para pruebas
  > **Respuesta:** Correcto. Se elimina el método completo de `AdminController` y la línea del `permitAll()` en SecurityConfig.

---

### [SEG-004] ALTA — Registro público de usuarios sin verificación

- **Categoría:** Control de acceso roto
- **Archivo:** `security/SecurityConfig.java`
- **Detalle:** `/auth/registrar` es público. Cualquier bot puede crear miles de cuentas con acceso a endpoints `authenticated()`.
- **Estado:** PENDIENTE
- **Opciones de solución:**
  - (A) Requerir `ROLE_ADMIN` para crear usuarios (solo el admin crea cuentas)
  - (B) Implementar verificación de email antes de activar la cuenta
  - (C) Agregar CAPTCHA en el frontend como mínimo
  - **Nota:** Para la A lo que pasa es que los usuarios pueden registrarse, aquí hay que limitarlo para que no estén a cada rato haciendo cuentas, no puede pasar que un bot se esté registrando sin parar.
    > **Respuesta:** Exacto. Con la Opción A, el endpoint `/auth/registrar` pasa a requerir `ROLE_ADMIN`, así solo el administrador puede crear cuentas. Un bot no puede auto-registrarse porque el endpoint ya no es público.
  - Siguemos con dudas para la Letra A que pasa si un cliente se quiere registrar coimo se le haria afuerzas yo la tendria que hacer?
    > **Respuesta:** Sí, con la Opción A tú serías el único que puede crear cuentas. El flujo sería: el cliente te contacta (por correo, WhatsApp, lo que uses), tú entras al panel de admin y creas la cuenta manualmente. Es la opción más segura pero menos cómoda si esperas muchos registros. Si los clientes se registran solos con frecuencia, la Opción C (CAPTCHA) es mejor porque mantiene el auto-registro pero bloquea los bots.
  - **Nota:** Para la B hay que implementarlo, el OAuth2 para Google y Facebook es diferente o es el mismo OAuth2?
  - Seguimos con dudas, pero la cosa es que quiero que el cliente se pueda hacer su propia cuenta para que use la aplicacioon lo que podemos hacer es validar el correo y una forma de autenticar por numero telefonico o la aplicacion authenticator no?
    > **Respuesta:** Sí, eso es lo correcto si quieres auto-registro. Las opciones que mencionas son:
    > - **Validación de correo:** Al registrarse, el backend manda un email con un link o código. La cuenta queda inactiva hasta que el usuario confirma. Esto elimina cuentas con emails falsos. Se implementa con un campo `emailVerificado` en la BD y Spring Mail para enviar el correo.
    > - **Número telefónico (SMS/OTP):** Al registrarse pides el teléfono y mandas un código por SMS. Requiere un servicio externo como Twilio (tiene costo por SMS). Es más robusto que el email pero más caro.
    > - **Authenticator app (TOTP):** Es el código de 6 dígitos que cambia cada 30 segundos, como Google Authenticator. Se usa más como segundo factor (2FA) después del login, no para el registro inicial. Requiere que el usuario instale una app.
    > **Recomendación práctica:** Validación de correo es lo más natural para tu caso — el cliente se registra solo, confirma su email, y listo. Es gratis, fácil de implementar con Spring Mail, y evita cuentas falsas. El CAPTCHA se puede agregar encima como capa extra contra bots.
  - **Nota:** Para la B hay que implementarlo, el OAuth2 para Google y Facebook es diferente o es el mismo OAuth2?
    > **Respuesta:** Son diferentes. Lo que ya tienes es JWT propio. OAuth2 social ("Iniciar sesión con Google/Facebook") es una implementación distinta que requiere registrar la app en Google Cloud Console, agregar la dependencia `spring-boot-starter-oauth2-client` y cambiar el flujo de login. La ventaja es que Google ya verifica el email, eliminando cuentas falsas, pero es trabajo adicional considerable.
  - **Nota:** Para la letra C agregar el captcha ¿cómo sería?
    > **Respuesta:** Se agrega Google reCAPTCHA v3 (invisible) en el formulario del frontend. Cuando el usuario envía el formulario, el frontend manda un token de reCAPTCHA al backend, y el backend lo valida con la API de Google antes de crear la cuenta. Efectivo contra bots automáticos.
  - **Recomendación:** Opción A es la más rápida y suficiente para este proyecto.
  - **DECISIÓN FINAL:** Validación de correo electrónico — el cliente se registra solo, el backend manda un email con un código/link, la cuenta queda inactiva hasta confirmar. Se implementa con Spring Mail.

---

### [SEG-005] ALTA — Refresh token puede usarse como access token

- **Categoría:** Confusión de tokens JWT
- **Archivo:** `jwt/JwtUtil.java`
- **Detalle:** `validateToken()` no verifica el claim `type`. Un refresh token (7 días de vida) puede usarse como Bearer token en Authorization para autenticarse en cualquier endpoint.
- **Estado:** PENDIENTE
- **Solución:** En `JwtUtil.validateToken()` agregar verificación del claim `type`:
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

---

### [SEG-006] ALTA — Sin validación de tipo MIME ni tamaño en uploads de imágenes

- **Categoría:** Subida de archivos sin restricciones
- **Archivos:** `service/VarianteServiceImpl.java`, `service/SubirDocumentosServiceImpl.java`
- **Detalle:** Se acepta cualquier tipo de archivo y cualquier tamaño. `SubirDocumentosServiceImpl` solo verifica la extensión del nombre (bypasseable).
- **Estado:** PENDIENTE
- **Solución:** Agregar antes de procesar cada imagen:
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

---

### [SEG-007] ALTA — Sin límite en parámetros de paginación (DoS)

- **Categoría:** Validación de entrada insuficiente / DoS
- **Archivos:** `ProductosControllerImpl.java`, `VarianteController.java`, `ClienteControllerImpl.java` y más
- **Código vulnerable:**
```java
@RequestParam int size, @RequestParam int page
// size=999999 ejecuta consulta con millones de registros
```
- **Impacto:** Una sola petición anónima con `size=999999` puede agotar la memoria del JVM y causar caída del servicio.
- **Estado:** PENDIENTE
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
- **Solución:** Cambiar de `permitAll()` a `authenticated()`.
- **Nota:** Entonces aquí solo para los que estén autenticados, ya que el cliente puede buscar su ID, ¿o eso también es peligroso ya que si está autenticado podría buscar cualquier ID no?
  > **Respuesta:** Exacto, tienes razón. Poner `authenticated()` es el primer paso pero no es suficiente — cualquier usuario autenticado podría buscar los datos de otro cliente cambiando el ID en la URL (eso se llama **IDOR**). La solución correcta es que el endpoint no reciba el ID como parámetro, sino que lo lea directamente del JWT:
  > ```java
  > String username = SecurityContextHolder.getContext().getAuthentication().getName();
  > ```
  > Así cada usuario solo puede ver sus propios datos, sin importar qué ID escriba.
> -seguimos con duas si, lo que quiero es que no porque este autenticado pueda ver los datos de los demas usuario ese cambio hay que hacerlo asi para que no pueda ver los datos de otros

---

### [SEG-009] MEDIA — Logs del chatbot registran mensaje completo del usuario ✅ PENDIENTE

- **Categoría:** Violación de privacidad / Datos sensibles en logs
- **Archivo:** `controller/ChatbotController.java`
- **Código vulnerable:**
```java
log.info("Chatbot - IP: {}, mensaje: {}", ip, request.getMensaje());
```
- **Solución:** Cambiar a:
```java
log.info("Chatbot - IP: {}, longitud mensaje: {}", ip, request.getMensaje().length());
```
- **Nota:** hay que corregirlos
  > **Respuesta:** Sí. Se cambia esa línea en `ChatbotController.java` — queda registrada solo la longitud del mensaje, no el contenido privado del usuario.

---

### [SEG-010] MEDIA — Headers de seguridad HTTP no configurados explícitamente

- **Categoría:** Configuración de seguridad incompleta
- **Archivo:** `security/SecurityConfig.java`
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
---

### [SEG-011] MEDIA — AuthRequest sin límite de longitud en campos ✅ CORREGIDO

- **Categoría:** Validación de entrada insuficiente
- **Archivo:** `src/main/java/com/ventas/key/mis/productos/models/AuthRequest.java`
- **Solución aplicada:** Agregados `@Size` en `userName` (3-100 chars), `password` (6-200 chars) y `@Email` + `@Size` en `email`.
- **Nota:** hay que agregar la validacion
  > **Respuesta:** Ya está aplicada en el código — se verificó que `@Size` y `@Email` están correctamente en `AuthRequest.java`. Este punto ya está resuelto.

---

### [SEG-012] MEDIA — ChatbotRequest sin límite en historial de mensajes

- **Categoría:** Validación de entrada / Ataque de costo
- **Archivo:** `chatbot/ChatbotRequest.java`
- **Detalle:** El historial puede tener mensajes ilimitados de longitud ilimitada, generando llamadas masivas y costosas a la API de OpenAI.
- **Solución:**
```java
@Size(max = 20, message = "El historial no puede superar 20 mensajes")
private List<@Valid MensajeHistorial> historial;

// En MensajeHistorial:
@Size(max = 2000)
private String contenido;
```
- **Nota:** hay que agregar la validacion
  > **Respuesta:** Sí. Se agregan las anotaciones `@Size` en `ChatbotRequest` y en `MensajeHistorial`. Esto evita que alguien mande un historial enorme que genere una llamada costosa a OpenAI.

---

### [SEG-013] MEDIA — Swagger habilitado en perfil Docker (producción)

- **Categoría:** Exposición de información
- **Archivo:** `src/main/resources/application-docker.yml`
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
- **Misma solución que SEG-015:** Migrar `ipInfoMap` a Redis.
- **Nota:** no sé a qué se refiere
  > **Respuesta:** Mismo problema que SEG-015 pero para el chatbot. El mapa que recuerda qué IPs están bloqueadas por enviar mensajes incomprensibles está en RAM. Al reiniciar el servidor, todas las IPs bloqueadas quedan libres automáticamente. La solución es moverlo a Redis igual que el rate limiter.
> Hay que corregirlo tambien

---

### [SEG-017] BAJA — Endpoint de simulación de pago sin doble capa de seguridad

- **Categoría:** Defensa en profundidad insuficiente
- **Solución:** Agregar `@PreAuthorize("hasRole('ADMIN')")` al método de simulación en `MercadoPagoController`.
- **Nota:** ¿cómo sería 2 capas de seguridad?
  > **Respuesta:**
  > - **Capa 1 (SecurityConfig):** La regla general en la configuración HTTP que dice que `/admin/**` requiere `ROLE_ADMIN`.
  > - **Capa 2 (@PreAuthorize en el método):** La anotación directamente en el método del controlador que también verifica el rol.
  > Si por error de un refactor el endpoint se mueve fuera de `/admin/`, la capa 2 sigue protegiendo. Son dos barreras independientes — si una falla, la otra cubre.
-hayq ue agregar esa capa de seguridad
---

### [SEG-018] BAJA — JJWT versión 0.11.5 con API deprecated

- **Categoría:** Dependencia desactualizada
- **Solución:** Actualizar a JJWT 0.12.x en `pom.xml`.
- **Nota:** Hay que actualizar la dependencia que sea compatible
  > **Respuesta:** Sí. Se actualiza en `pom.xml` de `0.11.5` a `0.12.x`. La API cambió entre versiones, así que también hay que ajustar el código en `JwtUtil.java` (algunos métodos de 0.11 fueron eliminados en 0.12).
    -hay que hacer los cambios
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

1. **JWT Blacklist en Redis al hacer logout** — El claim `jti` existe en los access tokens; guardarlos en Redis con TTL igual al tiempo restante de expiración para invalidarlos efectivamente al cerrar sesión.
2. **Trusted proxies para X-Forwarded-For** — Agregar `server.forward-headers-strategy: NATIVE` si el VPS usa nginx/Traefik como reverse proxy.
3. **Límite de sesión absoluta** — El refresh token se renueva indefinidamente. Implementar máximo de 30 días comparando `sessionStart`.
4. **Verificar que `PruebaControllerImpl`** no esté expuesto en producción.
5. **Rotar API key de OpenAI** — Verificar en el historial de Git que la clave real nunca fue commiteada.

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
| `security/SecurityConfig.java` | SEG-003, SEG-004, SEG-008, SEG-010 | 🟠 ALTA |
| `jwt/JwtUtil.java` | SEG-005, SEG-018 | 🟠 ALTA |
| `service/VarianteServiceImpl.java` | SEG-006 | 🟠 ALTA |
| `controller/ProductosControllerImpl.java` | SEG-007 | 🟠 ALTA |
| `chatbot/ChatbotRequest.java` | SEG-012 | 🟡 MEDIA |
| `chatbot/ChatbotController.java` | SEG-009 | 🟡 MEDIA |
| `src/main/resources/application-docker.yml` | SEG-013 | 🟡 MEDIA |
| `controller/MercadoPagoController.java` | SEG-017 | 🟢 BAJA |
