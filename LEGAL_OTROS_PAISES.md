# ¿Le tocan leyes de otros países a Novedades Jade? — investigación (2026-10-07)

> ⚠️ No es asesoría legal. Complementa `LEGAL_MEXICO_INVESTIGACION.md` y `LEGAL_PLAN_DE_ACCION.md`.

## Respuesta corta

**Como vendes solo en México y a clientes en México, la ley que te obliga es la mexicana.** Ninguna
ley de otro país te obliga hoy de forma directa. Pero:

1. Usas **empresas extranjeras** que guardan o procesan datos de tus clientes (servidor en Europa,
   OpenAI y Gmail en EE. UU.). La ley que aplica sigue siendo la mexicana (tú eres el responsable),
   pero eso tiene que estar **dicho en tu aviso de privacidad** y esas empresas te ponen sus propias
   **condiciones de uso** (contratos), que sí tienes que cumplir.
2. **Las redes sociales, WhatsApp, Mercado Pago y las tiendas de apps** tienen reglas propias. No son
   leyes, pero si no las cumples **te cierran la cuenta** o te rechazan la app.
3. Hay leyes extranjeras que **se activarían** solo si un día haces algo de lo siguiente: vender o
   enviar a otros países, mandar correos de promoción a gente de EE. UU. o crecer muchísimo.

---

## 1. Dónde viven los datos de tus clientes hoy

| Servicio | Empresa y país | Qué datos pasan por ahí |
|---|---|---|
| Servidor y base de datos | **OVHcloud** (Francia). El centro de datos parece estar en Europa por la IP **[Por confirmar en el panel de OVH]** | **Todo**: cuentas, pedidos, direcciones, conversaciones |
| DNS y dominio | **Hostinger** (Lituania) | No guarda datos de clientes |
| Correo (tickets, códigos) | **Gmail / Google** (EE. UU.) | Correo del cliente, ticket |
| Chatbot y bot de redes | **OpenAI** (EE. UU.) | El texto que escribe el cliente |
| Cobros con tarjeta | **Mercado Pago** (México, grupo Mercado Libre) | Datos de pago (no pasan por tu servidor) |
| Redes | **Meta** (EE. UU. / Irlanda) y **TikTok** (Singapur / EE. UU.) | Mensajes y comentarios |
| Código e imágenes del sistema | **GitHub** y **Docker Hub** (EE. UU.) | Código, sin datos de clientes |

---

## 2. Europa — GDPR (Reglamento General de Protección de Datos)

- **¿Te aplica por tener el servidor en Francia?** **No.** Según el Comité Europeo de Protección de
  Datos (Guía 3/2018), un responsable que está **fuera de la UE no queda sujeto al GDPR solo por
  contratar un proveedor en la UE**. Quien sí cumple el GDPR es OVH, como "encargado".
- **¿Cuándo te aplicaría?** Si **ofreces productos a personas en la UE** (precios en euros, envíos a
  Europa, anuncios dirigidos allá) o si **sigues su comportamiento** (analítica o publicidad dirigida
  a europeos). Hoy no lo haces.
- **Qué hacer:** decir en el aviso de privacidad que los datos se guardan en servidores de OVHcloud
  en Europa. Si un día vendes a Europa, el GDPR se vuelve obligatorio (base legal, cartel de cookies,
  representante en la UE, etc.).

Fuentes: [EDPB — Guía 3/2018, versión final (Covington)](https://www.insideprivacy.com/data-privacy/edpb-adopts-final-version-of-guidelines-on-territorial-scope-of-the-gdpr/) · [Privacy World: ámbito territorial del GDPR](https://www.privacyworld.blog/2019/12/territorial-scope-of-the-gdpr-following-edpbs-final-guidelines-part-1/) · [Elvinger Hoss: guía del ámbito territorial](https://elvingerhoss.lu/publications/guidelines-territorial-scope-gdpr)

---

## 3. Estados Unidos

| Ley | ¿Aplica? | Por qué |
|---|---|---|
| **CCPA / CPRA** (California) | **No** | Solo para quien factura más de **$26.6 millones de dólares** al año, o maneja datos de más de 100,000 californianos, o gana más de la mitad vendiendo datos |
| **CAN-SPAM** (correos de promoción) | **Solo si** mandas promociones por correo a personas en EE. UU. | Pide: no engañar en el asunto, decir que es publicidad, poner tu domicilio y una forma fácil de darse de baja. Es casi lo mismo que pide México (LFPC 76 bis VI), así que cumpliendo lo mexicano quedas cubierto |
| **COPPA** (menores de 13 años) | **No** | La tienda es para mayores de edad y no se dirige a niños de EE. UU. |
| Leyes de IA de EE. UU. o de estados | **No** | Aplican a empresas que operan allá |

Fuentes: [Feroot: aplicabilidad del CCPA 2026](https://www.feroot.com/blog/ccpa-applicability-website-california-law/) · [Clym: guía de aplicabilidad del CCPA](https://www.clym.io/blog/ccpa-applicability-guide) · [EmailOctopus: CAN-SPAM para remitentes de otros países](https://emailoctopus.com/blog/can-spam-act-for-international-marketers)

---

## 4. Contratos con proveedores que sí tienes que cumplir

### OpenAI (chatbot y bot de redes)
- Por API, **no entrena sus modelos con tus datos** salvo que tú lo actives. Guarda lo que mandas
  **hasta 30 días** para detectar abusos (o más si la ley se lo exige).
- Ofrece un **Acuerdo de Tratamiento de Datos (DPA)** para negocios: conviene aceptarlo desde la
  cuenta de OpenAI, porque te sirve de prueba ante la LFPDPPP de que tu "encargado" protege los datos.
- Sus políticas de uso piden no engañar a las personas haciéndoles creer que hablan con un humano
  (ya está en el aviso de privacidad y en el bot).

### Mercado Pago (cobros con tarjeta)
- **PCI DSS** es la norma de seguridad de las tarjetas. Como el cobro pasa por la terminal o el
  checkout de Mercado Pago y **tu servidor nunca ve los números de tarjeta**, te toca el nivel más
  simple (**cuestionario SAQ A**). Lo importante: **nunca** pedir ni guardar números de tarjeta por
  chat, WhatsApp, correo ni en el sistema.
- Cumplir sus términos: productos prohibidos, contracargos, devoluciones por el mismo medio.

### Google (Gmail para mandar correos)
- Una cuenta de Gmail normal tiene **límites de envío diarios** y no está pensada para negocio. Si
  crecen los correos, conviene **Google Workspace** o un servicio de correo transaccional, con el
  dominio `@novedades-jade.com.mx` (además ayuda en la verificación de Meta).

### OVHcloud y Hostinger
- Cumplir sus términos de uso. OVH, por ser europea, ya cumple el GDPR como encargado.

Fuentes: [OpenAI: datos de la API (30 días)](https://conductatlas.com/platform/openai/openai-api-data-usage-policies/provision/CA-P-061207/api-inputs-and-outputs-retained-up-to-30-days/) · [OpenAI: no entrena con datos de negocio](https://conductatlas.com/platform/openai/openai-api-data-usage-policies/provision/CA-P-061188/no-default-model-training-on-business-data/) · [TechCrunch: política de datos de OpenAI](https://techcrunch.com/?p=2493192) · [Mercado Pago: PCI DSS](https://www.mercadopago.com.mx/developers/es/docs/checkout-api-payments/additional-content/security/pci.md)

---

## 5. Reglas de las plataformas (no son leyes, pero te cierran la cuenta)

### Facebook e Instagram (Meta)
- **Sorteos y concursos:** solo desde la página o cuenta del negocio; **bases** publicadas; aviso de
  que **Meta no patrocina ni administra** la dinámica; **prohibido** pedir "comparte en tu muro",
  "comparte en el muro de un amigo" o "etiqueta a tus amigos" como forma de participar (sí se puede
  pedir comentar).
- **Mensajes automáticos:** el cliente debe saber que habla con un asistente; ventana de 24 h para
  contestar (ya documentado en `ALTA_NEGOCIO_META_TIKTOK.md`).
- **Política de comercio de Meta:** no vender productos prohibidos ni **falsificados**.

### WhatsApp Business
- **Para mandar un mensaje que tú inicias (promociones, avisos), el cliente tiene que haber aceptado
  antes (opt-in)**, de forma clara: que acepta recibir mensajes de **Novedades Jade** por WhatsApp y
  de qué tipo (avisos de pedido, ofertas). Sin prueba de esa aceptación, te pueden bloquear el número.
- Hoy el sistema no manda WhatsApp automáticos; si se agrega, hace falta una casilla de aceptación.
- Prohibido en catálogos de WhatsApp: armas, alcohol, tabaco, medicamentos, apuestas con dinero,
  productos falsos, etc. Ropa, bolsas y perfumes originales **sí** se permiten.

### TikTok
- **Contenido comercial:** activar el interruptor **"Divulgar contenido comercial" → "Tu marca"** en
  cada video que promueve la tienda, o le baja el alcance o lo quita.
- **Sorteos (política renovada el 25-feb-2026):** en el **video** deben verse el premio y su valor,
  quién puede participar, cómo se participa, cómo se elige al ganador y la fecha de cierre.

### Google Play y App Store (solo si se publica la app)
- El proyecto tiene una app de Android (`barcodeScannerApp`, parece de uso interno para escanear).
  **Si algún día se publica una app para clientes:**
  - **Google Play:** formulario de "Seguridad de los datos", aviso de privacidad, y si se crean
    cuentas: **borrar la cuenta desde la app** y un **enlace web** para pedirlo (ya existe
    `/eliminar-datos`).
  - **Apple (5.1.1 v):** **borrar la cuenta dentro de la app**, no solo desactivarla.

Fuentes: [Woobox: reglas de sorteos en Facebook e Instagram 2026](https://blog.woobox.com/ultimate-contest-rules-guide-facebook-instagram-youtube-twitter/) · [Comment Picker: reglas de Facebook](https://commentpicker.com/articles/facebook-giveaway-rules.php) · [Rasayel: política de mensajería de WhatsApp](https://docs.rasayel.io/whatsapp-messaging-policy) · [WhatsApp Business Policy](https://whatsappbusiness.com/policy/?facet1=crm) · [Optimove: requisitos de campañas de WhatsApp](https://academy.optimove.com/hc/en-us/articles/25863306876829-Regulatory-Requirements-for-WhatsApp-Campaigns) · [Darkroom: reglas de TikTok 2026](https://www.darkroomagency.com/observatory/what-brands-need-to-know-about-tiktok-new-rules-2026) · [Gleam: sorteos en TikTok](https://gleam.io/blog/tiktok-giveaway-rules/) · [Google Play: borrado de cuentas](https://support.google.com/googleplay/android-developer/answer/13327111) · [Apple: guía 5.1.1(v)](https://developer.apple.com/forums/thread/693997)

---

## 6. Si algún día haces esto, revisa leyes de otro país

| Si… | Entonces… |
|---|---|
| Envías o vendes a **EE. UU.** | Aduana, impuestos y etiquetado de allá (FTC para textiles); CAN-SPAM si mandas promociones |
| Vendes o anuncias para **Europa** | **GDPR** completo, ley europea de consumo (14 días para devolver), IVA europeo |
| **Importas** mercancía | Padrón de Importadores (México), NOMs en la aduana, reglas del país de origen |
| Publicas una **app para clientes** | Reglas de Google Play y App Store (sección 5) |
| Usas **anuncios dirigidos** (Pixel de Meta, Google Ads) | Aviso de cookies y de rastreo en México; si llegan a europeos, GDPR |
