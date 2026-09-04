# Investigación de nuevas features — 2026-09-02

**Objetivo:** documento de lectura sobre los temas pedidos el 2026-09-02: pasarela de pago,
reseñas, WhatsApp, correos de recordatorio/seguimiento, alertas de stock y programa de lealtad.
Se actualizó el mismo día para agregar: (a) las URLs oficiales de cada pasarela de pago
investigada, y (b) el registro de lo que SÍ se implementó ese día (privacidad + preferencia de
correos + seguimiento de pedido + alerta de stock) con sus pasos de validación. Pago,
WhatsApp y lealtad siguen sin código — son la base para decidir qué construir después.

---

## 0. Cambios implementados el 2026-09-02 (código real, en `dev` local)

Todavía sin subir a `dev`/`qa` remoto — local hasta que se pida explícitamente "sube". Backend
compilado (`mvn compile` OK), frontend verificado (`tsc --noEmit` y `ng build` OK).

### 0.1 Aviso de privacidad al registrarse
- **Qué cambió:** checkbox obligatorio en el registro público (`/usuarios/registrar`) que enlaza
  a `/privacidad` (la pantalla ya existía, no estaba conectada a nada). Se guarda
  `Usuario.aceptoPrivacidad` + `Usuario.fechaAceptoPrivacidad` en el momento del registro.
  Validado en dos capas: `@AssertTrue` en `RegistroRequest` (rechaza la petición con 400 si
  falta) y un chequeo defensivo igual dentro de `RegistroService.registrarUsuario(...)`.
- **Archivos:** `entity/Usuario.java`, `models/RegistroRequest.java`, `service/RegistroService.java`,
  `controller/AuthController.java` (back) · `add-usuarios.component.ts/.html/.scss` (front).
- **Cómo validarlo:**
  1. Ir a `/usuarios/registrar` (sin sesión iniciada).
  2. Intentar enviar el formulario sin marcar el checkbox → el botón "Registrarse" debe quedar
     deshabilitado.
  3. Marcar el checkbox, completar el registro normal → revisar en BD que
     `usuario_modificacion.acepto_privacidad = 1` y `fecha_acepto_privacidad` tiene fecha/hora.
  4. Click en el link "aviso de privacidad" → debe abrir `/privacidad` en pestaña nueva.
  5. Como ADMIN, entrar a editar OTRO usuario ("Actualizar usuario") → el checkbox no debe
     aparecer (no aplica cuando el admin edita a otro).

### 0.2 Preferencia de correos (`Cliente.recibirCorreos`, default activado)
- **Qué cambió:** endpoint dedicado `PUT /v1/clientes/{id}/preferencias-correo` — separado a
  propósito del guardado general de cliente (`save()`/`update()`), porque ese guardado hace
  merge del objeto completo y cualquier form que no mande el campo lo resetearía al default de
  la clase. El toggle vive en dos pantallas: "Mis datos" del cliente (self-service) y
  `clientes/mostrar/:id` del admin (por cada cliente).
- **Archivos:** `entity/Cliente.java`, `controller/ClienteControllerImpl.java` (preserva el valor
  existente en el guardado general + nuevo endpoint), `service/ClienteServiceImpl.java`,
  `models/PreferenciaCorreoRequest.java` (back) · `cliente.service.ts`, `cliente.model.ts`,
  `mis-datos.component.ts/.html`, `clientes-mostrar.component.ts/.html` (front).
- **Cómo validarlo:**
  1. Loguearse como cliente → "Mis datos" → confirmar que el toggle "Recibir correos..."
     aparece activado por default.
  2. Apagarlo → en la pestaña Network del navegador confirmar `PUT
     .../v1/clientes/{id}/preferencias-correo` con `{"recibirCorreos":false}` → 200.
  3. Recargar la página → el toggle debe seguir apagado (persistencia real, no solo en memoria).
  4. **El caso que importa:** editar cualquier OTRO campo (ej. teléfono) y darle "Guardar
     cambios" (el botón grande del formulario, no el toggle) → recargar → el toggle debe
     **seguir apagado**. Si se prendiera solo, es el bug que la preservación en `save()` evita.
  5. Como ADMIN, ir a Clientes → Buscar → "Ver/Editar" sobre ese mismo cliente → debe verse el
     mismo toggle apagado, y el admin debe poder reactivarlo.
  6. Con la sesión de OTRO cliente (no dueño, no admin), intentar pegarle al endpoint con el id
     de este cliente → debe responder 403.

### 0.3 Correo de seguimiento de pedido
- **Qué cambió:** correo automático cuando un pedido pasa a "Entregado" (confirmación desde
  admin) o a "cancelado". Respeta `Cliente.recibirCorreos` — si está apagado, no se envía nada
  (ni error, simplemente no se llama). No toca el ticket de compra normal, que sigue siendo un
  flujo aparte y siempre activo.
- **Archivos:** `service/PedidoServiceImpl.java` (hook en las dos transiciones de estado),
  `service/EmailService.java` (`enviarSeguimientoPedido`).
- **Cómo validarlo (necesita SMTP real configurado en el ambiente, dev/qa lo tienen):**
  1. Con un cliente de prueba con correo real y `recibirCorreos=true`, hacer un pedido y
     confirmarlo desde el panel admin (el flujo que dispara "Entregado").
  2. Revisar bandeja de entrada → debe llegar "Tu pedido #X — Entregado — Novedades Jade".
  3. Cancelar otro pedido del mismo cliente → debe llegar "Tu pedido #X — cancelado — ...".
  4. Repetir con `recibirCorreos=false` en ese cliente → confirmar/cancelar un pedido → NO debe
     llegar correo, y en el log del backend no debe aparecer ningún warning de envío fallido
     (simplemente no se intenta).

### 0.4 Alerta de "volvió el stock" (Favoritos)
- **Qué cambió:** cuando el admin reabastece una variante (el stock pasa de 0 a más de 0 al
  editarla), se avisa por correo a todos los clientes que la tienen en Favoritos y tienen
  `recibirCorreos=true`. No hay bandera de "ya avisado" en BD — se apoya en que la transición
  0→N solo ocurre una vez por cada ciclo real de agotado→reabastecido, así que no hay reenvíos
  duplicados mientras el stock se mantenga arriba de 0.
- **Archivos:** `service/VarianteServiceImpl.java` (`ajustarStock` ahora detecta la transición,
  `notificarRestock`), `repository/IFavoritoRepository.java` (`findAllByVariante_Id`),
  `service/EmailService.java` (`enviarAlertaStock`).
- **Cómo validarlo:**
  1. Con un cliente de prueba, marcar una variante como favorita.
  2. Como admin, editar esa variante y bajar su stock a 0 → guardar.
  3. Editar de nuevo esa misma variante y subir el stock (ej. a 10) → guardar.
  4. Revisar bandeja de entrada del cliente → debe llegar "¡Ya volvió el stock! — Novedades Jade".
  5. Editar la variante otra vez sin pasar por 0 (ej. de 10 a 15) → **no** debe reenviarse el
     correo (evita duplicados).
  6. Volver a bajarla a 0 y reabastecer de nuevo → **sí** debe reenviarse (es un ciclo nuevo).

### 0.5 Alerta de stock bajo al admin (digest diario)
- **Qué cambió:** a diferencia de la alerta de restock de Favoritos (que dispara por evento en
  `VarianteServiceImpl`), esta es un **barrido diario a las 7:00 a.m.** (`StockBajoScheduler`)
  que revisa todas las variantes habilitadas con stock en o por debajo de un umbral y manda UN
  correo por admin con la lista completa. Se eligió barrido en vez de enganchar cada punto donde
  baja el stock porque hay al menos 5 lugares distintos que lo decrementan (pedidos, venta
  directa, abonos, rifas, ajuste manual de producto) — enganchar los 5 es frágil y fácil de
  romper con un sexto lugar futuro; el barrido los cubre todos sin tocarlos, y de paso sirve de
  recordatorio mientras la variante siga baja (no solo la primera vez).
  El umbral es configurable por el admin (default 5 si nunca se configura) desde
  Sistema > Negocio & Contactos.
- **Archivos:** `entity/ConfiguracionNegocio.java` (+ `umbralStockBajo`), `dto/negocio/NegocioConfigDto.java`,
  `dto/negocio/AlertaStockUpdateDto.java` (nuevo), `controller/NegocioController.java` (`PUT
  /v1/negocio/alertas-stock`), `service/NegocioService.java`, `service/StockBajoService.java`
  (nuevo), `scheduler/StockBajoScheduler.java` (nuevo), `repository/IVarianteRepository.java`
  (`findConStockBajo`), `repository/IUsuarioRepository.java` (`findByRoles_NombreRolAndEnabledTrue`),
  `service/EmailService.java` (`enviarAlertaStockBajo`) (back) · `negocio.service.ts`,
  `config-negocio.component.ts/.html` — nueva sección "📦 Alertas de stock bajo" (front).
- **Cómo validarlo:**
  1. Como ADMIN, ir a Sistema > Negocio & Contactos → sección "Alertas de stock bajo" → debe
     mostrar 5 por default si nunca se configuró.
  2. Cambiar el umbral (ej. a 10) → Guardar → recargar → debe seguir en 10 (persistencia).
  3. Confirmar que hay al menos una variante habilitada con stock ≤ ese umbral (bajarle el stock
     a una de prueba si hace falta).
  4. Ejecutar el barrido a mano sin esperar a las 7 a.m. — desde un cliente REST/consola con el
     bean `StockBajoService` (o esperar a la hora real en un ambiente donde el scheduler esté
     activo) → debe llegar a cada admin con correo real un correo "Aviso de stock bajo (N)" con
     la lista de variantes y su stock actual.
  5. Subir esa variante por encima del umbral → correr el barrido de nuevo → esa variante ya no
     debe aparecer en el correo (o no debe llegar correo si era la única baja).
  6. Si no hay ninguna variante baja, el barrido no debe mandar ningún correo (se evita el "todo
     bien" diario, revisar el log: `StockBajoService: sin variantes en o por debajo del umbral`).

### 0.6 Pendiente antes de desplegar
Correr en las bases de dev/qa:
- `src/main/resources/static/migration_privacidad_preferencias_correo.sql` (`acepto_privacidad`/
  `fecha_acepto_privacidad` en `usuario_modificacion`, `recibir_correos` en `clientes`)
- `src/main/resources/static/migration_umbral_stock_bajo.sql` (`umbral_stock_bajo` en
  `configuracion_negocio`)

Sin esto el backend no arranca limpio contra esas BDs.

---

## 1. Reseñas de producto

**Ya existe, completo y funcionando de punta a punta. No hay que construir nada.**

### Backend (`proyecto_key`)
- Entidad `entity/Resena.java` — tabla `resena`. Relación `cliente` + `variante`
  (`ManyToOne`), con constraint único cliente+variante (una reseña por compra/variante).
  Campos: `calificacion` (Integer, estrellas), `comentario` (TEXT), `fechaCreacion`,
  `respuestaAdmin`/`fechaRespuesta` (el admin puede responder públicamente la reseña).
- `repository/IResenaRepository.java`, `service/ResenaServiceImpl.java`,
  `controller/ResenaController.java`.
- Endpoints bajo `v1/resenas`:
  - `POST` crear
  - `PUT /{id}` editar
  - `DELETE /{id}` eliminar
  - `PUT /{id}/responder` responder como admin
  - `GET /variante/{id}` listar paginado por variante
  - `GET /variante/{id}/resumen` → `ResenaResumenDto` con `promedio` (Double),
    `totalResenas` (Long) y `conteoPorEstrella` (Map<Integer,Long>)
  - `GET /mis-resenas` — reseñas del cliente logueado
- Migraciones: `migration_favoritos_resenas.sql`, `migration_respuesta_resena_historial_acceso.sql`.

### Frontend (`producto_venta_online`)
- `src/app/resenas/models/resena.model.ts`, `src/app/resenas/service/resena.service.ts`.
- Integrado en `variante/detalle-variante`: resumen con estrellas y promedio, listado
  paginado, formulario propio para crear/editar (selector de estrellas), botón eliminar,
  conteo por número de estrellas.

**Conclusión:** feature completa. Si algo no se ve bien en producción es un bug puntual a
revisar, no una feature faltante.

---

## 2. Pasarela de pago

### 2.1 Lo que YA existe en el código

Hay integración con Mercado Pago, pero es **la API de terminal física (Point/mPOS)**, no la de
pago online (Checkout Pro). Son productos distintos de Mercado Pago con SDKs distintos.

**Backend:**
- `pom.xml`: dependencia `com.mercadopago:sdk-java:2.1.24`
- `service/MercadoPagoService.java` + `controller/MercadoPagoController.java` (`/v1/mp/*`):
  usan `PointClient`, `PointPaymentIntentRequest`, `PointStatusPaymentIntent` — cobro con
  terminal física conectada por Bluetooth/USB a un `device-id`.
  - `POST /v1/mp/iniciar`, `GET /v1/mp/estado/{id}`, `POST /v1/mp/webhook`,
    `DELETE /v1/mp/cancelar/{id}`, historiales.
  - El webhook (`procesarWebhook`) espera `type=point_integration_ipn`, específico de Point.
    **No sirve** para las notificaciones de Checkout Pro (`payment`/`merchant_order`).
- Entidad `MpPaymentIntent` (tabla `mp_payment_intent`): `intentId`, `pedidoId`, `clienteId`,
  `monto`, `cuotas`, `estado` (OPEN/FINISHED/CANCELED/ERROR).
- Config: `application-dev.yml` con credenciales de prueba falsas (`access-token: asdasd`,
  `device-id: aaaa`); `application-qa.yml`/`application-docker.yml` usan
  `${ACCESS_TOKE_MERCADO_PAGO}` y `${DEVICE_ID}` por variable de entorno.
  **No existe `application-prod.yml`.**
- Catálogo genérico de formas de pago (no ligado a MP): `TipoPago` (texto libre: "Efectivo",
  "Transferencia", "Tarjeta"), `PagosYMeses`, `DetallePago` (tarifa + IVA de terminal),
  expuesto en `PagosCatalogoController` (`/v1/pagos/tipos-pago`, `/tarifas`, `/iva`,
  `/opciones`, `/opciones-por-tipo/{id}`, `/opciones-estructuradas`). Todo `GET`, sin alta
  desde UI — se inserta manual en BD (ver `PAGOS_ALTA_BACK.md`).
- `Venta.estadoVenta`/`detallePago`/`pagosYMeses`, `Pedido.totalPagado`/`totalPedido`,
  `AbonoPedido.metodoPago` (string libre EFECTIVO/TRANSFERENCIA/TARJETA) — todo pensado para
  cobro presencial o abono/crédito registrado a mano por el negocio.

**Frontend:**
- `pedidos/pago.service.ts` envuelve los mismos endpoints `/v1/pagos/*` y `/v1/mp/*`. Se usa en
  `VentaDirectaComponent` (venta en mostrador) y `HistorialMpComponent` (admin, historial de
  cobros por terminal) — no en el flujo de compra del cliente final.
- Modelos en `pedidos/mis-pedidos/models/IPago.model.ts` reflejan la misma API de Point.
- El flujo de compra del cliente (`flores/configurar/configurar-ramo.component.ts`,
  `services/carrito/carrito.service.ts`, `carrito-variante.service.ts`) termina en
  **crear el Pedido** (`savePedido`) y ya. No hay redirección a ningún checkout de pago, ni
  componente de "pagar en línea": el cobro se hace después, presencial o vía abono/transferencia
  que registra el negocio.
- No existe ningún componente `checkout-pago`, ni SDK JS de Mercado Pago
  (`mercadopago.js`/Checkout Bricks) en el frontend.

**Resumen:** la cuenta y el SDK de MP ya están enchufados, pero solo para cobro presencial con
terminal física. El checkout online (que el cliente pague desde la web sin terminal) **no existe
y hay que construirlo desde cero.**

### 2.2 Qué se necesita para Checkout Pro (pago online)

**Requisitos previos de Mercado Pago** (developers.mercadopago.com.mx):
- Cuenta de vendedor en Mercado Pago (ya la tienen, se usa para Point).
- Credenciales de aplicación: `public key` + `access token`. Hay sandbox con hasta 10 usuarios
  de prueba (expiran a los 60 días de inactividad).
- Para pasar a producción: cumplir requisitos legales/seguridad estándar del país (protección de
  datos del cliente, cumplimiento normativo) — no hay un mínimo de ventas para poder arrancar a
  cobrar online (eso sí aplica para *beneficios* de la terminal Point física, ver abajo).

**Trabajo a construir:**
1. Backend: generar una `Preference` (o `Payment`) vía SDK/API REST de MP y un endpoint que
   devuelva `init_point` para redirigir al cliente. Tarjeta, SPEI y OXXO vienen incluidos en la
   preferencia — no se programa cada método por separado.
2. Un **webhook nuevo** para notificaciones `payment`/`merchant_order` (el actual es solo de
   Point y no aplica aquí).
3. Estado de orden online ligado a `Pedido` (pendiente/pagado/fallido) — no reusar `MpPaymentIntent`
   tal cual, es de Point.
4. Frontend: paso carrito → checkout → redirección a Mercado Pago → pantalla de retorno
   (success/failure/pending).
5. `application-prod.yml` con credenciales reales de producción (no existe hoy).

### 2.3 Terminal física Point (si en algún momento cobran en persona con lector)

- Point Mini desde ~$99 MXN de hardware.
- Comisión ~3.5% + IVA en débito (variable en crédito).
- Para vendedores nuevos: piden $15,000 MXN acumulados en transacciones de al menos 5
  pagadores distintos antes de acceder a ciertos beneficios/tasas preferenciales.
- Esto es lo que YA tienen integrado (`/v1/mp/*`), no requiere trabajo adicional salvo que
  quieran mejorar la UI de venta directa.

### 2.4 Alternativas de pasarela investigadas (México, 2026)

**Corrección 2026-09-02:** las URLs de comisiones de la primera versión de este documento
apuntaban a portales de DESARROLLADOR (requieren cuenta/sesión para cargar — por eso Conekta
"no cargaba"). Las de abajo son las páginas PÚBLICAS de precios, sin login.

| Gateway | Comisión tarjeta (con IVA salvo que se indique) | Fuerte en | Nota |
|---|---|---|---|
| **Mercado Pago** | Variable según plazo de liberación (al instante / 7 días / 30 días) — a menor plazo, mayor comisión | Reconocimiento de marca, ya está la cuenta creada | Recomendado como principal — reutiliza cuenta existente |
| **Conekta** | 2.9% + $2.5 MXN + IVA · OXXO $10–13 MXN + IVA · SPEI $12.5 MXN + IVA | Mejor relación precio/valor, soporte 100% en español, MSI nativos | Buena alternativa/backup si se quiere OXXO más barato |
| **OpenPay** | 2.9% + $2.5 MXN (tarjeta nacional) · 3.99% + IVA (tarjeta extranjera) | MSI hasta 24 meses, respaldo BBVA, sin renta mensual ni membresía | Interesante si el volumen de ventas crece |
| **Stripe** | 3.60% + $3 MXN + IVA | Multimoneda, control técnico | **Más caro que Conekta/OpenPay en la comisión base** — corregido respecto a la versión anterior de este doc, que no traía el número real |
| **PayPal** | 3.95% + $4 MXN + IVA | Reconocimiento de marca internacional | La comisión más alta de las cinco |
| **Clip** | — | Fuerte si además se necesita POS físico unificado con online bajo un solo proveedor | Solo relevante si se reemplaza también la parte presencial |

**Recomendación (sin cambios):** Mercado Pago Checkout Pro como principal (ya se tiene la cuenta
y experiencia con su plataforma vía Point). Conekta queda como alternativa a evaluar más adelante
si se necesita mejor tarifa en OXXO/SPEI — no es indispensable para el primer lanzamiento. Con
las cifras reales confirmadas, Stripe queda descartado por precio (es la segunda comisión más
alta de tarjeta, después de PayPal) — no solo por SPEI/OXXO débil como se dijo antes.

**Pendiente de decidir:** si se implementa Mercado Pago solo, o Mercado Pago + Conekta desde el
arranque (impacta cuánto trabajo de entrada).

### 2.5 URLs oficiales para validar esta información antes de implementar

**Mercado Pago**
- Comisiones por cobro (Checkout, Link de pago, Point) — página pública: https://www.mercadopago.com.mx/ayuda/costo-recibir-pagos_220
- Checkout Pro — overview / cómo integrarlo (requiere cuenta de developer para ver todo el contenido): https://www.mercadopago.com.mx/developers/es/docs/checkout-pro/overview
- Checkout API — requisitos previos (cuenta, credenciales, sandbox): https://www.mercadopago.com.mx/developers/es/docs/checkout-api/prerequisites
- Terminal Point Mini — ficha del producto: https://www.mercadopago.com.mx/herramientas-para-vender/lectores-point/point-mini
- Términos del programa (de aquí sale el umbral de $15,000 MXN / 5 pagadores): https://www.mercadopago.com.mx/ayuda/5251
- Repositorios oficiales de SDKs: https://github.com/mercadopago

**Conekta**
- **Comisiones — página pública de precios (sin login):** https://www.conekta.com/pricing
- Portal de desarrolladores (requiere cuenta): https://developers.conekta.com/
- API Keys de producción (cómo se obtienen, ya adentro del portal): https://developers.conekta.com/docs/api-keys-producci%C3%B3n

**OpenPay**
- **Comisiones — tabla pública de precios (sin login):** https://www.openpay.mx/comisiones
- Documentación / introducción: https://documents.openpay.mx/docs/introduction.html
- Referencia de API: https://documents.openpay.mx/docs/api
- Ambiente de pruebas (sandbox): https://sandbox-dashboard.openpay.mx

**Stripe**
- **Comisiones — página pública de precios (sin login):** https://stripe.com/pricing
- Pagos con OXXO (guía técnica, español): https://docs.stripe.com/payments/oxxo?locale=es-419
- Nota: Stripe no tiene SPEI nativo como método de pago dedicado en Payment Intents (sí acepta
  tarjetas y OXXO) — confirmar contra la documentación si esto cambió antes de descartarlo por
  ese motivo (aunque ya quedó descartado por precio, ver 2.4).

**PayPal**
- **Comisiones para negocios en México — página pública (sin login):** https://www.paypal.com/mx/business/paypal-business-fees
- Portal de desarrolladores (requiere cuenta): https://developer.paypal.com

Antes de escribir código de integración, revisar la página de comisiones de cada uno (arriba,
marcadas en negrita) porque los términos y comisiones cambian con el tiempo — este documento es
de 2026-09-02 y no se actualiza solo. Las páginas de "portal de desarrolladores" son las que
piden iniciar sesión/crear cuenta — normal que no carguen sin loguearse, no es un error del link.

---

## 3. WhatsApp

### 3.1 Lo que ya existe en el código

`service/WhatsappService.java` — **ya está implementado y conectado**, pero inactivo en la
práctica:
- `@Value("${whatsapp.proveedor:ninguno}")` — por defecto `"ninguno"`, y **ningún** `application-*.yml`
  (dev/qa/docker) lo configura → hoy no envía nada.
- El único proveedor soportado es **CallMeBot**, un relay **no oficial** (no es la WhatsApp
  Business API real de Meta) — funciona simulando una sesión de WhatsApp Web. Este tipo de
  automatización por fuera de la API oficial **es justo lo que Meta detecta y banea** cuando hay
  volumen o patrones de bot.
- Ya está conectado a `VentaServiceImpl.java` (línea 398) y `AbonoServiceImpl.java` (líneas 317
  y 521) — se dispara al confirmar venta/abono, pero como el proveedor no está configurado, no
  hace nada.
- Coincide con el estado documentado en `PLAN_MEJORAS.md` ítem #3: **"🚫 EN PAUSA — ver decisión
  2026-07-01"** — ya se había decidido no activarlo tal como estaba.
- El botón flotante de WhatsApp que sí se ve en la app es solo un link `wa.me/...` (abre WhatsApp
  del cliente para chatear manualmente con el negocio) — no es envío automatizado.

### 3.2 Cómo enviar WhatsApp sin arriesgar el número (investigado)

Para mensajes automatizados (confirmación de pedido, seguimiento, etc.) sin riesgo de baneo, la
única vía soportada por Meta es la **WhatsApp Business Platform (Cloud API)**, directa o vía un
proveedor autorizado (BSP) como Twilio o 360dialog:

- Requiere cuenta verificada en **Meta Business Manager** + número de teléfono dedicado (no puede
  ser un número que ya use WhatsApp normal).
- Límites de envío por "tier", escalan según calidad de conversación y tasa de respuesta
  positiva: empieza en 250 conversaciones/día → sube a 1,000 → 10,000, automáticamente si la
  cuenta mantiene buena calidad.
- Lo que realmente evita el baneo no es el volumen sino la **tasa de reportes/bloqueos**:
  - Solo enviar a clientes que dieron **opt-in explícito** (ej. checkbox al comprar: "quiero
    recibir actualizaciones de mi pedido por WhatsApp").
  - Usar **plantillas de mensaje pre-aprobadas por Meta** (no texto libre) para el primer
    contacto o fuera de la ventana de 24h de conversación abierta.
  - Escalar gradual, no mandar en ráfaga desde el día uno.
- **Cambio de precios desde el 1 oct 2026:** Meta empieza a cobrar también las plantillas de
  categoría "Utilidad" (ej. confirmación/seguimiento de pedido) dentro de la ventana de 24h —
  antes eran gratis en esa ventana. Hay que contemplar este costo por mensaje en el presupuesto.

**Conclusión:** el `WhatsappService` actual (CallMeBot) no es viable para producción a futuro —
hay que sustituirlo por Cloud API oficial o un BSP cuando se decida activar WhatsApp. Mientras
tanto seguir en pausa (como ya estaba decidido) no tiene costo, porque hoy no está enviando nada.

**Pendiente de decidir:** si se prioriza esta migración ahora o después de correos/alertas de
stock (que no dependen de aprobación externa de Meta).

---

## 4. Correos: recordatorio con opt-out y seguimiento de pedido

### 4.1 Lo que ya existe

`service/EmailService.java` — hoy solo maneja:
- `enviarTicket(...)` — comprobante de compra
- `enviarCodigoVerificacion(...)` — verificación de correo
- `enviarCodigoResetPassword(...)` — recuperar contraseña
- `enviarCodigoReclamoVenta(...)` — reclamo de venta
- `enviarNotificacionGanador(...)` — ganador de rifa

**No existe ningún correo de recordatorio, de seguimiento de pedido, ni ningún campo de
preferencia de correo (opt-in/opt-out) en `Cliente` ni en ninguna entidad.**

### 4.2 Qué falta construir (dos features distintas, aunque las pidió juntas)

**A) Correo de recordatorio + toggle de opt-out en la app**
- Campo nuevo en `Cliente` (ej. `recibirCorreosRecordatorio: boolean`, default `true`).
- Endpoint `PUT` para que el cliente cambie su preferencia desde "Mis datos" en la app.
- El servicio/scheduler que mande el recordatorio debe **verificar ese campo antes de enviar**.
- Falta definir: ¿recordatorio de qué? (carrito abandonado, cliente inactivo hace N días,
  producto que vio y no compró). Hay que decidir el disparador antes de programar.

**B) Correo de seguimiento/tracking de pedido**
- Correo automático cuando cambia el estado del `Pedido` (ej. "en preparación" → "enviado" →
  "entregado"). Requiere revisar qué estados de pedido ya existen en `Pedido`/`estadoVenta` para
  enganchar el envío a esos cambios de estado, no crear una máquina de estados nueva.
- Debe respetar el mismo campo de opt-out de (A) — o puede tratarse como transaccional
  (no opcional, como el ticket de compra) si se decide que es información esencial del pedido y
  no "marketing". **Esto hay que decidirlo**: normalmente el recordatorio/marketing sí es
  opt-out, pero el seguimiento de un pedido que ya se pagó suele considerarse transaccional
  (como el ticket) y no se desactiva.

---

## 5. Alertas de stock

**Ojo: hay dos cosas distintas con el mismo nombre.**

### 5.1 Ya pendiente en `PLAN_MEJORAS.md` (ítem #4) — alerta al ADMIN
**✅ Implementado 2026-09-02** — ver sección 0.5 arriba (digest diario 7 a.m., umbral configurable
desde Sistema > Negocio & Contactos). Era para que el negocio supiera cuándo reabastecer, no para
el cliente — eso es lo de la sección 5.2.

### 5.2 Lo que pidió ahora — alerta al CLIENTE, ligada a Favoritos
Esto es nuevo: que un cliente con un producto en Favoritos que está sin stock reciba una alerta
(correo o notificación en la app) cuando ese producto/variante **vuelve a tener stock**.

**Lo que ya existe como base:** módulo `favoritos` (backend + frontend) ya funcional para
guardar/quitar favoritos — hay que confirmar el modelo exacto antes de diseñar el trigger, pero
la relación cliente↔variante favorita ya está resuelta, no hay que construirla de cero.

**Falta construir:**
- Detectar la transición de "sin stock" a "con stock" en una variante (evento o comparación en
  el mismo `ProductosServiceImpl`/`VarianteService` que actualiza el stock).
- Al detectarla, consultar qué clientes tienen esa variante en Favoritos y encolar un correo
  (o notificación in-app) — con el mismo opt-out del punto 4 si se decide tratarlo como
  marketing, o siempre enviarlo si se considera relevante para el cliente aunque no sea
  transaccional (a decidir).
- Evitar reenvíos: una vez avisado, no volver a avisar hasta que vuelva a agotarse y reabastecerse.

---

## 6. Programa de lealtad

**No existe nada hoy** (ni entidades, ni cálculo de puntos, ni pantalla).

### Lo que hay que diseñar
1. **Cómo se ganan puntos**: ¿por monto gastado (ej. $1 = 1 punto), por compra completada
   independiente del monto, o combinación?
2. **Cómo se canjean**: ¿descuento directo en checkout, productos/recompensas específicas, o
   ambas?
3. **Vigencia de los puntos — pedido explícitamente que sea CONFIGURABLE**, no un valor fijo en
   código. Implica:
   - Un campo de configuración (ej. en la tabla/pantalla de "Config negocio" que ya existe —
     `config-negocio` — agregar `vigenciaPuntosMeses` o similar) que el admin pueda cambiar sin
     tocar código.
   - Un job/scheduler que expire los puntos vencidos según ese valor configurable (similar en
     espíritu al `ImagenScheduler` que ya corre en cron para otra cosa).
   - Decidir si la vigencia aplica por lote de puntos (cada compra vence independiente, tipo
     FIFO) o si es una vigencia global de la cuenta (más simple pero menos preciso).
4. **Dónde se ve**: sección en "Mis datos"/perfil del cliente con saldo de puntos y quizás
   historial de movimientos (ganados/canjeados/expirados).

**Pendiente de decidir:** todo el punto 1 y 2 (mecánica de acumulación y canje) — sin eso no se
puede empezar a programar el modelo de datos.

---

## Resumen ejecutivo — qué está listo vs qué falta

| Feature | Estado |
|---|---|
| Reseñas | ✅ Completo, no requiere trabajo |
| Pago con terminal física (Point) | ✅ Completo, ya en uso en venta directa |
| Pago online (Checkout Pro) | ❌ No existe — construir desde cero (backend + frontend) |
| WhatsApp automatizado | ⚠️ Código existe pero usa método no oficial y está apagado — hay que decidir cuándo migrar a Cloud API oficial |
| Correo de recordatorio + opt-out | ❌ No existe — falta campo de preferencia + definir disparador |
| Correo de seguimiento de pedido | ❌ No existe — falta enganchar a cambios de estado de `Pedido` |
| Alerta stock bajo (admin) | ✅ Implementado 2026-09-02 (ver 0.5) |
| Alerta "volvió el stock" (cliente, vía Favoritos) | ❌ No existe — nueva, pero reutiliza el módulo de Favoritos ya construido |
| Programa de lealtad | ❌ No existe — falta decidir mecánica de puntos antes de programar el modelo |

---

## Decisiones pendientes antes de programar

1. **Pago:** ¿Mercado Pago Checkout Pro solo, o + Conekta desde el arranque?
2. **WhatsApp:** ¿se prioriza la migración a Cloud API ahora, o se deja en pausa y se atacan
   primero correos/alertas (no dependen de aprobación de Meta)?
3. **Correo de recordatorio:** ¿cuál es el disparador? (carrito abandonado / inactividad / vista
   sin compra)
4. **Correo de seguimiento de pedido:** ¿opt-out como el recordatorio, o transaccional siempre-on
   como el ticket?
5. **Alerta de stock al cliente:** ¿correo, notificación in-app, o ambas?
6. **Lealtad:** mecánica de acumulación y de canje (la vigencia ya se definió: configurable).
