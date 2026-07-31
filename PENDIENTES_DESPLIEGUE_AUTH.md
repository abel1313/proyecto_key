# Checklist de despliegue — correcciones de seguridad de autenticación

**Fecha:** 2026-07-31 · **Rama:** `dev` · **Estado:** commiteado y pusheado · migraciones SQL hechas en QA y producción

Este documento es el **plan de acción**. El detalle técnico de cada corrección está en
[`SEGURIDAD_AUTH.md`](SEGURIDAD_AUTH.md); lo que el front necesita saber está en
[`CAMBIOS_FRONT.md`](CAMBIOS_FRONT.md).

**Resultado de la tanda:** 16 hallazgos corregidos · 2 no aplican (#15 y #16) · 0 pendientes de programar.

---

## Orden recomendado

```
1. Commit en dev                        ✅ hecho
2. Migraciones SQL en QA y producción   ✅ hechas
3. Merge dev → qa                       ← SIGUIENTE
4. Probar los 5 escenarios en QA
5. Avisar al front
6. Merge qa → main
7. Post-despliegue (rotar clave, encender flags)
```

---

## 1. ✅ Commit y push en `dev`

✅ Hecho — 10 commits en `dev`, pusheados a `origin/dev` el 2026-07-31.

**19 archivos modificados, 9 nuevos.**

<details>
<summary>Archivos nuevos</summary>

- `entity/SesionRefresh.java`
- `repository/ISesionRefreshRepository.java`
- `service/SesionRefreshService.java`
- `scheduler/SesionRefreshScheduler.java`
- `exeption/ExceptionCodigoInvalido.java`
- `static/migration_intentos_codigo_reset.sql`
- `static/migration_intentos_codigo_verificacion.sql`
- `static/migration_sesion_refresh.sql`
- `SEGURIDAD_AUTH.md`

</details>

---

## 2. ✅ Migraciones SQL — HECHAS (QA y producción)

Ejecutadas el 2026-07-31 en **ambas** bases. `ddl-auto: none` en todos los perfiles, así que nada
de esto se creaba solo — sin ellas el login truena al desplegar.

Correrlas en producción **antes** de desplegar el código fue seguro: las columnas nuevas tienen
`DEFAULT 0` y la tabla nueva no la usa nadie todavía, así que el `main` actual las ignora.

| # | Script (`src/main/resources/static/`) | Qué hace |
|---|---|---|
| 1 | `migration_intentos_codigo_reset.sql` | Columna `intentos_codigo_reset` en `usuario_modificacion` |
| 2 | `migration_intentos_codigo_verificacion.sql` | Columna `intentos_codigo_verificacion` en `usuario_modificacion` |
| 3 | `migration_sesion_refresh.sql` | **Tabla nueva** `sesion_refresh` |

**Mapeo de bases (de `CLAUDE.md`):**

| Rama | Base de datos | Cuándo correr |
|---|---|---|
| `dev` y `qa` | `inventario_key_qa` (la misma) | ✅ **Ejecutadas 2026-07-31** |
| `main` | `inventario_key` | ✅ **Ejecutadas 2026-07-31** (adelantadas al despliegue) |

---

## 3. Merge `dev` → `qa`

⬜ Pendiente

```bash
git checkout qa && git pull origin qa
git merge dev --no-ff -m "Merge dev → qa: correcciones de seguridad en autenticacion"
git push origin qa
```

---

## 4. Probar en QA — 5 escenarios

No hay tests automatizados de estos flujos; la verificación fue compilación + revisión. Estos son
los que tocan la lógica nueva de sesiones, ordenados por riesgo:

| # | Prueba | Resultado esperado |
|---|---|---|
| 1 | ⬜ Login → refresh → logout → refresh otra vez | El último refresh da **401** |
| 2 | ⬜ Refresh dos veces con el **mismo** token (el ya rotado) | La segunda vez **mata la sesión** (detección de reuso) |
| 3 | ⬜ Cambiar contraseña estando logueado | La sesión anterior muere; hay que volver a entrar |
| 4 | ⬜ Deshabilitar un usuario en BD (`enabled = 0`) | Su siguiente request da **401**, sin esperar 15 min |
| 5 | ⬜ Login con `passwordTemporal = true` | **403** en todo salvo cambiar contraseña |

> `rate-limit-habilitado: false` en QA **no afecta** a estas pruebas — son de sesión, no de rate
> limit. Se pueden hacer tal cual está.

---

## 5. Avisar al front

⬜ Pendiente

Ya está publicado en su repo (`documentos_front_back_nodevedaades_jade`, commit `7eb3234`), pero
conviene que alguien se los mencione de viva voz. **Tres acciones les tocan:**

1. **`passwordTemporal` ahora se fuerza** → si no redirigen a cambiar contraseña, el usuario recibe
   403 en todo lo demás.
2. **Cambiar contraseña cierra la sesión propia** → deben mandar al login después.
3. **`X-Requested-With`** en refresh/logout → agregarlo y avisar cuando esté desplegado.

---

## 6. Merge `qa` → `main`

Las migraciones de producción **ya están hechas** (se adelantaron el 2026-07-31). Fue seguro: las
columnas nuevas tienen `DEFAULT 0` y la tabla nueva no la usa nadie hasta que se despliegue el
código, así que el `main` actual las ignora sin enterarse.

⬜ Pendiente el merge.

```bash
git checkout main && git pull origin main
git merge qa --no-ff -m "Merge qa → main: correcciones de seguridad en autenticacion"
git push origin main
```

**Antes del merge a `main`, confirmar:** que ningún cliente real entraba por los orígenes CORS que
se quitaron de producción (`http://localhost:4200`, `http://51.178.29.99:30001`,
`qa.shop.novedades-jade.com.mx`). Ver hallazgo 10.

---

## 7. Post-despliegue

| ⬜ | Qué | Cuándo / por qué |
|---|---|---|
| ⬜ | **Todos los usuarios se deslogean una vez** | Inevitable: los refresh tokens viejos no tienen `jti`/`sessionId`. Desplegar en horario de poco movimiento |
| ⬜ | **Rotar `TOKEN_JWT`** | La clave anterior (`miClaveSuperSeguraDe32Caracteres`) quedó en el historial de git. Sacarla del código no la borra del historial |
| ⬜ | **Encender `seguridad.exigir-header-refresh: true`** | Sólo cuando el front confirme que manda `X-Requested-With`. Primero QA, después producción. Si se enciende antes, todos pierden la sesión a los 15 min |
| ⬜ | **Quitar `rate-limit-habilitado: false`** de `application-qa.yml` | Al terminar de validar. Mientras esté apagado, QA está publicado en internet con la fuerza bruta de login libre |

---

## Lo que NO quedó pendiente (para que no se busque de nuevo)

- **#15 — proxies / `X-Forwarded-For`:** verificado el 2026-07-31, **hay exactamente un proxy**
  (nginx en la VPS; sin ingress de K8s, sin Cloudflare en modo proxied). El código ya era correcto.
  Sólo habría que revisarlo si se activa la nube naranja de Cloudflare o se mete un CDN/WAF delante.
- **#16 — rate limit en QA:** decisión tomada, se deja apagado mientras dure la tanda de pruebas.
