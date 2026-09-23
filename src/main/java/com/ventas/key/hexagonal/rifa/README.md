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

## Decisiones tomadas (2026-09-22)

Las tres dudas de abajo quedaron cerradas por el usuario, y una cuarta salio de revisar el
codigo del sorteo. Estan aca arriba porque condicionan todo el modelado.

### D1 — Los boletos viejos quedan como estan
No se migra nada. Las filas ya cargadas siguen igual y entran solas en los grupos nuevos,
porque hoy ya son una fila por participacion. Cero riesgo sobre datos de produccion.

### D2 — La URL de participacion se carga en uno de dos modos
Cada participacion lleva **una sola URL**, y al darla de alta se elige como se valida:

| Modo | Que hace |
|---|---|
| `UNICA` | El back rechaza la URL si **ese mismo perfil** (misma red + mismo link del cliente) ya la tiene en esa rifa, y dice de quien es |
| `REPETIDA_PERMITIDA` | Se acepta aunque ya exista |

**Que cuenta como duplicado** (corregido 2026-09-23): red + perfil del cliente + URL de la
publicacion. Otro participante con la misma publicacion **no** es duplicado: en un sorteo por
publicacion todos pegan la misma URL. Antes se buscaba en toda la rifa y el segundo
participante se rechazaba como "ya dado de alta".

El mismo perfil en la misma publicacion es el caso "compartio y ademas comento": el back
contesta 409, el front pregunta si es otra participacion y la reenvia como
`REPETIDA_PERMITIDA`.

Se llena **una o la otra, nunca las dos**: una participacion es un boleto, y llenar las dos
no lo convierte en dos. El modo es por participacion, no una configuracion global: el caso
real es "normalmente quiero que sea unica, pero esta en particular se repite de verdad y
necesito poder cargarla igual".

### D3 — Facebook e Instagram del mismo cliente son dos grupos
Confirmada R2 tal cual: la clave es (plataforma + perfil). Los boletos se suman al total
del cliente, pero cada red se carga y se ve por separado.

### D5 — El Problema 1 (scroll) se reparte entre back y front

El colapsar en si es del front: el back no pliega renglones. Lo que aporta el back:

| Pieza | Quien | Estado |
|---|---|---|
| Un renglon por perfil en vez de uno por participacion | back | ✅ el agrupamiento |
| El total de boletos visible sin abrir el grupo | back | ✅ `totalBoletos` |
| Todo en una sola llamada (expandir no pide nada mas) | back | ✅ participaciones anidadas |
| Lo ultimo cargado arriba, no al final de la lista | back | ✅ orden por `ultimaParticipacion` |
| Plegar/desplegar el renglon, y que el alta quede fija | front | ⬜ pendiente |

El orden importa mas de lo que parece: con orden de insercion, el grupo recien cargado queda
**al final**, que es literalmente "bajar hasta abajo y volver a subir". Ahora sale primero.

El front puede reordenar como quiera — `ultimaParticipacion` viaja en el response.

---

### D4 — La fila sigue siendo el boleto 🔴 la que mas condiciona

**Hallazgo al revisar el sorteo antes de modelar.** En `BoletoRifaServiceImpl.sortear()`:

```java
List<BoletoRifa> enJuego = iBoletoRifaRepository.findEnJuegoByRifaId(rifaId);
BoletoRifa elegido = enJuego.get(new Random().nextInt(enJuego.size()));
```

El sorteo elige una **fila**, uniforme. `urlsCompartido` **no afecta las probabilidades hoy**,
y `descartado` tambien es por fila.

Consecuencia: si el rediseno agrupara las 3 participaciones de un perfil en UNA fila con 3
URLs, esa persona pasaria de 3 chances a 1 — lo contrario de R1.

**Por eso el agrupamiento es de lectura, no de almacenamiento:**

- por debajo se sigue creando **una fila por URL de participacion** (una fila = un boleto = una chance)
- el alta carga nombre, plataforma y perfil **una sola vez** y genera las N filas
- la consulta las devuelve **agrupadas** por (plataforma + perfil)
- **el sorteo no se toca**, y nadie cambia de probabilidad

Esto cumple R1, R2, R3 y R4 sin tocar `GanadorRifaServiceImpl` ni `BoletoRifaServiceImpl.sortear()`,
y es lo unico compatible con D1.

`urlsCompartido` (la `@ElementCollection`) queda como esta para no romper los datos viejos,
pero **el formato nuevo no la usa**: la URL de participacion del boleto nuevo va en su propia
fila. No agregar URLs ahi pensando que suman boletos — no suman.

---

## Dudas a resolver antes de modelar (RESUELTAS — ver arriba)

1. **Los boletos ya cargados** — ¿se migran al esquema agrupado, o los viejos quedan como
   estan y solo los nuevos usan el formato?
2. **Una URL de participacion repetida** — si se pega dos veces el mismo link, ¿son dos
   boletos o se rechaza como duplicado?
3. **Un cliente en dos plataformas** — Facebook e Instagram: ¿son dos grupos separados (uno
   por plataforma) o un solo cliente con dos redes? Por R2 serian dos grupos; confirmar.
