# Credenciales expuestas — qué cambiar y cómo (2026-10-15)

Origen: Gitleaks sobre el historial de `proyecto_key` (1006 commits), `producto_venta_online` (905) y
`micro_imagenes` (1). Valores ocultos con `--redact`. **Este documento no lleva ninguna contraseña ni
llave, y no debe llevarlas nunca.**

Los repositorios `proyecto_key` y `producto_venta_online` son **públicos** (`private: false`); `micro_imagenes`
es privado. Todo lo de abajo que está en esos dos repos lo puede leer cualquiera.

---

## 0. Resumen: 3 cosas reales, el resto es falso positivo

| # | Qué | Dónde está | Gravedad | Por qué importa |
|---|---|---|---|---|
| A | **Token de DipoMex** (la llave de Copomex también, pero esa ya no se usa) escritos en el código | `src/main/java/com/ventas/key/mis/productos/service/DopoMexService.java` líneas **24** (`API_KEY`, se manda en el header `APIKEY`) y **25** (`API_KEY_COPO_MEX`, se manda como `?token=`). Siguen en `main`, `qa` y `dev`. Historial: commits `76f8963` (2025-10-26), `c04685d` (2025-10-27), `27e480a` (2026-07-21) | **Alta** | Cualquiera las copia y gasta tu cuota; si se agota, Mis datos y el código postal dejan de llenar el municipio |
| B | **Usuario y contraseña de RabbitMQ**, prod y QA | `k8s/prod/secret.yaml` línea 9, `k8s/qa/secret.yaml` línea 9 (base64, que no cifra nada) y **en texto plano** en `k8s/DEPLOY_COMMANDS.md` (línea 375 y el cuadro de «Password de Rabbit» y los `kubectl set env` de las secciones QA y PROD). Commits `3d00cd1` (2026-05-23) y `27e480a` (2026-07-21) | **Media** | RabbitMQ es `ClusterIP`: no se alcanza desde internet, solo desde dentro del cluster. Pero la contraseña es pública y sigue el patrón `Rabbit…#2026`, fácil de adivinar |
| C | **Access key de AWS «antigua» que `PENDIENTES.md` dice que sigue activa** | `PENDIENTES.md` línea **36** (commits `f76059c`, `67ae3f5`, `27e480a`) y 7 menciones en `micro_imagenes` (`BUGS_REPORT.md:132`, `seguridad/SECURITY_REVIEW_2026-05-26.md` líneas 127, 135, 140, 145, 201, 227) | **Media/Alta si sigue activa** | Un identificador `AKIA…` solo no entra a nada, pero si la llave secreta de ese par se filtró alguna vez, abre tu cuenta de AWS |

**Falsos positivos (no hacer nada):**

| Archivo:línea | Qué es realmente |
|---|---|
| `UsuarioServiceImpl.java:95` y `:141` | Constante `CHARSET_PASSWORD_ALEATORIA`: la lista de letras para generar contraseñas aleatorias |
| `QA_ROADMAP_2026-09-02.md:81, 84, 292, 316` y `posibles_errores.md:14` | Tokens JWT pegados en documentos. Duran 15 min: ya caducaron (se verificó la fecha `exp`) |
| `CAMBIOS_FRONT.md:2458-2459` (back) y `CAMBIOS_FRONT.md:2441`, `PLAN_MEJORAS.md:2458` (front) | `cliente_id`: identificador público, no es secreto |
| `micro_imagenes/.../SecurityConfigTest.java:49` | Dato de prueba |

---

## 1. Orden recomendado

1. **A — DipoMex / Copomex** (la más urgente: sigue en el código actual).
2. **B — RabbitMQ en QA**, comprobar, y luego **B — RabbitMQ en producción**.
3. **C — AWS** (5 minutos en la consola web, puede ir en paralelo con cualquier otro paso).
4. **D — Limpiar los repositorios** (quitar los archivos y activar la protección de GitHub).
5. **E — Comprobar** que ya no queda nada.

Regla de oro: **primero QA, luego producción**, y producción fuera de horario de ventas.

---

## 2. Paso A — Llave de DipoMex (y la de Copomex, que sobra)

**Aclaración (2026-10-15):** en el código hay dos llaves, pero solo una se usa.
| Constante | Servicio | Cómo se manda | ¿Se usa? |
|---|---|---|---|
| `API_KEY` (línea 24) | **DipoMex** (`api.tau.com.mx/dipomex/v1/`) | header `APIKEY` | **Sí**: Mis datos y ver cliente llenan el municipio con ella (`getCodigoPostal`) |
| `API_KEY_COPO_MEX` (línea 25) | Copomex (`api.copomex.com`) | `?token=` | **No**: el método `getCodigoPostal2` que la usa no lo llama nadie |

En tu panel de DipoMex solo aparece **un token**: ese es el de `API_KEY`. Es lo normal, no falta nada.

### A.1 Qué cambia en el código (lo hago yo, en `dev` → `qa` → `main`, cuando me digas «sube»)
- `API_KEY` deja de estar escrita y se lee de la variable de entorno **`DIPOMEX_API_KEY`**.
- `API_KEY_COPO_MEX` y el método `getCodigoPostal2` **se borran** (código muerto). Así no hay variable nueva que poner para Copomex.
- Sin la variable, el servicio no revienta: el código postal responde el error claro de siempre y Mis datos deja escribir el municipio a mano.

### A.2 Orden para no cortar el código postal
1. Yo subo el cambio de código.
2. **Tú** pones la variable con el token **actual** (todavía no lo cambies), en QA y luego en producción:
   ```bash
   # QA
   kubectl set env deployment/proyecto-key-deployment -n qa DIPOMEX_API_KEY='<el token actual de DipoMex>'

   # Producción
   kubectl set env deployment/proyecto-key-deployment -n default DIPOMEX_API_KEY='<el token actual de DipoMex>'
   ```
   `kubectl set env` reinicia el deployment solo. Si pide permiso, antepón `sudo` (el workflow del repo usa `sudo kubectl`).
3. Comprobar: tienda → Mis datos → escribir un código postal → debe llenar municipio y colonias.
4. En tu cuenta de DipoMex: **regenerar el token** (la anterior deja de servir).
5. Repetir el paso 2 con el token **nuevo** (QA primero, producción después) y repetir la comprobación del paso 3.

### A.3 Copomex
- Si **no tienes cuenta** en Copomex, no hay nada que hacer: el token ya no se usa en el código y, al no tener cuenta tuya, solo queda como texto viejo en el historial.
- Si **sí tienes cuenta**, entra y regenera o borra ese token (para que el viejo, que sigue público en el historial, no sirva). No hace falta ponerlo en ningún lado.

### A.4 Qué NO hacer
- No pegues el token en el chat ni en ningún `.md`.
- No regeneres antes de que el código lea de la variable: cortarías el código postal.

---

## 3. Paso B — Contraseña de RabbitMQ

### B.0 Cosas que hay que saber antes (de los manifiestos del repo)
| Dato | QA | Producción |
|---|---|---|
| Namespace | `qa` | `default` |
| StatefulSet / pod de RabbitMQ | `rabbitmq` / `rabbitmq-0` | `rabbitmq` / `rabbitmq-0` |
| Secret | `rabbitmq-secret` (llaves `username` y `password`) | `rabbitmq-secret` |
| Variables con que el **back** se conecta | `SPRING_RABBITMQ_USERNAME` y `SPRING_RABBITMQ_PASSWORD` (las lee `application-qa.yml`) | `RABBITMQ_USERNAME` y `RABBITMQ_PASSWORD` (las lee `application-docker.yml`) |
| Deployment del back | `proyecto-key-deployment` | `proyecto-key-deployment` |
| Deployment del **micro de imágenes** | también conecta a Rabbit con las mismas credenciales: nombre con `kubectl get deployments -n qa` | ídem con `-n default` |

⚠️ **Trampa 1:** `RABBITMQ_DEFAULT_PASS` solo se usa la **primera vez** que RabbitMQ crea su disco. Cambiar el Secret **no**
cambia la contraseña real. Hay que cambiarla **dentro** de RabbitMQ con `rabbitmqctl change_password`.

⚠️ **Trampa 2:** el micro de imágenes también usa esa contraseña. Si solo cambias el back, el micro se queda sin
conexión a Rabbit. Hay que actualizar **los dos deployments**.

⚠️ **Trampa 3 (pasó en producción el 2026-10-15):** `imagenes-deployment` en producción tiene **los dos juegos de variables**: `RABBITMQ_USERNAME/PASSWORD` **y** `SPRING_RABBITMQ_USERNAME/PASSWORD`. Spring toma las `SPRING_…`. Cambiar solo `RABBITMQ_PASSWORD` dejó al micro con la contraseña vieja (`ACCESS_REFUSED`, reinicios en bucle). Al cambiar la contraseña hay que poner **ambas** en el micro. Revisar siempre los nombres con el comando de B.1 antes de cambiar.

### B.1 Antes de tocar nada (en la VPS, por SSH)
```bash
# ¿Qué variables tiene el back hoy? (solo nombres, sin valores)
kubectl get deployment proyecto-key-deployment -n qa -o jsonpath='{.spec.template.spec.containers[0].env[*].name}'; echo

# ¿Cuál es el deployment del micro de imágenes?
kubectl get deployments -n qa
```
Confirma que ves `SPRING_RABBITMQ_USERNAME` y `SPRING_RABBITMQ_PASSWORD`. Anota el nombre del deployment del micro
(en esta guía le llamo `MICRO`).

### B.2 Cambiar la contraseña en QA
```bash
# 1. Generar una contraseña nueva (queda solo en esta sesión; no se escribe en ningún archivo)
NUEVA=$(openssl rand -base64 24 | tr -d '/+=' | cut -c1-24)
echo "$NUEVA"        # cópiala AHORA a tu gestor de contraseñas

# 2. Leer el usuario actual del Secret
USUARIO=$(kubectl get secret rabbitmq-secret -n qa -o jsonpath='{.data.username}' | base64 -d)

# 3. Cambiarla DENTRO de RabbitMQ (los mensajes en cola no se pierden)
kubectl exec -n qa rabbitmq-0 -- rabbitmqctl change_password "$USUARIO" "$NUEVA"

# 4. Actualizar el Secret (para que un reinicio de Rabbit conserve la nueva)
kubectl create secret generic rabbitmq-secret -n qa \
  --from-literal=username="$USUARIO" --from-literal=password="$NUEVA" \
  --dry-run=client -o yaml | kubectl apply -f -

# 5. Decirle al back y al micro la nueva (cada set env reinicia su deployment)
kubectl set env deployment/proyecto-key-deployment -n qa SPRING_RABBITMQ_PASSWORD="$NUEVA"
kubectl set env deployment/MICRO -n qa SPRING_RABBITMQ_PASSWORD="$NUEVA"
```
Si al revisar el micro (B.1) ves que usa `RABBITMQ_PASSWORD` y no `SPRING_RABBITMQ_PASSWORD`, usa ese nombre en su línea.

Desde el paso 3 hasta el 5 (menos de un minuto) QA no puede hablar con Rabbit: hazlo sin prisa pero seguido.

### B.3 Comprobar QA
```bash
# El back conectó (no debe decir ACCESS_REFUSED)
kubectl logs deployment/proyecto-key-deployment -n qa --tail=100 | grep -i -E "rabbit|ACCESS_REFUSED"

# Rabbit ve conexiones del usuario
kubectl exec -n qa rabbitmq-0 -- rabbitmqctl list_connections user
```
Y en pantalla: entrar a QA, hacer algo que mande aviso (por ejemplo una subida de imagen en Agregar modelo o un pedido de prueba).

**Si algo sale mal:** puedes volver a la contraseña anterior (sigue en el repositorio) con el mismo `change_password`.
Por eso se hace primero en QA.

### B.4 Cambiar la contraseña en producción
Exactamente lo mismo, con estos cambios:
- `-n qa` → `-n default`
- `SPRING_RABBITMQ_PASSWORD` → **`RABBITMQ_PASSWORD`** (el back de producción lee `application-docker.yml`)
- Antes: `kubectl get deployments -n default` para el nombre del micro, y revisar sus nombres de variables con la misma instrucción de B.1.
- Hora: fuera de horario de ventas.
- Esta contraseña es **distinta** de la de QA: genera otra con `openssl`, no reutilices.

### B.5 Después
- Borrar la contraseña de la guía: `k8s/DEPLOY_COMMANDS.md` (yo lo hago en el paso D).
- Guardar las dos contraseñas nuevas solo en tu gestor de contraseñas.

---

## 4. Paso C — AWS (ya no se usa: limpieza)

Decisión del dueño (2026-10-15): AWS **ya no se usa**. Las imágenes viven en disco y el código del back no tiene ninguna
referencia a AWS (se verificó con búsqueda en `src/`, `pom.xml`, `Dockerfile` y workflows).

**En el repositorio (rama `chore/limpieza-credenciales-aws`, ya hecho):** se quitó la sección de `PENDIENTES.md`, el renglón de
`PENDIENTES_2026-10-08.md` y la dirección `amazonaws.com` comentada de `application-dev.yml`.

**En el cluster (lo haces tú):** los dos deployments tienen variables de AWS que ya nadie lee. Se quitan con un guion al final del nombre:
```bash
# Producción
kubectl set env deployment/proyecto-key-deployment -n default ACCESS_KEY_AWS- SECRET_KEY_AWS- REGION_AWS- NAME_BUCKET_AWS-
kubectl set env deployment/imagenes-deployment    -n default ACCESS_KEY_AWS- SECRET_KEY_AWS- REGION_AWS- NAME_BUCKET_AWS-
# QA (si las tienen; si dice que no existen, no pasa nada)
kubectl set env deployment/proyecto-key-deployment -n qa ACCESS_KEY_AWS- SECRET_KEY_AWS- REGION_AWS- NAME_BUCKET_AWS-
kubectl set env deployment/imagenes-deployment    -n qa ACCESS_KEY_AWS- SECRET_KEY_AWS- REGION_AWS- NAME_BUCKET_AWS-
```
Cada uno reinicia su deployment (≈1 minuto). Hazlo **después** de comprobar en tienda que las imágenes se ven.

**En AWS (lo haces tú, 5 minutos):** consola → IAM → Usuarios → el usuario → Credenciales de seguridad → Claves de acceso → **Desactivar**
la llave que empieza con `AKIA` (el identificador completo está en el historial de `PENDIENTES.md`). Déjala desactivada un día; si no se rompe nada, **Eliminar**.
Si ya no usas esa cuenta de AWS para nada, también puedes cerrar el usuario completo.

---

## 5. Paso D — Limpiar los repositorios (lo hago yo cuando me digas «sube»)

Después de haber cambiado las contraseñas y llaves:

1. `git rm --cached k8s/prod/secret.yaml k8s/qa/secret.yaml` y agregar `k8s/*/secret.yaml` al `.gitignore`.
   Los archivos se quedan en tu disco para que los uses con `kubectl apply`; solo dejan de subirse.
2. `k8s/DEPLOY_COMMANDS.md`: cambiar las contraseñas escritas (línea 375, el cuadro de «Password de Rabbit» y los
   `kubectl set env`) por `<tu-contraseña>` y poner el procedimiento de B.2 en lugar del viejo.
3. `PENDIENTES.md` línea 36 (y `micro_imagenes`: `BUGS_REPORT.md:132`, `SECURITY_REVIEW_2026-05-26.md`): quitar el identificador `AKIA…` completo.
4. Activar en GitHub (lo haces tú, 2 minutos por repositorio): repositorio → **Settings** → **Code security** (o *Advanced Security*) →
   **Secret scanning: Enable** y **Push protection: Enable**. Es gratis en repositorios públicos y bloquea un `git push` que lleve una llave.
5. **No se reescribe el historial** (`git filter-repo`/BFG): con 1000 commits, ramas `dev/qa/main`, deploys por rama y repos públicos
   que ya se pudieron clonar, es riesgoso y no sirve de nada una vez cambiadas las contraseñas — lo viejo queda muerto.

**Decisión tuya (no urgente):** ¿los repos del back y front deben seguir públicos? Pasarlos a privados es
**Settings → General → Danger Zone → Change visibility**. Ojo: en privados las GitHub Actions gastan minutos de tu plan
(2000 al mes gratis); el build de Docker y el deploy a QA/producción usan Actions. Hoy hay también documentos de negocio
(Meta, TikTok, legal) a la vista de cualquiera.

---

## 6. Paso E — Comprobar al final

En tu PowerShell, sobre el código (sin historial) deben quedar solo los falsos positivos de la tabla del punto 0:

```
docker run --rm -v D:\proyectos\proyecto_key_new:/repo zricethezav/gitleaks:latest detect --source /repo --no-git --redact -v
```
(`--no-git` revisa solo los archivos de hoy, no los commits viejos.) Debe salir: sin `DopoMexService.java`, sin `secret.yaml`, sin la línea 36 de
`PENDIENTES.md`. Quedan los JWT caducados de `QA_ROADMAP_2026-09-02.md` y `posibles_errores.md`; si quieres que no vuelvan a salir, dime y agrego un
`.gitleaks.toml` que los ignore.

---

## 7. Lista para ir palomeando

- [ ] A — Yo cambio `DopoMexService` a la variable `DIPOMEX_API_KEY` y borro lo de Copomex (tú dices «sube»)
- [ ] A — Variable `DIPOMEX_API_KEY` en QA con el token actual → código postal funciona
- [ ] A — Variables en producción → código postal funciona
- [ ] A — Token regenerado en DipoMex y puesto nuevo en QA y producción (y el de Copomex regenerado/borrado si tienes cuenta)
- [x] B — Contraseña de Rabbit cambiada en QA (back + micro) y comprobada (2026-10-15)
- [x] B — Contraseña de Rabbit cambiada en producción (back + micro) y comprobada (2026-10-15; el micro necesitó también SPRING_RABBITMQ_PASSWORD)
- [ ] C — Variables `*_AWS` quitadas de los deployments (prod y QA)
- [ ] C — Llave de AWS desactivada en IAM (y eliminada al día siguiente)
- [ ] D — `secret.yaml` fuera del repo, `DEPLOY_COMMANDS.md` y `PENDIENTES.md` limpios
- [ ] D — Secret scanning + Push protection activos en los dos repos públicos
- [ ] E — Gitleaks `--no-git` limpio
