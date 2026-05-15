# RabbitMQ — Comandos para ejecutar en VPS (K8s)

> NOTA: No tienes Helm instalado. Todos los recursos se crean con kubectl + YAML directo.
> Ejecutar con acceso SSH al VPS como usuario con permisos kubectl.

---

## PASO 0 — Verificar acceso y namespaces

```bash
kubectl get namespaces
# Deben aparecer: qa y default
```

---

## PASO 1 — Crear el Secret de credenciales en ambos namespaces

> Cambia la password por una segura antes de ejecutar.

```bash
# QA
kubectl create secret generic rabbitmq-secret \
  --from-literal=username=admin \
  --from-literal=password=RabbitPassword#2026 \
  -n qa

# PROD
kubectl create secret generic rabbitmq-secret \
  --from-literal=username=admin \
  --from-literal=password=RabbitPassword#2026 \
  -n default
```

Para verificar que se crearon:
```bash
kubectl get secret rabbitmq-secret -n qa
kubectl get secret rabbitmq-secret -n default
```

---

## PASO 2 — Crear el archivo rabbitmq-k8s.yml

Guarda este contenido en el VPS como `rabbitmq-k8s.yml`:

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: rabbitmq
spec:
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
          image: rabbitmq:3-management
          ports:
            - containerPort: 5672
            - containerPort: 15672
          env:
            - name: RABBITMQ_DEFAULT_USER
              valueFrom:
                secretKeyRef:
                  name: rabbitmq-secret
                  key: username
            - name: RABBITMQ_DEFAULT_PASS
              valueFrom:
                secretKeyRef:
                  name: rabbitmq-secret
                  key: password
          resources:
            requests:
              memory: "128Mi"
              cpu: "100m"
            limits:
              memory: "256Mi"
              cpu: "200m"
---
apiVersion: v1
kind: Service
metadata:
  name: rabbitmq
spec:
  selector:
    app: rabbitmq
  ports:
    - name: amqp
      port: 5672
      targetPort: 5672
    - name: management
      port: 15672
      targetPort: 15672
```

Aplicar en ambos namespaces:
```bash
kubectl apply -f rabbitmq-k8s.yml -n qa
kubectl apply -f rabbitmq-k8s.yml -n default
```

Verificar que levantó:
```bash
kubectl get pods -n qa | grep rabbitmq
kubectl get pods -n default | grep rabbitmq
# Debe mostrar STATUS: Running
```

---

## PASO 3 — Agregar variables de entorno a mis-productos

> El nombre del deployment viene del workflow: proyecto-key-deployment

```bash
# QA
kubectl set env deployment/proyecto-key-deployment \
  RABBITMQ_USERNAME=admin \
  RABBITMQ_PASSWORD=RabbitPassword#2026 \
  -n qa

# PROD
kubectl set env deployment/proyecto-key-deployment \
  RABBITMQ_USERNAME=admin \
  RABBITMQ_PASSWORD=RabbitPassword#2026 \
  -n default
```

---

## PASO 4 — Agregar variables de entorno a micro_imagenes

> Revisar el nombre exacto del deployment de micro_imagenes con:
> kubectl get deployments -n qa

```bash
# Ejemplo (ajustar el nombre del deployment):
# QA
kubectl set env deployment/<nombre-deployment-imagenes> \
  RABBITMQ_USERNAME=admin \
  RABBITMQ_PASSWORD=RabbitPassword#2026 \
  -n qa

# PROD
kubectl set env deployment/<nombre-deployment-imagenes> \
  RABBITMQ_USERNAME=admin \
  RABBITMQ_PASSWORD=RabbitPassword#2026 \
  -n default
```

---

## PASO 5 — Reiniciar los deployments para tomar los nuevos env vars

```bash
# QA
kubectl rollout restart deployment/proyecto-key-deployment -n qa
kubectl rollout restart deployment/<nombre-deployment-imagenes> -n qa

# PROD
kubectl rollout restart deployment/proyecto-key-deployment -n default
kubectl rollout restart deployment/<nombre-deployment-imagenes> -n default
```

---

## PASO 6 — Verificar que todo está correcto

```bash
# Ver todos los pods corriendo en QA
kubectl get pods -n qa

# Ver logs de mis-productos para confirmar conexión a Rabbit
kubectl logs -f deployment/proyecto-key-deployment -n qa | grep -i rabbit

# Ver logs de micro_imagenes para confirmar que consume mensajes
kubectl logs -f deployment/<nombre-deployment-imagenes> -n qa | grep -i rabbit
```

En los logs debes ver algo como:
```
Created new connection: rabbitConnectionFactory
```

---

## NOTA — Helm (para el futuro)

Actualmente no tienes Helm instalado. Con Helm el PASO 2 se reduciría a:
```bash
helm repo add bitnami https://charts.bitnami.com/bitnami
helm install rabbitmq bitnami/rabbitmq --set auth.username=admin --set auth.password=xxx -n qa
```
Cuando lo instales, considera migrar la infra de RabbitMQ a Helm para QA y PROD.

Instalar Helm en el VPS:
```bash
curl https://raw.githubusercontent.com/helm/helm/main/scripts/get-helm-3 | bash
```

---

## Resumen de nombres de servicio K8s

| Servicio   | Host en Spring         | Namespace |
|------------|------------------------|-----------|
| MySQL      | ${DB_HOST}             | externo   |
| Redis      | redis                  | qa/default|
| RabbitMQ   | rabbitmq               | qa/default|

El nombre `rabbitmq` funciona porque K8s resuelve el Service del mismo namespace automáticamente.