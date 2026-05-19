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

---

### [SEG-011] MEDIA — AuthRequest sin límite de longitud en campos ✅ CORREGIDO

- **Categoría:** Validación de entrada insuficiente
- **Archivo:** `src/main/java/com/ventas/key/mis/productos/models/AuthRequest.java`
- **Solución aplicada:** Agregados `@Size` en `userName` (3-100 chars), `password` (6-200 chars) y `@Email` + `@Size` en `email`.

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

---

### [SEG-016] BAJA — Bloqueo de IPs del chatbot tampoco persiste

- **Categoría:** Riesgo arquitectural
- **Misma solución que SEG-015:** Migrar `ipInfoMap` a Redis.

---

### [SEG-017] BAJA — Endpoint de simulación de pago sin doble capa de seguridad

- **Categoría:** Defensa en profundidad insuficiente
- **Solución:** Agregar `@PreAuthorize("hasRole('ADMIN')")` al método de simulación en `MercadoPagoController`.

---

### [SEG-018] BAJA — JJWT versión 0.11.5 con API deprecated

- **Categoría:** Dependencia desactualizada
- **Solución:** Actualizar a JJWT 0.12.x en `pom.xml`.

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

---

## Recomendaciones Adicionales

1. **JWT Blacklist en Redis al hacer logout** — El claim `jti` existe en los access tokens; guardarlos en Redis con TTL igual al tiempo restante de expiración para invalidarlos efectivamente al cerrar sesión.
2. **Trusted proxies para X-Forwarded-For** — Agregar `server.forward-headers-strategy: NATIVE` si el VPS usa nginx/Traefik como reverse proxy.
3. **Límite de sesión absoluta** — El refresh token se renueva indefinidamente. Implementar máximo de 30 días comparando `sessionStart`.
4. **Verificar que `PruebaControllerImpl`** no esté expuesto en producción.
5. **Rotar API key de OpenAI** — Verificar en el historial de Git que la clave real nunca fue commiteada.

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