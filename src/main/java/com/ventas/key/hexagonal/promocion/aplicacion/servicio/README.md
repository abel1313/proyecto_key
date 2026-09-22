# aplicacion/servicio/

> **[Hexagonal: implementación del puerto de entrada]**
> **[Clean: Use Case Interactor]**

Implementa las interfaces de `dominio/puerto/entrada/`. Usa las de
`dominio/puerto/salida/` para hablar con el exterior.

## Qué va
- `@Service` de Spring (esta es la única capa interna donde se acepta, por la inyección)
- `@Transactional`
- La secuencia de pasos de cada caso de uso

## Qué NO va
- Reglas de negocio → `dominio/modelo/`
- `@RestController`, `ResponseEntity` → `infraestructura/entrada/rest/`
- `@Entity`, `EntityManager`, SQL → `infraestructura/salida/persistencia/`
- Llamadas directas a `WebClient`, `RestTemplate`, `Files.write()` → esos van detrás de
  un puerto de salida

## Ejemplo

```java
@Service
@RequiredArgsConstructor
public class GestionarCarroService implements GestionarCarroCasoUso {

    private final CarroRepositorioPort repositorio;      // puerto, no implementación
    private final NotificacionPort notificacion;

    @Override
    @Transactional
    public void encender(String placa) {
        Carro carro = repositorio.buscarPorPlaca(placa)
                .orElseThrow(() -> new CarroNoEncontradoException(placa));

        carro.encender();          // ← la REGLA la aplica el modelo, no este service

        repositorio.guardar(carro);
        notificacion.avisarEncendido(placa);
    }
}
```

El service no pregunta `if (carro.getEstado() == ENCENDIDO)`. Eso sería sacarle la regla
al modelo y dejarla suelta acá — y el día que otro service haga lo mismo, la regla
quedaría duplicada y podrían quedar desincronizadas.

## ⚠️ Un método = una responsabilidad

Un método que escribe en disco escribe en disco. No guarda en BD ni publica a Rabbit ni
invalida caché. Si un caso de uso necesita las cuatro cosas, las llama a las cuatro —
pero cada una sigue siendo reutilizable por separado.

**El costo de no hacerlo:** en `ProductosServiceImpl` había un método que escribía la
imagen en disco *y* la subía al micro. Resultado: cada imagen quedaba guardada dos veces
(ver `DUPLICADO_IMAGENES_AGREGAR_MODELO.md`).
