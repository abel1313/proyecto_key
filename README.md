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

---

# Módulo de Gastos de Viaje (compras)

## Por qué se creó

Antes, la tabla `gastos_surtir` solo guardaba un texto y un precio suelto, sin ninguna relación entre los gastos. No había forma de saber cuánto costó **en total** una salida a comprar mercancía, ni separar por tipo de gasto (transporte, comida, flete, etc.).

Este módulo agrega el concepto de **viaje de compra**: un viaje agrupa todos los gastos que se hicieron en esa salida (taxi, comida, flete, hospedaje) y permite ver el costo total de ese viaje.

---

## Qué hace cada parte

### Tabla `viaje_compra` (nueva)
Representa una salida a comprar mercancía. Es el "encabezado" del viaje.
Ejemplo: "Compras CDMX enero 2026 — Tepito".

Cada viaje puede tener muchos gastos asociados.

### Tabla `gastos_surtir` (modificada)
Ya existía pero solo tenía descripción y precio, sin agrupación.
Se le agregan tres columnas:
- `tipo` → clasifica el gasto (TRANSPORTE, COMIDA, FLETE, HOSPEDAJE, OTRO)
- `fecha` → cuándo ocurrió ese gasto puntual
- `viaje_id` → a qué viaje pertenece (FK a `viaje_compra`)

De esta forma cada gasto queda "pegado" a su viaje y se puede sumar el total.

---

## Scripts SQL

### DDL — Crear tabla `viaje_compra`

```sql
CREATE TABLE viaje_compra (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    descripcion  VARCHAR(150) NOT NULL,
    destino      VARCHAR(100),
    fecha_inicio DATE         NOT NULL,
    fecha_fin    DATE,
    notas        TEXT
);
```

**Explicación columna por columna:**
- `descripcion` → nombre del viaje, ej: "Compras Tepito enero"
- `destino` → lugar a donde se fue, ej: "CDMX / Tepito"
- `fecha_inicio` → día que saliste
- `fecha_fin` → día que regresaste (puede quedar null si es viaje de un día)
- `notas` → cualquier observación libre del viaje

---

### DDL — Modificar tabla `gastos_surtir`

```sql
-- Agregar tipo de gasto
ALTER TABLE gastos_surtir
    ADD COLUMN tipo  VARCHAR(20) NOT NULL DEFAULT 'OTRO'
                     COMMENT 'TRANSPORTE | COMIDA | FLETE | HOSPEDAJE | OTRO';

-- Agregar fecha del gasto
ALTER TABLE gastos_surtir
    ADD COLUMN fecha DATE;

-- Agregar relación al viaje (permite NULL para gastos que no son de viaje)
ALTER TABLE gastos_surtir
    ADD COLUMN viaje_id INT,
    ADD CONSTRAINT fk_gasto_viaje
        FOREIGN KEY (viaje_id) REFERENCES viaje_compra(id)
        ON DELETE SET NULL;
```

**Por qué `viaje_id` permite NULL:**
Los gastos que ya existían en la tabla no pertenecen a ningún viaje. Permitir NULL evita que los datos actuales rompan y deja la posibilidad de guardar gastos generales que no sean de un viaje específico.

**Por qué `ON DELETE SET NULL`:**
Si algún día se elimina un viaje, los gastos no se borran — quedan con `viaje_id = NULL`. Así no perdés el historial de gastos.

---

### DML — Datos de ejemplo para probar

```sql
-- Crear un viaje de prueba
INSERT INTO viaje_compra (descripcion, destino, fecha_inicio, fecha_fin, notas)
VALUES ('Compras Tepito enero', 'CDMX - Tepito', '2026-01-15', '2026-01-16',
        'Primera compra del año');

-- Agregar gastos a ese viaje (id=1)
INSERT INTO gastos_surtir (descripcion_gasto, precio_gasto, tipo, fecha, viaje_id)
VALUES
    ('Taxi al aeropuerto',  180.00, 'TRANSPORTE', '2026-01-15', 1),
    ('Vuelo CDMX ida',     1200.00, 'TRANSPORTE', '2026-01-15', 1),
    ('Comida día 1',        250.00, 'COMIDA',      '2026-01-15', 1),
    ('Hospedaje 1 noche',   600.00, 'HOSPEDAJE',   '2026-01-15', 1),
    ('Comida día 2',        180.00, 'COMIDA',      '2026-01-16', 1),
    ('Flete mercancía',    1500.00, 'FLETE',        '2026-01-16', 1),
    ('Vuelo regreso',      1100.00, 'TRANSPORTE', '2026-01-16', 1);

-- Ver el total del viaje
SELECT
    v.descripcion,
    v.destino,
    v.fecha_inicio,
    v.fecha_fin,
    COUNT(g.id)        AS num_gastos,
    SUM(g.precio_gasto) AS total_gastado
FROM viaje_compra v
LEFT JOIN gastos_surtir g ON g.viaje_id = v.id
GROUP BY v.id;

-- Ver el desglose por tipo de gasto de un viaje
SELECT
    g.tipo,
    COUNT(*)            AS cantidad,
    SUM(g.precio_gasto) AS subtotal
FROM gastos_surtir g
WHERE g.viaje_id = 1
GROUP BY g.tipo
ORDER BY subtotal DESC;
```

---

## Clases Java a crear/modificar

| Clase | Archivo | Cambio |
|---|---|---|
| `Viaje` | entity/Viaje.java | **Nueva** — mapea `viaje_compra`, tiene lista de gastos |
| `Gastos` | entity/Gastos.java | Agregar campos `tipo`, `fecha`, relación ManyToOne a `Viaje` |
| `TipoGasto` | entity/TipoGasto.java | **Nuevo** — enum con valores: TRANSPORTE, COMIDA, FLETE, HOSPEDAJE, OTRO |
| `IViajeRepository` | repository/IViajeRepository.java | **Nuevo** — queries para buscar viajes con total |
| `ViajeService` | service/ViajeService.java | **Nuevo** — crear viaje, agregar gasto, obtener resumen |
| `ViajeController` | controller/ViajeController.java | **Nuevo** — endpoints REST |
| `GastosRequest` | dto/GastosRequest.java | Agregar campos `tipo`, `fecha`, `viajeId` |

---

## Endpoints que tendrá el módulo

```
POST   /viajes                     → crear un nuevo viaje
GET    /viajes                     → listar viajes con su total (paginado)
GET    /viajes/{id}                → ver un viaje con todos sus gastos y total
PUT    /viajes/{id}                → editar datos del viaje
DELETE /viajes/{id}                → eliminar viaje (los gastos quedan con viaje_id=null)

POST   /gastos/save                → ya existe, se actualizará para aceptar viajeId y tipo
GET    /gastos/getGastos           → ya existe
GET    /gastos/porViaje/{viajeId}  → nuevo: listar gastos de un viaje específico
```

---

## Orden de ejecución

1. Ejecutar el DDL de `viaje_compra` (crear tabla nueva)
2. Ejecutar el DDL de `ALTER TABLE gastos_surtir` (agregar columnas)
3. Verificar con las queries DML de ejemplo
4. Implementar las clases Java

