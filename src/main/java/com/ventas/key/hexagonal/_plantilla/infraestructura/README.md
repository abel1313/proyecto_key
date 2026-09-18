# infraestructura/

> **[Hexagonal: los Adaptadores]**
> **[Clean: Frameworks & Drivers — el círculo de más afuera]**

Todo lo que es tecnología concreta. Es la capa **desechable**: si mañana se cambia MySQL
por Postgres, o REST por gRPC, solo se toca acá.

## Qué va
- Controllers (`entrada/rest/`)
- Entidades JPA, repositorios, adaptadores de persistencia (`salida/persistencia/`)
- Clientes HTTP, productores de Rabbit, acceso a disco (`salida/cliente/`)
- DTOs de request y response (`dto/`)
- Mappers entre DTO ↔ modelo de dominio, y entre entidad JPA ↔ modelo de dominio

## La regla direccional
Esta capa **puede importar** de `dominio/` y `aplicacion/`.
`dominio/` y `aplicacion/` **nunca** importan de acá.

## Entrada vs salida

| | Quién empieza la conversación | Ejemplos |
|---|---|---|
| `entrada/` | **alguien de afuera** llama a la app | REST, consumer de Rabbit, tarea programada |
| `salida/` | **la app** llama para afuera | JPA, cliente HTTP, disco, productor de Rabbit |

En Hexagonal: entrada = *driving* (te manejan), salida = *driven* (vos manejás).
