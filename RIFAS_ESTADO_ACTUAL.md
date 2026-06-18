# Módulo de Rifas — Análisis del estado actual del código (2026-06-11)

> Este documento describe **lo que hay implementado hoy en el código**, no un plan. Sirve de panorama
> base antes de pedir cambios nuevos. Los documentos `RIFAS_FRONT_CONTRATO.md`, `RIFAS_FRONT_GUIA.md`,
> `RIFAS_REDISENO_COMPLETO.md` y `rifa_cambios.md` describían planes de rediseño — la mayor parte de
> ese rediseño **ya está implementado**, salvo lo que se marca abajo como pendiente o legacy.

---

## 1. Visión general del flujo

- El **admin** gestiona todo (no hay registro libre de clientes).
- Una **sesión de rifa** (`ConfigurarRifa`) agrupa **N variantes** a rifar (`ConfigurarRifaVariante`),
  cada una con: `palabraClave` (única dentro de la rifa), `giroGanador`, `orden`, `permitirNuevos`.
- Al agregar una variante a la rifa se **descuenta 1 del stock** de esa variante; al eliminarla se devuelve.
- Los **participantes** (`Concursante`) se registran con una `palabraClave` que los liga a una variante.
- Cada participante tiene `boletos` (papeletas ponderadas) — ver sección 5, **funcionalidad nueva no
  documentada para el front**.
- El **sorteo** (`POST /ganadorRifa/sortear/{id}`) elige un concursante elegible al azar (ponderado por
  `boletos`) de la variante actual. Si el giro alcanza `giroGanador` → es ganador; si no → descartado.
- Al haber ganador, el admin decide cómo pasan los participantes a la siguiente variante
  (`continuarVariante` con modo `RESTANTES` / `CERO` / `NUEVOS`), y queda un registro en
  `HistorialRifaVariante`.
- Cada giro se emite por WebSocket a `/topic/ruleta`.

---

## 2. Entidades reales (paquete `entity`)

### `ConfigurarRifa` → tabla `configurar_rifa`
| Campo | Tipo | Notas |
|---|---|---|
| id | Integer | PK |
| fechaHoraLimite | LocalDateTime | plazo para registrar participantes |
| activa | Boolean | se pone en `false` cuando se sortea la última variante |
| variantes | `List<ConfigurarRifaVariante>` | `@OneToMany`, cascade ALL + orphanRemoval, ordenadas por `orden` |

### `ConfigurarRifaVariante` → tabla `configurar_rifa_variante`
| Campo | Tipo | Notas |
|---|---|---|
| id | Integer | PK |
| configurarRifa | FK | rifa a la que pertenece |
| variante | FK → `Variantes` | variante de producto que se rifa |
| palabraClave | String(50) | **única por rifa**, se guarda en MAYÚSCULAS y `trim()` |
| giroGanador | int | giro en el que sale el ganador (default 1) |
| orden | int | posición dentro de la rifa |
| permitirNuevos | boolean | si se aceptan nuevos participantes durante este sorteo |
| stockReservado | int | unidades descontadas del stock (default 1) |

### `Concursante` → tabla `concursantes`
| Campo | Tipo | Notas |
|---|---|---|
| id | Integer | PK |
| nombre | String | requerido |
| apellidoPaterno | String | |
| telefono | String | |
| descartado | boolean | true = ya salió eliminado en algún giro |
| ordenDesde | int | desde qué variante (orden) participa, default 1 |
| palabraClave | String(50) | liga al concursante con la variante actual |
| clientePedidoId | Integer (nullable) | id del cliente real si vino del módulo de pedidos |
| **boletosBase** | int | compras del mes (sin ponderar), default 1 |
| **boletos** | int | papeletas ponderadas por score de cumplimiento, default 1 |
| configurarRifa | FK | rifa a la que pertenece |

### `GanadorRifa` → tabla `ganador_rifa`
Cada fila = **un giro** de la ruleta.
| Campo | Notas |
|---|---|
| concursante | FK |
| configurarRifaVariante | FK |
| descartado | `true` = eliminado en ese giro, `false` = ganador de esa variante |

### `HistorialRifaVariante` → tabla `historial_rifa_variante`
Se crea **una fila cada vez que se llama `continuarVariante`** (resumen de la variante recién cerrada).
| Campo | Notas |
|---|---|
| configurarRifa | FK |
| configurarRifaVariante | variante que se cerró |
| concursanteGanador | FK nullable |
| orden | orden de esa variante |
| modoContinuacion | enum `RESTANTES` / `CERO` / `NUEVOS` |

---

## 3. Endpoints actuales (todos bajo `/v1/...`, rol **ADMIN**, ver `SecurityConfig.java:117-121`)

### `/v1/configurarRifa` — `ConfigurarRifaControllerImpl`
CRUD genérico (`AbstractController`): `POST /save`, `PUT /update/{id}`, `GET /getOne/{id}`, `GET /getAll?page=&size=`, `DELETE /delete`.

| Endpoint | Método | Devuelve |
|---|---|---|
| `/activas` | GET | `List<ConfigurarRifaResumenDto>` (id, fechaHoraLimite, activa, **totalVariantes**, **variantesSorteadas**) |
| `/activas/hoy` | GET | igual, filtrado a `fechaHoraLimite` de hoy |

⚠️ El `POST /save` genérico acepta el body completo de `ConfigurarRifa`, incluyendo `variantes` (cascade ALL).
Si se usa así para crear variantes **no se descuenta stock** — el flujo correcto para variantes es
`/configurarRifaVariante/save` (sección siguiente).

### `/v1/configurarRifaVariante` — `ConfigurarRifaVarianteController`
| Endpoint | Método | Qué hace |
|---|---|---|
| `/save` | POST | Agrega variante a la rifa. Valida: rifa activa, `palabraClave` única (case-insensitive, se normaliza a MAYÚSCULAS), variante con stock ≥ 1. **Descuenta 1 del stock.** |
| `/porRifa/{rifaId}` | GET | Lista `ConfigurarRifaVarianteDto[]` (incluye `VarianteResumenDto` con imagen base64, stock, precio, nombreProducto, codigoBarras) |
| `/palabrasClave/{rifaId}` | GET | `List<String>` — para poblar el select del front |
| `/{id}` | DELETE | Elimina la variante de la rifa y **devuelve el stock** (`stockReservado`) |
| `/{id}/palabraClave` | PUT | Body `{palabraClave}` — cambia la palabra clave (valida que no choque con otra existente en la rifa) |

### `/v1/concursante` — `ConcursanteControllerImpl`
CRUD genérico + extras:

| Endpoint | Método | Qué hace |
|---|---|---|
| `/registrar?forzar=` | POST | Registra concursante. Valida rifa activa y `fechaHoraLimite` (salvo `forzar=true`). Si trae `clientePedidoId`, calcula `boletosBase`/`boletos` con el mes actual. |
| `/porRifa/{configurarRifaId}` | GET | Todos los concursantes de la rifa |
| `/elegibles/{configurarRifaId}` | GET | Solo `descartado=false` |
| `/clientesPorMes?mes=YYYY-MM` | GET | `List<ClientePedidoDto>` — clientes únicos (registrados o no) con pedidos ese mes |
| `/importarDePedidos` | POST | Importa masivo desde `/clientesPorMes`. Si el request trae `mes`, calcula boletos por cada cliente con `clientePedidoId` |

### `/v1/ganadorRifa` — `GanadorRifaControllerImpl`
CRUD genérico sobre `GanadorRifa` + extras:

| Endpoint | Método | Qué hace |
|---|---|---|
| `/sortear/{configurarRifaId}` | POST | Ejecuta **un giro**. Sin params. Pondera por `boletos`. Marca `activa=false` en la rifa si era la última variante y salió ganador. Emite resultado por WebSocket. |
| `/continuarVariante/{configurarRifaId}?modo=` | POST | `modo` = `RESTANTES` / `CERO` / `NUEVOS`. Cierra historial de la variante anterior y aplica el modo a la siguiente. |
| `/estado/{configurarRifaId}` | GET | `SorteoEstadoDto` completo (ver sección 4) |
| `/reiniciar/{configurarRifaId}?completo=` | POST | `completo=false` → conserva concursantes, resetea `descartado`. `completo=true` → borra concursantes también. Siempre borra `ganador_rifa` e `historial_rifa_variante` y reactiva la rifa. |

### `/v1/rifa` — `RifaControllerImpl` ⚠️ **LEGACY / posible código muerto**
CRUD genérico sobre la entidad `Rifa` (tabla `rifas`, con `cliente`, `palabraRifa`, `configurarRifa`) +
`POST /registrar`, `GET /listConcursantes/{id}`, `GET /getRifasPorHora`, y un `@MessageMapping("/actualizar")`
que reenvía a `/topic/ruleta`. **No es invocado por ningún flujo activo** descrito en los contratos de
front ni usado por `ConcursanteServiceImpl`/`GanadorRifaServiceImpl`. Ver sección 6.

---

## 4. `SorteoEstadoDto` (respuesta de `GET /ganadorRifa/estado/{id}`)

```java
configurarRifa: ConfigurarRifa          // entidad cruda (variantes ignoradas vía @JsonIgnoreProperties)
totalConcursantes: int
totalVariantes: int
varianteNumeroActual: int               // 1-based
varianteActual: ConfigurarRifaVariante  // entidad cruda (no el DTO con imagen)
giroActual: int
giroGanador: int
elegibles: List<Concursante>            // de la palabraClave de varianteActual, descartado=false
descartados: List<Concursante>          // de la palabraClave de varianteActual, descartado=true
historial: List<HistorialRifaVariante>  // entidades crudas
rifaTerminada: boolean
```

⚠️ **Diferencia vs los contratos documentados**: `varianteActual` y `historial` exponen **entidades JPA
crudas**, no los DTOs limpios (`ConfigurarRifaVarianteDto` con imagen/stock) que sí usa
`SorteoResultadoDto` (respuesta de `/sortear`). Si el front espera `imagenBase64`/`stock` dentro de
`varianteActual` en `/estado`, **hoy no los recibe** — solo recibe los IDs/campos planos de la entidad.

`SorteoResultadoDto` (respuesta de `/sortear`):
```java
descartado: boolean
concursante: Concursante
varianteActual: ConfigurarRifaVarianteDto   // este sí trae imagen, stock, nombreProducto, etc.
rifaTerminada: boolean
```

---

## 5. Sistema de "boletos" ponderados — ⚠️ funcionalidad nueva, NO documentada para el front

Esto **no aparece en ningún `RIFAS_*.md` ni en `CAMBIOS_FRONT.md`**, pero ya está implementado y activo
en el sorteo:

- `Concursante.boletosBase` y `Concursante.boletos` (ambos default 1).
- Cálculo (`ConcursanteServiceImpl.calcularBoletos`, query `IPedidoRepository.calcularScore`):
  - `cumplimientos` = pedidos del cliente con `estado_pedido = 'Entregado'`
  - `incumplimientos` = pedidos con `motivo_cancelacion IN ('TIMEOUT','NO_SE_PRESENTO')`
  - `comprasMes` = pedidos `'Entregado'` del mes indicado
  - `score = cumplimientos / (cumplimientos + incumplimientos)` (1.0 si no hay datos)
  - `boletosBase = max(1, comprasMes)`
  - `boletos = max(1, round(comprasMes * score))`
- Se calcula en:
  - `POST /concursante/registrar` si el body trae `clientePedidoId` (usa el mes actual)
  - `POST /concursante/importarDePedidos` si el request trae `mes` y el cliente tiene `clientePedidoId`
- **El sorteo (`sortear`) usa `boletos` como peso**: cada concursante tiene tantas "papeletas" como
  `boletos`; se elige una al azar entre el total. Un cliente frecuente y cumplido tiene más
  probabilidad de ganar.
- Concursantes agregados manualmente sin `clientePedidoId` quedan con `boletos = 1` (peso normal).

**Pendiente de decidir con el front**: si se debe mostrar `boletos`/`boletosBase` en la UI (ej. "Juan
tiene 3 boletos"), y documentarlo en `CAMBIOS_FRONT.md`.

---

## 6. Código / tablas candidatas a limpieza (legacy del rediseño anterior)

| Elemento | Estado |
|---|---|
| `Rifa.java`, `RifaControllerImpl.java`, `RifaServiceImpl.java`, `IRifaService.java`, `IRifaRepository.java`, tabla `rifas`, endpoints `/v1/rifa/**` | No referenciados por el flujo nuevo (`Concursante`/`ConfigurarRifaVariante`). `RIFAS_REDISENO_COMPLETO.md` ya proponía eliminarlos. |
| `ConfigurarRifaProducto.java`, `IConfigurarRifaProductoRepository.java`, tabla `configurar_rifa_producto` | Reemplazada por `ConfigurarRifaVariante`. Repositorio sin uso fuera de sí mismo. |
| `RIFAS_SQL_MIGRATION.sql` | Migración del esquema intermedio "producto por rifa", ya superada por `RIFAS_DDL_COMPLETO.sql`. |
| `TarifaTerminal.java` | **No es de rifas** — pertenece al módulo de pagos/terminal (PagosCatalogo). Apareció en la búsqueda solo por estar en el mismo paquete `entity`. |

---

## 7. Vigencia de los documentos existentes

| Documento | Vigencia hoy |
|---|---|
| `RIFAS_REDISENO_COMPLETO.md` | Plan de rediseño — **ya implementado** en su mayoría (variantes, palabraClave, historial, continuarVariante, importar de pedidos) |
| `rifa_cambios.md` | Continuación del rediseño — el contrato que describe coincide bastante con lo implementado, salvo `boletos` (sección 5) |
| `RIFAS_FRONT_CONTRATO.md` | El más cercano al estado actual, pero: no menciona `boletos`/`boletosBase`, y no documenta que `/estado` devuelve entidades crudas (no DTOs) en `varianteActual`/`historial` |
| `RIFAS_FRONT_GUIA.md` | **Desactualizado** — describe el modelo anterior "1 rifa = N productos" (`configurarRifaProducto`, `productoActual`, `palabraRifa`), ya reemplazado por variantes |
| `RIFAS_DDL_COMPLETO.sql` | DDL coincide con las entidades actuales |

---

## 8. Puntos abiertos para definir contigo

1. ¿Qué necesitas exactamente ahora? (nueva funcionalidad, bug, limpieza, documentar para el front, etc.)
2. ¿Limpiamos el código legacy de la sección 6 (`Rifa`, `ConfigurarRifaProducto` y relacionados)?
3. ¿Documentamos el sistema de `boletos` en `CAMBIOS_FRONT.md` y/o lo exponemos en algún DTO de respuesta?
4. ¿`GET /ganadorRifa/estado/{id}` debería devolver `ConfigurarRifaVarianteDto`/historial "limpio" (con
   imagen/stock) en lugar de las entidades crudas, para que el front no tenga que hacer otra llamada?
5. ¿Actualizamos/archivamos `RIFAS_FRONT_GUIA.md` ya que describe un modelo obsoleto?
