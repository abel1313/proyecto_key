# Cómo conseguir las credenciales de Facebook (Graph API)

Guía para obtener lo que pide `facebook.page-id` y `facebook.page-access-token` (ver
`application-qa.yml` / `application-docker.yml`). Es un trámite de una sola vez por app, no por
cada publicación.

## Lo que necesitas al final

- `FACEBOOK_PAGE_ID` — el ID numérico de la página de Facebook del negocio.
- `FACEBOOK_PAGE_ACCESS_TOKEN` — un token de página de **larga duración** (no expira en 60 días
  si se generó del modo correcto, ver paso 5).

## Requisitos previos

- Ser administrador de la Página de Facebook del negocio.
- Tener (o crear) un **Business Manager** de Meta vinculado a esa página —
  business.facebook.com → Configuración del negocio → Páginas → Agregar. Sin esto, algunos
  permisos no se pueden pedir.

## Paso a paso

### 1. Crear la App en Meta for Developers
1. Entrar a https://developers.facebook.com/apps → "Crear app".
2. Tipo de app: **"Business"**.
3. Vincularla al Business Manager del negocio (se pide durante la creación o después en
   Configuración de la app → Básico).
4. Guardar el **App ID** y el **App Secret** (Configuración → Básico) — no se usan directo en
   el código de este micro, pero los vas a necesitar para pasos posteriores (extender token) y
   para el futuro webhook de comentarios.

### 2. Agregar el producto "Facebook Login" / Graph API Explorer
No hace falta configurar Facebook Login como tal — el token se genera manualmente desde el
**Graph API Explorer** (developers.facebook.com/tools/explorer), que ya viene disponible en
cualquier app.

### 3. Generar un User Access Token con los permisos correctos
1. En Graph API Explorer, elegir tu App arriba a la derecha.
2. "User or Page": elegir **User Token**.
3. En "Permissions", agregar:
   - `pages_show_list`
   - `pages_manage_posts`
   - `pages_manage_engagement` (para cuando se agregue el bot de comentarios)
   - `pages_read_engagement`
4. "Generate Access Token" → loguearte con la cuenta que administra la página → aceptar permisos.

### 4. Obtener el Page ID y el Page Access Token
1. Con el User Token generado, hacer esta llamada en el mismo Explorer (o en la barra de
   consulta): `GET /me/accounts`
2. La respuesta trae la lista de páginas que administra ese usuario, cada una con su propio
   `id` (= **Page ID**) y su propio `access_token` (= **Page Access Token**, corto, ~1-2 horas).

### 5. Convertir el Page Access Token en uno de larga duración
El token del paso 4 es corto. Para hacerlo de larga duración:
1. Primero extender el **User Token** (no el de página) a 60 días:
   ```
   GET https://graph.facebook.com/v21.0/oauth/access_token
     ?grant_type=fb_exchange_token
     &client_id={APP_ID}
     &client_secret={APP_SECRET}
     &fb_exchange_token={USER_TOKEN_CORTO}
   ```
2. Repetir `GET /me/accounts` pero ahora con ese User Token ya extendido → el
   `access_token` de la página que te devuelve esta vez **ya no expira** (mientras el usuario
   siga siendo admin de la página y no revoque el acceso). Ese es el que va en
   `FACEBOOK_PAGE_ACCESS_TOKEN`.

### 6. Cargar las credenciales en el ambiente
- **Dev (local):** editar `application-dev.yml` — los valores `page-id`/`page-access-token` ya
  están ahí como placeholders (`asdasd`), reemplázalos localmente (ese archivo no se sube al
  repo con datos reales, según la convención de este proyecto).
- **QA / Docker:** son variables de entorno — agregar `FACEBOOK_PAGE_ID` y
  `FACEBOOK_PAGE_ACCESS_TOKEN` al `docker-compose`/secreto del ambiente correspondiente, junto a
  las que ya existen (`OPENAI_API_KEY`, `ACCESS_TOKE_MERCADO_PAGO`, etc.).

## Modo desarrollo vs. producción (importante)

Mientras la app de Meta esté en **modo desarrollo** (así nace toda app nueva), la Graph API solo
deja publicar en páginas donde el usuario dueño del token esté agregado como
Admin/Developer/Tester **de la app** — sirve perfecto para probar con tu propia página.

Para que cualquier publicación funcione en "modo producción" real (o si en algún momento el
token es de un usuario que no es dueño de la app), Meta exige **App Review** de los permisos
`pages_manage_posts` / `pages_manage_engagement`: piden un video demostrando el flujo real de la
app y, dependiendo del caso, verificación del negocio (Business Verification). No es inmediato
(puede tardar varios días) — conviene iniciarlo con tiempo si se planea usar con un token que no
sea el tuyo propio. Si el token es siempre de la cuenta admin de la página del propio negocio,
en la práctica el modo desarrollo puede ser suficiente y no hace falta pasar por App Review.
