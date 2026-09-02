# Roadmap de pruebas — cambios en QA (2026-09-02)

Guía paso a paso para probar en el ambiente de QA todo lo que se subió hoy (backend hasta
`c3fa3ef`, frontend hasta `a16cc0a` — incluye las secciones 1 a 6 de siempre más lo agregado tras
tus comentarios: visibilidad de aceptación de privacidad para admin/cliente y la política de
cancelación/contracargos en Términos, sección 7). Cada bloque trae la **ruta de clics exacta** en
el menú (qué acordeón abrir, qué opción elegir) además de los pasos y qué esperar ver. No incluye
pasarelas de pago — eso vive aparte en `feature/pasarelas-pago` y no se toca hasta que se
apruebe.

---

## 0. Antes de probar nada — bloqueante

Confirma que estas dos migraciones ya corrieron contra la base de datos de QA
(`inventario_key_qa`). Si no corrieron, el backend va a tronar o los campos van a comportarse
como si no existieran:

```
src/main/resources/static/migration_privacidad_preferencias_correo.sql
src/main/resources/static/migration_umbral_stock_bajo.sql
```

Cómo confirmarlo rápido: en la BD de QA, `DESCRIBE usuario_modificacion;` debe mostrar
`acepto_privacidad` y `fecha_acepto_privacidad`; `DESCRIBE clientes;` debe mostrar
`recibir_correos`; `DESCRIBE configuracion_negocio;` debe mostrar `umbral_stock_bajo`.

Además, para probar los correos (seguimiento de pedido, stock bajo, restock) necesitas que el
SMTP de QA esté configurado y funcionando, y usar un correo real que puedas revisar.

---

## 1. Aviso de privacidad al registrarse

> 💬 **Tu comentario:** ¿aceptar el aviso de privacidad es lo mismo que aceptar que en una
> cancelación el cliente asume el cobro? ¿Hay que mostrar un mensaje al cancelar, de forma
> cordial y legal, sin decir directo que es "por la tarjeta"? Y el caso de fraude: alguien
> compra con tarjeta, recibe la mercancía, y luego dice que no reconoce la compra para quedarse
> con producto y dinero — ¿qué podemos hacer y cómo lo evitamos?
>
> **Respuesta:** son dos documentos distintos y el aviso de privacidad NO cubre ninguno de los
> dos. El aviso de privacidad (lo que ya existe en `/privacidad`) es sobre qué datos personales
> recaba la tienda y cómo los usa — no habla de cobros, cancelaciones ni reembolsos. Lo que
> describes (mensaje de cancelación, política de reembolso, y el caso de fraude con
> contracargo/"no reconozco la compra") es 100% del dominio de la pasarela de pago, y ya lo
> dejé anotado en `PASARELAS_PAGO_MP_OPENPAY_PAYPAL.md` sección 5 (reembolsos) — falta
> agregar ahí el tema puntual del fraude por contracargo ("friendly fraud"), que es real y
> conocido: Mercado Pago/OpenPay/PayPal manejan esto con su propio proceso de disputa (el
> negocio sube evidencia — confirmación de entrega, firma, dirección que coincide — y la red de
> la tarjeta decide), y la prevención pasa por exigir firma de recibido y activar 3D Secure en
> el checkout (traslada la responsabilidad del fraude al banco emisor cuando se usa
> correctamente). Esto se investiga a fondo cuando se diseñe el checkout de pasarelas, no antes
> — no aplica todavía porque hoy no hay cobro con tarjeta online.
>
> **Respuesta (¿dónde va este contenido — privacidad, términos, o juntos?):** en
> **Términos y condiciones** (`/termConditions`), NO en privacidad, y no conviene juntarlos —
> son dos fundamentos legales distintos en México (LFPDPPP para privacidad; protección al
> consumidor para las reglas de venta). Ya existe una sección real ahí, "Cambios, devoluciones
> y cancelaciones" — es donde hay que ampliar el tema de contracargos cuando se implemente el
> pago con tarjeta. No lo escribo todavía porque el texto depende de qué pasarela se elija y su
> política real de reembolso.
>
> **Respuesta (checkbox básico / no dice que hay que aceptar):** tenías razón en las dos cosas,
> ya corregido — ver "✅ Corregido" abajo.

**Ruta de clics:** cierra sesión (o abre una ventana privada) → en la pantalla de **Login**,
abajo del botón de iniciar sesión, dale clic a **"Regístrate aquí"**. Eso te lleva al formulario
de registro público.

**Pasos:**
1. Llena el formulario de registro (usuario, correo, contraseña) SIN marcar el checkbox de
   privacidad.
2. Intenta enviarlo.

Res => ya se ve que se acepto el aviso de privacidad

Res
curl 'https://qa.backend.novedades-jade.com.mx/mis-productos/v1/auth/verificar-correo' \
-H 'Accept: application/json, text/plain, */*' \
-H 'Accept-Language: es-419,es;q=0.7' \
-H 'Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJwYW50YWxsYXNBY2Npb25lcyI6WyJwcm9kdWN0b3MvYnVzY2FyOmRlc2Nhcmdhci1leGNlbCIsInByb2R1Y3Rvcy9idXNjYXI6ZmlsdHJvLXNpbi1pbWFnZW5lcyIsInByb2R1Y3Rvcy9idXNjYXI6aGFiaWxpdGFyIiwicHJvZHVjdG9zL2J1c2NhcjpjcmVhci12YXJpYW50ZXMiLCJ0aWVuZGEvYnVzY2FyOmZpbHRyby1zaW4taW1hZ2VuZXMiLCJwcm9kdWN0b3MvYnVzY2FyOmZpbHRyby1jb2RpZ28tcmVhbCIsInByb2R1Y3Rvcy9idXNjYXI6ZmlsdHJvLWNvbi1pbWFnZW5lcyIsInByb2R1Y3Rvcy9idXNjYXI6ZmlsdHJvLXNpbi1zdG9jayIsInRpZW5kYS9idXNjYXI6ZmlsdHJvLW5vLWhhYmlsaXRhZG9zIiwicHJvZHVjdG9zL2J1c2NhcjpmaWx0cm8tY29uLXN0b2NrIiwidGllbmRhL2J1c2NhcjpmaWx0cm8tY29kaWdvLWdlbmVyYWRvIiwicHJvZHVjdG9zL2J1c2NhcjpmaWx0cm8tZmVjaGEtY3JlYWNpb24iLCJ0aWVuZGEvYnVzY2FyOmZpbHRyby1mZWNoYS1jcmVhY2lvbiIsInRpZW5kYS9idXNjYXI6ZmlsdHJvLWNvbi1pbWFnZW5lcyIsInByb2R1Y3Rvcy9idXNjYXI6ZmlsdHJvLWhhYmlsaXRhZG9zIiwicHJvZHVjdG9zL2J1c2Nhcjpjb21wYXJ0aXItaW1hZ2VuIiwicHJvZHVjdG9zL2J1c2NhcjpmaWx0cm8tbm8taGFiaWxpdGFkb3MiLCJ0aWVuZGEvYnVzY2FyOmZpbHRyby1jb2RpZ28tcmVhbCIsInByb2R1Y3Rvcy9idXNjYXI6ZWxpbWluYXIiLCJ0aWVuZGEvYnVzY2FyOmZpbHRyby1zaW4tc3RvY2siLCJ0aWVuZGEvYnVzY2FyOmZpbHRyby1oYWJpbGl0YWRvcyIsInRpZW5kYS9idXNjYXI6ZmlsdHJvLWNvbi1zdG9jayIsInByb2R1Y3Rvcy9idXNjYXI6ZmlsdHJvLWNvZGlnby1nZW5lcmFkbyJdLCJyb2xlcyI6WyJQUk9EVUNUT1NfTEVFUiIsIkNMSUVOVEVTX0VMSU1JTkFSIiwiUEVESURPU19FTElNSU5BUiIsIkdBU1RPU19HRVNUSU9OQVIiLCJWQVJJQU5URVNfRURJVEFSIiwiVkVOVEFTX0NSRUFSIiwiUFJPRFVDVE9TX0VMSU1JTkFSIiwiVkFSSUFOVEVTX0xFRVIiLCJDTElFTlRFU19FRElUQVIiLCJWQVJJQU5URVNfQ1JFQVIiLCJWRU5UQVNfTEVFUiIsIlBFRElET1NfQ1JFQVIiLCJSSUZBU19HRVNUSU9OQVIiLCJQUk9EVUNUT1NfQ1JFQVIiLCJQQUdPU19MRUVSIiwiQ0xJRU5URVNfTEVFUiIsIk1QX0NPQlJBUiIsIlJPTEVfQURNSU4iLCJVU1VBUklPU19HRVNUSU9OQVIiLCJQRURJRE9TX0VESVRBUiIsIlBFRElET1NfTEVFUiIsIlBST0RVQ1RPU19FRElUQVIiLCJDTElFTlRFU19DUkVBUiIsIklNQUdFTkVTX0dFU1RJT05BUiJdLCJpZFVzdWFyaW8iOjQzLCJwYW50YWxsYXMiOlsidXN1YXJpb3MvYnVzY2FyIiwiYWRtaW4vcHJlc2VudGFjaW9uIiwicmlmYXMvYnVzY2FyIiwiY2hhdCIsInFyIiwiYWRtaW4vcHJvbW9jaW9uZXMiLCJyZXBvcnRlcyIsImZsb3Jlcy9yYW1vcyIsImFkbWluL2NpbnRhIiwibHVnYXJlcy1lbnRyZWdhIiwicHJvbW9jaW9uZXMiLCJnZXN0aW9uLW1lbnUvcm9sZXMiLCJob21lIiwicmlmYXMvbWVzIiwicHJvZHVjdG9zL2J1c2NhciIsImZsb3Jlcy9yYW1vcy1hZG1pbiIsImFkbWluL2RpYWdub3N0aWNvLWltYWdlbmVzIiwicmlmYXMvYWdyZWdhciIsImdhc3Rvcy9idXNjYXIiLCJhZG1pbi9oYXNodGFncyIsImZsb3Jlcy9lbnRyZWdhcyIsInBlZGlkb3MvbWlzLXBlZGlkb3MiLCJhZG1pbi9jaGF0IiwiY2FyZ2EtaW1hZ2VuZXMiLCJhZG1pbi9jYWNoZSIsImRhc2hib2FyZCIsImZsb3Jlcy9mcmFzZXMiLCJwZWRpZG9zL2hpc3RvcmlhbC1tcCIsInRpZW5kYS92ZW50YS1kaXJlY3RhIiwiYWRtaW4vbmVnb2NpbyIsInByb2R1Y3Rvcy9hZ3JlZ2FyIiwiYWJvbm9zIiwidGllbmRhL2J1c2NhciIsImdlc3Rpb24tbWVudSIsImNsaWVudGVzL2J1c2NhciIsImZhdm9yaXRvcyIsInBlcnNvbmFsaXphY2lvbiIsInRpZW5kYS92ZW50YSIsInBhbGFicmFzLWNsYXZlIiwiYWRtaW4vZmFjZWJvb2siLCJhZG1pbi9yZWNvbmNpbGlhY2lvbi1pbWFnZW5lcyIsImZsb3Jlcy9jYXRhbG9nb3MiLCJsb2dpbiIsImZsb3Jlcy9jb25maWd1cmFyIiwidGllbmRhL2Nhcmdhci1leGNlbCJdLCJwYW50YWxsYXNFc2NyaXR1cmEiOlsidXN1YXJpb3MvYnVzY2FyIiwiYWRtaW4vcHJlc2VudGFjaW9uIiwicmlmYXMvYnVzY2FyIiwiY2hhdCIsInFyIiwiYWRtaW4vcHJvbW9jaW9uZXMiLCJyZXBvcnRlcyIsImZsb3Jlcy9yYW1vcyIsImFkbWluL2NpbnRhIiwibHVnYXJlcy1lbnRyZWdhIiwicHJvbW9jaW9uZXMiLCJnZXN0aW9uLW1lbnUvcm9sZXMiLCJob21lIiwicmlmYXMvbWVzIiwicHJvZHVjdG9zL2J1c2NhciIsImZsb3Jlcy9yYW1vcy1hZG1pbiIsImFkbWluL2RpYWdub3N0aWNvLWltYWdlbmVzIiwicmlmYXMvYWdyZWdhciIsImdhc3Rvcy9idXNjYXIiLCJhZG1pbi9oYXNodGFncyIsImZsb3Jlcy9lbnRyZWdhcyIsInBlZGlkb3MvbWlzLXBlZGlkb3MiLCJhZG1pbi9jaGF0IiwiY2FyZ2EtaW1hZ2VuZXMiLCJhZG1pbi9jYWNoZSIsImRhc2hib2FyZCIsImZsb3Jlcy9mcmFzZXMiLCJwZWRpZG9zL2hpc3RvcmlhbC1tcCIsInRpZW5kYS92ZW50YS1kaXJlY3RhIiwiYWRtaW4vbmVnb2NpbyIsInByb2R1Y3Rvcy9hZ3JlZ2FyIiwiYWJvbm9zIiwidGllbmRhL2J1c2NhciIsImdlc3Rpb24tbWVudSIsImNsaWVudGVzL2J1c2NhciIsImZhdm9yaXRvcyIsInBlcnNvbmFsaXphY2lvbiIsInRpZW5kYS92ZW50YSIsInBhbGFicmFzLWNsYXZlIiwiYWRtaW4vZmFjZWJvb2siLCJhZG1pbi9yZWNvbmNpbGlhY2lvbi1pbWFnZW5lcyIsImZsb3Jlcy9jYXRhbG9nb3MiLCJsb2dpbiIsImZsb3Jlcy9jb25maWd1cmFyIiwidGllbmRhL2Nhcmdhci1leGNlbCJdLCJqdGkiOiJiNGZmODcyYS0wMTZlLTRmZjYtODA5YS0wNDI2ZDBhODk4ZDYiLCJzdWIiOiJhZG1pbiIsImlhdCI6MTc4ODM3MTkwOSwiZXhwIjoxNzg4MzcyODA5fQ.OchoEMLTXFf7ZJPi4c5v0pcqjAIs786fdV6AtfUlXzY' \
-H 'Connection: keep-alive' \
-H 'Content-Type: application/json' \
-b 'refreshToken=eyJhbGciOiJIUzI1NiJ9.eyJzZXNzaW9uU3RhcnQiOjE3ODgzNjkxNjMwNDUsImlkVXN1YXJpbyI6NDMsInNlc3Npb25JZCI6IjQ2YTY5ZDZjLTg5N2UtNDFlZC1iNWEzLWMxMDg2MmIxMjhlNCIsInR5cGUiOiJyZWZyZXNoIiwianRpIjoiOGQ1MTc3YjctMmE4MS00NjY3LWFmY2YtOWQ2NzgwMWVkMDAyIiwic3ViIjoiYWRtaW4iLCJpYXQiOjE3ODgzNzE5MDksImV4cCI6MTc4ODk3NjcwOX0.pzWP0lF5il9kqQzdfmvIGETg8RVogmPDEVVxB4epR84' \
-H 'Origin: https://qa.shop.novedades-jade.com.mx' \
-H 'Referer: https://qa.shop.novedades-jade.com.mx/' \
-H 'Sec-Fetch-Dest: empty' \
-H 'Sec-Fetch-Mode: cors' \
-H 'Sec-Fetch-Site: same-site' \
-H 'Sec-GPC: 1' \
-H 'User-Agent: Mozilla/5.0 (Linux; Android 15; Pixel 9) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Mobile Safari/537.36' \
-H 'sec-ch-ua: "Not;A=Brand";v="8", "Chromium";v="150", "Brave";v="150"' \
-H 'sec-ch-ua-mobile: ?1' \
-H 'sec-ch-ua-platform: "Android"' \
--data-raw '{"userName":"inicioSesion","codigo":"771964"}'
Codigo de verificacion invalido

**Qué esperar:** el botón "Registrarse" debe estar deshabilitado mientras el checkbox no esté
marcado — no debería dejarte enviar el formulario en absoluto.

**Pasos (continuación):**
3. Marca el checkbox "Acepto el aviso de privacidad".
4. Click en el link "aviso de privacidad" (antes de enviar).

**Qué esperar:** debe abrirse `/privacidad` en una pestaña nueva, con contenido real (no en
blanco).

> 💬 **Tu comentario:** "Al dar clic en el aviso de privacidad no se abre nada para que lo
> revises."
>
> **✅ Corregido.** El link estaba metido DENTRO del `<label>` que envuelve el checkbox — el
> navegador prioriza el toggle del checkbox sobre la navegación del link y por eso no abría
> nada. Lo saqué del label, como línea aparte con ícono ("↗ Leer el aviso de privacidad
> completo"), y de paso rediseñé toda la caja: ahora tiene fondo/borde propio, el checkbox es
> más grande, y el texto dice claramente "Acepto el **aviso de privacidad** para crear mi
> cuenta" en vez del checkbox suelto y sin contexto que había antes.

> 💬 **Tu comentario:** "Cuando se reenvía el código nuevo es necesario que el código que se
> puso hay que limpiarlo... porque ya lo reenvié 2 veces y dice que es incorrecto."
>
> **✅ Corregido.** Era un bug real, independiente de lo de privacidad — al reenviar, el campo
> se quedaba mostrando el código viejo (ya inválido en el back) y si lo mandabas sin
> retipearlo, claro que decía "incorrecto". Ahora al reenviar se limpia el campo
> automáticamente.

**Pasos (continuación):**
5. Completa el registro normal (verificación de correo incluida, como ya lo probaste antes).

**Qué esperar:** el registro se completa igual que siempre — este cambio no debe alterar el
flujo de verificación de correo que ya conocías.

**Verificación en BD (opcional, para admin/dev):**
```sql
SELECT username, acepto_privacidad, fecha_acepto_privacidad
FROM usuario_modificacion WHERE username = 'el_usuario_que_registraste';
```
Debe mostrar `acepto_privacidad = 1` y una fecha/hora reciente.

**Caso negativo a probar:** como ADMIN, menú lateral → acordeón **🛠️ Sistema** → **"👥 Usuarios"**
→ busca un usuario y edítalo ("Actualizar usuario"). **El checkbox de privacidad NO debe
aparecer ahí** — solo aplica al autoregistro.

> 💬 **Tu comentario:** "Ya entré como admin, pero no veo en la pantalla la opción que
> mencionas para validarlo — si lo acepta no aparece, pero se supone que para poder generar un
> registro tiene que aparecer o aceptar el aviso, ¿no?"
>
> **Respuesta:** lo que probaste salió bien — que NO aparezca ahí es el comportamiento
> correcto, no un bug. Son dos pantallas distintas con dos propósitos distintos:
> - **Registro público** (`/usuarios/registrar`, sección de arriba) — el checkbox SÍ aparece
>   y SÍ es obligatorio, porque ahí es donde una persona crea su cuenta por primera vez.
> - **Admin editando a otro usuario** ("Actualizar usuario") — el checkbox NO aparece a
>   propósito, porque el admin no está creando la cuenta, solo edita datos de alguien que ya
>   se registró (y ya aceptó, o no existiría la cuenta).
>
> Dicho esto, tu pregunta de fondo era válida — confirmaste que sí, sí o sí es obligatorio para
> registrarse (ya estaba así), y pediste que tanto el admin como el propio cliente pudieran
> verlo. **✅ Agregado:**
> - **Admin** — en "Actualizar usuario" (esta misma pantalla), arriba de todo, ahora se ve un
>   aviso: "✅ Aceptó el aviso de privacidad el DD/MM/AAAA HH:mm" (en rojo "❌ No aceptó..." si
>   es una cuenta vieja de antes de este control). Es de solo lectura, no se puede editar desde
>   ahí — se aceptó una sola vez, en el registro.
> - **El propio cliente** — en el menú de usuario → **"Mi perfil"**, arriba de "Datos de
>   cuenta", la misma info con link directo al aviso de privacidad.

**Ruta de clics (probar lo agregado — lado admin):** como ADMIN, menú lateral → acordeón
**🛠️ Sistema** → **"👥 Usuarios"** → busca el usuario que registraste en el paso 1 → botón para
editarlo ("Actualizar usuario").

**Qué esperar:** arriba de todo el formulario, antes de "Nombre de usuario", debe verse un aviso
de solo lectura: "✅ Aceptó el aviso de privacidad el DD/MM/AAAA HH:mm" (con ícono verde). Si
pruebas con una cuenta vieja (creada antes de este control), debe verse en rojo "❌ No aceptó el
aviso de privacidad...".

**Ruta de clics (probar lo agregado — lado cliente):** loguéate con el usuario que registraste →
menú lateral, hasta abajo, tarjeta con tu nombre → **"Mi perfil"**.

**Qué esperar:** arriba de la sección "Datos de cuenta" (antes del campo "Nombre de usuario")
debe verse el mismo aviso, con un link a **aviso de privacidad** que abre `/privacidad` en
pestaña nueva.

---

## 2. Preferencia de correos — lado del cliente

**Ruta de clics:** loguéate como cliente → en el menú lateral, hasta abajo hay una tarjeta con
tu nombre de usuario → ahí dale clic a **"Mis datos"** (ícono 👤).

**Pasos:**
1. Entra a "Mis datos" y busca la sección **"Preferencias"**, con un toggle que dice algo como
   "Recibir correos de seguimiento de pedido y alertas de stock".

**Qué esperar:** el toggle debe aparecer **activado** por default (así nace todo cliente nuevo o
existente que nunca lo tocó).

**Pasos (continuación):**
2. Apágalo.
3. Recarga la página completa (F5).

**Qué esperar:** el toggle debe seguir apagado después de recargar — si vuelve a aparecer
prendido, algo no se guardó bien.

**Pasos (continuación) — el caso que más importa probar:**
4. Con el toggle todavía apagado, edita cualquier OTRO dato (ej. tu número de teléfono) y dale
   click al botón grande **"Guardar cambios"** del formulario (no al toggle).
5. Recarga la página otra vez.

**Qué esperar:** el toggle debe **seguir apagado**. Si se prende solo después de guardar el
formulario general, es un bug — el diseño evita justo eso (el toggle usa un endpoint aparte a
propósito).

6. Vuelve a prenderlo, para dejarlo en su estado normal.

---

## 3. Preferencia de correos — lado del admin (por cliente)

**Ruta de clics:** como ADMIN, en el menú lateral (fuera de cualquier acordeón, es un ítem
suelto) → **👥 Clientes** → se abre el buscador → busca al cliente de prueba → botón
**"👁️ Ver/Editar"** sobre su fila.

**Pasos:**
1. Busca la misma sección "Preferencias" en esa pantalla.

**Qué esperar:** debe mostrar el estado real de ESE cliente (si en el paso 2 lo dejaste
apagado para algún cliente de prueba, aquí debe verse apagado).

**Pasos (continuación):**
2. Cámbialo desde aquí (como admin).
3. Vuelve a entrar como ese cliente (o recarga si ya estás logueado como él) → "Mis datos".

**Qué esperar:** el cambio que hizo el admin se refleja también del lado del cliente — es el
mismo dato, dos pantallas distintas para tocarlo.

---

## 4. Correo de seguimiento de pedido

**Requiere:** un cliente de prueba con correo real (que puedas revisar) y con la preferencia de
correos **activada** (ver sección 2).

**Ruta de clics (admin):** menú lateral → acordeón **📋 Pedidos** (dale clic para desplegarlo) →
**"Mis pedidos"**. Ahí, sobre el pedido del cliente de prueba, están los botones **"Confirmar
cobro"** y **"Cancelar"**.

**Pasos:**
1. Genera un pedido con ese cliente (flujo normal de compra, como Tienda o Arma tu ramo).
2. En Pedidos → Mis pedidos, sobre ese pedido, dale **"Confirmar cobro"** (lo pasa a "Entregado").

**Qué esperar:** al correo del cliente debe llegar un mensaje con asunto tipo
**"Tu pedido #X — Entregado — Novedades Jade"**, con el estado en una tarjeta destacada.

**Pasos (continuación):**
3. Genera otro pedido con el mismo cliente y dale **"Cancelar"** en la misma pantalla.

**Qué esperar:** debe llegar un correo "Tu pedido #X — cancelado — Novedades Jade".

**Caso negativo a probar:**
4. Apaga la preferencia de correos de ese cliente (sección 2 o 3).
5. Confirma o cancela otro pedido suyo.

**Qué esperar:** **NO debe llegar ningún correo** — pero el pedido sí se debe confirmar/cancelar
normalmente (el envío del correo es "silencioso": si falla o se omite, no debe romper la
operación del pedido).

---

## 5. Alerta de "volvió el stock" (Favoritos)

**Requiere:** un cliente de prueba con correo real y preferencia de correos activada.

**Ruta de clics (cliente) — marcar favorito:** loguéate como ese cliente → menú lateral →
**🛍️ Tienda** (ítem suelto, arriba del todo) → en cualquier tarjeta de producto, dale clic al
ícono de corazón 🤍 (esquina de la tarjeta) — se pone ❤️.

**Ruta de clics (admin) — editar el stock de esa misma variante:** menú lateral → acordeón
**📦 Catálogo** → **"🔍 Modelos"** → busca el producto que marcaste como favorito → ábrelo →
entra a la variante correspondiente (talla/color) → cambia el campo de stock.

**Pasos:**
1. Con el cliente de prueba, marca una variante como Favorito (ruta de arriba).
2. Como ADMIN, entra a esa variante y bájale el stock a **0** (guardar).
3. Como ADMIN, entra a la MISMA variante otra vez y súbele el stock (ej. a 10) — guardar.

**Qué esperar:** al correo del cliente debe llegar **"¡Ya volvió el stock! — Novedades Jade"**
con el nombre del producto y (si aplica) talla/color.

**Pasos (continuación) — probar que no duplica avisos:**
4. Edita la variante de nuevo sin que pase por 0 (ej. de 10 a 15, guardar).

**Qué esperar:** **NO debe llegar otro correo** — el aviso solo se dispara en la transición real
de sin-stock a con-stock, no en cualquier edición.

**Pasos (continuación):**
5. Bájala a 0 otra vez y vuelve a subirle stock.

**Qué esperar:** esta vez **sí debe volver a llegar** el correo — es un ciclo nuevo de
agotado→reabastecido.

---

## 6. Alerta de stock bajo al admin (digest diario)

**Ruta de clics:** como ADMIN, en el menú lateral, dale clic al acordeón **🛠️ Sistema** para
desplegarlo → ahí verás varias opciones, entre ellas **"🏪 Negocio & Contactos"** → dale clic →
se abre la pantalla de configuración del negocio → baja hasta la sección
**"📦 Alertas de stock bajo"** (es la última, después de Estado/Horario/Contactos).

**Pasos:**
1. Verifica que el campo de umbral muestre **5** por default (si nunca se ha tocado).
2. Cámbialo (ej. a 10) → **Guardar umbral**.
3. Recarga la página.

**Qué esperar:** debe seguir mostrando 10 (persistencia).

**Pasos (continuación) — probar el envío real:**
4. Asegúrate de que exista al menos una variante habilitada con stock ≤ el umbral configurado
   (bájale el stock a una de prueba si hace falta, por otra vía distinta a "guardarConImagenes"
   no cuenta — usa la pantalla normal de editar variante).
5. El envío real ocurre solo, todos los días a las **7:00 a.m.** — para probarlo sin esperar,
   pídele a quien tenga acceso al servidor/consola de QA que dispare manualmente el método
   `StockBajoService.verificarYNotificar()` (o espera a que sean las 7 a.m. en un ambiente donde
   el scheduler esté activo).

**Qué esperar:** cada usuario con rol ADMIN y correo activo debe recibir un correo
**"Aviso de stock bajo (N)"** con la lista completa de variantes bajas y su stock actual.

**Pasos (continuación):**
6. Sube el stock de esa variante por encima del umbral y vuelve a disparar el barrido.

**Qué esperar:** esa variante ya no debe aparecer en el correo (o no debe llegar correo si era
la única baja) — y en el log del backend debe verse
`StockBajoService: sin variantes en o por debajo del umbral`.

---

## 7. Términos y condiciones — cancelación con tarjeta y contracargos

Respuesta a tu comentario de la sección 1 sobre dónde debía ir el tema de cobros/cancelación con
tarjeta y fraude por contracargo: se agregó a Términos y condiciones, no a Privacidad. El texto es
general (no menciona ninguna pasarela específica todavía, porque aún no se elige ni implementa
una) — cuando se implemente la pasarela elegida, este texto se puede afinar con detalles propios
de esa pasarela si hace falta.

**Ruta de clics:** no requiere sesión — desde cualquier pantalla, en el pie de página o donde
esté enlazado, dale clic a **"Términos y condiciones"** (o navega directo a `/termConditions`).
También se puede llegar desde el checkbox de privacidad en el registro → aviso de privacidad →
ahí mismo hay link cruzado a Términos, o desde la sección "Privacidad" al final de la propia
página de Términos.

**Pasos:**
1. Entra a `/termConditions`.
2. Busca la sección **"Cambios, devoluciones y cancelaciones"** (a la mitad de la página).

**Qué esperar:** además del párrafo que ya existía (defectos de fabricación, cambios de
perfumería, cancelación antes de entrega), ahora deben verse dos párrafos nuevos:
- Uno explicando que la cancelación de un pago con tarjeta se resuelve con reembolso al mismo
  medio de pago (no en efectivo), procesado por el banco emisor, y que el tiempo en que se ve
  reflejado depende de las políticas del banco.
- Otro explicando que ante un problema con un cargo hay que contactar primero a Novedades Jade
  (no ir directo al banco), y qué pasa si de todos modos se presenta un contracargo no
  fundamentado (evidencia ante el banco/pasarela, posible suspensión de cuenta, acciones legales
  en caso de fraude comprobado).

---

## Checklist rápido para ir tachando

- [ ] Migraciones corridas en BD de QA (paso 0)
- [ ] Registro bloquea sin checkbox de privacidad; link a `/privacidad` funciona
- [ ] Checkbox de privacidad NO aparece cuando admin edita a otro usuario
- [ ] Toggle de correos en "Mis datos": persiste tras recargar
- [ ] Toggle de correos: sobrevive guardar otros campos del formulario (no se resetea)
- [ ] Toggle de correos visible y editable desde admin en clientes/mostrar
- [ ] Correo de seguimiento llega al confirmar pedido ("Entregado")
- [ ] Correo de seguimiento llega al cancelar pedido
- [ ] Correo de seguimiento NO llega si `recibirCorreos = false`
- [ ] Correo de restock llega al pasar de stock 0 a >0 en una variante favorita
- [ ] Correo de restock NO se duplica en ediciones que no cruzan por 0
- [ ] Umbral de stock bajo configurable y persiste
- [ ] Digest de stock bajo llega a todos los admin con la lista correcta
- [ ] Admin ve aviso de aceptación de privacidad (fecha) en "Actualizar usuario"
- [ ] Cliente ve el mismo aviso en "Mi perfil", con link a `/privacidad`
- [ ] Términos y condiciones muestra los 2 párrafos nuevos de cancelación/contracargos con tarjeta

---

**Si algo falla:** anota el paso exacto y lo que viste vs. lo que esperabas — con eso puedo ir
directo al archivo/línea en cuestión sin tener que re-investigar todo el flujo de nuevo.
