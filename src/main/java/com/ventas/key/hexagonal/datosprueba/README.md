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

## Qué se ve al terminar

- En **🛍️ Tienda**, los artículos de prueba mezclados con los reales (con foto, si QA tiene fotos).
- En **🔍 Modelos**, los modelos con marca "Prueba QA".
- En **Mis pedidos** y **💳 Créditos / Abonos**, los pedidos de prueba, a nombre de "Cliente Prueba QA NNN".
- En **Reportes**, las ventas de contado y las de los pedidos que se terminaron de pagar.

## Capas

```
dominio/
  modelo/      PlanDeDatos (R11), CatalogoAleatorio, ModeloDePrueba, ArticuloDePrueba,
               ArticuloGuardado, TipoPedidoPrueba (R9), Avance
  excepcion/   DatosPruebaException y sus 3 casos (R1, R3/R15, R11)
  puerto/      entrada: DatosPruebaCasoUso · salida: AmbientePort, CatalogoPruebaPort, PedidosPruebaPort
aplicacion/    GenerarDatosPruebaService: valida, arranca en segundo plano, orquesta lotes y pedidos
infraestructura/
  entrada/rest DatosPruebaController (/v1/admin/datos-prueba)
  salida/      AmbienteMysqlAdapter (R1), CatalogoPruebaJdbcAdapter (lotes JDBC, R4–R7, R12, R14),
               PedidosPruebaVentaAdapter (venta directa y abono reales, R8, R10)
  config/      DatosPruebaConfig: un hilo propio para la corrida
```
