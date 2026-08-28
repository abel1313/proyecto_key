# Logos — despliegue y troubleshooting

Feature: catálogo de logos (`Logo`/`LogoService`/`LogoController`) + logo real en el
encabezado de los correos (`EmailService`). Agregada 2026-08-28. Este doc es la checklist para
cuando falle algo (correo sigue con el ícono 🛍️, pantalla de Logos vacía, etc.) — para no tener
que re-investigar desde cero.

---

## 1. Piezas que tienen que estar TODAS puestas

Si falta cualquiera de estas 3, el correo sigue mostrando el ícono genérico en vez del logo
(no rompe nada, pero tampoco muestra el logo):

| # | Qué | Dónde | Cómo se verifica |
|---|---|---|---|
| 1 | Tabla `logo` creada | BD de cada ambiente | `SHOW TABLES LIKE 'logo';` |
| 2 | Variable `APP_PUBLIC_BASE_URL` seteada | Deployment de K8s del backend | `kubectl set env deployment/proyecto-key-deployment --list -n <ns> \| grep APP_PUBLIC_BASE_URL` |
| 3 | Al menos un logo subido y marcado activo | Fila en tabla `logo` con `activo=1` | `SELECT id, nombre_original, activo FROM logo;` |

El archivo físico (PNG) vive en disco en `guardar-imagenes.ruta_imagenes` (`/app/imagenes` en
QA/docker) — **pero eso solo no alcanza**. Sin la fila en `logo` apuntando a ese archivo, la app
no sabe que existe. La única forma correcta de que archivo+fila queden sincronizados es subir el
logo por la pantalla **Personalización → Logos** (botón "➕ Subir logo") o por `POST /logos` — no
copiar el archivo a mano al servidor.

---

## 2. Migración de BD (paso 1)

Archivo: `src/main/resources/static/migration_logo.sql`. Ejecutar a mano (proyecto usa
`ddl-auto: none`, no se crea sola).

```sql
CREATE TABLE logo (
    id               INT NOT NULL AUTO_INCREMENT,
    nombre_archivo   VARCHAR(300) NOT NULL,
    extension        VARCHAR(10)  NULL,
    nombre_original  VARCHAR(200) NULL,
    activo           TINYINT(1)   NOT NULL DEFAULT 0,
    creado_en        DATETIME     NULL,
    PRIMARY KEY (id)
);

CREATE INDEX idx_logo_activo ON logo (activo);
```

| Ambiente | Base de datos | Estado |
|---|---|---|
| dev / qa | `inventario_key_qa` (comparten la misma BD) | ✅ Ejecutado 2026-08-28 |
| main / prod | `inventario_key` (sin sufijo) | ⏳ Pendiente — recién cuando se promueva la feature a `main` |

---

## 3. Variable de entorno `APP_PUBLIC_BASE_URL` (paso 2)

URL pública real del backend (dominio + `/mis-productos`) — sin esto `EmailService` no arma el
`<img>` del logo aunque haya uno activo (necesita una URL que un cliente de correo externo pueda
resolver, no una ruta relativa). Se inyecta con `kubectl set env`, mismo mecanismo que ya usan
para las credenciales de Rabbit (ver `k8s/DEPLOY_COMMANDS.md`).

### QA (namespace `qa`)

```bash
kubectl set env deployment/proyecto-key-deployment \
  APP_PUBLIC_BASE_URL=https://qa.backend.novedades-jade.com.mx/mis-productos \
  -n qa

kubectl rollout restart deployment/proyecto-key-deployment -n qa
```

**Estado: ✅ Aplicado 2026-08-28.** Confirmado con:
```bash
kubectl set env deployment/proyecto-key-deployment --list -n qa | grep APP_PUBLIC_BASE_URL
# APP_PUBLIC_BASE_URL=https://qa.backend.novedades-jade.com.mx/mis-productos
```

### PROD (namespace `default`)

```bash
kubectl set env deployment/proyecto-key-deployment \
  APP_PUBLIC_BASE_URL=https://backend.novedades-jade.com.mx/mis-productos \
  -n default

kubectl rollout restart deployment/proyecto-key-deployment -n default
```

⚠️ **Dominio de prod sin confirmar** — `https://backend.novedades-jade.com.mx` es una suposición
por analogía con el de QA (`qa.backend...` → sin el `qa.`). Verificar el dominio real antes de
correr esto. **Estado: ⏳ Pendiente** — la feature todavía no está en `main` (ver sección 5).

---

## 4. Subir y activar el logo (paso 3)

1. Entrar como ADMIN → **Personalización → Logos** (`/personalizacion/logos`).
2. "➕ Subir logo" con el archivo real (los dos que ya están en
   `producto_venta_online/src/assets/imagenes/`: `logo_fondo.png` / `logo_sin_fondo.png`).
3. Click en **"Usar en correos"** sobre el que se quiera activo (selección única, desactiva
   cualquier otro automáticamente).
4. Disparar un correo real de prueba (ej. "olvidé mi contraseña") y confirmar que el encabezado
   muestra el logo en vez de 🛍️.

---

## 5. Promoción a `main`/prod — falta

Por la regla de `CLAUDE.md` (dev → qa → main, promover solo cuando QA ya validó), esta feature
sigue en `dev`/`qa` nada más. Cuando se decida promoverla:

1. `git checkout main && git pull && git merge qa --no-ff -m "Merge qa → main: ..." && git push origin main`
   (mismo criterio del repo: si `qa` trae algo que main no debe recibir todavía, no es merge
   directo, ver excepción de "feature bloqueada" en `CLAUDE.md`).
2. Correr `migration_logo.sql` contra `inventario_key` (BD de prod).
3. Setear `APP_PUBLIC_BASE_URL` en el deployment de `-n default` (comando de la sección 3).
4. Repetir el paso 4 (subir/activar logo) — la tabla `logo` es independiente por ambiente, un
   logo subido en QA NO existe en prod.

---

## 6. Síntomas comunes y qué revisar

| Síntoma | Causa probable |
|---|---|
| Correo sigue con el ícono 🛍️ | Falta alguna de las 3 piezas de la sección 1 — revisar en orden: tabla → env var → logo activo |
| Pantalla Personalización → Logos vacía aunque "ya se subió" | El archivo se copió a mano al servidor sin pasar por el botón/endpoint — no hay fila en `logo`. Subir de nuevo por la pantalla. |
| `GET /logos` da 403 | Falta el permiso de pantalla `personalizacion` para ese rol (Gestión de roles) |
| El logo no carga en el correo pero sí en la pantalla de Logos | `APP_PUBLIC_BASE_URL` mal seteada, apunta a un dominio no accesible desde afuera, o al pod no se le reinició después de `kubectl set env` |
| Cambié `APP_PUBLIC_BASE_URL` y no pasó nada | Falta el `kubectl rollout restart` — `set env` solo actualiza el manifest, el pod viejo sigue corriendo con el env anterior hasta que se reinicia |
