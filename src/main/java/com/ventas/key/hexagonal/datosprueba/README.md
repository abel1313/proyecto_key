# Dominio `datosprueba` — generar catálogo y pedidos de prueba en QA

Pedido del dueño (2026-10-01): *"meter unos 20 mil productos, dividirlos en artículos para hacer
pruebas y no tener que estar agregándolos manualmente; ejecutarlo una vez y que se guarden en
segundo plano; y también agregar pedidos aleatorios"*.

Se arranca con un botón (o `POST /v1/admin/datos-prueba/generar`), la respuesta regresa al momento
y el trabajo sigue en segundo plano. El avance se consulta con `GET /v1/admin/datos-prueba/avance`.

## Reglas

| # | Regla | Por qué |
|---|---|---|
| R1 | **Solo corre en `inventario_key_qa`.** El nombre de la base se le pregunta a MySQL (`SELECT DATABASE()`), no a la configuración. En cualquier otra base se niega antes de escribir nada | Un `yml` mal copiado no puede llevar 20 mil productos falsos a producción |
| R2 | **Solo administrador** (`ROLE_ADMIN`), no basta con tener la pantalla de caché | Llena la base; no es una acción de un rol limitado |
| R3 | **Una corrida a la vez.** Si hay una en curso, la segunda se rechaza (409) | Dos corridas juntas se pelean la numeración de los códigos |
| R4 | **Todo lo creado se reconoce:** código de barras `2098` + 9 dígitos y marca **"Prueba QA"** en modelos y artículos; clientes **"Cliente Prueba QA NNN"**; pedidos con la observación **`[DATOS DE PRUEBA]`** | Para encontrarlos, filtrarlos y darlos de baja sin tocar nada real. El rango `2099…` ya lo usa `datos_prueba_qa_catalogo.sql` |
| R5 | **Volver a correr no duplica:** sigue la numeración desde el código `2098…` más alto que ya exista | Se puede correr en partes (5 mil hoy, 15 mil mañana) |
| R6 | **Stock de cada artículo entre 6 y 20**, y el stock del modelo es la suma de sus artículos (stock libre 0) | La alerta diaria avisa de artículos con stock ≤ 5: con stock bajo, el correo listaría miles de artículos de prueba |
| R7 | **Imágenes: se reusan las que ya hay en QA** (de productos reales, sin subir nada nuevo). Cada artículo queda ligado a una; el modelo, a la de su primer artículo. Si QA no tiene ninguna imagen, se crean igual pero **no salen en la tienda** (regla de las 4 condiciones) y el avance lo dice | Sin imagen un artículo no aparece en la tienda pública. Dar de baja nunca borra una foto real |
| R8 | **Los pedidos pasan por la venta directa y el abono reales**, con sus reglas: Apartado sin dinero, Ir pagando con enganche, el abono nunca mayor al saldo, sin tarjeta en Apartado/Ir pagando, el precio del catálogo, el stock que se descuenta | Los pedidos de prueba se comportan igual que los de la pantalla |
| R9 | **Mezcla de pedidos:** 40 % Contado en efectivo (queda Entregado), 20 % Apartado sin dinero, 10 % Apartado pagado completo, 30 % Ir pagando (enganche del 20 al 50 %, de 0 a 2 abonos más; uno de cada 4 se termina de pagar). De 1 a 3 artículos por pedido, de 1 a 2 piezas cada uno | Que haya de todo para probar Créditos / Abonos, Mis pedidos, Reportes |
| R10 | **Sin notificaciones:** los clientes de prueba no tienen correo ni teléfono y los pedidos no piden ticket | Ningún correo ni WhatsApp sale a nadie |
| R11 | **Topes por corrida:** de 1 a 20 000 modelos, de 1 a 4 artículos por modelo, de 0 a 3 000 pedidos | Que un número mal escrito no cree 2 millones de filas |
| R12 | **Se guarda en lotes de 500 modelos**, cada lote en su propia transacción. Si algo falla a la mitad, lo ya guardado se queda y el avance dice dónde paró; volver a correr sigue la numeración (R5) | Una falla en el lote 30 no tira 15 mil modelos ya guardados |
| R13 | **Un pedido que falla no detiene la corrida:** se cuenta en "pedidos con error" con el último motivo. Si fallan más de la mitad de los primeros 20, se detiene | Un error de datos no debe dejar 3 000 intentos fallidos en el log |
| R14 | **Dar de baja** (`POST /v1/admin/datos-prueba/dar-de-baja`): los modelos y artículos con código `2098…` y marca "Prueba QA" quedan con `habilitado = '0'` y se quitan sus ligas de imagen. Nunca `DELETE` de modelos ni artículos, nunca se borra una imagen. Los pedidos de prueba se quedan como historial | Baja lógica, igual que en el resto del sistema |
| R15 | Con una corrida en curso **no se puede dar de baja** (409) | Se daría de baja a la mitad de un lote |
| R16 | **Preparar prueba N** (2026-10-13, pedido del dueño: *"genera los pedidos que necesita cada prueba y yo solo hago la prueba"*). Cada vez que se toca el botón se crean **datos nuevos**: un modelo de prueba con un artículo (marca "Prueba QA", código `2098…`, $150, stock 30) y los pedidos de esa prueba, con la observación `[DATOS DE PRUEBA]`. Todo 🕓 Pendiente que crea lleva la zona de prueba: así no tiene fecha para recoger y el cancelador automático (`PedidoCancelacionScheduler`) no lo cancela a los pocos días. Pasan por los mismos servicios que la pantalla (R8): el carrito del cliente (`savePedido`), la venta directa, el abono y ✖ Cancelar. Mismas reglas R1 y R2 (solo QA, solo administrador). Pruebas que se pueden preparar: las de la tabla de abajo (12, 14-QA, 15, 16, 17, 18, 20, 21, 22 y 24) | Una prueba no depende de lo que otra ya canceló, pagó o unió (pasó con K5 y K6). El dueño no pierde tiempo armando pedidos |
| R17 | Excepción a R10 para las pruebas que necesitan un **🕓 Pendiente**: ese pedido solo lo hace un cliente **con cuenta**, con correo verificado y datos completos (si no, `savePedido` lo rechaza). Se usa **un solo** cliente, "Cliente Prueba QA", con correo `cliente.prueba.qa@ejemplo.invalid` y teléfono `0000000000`, y se reusa. `.invalid` es un dominio reservado: la confirmación del pedido no le llega a nadie (a lo más regresa un rebote al buzón de envío, que no se revisa) | Un Pendiente no se puede crear por la venta directa |
| R18 | **Catálogo de cada prueba** (2026-10-13, pedido del dueño: *"dar de alta lo que se necesita para las pruebas"*). Los modelos y artículos que pide una prueba (Art-A de $100, Art-200, un modelo deshabilitado, uno agotado, un artículo viejo dado de baja que guarda stock…) salen del mismo generador del catálogo de prueba (R4–R7): nombre **"QA Prueba N <clave> #2098…"**, marca "Prueba QA", foto real de QA. Después se dejan en el estado que pide la prueba por `EscenariosPruebaPort` (stock del modelo, habilitar modelo, artículo deshabilitado con stock), con JDBC y **solo sobre modelos de marca "Prueba QA"**: el adaptador lo revisa antes de escribir y se niega si no lo es. El precio propio del artículo pasa por el mismo caso de uso que el 💲 de la tarjeta (`CambiarPrecioArticuloCasoUso`). Al terminar se limpia la caché del catálogo | Ninguna prueba le pide al dueño que busque o anote un artículo en cierto estado. La caché se limpia porque si no, Tienda sigue mostrando el catálogo de antes y lo recién creado no aparece |
| R19 | **Preparar todas** (`POST /v1/admin/datos-prueba/preparar-todas`): corre todas las pruebas en orden. Si una falla, las demás siguen y esa regresa con `error` (el motivo). Mismas reglas R1 y R2 | El dueño deja listo todo el documento de pruebas con un clic |

### Qué deja cada prueba (Sistema → 🗑️ Limpiar caché → 🧪 Datos de prueba → **Preparar prueba**)

| Prueba | Lo que da de alta | Para qué |
|---|---|---|
| **12** Lo legal: Art-200 y tickets para imprimir | **Art-200**: artículo de $200 (nace con 12, quedan 10 después de las dos ventas) · **T1**: 🛒 venta de contado de Art-200, ya cobrada · **T2**: 💳 Ir pagando de Art-200 con un abono de $50 | 12.6 Venta directa con Art-200; 12.7 imprimir el ticket de venta (T1) y el de abono (T2) |
| **14-QA** 🧩 Productos del modelo | **M3**: modelo con stock 3 y un artículo con stock 1 con foto | 🧩 Productos dice "Puedes crear: 2" |
| **15** Habilitar un artículo viejo que guarda stock | **M5**: modelo con stock 5, talla CH habilitada con 4 y talla M **deshabilitada que guarda 3** · **M10**: igual pero con stock 10 y de otra categoría | Al habilitar la M de M5 el modelo queda con 1 libre; en M10 se queda con sus 3 |
| **16** 🕓 Pendiente, cambiarlo a Apartado y Entregas por zona | **A** y **B**: 🕓 Pendiente del cliente con cuenta · **C**: 💳 Ir pagando con $50 · **D**: 🛒 Contado. Todos en "Zona Prueba QA" y sin llevárselos (C y D: "todavía no") | Filtros de Pendiente y Contado, pasar A a Apartado y B a Ir pagando, Entregas por zona |
| **17** Filtros de Tienda, precio $100 y Venta directa | **Filtros**: modelo con tallas CH, M y G habilitadas con 5 y XL deshabilitada con 5 · **Art-A**: $100, stock 10 · **Descuento**: $130 que se vende al descuento de $100 · **Stock-1**: stock 1 | 17.1 filtros de talla, 17.2 precio $100 (salen Art-A y Descuento), 17.3 Venta directa con stock 1 |
| **18** Agregar artículo | **M-OFF**: modelo deshabilitado (su artículo sigue habilitado) · **M-0**: modelo habilitado con stock 4, todo repartido (libre 0) | 🧩 Agregar producto con un modelo deshabilitado y con uno sin stock libre |
| **20** Pedidos unidos: cambiar la forma de cobro del grupo, y un modelo agotado | **P1** y **P2**: 🕓 Pendiente de "Cliente Prueba QA", $150 cada uno, en "Zona Prueba QA" · **Agotado**: modelo con stock 0 | 20.2 unirlos y cambiar la forma de cobro del grupo; 20.3 el modelo agotado |
| **21** La fecha de entrega en Pedidos y en Entregas por zona | **P1**: 📦 Apartado que debe $150 · **P2**: 💳 Ir pagando ya pagado completo que falta entregar. Los dos en la zona **"Zona Prueba QA"** (se crea si no existe, costo de envío $0) y sin fecha | Ponerles fecha en Entregas por zona y verla en Mis pedidos |
| **22** 🕓 Pendiente en Pago y Mis pedidos sin cancelados | **PE**: 🕓 Pendiente (en "Zona Prueba QA") · **AP**: 📦 Apartado que debe $150 · **CA**: Apartado cancelado ("el cliente avisó") | Filtros de Pago y Mis pedidos al abrir |
| **24** Unir un Ir pagando que ya tiene dinero | **K4**: 💳 Ir pagando sin dinero · **K6**: 💳 Ir pagando que ya dio $40. Del mismo cliente de prueba, los dos "ya se lo llevó" | Que "🔗 Unir" encuentre un Ir pagando con dinero (lo de K4/K5/K6 del 2026-10-13) |

Cada prueba regresa sus `pedidos` (clave, número y qué es) y su `catalogo` (clave, modelo, artículo, nombre, código de barras y qué es), con las **mismas claves** que usa el documento de pruebas (Art-A, M-OFF, P1, K4…).

Endpoints: `GET /v1/admin/datos-prueba/escenarios` (lista en `lista`), `POST /v1/admin/datos-prueba/preparar/{prueba}` (201 con lo que se creó; 403 fuera de QA; 400 si la prueba no existe) y `POST /v1/admin/datos-prueba/preparar-todas` (201, una por prueba en `lista`; la que falló trae `error`).

**Para agregar otra prueba:** skill `preparar-prueba` (receta completa). En corto: un valor nuevo en `EscenarioDePrueba` y su método en `PrepararPruebaService`. Si necesita algo que todavía no existe, va como método nuevo de `EscenariosPruebaPort`: pedidos, ventas y abonos siempre por el servicio real (nunca un `INSERT` a las tablas de pedidos); catálogo solo sobre modelos "Prueba QA" (R18).

## Qué se ve al terminar

- En **🛍️ Tienda**, los artículos de prueba mezclados con los reales (con foto, si QA tiene fotos).
- En **🔍 Modelos**, los modelos con marca "Prueba QA".
- En **Mis pedidos** y **💳 Créditos / Abonos**, los pedidos de prueba, a nombre de "Cliente Prueba QA NNN".
- En **Reportes**, las ventas de contado y las de los pedidos que se terminaron de pagar.

## Capas

```
dominio/
  modelo/      PlanDeDatos (R11), CatalogoAleatorio, ModeloDePrueba, ArticuloDePrueba,
               ArticuloGuardado, TipoPedidoPrueba (R9), Avance,
               EscenarioDePrueba, EscenarioPreparado, PedidoPreparado (R16),
               CatalogoPreparado, DatosDelModelo (R18)
  excepcion/   DatosPruebaException y sus 3 casos (R1, R3/R15, R11)
  puerto/      entrada: DatosPruebaCasoUso, PrepararPruebaCasoUso (R16, R19)
               salida: AmbientePort, CatalogoPruebaPort, PedidosPruebaPort, EscenariosPruebaPort (R16–R18)
aplicacion/    GenerarDatosPruebaService: valida, arranca en segundo plano, orquesta lotes y pedidos
               PrepararPruebaService: el catálogo y los pedidos de la prueba elegida, o de todas (R16, R18, R19)
infraestructura/
  entrada/rest DatosPruebaController y PrepararPruebaController (/v1/admin/datos-prueba)
  salida/      AmbienteMysqlAdapter (R1), CatalogoPruebaJdbcAdapter (lotes JDBC, R4–R7, R12, R14),
               PedidosPruebaVentaAdapter (venta directa y abono reales, R8, R10)
               EscenariosPruebaAdapter (zona, cliente con cuenta, carrito, venta directa y cancelar, R16, R17;
               estado del catálogo de prueba, precio y caché, R18)
  config/      DatosPruebaConfig: un hilo propio para la corrida
```
