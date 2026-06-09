# Reporte de Documentación Swagger — Proyecto Key New

**Fecha:** 2026-05-19

---

## Estado antes de la documentación

**Cero anotaciones OpenAPI en todo el proyecto** — ningún controller, ningún DTO tenía `@Tag`, `@Operation`, `@ApiResponse`, `@Parameter` ni `@Schema`. No existía clase de configuración OpenAPI.

---

## Acceso al Swagger UI

| Entorno | URL |
|---------|-----|
| Local / Dev | `http://localhost:9091/mis-productos/swagger-ui.html` |
| Local / Dev — API Docs JSON | `http://localhost:9091/mis-productos/v3/api-docs` |

> Swagger solo está habilitado en los perfiles `dev`. En producción (docker) debe deshabilitarse (ver SEG-013 en REPORTE_SEGURIDAD.md).

Para usar los endpoints protegidos desde Swagger UI:
1. Llamar `POST /auth/login` con usuario y contraseña
2. Copiar el `accessToken` de la respuesta
3. Hacer clic en el botón **Authorize** (candado) en la parte superior del Swagger UI
4. Pegar el token — Swagger lo enviará automáticamente en el header `Authorization: Bearer <token>`

---

## Cambios Realizados

### Archivos Creados

| Archivo | Descripción |
|---------|-------------|
| `config/OpenApiConfig.java` | Configuración global: título, descripción, versión y esquema de seguridad JWT Bearer |

### Controllers Documentados

#### AbstractController
Clase genérica de CRUD — documentados 5 métodos:

| Método HTTP | Ruta | Descripción |
|-------------|------|-------------|
| DELETE | `/delete` | Eliminar registro |
| GET | `/getAll` | Listar registros paginados |
| GET | `/getOne/{tipoDato}` | Buscar por identificador |
| POST | `/save` | Crear nuevo registro |
| PUT | `/update/{tipoDato}` | Actualizar registro existente |

#### AdminController — `@Tag: Administracion`

| Método HTTP | Ruta | Descripción |
|-------------|------|-------------|
| GET | `/admin/test-rabbit` | Prueba de RabbitMQ (temporal) |
| DELETE | `/admin/cache` | Limpiar cache Redis |

#### AuthController — `@Tag: Autenticacion`

| Método HTTP | Ruta | Descripción |
|-------------|------|-------------|
| POST | `/auth/login` | Iniciar sesion con rate limiting |
| POST | `/auth/refresh` | Renovar access token desde cookie |
| POST | `/auth/logout` | Cerrar sesion |
| POST | `/auth/registrar` | Registrar nuevo usuario |
| GET | `/auth/validar` | Validar token JWT |

#### ChatbotController — `@Tag: Chatbot`

| Método HTTP | Ruta | Descripción |
|-------------|------|-------------|
| POST | `/chatbot/mensaje` | Enviar mensaje al asistente virtual |

#### ClienteControllerImpl — `@Tag: Clientes`
Hereda 5 endpoints de AbstractController, más:

| Método HTTP | Ruta | Descripción |
|-------------|------|-------------|
| POST | `/clientes/save` | Crear o actualizar cliente (override) |
| GET | `/clientes/buscarPorIdCliente/{idCliente}` | Buscar cliente por ID |
| GET | `/clientes/buscar` | Buscar clientes por nombre (paginado) |

#### ProductosControllerImpl y VarianteController
Estos controllers tienen muchos endpoints y requieren que el agente de Swagger complete sus anotaciones (pendiente — ver sección Pendiente).

---

## Pendiente de Completar

Los siguientes controllers tienen gran cantidad de endpoints y deben documentarse en una segunda pasada:

### ProductosControllerImpl
Endpoints a documentar con `@Tag(name = "Productos")`:
- `GET /productos/obtenerProductos` — publico
- `GET /productos/buscarNombreOrCodigoBarra` — publico
- `POST /productos/save`
- `PUT /productos/update`
- `GET /productos/findById/{id}`
- `DELETE /productos/deleteBy/{id}`
- `GET /productos/admin/diagnostico-imagenes/{productoId}`
- Y más endpoints de admin (habilitarDeshabilitar, sin stock, reporte Excel, etc.)

### VarianteController
Endpoints a documentar con `@Tag(name = "Variantes")`:
- `GET /variantes/buscar` — publico
- `GET /variantes/porProducto/{productoId}`
- `POST /variantes/guardarConImagenes`
- `POST /variantes/inicializarDesdeProducto`
- `GET /variantes/imagenes/{varianteId}`
- `DELETE /variantes/{varianteId}/imagenes`
- `GET /variantes/admin/diagnostico-imagenes/{varianteId}`
- Y más endpoints de paginacion y resumen

### DTOs con @Schema
Los siguientes DTOs deben recibir anotaciones `@Schema` en sus campos:
- `ProductoDTO` — nombre, precioVenta, precioCosto, codigoBarras, habilitado, stock
- `VarianteDto` / `VarianteDetalle` — talla, color, stock, precio
- `AuthResponse` — accessToken
- `PginaDto` — pagina, totalPaginas, totalRegistros

---

## Configuracion de springdoc en application.yml

```yaml
# Perfil dev (ya configurado correctamente)
springdoc:
  swagger-ui:
    enabled: true
    path: /swagger-ui.html
  api-docs:
    enabled: true
    path: /v3/api-docs

# Perfil docker/prod (AGREGAR — ver SEG-013)
springdoc:
  swagger-ui:
    enabled: false
  api-docs:
    enabled: false
```