# Auditoría de la VPS — Guía para documentar todo

> Objetivo: tener un respaldo completo de cómo está armada la infraestructura
> para poder replicarla o moverla a otra VPS en cualquier momento.
>
> Cómo usar este doc: conéctate a la VPS por SSH y ejecuta cada comando.
> Pega la respuesta debajo de cada pregunta.

---

## Cómo conectarse a la VPS

```bash
ssh usuario@IP_DE_TU_VPS
# Ejemplo: ssh root@51.178.29.99
```

---

## BLOQUE 1 — La VPS en sí

### P1. ¿Cuál es el proveedor?
> Respuesta: OVHcloud

### P2. ¿Cuánta RAM, CPU y disco tiene?
```bash
# RAM y CPU
free -h && nproc && lscpu | grep "Model name"

ubuntu@vps-da9a48f5:~$ free -h && nproc && lscpu | grep "Model name"
               total        used        free      shared  buff/cache   available
Mem:           7.6Gi       4.2Gi       611Mi        70Mi       3.1Gi       3.4Gi
Swap:             0B          0B          0B
4
Model name:                              Intel Core Processor (Haswell, no TSX)
ubuntu@vps-da9a48f5:~$


# Disco
df -h /
```
> Respuesta: Filesystem      Size  Used Avail Use% Mounted on
/dev/sda1        72G   42G   31G  58% /
ubuntu@vps-da9a48f5:~$


### P3. ¿Qué sistema operativo y versión?
```bash
cat /etc/os-release
```
> Respuesta: ubuntu@vps-da9a48f5:~$ cat /etc/os-release
PRETTY_NAME="Ubuntu 25.10"
NAME="Ubuntu"
VERSION_ID="25.10"
VERSION="25.10 (Questing Quokka)"
VERSION_CODENAME=questing
ID=ubuntu
ID_LIKE=debian
HOME_URL="https://www.ubuntu.com/"
SUPPORT_URL="https://help.ubuntu.com/"
BUG_REPORT_URL="https://bugs.launchpad.net/ubuntu/"
PRIVACY_POLICY_URL="https://www.ubuntu.com/legal/terms-and-policies/privacy-policy"
UBUNTU_CODENAME=questing
LOGO=ubuntu-logo
ubuntu@vps-da9a48f5:~$


### P4. ¿Cuál es la IP pública?
```bash
curl -s ifconfig.me
```
> Respuesta: ubuntu@vps-da9a48f5:~$ curl -s ifconfig.me
2001:41d0:305:2100::f2bdubuntu@vps-da9a48f5:~$


---

## BLOQUE 2 — Kubernetes

### P5. ¿Es K3s o K8s completo? ¿Qué versión?
```bash
# Si tienes K3s:
k3s --version

# Si tienes K8s normal:
kubectl version --short
```
> Respuesta: ubuntu@vps-da9a48f5:~$ k3s --version
k3s version v1.34.6+k3s1 (234e6132)
go version go1.24.13
ubuntu@vps-da9a48f5:~$


### P6. ¿Qué namespaces tienes?
```bash
kubectl get namespaces
```
> Respuesta: ubuntu@vps-da9a48f5:~$ kubectl get namespaces
NAME              STATUS   AGE
cert-manager      Active   58d
default           Active   60d
futbol            Active   28d
kube-node-lease   Active   60d
kube-public       Active   60d
kube-system       Active   60d
metallb-system    Active   58d
produccion        Active   52d
qa                Active   52d


### P7. ¿Qué pods están corriendo en cada namespace?
```bash
# Ver todos los pods de todos los namespaces
kubectl get pods -A

ubuntu@vps-da9a48f5:~$ kubectl get pods -A
NAMESPACE        NAME                                             READY   STATUS             RESTARTS        AGE
cert-manager     cert-manager-869f77f5d9-nc6ml                    1/1     Running            2 (24h ago)     58d
cert-manager     cert-manager-cainjector-7cd8764d8b-r6hsk         1/1     Running            2 (24h ago)     58d
cert-manager     cert-manager-webhook-697967d48d-z9hdq            1/1     Running            2 (24h ago)     58d
default          imagenes-deployment-bcf4b849-wndqb               0/1     CrashLoopBackOff   1438 (5m ago)   7d
default          proyecto-key-deployment-6f9b5b5ff4-mg85r         1/1     Running            1 (24h ago)     7d
default          proyecto-key-front-deployment-97f5c48cd-t6hch    1/1     Running            1 (24h ago)     7d
default          rabbitmq-0                                       1/1     Running            1 (24h ago)     7d1h
default          redis-5ffb94bb5b-fmmqj                           1/1     Running            2 (24h ago)     57d
futbol           prioridades-backend-656f776ff6-8wp5r             1/1     Running            1 (24h ago)     28d
kube-system      coredns-76c974cb66-krzng                         1/1     Running            2 (24h ago)     60d
kube-system      helm-install-traefik-crd-6kdd7                   0/1     Completed          0               60d
kube-system      helm-install-traefik-zlbg2                       0/1     Completed          2               60d
kube-system      local-path-provisioner-8686667995-k69w5          1/1     Running            2 (24h ago)     60d
kube-system      metrics-server-c8774f4f4-q8mwd                   1/1     Running            2 (24h ago)     60d
metallb-system   controller-7dbf649dcc-kt567                      1/1     Running            2 (24h ago)     58d
metallb-system   speaker-rtwhg                                    1/1     Running            2 (24h ago)     58d
qa               imagenes-deployment-6885d7775-7b7jb              1/1     Running            1 (24h ago)     7d3h
qa               proyecto-key-deployment-599b48449f-46gqt         1/1     Running            1 (28h ago)     2d15h
qa               proyecto-key-front-deployment-6ff6f47f5d-gnmvt   1/1     Running            1 (24h ago)     7d
qa               rabbitmq-0                                       1/1     Running            1 (24h ago)     27d
qa               redis-5ffb94bb5b-m89m5                           1/1     Running            2 (24h ago)     51d


# Ver con más detalle (nodo, IP, estado)
kubectl get pods -A -o wide
```
> Respuesta: ubuntu@vps-da9a48f5:~$ kubectl get pods -A -o wide
NAMESPACE        NAME                                             READY   STATUS      RESTARTS           AGE     IP             NODE           NOMINATED NODE   READINESS GATES
cert-manager     cert-manager-869f77f5d9-nc6ml                    1/1     Running     2 (24h ago)        58d     10.42.0.124    vps-da9a48f5   <none>           <none>
cert-manager     cert-manager-cainjector-7cd8764d8b-r6hsk         1/1     Running     2 (24h ago)        58d     10.42.0.133    vps-da9a48f5   <none>           <none>
cert-manager     cert-manager-webhook-697967d48d-z9hdq            1/1     Running     2 (24h ago)        58d     10.42.0.137    vps-da9a48f5   <none>           <none>
default          imagenes-deployment-bcf4b849-wndqb               1/1     Running     1439 (5m18s ago)   7d      10.42.0.126    vps-da9a48f5   <none>           <none>
default          proyecto-key-deployment-6f9b5b5ff4-mg85r         1/1     Running     1 (24h ago)        7d      10.42.0.125    vps-da9a48f5   <none>           <none>
default          proyecto-key-front-deployment-97f5c48cd-t6hch    1/1     Running     1 (24h ago)        7d      10.42.0.135    vps-da9a48f5   <none>           <none>
default          rabbitmq-0                                       1/1     Running     1 (24h ago)        7d1h    10.42.0.130    vps-da9a48f5   <none>           <none>
default          redis-5ffb94bb5b-fmmqj                           1/1     Running     2 (24h ago)        57d     10.42.0.141    vps-da9a48f5   <none>           <none>
futbol           prioridades-backend-656f776ff6-8wp5r             1/1     Running     1 (24h ago)        28d     10.42.0.128    vps-da9a48f5   <none>           <none>
kube-system      coredns-76c974cb66-krzng                         1/1     Running     2 (24h ago)        60d     10.42.0.131    vps-da9a48f5   <none>           <none>
kube-system      helm-install-traefik-crd-6kdd7                   0/1     Completed   0                  60d     <none>         vps-da9a48f5   <none>           <none>
kube-system      helm-install-traefik-zlbg2                       0/1     Completed   2                  60d     <none>         vps-da9a48f5   <none>           <none>
kube-system      local-path-provisioner-8686667995-k69w5          1/1     Running     2 (24h ago)        60d     10.42.0.134    vps-da9a48f5   <none>           <none>
kube-system      metrics-server-c8774f4f4-q8mwd                   1/1     Running     2 (24h ago)        60d     10.42.0.138    vps-da9a48f5   <none>           <none>
metallb-system   controller-7dbf649dcc-kt567                      1/1     Running     2 (24h ago)        58d     10.42.0.132    vps-da9a48f5   <none>           <none>
metallb-system   speaker-rtwhg                                    1/1     Running     2 (24h ago)        58d     51.178.29.99   vps-da9a48f5   <none>           <none>
qa               imagenes-deployment-6885d7775-7b7jb              1/1     Running     1 (24h ago)        7d3h    10.42.0.127    vps-da9a48f5   <none>           <none>
qa               proyecto-key-deployment-599b48449f-46gqt         1/1     Running     1 (28h ago)        2d15h   10.42.0.129    vps-da9a48f5   <none>           <none>
qa               proyecto-key-front-deployment-6ff6f47f5d-gnmvt   1/1     Running     1 (24h ago)        7d      10.42.0.139    vps-da9a48f5   <none>           <none>
qa               rabbitmq-0                                       1/1     Running     1 (24h ago)        27d     10.42.0.140    vps-da9a48f5   <none>           <none>
qa               redis-5ffb94bb5b-m89m5                           1/1     Running     2 (24h ago)        51d     10.42.0.136    vps-da9a48f5   <none>           <none>
ubuntu@vps-da9a48f5:~$


### P8. ¿Qué deployments tienes?
```bash
kubectl get deployments -A
```
> Respuesta: ubuntu@vps-da9a48f5:~$ kubectl get deployments -A
NAMESPACE        NAME                            READY   UP-TO-DATE   AVAILABLE   AGE
cert-manager     cert-manager                    1/1     1            1           58d
cert-manager     cert-manager-cainjector         1/1     1            1           58d
cert-manager     cert-manager-webhook            1/1     1            1           58d
default          imagenes-deployment             1/1     1            1           59d
default          proyecto-key-deployment         1/1     1            1           51d
default          proyecto-key-front-deployment   1/1     1            1           58d
default          redis                           1/1     1            1           57d
futbol           prioridades-backend             1/1     1            1           28d
kube-system      coredns                         1/1     1            1           60d
kube-system      local-path-provisioner          1/1     1            1           60d
kube-system      metrics-server                  1/1     1            1           60d
metallb-system   controller                      1/1     1            1           58d
qa               imagenes-deployment             1/1     1            1           51d
qa               proyecto-key-deployment         1/1     1            1           51d
qa               proyecto-key-front-deployment   1/1     1            1           51d
qa               redis                           1/1     1            1           51d
ubuntu@vps-da9a48f5:~$


### P9. ¿Qué servicios (Services) tienes expuestos?
```bash
kubectl get services -A
```
> Respuesta: ubuntu@vps-da9a48f5:~$ kubectl get services -A
NAMESPACE        NAME                         TYPE        CLUSTER-IP      EXTERNAL-IP   PORT(S)                  AGE
cert-manager     cert-manager                 ClusterIP   10.43.189.91    <none>        9402/TCP                 58d
cert-manager     cert-manager-webhook         ClusterIP   10.43.232.174   <none>        443/TCP                  58d
default          imagenes-service             NodePort    10.43.205.216   <none>        80:30096/TCP             59d
default          kubernetes                   ClusterIP   10.43.0.1       <none>        443/TCP                  60d
default          proyecto-key-front-service   NodePort    10.43.27.199    <none>        80:30001/TCP             59d
default          proyecto-key-service         NodePort    10.43.32.66     <none>        80:30010/TCP             59d
default          rabbitmq                     ClusterIP   10.43.171.146   <none>        5672/TCP,15672/TCP       7d1h
default          redis                        ClusterIP   10.43.127.134   <none>        6379/TCP                 57d
futbol           prioridades-backend-svc      NodePort    10.43.203.120   <none>        8765:31765/TCP           28d
kube-system      kube-dns                     ClusterIP   10.43.0.10      <none>        53/UDP,53/TCP,9153/TCP   60d
kube-system      metrics-server               ClusterIP   10.43.170.195   <none>        443/TCP                  60d
metallb-system   webhook-service              ClusterIP   10.43.249.71    <none>        443/TCP                  58d
qa               imagenes-service             NodePort    10.43.35.197    <none>        80:31096/TCP             51d
qa               proyecto-key-front-service   NodePort    10.43.107.130   <none>        80:31001/TCP             51d
qa               proyecto-key-service         NodePort    10.43.90.206    <none>        80:31010/TCP             51d
qa               rabbitmq                     ClusterIP   10.43.188.218   <none>        5672/TCP,15672/TCP       27d
qa               redis                        ClusterIP   10.43.14.22     <none>        6379/TCP                 51d
ubuntu@vps-da9a48f5:~$


### P10. ¿Qué Ingress tienes configurado?
```bash
kubectl get ingress -A
ubuntu@vps-da9a48f5:~$ kubectl get ingress -A
No resources found
ubuntu@vps-da9a48f5:~$

# Ver detalle de cada ingress (rutas, dominios, TLS)
kubectl describe ingress -A
```
> Respuesta: ubuntu@vps-da9a48f5:~$ kubectl describe ingress -A
No resources found
ubuntu@vps-da9a48f5:~$


### P11. ¿Qué Ingress Controller usas y qué versión?
```bash
kubectl get pods -A | grep ingress
kubectl get deployment -A | grep ingress
```
> Respuesta: ubuntu@vps-da9a48f5:~$ kubectl get pods -A | grep ingress
ubuntu@vps-da9a48f5:~$
ubuntu@vps-da9a48f5:~$ kubectl get deployment -A | grep ingress
ubuntu@vps-da9a48f5:~$


### P12. ¿Qué Secrets tienes configurados?
```bash
# Solo nombres, no valores (los valores son privados)
kubectl get secrets -A
```
> Respuesta: ubuntu@vps-da9a48f5:~$ kubectl get secrets -A
NAMESPACE        NAME                                TYPE                               DATA   AGE
cert-manager     cert-manager-webhook-ca             Opaque                             3      58d
cert-manager     letsencrypt-prod                    Opaque                             1      58d
default          db-secret                           Opaque                             18     51d
futbol           prioridades-secrets                 Opaque                             4      28d
kube-system      chart-values-traefik                helmcharts.helm.cattle.io/values   1      60d
kube-system      chart-values-traefik-crd            helmcharts.helm.cattle.io/values   0      60d
kube-system      k3s-serving                         kubernetes.io/tls                  2      60d
kube-system      sh.helm.release.v1.traefik-crd.v1   helm.sh/release.v1                 1      60d
kube-system      sh.helm.release.v1.traefik.v1       helm.sh/release.v1                 1      60d
kube-system      vps-da9a48f5.node-password.k3s      k3s.cattle.io/node-password        1      60d
metallb-system   memberlist                          Opaque                             1      58d
metallb-system   webhook-server-cert                 Opaque                             4      58d
qa               db-secret                           Opaque                             16     51d
ubuntu@vps-da9a48f5:~$


### P13. ¿Qué ConfigMaps tienes?
```bash
kubectl get configmaps -A
```
> Respuesta: ubuntu@vps-da9a48f5:~$ kubectl get configmaps -A
NAMESPACE         NAME                                                   DATA   AGE
cert-manager      kube-root-ca.crt                                       1      58d
default           kube-root-ca.crt                                       1      60d
futbol            kube-root-ca.crt                                       1      28d
kube-node-lease   kube-root-ca.crt                                       1      60d
kube-public       kube-root-ca.crt                                       1      60d
kube-system       chart-content-traefik                                  0      60d
kube-system       chart-content-traefik-crd                              0      60d
kube-system       cluster-dns                                            2      60d
kube-system       coredns                                                2      60d
kube-system       extension-apiserver-authentication                     6      60d
kube-system       kube-apiserver-legacy-service-account-token-tracking   1      60d
kube-system       kube-root-ca.crt                                       1      60d
kube-system       local-path-config                                      4      60d
metallb-system    kube-root-ca.crt                                       1      58d
metallb-system    metallb-excludel2                                      1      58d
produccion        kube-root-ca.crt                                       1      52d
qa                kube-root-ca.crt                                       1      52d
ubuntu@vps-da9a48f5:~$


### P14. ¿Tienes PersistentVolumes (almacenamiento persistente)?
```bash
kubectl get pv
kubectl get pvc -A
```
> Respuesta: ubuntu@vps-da9a48f5:~$ kubectl get pv
NAME                                       CAPACITY   ACCESS MODES   RECLAIM POLICY   STATUS   CLAIM                              STORAGECLASS   VOLUMEATTRIBUTESCLASS   REASON   AGE
pvc-16cc844d-7714-4121-98b8-7db79d6ed8cb   2Gi        RWO            Delete           Bound    default/rabbitmq-data-rabbitmq-0   local-path     <unset>                          7d1h
pvc-c9f61771-b7e7-4bbc-ad49-737e9b0fdff5   2Gi        RWO            Delete           Bound    qa/rabbitmq-data-rabbitmq-0        local-path     <unset>                          27d
pvc-cd8ed94c-2add-4eac-96ee-e09c53f4cc2a   2Gi        RWO            Delete           Bound    qa/data-rabbitmq-0                 local-path     <unset>                          27d
ubuntu@vps-da9a48f5:~$

ubuntu@vps-da9a48f5:~$ kubectl get pvc -A
NAMESPACE   NAME                       STATUS   VOLUME                                     CAPACITY   ACCESS MODES   STORAGECLASS   VOLUMEATTRIBUTESCLASS   AGE
default     rabbitmq-data-rabbitmq-0   Bound    pvc-16cc844d-7714-4121-98b8-7db79d6ed8cb   2Gi        RWO            local-path     <unset>                 7d1h
qa          data-rabbitmq-0            Bound    pvc-cd8ed94c-2add-4eac-96ee-e09c53f4cc2a   2Gi        RWO            local-path     <unset>                 27d
qa          rabbitmq-data-rabbitmq-0   Bound    pvc-c9f61771-b7e7-4bbc-ad49-737e9b0fdff5   2Gi        RWO            local-path     <unset>                 27d
ubuntu@vps-da9a48f5:~$



---

## BLOQUE 3 — Los YMLs actuales de K8s

> Estos comandos exportan los manifiestos reales de lo que está corriendo.
> Son los YMLs "verdaderos" aunque los de la PC estén desactualizados.

### Mapa de namespaces (ya confirmado)

| Namespace | Para qué es | Apps que tiene |
|---|---|---|
| `default` | **PRODUCCIÓN** | proyecto-key back, front, imagenes, rabbitmq, redis |
| `qa` | **QA** | proyecto-key back, front, imagenes, rabbitmq, redis |
| `futbol` | Otro proyecto | prioridades-backend |
| `cert-manager` | Certificados SSL | cert-manager (Let's Encrypt) |
| `metallb-system` | Load balancer interno | MetalLB |
| `produccion` | Namespace vacío (solo existe) | — |

### Puertos expuestos por NodePort (ya confirmado)

| App | Namespace | Puerto externo |
|---|---|---|
| proyecto-key back | default (prod) | 30010 |
| proyecto-key front | default (prod) | 30001 |
| imagenes | default (prod) | 30096 |
| proyecto-key back | qa | 31010 |
| proyecto-key front | qa | 31001 |
| imagenes | qa | 31096 |
| prioridades-backend | futbol | 31765 |

> El nginx externo recibe en 80/443 y redirige a estos puertos según el dominio.

---

### ⚠️ ALERTA DETECTADA

**`imagenes-deployment` en `default` (PRODUCCIÓN) está en CrashLoopBackOff**
con 1438+ reinicios en 7 días. El microservicio de imágenes de producción
lleva una semana fallando. Hay que revisarlo.

```bash
# Ver qué error tiene:
kubectl logs deployment/imagenes-deployment -n default --tail=50

# Ver por qué crashea:
kubectl describe pod -n default -l app=imagenes-deployment
```

---

### Exportar namespace PRODUCCIÓN (default)

```bash
# Deployments
kubectl get deployment -n default -o yaml > prod_deployments.yaml

# Services
kubectl get service -n default -o yaml > prod_services.yaml

# Secrets (solo estructura, los valores vienen encriptados en base64)
kubectl get secret -n default -o yaml > prod_secrets.yaml

# StatefulSets (rabbitmq usa StatefulSet)
kubectl get statefulset -n default -o yaml > prod_statefulsets.yaml

# PersistentVolumeClaims
kubectl get pvc -n default -o yaml > prod_pvc.yaml

# Todo junto en un solo archivo
kubectl get all,secret,pvc,configmap -n default -o yaml > prod_completo.yaml
```

### Exportar namespace QA

```bash
kubectl get deployment -n qa -o yaml > qa_deployments.yaml
kubectl get service -n qa -o yaml > qa_services.yaml
kubectl get secret -n qa -o yaml > qa_secrets.yaml
kubectl get statefulset -n qa -o yaml > qa_statefulsets.yaml
kubectl get pvc -n qa -o yaml > qa_pvc.yaml

# Todo junto
kubectl get all,secret,pvc,configmap -n qa -o yaml > qa_completo.yaml
```

### Exportar namespace futbol

```bash
kubectl get all,secret,configmap -n futbol -o yaml > futbol_completo.yaml
```

### Exportar Traefik (el que maneja el enrutamiento HTTP/HTTPS)

```bash
# Traefik corre en kube-system (viene por defecto en K3s)
kubectl get all -n kube-system -o yaml > kube_system_completo.yaml

# IngressRoutes de Traefik (las reglas de dominio → puerto)
kubectl get ingressroute -A -o yaml > traefik_ingressroutes.yaml 2>/dev/null || echo "No hay IngressRoutes"
kubectl get middleware -A -o yaml > traefik_middlewares.yaml 2>/dev/null || echo "No hay middlewares"
```

### Exportar cert-manager (SSL)

```bash
kubectl get all,secret,certificate,clusterissuer -n cert-manager -o yaml > cert_manager_completo.yaml
```

### Bajar todos los archivos a tu PC de una vez

```bash
# Ejecutar desde tu PC (no desde la VPS)
scp ubuntu@2001:41d0:305:2100::f2bd:/root/prod_completo.yaml ./respaldo_vps/
scp ubuntu@2001:41d0:305:2100::f2bd:/root/qa_completo.yaml ./respaldo_vps/
scp ubuntu@2001:41d0:305:2100::f2bd:/root/futbol_completo.yaml ./respaldo_vps/
scp ubuntu@2001:41d0:305:2100::f2bd:/root/traefik_ingressroutes.yaml ./respaldo_vps/
scp ubuntu@2001:41d0:305:2100::f2bd:/root/cert_manager_completo.yaml ./respaldo_vps/

# Si la IPv6 da problema, usar la IPv4 de la VPS
```

> **Guarda esos archivos en una carpeta `respaldo_vps/` fuera del repo de código.**
> Son tu respaldo completo — con ellos puedes recrear toda la infraestructura en otra VPS.

---

## BLOQUE 4 — Base de datos MySQL

### P15. ¿MySQL corre dentro de K8s o fuera (Docker/proceso)?
```bash
# Ver si hay un pod de mysql en K8s
kubectl get pods -A | grep mysql

# Ver si corre como proceso directo en la VPS
systemctl status mysql 2>/dev/null || systemctl status mysqld 2>/dev/null
```
> Respuesta: **Corre directo en la VPS como servicio systemd** (no en K8s ni Docker).
> MySQL Community Server — activo y operacional. Usa 614 MB RAM.

### P16. ¿Qué bases de datos tienes?
```bash
mysql -u root -p -e "SHOW DATABASES;"

ubuntu@vps-da9a48f5:~$ mysql -u root -p -e "SHOW DATABASES;"
Enter password:
+---------------------+
| Database            |
+---------------------+
| futbol_predicciones |
| information_schema  |
| inventario_key      |
| inventario_key_qa   |
| mysql               |
| performance_schema  |
| sys                 |
+---------------------+
ubuntu@vps-da9a48f5:~$ ^C



# O con usuario específico:
mysql -u user_ventas_qa -p -e "SHOW DATABASES;"
```
> Respuesta: ubuntu@vps-da9a48f5:~$ mysql -u user_ventas_qa -p -e "SHOW DATABASES;"
Enter password:
+---------------------+
| Database            |
+---------------------+
| futbol_predicciones |
| information_schema  |
| inventario_key_qa   |
| performance_schema  |
+---------------------+
ubuntu@vps-da9a48f5:~$ ^C


### P17. ¿Cuánto espacio ocupa la base de datos?
```bash
mysql -u root -p -e "
SELECT table_schema AS 'Base de datos',
ROUND(SUM(data_length + index_length) / 1024 / 1024, 2) AS 'Tamaño (MB)'
FROM information_schema.tables
GROUP BY table_schema;"
```
> Respuesta: ubuntu@vps-da9a48f5:~$ mysql -u root -p -e "
SELECT table_schema AS 'Base de datos',
ROUND(SUM(data_length + index_length) / 1024 / 1024, 2) AS 'Tamaño (MB)'
FROM information_schema.tables
GROUP BY table_schema;"
Enter password:
+---------------------+--------------+
| Base de datos       | Tamaño (MB)  |
+---------------------+--------------+
| futbol_predicciones |        13.64 |
| information_schema  |         0.00 |
| inventario_key      |         1.61 |
| inventario_key_qa   |         5.30 |
| mysql               |         2.80 |
| performance_schema  |         0.00 |
| sys                 |         0.02 |
+---------------------+--------------+
ubuntu@vps-da9a48f5:~$


### P18. ¿Tienes backups automáticos de MySQL?
```bash
# Ver si hay un cron de backup
crontab -l | grep mysql
ls -lh /backups/ 2>/dev/null || ls -lh /var/backups/ 2>/dev/null
```
> Respuesta: ubuntu@vps-da9a48f5:~$ crontab -l | grep mysql
ls -lh /backups/ 2>/dev/null || ls -lh /var/backups/ 2>/dev/null
total 2.1M
-rw-r--r-- 1 root root  50K Jun 12 00:00 alternatives.tar.0
-rw-r--r-- 1 root root 2.5K Jun 11 00:00 alternatives.tar.1.gz
-rw-r--r-- 1 root root 2.5K Jun  7 00:00 alternatives.tar.2.gz
-rw-r--r-- 1 root root 2.5K Jun  5 00:00 alternatives.tar.3.gz
-rw-r--r-- 1 root root 2.5K May 28 00:00 alternatives.tar.4.gz
-rw-r--r-- 1 root root 2.5K May 16 00:00 alternatives.tar.5.gz
-rw-r--r-- 1 root root 2.3K Apr 18 00:00 alternatives.tar.6.gz
-rw-r--r-- 1 root root  42K Jun  4 06:17 apt.extended_states.0
-rw-r--r-- 1 root root 4.6K May 15 20:04 apt.extended_states.1.gz
-rw-r--r-- 1 root root 4.7K May 15 19:57 apt.extended_states.2.gz
-rw-r--r-- 1 root root 4.5K Apr 20 03:47 apt.extended_states.3.gz
-rw-r--r-- 1 root root 4.4K Apr 19 04:06 apt.extended_states.4.gz
-rw-r--r-- 1 root root 4.4K Apr 17 21:03 apt.extended_states.5.gz
-rw-r--r-- 1 root root 4.2K Apr 17 20:27 apt.extended_states.6.gz
-rw-r--r-- 1 root root    0 Jun 12 00:00 dpkg.arch.0
-rw-r--r-- 1 root root   32 Jun 11 00:00 dpkg.arch.1.gz
-rw-r--r-- 1 root root   32 Jun  7 00:00 dpkg.arch.2.gz
-rw-r--r-- 1 root root   32 Jun  6 00:00 dpkg.arch.3.gz
-rw-r--r-- 1 root root   32 Jun  5 00:00 dpkg.arch.4.gz
-rw-r--r-- 1 root root   32 Jun  3 00:00 dpkg.arch.5.gz
-rw-r--r-- 1 root root   32 May 29 00:00 dpkg.arch.6.gz
-rw-r--r-- 1 root root 1.4K May 15 20:01 dpkg.diversions.0
-rw-r--r-- 1 root root  294 May 15 20:01 dpkg.diversions.1.gz
-rw-r--r-- 1 root root  294 May 15 20:01 dpkg.diversions.2.gz
-rw-r--r-- 1 root root  294 May 15 20:01 dpkg.diversions.3.gz
-rw-r--r-- 1 root root  294 May 15 20:01 dpkg.diversions.4.gz
-rw-r--r-- 1 root root  294 May 15 20:01 dpkg.diversions.5.gz
-rw-r--r-- 1 root root  294 May 15 20:01 dpkg.diversions.6.gz
-rw-r--r-- 1 root root  138 Apr 20 03:47 dpkg.statoverride.0
-rw-r--r-- 1 root root  140 Apr 20 03:47 dpkg.statoverride.1.gz
-rw-r--r-- 1 root root  140 Apr 20 03:47 dpkg.statoverride.2.gz
-rw-r--r-- 1 root root  140 Apr 20 03:47 dpkg.statoverride.3.gz
-rw-r--r-- 1 root root  140 Apr 20 03:47 dpkg.statoverride.4.gz
-rw-r--r-- 1 root root  140 Apr 20 03:47 dpkg.statoverride.5.gz
-rw-r--r-- 1 root root  140 Apr 20 03:47 dpkg.statoverride.6.gz
-rw-r--r-- 1 root root 731K Jun 11 06:14 dpkg.status.0
-rw-r--r-- 1 root root 185K Jun 10 06:06 dpkg.status.1.gz
-rw-r--r-- 1 root root 185K Jun  6 06:03 dpkg.status.2.gz
-rw-r--r-- 1 root root 185K Jun  5 06:24 dpkg.status.3.gz
-rw-r--r-- 1 root root 185K Jun  4 06:17 dpkg.status.4.gz
-rw-r--r-- 1 root root 185K Jun  2 06:22 dpkg.status.5.gz
-rw-r--r-- 1 root root 185K May 28 06:06 dpkg.status.6.gz
ubuntu@vps-da9a48f5:~$


### Cómo sacar un backup manual ahora mismo
```bash
# Backup de todas las bases de datos
mysqldump -u root -p --all-databases > backup_completo_$(date +%Y%m%d).sql

ubuntu@vps-da9a48f5:~$ mysqldump -u root -p --all-databases > backup_completo_$(date +%Y%m%d).sql
Enter password:
ubuntu@vps-da9a48f5:~$


# Backup de una base específica
mysqldump -u root -p inventario_key > backup_inventario_key_$(date +%Y%m%d).sql
mysqldump -u root -p inventario_key_qa > backup_inventario_key_qa_$(date +%Y%m%d).sql

ubuntu@vps-da9a48f5:~$ mysqldump -u root -p inventario_key > backup_inventario_key_$(date +%Y%m%d).sql
Enter password:
ubuntu@vps-da9a48f5:~$ mysqldump -u root -p inventario_key_qa > backup_inventario_key_qa_$(date +%Y%m%d).sql
Enter password:
ubuntu@vps-da9a48f5:~$



```

---

## BLOQUE 5 — Redis

### P19. ¿Redis corre dentro de K8s o en Docker fuera?
```bash
kubectl get pods -A | grep redis
kubectl exec deployment/redis -n default -- redis-server --version
kubectl exec deployment/redis -n default -- redis-cli CONFIG GET requirepass
```
> Respuesta: **Corre dentro de K8s** — hay un pod en cada namespace.
> - `default/redis-5ffb94bb5b-fmmqj` → producción (57d)
> - `qa/redis-5ffb94bb5b-m89m5` → QA (51d)
> No hay Docker instalado en la VPS.

### P20. ¿Qué versión de Redis?
> Respuesta: **Redis 7.4.8** (dentro del pod de K8s en default)
> Nota: el binario del sistema es v8.0.2 pero el que corre en K8s es v7.4.8.

### P21. ¿Redis tiene contraseña configurada?
> Respuesta: **NO** — `requirepass` devuelve vacío. Sin contraseña.


---

## BLOQUE 6 — RabbitMQ

### P22. ¿RabbitMQ corre dentro de K8s o en Docker fuera?
```bash
kubectl get pods -A | grep rabbit
kubectl exec rabbitmq-0 -n default -- rabbitmq-diagnostics server_version
kubectl exec rabbitmq-0 -n default -- rabbitmqctl list_users
kubectl exec rabbitmq-0 -n default -- rabbitmqctl list_vhosts
```
> Respuesta: **Corre dentro de K8s como StatefulSet** — hay uno en cada namespace.
> - `default/rabbitmq-0` → producción (7d)
> - `qa/rabbitmq-0` → QA (27d)

### P23. ¿Qué versión de RabbitMQ?
> Respuesta: **RabbitMQ 3.13.7**

### P24. ¿Qué usuarios y vhosts tiene RabbitMQ?
> Usuarios: `admin` (rol: administrator)
> Vhosts: `/` (solo el default)

---

## BLOQUE 7 — Docker fuera de K8s

### P25 y P26 — N/A
> **Docker NO está instalado en la VPS.**
> Todo corre en K8s (K3s) o como servicio systemd directo (MySQL, Nginx).
> No hay docker-compose.yml ni contenedores Docker fuera de K8s.


---

## BLOQUE 8 — Nginx / Ingress

### P27. ¿Nginx corre dentro de K8s o también fuera?
> Respuesta: **Nginx corre FUERA de K8s como servicio systemd** — versión 1.28.0 (Ubuntu).
> Es el punto de entrada: recibe en 80/443 y hace proxy_pass a los NodePorts de K8s.
> K3s trae Traefik pero no se usa para enrutamiento externo — Nginx lo reemplaza.

**Mapa de rutas Nginx → K8s (6 virtual hosts):**

| Dominio | Nginx proxy_pass | App en K8s |
|---|---|---|
| `backend.novedades-jade.com.mx` | `51.178.29.99:30010` | proyecto-key back (prod) |
| `backend-imagenes.novedades-jade.com.mx` | `127.0.0.1:30096` | micro_imagenes (prod) |
| `shop.novedades-jade.com.mx` | `127.0.0.1:30001` | front Angular (prod) |
| `qa.backend.novedades-jade.com.mx` | `127.0.0.1:31010` | proyecto-key back (QA) |
| `qa.backend-imagenes.novedades-jade.com.mx` | `127.0.0.1:31096` | micro_imagenes (QA) |
| `qa.shop.novedades-jade.com.mx` | `127.0.0.1:31001` | front Angular (QA) |

Config base: `/etc/nginx/nginx.conf` — incluye `/etc/nginx/sites-enabled/*`
Sites habilitados: `backend`, `backend-imagenes`, `backend-imagenes-qa`, `backend-qa`, `frontend`, `frontend-qa`

### P28. ¿Dónde están los certificados SSL?
> Respuesta: **Certbot + Let's Encrypt**, instalado directo en la VPS (no en K8s).
> cert-manager está instalado en K8s pero NO se usa para estos certificados.
> Para ver los certs: `sudo certbot certificates`

| Certificado | Dominios cubiertos | Vence |
|---|---|---|
| `backend-imagenes.novedades-jade.com.mx` | backend-imagenes.novedades-jade.com.mx | 2026-07-19 (32 días) |
| `backend.novedades-jade.com.mx` | backend.novedades-jade.com.mx | 2026-07-18 (31 días) |
| `front.novedades-jade.com.mx` | front.novedades-jade.com.mx | 2026-07-18 (31 días) |
| `qa.shop.novedades-jade.com.mx` | qa.shop + qa.backend-imagenes + qa.backend | 2026-07-25 (38 días) |
| `shop.novedades-jade.com.mx` | shop.novedades-jade.com.mx | 2026-07-18 (31 días) |

> ⚠️ Todos vencen en menos de 40 días. Certbot debe renovarlos automáticamente 30 días antes.
> Para verificar que el auto-renew funciona: `sudo certbot renew --dry-run`

---

## BLOQUE 9 — DNS en Hostinger

> Esto se revisa desde el panel de Hostinger, no desde la VPS.

### P29. Dominios y subdominios configurados
> Verificado desde los configs de Nginx (los 6 dominios con SSL activo):

| Subdominio | Apunta a (IP) | Para qué sirve |
|---|---|---|
| `backend.novedades-jade.com.mx` | 51.178.29.99 | Back proyecto-key **PROD** (NodePort 30010) |
| `backend-imagenes.novedades-jade.com.mx` | 51.178.29.99 | Back micro_imagenes **PROD** (NodePort 30096) |
| `shop.novedades-jade.com.mx` | 51.178.29.99 | Front Angular **PROD** (NodePort 30001) |
| `qa.backend.novedades-jade.com.mx` | 51.178.29.99 | Back proyecto-key **QA** (NodePort 31010) |
| `qa.backend-imagenes.novedades-jade.com.mx` | 51.178.29.99 | Back micro_imagenes **QA** (NodePort 31096) |
| `qa.shop.novedades-jade.com.mx` | 51.178.29.99 | Front Angular **QA** (NodePort 31001) |
| `front.novedades-jade.com.mx` | 51.178.29.99 | Alias de shop (cert activo, redirige a 404) |

> Para confirmar o ver todos los registros DNS: Hostinger → DNS Zone Editor → novedades-jade.com.mx
> Los registros tipo A deben todos apuntar a `51.178.29.99` (IPv4) o `2001:41d0:305:2100::f2bd` (IPv6).

---

## BLOQUE 10 — Variables de entorno actuales

> Importante: no guardes las contraseñas reales en este archivo si lo subes a Git.
> Usa `***` para valores sensibles y guárdalos en un lugar seguro aparte (como Bitwarden).

### P30. Variables de entorno de cada deployment
```bash
# Ver las env vars de un pod específico (reemplaza el nombre del pod)
kubectl describe pod NOMBRE_DEL_POD -n NAMESPACE | grep -A 50 "Environment:"
ubuntu@vps-da9a48f5:~$ kubectl describe pod NOMBRE_DEL_POD -n NAMESPACE | grep -A 50 "Environment:"
Error from server (NotFound): namespaces "NAMESPACE" not found
ubuntu@vps-da9a48f5:~$

# O sacar el yml completo del deployment
kubectl get deployment NOMBRE_DEPLOYMENT -n NAMESPACE -o yaml

ubuntu@vps-da9a48f5:~$ kubectl get deployment NOMBRE_DEPLOYMENT -n NAMESPACE -o yaml
Error from server (NotFound): namespaces "NAMESPACE" not found
ubuntu@vps-da9a48f5:~$


```

### Variables que necesitas tener documentadas por app:

**proyecto_key_new (back):**
- DB_HOST
- SPRING_DB_NAME
- SPRING_DATASOURCE_USERNAME
- SPRING_DATASOURCE_PASSWORD
- TOKEN_JWT
- ENDPOINT_IMAGENES
- OPENAI_API_KEY
- ACCESS_TOKE_MERCADO_PAGO
- DEVICE_ID
- SPRING_RABBITMQ_USERNAME
- SPRING_RABBITMQ_PASSWORD
- MAIL_USERNAME ← nuevo (chat)
- MAIL_PASSWORD ← nuevo (chat)
- CHAT_ADMIN_EMAIL ← nuevo (chat)

**micro_imagenes (back):**
- ___ (anotar aquí)

**front Angular:**
- ___ (anotar aquí)

---

## RESUMEN — Checklist de qué tienes que hacer

```
[x] Conectarte a la VPS por SSH
[x] Ejecutar todos los comandos de cada bloque
[x] Pegar las respuestas en este documento
[x] Exportar los YMLs de K8s (deployments, services, ingress)  ← prod_completo.yaml, qa_completo.yaml etc.
[x] Sacar backup de MySQL  ← backup_completo_20260616.sql + por DB
[x] Revisar los DNS en Nginx/Certbot y documentarlos  ← 6 dominios mapeados en Bloque 9
[ ] Confirmar registros DNS en panel Hostinger (verificar que todos apuntan a 51.178.29.99)
[ ] Guardar este documento + los YMLs en un lugar seguro (fuera del repo)
[ ] Configurar renovación automática de SSL: sudo certbot renew --dry-run  (vencen jul 2026)
[ ] Configurar backup automático de MySQL en cron (actualmente solo hay backups manuales)
```

---

## Cómo bajar los archivos de la VPS a tu PC

Una vez que tengas los archivos exportados en la VPS, los bajas así:

```bash
# Desde tu PC (no desde la VPS):
scp usuario@IP_VPS:/ruta/del/archivo ./destino-local/

# Ejemplo — bajar todos los YMLs:
scp root@51.178.29.99:/root/deployments_completos.yaml ./
scp root@51.178.29.99:/root/services_completos.yaml ./
scp root@51.178.29.99:/root/ingress_completos.yaml ./
scp root@51.178.29.99:/root/backup_completo_*.sql ./
```

---

*Documento generado el 2026-06-16. Actualizado con datos reales de la VPS.*

---

## BLOQUE 11 — Contraseñas y secrets (guardar en Bitwarden, NO en Git)

> Este bloque explica QUÉ contraseñas existen, DÓNDE están guardadas actualmente
> y CÓMO extraerlas para tenerlas en texto plano en un lugar seguro.
>
> ⚠️ NO escribas las contraseñas reales en este archivo — este archivo puede subirse a Git.
> Usa `***` aquí y guarda los valores reales en Bitwarden.

---

### ¿Por qué hay que hacer esto?

Los archivos `prod_secrets.yaml` y `qa_secrets.yaml` que exportaste tienen los valores
en **base64** — eso NO es cifrado, es solo codificación. Cualquiera que tenga el archivo
puede decodificarlo con un comando. Por eso hay que saber cuáles son y tenerlas en Bitwarden.

Si mañana cambias de VPS o alguien borra el cluster, necesitas esas contraseñas para
volver a crear los secrets y que las apps arranquen.

---

### Cómo extraer los secrets de K8s en texto plano

Conectarte a la VPS y ejecutar:

```bash
# Secrets de PRODUCCIÓN (namespace default)
kubectl get secret db-secret -n default -o json | \
  python3 -c "
import json,sys,base64
s=json.load(sys.stdin)
for k,v in s['data'].items():
    print(f'{k} = {base64.b64decode(v).decode()}')
"

# Secrets de QA (namespace qa)
kubectl get secret db-secret -n qa -o json | \
  python3 -c "
import json,sys,base64
s=json.load(sys.stdin)
for k,v in s['data'].items():
    print(f'{k} = {base64.b64decode(v).decode()}')
"

# Secrets del namespace futbol
kubectl get secret prioridades-secrets -n futbol -o json | \
  python3 -c "
import json,sys,base64
s=json.load(sys.stdin)
for k,v in s['data'].items():
    print(f'{k} = {base64.b64decode(v).decode()}')
"
```

Copia la salida y pégala en Bitwarden. Luego borra el historial: `history -c`

---

### Lista completa de contraseñas que debes tener en Bitwarden

#### 🔴 K8s Secrets — proyecto_key_new PROD (namespace default → db-secret)
| Variable | Para qué sirve | Valor |
|---|---|---|
| `TOKEN_JWT` | Firma los tokens de sesión | `***` |
| `SPRING_DATASOURCE_USERNAME` | Usuario MySQL prod | `***` |
| `SPRING_DATASOURCE_PASSWORD` | Contraseña MySQL prod | `***` |
| `DB_HOST` | IP/hostname de MySQL | `***` |
| `SPRING_DB_NAME` | Nombre de la base de datos prod | `***` |
| `ENDPOINT_IMAGENES` | URL del microservicio de imágenes | `***` |
| `OPENAI_API_KEY` | API Key de OpenAI (chatbot) | `***` |
| `ACCESS_TOKE_MERCADO_PAGO` | Token de MercadoPago | `***` |
| `DEVICE_ID` | Device ID de MercadoPago | `***` |
| `SPRING_RABBITMQ_USERNAME` | Usuario RabbitMQ | `***` |
| `SPRING_RABBITMQ_PASSWORD` | Contraseña RabbitMQ | `***` |
| `RECONCILIACION_ADMIN_USERNAME` | Usuario admin reconciliación | `***` |
| `RECONCILIACION_ADMIN_PASSWORD` | Contraseña admin reconciliación | `***` |
| `MAIL_USERNAME` | Correo Gmail para chat en vivo | `***` |
| `MAIL_PASSWORD` | App password Gmail | `***` |
| `CHAT_ADMIN_EMAIL` | Email donde llegan avisos del chat | `***` |

#### 🟡 K8s Secrets — proyecto_key_new QA (namespace qa → db-secret)
> Mismas variables que prod pero con valores de QA (distintas contraseñas/DBs)

#### 🟢 MySQL directo en la VPS
| Qué | Valor |
|---|---|
| Usuario root MySQL | root |
| Contraseña root MySQL | `***` |
| Usuario app prod | `***` |
| Contraseña app prod | `***` |
| Usuario app QA | `***` |
| Contraseña app QA | `***` |

> Para ver los usuarios: `mysql -u root -p -e "SELECT user, host FROM mysql.user;"`

#### 🔵 Acceso a la VPS
| Qué | Valor |
|---|---|
| IP VPS (IPv4) | 51.178.29.99 |
| IP VPS (IPv6) | 2001:41d0:305:2100::f2bd |
| Usuario SSH | ubuntu |
| Contraseña SSH o ruta de llave privada | `***` |
| Panel OVHcloud (email) | `***` |

#### 🟣 Servicios externos
| Servicio | Usuario | Contraseña / API Key |
|---|---|---|
| Hostinger (DNS) | `***` | `***` |
| OpenAI | `***` | `***` |
| MercadoPago | `***` | `***` |
| Gmail (app password chat) | `***` | `***` |

---

### Cómo recrear un secret en una VPS nueva

Si cambias de VPS, después de instalar K3s, creas los secrets así:

```bash
kubectl create secret generic db-secret -n default \
  --from-literal=TOKEN_JWT="valor_real" \
  --from-literal=SPRING_DATASOURCE_PASSWORD="valor_real" \
  --from-literal=DB_HOST="valor_real" \
  # ... una línea por cada variable
```

Con eso y los YAMLs del respaldo, las apps arrancan igual que en la VPS actual.

---

## BLOQUE 12 — Usuarios MySQL: quién es quién y por qué

> Este bloque explica qué usuario de MySQL pertenece a qué aplicación,
> qué bases de datos puede usar, y cómo recrearlo en una VPS nueva.

---

### Mapa de usuarios (resultado real de la VPS)

| Usuario | Conecta desde | Base de datos | App que lo usa |
|---|---|---|---|
| `user_ventas` | cualquier IP (`%`) | `inventario_key` | proyecto_key_new **PROD** |
| `user_ventas_qa` | cualquier IP (`%`) | `inventario_key_qa` | proyecto_key_new **QA** |
| `futbol_user` | cualquier IP (`%`) | `futbol_predicciones` | prioridades-backend (futbol) |
| `user_ventas_qa` | cualquier IP (`%`) | `futbol_predicciones` | acceso secundario (solo lectura/escritura básica) |
| `root` | localhost | todas | administración directa en la VPS |

> `nombre_base` y `nombre_base_datos` que aparecen en los grants son bases que ya no existen
> — son permisos huérfanos que quedaron de configuraciones anteriores, no afectan nada.

---

### Por qué cada usuario tiene esos permisos

**`user_ventas`** — usuario de la app en producción
- Tiene permisos completos en `inventario_key` porque la app necesita leer, escribir,
  crear tablas (Hibernate ddl-auto), ejecutar procedures, etc.
- Solo puede conectar desde cualquier IP (`%`) porque los pods de K8s tienen IPs dinámicas.

**`user_ventas_qa`** — usuario de la app en QA
- Igual que `user_ventas` pero sobre `inventario_key_qa`.
- También tiene acceso básico a `futbol_predicciones` (SELECT/INSERT/UPDATE/DELETE)
  — probablemente se le dio acceso en alguna prueba, no es crítico.

**`futbol_user`** — usuario de la app de futbol
- Permisos completos en `futbol_predicciones`.
- App independiente (prioridades-backend en namespace futbol).

---

### Cómo recrear los usuarios en una VPS nueva

```bash
# Conectarte a MySQL como root
mysql -u root -p

# Crear usuario prod
CREATE USER 'user_ventas'@'%' IDENTIFIED BY 'CONTRASEÑA_REAL';
GRANT ALL PRIVILEGES ON inventario_key.* TO 'user_ventas'@'%';

# Crear usuario QA
CREATE USER 'user_ventas_qa'@'%' IDENTIFIED BY 'CONTRASEÑA_REAL';
GRANT ALL PRIVILEGES ON inventario_key_qa.* TO 'user_ventas_qa'@'%';

# Crear usuario futbol
CREATE USER 'futbol_user'@'%' IDENTIFIED BY 'CONTRASEÑA_REAL';
GRANT ALL PRIVILEGES ON futbol_predicciones.* TO 'futbol_user'@'%';

# Aplicar cambios
FLUSH PRIVILEGES;
```

> Reemplaza `CONTRASEÑA_REAL` con el valor que tienes en Bitwarden.
> Las contraseñas las sacaste con el comando de decodificación de secrets de K8s
> (SPRING_DATASOURCE_PASSWORD en prod y qa, y el secret de futbol).

---

### Cómo verificar que quedó bien

```bash
# Ver todos los usuarios
mysql -u root -p -e "SELECT user, host FROM mysql.user;"

# Ver permisos de un usuario específico
mysql -u root -p -e "SHOW GRANTS FOR 'user_ventas'@'%';"
mysql -u root -p -e "SHOW GRANTS FOR 'user_ventas_qa'@'%';"
mysql -u root -p -e "SHOW GRANTS FOR 'futbol_user'@'%';"
```

---

### Cómo restaurar las bases de datos en una VPS nueva

```bash
# 1. Crear las bases vacías
mysql -u root -p -e "CREATE DATABASE inventario_key CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
mysql -u root -p -e "CREATE DATABASE inventario_key_qa CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
mysql -u root -p -e "CREATE DATABASE futbol_predicciones CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

# 2. Restaurar desde los backups
mysql -u root -p inventario_key < backup_inventario_key_20260616.sql
mysql -u root -p inventario_key_qa < backup_inventario_key_qa_20260616.sql
mysql -u root -p < backup_completo_20260616.sql  # restaura todo de una vez

# 3. Crear usuarios y darles permisos (ver sección anterior)
```
