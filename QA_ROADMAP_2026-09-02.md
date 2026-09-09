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

> 💬 **Tu comentario:** curl real desde el navegador mostrando que `POST /v1/auth/verificar-correo`
> devuelve **"Codigo de verificacion invalido"** con `{"userName":"inicioSesion","codigo":"771964"}`
> (User-Agent Android/Chrome móvil) — "sigue con errores al validar el código del correo".
>
> **✅ Corregido — causa raíz encontrada.** No era el back (revisé `UsuarioVerificacionService`:
> comparación de código simple, 15 min de expiración, 5 intentos — todo correcto). El bug estaba
> en el front: la pantalla de verificar-correo solo sabía "ya mandé un código" a través de
> `history.state` (lo que Angular pasa al navegar). En **móvil**, si el navegador descarga la
> pestaña mientras el usuario va a revisar su correo (algo muy común: sales de la app a Gmail y
> el sistema operativo libera la pestaña en segundo plano) y luego regresa, Angular arranca de
> cero y ese `history.state` se pierde — la pantalla, sin avisar, disparaba **otro código nuevo**,
> invalidando el que el usuario ya tenía abierto en su bandeja. El código se veía perfecto, pero
> el back ya tenía guardado uno distinto — de ahí "código inválido" con un código que a simple
> vista estaba bien. Fix: se agregó un respaldo en `sessionStorage` (esto sí sobrevive a que el
> navegador descargue/restaure la pestaña) para no reenviar en silencio si ya hay un código
> vigente (mismo margen de 15 minutos que usa el back). Commit `c4848f0` (frontend).
>
> **Para volver a probar:** genera un código nuevo (registro o reintenta login sin verificar),
> ve a tu correo SIN cerrar ni cambiar de pestaña de la app hasta escribir el código — o si
> cambias de pestaña, hazlo con calma, el respaldo nuevo debería sostener el código vigente
> aunque el navegador descargue la pestaña de en medio.

> 💬 **Tu comentario:** "sigue igual Codigo de verificacion invalido" (después del fix de arriba,
> ya desplegado en QA — confirmado con el workflow de GitHub Actions, corrió y terminó bien).
>
> **✅ Se encontraron y corrigieron DOS causas más, independientes de la de arriba:**
> 1. **El input del código tenía `autocomplete="one-time-code"`** — ese atributo activa el
     >    autofill de SMS de Android/Chrome (sugiere o inserta automáticamente un código detectado en
     >    un mensaje de texto reciente). Este código llega por **correo**, no por SMS: si el celular
     >    tenía cualquier otro SMS con un código de 6 dígitos alrededor de esa hora (banco,
     >    paquetería, verificación en dos pasos de otra app), Android pudo autocompletar el campo con
     >    ESE código ajeno sin que se notara — el usuario ve 6 dígitos y los manda creyendo que son
     >    los correctos, pero no son los que llegaron al correo. Se quitó el atributo.
> 2. **Bug real en el envío automático de código** (login con cuenta sin verificar, y registro
     >    nuevo): el front navegaba a la pantalla de verificación marcando `codigoEnviado: true`
     >    **aunque el envío del código hubiera fallado** (red, límite de intentos, etc.) — si fallaba,
     >    la pantalla de verificación creía que ya existía un código válido esperando y nunca
     >    reintentaba ni mostraba ningún error; el usuario quedaba escribiendo contra un código que
     >    nunca llegó a generarse. Ahora ese flag solo se manda cuando el envío sí tuvo éxito.
>
> Commit `fe537ae` (frontend), ya en `qa`. **Si al volver a probar sigue fallando**, lo más útil
> que me puedes mandar es: (a) otro curl igual de completo al que mandaste, (b) si fue en el
> mismo celular con otros SMS de códigos llegando cerca de esa hora, y (c) si fue inmediatamente
> después de recibir el correo o pasó un rato/cambiaste de pantalla de en medio — con eso puedo
> ir directo a la causa exacta en vez de seguir probando teorías a ciegas.

> 💬 **Tu comentario:** seguía fallando, con evidencia contundente: revisaste la BD (`SELECT
> codigo_verificacion, codigo_verificacion_expira, intentos_codigo_verificacion...`) y el código
> guardado coincidía EXACTO con el que mandabas, sin intentos fallidos registrados — y aun así
> "Codigo de verificacion invalido". Y lo probaste también desde el modal de admin ("Actualizar
> usuario" → "✉️ Verificar correo"), con el mismo resultado.
>
> **✅ Causa raíz real, encontrada con esa evidencia.** `enviarCodigoVerificacion()` (el método
> que manda el código) **siempre generaba y mandaba uno nuevo, sin importar si ya había uno
> vigente sin usar** — a diferencia de `solicitarCambioCorreo()` (el de cambio de correo), que
> ya tenía protección para esto. Cada vez que se abre la pantalla de verificación, cada intento
> de login con una cuenta sin verificar, y **cada apertura del modal "Verificar correo" del
> admin** disparaban un envío que invalidaba en silencio el código que ya estaba en tu correo —
> sin avisar nada. Si probaste el modal de admin más de una vez (normal al estar probando), cada
> apertura mandó un código distinto y el que tenías copiado dejó de ser válido, aunque coincidiera
> con el que viste en un correo anterior.
>
> **Fix:** se agregó `forzarNuevo` (default `false`) — los envíos automáticos ahora reutilizan el
> código vigente si no expiró, en vez de invalidarlo. El botón explícito "Reenviar código" sigue
> mandando uno nuevo siempre. Commits `39aa06f` (backend) + `5b07104` (frontend), ya en `qa`.
>
> **Para volver a probar:** abre el modal de admin o la pantalla de verificación UNA sola vez,
> usa el código de ESE correo (ya no debería importar si lo vuelves a abrir después — el código
> se mantiene vigente mientras no expire).

> 💬 **Tu comentario:** con ese fix ya avanzó, pero apareció un error nuevo y distinto en el log
> del back: `Field 'nombre_persona' doesn't have a default value` al verificar el correo de
> "inicioSesion" — un `INSERT INTO clientes` que tronaba.
>
> **✅ Corregido — y es buena señal:** este error confirma que el fix anterior sí funcionó (ya
> pasó la comparación del código, por eso llegó hasta el paso de auto-crear el Cliente vinculado
> al verificar). El bug era otro, en `crearClienteDesdeRegistro()`: inserta el Cliente auto-
> creado sin nombre/apellido paterno a propósito (todavía no existen, por eso
> `datos_completos = 0`), pero esas dos columnas seguían **NOT NULL sin default en la BD real**
> — a diferencia de correo/teléfono/apellido materno, que sí se habían vuelto opcionales en una
> migración anterior. Se mandan como `''` (vacío) en vez de alterar la tabla: la app ya trata
> `''` igual que `NULL` para estos dos campos. Commit `0c18308` (backend), ya en `qa`. No requiere
> correr ninguna migración nueva.

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

aqui un detalle, estoy revisando que para el cliente estas mostrando todos los filtro y se supune que eso se lo tiene que asignar
el admin, pero ya revise admin y no lo veo asi
Ahora necesito aclarar estos punto, se supone que lo que yo queria era lo que tengo en la pantalla poderlo asignar si asi quisiera
Ejemplo
tienda/buscar
Lo primero es revisar si este uri el cliente lo puede visitar o tiene acceso este es un ejemplo porque se supone
que este es publico pero digamos que no lo es, entonces primero el usuario tieiene este permiso
Por eso te decia que tenemos que tener todas las uri para asignarlas a los usuarios
Por ejemplo creo un rol Solo tienda por ejemplo entonces ya primero veo todas las rutas que existen pero claro tienen
que estar como unidas a que me refiero si ponemos esta uri tenemos que tener todo lo que tiene esta uri
ejemplo un buscador, que ese buscador al poner algo hace la peticion a un endpoint eso tambnien tenerlo para configurar que pasa si no quiero que tenga esa opcion
esta misma pantalla tiene el carrito lo mismo poner el carrito y la uri a la que hace la peticion para saber si se lo damos,
Tambien tiene los filtros pero me gustaria que fueran idividuales para solo agregar uno aqui lo mismo si agrego un filtro pues si debe dejar hacer la busqueda por ese filtro
Y asi con las demas opciones, ahora ya termine esta pantalla
Ahora me voy a catalogo modelos, lo mismo esta opcion se la asigno al usuario la podria ver, si por alguna razon no se la asigno entinces no tendria permiso a nuinguna de este modelo por ejemplo a la mejor
Puede pasar que no le tiene permiso para ese uri pero a la mejor se quedo seleccionado otros permisos dentro de ese modelo
Entonces en postman si podria hacer las peticiones entinces si no tiene ese menu no puede acceder a ningun otro endpoint entiendes?
Te lo digo porque modelo es similar a tienda solo que aca son los productos y tiene casi los mismos filtros igual lo mismo que en tienda
Otra pantalla CATALOGO AGREGAR MODELO y asi para los demas creo que el fitro solo es el que mas importa lo demas tiene que tener acceso o que le asigne ese permiso
Por eso te digo que tenemos que seleccionar o tener esas opciones ojo cada opcion que aparezca digamos en la asignacion de roles y permisso
por ejemplo eta la uri tienda/modelo, esta si es conocida peroq ue pasa si pones otra ruta que a la mejor no se
que es o a donde lleva entonces lo que necesito es no se si poner un link para que me muestre que es o a donde nos llevaria o que haria en esa pantalla
para tener claro las cosas?

otro menu que tienes que hacer similar a tienda buscar es el de pedidos hay varias cosas que quisiera asignar, por ejemplo
Los buscadores, ovio si no doy acceso a un buscador sus botones no tienen que aparecer pero si puede aparecen el buscador y solo 1 boton
por eso todo tiene que ser con permiso
y cada card de pedido tiene una opcion o un boton que tambien tiene que ser administrable para que lo revises bien y saques todos los endpoint
y EN PEDIDOS EN LA CARD DICE DETALLE LO SELECCIONAMOS Y NOS LLEVA A OTRA PANTALLA QUE TIENE botones lo mismo que sean administrables

entonces en los permisos como lo veo es asi
Asi quedaria

Menu
Pedido y aqui no se como se maneje si no hace peticiones a ningun lado pues lo dejamos pero si hace peticiones pues mencionarlas para saber que hace cuando selecciona ese item del menu
hISTORIAL MERCADO PAGO ES BOTON Ycomo algo qu diga que hace ese boton y a donde hace las peticiones para saber si lo activo o doy permiso o no
buscador pedido lo mismo a donde hace peticion y que hace
pagados lo mismo
cancelados
filtro por lugar lo mismo saber a done hace la peticion y que hace y para que y puedo asignar a un usuario
normal
apartado
ir pagando lo mismo
ESTAN LAS CARD AQUI por cada card
detalle
editar ramo
imprimir ticket
reenviar tiket
como llegar

y tambien los botones de chatbol y las redes sociales entiendes hasta aqui, no hagas nada primero tenemos que entender las cosas para seguir

para que revises por ejemplo tienda buscar y modelo buscar o producto buscar, ahi esta mostrando los filtros a un cliente norma
y se supone que eso es configurable aprte que ahorita ya van a ser mas configurables
---

## 2. Preferencia de correos — lado del cliente

**Ruta de clics:** loguéate como cliente → en el menú lateral, hasta abajo hay una tarjeta con
tu nombre de usuario → ahí dale clic a **"Mis datos"** (ícono 👤).

**Pasos:**
1. Entra a "Mis datos" y busca la sección **"Preferencias"**, con un toggle que dice algo como
   "Recibir correos de seguimiento de pedido y alertas de stock".

**Qué esperar:** el toggle debe aparecer **activado** por default (así nace todo cliente nuevo o
existente que nunca lo tocó).
Pero aqui pense que los correos de stock iba a ser para el admin es decir para mi, porque para el cliente si no hace el pedido solo se hace si tiene stock y no se guarda en base que quiso algo que ya no hay y sobre los correo tambien debe a ver un chec en donde le podamos decir o enviar las promociones que tenemos si en los clientes tiene el chec seleccionado entonces les llegaria y lo mismo cada cliente tiene que tener los chec para des habilitar lso correos, actualmente lo que se me ocurre es que tengan el check de las promociones nada mas y cuando yo haga una promocion envio los correos pero como te habia dicho en las promociones una opcion para prgramar enviar de 10 en 10 coprreo a todos los que tengan el check seleccionado y en ese correo tiene que mencionarle que si no quiere recibir los correos explicarle en dodne ir para quitarlos
Y hace fa;lta hacer eso en las promociones para poder hacer pruebas de enviar el correo en una promocion
Y tambien explicarle donde encontraria las promociones
Y esos si cuando se hace un pedido de parte del cleinte o del admin tiene que envioar el correo del ticket y para el clinete si el cliente hace un pedido me tiene que llegar un correo avisando que hizo un pedido

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
Aqui para hacer pruebas lo que hace falta es es poder enviar correos de promocion no? por ejemplo yo como admin
genero promociones y se las puedo enviar a lso que quieren recibir promociones y eso pero aqui hay que ver como los enviamos
no sea que nos banien el correo no se tu dime si lo ves asi me refiero a enviar cada cierto tiempo cada 5 min por ejemplo
pro cuando active la opcion es decir hagao la promocion y aparece quieres enviar la promocion en este correo y digo que si entonces se envian los 5 primeros
pasan 5 min y se envian a los otros 5 y asi, 5 es un decir que sea configurable tambien y otra cosa que puede pasar que diga
ya no quiero que me lleguen corres y pues ya no le llegan solo los de restablecimiento de contra solo ese seria como el mas importante no?



y YA QUE ANDO REVISANDO AQUI
Primero
Una persona puede entrar a la tienda sin tener que estar registrada, puede ver los productos y hasta puede agregar al carrito
pero lo que no puede hacer es generar el pedido porque primero se tiene que dar de alta como usuario y
entonces ahi cuando de generar pedido si no esta registrado tiene que mandar el mensaje que ya esta que dice que para genrar
el pedido se tiene que registrar primero me parece y no debe dejar generar pedido ni nada
entonces cuando el usuario se registra la opcion cliente se da de alta pero todo vacio solo el id se genera porque porque
para que pase el usuario tiene que ir a registrar sus datos para poder genrar un pedido para saber quien es entonces la segunda es
Si el cliente esta registrado solo registrados y aun no registra datos en cleinte, tiene que aparecer el modal de para generar el pedido tiene que llenar sus datos y mandarlo
a la pantalla de llenar sus datos del cliente y ya al final si tiene todo listo entonces ya puede hacer pedido, lo que esta pasando ahorita es acabo de generar un usuario y el cliente se genero
con el puro id y actualmene quiero generar el pedido pero dice que no encuentra el cliente y en este caso el cliente ya deberia estar ahora deberia mencionar que para generar un pedido
tiene que generar o llenar sus datos y con l opciona enviarlo me parece que eso ya esta solo es revisarlo bien y otra cosa cuando agrego cosas al carrito y me envia a generar el pedido
aparece la opcion de enviar  o lugar de entrega y eso no debe aparecer aca o si? porque si hace un pedido y lo paga en linea tiene que pasar al local a recogerno no? o como lo ves?
Hay que revisar porque a veces me dice o me aparece el modal que tengo que llenar mis datos y a veces sale el error que te menciono para que revises bien
Esto lo hice como cliente o usuario normal
req
curl 'https://qa.backend.novedades-jade.com.mx/mis-productos/v1/clientes/buscarPorIdCliente/28' \
-H 'Accept: application/json, text/plain, */*' \
-H 'Accept-Language: es-419,es;q=0.5' \
-H 'Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJwYW50YWxsYXNBY2Npb25lcyI6WyJwcm9kdWN0b3MvYnVzY2FyOmZpbHRyby1uby1oYWJpbGl0YWRvcyIsInByb2R1Y3Rvcy9idXNjYXI6ZWxpbWluYXIiLCJwcm9kdWN0b3MvYnVzY2FyOmZpbHRyby1jb24taW1hZ2VuZXMiLCJwcm9kdWN0b3MvYnVzY2FyOmZpbHRyby1jb2RpZ28tZ2VuZXJhZG8iLCJwcm9kdWN0b3MvYnVzY2FyOmZpbHRyby1jb24tc3RvY2siLCJwcm9kdWN0b3MvYnVzY2FyOmNyZWFyLXZhcmlhbnRlcyIsInByb2R1Y3Rvcy9idXNjYXI6ZmlsdHJvLWZlY2hhLWNyZWFjaW9uIiwicHJvZHVjdG9zL2J1c2Nhcjpjb21wYXJ0aXItaW1hZ2VuIiwicHJvZHVjdG9zL2J1c2NhcjpmaWx0cm8tc2luLXN0b2NrIiwicHJvZHVjdG9zL2J1c2NhcjpmaWx0cm8tc2luLWltYWdlbmVzIiwicHJvZHVjdG9zL2J1c2NhcjpkZXNjYXJnYXItZXhjZWwiLCJwcm9kdWN0b3MvYnVzY2FyOmZpbHRyby1oYWJpbGl0YWRvcyIsInByb2R1Y3Rvcy9idXNjYXI6aGFiaWxpdGFyIiwicHJvZHVjdG9zL2J1c2NhcjpmaWx0cm8tY29kaWdvLXJlYWwiXSwicm9sZXMiOlsiUEVESURPU19DUkVBUiIsIlBST0RVQ1RPU19MRUVSIiwiUk9MRV9VU1VBUklPIiwiUEVESURPU19MRUVSIl0sImlkVXN1YXJpbyI6NzEsInBhbnRhbGxhcyI6W10sInBhbnRhbGxhc0VzY3JpdHVyYSI6W10sImp0aSI6Ijk4Nzg5Y2QyLTU2YWMtNDczNS1hYjdiLWMzNTM1NDBjOGNjMiIsInN1YiI6ImluaWNpb1Nlc2lvbiIsImlhdCI6MTc4ODM4MjYxMiwiZXhwIjoxNzg4MzgzNTEyfQ.pR77m7YKRLgjRpv8SYv9Kxbv4b75QGZeh1V7CKWbtYw' \
-H 'Connection: keep-alive' \
-H 'Origin: https://qa.shop.novedades-jade.com.mx' \
-H 'Referer: https://qa.shop.novedades-jade.com.mx/' \
-H 'Sec-Fetch-Dest: empty' \
-H 'Sec-Fetch-Mode: cors' \
-H 'Sec-Fetch-Site: same-site' \
-H 'Sec-GPC: 1' \
-H 'User-Agent: Mozilla/5.0 (Linux; Android 15; Pixel 9) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Mobile Safari/537.36' \
-H 'sec-ch-ua: "Not;A=Brand";v="8", "Chromium";v="150", "Brave";v="150"' \
-H 'sec-ch-ua-mobile: ?1' \
-H 'sec-ch-ua-platform: "Android"'

resp
{
"mensaje": "Could not read JSON:Cannot construct instance of `java.util.Optional` (no Creators, like default constructor, exist): cannot deserialize from Object value (no delegate- or property-based Creator)\n at [Source: (byte[])\"{\"@class\":\"com.ventas.key.mis.productos.models.ResponseGeneric\",\"mensaje\":\"La peticion fue exitosa\",\"code\":200,\"data\":{\"@class\":\"java.util.Optional\",\"empty\":false,\"present\":true},\"lista\":null}\"; line: 1, column: 150] (through reference chain: com.ventas.key.mis.productos.models.ResponseGeneric[\"data\"]) ",
"code": 400,
"data": null,
"lista": null
}
y comoa dmin
curl 'https://qa.backend.novedades-jade.com.mx/mis-productos/v1/ventas/save' \
-H 'Accept: application/json, text/plain, */*' \
-H 'Accept-Language: es-419,es;q=0.5' \
-H 'Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJwYW50YWxsYXNBY2Npb25lcyI6WyJ0aWVuZGEvYnVzY2FyOmZpbHRyby1jb24taW1hZ2VuZXMiLCJ0aWVuZGEvYnVzY2FyOmZpbHRyby1uby1oYWJpbGl0YWRvcyIsInByb2R1Y3Rvcy9idXNjYXI6ZmlsdHJvLXNpbi1pbWFnZW5lcyIsInByb2R1Y3Rvcy9idXNjYXI6Y29tcGFydGlyLWltYWdlbiIsInRpZW5kYS9idXNjYXI6ZmlsdHJvLXNpbi1pbWFnZW5lcyIsInByb2R1Y3Rvcy9idXNjYXI6ZmlsdHJvLWhhYmlsaXRhZG9zIiwicHJvZHVjdG9zL2J1c2NhcjplbGltaW5hciIsInByb2R1Y3Rvcy9idXNjYXI6ZmlsdHJvLWNvbi1zdG9jayIsInRpZW5kYS9idXNjYXI6ZmlsdHJvLWNvbi1zdG9jayIsInByb2R1Y3Rvcy9idXNjYXI6ZmlsdHJvLXNpbi1zdG9jayIsInByb2R1Y3Rvcy9idXNjYXI6ZmlsdHJvLW5vLWhhYmlsaXRhZG9zIiwicHJvZHVjdG9zL2J1c2NhcjpmaWx0cm8tY29kaWdvLXJlYWwiLCJ0aWVuZGEvYnVzY2FyOmZpbHRyby1jb2RpZ28tcmVhbCIsInByb2R1Y3Rvcy9idXNjYXI6Y3JlYXItdmFyaWFudGVzIiwidGllbmRhL2J1c2NhcjpmaWx0cm8taGFiaWxpdGFkb3MiLCJ0aWVuZGEvYnVzY2FyOmZpbHRyby1jb2RpZ28tZ2VuZXJhZG8iLCJwcm9kdWN0b3MvYnVzY2FyOmRlc2Nhcmdhci1leGNlbCIsInRpZW5kYS9idXNjYXI6ZmlsdHJvLXNpbi1zdG9jayIsInByb2R1Y3Rvcy9idXNjYXI6aGFiaWxpdGFyIiwicHJvZHVjdG9zL2J1c2NhcjpmaWx0cm8tY29uLWltYWdlbmVzIiwicHJvZHVjdG9zL2J1c2NhcjpmaWx0cm8tY29kaWdvLWdlbmVyYWRvIiwicHJvZHVjdG9zL2J1c2NhcjpmaWx0cm8tZmVjaGEtY3JlYWNpb24iLCJ0aWVuZGEvYnVzY2FyOmZpbHRyby1mZWNoYS1jcmVhY2lvbiJdLCJyb2xlcyI6WyJQUk9EVUNUT1NfTEVFUiIsIkNMSUVOVEVTX0VMSU1JTkFSIiwiR0FTVE9TX0dFU1RJT05BUiIsIlBFRElET1NfRUxJTUlOQVIiLCJWQVJJQU5URVNfRURJVEFSIiwiVkVOVEFTX0NSRUFSIiwiUFJPRFVDVE9TX0VMSU1JTkFSIiwiVkFSSUFOVEVTX0xFRVIiLCJDTElFTlRFU19FRElUQVIiLCJWQVJJQU5URVNfQ1JFQVIiLCJWRU5UQVNfTEVFUiIsIlBFRElET1NfQ1JFQVIiLCJSSUZBU19HRVNUSU9OQVIiLCJQUk9EVUNUT1NfQ1JFQVIiLCJQQUdPU19MRUVSIiwiTVBfQ09CUkFSIiwiQ0xJRU5URVNfTEVFUiIsIlJPTEVfQURNSU4iLCJVU1VBUklPU19HRVNUSU9OQVIiLCJQRURJRE9TX0VESVRBUiIsIkNMSUVOVEVTX0NSRUFSIiwiUEVESURPU19MRUVSIiwiUFJPRFVDVE9TX0VESVRBUiIsIklNQUdFTkVTX0dFU1RJT05BUiJdLCJpZFVzdWFyaW8iOjQzLCJwYW50YWxsYXMiOlsiYWRtaW4vY2FjaGUiLCJhZG1pbi9wcm9tb2Npb25lcyIsImFib25vcyIsImFkbWluL3ByZXNlbnRhY2lvbiIsImhvbWUiLCJmYXZvcml0b3MiLCJmbG9yZXMvcmFtb3MtYWRtaW4iLCJ0aWVuZGEvY2FyZ2FyLWV4Y2VsIiwicGVkaWRvcy9oaXN0b3JpYWwtbXAiLCJwcm9kdWN0b3MvYnVzY2FyIiwiZmxvcmVzL2ZyYXNlcyIsInBlZGlkb3MvbWlzLXBlZGlkb3MiLCJkYXNoYm9hcmQiLCJhZG1pbi9uZWdvY2lvIiwiY2hhdCIsInJpZmFzL21lcyIsImFkbWluL3JlY29uY2lsaWFjaW9uLWltYWdlbmVzIiwibG9naW4iLCJmbG9yZXMvcmFtb3MiLCJ0aWVuZGEvdmVudGEiLCJyaWZhcy9idXNjYXIiLCJwZXJzb25hbGl6YWNpb24iLCJhZG1pbi9kaWFnbm9zdGljby1pbWFnZW5lcyIsInFyIiwiZmxvcmVzL2NvbmZpZ3VyYXIiLCJ1c3Vhcmlvcy9idXNjYXIiLCJhZG1pbi9jaGF0IiwiZmxvcmVzL2VudHJlZ2FzIiwiYWRtaW4vaGFzaHRhZ3MiLCJjbGllbnRlcy9idXNjYXIiLCJmbG9yZXMvY2F0YWxvZ29zIiwiYWRtaW4vZmFjZWJvb2siLCJnYXN0b3MvYnVzY2FyIiwicHJvZHVjdG9zL2FncmVnYXIiLCJwYWxhYnJhcy1jbGF2ZSIsImFkbWluL2NpbnRhIiwidGllbmRhL2J1c2NhciIsInJpZmFzL2FncmVnYXIiLCJnZXN0aW9uLW1lbnUvcm9sZXMiLCJ0aWVuZGEvdmVudGEtZGlyZWN0YSIsImx1Z2FyZXMtZW50cmVnYSIsImNhcmdhLWltYWdlbmVzIiwiZ2VzdGlvbi1tZW51IiwicHJvbW9jaW9uZXMiLCJyZXBvcnRlcyJdLCJwYW50YWxsYXNFc2NyaXR1cmEiOlsiYWRtaW4vY2FjaGUiLCJhZG1pbi9wcm9tb2Npb25lcyIsImFib25vcyIsImFkbWluL3ByZXNlbnRhY2lvbiIsImhvbWUiLCJmYXZvcml0b3MiLCJmbG9yZXMvcmFtb3MtYWRtaW4iLCJ0aWVuZGEvY2FyZ2FyLWV4Y2VsIiwicGVkaWRvcy9oaXN0b3JpYWwtbXAiLCJwcm9kdWN0b3MvYnVzY2FyIiwiZmxvcmVzL2ZyYXNlcyIsInBlZGlkb3MvbWlzLXBlZGlkb3MiLCJkYXNoYm9hcmQiLCJhZG1pbi9uZWdvY2lvIiwiY2hhdCIsInJpZmFzL21lcyIsImFkbWluL3JlY29uY2lsaWFjaW9uLWltYWdlbmVzIiwibG9naW4iLCJmbG9yZXMvcmFtb3MiLCJ0aWVuZGEvdmVudGEiLCJyaWZhcy9idXNjYXIiLCJwZXJzb25hbGl6YWNpb24iLCJhZG1pbi9kaWFnbm9zdGljby1pbWFnZW5lcyIsInFyIiwiZmxvcmVzL2NvbmZpZ3VyYXIiLCJ1c3Vhcmlvcy9idXNjYXIiLCJhZG1pbi9jaGF0IiwiZmxvcmVzL2VudHJlZ2FzIiwiYWRtaW4vaGFzaHRhZ3MiLCJjbGllbnRlcy9idXNjYXIiLCJmbG9yZXMvY2F0YWxvZ29zIiwiYWRtaW4vZmFjZWJvb2siLCJnYXN0b3MvYnVzY2FyIiwicHJvZHVjdG9zL2FncmVnYXIiLCJwYWxhYnJhcy1jbGF2ZSIsImFkbWluL2NpbnRhIiwidGllbmRhL2J1c2NhciIsInJpZmFzL2FncmVnYXIiLCJnZXN0aW9uLW1lbnUvcm9sZXMiLCJ0aWVuZGEvdmVudGEtZGlyZWN0YSIsImx1Z2FyZXMtZW50cmVnYSIsImNhcmdhLWltYWdlbmVzIiwiZ2VzdGlvbi1tZW51IiwicHJvbW9jaW9uZXMiLCJyZXBvcnRlcyJdLCJqdGkiOiJkOGMzNmUzMS02YmFjLTRhNTAtOThhZC03ZDk2MzMzNzExYzMiLCJzdWIiOiJhZG1pbiIsImlhdCI6MTc4ODM4MzUxOCwiZXhwIjoxNzg4Mzg0NDE4fQ.XvjpAhp_Aep5YLE1WxAM6mFkAuCiGDc4uSQv4Q28L6k' \
-H 'Connection: keep-alive' \
-H 'Content-Type: application/json' \
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
--data-raw '{"usuarioId":43,"clienteId":23,"detalles":[{"productoId":0,"varianteId":602,"cantidad":1,"precioVenta":300,"subTotal":300}],"pagosYMesesId":1}'
504 Gateway Time-out



hasta aqui no detenemos porque no puedo generar un pedido

> **✅ Corregido — causa raíz encontrada en el primer curl (el de `buscarPorIdCliente/28`).**
> Ese endpoint estaba cacheado en Redis devolviendo `Optional<Cliente>` -- el serializador de
> Redis guarda el Optional con metadatos de tipo que Jackson no sabe reconstruir al leerlo de
> vuelta, así que cualquier cache hit tronaba con "Cannot construct instance of
> java.util.Optional" en vez de devolver el cliente. Justo el escenario que describes: un
> cliente recién registrado (solo con el id, sin nombre/apellido/teléfono) intentando generar su
> primer pedido -- la pantalla necesita este endpoint para saber si ya tiene datos completos, y
> al fallar, probablemente por eso a veces te salía el modal de "completa tus datos" y a veces el
> error en su lugar (dependía de si esa consulta ya estaba en caché rota o no). Se cambió a
> devolver `Cliente` normal (no `Optional`) -- no afecta al front, que ya esperaba un objeto
> plano. Commit `4b6b87a` (backend), ya en `qa`.
>
> **Si al volver a probar el mismo cliente (id 28 o el que sea) sigue fallando**, puede que
> quede una entrada de caché ya "envenenada" de antes del fix -- pídele a quien tenga acceso al
> panel de admin que limpie la caché de Redis una vez (opción ya existente en `AdminController`)
> y vuelve a intentar.
>
> **El segundo curl (`ventas/save`, 504 Gateway Timeout) es otra cosa distinta** -- ese request
> hace una llamada real a Mercado Pago (`requiereTerminal`/pago con terminal Point), no es el
> checkout normal de cliente. No lo pude diagnosticar desde aquí (necesito logs del servidor en
> el momento exacto del timeout, no tengo acceso a eso). Si al generar un pedido normal como
> cliente (no ese curl directo con `pagosYMesesId`) sigue tronando después del fix de arriba,
> dime y lo investigamos aparte -- si era solo consecuencia del cliente no encontrado, ya debería
> quedar resuelto con este fix.

> 💬 **Tu comentario:** "pero es que seleccioné pago en efectivo, no sé por qué marca que por
> mercado, revisa el front como lo hace aquí tienda/venta-directa" -- eso explicaba el 504 de
> arriba: al elegir Efectivo, igual disparaba la llamada a Mercado Pago.
>
> **✅ Corregido -- y no era el front.** El front (`venta-directa.component.ts`) hace bien su
> parte: solo manda el `pagosYMesesId` de lo que el admin/cliente clickeó, tal cual viene
> configurado desde el back (`getOpcionesEstructuradas()`). La decisión de "esto necesita
> terminal de Mercado Pago o no" la toma el BACK, y ahí estaba el bug real:
> `VentaServiceImpl.saveVentaDetalle()` decidía `requiereTerminal` comparando el id de
> `tarifa_terminal` contra un número fijo (`!= 3`) -- asumiendo que la fila "sin terminal" de esa
> tabla siempre cae en el id 3. Esa tabla no tiene una migración que la siembre igual en cada
> ambiente, así que el id depende del orden en que se cargó en cada base -- en QA la fila
> correspondiente a "sin terminal" aparentemente no quedó en el id 3, así que Efectivo terminaba
> tratándose como si necesitara terminal igual.
>
> Se agregó un chequeo directo por el nombre de la forma de pago (`tipoPago.formaPago ==
> "Efectivo"`, sin importar mayúsculas/espacios) que gana sobre el id de tarifa_terminal -- ya
> no depende de qué id le haya tocado a esa fila en cada base de datos. Commit `bd64ee8`
> (backend), ya en `qa`.

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

Pero aqui debe de enviar correo si es que se llega a cancelar el pedido ya sea por el usuario o por el tiempo que no recogieron ahi tiene el tiempo

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
Ya lo veo que este correo es para el adminn, entonces para los clientes es solo si da corazoncito no?
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
Consulta que pasa si ya no metyo mas stock a ese variante cada dia enviaria el correo?

> 💬 **Tu pregunta:** "¿qué pasa si ya no metió más stock a esa variante, cada día enviaría el
> correo?"
>
> **Sí, todos los días** — es a propósito, no es un bug. `StockBajoService` no guarda "ya avisé de
> esta" en ningún lado: cada vez que corre el barrido (`StockBajoScheduler`) vuelve a consultar
> TODAS las variantes por debajo del umbral y manda el correo de nuevo con la lista completa,
> mientras la variante siga baja. El propio comentario en el código lo explica: sirve de
> **recordatorio mientras la variante siga baja**, no solo la primera vez que cruzó el umbral — la
> idea es que un admin no lo pueda "perder" en el inbox y olvidarse de reponer. Deja de llegar
> únicamente cuando subís el stock por encima del umbral (o si eliminás/deshabilitás la variante).
> Si preferís que solo avise la primera vez (y no todos los días hasta reponer), avisame y lo
> cambio — hoy es diseño intencional, no falta nada roto.

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
Hay que implementar lo que te comente que si cancela despues de pagar que se le va a cobrar pero al cliente


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

## 8. Fix crítico — OutOfMemoryError al cargar cualquier usuario

> 💬 **Tu comentario:** pegaste un log real del back con
> `Caused by: java.lang.OutOfMemoryError: Java heap space`, sobre un SQL enorme con 5 tablas
> de permisos/roles/submenus unidas por JOIN al cargar un usuario por id.
>
> **✅ Corregido — bug grave, no ligado a ninguna feature de hoy.** `Roles` tiene 4 colecciones
> `@ManyToMany` EAGER (permisos, submenus, submenusEscritura, acciones) y `Usuario` tiene una
> quinta (permisosExtra). Sin la configuración correcta, Hibernate las trae **todas juntas en un
> solo JOIN** al cargar el usuario — el producto cartesiano de 5 colecciones (ej. 20 acciones ×
> 15 permisos × 10 submenus × 8 submenusEscritura × 5 permisosExtra) se **multiplica** en vez de
> sumarse, y puede llegar a millones de filas para un solo usuario.
>
> Esto no pasaba solo al hacer login: `JwtAuthenticationFilter` llama a `loadUserByUsername()`
> (el mismo query) **en cada petición autenticada** — podía tronar con cualquier usuario
> logueado, en cualquier momento, no solo en un flujo específico.
>
> **Fix:** se agregó `@Fetch(FetchMode.SELECT)` a las 5 colecciones — siguen siendo EAGER (nada
> cambia para el código que ya las usa), pero cada una se trae en su propio SELECT separado en
> vez de unirse por JOIN, sin producto cartesiano. Verificado con la suite completa de tests del
> proyecto (todos pasan). Commit `bd4e870` (backend), ya en `qa`.
>
> **No hay una ruta de clics específica para probar esto** — es transversal a toda la app. Si
> notas lentitud o errores raros al navegar como cualquier usuario (no solo un flujo puntual),
> repórtalo — antes de este fix cualquier petición podía ser la que hiciera tronar el servidor
> por memoria.

---

## 9. Fix — imagen no aparecía en "Modelos buscar" al agregar un producto nuevo

> 💬 **Tu comentario:** "cuando agrego un modelo y cargo una imagen... se guarda correctamente,
> entonces lo busco en Modelos buscar... la imagen como tal no aparece aunque se haiga guardado,
> pero si en el modelo que agregué me voy a detalle-productos/detalle-producto/401 aquí sí
> muestra el producto." Después me pasaste el repo `micro_imagenes` para revisarlo.
>
> **✅ Corregido — causa raíz real, y era determinística (pasaba siempre, no a veces).** Al
> agregar un producto CON imagen en un solo paso, el back le genera un id local aleatorio a la
> imagen antes de subirla al microservicio de imágenes — pero el micro, al recibir el archivo,
> le asigna SU PROPIO id, distinto (son dos servicios generando ids independientes en momentos
> distintos). El código nunca guardaba la relación producto-imagen en la tabla local
> (`producto_imagen_copy`) con el id correcto — de hecho, no la guardaba en absoluto, solo
> mandaba un mensaje a RabbitMQ que **el propio microservicio de imágenes consume para su propia
> base**, no la nuestra.
>
> El listado/búsqueda arma la miniatura leyendo el id de esa tabla local — que por lo anterior
> quedaba vacía SIEMPRE para un producto creado así. El detalle sí funcionaba porque le pregunta
> directo al microservicio por el id del producto, sin pasar por esa tabla local.
>
> Se corrigió usando el id REAL que devuelve el micro (en vez del generado localmente antes de
> subir) al guardar la relación local — mismo patrón que ya usa correctamente otra pantalla
> (carga rápida de imágenes). De paso se corrigió el mismo problema en las imágenes de variantes
> cuando se sube imagen a un producto ya existente. Commit `0d55768` (backend), ya en `qa`.
>
> **Además**, mientras investigaba hasta el fondo (revisé también `micro_imagenes`): encontré que
> ese servicio tragaba en silencio cualquier error al generar la miniatura del listado (devolvía
> 204 sin loguear el detalle real) — ya se corrigió ahí también para que quede loggeado el error
> completo si algo similar vuelve a pasar (commit `6dc7a43` en `micro_imagenes`, rama `dev` —
> ese repo no lo mergeé a `qa`/`master` todavía, avísame si quieres que lo suba).
>
> **Para volver a probar:** agrega un producto nuevo con imagen desde "Catálogo → Agregar
> modelo", y revisa que la imagen sí aparezca en "Modelos buscar" (no solo en el detalle).

---

**Si algo falla:** anota el paso exacto y lo que viste vs. lo que esperabas — con eso puedo ir
directo al archivo/línea en cuestión sin tener que re-investigar todo el flujo de nuevo.




quiero hacer pruebas, pero desde mi terminal, es decir desde mi aplicacion le paso el dinero a al terminal y desde la terminal puedo hacer el cobro solo que en la terminal es solo por o metiendo la tarjeta ahi como le hago?
Hora:16:10
Mensaje del asistente:
Para hacer pruebas cobrando con tu terminal Point (que desde tu sistema “le pases el monto” y que en la terminal el cliente pague insertando/acercando/deslizando la tarjeta), lo que tenés que implementar es el flujo de “intención de pago”: tu app crea la intención por API y la terminal la carga automáticamente para que se cobre ahí.

Cómo probar (flujo recomendado)
1) Usá credenciales de producción para las pruebas de Point.

En Point, las pruebas se validan haciendo cargos reales por montos mínimos y luego devolviéndolos (reembolso) desde la terminal.

2) Asigná la intención al dispositivo (necesitás el deviceId del Point ya configurado).

3) Desde tu backend, llamá a la API Crear una intención de pago (POST) indicando al menos:

amount (monto)
external_reference (para identificar esta prueba en tu sistema)
Guía del endpoint: Crear intención de pago (API de Point)

4) (Opcional pero recomendado) Validá que la intención quedó bien creada consultándola con Buscar intención de pago (GET).

Buscar intención de pago

5) En la terminal Point, iniciás el cobro y el comprador paga con tarjeta (chip/NFC/banda).

La prueba completa está detallada acá: Probar el procesamiento de pagos

6) Verificá que te lleguen las notificaciones (Webhooks) y usá tu external_reference para conciliar el evento con tu sistema. (En la notificación vas a poder identificar el pago, por ejemplo con el payment.id).

7) Para confirmar el resultado del pago, consultá el pago por API con el id recibido:

Obtener pago por id

8) Para cerrar el circuito de prueba, hacé el reembolso desde la terminal (así se reintegra el dinero a la tarjeta), tal como indica la guía.

Lecturas útiles
Vista general de la integración de Point (cómo funciona el flujo): Mercado Pago Point
Etapas de pruebas: Prueba de integración
Para ayudarte con precisión
¿Qué terminal estás usando (Point Smart 1 / Point Smart 2) y ya la tenés configurada/vinculada a tu cuenta y con el deviceId a mano?



Mi mensaje:
tengo la 2
Hora:16:39
Mensaje del asistente:
Perfecto: con Point Smart 2 el flujo para “pasarle el monto desde tu app y cobrar en la terminal con tarjeta” es por intención de pago (payment intent). La terminal no recibe “saldo” como tal: recibe una intención con el monto y ahí se ejecuta el cobro (chip/NFC/banda).

Siguiente paso (Point Smart 2): dejá lista la terminal y obtené el deviceId
1) Configurá/vinculá la terminal a tu cuenta siguiendo la guía oficial:

Configurar la terminal Point

2) Con esa terminal ya configurada, identificá el **deviceId** del Point (lo vas a necesitar para crear la intención desde tu backend).

Crear el cobro “desde tu aplicación” (intención) y cobrar con tarjeta en la terminal
3) Desde tu backend, creá la intención apuntando a ese deviceId (POST):

Crear intención de pago (API de Point)

4) En la Point Smart 2, iniciás el cobro y el cliente paga con la tarjeta (insertar/acercar/deslizar).

El paso a paso completo de pruebas está acá:

Probar el procesamiento de pagos

5) Validá por API el resultado del pago con el id que recibas por notificaciones:

Obtener pago por id

Recomendación para agilizar tu integración (opcional)
Si estás desarrollando esto en tu IDE, podés usar el Mercado Pago MCP Server para consultar docs y armar el flujo más rápido:

Mercado Pago MCP Server

Para indicarte el paso exacto que te falta: ¿ya tenés la Point Smart 2 vinculada (puedes entrar al menú y operar) y ya viste el **deviceId**? Si me confirmas eso, te digo cuál sería el próximo llamado que deberías hacer desde tu backend y qué deberías ver en la terminal.



Mi mensaje:
seria este N950NCBC02460220?
Hora:16:40
Mensaje del asistente:
Ese valor (N950NCBC02460220) parece el número de serie que viene en la etiqueta del dispositivo, pero **no es el deviceId** que te pide la API para crear la intención de pago.

Para obtener el deviceId correcto tenés que listar tus dispositivos por API y usar el id que devuelve Mercado Pago:

1) Listá los dispositivos asociados a tu cuenta:

Endpoint: Obtener dispositivos
2) En la respuesta, buscá el dispositivo cuyo id termine con caracteres que coincidan con el serial de tu terminal (la doc indica que podés identificarlo así). Ese id completo (suele verse parecido a PAX_...__SMARTPOS...) es el que tenés que usar como deviceId.

3) Con ese deviceId, ya podés crear la intención:

Endpoint: Crear intención de pago (API de Point)
Si en algún momento te aparece el error de “serial con formato inválido”, fijate esta guía (clave: no recortar ni modificar el serial):

Cómo resolver “Device serial with invalid format”

Si querés, pegá aquí (tapando tu token) **solo el fragmento del id** que te devuelve el listado de dispositivos, y te digo cuál es el deviceId que corresponde a tu Point Smart 2.

---

## 10. Mercado Pago Point — cómo funcionan las devoluciones/reembolsos (para cerrar el circuito de pruebas)

> 💬 **Tu pregunta:** "ayudame a bajar cambios de back par que revises el roadmap y veas lo que
> agregue de mercado pago para la simulacion necesito que busques eso y lo complementes para el
> caso de las devoluciones si cobran o que pasa o como hacerle?"

Complementando el punto 8 del hilo pegado arriba ("cerrá el circuito de prueba haciendo el
reembolso") — esto es lo que dice la documentación oficial de Mercado Pago sobre reembolsos de
pagos con Point, verificado por búsqueda (no de memoria):

**¿Cobran algo por hacer la devolución? No.**
Al reembolsar un pago, Mercado Pago devuelve el dinero al cliente y **te acredita de vuelta la
comisión de venta que ya te habían cobrado** — o sea, ni vos ni el cliente pagan nada extra por la
devolución en sí. El cliente no queda con ningún cargo por el pago que te hizo. Esto aplica igual a
tus pruebas: cobrás el monto mínimo, lo devolvés, y no perdés la comisión ni el cliente (de prueba)
paga de más.

**¿Cómo se hace? Dos caminos:**

1. **Desde la terminal / app de Mercado Pago (manual, el que menciona el hilo pegado arriba):**
   Actividad → seleccionás el cobro → "Devolver dinero" → indicás el monto (podés devolver menos
   del total si es parcial) → confirmar. Es el camino más simple para cerrar el circuito de prueba
   sin escribir código.

2. **Por API (si en algún momento se quiere automatizar la devolución desde el back, no solo para
   pruebas manuales):** hay dos endpoints distintos según qué se está reembolsando:
    - `POST /v1/payments/{id}/refunds` — reembolso clásico sobre un pago ya aprobado, usando el
      `payment.id` (el mismo que ya se consulta en `MercadoPagoService.consultarEstado`/webhook vía
      `getPaymentIntentStatus`, o el que llega en la notificación). Sin `amount` en el body = reembolso
      total; con `amount` = reembolso parcial.
    - `POST /v1/point/orders/{order_id}/refund` (Point Orders API) — específico para pagos cobrados
      con Point vía el modelo de "orders". Mismo criterio: sin `amount` = total, con `amount` =
      parcial (soporta varios reembolsos parciales sobre la misma orden mientras la suma no supere
      el total). Solo funciona sobre órdenes con `status=processed`.
    - **Plazo:** hasta 90 días después de aprobado el pago (algunos medios de pago específicos de
      otros países tienen ventanas más cortas, no aplica a México/tarjeta física).
    - Respuesta exitosa: `201`, con el pago pasando a `refunded` (total) o `partially_refunded`
      (parcial).

**Estado actual en el código — esto NO está implementado, es solo para pruebas manuales por ahora:**
`MercadoPagoService.java` únicamente tiene `cancelar(intentId)`
(`PointClient().cancelPaymentIntent(...)`), que **cancela una intención de cobro que todavía no se
cobró** (antes de que el cliente pase la tarjeta) — no es un reembolso, es evitar que se llegue a
cobrar. No hay ningún método que llame a `/refunds` sobre un pago ya `FINISHED`.

Del lado de negocio, `PedidoServiceImpl` sí tiene el concepto de devolución (`esDevolucion` cuando
el pedido está "Entregado" o "PAGADO"), pero **solo maneja inventario** (regresa el stock) — no
dispara ningún reembolso real a Mercado Pago. Si un pedido se cobró con la terminal Point y se
marca como devuelto en el sistema, hoy el dinero no se devuelve solo: hay que hacerlo aparte, a
mano, desde la terminal/app como se explica arriba.

**Para tus pruebas de ahora mismo:** no hace falta tocar código — devolvé el cargo de prueba desde
la terminal o la app (opción 1) y listo, no se pierde comisión ni queda nada pendiente.

**Pregunta abierta (para cuando termines de probar el cobro):** ¿querés que además implemente la
devolución automática por API (opción 2) enganchada a cuando un admin marca un pedido pagado con
Point como devuelto, o por ahora lo dejamos manual desde la terminal/app como está? Es un cambio de
código nuevo, no algo que ya exista a medias.

Fuentes consultadas: [Reembolsar una order — API Orders (Point)](https://www.mercadopago.com.mx/developers/es/reference/in-person-payments/point/orders/refund-order/post),
[Crear reembolso — Reembolsos](https://www.mercadopago.com.mx/developers/es/reference/chargebacks/_payments_id_refunds/post),
[¿Cómo voy a recibir el reembolso de una compra de Point?](https://www.mercadopago.com.mx/ayuda/28577),
[Tengo que devolver un pago con Point](https://www.mercadopago.com.mx/ayuda/tengo-que-devolver-un-pago-con-point_2608),
[Generá reembolsos parciales vía API con Mercado Pago Point](https://www.mercadopago.com.ar/developers/es/news/2025/12/15/Generate-partial-refunds-via-API-with-Mercado-Pago-Point).

---

## 11. Correo de promociones — checkbox por cliente + envío en tandas de 10

> 💬 **Tu comentario:** "sobre los correo también debe a ver un chec en donde le podamos decir o
> enviar las promociones que tenemos si en los clientes tiene el chec seleccionado entonces les
> llegaría [...] en las promociones una opción para programar enviar de 10 en 10 correo a todos
> los que tengan el check seleccionado y en ese correo tiene que mencionarle que si no quiere
> recibir los correos explicarle en dónde ir para quitarlos [...] y también explicarle dónde
> encontraría las promociones."

### ⚠️ Paso manual OBLIGATORIO antes de probar — correr la migración

`ddl-auto` está en `none` en todos los ambientes (igual que todas las migraciones anteriores):
la columna nueva **no se crea sola**. Antes de desplegar/probar esto en QA hay que correr a mano
contra la BD de QA (`inventario_key_qa`, la misma de `dev`/`qa`):

```sql
ALTER TABLE clientes ADD COLUMN recibir_promociones TINYINT(1) NOT NULL DEFAULT 1;
```

(archivo `migration_recibir_promociones.sql`, mismo patrón que las demás). **Si no se corre esto
antes, TODO lo que toque la tabla `clientes` empieza a fallar** (Hibernate intenta leer una
columna que no existe) — no es opcional, es bloqueante para toda la app, no solo para esta feature.

### ✅ Lo que se construyó

- **Checkbox independiente** `recibirPromociones` en `Cliente` (además del ya existente
  `recibirCorreos` de seguimiento de pedido/stock) — se activa `true` por default para clientes
  nuevos y existentes (mismo criterio que `recibirCorreos`).
- **"Mis datos"** (cliente) y **"Ver cliente"** (admin, `clientes/buscar` → detalle) ahora
  muestran dos toggles separados en la sección Preferencias: uno para correos de
  seguimiento/stock, otro para promociones — se pueden prender/apagar independientemente uno del
  otro, cada uno guarda al instante (no hace falta darle a "Guardar cambios" del formulario).
- **Botón "✉️ Enviar correo"** en cada card de **Gestión de promociones** (admin): al darle clic,
  primero pregunta a cuántos clientes les llegaría (confirmación con el número real) y, si
  confirmás, dispara el envío.
- **Envío en tandas de 10** con 3 segundos de pausa entre tandas (para no disparar todos los
  correos de golpe y arriesgar que el proveedor de correo marque la cuenta como spam por ráfaga)
  — corre en su propio hilo en el backend, así que la pantalla no se queda "colgada" esperando; el
  admin ve "Envío iniciado a N cliente(s)" de inmediato y el envío sigue en segundo plano.
- **El correo** incluye: la descripción de la promoción, un botón "Ver promoción" que lleva a
  `/promociones` en la app, y al final una nota explicando que si ya no quiere recibir correos de
  promociones puede desactivarlos desde "Mi perfil" (con link directo cuando el ambiente tiene
  `app.public-base-url` configurado) — casilla "Recibir promociones".
- Solo le llega a clientes con **correo verificado** además del checkbox activado (no tiene caso
  mandarle promociones a un correo que nunca se confirmó que existe).

### Ruta de clics para probar

**Cliente (activar/desactivar):**
1. Inicia sesión como cliente → menú de usuario → **"Mis datos"**.
2. Baja hasta **Preferencias** → verás dos toggles: el de correos de seguimiento (ya existía) y el
   nuevo **"Recibir promociones"**, activado por default.
3. Apágalo → confirma que no se movió el otro toggle (son independientes) → recárgalo → debe
   seguir apagado.

**Admin (activar/desactivar en cualquier cliente):**
1. Menú lateral → **"👥 Clientes"** → buscar → abre el detalle de un cliente.
2. Misma sección Preferencias, mismos dos toggles, editables desde aquí también.

**Admin (enviar el correo):**
1. Menú lateral → acordeón de promociones → **Gestión de promociones**.
2. En una promoción **vigente** (activa y no vencida), botón **"✉️ Enviar correo"** debe estar
   habilitado; en una inactiva o vencida debe verse deshabilitado (con tooltip explicando por qué).
3. Debe salir un modal confirmando "Se enviará a N cliente(s)..." — si dice 0, es porque ningún
   cliente de prueba tiene el checkbox activado todavía (activa uno primero, paso de arriba).
4. Confirma → debe salir "Envío iniciado a N cliente(s)" casi de inmediato (no espera a que se
   termine de mandar) → revisa la bandeja del cliente de prueba, debe llegar el correo con la
   promoción, la imagen (si el combo tiene alguna variante con imagen cargada), el botón "Ver
   promoción" y la nota de cómo desactivarlo.

### 🔧 Correcciones de una segunda pasada (mismo día, tras tu feedback probando)

> 💬 **Tu feedback:** "para enviar el correo en promociones hace falta validar que la promoción
> esté válida [...] dice que sí envía el correo pero 'ver promoción' cuando doy clic no lleva a
> ningún lado por el token, dice esto {"mensaje":"Token inválido o expirado"...} lo mismo si doy
> 'mi perfil' [...] está muy sencillo, no se puede poner la imagen de la promoción para que sea
> llamativa?"

1. **✅ Validación de vigencia antes de enviar:** ahora el back rechaza el envío si la promoción
   está inactiva o vencida (antes solo validaba que existiera). El botón "Enviar correo" también
   se deshabilita en el front para promociones no vigentes, para que ni se pueda intentar.
2. **✅ Bug real de los links "Ver promoción"/"Mi perfil" — encontrado y corregido:** el correo
   armaba esos links con `app.public-base-url`, que es la URL del **backend**
   (`qa.backend.novedades-jade.com.mx/mis-productos`), no la del frontend — por eso el navegador
   mostraba el JSON crudo del backend ("Token inválido o expirado") en vez de abrir la app. Ahora
   usa `api.cors_angular` (ya configurada por ambiente, es la URL real del front —
   `qa.shop.novedades-jade.com.mx` en QA) para estos dos links. `app.public-base-url` se queda
   igual que antes, solo para la imagen del logo en el encabezado (esa sí debe ser del backend).
3. **✅ Imagen de la promoción en el correo:** se agrega la imagen de la primera variante del combo
   (si tiene una cargada) arriba de la descripción, para que no se vea tan plano. Si esa variante
   no tiene imagen todavía, el correo se ve igual que antes (no se rompe).

### ✅ Validación de existencias al CREAR/EDITAR una promoción (nuevo, mismo feedback)

> 💬 **Tu comentario:** "para generar las promociones hay que revisar que cuando se haga la
> promoción exista disponibilidad, porque se supone que hice la promoción y dice que no hay
> existencias y se supone que si hago una promoción es porque hay existencias."

Antes, crear/editar una promoción no validaba nada de stock — se podía guardar un combo cuya
cantidad requerida por variante fuera mayor al stock real, y el problema no se notaba hasta que
aparecía "❌ Sin disponibilidad" en el catálogo público. Ahora, al guardar, si **cualquier**
variante del combo no alcanza para armar ni 1 combo completo (stock actual < cantidad
configurada), se rechaza con un mensaje señalando cuál variante y cuánto stock hace falta.

**Ruta de clics para probar:** Gestión de promociones → Nueva promoción → agrega una variante y
ponle una **cantidad mayor a su stock actual** (ej. cantidad 5 en una variante con solo 2 en
stock) → Guardar → debe rechazar con el mensaje de existencias insuficientes, mencionando la
variante y los números exactos. Con cantidad dentro del stock disponible, debe guardar normal.

### ⬜ Lo que quedó pendiente de tu comentario original de promociones (no se hizo)

- El correo de "ticket"/confirmación al cliente y aviso al admin **cuando se genera un pedido** ya
  se resolvió — ver sección 12 más abajo.
- La opción de **programar** el envío (mandarlo más tarde, no solo "enviar ahora") no se
  construyó — hoy el botón manda de inmediato (en tandas de 10, pero arrancando al momento del
  clic). Si querías programarlo para una hora/fecha específica, es trabajo aparte.

---

## 12. Correo al generar un pedido — confirmación al cliente + aviso al admin

> 💬 **Tu comentario:** "cuando se hace un pedido de parte del cliente o del admin tiene que
> enviar el correo del ticket y para el cliente si el cliente hace un pedido me tiene que llegar
> un correo avisando que hizo un pedido."

### Lo que encontré (antes de tocar nada)

`PedidoServiceImpl.savePedido()` — el método real que crea el pedido (`POST
/v1/pedidos/savePedido`, lo puede llamar tanto un cliente logueado como un admin, mismo
endpoint) — **no mandaba ningún correo**. Lo único que ya existía:
- `notificarPedido`/"reenviar comprobante": el cliente lo dispara a mano desde la app después de
  generar el pedido (no es automático).
- `notificarSeguimientoPedido`: correo automático, pero solo dispara cuando el pedido pasa a
  **Entregado** o **cancelado** — nada al momento de crearlo.

O sea: el hueco que describiste era real, no había ningún correo automático al generar el pedido,
ni para el cliente ni para el admin.

### ✅ Lo que se construyó

- **Al cliente:** correo de confirmación **siempre** que se genera un pedido ("Recibimos tu
  pedido #X"), sin importar quién lo generó (cliente o admin) — es un comprobante, igual que el
  ticket de compra ya existente, así que **no depende del checkbox `recibirCorreos`** (mismo
  criterio que ese correo).
- **Al admin:** correo de aviso ("Nuevo pedido #X de [nombre]") a **todos los ADMIN activos con
  correo** (mismo patrón que la alerta de stock bajo) — pero **solo cuando el pedido lo generó el
  propio cliente**. Si lo generó un admin (ej. toma un pedido por teléfono), no se le avisa a sí
  mismo — ya lo sabe.
- Ninguno de los dos correos puede tumbar la creación del pedido si el envío falla (try/catch +
  log, mismo criterio que el resto de correos no críticos).

### ⚠️ Supuestos que hice — avísame si alguno no es lo que querías

1. Asumí que el correo al cliente es **siempre**, no depende de la preferencia de correos —
   porque lo describiste junto con "el correo del ticket", y el ticket ya existente tampoco
   depende de esa preferencia. Si en realidad querías que si respete `recibirCorreos`, lo cambio.
2. Asumí que el aviso al admin es solo cuando el pedido lo genera el **cliente**, porque dijiste
   "si el cliente hace un pedido me tiene que llegar un correo" — específicamente mencionaste al
   cliente como quien dispara el aviso. Si también querés que te avise cuando OTRO admin genera un
   pedido (no solo vos), dímelo.
3. Le avisa a **todos** los usuarios con rol ADMIN activo, no solo a uno — igual que la alerta de
   stock bajo. Si quieres que sea configurable (solo ciertos admins), es cambio aparte.

### Ruta de clics para probar

**Confirmación al cliente:**
1. Login como cliente → agrega productos al carrito → genera un pedido.
2. Revisa la bandeja del correo del cliente: debe llegar **"Recibimos tu pedido #X"** con el
   total, casi de inmediato.
3. Repite con el checkbox de "Recibir correos" (el general, no el de promociones) **desactivado**
   → el correo de confirmación debe llegar igual (no depende de ese checkbox).

**Aviso al admin:**
4. Con el mismo pedido del cliente (paso 1-2), revisa la bandeja de un usuario ADMIN activo →
   debe llegar **"Nuevo pedido #X de [nombre del cliente]"**.
5. Ahora genera un pedido **desde una cuenta ADMIN** (si existe ese flujo en la app) → el admin
   que lo generó NO debe recibir el aviso a sí mismo (verifica en su propia bandeja).
6. Si hay más de un ADMIN activo con correo, confirma que **todos** reciben el aviso, no solo uno.