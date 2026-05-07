# Sistema de Roles y Permisos

## Cómo funciona

Cada usuario tiene **un rol** (su puesto base) y opcionalmente **permisos extra** individuales.

- El **rol** define el paquete de permisos estándar para ese puesto.
- Los **permisos extra** permiten darle a un usuario específico acceso a algo puntual sin cambiarle el rol.

Ejemplo: Juan es cajero (ROLE_CAJERO) pero también necesita ver clientes.
En vez de subirlo a empleado (y darle todo lo de empleado), le agregás solo el permiso `CLIENTES_LEER`.

---

## Roles disponibles

| Rol | Para quién |
|---|---|
| `ROLE_ADMIN` | Dueño — acceso total |
| `ROLE_EMPLEADO` | Vendedor general |
| `ROLE_CAJERO` | Solo cobro con terminal |
| `ROLE_USUARIO` | Cliente de la tienda online |

---

## Permisos por rol

### ROLE_ADMIN
Tiene **todos** los permisos del sistema.

### ROLE_EMPLEADO
`PRODUCTOS_LEER` `PRODUCTOS_CREAR` `PRODUCTOS_EDITAR`
`VARIANTES_LEER` `VARIANTES_CREAR` `VARIANTES_EDITAR`
`PEDIDOS_LEER` `PEDIDOS_CREAR` `PEDIDOS_EDITAR` `PEDIDOS_ELIMINAR`
`VENTAS_LEER` `VENTAS_CREAR`
`CLIENTES_LEER` `CLIENTES_CREAR` `CLIENTES_EDITAR`
`MP_COBRAR`
`IMAGENES_GESTIONAR`
`PAGOS_LEER`

### ROLE_CAJERO
`PRODUCTOS_LEER` `PEDIDOS_LEER` `MP_COBRAR` `PAGOS_LEER`

### ROLE_USUARIO (cliente)
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
| `MP_COBRAR` | /mp/** (cobros con terminal) |
| `GASTOS_GESTIONAR` | /gastos/** |
| `RIFAS_GESTIONAR` | /rifa/** /ganadorRifa/** /configurarRifa/** /concursante/** |
| `USUARIOS_GESTIONAR` | /usuarios/** (ver, editar, cambiar roles) |
| `IMAGENES_GESTIONAR` | POST/PUT/DELETE /imagen/** |
| `PAGOS_LEER` | GET /pagos/** |

---

## Tablas en la base de datos

```
roles                    permisos
──────────────           ─────────────────────
id  nombre_rol           id  nombre_permiso
1   ROLE_ADMIN           1   PRODUCTOS_LEER
2   ROLE_USUARIO         2   PRODUCTOS_CREAR
3   ROLE_EMPLEADO        ...
4   ROLE_CAJERO

rol_permiso              usuario_permiso
──────────────           ──────────────────────
rol_id  permiso_id       usuario_id  permiso_id
1       1                5           14   ← Juan cajero puede ver clientes
1       2
...
```

---

## Endpoints de gestión (solo ROLE_ADMIN)

### Ver roles y permisos disponibles
```
GET /usuarios/roles        → lista todos los roles con sus IDs
GET /usuarios/permisos     → lista todos los permisos con sus IDs
```

### Ver usuarios
```
GET /usuarios/getAllPage?buscar=&page=1&size=10
```
Responde con: id, username, email, rol, permisosExtra[], enabled

### Cambiar el rol de un usuario
```
PUT /usuarios/{usuarioId}/rol/{rolId}
```
Ejemplo: cambiar a Juan (id=5) a empleado (rol id=3)
```
PUT /usuarios/5/rol/3
```

### Agregar un permiso extra a un usuario
```
POST /usuarios/{usuarioId}/permisos/{permisoId}
```
Ejemplo: darle a Juan (id=5) permiso CLIENTES_LEER (id=14)
```
POST /usuarios/5/permisos/14
```

### Quitar un permiso extra
```
DELETE /usuarios/{usuarioId}/permisos/{permisoId}
```

---

## Flujo típico para un empleado nuevo

1. El empleado se registra en `/auth/registrar` → recibe `ROLE_USUARIO` por defecto
2. El admin consulta `GET /usuarios/roles` para ver los IDs de roles
3. El admin cambia el rol: `PUT /usuarios/{id}/rol/{rolId}` usando el id de `ROLE_EMPLEADO`
4. Si ese empleado necesita algo extra: `POST /usuarios/{id}/permisos/{permisoId}`

---

## Clases Java involucradas

| Clase | Archivo | Qué hace |
|---|---|---|
| `Permiso` | entity/Permiso.java | Entidad de permiso, tabla `permisos` |
| `Roles` | entity/Roles.java | Entidad de rol con sus permisos, tabla `roles` |
| `Usuario` | entity/Usuario.java | Usuario con rol + permisosExtra, genera authorities para JWT |
| `IPermisoRepository` | repository/IPermisoRepository.java | Acceso a tabla permisos |
| `UsuarioServiceImpl` | service/UsuarioServiceImpl.java | cambiarRol, agregarPermisoExtra, quitarPermisoExtra |
| `UsuarioController` | controller/UsuarioController.java | Endpoints REST de gestión |
| `SecurityConfig` | security/SecurityConfig.java | Reglas de acceso por permiso |
| `RegistroService` | service/RegistroService.java | Registro → asigna ROLE_USUARIO por defecto |

---

## Script SQL de migración

Está en: `src/main/resources/static/querys.sql`

Crea las tablas `permisos`, `rol_permiso`, `usuario_permiso` e inserta todos los datos.