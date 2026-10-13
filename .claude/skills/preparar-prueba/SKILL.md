---
name: preparar-prueba
description: Cómo darle a una prueba de QA de Novedades Jade su botón "🧪 Preparar prueba N", que crea los pedidos (y lo que haga falta) para que el dueño solo haga la prueba. Usar SIEMPRE que se escriba una prueba nueva en PRUEBAS_PENDIENTES_*.md cuyo "Antes de empezar" pida crear pedidos, clientes o zonas; cuando el dueño diga "prepárame los datos", "no quiero estar creando pedidos"; o cuando una prueba dependa de pedidos que otra ya canceló o pagó.
---

# Preparar prueba N — los datos de cada prueba con un botón

**Pedido del dueño (2026-10-13):** *"genera los scripts para generar las pruebas que necesitas; si
necesitas varios pedidos, los agregas, y yo hago las pruebas en pedidos. Cada prueba así hay que
hacerla, para no perder tiempo en generar pedidos."*

**Por qué botones y no scripts SQL:** los pedidos salen de los mismos servicios que la pantalla
(carrito del cliente, venta directa, abono, ✖ Cancelar), así se comportan igual que los reales: stock,
estados, abonos, venta. Un `INSERT` a mano en las tablas de pedidos se salta reglas y prueba algo que
la app nunca haría. Además el dueño no tiene que correr nada en la base.

Reglas del dominio: R16 y R17 en `hexagonal/datosprueba/README.md` (también R1 solo QA y R2 solo admin).

---

## 1. Dónde vive

| Pieza | Archivo (back, `hexagonal/datosprueba/`) |
|---|---|
| Lista de pruebas que se pueden preparar | `dominio/modelo/EscenarioDePrueba.java` (enum: número + título) |
| Qué crea cada una | `aplicacion/servicio/PrepararPruebaService.java` (un método `pruebaNN`) |
| Lo que el dominio necesita de afuera | `dominio/puerto/salida/EscenariosPruebaPort.java` (zona, cliente con cuenta, pedido del cliente, venta directa, cancelar) y `PedidosPruebaPort` (clientes sin cuenta, abonar) |
| Cómo se hace con los servicios reales | `infraestructura/salida/escenarios/EscenariosPruebaAdapter.java` |
| Endpoints | `infraestructura/entrada/rest/PrepararPruebaController.java`: `GET /v1/admin/datos-prueba/escenarios` (en `lista`) y `POST /v1/admin/datos-prueba/preparar/{prueba}` (201) |
| Botones | front: `admin/cache` (Sistema → 🗑️ Limpiar caché → 🧪 Datos de prueba → "Preparar una prueba"). Salen solos de la lista del back: **el front no se toca** para agregar una prueba |

## 2. Agregar una prueba nueva (receta)

1. **Leer la prueba** en el documento de pruebas: qué pedidos pide, en qué estado, de qué cliente, con
   qué zona, con cuánto dinero, si ya se lo llevó. Ponerles las **mismas claves** que usa la prueba
   (P1, P2, A, B, K4…).
2. **Enum:** `P30("30", "Título en palabras del dueño")`.
3. **Servicio:** `case P30 -> prueba30(linea, usuarioId);` y el método. Usar lo que ya hay:
   - `lineaDeUnArticuloNuevo(...)` ya crea un modelo propio con **un** artículo de $150 y stock 30. No
     usar artículos reales.
   - 🕓 **Pendiente** (lo pide el cliente desde su cuenta): `escenarios.pedidoDelCliente(clienteConCuenta, lineas, zona)`, **siempre con zona** (ver sección 3).
   - **Apartado / Ir pagando / Contado:** `escenarios.ventaDirecta("APARTADO" | "FIADO" | "NORMAL", clienteSinCuenta, usuarioId, lineas, zonaOnull, seLoLlevo)`.
   - **Abono:** `pedidos.abonar(id, monto, usuarioId)`. Si cubre el total, pasa solo a PAGADO.
   - **Cancelado:** `escenarios.cancelar(id)` (motivo "el cliente avisó", como el botón).
   - **Zona:** `escenarios.zonaDePrueba()` ("Zona Prueba QA", envío $0).
   - **Clientes:** `escenarios.clienteConCuentaDePrueba()` ("Cliente Prueba QA", correo `.invalid`) y
     `pedidos.clientesDePrueba(1).get(0)` ("Cliente Prueba QA 001", sin cuenta).
   - Regresar `EscenarioPreparado` con cada `PedidoPreparado(clave, id, "qué es")` y el **siguiente paso**
     en una línea ("Abre P1 → 🔗 Unir… → busca P2").
4. **Si hace falta algo nuevo** (otra forma de crear algo): método nuevo en `EscenariosPruebaPort` +
   adaptador que llame **al servicio real**. Nunca SQL de escritura a tablas de pedidos, ventas o abonos.
   Leer con SQL sí se vale (p. ej. el `pedido_id` de una venta de contado, que la venta directa no regresa).
5. **Reglas de negocio que hay que respetar** (skill `reglas-pedidos`): Apartado sin dinero (si lleva
   dinero es Ir pagando); el abono nunca mayor al saldo; un 🕓 Pendiente solo lo hace un cliente con
   cuenta, correo verificado y datos completos.
6. **Comprobarlo** con la base desechable y el back completo (skill `probar-en-local`): llamar
   `casoUso.preparar("30", 1)` e imprimir de la base estado, tipo, total, pagado, entregado, zona y
   cliente de cada pedido. Que coincida con lo que pide la prueba. Borrar la prueba temporal.
7. **Documentar:**
   - README del dominio: fila en "Qué deja cada prueba" y el número en R16.
   - En el documento de pruebas, "Antes de empezar" dice **"🧪 Preparar prueba 30"** y una tabla
     Pedido / Qué es / Número para anotar.
   - Mensaje 400 de la lista (sale solo del enum).
   - `CAMBIOS_FRONT.md` solo si cambia el contrato.

## 3. Cosas que ya se aprendieron

- **Cada clic crea datos nuevos**, a propósito. Una prueba no debe depender de lo que otra ya canceló o
  pagó: pasó con K5 y K6 (cancelados en la Prueba 4) y el dueño creyó que el buscador de Unir fallaba.
- **Todo 🕓 Pendiente lleva zona** (`escenarios.zonaDePrueba()`). Sin zona se toma como "recoge en el
  local", le ponen fecha para recoger (hoy + 3) y `PedidoCancelacionScheduler` lo cancela solo 2 días
  después: la prueba amanecería sin datos. Con zona la fecha queda vacía y no se cancela.
- **El cliente con cuenta recibe la confirmación del pedido por correo**: por eso su correo es
  `cliente.prueba.qa@ejemplo.invalid` (dominio reservado: no le llega a nadie).
- **De contado, la venta directa regresa la venta, no el pedido**: el adaptador lee `ventas.pedido_id`.
- **Solo en `inventario_key_qa`** (R1): en prod el botón responde 403 *"…no se hizo nada"*.
- **Las pruebas que no necesitan pedidos** (textos, colores, catálogo, permisos) no llevan botón.

## 4. Pruebas que ya tienen botón (2026-10-13)

| Prueba | Crea |
|---|---|
| 16 | A, B 🕓 Pendiente (cliente con cuenta) · C Ir pagando con $50 · D Contado. Todos en "Zona Prueba QA", sin llevárselos |
| 20 | P1, P2 🕓 Pendiente del cliente con cuenta, en "Zona Prueba QA" |
| 21 | P1 Apartado que debe · P2 Ir pagando ya pagado que falta entregar. En "Zona Prueba QA", sin fecha |
| 22 | PE 🕓 Pendiente (en "Zona Prueba QA") · AP Apartado que debe · CA Apartado cancelado |
| 24 | K4 Ir pagando sin dinero · K6 Ir pagando con $40 (para unir) |
