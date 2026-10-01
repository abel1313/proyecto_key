---
name: pruebas-de-impacto
description: Antes de entregar CUALQUIER cambio en Novedades Jade (back o front) — un endpoint, una URI, un campo, una regla de pedidos, un método de servicio, una tabla, un componente, un color —, sacar todo lo que depende de lo que se movió (y lo que depende de eso, en cadena) y escribir por cada uno una prueba con pasos reales, lo que daba ANTES y lo que debe dar DESPUÉS, más la prueba del cambio mismo al final. Usar SIEMPRE al terminar un cambio, al renombrar (skill renombrar), al tocar pedidos/ventas/carrito (skill reglas-pedidos) y cuando el dueño pregunte "¿qué tengo que probar?".
---

# Pruebas de impacto — qué se movió, a quién le pega y cómo se comprueba

Escrita como la trabajaría alguien que ya vivió muchos despliegues: el error que llega a
producción casi nunca está en lo que cambiaste. Está en **lo que dependía de eso y nadie revisó**.
Cambias una URI y la pantalla que la usa sigue bien, pero el buscador de rifas usaba la misma, el
chatbot también, y la regla de seguridad que la protegía tenía la ruta escrita a mano.

Pedido del dueño (2026-10-01): *"si te digo que en pedido agregues esto, y pedido le pega a 5 cosas,
tienes que hacer esas 5 pruebas: anotar se movió esto, a esto primero se esperaba esto y ahora esto,
y así con todo lo que le pegue; al final el cambio que hice"*. Y: *"las mismas pruebas antes y
después, para que dé el mismo resultado y no haya afectado nada"*.

---

## 1. La regla

**Ningún cambio se entrega sin su mapa de impacto.** El mapa dice:

1. **Qué se movió**, exacto: `GET /v1/variantes/buscar` → `GET /v2/articulos/buscar`; el método
   `PedidoServiceImpl.cambiarTipoPedido()`; el campo `precioRebaja`; el token `--card-body-bg`.
2. **Quién le pega** (consumidores directos) y **quién le pega a esos** (el eco), hasta llegar a algo
   que una persona hace en una pantalla o a algo que corre solo (un job, un webhook, el bot).
3. **Una prueba por cada uno**, con: dónde (menú → pantalla → botón, con los nombres reales),
   qué se hace, **antes** (qué daba), **después** (qué debe dar), y **⛔ no puede pasar**.
4. **La prueba del cambio mismo, al final.** Primero se comprueba que no se rompió nada alrededor;
   después que lo nuevo hace lo que se pidió.

Si un consumidor **no debe** notar el cambio (es lo normal), su "después" es **igual** a su "antes".
Eso también se escribe: "debe dar exactamente lo mismo que antes". Es la prueba que más errores
encuentra.

## 2. Cómo sacar quién le pega — no de memoria, buscando

Se busca **por el nombre exacto de lo que se movió** en los dos repos. Lista mínima por tipo de cambio:

| Se movió… | Dónde buscar consumidores |
|---|---|
| **URI / endpoint** | Front: servicios (`grep -rn "/v1/variantes" src/app`), componentes que llaman a ese método del servicio, interceptores (listas de URLs públicas o sin spinner), pruebas `e2e/`. Back: `SecurityConfig` (reglas por URI escritas a mano: si la URI nueva no está, responde 401/403 o queda pública), otros servicios que llaman al mismo método, `micro_imagenes` u otros micros que llamen al back, el chatbot, `CAMBIOS_FRONT.md` |
| **Método de servicio (back)** | Todos los que lo llaman: `grep -rn "nombreMetodo(" src/main/java`; por cada llamador, su endpoint; por cada endpoint, su pantalla. Jobs `@Scheduled`, listeners de Rabbit, el bot de redes, el chatbot |
| **Campo de un DTO / JSON** | Interfaces del front que lo leen (`grep -rn "campo" src/app`), plantillas HTML que lo pintan, cálculos (totales, saldos), lo que se guarda en `localStorage`/`sessionStorage`, reportes Excel |
| **Regla de negocio** (pedidos, cobro, stock, precio) | Todo flujo que pase por esa regla: crear, editar, cobrar, abonar, cancelar, unir/separar, venta directa, carrito, chatbot. La skill `reglas-pedidos` tiene el mapa de flujos |
| **Tabla / columna** | Entidades (`@Table`, `@Column`), consultas nativas y JPQL (`grep -rn "nombre_columna\|campoJava" src/main/java`), migraciones, backfills, el limpiador nocturno, reportes |
| **Componente / estilo del front** | Pantallas que lo usan (`grep -rn "app-selector" src/app --include=*.html`), tokens que consume, día y noche, celular y escritorio |
| **Permiso / acción** | `SecurityConfig`, `PantallaGuard`, los `*ngIf` de botones, Gestión de roles, el JWT (hay que volver a entrar) |

**El eco:** por cada consumidor encontrado, se repite la búsqueda **sobre él**. Ejemplo:
`VarianteServiceImpl.buscarFiltrado()` ← `GET /v1/variantes/buscar-filtrado` ← `VarianteService.buscarFiltrado()`
(front) ← Tienda → Buscar **y** ← `RifaService.buscarArticulos()` ← Rifas → Agregar rifa → buscador de premios.
Dos pantallas, dos pruebas. Se para cuando se llega a una pantalla, a un job o a un sistema externo.

Para no adivinar nombres de menús, pantallas y botones: skill `nombres-reales`.

## 3. El "antes" — se mide, no se supone

Lo ideal es **correr las pruebas en QA con la versión vieja** (antes de subir el cambio) y anotar lo
que salió: el texto, el total, cuántos resultados, la captura. Eso es la **línea base**. Después de
subir, se corren **los mismos pasos con los mismos datos** y se compara.

- Si ya no se puede correr la versión vieja, el "antes" se saca **leyendo el código viejo**
  (`git show dev:ruta/archivo`) y se marca **"antes (según el código)"**.
- Los datos de prueba se anotan (número de pedido, código de barras, cliente) para que el antes y
  el después usen exactamente lo mismo.
- En el back, siempre que se pueda, la comparación también va **automatizada**: una prueba que pega a
  la URI vieja y a la nueva y exige la misma respuesta, o que corre el flujo viejo y el nuevo con los
  mismos datos. Correr `mvn test` completo, no solo la clase nueva.

## 4. Cómo se escribe — en `PRUEBAS_QA_<fecha>.md`

Va en el documento de pruebas del día (o en el de la rama, si el cambio vive en una rama propia),
con el formato que ya usa el dueño: **🔴 PRUEBA PENDIENTE**, *Antes de empezar*, *Pasos*,
*✅ Debe pasar*, *⛔ No puede pasar*. Además de la tabla de pendientes de arriba del documento.

```markdown
## Prueba N — <qué se cambió, en palabras del dueño>

### Mapa de impacto
| # | Se movió | Le pega a | Dónde se ve | Antes | Después |
|---|---|---|---|---|---|
| N.1 | `GET /v1/variantes/buscar-filtrado` → `/v2/articulos/buscar-filtrado` | Tienda → Buscar (filtros) | `tienda/buscar` | lista de artículos con filtros | **lo mismo** |
| N.2 | (mismo) | Rifas → Agregar rifa → buscar premio | `rifas/agregar` | lista de artículos para premio | **lo mismo** |
| N.3 | (mismo) | Regla de `SecurityConfig` (público) | sin sesión | responde 200 sin token | **lo mismo**, también en `/v2` |
| N.4 | **El cambio** | — | Herramientas del navegador → Red | la llamada iba a `/v1/variantes/...` | ahora va a `/v2/articulos/...` |

### 🔴 PRUEBA PENDIENTE — N.1 Tienda → Buscar con filtros
**Antes de empezar:** …datos de prueba (código de barras, pedido)…
**Pasos:** 1. … 2. … 3. …
**✅ Debe pasar:** … (igual que antes: mismos resultados, mismo orden, mismo total)
**⛔ No puede pasar:** error 401/403/404, lista vacía, spinner que no se quita, …
```

Reglas de redacción:
- **Nombres reales** de menús, pantallas y botones (skill `nombres-reales`), nunca inventados.
- Cada paso es una acción que se puede hacer con el mouse; nada de "verificar que el endpoint
  responda" sin decir **dónde** se ve (pantalla o Herramientas del navegador → Red, filtrando por
  la URI).
- El "antes" y el "después" van **lado a lado**. Si son iguales, se dice "lo mismo que antes".
- Si el cambio toca dinero (totales, saldos, abonos, precios), el resultado esperado va **con
  números**: "Total $450, Abonado $200, Resta $250", nunca "el total correcto".
- Al final de la sección, la prueba del cambio mismo.

## 5. Cuándo un consumidor no se puede probar en pantalla

Jobs nocturnos, webhooks de Meta/TikTok, el limpiador de imágenes, Rabbit: se escribe **cómo
dispararlo** (el endpoint de admin que lo corre, la hora, el mensaje que hay que mandar al bot)
y **qué revisar** (log, fila en la tabla con la consulta SQL exacta). Si de plano no se puede
disparar en QA, se anota como **⚠️ sin prueba en QA** con el motivo; nunca se omite en silencio.

## 6. Checklist antes de decir "ya quedó"

- [ ] Busqué por el nombre exacto de cada cosa que se movió, en back **y** front (y en otros micros si aplica).
- [ ] Seguí el eco hasta pantallas, jobs o sistemas externos.
- [ ] Revisé `SecurityConfig` si se movió una URI (la nueva tiene **las mismas** reglas que la vieja).
- [ ] Cada consumidor tiene su prueba con antes / después / ⛔.
- [ ] Las pruebas de "debe dar lo mismo" usan los mismos datos antes y después.
- [ ] La prueba del cambio mismo va al final.
- [ ] Hay prueba automática donde se pudo (`mvn test` completo en verde; `ng build` sin errores).
- [ ] Las pruebas quedaron en `PRUEBAS_QA_<fecha>.md` con 🔴 y en la tabla de pendientes.
- [ ] Si cambió un contrato con el front, también quedó en `CAMBIOS_FRONT.md`.

## 7. Lo que nunca se hace

- Entregar un cambio con "ya lo probé" sin decir **qué** se probó y **dónde**.
- Probar solo la pantalla que se pidió. El dueño pidió el cambio en una; los errores salen en las otras.
- Escribir el "antes" de memoria cuando se podía medir en QA.
- Dar por buena una prueba de dinero sin números.
