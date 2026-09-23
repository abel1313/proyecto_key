# Arquitectura Hexagonal + Clean Architecture

Todo lo **nuevo** que arranque de cero en `proyecto_key` se escribe aquí, con esta
estructura. Lo viejo (`mis/productos/controller`, `service`, `entity`, `repository`)
se queda donde está y se migra **solo cuando haya que tocarlo por otra razón** — no
hay refactor masivo.

---

## Las dos arquitecturas, y por qué las dos

Son dos formas de decir casi lo mismo. Se usan juntas porque cada una aporta un
vocabulario distinto, y en este proyecto conviene tener los dos nombres a mano:

| | Qué aporta |
|---|---|
| **Hexagonal** (Ports & Adapters) | El vocabulario de **puerto** y **adaptador**. Responde *"¿cómo entra y sale la información del núcleo?"* |
| **Clean Architecture** | El vocabulario de **capas concéntricas** y la **regla de dependencia**. Responde *"¿quién puede importar a quién?"* |

En cada carpeta de este árbol el README dice a qué corresponde en **una y otra**,
marcado así:

```
[Hexagonal: Driven Port]   [Clean: Interface Adapter]
```

---

## La regla que manda: dependencias hacia adentro

```
        infraestructura  ──importa──▶  aplicacion  ──importa──▶  dominio
              ▲                                                      │
              └──────────────── NUNCA al revés ◀────────────────────┘
```

- `dominio/` **no importa nada** de `aplicacion/` ni de `infraestructura/`.
  Tampoco importa Spring, JPA, Jackson ni nada de framework.
- `aplicacion/` importa de `dominio/`. Nada de infraestructura.
- `infraestructura/` importa de las dos. Aquí sí viven Spring, JPA, WebClient, Rabbit.

**Cómo se invierte la dependencia:** el dominio declara una *interfaz* de lo que
necesita (`puerto/salida/`), e infraestructura la *implementa*. El dominio nunca
sabe si detrás hay MySQL, un archivo o una llamada HTTP.

---

## Estructura: primero el dominio, después la capa

```
hexagonal/
├── _plantilla/          ← el molde. Se copia y se renombra para cada dominio nuevo
├── imagen/
│   ├── dominio/
│   ├── aplicacion/
│   └── infraestructura/
└── presentacion/
    ├── dominio/
    ├── aplicacion/
    └── infraestructura/
```

**Por qué el dominio va primero:** cada dominio queda autocontenido. Si mañana
`presentacion` se muda a `micro_imagenes`, se mueve la carpeta entera y listo. Además
la estructura dice de qué trata el sistema (imágenes, presentación) y no qué framework
usa (controllers, services, entities).

---

## Cómo arrancar un dominio nuevo

1. **Primero las reglas, después el código.** Antes de crear una sola clase se
   escriben y se acuerdan las reglas del dominio (ver `_plantilla/README.md`).
2. Copiar `_plantilla/` y renombrarla con el nombre del dominio.
3. Borrar los `README.md` de las carpetas que ese dominio no use.
4. Empezar por `dominio/modelo/` — el modelo y sus reglas. Sin Spring.
5. Después los puertos, después el caso de uso, y al final infraestructura.

---

## Una responsabilidad por método

Regla que nace de un problema real de este proyecto: `ClienteDiscoPort` en
`micro_imagenes` se llama "cliente disco" y la mitad de sus métodos consultan la base
de datos. Resultado: no se puede reusar la escritura en disco sin arrastrar la BD.

Un método que **escribe en disco** escribe en disco. No guarda en BD, no publica a
Rabbit, no invalida caché. El que orquesta es otro, y llama a los tres.
