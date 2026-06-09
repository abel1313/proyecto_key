# RabbitMQ con Helm en VPS — Guía Completa

> **¿Para qué sirve este archivo?**
> Este es el manual maestro para instalar y gestionar RabbitMQ en el VPS usando Helm.
> Aquí también están los archivos `values.yaml` listos para copiar y usar.
> Léelo completo antes de ejecutar cualquier comando.

---

## Contexto de tu VPS

```
VPS con Kubernetes (K8s ya instalado)
├── Namespace: qa        → entorno QA
│   ├── frontend         (deployment del front Angular)
│   ├── micro-imagenes   (microservicio de imágenes)
│   └── proyecto-key     (este microservicio - mis-productos)
│
└── Namespace: default   → entorno PROD
    ├── frontend
    ├── micro-imagenes
    └── proyecto-key
```

---

## ¿Qué es Helm?

Helm es el **gestor de paquetes para Kubernetes**. Piénsalo igual que `npm` para Node.js
o `Maven` para Java, pero en lugar de descargar librerías de código, descarga e instala
**aplicaciones completas** dentro de tu cluster de Kubernetes.

### El problema que resuelve

Para instalar RabbitMQ sin Helm necesitarías crear y mantener a mano estos archivos:

```
rabbitmq-statefulset.yaml     → los pods que corren RabbitMQ
rabbitmq-service.yaml         → para exponer los puertos
rabbitmq-configmap.yaml       → configuración de rabbit
rabbitmq-secret.yaml          → usuario y contraseña cifrados
rabbitmq-pvc.yaml             → volumen de disco para que los mensajes persistan
rabbitmq-serviceaccount.yaml  → permisos internos de K8s
rabbitmq-rbac.yaml            → roles de seguridad
... más archivos
```

Con Helm, todo eso viene empaquetado en lo que se llama un **Chart**, y lo instalas así:

```bash
helm install rabbitmq bitnami/rabbitmq --values rabbitmq-values-qa.yaml -n qa
```

**Un solo comando reemplaza todos esos archivos.**

---

## Conceptos clave de Helm

| Término | Qué es | Analogía |
|---|---|---|
| **Chart** | El paquete con todo lo necesario para instalar una app | Como un `.jar` ejecutable |
| **Release** | Una instalación concreta de un Chart en tu cluster | Como una instancia corriendo |
| **Repository** | Repositorio remoto donde están los Charts disponibles | Como Maven Central |
| **values.yaml** | Archivo donde personalizas la instalación (usuario, RAM, disco, etc.) | Como tu `application.yml` |
| **Namespace** | Espacio aislado dentro de K8s donde vive tu release | Como un package en Java |
| **Bitnami** | La empresa que mantiene el Chart oficial de RabbitMQ | El proveedor del paquete |

---

## ¿Por qué Helm y no kubectl con YAML directo?

El archivo `RABBIT_VPS_SETUP.md` ya tenía una instalación con `kubectl apply` que funciona,
pero Helm tiene ventajas importantes:

| Capacidad | kubectl + YAML | Helm |
|---|---|---|
| Instalar | Sí, pero muchos archivos | Sí, un solo comando |
| **Actualizar versión de RabbitMQ** | Editar YAML a mano, arriesgado | `helm upgrade` — automático |
| **Volver a versión anterior** | Muy difícil | `helm rollback` — un comando |
| **Ver historial de cambios** | No hay | `helm history rabbitmq` |
| **Desinstalar limpiamente** | Borrar cada YAML a mano | `helm uninstall rabbitmq` |
| **Gestionar secretos** | Manual | Integrado en el Chart |
| **Alta disponibilidad (cluster)** | Muy complejo de configurar | Una línea en values.yaml |

---

## Arquitectura de lo que vamos a montar

```
VPS
└── K8s Cluster
    ├── Namespace: qa (QA)
    │   └── Helm Release: rabbitmq
    │       ├── StatefulSet  → Pod con RabbitMQ corriendo
    │       ├── Service      → puerto 5672 (AMQP) accesible dentro del namespace
    │       ├── Service      → puerto 15672 (panel web de administración)
    │       ├── Secret       → usuario y contraseña cifrados por K8s
    │       └── PVC          → 2GB de disco para persistir mensajes y colas
    │
    └── Namespace: default (PROD)
        └── Helm Release: rabbitmq
            ├── StatefulSet
            ├── Service (5672)
            ├── Service (15672)
            ├── Secret
            └── PVC (5GB para prod)
```

Tu microservicio `proyecto-key-deployment` se conecta al **Service** de RabbitMQ
usando el nombre `rabbitmq` dentro del mismo namespace.
K8s resuelve ese nombre automáticamente — no necesitas IP ni URL externa.

---

## PASO 0 — Verificar el estado actual antes de instalar Helm

Conéctate al VPS por SSH y ejecuta estos comandos para ver qué hay ahora:

```bash
# Ver todos los namespaces que tienes
kubectl get namespaces

# Ver qué hay corriendo en qa (QA)
kubectl get all -n qa

# Ver qué hay en default (PROD)
kubectl get all -n default

# Verificar si ya hay algún RabbitMQ instalado
kubectl get pods -A | grep rabbit
kubectl get services -A | grep rabbit
kubectl get pvc -A | grep rabbit
```

### Si ya tienes RabbitMQ instalado con el YAML viejo (kubectl apply)

> Si ejecutaste el `RABBIT_VPS_SETUP.md` anterior, debes limpiar antes de usar Helm.
> Si NO ejecutaste ese manual todavía, salta directamente al PASO 1.

```bash
# Borrar en QA (qa)
kubectl delete deployment rabbitmq -n qa
kubectl delete service rabbitmq -n qa
kubectl delete pvc rabbitmq-data -n qa
kubectl delete secret rabbitmq-secret -n qa

# Borrar en PROD (default)
kubectl delete deployment rabbitmq -n default
kubectl delete service rabbitmq -n default
kubectl delete pvc rabbitmq-data -n default
kubectl delete secret rabbitmq-secret -n default
```

Verificar que ya no hay nada de rabbit:
```bash
kubectl get all -n qa | grep rabbit      # debe estar vacío
kubectl get all -n default | grep rabbit # debe estar vacío
```

---

## PASO 1 — Instalar Helm en el VPS

Helm se instala una sola vez en el VPS. Este paso no se repite.

```bash
# Descargar e instalar Helm v3 (el script oficial detecta el OS automáticamente)
curl https://raw.githubusercontent.com/helm/helm/main/scripts/get-helm-3 | bash
```

Verificar que se instaló correctamente:
```bash
helm version
# Debe mostrar algo como: version.BuildInfo{Version:"v3.x.x", ...}
```

---

## PASO 2 — Agregar el repositorio de Bitnami

Bitnami mantiene los Charts oficiales de RabbitMQ, Redis, MySQL, y muchas apps más.
Solo necesitas agregarlo una vez.

```bash
# Agregar el repositorio
helm repo add bitnami https://charts.bitnami.com/bitnami

# Actualizar la lista de Charts disponibles (como "apt-get update")
helm repo update

# Verificar que el repositorio está disponible
helm repo list
# Debe mostrar:
# NAME     URL
# bitnami  https://charts.bitnami.com/bitnami
```

Buscar el Chart de RabbitMQ para ver las versiones disponibles:
```bash
helm search repo bitnami/rabbitmq
```

---

## PASO 3 — Los archivos values.yaml

Aquí es donde configuras exactamente cómo quieres que se instale RabbitMQ.
Son archivos de texto que le dicen a Helm: cuánta memoria usar, qué contraseña
poner, cuánto disco reservar, etc. Piénsalos como el `application.yml` de Spring
pero para Kubernetes.

Vas a crear **dos archivos**: uno para QA y otro para PROD.

---

### ⚠️ Importante — dónde crear los archivos en el VPS

Estás conectado como el usuario `ubuntu`, **no como root**. Por eso:

- `/root/helm/` → **NO funciona** (es la carpeta de root, no tienes permiso)
- `sudo cd /root` → **NO funciona** (`cd` es un comando del shell, `sudo` no puede usarlo)

Usa tu carpeta de usuario, que sí tienes permiso:

```bash
# Crear la carpeta en tu home (ubuntu)
mkdir -p ~/helm

# Entrar a esa carpeta
cd ~/helm

# Verificar que estás en el lugar correcto
pwd
# Debe mostrar: /home/ubuntu/helm
```

> **¿Qué significa `~/helm`?**
> El símbolo `~` es un atajo que significa "mi carpeta de usuario".
> Para el usuario `ubuntu` equivale a `/home/ubuntu/helm`.
> Siempre tienes permiso de escritura ahí sin necesitar `sudo`.

---

### Crear el archivo para QA

**1.** Abre el editor:
```bash
nano ~/helm/rabbitmq-values-qa.yaml
```

**2.** Se abre una pantalla en blanco. Copia y pega exactamente este contenido:

```yaml
# ============================================================
# RabbitMQ — valores para QA (namespace: qa)
# Usados con: helm install rabbitmq bitnami/rabbitmq \
#               --values rabbitmq-values-qa.yaml -n qa
# ============================================================

# --- CREDENCIALES ---
# Son los datos para entrar al panel web y para que tus apps se conecten.
# SPRING_RABBITMQ_USERNAME y SPRING_RABBITMQ_PASSWORD en K8s deben coincidir con estos valores.
# --- IMAGEN ---
# Fija la versión de la imagen para evitar que Helm use un tag que no existe en Docker Hub.
image:
  tag: 3.13.7-debian-12-r4

auth:
  username: admin
  password: "RabbitQALuvianos#2026"      # Cambia esto por una contraseña segura
  erlangCookie: "erlang-cookie-qa-secreto-2026"
  # erlangCookie: necesario si en el futuro escalaras a múltiples nodos de Rabbit.
  # Debe ser una cadena aleatoria y mantenerse igual entre upgrades.

# --- NÚMERO DE PODS ---
# replicaCount: 1 = un solo nodo RabbitMQ.
# Para QA es suficiente. No necesitas más.
replicaCount: 1

# --- DISCO PERSISTENTE ---
# Aquí se guardan las colas y mensajes para que sobrevivan si el pod se reinicia.
persistence:
  enabled: true        # SIEMPRE true — si lo pones en false pierdes mensajes al reiniciar
  size: 2Gi            # 2 Gigabytes de disco para QA

# --- RECURSOS DE CPU Y MEMORIA ---
# requests: lo mínimo garantizado que reserva K8s para este pod.
# limits: el máximo que puede usar. Si lo supera, K8s lo reinicia.
resources:
  requests:
    memory: "128Mi"    # 128 Megabytes mínimos garantizados
    cpu: "100m"        # 0.1 cores mínimos (100 milicores)
  limits:
    memory: "256Mi"    # Máximo 256 Megabytes
    cpu: "300m"        # Máximo 0.3 cores

# --- TIPO DE SERVICIO ---
# ClusterIP = el puerto de RabbitMQ SOLO es accesible dentro del cluster K8s.
# No se expone al internet. Tus apps se conectan por nombre interno (rabbitmq).
# NodePort = abriría el puerto hacia el exterior (NO recomendado para producción).
service:
  type: ClusterIP

# --- PANEL WEB DE ADMINISTRACIÓN ---
# El plugin de management permite usar el panel web en el puerto 15672.
# Con port-forward puedes accederlo desde tu máquina local de forma segura.
metrics:
  enabled: false       # Prometheus metrics — dejarlo false hasta que tengas Prometheus

# --- PLUGINS HABILITADOS ---
# rabbitmq_management: activa el panel web (OBLIGATORIO para poder monitorear)
extraPlugins: "rabbitmq_management"
```

Guarda con `Ctrl+O` → `Enter` → `Ctrl+X`.

Verifica que el archivo quedó creado:
```bash
cat ~/helm/rabbitmq-values-qa.yaml
# Debe mostrarte el contenido que pegaste
```

---

### Crear el archivo para PROD

**1.** Abre el editor:
```bash
nano ~/helm/rabbitmq-values-prod.yaml
```

**2.** Se abre una pantalla en blanco. Copia y pega exactamente este contenido:

### values para PROD (namespace: default)

```yaml
# ============================================================
# RabbitMQ — valores para PROD (namespace: default)
# Usados con: helm install rabbitmq bitnami/rabbitmq \
#               --values rabbitmq-values-prod.yaml -n default
# ============================================================

# --- IMAGEN ---
# Fija la versión de la imagen para evitar que Helm use un tag que no existe en Docker Hub.
image:
  tag: 3.13.7-debian-12-r4

# --- CREDENCIALES ---
auth:
  username: admin
  password: "RabbitPROD#2026"     # OBLIGATORIO: cambia esto antes de ejecutar
  erlangCookie: "erlang-cookie-prod-secreto-diferente-2026"
  # El erlangCookie de prod DEBE ser diferente al de QA

# --- NÚMERO DE PODS ---
replicaCount: 1
# Cuando quieras alta disponibilidad en el futuro, cambia a 3 y ejecuta helm upgrade.

# --- DISCO PERSISTENTE ---
persistence:
  enabled: true
  size: 5Gi            # 5 Gigabytes para prod (más mensajes, más espacio)

# --- RECURSOS DE CPU Y MEMORIA ---
resources:
  requests:
    memory: "256Mi"
    cpu: "200m"
  limits:
    memory: "512Mi"    # El doble que QA para soportar carga real
    cpu: "500m"

# --- TIPO DE SERVICIO ---
service:
  type: ClusterIP      # Igual que QA, solo accesible dentro del cluster

metrics:
  enabled: false

extraPlugins: "rabbitmq_management"
```

Guarda con `Ctrl+O` → `Enter` → `Ctrl+X`.

---

### Verificar que el PASO 3 quedó completo

```bash
ls -la ~/helm/
# Debes ver exactamente estos dos archivos:
# rabbitmq-values-qa.yaml
# rabbitmq-values-prod.yaml
```

Si los ves, el PASO 3 está terminado. Continúa al PASO 4.

> **Nota sobre las contraseñas:** la contraseña que pusiste aquí
> (`RabbitQA#2026` y `RabbitPROD#2026`) deberá ser exactamente la misma
> que configures en el PASO 6 como variable de entorno `SPRING_RABBITMQ_PASSWORD`
> en tus deployments. Si no coinciden, Spring Boot no podrá conectarse a RabbitMQ.

---

## PASO 4 — Instalar RabbitMQ en QA (namespace: qa)

> **Nota:** Bitnami dejó de ofrecer sus imágenes Docker de forma gratuita desde agosto 2025.
> Por eso usamos la imagen oficial de RabbitMQ (`rabbitmq:3.13-management`) directamente con kubectl.

**1.** Crea el archivo en el VPS copiando y pegando este bloque completo en la terminal:

```bash
cat > ~/helm/rabbitmq-manifest-qa.yaml << 'EOF'
apiVersion: apps/v1
kind: StatefulSet
metadata:
  name: rabbitmq
  namespace: qa
spec:
  serviceName: rabbitmq
  replicas: 1
  selector:
    matchLabels:
      app: rabbitmq
  template:
    metadata:
      labels:
        app: rabbitmq
    spec:
      containers:
      - name: rabbitmq
        image: rabbitmq:3.13-management
        ports:
        - containerPort: 5672
          name: amqp
        - containerPort: 15672
          name: management
        env:
        - name: RABBITMQ_DEFAULT_USER
          value: "admin"
        - name: RABBITMQ_DEFAULT_PASS
          value: "RabbitQALuvianos#2026"
        resources:
          requests:
            memory: "128Mi"
            cpu: "100m"
          limits:
            memory: "256Mi"
            cpu: "300m"
        volumeMounts:
        - name: rabbitmq-data
          mountPath: /var/lib/rabbitmq
  volumeClaimTemplates:
  - metadata:
      name: rabbitmq-data
    spec:
      accessModes: ["ReadWriteOnce"]
      resources:
        requests:
          storage: 2Gi
---
apiVersion: v1
kind: Service
metadata:
  name: rabbitmq
  namespace: qa
spec:
  type: ClusterIP
  selector:
    app: rabbitmq
  ports:
  - name: amqp
    port: 5672
    targetPort: 5672
  - name: management
    port: 15672
    targetPort: 15672
EOF
```

**2.** Aplica el manifest:

```bash
kubectl apply -f ~/helm/rabbitmq-manifest-qa.yaml
```

**3.** Espera a que levante:

```bash
kubectl get pods -n qa -w
# Esperar hasta: rabbitmq-0   1/1   Running   0   ...
# Ctrl+C para salir
```

**4.** Verifica:

```bash
kubectl get pods -n qa | grep rabbitmq
kubectl get services -n qa | grep rabbitmq
kubectl get pvc -n qa | grep rabbitmq
# STATUS debe ser: Bound
```

---

## PASO 5 — Instalar RabbitMQ en PROD (namespace: default)

**1.** Crea el archivo en el VPS:

```bash
cat > ~/helm/rabbitmq-manifest-prod.yaml << 'EOF'
apiVersion: apps/v1
kind: StatefulSet
metadata:
  name: rabbitmq
  namespace: default
spec:
  serviceName: rabbitmq
  replicas: 1
  selector:
    matchLabels:
      app: rabbitmq
  template:
    metadata:
      labels:
        app: rabbitmq
    spec:
      containers:
      - name: rabbitmq
        image: rabbitmq:3.13-management
        ports:
        - containerPort: 5672
          name: amqp
        - containerPort: 15672
          name: management
        env:
        - name: RABBITMQ_DEFAULT_USER
          value: "admin"
        - name: RABBITMQ_DEFAULT_PASS
          value: "RabbitPROD#2026"
        resources:
          requests:
            memory: "256Mi"
            cpu: "200m"
          limits:
            memory: "512Mi"
            cpu: "500m"
        volumeMounts:
        - name: rabbitmq-data
          mountPath: /var/lib/rabbitmq
  volumeClaimTemplates:
  - metadata:
      name: rabbitmq-data
    spec:
      accessModes: ["ReadWriteOnce"]
      resources:
        requests:
          storage: 5Gi
---
apiVersion: v1
kind: Service
metadata:
  name: rabbitmq
  namespace: default
spec:
  type: ClusterIP
  selector:
    app: rabbitmq
  ports:
  - name: amqp
    port: 5672
    targetPort: 5672
  - name: management
    port: 15672
    targetPort: 15672
EOF
```

**2.** Aplica y verifica:

```bash
kubectl apply -f ~/helm/rabbitmq-manifest-prod.yaml

kubectl get pods -n default -w
# Esperar hasta: rabbitmq-0   1/1   Running

kubectl get pods -n default | grep rabbitmq
kubectl get services -n default | grep rabbitmq
kubectl get pvc -n default | grep rabbitmq
```

---

## PASO 6 — Configurar las variables de entorno en tus deployments

Spring Boot lee las credenciales de RabbitMQ desde variables de entorno.
El `application.yml` del perfil prod ya las tiene configuradas así:

```yaml
spring:
  rabbitmq:
    host: rabbitmq     # sobreescrito por SPRING_RABBITMQ_HOST en K8s
    port: 5672
    username: admin    # sobreescrito por SPRING_RABBITMQ_USERNAME en K8s
    password: changeme # sobreescrito por SPRING_RABBITMQ_PASSWORD en K8s
```

> Spring Boot mapea automáticamente las variables de entorno `SPRING_RABBITMQ_*`
> a `spring.rabbitmq.*`. No hace falta usar `${RABBITMQ_HOST}` en el yml;
> basta con setear las variables con el prefijo `SPRING_`.

Ahora hay que inyectarlas en los deployments de K8s:

### En QA (namespace: qa)

Primero, verifica el nombre exacto de tus deployments:
```bash
kubectl get deployments -n qa
# Esto te muestra todos los deployments. Busca el de mis-productos y el de imagenes.
```

Inyectar las variables:
```bash
# En el microservicio mis-productos (proyecto-key)
kubectl set env deployment/proyecto-key-deployment -n qa \
  SPRING_RABBITMQ_HOST=rabbitmq \
  SPRING_RABBITMQ_PORT=5672 \
  SPRING_RABBITMQ_USERNAME=admin \
  SPRING_RABBITMQ_PASSWORD=RabbitQALuvianos#2026 \
  SPRING_RABBITMQ_VIRTUAL_HOST=/

# En micro_imagenes (ajusta el nombre del deployment según lo que viste arriba)
kubectl set env deployment/imagenes-deployment -n qa \
  SPRING_RABBITMQ_HOST=rabbitmq \
  SPRING_RABBITMQ_PORT=5672 \
  SPRING_RABBITMQ_USERNAME=admin \
  SPRING_RABBITMQ_PASSWORD=RabbitQALuvianos#2026 \
  SPRING_RABBITMQ_VIRTUAL_HOST=/
```

### En PROD (namespace: default)

```bash
kubectl get deployments -n default

kubectl set env deployment/proyecto-key-deployment -n default \
  SPRING_RABBITMQ_HOST=rabbitmq \
  SPRING_RABBITMQ_PORT=5672 \
  SPRING_RABBITMQ_USERNAME=admin \
  SPRING_RABBITMQ_PASSWORD=RabbitPROD#2026 \
  SPRING_RABBITMQ_VIRTUAL_HOST=/

kubectl set env deployment/<nombre-deployment-imagenes> -n default \
  SPRING_RABBITMQ_HOST=rabbitmq \
  SPRING_RABBITMQ_PORT=5672 \
  SPRING_RABBITMQ_USERNAME=admin \
  SPRING_RABBITMQ_PASSWORD=RabbitPROD#2026 \
  SPRING_RABBITMQ_VIRTUAL_HOST=/
```

---

## PASO 7 — Reiniciar los deployments

Después de inyectar variables de entorno, los pods se reinician solos automáticamente.
Si no lo hacen, fuérzalo:

```bash
# QA
kubectl rollout restart deployment/proyecto-key-deployment -n qa
kubectl rollout restart deployment/imagenes-deployment -n qa

# PROD
kubectl rollout restart deployment/proyecto-key-deployment -n default
kubectl rollout restart deployment/<nombre-deployment-imagenes> -n default
```

---

## PASO 8 — Verificar que la conexión funciona

### Si RabbitMQ arrancó después que los microservicios

Spring Boot intenta conectarse a Rabbit al iniciar. Si Rabbit no estaba listo en ese momento,
el microservicio queda sin conexión aunque Rabbit levante después. Solución: reiniciar los pods:

```bash
kubectl rollout restart deployment/proyecto-key-deployment -n qa
kubectl rollout restart deployment/imagenes-deployment -n qa

# Espera a que estén Running de nuevo
kubectl get pods -n qa -w
```

### Revisar si la conexión se estableció

Busca en el historial de logs (sin `-f`, termina solo):

```bash
kubectl logs deployment/proyecto-key-deployment -n qa | grep -i rabbit
```

Debes ver algo como:
```
Created new connection: rabbitConnectionFactory
```

Si no aparece nada, amplía la búsqueda:
```bash
kubectl logs deployment/proyecto-key-deployment -n qa | grep -i "amqp\|rabbit\|5672"
```

> **Nota sobre el `-f`:** `kubectl logs -f ... | grep` se queda "congelado" si no hay actividad
> nueva relacionada con rabbit. Es normal. Usa los comandos de arriba (sin `-f`) para
> buscar en el historial.

### Verificar las variables de entorno del pod

Si no hay conexión y no hay errores, confirma que las variables llegaron al pod:

```bash
kubectl exec deployment/proyecto-key-deployment -n qa -- env | grep -i rabbit
# Deben aparecer SPRING_RABBITMQ_HOST, SPRING_RABBITMQ_USERNAME, etc.
```

Si no aparecen las variables con el prefijo `SPRING_`, ejecuta de nuevo el PASO 6.

### Ver logs de micro_imagenes

```bash
kubectl logs deployment/imagenes-deployment -n qa | grep -i rabbit
```

Si ves errores de autenticación, verifica que la contraseña en `SPRING_RABBITMQ_PASSWORD`
sea exactamente igual a la que configuraste en el manifest de RabbitMQ.

---

## PASO 9 — Acceder al panel web de RabbitMQ

El panel web está en el puerto 15672. Para verlo desde tu máquina local
usas `port-forward` que crea un túnel SSH temporal:

```bash
# Para QA — ejecuta esto en tu máquina local (no en el VPS)
kubectl port-forward svc/rabbitmq 15672:15672 -n qa

# Para PROD — ejecuta esto en tu máquina local
kubectl port-forward svc/rabbitmq 15673:15672 -n default
# Usamos el puerto 15673 en local para no chocar con QA
```

Luego abre en tu navegador:
- QA:   `http://localhost:15672`  → usuario: `admin` / contraseña: `RabbitQA#2026`
- PROD: `http://localhost:15673`  → usuario: `admin` / contraseña: `RabbitPROD#2026`

> El port-forward es temporal. Se cierra cuando cierras la terminal.
> Para el día a día de desarrollo es suficiente.

---

## Resumen de nombres de servicio K8s

Estos son los hosts que usan tus microservicios para conectarse a cada servicio.
K8s resuelve estos nombres automáticamente dentro del mismo namespace.

| Servicio   | Host en Spring (`application.yml`) | Namespace     |
|------------|-------------------------------------|---------------|
| MySQL      | `${DB_HOST}`                        | externo (VPS) |
| Redis      | `redis`                             | qa / default  |
| RabbitMQ   | `rabbitmq`                          | qa / default  |

---

## Comandos Helm del día a día

### Ver qué releases tienes instaladas

```bash
# Ver todas las releases en todos los namespaces
helm list -A

# Ver solo en QA
helm list -n qa

# Ver solo en PROD
helm list -n default
```

### Ver el estado de una release

```bash
helm status rabbitmq -n qa
helm status rabbitmq -n default
```

### Ver historial de cambios de una release

```bash
helm history rabbitmq -n qa
# Muestra cada upgrade, qué valores cambió, si fue exitoso o falló
```

### Actualizar la configuración (upgrade)

Si cambias algo en el `values.yaml` (por ejemplo la contraseña o el tamaño del disco):

```bash
helm upgrade rabbitmq bitnami/rabbitmq \
  --values rabbitmq-values-qa.yaml \
  --namespace qa
```

Helm aplica solo los cambios, no reinstala todo desde cero.

### Actualizar la versión de RabbitMQ

```bash
# Ver qué versión del Chart está disponible
helm search repo bitnami/rabbitmq --versions | head -5

# Actualizar a la última versión del Chart
helm repo update
helm upgrade rabbitmq bitnami/rabbitmq \
  --values rabbitmq-values-qa.yaml \
  --namespace qa
```

### Volver atrás si algo falla (rollback)

```bash
# Ver el historial para saber a qué revisión volver
helm history rabbitmq -n qa

# Volver a la revisión anterior
helm rollback rabbitmq -n qa

# Volver a una revisión específica (por ejemplo la 2)
helm rollback rabbitmq 2 -n qa
```

### Desinstalar RabbitMQ completamente

```bash
# Esto borra el pod, el service, el secret, pero NO borra el PVC (el disco con datos)
helm uninstall rabbitmq -n qa

# Si también quieres borrar el disco con todos los mensajes:
kubectl delete pvc data-rabbitmq-0 -n qa
```

---

## ¿Qué más puedes instalar con Helm en el futuro?

Una vez que domines el flujo de Helm, puedes hacer lo mismo con cualquier otra app:

```bash
# Redis (si lo mueves de kubectl a Helm)
helm install redis bitnami/redis --values redis-values.yaml -n qa

# Nginx Ingress (para exponer tus servicios con dominio y HTTPS)
helm install ingress-nginx ingress-nginx/ingress-nginx -n ingress-nginx --create-namespace

# Cert-Manager (certificados SSL automáticos con Let's Encrypt)
helm install cert-manager jetstack/cert-manager --set installCRDs=true -n cert-manager --create-namespace

# Prometheus + Grafana (monitoreo de todo el cluster)
helm install prometheus prometheus-community/kube-prometheus-stack -n monitoring --create-namespace

# Tu propio microservicio (crear un Chart personalizado para proyecto-key)
helm create proyecto-key-chart
```

---

## Solución de problemas comunes

### El pod de RabbitMQ queda en estado Pending

```bash
kubectl describe pod rabbitmq-0 -n qa
# Busca en la sección "Events" al final. Causas comunes:
# - No hay suficiente disco disponible en el VPS (el PVC no puede crearse)
# - No hay suficiente memoria disponible (baja los limits en values.yaml)
```

### Error de autenticación al conectar Spring Boot

```bash
# Ver las variables de entorno que tiene el pod de mis-productos
kubectl exec deployment/proyecto-key-deployment -n qa -- env | grep RABBIT
# Verifica que SPRING_RABBITMQ_USERNAME y SPRING_RABBITMQ_PASSWORD coincidan con los values.yaml
```

### La cola no aparece en el panel web

Las colas se crean cuando la primera app se conecta a RabbitMQ.
Si el panel muestra `Connections: 0`, tus apps no se han conectado todavía.
Revisa los logs:
```bash
kubectl logs deployment/proyecto-key-deployment -n qa | tail -50
```

### Ver todos los secretos que Helm creó

```bash
kubectl get secrets -n qa | grep rabbit
# bitnami/rabbitmq crea automáticamente un Secret con las credenciales
# No necesitas crear el Secret manualmente como en el setup anterior con kubectl
```

---

## Archivos relacionados en este proyecto

| Archivo | Qué contiene |
|---|---|
| `RABBIT_LOCAL_MANUAL.md` | Cómo usar RabbitMQ en desarrollo local con Docker |
| `RABBIT_VPS_SETUP.md` | Setup anterior con kubectl + YAML (sin Helm, referencia histórica) |
| `RABBITMQ_HELM_GUIDE.md` | **Este archivo** — guía definitiva con Helm |
| `src/main/resources/application.yml` | Config de Spring Boot (las vars `SPRING_RABBITMQ_*` la sobreescriben automáticamente) |
