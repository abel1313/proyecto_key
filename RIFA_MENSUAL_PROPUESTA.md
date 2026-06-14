# Rifa Mensual — Propuesta v2 (paso 1 de 2: mensual; luego diaria)

> Este documento es una **propuesta para revisar antes de programar nada**. Resume lo que entendí de
> tus requerimientos, qué de eso **ya existe** en el código, qué es **nuevo**, y cómo quedarían los
> endpoints (extendiendo `/v1/...` con campos opcionales nuevos — sin romper lo que ya usa el front,
> más un par de endpoints nuevos). Al final hay una lista de "puntos abiertos" para que me digas
> qué ajustar.

---

## 1. Lo que entendí, requisito por requisito

### 1.1 Origen de los participantes: pedidos **y** ventas de un mes
> "esa se tiene que sacar de los pedidos o de las ventas que se hayan hecho en un mes seleccionado"

**✅ Ya cubierto.** Cada venta directa de mostrador (`VentaServiceImpl.realizarVentaDirecta`) **siempre
crea también un `Pedido`** con `estadoPedido='Entregado'`. Por lo tanto `GET
/concursante/clientesPorMes?mes=YYYY-MM` ya trae tanto pedidos del catálogo en línea como ventas de
mostrador de ese mes — son la misma tabla.

### 1.2 Ventas hechas sin registrar al cliente, registrables al momento de armar la rifa
> "puede pasar que realice ventas sin registrarlos en el sistema pero en ese momento puedo
> registrarlo para hacer la rifa"

**✅ Ya cubierto, de dos formas:**
- Si la venta se hizo sin capturar datos del cliente, `clientesPorMes` la devuelve con
  `clientePedidoId: null, sinRegistro: true, nombre: ""` — el admin edita el nombre/teléfono antes de
  llamar a `importarDePedidos`.
- O el admin agrega al participante 100% manual (sin relación a ningún pedido) con `POST
  /concursante/registrar`, sin `clientePedidoId`. En ese caso queda con `boletos = 1` (peso normal,
  sin bono por compras).

### 1.3 Más compras en el mes = más probabilidad, **oculto**
> "entre mas compren en ese mes mas posibilidades tiene para ganar pero internamente sin que se
> enteren"

**Sistema de "boletos" — 🆕 fórmula ajustada (confirmado):**
- `boletosBase` = **suma de `detalle_pedidos.cantidad`** de los pedidos `Entregado` del cliente en
  el mes indicado (mínimo 1). Es decir, cuenta **unidades de producto compradas**, no nº de pedidos.
  Ejemplo: en junio, Juan hace 1 pedido con 3 prendas (1 bolsa + 2 carteras) → `boletosBase = 3`.
  María hace 2 pedidos de 1 prenda cada uno → `boletosBase = 2`.
  - ⚙️ Cambio de implementación: `IPedidoRepository.calcularScore` debe sumar `dp.cantidad` (join
    con `detalle_pedidos`) para `comprasMes`, en vez de `COUNT` de pedidos.
- `boletos` = `boletosBase` ajustado por su "score" histórico de cumplimiento
  (`Entregado` vs `TIMEOUT`/`NO_SE_PRESENTO`) — esto **no cambia**, sigue siendo a nivel pedido.
- `POST /ganadorRifa/sortear/{id}` arma el sorteo dándole a cada concursante tantas "papeletas" como
  `boletos` tenga, y elige una al azar — quien compró más (y cumple bien) tiene más papeletas.
- **Nadie ve este número en el resultado del giro** (`SorteoResultadoDto` no lo incluye).

**Los boletos SON la probabilidad, sin nada extra.** Es como una urna con papelitos: si Juan compró
3 veces tiene 3 papelitos con su nombre y María que compró 1 vez tiene 1. Si solo participan ellos
dos, hay 4 papelitos en total → Juan tiene 75% de probabilidad, María 25%. No hay ningún bono o
multiplicador adicional: la cantidad de `boletos` **es** directamente el peso de cada concursante en
el sorteo (`GanadorRifaServiceImpl.sortear()`: suma los `boletos` de todos los elegibles, saca un
número al azar entre 0 y ese total, y recorre acumulando hasta encontrar a quién le tocó).

⚠️ Único detalle: el campo `boletos`/`boletosBase` sí viaja **crudo** en
`GET /concursante/porRifa/{id}`, `GET /concursante/elegibles/{id}` y `GET /ganadorRifa/estado/{id}`
(porque ahí se serializa la entidad `Concursante` completa). Si el admin proyecta esa pantalla al
público, técnicamente se podría ver. Lo marco en "puntos abiertos" — es una decisión de
ocultarlo o no a nivel backend.

### 1.4 Multi-giro hasta el ganador (ej. "el 3er giro es el ganador")
> "tambien puede que te envien a la 3 vez es el ganador entonces haces la probabilidad la primera
> vez y descartas y lo eliminas y asi sucesivamente hasta que salga el ganador"

**✅ Ya implementado.** Cada variante tiene `giroGanador` (ej. 3). `sortear`:
- Giro 1 y 2 → `descartado: true` (ese concursante sale de la ruleta, queda `descartado=true`).
- Giro 3 → `descartado: false` → es el ganador de esa variante.

### 1.5 Modo "rifa de prueba" — para enseñarle a los participantes cómo funciona
> "debe de a ver una opcion para que la rifa valida... puedo dar girar o rifa y tu internamente
> haces la rifa y sale el ganador pero son de prueba"
>
> "esa rifa puede ser de prueba cuando de reiniciar regresan todos los usuarios incluyendo el que
> acabo de agregar tambien"

**🆕 Esto es lo único realmente nuevo.** Propuesta — **misma sesión** (`ConfigurarRifa`), con un
flag mutable que el front consulta para mostrar/ocultar el aviso:

- Nuevo campo `ConfigurarRifa.esPrueba` (boolean, default `false`).
- Cuando `esPrueba = true`:
  - El front (en `/activas`, `/activas/hoy` y `/ganadorRifa/estado/{id}`) ve `esPrueba: true` y
    muestra un mensaje/banner tipo **"⚠️ Esta rifa es de prueba"** sobre la ruleta.
  - `POST /ganadorRifa/sortear/{id}` funciona **exactamente igual** (misma probabilidad ponderada,
    mismo multi-giro, mismo descarte) — para que el admin pueda hacer una demo real frente al público.
  - Cada giro de la demo **sí se guarda normal** en `ganador_rifa`/`historial_rifa_variante` — por
    eso `GET /ganadorRifa/estado/{id}` muestra ganador/descartados igual que en la rifa real, para
    que el admin pueda enseñarle al público cómo se ve el resultado.
- Cuando el admin ya hizo la(s) demo(s) y va a hacer el sorteo real, cambia `esPrueba` a `false`
  (toggle, sección 2.1). Ese toggle hace, en un solo paso:
  1. **Borra los registros de la demo** (`ganador_rifa` + `historial_rifa_variante` de esa rifa) —
     esto es exactamente lo que ya hace `reiniciar(configurarRifaId, completo=false)` hoy, solo que
     ahora se dispara automáticamente al pasar a modo real.
  2. **Todos los concursantes vuelven a `descartado=false`** (elegibles otra vez), incluyendo los
     que se hayan agregado durante la demo — esto **ya funciona así hoy** en `reiniciar`.
  3. Reactiva la rifa (`activa=true`) y pone `esPrueba=false`.
  - El front deja de mostrar el aviso de "prueba" — para el público, esta rifa ahora **es la real**.
  - El admin hace `sortear` normalmente; estos giros sí generan los registros definitivos
    (`ganador_rifa`/`historial_rifa_variante`) que quedan guardados como el resultado oficial.
- Una rifa creada directamente con `esPrueba = false` (default) **nunca** muestra el aviso —
  es la rifa real desde el inicio, tal como pediste.

### 1.6 Editar / eliminar un participante mal capturado
> "puede pasar que me equivoque al registrar al usuario lo puedo eliminar o editar en su momento...
> como actualmente lo hace pero lo quiero mejorar"

**⚠️ Hoy existe pero es incómodo:**
- Eliminar: `DELETE /v1/concursante/delete` (genérico) — espera el `id` en el **body** de un DELETE.
- Editar: `PUT /v1/concursante/update/{id}` (genérico) — exige mandar el objeto `Concursante`
  **completo** (incluye `configurarRifa.id`, validación `@NotBlank nombre`, etc.)

**🆕 Propuesta — 2 endpoints nuevos, más simples** (ver sección 3).

### 1.7 (Implícito) Dos tipos de rifa: mensual y diaria
> "necesito 2 tipos de rifas la que es mensual y la que es por dia... iniciemos con la mensual"

**🆕 Propuesta** (para no rediseñar de nuevo en el paso 2): agregar
`ConfigurarRifa.tipo` (`"MENSUAL"` | `"DIARIA"`) y `ConfigurarRifa.mesReferencia` (solo si
`tipo="MENSUAL"`, formato `"YYYY-MM"`, informativo — para reportes / saber de qué mes se
importaron los participantes). No cambia la lógica de sorteo, solo etiqueta la sesión.

### 1.8 Distinguir a los concursantes agregados durante la prueba
> "deberia a ver 2 listas no una de la lista de los concursante mas lo que se agreguen... donde se
> esta haciendo la rifa de prueba"

**🆕 Propuesta:** nuevo campo `Concursante.agregadoEnPrueba` (boolean, default `false`).

- Se marca `true` automáticamente cuando `POST /concursante/registrar` se ejecuta mientras
  `configurarRifa.esPrueba = true` en ese momento.
- Viaja como un campo más en la entidad `Concursante` — **no rompe** el contrato actual de
  `/porRifa/{id}`, `/elegibles/{id}` ni `/estado/{id}` (que ya devuelven la entidad completa), solo
  se agrega esta propiedad.
- Con eso el front puede armar **2 listas** a partir de la misma respuesta:
  - **Concursantes** (`agregadoEnPrueba=false`): los importados de pedidos/ventas + correcciones
    manuales hechas fuera de modo prueba.
  - **Agregados durante la prueba** (`agregadoEnPrueba=true`): los que el admin metió mientras hacía
    la demo.
- Al pasar a sorteo real (toggle `esPrueba→false`, sección 2.1) estos concursantes **siguen
  participando** — tal como pediste, "regresan todos, incluyendo el que acabo de agregar". El flag
  solo es para que el admin los identifique y, si quiere, los elimine a mano
  (`DELETE /concursante/{id}`, sección 3) antes del sorteo real.

### 1.9 Buscar rifas que no son "de hoy"
> "hay que revisar que las rifas se muestren solo las del dia en el que estamos pero puedo traer
> otras rifas buscando"

**✅ Ya existe la distinción** entre:
- `GET /v1/configurarRifa/activas` → todas las rifas con `activa=true` (sin filtro de fecha).
- `GET /v1/configurarRifa/activas/hoy` → activas con `fechaHoraLimite` dentro del día de hoy
  (`ConfiguracionRifaServiceImpl.buscarActivasHoyResumen`, usado para la pantalla principal).

**🆕 Falta:** una forma de **buscar** una rifa que NO cae en "hoy" — por ejemplo una rifa mensual
configurada para el día 30 cuando hoy es el día 5, o revisar una rifa pasada. Propuesta — nuevo
endpoint:

`GET /v1/configurarRifa/buscar?desde=2026-06-25&hasta=2026-06-30` → rifas (activas o no) cuyo
`fechaHoraLimite` cae en ese rango de días. Con un solo día: `?desde=2026-06-30&hasta=2026-06-30`.
`GET /v1/configurarRifa/buscar?tipo=MENSUAL&mesReferencia=2026-06` → rifas mensuales de ese mes
(usa los campos nuevos de la sección 2).

Devuelve `List<ConfigurarRifaResumenDto>`, mismo formato que `/activas`. **Sin parámetros**, devuelve
las rifas activas del día de hoy (mismo resultado que `/activas/hoy`) — así nunca regresa "todo el
historial" por accidente.

**Cierre automático por tiempo — ya existe, y queda igual.** `RifaScheduler` corre todos los días a
las 2 AM y llama a `desactivarVencidas()`, que pone `activa=false` en cualquier rifa con
`fechaHoraLimite < (ahora - 1 día)` que siga activa (si nadie la sorteó, se cierra sola al día
siguiente).

**Confirmado:** `fechaHoraLimite` en una rifa `MENSUAL` es **el día que elijas para hacer el sorteo**
(puede ser del mes siguiente al `mesReferencia`, ej. `mesReferencia="2026-06"` con sorteo el
`2026-07-05`), y el sorteo se hace **antes de las 12:00** de ese día — mismo significado que hoy, no
requiere cambios. `mesReferencia` (de qué mes salen los participantes/compras) y `fechaHoraLimite`
(cuándo se sortea) son independientes, tal como ya estaba propuesto.

---

## 2. Cambios al modelo `ConfigurarRifa` (nuevos campos, todos opcionales/con default)

```json
{
  "id": 9,
  "fechaHoraLimite": "2026-07-01T20:00:00",
  "activa": true,
  "tipo": "MENSUAL",          // NUEVO. "MENSUAL" | "DIARIA". Nullable (rifas viejas quedan sin tipo)
  "mesReferencia": "2026-06", // NUEVO. Solo informativo si tipo="MENSUAL". Nullable
  "esPrueba": false           // NUEVO. default false. true = rifa de práctica/demo
}
```

`POST /v1/configurarRifa/save` y `PUT /v1/configurarRifa/update/{id}` aceptan estos 3 campos nuevos
(todos opcionales — si no se mandan, `esPrueba=false` y `tipo`/`mesReferencia` quedan `null`, sin
romper lo que el front ya manda hoy).

`GET /v1/configurarRifa/activas` y `/activas/hoy` devuelven estos 3 campos extra en
`ConfigurarRifaResumenDto`:

```json
{
  "id": 9,
  "fechaHoraLimite": "2026-07-01T20:00:00",
  "activa": true,
  "totalVariantes": 2,
  "variantesSorteadas": 0,
  "tipo": "MENSUAL",
  "mesReferencia": "2026-06",
  "esPrueba": false
}
```

### 2.1 Activar / desactivar el modo prueba (toggle)

Para no obligar al front a mandar el objeto `ConfigurarRifa` completo solo para cambiar este flag:

`PUT /v1/configurarRifa/{id}/esPrueba`

**Request:**
```json
{ "esPrueba": false }
```

**Response:**
```json
{ "code": 200, "data": { "id": 9, "esPrueba": false } }
```

El front llama esto cuando el admin presiona algo como **"Pasar a sorteo real"** (apaga el aviso de
prueba) o **"Modo demo"** (lo prende de nuevo).

⚠️ **Importante — qué hace internamente al pasar de `true` → `false`:** además de cambiar el flag,
ejecuta lo mismo que `reiniciar(configurarRifaId, completo=false)`:
- Borra los giros de la demo (`ganador_rifa` + `historial_rifa_variante` de esa rifa) para que no se
  mezclen con los resultados oficiales.
- Regresa a todos los concursantes a `descartado=false` (elegibles), incluidos los agregados durante
  la demo.
- Reactiva la rifa (`activa=true`).

Al pasar de `false` → `true` (activar modo demo) **no** se borra nada — solo cambia el flag y el
front empieza a mostrar el aviso "prueba".

---

## 3. Endpoints nuevos — editar / eliminar concursante

### `DELETE /v1/concursante/{id}`
Reemplaza el uso de `/delete` genérico para este caso de uso.

**Response OK:**
```json
{ "code": 200, "data": "Concursante eliminado" }
```

**Response error** (si ya participó en algún giro — tiene filas en `ganador_rifa`):
```json
{ "code": 400, "mensaje": "No se puede eliminar: el concursante ya participó en un sorteo" }
```

### `PUT /v1/concursante/{id}` (body parcial, reemplaza el `update` genérico para este caso)

**Request:**
```json
{
  "nombre": "Juan",
  "apellidoPaterno": "García",
  "telefono": "5551234567",
  "palabraClave": "BOLSA",
  "ordenDesde": 1
}
```
Solo estos 5 campos son editables. `boletos`, `boletosBase`, `descartado`, `configurarRifa` y
`clientePedidoId` **no se tocan** desde este endpoint (son manejados internamente).

**Response:**
```json
{
  "code": 200,
  "data": {
    "id": 201,
    "nombre": "Juan",
    "apellidoPaterno": "García",
    "telefono": "5551234567",
    "palabraClave": "BOLSA",
    "ordenDesde": 1,
    "descartado": false
  }
}
```

---

## 4. Mejora propuesta — evitar duplicados al importar de pedidos

`POST /concursante/importarDePedidos` hoy no valida si un `clientePedidoId` **ya fue importado antes**
a esa misma rifa (si el admin da clic dos veces, ese cliente queda registrado 2 veces → boletos
duplicados sin querer).

**Propuesta:** el servicio omite (no inserta) los `clientePedidoId` que ya existan como `Concursante`
en esa `configurarRifaId`, y el response indica cuáles se omitieron:

```json
{
  "code": 200,
  "data": {
    "importados": [
      { "id": 201, "nombre": "María López", "palabraClave": "BOLSA" }
    ],
    "omitidosYaRegistrados": [
      { "clientePedidoId": 102, "nombre": "Carlos Ruiz" }
    ]
  }
}
```

---

## 5. Flujo completo — Rifa mensual (con lo nuevo)

```
1. Crear la sesión, indicando el mes:
   POST /v1/configurarRifa/save
   { "fechaHoraLimite": "2026-07-01T20:00:00", "activa": true,
     "tipo": "MENSUAL", "mesReferencia": "2026-06", "esPrueba": false }
   → devuelve { id: 9, ... }

2. Agregar las variantes/premios a rifar (igual que hoy):
   POST /v1/configurarRifaVariante/save  (una vez por premio)

3. Traer compradores del mes (pedidos + ventas directas, ya unificado):
   GET /v1/concursante/clientesPorMes?mes=2026-06
   - Si una venta no tenía cliente asociado: sinRegistro=true, nombre="" → el admin lo completa
     antes de importar.

4. Importar en bloque (con protección de duplicados):
   POST /v1/concursante/importarDePedidos
   { configurarRifaId: 9, palabraClave: "BOLSA", ordenDesde: 1, mes: "2026-06", clientes: [...] }
   → boletos se calculan internamente, no se exponen al front.

5. Si el admin recuerda una venta que no quedó registrada, la agrega a mano:
   POST /v1/concursante/registrar
   { nombre: "...", telefono: "...", palabraClave: "BOLSA", configurarRifa: { id: 9 } }

6. Corregir errores de captura:
   PUT /v1/concursante/{id}     → corrige nombre/teléfono/palabraClave/ordenDesde
   DELETE /v1/concursante/{id}  → lo quita (si aún no participó en ningún giro)

7. (Opcional) Demo en vivo para el público:
   - Marcar la rifa con esPrueba=true (al crearla, o vía PUT /configurarRifa/9/esPrueba {esPrueba:true})
   - El front muestra el aviso "⚠️ Esta rifa es de prueba"
   - POST /ganadorRifa/sortear/9  → igual que el sorteo real (multi-giro, descartes, ganador)
   - POST /ganadorRifa/reiniciar/9?completo=false → todos regresan, incluidos los agregados durante
     la demo, listo para el sorteo real

8. Antes del sorteo real, apagar el modo prueba:
   PUT /v1/configurarRifa/9/esPrueba { "esPrueba": false }
   → el front deja de mostrar el aviso de "prueba"

9. Sorteo real (esPrueba=false):
   POST /ganadorRifa/sortear/9          (repetir hasta ganador, ponderado por boletos ocultos)
   POST /ganadorRifa/continuarVariante/9?modo=RESTANTES|CERO|NUEVOS  (si hay más premios)
   GET  /ganadorRifa/estado/9           (ver estado / historial en cualquier momento)
```

---

## 6. Puntos resueltos (diseño cerrado)

Todos los puntos abiertos quedaron resueltos. Resumen de decisiones:

1. **`esPrueba` — misma sesión.** El front muestra/oculta el aviso "Esta rifa es de prueba" según
   el flag `esPrueba`, que se apaga con `PUT /v1/configurarRifa/{id}/esPrueba` antes del sorteo real
   (sección 2.1).

2. **Nombre del campo**: `esPrueba` (boolean, default `false`).

3. **`boletos`/`boletosBase`**: NO se ocultan con `@JsonIgnore` (todos los endpoints de `/concursante`
   ya son ADMIN-only). Se documenta en `CAMBIOS_FRONT.md` que el front no debe mostrarlos en
   pantallas proyectadas al público.

4. **Importación duplicada**: se omiten silenciosamente los `clientePedidoId` ya registrados en esa
   rifa, devolviendo `omitidosYaRegistrados` en la respuesta.

5. **`tipo` / `mesReferencia`**: solo informativos + filtro de `/buscar` (sección 1.9). No alimentan
   automáticamente `clientesPorMes`/`importarDePedidos`.

6. **Eliminar concursante que ya participó en un giro**: se bloquea con error 400 (preserva
   integridad del historial).

7. **`GET /configurarRifa/buscar` sin parámetros**: devuelve las rifas activas del día de hoy (igual
   que `/activas/hoy`) — sección 1.9.

8. **Fórmula de boletos**: por **cantidad de productos** (`SUM(detalle_pedidos.cantidad)`), no por
   número de pedidos — sección 1.3.

9. **`fechaHoraLimite` en rifas `MENSUAL`**: es el día/hora del sorteo (antes de las 12:00 de ese
   día), puede ser del mes siguiente al `mesReferencia`. Mismo significado que hoy, sin cambios al
   cierre automático — sección 1.9.
