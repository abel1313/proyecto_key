# Rediseño Completo del Módulo de Rifas

---

## 1. Resumen del diseño

- El **admin** gestiona todo. Los participantes no se registran solos.
- Se rifan **variantes** (no productos genéricos) porque tienen stock, talla y color.
- Al agregar una variante a la rifa, se **reserva 1 unidad de su stock**. Al eliminarla, el stock regresa.
- La **palabraClave** identifica a qué variante pertenece cada participante. El sorteo solo usa participantes cuya palabraClave coincide con la variante actual.
- Al terminar una variante, el admin elige cómo continuar: `RESTANTES`, `CERO` o `NUEVOS`.
- La **rifa mensual** importa participantes desde pedidos del mes. Si el comprador no tiene cliente registrado, el admin lo agrega manualmente.
- Todo queda registrado en historial.

---

## 2. Tablas nuevas (DDL desde cero)

### `configurar_rifa_variante`
Reemplaza a `configurar_rifa_producto`. Cada fila es una variante que se rifa dentro de una sesión.

```sql
CREATE TABLE configurar_rifa_variante (
    id              INT PRIMARY KEY AUTO_INCREMENT,
    configurar_rifa_id INT NOT NULL,
    variante_id     INT NOT NULL,
    palabra_clave   VARCHAR(50) NOT NULL,
    giro_ganador    INT NOT NULL DEFAULT 1,
    orden           INT NOT NULL,
    permitir_nuevos TINYINT(1) NOT NULL DEFAULT 0,
    stock_reservado INT NOT NULL DEFAULT 1,
    UNIQUE KEY uq_rifa_palabra (configurar_rifa_id, palabra_clave),
    FOREIGN KEY (configurar_rifa_id) REFERENCES configurar_rifa(id),
    FOREIGN KEY (variante_id)        REFERENCES variantes(id)
);
```

| Campo | Descripción |
|-------|-------------|
| `palabra_clave` | Obligatoria y única dentro de la rifa. Identifica el pool de participantes |
| `giro_ganador` | Giro en el que cae el ganador (los anteriores son descartados) |
| `orden` | Secuencia dentro de la rifa (1, 2, 3…) |
| `permitir_nuevos` | Si se pueden agregar participantes durante este sorteo |
| `stock_reservado` | Unidades tomadas de la variante (por defecto 1) |

---

### `historial_rifa_variante`
Guarda el resumen de cada variante sorteada: quién ganó, cuántos descartados, cómo se continuó.

```sql
CREATE TABLE historial_rifa_variante (
    id                          INT PRIMARY KEY AUTO_INCREMENT,
    configurar_rifa_id          INT NOT NULL,
    configurar_rifa_variante_id INT NOT NULL,
    concursante_ganador_id      INT NULL,
    orden                       INT NOT NULL,
    modo_continuacion           ENUM('RESTANTES', 'CERO', 'NUEVOS') NULL,
    FOREIGN KEY (configurar_rifa_id)          REFERENCES configurar_rifa(id),
    FOREIGN KEY (configurar_rifa_variante_id) REFERENCES configurar_rifa_variante(id),
    FOREIGN KEY (concursante_ganador_id)      REFERENCES concursantes(id)
);
```

---

## 3. Tablas modificadas (ALTER TABLE)

### `concursantes` — agregar `palabra_clave` y `cliente_pedido_id`

```sql
ALTER TABLE concursantes
    ADD COLUMN palabra_clave     VARCHAR(50) NULL,
    ADD COLUMN cliente_pedido_id INT NULL;
```

| Campo | Descripción |
|-------|-------------|
| `palabra_clave` | Liga al concursante con una variante de la rifa |
| `cliente_pedido_id` | ID del cliente en pedidos (si vino de la rifa mensual). Nullable. |

---

### `ganador_rifa` — reemplazar `producto_id` / `configurar_rifa_producto_id` por `configurar_rifa_variante_id`

Si la tabla ya tiene `configurar_rifa_producto_id` por el cambio anterior:
```sql
ALTER TABLE ganador_rifa
    DROP FOREIGN KEY ganador_rifa_ibfk_2,
    DROP COLUMN configurar_rifa_producto_id,
    ADD COLUMN configurar_rifa_variante_id INT NOT NULL,
    ADD FOREIGN KEY (configurar_rifa_variante_id) REFERENCES configurar_rifa_variante(id);
```

Si todavía tiene `producto_id`:
```sql
ALTER TABLE ganador_rifa
    DROP FOREIGN KEY ganador_rifa_ibfk_2,
    DROP COLUMN producto_id,
    ADD COLUMN configurar_rifa_variante_id INT NOT NULL,
    ADD FOREIGN KEY (configurar_rifa_variante_id) REFERENCES configurar_rifa_variante(id);
```

---

## 4. Tablas a eliminar

| Tabla | Razón |
|-------|-------|
| `configurar_rifa_producto` | Reemplazada por `configurar_rifa_variante` |
| `rifas` | Registro libre de clientes. Ya no aplica — todo lo gestiona el admin |

```sql
DROP TABLE IF EXISTS configurar_rifa_producto;
DROP TABLE IF EXISTS rifas;
```

---

## 5. Flujo completo

```
PASO 1 — Crear sesión de rifa
  POST /configurarRifa/save → devuelve id de la sesión

PASO 2 — Buscar variante y agregarla con su palabraClave
  GET /variantes/buscar?termino=bolsa → el admin busca y selecciona
  POST /configurarRifaVariante/save → guarda variante + resta 1 stock
  (repetir para cada variante)
  DELETE /configurarRifaVariante/{id} → elimina y devuelve el stock

PASO 3 — Agregar participantes
  El front pide las palabrasClave disponibles:
    GET /configurarRifaVariante/palabrasClave/{rifaId}
  Agregar uno a uno:
    POST /concursante/registrar (con palabraClave del select)
  O importar desde pedidos del mes:
    GET /pedidos/clientesPorMes?mes=2026-01
    POST /concursante/importarDePedidos

PASO 4 — Girar ruleta
  POST /ganadorRifa/sortear/{rifaId}
  (repetir hasta que descartado=false → hay ganador)

PASO 5 — Elegir cómo continuar
  POST /ganadorRifa/continuarVariante/{rifaId}?modo=RESTANTES|CERO|NUEVOS

PASO 6 — Repetir PASO 4-5 para cada variante

PASO 7 — Ver historial final
  GET /ganadorRifa/estado/{rifaId} con rifaTerminada=true
```

---

## 6. Endpoints

### 6.0 Búsqueda de variantes para la rifa

El admin busca variantes por **código de barras del producto**. El endpoint ya existe:

```
GET /variantes/buscar?termino={codigoBarras}&pagina=1&size=10
```

**Response** (el front recibe por cada variante):
```json
{
  "code": 200,
  "data": {
    "pagina": 1,
    "totalPaginas": 1,
    "totalRegistros": 2,
    "t": [
      {
        "id": 42,
        "talla": "M",
        "color": "Negro",
        "stock": 8,
        "marca": "sin marca",
        "codigoBarras": "Su2287",
        "nombreProducto": "Jeans Short Brillo",
        "precio": 250.0,
        "imagenBase64": "base64string..."
      },
      {
        "id": 43,
        "talla": "L",
        "color": "Negro",
        "stock": 3,
        "codigoBarras": "Su2287",
        "nombreProducto": "Jeans Short Brillo",
        "precio": 250.0,
        "imagenBase64": "base64string..."
      }
    ]
  }
}
```

El front muestra la lista de variantes encontradas → el admin selecciona una → asigna una `palabraClave` → confirma.

> **Cambio en código**: se agregó `nombreProducto` a `VarianteResumenDto` y a `buildBaseResumenDto()` en `VarianteServiceImpl`.

---

### 6.1 Configuración de variantes

#### `POST /configurarRifaVariante/save`
Agrega una variante a la rifa y deduce 1 del stock.

**Request:**
```json
{
  "configurarRifaId": 5,
  "varianteId": 42,
  "palabraClave": "BOLSA",
  "giroGanador": 2,
  "orden": 1,
  "permitirNuevos": false
}
```
**Response:**
```json
{
  "code": 200,
  "data": {
    "id": 10,
    "variante": { "id": 42, "talla": "M", "color": "Negro", "stock": 7, "marca": "..." },
    "palabraClave": "BOLSA",
    "giroGanador": 2,
    "orden": 1,
    "permitirNuevos": false,
    "stockReservado": 1
  }
}
```

#### `GET /configurarRifaVariante/porRifa/{rifaId}`
Lista todas las variantes configuradas para la rifa.

**Response:**
```json
{
  "code": 200,
  "data": [
    {
      "id": 10,
      "variante": { "id": 42, "talla": "M", "color": "Negro", "stock": 7 },
      "palabraClave": "BOLSA",
      "giroGanador": 2,
      "orden": 1,
      "permitirNuevos": false
    }
  ]
}
```

#### `GET /configurarRifaVariante/palabrasClave/{rifaId}`
Devuelve solo las palabrasClave para poblar el select al agregar participantes.

**Response:**
```json
{
  "code": 200,
  "data": ["BOLSA", "PANTALON", "FALDA"]
}
```

#### `DELETE /configurarRifaVariante/{id}`
Elimina la variante de la rifa y regresa el stock.

**Response:**
```json
{ "code": 200, "data": "Variante eliminada y stock restaurado" }
```

#### `PUT /configurarRifaVariante/{id}/palabraClave`
Cambia la palabraClave de una variante (solo si no hay participantes con esa palabra aún).

**Request:**
```json
{ "palabraClave": "NUEVA_PALABRA" }
```

---

### 6.2 Participantes

#### `POST /concursante/registrar` (modificado)
Agrega `palabraClave` y `clientePedidoId` al request existente.

**Request:**
```json
{
  "nombre": "Juan",
  "apellidoPaterno": "García",
  "telefono": "5551234567",
  "palabraClave": "BOLSA",
  "ordenDesde": 1,
  "clientePedidoId": null,
  "configurarRifa": { "id": 5 }
}
```

#### `GET /pedidos/clientesPorMes?mes=2026-01`
Clientes únicos con pedidos en el mes indicado (formato YYYY-MM).

**Response:**
```json
{
  "code": 200,
  "data": [
    { "clientePedidoId": 101, "nombre": "María López", "apellido": "López", "telefono": "5559876543" },
    { "clientePedidoId": 102, "nombre": "Carlos Ruiz", "apellido": "Ruiz",  "telefono": "5551112222" },
    { "clientePedidoId": null, "nombre": "Sin nombre", "apellido": "", "telefono": "" }
  ]
}
```
> Si el pedido no tiene cliente registrado, `clientePedidoId = null`. El admin puede editar el nombre antes de importar.

#### `POST /concursante/importarDePedidos`
Importa masivamente clientes del mes como participantes.

**Request:**
```json
{
  "configurarRifaId": 5,
  "palabraClave": "BOLSA",
  "ordenDesde": 1,
  "clientes": [
    { "clientePedidoId": 101, "nombre": "María López", "apellido": "López", "telefono": "5559876543" },
    { "clientePedidoId": null, "nombre": "Pedro Sin Registro", "apellido": "", "telefono": "" }
  ]
}
```
**Response:**
```json
{
  "code": 200,
  "data": {
    "importados": 2,
    "concursantes": [
      { "id": 201, "nombre": "María López", "palabraClave": "BOLSA" },
      { "id": 202, "nombre": "Pedro Sin Registro", "palabraClave": "BOLSA" }
    ]
  }
}
```

---

### 6.3 Sorteo

#### `POST /ganadorRifa/sortear/{rifaId}`
Sin parámetros. El back calcula la variante actual y el giro.

**Response descartado:**
```json
{
  "code": 200,
  "data": {
    "descartado": true,
    "concursante": { "id": 205, "nombre": "Pedro Sánchez", "palabraClave": "BOLSA" },
    "varianteActual": { "id": 10, "palabraClave": "BOLSA", "orden": 1 },
    "rifaTerminada": false
  }
}
```
**Response ganador:**
```json
{
  "code": 200,
  "data": {
    "descartado": false,
    "concursante": { "id": 201, "nombre": "María López", "palabraClave": "BOLSA" },
    "varianteActual": { "id": 10, "palabraClave": "BOLSA", "orden": 1,
                        "variante": { "id": 42, "talla": "M", "color": "Negro" } },
    "rifaTerminada": false
  }
}
```

#### `POST /ganadorRifa/continuarVariante/{rifaId}?modo=RESTANTES`
Se llama cuando hay ganador y el admin elige cómo continuar hacia la siguiente variante.

| Modo | Qué hace |
|------|----------|
| `RESTANTES` | Los concursantes no eliminados de la variante actual pasan a la siguiente (su palabraClave se actualiza) |
| `CERO` | Todos los concursantes de la rifa resetean (`descartado=false`) y pasan a la siguiente variante |
| `NUEVOS` | Solo los nuevos registros van a la siguiente variante. Los anteriores no participan |

**Response:** mismo formato que `GET /ganadorRifa/estado/{rifaId}` con `varianteActual` apuntando ya a la siguiente.

#### `GET /ganadorRifa/estado/{rifaId}`
**Response:**
```json
{
  "code": 200,
  "data": {
    "configurarRifa": { "id": 5, "activa": true, "fechaHoraLimite": "2026-05-10T20:00:00" },
    "varianteActual": {
      "id": 10,
      "palabraClave": "BOLSA",
      "giroGanador": 2,
      "orden": 1,
      "totalVariantes": 3,
      "varianteNumeroActual": 1,
      "variante": { "id": 42, "talla": "M", "color": "Negro" }
    },
    "giroActual": 1,
    "elegibles": [
      { "id": 201, "nombre": "María López", "palabraClave": "BOLSA", "telefono": "5559876543" }
    ],
    "descartados": [],
    "historial": [],
    "rifaTerminada": false
  }
}
```

Cuando `rifaTerminada = true`:
```json
"historial": [
  {
    "orden": 1,
    "palabraClave": "BOLSA",
    "variante": { "id": 42, "talla": "M", "color": "Negro" },
    "ganador": { "id": 201, "nombre": "María López" },
    "descartados": [ { "id": 205, "nombre": "Pedro Sánchez" } ],
    "modoContinuacion": "RESTANTES"
  }
]
```

#### `GET /configurarRifa/activas` y `/activas/hoy` (modificar response)
Agregar campos de progreso:
```json
{
  "id": 5,
  "fechaHoraLimite": "2026-05-10T20:00:00",
  "activa": true,
  "totalVariantes": 3,
  "variantesSorteadas": 1
}
```

---

## 7. Cambios en código Java

### Entidades a crear
- `ConfigurarRifaVariante.java`
- `HistorialRifaVariante.java`

### Entidades a modificar
- `ConfigurarRifa.java` → quitar `@OneToMany ConfigurarRifaProducto`, agregar `@OneToMany ConfigurarRifaVariante`
- `Concursante.java` → agregar `palabraClave`, `clientePedidoId`
- `GanadorRifa.java` → cambiar `configurarRifaProducto` por `configurarRifaVariante`

### Entidades a eliminar
- `ConfigurarRifaProducto.java`
- `Rifa.java`

### Repositorios a crear
- `IConfigurarRifaVarianteRepository.java`
- `IHistorialRifaVarianteRepository.java`

### Repositorios a eliminar
- `IConfigurarRifaProductoRepository.java`
- `IRifaRepository.java`

### Repositorios a modificar
- `IGanadorRifaRepository.java` → queries apuntan a `configurarRifaVariante`
- `IConcursanteRepository.java` → agregar query por `palabraClave`
- `IPedidoRepository.java` → agregar `clientesPorMes`

### Servicios a crear
- `ConfigurarRifaVarianteService.java` → save (con descuento de stock), delete (con devolución de stock), listar, palabrasClave
- Dentro de `ConcursanteServiceImpl` → agregar `importarDePedidos`

### Servicios a modificar
- `GanadorRifaServiceImpl.java` → reescribir `sortear` para filtrar por `palabraClave`, agregar `continuarVariante`
- `ConfiguracionRifaServiceImpl.java` → agregar `totalVariantes` y `variantesSorteadas` al listado

### Servicios a eliminar
- `RifaServiceImpl.java`
- `ConfiguracionRifaServiceImpl.save()` ya no valida producto (lo quitamos antes)

### Controllers a crear
- `ConfigurarRifaVarianteController.java`

### Controllers a eliminar
- `RifaControllerImpl.java`

### DTOs a crear/modificar
- `SorteoEstadoDto.java` → reemplazar `productoActual` por `varianteActual` con imagen y stock
- `ImportarDePedidosRequest.java`
- `ClientePedidoDto.java`
- `ConfigurarRifaVarianteDto.java` (response con variante completa)
- `ContinuarVarianteRequest.java` (solo el modo)

---

## 8. Tablas en uso final

| Tabla | Estado |
|-------|--------|
| `configurar_rifa` | ✅ Se mantiene |
| `configurar_rifa_variante` | 🆕 Nueva |
| `concursantes` | ✅ Modificada |
| `ganador_rifa` | ✅ Modificada |
| `historial_rifa_variante` | 🆕 Nueva |
| `configurar_rifa_producto` | ❌ Eliminar |
| `rifas` | ❌ Eliminar |