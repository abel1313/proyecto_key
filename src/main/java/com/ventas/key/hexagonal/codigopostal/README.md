# Dominio `codigopostal`

Municipio, estado y colonias de un código postal. Lo usan **Mis datos** y **ver cliente** para llenar el
municipio solo. Antes se le preguntaba al servicio externo (DIPOMEX) cada vez; si fallaba, la pantalla se
quedaba callada.

## Reglas (acordadas con el dueño el 2026-10-14)

| # | Regla |
|---|---|
| R1 | Primero se busca en **nuestra tabla** `codigo_postal`. Si está, se responde de ahí y no se llama al externo. |
| R2 | Si no está, se llama al externo. Si lo encuentra, se **guarda** (municipio, estado, colonias y fecha) y se responde. |
| R3 | Si el externo dice que el código **no existe**, se responde 404 y **no se guarda** (un error de dedo no se queda). |
| R4 | Si el externo **falla o tarda más de 8 s**, se responde 502 *«No pudimos buscar… escribe el municipio a mano»* y no se guarda nada. La pantalla deja escribir el municipio: el pedido no se detiene. |
| R5 | Lo que el cliente **escribe a mano nunca entra a la tabla** (no está verificado). |
| R6 | Dos peticiones del mismo código nuevo a la vez: queda una sola fila (`UNIQUE codigo`); la segunda no falla. |
| R7 | Un código guardado hace **más de un año** se vuelve a consultar al externo. Si en ese momento el externo falla o ya no lo conoce, se usa el guardado. |

Extras que no cambian el contrato: si nuestra tabla no existe todavía o falla al leer/guardar, se sigue con
el servicio externo (se avisa en el log); un código que no son 5 dígitos es 404 sin llamar a nadie.

## Dónde está cada cosa

| Qué | Dónde |
|---|---|
| Modelo y vigencia (R7) | `dominio/modelo/CodigoPostalInfo` |
| Motivos de fallo | `dominio/excepcion/CodigoPostalException` (NO_VALIDO, NO_EXISTE → 404; SERVICIO_NO_DISPONIBLE → 502) |
| Puertos | `dominio/puerto/entrada/BuscarCodigoPostalCasoUso`, `dominio/puerto/salida/{CodigoPostalGuardadoPort, CodigoPostalExternoPort}` |
| R1–R4, R7 | `aplicacion/servicio/BuscarCodigoPostalService` |
| Tabla (R2, R6) | `infraestructura/salida/persistencia/CodigoPostalJpaAdapter` (entidad `CodigoPostalGuardado`, repositorio `ICodigoPostalGuardadoRepository`, en `mis/productos/` porque JPA solo escanea ahí) |
| Servicio externo | `infraestructura/salida/cliente/CodigoPostalExternoAdapter` → `DopoMexService` (tiempo límite y llave) |
| Endpoint | `infraestructura/entrada/rest/CodigoPostalController`: `GET /mis-productos/v1/dipomex/getCodigoPostal/{codigoPostal}` (público) |

## Tabla

`codigo_postal` (`migration_codigo_postal.sql`): `codigo` (único, 5 dígitos), `municipio`, `estado`,
`colonias` (arreglo JSON de textos), `consultado`.
