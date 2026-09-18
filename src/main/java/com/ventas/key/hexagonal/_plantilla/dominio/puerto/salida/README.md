# dominio/puerto/salida/

> **[Hexagonal: Driven Port / puerto secundario]**
> **[Clean: Interface Adapter — aquí es donde se invierte la dependencia]**

Lo que este dominio **necesita del mundo exterior**, declarado por el dominio mismo.

Es la clave de toda la arquitectura: el dominio dice *"necesito guardar esto en algún
lado"* sin saber si atrás hay MySQL, un archivo o una API. Infraestructura implementa.

## Qué va
Interfaces de lo que se necesita: persistencia, otro servicio, disco, cola de mensajes.

## Qué NO va
- Implementaciones (van en `infraestructura/salida/`)
- Nombres que revelen la tecnología. `CarroRepositorioPort` sí; `CarroJpaRepository` no —
  si el nombre dice "Jpa", el dominio ya sabe demasiado.

## Ejemplo

```java
public interface CarroRepositorioPort {
    Optional<Carro> buscarPorPlaca(String placa);
    Carro guardar(Carro carro);
}
```

## ⚠️ Un puerto = una responsabilidad

Esta regla nace de un problema real: en `micro_imagenes`, `ClienteDiscoPort` se llama
"cliente disco" pero la mitad de sus métodos consultan la base de datos. Resultado: no se
puede reusar la escritura en disco sin arrastrar la BD detrás.

**Mal** — un puerto que hace de todo:
```java
public interface AlmacenPort {
    String escribirEnDisco(byte[] bytes, String nombre);
    Imagen buscarEnBD(Long id);          // ← esto no es disco
    void publicarEnRabbit(Long id);      // ← esto tampoco
}
```

**Bien** — un puerto por cosa:
```java
public interface AlmacenArchivoPort {    // solo disco
    String escribir(byte[] contenido, String nombreOriginal);
    void borrar(String nombreArchivo);
}

public interface ImagenRepositorioPort { // solo persistencia
    Optional<Imagen> buscarPorId(Long id);
}
```

El caso de uso usa los dos. Cada uno se puede reusar por separado.
