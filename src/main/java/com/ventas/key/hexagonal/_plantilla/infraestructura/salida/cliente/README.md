# infraestructura/salida/cliente/

> **[Hexagonal: Driven Adapter]**
> **[Clean: Frameworks & Drivers]**

Todo lo que sale de esta aplicación y no es la base de datos.

## Qué va
- Clientes HTTP (`WebClient`, `RestTemplate`) hacia otros servicios — por ejemplo `micro_imagenes`
- Productores de RabbitMQ
- Acceso al sistema de archivos
- Envío de correo

Cada uno implementa un puerto de `dominio/puerto/salida/`.

## Ejemplo

```java
@Component
@RequiredArgsConstructor
public class ImagenClienteHttp implements AlmacenArchivoPort {

    private final WebClient webClient;

    @Override
    public String escribir(byte[] contenido, String nombreOriginal) {
        // multipart, timeouts, manejo de error del micro...
    }
}
```

El caso de uso solo conoce `AlmacenArchivoPort`. No sabe que hay HTTP de por medio, ni
que existe un micro. Si mañana se cambia por S3, se escribe otro adaptador y el dominio
ni se entera.

## Timeouts: siempre

Todo cliente HTTP lleva timeout de conexión **y** de respuesta. Sin timeout de respuesta,
si el otro servicio acepta la conexión y después no contesta, el hilo se queda esperando
para siempre; con el pool de Tomcat lleno de hilos así, se cuelga la aplicación entera —
no solo la pantalla que hizo la llamada.

Referencia de cómo está resuelto hoy: `hexagonal/infraestructura/ImageneClienteDisco.java`
en `mis/productos/`.
