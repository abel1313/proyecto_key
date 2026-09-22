# Dominio: rifa (boletos por participacion en redes)

Reglas del dominio. **Pendiente — se hace al final**, después de cerrar stock y la edición de
pedidos. Se anota ahora para no volver a explicarlo (pedido 2026-09-22).

---

## Lo que pasa hoy

### Problema 1 — la pantalla obliga a hacer scroll de ida y vuelta

Con muchos concursantes cargados, para agregar uno nuevo hay que bajar hasta el final de la
lista y después volver a subir. **Los concursantes ya cargados tienen que poder colapsarse**
para que el alta quede siempre a mano.

### Problema 2 — cada participación obliga a recargar todo de nuevo

Hoy, para registrar que un cliente participó, se carga: nombre → plataforma → URL de su
perfil. Eso da **un** boleto.

Si el mismo cliente hizo varias cosas en la misma red (dio like, compartió, comentó), hay
que repetir **todo** el alta por cada una — nombre, plataforma y perfil incluidos. Y quedan
como registros sueltos, sin que se vea que son la misma persona en la misma plataforma.

---

## Lo que se quiere

El alta se hace **una vez por cliente y plataforma**, y después se suman participaciones:

```
Cliente:    Juan Perez
Plataforma: Facebook
Perfil:     facebook.com/juan.perez        ← se carga UNA vez
   ├── url de lo que hizo (dio like)       → 1 boleto
   ├── url de lo que hizo (compartio)      → 1 boleto
   └── url de lo que hizo (comento)        → 1 boleto
                                              3 boletos
```

### R1 — Cada URL de participacion es un boleto
No cada alta. Si el cliente hizo tres cosas, tiene tres boletos aunque se hayan cargado en
una sola pasada.

### R2 — La clave es (plataforma + perfil del cliente)
Esa combinacion identifica al cliente en esa red y **no se repite**. Todas sus participaciones
cuelgan de ahi. Hoy dos participaciones del mismo perfil quedan como filas sueltas: se ve
"Facebook · juan · like" y aparte "Facebook · juan · compartio", cuando es la misma persona
en la misma red.

### R3 — Al actualizar se agregan o se quitan participaciones
Sin volver a cargar nombre, plataforma ni perfil. Se abre el grupo y se suma o resta una URL.

### R4 — Se muestra agrupado, no en filas sueltas
Un renglon por (plataforma + perfil), con sus URLs adentro y el total de boletos que suman.

---

## Lo que ya existe y sirve

`BoletoRifa` (ver `entity/`) ya tiene casi la forma correcta:

| Campo | Para que |
|---|---|
| `concursante` | el cliente |
| `plataforma` | enum de la red |
| `urlPerfilRedSocial` | el perfil del cliente en esa red |
| `urlsCompartido` | **lista** de URLs de participacion |
| `motivo`, `fecha`, `descartado` | metadatos |

O sea que la estructura de "un perfil con varias URLs" **ya se puede representar**. Lo que
falta es:

1. que el **conteo de boletos** use la cantidad de URLs, no la cantidad de filas
2. que el alta permita cargar varias URLs de una sin repetir cabecera
3. que la consulta devuelva agrupado por (plataforma + perfil)
4. que exista forma de agregar/quitar una URL de un grupo ya cargado
5. la restriccion de unicidad de (concursante + plataforma + perfil)

**Antes de tocar nada hay que revisar como cuenta los boletos el sorteo** (ver
`BoletoRifaPublicoTest` y el servicio de ruleta): si hoy cuenta filas, pasar a contar URLs
cambia las probabilidades de todos los concursantes ya cargados.

---

## Dudas a resolver antes de modelar

1. **Los boletos ya cargados** — ¿se migran al esquema agrupado, o los viejos quedan como
   estan y solo los nuevos usan el formato?
2. **Una URL de participacion repetida** — si se pega dos veces el mismo link, ¿son dos
   boletos o se rechaza como duplicado?
3. **Un cliente en dos plataformas** — Facebook e Instagram: ¿son dos grupos separados (uno
   por plataforma) o un solo cliente con dos redes? Por R2 serian dos grupos; confirmar.
