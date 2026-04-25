server.servlet.context-path=/mis-productos
server.port=8081
spring.datasource.url=jdbc:mysql://167.88.164.132:3306/inventario_key
spring.datasource.username=noveda11_trece
spring.datasource.password=Luvianos*13

spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver


spring.jpa.show-sql=true
logging.level.org.hibernate.SQL=DEBUG
logging.level.org.hibernate.orm.jdbc.bind=TRACE






Recapitulación completa del montaje con Cloudflare Tunnel y Docker
Me alegra que haya quedado. Aquí va la secuencia completa —clara, ordenada y replicable— de lo que hiciste para que tu proyecto local funcione “como si fuera un servidor” detrás de Cloudflare.

Visión general del objetivo
Objetivo: Exponer tu backend local (Docker) a Internet usando un Cloudflare Tunnel con tu dominio api.novedades-jade.com.mx.

Modelo de tráfico: Cliente → Cloudflare (DNS + red) → Tunnel (cloudflared) → backend:8081.

Resultado: Tu servicio local responde bajo HTTPS con tu subdominio público, sin abrir puertos en tu red.

Prerrequisitos
Dominio en Cloudflare: DNS administrado por Cloudflare.

Docker y Docker Compose: Instalados en la máquina local.

Aplicación backend: Ejecutándose en el puerto 8081 dentro de Docker.

Cloudflared token: Generado al crear el túnel (modo conector remoto).

Pasos que seguiste
1. Creación del túnel en Cloudflare
Entraste al Dashboard: Zero Trust → Access → Tunnels.

Creaste un túnel: Nombre descriptivo, por ejemplo, “ventas-jade-docker”.

Elegiste el entorno: Seleccionaste “Docker” (conector remoto).

Obtuviste el token: Cloudflare te mostró un comando con un token largo (no .json).

Ese token autoriza al conector a correr el túnel sin archivo de credenciales.

2. Definición de la ruta publicada (Published Application Route)
Hostname público: Subdominio api en el dominio novedades-jade.com.mx.

Tipo de servicio: HTTP.

Destino interno (URL): http://backend:8081.

Aquí “backend” es el nombre del servicio/host de Docker Compose.

3. Configuración de cloudflared (sin credenciales .json)
Elegiste el flujo con token: Decidiste correr el conector directamente con el token, sin archivo .json.

Config.yml mínimo (ingress): Opcional, pero alineado con la ruta:

api.novedades-jade.com.mx → http://backend:8081.

Fallback http_status:404.

4. Docker Compose final
Servicios definidos:

Backend: Corre en 8081.

Nginx (opcional): Proxy en 80 si lo necesitas.

Cloudflared: Ejecuta el túnel con el token.

Comando clave en cloudflared:

command: tunnel --no-autoupdate run --token <TU_TOKEN>

Levantaste los contenedores: docker-compose up -d.

Verificaste logs: docker logs -f cloudflared mostró conexiones a mex01, dfw06, y la “ingress” aplicada.

5. Verificación de servicio
Prueba HTTP: Navegaste a https://api.novedades-jade.com.mx/....

Cabeceras de Cloudflare: Opcionalmente comprobaste Server: cloudflare / CF-Ray.

Logs del backend: Viste tus log.info(...) por cada petición en docker logs -f backend.

Cómo replicarlo en otra PC
1. Preparar entorno:

Instalar Docker y Docker Compose.

Clonar tu proyecto y ajustar variables si es necesario.

2. Reutilizar o crear túnel:

Opción A (reusar túnel): Ve al túnel existente en el Dashboard y genera un nuevo token de conector para esa PC.

Cada máquina que correrá un conector debe tener su propio token (seguro).

Opción B (crear túnel nuevo): Repite la creación con nombre nuevo y guarda el token.

3. Configurar Publish Route:

Mantener el mismo hostname: api.novedades-jade.com.mx apuntando a http://backend:8081.

Si usas múltiples máquinas, decide si solo una sirve tráfico o si vas a balancear.

4. Docker Compose en la nueva máquina:

Pegar el token en el servicio cloudflared:

command: tunnel --no-autoupdate run --token <TOKEN_DE_ESA_PC>

Asegurar nombre del servicio backend: Coincida con backend si usas esa URL interna.

5. Levantar y verificar:

docker-compose up -d.

docker logs -f cloudflared y docker logs -f backend.

Probar https://api.novedades-jade.com.mx.

Notas útiles y solución de problemas
Tokens vs .json: El flujo que usaste es con token (conector remoto). No necesitas .json.

Tunnel HEALTHY pero sin respuestas: Revisa que el backend esté arriba, el puerto correcto, y que la “ingress” apunte a http://backend:8081.

DNS en Cloudflare: El subdominio debe existir y estar proxied (naranja) si lo manejas por DNS tradicional; con Tunnel, la ruta publicada hace el enlace.

Logs detallados del back: Usa docker logs -f backend. Cloudflared no imprime cada request por defecto.

TLS/HTTPS: Cloudflare sirve HTTPS al cliente; tu backend puede seguir en HTTP interno (http://backend:8081).

Si quieres, te preparo un paquete “lista de archivos” (docker-compose.yml y config.yml mínimos) para que lo reutilices tal cual en otra máquina, con un espacio marcado para pegar el token.






docker-compose up -d	Levanta todos los servicios definidos en tu docker-compose.yml en segundo plano.	Cada vez que quieras iniciar tu proyecto.
docker-compose down	Detiene y elimina los contenedores, redes y volúmenes creados por Compose.	Cuando quieras apagar todo limpio.
docker-compose restart	Reinicia los servicios.	Si cambiaste configuración y quieres que se recargue.
docker-compose logs -f <servicio>	Muestra los logs en tiempo real de un servicio (ej. backend, cloudflared).	Para ver qué está pasando en tu app o túnel.
docker-compose ps	Lista los contenedores activos de tu proyecto.	Para confirmar que todo está corriendo.
docker-compose build	Reconstruye las imágenes según tu Dockerfile.	Cuando cambias código o dependencias.
🔹 Comandos de Docker (individuales)
Comando	Qué hace	Ejemplo
docker ps	Lista todos los contenedores activos.	Ver qué está corriendo.
docker logs -f <nombre>	Muestra logs de un contenedor específico.	docker logs -f backend
docker exec -it <nombre> bash	Entra dentro de un contenedor con una terminal interactiva.	docker exec -it backend bash
docker stop <nombre>	Detiene un contenedor.	docker stop backend
docker rm <nombre>	Elimina un contenedor detenido.	docker rm backend
docker images	Lista las imágenes disponibles en tu máquina.	Ver qué imágenes tienes.
docker rmi <imagen>	Elimina una imagen.	Limpiar espacio.




Flujo típico de trabajo
Levantar proyecto:

bash
docker-compose up -d
Verificar contenedores activos:

bash
docker-compose ps
Ver logs del backend:

bash
docker-compose logs -f backend
Ver logs del túnel:

bash
docker-compose logs -f cloudflared
Apagar todo:

bash
docker-compose down




---

# Sistema de Roles y Permisos

## Por qué se cambió

El sistema anterior solo tenía dos roles (`ROLE_ADMIN` y `ROLE_USUARIO`) y no permitía dar permisos específicos a usuarios sin cambiarles el rol completo.

El nuevo sistema separa dos conceptos:
- **Rol**: paquete base de permisos según el puesto (empleado, cajero, etc.)
- **Permisos extra**: permisos individuales que se le agregan a un usuario puntual sin cambiarle el rol

Ejemplo: Juan es cajero pero necesita ver clientes → en vez de subirlo a empleado (y darle todo lo de empleado), le agregás solo el permiso `CLIENTES_LEER`.

---

## Tablas involucradas en la base de datos

### `roles` (ya existía)
Guarda los roles disponibles en el sistema.

| columna | tipo | descripción |
|---|---|---|
| id | INT PK | identificador |
| nombre_rol | VARCHAR | nombre del rol (ej. ROLE_ADMIN) |

Roles actuales: `ROLE_ADMIN`, `ROLE_USUARIO`, `ROLE_EMPLEADO`, `ROLE_CAJERO`

---

### `permisos` (nueva)
Guarda cada permiso granular del sistema. Un permiso representa exactamente una acción sobre un recurso.

| columna | tipo | descripción |
|---|---|---|
| id | INT PK | identificador |
| nombre_permiso | VARCHAR | nombre del permiso (ej. PRODUCTOS_LEER) |

---

### `rol_permiso` (nueva)
Relaciona qué permisos tiene cada rol. Si mañana querés que el cajero también pueda ver clientes, agregás una fila aquí.

| columna | tipo | descripción |
|---|---|---|
| rol_id | INT FK → roles | el rol |
| permiso_id | INT FK → permisos | el permiso que tiene ese rol |

---

### `usuario_permiso` (nueva)
Permisos individuales asignados a un usuario específico, por encima de lo que ya le da su rol.

| columna | tipo | descripción |
|---|---|---|
| usuario_id | INT FK → usuario_modificacion | el usuario |
| permiso_id | INT FK → permisos | el permiso extra |

---

### `usuario_modificacion` (ya existía, actualizada)
Se le agregó la relación con `usuario_permiso`. Antes solo tenía un rol, ahora también tiene permisos extra opcionales.

---

## Roles y sus permisos

### ROLE_ADMIN
Tiene todos los permisos sin excepción. Dueño del negocio.

### ROLE_EMPLEADO (vendedor)
`PRODUCTOS_LEER` `PRODUCTOS_CREAR` `PRODUCTOS_EDITAR`
`VARIANTES_LEER` `VARIANTES_CREAR` `VARIANTES_EDITAR`
`PEDIDOS_LEER` `PEDIDOS_CREAR` `PEDIDOS_EDITAR` `PEDIDOS_ELIMINAR`
`VENTAS_LEER` `VENTAS_CREAR`
`CLIENTES_LEER` `CLIENTES_CREAR` `CLIENTES_EDITAR`
`MP_COBRAR` `IMAGENES_GESTIONAR` `PAGOS_LEER`

### ROLE_CAJERO
`PRODUCTOS_LEER` `PEDIDOS_LEER` `MP_COBRAR` `PAGOS_LEER`

### ROLE_USUARIO (cliente de la tienda)
`PRODUCTOS_LEER` `PEDIDOS_LEER` `PEDIDOS_CREAR`

---

## Lista completa de permisos

| Permiso | Qué protege |
|---|---|
| `PRODUCTOS_LEER` | GET /productos/** |
| `PRODUCTOS_CREAR` | POST /productos/** |
| `PRODUCTOS_EDITAR` | PUT /productos/** |
| `PRODUCTOS_ELIMINAR` | DELETE /productos/** |
| `VARIANTES_LEER` | GET /variantes/** |
| `VARIANTES_CREAR` | POST /variantes/** |
| `VARIANTES_EDITAR` | PUT /variantes/** |
| `PEDIDOS_LEER` | GET /pedidos/** |
| `PEDIDOS_CREAR` | POST /pedidos/** |
| `PEDIDOS_EDITAR` | PUT /pedidos/** |
| `PEDIDOS_ELIMINAR` | DELETE /pedidos/** |
| `VENTAS_LEER` | GET /ventas/** |
| `VENTAS_CREAR` | POST /ventas/** |
| `CLIENTES_LEER` | GET /clientes/** |
| `CLIENTES_CREAR` | POST /clientes/** |
| `CLIENTES_EDITAR` | PUT /clientes/** |
| `CLIENTES_ELIMINAR` | DELETE /clientes/** |
| `MP_COBRAR` | /mp/** |
| `GASTOS_GESTIONAR` | /gastos/** |
| `RIFAS_GESTIONAR` | /rifa/** /ganadorRifa/** /configurarRifa/** /concursante/** |
| `USUARIOS_GESTIONAR` | /usuarios/** |
| `IMAGENES_GESTIONAR` | POST/PUT/DELETE /imagen/** |
| `PAGOS_LEER` | GET /pagos/** |

---

## Endpoints para gestionar roles y permisos (solo ROLE_ADMIN)

```
GET  /usuarios/roles                          → ver todos los roles con sus IDs
GET  /usuarios/permisos                       → ver todos los permisos con sus IDs
GET  /usuarios/getAllPage?buscar=&page=1&size=10  → ver usuarios con su rol y permisos extra

PUT  /usuarios/{usuarioId}/rol/{rolId}        → cambiar el rol de un usuario
POST /usuarios/{usuarioId}/permisos/{permisoId}  → agregar permiso extra a un usuario
DEL  /usuarios/{usuarioId}/permisos/{permisoId}  → quitar permiso extra de un usuario
```

### Flujo para dar de alta un empleado nuevo

1. El empleado se registra en `/auth/registrar` → recibe `ROLE_USUARIO` automáticamente
2. El admin consulta `GET /usuarios/roles` para ver el ID de `ROLE_EMPLEADO`
3. El admin cambia el rol: `PUT /usuarios/{id}/rol/{idRolEmpleado}`
4. Si ese empleado necesita un permiso puntual extra: `POST /usuarios/{id}/permisos/{idPermiso}`

---

## Clases Java modificadas o creadas

| Clase | Archivo | Cambio |
|---|---|---|
| `Permiso` | entity/Permiso.java | **Nueva** — entidad que mapea la tabla `permisos` |
| `Roles` | entity/Roles.java | Agregada relación ManyToMany con `Permiso` (tabla `rol_permiso`) |
| `Usuario` | entity/Usuario.java | Agregado `permisosExtra` (tabla `usuario_permiso`). `getAuthorities()` ahora devuelve el rol + permisos del rol + permisos extra |
| `IPermisoRepository` | repository/IPermisoRepository.java | **Nuevo** — acceso a la tabla `permisos` |
| `UsuarioServiceImpl` | service/UsuarioServiceImpl.java | Métodos nuevos: `cambiarRol`, `agregarPermisoExtra`, `quitarPermisoExtra`, `listarRoles`, `listarPermisos` |
| `UsuarioController` | controller/UsuarioController.java | Endpoints nuevos para gestionar roles y permisos |
| `SecurityConfig` | security/SecurityConfig.java | Reemplazado `hasRole("ADMIN")` por `hasAuthority("PERMISO_X")` en todas las reglas |
| `RegistroService` | service/RegistroService.java | Simplificado — siempre asigna `ROLE_USUARIO` por defecto al registrarse |
| `UserDto` | mapper/UserDto.java | Ahora expone `rol` (String) y `permisosExtra` (Set&lt;String&gt;) |

---

## Script SQL de migración

Ubicación: `src/main/resources/static/querys.sql`

Contiene:
1. Creación de tablas `permisos`, `rol_permiso`, `usuario_permiso`
2. Inserción de todos los permisos
3. Inserción de roles `ROLE_EMPLEADO` y `ROLE_CAJERO`
4. Asignación de permisos a cada rol

Se ejecuta **una sola vez** sobre la base de datos existente. No toca datos de usuarios ni el resto de tablas.

