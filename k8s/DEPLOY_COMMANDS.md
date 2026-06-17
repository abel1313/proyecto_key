# RabbitMQ en K8s — Guía completa de despliegue

---

## 1. Estructura de archivos

```
k8s/
├── qa/
│   ├── secret.yaml      ← credenciales de Rabbit encodeadas en base64
│   └── rabbitmq.yaml    ← StatefulSet + Service (lee del secret)
├── prod/
│   ├── secret.yaml      ← credenciales de Rabbit encodeadas en base64
│   └── rabbitmq.yaml    ← StatefulSet + Service (lee del secret)
└── DEPLOY_COMMANDS.md   ← este archivo
```

> IMPORTANTE: los secret.yaml contienen passwords. No commitearlos a git.
> Agrega k8s/*/secret.yaml al .gitignore si quieres protegerlos.

---

## 2. Qué hace cada recurso

### Secret
Guarda las credenciales de RabbitMQ de forma segura en K8s.
Los valores van en base64 (no es cifrado, solo encoding).
El StatefulSet los lee con `secretKeyRef` en lugar de poner la password en texto plano.

```yaml
kind: Secret
data:
  username: YWRtaW4=          # base64 de "admin"
  password: UmFiYml0UFJPRCMy  # base64 de la password real
```

Para ver la password real de un Secret ya creado:
```bash
kubectl get secret rabbitmq-secret -n default -o jsonpath='{.data.password}' | base64 -d
```

---

### StatefulSet
Levanta el pod de RabbitMQ (el contenedor que corre el broker de mensajes).

**¿Por qué StatefulSet y no Deployment?**

| | Deployment | StatefulSet |
|---|---|---|
| Nombre del pod | `rabbitmq-abc123` (aleatorio) | `rabbitmq-0` (fijo siempre) |
| Si el pod muere | nace con nombre nuevo | nace con el mismo nombre |
| El disco (PVC) | se desconecta del pod | va atado al pod por nombre |
| Para RabbitMQ | malo: pierde el disco al reiniciar | bueno: siempre recupera su disco |

Con `Deployment` normal, al reiniciar el pod nacería con nombre aleatorio, se
separaría del PVC y RabbitMQ arrancaría sin sus colas y mensajes. Por eso StatefulSet.

```yaml
kind: StatefulSet
metadata:
  name: rabbitmq
  namespace: default          # default = PROD | qa = QA
spec:
  replicas: 1                 # solo 1 instancia (no cluster de Rabbit)
  containers:
  - image: rabbitmq:3.13-management   # imagen oficial con panel web incluido
    env:
    - name: RABBITMQ_DEFAULT_USER     # usuario que Rabbit crea la primera vez
      valueFrom:
        secretKeyRef:                 # lo lee del Secret, no texto plano
          name: rabbitmq-secret
          key: username
    - name: RABBITMQ_DEFAULT_PASS     # password que Rabbit crea la primera vez
      valueFrom:
        secretKeyRef:
          name: rabbitmq-secret
          key: password
    resources:
      requests:               # mínimo garantizado que K8s reserva en el nodo
        memory: "256Mi"
        cpu: "200m"
      limits:                 # máximo permitido; si lo supera K8s reinicia el pod
        memory: "512Mi"
        cpu: "500m"
```

---

### volumeClaimTemplates (disco persistente)
El disco duro virtual donde RabbitMQ guarda las colas y mensajes.
Sin esto, al reiniciar el pod todas las colas desaparecen.
Con esto, el disco sobrevive reinicios, actualizaciones y caídas.

```yaml
volumeClaimTemplates:
- spec:
    accessModes: ["ReadWriteOnce"]  # solo un pod escribe a la vez
    resources:
      requests:
        storage: 5Gi               # 5 GB (PROD) / 2 GB (QA)
```

---

### Service (ClusterIP)
El DNS interno que permite a los otros pods encontrar a RabbitMQ por nombre.
El nombre del Service (`rabbitmq`) ES el hostname que Spring pone en el YML.
Sin el Service, los pods no se encuentran aunque estén en el mismo nodo físico.

```yaml
kind: Service
metadata:
  name: rabbitmq     # este nombre es el hostname dentro del cluster
spec:
  type: ClusterIP    # solo accesible dentro del cluster, no desde internet
  ports:
  - port: 5672       # AMQP: puerto que usa Spring para mensajes
  - port: 15672      # panel web de administración de RabbitMQ
```

---

## 3. Flujo completo de variables

El secreto de Rabbit NO es el mismo que las env vars de Spring. Son dos pares distintos:

```
secret.yaml              rabbitmq.yaml (pod Rabbit)    application-*.yml (Spring)
───────────              ──────────────────────────    ─────────────────────────────
username: admin    →     RABBITMQ_DEFAULT_USER    →    [Rabbit los usa para crear el usuario]
password: Rabbit   →     RABBITMQ_DEFAULT_PASS    →    [Rabbit los usa para crear la password]

                         kubectl set env (manual)  →   spring.rabbitmq.username: ${RABBITMQ_USERNAME}
                         RABBITMQ_USERNAME=admin        spring.rabbitmq.password: ${RABBITMQ_PASSWORD}
                         RABBITMQ_PASSWORD=Rabbit        spring.rabbitmq.host: rabbitmq  ← Service
                                                        spring.rabbitmq.port: 5672
```

**En resumen:**
- El Secret le dice a RabbitMQ qué usuario/password crear al iniciar
- El `kubectl set env` le dice a Spring con qué usuario/password conectarse
- Ambas passwords deben ser iguales para que la conexión funcione

---

### Variables por ambiente

| | QA | PROD |
|---|---|---|
| Namespace | `qa` | `default` |
| Spring profile / YML | `application-qa.yml` | `application-docker.yml` |
| Nombre env var usuario en Spring | `SPRING_RABBITMQ_USERNAME` | `RABBITMQ_USERNAME` |
| Nombre env var password en Spring | `SPRING_RABBITMQ_PASSWORD` | `RABBITMQ_PASSWORD` |
| Password de Rabbit | `RabbitQALuvianos#2026` | `RabbitPROD#2026` |

> El nombre de la variable cambia entre QA y PROD porque cada YML de Spring
> usa un nombre distinto: `${SPRING_RABBITMQ_USERNAME}` vs `${RABBITMQ_USERNAME}`.
> Si inyectas el nombre equivocado, Spring arranca pero no conecta a Rabbit
> y verás en los logs: `Connection refused` o `Authentication failure`.

---

### Diagrama visual

```
┌──────────────────────────────────────────────────────────────┐
│                   namespace: default (PROD)                   │
│                                                              │
│  [Secret: rabbitmq-secret]                                   │
│    username = admin          ─────────────────────────────►  │
│    password = RabbitPROD#2026 ────────────────────────────►  │
│                                                    ▼         │
│  [StatefulSet: rabbitmq → Pod rabbitmq-0]                    │
│    RABBITMQ_DEFAULT_USER = admin    (lee del Secret)         │
│    RABBITMQ_DEFAULT_PASS = RabbitPROD#2026                   │
│         │                                                    │
│         ▼                                                    │
│    [PVC: rabbitmq-data 5Gi]  ← colas y mensajes persisten   │
│                                                              │
│  [Service: rabbitmq ClusterIP]  ← DNS interno puerto 5672   │
│         ▲                   ▲                                │
│         │                   │                                │
│  [mis-productos pod]   [micro_imagenes pod]                  │
│    RABBITMQ_USERNAME=admin     RABBITMQ_USERNAME=admin       │
│    RABBITMQ_PASSWORD=Rabbit    RABBITMQ_PASSWORD=Rabbit      │
│    Spring lee vars y conecta a rabbitmq:5672                 │
│                                                              │
└──────────────────────────────────────────────────────────────┘
```

---

## 4. Prerrequisito — copiar archivos al VPS

Desde tu máquina local:
```bash
scp -r k8s/ usuario@tu-vps:/home/usuario/
```

---

## 5. Despliegue QA (namespace: qa)

### PASO 1 — Crear el Secret con las credenciales
```bash
# El Secret debe existir ANTES que el StatefulSet o el pod no arranca
kubectl apply -f k8s/qa/secret.yaml

# Verificar que se creó
kubectl get secret rabbitmq-secret -n qa
```

### PASO 2 — Levantar RabbitMQ (StatefulSet + Service + disco)
```bash
kubectl apply -f k8s/qa/rabbitmq.yaml
```

### PASO 3 — Verificar que levantó (NO continuar si falla)
```bash
kubectl get pods -n qa | grep rabbitmq
# STATUS debe ser: Running

kubectl get pvc -n qa | grep rabbitmq
# STATUS debe ser: Bound  (= disco asignado correctamente)
```

### PASO 4 — Inyectar credenciales a mis-productos
```bash
# SPRING_RABBITMQ_* es lo que lee application-qa.yml
kubectl set env deployment/proyecto-key-deployment \
  SPRING_RABBITMQ_USERNAME=admin \
  SPRING_RABBITMQ_PASSWORD=RabbitQALuvianos#2026 \
  -n qa
```

### PASO 5 — Inyectar credenciales a micro_imagenes
```bash
# Ver nombre exacto del deployment
kubectl get deployments -n qa

# Reemplazar <nombre-imagenes> con el nombre real
kubectl set env deployment/<nombre-imagenes> \
  SPRING_RABBITMQ_USERNAME=admin \
  SPRING_RABBITMQ_PASSWORD=RabbitQALuvianos#2026 \
  -n qa
```

### PASO 6 — Reiniciar pods para que lean los nuevos env vars
```bash
kubectl rollout restart deployment/proyecto-key-deployment -n qa
kubectl rollout restart deployment/<nombre-imagenes> -n qa
```

### PASO 7 — Verificar conexión en logs
```bash
kubectl logs -f deployment/proyecto-key-deployment -n qa | grep -i rabbit
# Debe aparecer: Created new connection: rabbitConnectionFactory
```

---

## 6. Despliegue PROD (namespace: default)

### PASO 1 — Crear el Secret con las credenciales
```bash
# El Secret debe existir ANTES que el StatefulSet o el pod no arranca
kubectl apply -f k8s/prod/secret.yaml

# Verificar que se creó
kubectl get secret rabbitmq-secret -n default
```

### PASO 2 — Levantar RabbitMQ (StatefulSet + Service + disco)
```bash
kubectl apply -f k8s/prod/rabbitmq.yaml
```

### PASO 3 — Verificar que levantó (NO continuar si falla)
```bash
kubectl get pods -n default | grep rabbitmq
# STATUS debe ser: Running

kubectl get pvc -n default | grep rabbitmq
# STATUS debe ser: Bound  (= disco asignado correctamente)
```

### PASO 4 — Inyectar credenciales a mis-productos
```bash
# RABBITMQ_* es lo que lee application-docker.yml
kubectl set env deployment/proyecto-key-deployment \
  RABBITMQ_USERNAME=admin \
  RABBITMQ_PASSWORD=RabbitPROD#2026 \
  -n default
```

### PASO 5 — Inyectar credenciales a micro_imagenes
```bash
# Ver nombre exacto del deployment
kubectl get deployments -n default

# Reemplazar <nombre-imagenes> con el nombre real
kubectl set env deployment/<nombre-imagenes> \
  RABBITMQ_USERNAME=admin \
  RABBITMQ_PASSWORD=RabbitPROD#2026 \
  -n default
```

### PASO 6 — Reiniciar pods para que lean los nuevos env vars
```bash
kubectl rollout restart deployment/proyecto-key-deployment -n default
kubectl rollout restart deployment/<nombre-imagenes> -n default
```

### PASO 7 — Verificar conexión en logs
```bash
kubectl logs -f deployment/proyecto-key-deployment -n default | grep -i rabbit
# Debe aparecer: Created new connection: rabbitConnectionFactory
```

---

## 7. Cómo manejar el Secret — encodear, decodear y actualizar

### ¿Qué es base64 en el Secret?
K8s no cifra los valores del Secret, solo los guarda en base64.
Base64 es solo un formato de texto, no es seguridad real.
La seguridad viene de los permisos de K8s (RBAC) que controlan quién puede leer el Secret.

---

### Encodear una password (para ponerla en secret.yaml)

En Linux/Mac (en el VPS):
```bash
echo -n "MiNuevaPassword#2026" | base64
# -n es importante: evita que se encode el salto de línea al final
```

En Windows (PowerShell):
```powershell
[Convert]::ToBase64String([System.Text.Encoding]::UTF8.GetBytes("MiNuevaPassword#2026"))
```

---

### Decodear una password (para ver qué tiene un Secret ya creado)

```bash
# Ver el valor encodeado
kubectl get secret rabbitmq-secret -n default -o yaml

# Ver la password en texto claro directamente
kubectl get secret rabbitmq-secret -n default -o jsonpath='{.data.password}' | base64 -d

# Ver el usuario en texto claro
kubectl get secret rabbitmq-secret -n default -o jsonpath='{.data.username}' | base64 -d
```

---

### Actualizar un campo del Secret (cambiar password)

**Opción A — editar el secret.yaml y re-aplicar (recomendado)**

1. Encodear la nueva password:
```bash
echo -n "NuevaPassword#2027" | base64
# resultado: TnVldmFQYXNzd29yZCMyMDI3
```

2. Actualizar el secret.yaml con el nuevo valor:
```yaml
data:
  password: TnVldmFQYXNzd29yZCMyMDI3  # NuevaPassword#2027
```

3. Aplicar el cambio en K8s:
```bash
kubectl apply -f k8s/prod/secret.yaml
```

4. Reiniciar el pod de RabbitMQ para que tome la nueva password:
```bash
kubectl rollout restart statefulset/rabbitmq -n default
```

5. Actualizar también el env var de mis-productos y micro_imagenes:
```bash
kubectl set env deployment/proyecto-key-deployment \
  RABBITMQ_PASSWORD=NuevaPassword#2027 \
  -n default

kubectl set env deployment/<nombre-imagenes> \
  RABBITMQ_PASSWORD=NuevaPassword#2027 \
  -n default

kubectl rollout restart deployment/proyecto-key-deployment -n default
kubectl rollout restart deployment/<nombre-imagenes> -n default
```

**Opción B — actualizar directo con kubectl patch (sin tocar el yaml)**

```bash
# Encodear la nueva password primero
NUEVA=$(echo -n "NuevaPassword#2027" | base64)

# Parchear el Secret en K8s
kubectl patch secret rabbitmq-secret -n default \
  -p "{\"data\":{\"password\":\"$NUEVA\"}}"
```

> Después de cualquier cambio al Secret siempre reiniciar el StatefulSet
> y los deployments de mis-productos y micro_imagenes para que tomen el nuevo valor.

---

## 8. Diferencias QA vs PROD — resumen

| | QA | PROD |
|---|---|---|
| Namespace | `qa` | `default` |
| Secret | `k8s/qa/secret.yaml` | `k8s/prod/secret.yaml` |
| Manifest | `k8s/qa/rabbitmq.yaml` | `k8s/prod/rabbitmq.yaml` |
| Password Rabbit | `RabbitQALuvianos#2026` | `RabbitPROD#2026` |
| Env var usuario (Spring) | `SPRING_RABBITMQ_USERNAME` | `RABBITMQ_USERNAME` |
| Env var password (Spring) | `SPRING_RABBITMQ_PASSWORD` | `RABBITMQ_PASSWORD` |
| Spring YML activo | `application-qa.yml` | `application-docker.yml` |
| Memory limit | 256Mi | 512Mi |
| CPU limit | 300m | 500m |
| Disco (PVC) | 2Gi | 5Gi |
