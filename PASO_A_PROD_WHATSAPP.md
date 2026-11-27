# Paso a producción — WhatsApp con la API de Meta (bot + pantalla)

Estado al 2026-11-27: todo vive en `dev` y `qa` (back y front) y en la rama `feature/whatsapp-cloud-api` de cada repo. **Nada está en `main`/`master`.**
Contexto y fases: `PLAN_WHATSAPP_BOT.md`. Reglas del módulo: `src/main/java/com/ventas/key/hexagonal/whatsapp/README.md` (R1–R22+).

> Regla de oro: **primero la migración, luego el back, luego el front** (CLAUDE.md, «llevar lo de QA a prod SIN borrar nada»). Jamás restaurar un dump en prod.

## 0. Antes de empezar (lo que tiene que estar listo)
- [ ] Probado en QA: webhook verificado, el bot contesta, pantalla Sistema → WhatsApp, horario con días, ubicación (solo dirección + login), pregunta desconocida → pasa con una persona. Casos en `TESTS_PENDIENTES.md`.
- [ ] Decisiones del dueño abiertas (ver §6) contestadas.
- [ ] Respaldo: `mysqldump -u root -p --single-transaction --routines --triggers inventario_key | gzip > ~/prod_antes_$(date +%Y%m%d_%H%M).sql.gz` y confirmar que pesa megas.

## 1. Migraciones SQL en `inventario_key` (prod), en este orden
El archivo `.sql` NO está en el VPS: llevarlo con `scp` desde la PC del dueño (o `cat > ... <<'EOF'`) y comprobar con `ls -l`. Comprobar la base: `SELECT DATABASE();` debe decir `inventario_key` (sin sufijo). Prompt `ubuntu@vps…$` para `mysql -u root -p inventario_key < archivo.sql`; el prompt `mysql>` solo para SQL.

| # | Script | Qué hace | Verificación |
|---|---|---|---|
| 1 | `migration_whatsapp_cloud.sql` | Tablas `whatsapp_cliente` y `whatsapp_mensaje` (utf8mb4) y la pantalla `admin/whatsapp` (Ver y Editar para ROLE_ADMIN). Corrida en QA con MySQL real (2026-11-27) | El `SELECT` del final del script |
| 2 | `migration_horario_fin_semana.sql` | Columnas `hora_apertura_fin_semana` y `hora_cierre_fin_semana` en `configuracion_negocio` (vacías = igual que entre semana). **Probada en MySQL en QA (2026-11-27): 2 filas** | 2 filas en `information_schema` |

- Son idempotentes y solo agregan. Si dice «ya existe», es normal.
- La #2 es obligatoria antes del back: la entidad ya mapea las columnas y sin ellas **fallan las consultas de Negocio** (login/estado del negocio).
- Después de la #1: **volver a entrar** (las pantallas viajan en el JWT) y comprobar que ROLE_ADMIN ve «WhatsApp».
- Anotarlas en la tabla «Migraciones ya corridas» de `CLAUDE.md` con fecha.

## 2. Variables de entorno del deployment de prod (`-n default`)
El deployment lee **cada variable por separado** (`secretKeyRef`): no basta con agregarla al Secret. Verificar midiendo caracteres, nunca `printenv | grep`:
`kubectl exec -n default deploy/proyecto-key-deployment -- sh -c 'echo ${#WHATSAPP_CLOUD_TOKEN}'`

| Variable | Qué es | Notas |
|---|---|---|
| `WHATSAPP_CLOUD_TOKEN` | Token **permanente** de un usuario del sistema de Meta | Un token temporal da error 190 en cada envío. Se carga con `read -rsp` en la terminal del VPS, nunca en el chat ni en el repo |
| `WHATSAPP_PHONE_NUMBER_ID` | Id del número en Meta | Del número real de producción |
| `WHATSAPP_VERIFY_TOKEN` | Texto aleatorio para verificar el webhook | Se pone igual en Meta. **No** usar uno adivinable |
| `WHATSAPP_APP_SECRET` | Secreto de la app de Meta (firma del webhook) | Sin él el webhook rechaza (403) |
| `WHATSAPP_API_VERSION` | Versión de Graph API | Default `v26.0` |
| `WHATSAPP_METODO_PAGO_ACTIVO` | `true` solo cuando Meta ya tiene método de pago | Sin él no se mandan plantillas |
| `WHATSAPP_BOT_PEDIDOS_HABILITADO` | Interruptor de que el bot genere Apartados | **Dejar `false`** al subir; encender solo cuando el dueño lo decida |
| `WHATSAPP_BOT_MAX_APARTADOS_ABIERTOS` | Tope de Apartados por número | Default 0 = ninguno (recomendado 3 cuando se encienda) |
| `WHATSAPP_BOT_USUARIO_ID` | Usuario con el que el bot registra las ventas | Definir cuál (ver §6) |
| `WHATSAPP_BOT_PAUSA_MINUTOS` | Pausa del bot al tomar una persona | Default 30 |
| `APP_TIENDA_BASE_URL` | Base de la tienda (`https://shop.novedades-jade.com.mx`) | Sin ella el bot no sabe a dónde mandar al login/privacidad |
| `CHAT_ADMIN_EMAIL` | A dónde llega el aviso «necesita una persona» | Confirmar que la lee alguien |
| `MAIL_USERNAME` / `MAIL_PASSWORD` | Cuenta SMTP | Necesarias para los avisos por correo |

Después de cambiar variables: `kubectl rollout restart deployment proyecto-key-deployment -n default`.

## 3. Meta (lo hace el dueño en developers.facebook.com / Business Manager)
- [ ] Webhook de prod: URL `https://<backend prod>/mis-productos/v1/whatsapp/webhook` (misma que se usó en QA, cambiando el host) + el `WHATSAPP_VERIFY_TOKEN` de prod; suscribir `messages`.
- [ ] App en modo **Publicada**; token permanente del usuario del sistema con permisos de WhatsApp.
- [ ] **Método de pago** en la cuenta de WhatsApp Business (para plantillas) y luego `WHATSAPP_METODO_PAGO_ACTIVO=true`.
- [ ] Plantillas enviadas a aprobación (promo solo con **BAJA** disponible antes — Fase 5).
- [ ] **Nunca borrar ni desvincular el número** de WhatsApp.
- [ ] Datos del negocio capturados en Sistema → Negocio y contactos: dirección, horario lunes–viernes **y sábado/domingo**, redes.

## 4. Git: cómo subir SIN arrastrar otras cosas
`dev`/`qa` llevan por delante trabajo de otros (renombrados, redes sociales, etc.) que `main` no tiene. Por eso **no** es un `git merge qa` normal:
1. Revisar qué commits de WhatsApp hay: back `git log origin/qa --oneline --grep=WhatsApp` y la rama `feature/whatsapp-cloud-api` de cada repo.
2. Back: `git checkout main && git pull`, `git checkout -b promo/whatsapp`, `git cherry-pick` de los commits de la rama `feature/whatsapp-cloud-api` (en orden), `mvn -q compile`, merge `--no-ff` a `main`, push. Dispara `producto-actions.yml` (deploy en ~1–2 min).
3. Front: lo mismo hacia `master` (`ng build --configuration=production`), dispara `proyecto-front-actions.yml`.
4. Bajar lo que cambió en `main` a `qa` y `dev` si hubo hotfix en medio.
5. Orden de despliegue: migraciones → variables → back → front. Si el back sube sin variables, WhatsApp falla pero **el resto del sistema sigue** (se vio en QA: sin token solo falla WhatsApp).

## 5. Después de desplegar — verificación en prod
1. Entrar con ROLE_ADMIN (volver a iniciar sesión) → Sistema → WhatsApp carga sin error.
2. Sistema → Negocio y contactos carga (si no, falta la migración #2).
3. Mandar un WhatsApp al número de prod: llega al panel, el bot contesta con la presentación «Jade, la asistente virtual de Novedades Jade».
4. «¿Dónde se ubican?» → solo la dirección, sin mapa; «mándame la ubicación» → login de la tienda.
5. «¿Qué horario tienen?» → lunes a viernes y sábado/domingo como están capturados.
6. Algo que no sabe → **no inventa**, pasa con una persona (dentro de horario: correo al admin; fuera: texto del horario).
7. Revisar el log del pod: sin errores de firma, 190 o 131047.

## 6. Decisiones del dueño que siguen abiertas
- [ ] ¿El bot respeta el botón Abrir/Cerrar negocio?
- [ ] Tope de Apartados por número (recomendado 3) y cuándo encender `WHATSAPP_BOT_PEDIDOS_HABILITADO`.
- [ ] Horario de **sábado y domingo** (o si cierran).
- [ ] 6 dudas del agente de desarrollo (precio de catálogo vs campaña, «quién recibe» al recoger, verificación de correo con código, duración del resumen 30 min, usuario «vendedor» del bot, correo solo a `CHAT_ADMIN_EMAIL`).
- [ ] Fases 3 (campañas/cancelación), 4 (fotos) y 5 (BAJA/plantillas/pago): no están construidas; no bloquean subir Fases 0–2.

## 7. Si algo sale mal (marcha atrás)
- El bot hace daño o contesta mal: poner `WHATSAPP_BOT_PEDIDOS_HABILITADO=false` y reiniciar; o quitar el webhook en Meta (no se toca el número).
- Las migraciones solo agregan: no hace falta revertirlas; el back viejo las ignora.
- Revertir el deploy: `kubectl rollout undo deployment proyecto-key-deployment -n default`.

## 8. Limitaciones conocidas (por si preguntan)
No probado con IA real, correo real ni Meta real de punta a punta en prod; el catálogo se corta a 1000 artículos antes del filtro de imagen; `HorarioDeAtencionNegocioAdapter` lee con `obtenerConfig()` (puede escribir al leer); riesgo de privacidad N3 por número reasignado (documentado en el README).
