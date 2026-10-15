# Cumplimiento legal de la tienda en línea (México) — revisión 2026-09-29

> 📌 **Actualizado el 2026-10-07:** la investigación completa (fiscal, rifas, antilavado, etiquetado,
> COFEPRIS, IMPI, municipio) está en `LEGAL_MEXICO_INVESTIGACION.md`, y la lista de qué falta, quién
> lo hace y dónde se saca, en `LEGAL_PLAN_DE_ACCION.md`; otros países, en `LEGAL_OTROS_PAISES.md`. Este documento se queda como la revisión de
> los 15 puntos del primer video.

> ⚠️ **Esto no es asesoría legal.** Es una investigación para saber qué preguntar y qué corregir.
> Antes de publicar textos legales definitivos, que los revise un abogado (y el contador para lo
> fiscal). Cada dato trae su fuente al final; lo marcado **[Por confirmar]** no se pudo leer en la
> fuente oficial (varias páginas de gobierno están bloqueadas desde este entorno).

**Origen:** un video con 15 puntos que "hay que tener para no meterse en problemas". Aquí va cada
punto: qué dice de verdad la ley mexicana, cómo está hoy la tienda (`shop.novedades-jade.com.mx`) y
qué falta. Al final hay **4 riesgos que el video no menciona** y que pesan más que varios de sus puntos.

---

## Resumen — qué hacer primero

| Prioridad | Qué | Por qué |
|---|---|---|
| 🔴 Alta | **Rifas: revisar si necesitan permiso de SEGOB** | Sin permiso pueden clausurar y multar. Ver riesgo R1. |
| 🔴 Alta | **Domicilio físico y teléfono del negocio visibles** antes de comprar | Obligatorio por LFPC art. 76 bis. Hoy no aparecen en ningún lado. |
| 🔴 Alta | **Completar el Aviso de privacidad** (quién es el responsable y su domicilio, finalidades necesarias vs. voluntarias, cómo revocar el consentimiento) | Obligatorio por la nueva LFPDPPP (2025). |
| 🔴 Alta | **Corregir Términos:** "dentro de los siguientes días" (falta el número), garantía mínima de 90 días, derecho a cancelar en 5 días hábiles | Contradicen o no cumplen la LFPC. |
| 🟠 Media | Costo de envío y fecha aproximada de entrega **antes** de pagar | LFPC art. 7 bis / 76 bis. Confirmar en la pantalla de compra. |
| 🟠 Media | **Decants de perfume** (reenvasar) | Etiquetado (NOM-141) y marcas registradas. Ver riesgo R2. |
| 🟠 Media | Fotos: que sean propias o con permiso | Ley Federal del Derecho de Autor. |
| 🟢 Baja | Accesibilidad, formularios con teclado | Buena práctica; no es obligatorio para particulares. |
| ✅ Ya está | Reseñas solo de compradores verificados; sin analíticas ni cookies de publicidad; precios en pesos con impuestos | — |

---

## Los 15 puntos del video

### 1. Afirmaciones sin respaldo (publicidad)
- **Ley:** LFPC art. 32 — la información y publicidad no puede ser engañosa o abusiva. PROFECO
  clasifica la publicidad engañosa como falsa, tendenciosa, parcial o artificiosa, y cuenta como
  engañosa la publicidad disfrazada de opinión o reseña. Multas de hasta **$2,345,728** o el 10% de
  los ingresos brutos anuales.
- **Aplica:** sí.
- **Cómo está:** nada detectado en el código, pero el riesgo está en los textos de productos, las
  publicaciones y las respuestas del bot.
- **Falta:** no decir "original", "100% piel", "garantizado", "el más barato", etc. sin poder
  probarlo. Ojo con los perfumes: decir "original" de un decant o de un producto sin factura del
  distribuidor. Revisar que el bot (OpenAI) no invente características.

### 2. Términos y condiciones
- **Ley:** LFPC art. 76 bis (comercio electrónico) — antes de la compra hay que informar términos,
  condiciones, costos, cargos adicionales y formas de pago. NMX-COE-001-SCFI-2018 detalla qué
  informar (proceso de compra, pagos y facturación, devoluciones, cancelación). La NMX es **norma
  voluntaria**, pero PROFECO la usa de referencia.
- **Aplica:** sí.
- **Cómo está:** existe `/termConditions` y ya se ve en el pie de página (2026-09-29). Problemas:
  - ❌ "contáctanos **dentro de los siguientes días**" → **falta el número de días**.
  - ❌ No menciona la **garantía mínima de 90 días** (LFPC art. 77, reforma 2018: no puede ser menor
    a 90 días desde la entrega).
  - ❌ No menciona el **derecho a revocar la compra en 5 días hábiles** desde la entrega (LFPC
    art. 56, aplica a ventas por internet). El consumidor paga el flete de regreso.
  - ⚠️ "Los perfumes no tienen cambio una vez abiertos": puede chocar con el art. 56 **[Por
    confirmar con abogado]**. Mejor: "sin cambio si el sello fue abierto, salvo defecto, y sin
    afectar tu derecho de revocación".
  - ❌ No dice cómo pedir factura (NMX-COE).
  - ❌ No trae domicilio físico ni teléfono (ver punto 15).

### 3. Etiquetas claras
- **Ley:**
  - **Ropa (pantalones, blusas):** NOM-004-SE-2021, vigente desde el 14-ene-2023. La etiqueta
    **física** de la prenda debe traer marca, composición, talla, cuidados, país de origen y
    responsable. Aplica si la composición textil es de 50% o más. El QR es opcional y no sustituye
    la etiqueta.
  - **Perfumes:** NOM-141-SSA1/SCFI-2012 (cosméticos preenvasados). Los perfumes tienen algunas
    excepciones.
  - **Bolsas:** depende del material (textil → NOM-004; piel o sintético → revisar otra NOM).
    **[Por confirmar]**
- **Aplica:** sí, a la mercancía física; la responsabilidad principal es de quien fabrica o
  importa, pero quien vende no debe ofrecer productos sin etiqueta.
- **Falta:** revisar que la mercancía llegue etiquetada. En la tienda, poner composición y talla en
  la descripción ayuda (no está confirmado que sea obligatorio en línea).

### 4. Seguimiento de analíticas
- **Ley:** LFPDPPP + Lineamientos del Aviso de Privacidad (2013): si se usan cookies, web beacons o
  similares, hay que avisarlo y dar forma de desactivarlas.
- **Cómo está:** ✅ **no hay** Google Analytics, Meta Pixel, Hotjar ni similares (revisado en el
  código del front).
- **Falta:** nada hoy. **Si algún día se agrega el Pixel de Meta o Analytics**, hay que actualizar
  el aviso de privacidad y poner un aviso de cookies.

### 5. Consentimiento en formularios
- **Ley:** LFPDPPP — dar el aviso de privacidad al momento de pedir los datos; las casillas de
  consentimiento **no pueden venir marcadas**. La ley de 2025 pide separar finalidades
  **necesarias** (vender y entregar) de **voluntarias** (promociones, publicidad): las voluntarias
  requieren consentimiento aparte.
- **Cómo está:** el registro guarda `aceptoPrivacidad` y la fecha en que se aceptó ✅.
- **Falta:** confirmar que la casilla no venga marcada y que tenga enlace al aviso. Si se mandan
  promociones por correo o WhatsApp, esa finalidad debe ser **opcional**, con su propia casilla.

### 6. Sitio accesible
- **Ley:** la accesibilidad web **solo es obligatoria para gobierno** (Acuerdo de accesibilidad web
  de 2015). Para particulares **no hay obligación**; WCAG 2.x es la referencia.
- **Falta:** buena práctica, no urgente: textos alternativos en las fotos, contraste y tamaño de
  letra.

### 7. Copyright de imágenes
- **Ley:** Ley Federal del Derecho de Autor (fotos) y Ley Federal de Protección a la Propiedad
  Industrial (logos y marcas). Usar imágenes ajenas con fines comerciales sin permiso es infracción
  (multas y, en casos graves, delito).
- **Falta:** usar **fotos propias** o del proveedor **con permiso por escrito**. No usar fotos de
  otras tiendas ni de Google. Los **logos de marcas de perfumes** en publicaciones: cuidado (ver R2).

### 8. Consentimiento de cookies
- **Ley:** México **no exige un banner de cookies** como Europa; exige **avisar** en el aviso de
  privacidad y dar opción de desactivarlas si rastrean.
- **Cómo está:** ✅ solo hay una cookie de sesión (renovar el inicio de sesión) y almacenamiento
  local del carrito, y el aviso ya lo explica. Son necesarias para que funcione el sitio.
- **Falta:** nada mientras no haya cookies de rastreo.

### 9. Leyes locales
- **Qué revisar:**
  - **Licencia de funcionamiento municipal** si hay local físico (en el municipio donde esté).
  - **SAT:** RFC y régimen (ver sección 11.8 de `ALTA_NEGOCIO_META_TIKTOK.md`). La constancia debe
    traer el domicilio del negocio.
  - Las rifas son **federales** (SEGOB), no locales (ver R1).
- **[Por confirmar]** qué pide el municipio para venta en línea sin local.

### 10. Reseñas falsas
- **Ley:** LFPC art. 32 y PROFECO — las reseñas inventadas o pagadas sin decirlo son publicidad
  engañosa.
- **Cómo está:** ✅ **bien resuelto**: solo puede reseñar quien **compró** ese artículo (se revisa
  contra las ventas) y solo una reseña por cliente y producto.
- **Falta:** no borrar reseñas negativas reales para dejar solo las buenas (eso también es
  engañoso). Si se paga a alguien por promocionar, que diga que es publicidad.

### 11. Integraciones de terceros
- **Ley:** LFPDPPP — informar a quién se mandan los datos. La ley de 2025 pide una cláusula donde
  el titular acepte o no las **transferencias**.
- **Cómo está:** el aviso menciona correo, hosting, OpenAI, Meta y TikTok ✅.
- **Falta:**
  - Agregar **Mercado Pago** (procesa pagos) y **WhatsApp** (avisos al cliente), si se usan.
  - Decir que OpenAI procesa fuera de México (EE. UU.).
  - Distinguir **encargados** (hosting, correo, OpenAI: procesan por cuenta de la tienda) de
    **transferencias**, y agregar la cláusula de aceptación si aplica. **[Por confirmar con
    abogado]**

### 12. Política de cookies
- Igual que el punto 8. Basta con la sección de cookies del aviso de privacidad mientras no haya
  rastreo. No hace falta una página aparte.

### 13. Formularios usables con teclado
- Accesibilidad, buena práctica (punto 6). No es obligación legal para particulares.

### 14. Solo datos necesarios
- **Ley:** LFPDPPP — principio de proporcionalidad: pedir solo lo necesario para cada finalidad.
- **Cómo está:** el aviso lista cuenta, contacto, pedidos, entrega y conversaciones; parece
  proporcional.
- **Falta:** revisar que ningún formulario pida datos que no se usan (fecha de nacimiento, CURP,
  etc.).

### 15. Datos del negocio visibles
- **Ley:** LFPC art. 76 bis — **antes de la compra** el proveedor debe dar su **domicilio físico,
  teléfonos** y medios para reclamaciones.
- **Cómo está:** ❌ solo aparece el correo `contacto@novedades-jade.com.mx`. **No hay domicilio ni
  teléfono** en ninguna página.
- **Falta:** poner nombre del responsable, domicilio físico, teléfono y correo en Términos, en el
  Aviso de privacidad y en una página o pie de "Contacto". Si no se quiere publicar el domicilio de
  casa, se necesita un domicilio del negocio (se liga con el tema del SAT).

---

## Riesgos que el video NO menciona

### R1. 🔴 Rifas — permiso de SEGOB
- **Ley:** Ley Federal de Juegos y Sorteos y su Reglamento. Los sorteos **en todas sus modalidades**
  (con venta de boletos, instantáneos o de promoción comercial) requieren **permiso previo** de
  SEGOB (Dirección General de Juegos y Sorteos), que se tramita **al menos 10 días hábiles antes**.
  Hacer un sorteo sin permiso puede llevar a **clausura del establecimiento** y otras sanciones.
  También se necesita permiso para **anunciarlo**.
- **Concursos de habilidad sin azar** no van con SEGOB, pero se avisan a PROFECO con 3 días de
  anticipación (NOM-028-SCFI-2007).
- **Cómo está:** la tienda tiene un módulo de rifas (boletos, participación desde Facebook,
  Instagram y TikTok, ruleta para elegir ganador). Eso es **sorteo por azar**.
- **Falta:** **consultarlo con un abogado antes de la próxima rifa.** Muchos negocios hacen
  giveaways sin permiso, pero la ley no hace excepción clara para los gratuitos **[Por confirmar: no
  se pudo leer el Reglamento completo]**. Además, las reglas de Instagram, Facebook y TikTok piden
  bases publicadas y deslindar a la plataforma.

### R2. 🟠 Decants de perfume
- Un decant es perfume **reenvasado**. Quien reenvasa pasa a ser responsable del etiquetado
  (NOM-141) y usa una **marca registrada ajena** en un envase que la marca no fabricó (Ley Federal de
  Protección a la Propiedad Industrial). No es automáticamente ilegal, pero es **zona gris**.
  **[Por confirmar con abogado]**
- **Mínimo:** no presentarlo como producto de la marca; decir claramente "decant (muestra reenvasada)
  de…", con la cantidad en ml, y no usar el logo de la marca como si fuera oficial.

### R3. 🔴 Garantía y cancelación (ya contado en el punto 2)
90 días de garantía mínima y 5 días hábiles para revocar la compra en línea. Los Términos de hoy no
lo dicen y uno de sus párrafos podría contradecirlo.

### R4. 🟠 Aviso de privacidad incompleto para la ley de 2025
La nueva LFPDPPP (DOF 20-mar-2025, vigente desde el 21-mar-2025; el INAI desapareció y ahora
supervisa la **Secretaría Anticorrupción y Buen Gobierno**) pide en el aviso integral:
- [ ] **Identidad y domicilio del responsable** → hoy solo dice "Novedades Jade". Falta el nombre
      de la persona física o moral responsable y su domicilio.
- [x] Datos que se tratan (ya están; no se tratan datos sensibles).
- [ ] **Finalidades separadas en necesarias y voluntarias** → hoy es una sola lista.
- [ ] **Medios para limitar el uso o la divulgación** de los datos.
- [~] Mecanismo **ARCO** (acceso, rectificación, cancelación, oposición): hay correo, pero falta el
      **procedimiento** (qué mandar, en cuánto tiempo se responde).
- [ ] **Cómo revocar el consentimiento.**
- [x] Cómo se avisan los cambios (ya está).
- [ ] Cláusula de transferencias, si aplica (punto 11).
- [ ] **Aviso simplificado** en el formulario de registro (identidad, finalidades y dónde leer el
      completo).

---

## Siguientes pasos

1. **Datos que solo tiene el dueño** (sin esto no se pueden completar los textos): nombre del
   responsable (persona física o empresa), domicilio del negocio que se puede publicar, teléfono,
   RFC y si se emiten facturas, cuántos días para reportar un defecto, costos de envío.
2. **Abogado:** rifas (R1), decants (R2), redacción final de Términos y Aviso de privacidad.
3. **Código (cuando se tengan los datos):** actualizar `terminos.component.html` y
   `privacidad.component.html`, agregar contacto al pie de página, aviso simplificado y casilla
   opcional de promociones en el registro.

---

## Fuentes

- LFPC art. 76 bis: [juristas.mx](https://juristas.mx/en/laws/ley-federal-de-proteccion-al-consumidor/articulo-76-bis) · [PROFECO/Economía — comercio electrónico](https://www.gob.mx/cms/uploads/attachment/file/589920/COMERCIO_ELECTRONICO_P.SE_141020.pdf) · [Greenberg Traurig — reforma LFPC 2025](https://www.gtlaw.com/en/insights/2025/12/reformas-a-la-ley-federal-de-proteccion-al-consumidor)
- LFPC art. 56 (revocación 5 días): [mLey.mx](https://mley.mx/LFPC/articulo/56/) · [leyes-mx.com](https://leyes-mx.com/ley_federal_de_proteccion_al_consumidor/56.htm)
- LFPC art. 77 (garantía 90 días): [leyes-mx.com](https://leyes-mx.com/ley_federal_de_proteccion_al_consumidor/77.htm) · [IDC — reforma 2018](https://idconline.mx/corporativo/2018/01/11/cambios-a-la-ley-de-profeco) · [PROFECO — garantía y compras en línea](https://www.gob.mx/profeco/articulos/garantia-y-compras-en-linea?idiom=es)
- LFPC art. 7 bis (precio total): [LFPC texto vigente](https://www.diputados.gob.mx/LeyesBiblio/pdf/LFPC.pdf) · [Xataka — PROFECO y precios con impuestos](https://www.xataka.com.mx/videojuegos/profeco-notifica-a-playstation-mexico-deben-mostrar-precios-pesos-impuestos-incluidos-podrian-ser-multados)
- Publicidad engañosa y multas: [El Imparcial](https://www.elimparcial.com/mexico/2025/09/13/te-estan-enganando-asi-se-ve-la-publicidad-falsa-segun-profeco/) · [La Silla Rota](https://lasillarota.com/dinero/2025/6/3/profeco-alerta-usuarios-por-publicidad-enganosa-539254.html)
- NMX-COE-001-SCFI-2018: [DOF](https://www.dof.gob.mx/nota_detalle.php?codigo=5559015&fecha=30/04/2019) · [Economía](https://platiica.economia.gob.mx/normalizacion/nmx-coe-001-scfi-2018/) · [Foro Jurídico](https://forojuridico.mx/a-quien-le-aplica-la-norma-mexicana-de-comercio-electronico/)
- Nueva LFPDPPP 2025: [Garrigues](https://www.garrigues.com/es_ES/noticia/mexico-nueva-ley-federal-proteccion-datos-personales-posesion-particulares-introduce) · [IAPP](https://iapp.org/news/a/entendiendo-la-ley-federal-de-protecci-n-de-datos-personales-en-posesi-n-de-los-particulares-en-mexico) · [IDC — ajustes al aviso](https://idconline.mx/corporativo/2025/03/31/ajustes-en-el-aviso-de-privacidad-con-la-ley-de-proteccion-de-datos) · [texto de la ley](https://www.diputados.gob.mx/LeyesBiblio/pdf/LFPDPPP.pdf)
- Cookies: [Lineamientos del Aviso de Privacidad (DOF 2013)](https://www.dof.gob.mx/nota_detalle.php?codigo=5284966&fecha=17%2F01%2F2013) · [Hunton](https://www.hunton.com/privacy-and-cybersecurity-law-blog/mexico-issues-cookie-and-web-beacon-rules-and-privacy-notice-requirements) · [ECIJA](https://www.ecija.com/actualidad-insights/publicidad-digital-y-privacidad-lo-que-las-cookies-revelan-y-la-ley-aun-no-regula-del-todo/)
- NOM-004-SE-2021: [DOF](https://www.dof.gob.mx/nota_detalle.php?codigo=5640655&fecha=14%2F01%2F2022) · [Economía](https://platiica.economia.gob.mx/normalizacion/nom-004-se-2021/)
- NOM-141-SSA1/SCFI-2012: [DOF](http://dof.gob.mx/nota_detalle.php?codigo=5269348&fecha=19%2F09%2F2012) · [Economía](https://platiica.economia.gob.mx/normalizacion/nom-141-ssa1-scfi-2012/)
- Accesibilidad: [Acuerdo de accesibilidad web (DOF 2015)](https://dof.gob.mx/nota_detalle.php?amp=&codigo=5418749&fecha=03%2F12%2F2015) · [LGIPD](https://www.diputados.gob.mx/LeyesBiblio/pdf/LGIPD.pdf)
- Derecho de autor: [LFDA](http://www.ordenjuridico.gob.mx/Documentos/Federal/html/wo17068.html) · [Legalario](https://legalario.com/blog/uso-de-imagenes-en-internet/)
- Rifas: [Ley Federal de Juegos y Sorteos](https://www.diputados.gob.mx/LeyesBiblio/pdf/109.pdf) · [Reglamento](https://www.diputados.gob.mx/LeyesBiblio/regley/Reg_LFJS.pdf) · [SEGOB — requisitos para sorteos](http://www.juegosysorteos.gob.mx/es/Juegos_y_Sorteos/Requisitos_para_Sorteos) · [Xataka — ¿necesitan permiso?](https://www.xataka.com.mx/aplicaciones/confetti-q12-necesitan-permisos-segob-para-hacer-concursos-mexico-esto-dicen-empresas-esto-dice-ley) · [LinkedIn — giveaways](https://es.linkedin.com/pulse/regulaci%C3%B3n-de-los-concursos-en-l%C3%ADnea-giveaways-aranda-garc%C3%ADa) · [Legarreta](https://legarreta.mx/promociones-y-sorteos/)
