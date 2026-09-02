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
