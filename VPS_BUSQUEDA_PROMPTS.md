# Prompts para pedirle a Claude Chat que busque una VPS

> Copia y pega estos prompts en claude.ai cuando quieras buscar o migrar VPS.
> Cada prompt ya tiene el contexto necesario para que Claude entienda tu setup.

---

## PROMPT 1 — Buscar VPS barata que soporte mi proyecto

```
primer
```

---

## PROMPT 2 — Migrar todo a una VPS nueva (cuando ya tienes la nueva VPS)

```
Necesito migrar mi aplicación de una VPS vieja a una nueva. Ya tengo todos los respaldos.

**Lo que tengo respaldado:**
- prod_completo.yaml — todo el namespace de producción de K8s
- qa_completo.yaml — todo el namespace QA de K8s
- futbol_completo.yaml — namespace futbol de K8s
- backup_completo_20260616.sql — dump completo de MySQL
- nginx_sites.tar.gz — configs de Nginx (6 virtual hosts con SSL)
- mysql_users_grants.sql — usuarios y permisos de MySQL
- Los secrets de K8s los tengo en Bitwarden en texto plano

**Stack de la VPS nueva (Ubuntu 22.04 o 24.04 LTS):**
Necesito instalar en orden:
1. MySQL
2. K3s
3. Nginx + Certbot

**Después restaurar:**
1. Bases de datos desde el dump SQL
2. Usuarios de MySQL
3. Aplicar los YAMLs de K8s
4. Recrear los secrets con las contraseñas reales
5. Configurar Nginx con los 6 virtual hosts
6. Renovar certificados SSL con Certbot

Dame los comandos exactos en orden para hacer todo esto paso a paso en Ubuntu.
```

---

## PROMPT 3 — Separar prod y QA en dos VPS distintas

```
Actualmente tengo prod y QA en la misma VPS usando namespaces de K3s.
Quiero separarlo en 2 VPS distintas: una solo para prod y otra solo para QA.

**Setup actual (1 VPS):**
- namespace default = producción
- namespace qa = QA
- MySQL en la misma VPS con inventario_key (prod) e inventario_key_qa (QA)
- Nginx con 6 dominios: 3 de prod y 3 de QA

**Lo que quiero:**
- VPS 1: solo producción (default namespace + inventario_key + 3 dominios prod)
- VPS 2: solo QA (qa namespace + inventario_key_qa + 3 dominios QA)

Tengo respaldos completos de todo (YAMLs, SQL dumps, configs de Nginx).

¿Qué pasos debo seguir para hacer esta separación sin perder datos ni tener downtime en producción?
Explícame el orden correcto y qué archivos usar de cada respaldo.
```

---

## PROMPT 4 — Configurar correo Gmail para notificaciones del chat en vivo

```
Tengo una app Spring Boot con un módulo de chat en vivo que envía emails 
cuando un visitante inicia una sesión de chat y el admin no está conectado.

La config en application.yml ya está lista:
spring:
  mail:
    host: smtp.gmail.com
    port: 587
    username: ${MAIL_USERNAME:}
    password: ${MAIL_PASSWORD:}
    properties:
      mail.smtp.auth: true
      mail.smtp.starttls.enable: true

La app está en K8s (K3s). Necesito:
1. Cómo generar un App Password en Gmail (no la contraseña normal)
2. Cómo agregar las variables MAIL_USERNAME y MAIL_PASSWORD al secret de K8s
3. Cómo verificar que el email funciona desde los logs de Spring Boot
```

---

## PROMPT 5 — Agregar backup automático de MySQL en cron

```
Tengo MySQL instalado directo en una VPS Ubuntu como servicio systemd.
Actualmente solo tengo backups manuales.

Bases de datos a respaldar:
- inventario_key (producción)
- inventario_key_qa (QA)
- futbol_predicciones

Necesito configurar un cron job que:
1. Haga dump de cada base de datos una vez al día (de madrugada)
2. Guarde los archivos con fecha en el nombre (backup_inventario_key_20260616.sql)
3. Borre backups de más de 7 días para no llenar el disco
4. Idealmente mande un email o log si el backup falla

Dame los comandos exactos para configurar esto en Ubuntu con crontab.
```
