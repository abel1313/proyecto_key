# dominio/

> **[Hexagonal: el hexágono — el núcleo]**
> **[Clean: Entities + Enterprise Business Rules — el círculo de más adentro]**

El corazón. Las reglas del negocio que serían verdad aunque cambiáramos de base de
datos, de framework, o aunque esto no fuera una app web.

## Qué va
- Modelos con sus reglas (`modelo/`)
- Interfaces de lo que el dominio necesita y ofrece (`puerto/`)
- Excepciones en lenguaje de negocio (`excepcion/`)

## Qué NO va
- `@Entity`, `@Table`, `@Column` — eso es JPA, va en `infraestructura/salida/persistencia/`
- `@Service`, `@Component`, `@Autowired` — eso es Spring
- `@JsonProperty` — eso es Jackson, va en `infraestructura/dto/`
- Cualquier `import org.springframework.*`, `jakarta.persistence.*`, `com.fasterxml.*`

## Cómo verificar que está bien
Abrí cualquier archivo de esta carpeta y mirá los `import`. Si hay uno de framework,
está mal ubicado.

Otra prueba: **¿podrías compilar esta carpeta sola, sin Spring en el classpath?** Si la
respuesta es no, algo se coló.
