# Pendientes y dudas — sesión del 2026-09-29

Todo lo que se preguntó o quedó abierto hoy, para no perderlo. Se tacha cuando se cierre.

---

## A. Pedidos unidos — saldo al separar ✅ (hecho, falta validar)

**Duda original:** uní 2 pedidos, di un abono de $200, al separarlos les dejé $100 a cada uno, pero
cada pedido seguía debiendo su total completo en lugar de descontar los $100.

**Qué era:** el back sí repartía bien los abonos (se comprobó con un test,
`SepararConAbonosJpaTest`); la pantalla solo mostraba el total, no lo pagado ni lo que falta.

**Qué se hizo** (en `dev` y `qa`, back y front):
- Card de pedidos: muestra "Pagado $X" y "Falta $Y".
- Detalle: el encabezado dice "Debe: $Y" (con total y pagado abajo).
- Separar: columna "Queda debiendo" por pedido mientras se escriben los montos.

- [ ] Validar en QA que se vea bien en los 3 lugares.
- [ ] Decidir cuándo sube a `main` (hoy solo está en `dev`/`qa`).

---

## B. Pedidos — quitar o cambiar un producto con abonos (en definición)

**Regla acordada:** cuando se quita o cambia un producto, la cuenta se hace **solo con ese pedido**.
Lo que sobre es de ese cliente; **no** se pasa a los otros pedidos del grupo.

**Cómo funciona hoy:** el total del pedido se recalcula y los abonos se quedan donde están. Eso está
bien. Lo que falta:

- [x] Si lo abonado ya cubre el nuevo total → el pedido pasa solo a **Pagado** (back, 2026-09-29).
- [ ] Si lo abonado es **más** que el total → mostrar **"Saldo a favor del cliente $X"** en el front
      (pendiente; el back ya deja el pedido Pagado).
- [x] **Bug corregido (back):** un pedido ya Pagado se podía editar; si se cambia un producto por uno más caro sigue
      diciendo Pagado aunque deba, y la venta registrada queda con los productos viejos. Debe
      regresar a Apartado / Ir pagando y borrarse esa venta.
- [x] El botón viejo de quitar producto ya no deja un pedido **sin productos**: pide cancelar.
- [x] **Decidido:** si regresan **todos** los productos de un pedido, no se quitan uno por uno:
      **se cancela el pedido**. El sistema no debe dejar quitar el último producto; debe decir
      "para regresar todo, cancela el pedido".

### Cancelar un pedido que está unido (cómo funciona hoy — no requiere cambios)

Caso: A, B y C están unidos y el cliente de C dice "ya no lo quiero".

- Se cancela **solo C**, con el botón de cancelar de ese pedido. A y B no se tocan.
- El grupo deja de contar a C: su total, lo que pagó y lo que debía ya no suman (regla R8 del
  grupo). Al separar, C tampoco entra al reparto.
- Los productos de C regresan al inventario (en "Ir pagando" no, porque la mercancía ya se la
  llevó; ahí lo que debía queda como deuda que no se cobró).
- Lo que C había abonado se queda registrado en C, y el mensaje de la cancelación dice
  **"Saldo a favor del cliente: $X"**: ese es el dinero a devolverle.
- **La devolución del dinero la haces tú** (efectivo o transferencia). El sistema la avisa, pero
  hasta donde se revisó no registra la salida del dinero.
- Si el cliente no quiere que le devuelvan el dinero sino dejarlo para otra compra, ya existe
  **transferir el abono a un pedido nuevo**.
- Si un pedido ya estaba **Pagado**, cancelarlo es una devolución: solo lo puede hacer un
  administrador.
- Ojo: si el que cancela es el **titular** (el que paga y recoge), primero cambiar "quién recoge"
  a otro pedido del grupo.

- [ ] Revisar si hace falta registrar la salida del dinero devuelto (para que el corte cuadre).

Ejemplo acordado (A, B, C con 2 productos de $100 cada uno):

| Paso | A | B | C |
|---|---|---|---|
| Unidos, dan $150, se separan con $50 c/u y se vuelven a unir | debe 150 | debe 150 | debe 150 |
| C regresa 1 producto ($100) | debe 150 | debe 150 | total 100, **debe 50** |
| Siguen pagando $300 | **Pagado** | **Pagado** | debe 50 |
| C paga sus $50 | Pagado | Pagado | **Pagado** |

Solo hay "saldo a favor" si a un pedido le quitan más de lo que le falta pagar. Ejemplo: A ya pagó
$200 y regresa un producto de $100 → A queda en $100 con $200 pagados → se le deben $100 a ese cliente.

---

## C. Dominio, DNS y correo (en investigación)

Detalle completo en `DOMINIO_DNS_CORREO.md`.

**Duda original:** Hosting-Mexico cobra el alojamiento (orden 609155); ¿qué es eso?, ¿no era el
dominio? ¿Puedo quedarme con el dominio y la VPS (shop, backend, QA) y cambiar solo el correo a un
proveedor más barato?

**Lo que ya se sabe:**
- El dominio ya se renovó. Lo que falta pagar es el alojamiento, donde viven los buzones de correo
  (el primer año venía en la compra).
- La tienda, el backend y QA están en la VPS, no en Hosting-Mexico.
- **El DNS vive dentro del alojamiento** (`ns1-hapi`/`ns2-hapi`). Si se deja vencer sin más, lo más
  probable es que también se caiga la tienda.
- Sí se puede quedarse con dominio + VPS y cambiar solo el correo, pero primero hay que sacar el DNS
  del alojamiento (sección 6 del documento).

- [ ] Enviar la pregunta a Hosting-Mexico (texto listo en la sección 7) y pegar la respuesta.
- [ ] Paso 2 en la VPS: respaldo de todos los registros DNS.
- [ ] Revisar el panel de Hosting-Mexico: precios, vencimientos, buzones, exportar zona DNS.
- [ ] Comparar precios de correo y decidir.
- [ ] **No dejar que se suspenda el alojamiento** hasta tener el cambio terminado.

---

## D. Otros pendientes que ya estaban

- [ ] Correr `backfill_estado_pedido_tipo.sql` en prod (`inventario_key`) antes de subir a `main`
      la corrección de `cambiarTipoPedido` (ver tabla de migraciones en `CLAUDE.md`).
