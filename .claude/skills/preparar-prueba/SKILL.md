---
name: preparar-prueba
description: Dar de alta TODO lo que necesita cada prueba de QA de Novedades Jade — modelos, artículos (con precio, stock, descuento, deshabilitados, agotados), clientes, zonas y pedidos — con el botón "🧪 Preparar prueba N" (y "Preparar todas"), para que el dueño solo haga la prueba. Usar SIEMPRE al escribir o cambiar una prueba en PRUEBAS_PENDIENTES_*.md cuyo "Antes de empezar" pida tener, anotar o crear algo (un artículo de $100, un modelo deshabilitado, dos pedidos…); cuando el dueño diga "prepárame los datos", "que ya estén los pedidos/artículos", "no quiero estar creando"; o cuando una prueba dependa de datos que otra ya canceló, pagó o vendió.
---

# Preparar prueba N — dar de alta lo que necesita cada prueba

**Pedido del dueño (2026-10-13):**
- *"genera los scripts para generar las pruebas que necesitas; si necesitas varios pedidos, los agregas,
  y yo hago las pruebas en pedidos. Cada prueba así hay que hacerla, para no perder tiempo."*
- *"me refiero a los artículos, que ya estén los pedidos de las pruebas como los pides; genera una skill
  para esto de las pruebas: dar de alta lo que se necesita para las pruebas."*

## La regla

**Ninguna prueba le pide al dueño que busque, anote o arme datos.** Si el "Antes de empezar" de una
prueba necesita algo que exista en un estado dado (un artículo de $100 con stock, un modelo
deshabilitado, un Ir pagando con $40, un pedido cancelado…), esa prueba lleva su botón
**🧪 Preparar prueba N** que lo da de alta, y el documento dice *"toca 🧪 Preparar prueba N"* con una
tabla de lo que crea. Lo único que no se prepara es **lo que es la prueba misma** (p. ej. en la 20.1 el
dueño crea la cuenta de cliente porque eso es lo que se prueba).

**Por qué botones y no scripts SQL:** los pedidos salen de los mismos servicios que la pantalla
(carrito del cliente, venta directa, abono, ✖ Cancelar), así se comportan igual que los reales: stock,
estados, abonos, venta. Un `INSERT` a mano en pedidos se salta reglas y prueba algo que la app nunca
haría. El dueño no corre nada en la base, y un botón se puede tocar las veces que haga falta.

Reglas del dominio: `hexagonal/datosprueba/README.md` — R1 (solo QA), R2 (solo admin), R16 (pedidos),
R17 (cliente con cuenta), R18 (catálogo), R19 (Preparar todas).

---

## 1. Dónde vive

| Pieza | Archivo (back, `hexagonal/datosprueba/`) |
|---|---|
| Lista de pruebas | `dominio/modelo/EscenarioDePrueba.java` (enum: número + título, en orden de número) |
| Qué crea cada una | `aplicacion/servicio/PrepararPruebaService.java` (un método `pruebaNN`) |
| Lo que el dominio necesita de afuera | `dominio/puerto/salida/EscenariosPruebaPort.java` (pedidos: zona, cliente con cuenta, carrito, venta directa, cancelar; catálogo: stock del modelo, habilitar modelo, artículo viejo deshabilitado con stock, precio del artículo, limpiar caché), `CatalogoPruebaPort` (dar de alta modelos con artículos y foto) y `PedidosPruebaPort` (clientes sin cuenta, abonar) |
| Cómo se hace | `infraestructura/salida/escenarios/EscenariosPruebaAdapter.java` |
| Endpoints | `GET /v1/admin/datos-prueba/escenarios` (en `lista`), `POST /v1/admin/datos-prueba/preparar/{prueba}` (201) y `POST /v1/admin/datos-prueba/preparar-todas` (201, en `lista`) |
| Botones | front `admin/cache` (Sistema → 🗑️ Limpiar caché → 🧪 Datos de prueba → "Preparar una prueba"). Salen solos de la lista del back: **el front no se toca** para agregar una prueba |

## 2. Agregar una prueba (receta)

1. **Leer el "Antes de empezar" y los pasos** de la prueba y listar cada cosa que debe existir, en qué
   estado y con qué nombre la llama la prueba (Art-A, M-OFF, P1, K4…). Usar **esas mismas claves**.
2. **Enum:** `P30("30", "Título en palabras del dueño")`, en su lugar por número.
3. **Servicio:** `case P30 -> prueba30(...)` y el método, con lo que ya hay:

   **Catálogo (R18)**
   - `modeloNuevo(escenario, "Art-A", categoria, precio, List.of(new ArticuloDePrueba(talla, color, stock), …))`
     → un modelo **"QA Prueba 30 Art-A #2098…"** con esos artículos, marca "Prueba QA", foto real de QA
     (R7) y stock del modelo = suma de los artículos. Regresa los artículos en el mismo orden.
   - Dejarlo en el estado que pide la prueba:
     `escenarios.stockDelModelo(art, n)` (stock libre = n − repartido) ·
     `escenarios.habilitarModelo(art, false)` (modelo deshabilitado) ·
     `escenarios.articuloDeshabilitadoConStock(art, n)` (artículo viejo dado de baja que guarda stock) ·
     `escenarios.precioDelArticulo(art, 130, 100, true)` (precio propio con descuento: la tarjeta dice $100;
     usa el mismo caso de uso que el 💲 de la tarjeta). Agotado = artículo con stock 0.
   - Reportarlo con `dato("Art-A", CatalogoPreparado.ARTICULO | MODELO, art, "qué es")`: lleva el nombre
     y el código de barras para buscarlo en pantalla.

   **Pedidos (R16)**
   - `lineaDeUnArticuloNuevo(escenario)` → un artículo propio de $150 con stock 30 para vender.
   - 🕓 **Pendiente:** `escenarios.pedidoDelCliente(clienteConCuenta, lineas, zona)`, **siempre con zona**.
   - **Apartado / Ir pagando / Contado:** `escenarios.ventaDirecta("APARTADO" | "FIADO" | "NORMAL",
     clienteSinCuenta, usuarioId, lineas, zonaOnull, seLoLlevo)`.
   - **Abono:** `pedidos.abonar(id, monto, usuarioId)` (si cubre el total, pasa solo a PAGADO).
   - **Cancelado:** `escenarios.cancelar(id)`.
   - **Zona:** `escenarios.zonaDePrueba()`. **Clientes:** `escenarios.clienteConCuentaDePrueba()` y
     `pedidos.clientesDePrueba(1).get(0)`.

   Regresar `new EscenarioPreparado(prueba, titulo, pedidos, catalogo, siguientePaso, null)` con el
   **siguiente paso** en una línea, con nombres reales de pantalla (skill `nombres-reales`).
4. **Si hace falta algo nuevo:** método en `EscenariosPruebaPort` + adaptador.
   - **Pedidos, ventas, abonos:** siempre por el **servicio real**; nunca SQL de escritura.
   - **Catálogo:** JDBC como el generador, **solo sobre modelos con marca "Prueba QA"** (el adaptador lo
     verifica en `modeloDePrueba()` y se niega si no). Si existe un caso de uso para ese cambio (precio,
     habilitar), usarlo.
   - Leer con SQL sí se vale (p. ej. el `pedido_id` de una venta de contado).
5. **Reglas de negocio** (skill `reglas-pedidos`): Apartado sin dinero; abono nunca mayor al saldo; un
   🕓 Pendiente solo lo hace un cliente con cuenta, correo verificado y datos completos.
6. **Comprobarlo** con la base desechable y el back completo (skill `probar-en-local`): `casoUso.preparar("30", 1)`
   y `casoUso.prepararTodas(1)`, e imprimir de la base el estado de cada cosa (stock y habilitado del modelo
   y del artículo, precio, estado/tipo/pagado/entregado del pedido). Que coincida con lo que pide la
   prueba. Borrar la prueba temporal.
7. **Documentar:**
   - README del dominio: fila en "Qué deja cada prueba".
   - Documento de pruebas: el "Antes de empezar" dice **"🧪 Preparar prueba 30"** y una tabla
     Clave / Qué es / Número o código para anotar. Quitar cualquier "busca uno que…", "anota uno con…"
     o SQL para prepararlo.
   - `CAMBIOS_FRONT.md` solo si cambia el contrato.

## 3. Cosas que ya se aprendieron

- **Las claves (K4, P1, T1, Art-A…) son etiquetas del documento, no algo que exista en pantalla.** El
  pedido no lleva su clave escrita (la nota `[DATOS DE PRUEBA]` no se muestra en ninguna pantalla), así que
  el dueño no puede "buscar K4". En el documento: **pedido → "escribe el número de K4"** (el del recuadro 🧪);
  **modelo o artículo → su nombre ("QA Prueba 17 Art-A") o su código de barras**. Pasó el 2026-10-13: el
  dueño no encontraba K4 en ninguna parte, y además le mandé a buscar **M3**, **AP**, **P1** y **A**.
- **Los buscadores del sistema piden 3 letras o más** (desde 1 dígito si es el número de un pedido). Antes de
  escribir "busca X" en un documento de pruebas o en el `siguientePaso` de una prueba, comprobar que X cumple
  esa regla. Para revisar: `grep -nE "(busca|Busca)[^|]{0,25}\b(P1|P2|K4|K6|T1|T2|PE|AP|CA|A|B|C|D|M3|M5)\b"
  PRUEBAS_PENDIENTES_*.md` debe salir sin pasos de prueba.
- **El recuadro 🧪 se queda guardado** en el navegador (por prueba, con la hora): antes se perdía al cambiar
  de pantalla y con él los números de los pedidos. Preparar una prueba otra vez reemplaza solo esa.
- **Cada clic crea datos nuevos**, a propósito: una prueba no debe depender de lo que otra ya canceló,
  pagó o vendió (pasó con K5 y K6, cancelados en la Prueba 4: el dueño creyó que fallaba el buscador de Unir).
- **Todo 🕓 Pendiente lleva zona.** Sin zona se toma como "recoge en el local", le ponen fecha para recoger
  (hoy + 3) y `PedidoCancelacionScheduler` lo cancela solo 2 días después.
- **El cliente con cuenta recibe la confirmación por correo**: su correo es `cliente.prueba.qa@ejemplo.invalid`.
- **De contado, la venta directa regresa la venta, no el pedido**: el adaptador lee `ventas.pedido_id`.
- **Después de preparar se limpia la caché** (`escenarios.limpiarCache()`): si no, Tienda sigue mostrando
  el catálogo de antes y el dueño no encuentra lo que se acaba de crear.
- **Las fotos se reusan de QA** (R7): en una base vacía los artículos quedan sin foto y no salen en la
  tienda pública (regla de las 4 condiciones); en QA sí tienen.
- **Al vender, el stock del artículo baja**: si la prueba necesita "stock 10" después de crear pedidos con
  ese artículo, darlo de alta con más (Art-200 nace con 12 y se venden 2).
- **"Preparar todas" (R19)** corre todas en orden; si una falla, las demás siguen y esa trae `error`.
- **Solo en `inventario_key_qa`** (R1): en prod responde 403 *"…no se hizo nada"*.

## 4. Pruebas con botón (2026-10-13)

| Prueba | Da de alta |
|---|---|
| 12 | **Art-200** ($200, stock 10) · **T1** venta de contado (ticket) · **T2** Ir pagando con abono de $50 (ticket de abono) |
| 14-QA | **M3**: modelo con stock 3 y un artículo con stock 1 con foto (🧩 Productos dice "Puedes crear: 2") |
| 15 | **M5** (stock 5) y **M10** (stock 10, otra categoría), cada uno con talla CH habilitada con 4 y talla M **deshabilitada que guarda 3** |
| 16 | **A**, **B** 🕓 Pendiente (cliente con cuenta) · **C** Ir pagando con $50 · **D** Contado. Todos en "Zona Prueba QA", sin llevárselos |
| 17 | **Filtros** (tallas CH, M, G habilitadas con 5; XL deshabilitada con 5) · **Art-A** ($100, stock 10) · **Descuento** ($130 que se vende a $100) · **Stock-1** |
| 18 | **M-OFF** (modelo deshabilitado) · **M-0** (habilitado, libre 0) |
| 20 | **P1**, **P2** 🕓 Pendiente en "Zona Prueba QA" · **Agotado** (modelo con stock 0) |
| 21 | **P1** Apartado que debe · **P2** Ir pagando ya pagado que falta entregar. En "Zona Prueba QA", sin fecha |
| 22 | **PE** 🕓 Pendiente · **AP** Apartado que debe · **CA** Apartado cancelado |
| 24 | **K4** Ir pagando sin dinero · **K6** Ir pagando con $40 (para unir) |

Sin botón, a propósito: 13, 19, 23 (solo pantallas y colores), 20.1 (crear la cuenta es la prueba) y 6
(es el generador masivo).
