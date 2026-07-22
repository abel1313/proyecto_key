# Formas de pago vacías en QA — para el equipo de BACK

**Fecha:** 2026-07-16
**De:** Front
**Para:** Back
**Acción requerida:** ✅ Dar de alta datos en BD de QA. No requiere cambios de código (los endpoints ya existen).

---

## Por qué es de back

El front solo **lee** las formas de pago. Los 6 endpoints relacionados son todos `GET`:

```
GET /v1/pagos/tipos-pago
GET /v1/pagos/tarifas
GET /v1/pagos/iva
GET /v1/pagos/opciones
GET /v1/pagos/opciones-estructuradas
GET /v1/pagos/opciones-por-tipo/{tipoPagoId}
```

No hay ningún `POST`/`PUT` para crear `TipoPago`, `DetallePago` o `PagosYMeses`, ni pantalla en el
sistema para darlos de alta. El front depende 100% de que esas filas ya existan en la base de
datos.

---

## Cómo confirmarlo en 10 segundos

Pídeles correr esto con un token válido:

```
GET https://qa.backend.novedades-jade.com.mx/mis-productos/v1/pagos/opciones-estructuradas
```

- Si responde `"data": []` (vacío) → confirmado: falta insertar las formas de pago (Efectivo,
  Transferencia, Tarjeta, etc.) en la BD de QA.
- Si responde `"data": [ ... ]` con opciones → no es esto, avísenme para revisar otra cosa.

**Importante:** revisar también la tabla `pagos_y_meses`, no solo `tipo_pago`. Si `tipo_pago` tiene
filas pero `pagos_y_meses` está vacía, el endpoint puede devolver tipos de pago listados con
`mostrarMeses: false` y sin opciones útiles — no un array vacío limpio. Ese caso también hay que
resolverlo del lado de datos.

---

## Pregunta que sí necesito que respondan

¿Cómo se supone que se dan de alta las formas de pago? No hay pantalla para eso en el sistema.
Es o un `INSERT` manual en BD, o un seed que debió correr al montar QA y no corrió. Eso lo saben
ellos.
