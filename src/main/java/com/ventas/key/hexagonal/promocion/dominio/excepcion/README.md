# dominio/excepcion/

> **[Hexagonal: dentro del hexágono]**
> **[Clean: Entities]**

Qué puede salir mal, **en lenguaje del negocio**.

## Qué va
Excepciones con nombre de negocio: `CarroYaEncendidoException`, `PlacaInvalidaException`.

## Qué NO va
- `HttpStatus`, `ResponseStatusException` — traducir la excepción a un código HTTP es
  trabajo del `@RestControllerAdvice`, en infraestructura
- Excepciones genéricas tipo `RuntimeException("error")`: no dicen qué pasó ni permiten
  que quien llama reaccione distinto según el caso

## Ejemplo

```java
public class CarroApagadoException extends RuntimeException {
    public CarroApagadoException(String placa) {
        super("El carro " + placa + " está apagado: hay que encenderlo antes de arrancar");
    }
}
```

El mensaje explica **qué pasó y qué hacer**. Ese texto suele terminar en la pantalla del
usuario, así que se escribe pensando en quien lo va a leer.
