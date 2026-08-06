# Auditoría de seguridad — AuthController y capa de autenticación

**Fecha de auditoría:** 2026-07-30
**Rama de trabajo:** `dev`
**Alcance revisado:**
`AuthController`, `JwtUtil`, `JwtAuthenticationFilter`, `SecurityConfig`, `LoginRateLimiterService`,
`UsuarioDetailsService`, `PasswordResetService`, `UsuarioVerificacionService`, `RegistroService`,
entidad `Usuario`, DTOs de auth y los YML de los perfiles `dev` / `qa` / `docker`.

---

## Cómo se usa este documento

Cada hallazgo tiene un **estado** que se actualiza conforme se va corrigiendo:

| Estado | Significado |
|---|---|
| ⬜ Pendiente | Detectado, sin corregir |
| ✅ Corregido | Corregido y anotado con fecha + qué se cambió |
| ⏭️ No aplica | Se decidió no corregir; queda la justificación |

**Progreso: 16 / 18 corregidos · 2 no aplican (#15 y #16) · 0 pendientes**

- **#15** se verificó y **no era un bug**: hay un solo proxy delante, que es justo el caso para el
  que el código ya estaba escrito. No requería cambio.
- **#16** es una decisión tomada (rate limit apagado en QA mientras dure la tanda de pruebas).

> Todo lo corregido está en la rama `dev`, **sin commitear y sin desplegar**. Antes de desplegar
> hay que leer la sección "Qué hacer al desplegar" al final de este documento — hay dos
> migraciones SQL obligatorias y un cambio de comportamiento que afecta al front.

---

## Resumen

| # | Sev | Hallazgo | Estado |
|---|---|---|---|
| 1 | 🔴 Crítico | `restablecer-password` sin rate limit → brute force del código | ✅ Corregido |
| 2 | 🔴 Crítico | `verificar-correo` / `confirmar-cambio-correo` sin rate limit | ✅ Corregido |
| 3 | 🟠 Alto | Logout no invalida el refresh token del lado del servidor | ✅ Corregido |
| 4 | 🟠 Alto | `refresh` no revalida `enabled` ni `correoVerificado` | ✅ Corregido |
| 5 | 🟠 Alto | Sesión infinita: `sessionStart` nunca se evalúa; sin detección de reuso | ✅ Corregido |
| 6 | 🟠 Alto | Cambiar/restablecer contraseña no cierra las sesiones abiertas | ✅ Corregido |
| 7 | 🟡 Medio | Rate limiter en memoria sin cota ni limpieza → fuga de memoria | ✅ Corregido |
| 8 | 🟡 Medio | Lockout dirigido: 5 requests bloquean a un usuario concreto | ✅ Corregido |
| 9 | 🟡 Medio | Secreto JWT hardcodeado y versionado en git | ✅ Corregido |
| 10 | 🟡 Medio | Orígenes de desarrollo en el CORS de producción | ✅ Corregido |
| 11 | 🟡 Medio | CSRF deshabilitado + `SameSite=None` en endpoints con cookie | ✅ Corregido (apagado por defecto) |
| 12 | 🔵 Bajo | `/v1/auth/validar` acepta refresh tokens como válidos | ✅ Corregido |
| 13 | 🔵 Bajo | Contraseñas de 3 caracteres permitidas; mensaje no coincide | ✅ Corregido |
| 14 | 🔵 Bajo | `passwordTemporal` no se fuerza en el backend | ✅ Corregido |
| 15 | 🔵 Bajo | `resolverIp` depende de que haya exactamente un proxy | ⏭️ No aplica (verificado: hay 1) |
| 16 | 🔵 Bajo | Rate limit desactivado en QA, que está expuesto a internet | ⏭️ No aplica (por ahora) |
| 17 | 🔵 Bajo | `enviar-codigo-verificacion` sin límite por IP → spam de correo | ✅ Corregido |
| 18 | 🔵 Bajo | `log.error` en login pierde el stack trace | ✅ Corregido |

---

# 🔴 Críticos

## 1. `restablecer-password` sin rate limit → brute force del código = toma de cuenta

**Estado:** ✅ Corregido — 2026-07-31
**Ubicación:** `AuthController.java:259-268`, `PasswordResetService.java:41-58`

### Qué pasa
`olvide-password` sí tiene rate limit por IP (`AuthController.java:245`), pero
`restablecer-password` **no tiene ninguno**. Y en `PasswordResetService.restablecerPassword()`
no hay contador de intentos: el código de 6 dígitos sigue vivo los 15 minutos completos y se
puede probar sin límite de veces.

### Impacto
Son 1,000,000 de combinaciones sin ninguna traba. Con concurrencia moderada se agota en minutos;
si el código expira, el atacante pide otro con `olvide-password` y sigue. Sólo necesita conocer
el correo de la víctima — incluido el de un ADMIN. **Toma de control total de cualquier cuenta.**

### Qué se hizo
- Rate limit por IP (`reset-ip:`) **y** por email (`reset-mail:`) en `restablecer-password`,
  ambos detrás del flag `seguridad.rate-limit-habilitado` → responde 429.
- Campo `Usuario.intentosCodigoReset`: al llegar a 5 fallos se invalida `codigoResetPassword` y
  hay que pedir uno nuevo por `olvide-password`, que sí está limitado por IP.
- `PasswordResetService` usa `@Transactional(noRollbackFor = ExceptionCodigoInvalido.class)` —
  sin eso el rollback borraba el incremento del contador y el límite nunca aplicaba.
- Se unificó el mensaje de todos los caminos de fallo para no revelar si el correo existe ni si
  el código era correcto pero expiró.
- El `catch` del controller ya no devuelve el `getMessage()` de excepciones inesperadas (NPE,
  error de BD), sólo el de `ExceptionCodigoInvalido`.
- Migración: `src/main/resources/static/migration_intentos_codigo_reset.sql`.

---

## 2. `verificar-correo` / `confirmar-cambio-correo` sin rate limit

**Estado:** ✅ Corregido — 2026-07-31
**Ubicación:** `AuthController.java:225-234` y `:337-347`, `UsuarioVerificacionService.java:47-69` y `:113-131`

### Qué pasa
Mismo patrón que el hallazgo 1: código de 6 dígitos, sin límite de intentos y sin contador de
fallos. `verificar-correo` además es público.

### Impacto
- `verificar-correo`: permite activar por fuerza bruta una cuenta registrada con un correo ajeno.
- `confirmar-cambio-correo`: requiere sesión válida, así que el atacante sólo puede atacar su
  propia cuenta — impacto bajo, pero es el mismo agujero y conviene cerrarlo igual.

### Qué se hizo
Mismo mecanismo del hallazgo 1, reutilizando el patrón:
- `verificar-correo`: rate limit por IP (`verif-cod-ip:`) y por usuario (`verif-cod-usr:`) → 429.
- `confirmar-cambio-correo`: rate limit por usuario autenticado (`cambio-correo:`) → 429.
- Campo `Usuario.intentosCodigoVerificacion`, compartido por los dos flujos porque ambos usan el
  mismo par `codigoVerificacion` / `codigoVerificacionExpira`. A los 5 fallos el código se
  invalida.
- `UsuarioVerificacionService` con `noRollbackFor = ExceptionCodigoInvalido.class`, incluida la
  variante self-service `confirmarCambioCorreo(String, String)`: al ser llamada interna, el proxy
  no aplica la anotación de la variante que recibe el `Usuario`, así que el punto de entrada lleva
  la suya.
- Se invirtió el orden de las validaciones (expiración antes que comparar el código) para no
  quemar intentos con un código ya expirado.
- El `catch` de `verificar-correo` ya no devuelve `"Usuario no encontrado"` al cliente — en un
  endpoint público eso permitía enumerar qué usernames existen.
- Migración: `src/main/resources/static/migration_intentos_codigo_verificacion.sql`.

**Nota:** el endpoint admin `PUT /v1/usuarios/{id}/confirmar-cambio-correo`
(`UsuarioController.java:88`) sigue sin rate limit propio, pero exige rol ADMIN y ahora hereda el
contador de intentos del servicio, así que el código igual se invalida a los 5 fallos.

---

# 🟠 Altos

## 3. Logout no invalida el refresh token del lado del servidor

**Estado:** ✅ Corregido — 2026-07-31 (junto con 5 y 6)
**Ubicación:** `AuthController.java:173-177`, `JwtUtil.java:46`

### Qué pasa
`logout()` sólo manda `Max-Age=0` para que el navegador borre la cookie. **El refresh token
sigue siendo criptográficamente válido durante 7 días.** No hay ninguna lista de revocación.

En `JwtUtil.java:46` se genera un `jti` con el comentario *"para poder invalidarlo"*, pero no
existe ningún componente que lo consulte — el `jti` hoy no se usa para nada.

### Impacto
Si el refresh token fue capturado (máquina compartida, proxy corporativo, backup, log de un
intermediario), cerrar sesión **no corta el acceso del atacante**. El usuario cree que se
desconectó y no es así.

### Qué se hizo — mecanismo común de los hallazgos 3, 5 y 6

Se hizo el refresh token **stateful** con una tabla nueva, `sesion_refresh`
(`migration_sesion_refresh.sql`). Piezas nuevas:

| Archivo | Rol |
|---|---|
| `entity/SesionRefresh.java` | Una fila **por sesión**, no por token |
| `repository/ISesionRefreshRepository.java` | Búsqueda por `sessionId`, borrado por usuario y barrido de expiradas |
| `service/SesionRefreshService.java` | Ciclo de vida: crear, rotar, cerrar, cerrar todas |
| `scheduler/SesionRefreshScheduler.java` | Limpieza diaria (3:30 AM) de sesiones vencidas |

El refresh token ahora lleva dos datos nuevos: el `jti` (en el claim estándar) y el `sessionId`
(familia de la sesión). La fila guarda el `jti` del **único** refresh válido en este momento y se
actualiza en cada rotación; el `sessionId` no cambia nunca.

**Logout** (`AuthController.logout`) lee la cookie, extrae el `sessionId` y borra la fila: el
refresh token deja de servir en el acto, aunque alguien tenga una copia. Antes sólo se le pedía al
navegador que olvidara la cookie.

---

---

## 4. `refresh` no revalida `enabled` ni `correoVerificado`

**Estado:** ✅ Corregido — 2026-07-31
**Ubicación:** `AuthController.java:152-163`, `JwtAuthenticationFilter.java:58`, `JwtUtil.java:95-102`

### Qué pasa
`refresh()` carga el usuario y emite tokens nuevos **sin verificar `isEnabled()` ni
`correoVerificado`**. El login sí valida ambas cosas (`AuthController.java:111` y `:126`) — la
brecha es que el refresh se saltó ese control.

`JwtAuthenticationFilter` tampoco lo comprueba: `validateToken(token, userDetails)` sólo compara
el username y que no sea un refresh token.

### Impacto
**Deshabilitar un usuario en la BD no cierra su sesión.** Sigue renovando su access token durante
7 días como si nada. Para un usuario dado de baja o comprometido, la baja no surte efecto.

### Qué se hizo
- `refresh()` revalida `isEnabled()` y, para no-admins, `correoVerificado` — los mismos dos
  controles que ya hacía el login. Si falla, responde 401 **y limpia la cookie**, para que el
  front no siga reintentando con un refresh token que ya no sirve.
- `JwtAuthenticationFilter` comprueba `userDetails.isEnabled()` antes de poblar el
  `SecurityContext`. Si el usuario está deshabilitado no se autentica y la request termina en 401,
  aunque su access token siga siendo criptográficamente válido.

**Efecto:** deshabilitar un usuario en la BD ahora sí le corta el acceso — como máximo en lo que
tarda la request en curso, no en 7 días.

---

## 5. Sesión infinita: `sessionStart` nunca se evalúa; sin detección de reuso

**Estado:** ✅ Corregido — 2026-07-31 (ver mecanismo en el hallazgo 3)
**Ubicación:** `JwtUtil.java:54-70`, `AuthController.java:157-159`

### Qué pasa
`JwtUtil.generateRefreshToken()` documenta que `sessionStart` existe *"para poder calcular la
duración absoluta de la sesión"*, y `refresh()` lo lee y lo reinyecta en el token nuevo — pero
**nadie lo compara nunca contra un máximo**.

Además la rotación es aparente: se emite un refresh token nuevo, pero **el anterior sigue siendo
válido** hasta su expiración natural, y no hay detección de reuso.

### Impacto
- La cadena de refresh es infinita: cada renovación entrega otros 7 días, para siempre. La sesión
  nunca caduca de verdad.
- Robado un refresh token, el atacante lo renueva indefinidamente **en paralelo** con el usuario
  legítimo, y nada lo delata.

### Qué se hizo
`SesionRefreshService.rotar()` recibe el `sessionId` y el `jti` que llegan en el token y rechaza
en cuatro casos, cada uno cerrando lo que corresponda:

| Caso | Qué hace |
|---|---|
| La familia no existe | 401 — la sesión ya se cerró (logout, cambio de contraseña) |
| El `jti` no es el vigente | **Reuso detectado** → borra la familia completa y 401 |
| El refresh vigente expiró | Borra la fila y 401 |
| Pasaron más de **30 días** desde `sessionStart` | Borra la fila y 401 — límite absoluto |

Con esto la rotación es real (el token anterior queda muerto, no sólo "reemplazado") y
`sessionStart` por fin se evalúa contra un máximo, así que la cadena de renovaciones deja de ser
infinita. El límite absoluto vive en `SesionRefreshService.SESION_MAXIMA_DIAS`.

---

## 6. Cambiar/restablecer contraseña no cierra las sesiones abiertas

**Estado:** ✅ Corregido — 2026-07-31 (ver mecanismo en el hallazgo 3)
**Ubicación:** `PasswordResetService.java:52-57` y `:70-77`

### Qué pasa
Ni `restablecerPassword()` ni `cambiarPassword()` invalidan las sesiones existentes.

### Impacto
El caso de uso *"me entraron a la cuenta, cambio la contraseña"* **no expulsa al atacante**: su
refresh token sigue vivo 7 días. Es la consecuencia directa de los hallazgos 3 y 5.

### Qué se hizo
`sesionRefreshService.cerrarTodasLasSesiones(usuarioId)` se llama en los **tres** puntos donde
cambia una contraseña:

- `PasswordResetService.restablecerPassword()` — reset con código por correo
- `PasswordResetService.cambiarPassword()` — cambio voluntario estando logueado
- `UsuarioServiceImpl.resetearPasswordAleatoria()` — reseteo hecho por un ADMIN

El tercero no estaba en el hallazgo original pero es el mismo agujero, y encima es el que suele
usarse justo cuando una cuenta ya está comprometida.

**Efecto:** cambiar la contraseña ahora expulsa de verdad a quien estuviera dentro. El propio
usuario también tiene que volver a iniciar sesión — es el comportamiento correcto y el front debe
esperarlo.

---

# 🟡 Medios

## 7. Rate limiter en memoria sin cota ni limpieza → fuga de memoria

**Estado:** ✅ Corregido — 2026-07-31
**Ubicación:** `LoginRateLimiterService.java:18,33`

### Qué pasa
`private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();` crea una entrada por
cada IP **y por cada username probado**, y **nunca se limpia**.

### Impacto
- Un atacante haciendo login con usernames aleatorios hace crecer el mapa sin límite hasta
  agotar la memoria del proceso. Es un DoS barato.
- Al ser local al proceso, si se escala a N pods el límite real es 5×N y depende de a qué pod
  caiga cada request.

### Qué se hizo
Sin agregar dependencias nuevas (no hay Caffeine en el `pom.xml`), cada entrada guarda ahora su
marca de último uso:

- **Caducidad por inactividad:** un `@Scheduled(fixedDelay = 300_000)` dentro del propio servicio
  elimina cada 5 minutos las entradas sin uso en más de una ventana completa (15 min). A esa
  altura el bucket ya se recargó del todo, así que conservarlo no aporta nada.
- **Cota dura:** `MAX_ENTRADAS = 50_000`. Al alcanzarla se fuerza una limpieza; si aun así sigue
  llena, se **rechaza** el intento (fail-closed). Es deliberado: bajo un ataque que genere decenas
  de miles de claves distintas, es preferible rechazar logins que quedarse sin memoria y tirar el
  micro entero.
- `segundosHastaRecarga()` ya no crea entrada: consultar por una clave desconocida hacía crecer el
  mapa.

**Lo que NO cambió:** el limitador sigue siendo local al proceso, así que con N pods el límite
efectivo es 5×N. Migrarlo a Redis (que ya está en el proyecto) es lo que corresponde el día que se
escale horizontalmente. Queda anotado en el código.

---

## 8. Lockout dirigido: 5 requests bloquean a un usuario concreto

**Estado:** ✅ Corregido — 2026-07-31
**Ubicación:** `AuthController.java:96`

### Qué pasa
El bucket `usr:<username>` se consume **antes** de intentar autenticar, y también se consume en
los logins **exitosos**.

### Impacto
- Cinco POST con contraseña basura dejan al admin sin poder entrar durante 15 minutos. DoS
  dirigido trivial contra cualquier cuenta cuyo username se conozca.
- Cinco entradas legítimas en 15 minutos autobloquean al propio usuario.

### Qué se hizo
Se separó *consultar* de *gastar*. El servicio expone ahora tres operaciones distintas:

| Método | Cuándo |
|---|---|
| `hayIntentosDisponibles(clave)` | Antes de autenticar — **no consume nada** |
| `registrarFallo(clave)` | Sólo tras `BadCredentialsException` o `DisabledException` |
| `limpiarIntentos(clave)` | Login correcto — borra el historial de fallos |

Se aplicó **también a la clave por IP**, no sólo a la de username: con NAT de oficina el problema
era igual o peor (cinco entradas legítimas desde la misma IP pública bloqueaban a todos).

Dos decisiones que conviene no "arreglar" después:
- `DisabledException` **sí** gasta intento. El provider comprueba `enabled` antes que la
  contraseña, así que no gastarlo dejaría un oráculo ilimitado de "esta cuenta existe y está
  deshabilitada".
- Un login correcto **no** limpia la clave por IP, sólo la de username. Esa clave la comparten
  `registrar` y `olvide-password`, y un login bueno no debería borrar los fallos acumulados de esa
  IP.

**Lo que sigue siendo posible:** cinco intentos fallidos contra un username conocido lo bloquean
15 minutos. Eso es inherente a cualquier lockout por usuario; lo que se eliminó es el autobloqueo
del usuario legítimo.

---

## 9. Secreto JWT hardcodeado y versionado en git

**Estado:** ✅ Corregido — 2026-07-31
**Ubicación:** `application.yml:65`, `application-dev.yml:62`

### Qué pasa
La clave `miClaveSuperSeguraDe32Caracteres` está en texto plano en el repositorio. Los perfiles
`qa` y `docker` sí usan `${TOKEN_JWT}` sin valor por defecto (falla al arrancar si falta, lo cual
es correcto: *fail-closed*), pero el perfil base — que es el activo por defecto,
`application.yml:3` `active: dev` — queda con la clave pública.

### Impacto
Cualquiera con acceso al repositorio puede firmar tokens de ADMIN válidos en cualquier ambiente
que caiga al perfil por defecto. Además son exactamente 32 caracteres = 256 bits, el mínimo justo
para HS256.

### Qué se hizo

| Perfil | Antes | Ahora |
|---|---|---|
| `application.yml` (base) | clave real en texto plano | `${TOKEN_JWT}` **sin default** → fail-closed |
| `application-dev.yml` | clave real en texto plano | `${TOKEN_JWT:claveDeDesarrolloLocalNoUsarEnProd32}` |
| `application-qa.yml` / `docker` | ya usaban `${TOKEN_JWT}` | sin cambios |

El perfil base queda fail-closed: si un ambiente no define `TOKEN_JWT` ni sobreescribe la
propiedad, el micro **no arranca** en vez de arrancar con una clave pública. `dev` conserva un
valor por defecto para que el arranque local siga funcionando sin configurar nada, pero ese valor
ya no es un secreto real — es explícitamente de desarrollo y así está nombrado.

⚠️ **La clave anterior (`miClaveSuperSeguraDe32Caracteres`) queda en el historial de git.** Sacarla
del código no la borra del historial: si algún ambiente la estuvo usando de verdad, hay que
**rotar `TOKEN_JWT`** en ese ambiente. Rotarla invalida todos los tokens vigentes y obliga a todos
a iniciar sesión de nuevo.

---

## 10. Orígenes de desarrollo en el CORS de producción

**Estado:** ✅ Corregido — 2026-07-31
**Ubicación:** `SecurityConfig.java:219-231`

### Qué pasa
La configuración CORS es **única para todos los perfiles** (está hardcodeada en Java) e incluye
`http://localhost:4200` y `http://51.178.29.99:30001` — dos orígenes en HTTP plano — junto con
`allowCredentials(true)`.

### Impacto
En producción quedan habilitados con credenciales orígenes que no deberían existir ahí. Cualquier
página que la víctima abra servida desde `localhost:4200` puede llamar a la API con sus cookies.

### Qué se hizo
`SecurityConfig` ya no lleva la lista hardcodeada: lee `seguridad.cors.origenes-permitidos`
(separada por comas) y cada perfil define la suya.

| Perfil | Orígenes |
|---|---|
| base | `http://localhost:4200` — mínimo, para que un ambiente mal configurado no herede los de prod |
| `dev` | localhost:4200, 51.178.29.99:30001, netlify |
| `qa` | qa.shop.novedades-jade.com.mx, netlify |
| `docker` (producción) | shop, front, novedades-jade, www.novedades-jade, netlify — **sólo HTTPS** |

⚠️ **Revisar antes de desplegar a producción:** de `docker` se quitaron `http://localhost:4200`,
`http://51.178.29.99:30001` y `qa.shop.novedades-jade.com.mx`. Es justo lo que pedía el hallazgo,
pero si algún cliente real estaba entrando por alguno de esos orígenes, dejará de funcionar —
conviene confirmarlo antes del merge a `main`. Se dejó `venta-bolsas-online.netlify.app` por si el
front sigue publicado ahí.

---

## 11. CSRF deshabilitado + `SameSite=None` en endpoints con cookie

**Estado:** ✅ Corregido — 2026-07-31 · **apagado por defecto, requiere acción del front**
**Ubicación:** `SecurityConfig.java:46`, `AuthController.java:369`

### Qué pasa
CSRF está deshabilitado globalmente y la cookie de refresh se emite con `SameSite=None` (necesario
porque el front está en otro dominio).

### Impacto
Acotado: la API se autentica con Bearer, no con cookie, así que sólo `/v1/auth/refresh` y
`/v1/auth/logout` dependen de la cookie. Un sitio de terceros puede forzar esas dos llamadas desde
el navegador de la víctima, pero **no puede leer la respuesta** (CORS lo bloquea). Hoy es molestia,
no robo de sesión. Cuando se implemente la rotación con detección de reuso (hallazgo 5), pasa a ser
un vector de expulsión de sesión y hay que cubrirlo.

### Reevaluación tras el hallazgo 5
El diagnóstico original decía que hoy era molestia, no robo de sesión — y que al implementar la
rotación con detección de reuso pasaría a ser un vector de expulsión. **Eso ya pasó:** con el
hallazgo 5 hecho, un sitio de terceros que fuerce un `POST /refresh` desde el navegador de la
víctima rota el token; el usuario legítimo se queda con el anterior, y en su siguiente refresh se
detecta reuso y **se le mata la sesión**. Sube de molestia a DoS de sesión dirigido.

### Qué se hizo
`refresh` y `logout` exigen el header `X-Requested-With`, que un formulario cross-site no puede
enviar sin pasar por preflight CORS. Responden **403** si falta.

🚩 **Viene apagado por defecto** (`seguridad.exigir-header-refresh: false`). Encenderlo antes de
que el front mande el header **rompería el refresh de todos los usuarios**: se quedarían sin sesión
a los 15 minutos, cuando expire su access token.

**Para activarlo:**
1. El front agrega el header `X-Requested-With: XMLHttpRequest` a `POST /v1/auth/refresh` y
   `POST /v1/auth/logout`.
2. Confirmado eso, poner `seguridad.exigir-header-refresh: true` en el YML del ambiente.
3. Empezar por QA, verificar que el refresh sigue funcionando, y recién después producción.

---

# 🔵 Bajos

## 12. `/v1/auth/validar` acepta refresh tokens como válidos

**Estado:** ✅ Corregido — 2026-07-31
**Ubicación:** `AuthController.java:354-366`, `JwtUtil.java:104-114`

`validateToken(String)` sólo verifica firma y expiración, no el tipo de token. El filtro sí los
rechaza (`JwtAuthenticationFilter.java:45`, correcto), así que **no escala a acceso**; el endpoint
simplemente responde "válido" a algo que no sirve para autenticar. Es `permitAll` y sin rate limit,
así que funciona además como oráculo de validez de tokens.

**Qué se hizo:** `/validar` ahora exige `validateToken(token) && !isRefreshToken(token)`. Un refresh
token responde 401, igual que cualquier otro token que no sirva para autenticar.

---

## 13. Contraseñas de 3 caracteres permitidas; mensaje no coincide

**Estado:** ✅ Corregido — 2026-07-31
**Ubicación:** `RegistroRequest.java`, `CambiarPasswordRequest.java`, `RestablecerPasswordRequest.java`, `AuthRequest.java`

Los DTOs declaran `@Size(min = 3, ...)` con el mensaje *"La contrasena debe tener entre 6 y 200
caracteres"*. Ni el mensaje coincide con la regla, ni 3 caracteres es una política aceptable.

**Qué se hizo:** `min = 8` con el mensaje alineado ("entre 8 y 200 caracteres") en
`RegistroRequest`, `CambiarPasswordRequest` y `RestablecerPasswordRequest`. `AuthRequest` (login)
se quedó en 3 a propósito — subirlo no aporta seguridad y dejaría fuera a quien ya tiene una
contraseña corta —; sólo se corrigió su mensaje, que decía 6 cuando la regla era 3.

Compatible con `UsuarioServiceImpl.generarPasswordAleatoria()`, que genera contraseñas de 8.

---

## 14. `passwordTemporal` no se fuerza en el backend

**Estado:** ✅ Corregido — 2026-07-31 · **cambio de comportamiento visible para el front**
**Ubicación:** `AuthController.java:122`, `Usuario.java:87-89`

El login devuelve el flag `passwordTemporal`, pero entrega un access token con permisos completos.
Que el usuario "esté obligado" a cambiar la contraseña es sólo una convención del front: se puede
ignorar y operar normalmente con la contraseña que le puso el ADMIN.

**Qué se hizo:** `JwtAuthenticationFilter` bloquea con **403** cualquier ruta que no esté en la
whitelist cuando el usuario tiene `passwordTemporal = true`. Rutas permitidas:

- `PUT /v1/auth/cambiar-password`
- `POST /v1/auth/logout`
- `POST /v1/auth/refresh`
- `GET /v1/auth/validar`

El 403 viene con el envoltorio `ResponseGeneric` de siempre y el mensaje *"Debes cambiar tu
contrasena temporal antes de continuar"*. Se compara contra `getServletPath()`, que ya viene sin
el `context-path` (`/mis-productos`), para que no dependa de cómo esté desplegado.

⚠️ **Para el front:** hasta ahora, ignorar el flag `passwordTemporal` del login y navegar
normalmente *funcionaba*. Ya no. Si el front no redirige a la pantalla de cambio de contraseña,
el usuario con contraseña temporal recibirá 403 en todo lo demás.

---

## 15. `resolverIp` depende de que haya exactamente un proxy

**Estado:** ⏭️ No aplica — verificado el 2026-07-31: **hay exactamente un proxy**, el código ya era
correcto para esa topología. No se tocó nada.
**Ubicación:** `AuthController.java:393-402`

Toma la IP más a la derecha de `X-Forwarded-For`. Es lo correcto con **exactamente un** proxy de
confianza (evita que el cliente falsee su IP). Con dos saltos (CDN + ingress) esa posición pasa a
ser la IP del proxy → todos los usuarios comparten un mismo bucket y 5 fallos bloquean el login
para todo el mundo.

### Topología real, verificada

```
Navegador  →  nginx externo (80/443)  →  NodePort  →  pod Java
                    ↑
              único proxy
```

Evidencia recogida el 2026-07-31:

| Comprobación | Resultado |
|---|---|
| `dig shop.novedades-jade.com.mx` | `51.178.29.99` — la VPS directa, **no** un rango de Cloudflare |
| `dig qa.shop.novedades-jade.com.mx` | `51.178.29.99` — la misma VPS |
| `kubectl get ingress -A` (`VPS_AUDITORIA.md` P10) | *No resources found* — no hay ingress de K8s |
| `kubectl get pods -A \| grep ingress` (P11) | vacío — no hay ingress controller |
| `VPS_AUDITORIA.md` línea 347 | *"El nginx externo recibe en 80/443 y redirige a estos puertos según el dominio"* |

El DNS lo administra Cloudflare (ver `README.md`), pero los registros **no están en modo proxied**:
si lo estuvieran, `dig` devolvería una IP de Cloudflare y no la de la VPS. `novedades-jade.com.mx`
sí resuelve a `75.2.60.5` (Netlify), pero ese es el front, no el backend.

**Conclusión:** con un solo proxy de confianza, tomar la IP más a la derecha de `X-Forwarded-For`
es exactamente lo correcto — es lo que impide que el cliente falsee su IP anteponiendo valores al
header. **No hay nada que corregir.**

### Cuándo habría que volver a mirar esto

- Si se activa el **modo proxied de Cloudflare** (la nube naranja) para `shop` o `qa.shop`.
- Si se mete un **CDN, un WAF o un ingress de K8s** delante del nginx.

En cualquiera de esos casos pasarían a ser dos saltos, la IP más a la derecha sería la del proxy
intermedio y **todos los usuarios compartirían un mismo bucket de rate limit** — cinco fallos
bloquearían el login para todo el mundo. La señal para detectarlo: en los logs, los `WARN` de
rate limit mostrarían siempre la misma IP.

---

## 16. Rate limit desactivado en QA, que está expuesto a internet

**Estado:** ⏭️ No aplica por ahora — decisión tomada el 2026-07-31
**Ubicación:** `application-qa.yml:62-64`

`rate-limit-habilitado: false`. Es intencional y está documentado (evitar autobloqueos en pruebas
manuales), pero QA está publicado en `qa.shop.novedades-jade.com.mx`, así que ahí la fuerza bruta
de login es libre.

**Decisión:** se deja apagado en QA mientras dure esta tanda de correcciones, para poder probar sin
autobloquearse. El flag `seguridad.rate-limit-habilitado` ya existía y todos los rate limits nuevos
de esta tanda están detrás de él, así que **QA los deja pasar y producción los aplica** sin código
extra. Al terminar y validar todo, se quita la línea de `application-qa.yml`.

**Riesgo asumido, anotado también en el YML:** QA está publicado en
`qa.shop.novedades-jade.com.mx`, así que ahí la fuerza bruta de login sigue siendo libre. Y hay un
efecto secundario que conviene tener presente: **con el flag apagado, el camino del 429 nunca se
ejercita en QA** — el día que se encienda en producción, ese código se estrena en producción.

La alternativa evaluada era límites altos por perfil (ej. 50/15 min en QA contra 5 en prod) en vez
de apagado: mismo código corriendo en ambos lados, y "quitarlo al final" sería bajar un número en
vez de cambiar de comportamiento. Se descartó por ahora para no tocar más el
`LoginRateLimiterService`; queda como la mejora natural si algún día molesta.

---

## 17. `enviar-codigo-verificacion` sin límite por IP → spam de correo

**Estado:** ✅ Corregido — 2026-07-31
**Ubicación:** `AuthController.java:203-218`

Limita sólo por username (`verif-usr:`), no por IP. Y acepta username **o** email, así que sirve
para mandar correos a la dirección de otra persona (acotado a 5 cada 15 min por cuenta, pero sin
tope global por origen).

**Qué se hizo:** se agregó el límite por IP (`verif-envio-ip:`) además del que ya existía por
username. Ahora hay tope por origen, no sólo por cuenta destino.

---

## 18. `log.error` en login pierde el stack trace

**Estado:** ✅ Corregido — 2026-07-31
**Ubicación:** `AuthController.java:130`

`log.error("Error inesperado en login: {}", e.getMessage())` registra sólo el mensaje. Cuando algo
falle de verdad en producción no habrá traza para diagnosticarlo.

**Qué se hizo:** la excepción va como último argumento, para que SLF4J imprima el stack completo.
Mismo arreglo aplicado en los `catch` genéricos nuevos de `restablecer-password` y
`verificar-correo`, donde además el mensaje interno ya no se devuelve al cliente.

---

# Lo que está correcto (no tocar)

Conviene dejarlo por escrito para que no se "arregle" por error más adelante:

- **Los roles no se leen del JWT.** `JwtAuthenticationFilter` recarga siempre el usuario desde la
  BD vía `UsuarioDetailsService`, así que el claim `roles` del token es decorativo. No se puede
  escalar privilegios forjando el contenido de un token.
- **`RegistroService.java:31` fuerza `ROLE_USUARIO`.** No hay forma de auto-registrarse como admin.
- **Cookie `HttpOnly`** con `Path` acotado a `/v1/auth` — el refresh token no viaja al resto de la
  API ni es accesible desde JavaScript.
- **BCrypt** para el hash de contraseñas.
- **`olvide-password` no revela si el correo existe** (`PasswordResetService.java:31-37`): responde
  200 siempre, evitando enumeración de cuentas.
- **`Usuario.isEnabled()` no depende de `correoVerificado`** a propósito
  (`Usuario.java:106-113`): así una contraseña incorrecta siempre responde 401 y nunca filtra, vía
  un 403 de "verifica tu correo", que esa combinación usuario/contraseña era válida. Está bien
  pensado y bien documentado.
- **El filtro JWT rechaza refresh tokens usados como access token**
  (`JwtAuthenticationFilter.java:45`).
- **`SecurityConfig` devuelve 401 vs 403 diferenciados**, que es lo que necesita el interceptor del
  front para disparar el refresh.

---

# Qué hacer al desplegar

El código compila (`BUILD SUCCESS`) pero **nada de esto está commiteado ni desplegado**. Antes de
subirlo hay que atender lo siguiente, en este orden.

## 1. Migraciones SQL — obligatorias, antes de arrancar la app

`ddl-auto` está en `none` en todos los perfiles, así que **ninguna tabla ni columna se crea sola**.
Si se despliega sin correr esto, el login truena.

| Script (`src/main/resources/static/`) | Qué hace |
|---|---|
| `migration_intentos_codigo_reset.sql` | Columna `intentos_codigo_reset` en `usuario_modificacion` |
| `migration_intentos_codigo_verificacion.sql` | Columna `intentos_codigo_verificacion` en `usuario_modificacion` |
| `migration_sesion_refresh.sql` | **Tabla nueva** `sesion_refresh` |

Recordar el mapeo de bases del `CLAUDE.md`: `dev` y `qa` → `inventario_key_qa`; `main` →
`inventario_key`. Al ser la misma BD para dev y qa, los scripts se corren **una sola vez** para
ambas ramas.

## 2. Todos los usuarios se van a desloguear

Los refresh tokens actuales no tienen `jti` ni `sessionId`, así que no se pueden renovar contra la
tabla nueva: al intentar refrescar reciben 401 y tienen que volver a iniciar sesión. Es de una sola
vez, pero conviene desplegar en horario de poco movimiento.

Si además se rota `TOKEN_JWT` (recomendado, ver hallazgo 9), el corte es inmediato en vez de al
expirar el access token.

## 3. Dos cosas que el front tiene que saber

- **`passwordTemporal` ahora se fuerza** (hallazgo 14). Ignorar el flag y navegar ya no funciona:
  devuelve 403 en todo lo que no sea cambiar la contraseña.
- **Cambiar la contraseña cierra todas las sesiones** (hallazgo 6), incluida la del propio usuario.
  Después de un cambio de contraseña hay que mandarlo al login.

Ambas van a `CAMBIOS_FRONT.md`, que es la única fuente de verdad de cara al front.

## 4. Pendientes que quedan abiertos

| Qué | Quién lo destraba |
|---|---|
| **#11** — encender `exigir-header-refresh` | Cuando el front mande `X-Requested-With` |
| **#16** — quitar `rate-limit-habilitado: false` de QA | Al terminar de validar esta tanda |
| **#9** — rotar `TOKEN_JWT` en los ambientes que usaran la clave versionada | Decisión + acceso a los secrets |
| **#10** — confirmar que ningún cliente real entraba por los orígenes quitados de producción | Antes del merge a `main` |

## 5. Sin cobertura de tests

El proyecto no tiene tests de estos flujos y no se agregaron en esta tanda. La verificación fue
compilación + revisión. **Lo que más conviene probar a mano en QA**, por orden de riesgo:

1. Login → refresh → logout → intentar refresh otra vez (debe dar 401).
2. Refresh dos veces con el **mismo** token viejo → la segunda debe matar la sesión (detección de
   reuso).
3. Cambiar contraseña estando logueado → la sesión anterior debe morir.
4. Deshabilitar un usuario en BD → su siguiente request debe dar 401 sin esperar 15 minutos.
5. Login con un usuario que tenga `passwordTemporal = true` → todo 403 salvo cambiar contraseña.

Ojo con el punto 1 y 2 en QA: `rate-limit-habilitado: false` no afecta a estos flujos (son de
sesión, no de rate limit), así que sí se pueden probar ahí tal cual está.
