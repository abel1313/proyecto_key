# infraestructura/dto/

> **[Hexagonal: parte del adaptador]**
> **[Clean: Interface Adapters]**

La forma del JSON que entra y sale por la API. **No son modelos del dominio.**

## Por qué separados del modelo

Si el controller devuelve el modelo de dominio directamente:

- Cualquier cambio interno del modelo **rompe el contrato del front** sin avisar
- Se filtran campos internos que el cliente no debería ver (rutas de disco, ids internos)
- El modelo termina llenándose de `@JsonIgnore` — o sea, decisiones de la API metidas
  en el corazón del negocio

## Convención de nombres

| Sufijo | Para qué |
|---|---|
| `...Request` | lo que entra |
| `...Response` | lo que sale |

## Ejemplo

```java
public record CarroRequest(
        @NotBlank String placa,
        @NotNull String modelo) {

    public DatosNuevoCarro aDatosNuevoCarro() {
        return new DatosNuevoCarro(placa, modelo);
    }
}

public record CarroResponse(String placa, String estado) {

    public static CarroResponse de(Carro carro) {
        return new CarroResponse(carro.getPlaca(), carro.getEstado().name());
    }
}
```

Las anotaciones de validación (`@NotBlank`, `@Size`) van **acá**, no en el modelo de
dominio: son validaciones del formato de entrada, no reglas del negocio. La regla de
negocio equivalente igual se valida en el modelo — el DTO solo evita que basura obvia
llegue hasta ahí.
