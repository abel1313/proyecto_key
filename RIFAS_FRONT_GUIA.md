# Guía de Rifas para el Front

Base URL: `{host}/mis-productos`
Todos los endpoints requieren `Authorization: Bearer {jwt}` con rol ADMIN.

---

## Qué cambió vs la versión anterior

| Antes | Ahora |
|-------|-------|
| Una rifa = un producto | Una rifa = N productos en orden |
| `POST /sortear` recibía `vueltaActual` y `totalVueltas` | `POST /sortear` no recibe parámetros — el back calcula todo |
| El front controlaba en qué vuelta estaba | El back sabe solo el producto actual y el giro actual |
| No había historial por producto | `GET /estado` devuelve historial completo de descartados y ganadores |
| No había rifas del día | `GET /configurarRifa/activas/hoy` |

---

## Flujo completo

```
1. CREAR la sesión de rifa
2. AGREGAR productos a la rifa (con su orden y giro ganador)
3. AGREGAR participantes
4. GIRAR la ruleta (repetir hasta terminar todos los productos)
5. VER estado / historial
```

---

## PASO 1 — Crear la sesión de rifa

```
POST /configurarRifa/save
Body:
{
  "fechaHoraLimite": "2026-05-10T23:59:00",
  "activa": true,
  "productos": []
}
```

Response: devuelve el `ConfigurarRifa` con su `id`. Guardar ese `id` para los siguientes pasos.

---

## PASO 2 — Agregar productos a la rifa

Por cada producto que quieras rifar, hacer una llamada:

```
POST /save  (AbstractController de ConfigurarRifaProducto)
```

**Nota:** Hay que exponer un endpoint específico o usar el CRUD genérico.
Por ahora el endpoint recomendado es llamar `save` del CRUD con:

```json
{
  "configurarRifa": { "id": 5 },
  "producto":       { "id": 270 },
  "orden":          1,
  "giroGanador":    3,
  "permitirNuevos": false
}
```

| Campo | Qué significa |
|-------|---------------|
| `configurarRifa.id` | ID de la sesión creada en paso 1 |
| `producto.id` | ID del producto que se rifa |
| `orden` | Posición en la secuencia (1 = primero, 2 = segundo...) |
| `giroGanador` | En qué giro cae el ganador. Si es 3: giro 1 y 2 son descartados, giro 3 es el ganador |
| `permitirNuevos` | Si se pueden agregar participantes nuevos para este producto |

**Ejemplo de sesión con 3 productos:**
```
Producto A → orden=1, giroGanador=3  (2 descartados, 1 ganador)
Producto B → orden=2, giroGanador=2  (1 descartado,  1 ganador)
Producto C → orden=3, giroGanador=1  (directo al ganador)
```

---

## PASO 3 — Agregar participantes

```
POST /concursante/registrar
Body:
{
  "nombre": "Juan",
  "apellidoPaterno": "Pérez",
  "telefono": "5551234567",
  "palabraRifa": "JADE001",
  "ordenDesde": 1,
  "configurarRifa": { "id": 5 }
}
```

| Campo | Qué significa |
|-------|---------------|
| `ordenDesde` | Desde qué producto participa. `1` = desde el inicio. `2` = solo a partir del producto 2 en adelante |
| `configurarRifa.id` | ID de la sesión |

**Para agregar participantes nuevos en medio de la rifa** (ej. para el producto 3):
- Usar `ordenDesde: 3` y el back los incluye automáticamente cuando llegue al producto 3

---

## PASO 4 — Girar la ruleta

```
POST /ganadorRifa/sortear/{configurarRifaId}
```

Sin body, sin parámetros. El back sabe solo:
- Qué producto se está rifando ahora
- En qué giro va dentro de ese producto
- Si es descartado o ganador

**Response cuando es DESCARTADO:**
```json
{
  "code": 200,
  "data": {
    "id": null,
    "concursante": { "id": 12, "nombre": "Juan", "apellidoPaterno": "Pérez" },
    "configurarRifaProducto": { "id": 3, "orden": 1, "giroGanador": 3, "producto": {...} },
    "descartado": true
  }
}
```

**Response cuando es GANADOR:**
```json
{
  "code": 200,
  "data": {
    "id": 7,
    "concursante": { "id": 8, "nombre": "María", "apellidoPaterno": "López" },
    "configurarRifaProducto": { "id": 3, "orden": 1, "giroGanador": 3, "producto": { "id": 270, "nombre": "Great Jeans" } },
    "descartado": false
  }
}
```

**Leer `descartado`:**
- `true` → animación de descarte, mostrar quién salió
- `false` → animación de ganador, mostrar quién ganó y qué producto

**Errors posibles:**
```json
{ "code": 400, "mensaje": "No hay concursantes elegibles para el producto 2" }
{ "code": 400, "mensaje": "Todos los productos ya fueron rifados" }
{ "code": 400, "mensaje": "Esta rifa ya fue completada o está inactiva" }
```

---

## PASO 5 — Ver estado actual

```
GET /ganadorRifa/estado/{configurarRifaId}
```

Response:
```json
{
  "code": 200,
  "data": {
    "configurarRifa": { "id": 5, "activa": true, "fechaHoraLimite": "2026-05-10T23:59:00" },
    "totalConcursantes": 10,

    "productoActual": {
      "id": 3, "orden": 1, "giroGanador": 3,
      "producto": { "id": 270, "nombre": "Great Jeans" },
      "permitirNuevos": false
    },
    "giroActual": 2,
    "giroGanador": 3,

    "elegibles": [ { "id": 8, "nombre": "María"}, ... ],
    "descartados": [ { "id": 12, "nombre": "Juan"}, ... ],

    "historial": [
      { "concursante": {"nombre":"Juan"}, "configurarRifaProducto": {"orden":1}, "descartado": true },
      { "concursante": {"nombre":"Pedro"}, "configurarRifaProducto": {"orden":1}, "descartado": true }
    ],

    "rifaTerminada": false
  }
}
```

| Campo | Para qué sirve en el front |
|-------|---------------------------|
| `productoActual` | Mostrar qué producto se está rifando ahora |
| `giroActual` | Mostrar "Giro 2 de 3" |
| `giroGanador` | Saber cuántos giros faltan para el ganador |
| `elegibles` | Lista de participantes en la ruleta |
| `descartados` | Lista de eliminados |
| `historial` | Historial completo por producto |
| `rifaTerminada` | Si es `true`, deshabilitar el botón de girar y mostrar resumen |

---

## Otros endpoints útiles

### Rifas activas del día
```
GET /configurarRifa/activas/hoy
```
Devuelve solo las rifas cuyo `fechaHoraLimite` es hoy y están activas.

### Todas las rifas activas
```
GET /configurarRifa/activas
```

### Reiniciar una rifa
```
POST /ganadorRifa/reiniciar/{configurarRifaId}?completo=false
```
- `completo=false` → conserva participantes, reinicia todo el sorteo
- `completo=true` → borra participantes también, empieza desde cero

### Ver participantes de una rifa
```
GET /concursante/porRifa/{configurarRifaId}
```

### Ver solo los elegibles
```
GET /concursante/elegibles/{configurarRifaId}
```

---

## Ejemplo de flujo visual en el front

```
[Pantalla de configuración]
  → Crear sesión de rifa (fecha límite)
  → Agregar producto 1: "Great Jeans" | giro ganador: 3
  → Agregar producto 2: "Bolsa roja"  | giro ganador: 2
  → Agregar participantes (10 nombres)

[Pantalla de la ruleta]
  → Muestra: "Producto actual: Great Jeans | Giro 1 de 3"
  → Botón: GIRAR →
      descartado=true  → animación descarte → "Juan eliminado"
  → "Giro 2 de 3" → GIRAR →
      descartado=true  → "Pedro eliminado"
  → "Giro 3 de 3 — ¡ESTE ES EL GANADOR!" → GIRAR →
      descartado=false → animación confetti → "¡María gana Great Jeans!"

  → Automáticamente pasa a: "Producto actual: Bolsa roja | Giro 1 de 2"
  → GIRAR → descartado=true → ...
  → GIRAR → descartado=false → ¡GANADOR!

  → rifaTerminada=true → mostrar resumen completo
```

---

## Cómo saber si el siguiente giro es el ganador

Para mostrar en el front "¡El siguiente giro es el ganador!" usa el estado:

```
giroActual === giroGanador → el PRÓXIMO giro define al ganador
```

Por ejemplo: `giroActual=2`, `giroGanador=3` → quedan `3-2=1` giros para el ganador.