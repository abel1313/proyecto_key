# infraestructura/salida/persistencia/

> **[Hexagonal: Driven Adapter / adaptador secundario]**
> **[Clean: Frameworks & Drivers — la BD]**

Implementa los puertos de salida que tienen que ver con guardar y leer datos.

## Qué va
- Entidades JPA (`@Entity`, `@Table`, `@Column`)
- Repositorios de Spring Data
- El **adaptador**, que implementa el puerto y traduce entidad ↔ modelo de dominio

## Tres piezas, no una

```java
// 1. La ENTIDAD JPA — vive solo acá, nunca sale de esta carpeta
@Entity
@Table(name = "carro")
class CarroEntity {
    @Id private Long id;
    @Column(name = "placa") private String placa;
    @Column(name = "estado") private String estado;
}

// 2. El REPOSITORIO de Spring Data
interface CarroJpaRepository extends JpaRepository<CarroEntity, Long> {
    Optional<CarroEntity> findByPlaca(String placa);
}

// 3. El ADAPTADOR — implementa el puerto del dominio y traduce
@Component
@RequiredArgsConstructor
public class CarroRepositorioAdapter implements CarroRepositorioPort {

    private final CarroJpaRepository jpa;

    @Override
    public Optional<Carro> buscarPorPlaca(String placa) {
        return jpa.findByPlaca(placa).map(this::aDominio);   // Entity → modelo
    }

    private Carro aDominio(CarroEntity e) { ... }
}
```

## Por qué no usar la @Entity como modelo de dominio

Es tentador y es la trampa más común. Si el modelo de dominio es la entidad JPA:

- El dominio queda atado a la forma de la tabla; cambiar la BD obliga a cambiar reglas
- Aparecen `LazyInitializationException` en lugares donde nadie espera una BD
- Hibernate necesita constructor vacío y setters → adiós a las invariantes del modelo
- No se puede testear el dominio sin levantar JPA

Son dos clases distintas a propósito, y el adaptador traduce entre ellas.
