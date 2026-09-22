# dominio/modelo/

> **[Hexagonal: dentro del hexágono]**
> **[Clean: Entities]**

Los objetos del negocio **con sus reglas adentro**. No son estructuras de datos con
getters y setters — son objetos que protegen sus propias reglas.

## Qué va
Clases Java planas que representan conceptos del negocio y validan sus invariantes.

## Qué NO va
- Anotaciones de JPA o Spring
- Modelos anémicos: puro `@Getter @Setter` sin ninguna regla. Si el modelo no tiene
  reglas, probablemente las reglas se están escapando al controller o al service.

## Ejemplo

```java
public class Carro {

    private final String placa;
    private EstadoMotor estado;

    public Carro(String placa) {
        if (placa == null || placa.isBlank()) {
            throw new PlacaInvalidaException("La placa es obligatoria");
        }
        this.placa = placa;
        this.estado = EstadoMotor.APAGADO;   // estado inicial, no se elige desde afuera
    }

    // La regla vive AQUÍ, no en el service ni en el controller
    public void encender() {
        if (estado == EstadoMotor.ENCENDIDO) {
            throw new CarroYaEncendidoException(placa);
        }
        this.estado = EstadoMotor.ENCENDIDO;
    }

    public void correr() {
        if (estado != EstadoMotor.ENCENDIDO) {
            throw new CarroApagadoException(placa);   // no se puede correr apagado
        }
        // ...
    }
}
```

Fijate que **no hay `setEstado()`**. El estado solo cambia por `encender()` o `apagar()`,
y esos métodos son los que hacen cumplir la regla. Si hubiera un setter público, cualquiera
podría poner el carro en ENCENDIDO sin pasar por la validación.
