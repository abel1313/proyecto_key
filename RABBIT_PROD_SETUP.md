# RabbitMQ en Prod (k8s) — Plan paso a paso

Fecha: 2026-06-09

---

## Contexto

Los `application-docker.yml` de **ambos** servicios ya tienen RabbitMQ configurado:
```yaml
spring:
  rabbitmq:
    host: rabbitmq      ← busca un Service de k8s con este nombre exacto
    port: 5672
    username: ${RABBITMQ_USERNAME}
    password: ${RABBITMQ_PASSWORD}
```
El código ya está listo. Solo faltaba infraestructura en prod (`namespace: default`).

---

## PASO 1 — Diagnóstico ✅ COMPLETADO (2026-06-09)

| Qué | Resultado |
|-----|-----------|
| Pod RabbitMQ | ❌ No existía |
| Service `rabbitmq` | ❌ No existía |
| Creds en `db-secret` | ❌ No existían (`rabbitmq-username` / `rabbitmq-password` ausentes) |
| Redis | ✅ Ya existía |

Pods antes de hacer cambios:
```
imagenes-deployment-5ddf4b7fcd-9sh25             1/1   Running
proyecto-key-deployment-7c67ddbbd7-99g8t         1/1   Running
proyecto-key-front-deployment-7565ff874d-2vvjv   1/1   Running
redis-5ffb94bb5b-fmmqj                           1/1   Running
```

---

## PASO 2 — Desplegar Rabbit + creds + env vars ✅ APLICADO (2026-06-09)

### A) Manifest aplicado
Archivo: `~/k8s-deployments/rabbit/rabbitmq-manifest-prod.yaml`

Contiene:
- `StatefulSet` con `namespace: default` ✅
- `Service` con `namespace: qa` ⚠️ — **VER ALERTA ABAJO**
- imagen: `rabbitmq:3.13-management`
- credenciales: user=`admin` / pass=`RabbitQALuvianos#2026`
- PVC: 2Gi
- Service: ClusterIP puertos 5672 y 15672

### B) Creds agregadas al secret `db-secret`
- key: `rabbitmq-username` = `admin`
- key: `rabbitmq-password` = `RabbitQALuvianos#2026`

### C) Env vars agregadas a los deployments
- `proyecto-key-deployment`: `RABBITMQ_USERNAME` + `RABBITMQ_PASSWORD` desde `db-secret`
- `imagenes-deployment`: `RABBITMQ_USERNAME` + `RABBITMQ_PASSWORD` desde `db-secret`

---

## ⚠️ ALERTA — Service posiblemente en namespace equivocado

En el manifest que se aplicó, el **StatefulSet** tiene `namespace: default` (correcto)
pero el **Service** tiene `namespace: qa` (incorrecto para prod).

Si el Service quedó en `namespace: qa`, los pods de prod no pueden llegar a RabbitMQ
porque buscan `host: rabbitmq` dentro de `namespace: default`.

**Verificar YA:**
```bash
# ¿Existe el service rabbitmq en default?
kubectl get service rabbitmq -n default

# ¿Existe en qa en cambio?
kubectl get service rabbitmq -n qa
```

**Si el service está en `qa` y no en `default` → corregir:**
```bash
# Crear el service en el namespace correcto
kubectl apply -f - <<EOF
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

---

## PASO 3 — Verificar que todo arrancó bien

```bash
# 1. Ver estado de todos los pods (debe aparecer rabbitmq-0 Running)
kubectl get pods -n default

# 2. Ver que el service rabbitmq existe en default
kubectl get services -n default

# 3. Logs de proyecto-key buscando conexión a Rabbit
kubectl logs deployment/proyecto-key-deployment -n default | grep -i "rabbit\|amqp\|connection refused\|started"

# 4. Logs de imagenes buscando conexión a Rabbit
kubectl logs deployment/imagenes-deployment -n default | grep -i "rabbit\|amqp\|connection refused\|started"
```

**Lo que buscamos:**
- ✅ Pod `rabbitmq-0` en estado `Running`
- ✅ Service `rabbitmq` en `default`
- ✅ En logs: `Started ...Application` sin errores de `Connection refused` ni `ACCESS_REFUSED`
- ❌ `Connection refused` → pod de rabbit no levantó o service en namespace equivocado
- ❌ `ACCESS_REFUSED` → credenciales incorrectas

---

## Estado actual

- [x] PASO 1 — Diagnóstico corrido (2026-06-09)
- [x] PASO 2 — Manifest aplicado, creds en secret, env vars en deployments (2026-06-09)
- [x] ⚠️ Service rabbitmq en `default` — confirmado OK (2026-06-09)
- [x] PASO 3 — Pods verificados y logs limpios (2026-06-09)

## ✅ COMPLETADO — Estado final prod (2026-06-09 21:21)

| Pod | Estado |
|-----|--------|
| `rabbitmq-0` | ✅ Running |
| `proyecto-key-deployment-846cc45859-pgsms` | ✅ Running (reiniciado con Rabbit) |
| `imagenes-deployment-7568778b7d-nn8sd` | ✅ Running (reiniciado con Rabbit) |
| `redis-5ffb94bb5b-fmmqj` | ✅ Running |
| `proyecto-key-front-deployment` | ✅ Running |

Logs limpios — sin `Connection refused` ni `ACCESS_REFUSED`.
Ambas apps arrancaron correctamente con RabbitMQ configurado.

---

## Notas

- El nombre del Service DEBE ser exactamente `rabbitmq` en `namespace: default`
- En QA el env var se llama `SPRING_RABBITMQ_USERNAME`, en prod es `RABBITMQ_USERNAME` — distintos
- Credenciales prod: `admin` / `RabbitQALuvianos#2026` (mismas que QA por ahora)
