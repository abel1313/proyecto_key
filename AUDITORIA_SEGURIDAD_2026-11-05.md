# ⚠ ACTUALIZACIÓN tras la segunda revisión (revisión con modelo potente, 2026-11-05)
La verificación independiente (informe completo: `scratchpad/revision/A_auditoria_verificada.md`) **empeoró y corrigió** esta auditoría. Lo que manda:

1. **CRÍTICO en producción — CRUD heredado de pedidos y clientes abierto.** Cualquier persona que se registre y verifique su correo puede: leer **todas las clientas** (`GET /v1/clientes/getAll`, `getOne`: nombre, correo, teléfono, direcciones **y código de verificación**), leer **todos los pedidos** (`GET /v1/pedidos/getAll`, `getOne`) y **marcar cualquier pedido como pagado y entregado** con `POST /v1/pedidos/save` (en la prueba sobre el código de `main` el pedido quedó `Entregado`, `totalPagado=1500` y la dirección borrada). Sobre el código de `dev`/`qa` la misma prueba da 403 en las cuatro peticiones. Lo cierran las seis reglas de `SecurityConfig` del commit `f3a1f35`. *(Yo había visto solo `POST /v1/pedidos/save` y lo puse como «Alto»: era más grave.)*
2. **CRÍTICO en producción — el chat en vivo no tiene autenticación.** Sin cuenta se leen todas las conversaciones y se puede contestar como la tienda («deposita a esta cuenta»). También lo cierra `f3a1f35`.
3. **ALTO, y está en `dev`/`qa` — hueco nuevo en el arreglo del chat.** Suscribirse a `/topic/chat.usuario.*` o `/topic/chat.inicio.*` pasa el control (el broker de Spring trata `*` como comodín): se leen las respuestas de la tienda a todas las clientas y se roban ids de conversación, con los que `GET /v1/chat/historial/{sesionId}` entrega el historial. **Hay que arreglarlo antes de llevar el chat a producción** (parche en el informe A).
4. **ALTO en producción — `savePedido` no revisa quién pide:** se pueden crear pedidos a nombre de otra clienta con estado `Entregado` o tipo `FIADO`. También lo arregla `f3a1f35`.
5. **`835c720` no basta para producción:** el header de refresh obligatorio y la limpieza de CORS solo se activan con el archivo de configuración de QA. Producción sigue con el origen `venta-bolsas-online.netlify.app` con credenciales y cookie `SameSite=None`: si alguien ajeno controla ese sitio, puede robar tokens de quien lo visite, incluida la administradora (**no se pudo verificar quién lo controla hoy**). Hay que ajustar a mano el archivo de producción: `exigir-header-refresh: true` y quitar ese origen.
6. **Correcciones a esta auditoría:**
   - **Webhook viejo `/v1/mp/webhook`: es FALSO que aplique el pago dos veces.** Nunca confirma ningún pedido (manda `pagosYMesesId=null` y `updatePedido` falla) y además deja en `ERROR` intentos que sí se cobraron. *(Deducido leyendo el código; falta una prueba unitaria.)* El hallazgo #1 de la tabla de abajo queda **corregido por este punto**.
   - **`/v1/dipomex/**` sí tiene controlador** (`hexagonal/codigopostal`) y es público a propósito: el hallazgo #3 queda **corregido**.
   - **`/v1/flores/**`: «limitar a GET» rompería la tienda** (el cálculo usa POST). La propuesta correcta es permitir solo esos 3 POST.
   - `buscarClientePorIdUsuario`: confirmado; el front solo lo llama con sesión y con el id propio, se puede cerrar sin romper nada.
   - Subida de imágenes: solo personal con permiso; el tipo real del archivo lo valida el micro de imágenes (**no revisado**). Acceso entre cuentas: las rutas propias revisan a la dueña salvo el CRUD heredado, el historial del chat por `sesionId` y los abonos (dependen solo de la pantalla del panel; no se verificó en la base que ningún rol de clienta la tenga). Logs: registran correos y contenido del chat, sin contraseñas ni tokens. Los 400 devuelven mensajes internos de Spring/Jackson.

**Orden propuesto para producción:** (1) reglas del CRUD heredado y la configuración que oculta el código de verificación; (2) `savePedido` y `verificar-correo`; (3) el chat **junto con** el arreglo de los comodines; (4) archivo de configuración de producción a mano; (5) el resto de `835c720`.

---

# Auditoría de seguridad del código — 2026-11-05 (noche)

Alcance: lectura del código de `dev` (= `qa`) y comparación con `main` (producción). **Solo lectura**: no se cambió ningún comportamiento ni se tocó producción. Lo que dice "verificado" lo leí en el código; lo que dice "por probar" es una sospecha que necesita una prueba antes de afirmarla. **No es una auditoría completa**: faltan revisar a fondo subida de imágenes, el chatbot y las pruebas de acceso entre cuentas (IDOR) endpoint por endpoint.

## Lo más importante: producción NO tiene las correcciones de seguridad que ya están en dev/qa
`git log origin/main..origin/qa` muestra que **no están en `main`** (producción):

| Commit | Qué corrige | Riesgo si falta en producción |
|---|---|---|
| `f3a1f35` | Cierra el CRUD heredado de pedidos y clientes, oculta el código de verificación, autentica el chat WebSocket, valida `savePedido` y textos | **Alto**: en `main` no existe el bloqueo `denyAll` de `POST /v1/pedidos/save`, `PUT /v1/pedidos/update/**` y `DELETE /v1/pedidos/delete`; las rutas legadas solo piden sesión iniciada, así que una clienta con sesión podría tocar pedidos que no son suyos (**verificado con prueba**: ver «Pruebas de autorización» abajo) |
| `835c720` | Header de refresh obligatorio, CORS y swagger de QA, `simular-pago` solo en pruebas, webhook de Facebook cerrado sin secreto, topes de tamaño | **Medio-alto**: sin el header de refresh, `/refresh` y `/logout` con cookie `SameSite=None` quedan expuestos a peticiones desde otro sitio (CSRF está desactivado en el back) |
| `1ecce67`, `ea321b7` (parcial) | Chatbot: al cliente solo precio normal, sin existencias ni códigos | Ya se llevó a producción en dos hotfix; confirmar que `1ecce67` completo está |

**Recomendación:** llevar `f3a1f35` y `835c720` a producción **antes que cualquier función nueva**, con el mismo método que el escáner (rama desde `main`, solo esos cambios, pruebas, y orden tuya para subir). Tienen que probarse juntos con la pantalla de pedidos y clientes del front, porque cambian rutas que el front podría usar.

## Hallazgos nuevos en el código de dev/qa (verificados al leer)
| # | Gravedad | Hallazgo | Propuesta |
|---|---|---|---|
| 1 | Media | `POST /v1/mp/webhook` (cobro viejo con terminal) es público y **no valida firma**. Lo bueno: vuelve a preguntarle a Mercado Pago el estado del `intentId`, así que un aviso falso no marca un pago. Lo malo: no es idempotente. Cada aviso con estado `FINISHED` vuelve a correr `updatePedido`. Mercado Pago reenvía avisos, así que puede aplicar el pago más de una vez (**por probar**: no verifiqué qué hace `updatePedido` al repetirse). Además el controlador escribe todo el cuerpo recibido en el log. | Si el intento ya está en `FINISHED`, salir sin hacer nada; validar `x-signature` como el módulo nuevo; no registrar el cuerpo completo. |
| 2 | Baja | `GET /v1/usuarios/buscarClientePorIdUsuario/{id}` es público y devuelve el id de cliente de cualquier usuario. Permite recorrer ids. | Exigir sesión y que sea el mismo usuario o administrador (revisar primero si el front lo llama antes de iniciar sesión). |
| 3 | Baja | Reglas públicas sin controlador: `GET /v1/admin/test-rabbit` y `/v1/dipomex/**` están como `permitAll` pero **no encontré ningún controlador** con esas rutas. Son reglas muertas que abren una puerta por si alguien agrega algo después. | Quitarlas. |
| 4 | Baja-media | `/v1/flores/**` está como `permitAll` para **todos los métodos** (cualquier ruta nueva bajo `/v1/flores/` nace pública). Hoy solo debe ser el motor de cálculo. | Limitar a las rutas de cálculo y a `GET`. |
| 5 | Info | `/v1/escaner/**` es público por diseño (el celular se enlaza con un código). El canal por cuenta y las fotos ya exigen sesión/administrador. | Confirmar que hay límite de intentos para adivinar códigos. |
| 6 | Info | El límite de intentos de login es por IP (5 cada 15 min). Detrás del proxy, la IP depende de `X-Forwarded-For`. | Por probar que no se pueda falsificar ese header para saltarse el límite. |
| 7 | Info | Los valores como `password: guest` y `api-key: asdasdsa` en `application-dev.yml` son marcadores de desarrollo (verificado: qa y docker exigen variables de entorno). Sin secretos reales encontrados en los archivos versionados que revisé. | Mantener. Ver el historial de git por secretos viejos (pendiente en `PENDIENTES`). |

## Revisado y bien
- No hay consultas armadas con texto concatenado a partir de datos de la persona (las `@Query` encontradas son literales de varias líneas).
- Cookie de refresh: `HttpOnly`, `Secure` y `SameSite` según ambiente.
- Actuator: solo `health` público; el resto exige administrador.
- Contraseñas con `PasswordEncoder`.
- El generador de datos de prueba solo corre en `inventario_key_qa`.

## Falta revisar (no lo hice todavía)
Subida de imágenes (tipo real y tamaño), prueba automática de acceso entre cuentas en cada endpoint con `{id}`, inyección de instrucciones al chatbot (30 casos), `@RequestBody` con entidades completas (asignación masiva), y el micro de imágenes.


## Pruebas de autorización (hechas esta noche, con la seguridad real)
`src/test/.../security/AutorizacionPorEndpointTest.java`: 27 rutas del panel × (sin sesión → 401/403; clienta común → 403), 9 rutas «solo con sesión» (sin sesión → 401/403) y la vista previa pública. **En `dev`/`qa`: 64 de 64 pasan.**

Corrí la **misma prueba contra el código de producción (`main`)**:
- **`POST /v1/pedidos/save` con una clienta común (ROLE_USUARIO) NO responde 403**: pasa la seguridad y llega al código que guarda (en la prueba respondió 500 solo porque no hay base de datos conectada). Es el CRUD heredado que `f3a1f35` cierra en `dev`/`qa`. **Confirmado: producción lo tiene abierto a cualquier cuenta con sesión.** `PUT /v1/pedidos/update/**` y `DELETE /v1/pedidos/delete` sí respondieron 403 en `main`.
- Las otras 3 fallas en `main` son rutas de módulos que aún no están en producción (`acepta-msi`, devolución de tarjeta, vista previa): no aplican.

## Concurrencia de stock — verificada con los servicios reales (segunda revisión)
Informe completo, parche (diff) y registros: `scratchpad/revision/C_concurrencia_verificada.md`, `C_parche_main.diff`, `C_parche_tests_cableado.diff`. Se probó llamando a `VentaServiceImpl`, `PedidoServiceImpl` y `AbonoServiceImpl` con dos hilos, en H2 **y en un MySQL 8 desechable** (REPEATABLE READ; ninguna base real). *Esto reemplaza mi versión anterior de esta sección.*

| # | Veredicto | Qué pasa |
|---|---|---|
| S1 | **Confirmado, y peor en MySQL** | Dos artículos distintos del mismo producto: en H2 el producto queda en 9 en vez de 8; **en MySQL da un deadlock y una venta falla con error 500**. En MySQL, el mismo artículo vendido dos veces a la vez también deja el producto en 9. |
| S2 | **Confirmado, con el mecanismo exacto** | `findByIdWithLock` sí bloquea y espera, pero si el producto ya estaba en la sesión de Hibernate 6, devuelve esa instancia con el valor de **antes** de esperar. |
| S3 | Confirmado **solo leyendo el código** | Rifas: lee sin bloqueo y guarda el valor completo; además del −1 puede borrar una venta simultánea. |
| S4 | Confirmado con matiz | Dos cancelaciones seguidas: la guarda funciona. Dos simultáneas: se aplican las dos sin error (el stock no se duplica solo porque la segunda escribe valores viejos). Lo que sí falla: **una cancelación simultánea con una venta borra la venta** (queda 10/5 en vez de 9/4). |

**Otros lugares no seguros:** los dos adaptadores de edición de pedido (`MovimientoStockJpaAdapter`, `CatalogoArticuloJpaAdapter`) tienen el problema de S2; `transferirAbono` el de S1; el vencimiento de rifas y las ediciones de stock del administrador leen sin bloqueo. Aparte, **sin probar**: `transferirAbono` no marca el pedido de origen como transferido, así que parece que el mismo saldo se puede transferir más de una vez.

**Corrección importante a mi propuesta anterior:** el `UPDATE` atómico **no** es el arreglo. El producto que sigue en memoria con el valor viejo se vuelve a guardar completo al final del método y deshace la resta atómica, y las validaciones seguirían leyendo el valor viejo.

**Parche verificado (no aplicado):** un componente `BloqueoStock.bloquear(entidad)` que hace `em.flush()` y luego `em.refresh(entidad, PESSIMISTIC_WRITE)`, usado antes de leer el stock del artículo y del producto en venta directa, pedido, las dos cancelaciones, quitar artículos, transferir abono, rifas y los dos adaptadores; más `IPedidoRepository.findByIdWithLock` para que las cancelaciones bloqueen el pedido. Orden de bloqueo único: **pedido → artículo → producto**. No cambia la aritmética. Con el parche: suite completa 935 pruebas, 0 fallos; las pruebas de servicio dan lo esperado en H2 y MySQL, sin deadlock. Riesgo que queda: dos ventas de varias líneas en orden inverso pueden chocar (MySQL revierte una, sin dañar datos); se evita bloqueando por id, no incluido en el parche mínimo. Plan de prueba en QA y consulta para detectar descuadres ya existentes: en el informe C.
