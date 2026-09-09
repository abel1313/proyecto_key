# Cómo conseguir las credenciales de TikTok (Content Posting API)

Guía para arrancar desde cero con TikTok, mismo espíritu que `FACEBOOK_SETUP.md` con Meta. A
diferencia de Facebook/Instagram, **es una plataforma nueva para este proyecto** — no hay
credenciales previas ni código que reusar, y algunos detalles de este documento pueden necesitar
ajuste una vez que estés dentro del portal real de TikTok (su documentación cambia seguido y no
tengo forma de verificarla en vivo desde aquí). Trátalo como punto de partida, no como receta
exacta paso a paso.

## Lo que vamos a necesitar al final

- `TIKTOK_CLIENT_KEY` / `TIKTOK_CLIENT_SECRET` — credenciales de la app, de Configuración → Básico.
- `TIKTOK_ACCESS_TOKEN` — token de acceso de la cuenta del negocio. **Corta duración (~24h)** —
  a diferencia de Facebook, TikTok no tiene un token que "no expira nunca".
- `TIKTOK_REFRESH_TOKEN` — para renovar el access token sin volver a pedir login (dura ~1 año,
  y se refresca solo cada vez que se usa). El back va a necesitar guardar este valor y refrescar
  el access token automáticamente antes de publicar, no es un dato que se cargue una sola vez y
  se olvide como el de Facebook.
- El **Open ID** de la cuenta de TikTok del negocio (identificador de usuario que devuelve TikTok
  al autorizar, lo vamos a necesitar igual que el `page-id` de Facebook).

## Requisito previo — el gran diferenciador con Meta

**La cuenta de TikTok del negocio tiene que ser cuenta Business** (no personal). Se convierte
desde la misma app de TikTok: Perfil → Configuración → "Cambiar a cuenta Business/Creator" — es
gratis e inmediato, a diferencia de vincular Instagram a una página de Facebook, aquí no depende
de nada más.

## Paso a paso

### 1. Crear cuenta de desarrollador y la app
1. Entrar a **https://developers.tiktok.com** → crear cuenta de desarrollador (puede pedir
   verificar correo/teléfono).
2. "Manage apps" → "Create an app".
3. Llenar nombre, categoría, descripción — cosas básicas, sin trámite especial en este paso.
4. Guardar **Client Key** y **Client Secret** que te da la app recién creada.

### 2. Agregar el producto "Content Posting API"
1. Dentro de la app, en la sección de productos/APIs, agregar **"Content Posting API"** (puede
   aparecer junto a "Login Kit" — Login Kit también hace falta, es el que da el login/OAuth).
2. Va a pedir elegir los **scopes** (permisos) que necesitas. Los que aplican a este proyecto:
   - `user.info.basic` — datos básicos de la cuenta (para confirmar que se conectó la correcta).
   - `video.publish` — publicar directamente en el feed del usuario.
   - Puede que también pida `video.upload` como scope separado (subir sin publicar directo,
     el modo "borrador" que menciono abajo) — revisar qué combinación exige el portal en el
     momento, esto es lo que más puede haber cambiado desde que se escribió este documento.

### 3. Configurar el redirect URI de OAuth
TikTok usa OAuth2 estándar (a diferencia del Graph API Explorer de Meta, que genera el token ahí
mismo sin salir del navegador): la app necesita una URL de retorno registrada.
1. En la configuración de la app, agregar una **Redirect URI** — puede ser cualquier URL que
   controlemos, incluso una que no exista todavía (ej. `https://shop.novedades-jade.com.mx/tiktok/callback`),
   el objetivo es solo capturar el `code` que manda TikTok al final del login, aunque sea a mano
   copiándolo de la barra de direcciones si no hay una página real esperando ahí.
2. Guardar esa URL — se necesita exacta (carácter por carácter) tanto aquí como en la llamada de
   autorización del paso 4.

### 4. Autorizar la cuenta del negocio y sacar el `code`
1. Armar esta URL (reemplazando client_key y redirect_uri) y abrirla en el navegador **logueado
   con la cuenta Business del negocio**:
   ```
   https://www.tiktok.com/v2/auth/authorize/
     ?client_key={CLIENT_KEY}
     &scope=user.info.basic,video.publish
     &response_type=code
     &redirect_uri={REDIRECT_URI}
     &state=cualquier_valor_random
   ```
2. TikTok pide loguearse y autorizar los permisos. Al aceptar, redirige a tu `redirect_uri` con
   un parámetro `?code=...` en la URL — cópialo, dura poco (unos minutos).

### 5. Cambiar el `code` por el access token + refresh token
```
POST https://open.tiktokapis.com/v2/oauth/token/
Content-Type: application/x-www-form-urlencoded

client_key={CLIENT_KEY}
&client_secret={CLIENT_SECRET}
&code={CODE_DEL_PASO_4}
&grant_type=authorization_code
&redirect_uri={REDIRECT_URI}
```
La respuesta trae `access_token`, `refresh_token`, `open_id` y `expires_in` (segundos, ~24h para
el access token). Guardar los 3 primeros — con eso ya se puede cargar todo en el ambiente.

### 6. Cargar las credenciales en el ambiente
Mismo criterio que Facebook: `TIKTOK_CLIENT_KEY`/`TIKTOK_CLIENT_SECRET` como placeholders locales
en `application-dev.yml` (reemplazar con los reales sin subirlos al repo), y como variables de
entorno en QA/Docker. `TIKTOK_ACCESS_TOKEN`/`TIKTOK_REFRESH_TOKEN` van a necesitar guardarse en
algún lugar que el back pueda actualizar solo (no tiene sentido como variable de entorno fija, ya
que el access token expira cada 24h) — probablemente una tabla nueva o reusar el patrón de
`ConfiguracionNegocio`, a definir cuando se llegue al código.

## Sandbox/pruebas vs. producción real — el equivalente al "modo desarrollo" de Meta

TikTok tiene su propia versión de esa restricción, y es más estricta que la de Meta:

- **Mientras la app no pase la auditoría de TikTok ("App audit" / "unaudited client"):** el
  Content Posting API en modo Direct Post (publicar directo, sin que el usuario confirme en la
  app) solo funciona con cuentas que agregues explícitamente como **testers** en el portal de
  desarrollador — igual que Meta permite publicar sin revisión mientras el usuario sea
  Admin/Developer/Tester de la app. Si la cuenta del negocio se agrega como tester, alcanza para
  probar sin pasar por auditoría todavía.
- **Un detalle propio de TikTok, sin equivalente en Meta:** mientras la app esté "sin auditar",
  los videos publicados por Direct Post **se fuerzan a visibilidad privada** (`SELF_ONLY`) sin
  importar qué visibilidad pidas en la llamada — es una restricción de la plataforma, no del
  código. Para que salgan públicos de verdad hace falta pasar la auditoría.
- **Alternativa sin auditoría, si se necesita publicar público antes de pasar el review:** el
  modo **"Upload"** (`/v2/post/publish/inbox/video/init/`) sube el video como borrador al inbox
  de la app de TikTok del usuario, y la persona lo termina de publicar manualmente desde su
  celular. No publica solo, pero tampoco tiene la restricción de visibilidad privada. Puede ser
  el punto de partida mientras se tramita la auditoría completa.
- **Auditoría completa** ("Content Posting API audit"): se pide desde el portal, piden explicar el
  caso de uso y puede que un video de demostración — no tengo referencia de cuánto tarda TikTok
  en revisar (a diferencia de Meta, este proyecto nunca pasó por ahí antes). Conviene iniciarla
  con tiempo en cuanto se tenga la app básica funcionando en modo tester/sandbox.

## Qué falta decidir antes de escribir código

1. **Confirmar en el portal real** que los nombres de los scopes/endpoints de este documento
   siguen vigentes — TikTok ha renombrado cosas de su API con cierta frecuencia.
2. **Dónde vive el refresh token** y quién dispara el refresh automático antes de que expire cada
   24h (a diferencia de Facebook, esto si necesita lógica nueva, no es "cargar una vez y ya").
3. **Direct Post vs. Upload** como primera versión — Direct Post da mejor experiencia pero exige
   auditoría para salir público; Upload funciona ya pero requiere que alguien termine de publicar
   desde el celular. Se puede empezar por Upload para probar el flujo completo del lado del back
   sin esperar a la auditoría, y cambiar a Direct Post cuando esté aprobada.
