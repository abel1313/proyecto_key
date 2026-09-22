# aplicacion/

> **[Hexagonal: dentro del hexágono, pegado al borde]**
> **[Clean: Use Cases — Application Business Rules]**

Orquesta. **No decide reglas de negocio** (esas están en `dominio/modelo/`): coordina
los pasos, en orden, y maneja qué hacer si algo falla a mitad.

## La diferencia con dominio/modelo/

| | Dónde vive |
|---|---|
| *"Un carro apagado no puede correr"* | `dominio/modelo/Carro.java` — es una regla del negocio |
| *"Buscar el carro, encenderlo, guardarlo y avisar por Rabbit"* | `aplicacion/servicio/` — es una secuencia de pasos |

Si te encontrás escribiendo un `if` con una regla de negocio acá, probablemente va en el
modelo.
