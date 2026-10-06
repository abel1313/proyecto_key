---
name: buscadores
description: Reglas de TODO buscador de Novedades Jade (front y back) — cuándo sale la búsqueda, cuánto espera, mínimo de letras, qué pasa con vacío, cómo no se mezclan respuestas viejas, y qué resultados puede ofrecer según dónde está (venta/pedido = solo lo que se puede vender). Usar SIEMPRE antes de crear o tocar un buscador, autocomplete o filtro de texto, y cuando el dueño diga "busca por cada letra", "salen productos sin stock", "el buscador se quedó trabado" o "muestra lo de antes".
---

# Buscadores — una sola forma de buscar en toda la app

Pedido del dueño (2026-10-06, detalle de pedido): *"en la búsqueda del cambio primero hace la búsqueda
por letra y ya habíamos quedado cómo sería… y sigue apareciendo productos con stock en 0 y productos
que tienen stock pero no están habilitados"*. Las reglas ya estaban en `CLAUDE.md` (sección "Reglas de
comportamiento del catálogo y los buscadores"); esta skill las junta con las que faltaban para que
**cada buscador nuevo salga bien a la primera**.

---

## 1. Cuándo sale la búsqueda (front)

| Regla | Por qué |
|---|---|
| **Espera 400 ms** después de la última tecla (`debounceTime(400)`). Escribir "blusa" = **1** búsqueda, no 3 ("blu", "blus", "blusa") | Una por letra hace que la lista brinque y llena el back de peticiones |
| **Texto con letras: mínimo 3** (`Constants.MIN_CARACTERES_BUSQUEDA`). Con 1–2 **no sale al back** y la lista se limpia (o se avisa *"Escribe al menos 3 letras…"*) | Con 1–2 el `LIKE '%x%'` barre casi todo el catálogo |
| **Solo números** (número de pedido, `#120`): desde **1** dígito | Hay pedidos #1–#9 |
| **Vacío SÍ dispara** y significa "quitar el filtro y traer todo" (o limpiar la lista en un buscador de selección) | Si no, queda en pantalla el resultado anterior |
| Vacío y corto **se atienden antes del Subject**, no con un `filter()` antes del `debounceTime` | Un `filter(t => t.length >= 3)` también se come el vacío y limpiar el input no recarga nada |
| **`switchMap`** entre el texto y la petición | Descarta la respuesta de una búsqueda vieja que llega tarde (si no, "bl…" pisa a "blusa") |
| **`catchError` DENTRO del `switchMap`**, devolviendo lista vacía | El back contesta 404/400 cuando no encuentra nada. Si el error llega al `subscribe`, RxJS termina la suscripción y el buscador queda **muerto** hasta recargar |
| `takeUntil(destroy$)` | No dejar suscripciones vivas al salir de la pantalla |
| Paginar/filtrar con chips **por el mismo `switchMap`** | Así un clic rápido tampoco mezcla respuestas |

Plantilla (la de `detalle-pedido.component.ts` → `prepararBuscadorArticulo()` y `mis-pedidos.component.ts`):

```ts
private readonly termino$ = new Subject<string>();

ngOnInit(): void {
  this.termino$.pipe(
    debounceTime(400),
    map(t => t.trim()),
    switchMap(t => t.length < Constants.MIN_CARACTERES_BUSQUEDA
      ? of([])
      : this.servicio.buscar(t).pipe(
          map(r => r?.t ?? []),
          catchError(() => of([]))          // ← adentro, nunca en el subscribe
        )),
    takeUntil(this.destroy$)
  ).subscribe(lista => { this.resultados = lista; this.buscando = false; });
}

alEscribir(): void {
  const t = (this.texto ?? '').trim();
  if (t.length < Constants.MIN_CARACTERES_BUSQUEDA) { this.resultados = []; }   // vacío/corto: ya
  else { this.buscando = true; }
  this.termino$.next(t);
}
```

## 2. Qué puede ofrecer (back) — depende de dónde está el buscador

| Dónde | Qué trae | Endpoint |
|---|---|---|
| **Tienda → Buscar** (administrar) | Todo: con y sin stock, habilitados y dados de baja (para administrarlos) | `GET /v1/variantes/buscar`, `/admin/filtrar` |
| **Tienda pública** (cliente) | stock > 0, artículo y modelo habilitados, no catálogo interno, **con imagen** | `/v1/variantes/buscar-filtrado` |
| **Vender o meter en un pedido** (⇄ / ➕ del detalle de pedido, y cualquier buscador nuevo que agregue a una venta o pedido) | **Solo lo que se puede vender ahora:** stock > 0 **en el artículo y en su modelo**, artículo **y** modelo habilitados, no catálogo interno. **No** exige imagen | `GET /v1/variantes/para-pedido` |

- **Nunca reusar el buscador de administrar para vender.** Fue el error del 2026-10-06: ⇄ usaba
  `/buscar`, que al admin le trae lo sin stock y lo dado de baja; se elegía y el back lo rechazaba al guardar.
- El back **igual valida al guardar** (stock y habilitado): el filtro del buscador es para no ofrecer
  lo que no se puede, no reemplaza la validación.
- **Sin caché** en un buscador donde importa el stock (cambia con cada venta y edición de pedido). La
  caché de `variantesProductoCache` es para el catálogo, no para vender.
- Sin resultados: en un buscador nuevo, **200 con lista vacía** (no 404). Los viejos que contestan 404
  se dejan así; el front ya lo trata como "sin resultados".
- Texto con `%` o `_`: escaparlo (`PedidosFiltradosJdbcAdapter.escaparLike`) para que no sea comodín.

## 3. Texto en pantalla

- Placeholder que diga **por qué se puede buscar**: *"Número, nombre, teléfono, correo o artículo…"*.
  Si solo dice "número", nadie sabe que también busca por nombre (pasó en Mis pedidos).
- Sin resultados, decir **por qué** si hay un filtro escondido: *"No hay ningún artículo con
  existencias que coincida. Los que no tienen stock o están dados de baja no salen aquí."*
- Nombres reales (skill `nombres-reales`).

## 4. Cómo se prueba (va en la guía de QA)

| # | Haz esto | Debes ver |
|---|---|---|
| 1 | Herramientas del navegador (F12) → **Red**, filtra por la URI. Escribe "blusa" letra por letra | **Una** petición, con `blusa` |
| 2 | Escribe 2 letras | **Ninguna** petición; lista vacía o aviso |
| 3 | Escribe algo que no existe | "Sin resultados" y el buscador **sigue funcionando** al escribir otra cosa |
| 4 | Borra todo | Lista completa (o vacía si es de selección) |
| 5 | En un buscador de venta/pedido, busca un modelo con una talla en 0 y otra dada de baja | **No salen**; en Tienda → Buscar **sí** |

Prueba automática del back: una de repositorio con artículos sin stock, dados de baja y modelo
deshabilitado que **no** deben salir (`VarianteParaPedidoRepositoryTest`).

## 5. Estado de los buscadores (2026-10-06)

Ya cumplen: Mis pedidos (`mis-pedidos`), ⇄/➕ del detalle de pedido (`detalle-pedido`), Tienda → Buscar,
Modelos (`productos/producto/all`), Agregar/Actualizar artículo, Rifas (boletos, rifa del mes, agregar
rifa), Créditos / Abonos, Unir pedidos.

**Buscan con cada tecla (sin espera ni `switchMap`) — arreglar cuando se toque esa pantalla** (regla
oportunista, igual que el renombrado):
- `buscador/buscar-generico`
- `rifas/buscar-rifa`
- `usuarios/usuarios/buscar-usuarios`
- `ventas/venta-producto/add-venta` (además revisar que solo ofrezca lo que se puede vender, sección 2)

Al arreglar uno, moverlo a "Ya cumplen".
