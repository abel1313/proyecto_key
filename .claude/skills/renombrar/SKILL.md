---
name: renombrar
description: Renombrar un término en todo el proyecto Novedades Jade (front y back) de forma segura y completa, en una rama propia. Usar cuando el dueño pida "cambia X por Y en todo el proyecto", "renombra…", o para el renombrado en curso variante → artículo. Cubre inventario, decisión de qué se renombra y qué no, español (género y número), contrato con el front, base de datos, verificación y entrega.
---

# Renombrar un término en todo el proyecto

Esta skill está escrita como la trabajaría alguien con años haciendo renombres en sistemas en
producción: un renombre parece un buscar-y-reemplazar, y es la forma más fácil de romper una app que
funciona. La regla de oro es **inventario primero, cambios después, y nunca a ciegas**.

Pedido del dueño (2026-10-01): una skill **genérica** ("cambia X por Y en todo el proyecto"), que se
haga **en una rama nueva** para no afectar lo demás. Rama: sale de `dev` y se junta a `dev` cuando
esté probada; de ahí sigue el flujo normal `dev → qa → main`.

---

## 0. Antes de tocar nada: la petición exacta

Escribir y confirmar con el dueño, en una tabla, **antes de crear la rama**:

| Dato | Ejemplo (variante → artículo) |
|---|---|
| Término viejo y nuevo, con sus formas | variante / variantes / Variante / VARIANTE → artículo / artículos / Artículo / ARTÍCULO |
| Género y artículo gramatical | "la variante" (f) → "el artículo" (m): cambian *la/el, una/un, esta/este, nueva/nuevo, seleccionada/seleccionado* |
| Alcance pedido | solo textos de pantalla / también código / también base y endpoints |
| Repos | front (`producto_venta_online`), back (`proyecto_key`), micro (`micro_imagenes`) |
| Qué NO se toca aunque coincida | palabras que contienen el término y significan otra cosa (ej. "invariante", `IVariable…`) |

Si el alcance no está dicho, **se pregunta**. No se asume "todo".

## 1. Los 4 niveles — de menos a más riesgo

Cada nivel es un commit (o varios) separado. Se puede parar en cualquier nivel.

| Nivel | Qué incluye | Riesgo | Cómo se hace |
|---|---|---|---|
| **1. Lo que ve el usuario** | Textos del HTML, mensajes `Swal`, `title`, `placeholder`, mensajes de error del back (`RuntimeException("…")`), correos, tickets, textos del chatbot | Bajo | A mano, archivo por archivo, cuidando género y número |
| **2. Código interno** | Variables locales, comentarios, nombres de métodos privados, nombres de archivos y clases **que no son contrato** | Medio | Refactor del IDE/compilador; nunca `sed` ciego sobre identificadores |
| **3. Contrato con el front** | Rutas de endpoints, nombres de campos en JSON (`varianteId`), DTOs, rutas de Angular | Alto | **Versionado**: se crea `/v2/...` con el nombre nuevo y `/v1/` sigue vivo hasta que el front migre (regla de `CLAUDE.md`). Nunca se cambia un endpoint en el mismo lugar |
| **4. Base de datos** | Tablas, columnas, FKs, índices, valores guardados como texto | Muy alto | Migración SQL probada (regla de `CLAUDE.md`: base desechable, dos veces, consultas de verificación), anotada en la tabla de migraciones; entidades `@Table`/`@Column` al mismo tiempo; correr en `inventario_key_qa` antes que en prod |

Para **variante → artículo** hoy rige `CLAUDE.md`, sección "Renombrado en curso": niveles 1 y 2
(variables locales y comentarios) sí; clases, DTOs, campos, rutas y tablas **todavía no** mientras
las rutas digan `variantes`. Esta skill no cambia esa decisión: si el dueño pide más, se confirma
y se actualiza `CLAUDE.md` en el mismo cambio.

## 2. Paso a paso

1. **Rama** — en cada repo que se toque:
   ```bash
   git checkout dev && git pull origin dev
   git checkout -b rename/<viejo>-a-<nuevo>        # ej. rename/variante-a-articulo
   ```
2. **Inventario** — antes de cambiar una sola línea, sacar la lista completa y guardarla en
   `RENOMBRE_<viejo>_A_<nuevo>.md` (en el back), agrupada por nivel:
   ```bash
   # todas las formas, sin distinguir mayúsculas, excluyendo dependencias y compilados
   grep -rniE "variantes?" --include=*.{ts,html,scss,java,sql,md,yml,properties} \
        --exclude-dir={node_modules,dist,target,.git} .
   # contar por archivo para ver dónde está el grueso
   grep -rliE "variantes?" ... | xargs -I{} sh -c 'echo "$(grep -ciE "variantes?" {}) {}"' | sort -rn
   ```
   Revisar también: plantillas de correo, textos del chatbot (prompts), mensajes de WhatsApp,
   reportes/Excel, datos semilla en `.sql`, nombres de permisos (`submenu.ruta`, `accion`), y los
   nombres del menú en la base (skill `nombres-reales`).
3. **Clasificar** cada aparición en: renombrar / no tocar (contrato, BD, otra palabra) / dudosa.
   Las dudosas se le preguntan al dueño **todas juntas**, no una por una.
4. **Cambiar por nivel**, de 1 a 4, y hasta donde se acordó. En español, leer la frase completa:
   - "Selecciona una variante" → "Selecciona un artículo"
   - "Variantes seleccionadas: 3" → "Artículos seleccionados: 3"
   - "¿Eliminar esta variante?" → "¿Eliminar este artículo?"
   - Acentos: "Artículo", "artículos" (nunca "Articulo" en un texto de pantalla).
5. **Verificar** (todo tiene que pasar antes de juntar):
   - Back: `mvn test` completo.
   - Front: `ng build` sin errores; pruebas E2E si existen para esas pantallas.
   - Grep de residuos: el término viejo solo debe quedar donde se decidió no tocar (lista del inventario).
   - Revisión en el navegador de las pantallas cambiadas, de día y de noche (skill `diseno-componentes`).
   - Si hubo nivel 3: el `/v1/` viejo sigue respondiendo igual; documentar el `/v2/` en `CAMBIOS_FRONT.md`.
   - Si hubo nivel 4: migración corrida en una base desechable dos veces y anotada.
6. **Entregar** al dueño: qué cambió por nivel, qué se dejó a propósito y por qué, y las pantallas
   a revisar (agregar la prueba a `PRUEBAS_QA_<fecha>.md` como 🔴 PRUEBA PENDIENTE).
7. **Juntar** cuando el dueño lo apruebe: merge `--no-ff` de la rama a `dev`, y de ahí el flujo normal.
   Si `dev` avanzó mientras tanto, primero se trae `dev` a la rama y se vuelve a verificar.

## 3. Lo que un renombre nunca hace

- `sed -i 's/variante/articulo/g'` sobre todo el repo. Rompe imports, rutas, JSON, SQL y frases.
- Cambiar un campo del JSON o una ruta del back en el mismo commit que el front que la usa, sin `/v2/`.
- Renombrar una columna o tabla sin migración probada, o con la entidad desalineada.
- Mezclar el renombre con otros cambios (un hotfix, una mejora): el diff de un renombre tiene que
  poder revisarse solo. Los hotfixes a `main` **no** llevan renombre (regla de `CLAUDE.md`).
- Dejar documentos desactualizados: `CLAUDE.md`, skills, `CAMBIOS_FRONT.md` y `PRUEBAS_QA_*.md` que
  citen el nombre viejo se actualizan en la misma rama.
