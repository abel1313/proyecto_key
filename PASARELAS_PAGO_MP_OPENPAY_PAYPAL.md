# Pasarelas de pago: Mercado Pago + OpenPay + PayPal — 2026-09-02

**Rama:** `feature/pasarelas-pago` (backend y frontend). Todo lo de pagos vive aquí — nada de esta
rama se mezcla con `dev` hasta que se decida que está lista, y ningún cambio que no sea de pagos
se hace en esta rama. Ver regla de "feature que no llega a main junto con el resto" en `CLAUDE.md`.

**Objetivo de este documento:** información verídica (buscada en internet, no inventada) para las
3 pasarelas elegidas — qué se necesita para darlas de alta, cómo funcionan sus MSI, cómo se
implementan técnicamente de punta a punta, cómo restringir el pago online por zona de entrega, y
cómo funcionan los reembolsos en cada una. Todavía no hay código de pagos escrito — es la base
para decidir el diseño antes de programar.

---

## 1. Requisitos para dar de alta cada cuenta

| | Mercado Pago | OpenPay | PayPal |
|---|---|---|---|
| **Tipo de cuenta** | Persona física o moral | Persona física **con actividad empresarial** o persona moral | Persona física (18+, residente en México) o negocio |
| **RFC** | Requerido, con homoclave completa (13 caracteres persona física, 12 persona moral) | Requerido — "por regulación oficial, para integrar Openpay debes estar dado de alta ante el SAT" | No exigido para abrir cuenta personal; sí para cuenta business |
| **Documento fiscal** | Constancia de Situación Fiscal actualizada (se descarga del portal del SAT) | Constancia de situación fiscal expedida en los últimos 6 meses + Pasaporte o INE | — |
| **Requisito particular** | Ninguno adicional relevante | **Debe ser cliente BBVA con cuenta de cheques empresarial** — esto es un filtro real, hay que confirmar si el negocio ya tiene o puede abrir una | Ninguno adicional para cuenta básica |
| **Costo de alta** | Gratis | Gratis (aprobación sujeta a revisión de OpenPay) | Gratis |

> 💬 **Tu comentario:** "para este que se tiene que tener chequera hay que descartarlo"
>
> **✅ Decidido: OpenPay queda descartado.** De las 3 pasarelas investigadas quedan **Mercado Pago
> y PayPal**. No se vuelve a mencionar OpenPay en el resto de este documento salvo como referencia
> histórica de por qué se descartó.

**Nota importante (histórica):** el requisito de OpenPay de ser cliente BBVA con cuenta empresarial
era el que lo descartó — no depende de nosotros, depende de una relación bancaria que no se tiene.

### 1.1 Separar cuentas personales del negocio sin tener RFC de negocio — qué dice el SAT

> 💬 **Tu pregunta:** "quiero que busques cuál será la forma de hacer los cobros si no hay algún
> problema con el SAT o con el estado porque no tengo RFC para el negocio, tengo RFC yo
> personalmente persona física pero quiero que el negocio sea aparte pero no tengo dado de alta el
> negocio."

Investigado en fuentes oficiales/especializadas (no inventado). **Aviso: esto es información
general, no es asesoría fiscal formal — para la decisión final conviene confirmarlo con un
contador**, pero esto es lo que encontré:

- **Ya tenés RFC** (persona física), pero por lo que describís (cuentas de MP/PayPal del negocio
  abiertas con tu RFC personal, sin haber dado de alta actividad empresarial) el RFC probablemente
  está registrado en un régimen que **no cubre ingresos por actividad empresarial** (ej. régimen de
  sueldos/asalariados). El dinero que entra por la tienda es, a ojos del SAT, ingreso por actividad
  empresarial — y recibirlo bajo un RFC sin ese régimen dado de alta es una **actividad no
  registrada** ante el SAT: infracción al Artículo 27 del Código Fiscal de la Federación, con multa
  de **$4,220 a $13,020 MXN** por no registrarse correctamente (Artículo 80 CFF).
- **Desde el 1 de abril de 2026 el SAT tiene acceso en tiempo real a los datos de las plataformas
  digitales** (Mercado Pago, PayPal, etc.) — ya no es "capaz que no se dan cuenta", están viendo
  las operaciones conforme pasan.
- **No hace falta crear una persona moral (empresa aparte) para separar esto.** La vía normal para
  "quiero seguir siendo persona física pero que el negocio esté formalmente separado/declarado" es
  dar de alta (o actualizar) tu mismo RFC con el régimen **RESICO (Régimen Simplificado de
  Confianza)** si tus ingresos anuales no superan $3,500,000 MXN — permite actividad empresarial
  con tasas de ISR reducidas, requiere e.firma y Buzón Tributario activos, y expedir CFDI (factura
  electrónica) por lo que vendas. Es el régimen pensado exactamente para este caso (negocio chico
  operado por una persona física).
- **Consecuencia práctica de NO estar dado de alta correctamente:** las plataformas (MP/PayPal)
  pueden aplicarte retenciones altas por default sobre lo que te depositan (hasta 20% de ISR + 100%
  del IVA cuando no detectan un RFC válido con el régimen correcto) — aparte del riesgo de multa
  del SAT. Dado de alta en RESICO, esas retenciones bajan mucho y quedás del lado correcto.

**Siguiente paso recomendado (no es código, es trámite):** dar de alta actividad empresarial bajo
RESICO en tu RFC actual (trámite en el portal del SAT, gratis) antes de escalar el volumen de
cobros por MP/PayPal — así separás formalmente lo personal de lo del negocio sin necesitar
constituir una empresa aparte.

**Fuentes:**
- Mercado Pago — RFC y cuenta empresa: https://www.mercadopago.com.mx/blog/dar-de-alta-rfc-cuenta-negocio · https://www.mercadopago.com.mx/blog/requisitos-cuenta-empresa-mercado-pago
- PayPal — condiciones de uso: https://www.paypal.com/mx/legalhub/paypal/useragreement-full
- Obligaciones fiscales SAT para ventas digitales 2026: https://base.com/es-MX/blog/obligaciones-fiscales-sat-ecommerce-2026/
- SAT vigilando PayPal y Mercado Pago en tiempo real desde abril 2026: https://www.mibolsillo.com/tips/confirmado-sat-vigila-paypal-y-mercado-pago-en-2026-transferencias-bajo-revision-fiscal-evita-multas-20260115-0025.html
- Persona física con actividad empresarial (SAT oficial): https://www.sat.gob.mx/portal/public/personas-fisicas/pf-actividades-empresariales-y-profesionales
- RESICO 2026 — requisitos y cómo darte de alta: https://idconline.mx/fiscal-contable/2026/01/14/resico-2026-requisitos-para-que-personas-fisicas-se-incorporen-o-cambien-de-regimen · https://finanzasactivas.com/resico/

---

## 2. Meses sin intereses (MSI)

| | Mercado Pago | OpenPay | PayPal |
|---|---|---|---|
| **Plazos** | Configurables por el vendedor (ej. hasta 6, 9, 12 meses) | 3, 6, 9, 12, 18 o 24 meses, según monto mínimo de compra por plazo | 3 a 24 meses (4 y 24 meses solo con tarjetas Banamex) |
| **Quién paga la comisión de MSI** | El vendedor — comisión adicional que varía según el número de meses elegido por el comprador | El vendedor — comisión adicional sobre la tasa base, "sujeta a análisis y aprobación previa de OpenPay" | El vendedor — comisión porcentual adicional aplicada a cada mensualidad |
| **Tarjetas que aplican** | Crédito Visa/Amex/Mastercard de bancos específicos (varía) | Según convenio de OpenPay con cada banco | Lista larga de bancos con convenio (Amex, Banamex, Banorte, BBVA, HSBC, Santander, Nu, BanCoppel, Banco Azteca, Inbursa, Scotiabank, y más) |
| **Cómo se activa** | Panel de cuenta → Gestión → Comisiones y MSI → medio de cobro → "Ofrecer MSI" | Se configura en el dashboard de OpenPay al dar de alta el método de pago | Se activa desde el panel de negocio de PayPal, requiere que el saldo de PayPal del comprador esté en $0 (si no, usa ese saldo antes que la tarjeta y no aplica MSI) |
| **% exacto de comisión** | **No es público** — solo visible dentro de la cuenta ya dada de alta | **No es público** — "podrían diferir de las tasas publicadas en el sitio", sujeto a aprobación | **No es público** en cifra exacta, se suma a la comisión base por transacción |

**Conclusión práctica:** ninguna de las 3 publica el % exacto de la comisión de MSI en internet —
solo se ve dentro del panel de cada cuenta ya creada. Para decidir cuál ofrece mejor MSI hay que
dar de alta las 3 cuentas (gratis) y comparar dentro de cada panel antes de elegir cuál activar.

**Fuentes:**
- Mercado Pago MSI: https://www.mercadopago.com.mx/ayuda/mensualidades-sin-intereses_2255 · https://www.mercadopago.com.mx/blog/activar-meses-sin-intereses-checkout-mercado-pago
- OpenPay MSI: https://www.klar.mx/tarjeta-meses-sin-intereses/openpay · https://www.openpay.mx/comisiones
- PayPal MSI: https://www.paypal.com/mx/business/accept-payments/checkout/installments · https://www.paypal.com/mx/brc/article/installments-merchant

> 💬 **Tu pregunta:** "¿cómo voy a saber cuánto cobran, en dónde lo puedo buscar para agregar el
> precio correcto?"
>
> Como la cuenta de Mercado Pago que ya usás para Point es la del negocio, entrás directo:
> **Mercado Pago → Tu negocio → Configuración → Comisiones y MSI** (mismo menú que activa el
> "Ofrecer MSI" mencionado arriba) — ahí aparece el % exacto por número de meses, ya calculado para
> tu cuenta. Para PayPal es el equivalente dentro del **Panel de negocio de PayPal → Tarifas**. No
> hay atajo público — hay que entrar a cada panel una vez tengas la cuenta lista para cobros online
> (la de Point ya la tenés; falta dar de alta PayPal si no está).

---

## 3. Implementación técnica completa (de punta a punta)

### 3.1 Mercado Pago — Checkout Pro

1. **Alta de aplicación** en el panel de developers → obtener `public key` + `access token` (ya
   se tiene cuenta, se usa hoy para Point — ver `INVESTIGACION_NUEVAS_FEATURES_2026-09-02.md`
   sección 2.1 para lo que ya existe).
2. **Backend:** crear una `Preference` (SDK Java o REST directo) con los ítems del pedido,
   `back_urls` (success/failure/pending) y `notification_url` (webhook). Devuelve un `init_point`
   — la URL a la que se redirige al cliente.
3. **Frontend:** al confirmar el pedido, llamar al endpoint nuevo del back que crea la
   `Preference`, y redirigir (`window.location.href`) al `init_point` recibido.
4. **Webhook nuevo** (distinto del que ya existe para Point): recibe notificaciones de tipo
   `payment` y `merchant_order` en `POST /v1/mp/checkout/webhook` (ruta a definir) — hay que leer
   el `payment_id` de la notificación y consultar `GET /v1/payments/{id}` para confirmar el estado
   real (nunca confiar en el body del webhook a ciegas, es solo un aviso de "algo cambió").
5. **Pantalla de retorno:** una ruta en el front (`/pago/resultado` o similar) que lee los query
   params que Mercado Pago agrega a las `back_urls` (`payment_id`, `status`, `merchant_order_id`)
   y muestra éxito/pendiente/fallo, y actualiza el estado del `Pedido`.
6. **Estado del pedido:** agregar campo de estado de pago (pendiente/pagado/fallido) ligado al
   `Pedido`, distinto de `estadoPedido` (que es logístico: pendiente/entregado/cancelado).

### 3.2 OpenPay

1. **Alta y credenciales:** `MERCHANT_ID` + llaves pública/privada desde el dashboard de OpenPay
   (sandbox primero: `sandbox-dashboard.openpay.mx`).
2. **Backend:** usar el SDK Java de OpenPay (o REST directo) para crear el cargo —
   `POST /v1/{MERCHANT_ID}/charges` con el método de pago (tarjeta tokenizada desde el front, o
   redirección a su formulario hospedado).
3. **Frontend:** OpenPay ofrece un formulario de tarjeta que tokeniza los datos ANTES de que
   lleguen al backend (con su JS SDK) — el back nunca ve el número de tarjeta completo, solo el
   token, que es el patrón correcto de seguridad/PCI.
4. **Webhooks:** configurarlos desde el dashboard de OpenPay (Perfil de comercio → Webhooks →
   "+Add Webhook"), apuntando a un endpoint HTTPS propio. Reintenta la entrega hasta recibir una
   respuesta exitosa — el endpoint debe responder 200 rápido y procesar async si hace falta.
5. **Estado del pedido:** igual que MP, un campo de estado de pago propio ligado al `Pedido`.

### 3.3 PayPal — Orders API v2

1. **Alta de app** en developer.paypal.com → `client ID` + `secret`.
2. **Frontend:** cargar el JS SDK de PayPal (`<script src="https://www.paypal.com/sdk/js?...">`)
   con el `client ID`, renderizar los botones de PayPal en la pantalla de checkout.
3. **Backend — crear orden:** endpoint propio que llama a `POST /v2/checkout/orders` de PayPal
   con el monto y los ítems, devuelve el `orderID` al front.
4. **Backend — capturar pago:** cuando el cliente aprueba en los botones de PayPal, el front
   llama a un endpoint propio que hace `POST /v2/checkout/orders/{orderID}/capture` — ahí es
   cuando el dinero realmente se mueve.
5. **Webhook:** registrar la URL en el Developer Dashboard, escuchar
   `CHECKOUT.ORDER.APPROVED` (para disparar la captura si no se hizo ya del lado del front) y
   `PAYMENT.CAPTURE.COMPLETED`/`PAYMENT.CAPTURE.DENIED` para confirmar el resultado final.
   **Verificar la firma del webhook** — PayPal expone un endpoint de verificación
   (`POST /v1/notifications/verify-webhook-signature`) para confirmar que la notificación es
   legítima antes de actuar sobre ella.
6. **Estado del pedido:** igual patrón que los otros dos.

### 3.4 Lo común a los 3 (para no repetir 3 veces el mismo diseño)

- Un campo de **estado de pago** en `Pedido` (o una tabla aparte tipo `PagoOnline`), independiente
  del `estadoPedido` logístico que ya existe.
- Un **selector de método de pago** en el checkout que solo aparece si la zona de entrega tiene
  cobertura (ver sección 4).
- Un **webhook por proveedor** — no se puede compartir uno solo entre los 3, cada uno tiene su
  propio formato de notificación y su propia forma de verificar autenticidad.
- Nunca confiar en el estado que reporta el navegador del cliente al volver del checkout — siempre
  reconfirmar contra la API del proveedor (`GET` al pago/orden) antes de marcar el pedido como
  pagado.

---

## 4. Restricción de pago online por zona de entrega

**Lo que ya existe en el código (confirmado, no es que haya que construirlo desde cero):**
- `LugarEntrega` ya tiene **coordenadas** (`latitud`/`longitud`, el centroide de una zona) y
  **costo de envío**.
- `LugarEntregaAnillo` ya modela **círculos concéntricos de cobertura** por radio en metros
  (`radioMetros`, `costoEnvio`, `orden`) — es decir, el sistema YA sabe calcular "¿este punto
  cae dentro del área que cubrimos?" vía `POST /v1/lugares-entrega/{id}/calcular-costo`.
- `SelectorUbicacionComponent` (mapa Leaflet+OpenStreetMap) ya deja al cliente marcar su punto
  exacto, y `AnillosEditorComponent` ya deja al admin dibujar esos anillos de cobertura.

**El único problema:** hoy todo esto **solo se usa en el módulo de flores eternas**. El checkout
general de la tienda (ropa/bolsas) no pide `LugarEntrega` ni valida cobertura — el cliente escribe
su dirección en texto libre y ya. Está documentado explícitamente en el código
(`lugar-entrega.model.ts`): *"ningún flujo del checkout normal de la tienda lee este campo — y no
debe empezar a leerlo sin que el dueño lo pida"*. Como ahora sí lo estás pidiendo (para pago
online), esto se puede activar.

**Diseño propuesto (dos opciones, a decidir):**

1. **Reusar `calcular-costo` tal cual existe** — en el checkout general, el cliente marca su
   punto en el mismo `SelectorUbicacionComponent` ya construido; el back llama a
   `calcular-costo` y, si el punto NO cae dentro de ningún anillo de ningún `LugarEntrega`, el
   pago con pasarela online no se ofrece (se sigue permitiendo pago contra entrega/transferencia
   si el negocio decide igual surtir ese pedido fuera de su zona normal, o se bloquea el pedido
   entero — a decidir).
2. **Agregar un campo explícito** `zonaConCobertura`/`envioDisponible` (booleano) en
   `LugarEntrega`, más simple de razonar para el admin ("esta zona sí/no tiene envío") pero
   duplica lo que los anillos ya casi resuelven por distancia.

**Recomendación:** la opción 1 (reusar el sistema de anillos) es más precisa —cubre "cerca pero
fuera del radio"— y no duplica lógica ya construida. La opción 2 es más simple de explicar pero
menos exacta. Se puede combinar: anillos para el cálculo fino, y un flag manual de emergencia por
si el admin quiere apagar una zona completa sin tocar los anillos uno por uno.

> 💬 **Tu comentario (2026-09-03):** "hay que revisar en las pasarelas que los cobros solo se
> puedan hacer en las zonas que solo aceptamos o los que vamos a hacer las entregas."
>
> **✅ Decidido:** si el punto del cliente NO cae en ninguna zona de cobertura (ningún
> `LugarEntrega`/anillo), **se bloquea el pedido completo, no solo el pago online** — tus palabras:
> "todos los pagos, porque solo llevamos a las zonas que agregamos". Tiene sentido: si no
> repartimos ahí, tampoco tiene caso tomar el pedido para pago en persona o transferencia, porque
> físicamente no se le va a poder entregar. Queda **opción 1** de arriba (reusar `calcular-costo`)
> pero sin la rama de "se sigue permitiendo pago contra entrega/transferencia" — fuera de zona, el
> checkout general simplemente no deja generar el pedido.

---

## 5. Reembolsos

| | Mercado Pago | OpenPay | PayPal |
|---|---|---|---|
| **Plazo para reembolsar** | Hasta 180 días desde la aprobación del pago | No se encontró un plazo publicado explícito — confirmar dentro del dashboard | Hasta 180 días desde la transacción (plazo default) |
| **Endpoint / mecanismo** | Cancelación (pago aún no aprobado): `PUT /v1/payments/{payment_id}` · Reembolso (ya aprobado): API de reembolsos sobre la orden | `POST /v1/{MERCHANT_ID}/charges/{TRANSACTION_ID}/refund` (o bajo `/customers/{id}/charges/.../refund` si es cargo a cliente registrado) | `POST /v2/payments/captures/{CAPTURE_ID}/refund` — permite reembolsos parciales, se puede llamar varias veces hasta cubrir el total capturado |
| **Requisito de saldo** | Debe haber saldo suficiente en la cuenta de Mercado Pago para cubrir el reembolso | No especificado en la documentación pública | No especificado explícitamente, pero el monto se descuenta de la cuenta PayPal del negocio |
| **¿Se devuelve la comisión cobrada?** | No — la comisión original no se regresa | No especificado, asumir que no (patrón estándar en la industria) | **No** — confirmado explícitamente: "las fees que originalmente pagaste como vendedor no se te regresan" |
| **Reembolso parcial** | Sí, soportado | Sí, con parámetro de monto opcional | Sí, múltiples parciales hasta el total |

**Implicación de negocio importante:** en las 3, si el negocio hace un reembolso, **pierde la
comisión que ya pagó por esa venta** — es un costo real a considerar en la política de
devoluciones (ej. si el negocio decide absorber ese costo o cobrárselo al cliente en casos de
devolución por cambio de opinión vs. error del negocio).

> 💬 **Tu pregunta:** "para cobrarse al cliente [la comisión perdida] como lo haríamos o cómo se
> hace" — y en `QA_ROADMAP_2026-09-02.md` sección 7 ya habías dejado dicho: "hay que implementar
> que si cancela después de pagar se le va a cobrar [algo] al cliente."
>
> **Cómo se hace en la práctica (ninguna pasarela te deja "cobrar solo la comisión" como
> transacción propia):** no existe un endpoint de "cárgale la comisión al cliente" — la forma real
> es que el reembolso que le hacés al cliente sea **por el total menos la comisión que perdiste**,
> usando el reembolso PARCIAL que las 3 pasarelas soportan (`amount` en el request, sección de
> arriba). Ejemplo: pedido de $500, comisión ya cobrada $17.40 (3.48% MP) → si el cliente cancela
> por su cuenta después de pagar, se le reembolsan $482.60 en vez de $500. El cliente ve reflejado
> menos dinero del que pagó — eso ES "cobrarle" la comisión, no hay un cargo aparte.
>
> **Pendiente de decidir (política de negocio, no técnico):** ¿esto aplica siempre que el cliente
> cancela después de pagar, o solo si la cancelación es por decisión del cliente (no por un error
> del negocio, producto agotado, etc.)? Esto también hay que reflejarlo en el texto de Términos y
> condiciones (sección 7 del roadmap) para que el cliente lo sepa ANTES de pagar, no se entere al
> momento de cancelar.

**Fuentes:**
- Mercado Pago: https://www.mercadopago.com.mx/developers/en/docs/checkout-api-orders/refunds-cancellations
- PayPal: https://docs.paypal.ai/reference/api/rest/captures/refund-captured-payment · https://developer.paypal.com/docs/multiparty/issue-refund/

---

## 6. Mockups visuales

Ver artifact publicado por separado con ejemplos de cómo se vería la selección de método de pago
en el checkout (pantalla de selección, tarjetas de MSI, y pantalla de retorno). No es código
final, es para elegir dirección visual antes de construir la pantalla real.

---

## 7. Resumen final — qué ya tenemos y qué falta para avanzar

> 💬 **Tu pedido:** "quiero ya al final de este doc pongas ya el resumen final de que vamos a
> necesitar y hay que poner lo que hace falta para ir revisando."

### ✅ Ya resuelto / decidido
- ✅ Comparadas las 3 pasarelas candidatas (requisitos, MSI, implementación técnica, reembolsos).
- ✅ **OpenPay descartado** — requiere ser cliente BBVA empresarial, no aplica.
- ✅ Cuenta de Mercado Pago del negocio ya existe y ya está en uso (Point) — no hay que crearla de
  cero para Checkout Pro, es la misma cuenta.
- ✅ Investigado qué pasa fiscalmente por no tener el negocio dado de alta ante el SAT (sección
  1.1) — camino recomendado: RESICO sobre tu RFC actual.
- ✅ El sistema de zonas de cobertura (`LugarEntrega`/`LugarEntregaAnillo`/anillos) ya existe en
  código y se puede reusar para restringir pago online por zona (sección 4) — no hay que
  construirlo desde cero, solo conectarlo al checkout general.
- ✅ Confirmado: en las 3 pasarelas, al reembolsar se pierde la comisión — y cómo trasladar ese
  costo al cliente es vía reembolso parcial (sección 5).
- ✅ Mockups visuales del checkout ya publicados (sección 6).
- ✅ **Zona sin cobertura → se bloquea el pedido completo** (todas las formas de pago), no solo el
  pago online (sección 4).
- ✅ **Flag de MSI es sí/no simple** en `Producto` y en `Promocion` (con herencia de Producto a
  Variante, no configurable por número de meses) — sección 8.

### ⬜ Falta decidir (negocio, no requiere código todavía)
- ⬜ **Confirmar con un contador** el trámite de RESICO/actividad empresarial antes de escalar
  cobros — recomendado no arrancar en producción con volumen real hasta resolver esto.
- ⬜ Dar de alta la cuenta de **PayPal Business** (si no está ya) para poder comparar su % de MSI
  contra el de Mercado Pago dentro del panel.
- ⬜ Decidir la política exacta de "se cobra la comisión perdida al cliente": ¿siempre que cancele
  después de pagar, o solo si la cancelación es por su decisión (no por error/falta de stock del
  negocio)? Esto debe quedar escrito en Términos y condiciones antes de activarlo.
- ⬜ Decidir de dónde sale el horario de entrega a domicilio en el checkout general (¿lo agenda el
  admin a mano, o se calcula por zona/ruta como en flores eternas?) — sección 9.

### ⬜ Falta construir (una vez tomadas las decisiones de arriba)
- ⬜ Campo de **estado de pago** en `Pedido` (independiente del `estadoPedido` logístico).
- ⬜ Endpoint + `Preference` de **Mercado Pago Checkout Pro** (back) y redirección al `init_point`
  (front) — sección 3.1.
- ⬜ Integración de **PayPal Orders API v2** (crear orden + capturar) — sección 3.2.
- ⬜ Webhook propio por pasarela (uno para MP Checkout Pro, uno para PayPal) — no se comparte con
  el webhook de Point que ya existe.
- ⬜ Pantalla de retorno del checkout (`/pago/resultado` o similar) que confirma el estado contra la
  API del proveedor, nunca contra lo que dice la URL de vuelta a ciegas.
- ⬜ Activar la validación de zona de cobertura en el checkout general de la tienda (hoy solo
  flores eternas la usa).
- ⬜ El endpoint de reembolso parcial (uno por pasarela) para cuando se decida automatizar la
  devolución (hoy es manual, ver también sección 10 de `QA_ROADMAP_2026-09-02.md` para el caso de
  Point).
- ⬜ Ver sección 8 (nueva, 2026-09-03): flag de elegibilidad de MSI por Producto/Variante/Promoción.
- ⬜ Ver sección 9 (nueva, 2026-09-03): aviso de horario de recolección/entrega tras generar pedido.

---

## 8. Elegibilidad de "meses sin intereses" por Producto/Variante y por Promoción

> 💬 **Tu pedido (2026-09-03):** "para mercado pago hay que agregar una opción en la cual si está
> activado ese producto en productos que las variantes deben heredarlo... si está activado es que
> se dejaría hacer el pago con tarjeta a meses, con eso solo si tiene la opción lo puede hacer
> todas las variantes porque en el producto se agregó esa opción, lo mismo para las promociones,
> pero el pago con tarjeta o crédito sí se podrían hacer pero a 1 solo mes... y además agregarlo
> para la forma de pago en línea igual que tenga o sirva para saber si se puede pagar a 3 meses,
> igual el checkout porque si se va a poder hacer el pago en línea pero solo a 1 pago con
> tarjetas."

**Diseño según lo que describís (confirmando que entendí bien):**
- Es un flag **por Producto**: `permiteMesesSinIntereses` (o el nombre que prefieras), no por
  Variante — **las variantes lo heredan de su producto, no se configura variante por variante**.
  Si el producto lo tiene activado, todas sus variantes quedan elegibles para MSI; si no, esas
  variantes solo aceptan tarjeta/crédito a **1 pago** (de contado, sin cuotas).
- **Independiente, mismo patrón en Promoción** — un flag propio en `Promocion`, no heredado del
  producto ni al revés (una promoción puede activar MSI aunque el/los productos que agrupa no lo
  tengan marcado individualmente, y viceversa).
- El checkout (pago en línea) lee este flag al armar el cobro: si CUALQUIER línea del carrito no
  es elegible, esa línea (o el pedido completo, según se decida) se limita a 1 pago; si todo es
  elegible, se ofrece la opción de meses.

**Esto encaja con la API real de Mercado Pago** (no es solo un flag decorativo): la `Preference`
de Checkout Pro (sección 3.1) acepta `payment_methods.installments` — ahí se manda el máximo de
cuotas permitido PARA ESA preferencia específica. Si el flag da "no elegible", se crea la
`Preference` con `installments: 1` (fuerza pago de contado); si da "elegible", se manda el máximo
que la cuenta tenga configurado (o el tope que quieras, ej. 3). Es decir, esto se resuelve al
armar cada `Preference`, no hace falta configurarlo dentro del panel de Mercado Pago por producto.

**✅ Decidido (2026-09-03):**
- **El flag es sí/no simple**, no configurable por número de meses — un booleano en `Producto` y
  otro en `Promocion`. El número exacto de meses que se ofrecen (3/6/9/12) lo sigue controlando la
  cuenta de Mercado Pago/PayPal desde su propio panel; nuestro flag solo decide si ese producto o
  promoción entra a MSI o se limita a 1 pago.
- **Todavía NO se programa el modelo de datos** — dijiste "esperar" a decidir el resto del diseño
  de pagos (sección 7) antes de tocar entidades, para no tener que ajustarlo dos veces. Queda
  anotado aquí listo para construirse cuando lo pidas.

---

## 9. Aviso de horario al cliente: recoger en tienda vs. entrega a domicilio

> 💬 **Tu pedido (2026-09-03):** "recuerda que si hacen pedido y pagan o no pagan, nosotros le
> avisamos: si pasa al local puede pasar cuando esté abierto, pero si se va a llevar a domicilio
> entonces le tenemos que avisar cuándo lo llevaríamos a su lugar."

Esto **no depende de si el pedido se pagó en línea o no** — aplica a cualquier pedido, ligado a
si es para **recoger en tienda** o **entrega a domicilio**:
- **Recoger en tienda:** el correo de confirmación de pedido (ya existe, `QA_ROADMAP` sección 4)
  solo necesita recordarle el **horario de atención del negocio** (ya configurado en
  `ConfiguracionNegocio` — Estado/Horario/Contactos) — no hace falta un horario específico por
  pedido.
- **Entrega a domicilio:** el correo tiene que decirle **cuándo se le va a llevar** — un horario o
  ventana de entrega concreta para ESE pedido, no el horario general del negocio.

**Lo que ya existe y se puede reusar:** el módulo de flores eternas ya maneja logística de entrega
por pedido (`entregas`, con lugar/anillo de cobertura). Lo que falta es: (a) que el checkout
general (ropa/bolsas) también capture si el pedido es para recoger o a domicilio — hoy, según
dejaste anotado en `QA_ROADMAP_2026-09-02.md`, no está claro si el checkout general debería
mostrar la opción de "lugar de entrega" o no — y (b) definir de dónde sale el horario de entrega
para un pedido a domicilio del checkout general (¿lo agenda el admin manualmente al procesar el
pedido, o se calcula automático por zona/ruta como en flores?).

---

## 10. Implementación de Mercado Pago Checkout Pro y PayPal — 2026-09-03

### ✅ Lo que ya se construyó (back y front)

**Backend:**
- `PagoOnline` — tabla unificada de seguimiento de pagos online (decidida en esta sesión):
  `proveedor` (`MP_CHECKOUT`/`PAYPAL`), `pedidoId`, `clienteId`, `referenciaExterna` (preference
  id / order id), `pagoIdExterno` (payment id / capture id), `monto`, `estado`
  (`CREATED`/`APPROVED`/`REJECTED`/`CANCELLED`/`REFUNDED`). Migración:
  `migration_pago_online.sql` (pendiente de correr en QA/prod, igual que las demás).
- `MercadoPagoCheckoutService` — Checkout Pro (SDK que ya estaba en el proyecto, `sdk-java`, sin
  dependencia nueva): crea la `Preference`, guarda el `PagoOnline`, y el webhook
  (`POST /v1/mp/checkout/webhook`, público) confirma consultando directo a la API de MP (nunca se
  confía en el body del webhook a ciegas).
- `PayPalCheckoutService` — Orders API v2 con el **SDK oficial** (`com.paypal.sdk:checkout-sdk`,
  agregado al `pom.xml`, decidido sobre REST directo). Crea la orden y expone `capturarOrden`
  — a diferencia de MP, PayPal **no confirma solo con un webhook** en este flujo simple: hay que
  capturar la orden explícitamente cuando el cliente vuelve aprobada (lo dispara el front).
- `PedidoServiceImpl.confirmarPagoOnline` — cuando cualquiera de las dos pasarelas confirma el
  pago, marca el pedido como `PAGADO` y genera la `Venta` bajo el catálogo `TARJETA` existente
  (decidido en esta sesión: no se crean renglones nuevos en `pagos_y_meses` por pasarela, ver
  sección 8 de este mismo doc).
- Seguridad (`SecurityConfig`): `POST /v1/mp/checkout/preference/{id}` y `/v1/paypal/**` requieren
  sesión (cualquier cliente logueado, el service valida que el pedido sea suyo);
  `POST /v1/mp/checkout/webhook` es público (MP llama desde afuera, mismo criterio que el webhook
  de Point que ya existía).

**Frontend:**
- `PagoService` (`pedidos/pago.service.ts`) — `crearPreferenceCheckoutMP`, `crearOrdenPaypal`,
  `capturarOrdenPaypal`.
- Botón **"Pagar en línea"** en "Mis pedidos" (visible solo al propio cliente, no al admin, y solo
  si el pedido no está ya pagado) — pregunta Mercado Pago o PayPal y redirige
  (`window.location.href`) a la pasarela elegida.
- `PagoResultadoComponent` (`/pago/resultado`, ruta pública) — pantalla a la que regresa el
  navegador tras pagar. Para PayPal dispara la captura automáticamente (usa el `token` que PayPal
  agrega solo a la URL); para MP solo muestra el mensaje, la confirmación real ya llegó por
  webhook.

### ⚠️ Qué falta para poder probar — credenciales y dónde conseguirlas

**Mercado Pago Checkout Pro: NO hace falta ninguna credencial nueva.** Usa el mismo
`mercadopago.access-token` que ya está configurado y en uso para Point (`ACCESS_TOKE_MERCADO_PAGO`
en QA/prod) — es la misma cuenta, solo un endpoint distinto de la misma API. Con eso ya alcanza
para probar en sandbox (`mercadopago.sandbox: true` en QA, ya está así).

**PayPal: SÍ hace falta dar de alta la app y conseguir credenciales — todavía no existen.**
1. Entra a **[developer.paypal.com](https://developer.paypal.com/api/rest)** e inicia sesión con
   tu cuenta de PayPal (o créala si no tienes una — no hace falta que sea "Business" todavía para
   sacar credenciales de **sandbox**, pero sí para las de **producción** más adelante).
2. En el **Developer Dashboard**, activa el toggle **Sandbox** (arriba) y ve a
   **Apps & Credentials**.
3. Dale **Create App** → ponle un nombre (ej. "Novedades Jade") → tipo **Merchant** → **Create App**.
4. Ahí mismo vas a ver el **Client ID** y el **Secret** (dale clic a "Show" para verlo) — esas son
   las credenciales de **sandbox**, sirven para probar sin mover dinero real.
5. Copia esos dos valores y agrégalos como variables de entorno en el ambiente de QA:
   - `PAYPAL_CLIENT_ID`
   - `PAYPAL_CLIENT_SECRET`
   (ya están referenciadas en `application-qa.yml`/`application-dev.yml`/`application-docker.yml`
   — solo falta que las variables de entorno existan con el valor real).
6. Cuando quieras probar/lanzar en **producción**, cambia el toggle a **Live** en el mismo
   dashboard y repite el paso 3-4 — esas credenciales solo funcionan si la cuenta de PayPal detrás
   ya es una cuenta **Business** verificada (ver sección 1 de este doc, "Dar de alta la cuenta de
   PayPal Business" sigue pendiente en la lista de tareas).

**Antes de probar en QA, correr la migración a mano** (mismo criterio que siempre, `ddl-auto: none`):
```sql
-- contenido completo en migration_pago_online.sql
CREATE TABLE pago_online ( ... );
```

### Ruta de clics para probar (una vez con las credenciales de PayPal)

1. Como cliente, genera un pedido (que quede con stock/detalles válidos).
2. Ve a **"Mis pedidos"** → en la card del pedido pendiente, botón **"Pagar en línea"**.
3. Elige **Mercado Pago** → te lleva al checkout de MP (sandbox) → paga con una
   [tarjeta de prueba](https://www.mercadopago.com.mx/developers/es/docs/checkout-pro/additional-content/your-integrations/test/cards)
   → debe volver a `/pago/resultado?estado=success` y, unos segundos después (vía webhook), el
   pedido debe pasar a **PAGADO** en "Mis pedidos".
4. Repite el pedido de prueba y esta vez elige **PayPal** → inicia sesión con una
   [cuenta de sandbox de PayPal](https://developer.paypal.com/dashboard/accounts) (comprador de
   prueba) → aprueba el pago → debe volver a `/pago/resultado?estado=success`, capturar solo, y el
   pedido debe pasar a **PAGADO** de inmediato (no depende de webhook).
5. Verifica en ambos casos que se generó la **Venta** correspondiente (reportes/historial) bajo el
   método "TARJETA".

---

## 11. Reembolso de pagos online — respuesta a "¿se regresa el dinero Y el producto?"

> 💬 **Tu pregunta (2026-09-03):** "hay que hacer que se regrese el dinero si hay devolución [...]
> qué pasa si se lleva el producto y después cancela el producto, ¿se le regresaría el dinero y
> además el producto?"

**Construido, y diseñado a propósito en DOS pasos separados — no uno automático:**

1. **Cancelar el pedido** (ya existía, `PedidoServiceImpl.deletePedidoById`) — si el pedido ya
   estaba "Entregado"/"PAGADO", cancelarlo **ya era, y sigue siendo, una acción exclusiva de
   ADMIN** (regla que ya estaba en el código). Ahí se regresa el stock y la Venta pasa a
   "Devuelta". **Esto asume que el admin ya verificó que el producto físico regresó** — el sistema
   no tiene forma de comprobar eso por sí solo, es un paso operativo humano, no algo que el
   software pueda garantizar.
2. **Reembolsar el dinero** (nuevo, `POST /v1/pagos-online/{pedidoId}/reembolsar`, botón
   "Reembolsar" en Mis Pedidos admin) — **es un segundo clic aparte, deliberadamente NO
   automático al cancelar**. El back además **exige que el paso 1 ya haya pasado** (rechaza si el
   pedido no está cancelado todavía) — no se puede reembolsar sin haber cancelado primero.

**Por qué así y no automático:** justo para evitar el riesgo que preguntaste — que cancelar solo
(sin que el producto haya vuelto de verdad) dispare el dinero de vuelta también. Como cancelar un
pedido ya entregado sigue siendo ADMIN-only, y reembolsar es un botón aparte que un admin tiene
que apretar a propósito, nunca pasa "solo" con que el cliente pida cancelar — necesita que un
humano confirme la devolución física primero (cancelar) y decida reembolsar después.

**Nota:** el reembolso es **total** (no parcial) en esta primera versión, usando `PaymentClient.refund()`
de MP y la API de Payments de PayPal sobre el capture id. El reembolso parcial (por si en algún
momento quieren cobrarle al cliente la comisión perdida, ver sección 5) queda pendiente si hace
falta después.
