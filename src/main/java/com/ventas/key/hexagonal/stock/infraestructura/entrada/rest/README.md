# infraestructura/entrada/rest/

> **[Hexagonal: Driving Adapter / adaptador primario]**
> **[Clean: Interface Adapters — Controllers]**

Traduce HTTP a llamadas del dominio y de vuelta. **Nada más que eso.**

## Qué va
- `@RestController`, `@RequestMapping`, `@GetMapping`...
- Convertir DTO → modelo de dominio y modelo → DTO
- Códigos HTTP

## Qué NO va
- Reglas de negocio (`if` que decide algo del negocio)
- Acceso a repositorios o a la BD
- Llamadas a otros servicios

Un controller que hace más de 5 líneas por método, casi seguro tiene lógica que le
pertenece a otra capa.

## Versionado de URL

El `/v1/` va en el `@RequestMapping` de la **clase**, nunca en el método:

```java
@RestController
@RequestMapping("/v1/carros")        // ✅ acá
public class CarroController {

    @PostMapping("/{placa}/encender")   // ✅ sin /v1/
    public ResponseEntity<Void> encender(@PathVariable String placa) {
        casoUso.encender(placa);
        return ResponseEntity.noContent().build();
    }
}
```

## Ejemplo completo

```java
@RestController
@RequestMapping("/v1/carros")
@RequiredArgsConstructor
public class CarroController {

    private final GestionarCarroCasoUso casoUso;    // el PUERTO, no el service concreto

    @PostMapping
    public ResponseEntity<CarroResponse> registrar(@RequestBody @Valid CarroRequest req) {
        Carro carro = casoUso.registrar(req.aDatosNuevoCarro());
        return ResponseEntity.status(HttpStatus.CREATED).body(CarroResponse.de(carro));
    }
}
```

El controller depende de `GestionarCarroCasoUso` (dominio), no de
`GestionarCarroService` (aplicación). Flecha hacia adentro.
