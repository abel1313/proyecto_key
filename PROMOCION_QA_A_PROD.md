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
| 1 | `migration_menu_submenu.sql` | Crea tablas `menu` y `submenu` (catálogo de pantallas) | `SHOW TABLES LIKE 'submenu';` → debe existir | ⬜ |
| 2 | `migration_rol_submenu_usuario_submenu.sql` | Crea `rol_submenu` y `usuario_submenu` | `SHOW TABLES LIKE 'rol_submenu';` | ⬜ |
| 3 | `migration_permiso_escritura.sql` | Crea `rol_submenu_escritura` (permiso Editar separado de Ver) | `SHOW TABLES LIKE 'rol_submenu_escritura';` | ⬜ |
| 4 | `migration_accion_submenu.sql` | Crea `accion_submenu` y `rol_accion` + siembra las 6 acciones originales de Modelos | `SHOW TABLES LIKE 'accion_submenu';` | ⬜ |
| 5 | `migration_fix_submenu_gestion_menu.sql` | Agrega las filas de "Menús y submenús"/"Gestión de roles" al catálogo (si no, nadie puede entrar a asignar permisos) | `SELECT ruta FROM submenu WHERE ruta IN ('gestion-menu','gestion-menu/roles');` → debe traer 2 filas | ⬜ |
| 6 | `migration_filtros_granulares.sql` | Agrega columna `descripcion` a `accion_submenu` + separa "filtros-admin" en 9 checkboxes (Modelos y Tienda) | `SELECT COUNT(*) FROM accion_submenu WHERE clave LIKE 'filtro-%';` → 18 (9 x 2 pantallas) | ⬜ |
| 7 | `migration_descripcion_submenu.sql` | Agrega columna `descripcion` a `submenu` | `SHOW COLUMNS FROM submenu LIKE 'descripcion';` | ⬜ |
| 8 | `migration_descripcion_acciones_modelos.sql` | Llena `descripcion` de las 5 acciones originales de Modelos | `SELECT clave FROM accion_submenu WHERE submenu_id = (SELECT id FROM submenu WHERE ruta='productos/buscar') AND descripcion IS NULL;` → 0 filas | ⬜ |
| 9 | `migration_tema_variable.sql` | Crea tabla `tema_variable` (catálogo de Personalización) | `SHOW TABLES LIKE 'tema_variable';` | ⬜ |
| 10 | `migration_tema_variable_card_header_footer_v2.sql` | Agrega tokens `card-header-bg`/`card-footer-bg` | `SELECT clave FROM tema_variable WHERE clave IN ('card-header-bg','card-footer-bg');` → 2 filas | ⬜ |
| 11 | `migration_tema_variable_card_header_text.sql` | Agrega token `card-header-text` | `SELECT 1 FROM tema_variable WHERE clave='card-header-text';` | ⬜ |
| 12 | `migration_tema_variable_limpiar_card_header_footer.sql` | Borra 2 tokens viejos que no controlaban nada real (correr ANTES de la v2 de arriba si se aplican juntas, revisar orden) | `SELECT 1 FROM tema_variable WHERE clave IN ('card-header-bg','card-footer-bg');` → 0 filas (antes de correr la v2) | ⬜ |
| 13 | `migration_tema_variable_pk_semanticos.sql` | Agrega 12 tokens `--pk-success/-warning/-danger/-info` (+ variantes) | `SELECT COUNT(*) FROM tema_variable WHERE clave LIKE '--pk-%';` → 12 | ⬜ |
| 14 | `migration_tema_variable_placeholder.sql` | Agrega token `input-placeholder` | `SELECT 1 FROM tema_variable WHERE clave='input-placeholder';` | ⬜ |
| 15 | `migration_lugar_entrega_anillo.sql` | Crea tabla `lugar_entrega_anillo` (cobro por distancia) | `SHOW TABLES LIKE 'lugar_entrega_anillo';` | ⬜ |
| 16 | `migration_lugar_entrega_centroide.sql` | Agrega `latitud`/`longitud` a `lugares_entrega` | `SHOW COLUMNS FROM lugares_entrega LIKE 'latitud';` | ⬜ |
| 17 | `migration_lugar_entrega_recoger_en_tienda.sql` | Agrega `es_recoger_en_tienda` a `lugares_entrega` | `SHOW COLUMNS FROM lugares_entrega LIKE 'es_recoger_en_tienda';` | ⬜ |
| 18 | `migration_lugar_entrega_dia_entrega_semanal.sql` | Agrega `dia_entrega_semanal` a `lugares_entrega` | `SHOW COLUMNS FROM lugares_entrega LIKE 'dia_entrega_semanal';` | ⬜ |
| 19 | `migration_pedido_ubicacion_entrega.sql` | Agrega `latitud`/`longitud`/`referencias` a `pedidos` | `SHOW COLUMNS FROM pedidos LIKE 'referencias';` | ⬜ |
| 20 | `migration_fecha_creacion_producto_variante.sql` | Agrega `fecha_creacion` a `producto` y `variantes` | `SHOW COLUMNS FROM variantes LIKE 'fecha_creacion';` | ⬜ |
| 21 | `migration_umbral_stock_bajo.sql` | Agrega columna a `configuracion_negocio` (umbral de stock bajo) | `SHOW COLUMNS FROM configuracion_negocio LIKE 'umbral%';` | ⬜ |
| 22 | `migration_hashtags_default.sql` | Crea tabla `hashtags_default` | `SHOW TABLES LIKE 'hashtags_default';` | ⬜ |
| 23 | `migration_logo.sql` | Crea tabla `logo` | `SHOW TABLES LIKE 'logo';` | ⬜ |
| 24 | `migration_direcciones_autoincrement.sql` | `direcciones.id` pasa a AUTO_INCREMENT | `SHOW COLUMNS FROM direcciones LIKE 'id';` → columna `Extra` debe decir `auto_increment` | ⬜ |
| 25 | `migration_privacidad_preferencias_correo.sql` | Agrega `acepto_privacidad`/`fecha_acepto_privacidad` (usuario) y `recibir_correos` (clientes) | `SHOW COLUMNS FROM usuario_modificacion LIKE 'acepto_privacidad'; SHOW COLUMNS FROM clientes LIKE 'recibir_correos';` | ⬜ |
| 26 | `migration_recibir_promociones.sql` | Agrega `recibir_promociones` a `clientes` | `SHOW COLUMNS FROM clientes LIKE 'recibir_promociones';` | ⬜ |
| 27 | `migration_ramo_armado_variante_sombra.sql` | Agrega `variante_id` a `ramo_armado` | `SHOW COLUMNS FROM ramo_armado LIKE 'variante_id';` | ⬜ |
| 28 | `migration_fix_stock_color_flor.sql` | Fix de datos (sincroniza `color_flor.stock` con su variante sombra) — NO agrega columnas | `SELECT COUNT(*) FROM color_flor cf JOIN variantes v ON v.id=cf.variante_id WHERE cf.stock<>v.stock;` → debe dar 0 después de correrlo | ⬜ |

📝 *Nota: #10, #11, #12, #13, #14 (todas `tema_variable_*`) conviene correrlas EN ORDEN por
fecha si no se corrieron ya — algunas dependen de que la anterior ya haya corrido. Revisar la
fecha de cada archivo si hay dudas de orden.*

### 1.b — 🚫 Bloqueados (redes sociales) — NO correr en prod en esta tanda

Estas migraciones son parte de la feature bloqueada. Quedan documentadas acá para no perderlas
de vista, pero no se corren en prod hasta que la feature se resuelva (credenciales + App Review
de Meta):

- `migration_comentario_social.sql` / `migration_comentario_pausa.sql`
- `migration_mensaje_directo.sql`
- `migration_publicacion_social.sql` / `migration_publicacion_social_programada.sql` / `migration_publicacion_social_variante_opcional.sql`
- `migration_negocio_instagram_tiktok.sql`
- `migration_tiktok_token.sql`

---

## 2. VPS / Kubernetes — qué revisar en el ambiente

El deploy real corre TODO en la misma VPS (OVHcloud, ver `VPS_AUDITORIA.md`) con un k8s de un
solo nodo: `main` despliega al namespace `default`, `qa` al namespace `qa` (ver
`.github/workflows/producto-actions.yml` / `-qa.yml` — ambos hacen
`kubectl rollout restart deployment proyecto-key-deployment -n <namespace>`).

Esto se revisa CONECTADO a la VPS/kubectl, no desde acá — marcá cada ítem vos mismo (o pedime
que te arme el comando exacto si hace falta):

| # | Qué revisar | Comando sugerido | Estado |
|---|---|---|---|
| 1 | RabbitMQ configurado en `default` (recordatorio de `CLAUDE.md`: "main no tiene RabbitMQ configurado") — ¿sigue siendo así o ya se armó? | `kubectl get pods -n default \| grep -i rabbit` | ⬜ |
| 2 | Redis en `default` — activo y con la misma config que `qa` | `kubectl get pods -n default \| grep -i redis` | ⬜ |
| 3 | `TOKEN_JWT` en `default` — variable/secret presente | `kubectl get secret -n default -o yaml \| grep -i token` (sin imprimir el valor en ningún lado) | ⬜ |
| 4 | ConfigMap/env de `default` vs `qa` — diffear para ver qué le falta a prod de lo que ya tiene qa | `kubectl get configmap -n default -o yaml > /tmp/cm-default.yaml && kubectl get configmap -n qa -o yaml > /tmp/cm-qa.yaml && diff /tmp/cm-default.yaml /tmp/cm-qa.yaml` | ⬜ |
| 5 | `application.yml`/`application-prod.yml` vs `application-qa.yml` en el jar desplegado — confirmar que no falta ninguna propiedad nueva usada por el código que se va a promover | `kubectl exec -n default <pod> -- cat /app/BOOT-INF/classes/application.yml` (o el perfil que aplique) | ⬜ |
| 6 | Espacio en disco / memoria libres en la VPS antes de un rollout grande | `free -h && df -h /` (ver `VPS_AUDITORIA.md` bloque 1) | ⬜ |
| 7 | Docker Hub — confirmar que el workflow de `main` sigue subiendo bien la imagen (login/creds vigentes) | Revisar la última corrida en GitHub Actions de `producto-actions.yml` | ⬜ |

---

## 3. Bitácora — notas y confirmaciones

*(vas agregando fecha + qué revisaste + resultado; yo actualizo los estados de arriba a partir
de esto)*

- 2026-09-04 — Documento creado, sin revisar todavía.

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
