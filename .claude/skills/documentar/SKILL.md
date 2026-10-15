---
name: documentar
description: Cómo documentar cada cambio de Novedades Jade (back y front) — qué archivo recibe qué, con qué formato, y el checklist para poder decir "ya quedó documentado". Usar SIEMPRE al terminar un cambio de código, un plan, una regla del dueño, una tabla, una migración o un trámite externo; cuando el dueño diga "documenta", "anótalo", "actualiza los docs", "¿ya quedó documentado?", "¿tienes skill para documentar?"; y antes de ofrecer o ejecutar "sube". También al escribir CAMBIOS_FRONT.md, TESTS_PENDIENTES.md, PLAN_*.md, PRUEBAS_PENDIENTES_*.md o el README de un dominio hexagonal.
---

# Documentar — qué va dónde, cómo se escribe y cuándo se da por cerrado

**Pedido del dueño (2026-10-14):** *"¿y ya tienes skill para documentar?"* — hasta ese día las reglas vivían
repartidas en `CLAUDE.md` y en otras skills. Esta las junta en un solo lugar. **No reemplaza** a `CLAUDE.md`:
si algo choca, gana `CLAUDE.md`.

## 1. La regla

**Se documenta en el mismo cambio que el código**, nunca "después". Un cambio sin su documento no está
terminado. Y **no se da por cerrado** hasta pasar el checklist de la sección 5.

## 2. Qué cambió → dónde se anota

| Qué cambió | Dónde va | Cómo |
|---|---|---|
| **Algo que el front ve o debe saber**: endpoint, campo, comportamiento de pantalla, aviso, permiso | `CAMBIOS_FRONT.md` (raíz del back) — **única** fuente de verdad | Entrada nueva **arriba**, sección 3 |
| **Un test que habría hecho falta** (no se escriben tests) | `TESTS_PENDIENTES.md` del repo donde vive el código (back **y/o** front) | Fecha · Dónde · Tipo · **Debe comprobar** con casos concretos y de error, sección 4 |
| **Una prueba de QA para el dueño** | `PRUEBAS_PENDIENTES_<fecha>.md` (back) | Skills `dar-el-caso` (cada paso con Dónde / Haz / Debes ver), `pruebas-de-impacto` (mapa antes/después) y `preparar-prueba` (datos con 🧪) |
| **Reglas de un dominio hexagonal** | `hexagonal/<dominio>/README.md` | Reglas numeradas (R1, R2…), **acordadas con el dueño antes de programar** (skill `arquitectura-hexagonal-limpia`) |
| **Una decisión o regla de negocio del dueño** (flujo, pantallas, qué se hace y qué no) | `PLAN_*.md` del tema (sección nueva con fecha) | Cita **textual** de lo que pidió + tabla de reglas con id (C1, P1…) + qué quedó programado |
| **Tabla nueva en la base** | `CLAUDE.md`, inventario "toda tabla nueva se anota aquí" | Nombre real, para qué es y **qué `@Entity` la mapea** |
| **Migración `.sql`** | `CLAUDE.md`, tabla "Migraciones ya corridas — registro" | Probada en base desechable **dos veces**; con consulta de verificación al final que dé ≥ 1 fila (skill `probar-en-local`) |
| **Trámite externo** (Meta, TikTok, SAT…) | `ALTA_NEGOCIO_META_TIKTOK.md`, sección 8 (bitácora) | Cada paso con fecha, errores con el texto exacto, lo descartado, capturas, textos pegados. **Nunca** tokens ni contraseñas |
| **Una regla de comportamiento para sesiones futuras** | `CLAUDE.md` (y el del front si es del front) o una skill | Con el **por qué** y el día en que pasó |
| **Algo que se decidió NO hacer** | `CLAUDE.md` | Para no volver a investigarlo (ejemplo: *"el chat en vivo se queda sin imágenes"*) |
| **Una skill nueva o cambiada** | Enlazarla desde `CLAUDE.md` y desde `reglas-pedidos` (sección 6, "Skills relacionadas") | Una línea de cuándo se usa |

**No se crean** documentos nuevos de endpoints: `API_CONTRATOS.md`, `ENDPOINTS_FRONT.md`, `ENDPOINT_FRONT.md`,
`cambios_y_endpoints.md`, `endpoints_palabras_clave.md` y `ENDPOINTS_MIGRACION_V1.md` están **desactualizados**
y no se mantienen. Todo va en `CAMBIOS_FRONT.md`.

## 3. Cómo se escribe una entrada de `CAMBIOS_FRONT.md`

```markdown
### [BUG-KEY-NN] 🆕 Título corto en palabras del dueño (AAAA-MM-DD) — solo back | solo front | back y front

Pedido del dueño: *"su frase textual"*.

**Antes:** qué pasaba (lo que fallaba o cómo se veía).
**Ahora:** qué pasa.

**Back:** endpoints (método + URL completa con contexto `/mis-productos/...`), campos que el front consume
(solo los útiles; sin IDs de disco ni rutas de servidor), y los 204/404/422/500 posibles con una línea cada uno.
**Front:** pantalla, componente y qué cambia (textos, botones, validaciones).

Detalle: README del dominio / PLAN / prueba donde se comprueba.
```

- **Número siguiente**: mirar el último `BUG-KEY-NN` y sumar uno. Va **arriba** de los demás.
- **Siempre el "Antes" y el "Ahora"**, también en bugs de seguridad o autorización: el front necesita saber qué
  comportamiento cambia.
- Si el response es binario: Content-Type y que el body son bytes, no JSON.
- Si **no cambia el contrato** (solo front o solo back): decirlo en el título, para que nadie lo busque.
- **Un texto que cambió** (aviso, botón, título) se busca en todo el repo (`grep -rn "texto viejo"` en back y
  front: pruebas `PRUEBAS_*.md`, `e2e/`, CAMBIOS_FRONT) y se actualiza donde aparezca. *Pasó el 2026-10-14:* el
  e2e esperaba "¡Producto guardado!" y la 17.7 decía "¿Quieres agregar sus artículos ahora?" después de cambiar
  esos textos.

## 4. Cómo se escribe una entrada de `TESTS_PENDIENTES.md`

```markdown
### AAAA-MM-DD — Qué se hizo
**Dónde:** clase.método() / componente / endpoint
**Tipo:** unitario · MySQL (SQL nativo) · controller (URL y status) · e2e (Playwright contra QA)
**Debe comprobar:**
- [ ] acción → resultado esperado (incluye los casos de error: *"4 piezas de una línea de 3 → 400 «Solo hay 3»"*)
```

- Lo más nuevo va **arriba**. Una por cambio, en el repo que tiene el código (si toca los dos, una en cada uno).
- Si un cambio **rompe un test que ya existe**: no se arregla ni se borra; se anota en la sección "Tests existentes
  que quedaron viejos".
- Lo que **sí** se hace siempre: comprobar que compila (`mvn -q compile`, `ng build --configuration development`)
  y correr cada `.sql` en una base desechable.

## 5. Checklist antes de decir "ya quedó documentado"

No basta con documentar lo último que se dijo.

1. **Releer toda la conversación** (no solo el último mensaje) y listar cada endpoint, campo, flujo o comportamiento
   que se mencionó como necesario para el front o como pedido del dueño.
2. **Verificar, uno por uno**, que cada punto tenga su párrafo en el documento: se **vuelve a leer el archivo**, no se
   asume.
3. Si algo **ya se comportaba así antes** pero el dueño preguntó por eso, se documenta igual (para que no lo
   vuelva a preguntar).
4. Cada texto de pantalla cambiado está actualizado en pruebas, e2e y docs (sección 3, último punto).
5. El **pedido del dueño va citado textual** y su comentario 💬 copiado tal cual, con la respuesta ↳.
6. Hay entrada en `TESTS_PENDIENTES.md` (back y/o front) y, si hay algo que probar, en `PRUEBAS_PENDIENTES_*.md`.
7. Tabla nueva → inventario de `CLAUDE.md`. Migración → registro. Skill → enlazada.
8. **Sin secretos**: nada de tokens, contraseñas ni llaves, ni parciales.
9. Los términos del dueño: **artículo**, **modelo**, **Apartado**, **Ir pagando** (skill `reglas-pedidos`), no "variante" ni
   "crédito".
10. Fechas **absolutas** (2026-10-14), nunca "hoy" ni "ayer".

## 6. Después de documentar: avisar qué quedó y dónde

Al terminar se le dice al dueño, en español y sin jerga:
- **Qué archivos se tocaron** y qué se escribió en cada uno (una línea por archivo).
- **Si quedó sin subir**: *"solo en local; lo subo a dev y a qa cuando digas «sube»"*.
- Si se subió: el commit de cada rama y que el deploy de QA terminó en verde (skill `reglas-pedidos`, sección 1).

## 7. Lo que nunca se hace

- Tocar el **repo de documentos del front** (`documentos_front_back_nodevedaades_jade`) sin que el dueño lo pida en
  ese momento. Se puede **leer** para informar; escribir, copiar, commitear o pushear **no**. Si lo pide, se sigue el
  procedimiento de espejo de `CLAUDE.md` (sobrescribir el archivo completo, `diff` para confirmar, y si ambos
  lados cambiaron a la vez, mostrar el diff y preguntar cuál gana).
- Hacer **commit o push** sin que diga "sube" (o "haz commit"). Documentar sí se hace directo, sin preguntar.
- Crear documentos de endpoints nuevos fuera de `CAMBIOS_FRONT.md`.
- Escribir tests automáticos en vez de anotarlos.
- Dar por documentado algo sin haber **releído el archivo**.
- Anotar tokens, contraseñas o llaves.
