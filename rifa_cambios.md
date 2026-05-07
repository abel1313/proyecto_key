## 8. Aclaración final — participantes desde pedidos

- **No se importa desde clientes directamente**, sino desde el **módulo de pedidos**.
- Flujo: el admin filtra pedidos de un mes (ej. enero) → aparecen los clientes únicos que compraron ese mes → los selecciona → se registran como participantes de la rifa.
- Si un cliente hizo una compra pero no está registrado en el sistema, el admin puede agregarlo manualmente con el formulario de la rifa (no en el módulo de clientes).
- Los datos del participante que vienen de un pedido: `nombreCliente`, `numeroTelefonico` (de `IClienteQuery`). Apellido y correo si están disponibles.

---

## 9. Contrato completo front ↔ backend — lo que hay que crear

> Este es el contrato que el front necesita. El backend debe implementar exactamente estas formas de request y response para que el front funcione sin cambios.

---

### 9.1 Tablas nuevas / campos nuevos que necesita el backend

#### Tabla: `configurar_rifa_variante` (reemplaza o extiende `configurar_rifa_producto`)

| Campo | Tipo | Descripción |
|---|---|---|
| `id` | Long | PK autoincremental |
| `configurar_rifa_id` | Long FK | Rifa a la que pertenece |
| `variante_id` | Long FK | Variante que se rifa |
| `palabraClave` | String | **Obligatoria.** Identifica a qué variante pertenece cada participante |
| `giroGanador` | Int | En qué número de giro sale el ganador (mínimo 1) |
| `orden` | Int | Posición dentro de la rifa (1, 2, 3…) |
| `permitirNuevos` | Boolean | Si se pueden agregar participantes nuevos durante este sorteo |

#### Tabla: `concursante` — campos nuevos/modificados

| Campo | Tipo | Descripción |
|---|---|---|
| `palabraClave` | String | Palabra con la que se registró → liga al participante a una variante |
| `ordenDesde` | Int | Ya existe. Desde qué posición de variante puede participar |
| `clientePedidoId` | Long nullable | ID del cliente en el módulo de pedidos (si vino de ahí) |

#### Tabla: `historial_rifa_variante` (nueva, para el resumen final)

| Campo | Tipo | Descripción |
|---|---|---|
| `id` | Long | PK |
| `configurar_rifa_id` | Long FK | Rifa |
| `variante_id` | Long FK | Variante sorteada |
| `concursante_ganador_id` | Long FK | Quién ganó |
| `descartados` | JSON / relación | Lista de concursantes descartados en esa variante |
| `orden` | Int | En qué posición de la rifa fue sorteada |

---

### 9.2 Endpoints — configuración de variantes en la rifa

#### `POST /configurarRifaVariante/save`
**Front envía:**
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
**Front espera recibir:**
```json
{
  "data": {
    "id": 10,
    "variante": {
      "id": 42,
      "nombre": "Bolsa tipo shopping",
      "stock": 8,
      "codigoBarras": "1234567890",
      "imagenBase64": "base64string..."
    },
    "palabraClave": "BOLSA",
    "giroGanador": 2,
    "orden": 1,
    "permitirNuevos": false
  }
}
```

#### `GET /configurarRifaVariante/porRifa/{rifaId}`
**Front espera recibir:**
```json
{
  "data": [
    {
      "id": 10,
      "variante": { "id": 42, "nombre": "Bolsa tipo shopping", "stock": 8, "codigoBarras": "1234567890", "imagenBase64": "..." },
      "palabraClave": "BOLSA",
      "giroGanador": 2,
      "orden": 1,
      "permitirNuevos": false
    },
    {
      "id": 11,
      "variante": { "id": 55, "nombre": "Pantalón negro S", "stock": 3, "codigoBarras": "9876543210", "imagenBase64": "..." },
      "palabraClave": "PANTALON",
      "giroGanador": 3,
      "orden": 2,
      "permitirNuevos": true
    }
  ]
}
```

#### `DELETE /configurarRifaVariante/{id}`
**Front espera:** `{ "data": "eliminado" }` o status 200.

#### `PUT /configurarRifaVariante/{id}/palabraClave`
**Front envía:**
```json
{ "palabraClave": "NUEVA_PALABRA" }
```
**Front espera:** `{ "data": { "id": 10, "palabraClave": "NUEVA_PALABRA" } }`

---

### 9.3 Endpoints — participantes

#### `POST /concursante/registrar` (ya existe, añadir campos)
**Front envía:**
```json
{
  "nombre": "Juan",
  "apellidoPaterno": "García",
  "telefono": "5551234567",
  "palabraClave": "BOLSA",
  "configurarRifa": { "id": 5 },
  "ordenDesde": 1,
  "clientePedidoId": null
}
```
**Front espera:**
```json
{
  "data": {
    "id": 201,
    "nombre": "Juan",
    "apellidoPaterno": "García",
    "telefono": "5551234567",
    "palabraClave": "BOLSA",
    "ordenDesde": 1
  }
}
```

#### `GET /pedidos/clientesPorMes?mes=2025-01` — NUEVO
El front usa el mes seleccionado por el admin para traer clientes únicos de ese mes.
**Front espera:**
```json
{
  "data": [
    { "id": 101, "nombre": "María López", "apellido": "López", "telefono": "5559876543" },
    { "id": 102, "nombre": "Carlos Ruiz", "apellido": "Ruiz",  "telefono": "5551112222" }
  ]
}
```

#### `POST /concursante/importarDePedidos` — NUEVO
El admin selecciona clientes del mes y los importa masivamente.
**Front envía:**
```json
{
  "configurarRifaId": 5,
  "palabraClave": "BOLSA",
  "ordenDesde": 1,
  "clientes": [
    { "clientePedidoId": 101, "nombre": "María López", "apellido": "López", "telefono": "5559876543" },
    { "clientePedidoId": 102, "nombre": "Carlos Ruiz", "apellido": "Ruiz",  "telefono": "5551112222" }
  ]
}
```
**Front espera:**
```json
{
  "data": {
    "importados": 2,
    "concursantes": [
      { "id": 201, "nombre": "María López", "palabraClave": "BOLSA" },
      { "id": 202, "nombre": "Carlos Ruiz", "palabraClave": "BOLSA" }
    ]
  }
}
```

---

### 9.4 Endpoints — sorteo (modificaciones a los existentes)

#### `GET /ganadorRifa/estado/{rifaId}` — MODIFICAR RESPONSE
**Front espera ahora:**
```json
{
  "data": {
    "configurarRifa": { "id": 5, "fechaHoraLimite": "2026-02-01T20:00:00" },
    "varianteActual": {
      "id": 10,
      "palabraClave": "BOLSA",
      "giroGanador": 2,
      "orden": 1,
      "permitirNuevos": false,
      "variante": { "id": 42, "nombre": "Bolsa tipo shopping", "stock": 8, "codigoBarras": "1234567890", "imagenBase64": "..." }
    },
    "giroActual": 1,
    "totalVariantes": 3,
    "varianteNumeroActual": 1,
    "elegibles": [
      { "id": 201, "nombre": "Juan García", "palabraClave": "BOLSA", "telefono": "5551234567" }
    ],
    "descartados": [],
    "ganador": null,
    "rifaTerminada": false,
    "historial": []
  }
}
```

Cuando `rifaTerminada = true`, el `historial` viene con datos:
```json
"historial": [
  {
    "orden": 1,
    "variante": { "id": 42, "nombre": "Bolsa tipo shopping" },
    "ganador": { "id": 201, "nombre": "Juan García", "telefono": "5551234567" },
    "descartados": [
      { "id": 205, "nombre": "Pedro Sánchez" }
    ]
  }
]
```

#### `POST /ganadorRifa/sortear/{rifaId}` — MODIFICAR RESPONSE
Ya no lleva params. Response:
```json
{
  "data": {
    "descartado": true,
    "concursante": { "id": 205, "nombre": "Pedro Sánchez", "palabraClave": "BOLSA" },
    "variante": { "id": 42, "nombre": "Bolsa tipo shopping" },
    "rifaTerminada": false
  }
}
```

#### `POST /ganadorRifa/continuarVariante/{rifaId}?modo=RESTANTES` — NUEVO
Se llama en la pantalla de transición cuando el admin elige cómo continuar.
`modo` puede ser:
- `RESTANTES` — pool = elegibles que sobran (sin descartados ni ganador)
- `CERO` — pool = todos los participantes originales regresan
- `NUEVOS` — solo abre la puerta a agregar nuevos; los restantes siguen igual

**Front envía:** solo el query param `?modo=RESTANTES` (sin body).
**Front espera:** mismo formato que `getEstado`, con `varianteActual` ya apuntando a la siguiente variante.

---

### 9.5 Endpoints — listado de rifas (modificar response)

#### `GET /configurarRifa/activas` y `GET /configurarRifa/activas/hoy`
**Front espera añadir estos campos a cada rifa:**
```json
{
  "data": [
    {
      "id": 5,
      "fechaHoraLimite": "2026-02-01T20:00:00",
      "activa": true,
      "totalVariantes": 3,
      "variantesSorteadas": 1
    }
  ]
}
```

---

### 9.6 Resumen — qué hay que crear vs qué ya existe

| Endpoint | Estado |
|---|---|
| `POST /configurarRifa/save` | ✅ Existe |
| `GET /configurarRifa/activas` | ✅ Existe — agregar `totalVariantes` y `variantesSorteadas` |
| `GET /configurarRifa/activas/hoy` | ✅ Existe — mismo cambio |
| `POST /configurarRifaVariante/save` | 🆕 Nuevo (o renombrar `configurarRifaProducto`) |
| `GET /configurarRifaVariante/porRifa/{id}` | 🆕 Nuevo |
| `DELETE /configurarRifaVariante/{id}` | 🆕 Nuevo |
| `PUT /configurarRifaVariante/{id}/palabraClave` | 🆕 Nuevo |
| `POST /concursante/registrar` | ✅ Existe — agregar `palabraClave` y `clientePedidoId` |
| `GET /concursante/porRifa/{rifaId}` | ✅ Existe |
| `GET /concursante/elegibles/{rifaId}` | ✅ Existe |
| `POST /concursante/importarDePedidos` | 🆕 Nuevo |
| `GET /pedidos/clientesPorMes?mes=YYYY-MM` | 🆕 Nuevo |
| `POST /ganadorRifa/sortear/{rifaId}` | ✅ Existe — quitar params, modificar response |
| `GET /ganadorRifa/estado/{rifaId}` | ✅ Existe — ampliar response |
| `POST /ganadorRifa/continuarVariante/{id}?modo=` | 🆕 Nuevo |
| `POST /ganadorRifa/reiniciar/{id}?completo=` | ✅ Existe |
