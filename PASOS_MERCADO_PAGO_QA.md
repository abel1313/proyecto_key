# Poner a prueba el pago en línea con Mercado Pago (QA, credenciales de PRUEBA)

Qué hace esto: en QA el pago en línea del cliente deja de ser de mentira y pasa por **Checkout Pro de Mercado
Pago** (página segura de Mercado Pago) con dinero de prueba. La terminal de QA sigue siendo la simulada.

> **Nunca pegues tokens ni contraseñas en el chat, en documentos ni en git.** Se escriben solo en la VPS (paso 3).
> Si alguna vez se pegó uno en un chat, hay que regenerarlo en Mercado Pago.

## Lo que se necesita (todo de PRUEBA, no de producción)

| Qué | De dónde sale | Para qué |
|---|---|---|
| **Access Token de prueba** | Mercado Pago Developers → tu aplicación → *Credenciales de prueba* → "Access Token" (empieza con `TEST-`) | Crear el cobro y consultarlo |
| **Clave secreta del webhook** | Tu aplicación → *Webhooks* → *Configurar notificaciones* → modo **prueba** → "Clave secreta" | Comprobar que los avisos vienen de Mercado Pago |
| **Usuarios de prueba** (comprador y vendedor) | Tu aplicación → *Cuentas de prueba* | El comprador de prueba es con quien pagas |
| **Tarjetas de prueba** | Documentación de Mercado Pago → "Tarjetas de prueba" (México) | Aprobar o rechazar un pago |

## Paso 1 — Configurar el aviso (webhook) en Mercado Pago

En tu aplicación → *Webhooks* → *Configurar notificaciones* → modo **prueba**:
- **URL:** `https://qa.backend.novedades-jade.com.mx/mis-productos/v1/pagos-tarjeta/webhook`
- **Eventos:** marca **Pagos** (`payment`).
- Guarda y copia la **clave secreta** que te muestra.

## Paso 2 — Esperar a que QA tenga este código

Se sube a `dev` y `qa` solo cuando tú lo ordenes. Hasta entonces el cambio no existe en QA.

## Paso 3 — Cargar las variables en la VPS (namespace `qa`)

El Access Token ya existe en QA como `ACCESS_TOKE_MERCADO_PAGO` (lo usa la terminal vieja). **Cámbialo por el
token de PRUEBA** y agrega las otras dos. Escribe tú los valores en tu terminal de la VPS:

```bash
kubectl set env deployment/proyecto-key-deployment -n qa \
  ACCESS_TOKE_MERCADO_PAGO='TEST-...tu-token-de-prueba...' \
  MP_WEBHOOK_SECRET='...tu-clave-secreta-del-webhook...' \
  PAGOS_PASARELA_EN_LINEA=checkout
kubectl rollout status deployment/proyecto-key-deployment -n qa
kubectl -n qa get pods
```

Todos los pods deben quedar `Running` y `1/1`. Para **volver a lo de mentira** (sin tocar código):
`kubectl set env deployment/proyecto-key-deployment -n qa PAGOS_PASARELA_EN_LINEA=simulada`

## Paso 4 — Probar

1. Entra a QA como **cliente** (no admin), arma un carrito de más de $300 y pulsa **Generar pedido** → **Pagar ahora**.
2. Te lleva a Mercado Pago. Entra con el **usuario comprador de prueba** y paga con una **tarjeta de prueba**.
3. Al terminar regresa a QA, a la pantalla "Confirmando tu pago…" y luego "¡Pago aprobado!". El pedido queda
   **Pagado, falta entregar**.
4. Repite con una tarjeta que Mercado Pago rechace: debe decir "No se pudo cobrar" y el pedido sigue Pendiente.
5. Desde un admin, **Devolver** el cobro (reembolso) y revisa que Mercado Pago lo muestre reembolsado.

## Cosas que se confirman en esta primera prueba (no se pudieron verificar sin credenciales)

- Que la página de Mercado Pago de **prueba** abra. La dirección de pago ya NO se arma a mano: la da Mercado Pago al
  crear el cobro (`init_point`). Si con cuentas de prueba hace falta la variante sandbox, se activa sin tocar código con
  `kubectl set env deployment/proyecto-key-deployment -n qa PAGOS_TARJETA_CHECKOUT_SANDBOX=true`. En el log del pod
  aparece `Preferencia ...: init_point=... sandbox_init_point=...` para ver las dos.
- Que solo aparezca **tarjeta** (se excluyen efectivo en tienda, cajero y transferencia) y los meses sin
  intereses que ofrece Mercado Pago para esa tarjeta de prueba.
- Que el aviso (webhook) llegue: en los logs del pod debe verse `Webhook de pago ...: cobro N queda APROBADO`. Aunque no
  llegue, la pantalla de resultado confirma el pago preguntando a Mercado Pago, así que el pedido igual se actualiza.
- Que el reembolso funcione.

## Producción

No se toca todavía. Antes: credenciales de PRODUCCIÓN, el webhook en modo producción, la migración
`migration_pago_tarjeta.sql` en `inventario_key` y la orden expresa del dueño.
