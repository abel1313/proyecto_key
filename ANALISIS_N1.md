# Análisis de Problemas N+1 JPA/Hibernate

Fecha: 2026-06-09

---

## `proyecto_key_new`

### ALTA severidad — actuar pronto

---

#### 1. `GanadorRifaServiceImpl.java` — `continuarVariante()` (líneas 158-162)

**Problema:** Carga lista de `GanadorRifa` sin JOIN FETCH, luego accede a `.getConfigurarRifaVariante().getId()` y `.getConcursante()` dentro de un `.stream().filter()` → dispara 1 query por ganador para cada relación.

**Relaciones afectadas:**
- `GanadorRifa.configurarRifaVariante` (ManyToOne - LAZY)
- `GanadorRifa.concursante` (ManyToOne - LAZY)

**Costo:** Con N ganadores = mínimo 1 + N + N queries

**Fix:** Agregar JOIN FETCH en `IGanadorRifaRepository.findGanadoresByRifaId()`:
```java
@Query("SELECT g FROM GanadorRifa g JOIN FETCH g.configurarRifaVariante JOIN FETCH g.concursante WHERE g.configurarRifaVariante.configurarRifa.id = :rifaId AND g.descartado = false")
List<GanadorRifa> findGanadoresByRifaId(@Param("rifaId") Integer rifaId);
```

---

#### 2. `ProductosServiceImpl.java` — `compartirImagenesVariante()` (líneas 350-362)

**Problema:** Doble loop anidado. `findByProductoId()` carga `ProductoImagen` sin su `Imagen`. Dentro del loop, cada acceso a `.getImagen()` dispara una query separada.

**Relaciones afectadas:**
- `ProductoImagen.imagen` (ManyToOne - LAZY)

**Costo:** N variantes × M imágenes = hasta N×M queries extra

**Fix:** Agregar JOIN FETCH en el repository:
```java
@Query("SELECT pi FROM ProductoImagen pi JOIN FETCH pi.imagen WHERE pi.producto.id = :productoId")
List<ProductoImagen> findByProductoIdWithImagen(@Param("productoId") Integer productoId);
```

---

#### 3. `ConfigurarRifaVarianteService.java` — `listarPorRifa()` (líneas 91-94)

**Problema:** `findByConfigurarRifaIdOrderByOrdenAsc()` sin JOIN FETCH. Luego `.map(this::toDto)` accede a `.getVariante()` y dentro `.getProducto()` por cada elemento de la lista → 2 queries extra por item.

**Relaciones afectadas:**
- `ConfigurarRifaVariante.variante` (ManyToOne - LAZY)
- `Variantes.producto` (ManyToOne - LAZY)

**Costo:** Con N variantes = 1 + N + N queries

**Fix:** Agregar `@EntityGraph` en el repository:
```java
@EntityGraph(attributePaths = {"variante", "variante.producto"})
List<ConfigurarRifaVariante> findByConfigurarRifaIdOrderByOrdenAsc(Integer configurarRifaId);
```

---

#### 4. `GanadorRifaServiceImpl.java` — `reiniciar()` (líneas 247-267)

**Problema:** `findByConfigurarRifaId()` sin JOIN FETCH antes de `deleteAll()`. Hibernate puede cargar relaciones lazy de cada entidad durante la operación de delete en cascada.

**Relaciones afectadas:**
- Todas las de `GanadorRifa` y `HistorialRifaVariante` (LAZY)

**Costo:** Potencial N queries adicionales por cascade al eliminar

**Fix:** Usar `@Modifying @Query` con DELETE directo en lugar de findAll + deleteAll:
```java
@Modifying
@Query("DELETE FROM GanadorRifa g WHERE g.configurarRifaVariante.configurarRifa.id = :rifaId")
void deleteByConfigurarRifaId(@Param("rifaId") Integer rifaId);
```

---

### MEDIA severidad

---

#### 5. `IGanadorRifaRepository.java` — `findGanadoresByRifaId()` (línea 22)

**Problema:** La query navega relación anidada en el WHERE (`g.configurarRifaVariante.configurarRifa.id`) pero no hace JOIN FETCH en el SELECT. Las relaciones `configurarRifaVariante` y `concursante` quedan lazy y se cargan individualmente al acceder a ellas.

**Fix:** Ver fix del punto #1 (mismo método).

---

#### 6. `IConfigurarRifaVarianteRepository.java` — `findByConfigurarRifaIdOrderByOrdenAsc()` (línea 13)

**Problema:** Método generado por Spring Data sin `@EntityGraph`. Usado en múltiples services que acceden a `.getPalabraClave()` — cada acceso es una query extra.

**Fix:** Ver fix del punto #3 (mismo método).

---

### BAJA severidad

---

#### 7. `Usuario.java` — `roles` y `permisosExtra` (líneas 39, 43)

**Problema:** Ambas relaciones son `FetchType.EAGER`. Además `Roles` también tiene EAGER a `Permiso` → 3 niveles de carga transitiva automática.

**Estado:** Aceptable porque es carga de usuario individual (no bulk). Considerar cambiar a LAZY + JOIN FETCH explícito en login si se nota lentitud.

---

#### 8. `Variantes.java` — `palabraClave` (línea 39)

**Problema:** `FetchType.EAGER` en relación `palabraClave`.

**Estado:** Aceptable, es tabla de lookup pequeña. Sin N+1 evidente.

---

## `micro_imagenes` — Riesgo bajo

El micro no tiene relaciones JPA explícitas (@OneToMany/@ManyToOne activas). Las entidades son planas. Las queries principales ya usan proyecciones JPQL y JOINs correctos.

---

#### 9. `ProductoImagenService.java` — `buscarImagenProducto()` (líneas 117-125)

**Problema:** Loop sobre lista de IDs llamando `imagenCasoUso.getOne(imagenId)` por cada uno = "N+1 de disco" (query a BD + lectura de archivo por cada ID).

**Mitigante actual:** Límite hardcodeado de 5 intentos (línea 118).

**Fix sugerido (no urgente):** Precargar todas las imágenes del producto en una sola operación batch antes del loop, elegir la primera disponible.

---

## Queries ya optimizadas (no tocar)

| Archivo | Método | Por qué está bien |
|---------|--------|-------------------|
| `IImagenProductoInfraRepository.java:37` | `listarImagenesProducto()` | Usa constructor JPQL (proyección), sin lazy |
| `IImagenProductoInfraRepository.java:54` | `listarConDetalle()` | Query nativa con JOIN explícito |

---

## Checklist de fixes

- [x] Fix #1 — JOIN FETCH en `findGanadoresByRifaId()` (cubre también punto #5) — `IGanadorRifaRepository.java`
- [x] Fix #2 — JOIN FETCH en `findByProductoId()` de `ProductoImagen` — `IProductoImagenRepository.java`
- [x] Fix #3 — JOIN FETCH en `findByConfigurarRifaIdOrderByOrdenAsc()` con variante+producto (cubre también punto #6) — `IConfigurarRifaVarianteRepository.java`
- [x] Fix #4 — DELETE directo en `reiniciar()` vía `@Modifying @Query` — `IGanadorRifaRepository.java`, `IHistorialRifaVarianteRepository.java`, `GanadorRifaServiceImpl.java`
- [x] Fix #9 — Batch precarga en `buscarImagenProducto()` en micro_imagenes — `ProductoImagenService.java`
