# ROADMAP DE VALIDACIÓN — Cambios RIFA PLATAFORMAS + ENTREGAS POR ZONA
**Fecha:** 2026-09-09 | **Ramas:** `dev` y `qa`

---

## 📋 RESUMEN DE CAMBIOS

| Área | Qué cambió | Riesgo | Prioridad |
|---|---|---|---|
| **RIFA NUEVA** | Fechas ahora persisten en el DTO + nuevos endpoints de edición | Alto | 🔴 CRÍTICO |
| **ENTREGAS POR ZONA** | Filtro por rango de fechas + dos calendarios + presets | Medio | 🟡 ALTO |
| **Componente** | Nuevo `app-selector-fecha` (calendar compartido) | Bajo | 🟢 OK |

---

## 🎯 TEST PLAN — RIFA PLATAFORMAS

### **Test #1: Persistencia de fechas (Task #36)**
**Dónde:** Menú → **Rifas** → **Agregar nueva rifa** → Configurar rifa  
**Pasos:**
1. Crear una rifa PLATAFORMAS nueva
2. Ir a la sección "Rango de boletos" y seleccionar:
   - Fecha inicio: `2026-09-01`
   - Fecha fin: `2026-09-30`
3. Guardar la rifa
4. **Esperar:** El wizard debería mostrar el rango guardado en los calendarios
5. Recargar la página (`F5`)
6. **Validar:** Los calendarios deben mantener las mismas fechas (NO pedir que se configure de nuevo)

**Resultado esperado:**
- ✅ Las fechas están en los calendarios después de guardar
- ✅ Las fechas persisten después de recargar

---

### **Test #2: Calendario decente (Task #37 — Nuevo componente)**
**Dónde:** Mismo flujo que Test #1, en los selectores de fecha  
**Qué probar:**
1. Click en cualquier selector de fecha → debe abrir un popover con calendario
2. El calendario debe tener:
   - Navegación mes anterior/siguiente (flechas)
   - Día de hoy marcado con borde
   - Día seleccionado marcado con fondo (color de tema)
   - Días desactivados con tachadura y grises
3. Cambiar de mes (click en flechas) → debe actualizar el calendario
4. Seleccionar un día → debe cerrarse el popover y rellenar el campo
5. **Verificar que use colores del tema** (no Material Design indigo/pink fijo)

**Resultado esperado:**
- ✅ Calendario abre/cierra correctamente
- ✅ Navegación de meses funciona
- ✅ Selección de día rellena el campo
- ✅ Colores coinciden con el tema de Personalización

---

### **Test #3: Validaciones (Task #38)**
**Dónde:** Menú → **Rifas** → **Agregar nueva rifa** → Configurar rifa  
**Validaciones a probar:**

#### 3.1 Fecha inicio > fecha fin
- Configurar: Inicio `2026-09-30` | Fin `2026-09-01`
- **Esperado:** Mensaje de error (`"La fecha de inicio no puede ser posterior a la final"`)

#### 3.2 Plataforma obligatoria
- Ir a "Agregar participante"
- Click en botón "Compartir en redes" (capturar boleto)
- Intentar guardar **sin seleccionar plataforma**
- **Esperado:** Error rojo (`"La plataforma del boleto es obligatoria"`)
- **Valores válidos:** Facebook, Instagram, TikTok, Otro

#### 3.3 URL de perfil obligatoria
- Seleccionar una plataforma
- Dejar vacío "URL del perfil en red social"
- Intentar guardar
- **Esperado:** Error (`"URL del perfil es obligatoria"`)

#### 3.4 Fecha del boleto dentro del rango
- Rifa configurada: `2026-09-01` a `2026-09-30`
- Intentar agregar boleto con fecha `2026-10-15` (fuera del rango)
- **Esperado:** Error (`"La fecha del boleto debe estar entre 2026-09-01 y 2026-09-30"`)

**Resultado esperado:**
- ✅ Todos los mensajes de error salen en rojo/alerta
- ✅ El formulario NO se envía si hay error
- ✅ Después de corregir el error, funciona

---

### **Test #4: Inline editing — Editar premio (Task #38 — Feature nueva)**
**Dónde:** Menú → **Rifas** → **Buscar rifa** → seleccionar PLATAFORMAS cerrada  
**Pasos:**
1. Ir a la tabla de "Premios"
2. Encontrar una fila con un premio
3. Click en el icono de editar (lápiz) en esa fila
4. **Fila entra en modo edición** → Se abre row con campos editables
5. Cambiar "Giro ganador" de (ej) `1` a `5`
6. Click en checkmark para guardar
7. **Validar:**
   - ✅ La fila vuelve a mostrar modo lectura
   - ✅ El nuevo valor aparece (Giro ganador = 5)
   - ✅ NO se refrescó la página (edición inline)

**Casos de error:**
- Cambiar a un giro que ya existe en otro premio de esa rifa → debe mostrar error inline
- Cambiar a variante sin stock → debe mostrar error inline

---

### **Test #5: Inline editing — Editar boleto (Task #38 — Feature nueva)**
**Dónde:** Misma pantalla de rifa, tabla de "Boletos"  
**Pasos:**
1. Encontrar una fila con un boleto ya registrado
2. Click en lápiz para editar
3. **Fila entra en modo edición**
4. Cambiar "Motivo" (ej: de "Compartió el reel" a "Compartió el post")
5. Click en checkmark
6. **Validar:**
   - ✅ La fila vuelve a modo lectura
   - ✅ El nuevo motivo aparece
   - ✅ El boleto NO cambió de dueño (mismo `concursanteId`)

**Nota:** El participante sigue siendo el mismo — no hay combobox para cambiar de participante en esta edición.

---

### **Test #6: Editar participante (Existing endpoint, mejorado)**
**Dónde:** Mismo flujo, sección "Participantes"  
**Pasos:**
1. Encontrar un participante
2. Click en lápiz
3. **Fila entra en modo edición**
4. Cambiar nombre (ej: "Juan" → "Juan Carlos")
5. Cambiar teléfono
6. Click en checkmark
7. **Validar:**
   - ✅ Nombre actualizado
   - ✅ Teléfono actualizado
   - ✅ Los boletos del participante NO se borraron

**Antes de esta sesión:** editar un participante no era posible desde la UI; había que eliminar y recrear.

---

### **Test #7: Rifa no desaparece cuando se cierra (Task #36 — Bug fix)**
**Dónde:** Menú → **Rifas** → **Buscar rifa**  
**Pasos:**
1. Crear/usar una rifa PLATAFORMAS que ya pasó su fecha límite (`activa=false`)
2. Click en "Buscar rifa"
3. **Validar:**
   - ✅ La rifa cerrada sigue visible en la lista
   - ✅ Se ve claramente que está cerrada (badge, color, etc.)
   - ✅ Puedo hacer click para abrirla y seguir editando

**Antes:** Si usabas el endpoint `/activas`, la rifa desaparecía y no había forma de volver a abrirla.  
**Ahora:** Usamos `/buscar?tipo=PLATAFORMAS` que devuelve todas.

---

## 🎯 TEST PLAN — ENTREGAS POR ZONA

### **Test #8: Filtro por rango de fechas (Task #39 — Feature nueva)**
**Dónde:** Menú → **Entregas** → **Por zona**  
**Pasos:**
1. Seleccionar una "Zona de entrega"
2. **Nuevo:** Aparecen dos calendarios:
   - "Desde" (fecha inicial)
   - "Hasta" (fecha final)
3. Seleccionar un rango (ej: `2026-09-01` a `2026-09-15`)
4. **Validar:**
   - ✅ La lista de pedidos se filtra (solo muestra pedidos en ese rango)
   - ✅ El campo "Sugerencia de entrega" se calcula con la semana en curso (no cambia por el rango)

---

### **Test #9: Presets rápidos (Task #39 — UX mejorada)**
**Dónde:** Misma pantalla, abajo de los calendarios  
**Qué son:** Botones que precargan rangos:
- "Semana en curso" → calcula lunes a viernes de esta semana
- "Últimos 7 días" → ayer hacia atrás
- "Últimas 2 semanas" → hace 14 días hasta hoy
- "Mes en curso" → 1 de este mes hasta hoy

**Pasos:**
1. Click en uno de los presets (ej: "Últimos 7 días")
2. **Validar:**
   - ✅ Los dos calendarios se rellenan con el rango
   - ✅ La lista de pedidos se actualiza al rango del preset

---

### **Test #10: Validación de rango (Task #39 — Data consistency)**
**Dónde:** Mismo flujo  
**Pasos:**
1. Configurar "Desde": `2026-09-30`
2. Configurar "Hasta": `2026-09-01`
3. Intentar filtrar
4. **Esperado:** Mensaje de error (`"La fecha inicial no puede ser posterior a la final"`)

---

### **Test #11: Sin parámetros = semana en curso (Backward compat)**
**Dónde:** Cargar la pantalla sin tocar nada  
**Pasos:**
1. Entrar a **Entregas** → **Por zona**
2. Seleccionar una zona
3. **SIN hacer click en presets, SIN tocar calendarios**
4. **Validar:**
   - ✅ Se cargan los pedidos de la **semana en curso** (igual que antes)
   - ✅ Calendarios vacíos (ningún rango pre-cargado)
   - ✅ Es compatible hacia atrás (no rompe el flujo viejo)

---

### **Test #12: Programar entrega usa el mismo rango (Task #39 — Critical fix)**
**Dónde:** Después de filtrar un rango  
**Pasos:**
1. Filtrar zona con rango: `2026-09-01` a `2026-09-15`
2. Ver lista de ~5 pedidos en ese rango
3. Click en "Programar entrega"
4. Rellenar: fecha de entrega (`2026-09-16`), hora, punto de encuentro
5. Click en guardar
6. **Validar:**
   - ✅ Se envían correos **solo a los clientes del rango listado**
   - ✅ El contador dice "5 correos enviados" (ó el número exacto que viste en la tabla)
   - ✅ Puedes verificar revisando el correo (en QA/test) o el log del backend

**Importancia CRÍTICA:** Antes, el backend recalculaba la semana por su cuenta → podía enviar correos a clientes distintos de los que veías en pantalla. Ahora le pasas el rango desde el front.

---

## 🎯 TEST PLAN — COMPONENTE COMPARTIDO

### **Test #13: Selector de fecha (app-selector-fecha) — Diseño**
**Dónde:** Tests #1, #2, #8, #9  
**Qué validar:**
1. ✅ El popover tiene esquinas redondeadas (6-14px según tema)
2. ✅ El borde y sombra vienen del token `--card-border` y `--card-shadow-hover`
3. ✅ El texto del mes es gris oscuro/claro según tema (no hardcodeado)
4. ✅ Las flechas de navegación tienen el color accent del tema
5. ✅ El día seleccionado tiene fondo accent (no indigo fijo)
6. ✅ Los días deshabilitados están grises/tachados

**Cómo verificar colores:** Abre DevTools → Inspect selector → mira las custom properties en `<html>` (ej: `--app-accent`, `--card-bg`).

---

## 🔴 CRÍTICOS — Obligatorio antes de dar por "OK"

| # | Test | Rama | Pasos |
|---|---|---|---|
| 1 | Persistencia de fechas RIFA | `qa` | Crear RIFA → F5 → ✅ fechas quedan |
| 7 | Rifa no desaparece | `qa` | Cerrar rifa → Buscar → ✅ sigue visible |
| 11 | Sin parámetros = semana actual | `qa` | Cargar ENTREGAS → ✅ semana en curso |
| 12 | Programar usa mismo rango | `qa` | Filtrar → Programar → ✅ mismos clientes reciben correo |
| 14 | Editar participante no bloquea boletos | `qa` | Participante + editar + agregar boletos → ✅ funciona |
| 15 | Eliminar en edición no causa error | `qa` | Editar → Eliminar → Guardar → ✅ sin "not found" |
| 16 | Agregar premio a rifa sorteada | `qa` | Rifa con giros → Agregar premio → ✅ aviso aparece |

---

## 🟡 ALTOS — Conviene revisar en QA

| # | Test | Rama | Pasos |
|---|---|---|---|
| 2 | Calendario decente | `qa` | Click en fecha → ✅ navega, colores OK |
| 3 | Validaciones | `qa` | Fecha inicio > fin → ✅ error rojo |
| 4,5,6 | Inline editing | `qa` | Editar premio/boleto/participante → ✅ sin F5 |
| 8,9,10 | Filtro ENTREGAS | `qa` | Calendario → presets → validación → ✅ OK |
| 17 | Cambiar giro en rifa sorteada | `qa` | Rifa con giros → Editar premio → ✅ aviso aparece |
| 18 | Clarificación URLs | `qa` | Agregar boleto → Verificar hints → ✅ diferencia clara |
| 19 | Aviso terminal state | `qa` | Rifa completa → Ruleta → ✅ aviso aparece |

---

## 🟢 BAJOS — Spot-check suficiente

| # | Test | Rama | Pasos |
|---|---|---|---|
| 13 | Diseño calendar | `qa` | Verificar colores vs tema (DevTools) |

---

## 🆕 CAMBIOS ADICIONALES — Fixes en flujo de boletos y rifa state (2026-09-09)

### **Test #14: Editar participante no bloquea registro de boletos**
**Dónde:** Menú → **Rifas** → **Buscar rifa** → seleccionar rifa  
**Escenario:** Participante con 0 boletos  
**Pasos:**
1. Buscar/crear una rifa con participante registrado con **0 boletos**
2. Click en lápiz para editar ese participante
3. Cambiar nombre o teléfono
4. Click en checkmark para guardar
5. **Validar:**
   - ✅ El participante se actualiza
   - ✅ El panel de "Agregar boletos" sigue visible y **activo** (no se cierra)
   - ✅ Puedo clickear "Compartir en redes" y agregar boletos sin error

**Antes:** Después de editar, el participante se deseleccionaba y no permitía agregar boletos.  
**Ahora:** La función `guardarParticipante()` llama `seleccionarConcursante()` para mantener el panel abierto.

---

### **Test #15: Eliminar participante en edición no causa "not found"**
**Dónde:** Mismo flujo  
**Pasos:**
1. Editar un participante (fila en modo edición)
2. **Mientras está en edición**, hacer click en el botón "Eliminar" ❌
3. Confirmar la eliminación en el SweetAlert
4. **Sin recargar la página**, cambiar algo en el formulario de boletos
5. Click en "Guardar"
6. **Validar:**
   - ✅ El participante se eliminó correctamente
   - ✅ El formulario se resetea a "crear nuevo participante" (no intenta guardar a ID inexistente)
   - ✅ NO sale error "Participante no encontrado" (antes sí salía)

**Antes:** Si eliminas un participante que estaba en edición, el siguiente intento de guardar fallaba con "not found".  
**Ahora:** `eliminarParticipante()` limpia `participanteEditandoId` cuando borra un participante activo.

---

### **Test #16: Agregar/cambiar premios en rifa ya sorteada — aviso de reinicio**
**Dónde:** Menú → **Rifas** → **Buscar rifa** → seleccionar rifa que **ya tiene sorteados algunos premios**  
**Pasos:**
1. Buscar/usar una rifa PLATAFORMAS que ya corrió sorteo (tiene `boletosDescartados` > 0 ó `rifaTerminada = true`)
2. Ir a la tabla de "Premios"
3. Click en agregar un nuevo premio
4. Rellenar los datos (variante, giro, etc.)
5. Click en guardar
6. **SweetAlert aparece con aviso:**
   - Texto: _"Esta rifa ya sorteó algunos premios. Agregar uno nuevo no recalculará los giros anteriores."_
   - Botones: "Reiniciar rifa primero" | "Proceder de todas formas"
7. Click en "Proceder de todas formas"
8. **Validar:**
   - ✅ El premio se agregó
   - ✅ Los giros anteriores **NO cambiaron**
   - ✅ El nuevo premio aparece en la tabla sin afectar los cálculos

**Casos adicionales:**
- Si hace click en "Reiniciar rifa primero", debe llevarlo al diálogo de reinicio
- Si cancela el diálogo, no hace nada (regresa al estado previo)

**Antes:** Agregar un premio a rifa ya sorteada causaba que el botón "Girar" se quedara deshabilitado (giro = 0).  
**Ahora:** Se detecta con `rifaYaEmpezo` getter y se envuelve con `sobreRifaEmpezada()` que advierte.

---

### **Test #17: Cambiar giro ganador en rifa ya sorteada — aviso igual**
**Dónde:** Mismo flujo, tabla de "Premios"  
**Pasos:**
1. Estar en rifa que ya corrió sorteo
2. Click en lápiz para editar un premio existente
3. Cambiar el "Giro ganador" a otro número
4. Click en checkmark para guardar
5. **SweetAlert igual al Test #16 aparece**
6. Click en "Proceder de todas formas"
7. **Validar:**
   - ✅ El giro se actualizó
   - ✅ El cambio **no recalcula los giros de variantes anteriores**
   - ✅ El ruleta sigue funcionando (no quedar en giro = 0)

**Antes:** Cambiar giro en rifa sorteada podía dejar el ruleta con `giro 0` y botón deshabilitado.

---

### **Test #18: Clarificación de URLs — Perfil vs Publicación**
**Dónde:** Menú → **Rifas** → **Buscar rifa** → **Agregar participante** → **Compartir en redes**  
**Validar hints de UI:**
1. En el campo "Perfil en red social":
   - **Hint:** _"El link a la cuenta de la persona (facebook.com/su-perfil), no el de la publicación: es con lo que se revisa después si sigue siguiendo la página."_
   - Intentar guardar con un link de publicación (ej: facebook.com/share/p/XXXXX)
   - **Validar:** Que tenga claro que ese no es el campo para eso
2. En el campo "URLs de lo que compartió":
   - **Hint:** _"El link de cada publicación que compartió, no el de su perfil."_
   - Aquí es donde van los links de posts/reels/videos que la persona shared
   - Ej: facebook.com/share/p/XXXXX, facebook.com/photo.php?fbid=123

**Validar:**
- ✅ Los hints son claros y visibles (no recortados)
- ✅ El usuario entiende la diferencia entre ambos campos sin ambigüedad
- ✅ Guardar con ambos correctos funciona sin error

**Antes:** Ambos campos se llamaban "URL" de formas similares → confusión.

---

### **Test #19: Aviso de terminal state — Rifa completamente sorteada**
**Dónde:** Menú → **Rifas** → **Buscar rifa** → Rifa que ya sorteó **TODOS** los premios  
**Pasos:**
1. Ir a una rifa PLATAFORMAS que ya terminó (`rifaTerminada = true`)
2. Hacer scroll a la sección "Ruleta/Sorteo"
3. **Validar:**
   - ✅ En lugar de mostrar "giro X de Y", aparece **aviso azul:**
     - Icono: ℹ️
     - Texto: _"Esta rifa ya sorteó todos sus premios, por eso no gira. Usa Reiniciar si quieres volver a sortear."_
   - ✅ El botón "Girar" sigue deshabilitado (no hay más giros)
4. Click en "Reiniciar"
5. **Validar:**
   - ✅ Se abre el diálogo de reinicio
   - ✅ Después de reiniciar, el aviso desaparece y vuelve a haber giros

**Antes:** Si la rifa terminaba, el ruleta se quedaba congelado sin explicación clara.

---

## 📝 NOTAS TÉCNICAS PARA QA

### Backend
- **Rama:** `dev` (ya mergeada en `qa`)
- **Compilación:** ✅ `mvn -q -o compile` sin errores
- **Archivos clave:**
  - `ConfigurarRifaResumenDto.java` — Nuevos campos `fechaInicioBoletos`, `fechaFinBoletos`
  - `BoletoRifaServiceImpl.java` — Método `editar()` con validaciones
  - `ConfigurarRifaVarianteService.java` — Método `editar()` para premios
  - `EntregaZonaServiceImpl.java` — Método `listarPendientes(id, desde, hasta)` con `resolverRango()`

### Frontend
- **Rama:** `qa`
- **Build:** ✅ `ng build --configuration=qa` sin errores
- **Archivos clave (actualizados 2026-09-09):**
  - `src/app/shared/selector-fecha/` — Nuevo componente calendar
  - `boletos-rifa.component.ts/html/scss` — Fixes en lifecycle de participantes, rifa state detection, hints de URL
  - `entregas-zona.component.*` — Calendarios + presets + validación

### Base de datos
- **BD para dev/qa:** `inventario_key_qa`
- **Sin migración nueva** — todos los cambios son en DTOs y queries, no schema

---

## ✅ CHECKLIST DE LIBERACIÓN

- [ ] Test #1 (Persistencia) — ✅ OK
- [ ] Test #2 (Calendario) — ✅ OK
- [ ] Test #3 (Validaciones) — ✅ OK
- [ ] Test #4 (Editar premio) — ✅ OK
- [ ] Test #5 (Editar boleto) — ✅ OK
- [ ] Test #6 (Editar participante) — ✅ OK
- [ ] Test #7 (Rifa no desaparece) — ✅ OK
- [ ] Test #8 (Filtro ENTREGAS) — ✅ OK
- [ ] Test #9 (Presets) — ✅ OK
- [ ] Test #10 (Validación rango) — ✅ OK
- [ ] Test #11 (Compat hacia atrás) — ✅ OK
- [ ] Test #12 (Programar = rango) — ✅ OK
- [ ] Test #13 (Diseño) — ✅ OK
- [ ] Test #14 (Editar participante no bloquea boletos) — ⏳ Por verificar en QA
- [ ] Test #15 (Eliminar en edición sin error) — ⏳ Por verificar en QA
- [ ] Test #16 (Agregar premio a rifa sorteada) — ⏳ Por verificar en QA
- [ ] Test #17 (Cambiar giro en rifa sorteada) — ⏳ Por verificar en QA
- [ ] Test #18 (Clarificación URLs) — ⏳ Por verificar en QA
- [ ] Test #19 (Aviso terminal state) — ⏳ Por verificar en QA
- [ ] Revisar CAMBIOS_FRONT.md — ✅ OK
- [ ] Dev compila — ✅ OK
- [ ] QA compila — ✅ OK

---

## 📞 CONTACTO SI ALGO EXPLOTA

Si un test falla, anota:
1. Test # y pantalla
2. Paso exacto donde falló
3. Comportamiento observado vs esperado
4. Logs (F12 → Console o backend stdout)
5. Screenshot si es de UI

Sube todo a un issue o mensaje con esa info y es rápido pinchar el bug.

