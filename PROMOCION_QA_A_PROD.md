# Promoción QA → Prod — checklist de preparación

**Estado:** 🟡 en preparación, NO ejecutar el merge todavía. Este documento se arma con
anticipación para saber qué falta antes de poder pasar `qa` a `main`, mismo estilo que
`PENDIENTES_DESPLIEGUE_AUTH.md` (esa era de la tanda de seguridad de auth, ya cerrada).

**Cómo se usa:** vos vas revisando cada sección, corriendo lo que se pueda revisar desde ya
(las verificaciones no tocan nada, son solo consultas), y marcás ✅/❌ o agregás una nota debajo
de cada ítem. Yo voy actualizando el documento según lo que confirmes. Cuando todo quede en
✅ o explícitamente descartado, recién ahí se arma el plan puntual del merge/cherry-pick.

Leyenda: ⬜ sin revisar · ✅ confirmado en prod/VPS · ❌ falta correr/ajustar · 🚫 bloqueado (no
va a prod en esta tanda) · 📝 nota

---

## 0. Contexto — por qué esto no es un merge directo

Igual que el 2026-08-21 (ver `CLAUDE.md`, sección "Feature que no va a llegar a main"): **redes
sociales sigue mezclada en `dev`/`qa` sin rama propia** (confirmado — sus migraciones y commits
siguen entrando directo a `qa`, no a una `feature/` aparte). Mientras las credenciales de prod y
el App Review de Meta sigan sin resolverse, promover `qa` a `main` **no puede ser un
`git merge qa` normal** — hay que repetir la exclusión manual (cherry-pick de los commits que no
son de redes sociales) como se hizo esa vez.

**Esta sección del plan (cuáles commits sí/cuáles no) se arma en detalle recién cuando decidamos
la fecha real del merge** — ahora mismo cambia todos los días con cada commit nuevo a `qa`, así
que armarla hoy quedaría desactualizada. Lo que sí es estable y vale la pena dejar listo desde
ya son las secciones 1 y 2 de abajo.

**Aclaración 2026-09-04:** las migraciones de la sección 1.c (más abajo) se ejecutaron en QA y
prod **directamente, adelantadas al código** — el código de `feature/permisos-finos` sigue sin
fusionar a `dev`/`qa`/`main`, esto es solo para que el documento quede al día con lo que ya
existe en la base de datos. No se tocó ninguna rama de git.

**Ramas de esta sesión (`feature/permisos-finos`, `feature/filtro-seguridad`) NO son parte de
esta promoción** — siguen en pruebas aparte, ver `ROADMAP_PRUEBAS_PERMISOS_TIENDA_Y_FILTRO_SEGURIDAD.md`.
No se mezclan a `qa` hasta que las apruebes.

---

## 1. Scripts SQL — inventario de lo agregado en `qa` desde el último merge a `main`

Base: diff de archivos `migration_*.sql` entre el commit del último merge qa→main
(`287fbeb`, 2026-08-21) y `origin/qa` hoy. Correr cada verificación contra la base de
**producción** (`inventario_key`, sin sufijo — ver mapeo de `CLAUDE.md`).

### 1.a — Seguros para promover (no son de redes sociales)

| # | Script | Qué hace | Verificación en prod (`inventario_key`) | Estado |
|---|---|---|---|---|
| 1 | `migration_menu_submenu.sql` | Crea tablas `menu` y `submenu` (catálogo de pantallas) | `SHOW TABLES LIKE 'submenu';` → debe existir | ✅ existe |
| 2 | `migration_rol_submenu_usuario_submenu.sql` | Crea `rol_submenu` y `usuario_submenu` | `SHOW TABLES LIKE 'rol_submenu';` | ✅ existe |
| 3 | `migration_permiso_escritura.sql` | Crea `rol_submenu_escritura` (permiso Editar separado de Ver) | `SHOW TABLES LIKE 'rol_submenu_escritura';` | ✅ existe |
| 4 | `migration_accion_submenu.sql` | Crea `accion_submenu` y `rol_accion` + siembra las 6 acciones originales de Modelos | `SHOW TABLES LIKE 'accion_submenu';` | ✅ existe |
| 5 | `migration_fix_submenu_gestion_menu.sql` | Agrega las filas de "Menús y submenús"/"Gestión de roles" al catálogo (si no, nadie puede entrar a asignar permisos) | `SELECT ruta FROM submenu WHERE ruta IN ('gestion-menu','gestion-menu/roles');` → debe traer 2 filas | ✅ existen 2 |
| 6 | `migration_filtros_granulares.sql` | Agrega columna `descripcion` a `accion_submenu` + separa "filtros-admin" en 9 checkboxes (Modelos y Tienda) | `SELECT COUNT(*) FROM accion_submenu WHERE clave LIKE 'filtro-%';` → 18 (9 x 2 pantallas) | ✅ existen 18 |
| 7 | `migration_descripcion_submenu.sql` | Agrega columna `descripcion` a `submenu` | `SHOW COLUMNS FROM submenu LIKE 'descripcion';` | ✅ existe |
| 8 | `migration_descripcion_acciones_modelos.sql` | Llena `descripcion` de las 5 acciones originales de Modelos | `SELECT clave FROM accion_submenu WHERE submenu_id = (SELECT id FROM submenu WHERE ruta='productos/buscar') AND descripcion IS NULL;` → 0 filas | ✅ query vacía (0 filas = correcto) |
| 9 | `migration_tema_variable.sql` | Crea tabla `tema_variable` (catálogo de Personalización) | `SHOW TABLES LIKE 'tema_variable';` | ✅ existe |
| 10 | `migration_tema_variable_card_header_footer_v2.sql` | Agrega tokens `card-header-bg`/`card-footer-bg` | `SELECT clave FROM tema_variable WHERE clave IN ('card-header-bg','card-footer-bg');` → 2 filas | ✅ existen 2 |
| 11 | `migration_tema_variable_card_header_text.sql` | Agrega token `card-header-text` | `SELECT 1 FROM tema_variable WHERE clave='card-header-text';` | ✅ existe |
| 12 | `migration_tema_variable_limpiar_card_header_footer.sql` | Borra 2 tokens viejos que no controlaban nada real (correr ANTES de la v2 de arriba si se aplican juntas, revisar orden) | `SELECT 1 FROM tema_variable WHERE clave IN ('card-header-bg','card-footer-bg');` → 0 filas (antes de correr la v2) | ✅ ya no existen (correcto, se borraron) |
| 13 | `migration_tema_variable_pk_semanticos.sql` | Agrega 12 tokens `pk-success/-warning/-danger/-info` (+ variantes) | `SELECT COUNT(*) FROM tema_variable WHERE grupo='Estados';` → 12 | ✅ hay 12 (con la consulta corregida — confirmado, sí está corrido, era mi consulta original la que estaba mal) |
| 14 | `migration_tema_variable_placeholder.sql` | Agrega token `input-placeholder` | `SELECT 1 FROM tema_variable WHERE clave='input-placeholder';` | ✅ existe (1 en ambos) |
| 15 | `migration_lugar_entrega_anillo.sql` | Crea tabla `lugar_entrega_anillo` (cobro por distancia) | `SHOW TABLES LIKE 'lugar_entrega_anillo';` | ✅ existe en ambos |
| 16 | `migration_lugar_entrega_centroide.sql` | Agrega `latitud`/`longitud` a `lugares_entrega` | `SHOW COLUMNS FROM lugares_entrega LIKE 'latitud';` | ✅ existe en ambos |
| 17 | `migration_lugar_entrega_recoger_en_tienda.sql` | Agrega `es_recoger_en_tienda` a `lugares_entrega` | `SHOW COLUMNS FROM lugares_entrega LIKE 'es_recoger_en_tienda';` | ✅ existe |
| 18 | `migration_lugar_entrega_dia_entrega_semanal.sql` | Agrega `dia_entrega_semanal` a `lugares_entrega` | `SHOW COLUMNS FROM lugares_entrega LIKE 'dia_entrega_semanal';` | ✅ existe |
| 19 | `migration_pedido_ubicacion_entrega.sql` | Agrega `latitud`/`longitud`/`referencias` a `pedidos` | `SHOW COLUMNS FROM pedidos LIKE 'referencias';` | ✅ existe |
| 20 | `migration_fecha_creacion_producto_variante.sql` | Agrega `fecha_creacion` a `producto` y `variantes` | `SHOW COLUMNS FROM variantes LIKE 'fecha_creacion';` | ✅ existe |
| 21 | `migration_umbral_stock_bajo.sql` | Agrega columna a `configuracion_negocio` (umbral de stock bajo) | `SHOW COLUMNS FROM configuracion_negocio LIKE 'umbral%';` | ✅ existe |
| 22 | `migration_hashtags_default.sql` | Crea tabla `hashtags_default` | `SHOW TABLES LIKE 'hashtags_default';` | ✅ existe |
| 23 | `migration_logo.sql` | Crea tabla `logo` | `SHOW TABLES LIKE 'logo';` | ✅ existe |
| 24 | `migration_direcciones_autoincrement.sql` | `direcciones.id` pasa a AUTO_INCREMENT | `SHOW COLUMNS FROM direcciones LIKE 'id';` → columna `Extra` debe decir `auto_increment` | ✅ lo tiene |
| 25 | `migration_privacidad_preferencias_correo.sql` | Agrega `acepto_privacidad`/`fecha_acepto_privacidad` (usuario) y `recibir_correos` (clientes) | `SHOW COLUMNS FROM usuario_modificacion LIKE 'acepto_privacidad'; SHOW COLUMNS FROM clientes LIKE 'recibir_correos';` | ✅ existe |
| 26 | `migration_recibir_promociones.sql` | Agrega `recibir_promociones` a `clientes` | `SHOW COLUMNS FROM clientes LIKE 'recibir_promociones';` | ✅ existe |
| 27 | `migration_ramo_armado_variante_sombra.sql` | Agrega `variante_id` a `ramo_armado` | `SHOW COLUMNS FROM ramo_armado LIKE 'variante_id';` | ✅ existe |
| 28 | `migration_fix_stock_color_flor.sql` | Fix de datos (sincroniza `color_flor.stock` con su variante sombra) — NO agrega columnas | `SELECT COUNT(*) FROM color_flor cf JOIN variantes v ON v.id=cf.variante_id WHERE cf.stock<>v.stock;` → debe dar 0 después de correrlo | ✅ da 0 |

📝 *Nota: #10, #11, #12, #13, #14 (todas `tema_variable_*`) conviene correrlas EN ORDEN por
fecha si no se corrieron ya — algunas dependen de que la anterior ya haya corrido. Revisar la
fecha de cada archivo si hay dudas de orden.*

**⚠️ Único pendiente real de esta sección: #13 (`migration_tema_variable_pk_semanticos.sql`)**
no está corrido ni en QA ni en prod. Este es el contenido REAL del archivo (lo saqué directo de
`qa` con `git show origin/qa:migration_tema_variable_pk_semanticos.sql`, no es un placeholder):

```sql
INSERT INTO tema_variable (clave, etiqueta, grupo, tipo, valor_claro, valor_oscuro, orden) VALUES
    -- Éxito (verde) -- badges "activo/entregado/aprobado", botones agregar/activar, montos positivos
    ('pk-success',      'Éxito — color principal',        'Estados', 'color', '#16a34a', '#34d399', 1),
    ('pk-success-to',   'Éxito — variante oscura/degradado', 'Estados', 'color', '#059669', '#10b981', 2),
    ('pk-success-soft', 'Éxito — fondo suave (badges/cajas)', 'Estados', 'color', 'rgba(22,163,74,0.10)', 'rgba(52,211,153,0.14)', 3),
    -- Advertencia (ámbar) -- badges "pendiente/apartado", avisos "revisa esto"
    ('pk-warning',      'Advertencia — color principal',        'Estados', 'color', '#f59e0b', '#fbbf24', 4),
    ('pk-warning-to',   'Advertencia — variante oscura/degradado', 'Estados', 'color', '#d97706', '#f59e0b', 5),
    ('pk-warning-soft', 'Advertencia — fondo suave (badges/cajas)', 'Estados', 'color', 'rgba(245,158,11,0.10)', 'rgba(251,191,36,0.14)', 6),
    -- Peligro/error (rojo) -- botones eliminar/cancelar/rechazar, cajas de error, montos negativos
    ('pk-danger',       'Peligro/Error — color principal',        'Estados', 'color', '#ef4444', '#f87171', 7),
    ('pk-danger-to',    'Peligro/Error — variante oscura/degradado', 'Estados', 'color', '#dc2626', '#ef4444', 8),
    ('pk-danger-soft',  'Peligro/Error — fondo suave (badges/cajas)', 'Estados', 'color', 'rgba(239,68,68,0.10)', 'rgba(248,113,113,0.14)', 9),
    -- Informativo (azul) -- badges "en curso/procesando", botones editar/compartir/transferir
    ('pk-info',         'Informativo — color principal',        'Estados', 'color', '#3b82f6', '#60a5fa', 10),
    ('pk-info-to',      'Informativo — variante oscura/degradado', 'Estados', 'color', '#2563eb', '#3b82f6', 11),
    ('pk-info-soft',    'Informativo — fondo suave (badges/cajas)', 'Estados', 'color', 'rgba(59,130,246,0.10)', 'rgba(96,165,250,0.14)', 12)
ON DUPLICATE KEY UPDATE clave = clave;
```

**Nota sobre la verificación de la fila #13 de la tabla**: la consulta original de este
documento buscaba `clave LIKE '--pk-%'` pero la clave real en la BD es `pk-success` (SIN el
prefijo `--` — ese prefijo es solo la sintaxis de variable CSS, no forma parte del valor
guardado). La consulta correcta es:
```sql
SELECT COUNT(*) FROM tema_variable WHERE grupo = 'Estados';
-- → debe dar 12
```

### 1.b — 🚫 Bloqueados (redes sociales) — NO correr en prod en esta tanda

Estas migraciones son parte de la feature bloqueada. Quedan documentadas acá para no perderlas
de vista, pero no se corren en prod hasta que la feature se resuelva (credenciales + App Review
de Meta):

- `migration_comentario_social.sql` / `migration_comentario_pausa.sql`
- `migration_mensaje_directo.sql`
- `migration_publicacion_social.sql` / `migration_publicacion_social_programada.sql` / `migration_publicacion_social_variante_opcional.sql`
- `migration_negocio_instagram_tiktok.sql`
- `migration_tiktok_token.sql`

**Qué son y por qué están acá** (dejaste la duda arriba — respondiendo): son las tablas/columnas
del módulo de **redes sociales** — comentarios de Facebook con sus pausas, mensajes directos de
Instagram, publicaciones programadas (con el archivo pendiente guardado en BD hasta que se
publica), la config de URLs de Instagram/TikTok en el negocio, y el token de TikTok. Ya están en
`qa` (alguien las corrió ahí en su momento) pero **no van a `main`/prod en esta tanda** porque
`CLAUDE.md` documenta que esa feature quedó bloqueada el 2026-08-21 por credenciales de prod sin
definir y el App Review de Meta sin aprobar — por eso las excluí explícitamente de la lista de
1.a, para que no se corran por accidente en prod junto con el resto. No hace falta que hagas
nada con estas — quedan documentadas solo para que no se pierdan de vista el día que se
desbloqueen.

### 1.c — ✅ Ya ejecutadas en QA y prod, adelantadas al código (rama `feature/permisos-finos`)

Estas NO vienen de `qa` — son de una rama propia (`feature/permisos-finos`, ver
`ROADMAP_PRUEBAS_PERMISOS_TIENDA_Y_FILTRO_SEGURIDAD.md`) que todavía no se fusionó a `dev`. El
usuario las fue corriendo en QA y prod a medida que se armaban, antes de que el código llegue.
Cuando esa rama se fusione a `dev`→`qa`→`main`, estos scripts YA están aplicados — no hay que
volver a correrlos, solo confirmar con la verificación de cada uno si hiciera falta.

| # | Script | Qué hace | Verificación |
|---|---|---|---|
| 1 | `migration_accion_tienda_habilitar_compartir.sql` | Siembra las acciones `habilitar`/`compartir-imagen` para `tienda/buscar` | `SELECT clave FROM accion_submenu a JOIN submenu s ON s.id=a.submenu_id WHERE s.ruta='tienda/buscar' AND a.clave IN ('habilitar','compartir-imagen');` → 2 filas | hay 2 filas
| 2 | `migration_descripcion_acciones_modelos.sql` | Llena `descripcion` de las 5 acciones originales de Modelos | ya cubierta en 1.a #8 |
| 3 | `migration_accion_modelos_etiquetas_y_escaner.sql` | Renombra etiquetas de Modelos con ícono real + agrega la acción `escanear-codigo` | `SELECT etiqueta FROM accion_submenu a JOIN submenu s ON s.id=a.submenu_id WHERE s.ruta='productos/buscar' AND a.clave='escanear-codigo';` → 1 fila |existe
| 4 | `migration_accion_submenu_categoria.sql` | Agrega `accion_submenu.categoria` + renumera `orden` (agrupa Filtros/Tarjeta/Buscador en Modelos y Tienda) | `SELECT DISTINCT categoria FROM accion_submenu WHERE categoria IS NOT NULL;` → varias filas, no vacío | existen
| 5 | `migration_submenu_descripcion_escritura.sql` | Agrega `submenu.descripcion_escritura` + la llena para los grupos compartidos (Modelos/Agregar modelo/Agregar producto, Rifas, Facebook/Hashtags) | `SELECT ruta FROM submenu WHERE descripcion_escritura IS NOT NULL;` → varias filas | existen

---

## 2. VPS / Kubernetes — qué revisar en el ambiente

El deploy real corre TODO en la misma VPS (OVHcloud, ver `VPS_AUDITORIA.md`) con un k8s de un
solo nodo: `main` despliega al namespace `default`, `qa` al namespace `qa` (ver
`.github/workflows/producto-actions.yml` / `-qa.yml` — ambos hacen
`kubectl rollout restart deployment proyecto-key-deployment -n <namespace>`).

Esto se revisa CONECTADO a la VPS/kubectl, no desde acá — marcá cada ítem vos mismo (o pedime
que te arme el comando exacto si hace falta). El comando del ítem 1 anterior te tiró error
(`unknown shorthand flag: 'i' in -i`) porque tenía un `\` de más antes del pipe — abajo van
limpios, en un bloque de código aparte de la tabla para que no pase de nuevo.

| # | Qué revisar | Estado |
|---|---|---|
| 1 | RabbitMQ configurado en `default` | ✅ **`rabbitmq-0` Running** — OJO: esto contradice la nota vieja de `CLAUDE.md` ("main no tiene RabbitMQ configurado") — esa afirmación quedó desactualizada, hay que corregirla ahí también cuando se pueda |
| 2 | Redis en `default` | ✅ `redis-...-fmmqj` Running |
| 3 | `TOKEN_JWT` en `default` | ✅ presente (`token-jwt: bW...`, no hace falta ver más) |
| 4 | ConfigMap/env de `default` vs `qa` | ✅ diff corrido — solo difieren metadatos de k8s (`creationTimestamp`/`namespace`/`resourceVersion`/`uid`), el contenido real es igual. Ojo: esto probablemente significa que los ConfigMaps no son donde vive la config sensible de esta app (esa se inyecta con `kubectl set env` directo al Deployment, según `k8s/DEPLOY_COMMANDS.md`) — no es una comparación tan reveladora como pensé |
| 5 | `application-docker.yml` del pod | ⬜ **no se llegó a correr** — lo que se pegó fue `kubectl get pods -n qa` (otro comando, no el de este ítem). Falta correr el de abajo cuando puedas |
| 6 | Espacio en disco / memoria libres | ⚠️ **memoria ajustada**: 7.6Gi total, solo 179Mi libres "real" (aunque 1.8Gi "available" contando cache reciclable — eso es lo que más importa en Linux). Disco: 83% usado (13G libres de 72G). No es crítico hoy, pero antes de un rollout con varios pods nuevos a la vez conviene tenerlo presente — si se pone justo, un rollout puede hacer que k8s mate pods por OOM |
| 7 | Docker Hub | ⬜ falta revisar |

Comandos (copiar tal cual, sin backslash antes del `|`):

```bash
# 1. RabbitMQ en default
kubectl get pods -n default | grep -i rabbit
ubuntu@vps-da9a48f5:~$ kubectl get pods -n default | grep -i rabbit
rabbitmq-0                                       1/1     Running   3 (57d ago)   86d
ubuntu@vps-da9a48f5:~$ 

# 2. Redis en default
kubectl get pods -n default | grep -i redis
ubuntu@vps-da9a48f5:~$ kubectl get pods -n default | grep -i redis
redis-5ffb94bb5b-fmmqj                           1/1     Running   4 (57d ago)   137d
ubuntu@vps-da9a48f5:~$ 

# 3. TOKEN_JWT presente (sin imprimir el valor)
kubectl get secret -n default -o yaml | grep -i token
ubuntu@vps-da9a48f5:~$ kubectl get secret -n default -o yaml | grep -i token
    token-jwt: bW
    
# 4. Diff de configmaps default vs qa
kubectl get configmap -n default -o yaml > /tmp/cm-default.yaml

ubuntu@vps-da9a48f5:~$ kubectl get configmap -n default -o yaml > /tmp/cm-default.yaml
ubuntu@vps-da9a48f5:~$ 


kubectl get configmap -n qa -o yaml > /tmp/cm-qa.yaml
ubuntu@vps-da9a48f5:~$ kubectl get configmap -n qa -o yaml > /tmp/cm-qa.yaml
ubuntu@vps-da9a48f5:~$ 

diff /tmp/cm-default.yaml /tmp/cm-qa.yaml
ubuntu@vps-da9a48f5:~$ diff /tmp/cm-default.yaml /tmp/cm-qa.yaml
23c23
<     creationTimestamp: "2026-04-17T20:34:15Z"
---
>     creationTimestamp: "2026-04-25T20:08:53Z"
25,27c25,27
<     namespace: default
<     resourceVersion: "351"
<     uid: bff8fa17-fc0e-40e6-859a-398710394c2a
---
>     namespace: qa
>     resourceVersion: "292528"
>     uid: a4e3535c-4f1a-4633-a491-6dccb57ec4a3
ubuntu@vps-da9a48f5:~$ 

# 5. application-docker.yml del pod (ver sección 5 sobre el nombre del perfil)
kubectl exec -n default <pod> -- cat /app/BOOT-INF/classes/application-docker.yml

ubuntu@vps-da9a48f5:~$ kubectl get pods -n qa
NAME                                             READY   STATUS    RESTARTS      AGE
imagenes-deployment-77f76db54c-clzz7             1/1     Running   0             45h
proyecto-key-deployment-79bb5487c9-f7ckw         1/1     Running   0             13h
proyecto-key-front-deployment-7c8777dc68-gk57p   1/1     Running   0             13h
rabbitmq-0                                       1/1     Running   3 (57d ago)   107d
redis-5ffb94bb5b-m89m5                           1/1     Running   4 (57d ago)   131d
ubuntu@vps-da9a48f5:~$ 

# 6. Disco y memoria libres
free -h && df -h /

ubuntu@vps-da9a48f5:~$ free -h && df -h /
               total        used        free      shared  buff/cache   available
Mem:           7.6Gi       5.8Gi       179Mi        72Mi       2.0Gi       1.8Gi
Swap:             0B          0B          0B
Filesystem      Size  Used Avail Use% Mounted on
/dev/sda1        72G   60G   13G  83% /
ubuntu@vps-da9a48f5:~$ 

# 7. Docker Hub -- revisar la última corrida de producto-actions.yml en GitHub Actions
```

---

## 3. Bitácora — notas y confirmaciones

*(vas agregando fecha + qué revisaste + resultado; yo actualizo los estados de arriba a partir
de esto)*

- 2026-09-04 — Documento creado, sin revisar todavía.
- 2026-09-04 — Revisada la sección 1.a completa (28 scripts): **todos ✅** en prod, incluido el
  #13 que parecía faltar (era mi consulta de verificación la que estaba mal escrita). Sección
  1.c también confirmada. Sección 2 (VPS): ítems 1-4 y 6 revisados (RabbitMQ y Redis SÍ están
  configurados en `default` — corrige la nota vieja de `CLAUDE.md`; memoria de la VPS ajustada,
  ojo con eso). Faltan: ítem 5 (cat del yml del pod, se corrió otro comando por error) y 7
  (Docker Hub). Sección 1.a y 1.c ya están listas para el día del merge real.

---

## 4. Cuando ya esté todo en ✅ — próximos pasos (no arrancar antes)

1. Armar la lista puntual de commits de `qa` a incluir/excluir (redes sociales afuera) — recién
   ahí, con la fecha real, para que no quede desactualizada.
2. Congelar `qa` (avisar que no se suba nada más mientras se arma el cherry-pick).
3. Cherry-pick de los commits no-redes-sociales a una rama de trabajo, revisar que compile y
   pase los tests.
4. Correr los scripts de la sección 1.a en prod que sigan en ⬜ (los que ya estén ✅ no se
   repiten).
5. Merge/PR de esa rama de trabajo a `main`, avisar al front si hay contrato nuevo
   (`CAMBIOS_FRONT.md`).
6. Post-despliegue: revisar logs del pod de `default` los primeros minutos, confirmar que no
   hay 500 por columna/tabla faltante.

---

## 5. Renombrar el perfil `docker` → `prod` (tu pregunta 2026-09-04)

**Decisión 2026-09-04: NO renombrar todavía.** El perfil sigue llamándose `docker` — `application-docker.yml`
sigue siendo el que corre en prod, sin tocar. Lo único que SÍ se hizo ya es el paso 1 de la
lista de abajo (limpiar el bloque `prod` viejo y sin usar de `application.yml`, que no
dependía de nada de esto). El resto queda como explicación de qué implicaría el día que
decidas hacer el cambio real — no arrancar el resto todavía.

### Qué hay hoy exactamente
En `src/main/resources/` tenés 4 archivos de perfil: `application-dev.yml`,
`application-qa.yml`, `application-local-qa.yml` y **`application-docker.yml`** — este último es
el que hoy corre en producción de verdad (namespace `default`), confirmado en
`k8s/DEPLOY_COMMANDS.md`: *"Spring YML activo (PROD): `application-docker.yml`"*. El nombre
"docker" acá es el PERFIL DE SPRING (`spring.profiles.active`), sin relación con el
`docker-compose.yml` que usás para tu entorno local — son 2 cosas distintas que comparten
palabra por casualidad.

### ⚠️ El problema real de renombrar sin más
`application.yml` (el archivo BASE, el que aplica siempre) **ya tiene un segundo bloque con
`on-profile: prod`** — un intento de perfil "prod" viejo y a medias, que le faltan: Redis,
RabbitMQ, mail, MercadoPago, OpenAI, WhatsApp, Facebook/Instagram/TikTok, Swagger, y usa
NOMBRES DE VARIABLE DE ENTORNO DISTINTOS para la base de datos:

| | Bloque `prod` viejo (en `application.yml`) | `application-docker.yml` (el real) |
|---|---|---|
| URL de la BD | `${DB_URL}` | `jdbc:mysql://${DB_HOST}:3306/${SPRING_DB_NAME}` |
| Usuario BD | `${DB_USER}` | `${SPRING_DATASOURCE_USERNAME}` |
| Password BD | `${DB_PASS}` | `${SPRING_DATASOURCE_PASSWORD}` |

Si solo cambiás `on-profile: docker` → `on-profile: prod` en `application-docker.yml` (o
renombrás el archivo), Spring va a activar LOS DOS bloques a la vez (ambos dicen
`on-profile: prod`) y los va a mezclar — con la mala suerte de que pisen entre sí quedaría el
pod sin poder conectar a la base de datos (o conectando con las variables equivocadas) al
arrancar. Este es el paso que hay que resolver PRIMERO, antes de cualquier otra cosa.

### Pasos, en orden
1. ✅ **Hecho (2026-09-04)** — Se borró el bloque `on-profile: prod` viejo dentro de
   `application.yml` (era un intento abandonado de antes de que "docker" se volviera la
   convención real; no lo usaba nada, así que no afecta a `dev`/`qa`/`docker`). El resto de
   `application.yml` queda igual. `application-docker.yml` NO se tocó.
2. **Confirmar el perfil realmente activo hoy en el pod de prod**, antes de asumir nada:
   ```bash
   kubectl get deployment proyecto-key-deployment -n default -o jsonpath='{.spec.template.spec.containers[0].env}' | grep -i profile
   ```
3. **Renombrar el archivo** `application-docker.yml` → `application-prod.yml`, y adentro cambiar
   `on-profile: docker` → `on-profile: prod`. El resto del contenido (todas las propiedades) NO
   cambia — solo el nombre del archivo y esa línea.
4. **Actualizar la variable de entorno del Deployment** en la VPS (el nombre exacto lo confirma
   el paso 2 — probablemente `SPRING_PROFILES_ACTIVE`):
   ```bash
   kubectl set env deployment/proyecto-key-deployment SPRING_PROFILES_ACTIVE=prod -n default
   kubectl rollout restart deployment/proyecto-key-deployment -n default
   ```
5. **Revisar logs inmediatamente después del restart** — que arranque sin error de datasource
   ni de ninguna propiedad faltante:
   ```bash
   kubectl logs -f deployment/proyecto-key-deployment -n default
   ```
6. **Actualizar la documentación que menciona "docker" como el perfil de prod** para que no
   quede desactualizada: `k8s/DEPLOY_COMMANDS.md` (la tabla de la sección 8 y los comandos del
   PASO 4/6 de "Despliegue PROD" mencionan `application-docker.yml`).
7. **`micro_imagenes`** (el otro servicio que corre en el mismo namespace, mencionado en
   `k8s/DEPLOY_COMMANDS.md`) — si ese microservicio TAMBIÉN usa un perfil "docker" propio,
   revisar si aplica el mismo cambio ahí o si es independiente (repo aparte, no lo tengo
   disponible en esta sesión para confirmarlo).

📝 *No hace falta tocar nada de `qa`/`dev` — su perfil se llama distinto (`qa`, `dev`) y no
tiene este conflicto.*
