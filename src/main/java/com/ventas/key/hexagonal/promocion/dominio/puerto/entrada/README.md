# dominio/puerto/entrada/

> **[Hexagonal: Driving Port / puerto primario]**
> **[Clean: Use Case boundary — la frontera de los casos de uso]**

Lo que este dominio **sabe hacer**, visto desde afuera. Son interfaces: el contrato que
el mundo exterior puede pedirle.

Quien las llama: los adaptadores de entrada (`infraestructura/entrada/rest/`).
Quien las implementa: `aplicacion/servicio/`.

## Qué va
Interfaces con los casos de uso, nombrados como acciones del negocio.

## Qué NO va
- Implementaciones
- Tipos de Spring (`ResponseEntity`, `Pageable`, `MultipartFile`) — esos son del
  framework; el puerto habla en tipos del dominio

## Ejemplo

```java
public interface GestionarCarroCasoUso {

    Carro registrar(DatosNuevoCarro datos);
    void encender(String placa);
    void apagar(String placa);
}
```

## Por qué existe esta interfaz si hay una sola implementación

Para que el controller dependa del **dominio** y no de una clase concreta de
`aplicacion/`. Esa es la inversión de dependencia: la flecha apunta hacia adentro.
