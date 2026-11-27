# Pendientes para retomar — anotado 2026-11-27

Lista de lo que quedó abierto al cerrar los hotfix a producción del 2026-11-26. Se retoma **al final**, cuando el dueño lo pida. Al terminar algo, se marca ✅ con la fecha; no se borra.

---

## 1. Decidido «de momento que siga así» — retomar al final

### 1.1 Ventana «📍 Dirección y fecha de entrega — Pedido #N» (Mis pedidos)

Palabras del dueño (2026-11-27): *"bueno, que siga así de momento, pero hay que anotarlo para el final retomarlo para que lo tengas"*. Regla de negocio que aclaró: **nadie pide entrega a domicilio; todos recogen en el punto de encuentro que se les indica** (no saldría ni para la gasolina). Detalle completo y opciones A / B / C en `PLAN_PEDIDOS_VENTAS_ENTREGA.md` §12 y en la skill `reglas-pedidos` §2.4.

Qué falta decidir:
- [ ] Opción **A** (recomendada: el cliente ya no escribe la fecha; ve la que fija el admin; texto que explique la ventana), **B** (quitarle la ventana al cliente) o **C** (A + mínimo de compra para entrega).
- [ ] La fecha que escribe el cliente se guarda en `pedidos.fecha_recogida`, el mismo dato que fija el admin en **Entregas por zona** y que usa el **Corte de caja**.
- [ ] El plan (H12/P7) todavía habla de «a domicilio → dirección del cliente»: preguntar si ese caso se quita del «Cómo llegar» del detalle.
- [ ] La ventana no explica para qué sirve: texto corto dentro de la ventana y/o ayuda contextual «?».

---

## 2. Decisiones que le tocan al dueño

- [ ] **Dividir un pedido con precio de grupo por cantidad:** un pedido de 4 piezas a $80 ($320) que se parte 2 y 2 pasa a $400, porque cada mitad vuelve al precio normal de $100. ¿Es lo que se quiere?
- [ ] ¿Botones **🧪 Preparar prueba** para las pruebas 1 a 12 de Fletes? (hoy solo existe el de la prueba F).
- [ ] ¿Llevar **dashboards** (`graficasventas`) y **revisión de ventas** (`revisionventas`) a producción? Necesitan primero su propio reloj (`Clock`): hoy solo lo define `pagotarjeta`, que no va a prod, y sin él la aplicación no arranca.
- Pausados por decisión del dueño: **pago con tarjeta** (sigue en QA, Mercado Pago en pausa) y **WhatsApp** (rama `feature/whatsapp-publicaciones`, no va a prod por ahora).

---

## 3. Pruebas y comprobaciones del dueño (sin código)

- [ ] Con el token de una cuenta de **cliente**: `GET https://backend.novedades-jade.com.mx/mis-productos/migration_campana.sql` debe dar **403** (hotfix 4). Si da 200, nginx sirve esa carpeta por su cuenta: avisar.
- [ ] Volver a iniciar sesión en prod y probar Mis pedidos, un abono y cerrar sesión (el refresh ahora exige el encabezado `X-Requested-With`).
- [ ] Medir `FACEBOOK_APP_SECRET` en el VPS: `kubectl exec -n default deploy/proyecto-key-deployment -- sh -c 'echo ${#FACEBOOK_APP_SECRET}'`. Si da 0, el bot de Facebook/Instagram no recibe (el webhook ahora rechaza sin secreto).
- [ ] Pegar `VERIFICAR_MIGRACIONES_PROD.sql` en `inventario_key_qa` y pasar el resultado, para actualizar en `CLAUDE.md` las filas de `migration_pedido_datos_visibles.sql` y `migration_calculo_precio.sql` («sin verificar en QA»).
- [ ] Probar en QA lo anotado en `TESTS_PENDIENTES.md` (hotfix 1 a 4).

---

## 4. Código pendiente (lo hace Claude cuando se indique)

| # | Qué | Por qué importa | Dónde |
|---|---|---|---|
| 1 | Mover los 124 `.sql` fuera de `src/main/resources/static/` (en `dev`) y ajustar los `Path.of` de las 5 `Migracion*Test` | Es la causa de fondo del hotfix 4: hoy los protege una sola regla de `SecurityConfig` | `static/`, `Migracion*Test` |
| 2 | `/v3/api-docs` queda público en QA y devuelve la especificación completa de la API sin sesión | En prod ya está apagado (hotfix 1); falta ver con qué perfil corre el pod de QA | `application-qa.yml` (`springdoc.api-docs`) |
| 3 | Rifa por boletos: editar o quitar el boleto de una persona puede devolver el grupo de otra con el mismo perfil | Defecto confirmado leyendo el código | `GestionarBoletosService.editarParticipacion` / `quitarParticipacion` (`buscarGrupo` sin `concursanteId`) |
| 4 | Pool de conexiones en 5; cada venta pide una segunda conexión (`decidirCampana` con `REQUIRES_NEW`) | Bajo carga se pierde el descuento de campaña sin avisar | `application-docker.yml` / `application-qa.yml`, `VentaServiceImpl` |
| 5 | `cambiarPrecio` no bloquea la fila del pedido | Un abono simultáneo puede dejar el pedido pagado de más (poco probable) | `EditarArticulosService.cambiarPrecio` (compararlo con `DividirPedidoService`) |
| 6 | El webhook de Mercado Pago no valida firma | Lo mitiga que vuelve a preguntar el estado a Mercado Pago; cualquiera puede repetir llamadas | `MercadoPagoController` (`/v1/mp/webhook`) |
| 7 | La **ganancia** se calcula distinta según el canal: la venta directa resta la comisión de terminal; al liquidar un pedido no | Los reportes por categoría y por viaje suman esa ganancia | `VentaServiceImpl` vs `AbonoServiceImpl.crearVentaDesdePedido` |
| 8 | Fletes: borrar un viaje o quitar un producto con ligas solo de ventas devueltas da 500 en vez del mensaje amigable | Menor | `ViajesJpaAdapter` (`fk_vdl_renglon` es `ON DELETE RESTRICT`) |
| 9 | Campañas: el código puede llegar al correo de otra persona (mismo teléfono, otro correo); dos ventas simultáneas pueden crear dos personas; N+1 al abrir el reporte de campaña | Menores | `ParticiparEnCampanaService`, `PersonasCampanaJpaAdapter` |
| 10 | Un reporte vivo que pase de 15 min puede mostrarse un rato «Con error» y luego «Listo»; el programador de tareas tiene un solo hilo para ~14 tareas | Menor | `ReporteLimpiezaScheduler` |
| 11 | `POST /v1/mp/test/simular-pago/{id}` ya no existe: un admin que lo llame recibe 500 (no 404) | Solo estético; si se quiere 404, un manejador de `NoResourceFoundException` | `ExceptionGlobal` |

---

## 5. Hecho el 2026-11-26 (para no volver a preguntarlo)

Cuatro hotfix a producción, ya en `main`, `qa` y `dev`: (1) cobro protegido si falta una tabla, tope de tamaño de página, swagger apagado, CORS viejo fuera y encabezado de refresh obligatorio; (2) simulador de pago borrado y webhook de Facebook cerrado sin secreto; (3) reportes atascados y margen negativo; (4) scripts `.sql` y `openapi.yml` ya no se descargan por HTTP. Producción verificada: tiene todas las tablas, columnas y pantallas de las migraciones 11 a 22, y no hay filas con margen fuera de rango. Detalle en `CLAUDE.md` («Hotfix a producción del 2026-11-26») y en `TESTS_PENDIENTES.md`.
