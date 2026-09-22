# Plantilla de dominio

Este es el **molde**. No se compila nada de aquí — son solo carpetas con su README.

## Cómo se usa

```bash
cp -r _plantilla/ <nombre-del-dominio>/
```

Después se borran las carpetas que ese dominio no necesite (no todo dominio habla con
un cliente HTTP, por ejemplo) y se reemplazan los README por código real.

---

## PASO 0 — Antes de escribir una sola clase: las reglas del dominio

Esta es la parte que más se salta y la que más caro sale. **Primero se escriben y se
acuerdan las reglas, después se modela.**

El problema típico: llega el pedido como *"quiero vender carros, que el producto sea un
carro con estas características y que corra"*. Eso no alcanza para modelar. Faltan las
reglas que nadie dijo en voz alta pero que el dominio necesita:

- ¿Un carro apagado puede correr? ¿Hay que encenderlo primero?
- ¿Se puede encender un carro que ya está encendido?
- ¿Se puede apagar en movimiento?
- ¿Qué pasa si no tiene gasolina?

Sin eso, el modelo termina siendo una bolsa de setters y las reglas se desparraman por
los controllers.

### Checklist para acordar un dominio nuevo

Antes de crear el modelo, responder por escrito:

**Identidad**
- [ ] ¿Qué identifica de forma única a esta cosa?
- [ ] ¿Se crea, o ya existe y solo se modifica? (no todo dominio tiene "alta")
- [ ] ¿Se puede borrar? ¿Borrado real o baja lógica?

**Estados y transiciones**
- [ ] ¿Qué estados puede tener?
- [ ] ¿Qué transiciones son válidas y cuáles no? (apagado → encendido sí; encendido → encendido no)
- [ ] ¿Hay un estado inicial obligatorio?

**Invariantes** — lo que SIEMPRE tiene que ser verdad
- [ ] ¿Qué combinación de datos es imposible? (ej: activo = true sin imagen)
- [ ] ¿Hay un mínimo o un máximo? (ej: el carrusel nunca puede quedar sin ninguna imagen)
- [ ] ¿Qué campos son obligatorios de verdad, y cuáles opcionales?

**Validaciones de entrada**
- [ ] Rangos, formatos, tamaños máximos
- [ ] ¿Enum cerrado o texto libre? (si son 2 o 3 valores fijos, es enum)

**Efectos de borde**
- [ ] ¿Qué pasa con lo que reemplaza? (al subir una imagen nueva, ¿la vieja se borra? ¿antes o después de confirmar la nueva?)
- [ ] ¿Qué pasa si dos personas lo modifican a la vez?
- [ ] Si algo falla a mitad, ¿qué queda inconsistente?

**Quién puede**
- [ ] ¿Es público o requiere permiso?
- [ ] ¿Distintos roles ven o pueden cosas distintas?

---

## PASO 1 en adelante — el orden de construcción

Siempre de adentro hacia afuera:

1. `dominio/modelo/` — el modelo y sus reglas. **Sin Spring, sin JPA.**
2. `dominio/excepcion/` — qué puede salir mal, en lenguaje del negocio
3. `dominio/puerto/salida/` — qué necesita del mundo exterior (interfaces)
4. `dominio/puerto/entrada/` — qué sabe hacer, de cara afuera (interfaces)
5. `aplicacion/servicio/` — implementa los puertos de entrada, orquesta
6. `infraestructura/salida/` — implementa los puertos de salida (JPA, HTTP, disco)
7. `infraestructura/entrada/rest/` — el controller
8. `infraestructura/dto/` — request y response del controller

Si se arranca por el controller, el diseño termina girando alrededor del JSON en vez de
alrededor del negocio.
