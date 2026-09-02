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
para este que se tiene que tener chequera hay que descartarlo

**Nota importante:** el requisito de OpenPay de ser cliente BBVA con cuenta empresarial es el
único de los tres que depende de una relación bancaria específica — hay que confirmarlo antes de
invertir tiempo en su integración, porque si el negocio no es o no puede ser cliente BBVA, OpenPay
queda descartado sin importar lo demás.

Y aqui lo que estoy revisando y quiero que busques en fuentes confiables y sin enventar, necesito que busques dcual es la forma de hacer el negocio los pagos, es decir separar las cuentas personales con las del negocio, por ejemplo la ceunta de mercado pago que tengo para la aplicacion esta en en la cuenta ya del negocio, la de paypal igual aqui lo que quiero que busques es cual sera la forma de hacer los cobros si no hay algun problema con el sat o con el estado porque no tengo RFC para el negocio tengo rfc yo personalmente persona fisica pero quiero que el negocio sea aparte pero como te digo no tengo dado de alta el negocio para que veas que puede pasar

**Fuentes:**
- Mercado Pago — RFC y cuenta empresa: https://www.mercadopago.com.mx/blog/dar-de-alta-rfc-cuenta-negocio · https://www.mercadopago.com.mx/blog/requisitos-cuenta-empresa-mercado-pago
- OpenPay — requisitos de solicitudes de pago: https://ayuda.openpay.mx/ayuda/cuales-son-los-requisitos-para-utilizar-solicitudes-de-pago/
- OpenPay — guía de validación documental persona moral (PDF oficial): https://public.openpay.mx/web/descargables/2025/guia-validacion-documental-persona-moral.pdf
- PayPal — condiciones de uso: https://www.paypal.com/mx/legalhub/paypal/useragreement-full

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


Entonces como voy a saber cuanto cobran en donde lo puedo buscar para agregar el precio correcto?
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

**Pendiente de decidir:** ¿se bloquea SOLO el pago online fuera de cobertura (el pedido se sigue
tomando, pero se cobra en persona/transferencia), o se bloquea el pedido completo fuera de la
zona de envío? Esto no es solo una decisión de pagos — afecta el checkout general.

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

para cobrarse al cliente como lo hariamos o como se hace' otra cosa hay que inicar con mercado pago y paypal en la aplicacion pero primero quiero ya al final de este doc pomgas ya el resumen final de que vamos a necesitar y hay que poner lo que hace falta para ir revisando


**Fuentes:**
- Mercado Pago: https://www.mercadopago.com.mx/developers/en/docs/checkout-api-orders/refunds-cancellations
- OpenPay: https://documents.openpay.mx/docs/api (sección refund)
- PayPal: https://docs.paypal.ai/reference/api/rest/captures/refund-captured-payment · https://developer.paypal.com/docs/multiparty/issue-refund/

---

## 6. Mockups visuales

Ver artifact publicado por separado con ejemplos de cómo se vería la selección de método de pago
en el checkout (pantalla de selección, tarjetas de MSI, y pantalla de retorno). No es código
final, es para elegir dirección visual antes de construir la pantalla real.
