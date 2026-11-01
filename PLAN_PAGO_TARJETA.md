# Plan — pago con tarjeta (terminal del local y tienda en línea)

**Fecha:** 2026-10-21 · **Rama:** `feature/pago-tarjeta` (desde `dev`) · **Estado:** dominio, persistencia, planes MSI y pasarela simulada hechos y revisados dos veces (reconstruidos el 2026-10-21 tras perder el trabajo no subido; ahora la rama está en GitHub) · pendiente: adaptador del pedido, pasarelas reales, controllers y front.

Reglas, principios y contratos: `src/main/java/com/ventas/key/hexagonal/pagotarjeta/README.md`.

## Decisiones del dueño
- La terminal sigue en la API actual (no se migra); las pruebas son con una pasarela simulada en QA, sin mover dinero.
- Tarjeta solo cuando el cliente llega al local o cuando hace el pedido y paga él mismo. Apartado e Ir pagando: efectivo o transferencia.
- MSI solo en algunos productos: casilla por producto/artículo (apagada por default), mínimo $300, planes 3/6/9/12.
- El cambio se lleva solo a `dev` y `qa`, nunca a `main`, cuando esté probado y el dueño lo pida.

## Fases
1. ✅ Dominio + servicios + pruebas.
2. ✅ Persistencia (intentos, devoluciones, planes, transacción) + migración en QA.
3. ✅ Pasarela simulada (dev/qa).
4. ✅ Adaptador del pedido (`PedidoACobrarPort`, `ActualizarPedidoPort`) → `PedidoCobroAdapter`.
5. ⏳ Terminal real (`PointClient`) y Checkout Pro (`external_reference`, `expiration_date_to`, tope de cuotas, firma del webhook).
6. ✅ Controllers + permisos + `CAMBIOS_FRONT.md` (`PagoTarjetaController`, `/v1/pagos-tarjeta`; módulo apagado por `pagos.tarjeta.habilitado`).
7. ⏳ Front (casilla MSI, botón de pagar, estados) y pruebas en QA con la pasarela simulada.

---

## Plan actualizado (2026-10-28) — decisiones del dueño y orden

**Hallazgo:** hoy la terminal real se cobra por un camino **viejo** (`/v1/mp/*`, `MercadoPagoService` + `PointClient`) que es el que usa el front (`pedidos/pago.service.ts`, diálogo "Cobrar pedido" en `mis-pedidos` y `venta-directa`). El módulo nuevo (`/v1/pagos-tarjeta`) todavía no tiene front ni pasarela real. Dos caminos para lo mismo = riesgo de doble cobro; el viejo no tiene `external_reference`, devoluciones ni reconciliación, y su webhook no valida firma.

**Decisiones:**
1. La terminal **pasa al módulo nuevo**; un solo camino. Mientras convivan, `/v1/mp/iniciar` y `/v1/pagos-tarjeta` se rechazan entre sí si ya hay un cobro activo del mismo pedido o de la misma terminal.
2. **MSI:** casilla "Acepta meses sin intereses" por **producto** (aplica a todos sus artículos) y por **artículo** (NULL = hereda, 0/1 = propia), apagada por default. Si el pedido trae un artículo que no acepta, no hay meses y se explica cuál. Mínimo $300; planes 3/6/9/12; nunca en Apartado ni Ir pagando.
3. **Precio por cantidad** (Etapa de después, ver `PLAN_PRECIO_POR_CANTIDAD.md`).
4. **Lo que depende de datos de Mercado Pago va AL FINAL** (credenciales, terminal real, Checkout Pro, webhook firmado, producción).

**Orden aprobado:**
1. Anotar todo (este documento, `PLAN_PRECIO_POR_CANTIDAD.md`, skill `reglas-pedidos`).
2. Casilla de meses en producto y artículo (back + front + chips MSI).
3. ✅ (2026-10-28, ramas `feature/cobro-tarjeta` back y front) Botones de cobrar con tarjeta (terminal y en línea) funcionando con la pasarela **simulada**; los dos caminos de la terminal se rechazan entre sí; `venta-directa` y los grupos siguen por el camino anterior.
4. Pendientes de seguridad: bloquear abonos / agregar artículos / unir en grupo con cobro activo; pool de hilos de `@Scheduled`; mensaje claro si se enciende sin pasarela; freno iniciar→cancelar; lista de `COBRADO_SIN_APLICAR`; hotfix de `simular-pago` en producción (decisión del dueño).
5. Precio por cantidad.
6. **Al final:** adaptador real de la terminal (`PointClient`, `external_reference`, reembolso), Checkout Pro (`external_reference`, `expiration_date_to`, tope de cuotas, `back_urls`), webhook con firma (T5), filas de 6/9/12 meses en `pagos_y_meses` con tarifas reales, migración en producción y llevar a `main` por cherry-pick.

**Prerrequisito de producción (crítico):** mapear `aceptaMsi` en las entidades sin la columna en `inventario_key` tumba todo el catálogo (`Unknown column 'acepta_msi'`). La migración `migration_pago_tarjeta.sql` se corre en producción **antes** de llevar cualquier cosa a `main`.

**Datos que el dueño debe conseguir (solo para el paso 6; nunca por chat, van como secretos en Kubernetes):** credenciales de prueba y producción de Mercado Pago, usuarios y tarjetas de prueba, Checkout Pro habilitado, modelo y `device-id` de la terminal de producción, URL y secreto del webhook, tarifas reales por plan.

**Decisiones del dueño aún abiertas:** quién paga la comisión (la fórmula actual copia la de ventas y puede estar mal); si devolver afecta pedido/venta/stock; qué se hace con `COBRADO_SIN_APLICAR`; planes de meses habilitados; entrega del pago en línea (a dónde y cuándo).

## 🟡 PENDIENTE — Pago en línea con Mercado Pago real (anotado 2026-11-01)

**Estado:** el código está terminado y subido a `dev` y `qa` (Checkout Pro, webhook con firma, reembolso, enrutador por canal; `PASOS_MERCADO_PAGO_QA.md`). **La prueba real en QA falla** con la pantalla de Mercado Pago *"Algo salió mal… Una de las partes con la que intentas hacer el pago es de prueba"*. El dueño decidió dejarlo en pausa y retomarlo después.

**Qué se descartó (probado el 2026-10-31/11-01):**
- El token es de un usuario de prueba vendedor (`TESTUSER…`, `site_id` MLM) y crea cobros sin problema.
- Nuestra preferencia (JSON del SDK de Java) es idéntica a las que se crearon a mano con `curl`. Se probaron 13 variantes (cada campo suelto, todos juntos, título con raya larga, `installments` 1 y 3, tipos excluidos, vencimiento, `binary_mode`, `back_urls`, `notification_url`): **todas abren la pantalla "¿Cómo quieres pagar?"**.
- La dirección `init_point` que da Mercado Pago es la misma que armábamos a mano (`www.mercadopago.com.mx/checkout/v1/redirect?pref_id=…`).
- No es la sesión del navegador: falla también en ventana privada y sin haber abierto Mercado Pago.
- Dato que quedó sin explicar: el cobro de la app termina en `…/payment-option-form/?&p=<hash>` y las pruebas a mano en `…/payment-option-form/?preference-id=…`.
- El `GET /checkout/preferences/{id}` da **403** con este token (también para los cobros hechos a mano): por eso falla la consulta de la dirección y se usa la de respaldo. No es la causa del error.

**Hipótesis más probable (no confirmada):** el tipo de credencial. Una página de soporte de Mercado Pago (leída solo en el resultado de búsqueda; el dominio de la documentación está bloqueado desde el entorno del asistente) dice que este error suele venir de usar las "credenciales de prueba" o `sandbox_init_point`, y que para probar Checkout Pro hay que crear la preferencia con el **Access Token de producción de la cuenta de prueba vendedora** y pagar con la cuenta de prueba **compradora**.

**Cuando se retome (en este orden):**
1. Pagar un cobro hecho a mano (`curl`) con el comprador de prueba (tarjeta de prueba o "Ingresar con mi cuenta"). Si falla ahí también, es el tipo de credencial/cuenta, no el código.
2. Entrar a Mercado Pago Developers **con el usuario de prueba vendedor** (en ventana privada, nunca con la cuenta real), crear una aplicación ahí y usar **sus credenciales de producción** como `ACCESS_TOKE_MERCADO_PAGO` en `qa`.
3. Si sigue igual: crear un comprador nuevo con `POST /users/test_user` usando el mismo token (usuario y contraseña NO se pegan en chat), o abrir consulta con el soporte de Mercado Pago Developers.
4. Permitir los dominios `www.mercadopago.com.mx` y `www.mercadopago.com` en la red de la sesión del asistente para leer la documentación oficial.

**Pendientes de código (menores):** no consultar la preferencia (403): tomar `init_point` de la respuesta al crearla; dejar en el log la dirección que se entrega al navegador.

**Estado de QA mientras tanto:** las variables `ACCESS_TOKE_MERCADO_PAGO` (token de prueba), `MP_WEBHOOK_SECRET` y `PAGOS_PASARELA_EN_LINEA=checkout` están cargadas en el deployment `proyecto-key-deployment` (namespace `qa`). Para que el pago en línea de QA vuelva a funcionar con la pasarela simulada:
`kubectl set env deployment/proyecto-key-deployment -n qa PAGOS_PASARELA_EN_LINEA=simulada`
(Ojo: el token de la terminal vieja `/v1/mp` en QA también quedó reemplazado por el de prueba.)

**Sigue pendiente de fondo (sin tocar):** terminal real (modelo y device-id), reembolsos reales, tarifas 6/9/12, producción (credenciales, webhook en modo producción, migración `migration_pago_tarjeta.sql`).
