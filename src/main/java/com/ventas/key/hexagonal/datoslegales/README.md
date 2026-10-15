# Dominio `datoslegales` — los datos del negocio que la ley pide mostrar

Sale de `LEGAL_PLAN_DE_ACCION.md`, punto 4 (2026-10-07). La LFPC art. 76 bis III pide que, **antes de
comprar**, el cliente vea quién vende, el **domicilio físico**, **teléfonos** y un medio para
reclamar. Hoy la tienda solo mostraba el correo.

Los datos los captura el dueño en **Configuración del negocio → Datos legales**; la tienda los toma
de `GET /v1/datos-legales` (público) para el pie de página, Términos, Aviso de privacidad y el ticket.

| # | Regla | Dónde |
|---|---|---|
| D1 | `faltan` dice qué falta para el 76 bis III: nombre del responsable, domicilio, teléfono y correo | `DatosLegales.faltantes()` |
| D2 | Todo es opcional para guardar; lo que se escribe, con formato válido (400 con el motivo) | constructor de `DatosLegales` |
| D3 | RFC de 13 (persona física) o 12 (empresa), se guarda en mayúsculas | `DatosLegales` |
| D4 | Teléfono de 10 dígitos; acepta espacios, guiones, paréntesis y +52 al escribirlo, se guarda solo con dígitos | `DatosLegales` |
| D5 | Texto vacío = no capturado (se guarda NULL) | `DatosLegales.limpio()` |

Tabla `datos_legales_negocio`, **una sola fila con `id = 1`** (`migration_datos_legales_negocio.sql`).
Sin entidad JPA a propósito: SQL directo en `DatosLegalesJdbcAdapter` (id fijo, ver la lección de
`tiktok_token` en `CLAUDE.md`).
