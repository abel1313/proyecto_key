# Plan legal — qué tenemos, qué falta y cómo se saca (2026-10-07, ampliado el mismo día)

Sale de **`LEGAL_MEXICO_INVESTIGACION.md`** (qué pide cada ley de México, con fuentes) y de
**`LEGAL_OTROS_PAISES.md`** (otros países, proveedores extranjeros y reglas de redes). Aquí está lo práctico:
qué hay hoy, qué falta, quién lo hace, dónde se saca y cuánto cuesta.

> ⚠️ No es asesoría legal. Lo que dice **Abogado** o **Contador** hay que confirmarlo con ellos antes
> de hacerlo.

**Quién lo hace:** **Tú** = trámite tuyo · **Yo** = cambio en el sistema · **Contador** · **Abogado**.
**Prioridad:** 🔴 obligatorio y falta · 🟠 obligatorio o con riesgo, pero menos urgente · 🟢 recomendado.

---

## Avance (2026-10-07) — lo que ya se programó sin tus datos

En `dev`, **sin subir** (espera tu "sube"); se prueba con la **Prueba 12** de `GUIA_DE_PRUEBAS_QA.md`.

| Punto | Qué quedó | Qué falta |
|---|---|---|
| 4 Datos del negocio | **Sistema → Configuración del negocio → ⚖️ Datos legales**; salen solos en el pie de página, Términos, Aviso de privacidad y ticket | **Tú:** capturar nombre, domicilio, teléfono y RFC reales |
| 5 Términos | Reescritos: garantía 90 días, cancelar en 5 días hábiles, formas de pago (Apartado sin dinero, Ir pagando sin intereses), qué pasa con el dinero al cancelar, perfumes, PROFECO | Abogado: revisar; **tú:** si quieres dar más de 90 días o cambios por talla, avísame |
| 6 Aviso de privacidad | Responsable (de Datos legales), finalidades separadas, cómo limitar y revocar, ARCO con plazos, proveedores y países; aviso corto en el registro | Abogado: revisar |
| 7 Ir pagando / Apartado | Aviso "sin intereses, precio de contado, total, CAT 0%" en Venta directa y Carrito; "Abonos sin intereses (CAT 0%)" en el ticket | — |
| 8 Ticket | Datos del negocio arriba y "Garantía de 90 días" abajo | Factura: cuando estés en el SAT (punto 2) |
| 13 Bot | Regla nueva: solo lo del catálogo, nada de "original"/"garantizado"/efectos en la salud | **Tú:** lo mismo en tus publicaciones |
| 15 Seguridad | `SEGURIDAD_DATOS.md`: medidas documentadas y qué hacer si hay una fuga | DPA de OpenAI (punto 23) |
| 16 Aceptación de Términos | Casilla en el registro y fecha guardada | — |
| 20 Google | Título y datos de producto por artículo, `noindex` en "no disponible", sitemap y robots arreglados, fotos con nombre, sin canonical fijo | Ficha de Google (tú) |

**Lo que ya existía** (no hubo que hacer nada): límite de registros por IP (punto 20), botones de
WhatsApp/Facebook/Instagram/TikTok cuando el negocio está cerrado (punto 20), costo de envío por lugar
de entrega (punto 5), casilla "Recibir correos" en Mi perfil para los avisos que no son de la compra
(punto 6).

**Dato encontrado:** el bot dice que entregan en **Luvianos, el Estanco, Caja de Agua, Acatitlán,
Tejupilco (Estado de México) y Zacazonapan**. Eso contesta en parte la duda 2: el municipio sería
**Luvianos o Tejupilco, Estado de México** — confírmalo y dime si hay local abierto al público.

**Script:** `migration_datos_legales.sql` ✅ corrido en QA (2026-10-07). Falta en prod, antes de subir el back a `main` (sin él no se puede entrar al sistema).

---

## Resumen en una pantalla

| # | Qué | Prioridad | Quién | Bloqueado por |
|---|---|---|---|---|
| 1 | Darte de alta en el SAT con el negocio (RESICO), cambiar el domicilio fiscal y sacar la constancia nueva | 🔴 | Tú + Contador | — |
| 2 | Facturar: factura global cada mes y factura a quien la pida | 🔴 | Tú + Contador (+ Yo si se integra al sistema) | 1 |
| 3 | Rifas: permiso de SEGOB o cambiar la dinámica | 🔴 | Abogado + Tú | Dudas 4 y 5 |
| 4 | Datos del negocio visibles en la tienda (nombre, domicilio, teléfono) | 🔴 | Yo | Duda 1 |
| 5 | Términos: garantía de 90 días, cancelar en 5 días hábiles, días para reportar defectos, envío y factura | 🔴 | Yo (+ Abogado revisa) | Dudas 1, 8, 9 y 10 |
| 6 | Aviso de privacidad completo para la ley 2025 + aviso corto en el registro | 🔴 | Yo (+ Abogado revisa) | Duda 1 |
| 7 | Ir pagando: decir precio de contado, total y "sin intereses" antes de vender y en el comprobante | 🟠 | Yo | — |
| 8 | Comprobante (ticket) con todos los datos | 🟠 | Yo | Duda 1 |
| 9 | Promociones con vigencia y condiciones visibles | 🟠 | Yo + Tú | — |
| 10 | Decants de perfume: decidir qué hacer | 🟠 | Abogado / COFEPRIS + Tú | Duda 5 |
| 11 | Aviso de funcionamiento ante COFEPRIS (perfumes) | 🟠 | Tú | Confirmar si aplica |
| 12 | Etiquetas físicas en la mercancía | 🟠 | Tú (con proveedores) | — |
| 13 | Textos de productos, publicaciones y bot sin promesas que no se puedan probar | 🟠 | Yo + Tú | — |
| 14 | Promociones por WhatsApp, SMS o correo: REPEP y opción de darse de baja | 🟠 | Yo + Tú | Duda 7 |
| 15 | Seguridad de datos: plan por si hay una fuga | 🟠 | Yo | — |
| 16 | Guardar la aceptación de Términos con fecha (ya se guarda la de privacidad) | 🟢 | Yo | — |
| 17 | Registrar la marca "Novedades Jade" en el IMPI | 🟢 | Tú | e.firma (1) |
| 18 | Municipio: aviso o licencia si hay local | 🟢 / 🔴 si hay local | Tú | Duda 2 |
| 19 | Si hay empleados: IMSS y nómina | 🔴 si hay | Tú + Contador | Duda 3 |
| 20 | Lo de los videos que no es ley: sitemap, fotos con nombre, títulos por producto, WhatsApp, límite de registros | 🟢 | Yo | — |
| 21 | **Nunca vender réplicas o falsificados** de marcas (es delito, con cárcel) | 🔴 | Tú | Duda 13 |
| 22 | **Registrar con CURP la línea celular del negocio** (WhatsApp) antes de su fecha límite | 🔴 | Tú | Duda 15 |
| 23 | Aceptar el acuerdo de tratamiento de datos (DPA) de OpenAI y decir en el aviso dónde están los servidores | 🟠 | Tú + Yo | — |
| 24 | Reglas de Meta y TikTok en sorteos y publicaciones (interruptor "contenido comercial") | 🟠 | Tú | — |
| 25 | Correo del negocio con dominio propio (Google Workspace o servicio de correo) en lugar de Gmail personal | 🟢 | Tú + Yo | — |
| 26 | Si importas: Padrón de Importadores y sectores específicos (textiles) | 🔴 si importas | Tú + Contador | Duda 14 |
| 27 | Distintivo Digital PROFECO (sello voluntario de confianza) | 🟢 | Tú | 4, 5 y 6 hechos |

---

## Detalle de cada punto

### 1. 🔴 SAT: el negocio dado de alta
- **Tenemos:** RFC con "Sueldos y Salarios" (2022), domicilio del patrón.
- **Falta:** e.firma, cambiar el domicilio fiscal, agregar la actividad del negocio en **RESICO** y
  sacar la constancia nueva.
- **Cómo:** pasos completos en `ALTA_NEGOCIO_META_TIKTOK.md` §11.8.
  1. e.firma: cita en **citas.sat.gob.mx** ("e.firma personas físicas"). Llevar INE, CURP,
     comprobante de domicilio (máximo 3 meses), correo y una USB.
  2. Cambio de domicilio: en línea con e.firma ("Realiza tu cambio de domicilio en el RFC").
  3. Aviso de actualización de actividades: en línea con RFC y contraseña. Actividad: comercio al
     por menor de ropa, bolsas y accesorios por internet; régimen RESICO.
  4. Constancia nueva: portal del SAT o app **SAT Móvil**.
- **Costo:** gratis. El contador cobra aparte (normalmente una cuota mensual).
- **Ojo:** con RESICO llegan **declaraciones mensuales** (día 17) aunque no vendas. Sin presentarlas
  hay multas. Por eso con contador.
- **Desbloquea:** la verificación de Meta y TikTok, el registro de marca y la factura.

### 2. 🔴 Facturar
- **Tenemos:** ticket por correo. No hay facturas.
- **Falta:**
  - **Factura global** "público en general" (mensual, por ejemplo) de todo lo que no se facturó aparte.
  - **Factura** a quien la pida, con sus datos.
  - **Ir pagando facturado:** método PPD y complemento de pago por cada abono.
  - **Devoluciones:** nota de crédito.
- **Cómo:**
  - Gratis: el **facturador del SAT** (portal, "Factura electrónica"). Sirve para pocas facturas.
  - Un **PAC** o sistema de facturación (Facturama, Factura.com, Alegra… desde ~$200–$500 al mes;
    precios por confirmar).
  - Más adelante, **yo** puedo agregar a la tienda un botón **"Pedir factura"** que mande los datos
    al PAC.
- **Pregúntale al contador:** cada cuánto conviene la factura global y si te lleva él la facturación.

### 3. 🔴 Rifas
- **Tenemos:** módulo de rifas con boletos por participación en redes y por compra, ruleta y ganador.
- **Falta:** el **permiso de SEGOB** (lo pide todo sorteo abierto al público, también los gratuitos y
  los ligados a compras).
- **Opciones** (decide con abogado):
  - **A. Sacar el permiso** en cada rifa: trámite **SGOB-01-009-A** en la Dirección General de Juegos
    y Sorteos, mínimo **10 días hábiles antes**. Lleva bases, premios, fechas y mecánica.
  - **B. Cambiar a concurso de habilidad** (gana la mejor foto, la mejor frase…): sin azar no va con
    SEGOB; se avisa a PROFECO (NOM-028-SCFI-2007).
  - **C. Pausar las rifas** abiertas en redes mientras se decide.
- **Siempre:** bases publicadas (fechas, cómo participar, premios, cómo se elige, cuándo y cómo se
  entrega), aviso de que Facebook, Instagram y TikTok no patrocinan, y no pedir "comparte en tu muro"
  ni "etiqueta amigos" para participar.
- **Antilavado:** solo si un premio o boleto vale **~$38,000 o más** (325 UMA 2026). Con premios de la
  tienda no aplica.
- **Impuestos del premio [Contador]:** quien entrega el premio retiene **1% de ISR** sobre su valor y
  paga el **impuesto estatal a premios** (6% en CDMX; depende del estado), y lo informa al SAT.
- **Qué lleva la solicitud a SEGOB (sin venta de boletos):** escrito con nombre, vigencia, mecánica,
  cómo se participa, zona, medios, fechas, premios con su valor y tus datos; RFC o constancia, CURP
  e INE. Contestan en 10 días hábiles.
- **TikTok (desde feb-2026):** en el **video** deben verse premio y valor, quién participa, cómo,
  cómo se elige y la fecha de cierre.
- **Dónde:** [SEGOB — requisitos para sorteos](https://www.gob.mx/segob/acciones-y-programas/requisitos-para-sorteos) · [trámite SGOB-01-009-A](https://catalogonacional.gob.mx/FichaTramite?traHomoclave=SGOB-01-009-A).

### 4. 🔴 Datos del negocio visibles
- **Tenemos:** solo el correo `contacto@novedades-jade.com.mx`.
- **Falta:** nombre del responsable, **domicilio físico**, **teléfono** y RFC, **antes** de comprar
  (LFPC 76 bis III).
- **Cómo (Yo):** una sección **Contacto** en el pie de página (en todas las pantallas de la tienda) y
  en Términos y Aviso de privacidad. Los datos se toman de **Configuración del negocio** para
  cambiarlos sin programar.
- **Si no quieres publicar tu casa:** necesitas un domicilio del negocio (oficina virtual o local),
  y ese mismo va en el SAT.

### 5. 🔴 Términos y condiciones
- **Tenemos:** Términos en `/termConditions`, en el pie de página.
- **Falta:**
  - El **número de días** en "contáctanos dentro de los siguientes días".
  - **Garantía mínima de 90 días** desde la entrega (art. 77).
  - **Derecho a cancelar en 5 días hábiles** desde que recibe el producto (art. 56); el envío de
    regreso lo paga el cliente.
  - Perfumes: "sin cambio si el sello fue abierto, **salvo defecto y sin afectar** el derecho de
    cancelación" **[Abogado]**.
  - **Costo y tiempo de envío** antes de pagar.
  - **Cómo pedir factura.**
  - Formas de pago: contado, **Apartado** (sin dinero; se paga completo al recoger) e **Ir pagando**
    (abonos sin intereses), y qué pasa con el dinero si se cancela (saldo a favor o devolución).
  - Datos del negocio (punto 4).
  - Que no quede ninguna **cláusula abusiva** (art. 90): no valen y dan mala imagen ante PROFECO.
  - Saber que, si hay defecto o incumplimiento, además del cambio o reembolso corresponde una
    **bonificación mínima del 20%** (art. 92 ter).
- **Cómo (Yo):** reescribir `terminos.component.html` con tus datos; el abogado lo revisa.

### 6. 🔴 Aviso de privacidad (ley 2025)
- **Tenemos:** aviso en `/privacidad` con datos, finalidades, OpenAI, Meta, TikTok, cookies, derechos,
  menores. Casilla de aceptación en el registro, con fecha guardada.
- **Falta:**
  - **Quién es el responsable y su domicilio.**
  - Finalidades **separadas**: necesarias (vender, entregar, cobrar) y **voluntarias** (promociones)
    con casilla aparte.
  - **Cómo limitar** el uso de los datos.
  - **Procedimiento ARCO:** qué mandar (nombre, qué pide, identificación), a dónde y en cuántos días
    se contesta.
  - **Cómo revocar** el consentimiento.
  - Agregar **Mercado Pago** (pagos) y, si se usa, **WhatsApp**; decir que OpenAI y Gmail procesan en
    EE. UU. y que el servidor está en **OVHcloud (Europa)**.
  - **Aviso corto** en el formulario de registro.
- **Cómo (Yo):** reescribir `privacidad.component.html` y agregar el aviso corto en el registro; el
  abogado lo revisa.

### 7. 🟠 Ir pagando y Apartado: decir todo antes de vender
- **Tenemos:** el sistema calcula total, abonado y lo que falta; los abonos no cobran intereses.
- **Falta (LFPC art. 66):** que el cliente vea, **antes** de aceptar y en su comprobante: **precio de
  contado**, **total a pagar** (el mismo, sin intereses), **"sin intereses, CAT 0%"**, que puede
  pagar antes cuando quiera y qué pasa si cancela.
- **Cómo (Yo):** un texto fijo en la venta y en el comprobante de cada abono.

### 8. 🟠 Comprobante (ticket)
- **Tenemos:** ticket por correo con productos y total.
- **Falta revisar que traiga:** nombre del negocio, domicilio, teléfono y RFC; fecha y folio;
  productos, cantidades, precios y total; forma de pago; en abonos, lo pagado y lo que falta; y el
  texto "Si necesitas factura…".
- **Cómo (Yo):** ajustar las plantillas cuando tengamos tus datos.

### 9. 🟠 Promociones y ofertas
- **Falta:** que cada promoción (cinta, precio con descuento, combos) diga **hasta cuándo** o
  **cuántas piezas**, y sus condiciones. Si no, por ley sigue vigente hasta que avises por el mismo
  medio.
- **Meses sin intereses:** anunciarlos solo si de verdad no cuestan más que de contado; mostrar
  precio de contado y total.
- **Cómo:** Yo agrego vigencia y condiciones a la pantalla de promociones; tú las llenas.

### 10. 🟠 Decants de perfume
- **Riesgo:** reenvasar te vuelve responsable de la etiqueta (NOM-141) y usa una marca ajena.
- **Opciones:** (a) consultar a un abogado o a COFEPRIS; (b) mientras tanto, venderlos como
  "**decant (muestra reenvasada) de…**" con ml, sin el logo de la marca y sin decir "original";
  (c) dejar de vender decants.

### 11. 🟠 COFEPRIS — aviso de funcionamiento
- **Falta confirmar si te aplica** (venta al por menor de perfumes; con decants, casi seguro).
- **Dónde:** trámite en línea de COFEPRIS, **gratuito**. [Instructivo](https://www.gob.mx/cms/uploads/attachment/file/348567/Instructivo_Aviso_Funcionamiento.pdf).

### 12. 🟠 Etiquetas de la mercancía
- **Revisar con tus proveedores** que todo traiga etiqueta en español:
  - Ropa: NOM-004 (composición %, talla, cuidados, país, responsable).
  - Bolsas: NOM-020 si son de piel o imitación.
  - Perfumes: NOM-141.
- **No vender** mercancía sin etiqueta.
- **Yo:** poner composición y talla en la descripción de la tienda (ayuda, no es obligatorio en línea).

### 13. 🟠 Textos sin promesas que no se puedan probar
- No decir "original", "100% piel", "garantizado" o "el más barato" sin poder probarlo.
- **Yo:** revisar las instrucciones del chatbot y del bot de redes para que no inventen
  características. **Tú:** lo mismo en tus publicaciones.
- Si le pagas o regalas producto a alguien para que hable de la tienda, que ponga **#Publicidad**.

### 14. 🟠 Promociones por WhatsApp, SMS o correo
- **Si mandas promociones:** antes de cada campaña, **consultar el REPEP** de PROFECO y quitar esos
  números; en correos, **opción para darse de baja**; y casilla de promociones **opcional** en el
  registro (punto 6).
- **WhatsApp** además exige que el cliente **haya aceptado antes** recibir mensajes de Novedades Jade
  (y de qué tipo). Sin esa prueba, bloquean el número.
- Si mandas correos de promoción a gente en **EE. UU.**, CAN-SPAM pide lo mismo (baja fácil y tu
  domicilio en el correo).

### 15. 🟠 Seguridad de datos
- **Tenemos:** contraseñas cifradas, sesión segura, no guardamos tarjetas.
- **Falta (Yo):** un documento corto de qué hacer si hay una fuga (revisar, cerrar, **avisar a los
  clientes afectados**) y de quién tiene acceso a qué. La ley pide que las medidas estén
  **documentadas** (art. 18).
- **Nunca** pedir números de tarjeta por chat, WhatsApp o correo (PCI DSS y Mercado Pago).
- Multas de la LFPDPPP en 2026: de **$11,731 hasta $37.5 millones** según la gravedad.

### 16. 🟢 Aceptación de Términos con fecha
- **Yo:** guardar fecha y versión de los Términos aceptados al registrarse (hoy solo se guarda la de
  privacidad). Sirve de prueba ante un reclamo.

### 17. 🟢 Marca en el IMPI
- **Dónde:** **Marca en Línea** del IMPI, con e.firma. Antes, buscar que no exista en **MARCia**.
- **Costo:** **$3,126.41** (2026), ~10% menos pagando en línea. Dura 10 años. Clase 35 (venta)
  **[confirmar clases]**. El IMPI hace campañas con hasta 90% de descuento.

### 18. Municipio
- **Solo en línea, sin local:** normalmente nada más que el SAT; en algunos municipios, un aviso de
  apertura gratuito.
- **Con local:** licencia o aviso de funcionamiento, uso de suelo y Protección Civil. Depende del
  municipio (Duda 2).

### 19. Empleados
- **Si tienes:** alta patronal en el **IMSS**, impuesto estatal sobre nómina, contratos y NOM-035.
  Con contador.

### 20. 🟢 Lo de los videos que no es ley
Ya explicado en la conversación del 2026-10-07:
- Arreglar el **sitemap** (hoy manda a Google a una página que no existe y a pantallas con login).
- Fotos con el **nombre del artículo** ("Imagen variante" no sirve).
- **Título y descripción** por producto y datos estructurados.
- `noindex` en la página "no disponible".
- **Límite de registros** por IP.
- **Botón de WhatsApp** en la tienda.

---

### 21. 🔴 Nada de réplicas o falsificados
- Vender una bolsa o un perfume con una marca registrada que **no es original** es **delito**
  (LFPPI 2020, arts. 402–404): **de 3 a 10 años de prisión** si se vende de forma permanente o en
  establecimiento (una tienda en línea cuenta), y multa de miles de UMA. **Decir "réplica" no lo
  quita.** En octubre de 2026 el Senado aprobó endurecerlo.
- **Qué hacer (Tú):** solo vender **originales con factura del proveedor** o productos **sin marca
  ajena**. Si hay algo dudoso en el inventario, sacarlo de la tienda.

### 22. 🔴 Línea celular del negocio
- Toda línea debe quedar registrada con **CURP** (Ley de Telecomunicaciones 2025). La fecha límite va
  por el **último dígito** del número, del 15 de agosto al 31 de diciembre de 2026. Si no, **la
  suspenden** y se pierde el WhatsApp con los clientes.
- **Dónde:** la página o la app de tu compañía (Telcel, AT&T, Movistar…) con CURP e INE.

### 23. 🟠 OpenAI y servidores en otros países
- **Tú:** en la cuenta de OpenAI, aceptar el **acuerdo de tratamiento de datos (DPA)** para negocio.
- **Yo:** decirlo en el aviso de privacidad (punto 6).
- **No te aplica el GDPR** por tener el servidor en Francia (explicado en `LEGAL_OTROS_PAISES.md`).

### 24. 🟠 Reglas de Meta y TikTok
- **Sorteos:** aviso de que Meta y TikTok no patrocinan; no pedir "comparte en tu muro" ni "etiqueta
  a tus amigos"; en TikTok, los datos del sorteo en el video.
- **TikTok:** activar **"Divulgar contenido comercial" → "Tu marca"** en cada video de la tienda.

### 25. 🟢 Correo del negocio
- Gmail personal tiene límites de envío y no es para negocio. **Google Workspace** o un servicio de
  correo con `@novedades-jade.com.mx`; **yo** cambio la configuración del sistema.

### 26. Importación (solo si importas)
- **Padrón de Importadores** del SAT (gratis, en línea, hasta 10 días hábiles) y **sectores
  específicos** para textiles. Comprar por paquetería para revender puede contar como importación
  **[Contador]**.

### 27. 🟢 Distintivo Digital PROFECO
- Sello **voluntario** para tiendas en línea que cumplen. Se pide cuando ya estén los puntos 4, 5 y 6.

---

## Dudas que necesito que me contestes

| # | Pregunta | Para qué |
|---|---|---|
| 1 | ¿Nombre completo del responsable (o empresa), **domicilio que se pueda publicar**, teléfono y RFC? | Puntos 4, 5, 6 y 8 |
| 2 | ¿En qué **municipio y estado** está el negocio? ¿Tienes **local abierto al público** o es solo en línea y entregas? | Punto 18 |
| 3 | ¿Tienes **empleados** (aunque sea alguien por día o por comisión)? | Punto 19 |
| 4 | **Rifas:** ¿cómo se gana un boleto (comentar, compartir, comprar)? ¿Alguien paga por un boleto? ¿Cuánto valen los premios? ¿Cada cuánto rifas? | Punto 3 |
| 5 | **Perfumes:** ¿los decants los rellenas tú? ¿Los perfumes completos son originales con factura del distribuidor? | Puntos 10, 11 y 13 |
| 6 | ¿Ya tienes **contador**? ¿Cómo te pagan hoy (efectivo, transferencia, terminal Mercado Pago)? ¿Alguien te ha pedido factura? | Puntos 1 y 2 |
| 7 | ¿Mandas o quieres mandar **promociones** por WhatsApp, SMS o correo? | Punto 14 |
| 8 | **Envíos:** ¿cobras el envío? ¿Cuánto y en cuántos días llega? ¿Usas paquetería? | Punto 5 |
| 9 | ¿Cuántos **días** le das al cliente para reportar un defecto? ¿Cambias por talla o color? | Punto 5 |
| 10 | ¿Qué pasa con el dinero si un cliente cancela un **Apartado** o un **Ir pagando**: se lo devuelves o queda a favor? ¿Él escoge? | Puntos 5 y 7 |
| 11 | ¿Las **fotos** de la tienda son tuyas o del proveedor? | Riesgo de derechos de autor |
| 12 | ¿Vendes también en **Mercado Libre, Amazon** u otra plataforma? | Retenciones 2026 (punto 1) |
| 13 | ¿Algún producto es **réplica** o "AAA" de una marca? | Punto 21 |
| 14 | ¿Compras la mercancía a mayoristas **en México** o la **importas** (China, EE. UU., Shein, Temu…)? | Puntos 1 y 26 |
| 15 | La línea del **WhatsApp del negocio**: ¿a nombre de quién está y en qué dígito termina? ¿Ya la registraste con CURP? | Punto 22 |
| 16 | ¿Piensas publicar una **app** para clientes en Google Play o App Store? | `LEGAL_OTROS_PAISES.md` §5 |
| 17 | ¿Vendes o envías **fuera de México**? | `LEGAL_OTROS_PAISES.md` §6 |
| 18 | ¿Pagas **anuncios** en Facebook, Instagram, TikTok o Google? ¿Le pagas o regalas producto a alguien para que te promocione? | Puntos 13 y 24 |

---

## Orden sugerido

1. **Esta semana (tú):** contestar las dudas 1–18, sacar cita para la e.firma, **registrar la línea
   del negocio con CURP (22)** y **sacar de la tienda cualquier réplica (21)**.
2. **Con tus datos (yo):** puntos 4, 5, 6, 7, 8 y 16 en una rama, para que el abogado revise los
   textos ya armados.
3. **Contador:** puntos 1 y 2 (alta en RESICO y cómo facturar).
4. **Abogado (una sola consulta):** rifas (3), decants (10), revisión de Términos y Aviso (5 y 6).
   Llévale `LEGAL_MEXICO_INVESTIGACION.md` y este documento.
5. **Después:** IMPI (17), COFEPRIS (11), etiquetas con proveedores (12), OpenAI DPA (23), reglas de
   redes (24), correo del negocio (25), lo de los videos (20) y el Distintivo PROFECO (27).
