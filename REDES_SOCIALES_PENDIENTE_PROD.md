# Redes sociales — qué tenemos, qué falta y cómo activarlo en producción

Este documento resume el estado real de la feature de redes sociales (Facebook, Instagram,
TikTok) al 2026-08-21. El código **vive en `dev`/`qa`, no en `main`** — se excluyó a propósito del
merge de qa→main del 2026-08-21 (todo lo demás sí se llevó) porque no está listo para producción:
faltan credenciales de prod, migraciones sin correr, y la revisión de Meta sigue pendiente. Este
archivo es la guía para cuando se decida retomarlo.

## 1. Qué tenemos ya (probado en QA)

| Feature | Estado |
|---|---|
| Bot de respuesta a comentarios — Facebook | ✅ Probado en vivo, contesta en <10s |
| Bot de respuesta a comentarios — Instagram | ✅ Probado en vivo, contesta en <20s |
| Bot de respuesta a comentarios — TikTok | ❌ Descartado (ver nota abajo) |
| Bot de mensajes directos (DM) — Instagram | ⚠️ Código listo, **nunca se probó en vivo** |
| Publicar foto en Facebook / Instagram | ✅ Implementado |
| Publicar Reel en Facebook / Instagram | ✅ Implementado (subida reanudable) |
| Publicar video en TikTok (modo Upload) | ⚠️ Publica y la API confirma recepción, pero **el video no aparece en el celular** (bug sin resolver, pausado) |
| Programación unificada de publicaciones | ✅ Implementado, job propio (no usa el scheduler nativo de Meta) |
| Hashtags default por red | ✅ Implementado |
| Límite de 20 mensajes/hora del chatbot | ✅ Ya se llevó a `main` — es genérico, protege también el chat web, no depende de redes sociales |

**TikTok — comentarios descartados:** investigado con fuentes oficiales. El Content Posting API
(el que ya se usa para publicar video) no tiene endpoint de comentarios; esos viven en un producto
totalmente distinto ("TikTok API for Business", suite de Ads de pago). No vale la pena el esfuerzo
por ahora.

## 2. Estado real de permisos/revisión por plataforma

**Facebook + Instagram** (misma App de Meta, mismo Business Manager, página **NovedadesJade**):
- El permiso `instagram_manage_comments` **ya fue aprobado** y el webhook de comentarios está
  suscrito y activo (confirmado el 2026-08-20 vía `GET /{app-id}/subscriptions`).
- **La app sigue en modo desarrollo** — la revisión completa (App Review / Business Verification)
  está enviada pero sin aprobar. Mientras siga así: el bot **solo contesta si quien comenta tiene
  rol de Admin/Developer/Tester en la app** — un cliente público real todavía no recibe respuesta.
  Esto es lo único que realmente bloquea salir a producción con esta feature.
- El webhook también quedó suscrito al campo `messages` de Instagram (para el bot de DM), pero ese
  flujo nunca se probó con un mensaje real.

**TikTok:**
- Cuenta `novedadesjade8` autorizada (OAuth completado), `access_token`/`refresh_token`/`open_id`
  guardados en BD de QA, el refresh automático antes de expirar (~24h) funciona.
- App también "sin auditar" del lado de TikTok — mientras no pase su propio "App audit", el modo
  Direct Post fuerza los videos a visibilidad privada; por eso se usa el modo Upload (sube al
  inbox del usuario, alguien lo publica manualmente desde el celular) — y ese es justamente el paso
  que está fallando (el video no llega al inbox pese a que la API dice que sí).

## 3. Qué falta para activar esto en producción

### 3.1 Decidir credenciales: ¿mismas de QA o nuevas para prod?

La página de Facebook e Instagram usada en QA (`NovedadesJade`, page ID `645820348605806`,
Instagram account ID `17841444237033427`) **es la página real del negocio**, no una de pruebas —
así que lo más probable es que producción deba usar exactamente las mismas credenciales (mismo
Page Access Token de larga duración, mismo App Secret, mismo Webhook Verify Token, mismo Instagram
Account ID). **Confirmar esto explícitamente antes de desplegar** — si se decide separar Meta App
de prod y QA, hay que repetir el trámite completo de `FACEBOOK_SETUP.md` (rama qa/dev) para una
app nueva.

Mismo dilema con TikTok: ¿se reusa la cuenta `novedadesjade8` ya autorizada, o hace falta una
cuenta/app distinta para prod? El Client Key/Secret son de la app TikTok (reutilizables); el
access/refresh token es específico de la cuenta que se autorizó.

Las guías completas de cómo se sacó cada credencial desde cero (por si hay que repetir el trámite)
están en `FACEBOOK_SETUP.md` y `TIKTOK_SETUP.md`, en la rama `qa`/`dev` (no están en `main`).

### 3.2 Dónde meter cada credencial

`application-docker.yml` (el profile real que usa producción) necesita estos bloques nuevos —
existen ya en la rama `qa`, se excluyeron a propósito de este merge:

```yaml
facebook:
  page-id: ${FACEBOOK_PAGE_ID:}
  page-access-token: ${FACEBOOK_PAGE_ACCESS_TOKEN:}
  app-secret: ${FACEBOOK_APP_SECRET:}
  webhook-verify-token: ${FACEBOOK_WEBHOOK_VERIFY_TOKEN:}
  api-version: v21.0

instagram:
  account-id: ${INSTAGRAM_ACCOUNT_ID:}

tiktok:
  client-key: ${TIKTOK_CLIENT_KEY:}
  client-secret: ${TIKTOK_CLIENT_SECRET:}
```

Los valores reales van como **variables de entorno del deployment de producción** (mismo lugar
donde ya están `OPENAI_API_KEY`, `ACCESS_TOKE_MERCADO_PAGO`, etc.) — nunca hardcodeados en el yml:

- `FACEBOOK_PAGE_ID`, `FACEBOOK_PAGE_ACCESS_TOKEN`, `FACEBOOK_APP_SECRET`
- `FACEBOOK_WEBHOOK_VERIFY_TOKEN` — este **lo inventas tú** (no lo da Meta); tiene que ser
  exactamente el mismo valor que registres en el portal de Meta al dar de alta la URL del webhook.
- `INSTAGRAM_ACCOUNT_ID`
- `TIKTOK_CLIENT_KEY`, `TIKTOK_CLIENT_SECRET`
- El access/refresh token de TikTok **no** va en variable de entorno — se guarda en la tabla
  `tiktok_token` (vía `migration_tiktok_token.sql`) porque expira cada 24h y el back lo refresca
  solo. Hay que correr una vez el paso 4-5 de `TIKTOK_SETUP.md` (login OAuth manual) contra la
  cuenta que se vaya a usar en prod para sembrar esa tabla, igual que se hizo en QA.

**URL del webhook a registrar en el portal de Meta para producción** (ajustar si el dominio real
de prod es otro):
```
https://backend.novedades-jade.com.mx/mis-productos/v1/redes-sociales/facebook/webhook
```

### 3.3 Migraciones a correr contra `inventario_key` (prod) — en este orden

Ninguna se ha corrido en prod. `ddl-auto` está en `none`, no se crean solas — hay que correrlas a
mano, en este orden (por dependencias entre tablas):

1. `migration_publicacion_social.sql`
2. `migration_tiktok_token.sql`
3. `migration_publicacion_social_programada.sql`
4. `migration_hashtags_default.sql`
5. `migration_publicacion_social_variante_opcional.sql`
6. `migration_comentario_social.sql`
7. `migration_comentario_pausa.sql`
8. `migration_mensaje_directo.sql` — **solo si se activa también el bot de DM de Instagram**; si
   se decide lanzar primero solo comentarios/publicación, esta se puede dejar para después.

Los 8 archivos están en `src/main/resources/static/` de la rama `qa` (no están en `main`).

### 3.4 Nginx de producción

`VPS_AUDITORIA.md` ya lo señala como pendiente: `/etc/nginx/sites-available/backend` (prod) **no
tiene** `client_max_body_size`, a diferencia de `backend-qa` que ya se ajustó a `200M` el
2026-08-18. Sin esto, subir video a Facebook/Instagram/TikTok va a fallar con `413 Request Entity
Too Large` antes de llegar al pod. Aplicar en la VPS cuando se despliegue:

```bash
sudo sed -i '2a\    client_max_body_size 200M;' /etc/nginx/sites-available/backend
sudo nginx -t && sudo systemctl reload nginx
```

### 3.5 SecurityConfig — 2 reglas que se quitaron del merge a `main`

Al traer el código de `redessociales` de vuelta, hay que re-agregar estas dos reglas en
`SecurityConfig.java` (se sacaron limpiamente de este merge, el resto de cambios de seguridad de
ese archivo sí se llevó):

```java
// ── Webhook Facebook -- comentarios (llamada sin auth desde Meta,
// validado por firma X-Hub-Signature-256 dentro del controlador) ──────
.requestMatchers("/v1/redes-sociales/facebook/webhook").permitAll()
```
(justo después de la regla del webhook de MercadoPago)

```java
// ── Redes sociales (publicar variantes en Facebook) ───────────────
.requestMatchers("/v1/redes-sociales/**").hasRole("ADMIN")
```
(junto a la regla del dashboard)

### 3.6 Pendientes funcionales antes de anunciarlo a clientes reales

- **App Review de Meta sin aprobar** — mientras siga así, el bot de comentarios de Facebook/
  Instagram solo responde a Admin/Developer/Tester de la app, no a clientes públicos. Esto es lo
  que de verdad bloquea el lanzamiento real, no el código.
- **DM de Instagram sin probar en vivo** — nunca se mandó un mensaje directo de prueba real, solo
  se confirmó la suscripción del webhook al campo `messages`.
- **Bug de TikTok sin resolver** — los videos se confirman recibidos por la API pero no aparecen
  en el inbox/celular de la cuenta. Antes de anunciar "publicación automática en TikTok" habría
  que resolver esto o aceptarlo como limitación conocida.

## 4. Checklist para cuando se retome

- [ ] Confirmar si prod usa la misma App/página de Meta y la misma cuenta de TikTok que QA, o si
      hace falta tramitar credenciales separadas.
- [ ] Traer a `main` el código excluido: paquete `redessociales`, `FacebookWebhookController`,
      `RedesSocialesController`, `PublicacionSocialScheduler`, el método
      `responderComentarioRedSocial` de `ChatbotService`, `FACEBOOK_SETUP.md`, `TIKTOK_SETUP.md`.
- [ ] Agregar los bloques `facebook:`/`instagram:`/`tiktok:` a `application-docker.yml` de `main`.
- [ ] Re-agregar las 2 reglas de `SecurityConfig.java` (sección 3.5).
- [ ] Cargar las variables de entorno reales en el deployment de producción.
- [ ] Correr las 8 (o 7, si se pospone el DM) migraciones contra `inventario_key`, en el orden de
      la sección 3.3.
- [ ] Aplicar el fix de nginx en `/etc/nginx/sites-available/backend` (sección 3.4).
- [ ] Registrar/confirmar la URL del webhook en el portal de Meta para el dominio de prod.
- [ ] Si se usa una cuenta de TikTok nueva para prod, correr el login OAuth manual para sembrar
      `tiktok_token`.
- [ ] Decidir si se lanza ya con la limitación de "solo Admin/Developer/Tester" de Meta, o se
      espera a que la App Review se apruebe.
