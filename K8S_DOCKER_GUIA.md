# Guía de comandos — Kubernetes (K3s) y Docker

> Documento de referencia completo. Si quieres hacer algo, búscalo aquí
> y ejecuta el comando directamente.

---

## PARTE 1 — Kubernetes / K3s

---

### Ver el estado general

```bash
# Ver todos los pods de todos los namespaces
kubectl get pods -A

# Ver pods de un namespace específico
kubectl get pods -n default
kubectl get pods -n qa
kubectl get pods -n futbol

# Ver pods con más detalle (IP, nodo, estado)
kubectl get pods -A -o wide

# Ver deployments
kubectl get deployments -A

# Ver servicios (puertos expuestos)
kubectl get services -A

# Ver todo de un namespace de un vistazo
kubectl get all -n default
```

---

### Ver logs de una app

```bash
# Logs en tiempo real de un deployment
kubectl logs deployment/proyecto-key-deployment -n default -f

# Logs de las últimas 100 líneas
kubectl logs deployment/proyecto-key-deployment -n default --tail=100

# Logs del contenedor anterior (cuando crasheó)
kubectl logs deployment/proyecto-key-deployment -n default --previous

# Logs de un pod específico (reemplaza el nombre del pod)
kubectl logs proyecto-key-deployment-6f9b5b5ff4-mg85r -n default -f
```

---

### Ver detalle de un pod o deployment

```bash
# Ver detalle completo de un deployment (variables de entorno, imagen, etc.)
kubectl describe deployment proyecto-key-deployment -n default

# Ver detalle de un pod (útil cuando no arranca)
kubectl describe pod -n default -l app=proyecto-key

# Ver las variables de entorno de un pod
kubectl exec deployment/proyecto-key-deployment -n default -- env
```

---

### Reiniciar una app

```bash
# Reiniciar un deployment (hace rolling restart sin downtime)
kubectl rollout restart deployment/proyecto-key-deployment -n default

# Reiniciar todos los deployments de un namespace
kubectl rollout restart deployment -n default

# Ver el estado del reinicio
kubectl rollout status deployment/proyecto-key-deployment -n default
```

---

### Escalar una app (cambiar número de réplicas)

```bash
# Poner 2 réplicas (2 pods corriendo al mismo tiempo)
kubectl scale deployment proyecto-key-deployment -n default --replicas=2

# Volver a 1
kubectl scale deployment proyecto-key-deployment -n default --replicas=1

# Apagar temporalmente (0 réplicas = app detenida pero no eliminada)
kubectl scale deployment proyecto-key-deployment -n default --replicas=0
```

---

### Cambiar variables de entorno sin editar el YAML

```bash
# Agregar o cambiar una variable de entorno
kubectl set env deployment/proyecto-key-deployment -n default NOMBRE_VAR="valor"

# Agregar varias a la vez
kubectl set env deployment/proyecto-key-deployment -n default \
  VARIABLE_1="valor1" \
  VARIABLE_2="valor2"

# Ver las variables actuales del deployment
kubectl set env deployment/proyecto-key-deployment -n default --list
```

---

### Aplicar o actualizar un YAML

```bash
# Aplicar un archivo YAML (crea o actualiza lo que haya)
kubectl apply -f mi_deployment.yaml

# Aplicar todos los YAMLs de una carpeta
kubectl apply -f ./mis_yamls/

# Eliminar lo que está en un YAML
kubectl delete -f mi_deployment.yaml
```

---

### Secrets — contraseñas y variables sensibles

```bash
# Ver los secrets que existen
kubectl get secrets -A

# Ver el contenido de un secret (en base64)
kubectl get secret db-secret -n default -o yaml

# Decodificar todos los valores de un secret en texto plano
kubectl get secret db-secret -n default -o json | python3 -c "
import json,sys,base64
s=json.load(sys.stdin)
for k,v in s['data'].items():
    print(f'{k} = {base64.b64decode(v).decode()}')
"

# Crear un secret nuevo
kubectl create secret generic mi-secret -n default \
  --from-literal=MI_VARIABLE="mi_valor" \
  --from-literal=OTRA_VARIABLE="otro_valor"

# Actualizar una variable dentro de un secret existente
kubectl patch secret db-secret -n default \
  --type='json' \
  -p='[{"op":"replace","path":"/data/MI_VARIABLE","value":"'$(echo -n "nuevo_valor" | base64)'"}]'

# Eliminar un secret
kubectl delete secret mi-secret -n default
```

---

### Namespaces

```bash
# Ver todos los namespaces
kubectl get namespaces

# Crear un namespace nuevo
kubectl create namespace mi-namespace

# Eliminar un namespace (elimina TODO lo que hay dentro)
kubectl delete namespace mi-namespace
```

---

### Entrar dentro de un pod (como SSH pero al contenedor)

```bash
# Abrir una terminal dentro del pod
kubectl exec -it deployment/proyecto-key-deployment -n default -- /bin/bash

# Si no tiene bash, probar con sh
kubectl exec -it deployment/proyecto-key-deployment -n default -- /bin/sh

# Ejecutar un comando puntual sin entrar
kubectl exec deployment/redis -n default -- redis-cli PING
```

---

### Eliminar pods y deployments

```bash
# Eliminar un pod (K8s lo vuelve a crear automáticamente)
kubectl delete pod nombre-del-pod -n default

# Eliminar un deployment completo (y sus pods)
kubectl delete deployment proyecto-key-deployment -n default

# Eliminar un servicio
kubectl delete service proyecto-key-service -n default
```

---

### Exportar YAMLs (hacer respaldo de la configuración)

```bash
# Exportar todo un namespace
kubectl get all,secret,pvc,configmap -n default -o yaml > prod_completo.yaml
kubectl get all,secret,pvc,configmap -n qa -o yaml > qa_completo.yaml
kubectl get all,secret,pvc,configmap -n futbol -o yaml > futbol_completo.yaml

# Exportar solo deployments
kubectl get deployment -n default -o yaml > prod_deployments.yaml

# Exportar solo services
kubectl get service -n default -o yaml > prod_services.yaml
```

---

### Ver uso de recursos (RAM y CPU)

```bash
# Ver consumo de cada pod
kubectl top pods -A

# Ver consumo del nodo
kubectl top nodes
```

---

### Solucionar problemas comunes

```bash
# Pod en CrashLoopBackOff — ver por qué crashea
kubectl logs deployment/nombre-deployment -n namespace --previous
kubectl describe pod -n namespace -l app=nombre-app

# Pod en Pending — ver por qué no arranca
kubectl describe pod nombre-del-pod -n namespace

# Pod en ImagePullBackOff — la imagen no se puede descargar
kubectl describe pod nombre-del-pod -n namespace
# Buscar en el output: "Events" al final, ahí dice el error real
```

---

## PARTE 2 — Docker

> Docker no está instalado en la VPS actual, pero se usa en desarrollo local
> o si en el futuro se instala en otra VPS.

---

### Ver estado general

```bash
# Ver contenedores corriendo
docker ps

# Ver todos los contenedores (incluyendo detenidos)
docker ps -a

# Ver imágenes descargadas
docker images

# Ver uso de disco de Docker
docker system df
```

---

### Iniciar y detener contenedores

```bash
# Iniciar un contenedor detenido
docker start nombre-contenedor

# Detener un contenedor
docker stop nombre-contenedor

# Reiniciar un contenedor
docker restart nombre-contenedor

# Eliminar un contenedor (debe estar detenido)
docker rm nombre-contenedor

# Detener y eliminar de una vez
docker rm -f nombre-contenedor
```

---

### Ver logs de un contenedor

```bash
# Logs en tiempo real
docker logs nombre-contenedor -f

# Últimas 100 líneas
docker logs nombre-contenedor --tail=100

# Logs con fecha y hora
docker logs nombre-contenedor -t
```

---

### Entrar dentro de un contenedor

```bash
# Abrir terminal dentro del contenedor
docker exec -it nombre-contenedor /bin/bash

# Si no tiene bash
docker exec -it nombre-contenedor /bin/sh

# Ejecutar un comando puntual
docker exec nombre-contenedor env
```

---

### Docker Compose — levantar y bajar servicios

```bash
# Levantar todos los servicios definidos en docker-compose.yml
docker compose up -d

# Bajar todos los servicios
docker compose down

# Reiniciar un servicio específico
docker compose restart nombre-servicio

# Ver logs de todos los servicios
docker compose logs -f

# Ver logs de un servicio específico
docker compose logs nombre-servicio -f

# Ver estado de los servicios
docker compose ps
```

---

### Construir y subir imágenes

```bash
# Construir una imagen desde un Dockerfile
docker build -t nombre-imagen:version .

# Ejemplo
docker build -t proyecto-key-back:1.0 .

# Subir imagen a Docker Hub
docker push nombre-usuario/nombre-imagen:version

# Descargar una imagen
docker pull nombre-imagen:version
```

---

### Limpiar espacio en disco

```bash
# Eliminar contenedores detenidos, imágenes sin uso, redes huérfanas
docker system prune

# Lo mismo pero también elimina volúmenes sin uso (cuidado con datos)
docker system prune --volumes

# Eliminar solo imágenes sin usar
docker image prune -a
```

---

### Volúmenes — datos persistentes en Docker

```bash
# Ver volúmenes
docker volume ls

# Crear un volumen
docker volume create mi-volumen

# Eliminar un volumen
docker volume rm mi-volumen

# Ver detalle de un volumen (dónde está en el disco)
docker volume inspect mi-volumen
```

---

## PARTE 3 — Comparación rápida K8s vs Docker

| Quiero... | En K8s | En Docker |
|---|---|---|
| Ver apps corriendo | `kubectl get pods -A` | `docker ps` |
| Ver logs | `kubectl logs deployment/nombre -n ns -f` | `docker logs nombre -f` |
| Reiniciar app | `kubectl rollout restart deployment/nombre -n ns` | `docker restart nombre` |
| Entrar al contenedor | `kubectl exec -it deployment/nombre -n ns -- /bin/bash` | `docker exec -it nombre /bin/bash` |
| Ver variables de entorno | `kubectl exec deployment/nombre -n ns -- env` | `docker exec nombre env` |
| Apagar app | `kubectl scale deployment/nombre -n ns --replicas=0` | `docker stop nombre` |
| Eliminar app | `kubectl delete deployment nombre -n ns` | `docker rm -f nombre` |
| Ver consumo RAM/CPU | `kubectl top pods -A` | `docker stats` |
