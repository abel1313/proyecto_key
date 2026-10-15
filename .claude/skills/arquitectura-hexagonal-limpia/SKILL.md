---
name: arquitectura-hexagonal-limpia
description: Arquitectura Hexagonal (puertos y adaptadores) + Clean Architecture del back de Novedades Jade (proyecto_key). Usar SIEMPRE antes de crear un dominio, caso de uso, controller, entidad o servicio nuevo en el back, antes de mover código viejo de mis/productos a hexagonal/, y al revisar que un cambio respete la regla de dependencia. Dice qué va en cada capa, en qué orden se construye, cómo se migra lo viejo poco a poco y qué revisar antes de entregar.
---

# Arquitectura Hexagonal + Clean — cómo se construye el back

> **Desde 2026-10-06 no se escriben tests automáticos** (regla en `CLAUDE.md`): donde esta skill dice
> "prueba unitaria", "prueba automática" o "`mvn test` en verde", en su lugar se anota en
> `TESTS_PENDIENTES.md` qué test falta y qué debe comprobar, y solo se verifica que compile.

Escrita como la aplicaría un arquitecto con experiencia que ya migró sistemas vivos: la
arquitectura existe para que las **reglas del negocio** (lo que el dueño decide: Apartado sin
dinero, cómo se reparte un abono, cuándo un pedido queda Pagado) vivan en un lugar que no depende de
Spring, de MySQL ni del JSON del front. Si mañana cambia la base, el framework o la pantalla, las
reglas no se tocan.

Decisión del dueño (2026-10-01): **se va pasando poco a poco**. Lo nuevo nace con esta
arquitectura; lo viejo se migra pieza por pieza cuando se toca por otra razón.

Referencia del código: `src/main/java/com/ventas/key/hexagonal/README.md` y la plantilla
`hexagonal/_plantilla/` (un README por carpeta). Dominios ya hechos para copiar el estilo:
`grupopedido`, `preferenciafiltro`, `precio`, `stock`, `botredes`, `pedidoarticulo`.

---

## 1. Las dos arquitecturas en una tabla

| | Hexagonal (Ports & Adapters) | Clean Architecture |
|---|---|---|
| Pregunta que responde | ¿Cómo entra y sale la información del núcleo? | ¿Quién puede depender de quién? |
| Vocabulario | **Puerto** (interfaz) y **adaptador** (implementación). Puertos de **entrada** (lo que el núcleo ofrece) y de **salida** (lo que el núcleo necesita) | **Capas concéntricas**: Entidades → Casos de uso → Adaptadores de interfaz → Frameworks |
| En este proyecto | `dominio/puerto/entrada`, `dominio/puerto/salida`, `infraestructura/entrada`, `infraestructura/salida` | `dominio/` = Entidades · `aplicacion/` = Casos de uso · `infraestructura/` = Adaptadores + Frameworks |

Cada README de carpeta lo marca así: `[Hexagonal: Driven Port]   [Clean: Interface Adapter]`.

## 2. La regla que no se rompe: las dependencias apuntan hacia adentro

```
infraestructura ──▶ aplicacion ──▶ dominio        (nunca al revés)
```

- `dominio/` no importa **nada** de framework: ni `org.springframework.*`, ni `jakarta.persistence.*`,
  ni Jackson, ni Lombok de entidades JPA. Java puro. Si aparece uno de esos imports ahí, está mal.
- `aplicacion/` importa solo de `dominio/`.
- `infraestructura/` importa de las dos; aquí viven Spring, JPA, WebClient, Rabbit, el JSON.
- **Inversión de dependencia:** el dominio declara en `puerto/salida/` la interfaz de lo que
  necesita ("guardar un grupo", "registrar un abono"); infraestructura la implementa. El dominio
  nunca sabe si detrás hay MySQL, un archivo o una llamada HTTP.

Revisión rápida antes de entregar:
```bash
grep -rnE --include=*.java "import (org\.springframework|jakarta\.persistence|com\.fasterxml)" src/main/java/com/ventas/key/hexagonal/*/dominio
grep -rn --include=*.java "\.infraestructura\." src/main/java/com/ventas/key/hexagonal/*/dominio src/main/java/com/ventas/key/hexagonal/*/aplicacion
```
Los dos tienen que salir vacíos (al 2026-10-01 lo están).

## 3. Qué va en cada carpeta

```
hexagonal/<dominio>/
├── dominio/
│   ├── modelo/          records/clases con las REGLAS (validan en el constructor, métodos con nombre de negocio)
│   ├── excepcion/       una excepción por regla rota, con mensaje en palabras del dueño
│   └── puerto/
│       ├── entrada/     interfaz del caso de uso (lo que el dominio ofrece)
│       └── salida/      interfaces de lo que el dominio necesita (persistencia, otros sistemas)
├── aplicacion/
│   └── servicio/        implementa el puerto de entrada: orquesta, @Transactional, sin reglas propias
└── infraestructura/
    ├── entrada/rest/    @RestController: traduce HTTP ↔ caso de uso, nada de lógica
    ├── dto/             request/response del JSON (contrato con el front)
    └── salida/
        ├── persistencia/  adaptadores JPA que implementan los puertos de salida
        └── cliente/       adaptadores HTTP/otros sistemas
```

- **Las reglas viven en `dominio/modelo`.** Si una regla del dueño está en un `if` del controller o
  del servicio, está en el lugar equivocado. Ejemplo bueno: `GrupoPedidos.repartir()` decide cómo se
  reparte un abono y rechaza un pago parcial a Apartados.
- **El servicio orquesta**: lee por un puerto, le pide al modelo que decida, guarda por otro puerto.
- **Un método, una responsabilidad** (regla de `CLAUDE.md`): el que escribe en disco no guarda en BD.
- **Excepciones de dominio → HTTP** en un solo lugar (handler de infraestructura), con el mensaje
  que verá el dueño.

### Trampas propias de este proyecto
- Las **entidades JPA** de un dominio nuevo viven en `mis/productos/entity` y sus repositorios en
  `mis/productos/repository`, porque el escaneo de JPA solo cubre `com.ventas.key.mis.productos`
  (ver `grupopedido/README.md`). El adaptador de persistencia del dominio las usa; el dominio no.
- Componentes Spring sí se encuentran en `hexagonal/` (`scanBasePackages = "com.ventas.key"`).
- Si dos servicios se necesitan entre sí, Spring no arranca (ciclo): partir el caso de uso, como se
  hizo con `ConsultarGruposCasoUso` y `UnirPedidosCasoUso`.
- Cada tabla nueva se anota en `CLAUDE.md` con su entidad (regla del proyecto).

## 4. Orden de construcción (de adentro hacia afuera)

0. **Reglas primero.** Escribir y acordar con el dueño las reglas del dominio (checklist en
   `_plantilla/README.md`: identidad, estados y transiciones, invariantes, validaciones, efectos,
   permisos). Proponer las que no dijo. Sin reglas acordadas no hay código.
1. `dominio/modelo` + sus **pruebas unitarias** (sin Spring, rápidas).
2. `dominio/excepcion`.
3. `dominio/puerto/salida` → `dominio/puerto/entrada`.
4. `aplicacion/servicio` + prueba con puertos simulados (Mockito).
5. `infraestructura/salida` (JPA) + prueba de repositorio si hay SQL propio.
6. `infraestructura/entrada/rest` + `dto` → documentar en `CAMBIOS_FRONT.md`.
7. README del dominio con sus reglas (R1, R2, …) y "dónde está cada cosa".

Arrancar por el controller hace que el diseño gire alrededor del JSON en vez del negocio.

## 5. Migrar lo viejo poco a poco (patrón estrangulador)

Lo viejo (`mis/productos/{controller,service,entity,repository}`) **no se reescribe de golpe**.
Cuando hay que tocar una pieza por otra razón:

1. Identificar la **regla de negocio** que se toca y sacarla a un modelo de dominio nuevo
   (ej. `precio`, `stock`), con sus pruebas.
2. El servicio viejo **llama** al dominio nuevo en lugar de tener la regla adentro (puente). El
   endpoint y el contrato con el front no cambian.
3. Cuando todo un flujo ya pasa por el dominio, se crea el caso de uso y el controller nuevo
   (si cambia el contrato, `/v2/` conviviendo con `/v1/`).
4. Al final se borra el código viejo que quedó sin uso.

Cada paso deja el sistema funcionando y con pruebas en verde; nunca un "big bang".

## 6. Antes de entregar — checklist

- [ ] `dominio/` sin imports de framework ni de infraestructura (greps de la sección 2 vacíos).
- [ ] Las reglas del dueño están en el modelo y tienen prueba unitaria cada una.
- [ ] El controller no tiene lógica; el servicio no tiene reglas.
- [ ] Mensajes de error en palabras del dueño (skill `reglas-pedidos`, sección 1).
- [ ] `mvn test` completo en verde.
- [ ] Mapa de impacto y pruebas antes/después de todo lo que toca el cambio (skill `pruebas-de-impacto`).
- [ ] README del dominio, `CAMBIOS_FRONT.md` y `CLAUDE.md` (tablas) actualizados.
